package com.bidhub.server.model;

import java.util.Objects;

/**
 * Phương tiện giao thông (Vehicle) — subclass cụ thể của {@link Item}.
 *
 * <p>Thêm nhà sản xuất, năm sản xuất và số km đã đi.
 */
public final class Vehicle extends Item {

    /** Nhà sản xuất (ví dụ: "Toyota", "Honda", "Mercedes"). */
    private final String manufacturer;

    /** Năm sản xuất. */
    private final int year;

    /** Số km đã chạy (0 = mới, xe cũ thì > 0). */
    private final int mileageKm;

    /**
     * Tạo phương tiện mới.
     *
     * @param name          tên xe
     * @param description   mô tả
     * @param startingPrice giá khởi điểm, phải > 0
     * @param sellerId      id người bán
     * @param manufacturer  nhà sản xuất, không null
     * @param year          năm sản xuất
     * @param mileageKm     số km đã chạy (≥ 0)
     */
    public Vehicle(
            String name,
            String description,
            double startingPrice,
            String sellerId,
            String manufacturer,
            int year,
            int mileageKm) {
        super(name, description, startingPrice, sellerId, ItemType.VEHICLE);
        Objects.requireNonNull(manufacturer, "manufacturer không được null");
        if (mileageKm < 0) {
            throw new IllegalArgumentException("mileageKm không được âm: " + mileageKm);
        }
        this.manufacturer = manufacturer;
        this.year = year;
        this.mileageKm = mileageKm;
    }

    // Thêm vào Vehicle.java — constructor load từ DB
    public Vehicle(String id, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt,
                   String name, String description, double startingPrice, String sellerId,
                   String manufacturer, int year, int mileageKm) {
        super(id, createdAt, updatedAt, name, description, startingPrice, sellerId,
                com.bidhub.server.model.ItemType.VEHICLE);
        Objects.requireNonNull(manufacturer, "manufacturer không được null");
        if (mileageKm < 0) throw new IllegalArgumentException("mileageKm không được âm");
        this.manufacturer = manufacturer;
        this.year = year;
        this.mileageKm = mileageKm;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Vehicle trả về nhà sản xuất, năm và số km đã chạy.
     */
    @Override
    public String getCategoryDetails() {
        return "Nhà SX: " + manufacturer + " | Năm: " + year
                + " | Km đã đi: " + String.format("%,d", mileageKm);
    }

    /** Trả về nhà sản xuất. */
    public String getManufacturer() { return manufacturer; }
    /** Trả về năm sản xuất. */
    public int getYear() { return year; }
    /** Trả về số km đã chạy. */
    public int getMileageKm() { return mileageKm; }
}