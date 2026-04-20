package com.bidhub.common.exception;

/**
 * Ném khi tìm kiếm phiên đấu giá không tồn tại trong hệ thống.
 *
 * <p>Sử dụng từ Tuần 4 trong {@code RequestHandler}.
 */
public class AuctionNotFoundException extends BidHubException {

    /** Mã lỗi cố định. */
    public static final String ERROR_CODE = "AUCTION_NOT_FOUND";

    /**
     * Tạo AuctionNotFoundException.
     *
     * @param auctionId id của phiên đấu giá không tìm thấy
     */
    public AuctionNotFoundException(String auctionId) {
        super("Không tìm thấy phiên đấu giá với id: " + auctionId, ERROR_CODE);
    }
}