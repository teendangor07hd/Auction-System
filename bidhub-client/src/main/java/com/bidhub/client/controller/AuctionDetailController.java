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
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
    @FXML private Label lblTitle;
    @FXML private Label lblItemName;
    @FXML private Label lblDescription;
    @FXML private ImageView imgProduct;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblMinIncrement;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblHighestBidder;
    @FXML private Label lblCountdown;
    @FXML private Label lblStatus;
    @FXML private TextField tfBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnBack;

    // 📌 [Tieu chi: UX — Loading state]
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private ScrollPane chartScrollPane;

    // Nút zoom đồ thị
    @FXML private Button btnZoomIn;
    @FXML private Button btnZoomOut;
    @FXML private Button btnZoomReset;

    // 📌 [Tieu chi: Price Chart — FXML inject LineChart]
    @FXML
    private LineChart<String, Number> bidChart;



    @FXML private TableView<JsonNode> bidTable;
    @FXML private TableColumn<JsonNode, String> colBidderName;
    @FXML private TableColumn<JsonNode, String> colBidAmount;
    @FXML private TableColumn<JsonNode, String> colBidTime;

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
            if (com.bidhub.client.network.ClientSession.getInstance().isLoggedIn()) {
                com.bidhub.client.navigation.ViewRouter.getInstance()
                        .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST);
            } else {
                com.bidhub.client.navigation.ViewRouter.getInstance()
                        .navigateTo(com.bidhub.client.util.Views.LOGIN);
            }
        });

        // Setup bảng xếp hạng
        colBidderName.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().path("bidderName").asText("Unknown")));
        colBidAmount.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("%,.0f VND", cellData.getValue().path("bidAmount").asDouble(0))));
        colBidTime.setCellValueFactory(cellData -> {
            String rawTime = cellData.getValue().path("bidTime").asText("");
            if (rawTime.isEmpty()) return new javafx.beans.property.SimpleStringProperty("—");
            try {
                LocalDateTime ldt = LocalDateTime.parse(rawTime);
                return new javafx.beans.property.SimpleStringProperty(ldt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM")));
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty(rawTime);
            }
        });
        bidTable.setItems(bidData);

        // 📌 [Tieu chi: UX — TextField chi nhan so]
        UiUtils.applyNumericFilter(tfBidAmount);

        // 📌 [Tieu chi: Price Chart — khoi tao BidChartService va bind vao LineChart]
        bidChartService = new BidChartService();
        if (bidChart != null) {
            bidChart.getData().clear();
            bidChart.getData().add(bidChartService.getSeries());
            bidChart.setAnimated(false); // Tắt animation để realtime update nhanh hơn
            bidChart.setCreateSymbols(true);
            if (bidChart.getYAxis() instanceof javafx.scene.chart.NumberAxis yAxis) {
                yAxis.setForceZeroInRange(false);
            }
            if (bidChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis xAxis) {
                xAxis.setTickLabelRotation(0);
            }
        }

        // Nút zoom đồ thị
        if (btnZoomIn != null) {
            btnZoomIn.setOnAction(e -> zoomChart(1.35));
        }
        if (btnZoomOut != null) {
            btnZoomOut.setOnAction(e -> zoomChart(1.0 / 1.35));
        }
        if (btnZoomReset != null) {
            btnZoomReset.setOnAction(e -> resetChartZoom());
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

        lblStartingPrice.setText(String.format("%,.0f VND", auction.path("startingPrice").asDouble(0)));
        lblMinIncrement.setText(String.format("%,.0f VND", auction.path("minimumIncrement").asDouble(0)));
        lblCurrentPrice.setText(String.format("%,.0f VND", auction.path("currentHighestBid").asDouble(0)));
        lblHighestBidder.setText(auction.path("highestBidderName").asText(auction.path("highestBidderId").asText("Chưa có")));

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
        } catch (Exception ignored) {}

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
                    } catch (Exception ignored) {}
                }
            }
            // Sort bids by amount descending for the leaderboard
            bids.sort((b1, b2) -> Double.compare(b2.path("bidAmount").asDouble(0), b1.path("bidAmount").asDouble(0)));
            bidData.addAll(bids);
        }

        // Cập nhật kích thước biểu đồ tối ưu
        updateChartWidth();

        String status = auction.path("status").asText("");
        String statusVN = switch (status) {
            case "OPEN" -> { lblStatus.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;"); yield "Chờ bắt đầu"; }
            case "RUNNING" -> { lblStatus.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;"); yield "Đang diễn ra"; }
            case "FINISHED", "CLOSED" -> { lblStatus.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;"); yield "Đã kết thúc"; }
            default -> { lblStatus.setStyle("-fx-text-fill: #334155; -fx-font-weight: bold;"); yield status; }
        };
        lblStatus.setText(statusVN);

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
        if ("FINISHED".equals(status) || "CLOSED".equals(status)) {
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
            subReq.setToken(ClientSession.getInstance().getToken());
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
                            String bidder = eventNode.path("bidderName").asText(eventNode.path("bidderId").asText("Unknown"));
                            lblCurrentPrice.setText(String.format("%,.0f VND", newPrice));
                            lblHighestBidder.setText(bidder);

                            // Hiệu ứng nháy sáng (Flash) khi có bid mới
                            lblCurrentPrice.setStyle("-fx-background-color: #FEF08A; -fx-text-fill: #B45309; -fx-padding: 2 8; -fx-background-radius: 4;");
                            new Timeline(new KeyFrame(Duration.millis(600), k -> lblCurrentPrice.setStyle(""))).play();

                            // Đảm bảo eventNode có bidTime để hiển thị cột thời gian đồng nhất trên bảng xếp hạng
                            if (eventNode instanceof com.fasterxml.jackson.databind.node.ObjectNode objNode) {
                                String ts = eventNode.path("timestamp").asText("");
                                if (ts.isEmpty()) {
                                    ts = LocalDateTime.now().toString();
                                }
                                objNode.put("bidTime", ts);
                            }

                            // 📌 [Tieu chi: Price Chart — realtime addDataPoint khi nhan BID_UPDATE]
                            bidChartService.addDataPoint(LocalDateTime.now(), newPrice, bidder);

                            // Cập nhật kích thước biểu đồ khi có điểm mới
                            updateChartWidth();

                            // Them vao bang xep hang va sap xep
                            bidData.add(eventNode);
                            FXCollections.sort(bidData, (b1, b2) -> Double.compare(b2.path("bidAmount").asDouble(0), b1.path("bidAmount").asDouble(0)));

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
        if (!ClientSession.getInstance().isLoggedIn()) {
            UiUtils.showError("Yêu cầu đăng nhập", "Bạn cần đăng nhập để thực hiện đặt giá.");
            return;
        }

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
                // 🎉 Hiệu ứng pháo hoa khi đặt giá thành công
                Platform.runLater(this::launchFireworks);
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
        if (errorCode.contains("Gia dat phai cao hon") || errorCode.contains("Buoc gia toi thieu") || errorCode.contains("BID_TOO_LOW")) {
            return "Giá đặt quá thấp. Vui lòng đặt giá cao hơn giá hiện tại + bước tăng tối thiểu.";
        }
        if (errorCode.contains("Ban dang la nguoi dan dau")) {
            return "Bạn đang là người dẫn đầu phiên đấu giá này.";
        }
        if (errorCode.contains("Seller khong the tu dau gia") || errorCode.contains("CANNOT_BID_OWN_AUCTION")) {
            return "Không thể đặt giá phiên đấu giá của chính bạn.";
        }
        if (errorCode.contains("chưa bắt đầu")) {
            return "Phiên đấu giá chưa bắt đầu. Vui lòng chờ đến giờ.";
        }
        if (errorCode.contains("đã kết thúc") || errorCode.contains("AUCTION_NOT_RUNNING")) {
            return "Phiên đấu giá đã kết thúc. Không thể đặt giá.";
        }
        if (errorCode.contains("AUCTION_NOT_FOUND")) {
            return "Phiên đấu giá không tồn tại.";
        }
        if (errorCode.contains("UNAUTHORIZED")) {
            return "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
        }
        return errorCode;
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
            lblCountdown.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
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
        if (d.toSeconds() < 60) {
            lblCountdown.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        } else {
            lblCountdown.setStyle("-fx-text-fill: #334155; -fx-font-weight: bold;");
        }
    }

    /**
     * Khóa UI khi phiên đấu giá kết thúc.
     */
    private void disableBiddingUI() {
        lblStatus.setText("ĐÃ KẾT THÚC");
        lblStatus.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
        lblCountdown.setText("ĐÃ KẾT THÚC");
        lblCountdown.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
        btnPlaceBid.setDisable(true);
        tfBidAmount.setDisable(true);
        if (loadingSpinner != null) {
            loadingSpinner.setVisible(false);
        }
        if (!lblHighestBidder.getText().contains("🏆")) {
            lblHighestBidder.setText("🏆 " + lblHighestBidder.getText());
            lblHighestBidder.setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold; -fx-font-size: 16px;");
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

    /** Phóng to đồ thị theo factor cho trước. */
    @FXML
    public void handleZoomIn() {
        zoomChart(1.35);
    }

    /** Thu nhỏ đồ thị. */
    @FXML
    public void handleZoomOut() {
        zoomChart(1.0 / 1.35);
    }

    /** Đặt lại kích thước đồ thị về mặc định. */
    @FXML
    public void handleZoomReset() {
        resetChartZoom();
    }

    /**
     * Điều chỉnh chiều cao đồ thị theo factor — zoom theo chiều dọc
     * để không đẩy sang bảng xếp hạng.
     */
    private void zoomChart(double factor) {
        if (bidChart == null) return;
        double currentH = bidChart.getPrefHeight();
        if (currentH <= 0 || currentH == javafx.scene.layout.Region.USE_COMPUTED_SIZE) {
            currentH = 290.0;
        }
        double newH = currentH * factor;
        // Giới hạn zoom: tối thiểu 200px, tối đa 1200px
        newH = Math.max(200, Math.min(1200, newH));
        bidChart.setPrefHeight(newH);
        if (chartScrollPane != null) {
            chartScrollPane.setPrefHeight(newH + 30);
        }
    }

    private void resetChartZoom() {
        if (bidChart != null) {
            bidChart.setPrefHeight(290.0);
        }
        if (chartScrollPane != null) {
            chartScrollPane.setPrefHeight(320);
        }
    }

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

    /**
     * Hiệu ứng pháo hoa / confetti khi đặt giá thành công.
     * Dùng nhiều Label emoji bay lên và mờ dần.
     */
    private void launchFireworks() {
        javafx.scene.layout.Pane overlay = null;
        try {
            // Tìm root pane để đặt overlay
            if (btnPlaceBid != null && btnPlaceBid.getScene() != null) {
                javafx.scene.Parent root = btnPlaceBid.getScene().getRoot();
                if (root instanceof javafx.scene.layout.Pane pane) {
                    overlay = pane;
                } else if (root instanceof javafx.scene.layout.BorderPane bp && bp.getCenter() instanceof javafx.scene.layout.Pane p) {
                    overlay = p;
                }
            }
        } catch (Exception ignored) {}
        if (overlay == null) return;

        final javafx.scene.layout.Pane finalOverlay = overlay;
        String[] emojis = {"🎉", "🎊", "✨", "🏆", "🥇", "⭐", "💫", "🎯"};
        java.util.Random rand = new java.util.Random();
        double width = finalOverlay.getWidth() > 0 ? finalOverlay.getWidth() : 800;

        for (int i = 0; i < 20; i++) {
            final int idx = i;
            javafx.scene.control.Label lbl = new javafx.scene.control.Label(emojis[rand.nextInt(emojis.length)]);
            lbl.setStyle("-fx-font-size: " + (20 + rand.nextInt(20)) + "px;");
            double x = rand.nextDouble() * width;
            lbl.setLayoutX(x);
            lbl.setLayoutY(finalOverlay.getHeight() > 0 ? finalOverlay.getHeight() - 50 : 500);
            finalOverlay.getChildren().add(lbl);

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    Duration.millis(1000 + rand.nextInt(800)), lbl);
            tt.setByY(-(200 + rand.nextInt(200)));
            tt.setByX((rand.nextDouble() - 0.5) * 200);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                    Duration.millis(1000 + rand.nextInt(800)), lbl);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);

            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(lbl, tt, ft);
            pt.setDelay(Duration.millis(idx * 60));
            pt.setOnFinished(e2 -> finalOverlay.getChildren().remove(lbl));
            pt.play();
        }
    }

    private void updateChartWidth() {
        if (bidChart != null && bidChartService != null) {
            int numPoints = bidChartService.getSeries().getData().size();
            double computedWidth = Math.max(700, numPoints * 80);
            bidChart.setPrefWidth(computedWidth);
            bidChart.setMinWidth(computedWidth);
            
            // Auto scroll to the newest bids on the right
            Platform.runLater(() -> {
                if (chartScrollPane != null) {
                    chartScrollPane.setHvalue(1.0);
                }
            });
        }
    }
}
