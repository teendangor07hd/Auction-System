package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class MainLayoutController {

    private static final String CMD_LOGOUT = "LOGOUT";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SELLER = "SELLER";
    private static final String ROLE_BIDDER = "BIDDER";

    @FXML private BorderPane mainPane;
    @FXML private Button btnCreateAuction;
    @FXML private Button btnCreateItem;
    @FXML private Button btnAuctionList;
    @FXML private Button btnNotifications;
    @FXML private Button btnItemCatalog;
    @FXML private Button btnSellerDashboard;
    @FXML private Button btnAccount;
    @FXML private Button btnLogout;
    @FXML private Button btnLogin;
    @FXML private Button btnBidderItems;
    @FXML private Button adminBtn;
    @FXML private Label lblSidebarUser;
    @FXML private Label lblSidebarRole;

    @FXML
    public void initialize() {
        setupNavigationAndSecurity();
        setupActionHandlers();
    }

    private void setupNavigationAndSecurity() {
        btnCreateAuction.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.CREATE_AUCTION));
        btnCreateItem.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.CREATE_ITEM));
        btnAuctionList.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST));

        if (btnNotifications != null) {
            btnNotifications.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.NOTIFICATION_VIEW));
        }
        if (btnItemCatalog != null) {
            btnItemCatalog.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.ITEM_CATALOG));
        }

        boolean isLoggedIn = ClientSession.getInstance().isLoggedIn();
        String currentUsername = ClientSession.getInstance().getCurrentUsername();
        String currentRole = String.valueOf(ClientSession.getInstance().getCurrentRole());
        boolean isAdmin = ROLE_ADMIN.equals(currentRole);
        boolean isSeller = ROLE_SELLER.equals(currentRole);
        boolean isBidder = ROLE_BIDDER.equals(currentRole);

        if (lblSidebarUser != null) {
            lblSidebarUser.setText("👤 " + (currentUsername != null && !currentUsername.isBlank() ? currentUsername : "Khách"));
        }
        if (lblSidebarRole != null) {
            String roleDisplay = isLoggedIn ? currentRole : "Chưa đăng nhập";
            lblSidebarRole.setText("Vai trò: " + roleDisplay);
        }

        // Admin button
        if (adminBtn != null) {
            adminBtn.setVisible(isAdmin);
            adminBtn.setManaged(isAdmin);
        }

        // Seller buttons
        if (btnSellerDashboard != null) {
            boolean showSeller = isSeller || isAdmin;
            btnSellerDashboard.setVisible(showSeller);
            btnSellerDashboard.setManaged(showSeller);
        }

        // Bidder buttons
        if (btnBidderItems != null) {
            btnBidderItems.setVisible(isBidder);
            btnBidderItems.setManaged(isBidder);
        }

        // Login/Logout buttons
        if (btnLogin != null) {
            btnLogin.setVisible(!isLoggedIn);
            btnLogin.setManaged(!isLoggedIn);
        }
        if (btnLogout != null) {
            btnLogout.setVisible(isLoggedIn);
            btnLogout.setManaged(isLoggedIn);
        }
        if (btnAccount != null) {
            btnAccount.setVisible(isLoggedIn);
            btnAccount.setManaged(isLoggedIn);
        }

        // Ẩn các nút SELLER nếu chưa đăng nhập
        if (!isLoggedIn) {
            setVisible(btnCreateItem, false);
            setVisible(btnCreateAuction, false);
        }
    }

    private void setVisible(Button btn, boolean visible) {
        if (btn != null) { btn.setVisible(visible); btn.setManaged(visible); }
    }

    private void setupActionHandlers() {
        if (btnAccount != null) btnAccount.setOnAction(e -> showAccountPopup());
        if (btnLogout != null) btnLogout.setOnAction(e -> handleLogout());
    }

    @FXML
    public void handleLogin() {
        ViewRouter.getInstance().navigateTo(Views.LOGIN);
    }

    @FXML
    public void handleAdminPanel() {
        ViewRouter.getInstance().navigateTo(Views.ADMIN_VIEW);
    }

    @FXML
    public void handleNotifications() {
        ViewRouter.getInstance().navigateTo(Views.NOTIFICATION_VIEW);
    }

    @FXML
    public void handleItemCatalog() {
        ViewRouter.getInstance().navigateTo(Views.ITEM_CATALOG);
    }

    @FXML
    public void handleSellerDashboard() {
        ViewRouter.getInstance().navigateTo(Views.SELLER_DASHBOARD);
    }

    @FXML
    public void handleBidderItems() {
        ViewRouter.getInstance().navigateTo(Views.BIDDER_ITEMS);
    }

    private void handleLogout() {
        NetworkTask<MessageResponse> task = new NetworkTask<>(() -> {
            MessageRequest req = new MessageRequest();
            req.setType(CMD_LOGOUT);
            req.setToken(ClientSession.getInstance().getToken());
            return ServerGateway.getInstance().sendRequest(req);
        });

        Runnable forceLogout = () -> {
            ClientSession.getInstance().logout();
            ViewRouter.getInstance().navigateTo(Views.LOGIN);
        };

        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            MessageResponse resp = task.getValue();
            if (resp != null && resp.isOk()) {
                forceLogout.run();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Đăng xuất thất bại từ phía máy chủ: " + (resp != null ? resp.getMessage() : "Lỗi không xác định"));
                alert.showAndWait();
            }
        }));
        task.setOnFailed(ev -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi kết nối khi đăng xuất. Không thể ngắt phiên làm việc trên máy chủ.");
            alert.showAndWait();
        }));
        new Thread(task, "logout-thread").start();
    }

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
        String roleDisplay = (role != null) ? switch (role) {
            case "BIDDER" -> "Người Đấu Giá";
            case "SELLER" -> "Người Bán";
            case "ADMIN" -> "Quản Trị Viên";
            default -> role;
        } : "Không xác định";
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
}
