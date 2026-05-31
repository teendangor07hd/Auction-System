package com.bidhub.server.model;

import java.time.LocalDateTime;

/**
 * Người bán hàng trong hệ thống BidHub.
 *
 * <p>Kế thừa {@link User}, thêm field {@code totalItemsListed} theo dõi
 * tổng số sản phẩm đã đăng bán.
 */
public final class Seller extends User {

    /** Tổng số sản phẩm đã đăng bán — dùng cho thống kê. */
    private int totalItemsListed;

    /**
     * Tạo Seller mới với totalItemsListed = 0.
     *
     * @param username     tên đăng nhập
     * @param passwordHash hash mật khẩu
     * @param email        địa chỉ email
     */
    public Seller(String username, String passwordHash, String email) {
        super(username, passwordHash, email, UserRole.SELLER);
        this.totalItemsListed = 0;
    }

    /**
     * Constructor load từ database.
     *
     * @param id               id từ DB
     * @param createdAt        thời điểm tạo
     * @param updatedAt        thời điểm cập nhật
     * @param username         tên đăng nhập
     * @param passwordHash     hash mật khẩu
     * @param email            email
     * @param totalItemsListed tổng sản phẩm đã đăng
     */
    public Seller(
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String email,
            int totalItemsListed,
            boolean locked) {
        super(id, createdAt, updatedAt, username, passwordHash, email, UserRole.SELLER, locked);
        this.totalItemsListed = Math.max(0, totalItemsListed);
    }

    /**
     * Tăng tổng sản phẩm đã đăng lên 1.
     *
     */
    public void incrementItemsListed() {
        this.totalItemsListed++;
        markUpdated();
    }

    /**
     * Trả về tổng số sản phẩm đã đăng bán.
     *
     * @return số sản phẩm (≥ 0)
     */
    public int getTotalItemsListed() {
        return totalItemsListed;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Seller trả về thông tin: username, vai trò, tổng sản phẩm.
     */
    @Override
    public String getInfo() {
        return "Người bán: " + getUsername()
                + " | Tổng sản phẩm đã đăng: " + totalItemsListed;
    }
}