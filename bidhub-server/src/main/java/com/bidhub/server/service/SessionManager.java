package com.bidhub.server.service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton quản lý phiên đăng nhập: token ↔ userId.
 *
 * <p>Dùng 2 {@link ConcurrentHashMap} để tra cứu cả 2 chiều:
 * token → userId và userId → token.
 * Đảm bảo 1 user chỉ có 1 token tại 1 thời điểm — login mới thay token cũ.
 *
 */
public final class SessionManager {

    private static volatile SessionManager instance;

    private final ConcurrentHashMap<String, String> tokenToUserId;
    private final ConcurrentHashMap<String, String> userIdToToken;

    private SessionManager() {
        this.tokenToUserId = new ConcurrentHashMap<>();
        this.userIdToToken = new ConcurrentHashMap<>();
    }

    /**
     * Trả về instance duy nhất (thread-safe, double-checked locking).
     *
     * @return SessionManager instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Tạo phiên đăng nhập mới cho userId. Nếu userId đã có token cũ → thay thế.
     *
     * @param userId ID người dùng
     * @return token UUID mới
     */
    public synchronized  String createSession(String userId) {
        String token = AuthService.generateToken();

        // Nếu user đã có token cũ → xóa token cũ khỏi tokenToUserId
        String oldToken = userIdToToken.put(userId, token);
        if (oldToken != null) {
            tokenToUserId.remove(oldToken);
        }

        tokenToUserId.put(token, userId);
        return token;
    }

    /**
     * Hủy phiên đăng nhập — xóa token khỏi cả 2 map.
     *
     * @param token Token cần hủy
     */
    public synchronized void invalidateSession(String token) {
        if (token == null) {
            return;
        }
        String userId = tokenToUserId.remove(token);
        if (userId != null) {
            userIdToToken.remove(userId);
        }
    }

    /**
     * Tra cứu userId từ token.
     *
     * @param token Token cần tra cứu
     * @return Optional chứa userId nếu token hợp lệ, Optional.empty() nếu không
     */
    // synchronized đảm bảo nhất quán khi đọc song song với các write operation
    public synchronized Optional<String> getUserIdByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokenToUserId.get(token));
    }

    /**
     * Kiểm tra token có hợp lệ không.
     *
     * @param token Token cần kiểm tra
     * @return true nếu token hợp lệ
     */
    public synchronized boolean isValidToken(String token) {
        return token != null && tokenToUserId.containsKey(token);
    }

    /**
     * Lấy token hiện tại của userId.
     *
     * @param userId ID người dùng
     * @return Optional chứa token nếu user đang đăng nhập
     */
    public synchronized Optional<String> getTokenByUserId(String userId) {
        return Optional.ofNullable(userIdToToken.get(userId));
    }

    /** Xóa toàn bộ phiên — chỉ dùng cho mục đích test. */
    public synchronized void clearAll() {
        tokenToUserId.clear();
        userIdToToken.clear();
    }

    /** Trả về số phiên đăng nhập hiện tại — dùng để test hoặc giám sát. */
    public synchronized int activeSessionCount() {
        return tokenToUserId.size();
    }
}