package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD cho bảng {@code items}. Trường đặc thù (brand, artist...) được serialize vào
 * cột {@code extra_data} dạng JSON bằng Jackson.
 */
public class ItemDao {

    // ObjectMapper là thread-safe — khai báo static để tái sử dụng
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<>() {};

    private final Connection injectedConn;

    public ItemDao() {
        this.injectedConn = null;
    }

    public ItemDao(Connection conn) {
        this.injectedConn = conn;
    }

    private Connection acquireConnection() throws SQLException {
        return (injectedConn != null)
                ? injectedConn
                : DbConnectionProvider.getInstance().getConnection();
    }

    private void releaseConnection(Connection conn) {
        if (injectedConn == null) {
            DbConnectionProvider.getInstance().closeConnection(conn);
        }
    }

    /**
     * Lưu item vào DB. Serialize extra fields thành JSON vào cột {@code extra_data}.
     *
     * @param item đối tượng Electronics, Art, hoặc Vehicle
     * @throws RuntimeException nếu lỗi SQL hoặc JSON serialization
     */
    public void save(Item item) {
        String sql = """
        INSERT INTO items (id, name, description, starting_price, item_type,
            seller_id, extra_data, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, item.getId());
                ps.setString(2, item.getName());
                ps.setString(3, item.getDescription());
                ps.setDouble(4, item.getStartingPrice());
                ps.setString(5, item.getItemType().name());
                ps.setString(6, item.getSellerId());
                // 📌 [Tiêu chí: MVC — tầng DAO xử lý persistence]
                ps.setString(7, MAPPER.writeValueAsString(buildExtras(item)));
                ps.setString(8, item.getCreatedAt().toString());
                ps.setString(9, item.getUpdatedAt().toString());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("ItemDao.save thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Tìm item theo ID.
     *
     * @param id UUID string
     * @return {@link Optional} chứa Item đúng subclass, hoặc empty
     */
    public Optional<Item> findById(String id) {
        return querySingle("SELECT * FROM items WHERE id = ?", id);
    }

    /**
     * Tìm tất cả item của 1 seller.
     *
     * @param sellerId ID ngườii bán
     * @return danh sách Item, có thể rỗng
     */
    public List<Item> findBySellerId(String sellerId) {
        return queryList("SELECT * FROM items WHERE seller_id = ?", sellerId);
    }

    /** Trả về tất cả item trong hệ thống. */
    public List<Item> findAll() {
        return queryList("SELECT * FROM items", (String) null);
    }

    /**
     * Xóa item theo ID.
     *
     * @param id UUID string
     */
    public void deleteById(String id) {
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("ItemDao.deleteById thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Cập nhật tên, mô tả, giá khởi điểm của sản phẩm.
     * Không thay đổi item_type, seller_id, extra_data (chỉ sửa các trường metadata cơ bản).
     *
     * @param itemId         ID sản phẩm cần cập nhật
     * @param newName        tên mới (null = giữ nguyên)
     * @param newDescription mô tả mới (null = giữ nguyên)
     * @param newPrice       giá mới (&lt;0 = giữ nguyên)
     */
    public void updateItem(String itemId, String newName, String newDescription, double newPrice) {
        String sql = """
            UPDATE items
            SET name = COALESCE(?, name),
                description = COALESCE(?, description),
                starting_price = CASE WHEN ? >= 0 THEN ? ELSE starting_price END,
                updated_at = ?
            WHERE id = ?
            """;
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newName);
                ps.setString(2, newDescription);
                ps.setDouble(3, newPrice);
                ps.setDouble(4, newPrice);
                ps.setString(5, java.time.LocalDateTime.now().toString());
                ps.setString(6, itemId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("ItemDao.updateItem thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    private Optional<Item> querySingle(String sql, String param) {
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (param != null) ps.setString(1, param);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("ItemDao query thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    private List<Item> queryList(String sql, String param) {
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (param != null) ps.setString(1, param);
                ResultSet rs = ps.executeQuery();
                List<Item> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("ItemDao query list thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    // Tạo đúng subclass dựa vào item_type — dùng constructor DB-load (giữ nguyên id/timestamps)
    private Item mapRow(ResultSet rs) throws Exception {
        String id = rs.getString("id");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));
        String name = rs.getString("name");
        String description = rs.getString("description");
        double startingPrice = rs.getDouble("starting_price");
        String sellerId = rs.getString("seller_id");
        ItemType type = ItemType.valueOf(rs.getString("item_type"));

        // Parse JSON extra_data → Map; Jackson decode số nguyên thành Integer
        Map<String, Object> extras = MAPPER.readValue(rs.getString("extra_data"), MAP_TYPE);

        Item newItem = switch (type) {
            case ELECTRONICS -> new Electronics(
                    id, createdAt, updatedAt, name, description, startingPrice, sellerId,
                    (String) extras.get("brand"),
                    ((Number) extras.get("warrantyMonths")).intValue());
            case ART -> new Art(
                    id, createdAt, updatedAt, name, description, startingPrice, sellerId,
                    (String) extras.get("artist"),
                    ((Number) extras.get("yearCreated")).intValue());
            case VEHICLE -> new Vehicle(
                    id, createdAt, updatedAt, name, description, startingPrice, sellerId,
                    (String) extras.get("manufacturer"),
                    ((Number) extras.get("year")).intValue(),
                    ((Number) extras.get("mileageKm")).intValue());
        };
        if (extras.containsKey("imageUrl") && extras.get("imageUrl") != null) {
            newItem.setImageUrl((String) extras.get("imageUrl"));
        }
        return newItem;
    }

    // Tạo Map extras từ Item để serialize thành JSON
    private Map<String, Object> buildExtras(Item item) {
        Map<String, Object> extras = new HashMap<>();
        switch (item.getItemType()) {
            case ELECTRONICS -> {
                Electronics e = (Electronics) item;
                extras.put("brand", e.getBrand());
                extras.put("warrantyMonths", e.getWarrantyMonths());
            }
            case ART -> {
                Art a = (Art) item;
                extras.put("artist", a.getArtist());
                extras.put("yearCreated", a.getYearCreated());
            }
            case VEHICLE -> {
                Vehicle v = (Vehicle) item;
                extras.put("manufacturer", v.getManufacturer());
                extras.put("year", v.getYear());
                extras.put("mileageKm", v.getMileageKm());
            }
        }
        if (item.getImageUrl() != null) {
            extras.put("imageUrl", item.getImageUrl());
        }
        return extras;
    }
}