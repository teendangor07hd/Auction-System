package com.bidhub.server.model;

import java.util.Objects;

/**
 * Tác phẩm nghệ thuật (Art) — subclass cụ thể của {@link Item}.
 *
 * <p>Thêm tên nghệ sĩ và năm sáng tác.
 */
public final class Art extends Item {

    /** Tên nghệ sĩ/tác giả. */
    private final String artist;

    /** Năm sáng tác (ví dụ: 1990, 2024). */
    private final int yearCreated;

    /**
     * Tạo tác phẩm nghệ thuật mới.
     *
     * @param name          tên tác phẩm
     * @param description   mô tả
     * @param startingPrice giá khởi điểm, phải > 0
     * @param sellerId      id người bán
     * @param artist        tên nghệ sĩ, không null
     * @param yearCreated   năm sáng tác
     */
    public Art(
            String name,
            String description,
            double startingPrice,
            String sellerId,
            String artist,
            int yearCreated) {
        super(name, description, startingPrice, sellerId, ItemType.ART);
        Objects.requireNonNull(artist, "artist không được null");
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    // Thêm vào Art.java — constructor load từ DB
    public Art(String id, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt,
               String name, String description, double startingPrice, String sellerId,
               String artist, int yearCreated) {
        super(id, createdAt, updatedAt, name, description, startingPrice, sellerId,
                com.bidhub.server.model.ItemType.ART);
        if (artist == null) throw new IllegalArgumentException("artist không được null");
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Art trả về tên nghệ sĩ và năm sáng tác.
     */
    @Override
    public String getCategoryDetails() {
        return "Nghệ sĩ: " + artist + " | Năm sáng tác: " + yearCreated;
    }

    /** Trả về tên nghệ sĩ. */
    public String getArtist() { return artist; }
    /** Trả về năm sáng tác. */
    public int getYearCreated() { return yearCreated; }
}