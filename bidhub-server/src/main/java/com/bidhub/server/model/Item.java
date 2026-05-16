package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp trừu tượng đại diện cho sản phẩm đấu giá trong BidHub.
 *
 * <p>Kế thừa {@link Entity} (để có id, timestamps) và implement
 * {@link Displayable} (để có thể in thông tin). Subclass cụ thể thêm
 * field đặc thù của từng danh mục.
 *
 * <p>Quan trọng: Constructor validate {@code startingPrice > 0}.
 * Sản phẩm với giá khởi điểm <= 0 không hợp lệ về mặt nghiệp vụ.
 */
public abstract class Item extends Entity implements Displayable {

    private static final Logger logger = LoggerFactory.getLogger(Item.class);

    /** Ten san pham — bat buoc, khong rong. */
    private String name;

    /** Mô tả chi tiết sản phẩm. */
    private String description;

    /**
     * Giá khởi điểm (VND) — phải > 0.
     *
     * <p>Đây là mức giá tối thiểu để bắt đầu đấu giá.
     */
    private final double startingPrice;

    /** ID của Seller đã đăng sản phẩm này. */
    private final String sellerId;

    /** Loại sản phẩm — xác định subclass cụ thể. */
    private final ItemType itemType;

    /** Đường dẫn hoặc URL ảnh của sản phẩm. */
    private String imageUrl;

    /**
     * Constructor tạo Item mới.
     *
     * @param name          tên sản phẩm, không null, không rỗng
     * @param description   mô tả sản phẩm
     * @param startingPrice giá khởi điểm, phải > 0
     * @param sellerId      id của Seller, không null
     * @param itemType      loại sản phẩm, không null
     * @throws IllegalArgumentException nếu startingPrice <= 0 hoặc name rỗng
     */
    protected Item(
            String name,
            String description,
            double startingPrice,
            String sellerId,
            ItemType itemType) {
        super();
        validateName(name);
        Objects.requireNonNull(sellerId, "sellerId không được null");
        Objects.requireNonNull(itemType, "itemType không được null");
        if (startingPrice <= 0) {
            throw new IllegalArgumentException(
                    "Giá khởi điểm phải > 0, nhận được: " + startingPrice);
        }
        this.name = name;
        this.description = (description == null) ? "" : description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
        this.itemType = itemType;
    }

    /**
     * Constructor load từ database.
     *
     * @param id            id từ DB
     * @param createdAt     thời điểm tạo
     * @param updatedAt     thời điểm cập nhật
     * @param name          tên sản phẩm
     * @param description   mô tả
     * @param startingPrice giá khởi điểm
     * @param sellerId      id người bán
     * @param itemType      loại sản phẩm
     */
    protected Item(
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String name,
            String description,
            double startingPrice,
            String sellerId,
            ItemType itemType) {
        super(id, createdAt, updatedAt);
        this.name = name;
        this.description = (description == null) ? "" : description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
        this.itemType = itemType;
    }

    /**
     * Trả về thông tin đặc trưng theo danh mục của sản phẩm.
     *
     * <p>Ví dụ: Electronics trả về "Thương hiệu: Apple | Bảo hành: 12 tháng".
     * Đây là abstract method — bắt buộc subclass phải implement (Abstraction).
     *
     * @return chuỗi mô tả danh mục, không null
     */
    public abstract String getCategoryDetails();

    /** Validate tên sản phẩm: không null, không rỗng. */
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được null hoặc rỗng");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>In đầy đủ thông tin sản phẩm gồm: tên, loại, giá khởi điểm,
     * và thông tin danh mục riêng từ {@link #getCategoryDetails()}.
     */
    @Override
    public void printInfo() {
        logger.info("=== Thong tin san pham ===");
        logger.info("Ten     : {}", name);
        logger.info("Loai    : {}", itemType.getLabel());
        logger.info("Gia KD  : {:,.0f} VND", startingPrice);
        logger.info("Chi tiet: {}", getCategoryDetails());
        logger.info("Mo ta   : {}", description);
    }

    // Getters
    /** Trả về tên sản phẩm. */
    public String getName() { return name; }
    /** Trả về mô tả sản phẩm. */
    public String getDescription() { return description; }
    /** Trả về giá khởi điểm. */
    public double getStartingPrice() { return startingPrice; }
    /** Trả về id người bán. */
    public String getSellerId() { return sellerId; }
    /** Trả về loại sản phẩm. */
    public ItemType getItemType() { return itemType; }

    /** Cập nhật tên sản phẩm. */
    public void setName(String name) {
        validateName(name);
        this.name = name;
        markUpdated();
    }

    /** Cập nhật mô tả sản phẩm. */
    public void setDescription(String description) {
        this.description = (description == null) ? "" : description;
        markUpdated();
    }

    /** Trả về đường dẫn ảnh sản phẩm. */
    public String getImageUrl() { return imageUrl; }

    /** Cập nhật đường dẫn ảnh sản phẩm. */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        markUpdated();
    }
}