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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.beans.binding.Bindings;
import java.util.Map;

public class LoginController {
    private javafx.beans.property.BooleanProperty isLoading = new javafx.beans.property.SimpleBooleanProperty(false);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        // Logic Binding vô hiệu hóa nút Login khi chưa nhập đủ thông tin hoặc đang tải
        loginButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> usernameField.getText().isBlank()
                                || passwordField.getText().isBlank()
                                || isLoading.get(),
                        usernameField.textProperty(),
                        passwordField.textProperty(),
                        isLoading
                )
        );

        errorLabel.setVisible(false);
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setVisible(false);
        isLoading.set(true);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", username);
        payload.put("password", password);

        MessageRequest request = new MessageRequest(
                "LOGIN", ClientSession.getInstance().getToken(), payload);

        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(request));

        task.setOnSucceeded(e -> {
            isLoading.set(false);
            MessageResponse response = task.getValue();
            if (response.isOk()) {
                handleLoginSuccess(response);
            } else {
                errorLabel.setText(response.getMessage());
                errorLabel.setVisible(true);
            }
        });

        task.setOnFailed(e -> {
            isLoading.set(false);
            errorLabel.setText("Không kết nối được máy chủ. Thử lại sau.");
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
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
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
    }

    @FXML
    public void handleRegister() {
        ViewRouter.getInstance().navigateTo(Views.REGISTER);
    }
}