package com.bidhub.server.network;

import com.bidhub.common.exception.AuthenticationException;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

class SecurityContextTest {

    private Session createAuthSession(String userId) throws Exception {
        try (ServerSocket srv = new ServerSocket(0)) {
            Socket client = new Socket("localhost", srv.getLocalPort());
            Session session = new Session(srv.accept());
            session.setAuthenticatedUserId(userId);
            client.close();
            return session;
        }
    }

    private Session createUnauthSession() throws Exception {
        try (ServerSocket srv = new ServerSocket(0)) {
            Socket client = new Socket("localhost", srv.getLocalPort());
            Session session = new Session(srv.accept());
            client.close();
            return session;
        }
    }

    @Test
    @DisplayName("requireAuthenticated voi session hop le tra ve userId")
    void requireAuthenticated_validSession() throws Exception {
        Session session = createAuthSession("user-001");
        assertEquals("user-001", SecurityContext.requireAuthenticated(session));
        session.disconnect();
    }

    @Test
    @DisplayName("requireAuthenticated voi session null nem AuthenticationException")
    void requireAuthenticated_nullSession() {
        assertThrows(AuthenticationException.class,
                () -> SecurityContext.requireAuthenticated(null));
    }

    @Test
    @DisplayName("requireAuthenticated voi session chua login nem AuthenticationException")
    void requireAuthenticated_unauthSession() throws Exception {
        Session session = createUnauthSession();
        assertThrows(AuthenticationException.class,
                () -> SecurityContext.requireAuthenticated(session));
        session.disconnect();
    }
}