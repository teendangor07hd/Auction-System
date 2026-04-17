package com.bidhub.server.model;

import java.util.Map;

/**
 * Concrete Creator cho tác phẩm nghệ thuật — override factory method để tạo {@link Art}.
 *
 * <p>Yêu cầu trong {@code extras}:
 * <ul>
 *   <li>{@code "artist"} (String) — tên nghệ sĩ</li>
 *   <li>{@code "yearCreated"} (Integer) — năm sáng tác</li>
 * </ul>
 *
 * <p>Ví dụ:
 * <pre>{@code
 * ItemCreator creator = new ArtCreator();
 * Item painting = creator.createItem(
 *     "Mona Lisa bản sao", "Tranh sơn dầu",
 *     50_000_000.0, "seller-xyz",
 *     Map.of("artist", "Nguyễn Văn A", "yearCreated", 2020));
 * }</pre>
 */
public final class ArtCreator extends ItemCreator {

    /**
     * Tạo một đối tượng {@link Art} từ tham số đã cho.
     *
     * @param name          tên tác phẩm
     * @param description   mô tả
     * @param startingPrice giá khởi điểm (&gt; 0)
     * @param sellerId      id người bán
     * @param extras        phải có: {@code artist} (String), {@code yearCreated} (Integer)
     * @return instance {@link Art}
     */
    @Override
    public Item createItem(
            String name,
            String description,
            double startingPrice,
            String sellerId,
            Map<String, Object> extras) {
        String artist = requireString(extras, "artist");
        int yearCreated = requireInt(extras, "yearCreated");
        return new Art(name, description, startingPrice, sellerId, artist, yearCreated);
    }
}