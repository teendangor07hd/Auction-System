package com.bidhub.common.exception;

/**
 * Base exception của hệ thống BidHub.
 *
 * <p>Tất cả exception trong BidHub đều kế thừa lớp này, cho phép:
 * <ul>
 *   <li>Catch gọn: {@code catch (BidHubException e)} bắt được mọi lỗi nghiệp vụ</li>
 *   <li>Phân loại lỗi: mỗi subclass có {@code errorCode} riêng → client
 *       xử lý đúng case (ví dụ: "BID_INVALID" hiện "Giá đặt không hợp lệ")</li>
 *   <li>Tương thích JSON: {@code errorCode} sẽ được đưa vào response
 *       JSON từ Tuần 4 ({@code {"status":"ERROR","errorCode":"BID_INVALID"}})</li>
 * </ul>
 *
 * <p><b>Tại sao extends RuntimeException (unchecked)?</b>
 * Checked exception bắt buộc try-catch ở mọi nơi → code rối, khó đọc.
 * RuntimeException không bắt buộc khai báo trong signature nhưng vẫn
 * có thể catch khi cần xử lý cụ thể.
 *
 * <p>Ví dụ sử dụng (từ Tuần 5):
 * <pre>{@code
 * // Ném exception khi bid không hợp lệ
 * if (!auction.isValidBid(amount)) {
 *     throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại: " + amount);
 * }
 *
 * // Bắt tại tầng Request Handler
 * try {
 *     bidService.placeBid(auctionId, amount, userId);
 * } catch (BidHubException e) {
 *     sendError(session, e.getErrorCode(), e.getMessage());
 * }
 * }</pre>
 */
public class BidHubException extends RuntimeException {

    /**
     * Mã lỗi ngắn gọn để client xử lý logic.
     *
     * <p>Ví dụ: {@code "BID_INVALID"}, {@code "AUCTION_NOT_FOUND"}.
     * Quy ước: SCREAMING_SNAKE_CASE.
     */
    private final String errorCode;

    /**
     * Tạo BidHubException với message và errorCode.
     *
     * @param message   thông báo lỗi đọc được bởi con người
     * @param errorCode mã lỗi ngắn gọn cho client (SCREAMING_SNAKE_CASE)
     */
    public BidHubException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BidHubException(String message) {
        super(message);
        this.errorCode = "UNKNOWN_ERROR"; //gan loi mac dinh (final errorCode)
    }

    /**
     * Tạo BidHubException với message, errorCode, và nguyên nhân gốc.
     *
     * <p>Dùng khi wrap exception từ tầng dưới (ví dụ: SQLException).
     *
     * @param message   thông báo lỗi
     * @param errorCode mã lỗi
     * @param cause     nguyên nhân gốc
     */
    public BidHubException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Trả về mã lỗi ngắn gọn.
     *
     * @return errorCode (ví dụ: {@code "BID_INVALID"}), không bao giờ null
     */
    public String getErrorCode() {
        return errorCode;
    }
}