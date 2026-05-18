package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import com.bidhub.client.navigation.Navigable;
import com.bidhub.client.network.BidUpdateCallback;
import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.EventListenerThread;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.service.BidChartService;
import com.bidhub.client.util.UiUtils;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller chi tiết phiên đấu giá.
 * Hỗ trợ Real-time updates qua Observer Pattern và Countdown Timer.
 *
 * <p>// 📌 [B13] Implement {@link Navigable} → ViewRouter gọi onNavigateAway() khi rời màn hình.
 * // 📌 [B14] subscribeRealtimeEvents() chạy trên background thread (NetworkTask), không chạy trên FX thread.
 * // 📌 [B15] flashTimeline lưu reference → stop trong cleanup(), không leak.
 * // 📌 [B16] validate bid-against-self client-side trước khi gửi request.
 */
public class AuctionDetailController implements ContextAware, Navigable {

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
    @FXML private LineChart<String, Number> bidChart;

    // --- Logic Fields ---
    private String auctionId;
    private LocalDateTime endTime;
    private Timeline countdownTimeline;

    // [B15] flashTimeline — lưu reference để stop/cleanup tránh leak
    private Timeline flashTimeline;

    private final ObjectMapper mapper = new ObjectMapper();

    // Fields cho Real-time Event Listener
    private EventListenerThread eventListener;
    private Thread eventThread;
    private Socket eventSocket; // socket riêng cho event (B14 — tách khỏi request socket)
    private boolean isSubscribed = false;

    // 📌 [B16] lưu highestBidderId để validate bid-against-self
    private String highestBidderId = "";
    private String sellerId = "";

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
            onNavigateAway(); // Dọn dẹp tài nguyên trước khi thoát
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
     * [B13] Lifecycle hook — gọi bởi ViewRouter khi navigate rời khỏi màn hình này.
     * Dọn dẹp tất cả tài nguyên: countdown, event socket, event thread, flash timeline.
     */
    @Override
    public void onNavigateAway() {
        cleanup();
    }

