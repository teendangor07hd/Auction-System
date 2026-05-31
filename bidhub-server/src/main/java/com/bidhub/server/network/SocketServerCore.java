package com.bidhub.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loi server TCP — lắng nghe kết nối moi, submit moi kết nối vao thread pool.
 *
 * <p>Fixed pool 30: phuc vu tai concurrent Tuan 7 ma không spawn thread không gioi han.
 * Goi {@link #start(int)} cuối {@code ServerApp.main()} — blocking cho đến khi {@link #shutdown()}.
 */
public final class SocketServerCore {

    private static final Logger logger = LoggerFactory.getLogger(SocketServerCore.class);

    private final ExecutorService threadPool;
    private final RequestHandler requestHandler = new RequestHandler();
    private ServerSocket serverSocket;
    private volatile boolean running = false; // volatile: shutdown() từ thread khac thay ngay

    public SocketServerCore() {
        int poolSize = com.bidhub.server.config.ConfigLoader.getIntOrDefault("server.poolSize", 30);
        this.threadPool = Executors.newFixedThreadPool(poolSize);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "server-shutdown-hook"));
    }

    /**
     * Bắt đầu lắng nghe — blocking. Goi từ main thread sau khi tat ca setup xong.
     *
     * @param port cong lắng nghe, đọc từ ConfigLoader
     * @throws IOException nếu không bind được cong
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        logger.info("Dang lang nghe cong {}.", port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Session session = new Session(clientSocket);
                threadPool.submit(new ClientConnectionThread(session, requestHandler));
                logger.info("Client moi: {} | session={}", clientSocket.getInetAddress(), session.getSessionId());
            } catch (IOException e) {
                if (running) {
                    logger.error("Loi accept: {}", e.getMessage(), e);
                }
                // !running → ServerSocket da đóng boi shutdown() → thoat vòng lặp binh thuong
            }
        }
    }

    /** Đúng server — đóng ServerSocket, shutdown pool, cho toi da 5 giay. */
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
        logger.info("Server da dung.");
    }

    public boolean isRunning() {
        return running;
    }
}