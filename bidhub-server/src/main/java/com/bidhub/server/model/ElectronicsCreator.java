package com.bidhub.server.model;

import java.util.Map;

/**
 * Concrete Creator cho sản phẩm điện tử — override factory method để tạo {@link Electronics}.
 *
 * <p>Trong Factory Method Pattern:
 * <ul>
 *   <li>Class này là <b>ConcreteCreator</b>.</li>
 *   <li>{@link Electronics} là <b>ConcreteProduct</b> mà class này tạo ra.</li>
 * </ul>
 *
 * <p>Class này chỉ biết tạo {@link Electronics} — không biết gì về {@link Art} hay
 * {@link Vehicle}. Đây là Single Responsibility đúng nghĩa.
 *
 * <p>Ví dụ:
 * <pre>{@code
 * ItemCreator creator = new ElectronicsCreator();
 * Item laptop = creator.createItem(
 *     "MacBook Pro M3", "Chip Apple M3",
 *     35_000_000.0, "seller-abc",
 *     Map.of("brand", "Apple", "warrantyMonths", 12));
 * // laptop instanceof Electronics → true
 * }</pre>
 */
public final class ElectronicsCreator extends ItemCreator {

    /**
     * Tạo một đối tượng {@link Electronics} từ tham số đã cho.
     *
     * <p>Yêu cầu trong {@code extras}:
     * <ul>
     *   <li>{@code "brand"} (String) — thương hiệu, ví dụ: "Apple", "Samsung"</li>
     *   <li>{@code "warrantyMonths"} (Integer) — số tháng bảo hành, ≥ 0</li>
     * </ul>
     *
     * @param name          tên sản phẩm
     * @param description   mô tả
     * @param startingPrice giá khởi điểm (&gt; 0)
     * @param sellerId      id người bán
     * @param extras        phải có: {@code brand} (String), {@code warrantyMonths} (Integer)
     * @return instance {@link Electronics}
     * @throws IllegalArgumentException nếu extras thiếu field bắt buộc
     */
    @Override
    public Item createItem(
            String name,
            String description,
            double startingPrice,
            String sellerId,
            Map<String, Object> extras) {
        String brand = requireString(extras, "brand");
        int warrantyMonths = requireInt(extras, "warrantyMonths");
        return new Electronics(name, description, startingPrice, sellerId, brand, warrantyMonths);
    }
}