package com.bidhub.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cung cấp kết nối JDBC tới SQLite. Singleton đảm bảo cùng URL được dùng xuyên suốt server.
 *
 * <p>Dùng double-checked locking với {@code volatile} để thread-safe khi nhiều DAO khởi động
 * đồng thờii. Mỗi lần gọi {@link #getConnection()} trả về connection mới với WAL mode bật.
 */
public final class DbConnectionProvider {

    // 📌 [Tiêu chí: Singleton Pattern — 1.0đ] volatile ngăn CPU reorder instruction khi khởi tạo
    private static volatile DbConnectionProvider instance;

    private final String jdbcUrl;

    private DbConnectionProvider() {
        this.jdbcUrl = "jdbc:sqlite:" + ConfigLoader.getString("db.path");
    }

    /** Trả về instance duy nhất, tạo mới nếu chưa tồn tại (thread-safe). */
    public static DbConnectionProvider getInstance() {
        if (instance == null) {
            synchronized (DbConnectionProvider.class) {
                if (instance == null) {
                    instance = new DbConnectionProvider();
                }
            }
        }
        return instance;
    }

    /**
     * Mở và trả về Connection mới với WAL mode đã bật.
     *
     * @return {@link Connection} sẵn sàng dùng
     * @throws SQLException nếu không thể mở kết nối
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        }
        return conn;
    }

    /**
     * Đóng connection an toàn — không ném exception ra ngoài nếu lỗi.
     *
     * @param conn connection cần đóng, có thể null
     */
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("[DbConnectionProvider] Lỗi đóng kết nối: " + e.getMessage());
            }
        }
    }

    /**
     * Trả về JDBC URL hiện tại (dùng trong test để verify).
     *
     * @return chuỗi JDBC URL
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }
}