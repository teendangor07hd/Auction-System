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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

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
    @FXML private TableView<JsonNode> auctionTable;
    @FXML private TableColumn<JsonNode, String> colItemName;
    @FXML private TableColumn<JsonNode, String> colPrice;
    @FXML private TableColumn<JsonNode, String> colEndTime;
    @FXML private TableColumn<JsonNode, String> colStatus;

    // Sidebar & Navigation Buttons
    @FXML private Button btnCreateAuction;
    @FXML private Button btnCreateItem;
    @FXML private Button btnAccount;
    @FXML private Button btnLogout;
    @FXML private Button adminBtn; // Nút dành riêng cho Admin

    // UX Components
    @FXML private Button btnRefresh;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label lblEmptyMessage;

    // ========================================================================
    // INTERNAL STATE
    // ========================================================================
    private final ObjectMapper mapper = new ObjectMapper();
    private final ObservableList<JsonNode> auctionData = FXCollections.observableArrayList();

    /**
     * Vòng đời JavaFX: Khởi tạo dữ liệu và sự kiện ngay sau khi load xong UI.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        setupTableDoubleClickHandler();
        setupNavigationAndSecurity();
        setupActionHandlers();

        // Tải dữ liệu lần đầu tiên khi mở màn hình
        loadAuctionList();
    }

    /**
     * Ràng buộc (Bind) dữ liệu từ JsonNode vào các cột của TableView.
     */
    private void setupTableColumns() {
        colItemName.setCellValueFactory(cellData -> {
            JsonNode node = cellData.getValue();
            String name = node.has("itemName") ? node.get("itemName").asText("") : node.path("id").asText("");
            return new javafx.beans.property.SimpleStringProperty(name);
        });

        colPrice.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(cellData.getValue().path("currentHighestBid").asDouble(0))));

        colEndTime.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().path("endTime").asText("")));

        colStatus.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().path("status").asText("")));

        auctionTable.setItems(auctionData);
    }

    /**
     * Bắt sự kiện nhấp đúp (Double-click) vào một dòng trên bảng để xem chi tiết.
     */
    private void setupTableDoubleClickHandler() {
        auctionTable.setRowFactory(tv -> {
            TableRow<JsonNode> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    JsonNode selected = row.getItem();
                    String auctionId = selected.path("id").asText("");

                    if (!auctionId.isEmpty()) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("auctionId", auctionId);
                        ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL, params);
                    }
                }
            });
            return row;
        });
    }

    /**
     * Xử lý hiển thị các nút điều hướng dựa trên phân quyền (Role-based UI).
     */
    private void setupNavigationAndSecurity() {
        btnCreateAuction.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.CREATE_AUCTION));
        btnCreateItem.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.CREATE_ITEM));

        // Security Check: Hiển thị nút Admin Panel nếu user hiện tại là ADMIN
        String currentRole = String.valueOf(ClientSession.getInstance().getCurrentRole());
        boolean isAdmin = ROLE_ADMIN.equals(currentRole);

        if (adminBtn != null) {
            adminBtn.setVisible(isAdmin);
            // managed = false giúp thu gọn không gian UI nếu nút bị ẩn, tránh bị khoảng trống thừa
            adminBtn.setManaged(isAdmin);
        }
    }

    /**
     * Cài đặt logic cho các nút hành động (Refresh, Account, Logout).
     */
    private void setupActionHandlers() {
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> loadAuctionList());
        }

        btnAccount.setOnAction(e -> showAccountPopup());

        btnLogout.setOnAction(e -> handleLogout());
    }

    /**
     * Luồng xử lý sự kiện bấm nút Admin Panel (chỉ ADMIN mới thấy nút này).
     */
    @FXML
    public void handleAdminPanel() {
        ViewRouter.getInstance().navigateTo(Views.ADMIN_VIEW);
    }

    /**
     * Gọi API đăng xuất và xóa cache Session hiện tại.
     */
    private void handleLogout() {
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> {
            MessageRequest req = new MessageRequest();
            req.setType(CMD_LOGOUT);
            req.setToken(ClientSession.getInstance().getToken());
            return ServerGateway.getInstance().sendRequest(req);
        });

        // Bất kể server trả về thành công hay thất bại, client vẫn phải clear session và văng ra màn Login
        Runnable forceLogout = () -> {
            ClientSession.getInstance().logout();
            ViewRouter.getInstance().navigateTo(Views.LOGIN);
        };

        task.setOnSucceeded(ev -> forceLogout.run());
        task.setOnFailed(ev -> forceLogout.run());
        new Thread(task, "logout-thread").start();
    }

    /**
     * Hiển thị popup thông tin tài khoản (Username, Role)
     */
    private void showAccountPopup() {
        ClientSession session = ClientSession.getInstance();
        String username = session.getCurrentUsername();
        String role = session.getCurrentRole();

        if (username == null || username.isEmpty()) {
            username = "Chưa đăng nhập";
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông tin tài khoản");
        alert.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);

        Label lblUsernameTitle = new Label("Tên đăng nhập:");
        Label lblUsernameValue = new Label(username);
        lblUsernameValue.setStyle("-fx-font-weight: bold;");

        Label lblRoleTitle = new Label("Vai trò:");
        String roleDisplay = (role != null) ? role : "Không xác định";
        Label lblRoleValue = new Label(roleDisplay);
        lblRoleValue.setStyle("-fx-font-weight: bold;");

        grid.add(lblUsernameTitle, 0, 0);
        grid.add(lblUsernameValue, 1, 0);
        grid.add(lblRoleTitle, 0, 1);
        grid.add(lblRoleValue, 1, 1);

        GridPane.setHgrow(lblUsernameValue, Priority.ALWAYS);
        GridPane.setHgrow(lblRoleValue, Priority.ALWAYS);

        alert.getDialogPane().setContent(grid);
        alert.getDialogPane().setMinWidth(300);
        alert.showAndWait();
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
        if (auctionTable != null) {
            auctionTable.setVisible(!isEmpty);
        }
    }
}