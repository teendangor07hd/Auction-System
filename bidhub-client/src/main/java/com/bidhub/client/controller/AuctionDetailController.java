package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import com.bidhub.client.network.BidUpdateCallback;
import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.EventListenerThread;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.service.BidChartService; // Import service
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart; // Import LineChart
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

    // 📌 [Tieu chi: Price Chart — FXML inject LineChart]
    @FXML
    private LineChart<String, Number> bidChart;

    // --- Logic Fields ---
    private String auctionId;
    private LocalDateTime startTime;
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
                Platform.runLater(() -> showError(response.getMessage()));
            }
        });

        task.setOnFailed(e ->
                Platform.runLater(() -> showError("Không thể kết nối đến máy chủ.")));

        new Thread(task).start();
    }

    /**
     * Đổ dữ liệu vào UI và kích hoạt các bộ đếm/lắng nghe sự kiện.
     */
    private void populateLabels(JsonNode payload) {
        JsonNode auction = payload.path("auction");

        lblTitle.setText("Chi tiết phiên đấu giá");
        lblItemName.setText(auction.path("itemName").asText(auction.path("itemId").asText("Sản phẩm không tên")));
        lblDescription.setText(auction.path("description").asText("Không có mô tả."));

        lblStartingPrice.setText("Giá khởi điểm: " + auction.path("startingPrice").asDouble(0));
        lblCurrentPrice.setText("Giá hiện tại: " + auction.path("currentHighestBid").asDouble(0));
        lblHighestBidder.setText("Người dẫn đầu: " + auction.path("highestBidderName").asText(auction.path("highestBidderId").asText("Chưa có")));

        // 📌 [Tieu chi: Price Chart — load history data tu server de ve bieu do]
        JsonNode bidHistoryNode = payload.path("bidHistory");
        if (bidHistoryNode != null && bidHistoryNode.isArray()) {
            bidChartService.clearData();
            for (JsonNode bid : bidHistoryNode) {
                double amount = bid.path("bidAmount").asDouble(0);
                String timeStr = bid.path("bidTime").asText("");
                if (!timeStr.isEmpty()) {
                    try {
                        LocalDateTime time = LocalDateTime.parse(timeStr);
                        bidChartService.addDataPoint(time, amount);
                    } catch (Exception ignored) {}
                }
            }
        }

        String status = auction.path("status").asText("");
        lblStatus.setText("Trạng thái: " + status);

        // Xử lý Countdown
        String startTimeStr = auction.path("startTime").asText("");
        if (!startTimeStr.isEmpty()) {
            try {
                startTime = LocalDateTime.parse(startTimeStr);
            } catch (Exception ex) {}
        }
        
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

    private java.net.Socket eventSocket;

    /**
     * Đăng ký nhận sự kiện realtime từ server cho phiên đấu giá này.
     * Sử dụng một Socket ĐỘC LẬP để không tranh chấp InputStream với ServerGateway.
     */
    private void subscribeRealtimeEvents() {
        if (isSubscribed) return;

        try {
            // 1. Tao ket noi Socket doc lap cho realtime events
            String host = ServerGateway.getInstance().getServerHost();
            int port = ServerGateway.getInstance().getServerPort();
            eventSocket = new java.net.Socket(host, port);
            
            java.io.PrintWriter writer = new java.io.PrintWriter(eventSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));

            // 2. Gửi lệnh SUBSCRIBE lên server qua socket moi
            MessageRequest subReq = new MessageRequest();
            subReq.setType("SUBSCRIBE_AUCTION");
            subReq.setPayload(mapper.createObjectNode().put("auctionId", auctionId));
            writer.println(MessageMapper.toJson(subReq));

            // Doc response cho lenh SUBSCRIBE
            String responseLine = reader.readLine();
            MessageResponse subResp = MessageMapper.fromJson(responseLine, MessageResponse.class);
            if (!"OK".equals(subResp.getStatus())) {
                System.err.println("[AuctionDetail] Subscribe failed: " + subResp.getMessage());
                eventSocket.close();
                return;
            }

            // 3. Thiết lập Listener Thread để đọc stream từ Socket nay
            BidUpdateCallback callback = eventJson -> {
                try {
                    JsonNode eventNode = mapper.readTree(eventJson);
                    String eventType = eventNode.path("eventType").asText("");

                    // Luôn cập nhật UI bên trong Platform.runLater
                    Platform.runLater(() -> {
                        if ("BID_UPDATE".equals(eventType)) {
                            double newPrice = eventNode.path("bidAmount").asDouble(0);
                            String bidder = eventNode.path("bidderName").asText(eventNode.path("bidderId").asText("Unknown"));
                            lblCurrentPrice.setText("Giá hiện tại: " + newPrice);
                            lblHighestBidder.setText("Người dẫn đầu: " + bidder);

                            // 📌 [Tieu chi: Price Chart — realtime addDataPoint khi nhan BID_UPDATE]
                            bidChartService.addDataPoint(LocalDateTime.now(), newPrice);

                        } else if ("AUCTION_CLOSED".equals(eventType)) {
                            disableBiddingUI();

                        } else if ("AUCTION_EXTENDED".equals(eventType)) {
                            // 📌 [Tieu chi: Anti-Sniping — reset countdown khi nhan AUCTION_EXTENDED]
                            // Lay newEndTime tu event → cap nhat countdown
                            String newEndTimeStr = eventNode.path("newEndTime").asText("");
                            if (!newEndTimeStr.isEmpty()) {
                                endTime = LocalDateTime.parse(newEndTimeStr);
                                startCountdown(); // Khởi động lại timeline với thời gian mới
                            }
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[AuctionDetail] Callback error: " + e.getMessage());
                }
            };

            eventListener = new EventListenerThread(reader, callback);
            Thread thread = new Thread(eventListener, "EventListener-" + auctionId);
            thread.setDaemon(true); // Quan trọng: Thread sẽ tự tắt khi app đóng
            thread.start();

            isSubscribed = true;
            System.out.println("[AuctionDetail] Real-time subscription active via dedicated socket.");
        } catch (Exception e) {
            System.err.println("[AuctionDetail] Subscribe error: " + e.getMessage());
        }
    }

    private void placeBid() {
        String amountStr = tfBidAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showError("Vui lòng nhập số tiền muốn đấu giá.");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ex) {
            showError("Giá tiền không hợp lệ.");
            return;
        }

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
            if (!"OK".equals(response.getStatus())) {
                Platform.runLater(() -> showError(response.getMessage()));
            } else {
                tfBidAmount.clear();
                // Không cần loadAuctionDetail() lại vì đã có Real-time cập nhật
            }
        });

        task.setOnFailed(e -> Platform.runLater(() -> showError("Lỗi hệ thống khi đặt giá.")));
        new Thread(task).start();
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
        
        // Nếu chưa tới giờ bắt đầu
        if (startTime != null && now.isBefore(startTime)) {
            java.time.Duration d = java.time.Duration.between(now, startTime);
            long hours = d.toHours();
            int minutes = d.toMinutesPart();
            int seconds = d.toSecondsPart();
            lblCountdown.setText(String.format("Bắt đầu sau: %02d:%02d:%02d", hours, minutes, seconds));
            btnPlaceBid.setDisable(true); // Không cho phép đặt giá
            return;
        }

        // Đã bắt đầu, kiểm tra kết thúc
        if (now.isAfter(endTime)) {
            disableBiddingUI();
            return;
        }
        
        // Đang diễn ra
        btnPlaceBid.setDisable(false);
        java.time.Duration d = java.time.Duration.between(now, endTime);
        long hours = d.toHours();
        int minutes = d.toMinutesPart();
        int seconds = d.toSecondsPart();
        lblCountdown.setText(String.format("Còn lại: %02d:%02d:%02d", hours, minutes, seconds));
    }

    private void disableBiddingUI() {
        lblStatus.setText("Trạng thái: ĐÃ KẾT THÚC");
        lblCountdown.setText("ĐÃ KẾT THÚC");
        btnPlaceBid.setDisable(true);
        tfBidAmount.setDisable(true);
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Thông báo lỗi");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Dọn dẹp tài nguyên (Threads, Timelines) khi rời khỏi màn hình.
     */
    @FXML
    public void cleanup() {
        stopCountdown();
        stopEventListener();

        // Dong dedicated socket cho realtime events
        if (eventSocket != null && !eventSocket.isClosed()) {
            try {
                eventSocket.close();
            } catch (Exception e) {
                System.err.println("[AuctionDetail] Loi dong eventSocket: " + e.getMessage());
            }
        }
    }
}