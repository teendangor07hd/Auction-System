package com.bidhub.client.service;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service quan ly du lieu bieu do gia dau gia realtime.
 *
 * <p>Dung {@link XYChart.Series} de luu tru data point (thoi gian, gia).
 * Method {@link #addDataPoint(LocalDateTime, double)} format time thanh
 * string HH:mm:ss va tao data point moi.
 *
 * <p>// 📌 [Tieu chi: Price Chart — LineChart realtime bieu do gia]
 * // 📌 [Tieu chi: Kỹ thuật quan trọng — JavaFX XYChart.Series + ObservableList]
 *
 * @author Công Minh
 */
public final class BidChartService {

    // 📌 [Tieu chi: Price Chart — XYChart.Series cho du lieu bieu do gia]
    private final XYChart.Series<String, Number> series;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private int pointIndex = 0;

    /**
     * Tao BidChartService — khoi tao series voi ten "Lịch sử giá".
     *
     * <p>// 📌 [Tieu chi: Price Chart — series name hien thi tren chart legend]
     */
    public BidChartService() {
        this.series = new XYChart.Series<>();
        this.series.setName("Lịch sử giá");
    }

    /**
     * Them data point moi vao series — thoi gian va gia.
     *
     * <p>Format LocalDateTime thanh string HH:mm:ss de hien thi tren trục X.
     *
     * <p>// 📌 [Tieu chi: Price Chart — addDataPoint cho realtime va history]
     *
     * @param time  thoi gian dat gia
     * @param price gia dat
     */
    // 📌 [Tieu chi: Kỹ thuật quan trọng — DateTimeFormatter format LocalDateTime → String]
    public void addDataPoint(LocalDateTime time, double price) {
        addDataPoint(time, price, "Khách");
    }

    /**
     * Them data point moi vao series — thoi gian, gia, va ten nguoi dat gia.
     *
     * @param time       thoi gian dat gia
     * @param price      gia dat
     * @param bidderName ten nguoi dat gia
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
     * Xoa toan bo data point — dung khi chuyen auction hoac reload.
     *
     * <p>// 📌 [Tieu chi: Price Chart — clearData de reset chart khi chuyen auction]
     */
    public void clearData() {
        series.getData().clear();
        pointIndex = 0;
    }

    /**
     * Tra ve series de bind vao LineChart.
     *
     * <p>// 📌 [Tieu chi: Price Chart — getSeries de controller bind vao chart]
     *
     * @return XYChart.Series chua du lieu gia
     */
    public XYChart.Series<String, Number> getSeries() {
        return series;
    }

    /**
     * Tra ve so luong data point hien tai — dung cho test.
     *
     * @return so luong data point
     */
    public int getDataPointCount() {
        return series.getData().size();
    }
}