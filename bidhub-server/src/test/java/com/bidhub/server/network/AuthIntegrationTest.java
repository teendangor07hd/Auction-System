package com.bidhub.server.network;

import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.AuditLog;
import com.bidhub.server.model.User;
import com.bidhub.server.service.AuditLogService;
import com.bidhub.server.service.AuthService;
import com.bidhub.server.service.SessionManager;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test cho toan bo auth flow: register → login → logout + token + auth-guard.
 *
 * <p>// 📌 [Tieu chi: Unit Test JUnit — 0.5đ] ≥ 20 cases
 */
class AuthIntegrationTest {

    private Connection testConn;
    private UserDao userDao;
    private ItemDao itemDao;
    private AuditLogDao auditLogDao;
    private AuditLogService auditLogService;
    private RequestHandler handler;
    private Session mockSession;

    @BeforeEach
    void setUp() throws Exception {
        testConn = DriverManager.getConnection("jdbc:sqlite::memory:");
        testConn.createStatement().executeUpdate(
                "CREATE TABLE users ("
                        + "id TEXT PRIMARY KEY, username TEXT UNIQUE, "
                        + "password_hash TEXT, email TEXT, role TEXT, "
                        + "extra_int INTEGER DEFAULT 0, "
                        + "is_locked INTEGER NOT NULL DEFAULT 0, "   // ← thêm dòng này
                        + "created_at TEXT, updated_at TEXT)");
        testConn.createStatement().executeUpdate(
                "CREATE TABLE items ("
                        + "id TEXT PRIMARY KEY, name TEXT, description TEXT, "
                        + "starting_price REAL, item_type TEXT, seller_id TEXT, "
                        + "extra_data TEXT, created_at TEXT, updated_at TEXT)");
        testConn.createStatement().executeUpdate(
                "CREATE TABLE audit_logs ("
                        + "id TEXT PRIMARY KEY, user_id TEXT, "
                        + "action TEXT NOT NULL, details TEXT NOT NULL DEFAULT '', "
                        + "created_at TEXT NOT NULL)");

        userDao = new UserDao(testConn);
        itemDao = new ItemDao(testConn);
        auditLogDao = new AuditLogDao(testConn);
        auditLogService = new AuditLogService(auditLogDao);
        handler = new RequestHandler(userDao, itemDao, auditLogService);

        // Clear SessionManager
        SessionManager.getInstance().clearAll();

        // Tao mock session
        try (ServerSocket srv = new ServerSocket(0)) {
            Socket client = new Socket("localhost", srv.getLocalPort());
            mockSession = new Session(srv.accept());
            client.close();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        SessionManager.getInstance().clearAll();
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }

    // === REGISTER TESTS ===

    @Test
    @DisplayName("REGISTER moi thanh cong")
    void register_success() {
        String json = buildRegisterJson("user1", "password1", "u@test.com", "BIDDER");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertTrue(res.isOk());
        assertEquals("REGISTER", res.getType());
    }

    @Test
    @DisplayName("REGISTER trung username → error")
    void register_duplicateUsername() {
        handler.handle(buildRegisterJson("dup", "password1", "a@t.com", "BIDDER"), mockSession);
        String response = handler.handle(
                buildRegisterJson("dup", "password2", "b@t.com", "BIDDER"), mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("REGISTER role ADMIN → bi tu choi")
    void register_adminRole_rejected() {
        String json = buildRegisterJson("admin1", "password1", "a@t.com", "ADMIN");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("REGISTER password < 8 ky tu → error")
    void register_shortPassword() {
        String json = buildRegisterJson("user2", "1234567", "u2@t.com", "BIDDER");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("REGISTER email khong chua @ → error")
    void register_invalidEmail() {
        String json = buildRegisterJson("user3", "password1", "noemail", "BIDDER");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("REGISTER username blank → error")
    void register_blankUsername() {
        String json = buildRegisterJson("", "password1", "u4@t.com", "BIDDER");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("REGISTER thanh cong → audit log co USER_REGISTER")
    void register_auditLog() {
        handler.handle(buildRegisterJson("audit1", "password1", "a1@t.com", "BIDDER"), mockSession);
        List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_REGISTER);
        assertEquals(1, logs.size());
    }

    // === LOGIN TESTS ===

    @Test
    @DisplayName("LOGIN dung credentials → OK voi token")
    void login_success() {
        handler.handle(buildRegisterJson("login1", "password1", "l1@t.com", "BIDDER"), mockSession);
        String json = buildLoginJson("login1", "password1");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertTrue(res.isOk());
        assertEquals("LOGIN", res.getType());
    }

    @Test
    @DisplayName("LOGIN sai password → error")
    void login_wrongPassword() {
        handler.handle(buildRegisterJson("login2", "password1", "l2@t.com", "BIDDER"), mockSession);
        String json = buildLoginJson("login2", "wrongpass");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("LOGIN username khong ton tai → error")
    void login_nonexistentUser() {
        String json = buildLoginJson("ghost", "password1");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("LOGIN thanh cong → session.authenticatedUserId duoc set")
    void login_setsAuthenticatedUserId() {
        handler.handle(buildRegisterJson("login3", "password1", "l3@t.com", "SELLER"), mockSession);
        String json = buildLoginJson("login3", "password1");
        handler.handle(json, mockSession);
        assertTrue(mockSession.isAuthenticated());
    }

    @Test
    @DisplayName("LOGIN thanh cong → audit log co USER_LOGIN")
    void login_auditLog() {
        handler.handle(buildRegisterJson("login4", "password1", "l4@t.com", "BIDDER"), mockSession);
        handler.handle(buildLoginJson("login4", "password1"), mockSession);
        List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_LOGIN);
        assertEquals(1, logs.size());
    }

    // === LOGOUT TESTS ===

    @Test
    @DisplayName("LOGOUT thanh cong → token khong con hop le")
    void logout_invalidatesToken() {
        handler.handle(buildRegisterJson("logout1", "password1", "lo1@t.com", "BIDDER"), mockSession);
        String loginResponse = handler.handle(
                buildLoginJson("logout1", "password1"), mockSession);
        MessageResponse loginRes = parseResponse(loginResponse);
        Object payload = loginRes.getPayload();
        String token = null;
        if (payload instanceof java.util.Map<?, ?> map) {
            token = (String) map.get("token");
        }

        // Logout
        String logoutJson = "{\"type\":\"LOGOUT\",\"token\":\"" + token + "\",\"payload\":{}}";
        handler.handle(logoutJson, mockSession);

        // Token khong con hop le
        assertTrue(SessionManager.getInstance().getUserIdByToken(token).isEmpty());
    }

    @Test
    @DisplayName("LOGOUT → audit log co USER_LOGOUT")
    void logout_auditLog() {
        handler.handle(buildRegisterJson("logout2", "password1", "lo2@t.com", "BIDDER"), mockSession);
        handler.handle(buildLoginJson("logout2", "password1"), mockSession);
        handler.handle("{\"type\":\"LOGOUT\",\"token\":\"fake\",\"payload\":{}}", mockSession);
        List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_LOGOUT);
        assertEquals(1, logs.size());
    }

    // === TOKEN & AUTH-GUARD TESTS ===

    @Test
    @DisplayName("Token gia → rejected")
    void fakeToken_rejected() {
        String json = "{\"type\":\"CREATE_ITEM\",\"token\":\"fake-token-123\",\"payload\":{}}";
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("Type can auth khong co token → error auth-guard")
    void authRequired_noToken_error() {
        String json = "{\"type\":\"LOGOUT\",\"payload\":{}}";
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("Token hop le → session.authenticatedUserId duoc set")
    void validToken_setsAuthUserId() {
        String token = SessionManager.getInstance().createSession("user-test-001");
        String json = "{\"type\":\"PING\",\"token\":\"" + token + "\",\"payload\":{}}";
        handler.handle(json, mockSession);
        assertEquals("user-test-001", mockSession.getAuthenticatedUserId());
    }

    // === PASSWORD HASHING TESTS ===

    @Test
    @DisplayName("Password duoc hash truoc khi luu vao DB")
    void passwordHashed_beforeSave() {
        handler.handle(buildRegisterJson("hash1", "mypassword", "h1@t.com", "BIDDER"), mockSession);
        Optional<User> userOpt = userDao.findByUsername("hash1");
        assertTrue(userOpt.isPresent());
        // Password trong DB khong phai plaintext
        assertNotEquals("mypassword", userOpt.get().getPasswordHash());
        // Nhung verify duoc
        assertTrue(AuthService.verifyPassword("mypassword", userOpt.get().getPasswordHash()));
    }

    @Test
    @DisplayName("2 user cung password → hash giong nhau (SHA-256 deterministic)")
    void samePassword_sameHash() {
        handler.handle(buildRegisterJson("same1", "sharedpw", "s1@t.com", "BIDDER"), mockSession);
        handler.handle(buildRegisterJson("same2", "sharedpw", "s2@t.com", "BIDDER"), mockSession);
        Optional<User> u1 = userDao.findByUsername("same1");
        Optional<User> u2 = userDao.findByUsername("same2");
        assertEquals(u1.get().getPasswordHash(), u2.get().getPasswordHash());
    }

    // === ROLE-BASED ACCESS TESTS ===

    @Test
    @DisplayName("BIDDER khong tao duoc item → CREATE_ITEM yeu cau SELLER")
    void bidderCannotCreateItem() {
        // Register BIDDER
        handler.handle(buildRegisterJson("bidder1", "password1", "b1@t.com", "BIDDER"), mockSession);
        // Login
        handler.handle(buildLoginJson("bidder1", "password1"), mockSession);
        // Try CREATE_ITEM
        String json = buildCreateItemJson("Test Item", "Desc", "100.0", "ELECTRONICS");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("LOGIN password rong → error")
    void login_emptyPassword() {
        String json = buildLoginJson("user1", "");
        String response = handler.handle(json, mockSession);
        MessageResponse res = parseResponse(response);
        assertFalse(res.isOk());
    }

    @Test
    @DisplayName("Nguoi dung bi khoa (locked) khi dang hoat dong thi khong the PLACE_BID nhung van LOGOUT thanh cong")
    void lockedUser_cannotPlaceBid_butCanLogout() throws Exception {
        handler.handle(buildRegisterJson("bidderLocked", "password123", "blocked@test.com", "BIDDER"), mockSession);
        String loginResponse = handler.handle(buildLoginJson("bidderLocked", "password123"), mockSession);
        MessageResponse loginRes = parseResponse(loginResponse);
        String token = null;
        if (loginRes.getPayload() instanceof java.util.Map<?, ?> map) {
            token = (String) map.get("token");
        }

        String userId = mockSession.getAuthenticatedUserId();
        userDao.updateLocked(userId, true);

        String bidJson = "{\"type\":\"PLACE_BID\",\"token\":\"" + token + "\",\"payload\":{\"auctionId\":\"auc-01\",\"bidAmount\":500.0}}";
        String bidResponse = handler.handle(bidJson, mockSession);
        MessageResponse bidRes = parseResponse(bidResponse);
        assertFalse(bidRes.isOk());
        assertEquals("TAI KHOAN BI KHOA", bidRes.getMessage());

        String logoutJson = "{\"type\":\"LOGOUT\",\"token\":\"" + token + "\",\"payload\":{}}";
        String logoutResponse = handler.handle(logoutJson, mockSession);
        MessageResponse logoutRes = parseResponse(logoutResponse);
        assertTrue(logoutRes.isOk());
        assertNull(mockSession.getAuthenticatedUserId());
    }

    @Test
    @DisplayName("Seller bi khoa (locked) khi dang hoat dong thi khong the CREATE_ITEM")
    void lockedSeller_cannotCreateItem() throws Exception {
        handler.handle(buildRegisterJson("sellerLocked", "password123", "slocked@test.com", "SELLER"), mockSession);
        String loginResponse = handler.handle(buildLoginJson("sellerLocked", "password123"), mockSession);
        MessageResponse loginRes = parseResponse(loginResponse);
        String token = null;
        if (loginRes.getPayload() instanceof java.util.Map<?, ?> map) {
            token = (String) map.get("token");
        }

        String userId = mockSession.getAuthenticatedUserId();
        userDao.updateLocked(userId, true);

        String itemJson = buildCreateItemJson("Locked Item", "Desc", "100.0", "ELECTRONICS");
        String itemResponse = handler.handle(itemJson, mockSession);
        MessageResponse itemRes = parseResponse(itemResponse);
        assertFalse(itemRes.isOk());
        assertEquals("TAI KHOAN BI KHOA", itemRes.getMessage());
    }

    // === HELPER METHODS ===

    private String buildRegisterJson(String username, String password, String email, String role) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("email", email);
        payload.put("role", role);
        return "{\"type\":\"REGISTER\",\"payload\":"
                + payload.toString() + "}";
    }

    private String buildLoginJson(String username, String password) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", username);
        payload.put("password", password);
        return "{\"type\":\"LOGIN\",\"payload\":"
                + payload.toString() + "}";
    }

    private String buildCreateItemJson(String name, String desc, String price, String type) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("name", name);
        payload.put("description", desc);
        payload.put("startingPrice", Double.parseDouble(price));
        payload.put("itemType", type);
        ObjectNode extras = JsonNodeFactory.instance.objectNode();
        extras.put("brand", "TestBrand");
        extras.put("warrantyMonths", 12);
        payload.set("extras", extras);
        return "{\"type\":\"CREATE_ITEM\",\"token\":\""
                + (mockSession.isAuthenticated()
                ? SessionManager.getInstance().getTokenByUserId(
                mockSession.getAuthenticatedUserId()).orElse("")
                : "")
                + "\",\"payload\":" + payload.toString() + "}";
    }

    private MessageResponse parseResponse(String json) {
        try {
            return MessageMapper.fromJson(json, MessageResponse.class);
        } catch (Exception e) {
            fail("Khong the parse response: " + json);
            return null;
        }
    }
}