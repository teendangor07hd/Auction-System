package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.client.util.UiUtils;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller danh sách phiên đấu giá — hiển thị lưới dữ liệu và Sidebar Menu.
 * Đồng bộ với màn hình AuctionListView.fxml.
 */
public class AuctionListController {

    // ========================================================================
    // CONSTANTS (Nguyên tắc tránh Magic Strings)
    // ========================================================================
    private static final String CMD_LOGOUT = "LOGOUT";
    private static final String CMD_GET_AUCTION_LIST = "GET_AUCTION_LIST";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String STATUS_OK = "OK";

    // ========================================================================
    // FXML INJECTIONS
    // ========================================================================
    @FXML private FlowPane cardContainer;
    @FXML private ScrollPane scrollPane;

    // UX Components
    @FXML private Button btnRefresh;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label lblEmptyMessage;

    // ========================================================================
    // INTERNAL STATE
    // ========================================================================
    private final ObjectMapper mapper = new ObjectMapper();
    private final ObservableList<JsonNode> auctionData = FXCollections.observableArrayList();
    private final java.util.List<javafx.animation.Timeline> activeTimelines = new java.util.ArrayList<>();

    /**
     * Vòng đời JavaFX: Khởi tạo dữ liệu và sự kiện ngay sau khi load xong UI.
     */
    @FXML
    public void initialize() {
        setupActionHandlers();

        // Tải dữ liệu lần đầu tiên khi mở màn hình
        loadAuctionList();
    }

