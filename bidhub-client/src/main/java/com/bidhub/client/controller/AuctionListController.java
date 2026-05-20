package com.bidhub.client.controller;

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
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionListController {

    // --- FXML ---
    @FXML private FlowPane cardContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private Button btnRefresh;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label lblEmptyMessage;

    // Bộ lọc
    @FXML private TextField tfAucSearch;
    @FXML private ComboBox<String> cmbAucType;
    @FXML private TextField tfAucSeller;
    @FXML private ToggleButton btnAucAll;
    @FXML private ToggleButton btnAucRunning;
    @FXML private ToggleButton btnAucPending;
    @FXML private ToggleButton btnAucClosed;
    @FXML private TextField tfAucPriceMin;
    @FXML private TextField tfAucPriceMax;
    @FXML private Label lblAucResultCount;

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<JsonNode> allAuctions = new ArrayList<>();
    private final List<Timeline> activeTimelines = new ArrayList<>();
    private String currentStatusFilter = "ALL";

    @FXML
    public void initialize() {
        if (cmbAucType != null) {
            cmbAucType.setItems(FXCollections.observableArrayList("Tất cả", "ELECTRONICS", "ART", "VEHICLE"));
            cmbAucType.getSelectionModel().selectFirst();
        }

        if (btnRefresh != null) btnRefresh.setOnAction(e -> loadAuctionList());

        // Live search
        if (tfAucSearch != null) tfAucSearch.textProperty().addListener((o, a, b) -> applyFilters());
        if (tfAucSeller != null) tfAucSeller.textProperty().addListener((o, a, b) -> applyFilters());

        loadAuctionList();
    }

    // === Bộ lọc trạng thái ===
    @FXML public void handleAucFilterAll()     { currentStatusFilter = "ALL";      updateStatusBtns(); applyFilters(); }
    @FXML public void handleAucFilterRunning() { currentStatusFilter = "RUNNING";  updateStatusBtns(); applyFilters(); }
    @FXML public void handleAucFilterPending() { currentStatusFilter = "OPEN";  updateStatusBtns(); applyFilters(); }
    @FXML public void handleAucFilterClosed()  { currentStatusFilter = "FINISHED"; updateStatusBtns(); applyFilters(); }
    @FXML public void handleAucApplyFilter()   { applyFilters(); }
    @FXML public void handleAucClearFilter() {
        if (tfAucSearch != null) tfAucSearch.clear();
        if (tfAucSeller != null) tfAucSeller.clear();
        if (tfAucPriceMin != null) tfAucPriceMin.clear();
        if (tfAucPriceMax != null) tfAucPriceMax.clear();
        if (cmbAucType != null) cmbAucType.getSelectionModel().selectFirst();
        currentStatusFilter = "ALL";
        updateStatusBtns();
        applyFilters();
    }

    private void updateStatusBtns() {
        String active   = "-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14; -fx-font-size: 12px;";
        String inactive = "-fx-background-color: #2B3139; -fx-text-fill: #B7BDC6; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 14; -fx-font-size: 12px;";
        if (btnAucAll     != null) btnAucAll.setStyle("ALL".equals(currentStatusFilter)     ? active : inactive);
        if (btnAucRunning != null) btnAucRunning.setStyle("RUNNING".equals(currentStatusFilter) ? active : inactive);
        if (btnAucPending != null) btnAucPending.setStyle("OPEN".equals(currentStatusFilter)  ? active : inactive);
        if (btnAucClosed  != null) btnAucClosed.setStyle("FINISHED".equals(currentStatusFilter)  ? active : inactive);
    }

    private void applyFilters() {
        String search = tfAucSearch != null ? tfAucSearch.getText().toLowerCase().trim() : "";
        String seller = tfAucSeller != null ? tfAucSeller.getText().toLowerCase().trim() : "";
        String type   = (cmbAucType != null && !"Tất cả".equals(cmbAucType.getValue())) ? cmbAucType.getValue() : null;

        double priceMin = 0, priceMax = Double.MAX_VALUE;
        try { if (tfAucPriceMin != null && !tfAucPriceMin.getText().isBlank()) priceMin = Double.parseDouble(tfAucPriceMin.getText().replace(",", "")); } catch (Exception ignored) {}
        try { if (tfAucPriceMax != null && !tfAucPriceMax.getText().isBlank()) priceMax = Double.parseDouble(tfAucPriceMax.getText().replace(",", "")); } catch (Exception ignored) {}

        final double fMin = priceMin, fMax = priceMax;

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode node : allAuctions) {
            String name   = node.path("itemName").asText("").toLowerCase();
            String sname  = node.path("sellerName").asText("").toLowerCase();
            if (!search.isEmpty() && !name.contains(search) && !sname.contains(search)) continue;
            if (!seller.isEmpty() && !sname.contains(seller)) continue;
            if (type != null && !type.equals(node.path("itemType").asText(""))) continue;

            double price = node.path("startingPrice").asDouble(0);
            if (price < fMin || price > fMax) continue;

            String status = node.path("status").asText("OPEN");
            // FINISHED và PAID đều là “đã kết thúc”
            boolean isFinished = "FINISHED".equals(status) || "PAID".equals(status) || "CANCELED".equals(status);
            if (!"ALL".equals(currentStatusFilter)) {
                if ("FINISHED".equals(currentStatusFilter)) {
                    if (!isFinished) continue;
                } else if (!currentStatusFilter.equals(status)) {
                    continue;
                }
            }

            filtered.add(node);
        }

        populateCards(filtered);
        if (lblAucResultCount != null) lblAucResultCount.setText("Tìm thấy " + filtered.size() + " phiên đấu giá");
    }

    private void loadAuctionList() {
        Runnable onComplete = (btnRefresh != null && loadingSpinner != null)
                ? UiUtils.showLoading(btnRefresh, loadingSpinner)
                : () -> { if (btnRefresh != null) btnRefresh.setDisable(false); };

        MessageRequest req = new MessageRequest();
        req.setType("GET_AUCTION_LIST");

        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            Platform.runLater(() -> {
                allAuctions.clear();
                if ("OK".equals(response.getStatus()) && response.getPayload() != null) {
                    JsonNode payload = mapper.valueToTree(response.getPayload());
                    if (payload.isArray()) {
                        for (JsonNode node : payload) allAuctions.add(node);
                    }
                }
                applyFilters();
                updateEmptyStateUI();
                onComplete.run();
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                UiUtils.showError("Lỗi tải dữ liệu", "Không thể tải danh sách phiên đấu giá.");
                onComplete.run();
            });
        });

        new Thread(task, "fetch-auctions-thread").start();
    }

    private void populateCards(List<JsonNode> auctions) {
        for (Timeline t : activeTimelines) t.stop();
        activeTimelines.clear();
        if (cardContainer == null) return;
        cardContainer.getChildren().clear();

        for (JsonNode node : auctions) {
            String auctionId   = node.path("id").asText("");
            String itemName    = node.has("itemName") ? node.get("itemName").asText("") : auctionId;
            String imageUrl    = node.has("imageUrl") ? node.get("imageUrl").asText("") : null;
            double price       = node.path("currentHighestBid").asDouble(0);
            if (price == 0) price = node.path("startingPrice").asDouble(0);
            String startTimeRaw = node.path("startTime").asText("");
            String endTimeRaw   = node.path("endTime").asText("");
            String startTimeStr = startTimeRaw;
            String sellerName   = node.path("sellerName").asText("Không xác định");
            String status       = node.path("status").asText("OPEN");

            String statusVN = switch (status) {
                case "RUNNING" -> "Đang diễn ra";
                case "OPEN"    -> "Chờ bắt đầu";
                case "FINISHED", "PAID", "CANCELED", "CLOSED" -> "Đã kết thúc";
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
            String styleNormal = "-fx-background-color: #1E2329; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 10, 0, 0, 4); -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12;";
            String styleHover  = "-fx-background-color: #252D38; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.3), 14, 0, 0, 6); -fx-cursor: hand; -fx-border-color: #4F46E5; -fx-border-radius: 12;";
            card.setStyle(styleNormal);
            card.setOnMouseEntered(e -> card.setStyle(styleHover));
            card.setOnMouseExited(e -> card.setStyle(styleNormal));
            card.setOnMouseClicked(e -> {
                Map<String, Object> params = new HashMap<>();
                params.put("auctionId", auctionId);
                ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL, params);
            });

            // Image
            ImageView imageView = new ImageView();
            imageView.setFitWidth(280);
            imageView.setFitHeight(175);
            imageView.setPreserveRatio(false);
            Rectangle clip = new Rectangle(280, 175);
            clip.setArcWidth(24); clip.setArcHeight(24);
            imageView.setClip(clip);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try { imageView.setImage(new Image(imageUrl, true)); }
                catch (Exception e) {}
            }

            VBox content = new VBox();
            content.setSpacing(10);
            content.setPadding(new Insets(14));

            Label lblTitle = new Label(itemName);
            lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: white;");
            lblTitle.setMaxWidth(252);
            lblTitle.setWrapText(true);

            Label lblStatus = new Label(statusVN);
            String statusStyle = switch (status) {
                case "RUNNING" -> "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
                case "OPEN" -> "-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #F59E0B; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
                default -> "-fx-background-color: rgba(100,116,139,0.15); -fx-text-fill: #94A3B8; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
            };
            lblStatus.setStyle(statusStyle);

            HBox priceBox = makeInfoRow("💰", "Giá hiện tại", String.format("%,.0f đ", price), "#F59E0B");
            HBox timeBox  = makeInfoRow("📅", "Thời gian bắt đầu", startTimeStr, "#E2E8F0");
            HBox sellBox  = makeInfoRow("👤", "Người bán", sellerName, "#E2E8F0");

            Label lblCountdown = new Label("");
            lblCountdown.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

            java.time.LocalDateTime startDT = null, endDT = null;
            try { if (!startTimeRaw.isEmpty()) startDT = java.time.LocalDateTime.parse(startTimeRaw); } catch (Exception ignored) {}
            try { if (!endTimeRaw.isEmpty()) endDT = java.time.LocalDateTime.parse(endTimeRaw); } catch (Exception ignored) {}

            if (startDT != null && endDT != null) {
                final java.time.LocalDateTime fStart = startDT, fEnd = endDT;
                final String fStatus = status;
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if ("OPEN".equals(fStatus) && now.isBefore(fStart)) {
                        java.time.Duration d = java.time.Duration.between(now, fStart);
                        lblCountdown.setText(String.format("⏳ Bắt đầu sau: %d ngày %02d:%02d:%02d", d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
                        lblCountdown.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else if ("RUNNING".equals(fStatus) && now.isBefore(fEnd)) {
                        java.time.Duration d = java.time.Duration.between(now, fEnd);
                        lblCountdown.setText(String.format("🔥 Kết thúc sau: %d ngày %02d:%02d:%02d", d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
                        lblCountdown.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        lblCountdown.setText("✅ Đã kết thúc");
                        lblCountdown.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
                    }
                }));
                timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
                timeline.play();
                activeTimelines.add(timeline);
            }

            content.getChildren().addAll(lblStatus, lblTitle, priceBox, timeBox, sellBox, lblCountdown);
            card.getChildren().addAll(imageView, content);
            cardContainer.getChildren().add(card);
        }
    }

    private HBox makeInfoRow(String icon, String title, String value, String valueColor) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 5 8; -fx-background-radius: 50;");
        VBox info = new VBox(2);
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px;");
        Label lValue = new Label(value);
        lValue.setStyle("-fx-font-size: 12px; -fx-text-fill: " + valueColor + "; -fx-font-weight: bold;");
        info.getChildren().addAll(lTitle, lValue);
        row.getChildren().addAll(ico, info);
        return row;
    }

    private void updateEmptyStateUI() {
        boolean isEmpty = allAuctions.isEmpty();
        if (lblEmptyMessage != null) lblEmptyMessage.setVisible(isEmpty);
        if (scrollPane != null) scrollPane.setVisible(!isEmpty);
    }
}