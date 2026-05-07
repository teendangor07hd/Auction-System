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
 * Validation realtime: password confirmation bind, email check.
 * Submit → NetworkTask → REGISTER → thanh cong → navigate ve LoginView.
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
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
        passwordMatchLabel.setText("Mat khau xac nhan khong khop!");

        registerButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> usernameField.getText().isBlank()
                                || passwordField.getText().length() < 8
                                || emailField.getText().isBlank()
                                || !emailField.getText().contains("@")
                                || !passwordField.getText().equals(confirmPasswordField.getText()),
                        usernameField.textProperty(),
                        passwordField.textProperty(),
                        emailField.textProperty()
                )
        );

        errorLabel.setVisible(false);
        errorLabel.getStyleClass().add("error-message");
    }

    /**
     * Xu ly click nut "Dang ky" — gui request REGISTER qua NetworkTask.
     */
    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        String role = roleChoiceBox.getValue();

        // Client-side validation
        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Mat khau xac nhan khong khop!");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setVisible(false);
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
                registerButton.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            errorLabel.setText("Khong ket noi duoc may chu. Thu lai sau.");
            errorLabel.setVisible(true);
            registerButton.setDisable(false);
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