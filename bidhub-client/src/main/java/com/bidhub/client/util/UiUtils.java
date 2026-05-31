package com.bidhub.client.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Các helper method cho JavaFX UI — loading state, validation, numeric filter.
 *
 */
public final class UiUtils {

    private UiUtils() {
        // Utility class — không instance
    }

    /**
     * Bind loading state: disable button + hien ProgressIndicator khi task chay.
     *
     * @param button button cần disable
     * @param spinner ProgressIndicator
     * @return Runnable để goi khi task hoan thanh (re-enable button)
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
     * @param title   tieu để
     * @param message nội đúng loi
     */
    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Hien Alert thanh cong.
     *
     * @param title   tieu để
     * @param message nội đúng
     */
    public static void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Ap numeric-only filter cho TextField.
     *
     * <p>Chỉ cho phép so + dau cham thap phan.
     *
     * @param textField TextField cần filter
     */
    public static void applyNumericFilter(TextField textField) {
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            if (text.isEmpty()) {
                return change;
            }
            // Chỉ cho phép so và dau cham
            if (text.matches("[0-9.]*")) {
                // Chỉ cho phép 1 dau cham
                String currentText = ((TextField) change.getControl()).getText();
                if (".".equals(text) && currentText.contains(".")) {
                    return null; // Block — đã có dau cham
                }
                return change;
            }
            return null; // Block — không phai so
        });
        textField.setTextFormatter(formatter);
    }

    /**
     * Validate TextField không rong.
     *
     * @param textField TextField cần check
     * @param fieldName ten truong (cho error message)
     * @return true nếu hop le
     */
    public static boolean validateNotEmpty(TextField textField, String fieldName) {
        if (textField.getText() == null || textField.getText().isBlank()) {
            showError("Lỗi nhập liệu", fieldName + " không được để trống.");
            textField.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validate TextField là so duong.
     *
     * @param textField TextField cần check
     * @param fieldName ten truong (cho error message)
     * @return true nếu hop le
     */
    public static boolean validatePositiveNumber(TextField textField,
                                                 String fieldName) {
        try {
            double value = Double.parseDouble(textField.getText().trim());
            if (value <= 0) {
                showError("Lỗi nhập liệu", fieldName + " phải lớn hơn 0.");
                textField.requestFocus();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showError("Lỗi nhập liệu", fieldName + " phải là số hợp lệ.");
            textField.requestFocus();
            return false;
        }
    }
}