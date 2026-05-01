package com.bidhub.server.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Quản trị viên của hệ thống BidHub.
 *
 * <p>Kế thừa {@link User}, thêm {@code adminLevel} (cấp độ quản trị: 1-3).
 * Level 1: admin thường. Level 3: superadmin (quyền cao nhất).
 */
public final class Admin extends User {

    /** Cấp độ quản trị. Giá trị hợp lệ: 1, 2, hoặc 3. */
    private int adminLevel;

    /**
     * Tạo Admin mới.
     *
     * @param username     tên đăng nhập
     * @param passwordHash hash mật khẩu
     * @param email        địa chỉ email
     * @param adminLevel   cấp độ (1-3)
     * @throws IllegalArgumentException nếu adminLevel không hợp lệ
     */
    public Admin(String username, String passwordHash, String email, int adminLevel) {
        super(username, passwordHash, email, UserRole.ADMIN);
        this.adminLevel = validateAdminLevel(adminLevel);
    }

    /**
     * Constructor load từ database.
     *
     * @param id           id từ DB
     * @param createdAt    thời điểm tạo
     * @param updatedAt    thời điểm cập nhật
     * @param username     tên đăng nhập
     * @param passwordHash hash mật khẩu
     * @param email        email
     * @param adminLevel   cấp độ quản trị
     */
    public Admin(
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String email,
            int adminLevel) {
        super(id, createdAt, updatedAt, username, passwordHash, email, UserRole.ADMIN);
        this.adminLevel = validateAdminLevel(adminLevel);
    }

    /**
     * Validate admin level phải trong khoảng [1, 3].
     *
     * @param level giá trị cần kiểm tra
     * @return level nếu hợp lệ
     * @throws IllegalArgumentException nếu level ngoài khoảng cho phép
     */
    private static int validateAdminLevel(int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException(
                    "adminLevel phải trong khoảng [1, 3], nhận được: " + level);
        }
        return level;
    }

    /**
     * Trả về cấp độ quản trị.
     *
     * @return giá trị từ 1 đến 3
     */
    public int getAdminLevel() {
        return adminLevel;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Admin trả về thông tin: username, vai trò, cấp độ quản trị.
     */
    @Override
    public String getInfo() {
        return "Quản trị viên: " + getUsername()
                + " | Cấp độ: " + adminLevel;
    }
}