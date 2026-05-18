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

/**
 * Controller xử lý logic cho màn hình Quản trị viên (Admin Panel).
 *
 * <p>Trách nhiệm (Single Responsibility):
 * - Hiển thị danh sách người dùng (TableView).
 * - Cung cấp tính năng Khóa/Mở khóa tài khoản.
 * - Giao tiếp với Server thông qua Socket layer.
 *
 * <p>// 📌 [B37] Thêm null check trước khi gọi isArray() trên payload.
 * // 📌 [B38] getCurrentRole() giờ trả "" thay vì null (B4 fix) → không dùng String.valueOf() nữa.
 * // 📌 [B42/GAP1] Thêm nút RUN_INTEGRITY_CHECK + handler.
 */
public class AdminController implements com.bidhub.client.navigation.ContextAware {

    // ========================================================================
    // CONSTANTS
    // ========================================================================
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String CMD_GET_USER_LIST = "GET_USER_LIST";
    private static final String CMD_LOCK_USER = "LOCK_USER";
    private static final String CMD_UNLOCK_USER = "UNLOCK_USER";
    // [B42/GAP1] Command này trước đây khai báo nhưng không bao giờ gọi — giờ được sử dụng
    private static final String CMD_RUN_INTEGRITY_CHECK = "RUN_INTEGRITY_CHECK";

    // ========================================================================
    // FXML INJECTIONS
    // ========================================================================
    @FXML private TableView<UserInfo> userTable;
    @FXML private TableColumn<UserInfo, String> colUsername;
    @FXML private TableColumn<UserInfo, String> colEmail;
    @FXML private TableColumn<UserInfo, String> colRole;
    @FXML private TableColumn<UserInfo, String> colStatus;
    @FXML private Button lockBtn;
    @FXML private Button unlockBtn;
    @FXML private Label statusLabel;

    // ========================================================================
    // INTERNAL STATE
    // ========================================================================
    private final ObservableList<UserInfo> userData = FXCollections.observableArrayList();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setContext(java.util.Map<String, Object> params) {
        // Màn hình này không yêu cầu tham số ngữ cảnh khi điều hướng tới
    }

    /**
     * Hàm lifecycle của JavaFX, được gọi tự động sau khi load FXML.
     */
    @FXML
    public void initialize() {
        setupTableColumns();

        // [B38] Security Check: getCurrentRole() trả "" (không null) nhờ B4 fix
        String currentRole = ClientSession.getInstance().getCurrentRole();
        if (!ROLE_ADMIN.equals(currentRole)) {
            ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
            return;
        }

        loadUsers();
        setupTableSelectionListener();
    }

