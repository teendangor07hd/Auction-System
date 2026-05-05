package com.bidhub.server.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @Test
    @DisplayName("hashPassword tra ve chuoi hex 64 ky tu")
    void hashPassword_returns64CharHex() {
        String hash = AuthService.hashPassword("secret");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("hashPassword cung input luon tra ve cung output (deterministic)")
    void hashPassword_deterministic() {
        String hash1 = AuthService.hashPassword("password123");
        String hash2 = AuthService.hashPassword("password123");
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("verifyPassword dung mat khau tra ve true")
    void verifyPassword_correctPassword() {
        String hash = AuthService.hashPassword("mypassword");
        assertTrue(AuthService.verifyPassword("mypassword", hash));
    }

    @Test
    @DisplayName("verifyPassword sai mat khau tra ve false")
    void verifyPassword_wrongPassword() {
        String hash = AuthService.hashPassword("mypassword");
        assertFalse(AuthService.verifyPassword("wrongpassword", hash));
    }

    @Test
    @DisplayName("generateToken tra ve UUID format hop le")
    void generateToken_validUUID() {
        String token = AuthService.generateToken();
        assertNotNull(token);
        assertTrue(token.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("2 lan generateToken tra ve token khac nhau")
    void generateToken_unique() {
        String t1 = AuthService.generateToken();
        String t2 = AuthService.generateToken();
        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("hashPassword chuoi rong khong crash")
    void hashPassword_emptyString() {
        String hash = AuthService.hashPassword("");
        assertEquals(64, hash.length());
    }

    @Test
    @DisplayName("verifyPassword voi hash sai format tra ve false")
    void verifyPassword_invalidHash() {
        assertFalse(AuthService.verifyPassword("test", "nothex"));
    }
}