    /**
     * Bắt sự kiện nhấp vào thẻ sản phẩm để xem chi tiết.
     */
    private void navigateToDetail(String auctionId) {
        if (auctionId != null && !auctionId.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("auctionId", auctionId);
            ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL, params);
        }
    }

    private void populateCards() {
        if (cardContainer == null) return;
        
        for (javafx.animation.Timeline t : activeTimelines) {
            t.stop();
        }
        activeTimelines.clear();
        
        cardContainer.getChildren().clear();

        for (JsonNode node : auctionData) {
            String auctionId = node.path("id").asText("");
            String itemName = node.has("itemName") ? node.get("itemName").asText("") : auctionId;
            String imageUrl = node.has("imageUrl") ? node.get("imageUrl").asText("") : null;
            double price = node.path("currentHighestBid").asDouble(0);
            if (price == 0) price = node.path("startingPrice").asDouble(0);
            String startTimeRaw = node.path("startTime").asText("");
            String endTimeRaw = node.path("endTime").asText("");
            String startTimeStr = startTimeRaw;
            String sellerName = node.path("sellerName").asText("Khong xac dinh");
            String status = node.path("status").asText("PENDING");
            
            String statusVN = switch (status) {
                case "PENDING" -> "Chờ bắt đầu";
                case "RUNNING" -> "Đang diễn ra";
                case "CLOSED" -> "Đã kết thúc";
                default -> status;
            };

            try {
                if (!startTimeStr.isEmpty()) {
                    java.time.LocalDateTime time = java.time.LocalDateTime.parse(startTimeStr);
                    startTimeStr = time.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }
            } catch (Exception ignored) {}

            VBox card = new VBox();
            card.setSpacing(10);
            card.setPrefWidth(280);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-cursor: hand;");
            
            // Effect on hover
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 8); -fx-cursor: hand;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-cursor: hand;"));
            card.setOnMouseClicked(e -> navigateToDetail(auctionId));

            // Image
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
            imageView.setFitWidth(280);
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(false);
            
            // Clip for rounded top corners
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(280, 180);
            clip.setArcWidth(24);
            clip.setArcHeight(24);
            imageView.setClip(clip);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    imageView.setImage(new javafx.scene.image.Image(imageUrl));
                } catch (Exception e) {
                    // Fallback or ignore
                }
            }

            VBox content = new VBox();
            content.setSpacing(12);
            content.setPadding(new javafx.geometry.Insets(15));

            Label lblTitle = new Label(itemName);
            lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-wrap-text: true; -fx-text-fill: #1E1B4B;");
            lblTitle.setMaxWidth(250);
            
            Label lblStatus = new Label(statusVN);
            if (status.equals("RUNNING")) {
                lblStatus.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-padding: 3 8 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else if (status.equals("PENDING")) {
                lblStatus.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 3 8 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                lblStatus.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-padding: 3 8 3 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            }

            HBox priceBox = new HBox(10);
            priceBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label lblPriceIcon = new Label("$");
            lblPriceIcon.setStyle("-fx-background-color: #f1f3f5; -fx-padding: 5 10 5 10; -fx-background-radius: 50; -fx-text-fill: #868e96; -fx-font-weight: bold;");
            VBox priceInfo = new VBox(2);
            Label lblPriceTitle = new Label("Giá khởi điểm");
            lblPriceTitle.setStyle("-fx-text-fill: #868e96; -fx-font-size: 11px;");
            Label lblPriceValue = new Label(String.format("%,.0f đ", price));
            lblPriceValue.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1E1B4B;");
            priceInfo.getChildren().addAll(lblPriceTitle, lblPriceValue);
            priceBox.getChildren().addAll(lblPriceIcon, priceInfo);

            HBox timeBox = new HBox(10);
            timeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label lblTimeIcon = new Label("🕒");
            lblTimeIcon.setStyle("-fx-background-color: #f1f3f5; -fx-padding: 5 8 5 8; -fx-background-radius: 50; -fx-text-fill: #868e96;");
            VBox timeInfo = new VBox(2);
            Label lblTimeTitle = new Label("Thời gian bắt đầu");
            lblTimeTitle.setStyle("-fx-text-fill: #868e96; -fx-font-size: 11px;");
            Label lblTimeValue = new Label(startTimeStr);
            lblTimeValue.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E1B4B;");
            timeInfo.getChildren().addAll(lblTimeTitle, lblTimeValue);
            timeBox.getChildren().addAll(lblTimeIcon, timeInfo);

            HBox sellerBox = new HBox(10);
            sellerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label lblSellerIcon = new Label("👤");
            lblSellerIcon.setStyle("-fx-background-color: #f1f3f5; -fx-padding: 5 8 5 8; -fx-background-radius: 50; -fx-text-fill: #868e96;");
            VBox sellerInfo = new VBox(2);
            Label lblSellerTitle = new Label("Người bán");
            lblSellerTitle.setStyle("-fx-text-fill: #868e96; -fx-font-size: 11px;");
            Label lblSellerValue = new Label(sellerName);
            lblSellerValue.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E1B4B;");
            sellerInfo.getChildren().addAll(lblSellerTitle, lblSellerValue);
            sellerBox.getChildren().addAll(lblSellerIcon, sellerInfo);
            
            // Countdown
            HBox countdownBox = new HBox(10);
            countdownBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label lblCountdownIcon = new Label("⏳");
            lblCountdownIcon.setStyle("-fx-background-color: #fff3cd; -fx-padding: 5 8 5 8; -fx-background-radius: 50; -fx-text-fill: #856404;");
            Label lblCountdown = new Label("");
            lblCountdown.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1E1B4B;");
            countdownBox.getChildren().addAll(lblCountdownIcon, lblCountdown);
            
            java.time.LocalDateTime startDT = null;
            java.time.LocalDateTime endDT = null;
            try {
                if (!startTimeRaw.isEmpty()) startDT = java.time.LocalDateTime.parse(startTimeRaw);
                if (!endTimeRaw.isEmpty()) endDT = java.time.LocalDateTime.parse(endTimeRaw);
            } catch (Exception ignored) {}
            
            if (startDT != null && endDT != null) {
                final java.time.LocalDateTime fStart = startDT;
                final java.time.LocalDateTime fEnd = endDT;
                
                javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        if (status.equals("PENDING") && now.isBefore(fStart)) {
                            java.time.Duration d = java.time.Duration.between(now, fStart);
                            lblCountdown.setText(String.format("Bắt đầu sau: %d ngày %02d:%02d:%02d", d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
                            lblCountdown.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: transparent;");
                        } else if (status.equals("RUNNING") && now.isBefore(fEnd)) {
                            java.time.Duration d = java.time.Duration.between(now, fEnd);
                            lblCountdown.setText(String.format("Kết thúc sau: %d ngày %02d:%02d:%02d", d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
                            lblCountdown.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: transparent;");
                        } else {
                            lblCountdown.setText("Đã kết thúc");
                            lblCountdown.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: transparent;");
                        }
                    })
                );
                timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
                timeline.play();
                activeTimelines.add(timeline);
                // Trigger once immediately
                timeline.getOnFinished(); // No, just wait 1s or do it manually, we wait 1s is fine.
            }

            content.getChildren().addAll(lblStatus, lblTitle, priceBox, timeBox, sellerBox, countdownBox);
            card.getChildren().addAll(imageView, content);
            cardContainer.getChildren().add(card);
        }
    }

    /**
     * Cài đặt logic cho các nút hành động (Refresh).
     */
    private void setupActionHandlers() {
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> loadAuctionList());
        }
    }

    /**
     * Call API lấy danh sách phiên đấu giá kèm hiệu ứng UX (Loading Spinner & Empty State).
     */
    private void loadAuctionList() {
        // [Tiêu chí UX]: Disable nút refresh và hiện con xoay loading
        Runnable onComplete = (btnRefresh != null && loadingSpinner != null)
                ? UiUtils.showLoading(btnRefresh, loadingSpinner)
                : () -> { if (btnRefresh != null) btnRefresh.setDisable(false); };

        MessageRequest req = new MessageRequest();
        req.setType(CMD_GET_AUCTION_LIST);

        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();

            if (STATUS_OK.equals(response.getStatus()) && response.getPayload() != null) {
                JsonNode payload = mapper.valueToTree(response.getPayload());
                if (payload.isArray()) {
                    auctionData.clear();
                    for (JsonNode node : payload) {
                        auctionData.add(node);
                    }
                }
            } else {
                Platform.runLater(() -> UiUtils.showError("Lỗi hệ thống", response.getMessage()));
            }

            // [Tiêu chí UX]: Cập nhật UI hiển thị bảng data hoặc thông báo dữ liệu trống
            updateEmptyStateUI();
            populateCards();
            onComplete.run();
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> UiUtils.showError("Lỗi tải dữ liệu", "Không thể tải danh sách phiên đấu giá. Vui lòng thử lại."));
            onComplete.run();
        });

        new Thread(task, "fetch-auctions-thread").start();
    }

    /**
     * Bật/tắt hiển thị TableView và Label dựa trên việc list data có rỗng hay không.
     */
    private void updateEmptyStateUI() {
        boolean isEmpty = auctionData.isEmpty();

        if (lblEmptyMessage != null) {
            lblEmptyMessage.setVisible(isEmpty);
        }
        if (scrollPane != null) {
            scrollPane.setVisible(!isEmpty);
        }
    }
}