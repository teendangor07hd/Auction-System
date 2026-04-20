package com.bidhub.common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Exception khi form nhập liệu vi phạm nhiều quy tắc validation cùng lúc.
 *
 * <p>Khác các exception còn lại, {@code ValidationException} chứa
 * {@link List} các lỗi cụ thể — cho phép client hiển thị tất cả vấn đề
 * một lúc thay vì chỉ lỗi đầu tiên.
 *
 * <p>Ví dụ sử dụng (từ Tuần 5):
 * <pre>{@code
 * List<String> errors = new ArrayList<>();
 * if (username == null || username.isBlank()) {
 *     errors.add("Username không được để trống");
 * }
 * if (username != null && username.length() < 3) {
 *     errors.add("Username phải ≥ 3 ký tự");
 * }
 * if (password == null || password.length() < 6) {
 *     errors.add("Mật khẩu phải ≥ 6 ký tự");
 * }
 * if (!errors.isEmpty()) {
 *     throw new ValidationException(errors);
 * }
 * }</pre>
 */
public class ValidationException extends BidHubException {

    /** Mã lỗi cố định. */
    public static final String ERROR_CODE = "VALIDATION_ERROR";

    /**
     * Danh sách lỗi validation cụ thể.
     *
     * <p>Unmodifiable để tránh thay đổi ngoài ý muốn sau khi exception được tạo.
     */
    private final List<String> errors;

    /**
     * Tạo ValidationException với một lỗi duy nhất.
     *
     * <p>Convenience constructor cho trường hợp đơn giản có 1 lỗi.
     *
     * @param errorMessage mô tả lỗi, không null
     */
    public ValidationException(String errorMessage) {
        super(errorMessage, ERROR_CODE);
        Objects.requireNonNull(errorMessage, "errorMessage không được null");
        this.errors = List.of(errorMessage);
    }

    /**
     * Tạo ValidationException với nhiều lỗi.
     *
     * @param errors danh sách lỗi, không null, không rỗng
     * @throws IllegalArgumentException nếu errors null hoặc rỗng
     */
    public ValidationException(List<String> errors) {
        super(buildMessage(errors), ERROR_CODE);
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "ValidationException cần ít nhất 1 lỗi trong danh sách");
        }
        // Tạo defensive copy và wrap bằng unmodifiableList
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Trả về danh sách lỗi validation (unmodifiable).
     *
     * @return list lỗi, không bao giờ null, không bao giờ rỗng
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Trả về số lượng lỗi validation.
     *
     * @return số lỗi (≥ 1)
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Tạo message tổng hợp từ danh sách lỗi.
     *
     * @param errors danh sách lỗi
     * @return message tổng hợp
     */
    private static String buildMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Dữ liệu không hợp lệ";
        }
        return errors.size() + " lỗi validation: " + String.join("; ", errors);
    }
}