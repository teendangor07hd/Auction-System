package com.bidhub.client.network;

// [UPDATED] Các import đã được trỏ về đúng module bidhub-common
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

/**
 * Cổng giao tiếp duy nhất từ client đến server (Singleton Pattern).
 *
 * <p>Quản lý một kết nối TCP duy nhất. Đảm bảo đóng kết nối cũ trước khi mở
 * kết nối mới để tránh rò rỉ tài nguyên (Resource Leak).
 * Mọi network call phải bọc trong {@link NetworkTask} để không làm đơ (block) FX thread.
 *
 * <p>// 📌 [B5] connect()/disconnect()/isConnected() đều synchronized → tránh race condition.
 * <p>// 📌 [B6] socket timeout 30s → không block mãi nếu server không phản hồi.
 * <p>// 📌 [B7] disconnect() đóng writer → reader → socket theo đúng thứ tự.
 * <p>// 📌 [B9] connect timeout 5s thay vì new Socket() block mãi.
 */
public final class ServerGateway {

    /** Timeout đọc response — 30 giây. */
    private static final int READ_TIMEOUT_MS = 30_000;

    /** Timeout kết nối — 5 giây. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

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
            System.err.println("[ServerGateway] Lỗi đọc config, dùng cấu hình mặc định: " + e.getMessage());
            serverHost = "localhost";
            serverPort = 9090;
        }
    }

    /**
     * Mở kết nối TCP đến server.
     * Tự động dọn dẹp (cleanup) kết nối cũ nếu đang tồn tại.
     *
     * <p>// 📌 [B5] synchronized — tránh race với sendRequest()/disconnect().
     * <p>// 📌 [B9] Dùng socket.connect() với connect timeout 5s thay vì new Socket(host, port)
     * để không block mãi nếu server không reachable.
     *
     * @param host hostname server
     * @param port cổng server
     * @throws IOException nếu không kết nối được
     */
    public synchronized void connect(String host, int port) throws IOException {
        // Tránh Resource Leak: Ngắt kết nối cũ trước khi tạo kết nối mới
        if (isConnectedUnsafe()) {
            System.out.println("[ServerGateway] Đóng kết nối cũ để chuẩn bị kết nối mới...");
            disconnectUnsafe();
        }

        // [B9] connect timeout 5s — không block mãi nếu server unreachable
        Socket newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

        // [B6] socket read timeout 30s — không block mãi khi server không phản hồi
        newSocket.setSoTimeout(READ_TIMEOUT_MS);

        this.socket = newSocket;
        // autoFlush=true để println() đẩy data (flush) đi ngay lập tức
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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
        if (!isConnectedUnsafe()) {
            throw new IOException("Chưa kết nối server. Vui lòng kiểm tra lại mạng hoặc gọi connect() trước.");
        }

        // Gửi data (Serialize object thành chuỗi JSON)
        writer.println(MessageMapper.toJson(request));

        // Chờ nhận data từ Server (luồng sẽ đứng đợi ở đây, tối đa READ_TIMEOUT_MS)
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

    /**
     * Đóng kết nối TCP và giải phóng tài nguyên mạng.
     *
     * <p>// 📌 [B5] synchronized — tránh race với connect()/sendRequest().
     */
    public synchronized void disconnect() {
        disconnectUnsafe();
    }

    /**
     * Nội bộ: đóng kết nối KHÔNG synchronized — chỉ gọi từ method đã synchronized.
     *
     * <p>// 📌 [B7] Đóng theo thứ tự: writer → reader → socket.
     */
    private void disconnectUnsafe() {
        // [B7] Đóng writer trước để flush buffer và signal EOF phía server
        if (writer != null) {
            try { writer.close(); } catch (Exception ignored) {}
            writer = null;
        }
        // [B7] Đóng reader
        if (reader != null) {
            try { reader.close(); } catch (Exception ignored) {}
            reader = null;
        }
        // [B7] Đóng socket cuối cùng
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                System.out.println("[ServerGateway] Đã ngắt kết nối an toàn (Disconnected).");
            } catch (IOException e) {
                System.err.println("[ServerGateway] Lỗi khi đóng socket: " + e.getMessage());
            }
            socket = null;
        }
    }

    /** Kiểm tra trạng thái mạng hiện tại (thread-safe). */
    public synchronized boolean isConnected() {
        return isConnectedUnsafe();
    }

    /** Nội bộ: kiểm tra kết nối KHÔNG synchronized. */
    private boolean isConnectedUnsafe() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    // [B8] getSocket() đã bị xóa — không expose internal socket state
}