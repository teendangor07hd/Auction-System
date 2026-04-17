package com.bidhub.server.model;

/**
 * Loại sản phẩm trong hệ thống BidHub.
 *
 * <p>Mỗi loại tương ứng với một subclass cụ thể của {@link Item}:
 * <ul>
 *   <li>{@link #ELECTRONICS} → {@link Electronics}</li>
 *   <li>{@link #ART} → {@link Art}</li>
 *   <li>{@link #VEHICLE} → {@link Vehicle}</li>
 * </ul>
 *
 * <p>Enum được lưu vào cột {@code item_type} trong database dưới dạng tên enum.
 */
public enum ItemType {

    /** Hàng điện tử: laptop, điện thoại, thiết bị gia dụng. */
    ELECTRONICS("Đồ điện tử"),

    /** Tác phẩm nghệ thuật: tranh, tượng, thủ công mỹ nghệ. */
    ART("Tác phẩm nghệ thuật"),

    /** Phương tiện giao thông: ô tô, xe máy. */
    VEHICLE("Phương tiện");

    /** Tên hiển thị tiếng Việt dùng trên UI. */
    private final String label;

    ItemType(String label) {
        this.label = label;
    }

    /**
     * Trả về tên hiển thị tiếng Việt của loại sản phẩm.
     *
     * @return chuỗi nhãn (ví dụ: {@code "Đồ điện tử"})
     */
    public String getLabel() {
        return label;
    }
}