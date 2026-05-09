package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.beans.binding.Bindings;

/**
 * Controller cho man hinh dang nhap.
 *
 * <p>// 📌 [Tieu chi: MVC — Controller dieu phoi UI + network + navigation]
 * Flow: validate → NetworkTask → ServerGateway → ClientSession → navigateTo.
 * setOnSucceeded chay tren FX thread — KHONG can Platform.runLater().
 */
public class LoginController {
    private javafx.beans.property.BooleanProperty isLoading = new javafx.beans.property.SimpleBooleanProperty(false);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;


    @FXML
    public void initialize() {
        loginButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        // Nút sẽ disable nếu ô chữ trống HOẶC đang trong trạng thái loading
                        () -> usernameField.getText().isBlank()
                                || passwordField.getText().isBlank()
                                || isLoading.get(),
                        usernameField.textProperty(),
                        passwordField.textProperty(),
                        isLoading // Lắng nghe thêm biến isLoading
                )
        );
        errorLabel.setVisible(false);
        errorLabel.getStyleClass().add("error-message");
    }

    /**
     * Xu ly click nut "Dang nhap" — gui request LOGIN qua NetworkTask.
     */
    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setVisible(false);
        isLoading.set(true); // Bắt đầu loading, binding sẽ tự động disable nút

        // 📌 [Tieu chi: MVC — tao request JSON payload]
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", username);
        payload.put("password", password);

        MessageRequest request = new MessageRequest(
                "LOGIN", ClientSession.getInstance().getToken(), payload);

        // 📌 [Tieu chi: Ky thuat quan trong — NetworkTask khong block FX thread]
        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(request));

        task.setOnSucceeded(e -> {
            isLoading.set(false); // Đưa lên đầu: Dù thành công hay thất bại, tiến trình mạng đã xong thì gỡ loading.

            MessageResponse response = task.getValue();
            if (response.isOk()) {
                handleLoginSuccess(response);
            } else {
                errorLabel.setText(response.getMessage());
                errorLabel.setVisible(true);
            }
        });

        task.setOnFailed(e -> {
            isLoading.set(false); // Lỗi mạng (Exception) rơi vào đây, cũng gỡ loading.
            errorLabel.setText("Khong ket noi duoc may chu. Thu lai sau.");
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
    }

    /**
     * Xu ly dang nhap thanh cong — luu session va chuyen man hinh.
     */
    private void handleLoginSuccess(MessageResponse response) {
        // 📌 [Tieu chi: Quan ly nguoi dung — luu thong tin dang nhap vao ClientSession]
        Object payload = response.getPayload();
        if (payload instanceof java.util.Map<?, ?> map) {
            String token = (String) map.get("token");
            String userId = (String) map.get("userId");
            String username = (String) map.get("username");
            String role = (String) map.get("role");
            ClientSession.getInstance().login(token, userId, username, role);
        }
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
    }

    /**
     * Chuyen sang man hinh dang ky.
     */
    @FXML
    public void handleRegister() {
        ViewRouter.getInstance().navigateTo(Views.REGISTER);
    }
}