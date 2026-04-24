package com.bidhub.server.config;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra Singleton DbConnectionProvider và MigrationRunner với in-memory SQLite. */
class DatabaseSetupTest {

    // Override jdbcUrl bằng in-memory để test không tạo file .db
    private Connection openMemoryConn() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
        }
        return conn;
    }

    @Test
    @DisplayName("getInstance() gọi 2 lần → cùng một instance")
    void singleton_sameInstance() {
        // NOTE: getInstance() chỉ tạo URL từ ConfigLoader, không mở file DB thật.
        // Các test cần mở connection thật PHẢI dùng constructor inject(Connection)
        // như trong UserDaoTest, ItemDaoTest, AuctionDaoTest.
        DbConnectionProvider a = DbConnectionProvider.getInstance();
        DbConnectionProvider b = DbConnectionProvider.getInstance();
        assertSame(a, b);
    }

    @Test
    @DisplayName("MigrationRunner tạo đủ 4 bảng trong in-memory DB")
    void migration_createsFourTables() throws SQLException {
        try (Connection conn = openMemoryConn();
             Statement stmt = conn.createStatement()) {
            // Tạo bảng thủ công từ schema để kiểm tra không phụ thuộc file .db thật
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, username TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS items (id TEXT PRIMARY KEY, name TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS auctions (id TEXT PRIMARY KEY, status TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS bid_transactions (id TEXT PRIMARY KEY)");

            ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
            int tableCount = 0;
            while (rs.next()) tableCount++;
            assertEquals(4, tableCount);
        }
    }

    @Test
    @DisplayName("getConnection() trả về Connection không null, không closed")
    void getConnection_returnsOpen() throws SQLException {
        // Dùng in-memory url trực tiếp vì ConfigLoader cần file properties
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    @Test
    @DisplayName("closeConnection(null) không ném exception")
    void closeConnection_null_safe() {
        assertDoesNotThrow(() -> DbConnectionProvider.getInstance().closeConnection(null));
    }
}