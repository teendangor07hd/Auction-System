package com.bidhub.client.controller;

import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.util.UiUtils;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller cho trang "Sản Phẩm Của Tôi" (Seller Dashboard).
 * Hiển thị sản phẩm và phiên đấu giá của người bán hiện tại.
 * Hỗ trợ sửa/xóa sản phẩm và hủy phiên đấu giá.
 */
public class SellerDashboardController {

    @FXML private VBox itemListContainer;
    @FXML private VBox aucListContainer;
    @FXML private Label lblItemCount;
    @FXML private Label lblAucCount;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Button btnRefresh;

    // Edit overlay
    @FXML private StackPane editOverlay;
    @FXML private Label editTitle;
    @FXML private TextField editName;
    @FXML private TextArea editDescription;
    @FXML private TextField editPrice;
    @FXML private TextField editImageUrl;
    @FXML private Button btnEditSave;

    private final ObjectMapper mapper = new ObjectMapper();
    private String editingItemId = null; // ID sản phẩm đang chỉnh sửa

    @FXML private javafx.scene.control.Label lblEditImageName;

    @FXML
    public void initialize() {
        loadAll();
    }

    @FXML
    public void handleRefresh() { loadAll(); }

    @FXML
    public void handleCreateItem() { ViewRouter.getInstance().navigateTo(Views.CREATE_ITEM); }

    @FXML
    public void handleCreateAuction() { ViewRouter.getInstance().navigateTo(Views.CREATE_AUCTION); }

    @FXML
    public void handleEditCancel() { hideEditOverlay(); }

