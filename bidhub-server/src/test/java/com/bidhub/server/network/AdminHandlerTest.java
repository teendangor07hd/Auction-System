package com.bidhub.server.network;

import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageResponse;
import org.junit.jupiter.api.*;
import java.net.ServerSocket;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test cho 3 admin handlers trong RequestHandler:
 * handleGetUserList, handleLockUser, handleUnlockUser.
 *
 * <p>Test qua method {@code handle()} — gửi JSON request, nhan JSON response.
 * Các test này kiem tra auth guard và validation, Không cần DB that
 * (vi se nhan error truoc khi truy cap DB).
 *
 */
class AdminHandlerTest {

  private RequestHandler handler;
  private Session session;
  private ServerSocket srv;
  private Socket clientSocket;

  @BeforeEach
  void setUp() throws Exception {
    handler = new RequestHandler();
    srv = new ServerSocket(0);
    clientSocket = new Socket("localhost", srv.getLocalPort());
    session = new Session(srv.accept());
  }

  @AfterEach
  void tearDown() throws Exception {
    session.disconnect();
    clientSocket.close();
    srv.close();
  }

  // === GET_USER_LIST ===

  @Test
  @DisplayName("GET_USER_LIST chua login → ERROR 'chua dang nhap'")
  void getUserList_unauthenticated_returnsError() throws Exception {
    assertFalse(session.isAuthenticated());
    String resp = handler.handle(
        "{\"type\":\"GET_USER_LIST\",\"payload\":{}}", session);
    MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
    assertEquals("ERROR", r.getStatus());
    assertTrue(r.getMessage().contains("Ban chua dang nhap")
        || r.getMessage().contains("chua dang nhap"));
  }

  // === LOCK_USER ===

  @Test
  @DisplayName("LOCK_USER chua login → ERROR 'chua dang nhap'")
  void lockUser_unauthenticated_returnsError() throws Exception {
    assertFalse(session.isAuthenticated());
    String resp = handler.handle(
        "{\"type\":\"LOCK_USER\",\"payload\":{\"targetUserId\":\"u1\"}}", session);
    MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
    assertEquals("ERROR", r.getStatus());
    assertTrue(r.getMessage().contains("Ban chua dang nhap")
        || r.getMessage().contains("chua dang nhap"));
  }

  @Test
  @DisplayName("LOCK_USER voi targetUserId rong → ERROR validation")
  void lockUser_emptyTargetUserId_returnsError() throws Exception {
    // Gia lap da login (set authenticatedUserId)
    session.setAuthenticatedUserId("admin-fake");
    String resp = handler.handle(
        "{\"type\":\"LOCK_USER\",\"payload\":{\"targetUserId\":\"\"}}", session);
    MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
    assertEquals("ERROR", r.getStatus());
  }

  // === UNLOCK_USER ===

  @Test
  @DisplayName("UNLOCK_USER chua login → ERROR 'chua dang nhap'")
  void unlockUser_unauthenticated_returnsError() throws Exception {
    assertFalse(session.isAuthenticated());
    String resp = handler.handle(
        "{\"type\":\"UNLOCK_USER\",\"payload\":{\"targetUserId\":\"u1\"}}", session);
    MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
    assertEquals("ERROR", r.getStatus());
    assertTrue(r.getMessage().contains("Ban chua dang nhap")
        || r.getMessage().contains("chua dang nhap"));
  }

  @Test
  @DisplayName("UNLOCK_USER voi targetUserId rong → ERROR validation")
  void unlockUser_emptyTargetUserId_returnsError() throws Exception {
    session.setAuthenticatedUserId("admin-fake");
    String resp = handler.handle(
        "{\"type\":\"UNLOCK_USER\",\"payload\":{\"targetUserId\":\"\"}}", session);
    MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
    assertEquals("ERROR", r.getStatus());
  }

  // === GENERAL ===

  @Test
  @DisplayName("GET_USER_LIST, LOCK_USER, UNLOCK_USER deu require auth")
  void allAdminCommands_requireAuth() throws Exception {
    assertFalse(session.isAuthenticated());

    String[] commands = {"GET_USER_LIST", "LOCK_USER", "UNLOCK_USER"};
    for (String cmd : commands) {
      String resp = handler.handle(
          "{\"type\":\"" + cmd + "\",\"payload\":{}}", session);
      MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
      assertEquals("ERROR", r.getStatus(),
          cmd + " phai yeu cau dang nhap");
    }
  }

  @Test
  @DisplayName("LOCK_USER khong co field targetUserId → ERROR")
  void lockUser_missingTargetUserId_returnsError() throws Exception {
    session.setAuthenticatedUserId("admin-fake");
    String resp = handler.handle(
        "{\"type\":\"LOCK_USER\",\"payload\":{}}", session);
    MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
    assertEquals("ERROR", r.getStatus());
  }

  @Test
  @DisplayName("UNLOCK_USER khong co field targetUserId → ERROR")
  void unlockUser_missingTargetUserId_returnsError() throws Exception {
    session.setAuthenticatedUserId("admin-fake");
    String resp = handler.handle(
        "{\"type\":\"UNLOCK_USER\",\"payload\":{}}", session);
    MessageResponse r = MessageMapper.fromJson(resp, MessageResponse.class);
    assertEquals("ERROR", r.getStatus());
  }
}
