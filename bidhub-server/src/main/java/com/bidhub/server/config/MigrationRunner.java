package com.bidhub.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Đọc {@code schema.sql} từ classpath và thực thi khi server khởi động.
 * Dùng {@code CREATE TABLE IF NOT EXISTS} nên an toàn khi gọi nhiều lần.
 */
public final class MigrationRunner {

    private MigrationRunner() {}

    /**
     * Chạy toàn bộ schema.sql — tạo bảng nếu chưa tồn tại.
     *
     * @throws RuntimeException nếu không tìm thấy schema.sql hoặc SQL lỗi
     */
    public static void run() {
        String sql = loadSchemaSql();
        Connection conn = null;
        try {
            conn = DbConnectionProvider.getInstance().getConnection();
            try (Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            // 📌 [Tieu chi: Quan ly nguoi dung — migration is_locked cho DB cu]
            // Migration: them cot is_locked vao bang users (cho DB da tao tu Tuan 3)
            String alterTableSql =
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_locked "
                            + "INTEGER NOT NULL DEFAULT 0";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(alterTableSql);
                System.out.println("[MigrationRunner] Cot is_locked da san sang.");
            } catch (SQLException e) {
                // Cot da ton tai hoac loi khac — khong block server startup
                System.err.println("[MigrationRunner] Canh bao migration is_locked: "
                        + e.getMessage());
            }
            System.out.println("[MigrationRunner] Schema đã sẵn sàng.");
        } catch (SQLException e) {
            throw new RuntimeException("Migration thất bại: " + e.getMessage(), e);
        } finally {
            DbConnectionProvider.getInstance().closeConnection(conn);
        }
    }

    private static String loadSchemaSql() {
        try (InputStream is = MigrationRunner.class.getResourceAsStream("/db/schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("Không tìm thấy /db/schema.sql trong classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được schema.sql: " + e.getMessage(), e);
        }
    }
}