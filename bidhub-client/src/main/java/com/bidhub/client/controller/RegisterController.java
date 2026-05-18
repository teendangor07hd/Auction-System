package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller cho man hinh dang ky.
 *
 * <p>// 📌 [Tieu chi: MVC — Controller dieu phoi form validation + network]
 * // 📌 [B27] Password match binding watch CẢ visible text fields (toggle password).
 * // 📌 [B28] Re-bind registerButton.disableProperty() sau khi task hoàn thành.
 * // 📌 [B29] Email validation dùng regex cơ bản thay vì chỉ contains("@").
 * // 📌 [B31] Validate username length >= 3 ký tự.
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField passwordTextField;      // visible password toggle
    @FXML private TextField confirmPasswordTextField; // visible confirm toggle
    @FXML private Label passwordMatchLabel;
    @FXML private TextField emailField;
    @FXML private ChoiceBox<String> roleChoiceBox;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;

    @FXML
    public void initialize() {
        roleChoiceBox.setItems(
                FXCollections.observableArrayList("BIDDER", "SELLER"));
        roleChoiceBox.setValue("BIDDER");

        // Ẩn visible fields ban đầu (nếu có trong FXML)
        if (passwordTextField != null) {
            passwordTextField.setManaged(false);
            passwordTextField.setVisible(false);
        }
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.setManaged(false);
            confirmPasswordTextField.setVisible(false);
        }

        // 📌 [Tieu chi: MVC — bind realtime password confirmation]
        // [B27] Watch cả 4 text properties (PasswordField + TextField visible toggle)
        passwordMatchLabel.visibleProperty().bind(
                Bindings.createBooleanBinding(
                        () -> {
                            String pw = getPassword();
                            String cpw = getConfirmPassword();
                            return !cpw.isEmpty() && !pw.equals(cpw);
                        },
                        passwordField.textProperty(),
                        confirmPasswordField.textProperty(),
                        passwordTextField == null
                                ? passwordField.textProperty()  // fallback nếu không có toggle
                                : passwordTextField.textProperty(),
                        confirmPasswordTextField == null
                                ? confirmPasswordField.textProperty()
                                : confirmPasswordTextField.textProperty()
                )
        );
        passwordMatchLabel.setText("Mật khẩu xác nhận không khớp!");

        // Bind disable button — sẽ được re-bind sau khi task hoàn thành (B28)
        bindRegisterButton();

        errorLabel.setVisible(false);
        errorLabel.getStyleClass().add("error-message");
    }

    /**
     * Bind registerButton.disableProperty() với điều kiện validation.
     *
     * <p>// 📌 [B28] Tách thành method riêng để có thể gọi lại sau khi task hoàn thành (re-bind).
     * // 📌 [B29] Email validation dùng regex [\w+-.]+@[\w-]+\.[a-z]{2,} thay vì chỉ contains("@").
     * // 📌 [B31] Username phải có ít nhất 3 ký tự.
     */
    private void bindRegisterButton() {
        registerButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> {
                            // [B31] Username >= 3 ký tự, chỉ chứa chữ cái, số, underscore
                            String username = usernameField.getText();
                            if (username == null || username.trim().length() < 3) return true;
                            if (!username.trim().matches("[a-zA-Z0-9_]+")) return true;

                            // Password >= 8 ký tự
                            if (passwordField.getText().length() < 8) return true;

                            // [B29] Email regex cơ bản
                            String email = emailField.getText().trim();
                            if (!email.matches("[\\w+\\-.]+@[\\w\\-]+\\.[a-z]{2,}")) return true;

                            // Password khớp
                            return !getPassword().equals(getConfirmPassword());
                        },
                        usernameField.textProperty(),
                        passwordField.textProperty(),
                        confirmPasswordField.textProperty(),
                        emailField.textProperty()
                )
        );
    }

    /**
     * Lấy password từ field đang hiển thị (PasswordField hoặc TextField toggle).
     */
    private String getPassword() {
        if (passwordTextField != null && passwordTextField.isVisible()) {
            return passwordTextField.getText();
        }
        return passwordField.getText();
    }

    /**
     * Lấy confirm password từ field đang hiển thị.
     */
    private String getConfirmPassword() {
        if (confirmPasswordTextField != null && confirmPasswordTextField.isVisible()) {
            return confirmPasswordTextField.getText();
        }
        return confirmPasswordField.getText();
    }

    /**
     * Xu ly click nut "Dang ky" — gui request REGISTER qua NetworkTask.
     */
    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = getPassword();
        String confirmPassword = getConfirmPassword();
        String email = emailField.getText().trim();
        String role = roleChoiceBox.getValue();

        // Client-side validation
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Mật khẩu xác nhận không khớp!");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);

        // [B28] Unbind trước khi disable để tránh lỗi "bound value cannot be set"
        registerButton.disableProperty().unbind();
        registerButton.setDisable(true);

        // 📌 [Tieu chi: MVC — tao REGISTER request payload]
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("email", email);
        payload.put("role", role);

        MessageRequest request = new MessageRequest(
                "REGISTER", ClientSession.getInstance().getToken(), payload);

        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(request));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if (response.isOk()) {
                ViewRouter.getInstance().navigateTo(Views.LOGIN);
            } else {
                errorLabel.setText(response.getMessage());
                errorLabel.setVisible(true);
                // [B28] Re-bind sau khi task hoàn thành (thất bại) — nút sẽ tự disable lại đúng
                bindRegisterButton();
            }
        });

        task.setOnFailed(e -> {
            errorLabel.setText("Không kết nối được máy chủ. Thử lại sau.");
            errorLabel.setVisible(true);
            // [B28] Re-bind sau khi task thất bại
            bindRegisterButton();
        });

        new Thread(task).start();
    }

    /**
     * Chuyen ve man hinh dang nhap.
     */
    @FXML
    public void handleBackToLogin() {
        ViewRouter.getInstance().navigateTo(Views.LOGIN);
    }
}