    /**
     * Tải thông tin chi tiết phiên đấu giá từ Server.
     */
    private void loadAuctionDetail() {
        MessageRequest req = new MessageRequest();
        req.setType("GET_AUCTION_DETAIL");
        req.setToken(ClientSession.getInstance().getToken());
        req.setPayload(mapper.createObjectNode().put("auctionId", auctionId));

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus()) && response.getPayload() != null) {
                JsonNode payload = mapper.valueToTree(response.getPayload());
                populateLabels(payload);
            } else {
                UiUtils.showError("Lỗi hệ thống", response.getMessage());
            }
        });

        task.setOnFailed(e ->
                UiUtils.showError("Lỗi kết nối", "Không thể kết nối đến máy chủ."));

        new Thread(task, "load-auction-detail").start();
    }

    /**
     * Đổ dữ liệu vào UI và kích hoạt các bộ đếm/lắng nghe sự kiện.
     */
    private void populateLabels(JsonNode payload) {
        JsonNode auction = payload.path("auction");

        // [B16] Lưu sellerId và highestBidderId để validate trước khi đặt giá
        this.sellerId = auction.path("sellerId").asText("");
        this.highestBidderId = auction.path("highestBidderId").asText("");

        lblTitle.setText("Chi tiết phiên đấu giá");
        lblItemName.setText(auction.path("itemName").asText(
                auction.path("itemId").asText("Sản phẩm không tên")));
        lblDescription.setText(auction.path("description").asText("Không có mô tả."));

        double startingPrice = auction.path("startingPrice").asDouble(0);
        double currentBid = auction.path("currentHighestBid").asDouble(0);
        lblStartingPrice.setText("Giá khởi điểm: " + UiUtils.formatCurrency(startingPrice));
        lblCurrentPrice.setText("Giá hiện tại: " + UiUtils.formatCurrency(currentBid));
        lblHighestBidder.setText("Người dẫn đầu: " + (highestBidderId.isEmpty() ? "Chưa có" : highestBidderId));

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
        if ("FINISHED".equals(status) || "CANCELED".equals(status) || "PAID".equals(status)) {
            disableBiddingUI();
            return;
        }

        // [B14] subscribeRealtimeEvents() phải chạy trên background thread — KHÔNG chạy trên FX thread
        new Thread(this::subscribeRealtimeEvents, "event-subscribe-" + auctionId).start();
    }

    /**
     * Đăng ký nhận sự kiện realtime từ server cho phiên đấu giá này.
     *
     * <p>// 📌 [B14] Method này chạy trên background thread — tránh freeze FX thread
     * vì connect TCP là blocking operation.
     * <p>// 📌 [GAP4] Gửi token qua event socket để server biết ai subscribe.
     */
    private void subscribeRealtimeEvents() {
        if (isSubscribed) return;

        try {
            // [B14] Tạo socket riêng cho event (không dùng request socket của ServerGateway)
            // Điều này tránh race condition với sendRequest()
            ServerGateway gw = ServerGateway.getInstance();
            eventSocket = new Socket();
            eventSocket.connect(
                    new java.net.InetSocketAddress(gw.getServerHost(), gw.getServerPort()), 5000);
            eventSocket.setSoTimeout(0); // Không timeout — stream sẽ mở mãi

            java.io.PrintWriter eventWriter = new java.io.PrintWriter(eventSocket.getOutputStream(), true);
            BufferedReader eventReader = new BufferedReader(
                    new InputStreamReader(eventSocket.getInputStream()));

            // [GAP4] Gửi token để server xác định người subscribe
            MessageRequest subReq = new MessageRequest();
            subReq.setType("SUBSCRIBE_AUCTION");
            subReq.setToken(ClientSession.getInstance().getToken());
            subReq.setPayload(mapper.createObjectNode().put("auctionId", auctionId));
            eventWriter.println(com.bidhub.common.network.MessageMapper.toJson(subReq));

            // Đọc response subscribe (bỏ qua nội dung — chỉ cần không crash)
            String subResponse = eventReader.readLine();
            if (subResponse == null) {
                System.err.println("[AuctionDetail] Event socket đóng sau khi subscribe.");
                return;
            }

            BidUpdateCallback callback = eventJson -> {
                try {
                    JsonNode eventNode = mapper.readTree(eventJson);
                    String eventType = eventNode.path("eventType").asText("");

                    Platform.runLater(() -> {
                        if ("BID_UPDATE".equals(eventType)) {
                            double newPrice = eventNode.path("bidAmount").asDouble(0);
                            String bidder = eventNode.path("bidderId").asText("Unknown");
                            highestBidderId = bidder; // cập nhật để validate tiếp theo
                            lblCurrentPrice.setText("Giá hiện tại: " + UiUtils.formatCurrency(newPrice));
                            lblHighestBidder.setText("Người dẫn đầu: " + bidder);

                            // [B15] flashTimeline — lưu reference, không tạo mới vô hạn
                            flashPriceLabel();

                            // 📌 [Tieu chi: Price Chart — realtime addDataPoint khi nhan BID_UPDATE]
                            bidChartService.addDataPoint(LocalDateTime.now(), newPrice);

                        } else if ("AUCTION_CLOSED".equals(eventType)) {
                            // 📌 [Tieu chi: UX — countdown dung khi auction dong]
                            disableBiddingUI();

                        } else if ("AUCTION_EXTENDED".equals(eventType)) {
                            // 📌 [Tieu chi: Anti-Sniping — reset countdown khi nhan AUCTION_EXTENDED]
                            String newEndTimeStr = eventNode.path("newEndTime").asText("");
                            if (!newEndTimeStr.isEmpty()) {
                                try {
                                    endTime = LocalDateTime.parse(newEndTimeStr);
                                    startCountdown();
                                } catch (Exception ex) {
                                    System.err.println("[AuctionDetail] Parse newEndTime loi: " + ex.getMessage());
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[AuctionDetail] Callback error: " + e.getMessage());
                }
            };

            eventListener = new EventListenerThread(eventReader, callback);
            eventThread = new Thread(eventListener, "EventListener-" + auctionId);
            eventThread.setDaemon(true);
            eventThread.start();

            isSubscribed = true;
            System.out.println("[AuctionDetail] Real-time subscription active.");
        } catch (Exception e) {
            System.err.println("[AuctionDetail] Subscribe error: " + e.getMessage());
        }
    }

    /**
     * Hiệu ứng flash màu xanh khi giá mới được cập nhật.
     *
     * <p>// 📌 [B15] Lưu reference flashTimeline → stop() trước khi tạo mới để không leak.
     */
    private void flashPriceLabel() {
        // [B15] Stop timeline cũ nếu đang chạy
        if (flashTimeline != null) {
            flashTimeline.stop();
        }

        String originalStyle = lblCurrentPrice.getStyle();
        lblCurrentPrice.setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold; -fx-font-size: 16px;");

        flashTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1.5), ev ->
                        lblCurrentPrice.setStyle(originalStyle)
                )
        );
        flashTimeline.setCycleCount(1);
        flashTimeline.play();
    }

    /**
     * Xử lý gửi yêu cầu đặt giá lên Server.
     *
     * <p>// 📌 [B16] Validate bid-against-self client-side trước khi gửi request.
     */
    private void placeBid() {
        // Client-side validation
        if (!UiUtils.validateNotEmpty(tfBidAmount, "Số tiền đặt giá")) {
            return;
        }
        if (!UiUtils.validatePositiveNumber(tfBidAmount, "Số tiền đặt giá")) {
            return;
        }

        // [B16] Validate bid-against-self — không để gửi rồi server reject, UX kém
        String currentUserId = ClientSession.getInstance().getCurrentUserId();
        if (!sellerId.isEmpty() && sellerId.equals(currentUserId)) {
            UiUtils.showError("Không thể đặt giá", "Bạn không thể đặt giá phiên đấu giá của chính mình.");
            return;
        }
        if (!highestBidderId.isEmpty() && highestBidderId.equals(currentUserId)) {
            UiUtils.showError("Không thể đặt giá", "Bạn đang là người trả giá cao nhất. Hãy chờ người khác đặt giá.");
            return;
        }

        double bidAmount = Double.parseDouble(tfBidAmount.getText().trim());

        // Loading state
        Runnable onComplete = (loadingSpinner != null)
                ? UiUtils.showLoading(btnPlaceBid, loadingSpinner)
                : () -> btnPlaceBid.setDisable(false);

        MessageRequest req = new MessageRequest();
        req.setType("PLACE_BID");
        req.setToken(ClientSession.getInstance().getToken());
        req.setPayload(mapper.createObjectNode()
                .put("auctionId", auctionId)
                .put("bidAmount", bidAmount));

        NetworkTask<MessageResponse> task = new NetworkTask<>(
                () -> ServerGateway.getInstance().sendRequest(req));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if ("OK".equals(response.getStatus())) {
                tfBidAmount.clear();
                UiUtils.showInfo("Đặt giá thành công", "Đã đặt giá " + UiUtils.formatCurrency(bidAmount));
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
     * Map server error code sang message thân thiện cho user.
     */
    private String mapErrorMessage(String errorCode) {
        if (errorCode == null) {
            return "Lỗi không xác định. Vui lòng thử lại.";
        }
        return switch (errorCode) {
            case "BID_TOO_LOW"         -> "Giá đặt quá thấp. Vui lòng đặt giá cao hơn giá hiện tại + bước tăng tối thiểu.";
            case "AUCTION_NOT_RUNNING" -> "Phiên đấu giá đã kết thúc. Không thể đặt giá.";
            case "AUCTION_NOT_FOUND"   -> "Phiên đấu giá không tồn tại.";
            case "CANNOT_BID_OWN_AUCTION" -> "Không thể đặt giá phiên đấu giá của chính bạn.";
            case "UNAUTHORIZED"        -> "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
            default                    -> "Lỗi: " + errorCode + ". Vui lòng thử lại.";
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

    // [B15] Dừng flash timeline
    private void stopFlashTimeline() {
        if (flashTimeline != null) {
            flashTimeline.stop();
            flashTimeline = null;
        }
    }

    private void stopEventListener() {
        if (eventListener != null) {
            eventListener.stop(); // [B10] stop() giờ đóng reader → interrupt readLine()
            eventListener = null;
        }
        if (eventThread != null) {
            eventThread.interrupt();
            eventThread = null;
        }
        // Đóng event socket
        if (eventSocket != null && !eventSocket.isClosed()) {
            try { eventSocket.close(); } catch (IOException ignored) {}
            eventSocket = null;
        }
        isSubscribed = false;
    }

    /**
     * Dọn dẹp tất cả tài nguyên. Gọi từ {@link #onNavigateAway()} và btnBack handler.
     */
    @FXML
    public void cleanup() {
        stopCountdown();
        stopFlashTimeline(); // [B15]
        stopEventListener(); // [B13] + [B10]
    }
}