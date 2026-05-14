package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.client.util.UiUtils; // THÊM IMPORT UiUtils
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressIndicator;
import java.util.Map;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    // 📌 [Tieu chi: UX — Loading state component]
    @FXML private ProgressIndicator loadingSpinner;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    public void handleLogin() {
        // 📌 [Tieu chi: UX — Form validation client-side]
        if (!UiUtils.validateNotEmpty(usernameField, "Tên đăng nhập")) return;
        if (!UiUtils.validateNotEmpty(passwordField, "Mật khẩu")) return;

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setVisible(false);

        // 📌 [Tieu chi: UX — Loading state]
        Runnable onComplete = (loginButton != null && loadingSpinner != null)
                ? UiUtils.showLoading(loginButton, loadingSpinner)
                : () -> { if (loginButton != null) loginButton.setDisable(false); };

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", username);
        payload.put("password", password);

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
                errorLabel.setText("Không kết nối được máy chủ. Thử lại sau.");
                errorLabel.setVisible(true);
            });
            onComplete.run();
        });

        new Thread(task, "login-task").start();
    }

    private void handleLoginSuccess(MessageResponse response) {
        Object payload = response.getPayload();
        if (payload instanceof Map<?, ?> map) {
            String token = (String) map.get("token");
            String userId = (String) map.get("userId");
            String username = (String) map.get("username");
            String role = (String) map.get("role");
            ClientSession.getInstance().login(token, userId, username, role);
        }
        Platform.runLater(() -> ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST));
    }

    @FXML
    public void handleRegister() {
        ViewRouter.getInstance().navigateTo(Views.REGISTER);
    }
}