package com.bidhub.client.network;

/**
 * Singleton lưu trạng thái đăng nhập phía client.
 * Client Session làm nhiệm vụ ghi nhớ các thông tin người dùng và sẽ được các class khác
 * dùng để check các thuộc tính và trạng thái của người dùng.
 *
 * <p>Set bởi LoginController sau khi nhận response OK từ server.
 * Mọi Controller kiểm tra {@link #isLoggedIn()} trước khi thực hiện thao tác cần xác minh.
 *
 * <p>// 📌 [B3] login()/logout() đều synchronized → tránh race condition khi multi-thread.
 * <p>// 📌 [B4] getCurrentRole() trả empty string thay vì null → tránh "null" hiện trên UI.
 */
public final class ClientSession {

    private static volatile ClientSession instance;

    // [B3] Tất cả field volatile để đảm bảo visibility giữa các thread
    private volatile String token;
    private volatile String currentUserId;
    private volatile String currentUsername;
    private volatile String currentRole; // "BIDDER" | "SELLER" | "ADMIN"

    private ClientSession() {}

    /** Trả về instance duy nhất (thread-safe, double-checked locking). */
    public static ClientSession getInstance() {
        if (instance == null) {
            synchronized (ClientSession.class) {
                if (instance == null) {
                    instance = new ClientSession();
                }
            }
        }
        return instance;
    }

    /**
     * Lưu thông tin sau khi đăng nhập thành công.
     *
     * <p>// 📌 [B3] synchronized để atomic — tránh partial update giữa các thread.
     *
     * @param token    token UUID từ server
     * @param userId   id người dùng
     * @param username tên đăng nhập
     * @param role     "BIDDER" / "SELLER" / "ADMIN"
     */
    public synchronized void login(String token, String userId, String username, String role) {
        this.token = token;
        this.currentUserId = userId;
        this.currentUsername = username;
        this.currentRole = role;
    }

    /**
     * Reset toàn bộ. Gọi khi logout hoặc nhận lỗi auth từ server.
     *
     * <p>// 📌 [B3] synchronized để atomic.
     */
    public synchronized void logout() {
        this.token = null;
        this.currentUserId = null;
        this.currentUsername = null;
        this.currentRole = null;
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    public String getCurrentUserId() {
        return currentUserId != null ? currentUserId : "";
    }

    public String getCurrentUsername() {
        return currentUsername != null ? currentUsername : "";
    }

    public String getToken() {
        return token;
    }

    /**
     * Trả về role hiện tại.
     *
     * <p>// 📌 [B4] Trả về empty string thay vì null → tránh "null" hiện trên UI khi
     * gọi {@code String.valueOf(session.getCurrentRole())}.
     *
     * @return role string hoặc "" nếu chưa đăng nhập
     */
    public String getCurrentRole() {
        return currentRole != null ? currentRole : "";
    }
}
