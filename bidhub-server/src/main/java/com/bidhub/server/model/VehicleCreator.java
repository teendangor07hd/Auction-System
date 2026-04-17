package com.bidhub.server.model;

import java.util.Map;

/**
 * Concrete Creator cho phương tiện giao thông — override factory method để tạo {@link Vehicle}.
 *
 * <p>Yêu cầu trong {@code extras}:
 * <ul>
 *   <li>{@code "manufacturer"} (String) — nhà sản xuất, ví dụ: "Toyota"</li>
 *   <li>{@code "year"} (Integer) — năm sản xuất</li>
 *   <li>{@code "mileageKm"} (Integer) — số km đã chạy (≥ 0)</li>
 * </ul>
 *
 * <p>Ví dụ:
 * <pre>{@code
 * ItemCreator creator = new VehicleCreator();
 * Item car = creator.createItem(
 *     "Toyota Camry 2022", "Xe đẹp, bảo dưỡng đầy đủ",
 *     850_000_000.0, "seller-001",
 *     Map.of("manufacturer", "Toyota", "year", 2022, "mileageKm", 45000));
 * }</pre>
 */
public final class VehicleCreator extends ItemCreator {

    /**
     * Tạo một đối tượng {@link Vehicle} từ tham số đã cho.
     *
     * @param name          tên xe
     * @param description   mô tả
     * @param startingPrice giá khởi điểm (&gt; 0)
     * @param sellerId      id người bán
     * @param extras        phải có: {@code manufacturer} (String), {@code year} (Integer),
     *                      {@code mileageKm} (Integer)
     * @return instance {@link Vehicle}
     */
    @Override
    public Item createItem(
            String name,
            String description,
            double startingPrice,
            String sellerId,
            Map<String, Object> extras) {
        String manufacturer = requireString(extras, "manufacturer");
        int year = requireInt(extras, "year");
        int mileageKm = requireInt(extras, "mileageKm");
        return new Vehicle(name, description, startingPrice, sellerId, manufacturer, year, mileageKm);
    }
}

