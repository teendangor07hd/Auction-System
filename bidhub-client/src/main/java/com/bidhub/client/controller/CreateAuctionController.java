package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.util.UiUtils;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Controller tao phien dau gia — chi cho SELLER.
 *
 * <p>// 📌 [B23] Validate start time < end time.
 * // 📌 [B24] Validate thoi gian trong tuong lai.
 * // 📌 [B25] Spinner commit listener de validate gia tri nhap tay.
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
        // [B26] Helper method tránh lặp setup Spinner 6 lần
        setupHourSpinner(spStartHour, 12);
        setupHourSpinner(spEndHour, 12);

        btnSubmit.setOnAction(e -> createAuction());
        btnBack.setOnAction(e ->
                com.bidhub.client.navigation.ViewRouter.getInstance()
                        .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));

        // 📌 [Tieu chi: UX — TextField chi nhan so]
        UiUtils.applyNumericFilter(tfStartingPrice);
        UiUtils.applyNumericFilter(tfMinIncrement);
    }

    /**
     * [B26] Helper tạo và cấu hình Spinner giờ (0–23).
     * Thêm commit listener để validate giá trị nhập tay (B25).
     */
    private void setupHourSpinner(Spinner<Integer> spinner, int defaultValue) {
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, defaultValue);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);

        // [B25] Validate giá trị nhập tay khi mất focus hoặc Enter
        spinner.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitSpinnerValue(spinner);
            }
        });
        spinner.getEditor().setOnAction(e -> commitSpinnerValue(spinner));
    }

    /**
     * [B25] Commit và validate giá trị nhập tay vào Spinner.
     * Nếu không hợp lệ, reset về giá trị cũ hợp lệ.
     */
    private void commitSpinnerValue(Spinner<Integer> spinner) {
        try {
            String text = spinner.getEditor().getText();
            int value = Integer.parseInt(text.trim());
            SpinnerValueFactory<Integer> factory = (SpinnerValueFactory.IntegerSpinnerValueFactory) spinner.getValueFactory();
            int min = ((SpinnerValueFactory.IntegerSpinnerValueFactory) factory).getMin();
            int max = ((SpinnerValueFactory.IntegerSpinnerValueFactory) factory).getMax();
            if (value < min || value > max) {
                // Out of range — reset về min/max
                spinner.getValueFactory().setValue(value < min ? min : max);
            } else {
                spinner.getValueFactory().setValue(value);
            }
        } catch (NumberFormatException e) {
            // Invalid text — reset về giá trị hợp lệ hiện tại
            spinner.getEditor().setText(String.valueOf(spinner.getValue()));
        }
    }

    /**
     * Gui request CREATE_AUCTION den server.
     *
     * <p>// 📌 [B23] Validate startTime < endTime.
     * // 📌 [B24] Validate cả hai thời gian phải trong tương lai.
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

        // Commit spinner values trước khi đọc
        commitSpinnerValue(spStartHour);
        commitSpinnerValue(spEndHour);

        LocalDateTime startDateTime = buildDateTime(dpStartTime.getValue(), spStartHour.getValue());
        LocalDateTime endDateTime = buildDateTime(dpEndTime.getValue(), spEndHour.getValue());
        LocalDateTime now = LocalDateTime.now();

        // [B24] Validate thời gian trong tương lai
        if (!startDateTime.isAfter(now)) {
            UiUtils.showError("Lỗi thời gian", "Thời gian bắt đầu phải ở trong tương lai.");
            return;
        }

        // [B23] Validate startTime < endTime
        if (!endDateTime.isAfter(startDateTime)) {
            UiUtils.showError("Lỗi thời gian", "Thời gian kết thúc phải sau thời gian bắt đầu.");
            return;
        }

        String incStr = tfMinIncrement.getText().trim();
        double startingPrice = Double.parseDouble(tfStartingPrice.getText().trim());
        double minIncrement = incStr.isEmpty() ? 1.0 : Double.parseDouble(incStr);

        String startTime = startDateTime.toString();
        String endTime = endDateTime.toString();

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

    /**
     * Build LocalDateTime từ date + hour.
     */
    private LocalDateTime buildDateTime(LocalDate date, int hour) {
        return date.atTime(hour, 0, 0);
    }
}