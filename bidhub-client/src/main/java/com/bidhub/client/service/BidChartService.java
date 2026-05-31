package com.bidhub.client.service;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service quản lý du lieu bieu do gia đấu giá realtime.
 *
 * <p>Đúng {@link XYChart.Series} để lưu trữ data point (thoi gian, gia).
 * Method {@link #addDataPoint(LocalDateTime, double)} format time thanh
 * string HH:mm:ss và tạo data point moi.
 *
 *
 * @author Công Minh
 */
public final class BidChartService {

    private final XYChart.Series<String, Number> series;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private int pointIndex = 0;

    /**
     * Tạo BidChartService — khởi tạo series với ten "Lịch sử giá".
     *
     */
    public BidChartService() {
        this.series = new XYChart.Series<>();
        this.series.setName("Lịch sử giá");
    }

    /**
     * Thêm data point moi vao series — thoi gian và gia.
     *
     * <p>Format LocalDateTime thanh string HH:mm:ss để hien thi tren trục X.
     *
     *
     * @param time  thoi gian đặt giá
     * @param price gia dat
     */
    public void addDataPoint(LocalDateTime time, double price) {
        addDataPoint(time, price, "Khách");
    }

    /**
     * Thêm data point moi vao series — thoi gian, gia, và ten nguoi đặt giá.
     *
     * @param time       thoi gian đặt giá
     * @param price      gia dat
     * @param bidderName ten nguoi đặt giá
     */
    public void addDataPoint(LocalDateTime time, double price, String bidderName) {
        String timeStr = time.format(TIME_FORMATTER);
        String labelStr;

        // Tránh chồng chéo bằng cách chỉ hiển thị nhãn chữ cho mỗi 7 điểm dữ liệu,
        // các điểm khác dùng số lượng ký tự zero-width space duy nhất để JavaFX không gộp nhóm coordinate.
        if (pointIndex == 0 || bidderName.equals("Giá khởi điểm") || pointIndex % 7 == 0) {
            StringBuilder sb = new StringBuilder(timeStr);
            for (int i = 0; i < pointIndex; i++) {
                sb.append("\u200B");
            }
            labelStr = sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pointIndex; i++) {
                sb.append("\u200B");
            }
            labelStr = sb.toString();
        }

        pointIndex++;

        XYChart.Data<String, Number> data = new XYChart.Data<>(labelStr, price);
        data.setExtraValue(bidderName);
        
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                String fullTimeStr = time.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
                Tooltip tooltip = new Tooltip("Thời gian: " + fullTimeStr + "\nNgười đặt: " + bidderName + "\nGiá: " + String.format("%,.0f VNĐ", price));
                tooltip.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
                Tooltip.install(newNode, tooltip);
                
                // Hover effect
                newNode.setOnMouseEntered(e -> newNode.setStyle("-fx-background-color: #4F46E5; -fx-scale-x: 1.5; -fx-scale-y: 1.5; -fx-cursor: hand;"));
                newNode.setOnMouseExited(e -> newNode.setStyle(""));
            }
        });
        
        series.getData().add(data);
    }

    /**
     * Xóa toàn bộ data point — đúng khi chuyen auction hoac reload.
     *
     */
    public void clearData() {
        series.getData().clear();
        pointIndex = 0;
    }

    /**
     * Trả về series để bind vao LineChart.
     *
     *
     * @return XYChart.Series chua du lieu gia
     */
    public XYChart.Series<String, Number> getSeries() {
        return series;
    }

    /**
     * Trả về so luong data point hien tai — đúng cho test.
     *
     * @return so luong data point
     */
    public int getDataPointCount() {
        return series.getData().size();
    }
}