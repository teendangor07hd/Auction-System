package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Controller danh sach phien dau gia — giao dien co Sidebar Menu.
 * Dong bo voi AuctionListView.fxml (su dung BorderPane + VBox sidebar).
 */
public class AuctionListController {

    // TableView va cac cot
    @FXML private TableView<JsonNode> auctionTable;
    @FXML private TableColumn<JsonNode, String> colItemName;
    @FXML private TableColumn<JsonNode, String> colPrice;
    @FXML private TableColumn<JsonNode, String> colEndTime;
    @FXML private TableColumn<JsonNode, String> colStatus;

    // Nut bam trong Sidebar
    @FXML private Button btnCreateAuction;
    @FXML private Button btnCreateItem;
    @FXML private Button btnLogout;

    // Label hien thi vai tro nguoi dung (goc duoi sidebar)
    @FXML private Label lblUserRole;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObservableList<JsonNode> auctionData =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Cau hinh cac cot cho TableView
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

        // 2. Double-click de xem chi tiet phien dau gia
        auctionTable.setRowFactory(tv -> {
            TableRow<JsonNode> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    JsonNode selected = row.getItem();
                    String auctionId = selected.path("id").asText("");
                    if (!auctionId.isEmpty()) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("auctionId", auctionId);
                        com.bidhub.client.navigation.ViewRouter.getInstance()
                                .navigateTo(Views.AUCTION_DETAIL, params);
                    }
                }
            });
            return row;
        });

        // 3. Phan quyen hien thi nut
        // Tam thoi luon hien thi de de test (bo di cac dong setVisible)
        // Neu muon chi SELLER moi thay, mo comment cac dong ben duoi
        // btnCreateAuction.setVisible("SELLER".equals(ClientSession.getInstance().getCurrentRole()));
        // btnCreateItem.setVisible("SELLER".equals(ClientSession.getInstance().getCurrentRole()));

        // Dieu huong nut
        btnCreateAuction.setOnAction(e ->
                com.bidhub.client.navigation.ViewRouter.getInstance().navigateTo(Views.CREATE_AUCTION));
        btnCreateItem.setOnAction(e ->
                com.bidhub.client.navigation.ViewRouter.getInstance().navigateTo(Views.CREATE_ITEM));

        // 4. Nut Dang xuat
        btnLogout.setOnAction(e -> {
            NetworkTask<MessageResponse> task = new NetworkTask<>(() -> {
                MessageRequest req = new MessageRequest();
                req.setType("LOGOUT");
                req.setToken(ClientSession.getInstance().getToken());
                return ServerGateway.getInstance().sendRequest(req);
            });
            task.setOnSucceeded(ev -> {
                ClientSession.getInstance().logout();
                com.bidhub.client.navigation.ViewRouter.getInstance().navigateTo(Views.LOGIN);
            });
            task.setOnFailed(ev -> {
                ClientSession.getInstance().logout();
                com.bidhub.client.navigation.ViewRouter.getInstance().navigateTo(Views.LOGIN);
            });
            new Thread(task).start();
        });

        // 5. Hien thi thong tin nguoi dung (ten + vai tro)
        String role = ClientSession.getInstance().getCurrentRole();
        String username = ClientSession.getInstance().getCurrentUsername();
        if (username != null && role != null) {
            lblUserRole.setText(username + " (" + role + ")");
        } else {
            lblUserRole.setText("Chưa đăng nhập");
        }

        // 6. Load danh sach phien dau gia
        loadAuctionList();
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