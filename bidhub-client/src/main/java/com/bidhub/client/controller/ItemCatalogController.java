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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller cho trang Kho Sản Phẩm.
 * Hiển thị tất cả sản phẩm (đã/chưa đấu giá) với bộ lọc đa chiều.
 */
public class ItemCatalogController {

    @FXML private FlowPane itemContainer;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Button btnRefresh;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField tfSeller;
    @FXML private TextField tfPriceMin;
    @FXML private TextField tfPriceMax;
    @FXML private Label lblResultCount;
    @FXML private ToggleButton btnFilterAll;
    @FXML private ToggleButton btnFilterAvailable;
    @FXML private ToggleButton btnFilterAuctioning;
    @FXML private ToggleButton btnFilterSold;

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<JsonNode> allItems = new ArrayList<>();
    private String currentStatusFilter = "ALL";

    @FXML
    public void initialize() {
        cmbType.setItems(FXCollections.observableArrayList(
                "Tất cả", "ELECTRONICS", "ART", "VEHICLE"));
        cmbType.getSelectionModel().selectFirst();

        // Live search
        tfSearch.textProperty().addListener((obs, o, n) -> applyFilters());
        tfSeller.textProperty().addListener((obs, o, n) -> applyFilters());

        loadItems();
    }

    private void loadItems() {
        setLoading(true);
        MessageRequest req = new MessageRequest("GET_ITEM_LIST", null, null);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                setLoading(false);
                allItems.clear();
                if (resp.isOk()) {
                    JsonNode payload = mapper.valueToTree(resp.getPayload());
                    if (payload.isArray()) {
                        for (JsonNode node : payload) allItems.add(node);
                    }
                }
                applyFilters();
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            setLoading(false);
            lblResultCount.setText("Không thể tải dữ liệu. Kiểm tra kết nối máy chủ.");
        }));

        new Thread(task, "load-items").start();
    }

    @FXML public void handleRefresh() { loadItems(); }

    @FXML public void handleFilterAll()        { currentStatusFilter = "ALL";        updateFilterBtns(); applyFilters(); }
    @FXML public void handleFilterAvailable()  { currentStatusFilter = "AVAILABLE";  updateFilterBtns(); applyFilters(); }
    @FXML public void handleFilterAuctioning() { currentStatusFilter = "AUCTIONING"; updateFilterBtns(); applyFilters(); }
    @FXML public void handleFilterSold()       { currentStatusFilter = "SOLD";       updateFilterBtns(); applyFilters(); }

    @FXML public void handleApplyFilter() { applyFilters(); }

    @FXML public void handleClearFilter() {
        tfSearch.clear();
        tfSeller.clear();
        tfPriceMin.clear();
        tfPriceMax.clear();
        cmbType.getSelectionModel().selectFirst();
        currentStatusFilter = "ALL";
        updateFilterBtns();
        applyFilters();
    }

    private void updateFilterBtns() {
        String activeStyle   = "-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 16; -fx-font-size: 12px;";
        String inactiveStyle = "-fx-background-color: #2B3139; -fx-text-fill: #B7BDC6; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 16; -fx-font-size: 12px;";
        btnFilterAll.setStyle("ALL".equals(currentStatusFilter)        ? activeStyle : inactiveStyle);
        btnFilterAvailable.setStyle("AVAILABLE".equals(currentStatusFilter)  ? activeStyle : inactiveStyle);
        btnFilterAuctioning.setStyle("AUCTIONING".equals(currentStatusFilter) ? activeStyle : inactiveStyle);
        btnFilterSold.setStyle("SOLD".equals(currentStatusFilter)       ? activeStyle : inactiveStyle);
    }

    private void applyFilters() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase().trim();
        String sellerFilter = tfSeller.getText() == null ? "" : tfSeller.getText().toLowerCase().trim();
        String typeFilter = cmbType.getValue();
        if ("Tất cả".equals(typeFilter)) typeFilter = null;

        double priceMin = 0, priceMax = Double.MAX_VALUE;
        try { if (!tfPriceMin.getText().isBlank()) priceMin = Double.parseDouble(tfPriceMin.getText().replace(",", "")); } catch (Exception ignored) {}
        try { if (!tfPriceMax.getText().isBlank()) priceMax = Double.parseDouble(tfPriceMax.getText().replace(",", "")); } catch (Exception ignored) {}

        final String fType = typeFilter;
        final double fPriceMin = priceMin, fPriceMax = priceMax;

        List<JsonNode> filtered = new ArrayList<>();
        for (JsonNode item : allItems) {
            // Tên/seller search
            String name = item.path("name").asText("").toLowerCase();
            String seller = item.path("sellerName").asText("").toLowerCase();
            if (!search.isEmpty() && !name.contains(search) && !seller.contains(search)) continue;
            if (!sellerFilter.isEmpty() && !seller.contains(sellerFilter)) continue;

            // Loại sản phẩm
            if (fType != null && !fType.equals(item.path("itemType").asText(""))) continue;

            // Giá
            double price = item.path("startingPrice").asDouble(0);
            if (price < fPriceMin || price > fPriceMax) continue;

            // Trạng thái (dựa trên auctionStatus field từ server)
            String aStatus = item.path("auctionStatus").asText("AVAILABLE");
            if (!"ALL".equals(currentStatusFilter) && !currentStatusFilter.equals(aStatus)) continue;

            filtered.add(item);
        }

        renderItems(filtered);
        lblResultCount.setText("Tìm thấy " + filtered.size() + " sản phẩm");
    }

    private void renderItems(List<JsonNode> items) {
        itemContainer.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("Không tìm thấy sản phẩm nào phù hợp.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 15px; -fx-padding: 40;");
            itemContainer.getChildren().add(empty);
            return;
        }
        for (JsonNode item : items) {
            itemContainer.getChildren().add(createItemCard(item));
        }
    }

    private VBox createItemCard(JsonNode item) {
        String itemId   = item.path("id").asText("");
        String name     = item.path("name").asText("Sản phẩm");
        String imageUrl = item.path("imageUrl").asText("");
        String seller   = item.path("sellerName").asText("Không rõ");
        String type     = item.path("itemType").asText("");
        double price    = item.path("startingPrice").asDouble(0);
        String aStatus  = item.path("auctionStatus").asText("AVAILABLE");
        String desc     = item.path("description").asText("");

        VBox card = new VBox(0);
        card.setPrefWidth(240);
        String styleNormal = "-fx-background-color: #1E2329; -fx-background-radius: 14; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 8, 0, 0, 3);";
        String styleHover  = "-fx-background-color: #252D38; -fx-background-radius: 14; -fx-cursor: hand; -fx-border-color: #4F46E5; -fx-border-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.3), 14, 0, 0, 5);";
        card.setStyle(styleNormal);
        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e -> card.setStyle(styleNormal));

        // Khi click → mở dialog chỉ tiết
        final String fName = name, fDesc = desc, fSeller = seller, fType = type, fAStatus = aStatus, fImageUrl = imageUrl;
        final double fPrice = price;
        card.setOnMouseClicked(e -> showItemDetail(itemId, fName, fDesc, fSeller, fType, fAStatus, fImageUrl, fPrice));

        // Ảnh
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(155); imgPane.setMinHeight(155); imgPane.setMaxHeight(155);
        imgPane.setStyle("-fx-background-color: #2B3139; -fx-background-radius: 14 14 0 0;");

        ImageView iv = new ImageView();
        iv.setFitWidth(240); iv.setFitHeight(155); iv.setPreserveRatio(false);
        loadImageSafely(iv, imageUrl, 240, 155);
        Rectangle clip = new Rectangle(240, 155); clip.setArcWidth(28); clip.setArcHeight(28);
        iv.setClip(clip);
        imgPane.getChildren().add(iv);

        // Badge trạng thái
        String badgeText  = switch (aStatus) {
            case "AUCTIONING" -> "🔥 Đang đấu giá";
            case "SOLD"       -> "✅ Đã bán";
            default           -> "🆕 Chưa đấu giá";
        };
        String badgeColor = switch (aStatus) {
            case "AUCTIONING" -> "rgba(239,68,68,0.9)";
            case "SOLD"       -> "rgba(16,185,129,0.9)";
            default           -> "rgba(79,70,229,0.9)";
        };
        Label badge = new Label(badgeText);
        badge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 20;");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(9, 0, 0, 9));
        imgPane.getChildren().add(badge);

        // Nhãn loại sản phẩm (top-right)
        Label typeTag = new Label(translateType(type));
        typeTag.setStyle("-fx-background-color: rgba(0,0,0,0.55); -fx-text-fill: #E2E8F0; -fx-font-size: 9px; -fx-padding: 3 8; -fx-background-radius: 20;");
        StackPane.setAlignment(typeTag, Pos.TOP_RIGHT);
        StackPane.setMargin(typeTag, new Insets(9, 9, 0, 0));
        imgPane.getChildren().add(typeTag);

        // Info phần dưới card
        VBox info = new VBox(5);
        info.setPadding(new Insets(12, 14, 14, 14));

        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        lblName.setWrapText(true);

        HBox priceRow = new HBox(6);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label lblPriceTag = new Label("Giá d.kiến:");
        lblPriceTag.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        Label lblPrice = new Label(price > 0 ? String.format("%,.0f đ", price) : "Chưa định giá");
        lblPrice.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: 900; -fx-font-size: 14px;");
        priceRow.getChildren().addAll(lblPriceTag, lblPrice);

        Label lblSeller = new Label("👤 " + seller);
        lblSeller.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");

        info.getChildren().addAll(lblName, priceRow, lblSeller);
        card.getChildren().addAll(imgPane, info);
        return card;
    }

    /** Dialog chỉ tiết sản phẩm khi click vào card */
    private void showItemDetail(String id, String fallbackName, String fallbackDesc, String fallbackSeller,
                                String fallbackType, String fallbackAStatus, String fallbackImageUrl, double fallbackPrice) {
        MessageRequest req = new MessageRequest("GET_ITEM_DETAIL", null, mapper.createObjectNode().put("itemId", id));
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                String name = fallbackName;
                String desc = fallbackDesc;
                String type = fallbackType;
                String imageUrl = fallbackImageUrl;
                double price = fallbackPrice;
                String seller = fallbackSeller;
                String aStatus = fallbackAStatus;

                if (resp != null && resp.isOk()) {
                    JsonNode item = mapper.valueToTree(resp.getPayload());
                    name = item.path("name").asText(fallbackName);
                    desc = item.path("description").asText(fallbackDesc);
                    type = item.path("itemType").asText(fallbackType);
                    imageUrl = item.path("imageUrl").asText(fallbackImageUrl);
                    price = item.path("startingPrice").asDouble(fallbackPrice);
                    if (item.has("sellerName")) seller = item.path("sellerName").asText(fallbackSeller);
                    if (item.has("auctionStatus")) aStatus = item.path("auctionStatus").asText(fallbackAStatus);
                }
                displayItemDetailDialog(name, desc, seller, type, aStatus, imageUrl, price);
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            displayItemDetailDialog(fallbackName, fallbackDesc, fallbackSeller, fallbackType, fallbackAStatus, fallbackImageUrl, fallbackPrice);
        }));

        new Thread(task, "get-item-detail").start();
    }

    private void displayItemDetailDialog(String name, String desc, String seller,
                                         String type, String aStatus, String imageUrl, double price) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Chi tiết sản phẩm");
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0B0E11;");
        root.setPrefWidth(600);

        // Ảnh lớn
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(260);
        imgPane.setStyle("-fx-background-color: #1E2329;");
        ImageView bigIv = new ImageView();
        bigIv.setFitWidth(600); bigIv.setFitHeight(260); bigIv.setPreserveRatio(true);
        loadImageSafely(bigIv, imageUrl, 600, 260);
        imgPane.getChildren().add(bigIv);

        // Chỉ tiết bên dưới
        VBox detail = new VBox(14);
        detail.setPadding(new Insets(22, 28, 28, 28));

        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: 900;");
        lblName.setWrapText(true);

        // Các thẻ thông tin hàng ngang
        HBox tagsRow = new HBox(10);
        tagsRow.setAlignment(Pos.CENTER_LEFT);

        String badgeText = switch (aStatus) {
            case "AUCTIONING" -> "🔥 Đang đấu giá";
            case "SOLD"       -> "✅ Đã bán";
            default           -> "🆕 Chưa đấu giá";
        };
        String badgeColor = switch (aStatus) {
            case "AUCTIONING" -> "#EF4444";
            case "SOLD"       -> "#10B981";
            default           -> "#4F46E5";
        };
        Label statusTag = new Label(badgeText);
        statusTag.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 20;");
        Label typeTag = new Label("📂 " + translateType(type));
        typeTag.setStyle("-fx-background-color: #2B3139; -fx-text-fill: #E2E8F0; -fx-font-size: 12px; -fx-padding: 4 12; -fx-background-radius: 20;");
        tagsRow.getChildren().addAll(statusTag, typeTag);

        // Grid thông tin
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(10);
        grid.setStyle("-fx-background-color: #1E2329; -fx-padding: 16; -fx-background-radius: 12;");

        addGridRow(grid, 0, "💰 Giá dự kiến:", price > 0 ? String.format("%,.0f đ", price) : "Chưa định giá", "#F59E0B");
        addGridRow(grid, 1, "👤 Người bán:", seller, "#94A3B8");
        addGridRow(grid, 2, "📦 Loại hàng:", translateType(type), "#94A3B8");

        // Mô tả
        VBox descBox = new VBox(8);
        Label lblDescTitle = new Label("📝 Mô tả sản phẩm");
        lblDescTitle.setStyle("-fx-text-fill: #B7BDC6; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label lblDesc = new Label(desc.isBlank() ? "Chưa có mô tả." : desc);
        lblDesc.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-line-spacing: 4;");
        lblDesc.setWrapText(true);
        descBox.getChildren().addAll(lblDescTitle, lblDesc);

        Button btnClose = new Button("Đóng");
        btnClose.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        btnClose.setOnAction(e -> dialog.close());
        HBox btnRow = new HBox(btnClose);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        detail.getChildren().addAll(lblName, tagsRow, grid, descBox, btnRow);
        root.getChildren().addAll(imgPane, detail);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #0B0E11; -fx-background: #0B0E11;");

        dialog.setScene(new Scene(sp, 600, 620));
        dialog.show();
    }

    private void addGridRow(GridPane grid, int row, String label, String value, String valueColor) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + valueColor + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private String translateType(String type) {
        return switch (type) {
            case "ELECTRONICS" -> "Điện tử";
            case "ART"         -> "Nghệ thuật";
            case "VEHICLE"     -> "Phương tiện";
            default            -> type;
        };
    }

    private void loadImageSafely(ImageView iv, String url, double w, double h) {
        if (url != null && !url.isBlank()) {
            try {
                Image img = new Image(url, w, h, false, true, true);
                img.errorProperty().addListener((obs, old, err) -> {
                    if (err) Platform.runLater(() -> iv.setImage(null));
                });
                iv.setImage(img);
                return;
            } catch (Exception ignored) {}
        }
    }

    private void setLoading(boolean loading) {
        if (loadingSpinner != null) loadingSpinner.setVisible(loading);
        if (btnRefresh != null) btnRefresh.setDisable(loading);
    }
}
