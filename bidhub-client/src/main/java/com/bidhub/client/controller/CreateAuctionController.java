package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.util.UiUtils;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller tạo phiên đấu giá — chỉ dành cho SELLER.
 * Cho phép chọn sản phẩm, thiết lập giá và khoảng thời gian đấu giá.
 */
public class CreateAuctionController {

    @FXML private ComboBox<String> cbItemId;
    @FXML private TextField tfStartingPrice;
    @FXML private TextField tfMinIncrement;
    @FXML private DatePicker dpStartTime;
    @FXML private Spinner<Integer> spStartHour;
    @FXML private Spinner<Integer> spStartMinute;
    @FXML private Spinner<Integer> spStartSecond;
    @FXML private DatePicker dpEndTime;
    @FXML private Spinner<Integer> spEndHour;
    @FXML private Spinner<Integer> spEndMinute;
    @FXML private Spinner<Integer> spEndSecond;
    @FXML private Button btnSubmit;
    @FXML private Button btnBack;

    @FXML private ProgressIndicator loadingSpinner;

    private final ObjectMapper mapper = new ObjectMapper();

    /** Map lưu itemId theo tên hiển thị, dùng để tra cứu khi submit. */
    private final java.util.Map<String, String> itemDisplayToId = new java.util.LinkedHashMap<>();

    /**
     * Khởi tạo Spinner giờ/phút/giây cho thời điểm bắt đầu và kết thúc, tải danh sách sản phẩm của seller.
     */
    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> startHourFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
        spStartHour.setValueFactory(startHourFactory);
        spStartHour.setEditable(true);
        SpinnerValueFactory<Integer> startMinuteFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        spStartMinute.setValueFactory(startMinuteFactory);
        spStartMinute.setEditable(true);
        SpinnerValueFactory<Integer> startSecondFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        spStartSecond.setValueFactory(startSecondFactory);
        spStartSecond.setEditable(true);

        SpinnerValueFactory<Integer> endHourFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
        spEndHour.setValueFactory(endHourFactory);
        spEndHour.setEditable(true);
        SpinnerValueFactory<Integer> endMinuteFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        spEndMinute.setValueFactory(endMinuteFactory);
        spEndMinute.setEditable(true);
        SpinnerValueFactory<Integer> endSecondFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        spEndSecond.setValueFactory(endSecondFactory);
        spEndSecond.setEditable(true);

        btnSubmit.setOnAction(e -> createAuction());
        btnBack.setOnAction(e ->
                com.bidhub.client.navigation.ViewRouter.getInstance()
                        .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));

        UiUtils.applyNumericFilter(tfStartingPrice);
        UiUtils.applyNumericFilter(tfMinIncrement);

        loadMyItems();
    }

    /**
     * Gửi request LIST_MY_ITEMS đến server để lấy danh sách sản phẩm của seller,
     * sau đó điền vào ComboBox.
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
                Platform.runLater(() -> {
                    cbItemId.getItems().clear();
                    itemDisplayToId.clear();
                    try {
                        // Payload là List<Map> — parse từng item thành display name
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
                Platform.runLater(() ->
                        UiUtils.showError("Lỗi", "Không thể tải danh sách sản phẩm: " + response.getMessage()));
            }
        });

        task.setOnFailed(e ->
                Platform.runLater(() ->
                        UiUtils.showError("Lỗi kết nối", "Lỗi tải danh sách sản phẩm: " + task.getException().getMessage())));

        new Thread(task).start();
    }

    /**
     * Gửi request CREATE_AUCTION lên server sau khi đã xác thực dữ liệu form.
     */
    private void createAuction() {
        String selectedDisplay = cbItemId.getValue();
        // Tra cứu itemId thực từ map theo tên hiển thị được chọn
        String itemId = (selectedDisplay != null) ? itemDisplayToId.get(selectedDisplay) : null;

        if (itemId == null || itemId.isBlank()) {
            UiUtils.showError("Lỗi nhập liệu", "Vui lòng chọn sản phẩm.");
            return;
        }

        if (!UiUtils.validateNotEmpty(tfStartingPrice, "Giá khởi điểm")) return;
        if (!UiUtils.validatePositiveNumber(tfStartingPrice, "Giá khởi điểm")) return;

        if (dpStartTime.getValue() == null || dpEndTime.getValue() == null) {
            UiUtils.showError("Lỗi nhập liệu", "Vui lòng chọn thời gian bắt đầu và kết thúc.");
            return;
        }

        String incStr = tfMinIncrement.getText().trim();
        double startingPrice = Double.parseDouble(tfStartingPrice.getText().trim());
        double minIncrement = incStr.isEmpty() ? 1.0 : Double.parseDouble(incStr);

        String startTime = dpStartTime.getValue().toString() + "T"
                + String.format("%02d:%02d:%02d", spStartHour.getValue(), spStartMinute.getValue(), spStartSecond.getValue());
        String endTime = dpEndTime.getValue().toString() + "T"
                + String.format("%02d:%02d:%02d", spEndHour.getValue(), spEndMinute.getValue(), spEndSecond.getValue());

        Runnable onComplete = (btnSubmit != null && loadingSpinner != null)
                ? UiUtils.showLoading(btnSubmit, loadingSpinner)
                : () -> { if (btnSubmit != null) btnSubmit.setDisable(false); };

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
                UiUtils.showInfo("Thành công", "Đã tạo phiên đấu giá thành công!");
                Platform.runLater(() ->
                        com.bidhub.client.navigation.ViewRouter.getInstance()
                                .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));
            } else {
                Platform.runLater(() -> UiUtils.showError("Lỗi tạo phiên", response.getMessage()));
            }
            onComplete.run();
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> UiUtils.showError("Lỗi kết nối", "Không kết nối được máy chủ. Thử lại sau."));
            onComplete.run();
        });

        new Thread(task, "create-auction").start();
    }
}