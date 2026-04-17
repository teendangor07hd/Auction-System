package com.bidhub.server.model;

/**
 * Vai trò của người dùng trong hệ thống BidHub.
 *
 * <p>Mỗi {@link User} có đúng 1 vai trò, xác định quyền thực hiện thao tác:
 * <ul>
 *   <li>{@link #BIDDER} — đặt giá, xem đấu giá</li>
 *   <li>{@link #SELLER} — tạo sản phẩm, tạo phiên đấu giá</li>
 *   <li>{@link #ADMIN} — quản lý toàn hệ thống</li>
 * </ul>
 *
 * <p>Enum được lưu vào database dưới dạng chuỗi (tên enum).
 * Đọc lại: {@code UserRole.valueOf(dbString)}.
 */
public enum UserRole {

    /** Người đặt giá — chỉ xem và đặt, không tạo sản phẩm. */
    BIDDER("Người đặt giá"),

    /** Người bán — tạo sản phẩm và khởi tạo phiên đấu giá. */
    SELLER("Người bán"),

    /** Quản trị viên — toàn quyền. */
    ADMIN("Quản trị viên");

    /** Tên hiển thị tiếng Việt — dùng trên UI và log. */
    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Trả về tên hiển thị tiếng Việt của vai trò.
     *
     * @return chuỗi tên hiển thị (ví dụ: {@code "Người đặt giá"})
     */
    public String getDisplayName() {
        return displayName;
    }
}