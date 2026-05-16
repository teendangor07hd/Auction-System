package com.bidhub.client.controller;

import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.network.ClientSession;
import com.bidhub.client.util.Views;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AdminController implements com.bidhub.client.navigation.ContextAware {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String CMD_GET_USER_LIST = "GET_USER_LIST";
    private static final String CMD_LOCK_USER = "LOCK_USER";
    private static final String CMD_UNLOCK_USER = "UNLOCK_USER";
    
    private static final String CMD_GET_AUCTION_REPORT = "GET_AUCTION_REPORT";
    private static final String CMD_GET_BID_HISTORY_REPORT = "GET_BID_HISTORY_REPORT";
    private static final String CMD_GET_AUDIT_LOG = "GET_AUDIT_LOG";
    private static final String CMD_RUN_INTEGRITY_CHECK = "RUN_INTEGRITY_CHECK";

    @FXML private TabPane adminTabPane;

    @FXML private TableView<UserInfo> userTable;
    @FXML private TableColumn<UserInfo, String> colUsername;
    @FXML private TableColumn<UserInfo, String> colEmail;
    @FXML private TableColumn<UserInfo, String> colRole;
    @FXML private TableColumn<UserInfo, String> colStatus;
    @FXML private Button lockBtn;
    @FXML private Button unlockBtn;

    @FXML private TableView<AuctionReportInfo> auctionReportTable;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionItemName;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionStatus;
    @FXML private TableColumn<AuctionReportInfo, Double> colAuctionStartPrice;
    @FXML private TableColumn<AuctionReportInfo, Double> colAuctionHighestBid;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionWinnerName;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionId;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionItem;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionWinner;

    @FXML private TableView<BidHistoryInfo> bidHistoryTable;
    @FXML private TableColumn<BidHistoryInfo, String> colBidderName;
    @FXML private TableColumn<BidHistoryInfo, Double> colBidAmount;
    @FXML private TableColumn<BidHistoryInfo, String> colBidTime;
    @FXML private TableColumn<BidHistoryInfo, String> colBidId;
    @FXML private TableColumn<BidHistoryInfo, String> colBidAuctionId;
    @FXML private TableColumn<BidHistoryInfo, String> colBidderId;

    @FXML private TableView<AuditLogInfo> auditLogTable;
    @FXML private TableColumn<AuditLogInfo, String> colAuditTime;
    @FXML private TableColumn<AuditLogInfo, String> colAuditUserName;
    @FXML private TableColumn<AuditLogInfo, String> colAuditAction;
    @FXML private TableColumn<AuditLogInfo, String> colAuditDetails;
    @FXML private TableColumn<AuditLogInfo, String> colAuditId;
    @FXML private TableColumn<AuditLogInfo, String> colAuditUser;
    @FXML private Label statusLabel;

    private final ObservableList<UserInfo> userData = FXCollections.observableArrayList();
    private final ObservableList<AuctionReportInfo> auctionData = FXCollections.observableArrayList();
    private final ObservableList<BidHistoryInfo> bidData = FXCollections.observableArrayList();
    private final ObservableList<AuditLogInfo> auditData = FXCollections.observableArrayList();

    private static final ObjectMapper mapper = new ObjectMapper();
    private boolean auctionReportLoaded = false;
    private boolean bidHistoryLoaded = false;
    private boolean auditLogLoaded = false;

    @Override
    public void setContext(java.util.Map<String, Object> params) {}

    @FXML
    public void initialize() {
        setupTableColumns();
        String currentRole = String.valueOf(ClientSession.getInstance().getCurrentRole());
        if (!ROLE_ADMIN.equals(currentRole)) {
            ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
            return;
        }
        loadUsers();
        setupTableSelectionListener();
    }

    private void setupTableColumns() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        userTable.setItems(userData);

        colAuctionItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAuctionStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colAuctionHighestBid.setCellValueFactory(new PropertyValueFactory<>("currentHighestBid"));
        colAuctionWinnerName.setCellValueFactory(new PropertyValueFactory<>("winnerName"));
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colAuctionItem.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        colAuctionWinner.setCellValueFactory(new PropertyValueFactory<>("highestBidderId"));
        auctionReportTable.setItems(auctionData);

        colBidderName.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
        colBidAmount.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colBidTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colBidId.setCellValueFactory(new PropertyValueFactory<>("bidId"));
        colBidAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colBidderId.setCellValueFactory(new PropertyValueFactory<>("bidderId"));
        bidHistoryTable.setItems(bidData);

        colAuditTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colAuditUserName.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colAuditAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAuditDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colAuditId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuditUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        auditLogTable.setItems(auditData);
    }

    private void setupTableSelectionListener() {
        userTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                lockBtn.setDisable(true);
                unlockBtn.setDisable(true);
                return;
            }
            boolean isAdmin = ROLE_ADMIN.equals(newValue.getRole());
            boolean isLocked = "Đã khóa".equals(newValue.getStatus());
            lockBtn.setDisable(isAdmin || isLocked);
            unlockBtn.setDisable(!isLocked);
        });
    }

    private void executeRequest(String cmd, ObjectNode payload, java.util.function.Consumer<JsonNode> onSuccess) {
        MessageRequest req = new MessageRequest();
        req.setType(cmd);
        req.setToken(ClientSession.getInstance().getToken());
        if (payload != null) req.setPayload(payload);

        NetworkTask<String> task = new NetworkTask<>(() -> {
            MessageResponse resp = ServerGateway.getInstance().sendRequest(req);
            return com.bidhub.common.network.MessageMapper.toJson(resp);
        });

        task.setOnSucceeded(event -> {
            try {
                JsonNode root = mapper.readTree(task.getValue());
                String status = root.path("status").asText("");
                if ("OK".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                    JsonNode payloadNode = root.has("payload") ? root.get("payload") : root.get("data");
                    if (payloadNode == null && root.has("data")) payloadNode = root.get("data");
                    onSuccess.accept(payloadNode);
                } else {
                    Platform.runLater(() -> statusLabel.setText("Lỗi: " + root.path("message").asText("Thao tác thất bại")));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Lỗi parse dữ liệu: " + ex.getMessage()));
            }
        });

        task.setOnFailed(event -> {
            Platform.runLater(() -> statusLabel.setText("Lỗi mạng khi gọi " + cmd));
        });

        new Thread(task).start();
    }

    @FXML
    public void handleRefresh() {
        loadUsers();
    }
    
    private void loadUsers() {
        statusLabel.setText("Đang tải danh sách người dùng...");
        executeRequest(CMD_GET_USER_LIST, null, payload -> {
            Platform.runLater(() -> {
                userData.clear();
                if (payload != null && payload.isArray()) {
                    for (JsonNode node : payload) {
                        UserInfo info = new UserInfo();
                        info.setUsername(node.path("username").asText("N/A"));
                        info.setEmail(node.path("email").asText("N/A"));
                        info.setRole(node.path("role").asText("UNKNOWN"));
                        info.setStatus(node.path("isLocked").asBoolean() ? "Đã khóa" : "Bình thường");
                        info.setUserId(node.path("id").asText(""));
                        userData.add(info);
                    }
                }
                statusLabel.setText("Đã tải " + userData.size() + " người dùng.");
            });
        });
    }

    @FXML
    public void handleLockUser() {
        UserInfo selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;
        sendLockRequest(selectedUser.getUserId(), true);
    }

    @FXML
    public void handleUnlockUser() {
        UserInfo selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;
        sendLockRequest(selectedUser.getUserId(), false);
    }

    private void sendLockRequest(String userId, boolean isLock) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("targetUserId", userId);
        payload.put("userId", userId);
        executeRequest(isLock ? CMD_LOCK_USER : CMD_UNLOCK_USER, payload, res -> {
            Platform.runLater(() -> {
                statusLabel.setText(isLock ? "Đã khóa tài khoản." : "Đã mở khóa tài khoản.");
                loadUsers();
            });
        });
    }

    @FXML
    public void handleAuctionReportTab() {
        if (!auctionReportLoaded) loadAuctionReport();
    }

    @FXML
    public void loadAuctionReport() {
        statusLabel.setText("Đang tải báo cáo Auction...");
        executeRequest(CMD_GET_AUCTION_REPORT, null, payload -> {
            Platform.runLater(() -> {
                auctionData.clear();
                if (payload.isArray()) {
                    for (JsonNode node : payload) {
                        AuctionReportInfo info = new AuctionReportInfo();
                        info.setAuctionId(node.path("auctionId").asText(""));
                        info.setItemId(node.path("itemId").asText(""));
                        info.setItemName(node.path("itemName").asText("N/A"));
                        info.setStatus(node.path("status").asText(""));
                        info.setStartingPrice(node.path("startingPrice").asDouble(0));
                        info.setCurrentHighestBid(node.path("currentHighestBid").asDouble(0));
                        info.setHighestBidderId(node.path("highestBidderId").asText(""));
                        info.setWinnerName(node.path("winnerName").asText("N/A"));
                        auctionData.add(info);
                    }
                }
                auctionReportLoaded = true;
                statusLabel.setText("Đã tải " + auctionData.size() + " dòng báo cáo Auction.");
            });
        });
    }

    @FXML
    public void handleBidHistoryTab() {
        if (!bidHistoryLoaded) loadBidHistory();
    }

    @FXML
    public void loadBidHistory() {
        statusLabel.setText("Đang tải lịch sử Bid toàn hệ thống...");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("auctionId", "ALL");
        executeRequest(CMD_GET_BID_HISTORY_REPORT, payload, data -> {
            Platform.runLater(() -> {
                bidData.clear();
                if (data.isArray()) {
                    for (JsonNode node : data) {
                        BidHistoryInfo info = new BidHistoryInfo();
                        info.setBidId(node.path("bidId").asText(""));
                        info.setAuctionId(node.path("auctionId").asText(""));
                        info.setBidderId(node.path("bidderId").asText(""));
                        info.setBidderName(node.path("bidderName").asText("N/A"));
                        info.setBidAmount(node.path("bidAmount").asDouble(0));
                        info.setBidTime(node.path("bidTime").asText(""));
                        bidData.add(info);
                    }
                }
                bidHistoryLoaded = true;
                statusLabel.setText("Đã tải " + bidData.size() + " lượt bid.");
            });
        });
    }

    @FXML
    public void handleAuditLogTab() {
        if (!auditLogLoaded) loadAuditLog();
    }

    @FXML
    public void loadAuditLog() {
        statusLabel.setText("Đang tải nhật ký hệ thống...");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("limit", 200);
        executeRequest(CMD_GET_AUDIT_LOG, payload, data -> {
            Platform.runLater(() -> {
                auditData.clear();
                if (data.isArray()) {
                    for (JsonNode node : data) {
                        AuditLogInfo info = new AuditLogInfo();
                        info.setId(node.path("id").asText(""));
                        info.setCreatedAt(node.path("createdAt").asText(""));
                        info.setUserId(node.path("userId").asText(""));
                        info.setUserName(node.path("userName").asText("SYSTEM"));
                        info.setAction(node.path("action").asText(""));
                        info.setDetails(node.path("details").asText(""));
                        auditData.add(info);
                    }
                }
                auditLogLoaded = true;
                statusLabel.setText("Đã tải " + auditData.size() + " log hệ thống.");
            });
        });
    }

    @FXML
    public void handleBack() {
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
    }

    // DTO Classes
    public static class UserInfo {
        private String userId, username, email, role, status;
        public String getUserId() { return userId; } public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; } public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    }

    public static class AuctionReportInfo {
        private String auctionId, itemId, itemName, status, highestBidderId, winnerName;
        private double startingPrice, currentHighestBid;
        public String getAuctionId() { return auctionId; } public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
        public String getItemId() { return itemId; } public void setItemId(String itemId) { this.itemId = itemId; }
        public String getItemName() { return itemName; } public void setItemName(String itemName) { this.itemName = itemName; }
        public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
        public String getHighestBidderId() { return highestBidderId; } public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }
        public String getWinnerName() { return winnerName; } public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
        public double getStartingPrice() { return startingPrice; } public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
        public double getCurrentHighestBid() { return currentHighestBid; } public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }
    }

    public static class BidHistoryInfo {
        private String bidId, auctionId, bidderId, bidderName, bidTime;
        private double bidAmount;
        public String getBidId() { return bidId; } public void setBidId(String bidId) { this.bidId = bidId; }
        public String getAuctionId() { return auctionId; } public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
        public String getBidderId() { return bidderId; } public void setBidderId(String bidderId) { this.bidderId = bidderId; }
        public String getBidderName() { return bidderName; } public void setBidderName(String bidderName) { this.bidderName = bidderName; }
        public String getBidTime() { return bidTime; } public void setBidTime(String bidTime) { this.bidTime = bidTime; }
        public double getBidAmount() { return bidAmount; } public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }
    }

    public static class AuditLogInfo {
        private String id, createdAt, userId, userName, action, details;
        public String getId() { return id; } public void setId(String id) { this.id = id; }
        public String getCreatedAt() { return createdAt; } public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUserId() { return userId; } public void setUserId(String userId) { this.userId = userId; }
        public String getUserName() { return userName; } public void setUserName(String userName) { this.userName = userName; }
        public String getAction() { return action; } public void setAction(String action) { this.action = action; }
        public String getDetails() { return details; } public void setDetails(String details) { this.details = details; }
    }
}