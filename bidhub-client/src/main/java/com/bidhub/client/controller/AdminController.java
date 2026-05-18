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
    @FXML private Button stopAuctionBtn;
    @FXML private Button deleteAuctionBtn;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionItemName;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionStatus;
    @FXML private TableColumn<AuctionReportInfo, Double> colAuctionStartPrice;
    @FXML private TableColumn<AuctionReportInfo, Double> colAuctionHighestBid;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionWinnerName;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionId;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionItem;
    @FXML private TableColumn<AuctionReportInfo, String> colAuctionWinner;

    @FXML private TableView<BidHistoryInfo> bidHistoryTable;
    @FXML private TableColumn<BidHistoryInfo, String> colBidItemName;
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

    @FXML private ComboBox<String> cbUserRoleFilter;
    @FXML private ComboBox<String> cbUserStatusFilter;
    @FXML private TextField tfUserSearch;

    @FXML private ComboBox<String> cbAuctionStatusFilter;
    @FXML private TextField tfAuctionSearch;

    @FXML private TextField tfBidSearch;

    @FXML private ComboBox<String> cbAuditActionFilter;
    @FXML private TextField tfAuditSearch;

    private final ObservableList<UserInfo> userData = FXCollections.observableArrayList();
    private final ObservableList<AuctionReportInfo> auctionData = FXCollections.observableArrayList();
    private final ObservableList<BidHistoryInfo> bidData = FXCollections.observableArrayList();
    private final ObservableList<AuditLogInfo> auditData = FXCollections.observableArrayList();

    private javafx.collections.transformation.FilteredList<UserInfo> filteredUserData;
    private javafx.collections.transformation.FilteredList<AuctionReportInfo> filteredAuctionData;
    private javafx.collections.transformation.FilteredList<BidHistoryInfo> filteredBidData;
    private javafx.collections.transformation.FilteredList<AuditLogInfo> filteredAuditData;

    private static final ObjectMapper mapper = new ObjectMapper();
    private boolean auctionReportLoaded = false;
    private boolean bidHistoryLoaded = false;
    private boolean auditLogLoaded = false;

    @Override
    public void setContext(java.util.Map<String, Object> params) {}

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilterControls();
        String currentRole = String.valueOf(ClientSession.getInstance().getCurrentRole());
        if (!ROLE_ADMIN.equals(currentRole)) {
            ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
            return;
        }
        loadUsers();
        setupTableSelectionListener();
    }

    private void setupFilterControls() {
        if (cbUserRoleFilter != null) {
            cbUserRoleFilter.setItems(FXCollections.observableArrayList("Tất cả Vai trò", "ADMIN", "SELLER", "BIDDER"));
            cbUserRoleFilter.getSelectionModel().selectFirst();
        }
        if (cbUserStatusFilter != null) {
            cbUserStatusFilter.setItems(FXCollections.observableArrayList("Tất cả Trạng thái", "Bình thường", "Đã khóa"));
            cbUserStatusFilter.getSelectionModel().selectFirst();
        }
        if (cbAuctionStatusFilter != null) {
            cbAuctionStatusFilter.setItems(FXCollections.observableArrayList("Tất cả Trạng thái", "RUNNING", "PENDING", "FINISHED", "CLOSED"));
            cbAuctionStatusFilter.getSelectionModel().selectFirst();
        }
        if (cbAuditActionFilter != null) {
            cbAuditActionFilter.setItems(FXCollections.observableArrayList("Tất cả Hành động", "PLACE_BID", "AUCTION_CREATED", "ITEM_CREATED", "USER_LOGIN", "USER_LOGOUT", "LOCK_USER", "UNLOCK_USER"));
            cbAuditActionFilter.getSelectionModel().selectFirst();
        }
    }

    private void setupTableColumns() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<UserInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Hoạt động".equalsIgnoreCase(item) || "ACTIVE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    } else if ("Đã khóa".equalsIgnoreCase(item) || "LOCKED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ef6c00; -fx-font-weight: bold;");
                    }
                }
            }
        });
        filteredUserData = new javafx.collections.transformation.FilteredList<>(userData, p -> true);
        javafx.collections.transformation.SortedList<UserInfo> sortedUserData = new javafx.collections.transformation.SortedList<>(filteredUserData);
        sortedUserData.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedUserData);

        colAuctionItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAuctionStatus.setCellFactory(column -> new TableCell<AuctionReportInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("RUNNING".equalsIgnoreCase(item) || "OPEN".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    } else if ("FINISHED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #1565c0; -fx-font-weight: bold;");
                    } else if ("CANCELED".equalsIgnoreCase(item) || "CLOSED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ef6c00; -fx-font-weight: bold;");
                    }
                }
            }
        });
        colAuctionStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colAuctionStartPrice.setCellFactory(column -> new TableCell<AuctionReportInfo, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VND", item));
                }
            }
        });

        colAuctionHighestBid.setCellValueFactory(new PropertyValueFactory<>("currentHighestBid"));
        colAuctionHighestBid.setCellFactory(column -> new TableCell<AuctionReportInfo, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VND", item));
                }
            }
        });

        colAuctionWinnerName.setCellValueFactory(new PropertyValueFactory<>("winnerName"));
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colAuctionItem.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        colAuctionWinner.setCellValueFactory(new PropertyValueFactory<>("highestBidderId"));
        filteredAuctionData = new javafx.collections.transformation.FilteredList<>(auctionData, p -> true);
        javafx.collections.transformation.SortedList<AuctionReportInfo> sortedAuctionData = new javafx.collections.transformation.SortedList<>(filteredAuctionData);
        sortedAuctionData.comparatorProperty().bind(auctionReportTable.comparatorProperty());
        auctionReportTable.setItems(sortedAuctionData);

        colBidItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colBidderName.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
        colBidAmount.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colBidAmount.setCellFactory(column -> new TableCell<BidHistoryInfo, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VND", item));
                }
            }
        });

        colBidTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colBidTime.setComparator((t1, t2) -> {
            try {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                return java.time.LocalDateTime.parse(t1, fmt).compareTo(java.time.LocalDateTime.parse(t2, fmt));
            } catch (Exception e) {
                return t1.compareTo(t2);
            }
        });

        colBidId.setCellValueFactory(new PropertyValueFactory<>("bidId"));
        colBidAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colBidderId.setCellValueFactory(new PropertyValueFactory<>("bidderId"));
        filteredBidData = new javafx.collections.transformation.FilteredList<>(bidData, p -> true);
        javafx.collections.transformation.SortedList<BidHistoryInfo> sortedBidData = new javafx.collections.transformation.SortedList<>(filteredBidData);
        sortedBidData.comparatorProperty().bind(bidHistoryTable.comparatorProperty());
        bidHistoryTable.setItems(sortedBidData);

        colAuditTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colAuditTime.setComparator((t1, t2) -> {
            try {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                return java.time.LocalDateTime.parse(t1, fmt).compareTo(java.time.LocalDateTime.parse(t2, fmt));
            } catch (Exception e) {
                return t1.compareTo(t2);
            }
        });

        colAuditUserName.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colAuditAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colAuditDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
        colAuditId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuditUser.setCellValueFactory(new PropertyValueFactory<>("userId"));
        filteredAuditData = new javafx.collections.transformation.FilteredList<>(auditData, p -> true);
        javafx.collections.transformation.SortedList<AuditLogInfo> sortedAuditData = new javafx.collections.transformation.SortedList<>(filteredAuditData);
        sortedAuditData.comparatorProperty().bind(auditLogTable.comparatorProperty());
        auditLogTable.setItems(sortedAuditData);
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

        auctionReportTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                if (stopAuctionBtn != null) stopAuctionBtn.setDisable(true);
                if (deleteAuctionBtn != null) deleteAuctionBtn.setDisable(true);
                return;
            }
            boolean isRunning = "RUNNING".equalsIgnoreCase(newValue.getStatus()) || "OPEN".equalsIgnoreCase(newValue.getStatus());
            if (stopAuctionBtn != null) stopAuctionBtn.setDisable(!isRunning);
            if (deleteAuctionBtn != null) deleteAuctionBtn.setDisable(false);
        });
    }

    @FXML
    public void stopAuction() {
        AuctionReportInfo selected = auctionReportTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ObjectNode payload = mapper.createObjectNode();
        payload.put("auctionId", selected.getAuctionId());
        statusLabel.setText("Đang dừng phiên đấu giá " + selected.getAuctionId() + "...");
        executeRequest("ADMIN_STOP_AUCTION", payload, data -> {
            Platform.runLater(() -> {
                statusLabel.setText("Đã dừng phiên đấu giá thành công.");
                loadAuctionReport();
            });
        });
    }

    @FXML
    public void deleteAuction() {
        AuctionReportInfo selected = auctionReportTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ObjectNode payload = mapper.createObjectNode();
        payload.put("auctionId", selected.getAuctionId());
        statusLabel.setText("Đang xóa phiên đấu giá " + selected.getAuctionId() + "...");
        executeRequest("ADMIN_DELETE_AUCTION", payload, data -> {
            Platform.runLater(() -> {
                statusLabel.setText("Đã xóa phiên đấu giá thành công.");
                loadAuctionReport();
            });
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
        userData.clear();
        userTable.setPlaceholder(new Label("Đang tải danh sách người dùng... ⏳"));
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
                if (userData.isEmpty()) {
                    userTable.setPlaceholder(new Label("Không có dữ liệu người dùng"));
                } else {
                    userTable.setPlaceholder(new Label("Không tìm thấy kết quả phù hợp"));
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
        executeRequest(isLock ? CMD_LOCK_USER : CMD_UNLOCK_USER, payload, res -> {
            Platform.runLater(() -> {
                statusLabel.setText(isLock ? "Đã khóa tài khoản." : "Đã mở khóa tài khoản.");
                loadUsers();
            });
        });
    }

    @FXML
    public void handleAuctionReportTab() {
        loadAuctionReport();
    }

    @FXML
    public void loadAuctionReport() {
        statusLabel.setText("Đang tải báo cáo Auction...");
        auctionData.clear();
        auctionReportTable.setPlaceholder(new Label("Đang tải báo cáo Auction... ⏳"));
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
                if (auctionData.isEmpty()) {
                    auctionReportTable.setPlaceholder(new Label("Không có dữ liệu báo cáo Auction"));
                } else {
                    auctionReportTable.setPlaceholder(new Label("Không tìm thấy kết quả phù hợp"));
                }
                auctionReportLoaded = true;
                statusLabel.setText("Đã tải " + auctionData.size() + " dòng báo cáo Auction.");
            });
        });
    }

    @FXML
    public void handleBidHistoryTab() {
        loadBidHistory();
    }

    @FXML
    public void loadBidHistory() {
        statusLabel.setText("Đang tải lịch sử Bid toàn hệ thống...");
        bidData.clear();
        bidHistoryTable.setPlaceholder(new Label("Đang tải lịch sử Bid... ⏳"));
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
                        info.setItemName(node.path("itemName").asText("N/A"));
                        info.setBidderId(node.path("bidderId").asText(""));
                        info.setBidderName(node.path("bidderName").asText("N/A"));
                        info.setBidAmount(node.path("bidAmount").asDouble(0));
                        info.setBidTime(node.path("bidTime").asText(""));
                        bidData.add(info);
                    }
                }
                if (bidData.isEmpty()) {
                    bidHistoryTable.setPlaceholder(new Label("Không có dữ liệu lịch sử Bid"));
                } else {
                    bidHistoryTable.setPlaceholder(new Label("Không tìm thấy kết quả phù hợp"));
                }
                bidHistoryLoaded = true;
                statusLabel.setText("Đã tải " + bidData.size() + " lượt bid.");
            });
        });
    }

    @FXML
    public void handleAuditLogTab() {
        loadAuditLog();
    }

    @FXML
    public void loadAuditLog() {
        statusLabel.setText("Đang tải nhật ký hệ thống...");
        auditData.clear();
        auditLogTable.setPlaceholder(new Label("Đang tải nhật ký hệ thống... ⏳"));
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
                if (auditData.isEmpty()) {
                    auditLogTable.setPlaceholder(new Label("Không có dữ liệu nhật ký hệ thống"));
                } else {
                    auditLogTable.setPlaceholder(new Label("Không tìm thấy kết quả phù hợp"));
                }
                auditLogLoaded = true;
                statusLabel.setText("Đã tải " + auditData.size() + " log hệ thống.");
            });
        });
    }

    @FXML
    public void handleRunIntegrityCheck() {
        statusLabel.setText("Đang kiểm tra toàn vẹn dữ liệu...");
        executeRequest(CMD_RUN_INTEGRITY_CHECK, null, data -> {
            Platform.runLater(() -> {
                statusLabel.setText("Kiểm tra toàn vẹn hoàn tất.");
                if (data != null) {
                    int totalErrors = data.path("totalErrors").asInt(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("=== BÁO CÁO KIỂM TRA TOÀN VẸN DỮ LIỆU ===\n");
                    sb.append("Chức năng này giúp Quản trị viên (Admin) rà soát toàn bộ hệ thống để phát hiện các bất đồng bộ dữ liệu do lỗi logic cũ hoặc dữ liệu mẫu (seed data) cố tình để lại nhằm mục đích kiểm thử.\n\n");
                    sb.append("Tổng số vấn đề phát hiện: ").append(totalErrors).append("\n\n");

                    if (data.has("bidConsistencyErrors") && data.path("bidConsistencyErrors").isArray()) {
                        sb.append("[1] Bất đồng bộ dữ liệu Bid & Auction (").append(data.path("bidConsistencyErrors").size()).append("):\n");
                        if (data.path("bidConsistencyErrors").isEmpty()) {
                            sb.append("  ✓ Không phát hiện bất thường.\n");
                        } else {
                            for (JsonNode err : data.path("bidConsistencyErrors")) {
                                sb.append("  ⚠ ").append(err.asText()).append("\n");
                            }
                        }
                        sb.append("\n");
                    }
                    if (data.has("auctionWinnerErrors") && data.path("auctionWinnerErrors").isArray()) {
                        sb.append("[2] Trạng thái người chiến thắng Auction (").append(data.path("auctionWinnerErrors").size()).append("):\n");
                        if (data.path("auctionWinnerErrors").isEmpty()) {
                            sb.append("  ✓ Không phát hiện bất thường.\n");
                        } else {
                            for (JsonNode err : data.path("auctionWinnerErrors")) {
                                sb.append("  ⚠ ").append(err.asText()).append("\n");
                            }
                        }
                        sb.append("\n");
                    }
                    if (data.has("orphanedItemErrors") && data.path("orphanedItemErrors").isArray()) {
                        sb.append("[3] Dữ liệu sản phẩm mồ côi (không có Seller hợp lệ) (").append(data.path("orphanedItemErrors").size()).append("):\n");
                        if (data.path("orphanedItemErrors").isEmpty()) {
                            sb.append("  ✓ Không phát hiện bất thường.\n");
                        } else {
                            for (JsonNode err : data.path("orphanedItemErrors")) {
                                sb.append("  ⚠ ").append(err.asText()).append("\n");
                            }
                        }
                        sb.append("\n");
                    }

                    String report = sb.toString();
                    Alert alert = new Alert(totalErrors > 0 ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
                    alert.setTitle("Báo cáo Toàn vẹn Dữ liệu");
                    alert.setHeaderText(totalErrors > 0 ? "Phát hiện " + totalErrors + " vấn đề bất thường!" : "Hệ thống hoạt động ổn định (0 lỗi)");
                    TextArea area = new TextArea(report);
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.setPrefSize(650, 400);
                    alert.getDialogPane().setContent(area);
                    alert.showAndWait();
                }
            });
        });
    }

    @FXML
    public void filterUsers() {
        if (filteredUserData == null) return;
        String roleFilter = cbUserRoleFilter != null ? cbUserRoleFilter.getValue() : "Tất cả Vai trò";
        String statusFilter = cbUserStatusFilter != null ? cbUserStatusFilter.getValue() : "Tất cả Trạng thái";
        String searchText = tfUserSearch != null ? tfUserSearch.getText().toLowerCase().trim() : "";

        filteredUserData.setPredicate(user -> {
            if (user == null) return false;
            if (!"Tất cả Vai trò".equals(roleFilter) && roleFilter != null && !roleFilter.equalsIgnoreCase(user.getRole())) {
                return false;
            }
            if (!"Tất cả Trạng thái".equals(statusFilter) && statusFilter != null && !statusFilter.equalsIgnoreCase(user.getStatus())) {
                return false;
            }
            if (!searchText.isEmpty()) {
                String uName = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
                String uEmail = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                return uName.contains(searchText) || uEmail.contains(searchText);
            }
            return true;
        });
        statusLabel.setText("Đã lọc " + filteredUserData.size() + " / " + userData.size() + " người dùng.");
    }

    @FXML
    public void filterAuctions() {
        if (filteredAuctionData == null) return;
        String statusFilter = cbAuctionStatusFilter != null ? cbAuctionStatusFilter.getValue() : "Tất cả Trạng thái";
        String searchText = tfAuctionSearch != null ? tfAuctionSearch.getText().toLowerCase().trim() : "";

        filteredAuctionData.setPredicate(auction -> {
            if (auction == null) return false;
            if (!"Tất cả Trạng thái".equals(statusFilter) && statusFilter != null && !statusFilter.equalsIgnoreCase(auction.getStatus())) {
                return false;
            }
            if (!searchText.isEmpty()) {
                String iName = auction.getItemName() != null ? auction.getItemName().toLowerCase() : "";
                String wName = auction.getWinnerName() != null ? auction.getWinnerName().toLowerCase() : "";
                return iName.contains(searchText) || wName.contains(searchText);
            }
            return true;
        });
        statusLabel.setText("Đã lọc " + filteredAuctionData.size() + " / " + auctionData.size() + " dòng báo cáo Auction.");
    }

    @FXML
    public void filterBids() {
        if (filteredBidData == null) return;
        String searchText = tfBidSearch != null ? tfBidSearch.getText().toLowerCase().trim() : "";

        filteredBidData.setPredicate(bid -> {
            if (bid == null) return false;
            if (!searchText.isEmpty()) {
                String bName = bid.getBidderName() != null ? bid.getBidderName().toLowerCase() : "";
                return bName.contains(searchText);
            }
            return true;
        });
        statusLabel.setText("Đã lọc " + filteredBidData.size() + " / " + bidData.size() + " lượt bid.");
    }

    @FXML
    public void filterAuditLogs() {
        if (filteredAuditData == null) return;
        String actionFilter = cbAuditActionFilter != null ? cbAuditActionFilter.getValue() : "Tất cả Hành động";
        String searchText = tfAuditSearch != null ? tfAuditSearch.getText().toLowerCase().trim() : "";

        filteredAuditData.setPredicate(log -> {
            if (log == null) return false;
            if (!"Tất cả Hành động".equals(actionFilter) && actionFilter != null && !actionFilter.equalsIgnoreCase(log.getAction())) {
                return false;
            }
            if (!searchText.isEmpty()) {
                String uName = log.getUserName() != null ? log.getUserName().toLowerCase() : "";
                String details = log.getDetails() != null ? log.getDetails().toLowerCase() : "";
                return uName.contains(searchText) || details.contains(searchText);
            }
            return true;
        });
        statusLabel.setText("Đã lọc " + filteredAuditData.size() + " / " + auditData.size() + " log hệ thống.");
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
        private String bidId, auctionId, itemName, bidderId, bidderName, bidTime;
        private double bidAmount;
        public String getBidId() { return bidId; } public void setBidId(String bidId) { this.bidId = bidId; }
        public String getAuctionId() { return auctionId; } public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
        public String getItemName() { return itemName; } public void setItemName(String itemName) { this.itemName = itemName; }
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