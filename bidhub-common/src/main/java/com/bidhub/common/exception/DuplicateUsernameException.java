package com.bidhub.common.exception;

/**
 * Ném khi đăng ký với username đã tồn tại trong hệ thống.
 *
 * <p>Sử dụng từ Tuần 5 trong {@code UserService.register()}.
 */
public class DuplicateUsernameException extends BidHubException {

    /** Mã lỗi cố định. */
    public static final String ERROR_CODE = "USERNAME_TAKEN";

    /**
     * Tạo DuplicateUsernameException.
     *
     * @param username tên đăng nhập đã bị trùng
     */
    public DuplicateUsernameException(String username) {
        super("Tên đăng nhập '" + username + "' đã tồn tại trong hệ thống", ERROR_CODE);
    }
}