package com.bidhub.client.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Cac helper method cho JavaFX UI — loading state, validation, numeric filter.
 *
 * <p>// 📌 [Tieu chi: MVC — JavaFX UX helper]
 * // 📌 [B58] applyIntegerFilter() mới — chỉ cho phép số nguyên.
 * // 📌 [B59] validatePositiveNumber() gọi validateNotEmpty() trước → không crash khi rỗng.
 * // 📌 [B60] showError/showInfo dùng show() thay vì showAndWait() — tránh stacking modal dialogs.
 */
public final class UiUtils {

    private UiUtils() {
        // Utility class — khong instance
    }

    /**
     * Bind loading state: disable button + hien ProgressIndicator khi task chay.
     *
     * @param button  button can disable
     * @param spinner ProgressIndicator
     * @return Runnable de goi khi task hoan thanh (re-enable button)
     */
    public static Runnable showLoading(Button button, ProgressIndicator spinner) {
        button.setDisable(true);
        spinner.setVisible(true);
        return () -> {
            button.setDisable(false);
            spinner.setVisible(false);
        };
    }

    /**
     * Hien Alert loi.
     *
     * <p>// 📌 [B60] Dùng show() thay vì showAndWait() — tránh stacking modal dialogs.
     *
     * @param title   tieu de
     * @param message noi dung loi
     */
    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message != null ? message : "Lỗi không xác định.");
            alert.show(); // [B60] show() thay vì showAndWait()
        });
    }

    /**
     * Hien Alert thanh cong.
     *
     * <p>// 📌 [B60] Dùng show() thay vì showAndWait().
     *
     * @param title   tieu de
     * @param message noi dung
     */
    public static void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message != null ? message : "");
            alert.show(); // [B60] show() thay vì showAndWait()
        });
    }

    /**
     * Ap decimal numeric-only filter cho TextField.
     *
     * <p>Chi cho phep so + dau cham thap phan (dung cho gia tien, float).
     *
     * @param textField TextField can filter
     */
    public static void applyNumericFilter(TextField textField) {
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            if (text.isEmpty()) {
                return change;
            }
            // Chi cho phep so va dau cham
            if (text.matches("[0-9.]*")) {
                // Chi cho phep 1 dau cham
                String currentText = ((TextField) change.getControl()).getText();
                if (".".equals(text) && currentText.contains(".")) {
                    return null; // Block — da co dau cham
                }
                return change;
            }
            return null; // Block — khong phai so
        });
        textField.setTextFormatter(formatter);
    }

    /**
     * Ap integer-only filter cho TextField — KHÔNG cho phep dau cham.
     *
     * <p>// 📌 [B58] Dung cho cac field yeu cau so nguyen (warrantyMonths, year, mileageKm).
     * applyNumericFilter() cho phep decimal nen se crash khi Integer.parseInt("12.5").
     *
     * @param textField TextField can filter
     */
    public static void applyIntegerFilter(TextField textField) {
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            if (text.isEmpty()) {
                return change;
            }
            // Chi cho phep so nguyen, khong cho dau cham
            if (text.matches("[0-9]*")) {
                return change;
            }
            return null; // Block
        });
        textField.setTextFormatter(formatter);
    }

    /**
     * Validate TextField khong rong.
     *
     * @param textField TextField can check
     * @param fieldName ten truong (cho error message)
     * @return true neu hop le
     */
    public static boolean validateNotEmpty(TextField textField, String fieldName) {
        if (textField.getText() == null || textField.getText().isBlank()) {
            showError("Validation Error", fieldName + " khong duoc de trong");
            textField.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validate TextField la so duong.
     *
     * <p>// 📌 [B59] Gọi validateNotEmpty() trước → không crash NumberFormatException khi rỗng.
     *
     * @param textField TextField can check
     * @param fieldName ten truong (cho error message)
     * @return true neu hop le
     */
    public static boolean validatePositiveNumber(TextField textField, String fieldName) {
        // [B59] Kiểm tra rỗng trước để tránh crash
        if (!validateNotEmpty(textField, fieldName)) {
            return false;
        }
        try {
            double value = Double.parseDouble(textField.getText().trim());
            if (value <= 0) {
                showError("Validation Error", fieldName + " phai lon hon 0");
                textField.requestFocus();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showError("Validation Error", fieldName + " phai la so hop le");
            textField.requestFocus();
            return false;
        }
    }

    /**
     * Format số tiền VND (double → chuỗi có dấu phẩy ngăn cách).
     *
     * <p>// 📌 [B66] Dùng chung thay vì lặp trong nhiều controller.
     *
     * @param amount số tiền
     * @return chuỗi định dạng, ví dụ "1,500,000 VNĐ"
     */
    public static String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    /**
     * Dich itemType sang tieng Viet.
     *
     * <p>// 📌 [B61] Tach tu 3 controllers vao day.
     *
     * @param type "ELECTRONICS" | "ART" | "VEHICLE"
     * @return ten tieng Viet
     */
    public static String translateType(String type) {
        if (type == null) return "Không rõ";
        return switch (type) {
            case "ELECTRONICS" -> "Điện tử";
            case "ART"         -> "Nghệ thuật";
            case "VEHICLE"     -> "Phương tiện";
            default            -> type;
        };
    }
}