package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller cho trang Thông Báo.
 * 
 * <p>Cho phép người dùng xem thông báo hệ thống.
 * Admin có thể gửi thông báo đến toàn bộ người dùng.
 */
public class NotificationController {

    @FXML private VBox adminPanel;
    @FXML private TextField tfNotifTitle;
    @FXML private TextField tfNotifMessage;
    @FXML private Button btnSendNotif;
    @FXML private Label lblSendStatus;
    @FXML private VBox notifListContainer;
    @FXML private Label lblStatus;
    @FXML private Label lblUnreadCount;
    @FXML private Button btnMarkAllRead;
    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterUnread;
    @FXML private Button btnFilterSystem;

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<NotificationItem> allNotifications = new ArrayList<>();
    private String currentFilter = "ALL";

    private static final String BTN_ACTIVE =
            "-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-size: 12px;";
    private static final String BTN_INACTIVE =
            "-fx-background-color: #2B3139; -fx-text-fill: #B7BDC6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-size: 12px;";

    @FXML
    public void initialize() {
        // Hiện admin panel nếu là admin
        String role = String.valueOf(ClientSession.getInstance().getCurrentRole());
        if ("ADMIN".equals(role)) {
            adminPanel.setVisible(true);
            adminPanel.setManaged(true);
        }

        loadNotifications();
    }

