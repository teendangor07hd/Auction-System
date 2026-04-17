package com.bidhub.server.model;

import java.util.Objects;

/**
 * Sản phẩm điện tử (Electronics) — subclass cụ thể của {@link Item}.
 *
 * <p>Thêm 2 field đặc trưng: thương hiệu và thời gian bảo hành.
 */
public final class Electronics extends Item {

    /** Thương hiệu (ví dụ: "Apple", "Samsung", "Sony"). */
    private final String brand;

    /** Thời gian bảo hành tính bằng tháng (0 = không bảo hành). */
    private final int warrantyMonths;

    /**
     * Tạo sản phẩm điện tử mới.
     *
     * @param name          tên sản phẩm
     * @param description   mô tả
     * @param startingPrice giá khởi điểm, phải > 0
     * @param sellerId      id người bán
     * @param brand         thương hiệu, không null
     * @param warrantyMonths số tháng bảo hành (≥ 0)
     */
    public Electronics(
            String name,
            String description,
            double startingPrice,
            String sellerId,
            String brand,
            int warrantyMonths) {
        super(name, description, startingPrice, sellerId, ItemType.ELECTRONICS);
        Objects.requireNonNull(brand, "brand không được null");
        if (warrantyMonths < 0) {
            throw new IllegalArgumentException("warrantyMonths không được âm: " + warrantyMonths);
        }
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Electronics trả về thương hiệu và thời gian bảo hành.
     */
    @Override
    public String getCategoryDetails() {
        return "Thương hiệu: " + brand + " | Bảo hành: " + warrantyMonths + " tháng";
    }

    /** Trả về thương hiệu. */
    public String getBrand() { return brand; }
    /** Trả về thời gian bảo hành tính bằng tháng. */
    public int getWarrantyMonths() { return warrantyMonths; }
}