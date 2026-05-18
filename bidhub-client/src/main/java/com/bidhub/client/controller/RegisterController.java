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
        passwordMatchLabel.managedProperty().bind(passwordMatchLabel.visibleProperty());
        passwordMatchLabel.setText("Mật khẩu xác nhận không khớp!");

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
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("🙈");
            passwordTextField.requestFocus();
        } else {
            passwordField.setText(passwordTextField.getText());
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
            confirmPasswordTextField.setText(confirmPasswordField.getText());
            confirmPasswordTextField.setVisible(true);
            confirmPasswordTextField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            toggleConfirmBtn.setText("🙈");
            confirmPasswordTextField.requestFocus();
        } else {
            confirmPasswordField.setText(confirmPasswordTextField.getText());
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
        // Lấy password từ field đang hiển thị
        String password = isPasswordVisible ? passwordTextField.getText() : passwordField.getText();
        String confirmPassword = isConfirmVisible ? confirmPasswordTextField.getText() : confirmPasswordField.getText();
        String email = emailField.getText().trim();
        String role = selectedRole;

        // Client-side validation
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        hideError();
        registerButton.disableProperty().unbind();
        registerButton.setDisable(true);

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
                registerButton.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            showError("Không kết nối được máy chủ. Thử lại sau.");
            registerButton.setDisable(false);
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