package com.bidhub.server.service;

import com.bidhub.common.exception.UserNotFoundException;
import com.bidhub.common.exception.ValidationException;
import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.AuditLog;
import com.bidhub.server.model.Admin;
import com.bidhub.server.model.Bidder;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite cho AdminUserService — su dung in-memory SQLite.
 *
 * <p>// 📌 [Tieu chi: Testing — in-memory SQLite cho DAO test]
 */
class AdminUserServiceTest {

  private Connection conn;
  private UserDao userDao;
  private AuditLogDao auditLogDao;
  private AuditLogService auditLogService;
  private AdminUserService adminService;

  @BeforeEach
  void setUp() throws Exception {
    conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    // Tao schema bang users
    conn.createStatement().execute(
        "CREATE TABLE IF NOT EXISTS users ("
        + "id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, "
        + "password_hash TEXT NOT NULL, email TEXT, "
        + "role TEXT NOT NULL, is_locked INTEGER NOT NULL DEFAULT 0, "
        + "extra_int INTEGER NOT NULL DEFAULT 0, "
        + "created_at TEXT NOT NULL, updated_at TEXT NOT NULL)");
    conn.createStatement().execute(
        "CREATE TABLE IF NOT EXISTS audit_logs ("
        + "id TEXT PRIMARY KEY, user_id TEXT, "
        + "action TEXT NOT NULL, details TEXT NOT NULL DEFAULT '', "
        + "created_at TEXT NOT NULL)");

    userDao = new UserDao(conn);
    auditLogDao = new AuditLogDao(conn);
    auditLogService = new AuditLogService(auditLogDao);
    adminService = new AdminUserService(userDao, auditLogService);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (conn != null && !conn.isClosed()) {
      conn.createStatement().execute("DROP TABLE IF EXISTS users");
      conn.createStatement().execute("DROP TABLE IF EXISTS audit_logs");
      conn.close();
    }
  }

  /**
   * Tao va luu user voi id, username va role chi dinh.
   * Doc goc dung new Bidder(id, username, hash, email) nhung Bidder khong co
   * constructor 4-arg. Adapt: tao Bidder/Admin binh thuong (auto-gen id),
   * sau do dung id auto-gen de test.
   */
  private User createAndSaveUser(String id, String username, UserRole role) {
    User user;
    if (role == UserRole.ADMIN) {
      user = new Admin(username,
          AuthService.hashPassword("password123"),
          username + "@email.com", 1);
    } else {
      user = new Bidder(username,
          AuthService.hashPassword("password123"),
          username + "@email.com");
    }
    // Doc goc dung id truyen vao, nhung constructor tu tao UUID.
    // Test van dung vi ta dung user.getId() thay vi hardcode id.
    userDao.save(user);
    return user;
  }

  @Test
  @DisplayName("listAllUsers — sau save 3 users tra ve 3 users")
  void listAllUsers_threeUsers_returnsThree() {
    createAndSaveUser("u1", "bidder1", UserRole.BIDDER);
    createAndSaveUser("u2", "seller1", UserRole.BIDDER);
    createAndSaveUser("u3", "bidder2", UserRole.BIDDER);

    List<User> users = adminService.listAllUsers();
    assertEquals(3, users.size());
  }

  @Test
  @DisplayName("lockUser — isLocked tra ve true sau khi khoa")
  void lockUser_success_isLockedTrue() {
    User user = createAndSaveUser("u1", "bidder1", UserRole.BIDDER);

    adminService.lockUser(user.getId(), "admin-001");

    User locked = userDao.findById(user.getId()).orElseThrow();
    assertTrue(locked.isLocked());
  }

  @Test
  @DisplayName("unlockUser — isLocked tra ve false sau khi mo khoa")
  void unlockUser_afterLock_isLockedFalse() {
    User user = createAndSaveUser("u1", "bidder1", UserRole.BIDDER);
    adminService.lockUser(user.getId(), "admin-001");

    adminService.unlockUser(user.getId(), "admin-001");

    User unlocked = userDao.findById(user.getId()).orElseThrow();
    assertFalse(unlocked.isLocked());
  }

  @Test
  @DisplayName("lockUser — user khong ton tai nem UserNotFoundException")
  void lockUser_notFound_throwsException() {
    assertThrows(UserNotFoundException.class,
        () -> adminService.lockUser("nonexistent", "admin-001"));
  }

  @Test
  @DisplayName("lockUser — khoa Admin nem ValidationException")
  void lockUser_lockAdmin_throwsValidationException() {
    // Tao Admin user
    User admin = createAndSaveUser("admin-1", "adminUser", UserRole.ADMIN);

    assertThrows(ValidationException.class,
        () -> adminService.lockUser(admin.getId(), "admin-002"));
  }

  @Test
  @DisplayName("lockUser — audit log USER_LOCKED duoc tao")
  void lockUser_auditLogCreated() {
    User user = createAndSaveUser("u1", "bidder1", UserRole.BIDDER);

    adminService.lockUser(user.getId(), "admin-001");

    List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_LOCKED);
    assertFalse(logs.isEmpty());
    assertEquals("admin-001", logs.get(0).getUserId());
  }

  @Test
  @DisplayName("unlockUser — audit log USER_UNLOCKED duoc tao")
  void unlockUser_auditLogCreated() {
    User user = createAndSaveUser("u1", "bidder1", UserRole.BIDDER);
    adminService.lockUser(user.getId(), "admin-001");

    adminService.unlockUser(user.getId(), "admin-001");

    List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_UNLOCKED);
    assertFalse(logs.isEmpty());
  }

  @Test
  @DisplayName("unlockUser — user khong ton tai nem UserNotFoundException")
  void unlockUser_notFound_throwsException() {
    assertThrows(UserNotFoundException.class,
        () -> adminService.unlockUser("nonexistent", "admin-001"));
  }
}
