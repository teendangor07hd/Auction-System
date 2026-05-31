package com.bidhub.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import com.bidhub.server.service.NotificationBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Runnable xử lý 1 client: đọc JSON → RequestHandler → sendMessage.
 *
 * <p>Cleanup session trong finally — socket luon được đóng du có exception.
 */
public final class ClientConnectionThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientConnectionThread.class);

    private final Session session;
    private final RequestHandler handler;

    public ClientConnectionThread(Session session, RequestHandler handler) {
        this.session = session;
        this.handler = handler;
    }

    @Override
    public void run() {
        logger.info("Session bat dau: {}", session.getSessionId());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(session.getSocket().getInputStream()))) {

            String line;
            // readLine() → null khi client đóng connection (EOF) → thoat vòng lặp sach
            while ((line = reader.readLine()) != null) {
                String response = handler.handle(line, session);
                session.sendMessage(response);
            }

        } catch (IOException e) {
            // Client ngat dot ngot (Ctrl+C, kill process...) — không phai loi server
            logger.info("Client ngat: {}", session.getSessionId());
        } finally {
            session.disconnect();
            try {
                NotificationBroker.getInstance().unsubscribeAll(session);
            } catch (Exception e) {
                logger.error("unsubscribeAll loi: {}", e.getMessage(), e);
            }
            logger.info("Cleanup xong: {}", session.getSessionId());
        }
    }
}