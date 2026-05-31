package com.bidhub.common.exception;

/**
 * Ném khi bid không hợp lệ (thấp hơn giá hiện tại, không đủ increment, v.v.).
 *
 */
public class InvalidBidException extends BidHubException {

    /** Mã lỗi cố định của exception này. */
    public static final String ERROR_CODE = "BID_INVALID";

    /**
     * Tạo InvalidBidException.
     *
     * @param message mô tả lý do bid không hợp lệ
     */
    public InvalidBidException(String message) {
        super(message, ERROR_CODE);
    }
}