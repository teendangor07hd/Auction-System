package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller chi tiet phien dau gia — hien thi thong tin, countdown, form dat gia.
 *
 * <p>Implement {@link ContextAware} de nhan auctionId tu ViewRouter.
 * Countdown timer dung {@link Timeline} + {@link KeyFrame} cap nhat moi 1 giay.
 *
 * <p>Dong bo hoan toan voi AuctionDetailView.fxml (tat ca fx:id khop).
 */
public class AuctionDetailController implements ContextAware {

    @FXML private Label lblTitle;         // tiep de header
    @FXML private Label lblItemName;
    @FXML private Label lblDescription;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblHighestBidder;
    @FXML private Label lblCountdown;
    @FXML private Label lblStatus;
    @FXML private TextField tfBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnBack;

    private String auctionId;
    private LocalDateTime endTime;
    private Timeline countdownTimeline;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setContext(Map<String, Object> params) {
        this.auctionId = (String) params.get("auctionId");
        if (auctionId != null && !auctionId.isBlank()) {
            loadAuctionDetail();
        }
    }

    @FXML
    public void initialize() {
        btnPlaceBid.setOnAction(e -> placeBid());
        btnBack.setOnAction(e ->
                com.bidhub.client.navigation.ViewRouter.getInstance()
                        .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));
    }

    private void loadAuctionDetail() {
        MessageRequest req = new MessageRequest();
        req.setType("GET_AUCTION_DETAIL");
        req.setPayload(mapper.createObjectNode().put("auctionId", auctionId));

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus()) && response.getPayload() != null) {
                JsonNode payload = mapper.valueToTree(response.getPayload());
                populateLabels(payload);
            } else {
                String msg = response.getMessage();
                Platform.runLater(() -> showError(msg));
            }
        });

        task.setOnFailed(e ->
                Platform.runLater(() -> showError(task.getException().getMessage())));

        new Thread(task).start();
    }

    private void populateLabels(JsonNode payload) {
        JsonNode auction = payload.path("auction");

        // Header va ten san pham
        lblTitle.setText("Chi tiết phiên đấu giá");
        lblItemName.setText(auction.path("itemId").asText("Chưa rõ sản phẩm"));
        lblDescription.setText(auction.path("description").asText(""));

        lblStartingPrice.setText("Giá khởi điểm: " + auction.path("startingPrice").asDouble(0));
        lblCurrentPrice.setText("Giá hiện tại: " + auction.path("currentHighestBid").asDouble(0));
        lblHighestBidder.setText("Người dẫn đầu: " + auction.path("highestBidderId").asText("Chưa có"));
        lblStatus.setText("Trạng thái: " + auction.path("status").asText(""));

        String endTimeStr = auction.path("endTime").asText("");
        if (!endTimeStr.isEmpty()) {
            try {
                endTime = LocalDateTime.parse(endTimeStr);
                startCountdown();
            } catch (Exception ex) {
                lblCountdown.setText("Không thể phân tích thời gian");
            }
        }

        String status = auction.path("status").asText("");
        if ("FINISHED".equals(status)) {
            lblStatus.setText("ĐÃ KẾT THÚC");
            btnPlaceBid.setDisable(true);
            tfBidAmount.setDisable(true);
            stopCountdown();
        }
    }

    private void startCountdown() {
        stopCountdown();
        if (endTime == null) return;

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdown() {
        if (endTime == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            lblCountdown.setText("ĐÃ KẾT THÚC");
            btnPlaceBid.setDisable(true);
            tfBidAmount.setDisable(true);
            stopCountdown();
            return;
        }
        long seconds = java.time.Duration.between(now, endTime).getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        lblCountdown.setText(String.format("Còn lại: %02d:%02d:%02d", hours, minutes, secs));
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void placeBid() {
        String amountStr = tfBidAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showError("Vui lòng nhập giá đấu giá.");
            return;
        }
        double bidAmount;
        try {
            bidAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ex) {
            showError("Giá không hợp lệ. Vui lòng nhập số.");
            return;
        }

        MessageRequest req = new MessageRequest();
        req.setType("PLACE_BID");
        req.setPayload(mapper.createObjectNode()
                .put("auctionId", auctionId)
                .put("bidAmount", bidAmount));

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus())) {
                loadAuctionDetail();
            } else {
                Platform.runLater(() -> showError(response.getMessage()));
            }
        });

        task.setOnFailed(e ->
                Platform.runLater(() -> showError(task.getException().getMessage())));

        new Thread(task).start();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Lỗi");
        alert.showAndWait();
    }

    @FXML
    public void cleanup() {
        stopCountdown();
    }
}