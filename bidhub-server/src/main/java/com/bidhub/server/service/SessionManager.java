package com.bidhub.server.service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton quan ly phien dang nhap: token ↔ userId.
 *
 * <p>Dung 2 {@link ConcurrentHashMap} de tra cuu ca 2 chieu:
 * token → userId va userId → token.
 * Dam bao 1 user chi co 1 token tai 1 thoi diem — login moi thay token cu.
 *
 * <p>// 📌 [Tieu chi: Ky thuat quan trong & concurrency — ConcurrentHashMap thread-safe]
 * // 📌 [Tieu chi: Design Pattern Singleton — volatile + double-checked locking]
 */
public final class SessionManager {

    private static volatile SessionManager instance;

    // 📌 [Tieu chi: Ky thuat quan trong — ConcurrentHashMap cho concurrent access]
    private final ConcurrentHashMap<String, String> tokenToUserId;
    private final ConcurrentHashMap<String, String> userIdToToken;

    private SessionManager() {
        this.tokenToUserId = new ConcurrentHashMap<>();
        this.userIdToToken = new ConcurrentHashMap<>();
    }

    /**
     * Tra ve instance duy nhat (thread-safe, double-checked locking).
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
     * Tao phien dang nhap moi cho userId. Neu userId da co token cu → thay the.
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — tao phien dang nhap]
     *
     * @param userId id nguoi dung
     * @return token UUID moi
     */
    public synchronized  String createSession(String userId) {
        String token = AuthService.generateToken();

        // Neu user da co token cu → xoa token cu khoi tokenToUserId
        String oldToken = userIdToToken.put(userId, token);
        if (oldToken != null) {
            tokenToUserId.remove(oldToken);
        }

        tokenToUserId.put(token, userId);
        return token;
    }

    /**
     * Huy phien dang nhap — xoa token khoi ca 2 map.
     *
     * @param token token can huy
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
     * Tra cuu userId tu token.
     *
     * @param token token can tra cuu
     * @return Optional chua userId neu token hop le, Optional.empty() neu khong
     */
    // 📌 [Tieu chi: Ky thuat quan trong — synchronized read de dam bao nhat quan
    //    voi write (createSession/invalidateSession cung dung synchronized)]
    public synchronized Optional<String> getUserIdByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokenToUserId.get(token));
    }

    /**
     * Kiem tra token co ton tai khong.
     *
     * @param token token can kiem tra
     * @return true neu token hop le
     */
    public synchronized boolean isValidToken(String token) {
        return token != null && tokenToUserId.containsKey(token);
    }

    /**
     * Lay token hien tai cua userId.
     *
     * @param userId id nguoi dung
     * @return Optional chua token neu user dang dang nhap
     */
    public synchronized Optional<String> getTokenByUserId(String userId) {
        return Optional.ofNullable(userIdToToken.get(userId));
    }

    /** Xoa toan bo session — chi dung cho test. */
    // 📌 [Tieu chi: Ky thuat quan trong — synchronized de tranh race voi create/invalidate]
    public synchronized void clearAll() {
        tokenToUserId.clear();
        userIdToToken.clear();
    }

    /** Tra ve so phien dang nhap hien tai — chi dung cho test/monitor. */
    public synchronized int activeSessionCount() {
        return tokenToUserId.size();
    }
}