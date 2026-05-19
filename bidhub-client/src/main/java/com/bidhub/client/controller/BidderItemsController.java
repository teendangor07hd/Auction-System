package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.List;

public class BidderItemsController {

    @FXML private FlowPane cardContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private Label lblEmptyMessage;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label lblCount;

    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        loadWonAuctions();
    }

    private void loadWonAuctions() {
        if (loadingSpinner != null) loadingSpinner.setVisible(true);
        if (cardContainer != null) cardContainer.getChildren().clear();
        if (lblEmptyMessage != null) lblEmptyMessage.setVisible(false);

        MessageRequest req = new MessageRequest();
        req.setType("GET_WON_AUCTIONS");
        req.setToken(ClientSession.getInstance().getToken());

        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                if (loadingSpinner != null) loadingSpinner.setVisible(false);
                if (resp.isOk() && resp.getPayload() != null) {
                    JsonNode listNode = mapper.valueToTree(resp.getPayload());
                    if (listNode.isArray()) {
                        List<JsonNode> nodes = new ArrayList<>();
                        listNode.forEach(nodes::add);
                        renderCards(nodes);
                    }
                } else {
                    if (lblEmptyMessage != null) {
                        lblEmptyMessage.setText("Không thể tải dữ liệu: " + resp.getMessage());
                        lblEmptyMessage.setVisible(true);
                    }
                }
            });
        });
        task.setOnFailed(e -> Platform.runLater(() -> {
            if (loadingSpinner != null) loadingSpinner.setVisible(false);
            if (lblEmptyMessage != null) {
                lblEmptyMessage.setText("Lỗi kết nối đến máy chủ.");
                lblEmptyMessage.setVisible(true);
            }
        }));
        new Thread(task, "get-won-auctions").start();
    }

    private void renderCards(List<JsonNode> items) {
        if (cardContainer == null) return;
        cardContainer.getChildren().clear();

        if (lblCount != null) {
            lblCount.setText(items.size() + " sản phẩm");
        }

        if (items.isEmpty()) {
            if (lblEmptyMessage != null) {
                lblEmptyMessage.setText("Bạn chưa thắng phiên đấu giá nào.");
                lblEmptyMessage.setVisible(true);
            }
            return;
        }

        for (JsonNode node : items) {
            VBox card = createCard(node);
            cardContainer.getChildren().add(card);
        }
    }

    private VBox createCard(JsonNode node) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #1A1F2E; -fx-background-radius: 12; "
                + "-fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12; "
                + "-fx-padding: 15; -fx-cursor: hand;");
        card.setPrefWidth(280);

        String itemName = node.path("itemName").asText("Unknown");
        double price = node.path("currentHighestBid").asDouble(0);
        String seller = node.path("sellerName").asText("Unknown");
        String sellerEmail = node.path("sellerEmail").asText("");
        String endTime = node.path("endTime").asText("");

        // Hieu ung hover
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #2E364F; -fx-background-radius: 12; "
                + "-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12; "
                + "-fx-padding: 15; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #1A1F2E; -fx-background-radius: 12; "
                + "-fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12; "
                + "-fx-padding: 15; -fx-cursor: hand;"));

        // Click xem chi tiet
        String auctionId = node.path("id").asText();
        card.setOnMouseClicked(e -> {
            ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL, java.util.Map.of("auctionId", auctionId));
        });

        // Image placeholder
        StackPane imagePane = new StackPane();
        imagePane.setStyle("-fx-background-color: #2B3139; -fx-background-radius: 8;");
        imagePane.setPrefHeight(140);
        Label lblImg = new Label("Hình ảnh");
        lblImg.setStyle("-fx-text-fill: #64748B;");
        
        String url = node.path("imageUrl").asText("");
        if (!url.isBlank()) {
            try {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(url, true));
                iv.setFitWidth(250);
                iv.setFitHeight(140);
                iv.setPreserveRatio(true);
                imagePane.getChildren().add(iv);
            } catch (Exception ex) {
                imagePane.getChildren().add(lblImg);
            }
        } else {
            imagePane.getChildren().add(lblImg);
        }

        // Title
        Label title = new Label(itemName);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        title.setWrapText(true);

        // Seller
        Label lblSeller = new Label("Người bán: " + seller + (sellerEmail.isBlank() ? "" : " (" + sellerEmail + ")"));
        lblSeller.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");

        // Price
        Label lblPrice = new Label(String.format("Giá thắng: %,.0f VNĐ", price));
        lblPrice.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #10B981;");

        // Status badge
        Label badge = new Label("Đã thắng");
        badge.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Time
        String shortTime = endTime;
        if (shortTime.contains("T")) {
            shortTime = shortTime.replace("T", " ").substring(0, 16);
        }
        Label lblTime = new Label("Kết thúc lúc: " + shortTime);
        lblTime.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        HBox top = new HBox(badge);
        top.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(imagePane, title, lblSeller, lblPrice, top, lblTime);
        return card;
    }

    @FXML
    public void handleRefresh() {
        loadWonAuctions();
    }
}