    @FXML
    public void handleEditUploadImage() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm");
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        java.io.File file = fc.showOpenDialog(
                editImageUrl != null ? editImageUrl.getScene().getWindow() : null);
        if (file != null) {
            if (editImageUrl != null) editImageUrl.setText(file.toURI().toString());
            if (lblEditImageName != null) lblEditImageName.setText("✔ " + file.getName());
        }
    }

    // =====================================================================
    // Load dữ liệu
    // =====================================================================

    private void loadAll() {
        setLoading(true);
        loadMyItems();
        loadMyAuctions();
    }

    private void loadMyItems() {
        MessageRequest req = new MessageRequest("LIST_MY_ITEMS",
                ClientSession.getInstance().getToken(), null);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                setLoading(false);
                itemListContainer.getChildren().clear();
                if (resp.isOk()) {
                    JsonNode payload = mapper.valueToTree(resp.getPayload());
                    List<JsonNode> items = new ArrayList<>();
                    if (payload.isArray()) for (JsonNode n : payload) items.add(n);
                    lblItemCount.setText("(" + items.size() + ")");
                    if (items.isEmpty()) {
                        itemListContainer.getChildren().add(makeEmptyLabel("Bạn chưa tạo sản phẩm nào."));
                    } else {
                        for (JsonNode item : items) itemListContainer.getChildren().add(createItemRow(item));
                    }
                } else {
                    itemListContainer.getChildren().add(makeEmptyLabel("Không thể tải dữ liệu: " + resp.getMessage()));
                }
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            setLoading(false);
            itemListContainer.getChildren().clear();
            itemListContainer.getChildren().add(makeEmptyLabel("Lỗi kết nối. Thử lại sau."));
        }));

        new Thread(task, "load-my-items").start();
    }

    private void loadMyAuctions() {
        MessageRequest req = new MessageRequest("GET_MY_AUCTIONS",
                ClientSession.getInstance().getToken(), null);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                aucListContainer.getChildren().clear();
                if (resp.isOk()) {
                    JsonNode payload = mapper.valueToTree(resp.getPayload());
                    List<JsonNode> aucs = new ArrayList<>();
                    if (payload.isArray()) for (JsonNode n : payload) aucs.add(n);
                    lblAucCount.setText("(" + aucs.size() + ")");
                    if (aucs.isEmpty()) {
                        aucListContainer.getChildren().add(makeEmptyLabel("Bạn chưa tạo phiên đấu giá nào."));
                    } else {
                        for (JsonNode auc : aucs) aucListContainer.getChildren().add(createAucRow(auc));
                    }
                } else {
                    // Fallback: nếu endpoint chưa có, tải GET_AUCTION_LIST rồi lọc
                    loadMyAuctionsFallback();
                }
            });
        });

        task.setOnFailed(e -> Platform.runLater(this::loadMyAuctionsFallback));
        new Thread(task, "load-my-auctions").start();
    }

    /** Fallback: lấy tất cả auction rồi lọc theo sellerName hiện tại */
    private void loadMyAuctionsFallback() {
        String myUsername = ClientSession.getInstance().getCurrentUsername();
        MessageRequest req = new MessageRequest("GET_AUCTION_LIST", null, null);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));
        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                aucListContainer.getChildren().clear();
                if (resp.isOk()) {
                    JsonNode payload = mapper.valueToTree(resp.getPayload());
                    List<JsonNode> mine = new ArrayList<>();
                    if (payload.isArray()) {
                        for (JsonNode n : payload) {
                            if (myUsername.equalsIgnoreCase(n.path("sellerName").asText(""))) mine.add(n);
                        }
                    }
                    lblAucCount.setText("(" + mine.size() + ")");
                    if (mine.isEmpty()) {
                        aucListContainer.getChildren().add(makeEmptyLabel("Bạn chưa có phiên đấu giá nào."));
                    } else {
                        for (JsonNode auc : mine) aucListContainer.getChildren().add(createAucRow(auc));
                    }
                }
            });
        });
        task.setOnFailed(e -> {});
        new Thread(task, "load-my-auc-fallback").start();
    }

    // =====================================================================
    // Card tạo sản phẩm
    // =====================================================================
    private HBox createItemRow(JsonNode item) {
        String id       = item.path("id").asText("");
        String name     = item.path("name").asText("Không rõ");
        String desc     = item.path("description").asText("");
        String type     = item.path("itemType").asText("");
        double price    = item.path("startingPrice").asDouble(0);
        String imageUrl = item.path("imageUrl").asText("");
        String aStatus  = item.path("auctionStatus").asText("AVAILABLE");

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setStyle("-fx-background-color: #1E2329; -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12;");

        // Thumbnail
        StackPane thumb = new StackPane();
        thumb.setPrefWidth(72); thumb.setPrefHeight(72);
        thumb.setMinWidth(72); thumb.setMinHeight(72);
        thumb.setStyle("-fx-background-color: #2B3139; -fx-background-radius: 10;");
        ImageView iv = new ImageView();
        iv.setFitWidth(72); iv.setFitHeight(72); iv.setPreserveRatio(true);
        loadImageSafely(iv, imageUrl);
        thumb.getChildren().add(iv);

        // Thông tin
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        lblName.setWrapText(true);

        HBox tags = new HBox(8);
        tags.setAlignment(Pos.CENTER_LEFT);
        Label typeTag = new Label(translateType(type));
        typeTag.setStyle("-fx-background-color: #2B3139; -fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10;");

        String statusText  = switch (aStatus) { case "AUCTIONING" -> "🔥 Đang đấu giá"; case "SOLD" -> "✅ Đã bán"; default -> "🆕 Chưa đấu giá"; };
        String statusColor = switch (aStatus) { case "AUCTIONING" -> "#EF4444"; case "SOLD" -> "#10B981"; default -> "#4F46E5"; };
        Label statusTag = new Label(statusText);
        statusTag.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-text-fill: " + statusColor + "; -fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-weight: bold;");
        tags.getChildren().addAll(typeTag, statusTag);

        Label lblPrice = new Label(price > 0 ? "Giá d.kiến: " + String.format("%,.0f đ", price) : "Chưa định giá");
        lblPrice.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 12px;");
        Label lblDesc = new Label(desc.length() > 60 ? desc.substring(0, 60) + "…" : desc);
        lblDesc.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        info.getChildren().addAll(lblName, tags, lblPrice, lblDesc);

        // Nút hành động
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER);
        Button btnEdit = new Button("✏ Sửa");
        btnEdit.setStyle("-fx-background-color: #2B3139; -fx-text-fill: #E2E8F0; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 16; -fx-border-color: #475569; -fx-border-radius: 8; -fx-font-size: 12px;");
        btnEdit.setOnAction(e -> showEditItemDialog(id, name, desc, price, imageUrl));

        Button btnDelete = new Button("🗑 Xóa");
        btnDelete.setStyle("-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #EF4444; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 16; -fx-border-color: rgba(239,68,68,0.3); -fx-border-radius: 8; -fx-font-size: 12px;");
        btnDelete.setOnAction(e -> confirmDeleteItem(id, name));

        // Không cho sửa/xóa nếu đang đấu giá
        if ("AUCTIONING".equals(aStatus)) {
            btnEdit.setDisable(true);
            btnDelete.setDisable(true);
            btnDelete.setStyle(btnDelete.getStyle() + " -fx-opacity: 0.4;");
        }

        actions.getChildren().addAll(btnEdit, btnDelete);
        row.getChildren().addAll(thumb, info, actions);
        return row;
    }

    // =====================================================================
    // Card phiên đấu giá
    // =====================================================================
    private HBox createAucRow(JsonNode auc) {
        String aucId     = auc.path("id").asText("");
        String itemName  = auc.path("itemName").asText("Sản phẩm");
        String status    = auc.path("status").asText("OPEN");
        double startPrice = auc.path("startingPrice").asDouble(0);
        double curBid    = auc.path("currentHighestBid").asDouble(0);
        String startTime = auc.path("startTime").asText("");
        String endTime   = auc.path("endTime").asText("");
        String imageUrl  = auc.path("imageUrl").asText("");

        // Format thời gian
        String startStr = formatDateTime(startTime);
        String endStr   = formatDateTime(endTime);

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setStyle("-fx-background-color: #1E2329; -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 12;");

        // Thumbnail
        StackPane thumb = new StackPane();
        thumb.setPrefWidth(72); thumb.setPrefHeight(72);
        thumb.setMinWidth(72); thumb.setMinHeight(72);
        thumb.setStyle("-fx-background-color: #2B3139; -fx-background-radius: 10;");
        ImageView iv = new ImageView();
        iv.setFitWidth(72); iv.setFitHeight(72); iv.setPreserveRatio(true);
        loadImageSafely(iv, imageUrl);
        thumb.getChildren().add(iv);

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblName = new Label(itemName);
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        String statusVN = switch (status) { 
            case "RUNNING" -> "🔥 Đang diễn ra"; 
            case "OPEN" -> "⏳ Sắp bắt đầu"; 
            case "FINISHED" -> "🏆 Đã kết thúc — chờ thanh toán"; 
            case "PAID" -> "✅ Đã thanh toán"; 
            case "CANCELED" -> "🚫 Đã hủy"; 
            default -> status; 
        };
        String statusColor = switch (status) { 
            case "RUNNING" -> "#EF4444"; 
            case "OPEN" -> "#F59E0B"; 
            case "FINISHED" -> "#3B82F6"; 
            case "PAID" -> "#10B981"; 
            case "CANCELED" -> "#64748B"; 
            default -> "#64748B"; 
        };
        Label lblStatus = new Label(statusVN);
        lblStatus.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label lblPrice = new Label(String.format("Giá khởi: %,.0f đ  |  Giá cao nhất: %,.0f đ", startPrice, Math.max(curBid, startPrice)));
        lblPrice.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 12px;");

        Label lblTime = new Label("📅 " + startStr + " → " + endStr);
        lblTime.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        info.getChildren().addAll(lblName, lblStatus, lblPrice, lblTime);

        // Hiển thị thông tin người thắng cuộc nếu có
        String winnerName = auc.path("winnerName").asText("");
        String winnerEmail = auc.path("winnerEmail").asText("");
        if (!winnerName.isEmpty()) {
            Label lblWinner = new Label("🏆 Người thắng: " + winnerName + " (" + winnerEmail + ")");
            lblWinner.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: bold;");
            info.getChildren().add(lblWinner);
        }

        // Nút hành động
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER);

        Button btnView = new Button("👁 Xem");
        btnView.setStyle("-fx-background-color: #2B3139; -fx-text-fill: #E2E8F0; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 16; -fx-border-color: #475569; -fx-border-radius: 8; -fx-font-size: 12px;");
        btnView.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL,
                java.util.Map.of("auctionId", aucId)));

        Button btnCancel = new Button("🚫 Hủy phiên");
        btnCancel.setStyle("-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #EF4444; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 16; -fx-border-color: rgba(239,68,68,0.3); -fx-border-radius: 8; -fx-font-size: 12px;");
        btnCancel.setOnAction(e -> confirmCancelAuction(aucId, itemName));

        // Nút xác nhận đã thanh toán (chỉ hiện khi FINISHED)
        Button btnMarkPaid = new Button("💰 Đã thanh toán");
        btnMarkPaid.setStyle("-fx-background-color: rgba(16,185,129,0.1); -fx-text-fill: #10B981; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 16; -fx-border-color: rgba(16,185,129,0.3); -fx-border-radius: 8; -fx-font-size: 12px;");
        btnMarkPaid.setOnAction(e -> confirmMarkPaid(aucId, itemName));

        // Nút hủy vì bidder không mua (chỉ hiện khi FINISHED)
        Button btnCancelWinner = new Button("❌ Bidder không mua");
        btnCancelWinner.setStyle("-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #EF4444; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 16; -fx-border-color: rgba(239,68,68,0.3); -fx-border-radius: 8; -fx-font-size: 12px;");
        btnCancelWinner.setOnAction(e -> confirmCancelWinner(aucId, itemName));

        // Logic hiển thị nút theo trạng thái
        if ("OPEN".equals(status)) {
            // OPEN: chỉ cho hủy phiên (chưa bắt đầu)
            actions.getChildren().addAll(btnView, btnCancel);
        } else if ("FINISHED".equals(status)) {
            // FINISHED: hiện nút thanh toán + hủy vì không mua, ẩn nút hủy phiên cũ
            actions.getChildren().addAll(btnView, btnMarkPaid, btnCancelWinner);
        } else {
            // RUNNING / PAID / CANCELED: chỉ xem
            btnCancel.setDisable(true);
            btnCancel.setStyle(btnCancel.getStyle() + " -fx-opacity: 0.4;");
            actions.getChildren().addAll(btnView, btnCancel);
        }
        row.getChildren().addAll(thumb, info, actions);
        return row;
    }

    // =====================================================================
    // Edit Dialog
    // =====================================================================
    private void showEditItemDialog(String itemId, String name, String desc, double price, String imageUrl) {
        editingItemId = itemId;
        editTitle.setText("✏ Chỉnh sửa sản phẩm");
        editName.setText(name);
        editDescription.setText(desc);
        editPrice.setText(price > 0 ? String.valueOf((long) price) : "");
        if (editImageUrl != null) editImageUrl.setText(imageUrl != null ? imageUrl : "");
        showEditOverlay();
    }

    @FXML
    public void handleEditSave() {
        if (editingItemId == null) { hideEditOverlay(); return; }

        String newName  = editName.getText().trim();
        String newDesc  = editDescription.getText().trim();
        String priceStr = editPrice.getText().trim();
        String newImageUrl = editImageUrl != null ? editImageUrl.getText().trim() : "";

        if (newName.isEmpty()) {
            UiUtils.showError("Lỗi", "Tên sản phẩm không được để trống.");
            return;
        }

        double newPrice = 0;
        if (!priceStr.isEmpty()) {
            try { newPrice = Double.parseDouble(priceStr.replace(",", "")); }
            catch (NumberFormatException e) {
                UiUtils.showError("Lỗi", "Giá dự kiến phải là số hợp lệ.");
                return;
            }
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("itemId", editingItemId);
        payload.put("name", newName);
        payload.put("description", newDesc);
        payload.put("startingPrice", newPrice);
        payload.put("imageUrl", newImageUrl);

        btnEditSave.setDisable(true);
        MessageRequest req = new MessageRequest("UPDATE_ITEM", ClientSession.getInstance().getToken(), payload);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                btnEditSave.setDisable(false);
                hideEditOverlay();
                if (resp.isOk()) {
                    UiUtils.showInfo("Thành công", "Đã cập nhật sản phẩm.");
                    loadMyItems();
                } else {
                    UiUtils.showError("Lỗi", resp.getMessage());
                }
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            btnEditSave.setDisable(false);
            UiUtils.showError("Lỗi kết nối", "Không kết nối được máy chủ.");
        }));

        new Thread(task, "update-item").start();
    }

    // =====================================================================
    // Xóa sản phẩm / Hủy phiên
    // =====================================================================
    private void confirmDeleteItem(String itemId, String name) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc muốn xóa sản phẩm \"" + name + "\" không?");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) deleteItem(itemId);
        });
    }

    private void deleteItem(String itemId) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("itemId", itemId);
        MessageRequest req = new MessageRequest("DELETE_ITEM", ClientSession.getInstance().getToken(), payload);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));
        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                if (resp.isOk()) { UiUtils.showInfo("Đã xóa", "Sản phẩm đã được xóa."); loadMyItems(); }
                else UiUtils.showError("Lỗi", resp.getMessage());
            });
        });
        task.setOnFailed(e -> Platform.runLater(() -> UiUtils.showError("Lỗi kết nối", "Không kết nối được máy chủ.")));
        new Thread(task, "delete-item").start();
    }

    private void confirmCancelAuction(String aucId, String itemName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận hủy phiên");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc muốn hủy phiên đấu giá \"" + itemName + "\" không?\nHành động này không thể hoàn tác.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) cancelAuction(aucId);
        });
    }

    private void cancelAuction(String aucId) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("auctionId", aucId);
        MessageRequest req = new MessageRequest("CANCEL_AUCTION", ClientSession.getInstance().getToken(), payload);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));
        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                if (resp.isOk()) { UiUtils.showInfo("Đã hủy", "Phiên đấu giá đã được hủy."); loadMyAuctions(); }
                else UiUtils.showError("Lỗi", resp.getMessage());
            });
        });
        task.setOnFailed(e -> Platform.runLater(() -> UiUtils.showError("Lỗi kết nối", "Không kết nối được máy chủ.")));
        new Thread(task, "cancel-auction").start();
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    private void showEditOverlay() {
        if (editOverlay != null) { editOverlay.setVisible(true); editOverlay.setManaged(true); }
    }

    private void hideEditOverlay() {
        if (editOverlay != null) { editOverlay.setVisible(false); editOverlay.setManaged(false); }
        editingItemId = null;
    }

    private void setLoading(boolean loading) {
        if (loadingSpinner != null) loadingSpinner.setVisible(loading);
        if (btnRefresh != null) btnRefresh.setDisable(loading);
    }

    private Label makeEmptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-padding: 20;");
        lbl.setWrapText(true);
        return lbl;
    }

    private String translateType(String type) {
        return switch (type) {
            case "ELECTRONICS" -> "Điện tử";
            case "ART" -> "Nghệ thuật";
            case "VEHICLE" -> "Phương tiện";
            default -> type;
        };
    }

    private String formatDateTime(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        try {
            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(raw);
            return dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) { return raw; }
    }

    private void loadImageSafely(ImageView iv, String url) {
        if (url != null && !url.isBlank()) {
            try {
                Image img = new Image(url, 72, 72, true, true, true);
                img.errorProperty().addListener((o, a, b) -> { if (b) Platform.runLater(() -> iv.setImage(null)); });
                iv.setImage(img);
            } catch (Exception ignored) {}
        }
    }

    private void confirmMarkPaid(String aucId, String itemName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thanh toán");
        alert.setHeaderText(null);
        alert.setContentText("Xác nhận người thắng đã thanh toán cho \"" + itemName + "\"?");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) markPaid(aucId);
        });
    }

    private void markPaid(String aucId) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("auctionId", aucId);
        MessageRequest req = new MessageRequest("MARK_PAID", ClientSession.getInstance().getToken(), payload);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));
        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                if (resp.isOk()) { UiUtils.showInfo("Thành công", "Đã xác nhận thanh toán."); loadMyAuctions(); }
                else UiUtils.showError("Lỗi", resp.getMessage());
            });
        });
        task.setOnFailed(e -> Platform.runLater(() -> UiUtils.showError("Lỗi kết nối", "Không kết nối được máy chủ.")));
        new Thread(task, "mark-paid").start();
    }

    private void confirmCancelWinner(String aucId, String itemName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Hủy phiên — Bidder không mua");
        alert.setHeaderText(null);
        alert.setContentText("Xác nhận hủy phiên \"" + itemName + "\" vì người thắng không thanh toán?\nSản phẩm sẽ có thể đưa lên đấu giá lại.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) cancelWinner(aucId);
        });
    }

    private void cancelWinner(String aucId) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("auctionId", aucId);
        MessageRequest req = new MessageRequest("SELLER_CANCEL_FINISHED", ClientSession.getInstance().getToken(), payload);
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> ServerGateway.getInstance().sendRequest(req));
        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                if (resp.isOk()) { UiUtils.showInfo("Đã hủy", "Phiên đã hủy. Sản phẩm có thể đấu giá lại."); loadMyAuctions(); }
                else UiUtils.showError("Lỗi", resp.getMessage());
            });
        });
        task.setOnFailed(e -> Platform.runLater(() -> UiUtils.showError("Lỗi kết nối", "Không kết nối được máy chủ.")));
        new Thread(task, "cancel-winner").start();
    }
}
