package com.bidhub.common.exception;

/**
 * Ném khi tìm kiếm người dùng không tồn tại.
 *
 */
public class UserNotFoundException extends BidHubException {

    /** Mã lỗi cố định. */
    public static final String ERROR_CODE = "USER_NOT_FOUND";

    /**
     * Tạo UserNotFoundException.
     *
     * @param identifier username hoặc id của người dùng không tìm thấy
     */
    public UserNotFoundException(String identifier) {
        super("Không tìm thấy người dùng: " + identifier, ERROR_CODE);
    }
}