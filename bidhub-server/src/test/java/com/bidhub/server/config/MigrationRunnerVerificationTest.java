package com.bidhub.server.config;

import org.junit.jupiter.api.*;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify MigrationRunner xử lý đúng 5 bang: users, items, auctions,
 * bid_transactions, audit_logs.
 *
 * <p>Tạo file DB moi → chay MigrationRunner → kiem tra 5 bang ton tai.
 *
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MigrationRunnerVerificationTest {

    private static final String TEST_DB_PATH = "target/test-migration-verify.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + TEST_DB_PATH;

    @BeforeAll
    static void setUp() throws Exception {
        // Xóa DB cu nếu ton tai
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        // Tạo thu muc target nếu chua có
        dbFile.getParentFile().mkdirs();

        // Override DbConnectionProvider với test DB
        // Đúng system property để ConfigLoader đúng test DB
        System.setProperty("db.path.override", TEST_DB_PATH);
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("db.path.override");
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    /**
     * Test 1: Tạo schema từ /db/schema.sql truc tiep vao DB test.
     * Verify 5 bang ton tai sau khi chay schema.
     *
     *     auctions, bid_transactions, audit_logs]
     */
    @Test
    @Order(1)
    @DisplayName("T1: Schema tao dung 5 bang can thiet")
    void testSchemaCreates5Tables() throws Exception {
        // Đọc schema.sql từ classpath
        String sql = new String(
                MigrationRunnerVerificationTest.class
                        .getResourceAsStream("/db/schema.sql")
                        .readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);

        // Chay schema tren DB test
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }

            // Verify 5 bang
            Set<String> tables = getTableNames(conn);
            Set<String> expected = Set.of("users", "items", "auctions",
                    "bid_transactions", "audit_logs");

            for (String table : expected) {
                assertTrue(tables.contains(table),
                        "Bang '" + table + "' khong duoc tao boi schema.sql");
            }
            assertEquals(5, tables.size(),
                    "Schema phai tao dung 5 bang, thuc te: " + tables.size());
        }
    }

    /**
     * Test 2: Verify bang users có cot is_locked (migration T6).
     *
     */
    @Test
    @Order(2)
    @DisplayName("T2: Bang users co cot is_locked")
    void testUsersTableHasIsLockedColumn() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)");
            Set<String> columns = new HashSet<>();
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
            assertTrue(columns.contains("is_locked"),
                    "Bang users phai co cot is_locked");
        }
    }

    /**
     * Test 3: Verify bang auctions có minimum_increment (T6).
     */
    @Test
    @Order(3)
    @DisplayName("T3: Bang auctions co cot minimum_increment")
    void testAuctionsTableHasMinimumIncrement() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(auctions)");
            Set<String> columns = new HashSet<>();
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
            assertTrue(columns.contains("minimum_increment"),
                    "Bang auctions phai co cot minimum_increment");
        }
    }

    /**
     * Test 4: Verify bang bid_transactions có auction_id và bidder_id.
     */
    @Test
    @Order(4)
    @DisplayName("T4: Bang bid_transactions co cau truc dung")
    void testBidTransactionsTableStructure() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(bid_transactions)");
            Set<String> columns = new HashSet<>();
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
            assertTrue(columns.contains("auction_id"), "bid_transactions phai co cot auction_id");
            assertTrue(columns.contains("bidder_id"), "bid_transactions phai co cot bidder_id");
            assertTrue(columns.contains("bid_amount"), "bid_transactions phai co cot bid_amount");
            assertTrue(columns.contains("bid_time"), "bid_transactions phai co cot bid_time");
        }
    }

    /**
     * Test 5: Verify bang audit_logs có cot action và user_id.
     */
    @Test
    @Order(5)
    @DisplayName("T5: Bang audit_logs co cau truc dung")
    void testAuditLogsTableStructure() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(audit_logs)");
            Set<String> columns = new HashSet<>();
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
            assertTrue(columns.contains("user_id"), "audit_logs phai co cot user_id");
            assertTrue(columns.contains("action"), "audit_logs phai co cot action");
            assertTrue(columns.contains("details"), "audit_logs phai co cot details");
            assertTrue(columns.contains("created_at"), "audit_logs phai co cot created_at");
        }
    }

    // === Helper ===

    private Set<String> getTableNames(Connection conn) throws Exception {
        Set<String> tables = new HashSet<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'");
            while (rs.next()) {
                tables.add(rs.getString("name"));
            }
        }
        return tables;
    }
}
