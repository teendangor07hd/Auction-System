package com.bidhub.client.controller;

import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.util.UiUtils; // THÊM IMPORT UiUtils
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller tao phien dau gia — chi cho SELLER.
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

    // 📌 [Tieu chi: UX — Loading state component]
    @FXML private ProgressIndicator loadingSpinner;

    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> startHourFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
        spStartHour.setValueFactory(startHourFactory);
        spStartHour.setEditable(true);

        SpinnerValueFactory<Integer> endHourFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
        spEndHour.setValueFactory(endHourFactory);
        spEndHour.setEditable(true);

        btnSubmit.setOnAction(e -> createAuction());
        btnBack.setOnAction(e ->
                com.bidhub.client.navigation.ViewRouter.getInstance()
                        .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));

        // 📌 [Tieu chi: UX — TextField chi nhan so]
        UiUtils.applyNumericFilter(tfStartingPrice);
        UiUtils.applyNumericFilter(tfMinIncrement);
    }

    /**
     * Gui request CREATE_AUCTION den server.
     */
    private void createAuction() {
        String itemId = cbItemId.getValue();
        if (itemId == null || itemId.isBlank()) {
            UiUtils.showError("Lỗi nhập liệu", "Vui lòng chọn sản phẩm.");
            return;
        }

        // 📌 [Tieu chi: UX — Form validation client-side]
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
                + String.format("%02d:00:00", spStartHour.getValue());
        String endTime = dpEndTime.getValue().toString() + "T"
                + String.format("%02d:00:00", spEndHour.getValue());

        // 📌 [Tieu chi: UX — Loading state]
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