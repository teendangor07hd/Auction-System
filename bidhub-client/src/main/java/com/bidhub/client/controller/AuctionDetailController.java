package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import com.bidhub.client.network.BidUpdateCallback;
import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.EventListenerThread;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.service.BidChartService;
import com.bidhub.client.util.UiUtils;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    @FXML
    private Label lblTitle;
    @FXML
    private Label lblItemName;
    @FXML
    private Label lblDescription;
    @FXML
    private ImageView imgProduct;
    @FXML
    private Label lblStartingPrice;
    @FXML
    private Label lblCurrentPrice;
    @FXML
    private Label lblHighestBidder;
    @FXML
    private Label lblCountdown;
    @FXML
    private Label lblStatus;
    @FXML
    private TextField tfBidAmount;
    @FXML
    private Button btnPlaceBid;
    @FXML
    private Button btnBack;

    // 📌 [Tieu chi: UX — Loading state]
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private ScrollPane chartScrollPane;

    // 📌 [Tieu chi: Price Chart — FXML inject LineChart]
    @FXML
    private LineChart<String, Number> bidChart;

    @FXML
    private TableView<JsonNode> bidTable;
    @FXML
    private TableColumn<JsonNode, String> colBidderName;
    @FXML
    private TableColumn<JsonNode, String> colBidAmount;

    // --- Logic Fields ---
    private final ObservableList<JsonNode> bidData = FXCollections.observableArrayList();
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

        // Setup bảng xếp hạng
        colBidderName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().path("bidderName").asText("Unknown")));
        colBidAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                String.format("%,.0f", cellData.getValue().path("bidAmount").asDouble(0))));
        bidTable.setItems(bidData);

        // 📌 [Tieu chi: UX — TextField chi nhan so]
        UiUtils.applyNumericFilter(tfBidAmount);

        // 📌 [Tieu chi: Price Chart — khoi tao BidChartService va bind vao LineChart]
        bidChartService = new BidChartService();
        if (bidChart != null) {
            bidChart.getData().clear();
            bidChart.getData().add(bidChartService.getSeries());
            bidChart.setAnimated(false);
            bidChart.setCreateSymbols(true);
        }

        // Tooltip cho bảng xếp hạng
        setupLeaderboardTooltips();
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
                Platform.runLater(() -> UiUtils.showError("Lỗi hệ thống", response.getMessage()));
            }
        });

        task.setOnFailed(
                e -> Platform.runLater(() -> UiUtils.showError("Lỗi kết nối", "Không thể kết nối đến máy chủ.")));

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

        String imageUrl = auction.path("imageUrl").asText("");
        if (!imageUrl.isEmpty()) {
            try {
                Image img = new Image(imageUrl, true);
                if (imgProduct != null) {
                    imgProduct.setImage(img);
                }
            } catch (Exception e) {
                System.err.println("Cannot load image: " + imageUrl);
            }
        } else {
            // Hình ảnh mặc định nếu không có
            if (imgProduct != null) {
                imgProduct.setImage(new Image("https://via.placeholder.com/200x150?text=No+Image", true));
            }
        }

        lblStartingPrice.setText(String.format("%,.0f đ", auction.path("startingPrice").asDouble(0)));
        lblCurrentPrice.setText(String.format("%,.0f đ", auction.path("currentHighestBid").asDouble(0)));
        lblHighestBidder
                .setText(auction.path("highestBidderName").asText(auction.path("highestBidderId").asText("Chưa có")));

        // 📌 [Tieu chi: Price Chart — load history data tu server de ve bieu do]
        JsonNode bidHistoryNode = payload.path("bidHistory");
        bidChartService.clearData();
        bidData.clear();

        try {
            String startTimeRaw = auction.path("startTime").asText("");
            if (!startTimeRaw.isEmpty()) {
                LocalDateTime startDT = LocalDateTime.parse(startTimeRaw);
                double sPrice = auction.path("startingPrice").asDouble(0);
                bidChartService.addDataPoint(startDT, sPrice, "Giá khởi điểm");
            }
        } catch (Exception ignored) {
        }

        if (bidHistoryNode != null && bidHistoryNode.isArray()) {
            java.util.List<JsonNode> bids = new java.util.ArrayList<>();
            for (JsonNode bid : bidHistoryNode) {
                bids.add(bid);
                double amount = bid.path("bidAmount").asDouble(0);
                String timeStr = bid.path("bidTime").asText("");
                String bidderName = bid.path("bidderName").asText("Unknown");
                if (!timeStr.isEmpty()) {
                    try {
                        LocalDateTime time = LocalDateTime.parse(timeStr);
                        bidChartService.addDataPoint(time, amount, bidderName);
                    } catch (Exception ignored) {
                    }
                }
            }
            // Sort bids by amount descending for the leaderboard
            bids.sort((b1, b2) -> Double.compare(b2.path("bidAmount").asDouble(0), b1.path("bidAmount").asDouble(0)));
            bidData.addAll(bids);
        }

        String status = auction.path("status").asText("");
        String statusVN = switch (status) {
            case "PENDING" -> "Chờ bắt đầu";
            case "RUNNING" -> "Đang diễn ra";
            case "FINISHED" -> "Đã kết thúc";
            case "CLOSED" -> "Đã kết thúc";
            default -> status;
        };
        lblStatus.setText(statusVN);

        // Xử lý Countdown
        String startTimeStr = auction.path("startTime").asText("");
        if (!startTimeStr.isEmpty()) {
            try {
                startTime = LocalDateTime.parse(startTimeStr);
            } catch (Exception ex) {
            }
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
        if (isSubscribed)
            return;

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

                    Platform.runLater(() -> {
                        if ("BID_UPDATE".equals(eventType)) {
                            double newPrice = eventNode.path("bidAmount").asDouble(0);
                            String bidder = eventNode.path("bidderName")
                                    .asText(eventNode.path("bidderId").asText("Unknown"));
                            lblCurrentPrice.setText(String.format("%,.0f đ", newPrice));
                            lblHighestBidder.setText(bidder);

                            // 📌 [Tieu chi: Price Chart — realtime addDataPoint khi nhan BID_UPDATE]
                            bidChartService.addDataPoint(LocalDateTime.now(), newPrice, bidder);

                            // Them vao bang xep hang va sap xep
                            bidData.add(eventNode);
                            FXCollections.sort(bidData, (b1, b2) -> Double.compare(b2.path("bidAmount").asDouble(0),
                                    b1.path("bidAmount").asDouble(0)));

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
            System.out.println("[AuctionDetail] Real-time subscription active via dedicated socket.");
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
        if (endTime == null)
            return;

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

    /**
     * Khóa UI khi phiên đấu giá kết thúc.
     */
    private void disableBiddingUI() {
        lblStatus.setText("ĐÃ KẾT THÚC");
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

    // =====================================================================
    // Leaderboard Tooltip
    // =====================================================================

    /**
     * Gắn Tooltip cho cột bảng xếp hạng.
     * Khi nội dung bị cắt bớt sẽ hiện tooltip đầy đủ khi hover.
     */
    private void setupLeaderboardTooltips() {
        if (colBidderName != null) {
            colBidderName.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        Tooltip tp = new Tooltip(item);
                        tp.setStyle("-fx-font-size: 13px; -fx-padding: 6 10;");
                        setTooltip(tp);
                    }
                }
            });
        }
        if (colBidAmount != null) {
            colBidAmount.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        Tooltip tp = new Tooltip(item + " đồng");
                        tp.setStyle("-fx-font-size: 13px; -fx-padding: 6 10;");
                        setTooltip(tp);
                    }
                }
            });
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