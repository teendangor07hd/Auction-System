package com.bidhub.server.dao;

import com.bidhub.server.model.*;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test UserDao qua in-memory SQLite (jdbc:sqlite::memory:).
 * Dùng constructor inject(Connection) để tránh phụ thuộc vào DbConnectionProvider.
 */
class UserDaoTest {

    private Connection conn;
    private UserDao dao;

    @BeforeEach
    void setup() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement s = conn.createStatement()) {
            s.execute("""
          CREATE TABLE users (
            id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL, email TEXT NOT NULL,
            role TEXT NOT NULL, extra_int INTEGER NOT NULL DEFAULT 0,
            created_at TEXT NOT NULL, updated_at TEXT NOT NULL)
          """);
        }
        dao = new UserDao(conn); // inject in-memory connection
    }

    @AfterEach
    void teardown() throws SQLException { conn.close(); }

    @Test
    @DisplayName("save Bidder → findById trả về Optional<Bidder>")
    void save_bidder_findById_returnsBidder() {
        Bidder bidder = new Bidder("alice", "hash123", "alice@test.com");
        dao.save(bidder);

        Optional<User> found = dao.findById(bidder.getId());
        assertTrue(found.isPresent());
        assertInstanceOf(Bidder.class, found.get());
        assertEquals("alice", found.get().getUsername());
    }

    @Test
    @DisplayName("findByUsername không tồn tại → Optional.empty()")
    void findByUsername_notFound_returnsEmpty() {
        assertTrue(dao.findByUsername("ghost").isEmpty());
    }

    @Test
    @DisplayName("save 3 users → findAll size == 3, đúng subclass")
    void findAll_afterSaveThree_returnsCorrectSubclasses() {
        dao.save(new Bidder("bidder1", "h", "b1@test.com"));
        dao.save(new Seller("seller1", "h", "s1@test.com"));
        dao.save(new Admin("admin1", "h", "a1@test.com", 2));

        List<User> all = dao.findAll();
        assertEquals(3, all.size());
        assertTrue(all.stream().anyMatch(u -> u instanceof Bidder));
        assertTrue(all.stream().anyMatch(u -> u instanceof Seller));
        assertTrue(all.stream().anyMatch(u -> u instanceof Admin));
    }

    @Test
    @DisplayName("existsByUsername → true cho username đã save")
    void existsByUsername_existingUser_returnsTrue() {
        dao.save(new Bidder("charlie", "h", "c@test.com"));
        assertTrue(dao.existsByUsername("charlie"));
        assertFalse(dao.existsByUsername("stranger"));
    }

    @Test
    @DisplayName("findById sau save Seller → instanceof Seller")
    void save_seller_findById_returnsSeller() {
        Seller seller = new Seller("bob", "hash", "bob@test.com");
        dao.save(seller);

        Optional<User> found = dao.findById(seller.getId());
        assertTrue(found.isPresent());
        assertInstanceOf(Seller.class, found.get());
    }
}