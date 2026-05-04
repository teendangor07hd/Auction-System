package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.AuditLog;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD cho bảng {@code audit_logs}.
 *
 * <p>Theo đúng pattern DAO Tuần 3: 2 constructor, acquireConnection/releaseConnection,
 * PreparedStatement cho mọi query.
 */
public class AuditLogDao {

    private final Connection injectedConn;

    /** Constructor production. */
    public AuditLogDao() {
        this.injectedConn = null;
    }

    /** Constructor test — inject in-memory connection. */
    public AuditLogDao(Connection conn) {
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
     * Lưu bản ghi audit mới.
     *
     * @param log bản ghi cần lưu
     */
    public void save(AuditLog log) {
        String sql = """
        INSERT INTO audit_logs (id, user_id, action, details, created_at)
        VALUES (?, ?, ?, ?, ?)
        """;
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, log.getId());
                ps.setString(2, log.getUserId()); // null-safe: SQLite lưu NULL khi setString(null)
                ps.setString(3, log.getAction());
                ps.setString(4, log.getDetails());
                ps.setString(5, log.getCreatedAt().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDao.save thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Trả về tất cả bản ghi, mới nhất trước.
     *
     * @return danh sách AuditLog, có thể rỗng
     */
    public List<AuditLog> findAll() {
        return queryList("SELECT * FROM audit_logs ORDER BY created_at DESC", null);
    }

    /**
     * Tìm tất cả log của 1 user.
     *
     * @param userId id người dùng
     * @return danh sách log, mới nhất trước
     */
    public List<AuditLog> findByUserId(String userId) {
        return queryList(
                "SELECT * FROM audit_logs WHERE user_id = ? ORDER BY created_at DESC", userId);
    }

    /**
     * Tìm log theo mã action.
     *
     * @param action mã từ {@link com.bidhub.server.model.AuditActions}
     * @return danh sách log matching, mới nhất trước
     */
    public List<AuditLog> findByAction(String action) {
        return queryList(
                "SELECT * FROM audit_logs WHERE action = ? ORDER BY created_at DESC", action);
    }

    /**
     * Trả về N bản ghi mới nhất.
     *
     * @param limit số lượng tối đa cần lấy
     * @return danh sách tối đa {@code limit} bản ghi
     */
    public List<AuditLog> findRecent(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit phải > 0, nhận được: " + limit);
        }
        String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ?";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                List<AuditLog> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDao.findRecent thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    private List<AuditLog> queryList(String sql, String param) {
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (param != null) ps.setString(1, param);
                ResultSet rs = ps.executeQuery();
                List<AuditLog> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuditLogDao query thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        String userId = rs.getString("user_id"); // null khi system action — getString trả null, không crash
        String action = rs.getString("action");
        String details = rs.getString("details");
        return new AuditLog(id, createdAt, userId, action, details);
    }
}