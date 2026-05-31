package com.bidhub.server.model;

import java.util.Map;
import java.util.Objects;

/**
 * Abstract Creator trong Factory Method Pattern — tạo các đối tượng {@link Item}.
 *
 * <p><b>Factory Method Pattern (GoF) trong BidHub:</b>
 * <ul>
 *   <li><b>Creator</b>: {@code ItemCreator} (abstract class này) — định nghĩa factory method
 *       {@link #createItem} nhưng không biết class cụ thể nào sẽ được tạo.</li>
 *   <li><b>ConcreteCreator</b>: {@link ElectronicsCreator}, {@link ArtCreator},
 *       {@link VehicleCreator} — mỗi class override {@link #createItem} để tạo đúng loại.</li>
 *   <li><b>Product</b>: {@link Item} (abstract) — interface chung của object được tạo.</li>
 *   <li><b>ConcreteProduct</b>: {@link Electronics}, {@link Art}, {@link Vehicle}.</li>
 * </ul>
 *
 * <p><b>Tại sao là abstract class thay vì interface?</b>
 * Vì {@code ItemCreator} chứa các helper method {@code protected}
 * ({@link #requireString}, {@link #requireInt}) để các ConcreteCreator dùng lại.
 * Interface không cho phép protected method với shared implementation.
 *
 * <p><b>Cách dùng đúng:</b>
 * <pre>{@code
 * // Cách 1: Biết trước loại cụ thể
 * ItemCreator creator = new ElectronicsCreator();
 * Item laptop = creator.createItem("MacBook Pro", "Mạnh", 25_000_000.0, "seller-1",
 *     Map.of("brand", "Apple", "warrantyMonths", 12));
 *
 * ItemCreator creator = ItemCreator.forType(itemType);
 * Item item = creator.createItem(name, desc, price, sellerId, extras);
 * }</pre>
 *
 * <p><b>Khi thêm loại mới (JEWELRY):</b>
 * <ol>
 *   <li>Thêm {@code JEWELRY} vào {@link ItemType}.</li>
 *   <li>Tạo class {@code Jewelry extends Item}.</li>
 *   <li>Tạo class {@code JewelryCreator extends ItemCreator}.</li>
 *   <li>Thêm 1 case vào {@code forType()}.</li>
 * </ol>
 * Không sửa {@link ElectronicsCreator}, {@link ArtCreator}, hay {@link VehicleCreator}.
 */
public abstract class ItemCreator {

    /**
     * <b>Factory Method</b> — tạo một {@link Item} cụ thể.
     *
     * <p>Đây là phương thức cốt lõi của pattern. Mỗi ConcreteCreator override
     * phương thức này để quyết định class cụ thể nào được khởi tạo.
     *
     * @param name          tên sản phẩm, không null, không rỗng
     * @param description   mô tả sản phẩm (có thể null)
     * @param startingPrice giá khởi điểm, phải &gt; 0
     * @param sellerId      id người bán, không null
     * @param extras        map thông tin bổ sung đặc trưng theo loại sản phẩm
     * @return instance cụ thể của {@link Item} (không bao giờ null)
     * @throws IllegalArgumentException nếu tham số không hợp lệ hoặc extras thiếu field bắt buộc
     */
    public abstract Item createItem(
            String name,
            String description,
            double startingPrice,
            String sellerId,
            Map<String, Object> extras);

    /**
     * Trả về ConcreteCreator phù hợp với {@link ItemType} đã cho.
     *
     * <p>Phương thức tiện ích này dùng khi loại sản phẩm chỉ biết lúc runtime
     * (ví dụ: đọc từ JSON request của client). Không ảnh hưởng đến core pattern.
     *
     * @param type loại sản phẩm, không null
     * @return concrete creator tương ứng
     * @throws NullPointerException nếu {@code type} là null
     */
    public static ItemCreator forType(ItemType type) {
        Objects.requireNonNull(type, "ItemType không được null");
        return switch (type) {
            case ELECTRONICS -> new ElectronicsCreator();
            case ART         -> new ArtCreator();
            case VEHICLE     -> new VehicleCreator();
        };
    }

    // =========================================================================
    // Protected helpers — dùng chung cho tất cả ConcreteCreator
    // =========================================================================

    /**
     * Lấy giá trị {@code String} từ {@code extras} map.
     *
     * <p>Dùng trong ConcreteCreator để extract field bắt buộc.
     *
     * @param extras map extras được truyền vào {@link #createItem}
     * @param key    tên field cần lấy
     * @return giá trị String
     * @throws IllegalArgumentException nếu key không có hoặc giá trị không phải String
     */
    protected final String requireString(Map<String, Object> extras, String key) {
        Object value = (extras != null) ? extras.get(key) : null;
        if (!(value instanceof String str)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ": extras[\"" + key + "\"] phải là String, nhận: "
                            + (value == null ? "null" : value.getClass().getSimpleName()));
        }
        return str;
    }

    /**
     * Lấy giá trị {@code int} từ {@code extras} map.
     *
     * @param extras map extras
     * @param key    tên field cần lấy
     * @return giá trị int
     * @throws IllegalArgumentException nếu key không có hoặc giá trị không phải Integer
     */
    protected final int requireInt(Map<String, Object> extras, String key) {
        Object value = (extras != null) ? extras.get(key) : null;
        if (!(value instanceof Integer intVal)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ": extras[\"" + key + "\"] phải là Integer, nhận: "
                            + (value == null ? "null" : value.getClass().getSimpleName()));
        }
        return intVal;
    }
}