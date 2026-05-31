package com.bidhub.server.network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

/**
 * Đại diện 1 kết nối TCP đang sống. Mỗi client có đúng 1 Session suốt vòng đời kết nối.
 *
 */
public final class Session {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

    private final String sessionId;
    private final Socket socket;
    private final PrintWriter out;
    private volatile String authenticatedUserId;
    private volatile com.bidhub.server.model.UserRole userRole;

    /**
     * Tạo Session từ socket của ServerSocket.accept().
     *
     * @param socket socket từ {@code ServerSocket.accept()}
     * @throws IOException nếu không lấy được OutputStream
     */
    public Session(Socket socket) throws IOException {
        this.sessionId = UUID.randomUUID().toString();
        this.socket = socket;
        // autoFlush=true: println() gửi ngay, không cần flush() thủ công
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Gửi 1 dòng JSON response tới client — thread-safe.
     *
     * @param jsonResponse chuỗi JSON đã serialize
     */
    public synchronized void sendMessage(String jsonResponse) {
        out.println(jsonResponse);
    }

    /** Đóng socket và dọn dẹp. Gọi từ finally của ClientConnectionThread. */
    public void disconnect() {
        try {
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.debug("Loi khi dong ket noi Session {}: {}", sessionId, e.getMessage());
        }
    }

    public boolean isAuthenticated() {
        return authenticatedUserId != null;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAuthenticatedUserId() {
        return authenticatedUserId;
    }

    public void setAuthenticatedUserId(String userId) {
        this.authenticatedUserId = userId;
    }

    public com.bidhub.server.model.UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(com.bidhub.server.model.UserRole role) {
        this.userRole = role;
    }

    public Socket getSocket() {
        return socket;
    }
}