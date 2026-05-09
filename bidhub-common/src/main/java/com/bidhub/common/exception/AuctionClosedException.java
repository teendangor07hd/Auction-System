package com.bidhub.common.exception;

/**
 * Ném khi cố đặt giá vào phiên đấu giá đã đóng (FINISHED/PAID/CANCELED).
 *
 * <p>Khác với {@link InvalidBidException}: exception này về trạng thái phiên,
 * không phải về giá trị bid.
 */
public class AuctionClosedException extends BidHubException {

    /** Mã lỗi cố định. */
    public static final String ERROR_CODE = "AUCTION_CLOSED";

    /**
     * Tạo AuctionClosedException.
     *
     * @param auctionId id phiên đã đóng
     * @param status    trạng thái hiện tại của phiên
     */
    public AuctionClosedException(String auctionId, String status) {
        super("Phiên đấu giá " + auctionId + " đã đóng (status: " + status + ")", ERROR_CODE);
    }

    public AuctionClosedException(String message) {
        super(message);
    }
}