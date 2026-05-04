package com.bidhub.client.network;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientSessionTest {

    @BeforeEach
    void reset() {
        ClientSession.getInstance().logout();
    }

    @Test
    @DisplayName("getInstance() gọi 2 lần → cùng instance (Singleton)")
    void singleton_sameInstance() {
        assertSame(ClientSession.getInstance(), ClientSession.getInstance());
    }

    @Test
    @DisplayName("Khởi tạo → chưa đăng nhập, token null")
    void initial_notLoggedIn() {
        assertFalse(ClientSession.getInstance().isLoggedIn());
        assertNull(ClientSession.getInstance().getToken());
    }

    @Test
    @DisplayName("login() → isLoggedIn() = true, token và username đúng")
    void login_setsState() {
        ClientSession.getInstance().login("tok-123", "uid-1", "alice", "BIDDER");

        assertTrue(ClientSession.getInstance().isLoggedIn());
        assertEquals("tok-123", ClientSession.getInstance().getToken());
        assertEquals("alice", ClientSession.getInstance().getCurrentUsername());
        assertEquals("BIDDER", ClientSession.getInstance().getCurrentRole());
    }

    @Test
    @DisplayName("logout() → isLoggedIn() = false, tất cả field null")
    void logout_clearsAll() {
        ClientSession.getInstance().login("tok-abc", "uid-2", "bob", "SELLER");
        ClientSession.getInstance().logout();

        assertFalse(ClientSession.getInstance().isLoggedIn());
        assertNull(ClientSession.getInstance().getToken());
        assertNull(ClientSession.getInstance().getCurrentUserId());
    }
}