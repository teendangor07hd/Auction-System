package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.client.util.UiUtils;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Map;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingSpinner;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        if (passwordTextField != null) {
            passwordTextField.setManaged(false);
            passwordTextField.setVisible(false);
        }
    }

    @FXML
    private void handleTogglePassword() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
            passwordTextField.requestFocus();
            passwordTextField.selectEnd();
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            togglePasswordBtn.setText("👁");
            passwordField.requestFocus();
            passwordField.selectEnd();
        }
    }

    @FXML
    public void handleLogin() {
        // 1. Lấy password từ ô đang hiển thị
        String password = isPasswordVisible ? passwordTextField.getText() : passwordField.getText();

        // 2. Validate thủ công để chắc chắn không bị lỗi do ô nhập bị ẩn
        if (usernameField.getText().trim().isEmpty()) {
            errorLabel.setText("Vui lòng nhập tên đăng nhập.");
            errorLabel.setVisible(true);
            return;
        }

        if (password == null || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập mật khẩu.");
            errorLabel.setVisible(true);
            return;
        }

        String username = usernameField.getText().trim();
        errorLabel.setVisible(false);

        Runnable onComplete = (loginButton != null && loadingSpinner != null)
                ? UiUtils.showLoading(loginButton, loadingSpinner)
                : () -> { if (loginButton != null) loginButton.setDisable(false); };

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", username);
        payload.put("password", password); // Gửi mật khẩu dạng text thuần túy lên Server

        MessageRequest request = new MessageRequest(
                "LOGIN", ClientSession.getInstance().getToken(), payload);

        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(request));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if (response.isOk()) {
                handleLoginSuccess(response);
            } else {
                Platform.runLater(() -> {
                    errorLabel.setText(response.getMessage());
                    errorLabel.setVisible(true);
                });
            }
            onComplete.run();
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                errorLabel.setText("Lỗi kết nối máy chủ.");
                errorLabel.setVisible(true);
            });
            onComplete.run();
        });

        new Thread(task, "login-task").start();
    }

    private void handleLoginSuccess(MessageResponse response) {
        Object payload = response.getPayload();
        if (payload instanceof Map<?, ?> map) {
            ClientSession.getInstance().login(
                    (String) map.get("token"),
                    (String) map.get("userId"),
                    (String) map.get("username"),
                    (String) map.get("role")
            );
        }
        Platform.runLater(() -> ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST));
    }

    @FXML
    public void handleRegister() {
        ViewRouter.getInstance().navigateTo(Views.REGISTER);
    }
}