    /** Tải danh sách thông báo từ server */
    private void loadNotifications() {
        lblStatus.setText("Đang tải thông báo...");

        MessageRequest req = new MessageRequest();
        req.setType("GET_NOTIFICATIONS");
        req.setToken(ClientSession.getInstance().getToken());

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                allNotifications.clear();
                if (resp.isOk()) {
                    JsonNode payload = mapper.valueToTree(resp.getPayload());
                    if (payload.isArray()) {
                        for (JsonNode n : payload) {
                            allNotifications.add(new NotificationItem(
                                    n.path("id").asText(""),
                                    n.path("title").asText("Thông báo"),
                                    n.path("message").asText(""),
                                    n.path("type").asText("SYSTEM"),
                                    n.path("createdAt").asText(""),
                                    n.path("isRead").asBoolean(false)
                            ));
                        }
                    }
                }
                // Nếu không có thông báo hoặc server chưa hỗ trợ, thêm thông báo demo
                if (allNotifications.isEmpty()) {
                    addDemoNotifications();
                }
                renderNotifications();
                updateUnreadCount();
                lblStatus.setText("Đã tải " + allNotifications.size() + " thông báo.");
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            // Khi server chưa hỗ trợ endpoint này, hiện thông báo demo
            addDemoNotifications();
            renderNotifications();
            updateUnreadCount();
            lblStatus.setText("Hiển thị thông báo mẫu (server chưa hỗ trợ endpoint).");
        }));

        new Thread(task).start();
    }

    /** Thêm thông báo mẫu khi server chưa hỗ trợ */
    private void addDemoNotifications() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String yesterday = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        allNotifications.add(new NotificationItem("1",
                "Chào mừng đến BidHub!",
                "Cảm ơn bạn đã đăng ký tài khoản. Hãy khám phá các phiên đấu giá đang diễn ra ngay hôm nay!",
                "SYSTEM", now, false));

        allNotifications.add(new NotificationItem("2",
                "Hướng dẫn đặt giá",
                "Để đặt giá, hãy vào trang chi tiết phiên đấu giá và nhập số tiền muốn trả. Hệ thống Anti-Sniping sẽ tự động gia hạn nếu có giá mới ở phút cuối.",
                "SYSTEM", yesterday, true));
    }

    /** Render danh sách thông báo theo filter hiện tại */
    private void renderNotifications() {
        notifListContainer.getChildren().clear();

        List<NotificationItem> filtered = allNotifications.stream()
                .filter(n -> switch (currentFilter) {
                    case "UNREAD" -> !n.isRead();
                    case "SYSTEM" -> "SYSTEM".equals(n.getType());
                    default -> true;
                })
                .toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("Không có thông báo nào.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 15px; -fx-padding: 40;");
            notifListContainer.getChildren().add(empty);
            return;
        }

        for (NotificationItem item : filtered) {
            notifListContainer.getChildren().add(createNotifCard(item));
        }
    }

    /** Tạo card hiển thị cho một thông báo */
    private HBox createNotifCard(NotificationItem item) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String bg = item.isRead() ? "#1A1F26" : "#1E2329";
        String borderLeft = item.isRead() ? "rgba(255,255,255,0.05)" : "#4F46E5";

        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 12; " +
                "-fx-border-color: %s transparent transparent transparent; " +
                "-fx-border-width: 0 0 0 4; -fx-border-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 6, 0, 0, 2);",
                bg, borderLeft));

        // Icon thông báo
        Label icon = new Label(item.isRead() ? "○" : "●");
        icon.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (item.isRead() ? "#475569" : "#4F46E5") + "; -fx-padding: 0 4 0 0;");

        // Nội dung thông báo
        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label(item.getTitle());
        title.setStyle("-fx-text-fill: " + (item.isRead() ? "#94A3B8" : "white") +
                "; -fx-font-size: 14px; -fx-font-weight: " + (item.isRead() ? "normal" : "bold") + ";");

        Label typeTag = new Label(translateType(item.getType()));
        typeTag.setStyle("-fx-background-color: " + getTypeColor(item.getType()) +
                "; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 8; -fx-background-radius: 10;");

        titleRow.getChildren().addAll(title, typeTag);

        Label message = new Label(item.getMessage());
        message.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        message.setWrapText(true);

        Label time = new Label(formatTime(item.getCreatedAt()));
        time.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        content.getChildren().addAll(titleRow, message, time);

        card.getChildren().addAll(icon, content);

        // Click để đánh dấu đã đọc
        card.setOnMouseClicked(e -> {
            item.setRead(true);
            renderNotifications();
            updateUnreadCount();
        });
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(bg, "#252D38")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("#252D38", bg)));

        return card;
    }

    private String translateType(String type) {
        return switch (type) {
            case "SYSTEM" -> "Hệ thống";
            case "BID" -> "Đấu giá";
            case "AUCTION" -> "Phiên";
            default -> type;
        };
    }

    private String getTypeColor(String type) {
        return switch (type) {
            case "SYSTEM" -> "#4F46E5";
            case "BID" -> "#10B981";
            case "AUCTION" -> "#F59E0B";
            default -> "#64748B";
        };
    }

    private String formatTime(String iso) {
        try {
            LocalDateTime dt = LocalDateTime.parse(iso);
            LocalDateTime now = LocalDateTime.now();
            long minutes = java.time.Duration.between(dt, now).toMinutes();
            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            long hours = minutes / 60;
            if (hours < 24) return hours + " giờ trước";
            long days = hours / 24;
            return days + " ngày trước";
        } catch (Exception e) {
            return iso;
        }
    }

    private void updateUnreadCount() {
        long unread = allNotifications.stream().filter(n -> !n.isRead()).count();
        if (unread > 0) {
            lblUnreadCount.setText(unread + " thông báo chưa đọc");
        } else {
            lblUnreadCount.setText("Không có thông báo mới");
        }
    }

    /** Admin gửi thông báo toàn server */
    @FXML
    public void handleSendNotification() {
        String title = tfNotifTitle.getText().trim();
        String message = tfNotifMessage.getText().trim();

        if (title.isBlank()) {
            lblSendStatus.setText("⚠ Vui lòng nhập tiêu đề thông báo.");
            lblSendStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #F59E0B;");
            return;
        }
        if (message.isBlank()) {
            lblSendStatus.setText("⚠ Vui lòng nhập nội dung thông báo.");
            lblSendStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #F59E0B;");
            return;
        }

        btnSendNotif.setDisable(true);
        lblSendStatus.setText("Đang gửi...");
        lblSendStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #B7BDC6;");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("title", title);
        payload.put("message", message);
        payload.put("type", "SYSTEM");

        MessageRequest req = new MessageRequest();
        req.setType("SEND_NOTIFICATION");
        req.setToken(ClientSession.getInstance().getToken());
        req.setPayload(payload);

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                btnSendNotif.setDisable(false);
                if (resp.isOk()) {
                    lblSendStatus.setText("✓ Đã gửi thông báo đến toàn bộ người dùng!");
                    lblSendStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #10B981;");
                    tfNotifTitle.clear();
                    tfNotifMessage.clear();
                    // Thêm vào danh sách local
                    allNotifications.add(0, new NotificationItem(
                            java.util.UUID.randomUUID().toString(), title, message, "SYSTEM",
                            LocalDateTime.now().toString(), false));
                    renderNotifications();
                    updateUnreadCount();
                } else {
                    lblSendStatus.setText("✗ Lỗi: " + resp.getMessage());
                    lblSendStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #F6465D;");
                }
            });
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            btnSendNotif.setDisable(false);
            // Thêm vào local kể cả khi server chưa hỗ trợ
            allNotifications.add(0, new NotificationItem(
                    java.util.UUID.randomUUID().toString(), title, message, "SYSTEM",
                    LocalDateTime.now().toString(), false));
            renderNotifications();
            updateUnreadCount();
            lblSendStatus.setText("✓ Đã thêm thông báo (server chưa hỗ trợ broadcast).");
            lblSendStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #F59E0B;");
            tfNotifTitle.clear();
            tfNotifMessage.clear();
        }));

        new Thread(task).start();
    }

    @FXML
    public void handleMarkAllRead() {
        allNotifications.forEach(n -> n.setRead(true));
        renderNotifications();
        updateUnreadCount();
    }

    @FXML
    public void handleFilterAll() {
        currentFilter = "ALL";
        updateFilterButtons();
        renderNotifications();
    }

    @FXML
    public void handleFilterUnread() {
        currentFilter = "UNREAD";
        updateFilterButtons();
        renderNotifications();
    }

    @FXML
    public void handleFilterSystem() {
        currentFilter = "SYSTEM";
        updateFilterButtons();
        renderNotifications();
    }

    @FXML
    public void handleRefresh() {
        allNotifications.clear();
        loadNotifications();
    }

    private void updateFilterButtons() {
        btnFilterAll.setStyle("ALL".equals(currentFilter) ? BTN_ACTIVE : BTN_INACTIVE);
        btnFilterUnread.setStyle("UNREAD".equals(currentFilter) ? BTN_ACTIVE : BTN_INACTIVE);
        btnFilterSystem.setStyle("SYSTEM".equals(currentFilter) ? BTN_ACTIVE : BTN_INACTIVE);
    }

    // ===================== Inner DTO =====================

    public static class NotificationItem {
        private final String id;
        private final String title;
        private final String message;
        private final String type;
        private final String createdAt;
        private boolean read;

        public NotificationItem(String id, String title, String message,
                                String type, String createdAt, boolean read) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
            this.createdAt = createdAt;
            this.read = read;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getType() { return type; }
        public String getCreatedAt() { return createdAt; }
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
    }
}
