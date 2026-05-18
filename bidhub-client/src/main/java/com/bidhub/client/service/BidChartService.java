package com.bidhub.client.service;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
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
    // 📌 [B57] Gioi han toi da 100 data points — tranh chart cham sau ~200 bids
    private static final int MAX_DATA_POINTS = 100;

    // 📌 [Tieu chi: Kỹ thuật quan trọng — DateTimeFormatter format LocalDateTime → String]
    public void addDataPoint(LocalDateTime time, double price) {
        ObservableList<XYChart.Data<String, Number>> data = series.getData();

        // [B57] Xoa data point cu nhat khi vuot qua gioi han
        if (data.size() >= MAX_DATA_POINTS) {
            data.remove(0);
        }

        String timeStr = time.format(TIME_FORMATTER);
        data.add(new XYChart.Data<>(timeStr, price));
    }

    /**
     * Xoa toan bo data point — dung khi chuyen auction hoac reload.
     *
     * <p>// 📌 [Tieu chi: Price Chart — clearData de reset chart khi chuyen auction]
     */
    public void clearData() {
        series.getData().clear();
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