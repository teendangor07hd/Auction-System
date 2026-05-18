package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller cho màn hình đăng ký.
 *
 * <p>// 📌 [Tieu chi: MVC — Controller điều phối form validation + network]
 * Validation realtime: password confirmation bind, email check.
 * Submit → NetworkTask → REGISTER → thành công → navigate về LoginView.
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private Button toggleConfirmBtn;
    @FXML private Label passwordMatchLabel;
    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    @FXML private Button btnRoleBidder;
    @FXML private Button btnRoleSeller;

    private String selectedRole = "BIDDER"; // Mặc định là BIDDER
    private boolean isPasswordVisible = false;
    private boolean isConfirmVisible = false;
    private final javafx.beans.property.BooleanProperty isRegistering = new javafx.beans.property.SimpleBooleanProperty(false);

    // Style cho nút được chọn (active)
    private static final String STYLE_ACTIVE =
            "-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-font-size: 13px; -fx-padding: 14; -fx-background-radius: 10; -fx-cursor: hand; " +
            "-fx-border-color: #4F46E5; -fx-border-radius: 10; -fx-border-width: 2;";

    // Style cho nút chưa được chọn (inactive)
    private static final String STYLE_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #B7BDC6; -fx-font-weight: bold; " +
            "-fx-font-size: 13px; -fx-padding: 14; -fx-background-radius: 10; -fx-cursor: hand; " +
            "-fx-border-color: #475569; -fx-border-radius: 10; -fx-border-width: 2;";

    @FXML
    public void initialize() {
        // Mặc định chọn BIDDER
        updateRoleStyles();

        // Ẩn text fields cho password
        if (passwordTextField != null) {
            passwordTextField.setManaged(false);
            passwordTextField.setVisible(false);
        }
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.setManaged(false);
            confirmPasswordTextField.setVisible(false);
        }

        // Đồng bộ dữ liệu giữa TextField và PasswordField
        if (passwordTextField != null) {
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        }
        if (confirmPasswordTextField != null) {
            confirmPasswordTextField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
        }

        // 📌 [Tieu chi: MVC — bind realtime password confirmation]
        passwordMatchLabel.visibleProperty().bind(
                Bindings.createBooleanBinding(
                        () -> {
                            String pw = passwordField.getText();
                            String cpw = confirmPasswordField.getText();
                            return !cpw.isEmpty() && !pw.equals(cpw);
                        },
                        passwordField.textProperty(),
                        confirmPasswordField.textProperty()
                )
        );
        passwordMatchLabel.managedProperty().bind(passwordMatchLabel.visibleProperty());
        passwordMatchLabel.setText("Mật khẩu xác nhận không khớp!");

        registerButton.disableProperty().bind(isRegistering);

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /** Chọn vai trò BIDDER */
    @FXML
    public void handleSelectBidder() {
        selectedRole = "BIDDER";
        updateRoleStyles();
    }

    /** Chọn vai trò SELLER */
    @FXML
    public void handleSelectSeller() {
        selectedRole = "SELLER";
        updateRoleStyles();
    }

    /** Cập nhật giao diện nút role dựa trên selectedRole */
    private void updateRoleStyles() {
        if (btnRoleBidder != null && btnRoleSeller != null) {
            if ("BIDDER".equals(selectedRole)) {
                btnRoleBidder.setStyle(STYLE_ACTIVE);
                btnRoleSeller.setStyle(STYLE_INACTIVE);
            } else {
                btnRoleSeller.setStyle(STYLE_ACTIVE);
                btnRoleBidder.setStyle(STYLE_INACTIVE);
            }
        }
    }

    /** Hiển thị/ẩn mật khẩu */
    @FXML
    public void handleTogglePassword() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
            passwordTextField.requestFocus();
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            togglePasswordBtn.setText("👁");
            passwordField.requestFocus();
        }
    }

    /** Hiển thị/ẩn mật khẩu xác nhận */
    @FXML
    public void handleToggleConfirm() {
        isConfirmVisible = !isConfirmVisible;
        if (isConfirmVisible) {
            confirmPasswordTextField.setVisible(true);
            confirmPasswordTextField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            toggleConfirmBtn.setText("🙈");
            confirmPasswordTextField.requestFocus();
        } else {
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmPasswordTextField.setVisible(false);
            confirmPasswordTextField.setManaged(false);
            toggleConfirmBtn.setText("👁");
            confirmPasswordField.requestFocus();
        }
    }

    /**
     * Xử lý click nút "Đăng ký" — gửi request REGISTER qua NetworkTask.
     */
    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        String role = selectedRole;

        // Bắt lỗi ValidationException gom tất cả lỗi cùng lúc
        try {
            java.util.List<String> errors = new java.util.ArrayList<>();
            if (username.isBlank()) errors.add("Tên đăng nhập không được để trống.");
            if (password.length() < 8) errors.add("Mật khẩu phải có ít nhất 8 ký tự.");
            if (email.isBlank()) errors.add("Email không được để trống.");
            else if (!email.contains("@")) errors.add("Email không hợp lệ (phải chứa @).");
            if (!password.equals(confirmPassword)) errors.add("Mật khẩu xác nhận không khớp.");
            
            if (!errors.isEmpty()) {
                throw new com.bidhub.common.exception.ValidationException(errors);
            }
        } catch (com.bidhub.common.exception.ValidationException e) {
            showError(String.join("\n", e.getErrors()));
            return;
        }

        hideError();
        isRegistering.set(true);

        // 📌 [Tieu chi: MVC — tạo REGISTER request payload]
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
                showError(response.getMessage());
                isRegistering.set(false);
            }
        });

        task.setOnFailed(e -> {
            showError("Không kết nối được máy chủ. Thử lại sau.");
            isRegistering.set(false);
        });

        new Thread(task).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Chuyển về màn hình đăng nhập.
     */
    @FXML
    public void handleBackToLogin() {
        ViewRouter.getInstance().navigateTo(Views.LOGIN);
    }

    @FXML
    public void handleBackToHome() {
        ViewRouter.getInstance().navigateTo(Views.HOME);
    }
}