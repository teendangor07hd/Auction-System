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

    @FXML private BorderPane mainPane;
    @FXML private Button btnCreateAuction;
    @FXML private Button btnCreateItem;
    @FXML private Button btnAuctionList;
    @FXML private Button btnNotifications;
    @FXML private Button btnItemCatalog;
    @FXML private Button btnSellerDashboard;
    @FXML private Button btnAccount;
    @FXML private Button btnLogout;
    @FXML private Button adminBtn;

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

        String currentRole = String.valueOf(ClientSession.getInstance().getCurrentRole());
        boolean isAdmin = ROLE_ADMIN.equals(currentRole);
        boolean isSeller = ROLE_SELLER.equals(currentRole);

        if (adminBtn != null) {
            adminBtn.setVisible(isAdmin);
            adminBtn.setManaged(isAdmin);
        }

        // Hiện nút "Sản phẩm của tôi" chỉ cho SELLER (và ADMIN)
        if (btnSellerDashboard != null) {
            boolean showSeller = isSeller || isAdmin;
            btnSellerDashboard.setVisible(showSeller);
            btnSellerDashboard.setManaged(showSeller);
        }
    }

    private void setupActionHandlers() {
        btnAccount.setOnAction(e -> showAccountPopup());
        btnLogout.setOnAction(e -> handleLogout());
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

        task.setOnSucceeded(ev -> Platform.runLater(forceLogout));
        task.setOnFailed(ev -> Platform.runLater(forceLogout));
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
