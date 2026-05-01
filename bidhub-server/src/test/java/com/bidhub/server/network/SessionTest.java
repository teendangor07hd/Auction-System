package com.bidhub.server.network;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @Test
    @DisplayName("Session mới → chưa authenticated, sessionId không null")
    void newSession_notAuthenticated() throws Exception {
        try (ServerSocket srv = new ServerSocket(0)) {
            Socket client = new Socket("localhost", srv.getLocalPort());
            Session session = new Session(srv.accept());

            assertFalse(session.isAuthenticated());
            assertNotNull(session.getSessionId());
            assertNull(session.getAuthenticatedUserId());

            client.close();
            session.disconnect();
        }
    }

    @Test
    @DisplayName("setAuthenticatedUserId → isAuthenticated() = true")
    void setUserId_authenticated() throws Exception {
        try (ServerSocket srv = new ServerSocket(0)) {
            Socket client = new Socket("localhost", srv.getLocalPort());
            Session session = new Session(srv.accept());

            session.setAuthenticatedUserId("user-001");

            assertTrue(session.isAuthenticated());
            assertEquals("user-001", session.getAuthenticatedUserId());

            client.close();
            session.disconnect();
        }
    }

    @Test
    @DisplayName("sendMessage() → client đọc được đúng nội dung")
    void sendMessage_clientReceives() throws Exception {
        try (ServerSocket srv = new ServerSocket(0)) {
            Socket client = new Socket("localhost", srv.getLocalPort());
            Session session = new Session(srv.accept());

            session.sendMessage("{\"status\":\"OK\"}");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
            assertEquals("{\"status\":\"OK\"}", reader.readLine());

            client.close();
            session.disconnect();
        }
    }

    @Test
    @DisplayName("2 Session → sessionId luôn unique")
    void sessionIds_unique() throws Exception {
        try (ServerSocket srv = new ServerSocket(0)) {
            Socket c1 = new Socket("localhost", srv.getLocalPort());
            Session s1 = new Session(srv.accept());
            Socket c2 = new Socket("localhost", srv.getLocalPort());
            Session s2 = new Session(srv.accept());

            assertNotEquals(s1.getSessionId(), s2.getSessionId());

            c1.close(); s1.disconnect();
            c2.close(); s2.disconnect();
        }
    }
}