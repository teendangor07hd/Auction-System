package com.bidhub.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Dịch vụ xác thực: băm mật khẩu SHA-256, xác minh mật khẩu, sinh token UUID.
 *
 * <p>Mỗi method là pure function — không có state, thread-safe tự nhiên.
 * SHA-256 không dùng salt (cho mục đích học tập);
 * production nên dùng bcrypt hoặc PBKDF2.
 */
public final class AuthService {

    private AuthService() {}

    /**
     * Băm mật khẩu bằng SHA-256, trả về chuỗi hex 64 ký tự.
     *
     *
     * @param plain mật khẩu gốc
     * @return chuỗi hex 64 ký tự
     */
    public static String hashPassword(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    plain.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có sẵn trong JDK — không bao giờ xảy ra
            throw new RuntimeException("SHA-256 khong kha dung", e);
        }
    }

    /**
     * Xác minh mật khẩu: hash plain text rồi so sánh với hashed.
     *
     *    thay vì String.equals() để chống timing attack.
     *    String.equals() trả về false ngay khi gặp byte khác đầu tiên,
     *    attacker có thể đo thời gian response để suy ra từng ký tự của hash]
     *
     * @param plain  mật khẩu người dùng nhập
     * @param hashed mật khẩu đã hash lưu trong DB
     * @return true nếu khớp, false nếu sai
     */
    public static boolean verifyPassword(String plain, String hashed) {
        byte[] computedHash = hashPassword(plain).getBytes(StandardCharsets.UTF_8);
        byte[] storedHash = hashed.getBytes(StandardCharsets.UTF_8);
        // Constant-time comparison — không short-circuit khi gặp byte khác
        return MessageDigest.isEqual(computedHash, storedHash);
    }

    /**
     * Sinh token UUID ngẫu nhiên cho phiên đăng nhập.
     *
     *
     * @return chuỗi UUID format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
     */
    public static String generateToken() {
        return UUID.randomUUID().toString();
    }
}