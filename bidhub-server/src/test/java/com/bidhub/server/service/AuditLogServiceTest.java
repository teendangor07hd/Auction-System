package com.bidhub.server.service;

import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.model.AuditLog;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogServiceTest {

    private Connection testConn;
    private AuditLogDao auditLogDao;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() throws Exception {
        testConn = DriverManager.getConnection("jdbc:sqlite::memory:");
        // Tạo bang audit_logs trong in-memory DB
        testConn.createStatement().executeUpdate(
                "CREATE TABLE audit_logs ("
                        + "id TEXT PRIMARY KEY, "
                        + "user_id TEXT, "
                        + "action TEXT NOT NULL, "
                        + "details TEXT NOT NULL DEFAULT '', "
                        + "created_at TEXT NOT NULL)");
        auditLogDao = new AuditLogDao(testConn);
        auditLogService = new AuditLogService(auditLogDao);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }

    @Test
    @DisplayName("log() voi action hop le → auditLogDao.findAll() co ban ghi moi")
    void log_validAction_savesRecord() {
        auditLogService.log("user-001", "USER_LOGIN", "{}");

        List<AuditLog> logs = auditLogDao.findAll();
        assertEquals(1, logs.size());
        assertEquals("user-001", logs.get(0).getUserId());
        assertEquals("USER_LOGIN", logs.get(0).getAction());
    }

    @Test
    @DisplayName("log() khi DAO nem exception → khong nem exception ra ngoai")
    void log_daoThrows_doesNotPropagate() throws Exception {
        // Đóng connection để buoc DAO ném SQLException
        testConn.close();

        // Không ném exception ra ngoai
        assertDoesNotThrow(() ->
                auditLogService.log("user-001", "USER_LOGIN", "{}"));
    }

    @Test
    @DisplayName("log() voi userId null (system action) → luu duoc")
    void log_nullUserId_savesRecord() {
        auditLogService.log(null, "AUCTION_CLOSED", "{}");

        List<AuditLog> logs = auditLogDao.findAll();
        assertEquals(1, logs.size());
        assertNull(logs.get(0).getUserId());
        assertEquals("AUCTION_CLOSED", logs.get(0).getAction());
    }

    @Test
    @DisplayName("log() nhieu lan → findAll() tra ve dung so luong")
    void log_multipleTimes_correctCount() {
        auditLogService.log("user-001", "USER_LOGIN", "{}");
        auditLogService.log("user-001", "PLACE_BID", "{\"auctionId\":\"a1\"}");
        auditLogService.log("user-002", "USER_REGISTER", "{}");

        List<AuditLog> logs = auditLogDao.findAll();
        assertEquals(3, logs.size());
    }

    @Test
    @DisplayName("log() voi details '{}' → ban ghi luu dung details")
    void log_emptyDetails_savesCorrectly() {
        auditLogService.log("user-001", "USER_LOGOUT", "{}");

        List<AuditLog> logs = auditLogDao.findAll();
        assertEquals(1, logs.size());
        assertEquals("{}", logs.get(0).getDetails());
    }

    @Test
    @DisplayName("constructor production tao AuditLogService binh thuong")
    void constructorProduction() {
        assertDoesNotThrow(() -> new AuditLogService());
    }
}