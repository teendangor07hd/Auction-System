package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Map;

/** Skeleton controller cho AuctionDetailView. Implement ContextAware để nhận auctionId. */
public class AuctionDetailController implements ContextAware {

    @FXML private Label lblTitle;
    @FXML private Label lblStatus;
    @FXML private Label lblCurrentBid;
    @FXML private Label lblTimeRemaining;
    @FXML private TextField tfBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Label lblMessage;

    private String auctionId;

    @FXML
    public void initialize() {
        lblMessage.setVisible(false);
        btnPlaceBid.setDisable(true); // Enable sau khi load auction data (Tuần 5)
    }

    @Override
    public void setContext(Map<String, Object> params) {
        this.auctionId = (String) params.get("auctionId");
        lblTitle.setText("Phiên: " + auctionId); // Placeholder — thay bằng data thật Tuần 5
    }

    @FXML
    private void handlePlaceBid() {
        // Skeleton — Tuần 6 thêm NetworkClient gọi PLACE_BID request
        lblMessage.setText("Chức năng đặt giá sẽ sẵn sàng từ Tuần 6.");
        lblMessage.setVisible(true);
    }
}