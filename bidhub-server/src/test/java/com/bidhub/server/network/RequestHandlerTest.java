package com.bidhub.server.network;

import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageResponse;
import org.junit.jupiter.api.*;
import java.net.ServerSocket;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

class RequestHandlerTest {

    private RequestHandler handler;
    private Session session;
    private ServerSocket srv;
    private Socket clientSocket;

    @BeforeEach
    void setup() throws Exception {
        handler = new RequestHandler();
        srv = new ServerSocket(0);
        clientSocket = new Socket("localhost", srv.getLocalPort());
        session = new Session(srv.accept());
    }

    @AfterEach
    void teardown() throws Exception {
        session.disconnect();
        clientSocket.close();
        srv.close();
    }

    @Test
    @DisplayName("PING → status OK, message=pong")
    void ping_returnsOk() throws Exception {
        String resp = handler.handle("{\"type\":\"PING\",\"payload\":{}}", session);
        MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
        assertEquals("OK", r.getStatus());
        assertEquals("PING", r.getType());
    }

    @Test
    @DisplayName("Lệnh không tồn tại → status ERROR")
    void unknownCommand_returnsError() throws Exception {
        String resp = handler.handle("{\"type\":\"GHOST\",\"payload\":{}}", session);
        MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
        assertEquals("ERROR", r.getStatus());
    }

    @Test
    @DisplayName("Malformed JSON → ERROR, không crash")
    void malformedJson_returnsErrorNoException() throws Exception {
        String resp = handler.handle("this is not json", session);
        MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
        assertEquals("ERROR", r.getStatus());
    }

    @Test
    @DisplayName("PLACE_BID chưa login → ERROR 'chưa đăng nhập'")
    void authRequired_unauthSession_returnsError() throws Exception {
        assertFalse(session.isAuthenticated());
        String resp = handler.handle("{\"type\":\"PLACE_BID\",\"payload\":{}}", session);
        MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
        assertEquals("ERROR", r.getStatus());
        assertTrue(r.getMessage().contains("chưa đăng nhập"));
    }

    @Test
    @DisplayName("type=null trong JSON → không crash, trả về ERROR")
    void nullType_returnsError() throws Exception {
        String resp = handler.handle("{\"payload\":{}}", session);
        MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
        assertEquals("ERROR", r.getStatus());
    }
}