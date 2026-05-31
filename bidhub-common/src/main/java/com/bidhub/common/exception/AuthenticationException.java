package com.bidhub.common.exception;

/**
 * Ném khi xác thực thất bại (sai mật khẩu, token không hợp lệ, chưa đăng nhập).
 *
 */
public class AuthenticationException extends BidHubException {

    /** Mã lỗi cố định. */
    public static final String ERROR_CODE = "AUTH_FAILED";

    /**
     * Tạo AuthenticationException.
     *
     * @param message mô tả lý do xác thực thất bại
     */
    public AuthenticationException(String message) {
        super(message, ERROR_CODE);
    }
}