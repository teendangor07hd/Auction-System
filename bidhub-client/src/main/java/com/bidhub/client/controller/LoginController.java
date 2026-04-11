package com.bidhub.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller cho màn hình đăng nhập (LoginView.fxml).
 *
 * <p>Tuần 1: Chỉ có skeleton, chưa có logic kết nối server.
 * Logic thật (gọi API Login) sẽ được implement ở Tuần 5.
 *
 * <p>Pattern: JavaFX MVC
 * <ul>
 *   <li>View: LoginView.fxml</li>
 *   <li>Controller: LoginController (class này)</li>
 *   <li>Model: sẽ có User, ClientSession ở tuần 5</li>
 * </ul>
 */
public class LoginController {

    /** TextField nhập tên đăng nhập — kết nối từ FXML qua fx:id="usernameField" */
    @FXML
    private TextField usernameField;

    /** PasswordField nhập mật khẩu */
    @FXML
    private PasswordField passwordField;

    /** Label hiển thị lỗi (ẩn mặc định) */
    @FXML
    private Label errorLabel;

    /** Button đăng nhập */
    @FXML
    private Button loginButton;

    /**
     * JavaFX gọi method này SAU KHI tất cả @FXML fields đã được inject.
     * Dùng để setup bindings, event listeners, giá trị mặc định.
     *
     * <p>Tuần 1: Chỉ disable button khi fields rỗng (binding đơn giản).
     */
    @FXML
    public void initialize() {
        // Disable button khi username hoặc password đang rỗng
        // Bindings.or() trả về BooleanBinding: true khi ít nhất 1 field rỗng
        loginButton.disableProperty().bind(
                usernameField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        // Ẩn label lỗi ban đầu
        errorLabel.setVisible(false);
        errorLabel.setManaged(false); // Không chiếm không gian khi ẩn
    }

    /**
     * Xử lý khi người dùng click "Đăng nhập" hoặc nhấn Enter trong form.
     *
     * <p>Tuần 1: Chỉ in ra console để test — sẽ gọi API thật ở Tuần 5.
     * <p>onAction="#handleLogin" trong FXML kết nối đến method này.
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Tuần 1: Demo validation cơ bản (chưa gọi server)
        if (username.length() < 3) {
            showError("Tên đăng nhập phải có ít nhất 3 ký tự");
            return;
        }

        // TODO Tuần 5: Gọi NetworkTask → LoginCommand → nhận token → navigate AuctionList
        System.out.println("[DEBUG] Đăng nhập với username: " + username);
        showError(""); // Xóa lỗi cũ
        System.out.println("[DEBUG] Tuần 5 sẽ implement kết nối server thật");
    }

    /**
     * Xử lý khi người dùng click "Đăng ký".
     *
     * <p>TODO Tuần 5: ViewRouter.navigateTo(Views.REGISTER)
     */
    @FXML
    private void handleRegister() {
        System.out.println("[DEBUG] Chuyển sang màn hình đăng ký — implement tuần 5");
    }

    /**
     * Hiển thị thông báo lỗi dưới form.
     *
     * @param message nội dung lỗi; truyền chuỗi rỗng để ẩn label
     */
    private void showError(String message) {
        if (message == null || message.isBlank()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}