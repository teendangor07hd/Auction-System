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

/**
 * Controller xử lý màn hình đăng nhập của ứng dụng BidHub.
 * <p>
 * Chịu trách nhiệm thu thập thông tin đăng nhập từ người dùng,
 * thực hiện xác thực với máy chủ qua {@link ServerGateway},
 * và điều hướng đến màn hình phù hợp sau khi đăng nhập thành công.
 * </p>
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loadingSpinner;

    /** Trạng thái hiển thị mật khẩu hiện tại (true = đang hiển thị rõ). */
    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        if (passwordTextField != null) {
            passwordTextField.setManaged(false);
            passwordTextField.setVisible(false);
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        }
    }

    /**
     * Xử lý sự kiện bật/tắt hiển thị mật khẩu khi người dùng nhấn nút toggle.
     * <p>
     * Chuyển đổi giữa {@link PasswordField} (ẩn ký tự) và {@link TextField} (hiển thị rõ).
     * Hai trường được đồng bộ dữ liệu qua {@code bindBidirectional} nên nội đúng
     * luôn nhất quán khi chuyển đổi.
     * </p>
     */
    @FXML
    private void handleTogglePassword() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
            passwordTextField.requestFocus();
            passwordTextField.selectEnd();
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            togglePasswordBtn.setText("👁");
            passwordField.requestFocus();
            passwordField.selectEnd();
        }
    }

    /**
     * Xử lý sự kiện đăng nhập khi người dùng nhấn nút "Đăng nhập".
     * <p>
     * Thực hiện theo thứ tự:
     * <ol>
     *   <li>Lấy và kiểm tra tính hợp lệ của username và password.</li>
     *   <li>Hiển thị spinner loading và vô hiệu hoá nút đăng nhập.</li>
     *   <li>Gửi yêu cầu {@code LOGIN} đến máy chủ qua {@link ServerGateway} trên một Thread riêng.</li>
     *   <li>Xử lý kết quả: điều hướng thành công hoặc hiển thị thông báo lỗi.</li>
     * </ol>
     * </p>
     */
    @FXML
    public void handleLogin() {
        // Lấy password (do đã bindBidirectional nên lấy từ passwordField luôn đúng)
        String password = passwordField.getText();

        String username = usernameField.getText().trim();
        // Gom tất cả lỗi validation vào danh sách rồi ném ValidationException một lần
        try {
            java.util.List<String> errors = new java.util.ArrayList<>();
            if (username.isEmpty()) errors.add("Vui lòng nhập tên đăng nhập.");
            if (password == null || password.isEmpty()) errors.add("Vui lòng nhập mật khẩu.");
            
            if (!errors.isEmpty()) {
                throw new com.bidhub.common.exception.ValidationException(errors);
            }
        } catch (com.bidhub.common.exception.ValidationException e) {
            errorLabel.setText(String.join("\n", e.getErrors()));
            errorLabel.setVisible(true);
            return;
        }

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

    /**
     * Xử lý kết quả đăng nhập thành công từ máy chủ.
     * <p>
     * Trích xuất thông tin Token, userId, username và role từ payload phản hồi,
     * lưu vào {@link ClientSession}, sau đó điều hướng đến màn hình danh sách đấu giá.
     * </p>
     *
     * @param response Phản hồi từ máy chủ chứa thông tin phiên đăng nhập.
     */
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

    /**
     * Điều hướng người dùng đến màn hình đăng ký tài khoản mới.
     */
    @FXML
    public void handleRegister() {
        ViewRouter.getInstance().navigateTo(Views.REGISTER);
    }

    /**
     * Điều hướng người dùng quay về màn hình trang chủ.
     */
    @FXML
    public void handleBackToHome() {
        ViewRouter.getInstance().navigateTo(Views.HOME);
    }
}