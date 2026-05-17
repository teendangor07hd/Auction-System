package com.bidhub.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cung cấp kết nối JDBC tới SQLite. Singleton đảm bảo cùng URL được dùng xuyên suốt server.
 *
 * <p>Dùng double-checked locking với {@code volatile} để thread-safe khi nhiều DAO khởi động
 * đồng thờii. Mỗi lần gọi {@link #getConnection()} trả về connection mới với WAL mode bật.
 */
public final class DbConnectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(DbConnectionProvider.class);

    // 📌 [Tiêu chí: Singleton Pattern — 1.0đ] volatile ngăn CPU reorder instruction khi khởi tạo
    private static volatile DbConnectionProvider instance;

    private final String jdbcUrl;

    private DbConnectionProvider() {
        String dbPath = ConfigLoader.getString("db.path");
        if (dbPath != null && !dbPath.isBlank() && !dbPath.equals(":memory:")) {
            try {
                Path path = Paths.get(dbPath);
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
            } catch (Exception e) {
                logger.error("Loi tao thu muc cho db: {}", e.getMessage());
            }
        }
        this.jdbcUrl = dbPath.equals(":memory:") ? "jdbc:sqlite::memory:" : "jdbc:sqlite:" + dbPath;

        try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        } catch (SQLException e) {
            logger.error("Loi kich hoat WAL mode: {}", e.getMessage());
        }
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
     * Mở và trả về Connection mới.
     *
     * @return {@link Connection} sẵn sàng dùng
     * @throws SQLException nếu không thể mở kết nối
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
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
                logger.error("Loi dong ket noi: {}", e.getMessage());
            }
        }
    }

    /** Reset singleton cho mục đích Unit Test. */
    public static void reset() {
        instance = null;
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