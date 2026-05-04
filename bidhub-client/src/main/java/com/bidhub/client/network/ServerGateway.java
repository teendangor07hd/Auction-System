package com.bidhub.client.network;

// [UPDATED] Các import đã được trỏ về đúng module bidhub-common
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

/**
 * Cổng giao tiếp duy nhất từ client đến server (Singleton Pattern).
 *
 * <p>Quản lý một kết nối TCP duy nhất. Đảm bảo đóng kết nối cũ trước khi mở
 * kết nối mới để tránh rò rỉ tài nguyên (Resource Leak).
 * Mọi network call phải bọc trong {@link NetworkTask} để không làm đơ (block) FX thread.
 */
public final class ServerGateway {

    // Biến lưu trữ phiên bản duy nhất, dùng volatile để đảm bảo Thread-safe
    private static volatile ServerGateway instance;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String serverHost;
    private int serverPort;

    private ServerGateway() {
        loadConfig();
    }

    /** Trả về instance duy nhất (Double-checked locking). */
    public static ServerGateway getInstance() {
        if (instance == null) {
            synchronized (ServerGateway.class) {
                if (instance == null) {
                    instance = new ServerGateway();
                }
            }
        }
        return instance;
    }

    private void loadConfig() {
        try (var is = getClass().getResourceAsStream("/client.properties")) {
            Properties props = new Properties();
            if (is != null) props.load(is);
            serverHost = props.getProperty("server.host", "localhost");
            serverPort = Integer.parseInt(props.getProperty("server.port", "9090"));
        } catch (Exception e) {
            // In lỗi ra để tránh tình trạng Silent Fail (lỗi ngầm)
            System.err.println("[ServerGateway] Lỗi đọc config, dùng cấu hình mặc định: " + e.getMessage());
            serverHost = "localhost";
            serverPort = 9090;
        }
    }

    /**
     * Mở kết nối TCP đến server.
     * Tự động dọn dẹp (cleanup) kết nối cũ nếu đang tồn tại.
     *
     * @param host hostname server
     * @param port cổng server
     * @throws IOException nếu không kết nối được
     */
    public void connect(String host, int port) throws IOException {
        // Tránh Resource Leak: Ngắt kết nối cũ trước khi tạo kết nối mới
        if (isConnected()) {
            System.out.println("[ServerGateway] Đóng kết nối cũ để chuẩn bị kết nối mới...");
            disconnect();
        }

        socket = new Socket(host, port);
        // autoFlush=true để println() đẩy data (flush) đi ngay lập tức thay vì giữ trong bộ đệm
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        System.out.println("[ServerGateway] Kết nối thành công tới: " + host + ":" + port);
    }

    /**
     * Gửi request và chờ response — thao tác này có tính chất chặn (Blocking).
     * Dùng khóa (synchronized) để tránh Race Condition nếu nhiều luồng cùng gọi.
     * BẮT BUỘC gọi từ background thread (qua NetworkTask) để không làm treo giao diện.
     *
     * @param request đối tượng request đã được cấu trúc
     * @return đối tượng response từ server
     * @throws IOException nếu mất kết nối trong khi giao tiếp
     */
    public synchronized MessageResponse sendRequest(MessageRequest request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối server. Vui lòng kiểm tra lại mạng hoặc gọi connect() trước.");
        }

        // Gửi data (Serialize object thành chuỗi JSON)
        writer.println(MessageMapper.toJson(request));

        // Chờ nhận data từ Server (luồng sẽ đứng đợi ở đây)
        String responseLine = reader.readLine();

        if (responseLine == null) {
            throw new IOException("Server đóng kết nối bất ngờ (EOF - End Of File).");
        }

        try {
            // Deserialize chuỗi JSON ngược lại thành object MessageResponse
            return MessageMapper.fromJson(responseLine, MessageResponse.class);
        } catch (Exception e) {
            throw new IOException("Không thể parse JSON response từ server: " + responseLine, e);
        }
    }

    /** Đóng kết nối TCP và giải phóng tài nguyên mạng. */
    public void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                System.out.println("[ServerGateway] Đã ngắt kết nối an toàn (Disconnected).");
            } catch (IOException e) {
                System.err.println("[ServerGateway] Lỗi khi đóng socket: " + e.getMessage());
            }
        }
        // Gán các biến về trạng thái null để bộ thu gom rác (Garbage Collector) dọn dẹp
        socket = null;
        writer = null;
        reader = null;
    }

    /** Kiểm tra trạng thái mạng hiện tại. */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}