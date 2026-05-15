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
 * <p>
 * Trách nhiệm (Single Responsibility):
 * - Hiển thị danh sách người dùng (TableView).
 * - Cung cấp tính năng Khóa/Mở khóa tài khoản.
 * - Giao tiếp với Server thông qua Socket layer.
 */
public class AdminController implements com.bidhub.client.navigation.ContextAware {

    // ========================================================================
    // CONSTANTS (Hằng số định nghĩa các Command và Role để tránh Magic Strings)
    // ========================================================================
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String CMD_GET_USER_LIST = "GET_USER_LIST";
    private static final String CMD_LOCK_USER = "LOCK_USER";
    private static final String CMD_UNLOCK_USER = "UNLOCK_USER";

    // ========================================================================
    // FXML INJECTIONS (Các thành phần giao diện được bind từ file .fxml)
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
    // INTERNAL STATE (Trạng thái nội bộ của Controller)
    // ========================================================================

    /** * Danh sách dữ liệu observable để bind trực tiếp vào TableView.
     * Khi list này thay đổi, UI sẽ tự động update.
     */
    private final ObservableList<UserInfo> userData = FXCollections.observableArrayList();

    /** * Tái sử dụng (Reuse) ObjectMapper để tối ưu hiệu năng (Performance optimization).
     * ObjectMapper là thread-safe sau khi cấu hình xong.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setContext(java.util.Map<String, Object> params) {
        // Màn hình này không yêu cầu tham số ngữ cảnh (context params) khi điều hướng tới
    }

    /**
     * Hàm lifecycle của JavaFX, được gọi tự động sau khi load FXML.
     */
    @FXML
    public void initialize() {
        setupTableColumns();

        // Security Check: Đảm bảo chỉ User có role ADMIN mới được truy cập.
        // Ép kiểu (Type Casting) về String để giữ tính độc lập (Decoupling) giữa Client và Server.
        String currentRole = String.valueOf(ClientSession.getInstance().getCurrentRole());
        if (!ROLE_ADMIN.equals(currentRole)) {
            ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
            return;
        }

        // Tải dữ liệu ban đầu
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
     * Lắng nghe sự kiện (Event Listener) khi người dùng chọn một dòng trên bảng.
     * Dùng để bật/tắt (enable/disable) các nút hành động tương ứng.
     */
    private void setupTableSelectionListener() {
        userTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                lockBtn.setDisable(true);
                unlockBtn.setDisable(true);
                return;
            }

            // Business logic: Không cho phép Admin khóa tài khoản của một Admin khác.
            boolean isAdmin = ROLE_ADMIN.equals(newValue.getRole());
            lockBtn.setDisable(isAdmin);
            unlockBtn.setDisable(false); // Luôn cho phép mở khóa (nếu tài khoản đang bị khóa)
        });
    }

    /**
     * Gửi request lấy danh sách User từ Server (Background Thread)
     * và cập nhật lên TableView (UI Thread).
     */
    private void loadUsers() {
        MessageRequest req = new MessageRequest();
        req.setType(CMD_GET_USER_LIST);
        req.setToken(ClientSession.getInstance().getToken());

        // Sử dụng NetworkTask để xử lý network I/O bất đồng bộ (Asynchronous)
        NetworkTask<String> task = new NetworkTask<>(() -> {
            MessageResponse resp = ServerGateway.getInstance().sendRequest(req);
            return com.bidhub.common.network.MessageMapper.toJson(resp);
        });

        // Xử lý khi nhận response thành công (Callback chạy trên JavaFX Application Thread)
        task.setOnSucceeded(event -> {
            String jsonResponse = task.getValue();
            parseAndPopulateUserData(jsonResponse);
        });

        // Xử lý khi tác vụ mạng thất bại (Timeout, Mất kết nối, v.v.)
        task.setOnFailed(event -> {
            Platform.runLater(() -> statusLabel.setText("Không thể kết nối đến máy chủ để tải danh sách."));
        });

        // Khởi chạy thread mới
        new Thread(task).start();
    }

    /**
     * Phân tích (Parse) JSON response và đổ dữ liệu (Populate) vào TableView.
     * * @param jsonResponse Chuỗi JSON trả về từ Server.
     */
    private void parseAndPopulateUserData(String jsonResponse) {
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode dataArray = root.get("data");

            if (dataArray != null && dataArray.isArray()) {
                // Đảm bảo thao tác cập nhật UI luôn nằm trong Platform.runLater
                Platform.runLater(() -> {
                    userData.clear();
                    for (JsonNode userNode : dataArray) {
                        UserInfo info = new UserInfo();
                        info.setUsername(userNode.has("username") ? userNode.get("username").asText() : "N/A");
                        info.setEmail(userNode.has("email") ? userNode.get("email").asText() : "N/A");
                        info.setRole(userNode.has("role") ? userNode.get("role").asText() : "UNKNOWN");

                        // Parse status dựa trên boolean isLocked
                        boolean isLocked = userNode.has("isLocked") && userNode.get("isLocked").asBoolean();
                        info.setStatus(isLocked ? "Đã khóa" : "Bình thường");

                        info.setUserId(userNode.has("id") ? userNode.get("id").asText() : "");
                        userData.add(info);
                    }
                    statusLabel.setText("Đã tải " + userData.size() + " người dùng thành công.");
                });
            }
        } catch (Exception ex) {
            // Log lỗi cho developer và thông báo thân thiện cho user
            ex.printStackTrace();
            Platform.runLater(() -> statusLabel.setText("Lỗi xử lý dữ liệu: " + ex.getMessage()));
        }
    }

    /**
     * Xử lý sự kiện click nút Khóa tài khoản.
     * Yêu cầu xác nhận (Confirmation) trước khi thực hiện luồng nguy hiểm (Dangerous action).
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
     */
    @FXML
    public void handleUnlockUser() {
        UserInfo selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;

        // Mở khóa thường không phải là hành động phá hủy (destructive), có thể bỏ qua bước confirm.
        sendLockRequest(selectedUser.getUserId(), false);
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

        // Tạo ObjectNode an toàn (Type-safe) thay vì cộng chuỗi String thủ công
        // Giúp tránh các lỗi cú pháp JSON (như thiếu dấu ngoặc kép)
        com.fasterxml.jackson.databind.node.ObjectNode payloadNode = mapper.createObjectNode();
        payloadNode.put("userId", userId);
        req.setPayload(payloadNode);

        NetworkTask<String> task = new NetworkTask<>(() -> {
            MessageResponse resp = ServerGateway.getInstance().sendRequest(req);
            return com.bidhub.common.network.MessageMapper.toJson(resp);
        });

        task.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                String actionMsg = isLock ? "Đã khóa" : "Đã mở khóa";
                statusLabel.setText(actionMsg + " tài khoản thành công.");
                loadUsers(); // Refresh lại danh sách (Data synchronization)
            });
        });

        task.setOnFailed(event -> {
            Platform.runLater(() -> statusLabel.setText("Thao tác mạng thất bại. Vui lòng kiểm tra kết nối."));
        });

        new Thread(task).start();
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
    // INNER CLASSES (Lớp nội bộ dùng làm DTO - Data Transfer Object cho bảng)
    // ========================================================================

    /**
     * DTO (Data Transfer Object) lưu trữ thông tin User để hiển thị lên UI.
     * Phải có đầy đủ Getter/Setter đúng chuẩn Java Beans để PropertyValueFactory hoạt động.
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