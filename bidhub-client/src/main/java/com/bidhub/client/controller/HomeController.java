package com.bidhub.client.controller;

import com.bidhub.client.navigation.ViewRouter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class HomeController {

    @FXML private Button btnExplore;
    @FXML private Button btnLogin;

    @FXML
    public void initialize() {
        // Mở màn hình đấu giá nếu nhấn Khám phá
        btnExplore.setOnAction(e -> {
            ViewRouter.getInstance().navigate("/fxml/AuctionListView.fxml");
        });

        // Mở màn hình đăng nhập nếu nhấn Login
        btnLogin.setOnAction(e -> {
            ViewRouter.getInstance().navigate("/fxml/LoginView.fxml");
        });
    }
}
