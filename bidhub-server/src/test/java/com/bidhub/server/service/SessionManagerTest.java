package com.bidhub.server.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();
        sessionManager.clearAll();
    }

    @AfterEach
    void tearDown() {
        sessionManager.clearAll();
    }

    @Test
    @DisplayName("getInstance tra ve cung instance (Singleton)")
    void getInstance_sameInstance() {
        SessionManager s1 = SessionManager.getInstance();
        SessionManager s2 = SessionManager.getInstance();
        assertSame(s1, s2);
    }

    @Test
    @DisplayName("createSession tra ve token UUID hop le")
    void createSession_returnsToken() {
        String token = sessionManager.createSession("user-001");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("getUserIdByToken voi token hop le tra ve userId")
    void getUserIdByToken_validToken() {
        String token = sessionManager.createSession("user-001");
        assertEquals("user-001", sessionManager.getUserIdByToken(token).orElse(null));
    }

    @Test
    @DisplayName("getUserIdByToken voi token khong ton tai tra ve empty")
    void getUserIdByToken_invalidToken() {
        assertTrue(sessionManager.getUserIdByToken("fake-token").isEmpty());
    }

    @Test
    @DisplayName("getUserIdByToken voi null token tra ve empty")
    void getUserIdByToken_nullToken() {
        assertTrue(sessionManager.getUserIdByToken(null).isEmpty());
    }

    @Test
    @DisplayName("invalidateSession xoa token khoi ca 2 map")
    void invalidateSession_removesToken() {
        String token = sessionManager.createSession("user-001");
        sessionManager.invalidateSession(token);
        assertTrue(sessionManager.getUserIdByToken(token).isEmpty());
        assertTrue(sessionManager.getTokenByUserId("user-001").isEmpty());
    }

    @Test
    @DisplayName("invalidateSession voi null token khong crash")
    void invalidateSession_nullToken() {
        assertDoesNotThrow(() -> sessionManager.invalidateSession(null));
    }

    @Test
    @DisplayName("login moi thay token cu — 1 user chi co 1 token")
    void createSession_replacesOldToken() {
        String oldToken = sessionManager.createSession("user-001");
        String newToken = sessionManager.createSession("user-001");

        // Token cu không con hop le
        assertTrue(sessionManager.getUserIdByToken(oldToken).isEmpty());
        // Token moi hop le
        assertEquals("user-001", sessionManager.getUserIdByToken(newToken).orElse(null));
    }

    @Test
    @DisplayName("isValidToken kiem tra token ton tai")
    void isValidToken() {
        String token = sessionManager.createSession("user-001");
        assertTrue(sessionManager.isValidToken(token));
        assertFalse(sessionManager.isValidToken("fake"));
    }

    @Test
    @DisplayName("activeSessionCount dem dung so phien")
    void activeSessionCount() {
        assertEquals(0, sessionManager.activeSessionCount());
        sessionManager.createSession("user-001");
        assertEquals(1, sessionManager.activeSessionCount());
        sessionManager.createSession("user-002");
        assertEquals(2, sessionManager.activeSessionCount());
    }
}