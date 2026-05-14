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
 */
public final class UiUtils {

    private UiUtils() {
        // Utility class — khong instance
    }

    /**
     * Bind loading state: disable button + hien ProgressIndicator khi task chay.
     *
     * @param button button can disable
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
     * @param title   tieu de
     * @param message noi dung loi
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
     * @param title   tieu de
     * @param message noi dung
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
     * <p>Chi cho phep so + dau cham thap phan.
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
     * @param textField TextField can check
     * @param fieldName ten truong (cho error message)
     * @return true neu hop le
     */
    public static boolean validatePositiveNumber(TextField textField,
                                                 String fieldName) {
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
}