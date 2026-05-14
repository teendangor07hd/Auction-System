package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import com.bidhub.client.network.BidUpdateCallback;
import com.bidhub.client.network.EventListenerThread;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.service.BidChartService;
import com.bidhub.client.util.UiUtils; // THÊM IMPORT UiUtils
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller chi tiết phiên đấu giá.
 * Hỗ trợ Real-time updates qua Observer Pattern và Countdown Timer.
 */
public class AuctionDetailController implements ContextAware {

    // --- FXML Components ---
    @FXML private Label lblTitle;
    @FXML private Label lblItemName;
    @FXML private Label lblDescription;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblHighestBidder;
    @FXML private Label lblCountdown;
    @FXML private Label lblStatus;
    @FXML private TextField tfBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnBack;

    // 📌 [Tieu chi: UX — Loading state]
    @FXML private ProgressIndicator loadingSpinner;

    // 📌 [Tieu chi: Price Chart — FXML inject LineChart]
    @FXML
    private LineChart<String, Number> bidChart;

    // --- Logic Fields ---
    private String auctionId;
    private LocalDateTime endTime;
    private Timeline countdownTimeline;
    private final ObjectMapper mapper = new ObjectMapper();

    // Fields cho Real-time Event Listener
    private EventListenerThread eventListener;
    private boolean isSubscribed = false;

    // 📌 [Tieu chi: Price Chart — BidChartService quan ly du lieu chart]
    private BidChartService bidChartService;

    @Override
    public void setContext(Map<String, Object> params) {
        this.auctionId = (String) params.get("auctionId");
        if (auctionId != null && !auctionId.isBlank()) {
            loadAuctionDetail();
        }
    }

    @FXML
    public void initialize() {
        btnPlaceBid.setOnAction(e -> placeBid());
        btnBack.setOnAction(e -> {
            cleanup(); // Dọn dẹp tài nguyên trước khi thoát
            com.bidhub.client.navigation.ViewRouter.getInstance()
                    .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST);
        });

        // 📌 [Tieu chi: UX — TextField chi nhan so]
        UiUtils.applyNumericFilter(tfBidAmount);

