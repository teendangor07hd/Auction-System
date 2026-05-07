package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lớp trừu tượng đại diện cho người dùng trong hệ thống BidHub.
 *
 * <p>Mọi người dùng đều có: {@code username}, {@code passwordHash},
 * {@code email}, và {@code role}. Subclass cụ thể ({@link Bidder},
 * {@link Seller}, {@link Admin}) thêm các field riêng của từng loại.
 *
 * <p><b>Tại sao passwordHash thay vì password?</b> Mật khẩu KHÔNG BAO GIỜ
 * được lưu dưới dạng plaintext. Tuần 5 sẽ implement SHA-256 hashing.
 * Ở tuần 2, truyền vào chuỗi bất kỳ (stub) để test.
 *
 * <p><b>Encapsulation:</b> Tất cả field đều {@code private}, chỉ expose qua
 * getter. Setter chỉ cung cấp cho field thực sự cần thay đổi.
 */
public abstract class User extends Entity {

    /** Tên đăng nhập — duy nhất trong hệ thống, không đổi sau khi tạo. */
    private final String username;

    /** Hash mật khẩu (SHA-256). Không lưu plaintext. */
    private String passwordHash;

    /** Địa chỉ email. */
    private String email;

    /** Vai trò người dùng — xác định quyền hạn trong hệ thống. */
    private final UserRole role;

    // ╔══════════════════════════════════════════════════════════════╗
    // ║  ⚠️ STUB — ĐĂNG SẼ MERGE (feature/tuan-6-dang-auction-manager)  ║
    // ║  XÓA TOÀN BỘ BLOCK NÀY khi rebase từ develop sau khi Đăng merge ║
    // ╚══════════════════════════════════════════════════════════════╝
    private boolean locked = false;
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    // ╔══════════════════════════════════════════════════════════════╗
    // ║  ⚠️ END STUB — ĐĂNG                                          ║
    // ╚══════════════════════════════════════════════════════════════╝

    /**
     * Constructor tạo User mới — gọi bởi subclass qua {@code super(...)}.
     *
     * @param username     tên đăng nhập, không null, không rỗng
     * @param passwordHash hash mật khẩu, không null
     * @param email        địa chỉ email, không null
     * @param role         vai trò người dùng, không null
     * @throws IllegalArgumentException nếu username hoặc email rỗng/null
     */
    protected User(String username, String passwordHash, String email, UserRole role) {
        super(); // gọi Entity() để tạo UUID và timestamps
        validateUsername(username);
        Objects.requireNonNull(passwordHash, "passwordHash không được null");
        Objects.requireNonNull(email, "email không được null");
        Objects.requireNonNull(role, "role không được null");
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
    }

    /**
     * Constructor dùng khi load User từ database (id và timestamps đã có).
     *
     * @param id           id từ DB
     * @param createdAt    thời điểm tạo từ DB
     * @param updatedAt    thời điểm cập nhật từ DB
     * @param username     tên đăng nhập
     * @param passwordHash hash mật khẩu
     * @param email        email
     * @param role         vai trò
     */
    protected User(
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String email,
            UserRole role) {
        super(id, createdAt, updatedAt);
        validateUsername(username);
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
    }

    /**
     * Validate username: không null, không rỗng, độ dài 3-50 ký tự.
     *
     * @param username giá trị cần kiểm tra
     * @throws IllegalArgumentException nếu vi phạm
     */
    private static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được null hoặc rỗng");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException(
                    "Username phải có độ dài từ 3 đến 50 ký tự, hiện tại: " + username.length());
        }
    }

    /**
     * Trả về thông tin mô tả ngắn gọn về người dùng này.
     *
     * <p>Mỗi subclass override để trả về thông tin đặc trưng của mình —
     * đây là demo tính <b>Polymorphism</b> của OOP.
     *
     * @return chuỗi mô tả không null
     */
    public abstract String getInfo();

    // =========================================================================
    // Getters — các field đều private, chỉ expose qua getter (Encapsulation)
    // =========================================================================

    /**
     * Trả về tên đăng nhập.
     *
     * @return username, không bao giờ null
     */
    public String getUsername() {
        return username;
    }

    /**
     * Trả về hash mật khẩu.
     *
     * @return passwordHash, không bao giờ null
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Trả về địa chỉ email.
     *
     * @return email, không bao giờ null
     */
    public String getEmail() {
        return email;
    }

    /**
     * Trả về vai trò người dùng.
     *
     * @return {@link UserRole}, không bao giờ null
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Cập nhật hash mật khẩu (khi người dùng đổi mật khẩu).
     *
     * @param newPasswordHash hash mới, không null
     */
    public void setPasswordHash(String newPasswordHash) {
        Objects.requireNonNull(newPasswordHash, "passwordHash mới không được null");
        this.passwordHash = newPasswordHash;
        markUpdated();
    }

    /**
     * Cập nhật địa chỉ email.
     *
     * @param newEmail email mới, không null
     */
    public void setEmail(String newEmail) {
        Objects.requireNonNull(newEmail, "email mới không được null");
        this.email = newEmail;
        markUpdated();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[id=" + getId().substring(0, 7)
                + ", username=" + username
                + ", role=" + role.name() + "]";
    }
}