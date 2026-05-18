package com.bidhub.server.model;

import java.time.LocalDateTime;

/**
 * Người đặt giá trong hệ thống BidHub.
 *
 * <p>Kế thừa {@link User}, thêm field {@code totalBidsPlaced} theo dõi
 * tổng số lần đã đặt giá. Field này được cập nhật mỗi khi đặt giá thành công.
 *
 * <p>Override {@link #getInfo()} để thể hiện tính <b>Polymorphism</b>:
 * cùng lời gọi {@code user.getInfo()}, mỗi loại user trả về thông tin khác nhau.
 */
public final class Bidder extends User {

    /** Tổng số lần đã đặt giá — dùng cho thống kê. */
    private int totalBidsPlaced;

    /**
     * Tạo Bidder mới với totalBidsPlaced = 0.
     *
     * @param username     tên đăng nhập
     * @param passwordHash hash mật khẩu
     * @param email        địa chỉ email
     */
    public Bidder(String username, String passwordHash, String email) {
        super(username, passwordHash, email, UserRole.BIDDER);
        this.totalBidsPlaced = 0;
    }

    /**
     * Constructor load từ database.
     *
     * @param id              id từ DB
     * @param createdAt       thời điểm tạo từ DB
     * @param updatedAt       thời điểm cập nhật từ DB
     * @param username        tên đăng nhập
     * @param passwordHash    hash mật khẩu
     * @param email           email
     * @param totalBidsPlaced tổng số lần đặt giá
     */
    public Bidder(
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String username,
            String passwordHash,
            String email,
            int totalBidsPlaced,
            boolean locked) {
        super(id, createdAt, updatedAt, username, passwordHash, email, UserRole.BIDDER, locked);
        this.totalBidsPlaced = Math.max(0, totalBidsPlaced);
    }

    /**
     * Tăng tổng số lần đặt giá lên 1.
     *
     * <p>Gọi mỗi khi Bidder đặt giá thành công (từ Tuần 6).
     */
    public void incrementBidsPlaced() {
        this.totalBidsPlaced++;
        markUpdated();
    }

    /**
     * Trả về tổng số lần đã đặt giá.
     *
     * @return số lần đặt giá (≥ 0)
     */
    public int getTotalBidsPlaced() {
        return totalBidsPlaced;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Bidder trả về thông tin: username, vai trò, tổng bids.
     */
    @Override
    public String getInfo() {
        return "Người đặt giá: " + getUsername()
                + " | Tổng lần đặt giá: " + totalBidsPlaced;
    }
}