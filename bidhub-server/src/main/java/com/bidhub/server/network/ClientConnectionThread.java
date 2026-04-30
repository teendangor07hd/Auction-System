package com.bidhub.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Runnable xử lý 1 client: đọc JSON → RequestHandler → sendMessage.
 *
 * <p>Cleanup session trong finally — socket luôn được đóng dù có exception.
 */
public final class ClientConnectionThread implements Runnable {

    private final Session session;
    private final RequestHandler handler;

    public ClientConnectionThread(Session session) {
        this.session = session;
        this.handler = new RequestHandler();
    }

    @Override
    public void run() {
        System.out.println("[ClientThread] Session bắt đầu: " + session.getSessionId());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(session.getSocket().getInputStream()))) {

            String line;
            // readLine() → null khi client đóng connection (EOF) → thoát vòng lặp sạch
            while ((line = reader.readLine()) != null) {
                String response = handler.handle(line, session);
                session.sendMessage(response);
            }

        } catch (IOException e) {
            // Client ngắt đột ngột (Ctrl+C, kill process...) — không phải lỗi server
            System.out.println("[ClientThread] Client ngắt: " + session.getSessionId());
        } finally {
            session.disconnect();
            System.out.println("[ClientThread] Cleanup xong: " + session.getSessionId());
        }
    }
}