    /**
     * Cấu hình binding dữ liệu cho các cột trong TableView.
     */
    private void setupTableColumns() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        userTable.setItems(userData);
    }

    /**
     * Lắng nghe sự kiện khi người dùng chọn một dòng trên bảng.
     */
    private void setupTableSelectionListener() {
        userTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                lockBtn.setDisable(true);
                unlockBtn.setDisable(true);
                return;
            }
            // Business logic: Không cho phép Admin khóa tài khoản của Admin khác.
            boolean isAdmin = ROLE_ADMIN.equals(newValue.getRole());
            lockBtn.setDisable(isAdmin);
            unlockBtn.setDisable(false);
        });
    }

    /**
     * Gửi request lấy danh sách User từ Server (Background Thread).
     */
    private void loadUsers() {
        MessageRequest req = new MessageRequest();
        req.setType(CMD_GET_USER_LIST);
        req.setToken(ClientSession.getInstance().getToken());

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(event -> {
            // [B40] Làm việc trực tiếp với MessageResponse thay vì serialize rồi re-parse
            MessageResponse resp = task.getValue();
            parseAndPopulateUserData(resp);
        });

        task.setOnFailed(event ->
                Platform.runLater(() -> statusLabel.setText("Không thể kết nối đến máy chủ để tải danh sách.")));

        new Thread(task).start();
    }

    /**
     * Phân tích response và đổ dữ liệu vào TableView.
     *
     * <p>// 📌 [B37] Kiểm tra null trên payload VÀ gọi isArray() an toàn.
     *
     * @param response MessageResponse từ server
     */
    private void parseAndPopulateUserData(MessageResponse response) {
        try {
            if (response == null || response.getPayload() == null) {
                // [B37] Null check trước khi gọi isArray() — tránh NPE
                Platform.runLater(() -> statusLabel.setText("Không có dữ liệu từ server."));
                return;
            }

            // [B40] Convert trực tiếp từ payload object thay vì JSON → re-parse
            JsonNode dataArray = mapper.valueToTree(response.getPayload());

            if (!dataArray.isArray()) {
                Platform.runLater(() -> statusLabel.setText("Dữ liệu không đúng định dạng."));
                return;
            }

            Platform.runLater(() -> {
                userData.clear();
                for (JsonNode userNode : dataArray) {
                    UserInfo info = new UserInfo();
                    info.setUsername(userNode.path("username").asText("N/A"));
                    info.setEmail(userNode.path("email").asText("N/A"));
                    info.setRole(userNode.path("role").asText("UNKNOWN"));
                    boolean isLocked = userNode.path("isLocked").asBoolean(false);
                    info.setStatus(isLocked ? "Đã khóa" : "Bình thường");
                    info.setUserId(userNode.path("id").asText(""));
                    userData.add(info);
                }
                statusLabel.setText("Đã tải " + userData.size() + " người dùng thành công.");
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> statusLabel.setText("Lỗi xử lý dữ liệu: " + ex.getMessage()));
        }
    }

    /**
     * Xử lý sự kiện click nút Khóa tài khoản.
     *
     * <p>// 📌 [B39] Confirmation dialog trước khi thực hiện.
     */
    @FXML
    public void handleLockUser() {
        UserInfo selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận thao tác");
        confirmDialog.setHeaderText("Khóa tài khoản hệ thống");
        confirmDialog.setContentText("Bạn có chắc chắn muốn khóa tài khoản '" + selectedUser.getUsername() + "' không?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendLockRequest(selectedUser.getUserId(), true);
            }
        });
    }

    /**
     * Xử lý sự kiện click nút Mở khóa tài khoản.
     *
     * <p>// 📌 [B39] Thêm confirmation cho mở khóa.
     */
    @FXML
    public void handleUnlockUser() {
        UserInfo selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận thao tác");
        confirmDialog.setHeaderText("Mở khóa tài khoản");
        confirmDialog.setContentText("Mở khóa tài khoản '" + selectedUser.getUsername() + "'?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendLockRequest(selectedUser.getUserId(), false);
            }
        });
    }

    /**
     * Gửi request Khóa/Mở khóa User xuống Server.
     *
     * @param userId ID của User cần tác động.
     * @param isLock true nếu muốn khóa, false nếu muốn mở khóa.
     */
    private void sendLockRequest(String userId, boolean isLock) {
        MessageRequest req = new MessageRequest();
        req.setType(isLock ? CMD_LOCK_USER : CMD_UNLOCK_USER);
        req.setToken(ClientSession.getInstance().getToken());

        com.fasterxml.jackson.databind.node.ObjectNode payloadNode = mapper.createObjectNode();
        payloadNode.put("userId", userId);
        req.setPayload(payloadNode);

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                String actionMsg = isLock ? "Đã khóa" : "Đã mở khóa";
                statusLabel.setText(actionMsg + " tài khoản thành công.");
                loadUsers(); // Refresh lại danh sách
            });
        });

        task.setOnFailed(event ->
                Platform.runLater(() -> statusLabel.setText("Thao tác mạng thất bại. Vui lòng kiểm tra kết nối.")));

        new Thread(task).start();
    }

    /**
     * [B42/GAP1] Xử lý sự kiện RUN_INTEGRITY_CHECK — trước đây là dead code.
     * Gửi request đến server để kiểm tra tính toàn vẹn dữ liệu.
     */
    @FXML
    public void handleRunIntegrityCheck() {
        statusLabel.setText("Đang chạy kiểm tra toàn vẹn dữ liệu...");

        MessageRequest req = new MessageRequest();
        req.setType(CMD_RUN_INTEGRITY_CHECK);
        req.setToken(ClientSession.getInstance().getToken());

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(event -> {
            MessageResponse resp = task.getValue();
            Platform.runLater(() -> {
                if ("OK".equals(resp.getStatus())) {
                    statusLabel.setText("Kiểm tra toàn vẹn hoàn tất: " + resp.getMessage());
                    // Hiện kết quả chi tiết nếu có
                    if (resp.getPayload() != null) {
                        JsonNode result = mapper.valueToTree(resp.getPayload());
                        Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
                        resultAlert.setTitle("Kết quả kiểm tra toàn vẹn");
                        resultAlert.setHeaderText("Data Integrity Check Results");
                        resultAlert.setContentText(result.toPrettyString());
                        resultAlert.show();
                    }
                } else {
                    statusLabel.setText("Kiểm tra thất bại: " + resp.getMessage());
                }
            });
        });

        task.setOnFailed(event ->
                Platform.runLater(() -> statusLabel.setText("Không thể chạy kiểm tra toàn vẹn: kết nối thất bại.")));

        new Thread(task, "integrity-check").start();
    }

    /**
     * Xử lý sự kiện làm mới (Refresh) danh sách thủ công.
     */
    @FXML
    public void handleRefresh() {
        statusLabel.setText("Đang tải lại danh sách...");
        loadUsers();
    }

    /**
     * Xử lý sự kiện nút Back, điều hướng về màn hình danh sách đấu giá.
     */
    @FXML
    public void handleBack() {
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * DTO (Data Transfer Object) lưu trữ thông tin User để hiển thị lên UI.
     */
    public static class UserInfo {
        private String userId;
        private String username;
        private String email;
        private String role;
        private String status;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}