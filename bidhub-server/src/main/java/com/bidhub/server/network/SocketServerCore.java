package com.bidhub.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Lõi server TCP — lắng nghe kết nối mới, submit mỗi kết nối vào thread pool.
 *
 * <p>Fixed pool 30: phục vụ tải concurrent Tuần 7 mà không spawn thread không giới hạn.
 * Gọi {@link #start(int)} cuối {@code ServerApp.main()} — blocking cho đến khi {@link #shutdown()}.
 */
public final class SocketServerCore {

    // 📌 [Tiêu chí: Kiến trúc Client–Server — 0.5đ] Fixed pool tránh OOM khi nhiều client đồng thời
    private final ExecutorService threadPool = Executors.newFixedThreadPool(30);
    private ServerSocket serverSocket;
    private volatile boolean running = false; // volatile: shutdown() từ thread khác thấy ngay

    /**
     * Bắt đầu lắng nghe — blocking. Gọi từ main thread sau khi tất cả setup xong.
     *
     * @param port cổng lắng nghe, đọc từ ConfigLoader
     * @throws IOException nếu không bind được cổng
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("[SocketServerCore] Đang lắng nghe cổng " + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Session session = new Session(clientSocket);
                threadPool.submit(new ClientConnectionThread(session));
                System.out.println("[SocketServerCore] Client mới: "
                        + clientSocket.getInetAddress() + " | session=" + session.getSessionId());
            } catch (IOException e) {
                if (running) {
                    System.err.println("[SocketServerCore] Lỗi accept: " + e.getMessage());
                }
                // !running → ServerSocket đã đóng bởi shutdown() → thoát vòng lặp bình thường
            }
        }
    }

    /** Dừng server — đóng ServerSocket, shutdown pool, chờ tối đa 5 giây. */
    public void shutdown() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[SocketServerCore] Server đã dừng.");
    }

    public boolean isRunning() {
        return running;
    }
}