package com.bidhub.client.network;

/**
 * Singleton lưu trạng thái đăng nhập phía client
 * Client Session làm nhiệm vụ ghi nhớ các thông tin người dùng và sẽ được các class khác
 * dùng để check các thuộc tính và trạng thái của người dùng
 * <p>Set bởi LoginController sau khi nhận response OK từ server
 * Mọi Controller kiểm tra {@link #isLoggedIn()} trước khi thực hiện thao tác cần xác minh
 */
public final class ClientSession {
    private static volatile ClientSession instance;

    private String token;
    private String currentUserId;
    private String currentUsername;
    private String currentRole; //BIDDER | SELLER | ADMIN


    private ClientSession(){}

    /** Trả về instance duy nhất (thread-safe, double-checked locking)*/
    public static ClientSession getInstance(){
        if(instance == null){
            synchronized (ClientSession.class){
                if(instance == null){
                    instance = new ClientSession();
                }
            }
        }
        return instance;
    }

    /**
     * Lưu thông tin sau khi đăng nhập thành công.
     *
     * @param token    token UUID từ server
     * @param userId   id người dùng
     * @param username tên đăng nhập
     * @param currentRole     "BIDDER" / "SELLER" / "ADMIN"
     */
    public void login(String token, String userId, String username, String currentRole){
        this.token = token;
        this.currentUserId = userId;
        this.currentUsername = username;
        this.currentRole = currentRole;
    }

    /** Reset toàn bộ. Gọi khi logout hoặc nhận lỗi auth từ server. */
    public void logout(){
        this.token = null;
        this.currentUserId = null;
        this.currentUsername = null;
        this.currentRole = null;
    }

    public boolean isLoggedIn(){
        return token != null;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public String getToken() {
        return token;
    }

    public String getCurrentRole() {
        return currentRole;
    }
}
