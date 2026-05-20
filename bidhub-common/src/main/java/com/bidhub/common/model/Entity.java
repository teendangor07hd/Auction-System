package com.bidhub.common.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Lớp cơ sở trừu tượng cho tất cả các thực thể trong hệ thống BidHub.
 *
 * <p>Mỗi entity trong hệ thống ({@code User}, {@code Item}, {@code Auction},
 * {@code BidTransaction}) đều kế thừa lớp này để đảm bảo tính nhất quán:
 * mỗi object đều có ID duy nhất và timestamp tạo/cập nhật.
 *
 * <p><b>Tại sao abstract?</b> Entity không thể tồn tại độc lập — không ai
 * "tạo một Entity chung chung". Chỉ có thể tạo {@code Bidder}, {@code Auction}, v.v.
 * Dùng {@code abstract} để JVM ngăn chặn {@code new Entity()} trực tiếp.
 *
 * <p><b>equals/hashCode dựa trên id:</b> 2 entity cùng id = cùng 1 thực thể
 * trong domain, dù là 2 object khác nhau trong bộ nhớ. Quan trọng khi
 * dùng trong {@code Set}, {@code Map} hoặc so sánh sau khi load từ DB.
 *
 * <p>Ví dụ:
 * <pre>{@code
 * // Entity là abstract — không thể new trực tiếp
 * // Entity e = new Entity(); // ← compile error
 *
 * // Phải dùng subclass
 * Bidder bidder = new Bidder("alice", "hash", "alice@mail.com");
 * System.out.println(bidder.getId()); // → UUID như "550e8400-e29b-41d4..."
 * }</pre>
 */
public abstract class Entity {

    /** ID duy nhất, dạng UUID string. Bất biến sau khi tạo. */
    private final String id;

    /** Thời điểm tạo entity. Bất biến sau khi tạo. */
    private final LocalDateTime createdAt;

    /** Thời điểm cập nhật gần nhất. Thay đổi khi gọi {@link #markUpdated()}. */
    private volatile LocalDateTime updatedAt;

    /**
     * Constructor duy nhất — tự động gán UUID và timestamp khi tạo entity mới.
     *
     * <p>Gọi bởi constructor của subclass qua {@code super()}.
     */
    protected Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Constructor dùng khi load entity từ database (id và timestamps đã có sẵn).
     *
     * <p>Quan trọng: khi đọc từ DB, phải dùng constructor này để giữ nguyên id gốc,
     * không tạo UUID mới.
     *
     * @param id        id đã tồn tại (từ DB)
     * @param createdAt thời điểm tạo gốc
     * @param updatedAt thời điểm cập nhật gần nhất
     */
    protected Entity(String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Objects.requireNonNull(id, "id không được null");
        Objects.requireNonNull(createdAt, "createdAt không được null");
        Objects.requireNonNull(updatedAt, "updatedAt không được null");
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Cập nhật {@code updatedAt} về thời điểm hiện tại.
     *
     * <p>Gọi trong subclass bất cứ khi nào field quan trọng thay đổi.
     * Ví dụ: {@code Auction.updateHighestBid()} nên gọi {@code markUpdated()}.
     */
    protected final void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Trả về ID duy nhất của entity.
     *
     * @return chuỗi UUID, không bao giờ null
     */
    public final String getId() {
        return id;
    }

    /**
     * Trả về thời điểm tạo entity.
     *
     * @return {@code LocalDateTime} thời điểm tạo, không bao giờ null
     */
    public final LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Trả về thời điểm cập nhật gần nhất.
     *
     * @return {@code LocalDateTime} thời điểm cập nhật, không bao giờ null
     */
    public final LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * So sánh 2 entity dựa trên {@code id}.
     *
     * <p>2 entity cùng id = cùng 1 thực thể trong hệ thống, bất kể class hay trạng thái.
     *
     * @param o object cần so sánh
     * @return {@code true} nếu cùng class và cùng id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Entity other = (Entity) o;
        return Objects.equals(id, other.id);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Trả về biểu diễn chuỗi gồm class name và id (7 ký tự đầu của UUID).
     *
     * <p>Dùng để debug. Subclass nên override để thêm thông tin có nghĩa hơn.
     *
     * @return chuỗi dạng {@code "Bidder[550e840]"}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id.substring(0, Math.min(7, id.length())) + "]";
    }
}