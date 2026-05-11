package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD cho bảng {@code users}. Map {@code role} TEXT → đúng subclass Bidder/Seller/Admin.
 *
 * <p>Mọi method đều mở connection qua {@link DbConnectionProvider} (production) hoặc dùng
 * connection được inject qua constructor (test với in-memory SQLite).
 */
public class UserDao {

    /** Connection được inject từ test. Null = dùng DbConnectionProvider bình thường. */
    private final Connection injectedConn;

    /** Constructor mặc định — dùng trong production. */
    public UserDao() {
        this.injectedConn = null;
    }

    /** Constructor inject connection — chỉ dùng trong test với in-memory SQLite. */
    public UserDao(Connection conn) {
        this.injectedConn = conn;
    }

    /** Lấy connection: ưu tiên injectedConn, fallback về DbConnectionProvider. */
    private Connection acquireConnection() throws SQLException {
        return (injectedConn != null)
                ? injectedConn
                : DbConnectionProvider.getInstance().getConnection();
    }

    /** Đóng connection CHỈ KHI không phải injected (tránh đóng shared test connection). */
    private void releaseConnection(Connection conn) {
        if (injectedConn == null) {
            DbConnectionProvider.getInstance().closeConnection(conn);
        }
    }

    /**
     * Lưu user mới vào DB. Dùng INSERT — không hỗ trợ upsert.
     *
     * @param user đối tượng User cần lưu (Bidder, Seller, hoặc Admin)
     * @throws RuntimeException nếu lỗi SQL
     */
    public void save(User user) {
        String sql = """
        INSERT INTO users (id, username, password_hash, email, role, extra_int,
            created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getId());
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getPasswordHash());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getRole().name());
                ps.setInt(6, extractExtraInt(user));
                ps.setString(7, user.getCreatedAt().toString());
                ps.setString(8, user.getUpdatedAt().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.save thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Tìm user theo ID.
     *
     * @param id UUID string
     * @return {@link Optional} chứa User đúng subclass, hoặc empty nếu không có
     */
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.findById thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Tìm user theo username (dùng trong login, register validation).
     *
     * @param username tên đăng nhập
     * @return {@link Optional} empty nếu không tìm thấy
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.findByUsername thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Kiểm tra username đã tồn tại chưa (dùng trong register để ném DuplicateUsernameException).
     *
     * @param username tên cần kiểm tra
     * @return {@code true} nếu đã có trong DB
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.existsByUsername thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Trả về tất cả user trong hệ thống.
     *
     * @return danh sách User (có thể rỗng)
     */
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                List<User> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.findAll thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    // Tạo đúng subclass dựa vào cột role — cốt lõi của polymorphism trong DAO
    private User mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String email = rs.getString("email");
        UserRole role = UserRole.valueOf(rs.getString("role"));
        int extraInt = rs.getInt("extra_int");

        return switch (role) {
            case BIDDER -> new Bidder(id, createdAt, updatedAt, username, passwordHash, email, extraInt);
            case SELLER -> new Seller(id, createdAt, updatedAt, username, passwordHash, email, extraInt);
            case ADMIN  -> new Admin(id, createdAt, updatedAt, username, passwordHash, email, extraInt);
        };
    }

    // Lấy giá trị extra_int theo role (totalBidsPlaced / totalItemsListed / adminLevel)
    private int extractExtraInt(User user) {
        return switch (user.getRole()) {
            case BIDDER -> ((Bidder) user).getTotalBidsPlaced();
            case SELLER -> ((Seller) user).getTotalItemsListed();
            case ADMIN  -> ((Admin)  user).getAdminLevel();
        };
    }
}