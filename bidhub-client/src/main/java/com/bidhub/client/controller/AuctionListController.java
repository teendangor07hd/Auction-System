package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Controller danh sach phien dau gia — giao dien co Sidebar Menu.
 * Dong bo voi AuctionListView.fxml (su dung BorderPane + VBox sidebar).
 */
public class AuctionListController {

    // TableView va cac cot (Columns)
    @FXML private TableView<JsonNode> auctionTable;
    @FXML private TableColumn<JsonNode, String> colItemName;
    @FXML private TableColumn<JsonNode, String> colPrice;
    @FXML private TableColumn<JsonNode, String> colEndTime;
    @FXML private TableColumn<JsonNode, String> colStatus;

    // Nut bam (Buttons) trong Sidebar
    @FXML private Button btnCreateAuction;
    @FXML private Button btnCreateItem;
    @FXML private Button btnAccount;
    @FXML private Button btnLogout;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObservableList<JsonNode> auctionData =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Cau hinh cac cot cho TableView (Setup columns)
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

        // 2. Double-click de xem chi tiet phien dau gia (View details)
        auctionTable.setRowFactory(tv -> {
            TableRow<JsonNode> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    JsonNode selected = row.getItem();
                    String auctionId = selected.path("id").asText("");
                    if (!auctionId.isEmpty()) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("auctionId", auctionId);
                        ViewRouter.getInstance()
                                .navigateTo(Views.AUCTION_DETAIL, params);
                    }
                }
            });
            return row;
        });

        // 3. Dieu huong nut (Navigation)
        btnCreateAuction.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.CREATE_AUCTION));
        btnCreateItem.setOnAction(e -> ViewRouter.getInstance().navigateTo(Views.CREATE_ITEM));

        // 4. Nut Tai khoan (popup xem thong tin - Account info)
        btnAccount.setOnAction(e -> showAccountPopup());

        // 5. Nut Dang xuat (Logout)
        btnLogout.setOnAction(e -> {
            NetworkTask<MessageResponse> task = new NetworkTask<>(() -> {
                MessageRequest req = new MessageRequest();
                req.setType("LOGOUT");
                req.setToken(ClientSession.getInstance().getToken());
                return ServerGateway.getInstance().sendRequest(req);
            });
            task.setOnSucceeded(ev -> {
                ClientSession.getInstance().logout();
                ViewRouter.getInstance().navigateTo(Views.LOGIN);
            });
            task.setOnFailed(ev -> {
                ClientSession.getInstance().logout();
                ViewRouter.getInstance().navigateTo(Views.LOGIN);
            });
            new Thread(task).start();
        });

        // 6. Load danh sach phien dau gia (Fetch data)
        loadAuctionList();
    }

    /**
     * Hiển thị popup thông tin tài khoản (username, role)
     */
    private void showAccountPopup() {
        ClientSession session = ClientSession.getInstance();
        String username = session.getCurrentUsername();
        String role = session.getCurrentRole();
        if (username == null) {
            username = "Chưa đăng nhập";
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông tin tài khoản");
        alert.setHeaderText(null);

        // Tạo layout cho nội dung (Layout configuration)
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

        // Để label value co giãn (Responsive behavior)
        GridPane.setHgrow(lblUsernameValue, Priority.ALWAYS);
        GridPane.setHgrow(lblRoleValue, Priority.ALWAYS);

        alert.getDialogPane().setContent(grid);
        alert.getDialogPane().setMinWidth(300);
        alert.showAndWait();
    }

    private void loadAuctionList() {
        MessageRequest req = new MessageRequest();
        req.setType("GET_AUCTION_LIST");

        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus()) && response.getPayload() != null) {
                JsonNode payload = mapper.valueToTree(response.getPayload());
                if (payload.isArray()) {
                    auctionData.clear();
                    for (JsonNode node : payload) {
                        auctionData.add(node);
                    }
                }
            }
        });

        task.setOnFailed(e -> {
            System.err.println("[AuctionListController] Lỗi load danh sách: " +
                    task.getException().getMessage());
        });

        new Thread(task).start();
    }
}