        // 📌 [Tieu chi: Price Chart — khoi tao BidChartService va bind vao LineChart]
        bidChartService = new BidChartService();
        if (bidChart != null) {
            bidChart.getData().clear();
            bidChart.getData().add(bidChartService.getSeries());
            bidChart.setAnimated(false); // Tat animation de realtime update nhanh hon
        }
    }

    /**
     * Tải thông tin chi tiết phiên đấu giá từ Server.
     */
    private void loadAuctionDetail() {
        MessageRequest req = new MessageRequest();
        req.setType("GET_AUCTION_DETAIL");
        req.setPayload(mapper.createObjectNode().put("auctionId", auctionId));

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus()) && response.getPayload() != null) {
                JsonNode payload = mapper.valueToTree(response.getPayload());
                populateLabels(payload);
            } else {
                Platform.runLater(() -> UiUtils.showError("Lỗi hệ thống", response.getMessage()));
            }
        });

        task.setOnFailed(e ->
                Platform.runLater(() -> UiUtils.showError("Lỗi kết nối", "Không thể kết nối đến máy chủ.")));

        new Thread(task).start();
    }

    /**
     * Đổ dữ liệu vào UI và kích hoạt các bộ đếm/lắng nghe sự kiện.
     */
    private void populateLabels(JsonNode payload) {
        JsonNode auction = payload.path("auction");

        lblTitle.setText("Chi tiết phiên đấu giá");
        lblItemName.setText(auction.path("itemId").asText("Sản phẩm không tên"));
        lblDescription.setText(auction.path("description").asText("Không có mô tả."));

        lblStartingPrice.setText("Giá khởi điểm: " + auction.path("startingPrice").asDouble(0));
        lblCurrentPrice.setText("Giá hiện tại: " + auction.path("currentHighestBid").asDouble(0));
        lblHighestBidder.setText("Người dẫn đầu: " + auction.path("highestBidderId").asText("Chưa có"));

        String status = auction.path("status").asText("");
        lblStatus.setText("Trạng thái: " + status);

        // Xử lý Countdown
        String endTimeStr = auction.path("endTime").asText("");
        if (!endTimeStr.isEmpty()) {
            try {
                endTime = LocalDateTime.parse(endTimeStr);
                startCountdown();
            } catch (Exception ex) {
                lblCountdown.setText("Lỗi định dạng thời gian");
            }
        }

        // Nếu đã kết thúc thì disable form luôn
        if ("FINISHED".equals(status)) {
            disableBiddingUI();
        }

        // Kích hoạt Real-time (Observer pattern)
        subscribeRealtimeEvents();
    }

    /**
     * Đăng ký nhận sự kiện realtime từ server cho phiên đấu giá này.
     */
    private void subscribeRealtimeEvents() {
        if (isSubscribed) return;

        try {
            MessageRequest subReq = new MessageRequest();
            subReq.setType("SUBSCRIBE_AUCTION");
            subReq.setPayload(mapper.createObjectNode().put("auctionId", auctionId));

            MessageResponse subResp = ServerGateway.getInstance().sendRequest(subReq);
            if (!"OK".equals(subResp.getStatus())) {
                System.err.println("[AuctionDetail] Subscribe failed: " + subResp.getMessage());
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ServerGateway.getInstance().getSocket().getInputStream()));

            BidUpdateCallback callback = eventJson -> {
                try {
                    JsonNode eventNode = mapper.readTree(eventJson);
                    String eventType = eventNode.path("eventType").asText("");

                    Platform.runLater(() -> {
                        if ("BID_UPDATE".equals(eventType)) {
                            double newPrice = eventNode.path("bidAmount").asDouble(0);
                            String bidder = eventNode.path("bidderId").asText("Unknown");
                            lblCurrentPrice.setText("Giá hiện tại: " + newPrice);
                            lblHighestBidder.setText("Người dẫn đầu: " + bidder);

                            // 📌 [Tieu chi: Price Chart — realtime addDataPoint khi nhan BID_UPDATE]
                            bidChartService.addDataPoint(LocalDateTime.now(), newPrice);

                        } else if ("AUCTION_CLOSED".equals(eventType)) {
                            // 📌 [Tieu chi: UX — countdown dung khi auction dong]
                            disableBiddingUI();

                        } else if ("AUCTION_EXTENDED".equals(eventType)) {
                            // 📌 [Tieu chi: Anti-Sniping — reset countdown khi nhan AUCTION_EXTENDED]
                            String newEndTimeStr = eventNode.path("newEndTime").asText("");
                            if (!newEndTimeStr.isEmpty()) {
                                endTime = LocalDateTime.parse(newEndTimeStr);
                                startCountdown();
                            }
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[AuctionDetail] Callback error: " + e.getMessage());
                }
            };

            eventListener = new EventListenerThread(reader, callback);
            Thread thread = new Thread(eventListener, "EventListener-" + auctionId);
            thread.setDaemon(true);
            thread.start();

            isSubscribed = true;
            System.out.println("[AuctionDetail] Real-time subscription active.");
        } catch (Exception e) {
            System.err.println("[AuctionDetail] Subscribe error: " + e.getMessage());
        }
    }

    /**
     * Xử lý gửi yêu cầu đặt giá lên Server.
     */
    private void placeBid() {
        // Client-side validation
        if (!UiUtils.validateNotEmpty(tfBidAmount, "Số tiền đặt giá")) {
            return;
        }
        if (!UiUtils.validatePositiveNumber(tfBidAmount, "Số tiền đặt giá")) {
            return;
        }

        double bidAmount = Double.parseDouble(tfBidAmount.getText().trim());

        // Loading state
        Runnable onComplete = (loadingSpinner != null)
                ? UiUtils.showLoading(btnPlaceBid, loadingSpinner)
                : () -> btnPlaceBid.setDisable(false);

        MessageRequest req = new MessageRequest();
        req.setType("PLACE_BID");
        req.setPayload(mapper.createObjectNode()
                .put("auctionId", auctionId)
                .put("bidAmount", bidAmount));

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus())) {
                tfBidAmount.clear();
                UiUtils.showInfo("Đặt giá thành công", "Đã đặt giá "
                        + String.format("%,.0f VND", bidAmount));
            } else {
                // 📌 [Tieu chi: UX — hien Alert khi bid that bai]
                String errorMsg = response.getMessage();
                String userMsg = mapErrorMessage(errorMsg);
                UiUtils.showError("Đặt giá thất bại", userMsg);
            }
            onComplete.run();
        });

        task.setOnFailed(e -> {
            UiUtils.showError("Lỗi kết nối", "Không thể đặt giá. Kiểm tra kết nối mạng.");
            onComplete.run();
        });

        new Thread(task, "place-bid").start();
    }

    /**
     * Map server error code sang message thông thân thiện cho user.
     */
    private String mapErrorMessage(String errorCode) {
        if (errorCode == null) {
            return "Lỗi không xác định. Vui lòng thử lại.";
        }
        return switch (errorCode) {
            case "BID_TOO_LOW" -> "Giá đặt quá thấp. Vui lòng đặt giá cao hơn giá hiện tại + bước tăng tối thiểu.";
            case "AUCTION_NOT_RUNNING" -> "Phiên đấu giá đã kết thúc. Không thể đặt giá.";
            case "AUCTION_NOT_FOUND" -> "Phiên đấu giá không tồn tại.";
            case "CANNOT_BID_OWN_AUCTION" -> "Không thể đặt giá phiên đấu giá của chính bạn.";
            case "UNAUTHORIZED" -> "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
            default -> "Lỗi: " + errorCode + ". Vui lòng thử lại.";
        };
    }

    private void startCountdown() {
        stopCountdown();
        if (endTime == null) return;

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdown() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            disableBiddingUI();
            return;
        }
        java.time.Duration d = java.time.Duration.between(now, endTime);
        long hours = d.toHours();
        int minutes = d.toMinutesPart();
        int seconds = d.toSecondsPart();
        lblCountdown.setText(String.format("Còn lại: %02d:%02d:%02d", hours, minutes, seconds));
    }

    /**
     * Khóa UI khi phiên đấu giá kết thúc.
     */
    private void disableBiddingUI() {
        lblStatus.setText("Trạng thái: ĐÃ KẾT THÚC");
        lblCountdown.setText("ĐÃ KẾT THÚC");
        btnPlaceBid.setDisable(true);
        tfBidAmount.setDisable(true);
        if (loadingSpinner != null) {
            loadingSpinner.setVisible(false);
        }
        stopCountdown();
        stopEventListener();
    }

    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void stopEventListener() {
        if (eventListener != null) {
            eventListener.stop();
            eventListener = null;
        }
        isSubscribed = false;
    }

    @FXML
    public void cleanup() {
        stopCountdown();
        stopEventListener();
    }
}