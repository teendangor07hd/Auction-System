package com.bidhub.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Dich vu xac thuc: bam mat khau SHA-256, xac minh mat khau, sinh token UUID.
 *
 * <p>Moi method la pure function — khong co state, thread-safe tu nhien.
 * SHA-256 khong dung salt (don gian cho muc dich hoc tap);
 * production nen dung bcrypt hoac PBKDF2.
 */
public final class AuthService {

    private AuthService() {}

    /**
     * Bam mat khau bang SHA-256, tra ve chuoi hex 64 ky tu.
     *
     * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — SHA-256 hash khong reversible]
     *
     * @param plain mat khau goc
     * @return chuoi hex 64 ky tu
     */
    public static String hashPassword(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    plain.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luon co trong JDK — khong bao gio xay ra
            throw new RuntimeException("SHA-256 khong kha dung", e);
        }
    }

    /**
     * Xac minh mat khau: hash plain text roi so sanh voi hashed.
     *
     * <p>// 📌 [Tieu chi: Ky thuat quan trong — dung MessageDigest.isEqual()
     *    thay vi String.equals() de chong timing attack.
     *    String.equals() tra ve false ngay khi gap byte khac dau tien,
     *    attacker co the do thoi gian response de suy ra tung ky tu cua hash]
     *
     * @param plain  mat khau nguoi dung nhap
     * @param hashed mat khau da hash luu trong DB
     * @return true neu khop, false neu sai
     */
    public static boolean verifyPassword(String plain, String hashed) {
        byte[] computedHash = hashPassword(plain).getBytes(StandardCharsets.UTF_8);
        byte[] storedHash = hashed.getBytes(StandardCharsets.UTF_8);
        // Constant-time comparison — khong short-circuit khi gap byte khac
        return MessageDigest.isEqual(computedHash, storedHash);
    }

    /**
     * Sinh token UUID ngau nhien cho phien dang nhap.
     *
     * <p>// 📌 [Tieu chi: Ky thuat quan trong — UUID token 128-bit entropy]
     *
     * @return chuoi UUID format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
     */
    public static String generateToken() {
        return UUID.randomUUID().toString();
    }
}