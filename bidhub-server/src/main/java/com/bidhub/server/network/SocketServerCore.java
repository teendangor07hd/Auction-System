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
 * Loi server TCP — lang nghe ket noi moi, submit moi ket noi vao thread pool.
 *
 * <p>Fixed pool 30: phuc vu tai concurrent Tuan 7 ma khong spawn thread khong gioi han.
 * Goi {@link #start(int)} cuoi {@code ServerApp.main()} — blocking cho den khi {@link #shutdown()}.
 */
public final class SocketServerCore {

    private static final Logger logger = LoggerFactory.getLogger(SocketServerCore.class);

    // 📌 [Tieu chi: Kien truc Client–Server — 0.5d] Fixed pool tranh OOM khi nhieu client dong thoi
    private final ExecutorService threadPool = Executors.newFixedThreadPool(30);
    private ServerSocket serverSocket;
    private volatile boolean running = false; // volatile: shutdown() tu thread khac thay ngay

    /**
     * Bat dau lang nghe — blocking. Goi tu main thread sau khi tat ca setup xong.
     *
     * @param port cong lang nghe, doc tu ConfigLoader
     * @throws IOException neu khong bind duoc cong
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        logger.info("Dang lang nghe cong {}.", port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Session session = new Session(clientSocket);
                threadPool.submit(new ClientConnectionThread(session));
                logger.info("Client moi: {} | session={}", clientSocket.getInetAddress(), session.getSessionId());
            } catch (IOException e) {
                if (running) {
                    logger.error("Loi accept: {}", e.getMessage(), e);
                }
                // !running → ServerSocket da dong boi shutdown() → thoat vong lap binh thuong
            }
        }
    }

    /** Dung server — dong ServerSocket, shutdown pool, cho toi da 5 giay. */
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