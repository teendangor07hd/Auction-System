package com.bidhub.server.dao;

import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.AuditLog;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogDaoTest {

    private Connection conn;
    private AuditLogDao dao;

    @BeforeEach
    void setup() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement s = conn.createStatement()) {
            s.execute("""
          CREATE TABLE audit_logs (
            id TEXT PRIMARY KEY, user_id TEXT, action TEXT NOT NULL,
            details TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL)
          """);
        }
        dao = new AuditLogDao(conn);
    }

    @AfterEach
    void teardown() throws SQLException {
        conn.close();
    }

    @Test
    @DisplayName("save → findAll trả về đúng bản ghi vừa lưu")
    void save_findAll_returnsRecord() {
        dao.save(new AuditLog("user-1", AuditActions.USER_LOGIN, "{}"));
        List<AuditLog> all = dao.findAll();

        assertEquals(1, all.size());
        assertEquals(AuditActions.USER_LOGIN, all.get(0).getAction());
        assertEquals("user-1", all.get(0).getUserId());
    }

    @Test
    @DisplayName("findByUserId chỉ trả về log của đúng user, không lẫn user khác")
    void findByUserId_filtersCorrectly() {
        dao.save(new AuditLog("user-A", AuditActions.USER_LOGIN, "{}"));
        dao.save(new AuditLog("user-B", AuditActions.USER_LOGIN, "{}"));
        dao.save(new AuditLog("user-A", AuditActions.PLACE_BID, "{}"));

        List<AuditLog> resultA = dao.findByUserId("user-A");
        assertEquals(2, resultA.size());
        assertTrue(resultA.stream().allMatch(l -> "user-A".equals(l.getUserId())));
    }

    @Test
    @DisplayName("findByAction lọc đúng action, không trả action khác")
    void findByAction_onlyMatchingAction() {
        dao.save(new AuditLog("u1", AuditActions.USER_LOGIN, "{}"));
        dao.save(new AuditLog("u2", AuditActions.PLACE_BID, "{}"));
        dao.save(new AuditLog("u3", AuditActions.USER_LOGIN, "{}"));

        List<AuditLog> logins = dao.findByAction(AuditActions.USER_LOGIN);
        assertEquals(2, logins.size());
        assertTrue(logins.stream().allMatch(l -> AuditActions.USER_LOGIN.equals(l.getAction())));
    }

    @Test
    @DisplayName("findRecent(3) với 10 bản ghi → tối đa 3 kết quả")
    void findRecent_limitsResults() {
        for (int i = 0; i < 10; i++) {
            dao.save(new AuditLog("user-" + i, AuditActions.PLACE_BID, "{}"));
        }
        List<AuditLog> recent = dao.findRecent(3);
        assertTrue(recent.size() <= 3);
    }

    @Test
    @DisplayName("save userId=null (system action) → không crash, bản ghi lưu được")
    void save_nullUserId_succeeds() {
        dao.save(new AuditLog(null, AuditActions.AUCTION_CLOSED, "{\"auctionId\":\"a-1\"}"));

        List<AuditLog> all = dao.findAll();
        assertEquals(1, all.size());
        assertNull(all.get(0).getUserId());
        assertEquals("{\"auctionId\":\"a-1\"}", all.get(0).getDetails());
    }

    @Test
    @DisplayName("findRecent(0) → ném IllegalArgumentException")
    void findRecent_zeroLimit_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> dao.findRecent(0));
    }

    @Test
    @DisplayName("findRecent(-1) → ném IllegalArgumentException (không trả về toàn bảng)")
    void findRecent_negativeLimit_throwsException() {
        // Đảm bảo LIMIT -1 không âm thầm trả về tất cả rows
        for (int i = 0; i < 5; i++) {
            dao.save(new AuditLog("u" + i, AuditActions.PLACE_BID, "{}"));
        }
        assertThrows(IllegalArgumentException.class, () -> dao.findRecent(-1));
    }
}