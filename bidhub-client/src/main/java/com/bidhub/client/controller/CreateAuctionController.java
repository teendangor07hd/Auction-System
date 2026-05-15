package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.common.network.MessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

/**
 * Controller tao phien dau gia — chi cho SELLER.
 *
 * <p>Form: chon Item, nhap startingPrice, startTime/endTime, minimumIncrement.
 * Submit gui request CREATE_AUCTION den server.
 *
 * <p>// 📌 [Tieu chi: MVC — Controller thuc hien business logic tren client]
 */
public class CreateAuctionController {

    @FXML private ComboBox<String> cbItemId;
    @FXML private TextField tfStartingPrice;
    @FXML private TextField tfMinIncrement;
    @FXML private DatePicker dpStartTime;
    @FXML private Spinner<Integer> spStartHour;
    @FXML private DatePicker dpEndTime;
    @FXML private Spinner<Integer> spEndHour;
    @FXML private Button btnSubmit;
    @FXML private Button btnBack;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Map luu itemId theo display name de tra cuu khi submit */
    private final java.util.Map<String, String> itemDisplayToId = new java.util.LinkedHashMap<>();

    @FXML
    public void initialize() {
        // Factory riêng cho giờ bắt đầu
        SpinnerValueFactory<Integer> startHourFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
        spStartHour.setValueFactory(startHourFactory);
        spStartHour.setEditable(true);

        // Factory riêng cho giờ kết thúc
        SpinnerValueFactory<Integer> endHourFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
        spEndHour.setValueFactory(endHourFactory);
        spEndHour.setEditable(true);

        btnSubmit.setOnAction(e -> createAuction());
        btnBack.setOnAction(e ->
                com.bidhub.client.navigation.ViewRouter.getInstance()
                        .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));

        // 📌 [Tieu chi: Chuc nang dau gia — load danh sach item cua seller vao ComboBox]
        loadMyItems();
    }

    /**
     * Gui request LIST_MY_ITEMS den server de lay danh sach san pham cua seller.
     * Populate ComboBox voi ket qua tra ve.
     */
    private void loadMyItems() {
        MessageRequest req = new MessageRequest();
        req.setType("LIST_MY_ITEMS");
        req.setToken(ClientSession.getInstance().getToken());
        req.setPayload(mapper.createObjectNode());

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus())) {
                javafx.application.Platform.runLater(() -> {
                    cbItemId.getItems().clear();
                    itemDisplayToId.clear();
                    try {
                        // payload la List<Map> — parse tung item
                        JsonNode payloadNode = mapper.valueToTree(response.getPayload());
                        if (payloadNode.isArray()) {
                            for (JsonNode itemNode : payloadNode) {
                                String itemId = itemNode.path("itemId").asText("");
                                String name = itemNode.path("name").asText("???");
                                String type = itemNode.path("itemType").asText("");
                                String display = name + " [" + type + "]";
                                itemDisplayToId.put(display, itemId);
                                cbItemId.getItems().add(display);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("[CreateAuction] Loi parse item list: " + ex.getMessage());
                    }
                });
            } else {
                javafx.application.Platform.runLater(() ->
                        showError("Khong the tai danh sach san pham: " + response.getMessage()));
            }
        });

        task.setOnFailed(e ->
                javafx.application.Platform.runLater(() ->
                        showError("Loi tai danh sach san pham: " + task.getException().getMessage())));

        new Thread(task).start();
    }

    /**
     * Gui request CREATE_AUCTION den server.
     *
     * <p>// 📌 [Tieu chi: Chuc nang dau gia — client tao phien moi]
     */
    private void createAuction() {
        String selectedDisplay = cbItemId.getValue();
        // Tra cuu itemId thuc tu map
        String itemId = (selectedDisplay != null) ? itemDisplayToId.get(selectedDisplay) : null;
        String priceStr = tfStartingPrice.getText().trim();
        String incStr = tfMinIncrement.getText().trim();

        if (itemId == null || itemId.isBlank()) {
            showError("Vui long chon san pham.");
            return;
        }
        if (priceStr.isEmpty()) {
            showError("Vui long nhap gia khoi diem.");
            return;
        }
        if (dpStartTime.getValue() == null || dpEndTime.getValue() == null) {
            showError("Vui long chon thoi gian bat dau va ket thuc.");
            return;
        }

        double startingPrice;
        double minIncrement;
        try {
            startingPrice = Double.parseDouble(priceStr);
            minIncrement = incStr.isEmpty() ? 1.0 : Double.parseDouble(incStr);
        } catch (NumberFormatException ex) {
            showError("Gia khong hop le. Vui long nhap so.");
            return;
        }

        String startTime = dpStartTime.getValue().toString() + "T"
                + String.format("%02d:00:00", spStartHour.getValue());
        String endTime = dpEndTime.getValue().toString() + "T"
                + String.format("%02d:00:00", spEndHour.getValue());

        ObjectNode payload = mapper.createObjectNode();
        payload.put("itemId", itemId);
        payload.put("startingPrice", startingPrice);
        payload.put("minimumIncrement", minIncrement);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);

        MessageRequest req = new MessageRequest();
        req.setType("CREATE_AUCTION");
        req.setToken(ClientSession.getInstance().getToken());
        req.setPayload(payload);

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus())) {
                javafx.application.Platform.runLater(() ->
                        com.bidhub.client.navigation.ViewRouter.getInstance()
                                .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));
            } else {
                javafx.application.Platform.runLater(() ->
                        showError(response.getMessage()));
            }
        });

        task.setOnFailed(e ->
                javafx.application.Platform.runLater(() ->
                        showError(task.getException().getMessage())));

        new Thread(task).start();
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR, message);
        alert.setTitle("Loi");
        alert.showAndWait();
    }
}