package com.bidhub.client.controller;

import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.util.Map;

public class HomeController {

    @FXML private Button btnExplore;
    @FXML private Button btnLogin;
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblParticipants;
    @FXML private Label lblTotalVolume;
    @FXML private FlowPane hotAuctionsContainer;
    @FXML private Hyperlink linkSeeAll;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String PLACEHOLDER_URL = "https://placehold.co/260x160/1E2329/B7BDC6?text=BidHub";

    // Số lần thử lại khi server chưa kết nối
    private static final int MAX_RETRY = 5;
    private static final int RETRY_DELAY_MS = 600;

    @FXML
    public void initialize() {
        btnExplore.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST));
        btnLogin.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.LOGIN));
        if (linkSeeAll != null) {
            linkSeeAll.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST));
        }

        // Fix bug 6: Chờ kết nối sẵn sàng mới load dữ liệu
        ensureConnectedThenLoad();
    }

    /**
     * Đợi server kết nối xong (tối đa MAX_RETRY lần) rồi mới load data.
     * Fixes bug: HomeView tải trước khi BidHubApp.connectToServer() hoàn thành.
     */
    private void ensureConnectedThenLoad() {
        Thread waitThread = new Thread(() -> {
            for (int i = 0; i < MAX_RETRY; i++) {
                if (ServerGateway.getInstance().isConnected()) {
                    Platform.runLater(() -> {
                        loadStats();
                        loadHotAuctions();
                    });
                    return;
                }
                try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ex) { break; }
            }
            // Sau khi retry hết vẫn chưa kết nối → hiển thị thông báo lỗi
            Platform.runLater(() -> {
                if (hotAuctionsContainer != null) {
                    hotAuctionsContainer.getChildren().clear();
                    Label errLabel = new Label("Không kết nối được máy chủ. Hãy kiểm tra server và khởi động lại ứng dụng.");
                    errLabel.setStyle("-fx-text-fill: #F6465D; -fx-font-size: 14px; -fx-padding: 20;");
                    errLabel.setWrapText(true);
                    hotAuctionsContainer.getChildren().add(errLabel);
                }
            });
        }, "home-wait-connect");
        waitThread.setDaemon(true);
        waitThread.start();
    }

    private void loadStats() {
        MessageRequest req = new MessageRequest("GET_HOME_STATS", null, null);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));
        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            if (resp.isOk() && resp.getPayload() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) resp.getPayload();
                Platform.runLater(() -> {
                    long active = ((Number) stats.getOrDefault("activeAuctions", 0)).longValue();
                    long participants = ((Number) stats.getOrDefault("totalParticipants", 0)).longValue();
                    double volume = ((Number) stats.getOrDefault("totalVolume", 0.0)).doubleValue();

                    lblActiveAuctions.setText(String.valueOf(active));
                    lblParticipants.setText(participants == 0 ? "0" : String.format("%,d", participants));

                    if (volume == 0) {
                        lblTotalVolume.setText("0 đ");
                    } else if (volume >= 1_000_000_000) {
                        lblTotalVolume.setText(String.format("%.1f Tỷ đ", volume / 1_000_000_000.0));
                    } else if (volume >= 1_000_000) {
                        lblTotalVolume.setText(String.format("%.1f Tr đ", volume / 1_000_000.0));
                    } else {
                        lblTotalVolume.setText(String.format("%,.0f đ", volume));
                    }
                });
            }
        });
        task.setOnFailed(e -> {}); // Giữ mặc định "0"
        new Thread(task).start();
    }

    private void loadHotAuctions() {
        MessageRequest req = new MessageRequest("GET_AUCTION_LIST", null, null);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));
        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            if (resp.isOk()) {
                JsonNode payload = mapper.valueToTree(resp.getPayload());
                if (payload.isArray()) {
                    Platform.runLater(() -> {
                        hotAuctionsContainer.getChildren().clear();
                        int count = 0;
                        for (JsonNode node : payload) {
                            if (count >= 4) break;
                            hotAuctionsContainer.getChildren().add(createAuctionCard(node));
                            count++;
                        }
                        if (count == 0) {
                            Label emptyLabel = new Label("Chưa có phiên đấu giá nào đang diễn ra.");
                            emptyLabel.setStyle("-fx-text-fill: #B7BDC6; -fx-font-size: 15px; -fx-padding: 30;");
                            hotAuctionsContainer.getChildren().add(emptyLabel);
                        }
                    });
                }
            }
        });
        task.setOnFailed(e -> Platform.runLater(() -> {
            hotAuctionsContainer.getChildren().clear();
            Label errLabel = new Label("Không thể tải danh sách đấu giá. Kiểm tra kết nối máy chủ.");
            errLabel.setStyle("-fx-text-fill: #F6465D; -fx-font-size: 14px; -fx-padding: 20;");
            hotAuctionsContainer.getChildren().add(errLabel);
        }));
        new Thread(task).start();
    }

    private VBox createAuctionCard(JsonNode node) {
        String auctionId = node.path("id").asText("");
        String itemName = node.path("itemName").asText("Sản phẩm");
        String imageUrl = node.path("imageUrl").asText("");
        double price = node.path("currentHighestBid").asDouble(0);
        if (price == 0) price = node.path("startingPrice").asDouble(0);
        String status = node.path("status").asText("PENDING");
        final double finalPrice = price;

        VBox card = new VBox(10);
        card.setPrefWidth(250);
        String styleNormal = "-fx-background-color: #1E2329; -fx-background-radius: 14; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 4);";
        String styleHover  = "-fx-background-color: #252D38; -fx-background-radius: 14; -fx-cursor: hand; -fx-border-color: #4F46E5; -fx-border-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.3), 16, 0, 0, 6);";
        card.setStyle(styleNormal);
        card.setPadding(new Insets(0, 0, 15, 0));
        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e -> card.setStyle(styleNormal));
        card.setOnMouseClicked(e -> ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL, Map.of("auctionId", auctionId)));

        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: #2B3139; -fx-background-radius: 14 14 0 0;");
        imageContainer.setPrefHeight(165); imageContainer.setMaxHeight(165); imageContainer.setMinHeight(165);

        ImageView iv = new ImageView();
        iv.setFitWidth(250); iv.setFitHeight(165); iv.setPreserveRatio(false);
        loadImageSafely(iv, imageUrl, 250, 165);
        Rectangle clip = new Rectangle(250, 165); clip.setArcWidth(28); clip.setArcHeight(28);
        iv.setClip(clip);
        imageContainer.getChildren().add(iv);

        Label badge = new Label(status.equals("RUNNING") ? "● Đang diễn ra" : "◌ Chờ bắt đầu");
        badge.setStyle("-fx-background-color: " + (status.equals("RUNNING") ? "rgba(16,185,129,0.9)" : "rgba(100,116,139,0.9)") + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 20;");
        StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
        imageContainer.getChildren().add(badge);

        VBox info = new VBox(6);
        info.setPadding(new Insets(12, 15, 0, 15));
        Label lblName = new Label(itemName);
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        lblName.setWrapText(true);
        Label lblPrice = new Label(String.format("%,.0f đ", finalPrice));
        lblPrice.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: 900; -fx-font-size: 16px;");
        info.getChildren().addAll(lblName, lblPrice);
        card.getChildren().addAll(imageContainer, info);
        return card;
    }

    private void loadImageSafely(ImageView iv, String url, double w, double h) {
        if (url != null && !url.isBlank()) {
            try {
                Image img = new Image(url, w, h, false, true, true);
                img.errorProperty().addListener((obs, old, hasError) -> {
                    if (hasError) Platform.runLater(() -> iv.setImage(createPlaceholderImage(w, h)));
                });
                iv.setImage(img);
                return;
            } catch (Exception ignored) {}
        }
        iv.setImage(createPlaceholderImage(w, h));
    }

    private Image createPlaceholderImage(double w, double h) {
        try { return new Image(PLACEHOLDER_URL, w, h, false, true, true); } catch (Exception e) { return null; }
    }
}
