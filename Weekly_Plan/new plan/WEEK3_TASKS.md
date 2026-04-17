# 📋 TUẦN 3 — BÀI TẬP CHI TIẾT: DAO Layer + SQLite + MVC Routing

> **Kick-off meeting:** Thứ 2 20/04/2026 (tối)
> **Mid-week check-in:** Thứ 5 23/04/2026 (tối)
> **Deadline nộp bài:** Thứ 7 25/04/2026, 23:59
> **Review & Merge:** Chủ nhật 26/04/2026 (sáng)

---

## 🎯 MỤC TIÊU TUẦN 3

Tuần này kết nối domain model (tuần 2) với database và màn hình. Cuối tuần 3, cả nhóm phải có:

- ✅ SQLite kết nối thành công, 4 bảng được tạo tự động khi server khởi động
- ✅ `UserDao` + `ItemDao` CRUD đầy đủ bằng `PreparedStatement` (Quốc Minh)
- ✅ `AuctionDao` + `BidDao` với các query phức tạp hơn (Khoa)
- ✅ `DbConnectionProvider` Singleton thread-safe — chỉ 1 connection tới SQLite (Đăng)
- ✅ `ViewRouter` Singleton điều hướng màn hình JavaFX — mỗi `navigateTo()` load đúng FXML (Công Minh)
- ✅ 3 FXML skeleton mới: `AuctionListView`, `AuctionDetailView`, `CreateItemView` (Công Minh)
- ✅ Tổng ≥ 25 test cases mới — bao gồm DAO test dùng `jdbc:sqlite::memory:` (cộng dồn ≥ 65 toàn project)

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** tiêu chí barem: **Áp dụng design pattern phù hợp (1.0đ)** — Singleton cho `DbConnectionProvider` và `ViewRouter`; **Thiết kế kiến trúc Client-Server — chỉ Server truy cập database (0.5đ)**; **Áp dụng MVC với JavaFX + FXML (0.5đ)** — ViewRouter + ContextAware interface. Tuần 3 là cầu nối từ domain model sang chức năng thực tế.

> [!CAUTION]
> **Tuyệt đối không tạo lại các class đã có từ tuần 1 và 2:**
> `ConfigLoader`, `ServerApp`, `BidHubApp`, `LoginController`, `Views`, `Entity`, `User`, `Bidder`, `Seller`, `Admin`, `UserRole`, `Item`, `Electronics`, `Art`, `Vehicle`, `ItemFactory`, `Auction`, `AuctionStatus`, `BidTransaction`, `BidHubException` và các subclass của nó.
>
> Mọi DAO class tuần này **import và dùng** các class trên. Nếu thấy mình đang copy code — dừng lại, dùng `import` thay thế.

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI HỌC (Tự học)

> [!CAUTION]
> Phần tự học này **trực tiếp liên quan đến task của tất cả 4 người**. JDBC là nền tảng cho cả 3 DAO task (Đăng, Quốc Minh, Khoa). Singleton cần hiểu để review `DbConnectionProvider` của Đăng và `ViewRouter` của Công Minh. Không hiểu → không review được → **rủi ro vấn đáp giảng viên**.

---

### Bài 0.1 — JDBC & PreparedStatement

**Tài liệu bắt buộc đọc:**
- https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html (phần "Connecting" và "Using PreparedStatements")

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. `Connection`, `PreparedStatement`, `ResultSet` là gì? Mối quan hệ giữa chúng? Cái nào phải đóng trước?
2. Tại sao PHẢI dùng `PreparedStatement` thay vì `Statement` + nối chuỗi SQL?
3. Demo SQL Injection: nếu dùng `"SELECT * FROM users WHERE username = '" + input + "'"` — hacker có thể nhập gì để bypass login?
4. `try-with-resources` và JDBC: tại sao viết `try (Connection conn = ...; PreparedStatement ps = ...)` an toàn hơn `finally { conn.close() }`?
5. `ResultSet.next()` hoạt động thế nào? Tại sao phải gọi trước khi đọc dữ liệu?
6. **[Câu hỏi nâng cao]** `jdbc:sqlite::memory:` khác `jdbc:sqlite:file.db` thế nào? Tại sao dùng in-memory cho unit test?

---

### Bài 0.2 — Singleton Pattern (Thread-safe)

**Tài liệu bắt buộc đọc:**
- https://refactoring.guru/design-patterns/singleton (phần "Thread-safe Singleton")

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. Tại sao Singleton cần `private constructor`? Nếu để `public` thì có thể xảy ra điều gì?
2. Vẽ sơ đồ double-checked locking: tại sao cần check `instance == null` 2 lần? Lần check thứ 2 bên trong `synchronized` có cần không?
3. Tại sao field `instance` phải khai báo `volatile`? Không có `volatile` thì CPU reordering gây ra lỗi gì?
4. `DbConnectionProvider.getInstance()` được gọi từ 50 threads đồng thời — điều gì xảy ra? Cần bao nhiêu Connection thực sự được tạo?
5. `ViewRouter` cũng là Singleton — nhưng single-threaded (JavaFX Application Thread). Có cần `volatile` và `synchronized` không? Tại sao?
6. **[Câu hỏi nâng cao]** So sánh 3 cách implement Singleton: (a) double-checked locking, (b) static inner class (Holder pattern), (c) Enum Singleton. Cách nào tốt nhất và tại sao?

---

### Bài 0.3 — DAO Pattern

**Tài liệu bắt buộc đọc:**
- https://www.oracle.com/java/technologies/dataaccessobject.html (đọc phần mô tả pattern)

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. DAO (Data Access Object) pattern giải quyết vấn đề gì? Không có DAO, code business logic sẽ trông như thế nào?
2. Trong BidHub, tại sao chỉ server-side mới có DAO? Client không có DAO — vậy client lấy data từ đâu?
3. Vẽ luồng: `handlePlaceBid(req)` → `BidValidator` → `BidDao.save()` → `AuctionDao.updateHighestBid()`. Mỗi bước làm gì?
4. `Optional<User>` vs `User` (có thể null) — tại sao DAO nên trả về `Optional` thay vì `null`?
5. `UserDao.findByUsername()` và `UserDao.save()` đều dùng `PreparedStatement` — nhưng `findByUsername` dùng `executeQuery()`, `save()` dùng `executeUpdate()`. Khác nhau thế nào?
6. **[Câu hỏi nâng cao]** Tại sao không tạo `AbstractDao<T>` generic? Ở BidHub, giữ DAO đơn giản (class thẳng, không kế thừa) có lợi ích gì khi bị hỏi vấn đáp?

---

### Bài 0.4 — JavaFX Scene Navigation & MVC

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. Vẽ luồng điều hướng: User click "Xem chi tiết" trên `AuctionListView` → `ViewRouter.navigateTo(Views.AUCTION_DETAIL, params)` → điều gì xảy ra tiếp theo? FXML được load như thế nào?
2. `ContextAware` interface có method `setContext(Map<String, Object> params)` — tại sao cần interface này? Không có nó thì sao?
3. Trong JavaFX MVC: `View` là file FXML, `Controller` là class Java, `Model` là gì ở BidHub?
4. Tại sao `ViewRouter` là Singleton thay vì truyền `Stage` qua constructor cho từng controller?
5. `FXMLLoader.load()` vs `FXMLLoader.getController()` — dùng khi nào? Phân biệt 2 cách load FXML.
6. **[Câu hỏi nâng cao]** `Platform.runLater()` khi nào cần trong context navigation? Nếu `navigateTo()` được gọi từ background thread thì có an toàn không?

---

## 🔨 PHẦN CÁ NHÂN — NHIỆM VỤ RIÊNG

> [!IMPORTANT]
> Mỗi người code trên **branch riêng**, KHÔNG push thẳng vào `main` hay `develop`. Đến Chủ nhật mới tạo PR để review và merge.
>
> **Phụ thuộc tuần này:**
> - **Đăng phải push sớm** (Thứ 3 21/04): `DbConnectionProvider` và `schema.sql` là nền tảng để Quốc Minh và Khoa chạy DAO test. Nếu Đăng chậm, Quốc Minh + Khoa viết code trước và mock DB sau.
> - **Công Minh làm độc lập** — ViewRouter không phụ thuộc DAO.
> - **Quốc Minh và Khoa** bắt đầu viết DAO sau khi Đăng push schema (hoặc tự tạo bảng tạm trong test bằng `jdbc:sqlite::memory:`).

---

## 👤 ĐĂNG — DbConnectionProvider + Schema + MigrationRunner

```
Branch: feature/tuan-3-dang-database-setup
Phụ thuộc: Kế thừa ConfigLoader từ tuần 1 (đã merge vào develop)
Deadline: Thứ 3 21/04 (PHẢI push sớm để Quốc Minh + Khoa chạy được DAO test)
```

📌 **[Tiêu chí điểm: Design Patterns — Singleton (1.0đ, phần DbConnectionProvider)] + [Kiến trúc Client-Server — chỉ Server truy cập DB (0.5đ)]**

### 📝 Mô tả bài tập

Bạn tạo **lớp truy cập database** cho toàn server: Singleton `DbConnectionProvider` đảm bảo chỉ 1 connection SQLite tồn tại, `schema.sql` định nghĩa 4 bảng chính, và `MigrationRunner` tự động tạo bảng khi server khởi động lần đầu. Sau tuần này, mọi DAO chỉ cần gọi `DbConnectionProvider.getInstance().getConnection()` là có ngay connection để query.

### 📁 Cấu trúc file cần tạo

```
bidhub-server/src/
├── main/
│   ├── java/com/bidhub/server/
│   │   └── config/
│   │       ├── DbConnectionProvider.java   ← Singleton thread-safe quản lý Connection SQLite
│   │       └── MigrationRunner.java        ← Đọc schema.sql, tạo bảng nếu chưa có
│   └── resources/
│       └── db/
│           └── schema.sql                  ← DDL định nghĩa 4 bảng hệ thống
└── test/
    └── java/com/bidhub/server/config/
        └── DbConnectionProviderTest.java   ← Test Singleton + schema migration
```

### 📋 Yêu cầu chi tiết

#### Bước 1 — `schema.sql`

```sql
-- schema.sql
-- Đặt trong: bidhub-server/src/main/resources/db/schema.sql
-- Dùng CREATE TABLE IF NOT EXISTS để an toàn: chạy nhiều lần không bị lỗi.
-- SQLite dùng TEXT để lưu UUID, LocalDateTime (ISO 8601 format), Enum name.

-- Bảng người dùng
CREATE TABLE IF NOT EXISTS users (
    id           TEXT PRIMARY KEY,
    username     TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    email        TEXT NOT NULL,
    role         TEXT NOT NULL,           -- "BIDDER" | "SELLER" | "ADMIN"
    active       INTEGER NOT NULL DEFAULT 1, -- 1 = true, 0 = false (SQLite không có BOOLEAN)
    created_at   TEXT NOT NULL,
    updated_at   TEXT NOT NULL
);

-- Bảng sản phẩm đấu giá
CREATE TABLE IF NOT EXISTS items (
    id             TEXT PRIMARY KEY,
    name           TEXT NOT NULL,
    description    TEXT,
    starting_price REAL NOT NULL,
    item_type      TEXT NOT NULL,         -- "ELECTRONICS" | "ART" | "VEHICLE"
    seller_id      TEXT NOT NULL,
    extra_data     TEXT,                  -- JSON: {"brand":"Dell","warrantyMonths":24}
    created_at     TEXT NOT NULL,
    updated_at     TEXT NOT NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- Bảng phiên đấu giá
CREATE TABLE IF NOT EXISTS auctions (
    id                  TEXT PRIMARY KEY,
    item_id             TEXT NOT NULL,
    start_time          TEXT NOT NULL,
    end_time            TEXT NOT NULL,
    starting_price      REAL NOT NULL,
    current_highest_bid REAL NOT NULL,
    highest_bidder_id   TEXT,             -- NULL nếu chưa có ai đặt giá
    status              TEXT NOT NULL,    -- "OPEN" | "RUNNING" | "FINISHED" | "PAID" | "CANCELED"
    minimum_increment   REAL NOT NULL DEFAULT 1.0,
    created_at          TEXT NOT NULL,
    updated_at          TEXT NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
);

-- Bảng lịch sử đặt giá
CREATE TABLE IF NOT EXISTS bid_transactions (
    id         TEXT PRIMARY KEY,
    auction_id TEXT NOT NULL,
    bidder_id  TEXT NOT NULL,
    bid_amount REAL NOT NULL,
    bid_time   TEXT NOT NULL,
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (bidder_id)  REFERENCES users(id)
);
```

```bash
git add bidhub-server/src/main/resources/db/schema.sql
git commit -m "feat: thêm schema.sql định nghĩa 4 bảng users, items, auctions, bid_transactions"
```

---

#### Bước 2 — `DbConnectionProvider.java`

```java
package com.bidhub.server.config;

import com.bidhub.server.config.ConfigLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý kết nối tới SQLite database theo Singleton Pattern.
 *
 * <p><b>Tại sao Singleton?</b> SQLite cho phép nhiều connection đồng thời đọc,
 * nhưng chỉ 1 writer tại một thời điểm. Dùng 1 Connection duy nhất cho toàn server
 * giúp tránh conflict và đơn giản hoá quản lý transaction. Phù hợp với quy mô
 * của bài tập lớn này.
 *
 * <p><b>Thread-safety:</b> Cơ chế double-checked locking với {@code volatile}
 * đảm bảo chỉ 1 instance được tạo dù có nhiều thread gọi {@link #getInstance()}
 * đồng thời lần đầu.
 *
 * <p><b>WAL mode:</b> Write-Ahead Logging cho phép đọc và ghi đồng thời,
 * giảm lock contention khi nhiều thread query DB cùng lúc.
 *
 * <p>Cách dùng:
 * <pre>{@code
 * Connection conn = DbConnectionProvider.getInstance().getConnection();
 * try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
 *     ps.setString(1, userId);
 *     ResultSet rs = ps.executeQuery();
 *     // ... xử lý kết quả
 * }
 * // Không close connection — để DbConnectionProvider quản lý
 * }</pre>
 *
 * <p><b>Lưu ý quan trọng:</b> Không close Connection sau mỗi query.
 * Connection được tái sử dụng xuyên suốt vòng đời server. Chỉ đóng
 * khi server shutdown bằng {@link #closeConnection()}.
 */
public final class DbConnectionProvider {

  /**
   * Instance duy nhất.
   * {@code volatile} đảm bảo mọi thread thấy giá trị mới nhất (ngăn CPU reordering).
   */
  private static volatile DbConnectionProvider instance;

  /** Connection SQLite được giữ mở suốt vòng đời server. */
  private Connection connection;

  /**
   * Đường dẫn JDBC đọc từ config. Ví dụ: {@code "data/bidhub.db"}.
   * Trong test, sẽ được override bằng {@code "jdbc:sqlite::memory:"}.
   */
  private final String jdbcUrl;

  /**
   * Constructor private — chỉ được gọi 1 lần bên trong {@link #getInstance()}.
   *
   * @param jdbcUrl đường dẫn JDBC đến file SQLite
   */
  private DbConnectionProvider(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  /**
   * Trả về instance duy nhất của {@code DbConnectionProvider}.
   *
   * <p>Dùng double-checked locking để thread-safe mà không lock mọi lần gọi.
   *
   * @return instance duy nhất
   */
  public static DbConnectionProvider getInstance() {
    // Lần check thứ 1: Nếu đã có instance rồi, trả về ngay (không cần synchronized)
    if (instance == null) {
      synchronized (DbConnectionProvider.class) {
        // Lần check thứ 2 (QUAN TRỌNG): Thread khác có thể đã tạo instance
        // trong khoảng thời gian từ lần check 1 đến khi vào được synchronized block
        if (instance == null) {
          String dbPath = ConfigLoader.getString("db.path");
          instance = new DbConnectionProvider("jdbc:sqlite:" + dbPath);
        }
      }
    }
    return instance;
  }

  /**
   * Factory method dùng trong unit test — tạo instance với in-memory database.
   *
   * <p>In-memory database ({@code :memory:}) là database SQLite tồn tại trong RAM,
   * bị xóa hoàn toàn khi connection đóng. Phù hợp cho test vì:
   * <ul>
   *   <li>Không để lại file .db trong repo</li>
   *   <li>Mỗi test bắt đầu với database sạch</li>
   *   <li>Chạy nhanh hơn nhiều so với file trên disk</li>
   * </ul>
   *
   * <p><b>CHÚ Ý:</b> Method này chỉ dùng trong test. KHÔNG gọi trong production code.
   * Sau khi gọi, phải gọi {@link #resetForTesting()} trước mỗi test method để
   * đảm bảo state sạch.
   *
   * @return instance mới với in-memory SQLite (KHÔNG phải getInstance() singleton)
   */
  public static DbConnectionProvider createInMemoryForTesting() {
    return new DbConnectionProvider("jdbc:sqlite::memory:");
  }

  /**
   * Reset singleton instance — dùng trong test để tạo instance mới giữa các test.
   *
   * <p><b>CHÚ Ý:</b> Chỉ gọi trong {@code @BeforeEach} / {@code @AfterEach} của test.
   */
  public static void resetForTesting() {
    synchronized (DbConnectionProvider.class) {
      if (instance != null) {
        instance.closeConnection();
        instance = null;
      }
    }
  }

  /**
   * Trả về Connection tới SQLite, tạo mới nếu chưa có hoặc đã đóng.
   *
   * <p>Bật WAL mode sau khi tạo connection mới để hỗ trợ concurrent read.
   *
   * @return {@link Connection} đến SQLite, không bao giờ null
   * @throws RuntimeException nếu không kết nối được tới database
   */
  public synchronized Connection getConnection() {
    try {
      if (connection == null || connection.isClosed()) {
        connection = DriverManager.getConnection(jdbcUrl);
        // WAL mode: Write-Ahead Logging — giảm lock contention
        try (var stmt = connection.createStatement()) {
          stmt.execute("PRAGMA journal_mode=WAL;");
          stmt.execute("PRAGMA foreign_keys=ON;");  // Bật kiểm tra foreign key
        }
      }
      return connection;
    } catch (SQLException e) {
      throw new RuntimeException(
          "Không thể kết nối đến database: " + jdbcUrl, e);
    }
  }

  /**
   * Đóng connection — gọi khi server shutdown.
   *
   * <p>Sau khi gọi method này, {@link #getConnection()} sẽ mở connection mới.
   */
  public synchronized void closeConnection() {
    if (connection != null) {
      try {
        if (!connection.isClosed()) {
          connection.close();
        }
      } catch (SQLException e) {
        System.err.println("[WARN] Lỗi đóng DB connection: " + e.getMessage());
      } finally {
        connection = null;
      }
    }
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/config/DbConnectionProvider.java
git commit -m "feat: thêm DbConnectionProvider singleton thread-safe với double-checked locking và WAL mode"
```

---

#### Bước 3 — `MigrationRunner.java`

```java
package com.bidhub.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Chạy migration SQL khi server khởi động.
 *
 * <p>Đọc file {@code schema.sql} từ classpath và thực thi từng câu lệnh SQL.
 * Vì schema dùng {@code CREATE TABLE IF NOT EXISTS}, migration có thể chạy
 * nhiều lần mà không bị lỗi — bảng đã tồn tại sẽ được bỏ qua.
 *
 * <p>Cách dùng trong {@code ServerApp.main()}:
 * <pre>{@code
 * DbConnectionProvider dbProvider = DbConnectionProvider.getInstance();
 * MigrationRunner.run(dbProvider.getConnection());
 * }</pre>
 */
public final class MigrationRunner {

  /** Đường dẫn file SQL trong classpath (src/main/resources/db/schema.sql). */
  private static final String SCHEMA_FILE = "/db/schema.sql";

  /** Ngăn khởi tạo — chỉ có static method. */
  private MigrationRunner() {}

  /**
   * Đọc và thực thi toàn bộ câu lệnh trong {@code schema.sql}.
   *
   * <p>Logic: đọc file thành String → tách theo {@code ;} →
   * bỏ qua câu lệnh trống → {@code stmt.executeUpdate()} từng câu.
   *
   * @param connection connection đến SQLite (không được null, không được closed)
   * @throws RuntimeException nếu không đọc được file hoặc SQL sai cú pháp
   */
  public static void run(Connection connection) {
    String sql = readSchemaFile();
    executeSql(connection, sql);
    System.out.println("[MigrationRunner] Schema migration hoàn tất.");
  }

  /**
   * Đọc nội dung file schema.sql từ classpath.
   *
   * @return nội dung file dưới dạng String
   * @throws RuntimeException nếu file không tồn tại trong classpath
   */
  private static String readSchemaFile() {
    try (InputStream in = MigrationRunner.class.getResourceAsStream(SCHEMA_FILE)) {
      if (in == null) {
        throw new IllegalStateException(
            "Không tìm thấy file schema: " + SCHEMA_FILE
            + ". Kiểm tra bidhub-server/src/main/resources/db/schema.sql");
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Lỗi đọc file schema: " + SCHEMA_FILE, e);
    }
  }

  /**
   * Tách SQL theo {@code ;} và thực thi từng câu lệnh.
   *
   * @param connection connection SQLite
   * @param sql        nội dung SQL đầy đủ
   */
  private static void executeSql(Connection connection, String sql) {
    // Tách theo dấu ; — mỗi câu lệnh là 1 statement
    String[] statements = sql.split(";");
    try (Statement stmt = connection.createStatement()) {
      for (String statement : statements) {
        String trimmed = statement.trim();
        // Bỏ qua câu rỗng và comment thuần túy
        if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
          stmt.executeUpdate(trimmed);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi thực thi schema SQL: " + e.getMessage(), e);
    }
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/config/MigrationRunner.java
git commit -m "feat: thêm MigrationRunner đọc schema.sql và tạo bảng tự động khi server khởi động"
```

---

#### Bước 4 — `DbConnectionProviderTest.java`

```java
package com.bidhub.server.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho {@link DbConnectionProvider} và {@link MigrationRunner}.
 *
 * <p>Dùng {@code jdbc:sqlite::memory:} để:
 * <ul>
 *   <li>Không cần file .db trên disk</li>
 *   <li>Mỗi test method bắt đầu với database sạch hoàn toàn</li>
 *   <li>Chạy nhanh (không I/O disk)</li>
 * </ul>
 *
 * <p>Pattern: mỗi test dùng {@code createInMemoryForTesting()} thay vì
 * {@code getInstance()} để tránh làm ô nhiễm Singleton toàn cục.
 */
@DisplayName("DbConnectionProvider — Singleton + MigrationRunner")
class DbConnectionProviderTest {

  private DbConnectionProvider provider;

  @BeforeEach
  void setUp() {
    // Tạo in-memory provider mới cho mỗi test — database sạch hoàn toàn
    provider = DbConnectionProvider.createInMemoryForTesting();
  }

  @AfterEach
  void tearDown() {
    // Đóng in-memory connection sau mỗi test
    provider.closeConnection();
  }

  // ===================== DbConnectionProvider Tests =====================

  @Test
  @DisplayName("getConnection() trả về Connection không null và chưa đóng")
  void testGetConnection_ReturnsOpenConnection() throws Exception {
    // Act
    Connection conn = provider.getConnection();

    // Assert
    assertNotNull(conn, "Connection không được null");
    assertFalse(conn.isClosed(), "Connection phải đang mở");
  }

  @Test
  @DisplayName("getConnection() gọi 2 lần trả về cùng 1 Connection object")
  void testGetConnection_CalledTwice_ReturnsSameInstance() throws Exception {
    // Act
    Connection conn1 = provider.getConnection();
    Connection conn2 = provider.getConnection();

    // Assert — cùng 1 object (reference equality)
    assertSame(conn1, conn2, "Phải trả về cùng 1 Connection object (Singleton Connection)");
  }

  @Test
  @DisplayName("getInstance() Singleton — gọi nhiều lần trả về cùng 1 instance")
  void testGetInstance_Singleton_ReturnsSameInstance() {
    // Note: dùng resetForTesting để test Singleton thật sự
    DbConnectionProvider.resetForTesting();

    // Act
    DbConnectionProvider inst1 = DbConnectionProvider.getInstance();
    DbConnectionProvider inst2 = DbConnectionProvider.getInstance();

    // Assert
    assertSame(inst1, inst2, "getInstance() phải luôn trả về cùng 1 instance");

    // Cleanup
    DbConnectionProvider.resetForTesting();
  }

  @Test
  @DisplayName("closeConnection() sau đó getConnection() tạo connection mới thành công")
  void testCloseAndReopen_ConnectionRecreated() throws Exception {
    // Arrange
    Connection conn1 = provider.getConnection();
    assertFalse(conn1.isClosed());

    // Act
    provider.closeConnection();
    assertTrue(conn1.isClosed(), "Connection cũ phải bị đóng");

    Connection conn2 = provider.getConnection();

    // Assert
    assertNotNull(conn2);
    assertFalse(conn2.isClosed(), "Connection mới phải đang mở");
  }

  // ===================== MigrationRunner Tests =====================

  @Test
  @DisplayName("MigrationRunner.run() tạo đủ 4 bảng trong database")
  void testMigrationRunner_CreatesFourTables() throws Exception {
    // Arrange
    Connection conn = provider.getConnection();

    // Act
    MigrationRunner.run(conn);

    // Assert — kiểm tra 4 bảng tồn tại
    String[] expectedTables = {"users", "items", "auctions", "bid_transactions"};
    for (String tableName : expectedTables) {
      assertTrue(
          tableExists(conn, tableName),
          "Bảng '" + tableName + "' phải tồn tại sau migration"
      );
    }
  }

  @Test
  @DisplayName("MigrationRunner.run() gọi 2 lần không lỗi (IF NOT EXISTS)")
  void testMigrationRunner_RunTwice_NoError() throws Exception {
    // Arrange
    Connection conn = provider.getConnection();

    // Act + Assert — không throw exception
    assertDoesNotThrow(() -> {
      MigrationRunner.run(conn);
      MigrationRunner.run(conn); // Lần 2: IF NOT EXISTS nên bỏ qua
    }, "Chạy migration 2 lần không được throw exception");
  }

  @Test
  @DisplayName("Sau migration, bảng users có đúng cột id, username, password_hash, email, role")
  void testMigrationRunner_UsersTableHasCorrectColumns() throws Exception {
    // Arrange
    Connection conn = provider.getConnection();
    MigrationRunner.run(conn);

    // Act — dùng PRAGMA để lấy thông tin cột
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)")) {

      boolean hasId = false, hasUsername = false, hasPasswordHash = false;
      while (rs.next()) {
        String colName = rs.getString("name");
        if ("id".equals(colName)) hasId = true;
        if ("username".equals(colName)) hasUsername = true;
        if ("password_hash".equals(colName)) hasPasswordHash = true;
      }

      // Assert
      assertTrue(hasId, "Bảng users phải có cột 'id'");
      assertTrue(hasUsername, "Bảng users phải có cột 'username'");
      assertTrue(hasPasswordHash, "Bảng users phải có cột 'password_hash'");
    }
  }

  @Test
  @DisplayName("WAL mode được bật sau khi getConnection()")
  void testGetConnection_WalModeEnabled() throws Exception {
    // Arrange
    Connection conn = provider.getConnection();

    // Act
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
      rs.next();
      String mode = rs.getString(1);

      // Assert
      assertEquals("wal", mode, "WAL mode phải được bật");
    }
  }

  // ===================== Helper method =====================

  /**
   * Kiểm tra bảng có tồn tại trong SQLite database không.
   *
   * @param conn      connection đang mở
   * @param tableName tên bảng cần kiểm tra
   * @return {@code true} nếu bảng tồn tại
   */
  private boolean tableExists(Connection conn, String tableName) throws Exception {
    String sql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?";
    try (var ps = conn.prepareStatement(sql)) {
      ps.setString(1, tableName);
      ResultSet rs = ps.executeQuery();
      return rs.next() && rs.getInt(1) > 0;
    }
  }
}
```

```bash
git add bidhub-server/src/test/java/com/bidhub/server/config/DbConnectionProviderTest.java
git commit -m "test: thêm DbConnectionProviderTest kiểm tra singleton, migration, wal mode với in-memory SQLite"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Đăng

```bash
# Test 1: Compile module server
mvn compile -pl bidhub-server
# ✅ PASS: BUILD SUCCESS
# ❌ FAIL: Lỗi import ConfigLoader → kiểm tra dependency bidhub-common trong pom.xml

# Test 2: Chạy test DbConnectionProvider
mvn test -pl bidhub-server -Dtest="DbConnectionProviderTest"
# ✅ PASS: "Tests run: 5, Failures: 0, Errors: 0"
# ❌ FAIL: Tests run < 5 hoặc có lỗi

# Test 3: Kiểm tra singleton với 2 lần gọi
mvn test -pl bidhub-server -Dtest="DbConnectionProviderTest#testGetInstance_Singleton_ReturnsSameInstance"
# ✅ PASS: assertSame không fail

# Test 4: Kiểm tra migration tạo đủ 4 bảng
mvn test -pl bidhub-server -Dtest="DbConnectionProviderTest#testMigrationRunner_CreatesFourTables"
# ✅ PASS: 4 bảng tồn tại

# Test 5: Kiểm tra file schema.sql tồn tại trong resources
find bidhub-server/src/main/resources -name "schema.sql"
# ✅ PASS: bidhub-server/src/main/resources/db/schema.sql

# Test 6: Kiểm tra schema có CREATE TABLE IF NOT EXISTS
grep -c "CREATE TABLE IF NOT EXISTS" bidhub-server/src/main/resources/db/schema.sql
# ✅ PASS: 4 (mỗi bảng 1 lần)

# Test 7: Kiểm tra volatile keyword trong DbConnectionProvider
grep "volatile" bidhub-server/src/main/java/com/bidhub/server/config/DbConnectionProvider.java
# ✅ PASS: Có dòng "private static volatile DbConnectionProvider instance"

# Test 8: Chạy toàn bộ test
mvn test
# ✅ PASS: Tổng ≥ 65 tests, 0 failures
```

### ❌ FAIL nếu:
- `DbConnectionProvider` không có `volatile` → không thread-safe
- `DbConnectionProvider` constructor là `public` → có thể `new DbConnectionProvider()` → vi phạm Singleton
- `MigrationRunner` chạy schema và lỗi vì SQL cú pháp sai
- `schema.sql` không có `IF NOT EXISTS` → lỗi khi chạy lần 2
- `getConnection()` không `synchronized` → race condition khi connection bị đóng

---

## 👤 QUỐC MINH — UserDao & ItemDao

```
Branch: feature/tuan-3-quocminh-user-item-dao
Phụ thuộc: Đợi Đăng push DbConnectionProvider + schema.sql (hoặc dùng createInMemoryForTesting())
Deadline: Thứ 7 25/04 23:59
```

📌 **[Tiêu chí điểm: Quản lý người dùng & sản phẩm (1.0đ) — phần CRUD data access] + [Xử lý lỗi — PreparedStatement chống SQL Injection (1.0đ, gián tiếp)]**

### 📝 Mô tả bài tập

Bạn implement tầng truy cập dữ liệu cho `User` và `Item`. Mỗi DAO là 1 class Java thẳng (không kế thừa abstract gì), chứa các method JDBC với `PreparedStatement`. Đây là **lớp duy nhất được phép query database** cho User và Item — controller và service sẽ gọi DAO, không tự query.

> [!TIP]
> **DAO đơn giản là đủ điểm.** Không cần `AbstractDao<T>`, không cần generic. Class thẳng với `PreparedStatement` là đúng triết lý của project này — dễ đọc, dễ giải thích khi vấn đáp.

### 📁 Cấu trúc file cần tạo

```
bidhub-server/src/
├── main/java/com/bidhub/server/dao/
│   ├── UserDao.java                ← CRUD cho bảng users
│   └── ItemDao.java                ← CRUD cho bảng items (extra_data dùng Jackson)
└── test/java/com/bidhub/server/dao/
    ├── UserDaoTest.java            ← Test với in-memory SQLite
    └── ItemDaoTest.java            ← Test với in-memory SQLite
```

### 📋 Yêu cầu chi tiết

#### `UserDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.common.exception.BidHubException;
import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.Admin;
import com.bidhub.server.model.Bidder;
import com.bidhub.server.model.Seller;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho {@link User} — truy xuất bảng {@code users}.
 *
 * <p><b>Nguyên tắc quan trọng:</b>
 * <ul>
 *   <li>Tất cả SQL dùng {@link PreparedStatement} — KHÔNG nối chuỗi SQL trực tiếp.</li>
 *   <li>Connection lấy từ {@link DbConnectionProvider} — không tự tạo.</li>
 *   <li>PreparedStatement và ResultSet được đóng trong {@code try-with-resources}.</li>
 *   <li>Connection KHÔNG bị đóng ở đây — do {@link DbConnectionProvider} quản lý.</li>
 * </ul>
 *
 * <p>Cách dùng:
 * <pre>{@code
 * UserDao dao = new UserDao(DbConnectionProvider.getInstance());
 * Optional<User> user = dao.findByUsername("alice");
 * user.ifPresent(u -> System.out.println(u.getInfo()));
 * }</pre>
 */
public class UserDao {

  private final DbConnectionProvider dbProvider;

  /**
   * Constructor nhận {@link DbConnectionProvider} qua dependency injection.
   *
   * <p>Trong production: truyền {@code DbConnectionProvider.getInstance()}.
   * Trong test: truyền {@code DbConnectionProvider.createInMemoryForTesting()}.
   *
   * @param dbProvider provider quản lý Connection, không được null
   */
  public UserDao(DbConnectionProvider dbProvider) {
    if (dbProvider == null) {
      throw new IllegalArgumentException("dbProvider không được null");
    }
    this.dbProvider = dbProvider;
  }

  /**
   * Lưu user mới vào database.
   *
   * <p>Dùng {@code INSERT OR IGNORE} để không bị lỗi nếu gọi trùng lần 2.
   *
   * @param user user cần lưu, không được null
   * @throws BidHubException nếu lỗi database
   */
  public void save(User user) {
    if (user == null) {
      throw new IllegalArgumentException("user không được null");
    }
    String sql = """
        INSERT OR IGNORE INTO users
            (id, username, password_hash, email, role, active, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.getId());
      ps.setString(2, user.getUsername());
      ps.setString(3, user.getPasswordHash());
      ps.setString(4, user.getEmail());
      ps.setString(5, user.getRole().name());         // Enum → String
      ps.setInt(6, user.isActive() ? 1 : 0);           // boolean → int
      ps.setString(7, user.getCreatedAt().toString()); // LocalDateTime → ISO String
      ps.setString(8, user.getUpdatedAt().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi lưu user id=" + user.getId(), e);
    }
  }

  /**
   * Tìm user theo ID.
   *
   * @param id UUID của user cần tìm
   * @return {@link Optional} chứa user nếu tìm thấy, {@link Optional#empty()} nếu không
   */
  public Optional<User> findById(String id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi tìm user id=" + id, e);
    }
    return Optional.empty();
  }

  /**
   * Tìm user theo username (tên đăng nhập).
   *
   * <p>Dùng khi xử lý login: tìm user → verify password → tạo session.
   *
   * @param username tên đăng nhập cần tìm
   * @return {@link Optional} chứa user hoặc {@link Optional#empty()} nếu không tìm thấy
   */
  public Optional<User> findByUsername(String username) {
    String sql = "SELECT * FROM users WHERE username = ?";
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi tìm user username=" + username, e);
    }
    return Optional.empty();
  }

  /**
   * Kiểm tra username đã tồn tại trong database chưa.
   *
   * <p>Dùng khi đăng ký: kiểm tra trùng username trước khi INSERT.
   *
   * @param username tên đăng nhập cần kiểm tra
   * @return {@code true} nếu username đã tồn tại
   */
  public boolean existsByUsername(String username) {
    String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi kiểm tra username=" + username, e);
    }
  }

  /**
   * Lấy danh sách tất cả users trong database.
   *
   * <p>Dùng cho Admin dashboard.
   *
   * @return danh sách User, có thể rỗng nếu không có ai
   */
  public List<User> findAll() {
    String sql = "SELECT * FROM users ORDER BY created_at";
    List<User> result = new ArrayList<>();
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        result.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi lấy danh sách users", e);
    }
    return result;
  }

  /**
   * Map một hàng từ {@link ResultSet} sang đối tượng {@link User}.
   *
   * <p>Đọc cột {@code role} để khởi tạo đúng subclass ({@link Bidder}/{@link Seller}/{@link Admin}).
   * Đây là ví dụ của Polymorphism: caller nhận {@code User} nhưng bên trong
   * là đúng subclass tương ứng.
   *
   * @param rs ResultSet đang trỏ vào hàng hiện tại
   * @return đối tượng User đúng subclass
   * @throws SQLException nếu lỗi đọc cột
   */
  private User mapRow(ResultSet rs) throws SQLException {
    String id = rs.getString("id");
    String username = rs.getString("username");
    String passwordHash = rs.getString("password_hash");
    String email = rs.getString("email");
    UserRole role = UserRole.valueOf(rs.getString("role")); // String → Enum
    boolean active = rs.getInt("active") == 1;
    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
    LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));

    // Tạo đúng subclass dựa vào role — đây là Polymorphism trong DAO
    return switch (role) {
      case BIDDER -> new Bidder(id, username, passwordHash, email, active, createdAt, updatedAt);
      case SELLER -> new Seller(id, username, passwordHash, email, active, createdAt, updatedAt);
      case ADMIN  -> new Admin(id, username, passwordHash, email, active, createdAt, updatedAt);
    };
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/dao/UserDao.java
git commit -m "feat: thêm UserDao với save, findById, findByUsername, existsByUsername dùng PreparedStatement"
```

---

#### `ItemDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.Art;
import com.bidhub.server.model.Electronics;
import com.bidhub.server.model.Item;
import com.bidhub.server.model.ItemType;
import com.bidhub.server.model.Vehicle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object cho {@link Item} — truy xuất bảng {@code items}.
 *
 * <p>Cột {@code extra_data} lưu JSON chứa thông tin đặc thù của từng loại item:
 * <ul>
 *   <li>{@link Electronics}: {@code {"brand":"Dell","warrantyMonths":24}}</li>
 *   <li>{@link Art}: {@code {"artist":"Picasso","yearCreated":1937}}</li>
 *   <li>{@link Vehicle}: {@code {"manufacturer":"Toyota","year":2022,"mileageKm":15000.0}}</li>
 * </ul>
 *
 * <p>Dùng Jackson {@link ObjectMapper} (thread-safe, static) để serialize/deserialize
 * {@code extra_data} thay vì tự parse JSON thủ công.
 */
public class ItemDao {

  /**
   * ObjectMapper dùng chung — an toàn với nhiều thread vì là stateless sau khi configure.
   * Static để không tạo lại mỗi khi ItemDao được khởi tạo.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final DbConnectionProvider dbProvider;

  /**
   * Constructor nhận DbConnectionProvider qua DI.
   *
   * @param dbProvider provider quản lý Connection
   */
  public ItemDao(DbConnectionProvider dbProvider) {
    if (dbProvider == null) {
      throw new IllegalArgumentException("dbProvider không được null");
    }
    this.dbProvider = dbProvider;
  }

  /**
   * Lưu item mới vào database. Cột {@code extra_data} được serialize từ Map sang JSON.
   *
   * @param item item cần lưu, không được null
   */
  public void save(Item item) {
    if (item == null) {
      throw new IllegalArgumentException("item không được null");
    }
    String sql = """
        INSERT OR IGNORE INTO items
            (id, name, description, starting_price, item_type, seller_id,
             extra_data, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, item.getId());
      ps.setString(2, item.getName());
      ps.setString(3, item.getDescription());
      ps.setDouble(4, item.getStartingPrice());
      ps.setString(5, item.getItemType().name());
      ps.setString(6, item.getSellerId());
      ps.setString(7, serializeExtraData(item));  // Map → JSON String
      ps.setString(8, item.getCreatedAt().toString());
      ps.setString(9, item.getUpdatedAt().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi lưu item id=" + item.getId(), e);
    }
  }

  /**
   * Tìm item theo ID.
   *
   * @param id UUID của item
   * @return {@link Optional} chứa item nếu tìm thấy
   */
  public Optional<Item> findById(String id) {
    String sql = "SELECT * FROM items WHERE id = ?";
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi tìm item id=" + id, e);
    }
    return Optional.empty();
  }

  /**
   * Lấy danh sách item của một seller.
   *
   * <p>Dùng khi Seller vào trang "Sản phẩm của tôi".
   *
   * @param sellerId UUID của seller
   * @return danh sách item, có thể rỗng
   */
  public List<Item> findBySellerId(String sellerId) {
    String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY created_at DESC";
    List<Item> result = new ArrayList<>();
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi tìm items của seller=" + sellerId, e);
    }
    return result;
  }

  /**
   * Xóa item theo ID.
   *
   * @param id UUID của item cần xóa
   */
  public void deleteById(String id) {
    String sql = "DELETE FROM items WHERE id = ?";
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi xóa item id=" + id, e);
    }
  }

  /**
   * Serialize trường đặc thù của từng loại Item thành JSON string.
   *
   * @param item item cần serialize extra data
   * @return JSON string hoặc "{}" nếu không có extra data
   */
  private String serializeExtraData(Item item) {
    try {
      Map<String, Object> extras = new HashMap<>();
      if (item instanceof Electronics e) {
        extras.put("brand", e.getBrand());
        extras.put("warrantyMonths", e.getWarrantyMonths());
      } else if (item instanceof Art a) {
        extras.put("artist", a.getArtist());
        extras.put("yearCreated", a.getYearCreated());
      } else if (item instanceof Vehicle v) {
        extras.put("manufacturer", v.getManufacturer());
        extras.put("year", v.getYear());
        extras.put("mileageKm", v.getMileageKm());
      }
      return MAPPER.writeValueAsString(extras);
    } catch (Exception e) {
      return "{}"; // Fallback an toàn nếu serialize lỗi
    }
  }

  /**
   * Map một hàng ResultSet sang đối tượng {@link Item} đúng subclass.
   *
   * @param rs ResultSet đang trỏ vào hàng hiện tại
   * @return Item đúng subclass
   */
  private Item mapRow(ResultSet rs) throws SQLException {
    String id = rs.getString("id");
    String name = rs.getString("name");
    String description = rs.getString("description");
    double startingPrice = rs.getDouble("starting_price");
    ItemType type = ItemType.valueOf(rs.getString("item_type"));
    String sellerId = rs.getString("seller_id");
    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
    LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));

    Map<String, Object> extras = parseExtraData(rs.getString("extra_data"));

    return switch (type) {
      case ELECTRONICS -> new Electronics(
          id, name, description, startingPrice, sellerId,
          (String) extras.getOrDefault("brand", ""),
          ((Number) extras.getOrDefault("warrantyMonths", 0)).intValue(),
          createdAt, updatedAt);
      case ART -> new Art(
          id, name, description, startingPrice, sellerId,
          (String) extras.getOrDefault("artist", ""),
          ((Number) extras.getOrDefault("yearCreated", 0)).intValue(),
          createdAt, updatedAt);
      case VEHICLE -> new Vehicle(
          id, name, description, startingPrice, sellerId,
          (String) extras.getOrDefault("manufacturer", ""),
          ((Number) extras.getOrDefault("year", 0)).intValue(),
          ((Number) extras.getOrDefault("mileageKm", 0.0)).doubleValue(),
          createdAt, updatedAt);
    };
  }

  /**
   * Parse JSON string từ cột extra_data thành Map.
   *
   * @param json chuỗi JSON (có thể null hoặc rỗng)
   * @return Map kết quả, rỗng nếu parse lỗi
   */
  private Map<String, Object> parseExtraData(String json) {
    if (json == null || json.isBlank()) {
      return new HashMap<>();
    }
    try {
      return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      return new HashMap<>();
    }
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/dao/ItemDao.java
git commit -m "feat: thêm ItemDao với save, findById, findBySellerId, deleteById và serialize extra_data sang JSON"
```

---

#### `UserDaoTest.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.config.MigrationRunner;
import com.bidhub.server.model.Admin;
import com.bidhub.server.model.Bidder;
import com.bidhub.server.model.Seller;
import com.bidhub.server.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho {@link UserDao} — dùng in-memory SQLite.
 *
 * <p>Mỗi test method có database sạch hoàn toàn nhờ
 * {@code createInMemoryForTesting()} trong {@code @BeforeEach}.
 */
@DisplayName("UserDao — CRUD với in-memory SQLite")
class UserDaoTest {

  private DbConnectionProvider dbProvider;
  private UserDao userDao;

  @BeforeEach
  void setUp() {
    // Database mới hoàn toàn cho mỗi test
    dbProvider = DbConnectionProvider.createInMemoryForTesting();
    MigrationRunner.run(dbProvider.getConnection()); // Tạo bảng
    userDao = new UserDao(dbProvider);
  }

  @AfterEach
  void tearDown() {
    dbProvider.closeConnection();
  }

  @Test
  @DisplayName("save() Bidder rồi findById() trả về đúng Bidder instance")
  void testSave_Bidder_FindById_ReturnsBidder() {
    // Arrange
    Bidder bidder = new Bidder("alice", "hash_alice", "alice@mail.com");

    // Act
    userDao.save(bidder);
    Optional<User> found = userDao.findById(bidder.getId());

    // Assert
    assertTrue(found.isPresent(), "Phải tìm thấy sau khi save");
    assertInstanceOf(Bidder.class, found.get(), "Phải là instance của Bidder");
    assertEquals("alice", found.get().getUsername());
  }

  @Test
  @DisplayName("save() Seller rồi findById() trả về đúng Seller instance")
  void testSave_Seller_FindById_ReturnsSeller() {
    // Arrange
    Seller seller = new Seller("bob", "hash_bob", "bob@mail.com");

    // Act
    userDao.save(seller);
    Optional<User> found = userDao.findById(seller.getId());

    // Assert
    assertTrue(found.isPresent());
    assertInstanceOf(Seller.class, found.get(), "Phải là instance của Seller");
  }

  @Test
  @DisplayName("findByUsername() tìm đúng user theo username")
  void testFindByUsername_ExistingUser_ReturnsUser() {
    // Arrange
    Bidder bidder = new Bidder("carol", "hash_carol", "carol@mail.com");
    userDao.save(bidder);

    // Act
    Optional<User> found = userDao.findByUsername("carol");

    // Assert
    assertTrue(found.isPresent());
    assertEquals("carol", found.get().getUsername());
    assertEquals(bidder.getId(), found.get().getId());
  }

  @Test
  @DisplayName("findByUsername() với username không tồn tại → Optional.empty()")
  void testFindByUsername_NotExist_ReturnsEmpty() {
    // Act
    Optional<User> found = userDao.findByUsername("ghost_user_khong_ton_tai");

    // Assert
    assertTrue(found.isEmpty(), "Phải trả về Optional.empty() khi không tìm thấy");
  }

  @Test
  @DisplayName("existsByUsername() trả về true sau khi save user")
  void testExistsByUsername_AfterSave_ReturnsTrue() {
    // Arrange
    Bidder bidder = new Bidder("dave", "hash", "dave@mail.com");
    userDao.save(bidder);

    // Act + Assert
    assertTrue(userDao.existsByUsername("dave"));
    assertFalse(userDao.existsByUsername("notexist"));
  }

  @Test
  @DisplayName("findAll() sau khi save 3 users trả về list size = 3")
  void testFindAll_ThreeUsers_ReturnsSizeThree() {
    // Arrange
    userDao.save(new Bidder("user1", "h1", "u1@x.com"));
    userDao.save(new Seller("user2", "h2", "u2@x.com"));
    userDao.save(new Admin("user3", "h3", "u3@x.com"));

    // Act
    List<User> all = userDao.findAll();

    // Assert
    assertEquals(3, all.size(), "Phải có đúng 3 users");
  }

  @Test
  @DisplayName("save() Admin rồi findById() trả về đúng Admin instance")
  void testSave_Admin_FindById_ReturnsAdmin() {
    // Arrange
    Admin admin = new Admin("sysadmin", "hash_admin", "admin@mail.com");

    // Act
    userDao.save(admin);
    Optional<User> found = userDao.findById(admin.getId());

    // Assert
    assertTrue(found.isPresent());
    assertInstanceOf(Admin.class, found.get());
  }

  @Test
  @DisplayName("save() trùng id không throw — INSERT OR IGNORE")
  void testSave_DuplicateId_NoException() {
    // Arrange
    Bidder bidder = new Bidder("dupUser", "hash", "dup@mail.com");
    userDao.save(bidder);

    // Act + Assert — lần 2 không throw
    assertDoesNotThrow(() -> userDao.save(bidder));
    assertEquals(1, userDao.findAll().size(), "Vẫn chỉ có 1 user sau khi save trùng");
  }

  @Test
  @DisplayName("findById() với id không tồn tại trả về Optional.empty()")
  void testFindById_NotExist_ReturnsEmpty() {
    // Act
    Optional<User> found = userDao.findById("id-khong-ton-tai-abc-123");

    // Assert
    assertTrue(found.isEmpty());
  }
}
```

```bash
git add bidhub-server/src/test/java/com/bidhub/server/dao/UserDaoTest.java
git commit -m "test: thêm UserDaoTest 8 cases kiểm tra save, findById, findByUsername với in-memory SQLite"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Quốc Minh

```bash
# Test 1: Compile
mvn compile -pl bidhub-server
# ✅ PASS: BUILD SUCCESS

# Test 2: Chạy UserDaoTest
mvn test -pl bidhub-server -Dtest="UserDaoTest"
# ✅ PASS: "Tests run: 8, Failures: 0, Errors: 0"

# Test 3: Test findByUsername trả về đúng role
mvn test -pl bidhub-server -Dtest="UserDaoTest#testSave_Seller_FindById_ReturnsSeller"
# ✅ PASS

# Test 4: Chạy ItemDaoTest (nếu đã implement)
mvn test -pl bidhub-server -Dtest="ItemDaoTest"
# ✅ PASS: "Tests run: ≥ 5, Failures: 0"

# Test 5: Kiểm tra KHÔNG có SQL nối chuỗi trực tiếp
grep -rn '\".*\" + ' bidhub-server/src/main/java/com/bidhub/server/dao/
# ✅ PASS: Không có dòng nào nối chuỗi SQL với biến
# ❌ FAIL: Thấy dòng kiểu: "SELECT * FROM users WHERE username = '" + username + "'"

# Test 6: Kiểm tra PreparedStatement được dùng
grep -c "PreparedStatement" bidhub-server/src/main/java/com/bidhub/server/dao/UserDao.java
# ✅ PASS: ≥ 5

# Test 7: Chạy tổng test
mvn test
# ✅ PASS: ≥ 65 tests, 0 failures
```

### ❌ FAIL nếu:
- Bất kỳ SQL nào nối chuỗi trực tiếp với biến → SQL Injection vulnerability
- `findByUsername()` không trả về đúng subclass (`Bidder` vs `Seller` vs `Admin`)
- `findByUsername()` không tồn tại → trả về `null` thay vì `Optional.empty()`
- `UserDao` tự mở/đóng Connection thay vì dùng `DbConnectionProvider`
- Test không dùng `jdbc:sqlite::memory:` → để lại file `.db` trong repo

---

## 👤 CÔNG MINH — ViewRouter & FXML Skeleton (3 màn hình mới)

```
Branch: feature/tuan-3-congminh-viewrouter-fxml
Phụ thuộc: Kế thừa Views.java (tuần 1) và LoginView.fxml (tuần 1) — không tạo lại
Deadline: Thứ 7 25/04 23:59
```

📌 **[Tiêu chí điểm: Áp dụng MVC (JavaFX + FXML) — 0.5đ] + [Design Patterns — Singleton ViewRouter — 1.0đ, phần client]**

### 📝 Mô tả bài tập

Bạn xây dựng hệ thống **điều hướng màn hình** cho toàn bộ client JavaFX: `ViewRouter` Singleton thay thế việc gọi `FXMLLoader` rải rác khắp nơi — mọi controller chỉ cần `ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST)` là chuyển màn hình. Tuần này cũng tạo 3 FXML skeleton mới cho các màn hình sẽ implement ở tuần 5–6.

### 📁 Cấu trúc file cần tạo

```
bidhub-client/src/main/
├── java/com/bidhub/client/
│   ├── navigation/
│   │   ├── ViewRouter.java              ← Singleton điều hướng màn hình
│   │   └── ContextAware.java            ← Interface nhận params khi navigate
│   └── controller/
│       ├── AuctionListController.java   ← Controller skeleton cho AuctionListView
│       ├── AuctionDetailController.java ← Controller skeleton cho AuctionDetailView
│       └── CreateItemController.java    ← Controller skeleton cho CreateItemView
└── resources/
    └── fxml/
        ├── AuctionListView.fxml         ← Danh sách phiên đấu giá (TableView)
        ├── AuctionDetailView.fxml       ← Chi tiết + đặt giá
        └── CreateItemView.fxml          ← Form tạo sản phẩm (cho Seller)
```

### 📋 Yêu cầu chi tiết

#### `ContextAware.java`

```java
package com.bidhub.client.navigation;

import java.util.Map;

/**
 * Interface cho Controller nhận tham số khi được điều hướng tới.
 *
 * <p>Khi {@link ViewRouter#navigateTo(String, Map)} được gọi với params,
 * ViewRouter sẽ kiểm tra Controller có implements interface này không.
 * Nếu có → gọi {@link #setContext(Map)} để truyền params.
 *
 * <p>Ví dụ: Chuyển từ AuctionList sang AuctionDetail với auctionId:
 * <pre>{@code
 * // Trong AuctionListController:
 * Map<String, Object> params = Map.of("auctionId", selectedAuction.getId());
 * ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL, params);
 *
 * // Trong AuctionDetailController (implements ContextAware):
 * @Override
 * public void setContext(Map<String, Object> params) {
 *     String auctionId = (String) params.get("auctionId");
 *     loadAuctionDetail(auctionId);
 * }
 * }</pre>
 */
public interface ContextAware {

  /**
   * Nhận tham số điều hướng từ màn hình trước.
   *
   * <p>Được gọi bởi {@link ViewRouter} SAU KHI FXML được load và controller
   * được inject. Luôn implement method này nếu màn hình cần nhận data từ màn hình khác.
   *
   * @param params map tham số, không được null (có thể rỗng)
   */
  void setContext(Map<String, Object> params);
}
```

```bash
git add bidhub-client/src/main/java/com/bidhub/client/navigation/ContextAware.java
git commit -m "feat: thêm interface ContextAware để controller nhận params khi navigate"
```

---

#### `ViewRouter.java`

```java
package com.bidhub.client.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Singleton quản lý điều hướng màn hình trong JavaFX client.
 *
 * <p><b>Tại sao Singleton?</b> Stage chính là duy nhất trong JavaFX app.
 * ViewRouter giữ reference tới Stage này và swap Scene khi cần.
 * Bất kỳ Controller nào cũng cần navigate → phải có cách lấy ViewRouter
 * mà không cần truyền qua constructor. Singleton giải quyết vấn đề này.
 *
 * <p><b>Thread safety:</b> ViewRouter chỉ được gọi từ JavaFX Application Thread
 * (vì FXMLLoader và Stage chỉ an toàn trên FX thread). Không cần
 * double-checked locking như {@code DbConnectionProvider}.
 *
 * <p><b>Cách hoạt động:</b>
 * <ol>
 *   <li>Load FXML từ {@code /fxml/{viewName}.fxml}</li>
 *   <li>Lấy Controller từ FXMLLoader</li>
 *   <li>Nếu Controller implements {@link ContextAware} → inject params</li>
 *   <li>Thay Scene hiện tại trên Stage bằng Scene mới</li>
 * </ol>
 *
 * <p>Cách dùng:
 * <pre>{@code
 * // Khởi tạo 1 lần trong BidHubApp.start():
 * ViewRouter.getInstance().initialize(primaryStage);
 *
 * // Điều hướng không có params:
 * ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
 *
 * // Điều hướng có params:
 * ViewRouter.getInstance().navigateTo(
 *     Views.AUCTION_DETAIL,
 *     Map.of("auctionId", "abc-123")
 * );
 * }</pre>
 */
public final class ViewRouter {

  /** Instance duy nhất. Không cần volatile vì chỉ dùng trên FX thread. */
  private static ViewRouter instance;

  /** Stage chính của ứng dụng — được inject 1 lần trong {@link #initialize(Stage)}. */
  private Stage primaryStage;

  /** Ngăn khởi tạo từ bên ngoài. */
  private ViewRouter() {}

  /**
   * Trả về instance duy nhất của ViewRouter.
   *
   * @return ViewRouter instance
   */
  public static ViewRouter getInstance() {
    if (instance == null) {
      instance = new ViewRouter();
    }
    return instance;
  }

  /**
   * Khởi tạo ViewRouter với Stage chính — gọi 1 lần trong {@code BidHubApp.start()}.
   *
   * @param stage Stage chính của app JavaFX
   * @throws IllegalArgumentException nếu stage null
   */
  public void initialize(Stage stage) {
    if (stage == null) {
      throw new IllegalArgumentException("Stage không được null");
    }
    this.primaryStage = stage;
  }

  /**
   * Điều hướng tới màn hình mới không có tham số.
   *
   * @param viewName tên màn hình (dùng constants trong {@link com.bidhub.client.Views})
   * @throws IllegalStateException nếu chưa gọi {@link #initialize(Stage)}
   */
  public void navigateTo(String viewName) {
    navigateTo(viewName, Map.of());
  }

  /**
   * Điều hướng tới màn hình mới kèm tham số.
   *
   * <p>Nếu Controller của màn hình đích implements {@link ContextAware},
   * {@code setContext(params)} sẽ được gọi tự động sau khi FXML được load.
   *
   * @param viewName tên màn hình (phải khớp với tên file FXML trong /fxml/)
   * @param params   tham số truyền sang màn hình đích, không được null
   * @throws RuntimeException nếu không load được file FXML
   */
  public void navigateTo(String viewName, Map<String, Object> params) {
    if (primaryStage == null) {
      throw new IllegalStateException(
          "ViewRouter chưa được initialize(). Gọi initialize(stage) trong BidHubApp.start()");
    }
    if (params == null) {
      throw new IllegalArgumentException("params không được null — dùng Map.of() nếu không có params");
    }

    String fxmlPath = "/fxml/" + viewName + ".fxml";
    URL fxmlUrl = getClass().getResource(fxmlPath);
    if (fxmlUrl == null) {
      throw new RuntimeException(
          "Không tìm thấy FXML: " + fxmlPath
          + ". Kiểm tra file tồn tại trong bidhub-client/src/main/resources/fxml/");
    }

    try {
      FXMLLoader loader = new FXMLLoader(fxmlUrl);
      Parent root = loader.load();

      // Inject params nếu controller implements ContextAware
      Object controller = loader.getController();
      if (controller instanceof ContextAware contextAwareCtrl) {
        contextAwareCtrl.setContext(params);
      }

      // Swap Scene trên Stage — giữ nguyên kích thước cửa sổ
      Scene currentScene = primaryStage.getScene();
      if (currentScene != null) {
        currentScene.setRoot(root);
      } else {
        primaryStage.setScene(new Scene(root,
            primaryStage.getWidth() > 0 ? primaryStage.getWidth() : 1024,
            primaryStage.getHeight() > 0 ? primaryStage.getHeight() : 720));
      }
      primaryStage.show();

    } catch (IOException e) {
      throw new RuntimeException("Lỗi load FXML: " + fxmlPath, e);
    }
  }

  /**
   * Trả về Stage chính — dùng khi cần show Alert hoặc Dialog.
   *
   * @return Stage chính, có thể null nếu chưa initialize
   */
  public Stage getPrimaryStage() {
    return primaryStage;
  }

  /**
   * Reset instance — chỉ dùng trong test.
   */
  static void resetForTesting() {
    instance = null;
  }
}
```

```bash
git add bidhub-client/src/main/java/com/bidhub/client/navigation/ViewRouter.java
git commit -m "feat: thêm ViewRouter singleton điều hướng màn hình javafx với ContextAware injection"
```

---

#### Cập nhật `BidHubApp.java` — thêm `ViewRouter.initialize()`

```java
// Trong BidHubApp.java (đã có từ tuần 1) — CHỈ THÊM 1 DÒNG:
// Tìm đoạn code trong start() và thêm ViewRouter.getInstance().initialize(primaryStage);

@Override
public void start(Stage primaryStage) throws IOException {
    // THÊM DÒNG NÀY — khởi tạo ViewRouter với Stage
    ViewRouter.getInstance().initialize(primaryStage);

    // Phần còn lại giữ nguyên như tuần 1
    URL fxmlUrl = getClass().getResource("/fxml/LoginView.fxml");
    // ...
}
```

> [!TIP]
> **Không xóa code tuần 1 trong `BidHubApp.java`**. Chỉ thêm `ViewRouter.getInstance().initialize(primaryStage)` vào đầu method `start()`. Kế thừa — không viết lại.

```bash
git add bidhub-client/src/main/java/com/bidhub/client/BidHubApp.java
git commit -m "feat: khởi tạo ViewRouter trong BidHubApp.start() để sẵn sàng navigate"
```

---

#### `AuctionListView.fxml` + `AuctionListController.java` (Skeleton)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<!--
    AuctionListView.fxml — Danh sách tất cả phiên đấu giá
    Controller: com.bidhub.client.controller.AuctionListController

    Layout:
    BorderPane
    ├── TOP: HBox (title + Button tạo phiên — chỉ visible cho Seller)
    └── CENTER: TableView (danh sách phiên đấu giá)
-->
<BorderPane xmlns="http://javafx.com/javafx/21"
            xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.bidhub.client.controller.AuctionListController">

  <top>
    <HBox alignment="CENTER_LEFT" spacing="16"
          style="-fx-padding: 16; -fx-background-color: #1E3A5F;">
      <Label text="🔨 BidHub — Phiên đấu giá"
             style="-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;"/>
      <HBox HBox.hgrow="ALWAYS"/>
      <!-- Button chỉ visible khi user là Seller — sẽ set visibility từ Controller -->
      <Button fx:id="createAuctionButton"
              text="+ Tạo phiên mới"
              visible="false"
              onAction="#handleCreateAuction"
              style="-fx-background-color: #2563EB; -fx-text-fill: white;
                     -fx-padding: 8 16; -fx-cursor: hand;"/>
      <Button text="Đăng xuất"
              onAction="#handleLogout"
              style="-fx-background-color: #DC2626; -fx-text-fill: white;
                     -fx-padding: 8 16; -fx-cursor: hand;"/>
    </HBox>
  </top>

  <center>
    <VBox spacing="12" style="-fx-padding: 16;">
      <!-- TableView danh sách phiên đấu giá -->
      <TableView fx:id="auctionTable" VBox.vgrow="ALWAYS"
                 onMouseClicked="#handleRowDoubleClick"
                 placeholder="Chưa có phiên đấu giá nào">
        <columns>
          <TableColumn fx:id="colItemName" text="Sản phẩm" prefWidth="250"/>
          <TableColumn fx:id="colCurrentBid" text="Giá hiện tại" prefWidth="150"/>
          <TableColumn fx:id="colEndTime" text="Kết thúc lúc" prefWidth="180"/>
          <TableColumn fx:id="colStatus" text="Trạng thái" prefWidth="120"/>
        </columns>
      </TableView>
      <!-- Label trạng thái tải data -->
      <Label fx:id="statusLabel" text="" style="-fx-text-fill: #6B7280;"/>
    </VBox>
  </center>
</BorderPane>
```

```java
package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.Map;

/**
 * Controller cho màn hình danh sách phiên đấu giá (AuctionListView.fxml).
 *
 * <p>Tuần 3: Chỉ là skeleton — chưa có networking.
 * Data thật và networking sẽ được implement ở Tuần 6.
 *
 * <p>Implements {@link ContextAware} để nhận params khi được navigate tới.
 * Ví dụ: sau khi login thành công, LoginController navigate sang đây kèm
 * {@code Map.of("role", "BIDDER")} để biết có hiện nút "Tạo phiên" không.
 */
public class AuctionListController implements ContextAware {

  @FXML private TableView<Map<String, Object>> auctionTable;
  @FXML private TableColumn<Map<String, Object>, String> colItemName;
  @FXML private TableColumn<Map<String, Object>, String> colCurrentBid;
  @FXML private TableColumn<Map<String, Object>, String> colEndTime;
  @FXML private TableColumn<Map<String, Object>, String> colStatus;
  @FXML private Button createAuctionButton;
  @FXML private Label statusLabel;

  /**
   * Được gọi sau khi tất cả @FXML fields được inject.
   * Tuần 3: Setup cột, hiện thông báo placeholder.
   */
  @FXML
  public void initialize() {
    statusLabel.setText("Đang tải danh sách phiên đấu giá...");
    // TODO Tuần 6: Load data từ server qua NetworkTask
  }

  /**
   * Nhận params từ màn hình trước (thường là sau khi login xong).
   *
   * @param params Map có thể chứa: "role" (String), "username" (String)
   */
  @Override
  public void setContext(Map<String, Object> params) {
    if (params == null) return;
    String role = (String) params.getOrDefault("role", "");
    // Chỉ Seller mới thấy nút tạo phiên mới
    createAuctionButton.setVisible("SELLER".equals(role));
    statusLabel.setText("Xin chào " + params.getOrDefault("username", ""));
  }

  @FXML
  private void handleCreateAuction() {
    System.out.println("[DEBUG] Mở CreateItemView — implement tuần 5");
    // TODO Tuần 5: ViewRouter.getInstance().navigateTo(Views.CREATE_ITEM)
  }

  @FXML
  private void handleLogout() {
    System.out.println("[DEBUG] Đăng xuất — implement tuần 5");
    // TODO Tuần 5: ClientSession.getInstance().logout() rồi navigate Login
  }

  @FXML
  private void handleRowDoubleClick() {
    var selected = auctionTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
      System.out.println("[DEBUG] Chọn auction: " + selected.get("auctionId"));
      // TODO Tuần 6: ViewRouter.navigateTo(Views.AUCTION_DETAIL, Map.of("auctionId", id))
    }
  }
}
```

```bash
git add bidhub-client/src/main/resources/fxml/AuctionListView.fxml
git add bidhub-client/src/main/java/com/bidhub/client/controller/AuctionListController.java
git commit -m "feat: thêm AuctionListView fxml skeleton với TableView 4 cột và AuctionListController"
```

---

#### `AuctionDetailView.fxml` + `AuctionDetailController.java` (Skeleton)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<!--
    AuctionDetailView.fxml — Chi tiết phiên đấu giá + realtime bidding
    Controller: com.bidhub.client.controller.AuctionDetailController
-->
<BorderPane xmlns="http://javafx.com/javafx/21"
            xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.bidhub.client.controller.AuctionDetailController">

  <top>
    <HBox alignment="CENTER_LEFT" spacing="12"
          style="-fx-padding: 12 16; -fx-background-color: #1E3A5F;">
      <Button text="← Quay lại" onAction="#handleBack"
              style="-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;"/>
      <Label fx:id="titleLabel" text="Chi tiết đấu giá"
             style="-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;"/>
    </HBox>
  </top>

  <center>
    <HBox spacing="24" style="-fx-padding: 20;">

      <!-- Cột trái: thông tin sản phẩm + đặt giá -->
      <VBox spacing="16" prefWidth="400">
        <Label fx:id="itemNameLabel" text="Tên sản phẩm: —"
               style="-fx-font-size: 20px; -fx-font-weight: bold;"/>
        <Label fx:id="itemDescLabel" text="Mô tả: —"
               wrapText="true" style="-fx-text-fill: #374151;"/>
        <Separator/>

        <GridPane hgap="12" vgap="8">
          <Label text="Giá khởi điểm:" GridPane.columnIndex="0" GridPane.rowIndex="0"/>
          <Label fx:id="startingPriceLabel" text="—" GridPane.columnIndex="1" GridPane.rowIndex="0"
                 style="-fx-font-weight: bold;"/>

          <Label text="Giá hiện tại:" GridPane.columnIndex="0" GridPane.rowIndex="1"/>
          <Label fx:id="currentBidLabel" text="—" GridPane.columnIndex="1" GridPane.rowIndex="1"
                 style="-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #059669;"/>

          <Label text="Người dẫn đầu:" GridPane.columnIndex="0" GridPane.rowIndex="2"/>
          <Label fx:id="leaderLabel" text="—" GridPane.columnIndex="1" GridPane.rowIndex="2"/>

          <Label text="Kết thúc lúc:" GridPane.columnIndex="0" GridPane.rowIndex="3"/>
          <Label fx:id="endTimeLabel" text="—" GridPane.columnIndex="1" GridPane.rowIndex="3"/>

          <Label text="Đếm ngược:" GridPane.columnIndex="0" GridPane.rowIndex="4"/>
          <Label fx:id="countdownLabel" text="--:--:--" GridPane.columnIndex="1" GridPane.rowIndex="4"
                 style="-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #DC2626;"/>
        </GridPane>

        <Separator/>

        <!-- Form đặt giá -->
        <VBox spacing="8">
          <Label text="Đặt giá của bạn:" style="-fx-font-weight: bold;"/>
          <HBox spacing="8">
            <TextField fx:id="bidAmountField" promptText="Nhập số tiền..."
                       HBox.hgrow="ALWAYS" style="-fx-pref-height: 40px;"/>
            <Button fx:id="placeBidButton" text="Đặt giá"
                    onAction="#handlePlaceBid"
                    style="-fx-background-color: #2563EB; -fx-text-fill: white;
                           -fx-pref-height: 40px; -fx-cursor: hand; -fx-padding: 0 20;"/>
          </HBox>
          <Label fx:id="bidErrorLabel" text="" visible="false"
                 style="-fx-text-fill: #DC2626; -fx-font-size: 13px;"/>
        </VBox>
      </VBox>

      <!-- Cột phải: lịch sử giá -->
      <VBox spacing="8" HBox.hgrow="ALWAYS">
        <Label text="Lịch sử đặt giá" style="-fx-font-weight: bold; -fx-font-size: 16px;"/>
        <ListView fx:id="bidHistoryList" VBox.vgrow="ALWAYS"
                  placeholder="Chưa có lượt đặt giá nào"/>
      </VBox>

    </HBox>
  </center>
</BorderPane>
```

```java
package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.Map;

/**
 * Controller cho màn hình chi tiết đấu giá (AuctionDetailView.fxml).
 *
 * <p>Tuần 3: Chỉ là skeleton — nhận auctionId qua ContextAware, chưa load data thật.
 * Countdown timer, realtime update, đặt giá sẽ implement ở Tuần 6–7.
 */
public class AuctionDetailController implements ContextAware {

  @FXML private Label titleLabel;
  @FXML private Label itemNameLabel;
  @FXML private Label itemDescLabel;
  @FXML private Label startingPriceLabel;
  @FXML private Label currentBidLabel;
  @FXML private Label leaderLabel;
  @FXML private Label endTimeLabel;
  @FXML private Label countdownLabel;
  @FXML private TextField bidAmountField;
  @FXML private Button placeBidButton;
  @FXML private Label bidErrorLabel;
  @FXML private ListView<String> bidHistoryList;

  /** ID của phiên đấu giá đang xem — được inject qua setContext(). */
  private String currentAuctionId;

  @FXML
  public void initialize() {
    bidErrorLabel.setVisible(false);
    bidErrorLabel.setManaged(false);
    // TODO Tuần 6: Bind bidAmountField → disable placeBidButton khi rỗng
  }

  @Override
  public void setContext(Map<String, Object> params) {
    if (params == null) return;
    this.currentAuctionId = (String) params.get("auctionId");
    titleLabel.setText("Chi tiết đấu giá — ID: " + currentAuctionId);
    System.out.println("[DEBUG] AuctionDetailView nhận auctionId: " + currentAuctionId);
    // TODO Tuần 6: Load chi tiết auction từ server
  }

  @FXML
  private void handlePlaceBid() {
    System.out.println("[DEBUG] Đặt giá: " + bidAmountField.getText() + " — implement tuần 6");
  }

  @FXML
  private void handleBack() {
    System.out.println("[DEBUG] Quay lại AuctionList — implement tuần 5 với ViewRouter");
    // TODO Tuần 5: ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST)
  }
}
```

```bash
git add bidhub-client/src/main/resources/fxml/AuctionDetailView.fxml
git add bidhub-client/src/main/java/com/bidhub/client/controller/AuctionDetailController.java
git commit -m "feat: thêm AuctionDetailView skeleton với form đặt giá, countdown, bid history list"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Công Minh

```bash
# Test 1: Compile client
mvn compile -pl bidhub-client
# ✅ PASS: BUILD SUCCESS

# Test 2: Chạy app JavaFX
mvn javafx:run -pl bidhub-client
# ✅ PASS: Cửa sổ BidHub mở ra với LoginView (không crash)

# Test 3: Kiểm tra các FXML mở được trong Scene Builder
# → Mở Scene Builder → File → Open → AuctionListView.fxml
# ✅ PASS: Layout hiện đúng, không lỗi "Unknown class" hay "Could not resolve"

# Test 4: Kiểm tra ViewRouter là Singleton
grep "private static ViewRouter instance" \
  bidhub-client/src/main/java/com/bidhub/client/navigation/ViewRouter.java
# ✅ PASS: Có dòng này

# Test 5: Kiểm tra ViewRouter constructor private
grep "private ViewRouter()" \
  bidhub-client/src/main/java/com/bidhub/client/navigation/ViewRouter.java
# ✅ PASS: Có dòng này

# Test 6: Kiểm tra 3 FXML mới tồn tại
ls bidhub-client/src/main/resources/fxml/
# ✅ PASS: Thấy AuctionListView.fxml, AuctionDetailView.fxml, CreateItemView.fxml

# Test 7: Kiểm tra controller implements ContextAware
grep "implements ContextAware" \
  bidhub-client/src/main/java/com/bidhub/client/controller/AuctionListController.java
# ✅ PASS: Có dòng này

# Test 8: Demo navigateTo (chạy app, thêm debug button trong LoginController tạm thời)
# → Click debug → màn hình chuyển sang AuctionListView
# ✅ PASS: Stage hiện AuctionListView đúng
```

### ❌ FAIL nếu:
- `ViewRouter` constructor là `public` → không phải Singleton
- `navigateTo()` không kiểm tra `primaryStage == null` → NullPointerException khi quên `initialize()`
- FXML lỗi khi mở trong Scene Builder (package controller sai, fx:id trùng)
- `AuctionListController` không implements `ContextAware` → không nhận được params role khi login
- `ViewRouter` có `synchronized` method không cần thiết → trên FX thread không cần synchronized

---

## 👤 KHOA — AuctionDao & BidDao

```
Branch: feature/tuan-3-khoa-auction-bid-dao
Phụ thuộc: Đợi Đăng push DbConnectionProvider + schema.sql (hoặc dùng createInMemoryForTesting())
Deadline: Thứ 7 25/04 23:59
```

📌 **[Tiêu chí điểm: Chức năng đấu giá (1.0đ) — phần data access] + [Unit Test JUnit (0.5đ)]**

### 📝 Mô tả bài tập

Bạn implement `AuctionDao` và `BidDao` — 2 DAO phức tạp hơn UserDao vì có query điều kiện (`WHERE status='RUNNING'`, ORDER BY, aggregate function `MAX`). Ngoài ra viết test đầy đủ cho tất cả 4 DAO của cả nhóm.

### 📁 Cấu trúc file cần tạo

```
bidhub-server/src/
├── main/java/com/bidhub/server/dao/
│   ├── AuctionDao.java          ← CRUD + query phức tạp cho phiên đấu giá
│   └── BidDao.java              ← Lưu bid, lấy cao nhất, lịch sử theo phiên
└── test/java/com/bidhub/server/dao/
    ├── AuctionDaoTest.java      ← Test đầy đủ AuctionDao với in-memory SQLite
    └── BidDaoTest.java          ← Test đầy đủ BidDao
```

### 📋 Yêu cầu chi tiết

#### `AuctionDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho {@link Auction} — truy xuất bảng {@code auctions}.
 *
 * <p>Các method quan trọng:
 * <ul>
 *   <li>{@link #findActiveAuctions()} — dùng bởi {@code AuctionManager} khi server khởi động</li>
 *   <li>{@link #updateHighestBid(String, double, String)} — dùng sau mỗi bid hợp lệ</li>
 *   <li>{@link #updateStatus(String, AuctionStatus)} — dùng khi lifecycle task đóng phiên</li>
 *   <li>{@link #updateEndTime(String, LocalDateTime)} — dùng bởi Anti-Sniping (tuần 8)</li>
 * </ul>
 */
public class AuctionDao {

  private final DbConnectionProvider dbProvider;

  /**
   * Constructor nhận DbConnectionProvider qua DI.
   *
   * @param dbProvider không được null
   */
  public AuctionDao(DbConnectionProvider dbProvider) {
    if (dbProvider == null) {
      throw new IllegalArgumentException("dbProvider không được null");
    }
    this.dbProvider = dbProvider;
  }

  /**
   * Lưu phiên đấu giá mới vào database.
   *
   * @param auction phiên đấu giá cần lưu
   */
  public void save(Auction auction) {
    if (auction == null) {
      throw new IllegalArgumentException("auction không được null");
    }
    String sql = """
        INSERT OR IGNORE INTO auctions
            (id, item_id, start_time, end_time, starting_price,
             current_highest_bid, highest_bidder_id, status, minimum_increment,
             created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auction.getId());
      ps.setString(2, auction.getItemId());
      ps.setString(3, auction.getStartTime().toString());
      ps.setString(4, auction.getEndTime().toString());
      ps.setDouble(5, auction.getStartingPrice());
      ps.setDouble(6, auction.getCurrentHighestBid());
      // highestBidderId có thể null khi chưa có ai bid
      ps.setString(7, auction.getHighestBidderId()); // setString(x, null) = SQL NULL
      ps.setString(8, auction.getStatus().name());
      ps.setDouble(9, auction.getMinimumIncrement());
      ps.setString(10, auction.getCreatedAt().toString());
      ps.setString(11, auction.getUpdatedAt().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi lưu auction id=" + auction.getId(), e);
    }
  }

  /**
   * Tìm phiên đấu giá theo ID.
   *
   * @param id UUID của phiên đấu giá
   * @return {@link Optional} chứa Auction nếu tìm thấy
   */
  public Optional<Auction> findById(String id) {
    String sql = "SELECT * FROM auctions WHERE id = ?";
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi tìm auction id=" + id, e);
    }
    return Optional.empty();
  }

  /**
   * Lấy danh sách tất cả phiên đấu giá đang RUNNING.
   *
   * <p>Được gọi bởi {@code AuctionManager.start()} khi server khởi động
   * để nạp các phiên đang diễn ra vào RAM.
   *
   * @return danh sách Auction có status=RUNNING, có thể rỗng
   */
  public List<Auction> findActiveAuctions() {
    String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY end_time ASC";
    List<Auction> result = new ArrayList<>();
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, AuctionStatus.RUNNING.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi lấy danh sách active auctions", e);
    }
    return result;
  }

  /**
   * Cập nhật trạng thái phiên đấu giá.
   *
   * <p>Dùng khi lifecycle task kết thúc phiên: {@code RUNNING → FINISHED}.
   *
   * @param auctionId UUID của phiên đấu giá
   * @param newStatus trạng thái mới
   */
  public void updateStatus(String auctionId, AuctionStatus newStatus) {
    String sql = "UPDATE auctions SET status = ?, updated_at = ? WHERE id = ?";
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newStatus.name());
      ps.setString(2, LocalDateTime.now().toString());
      ps.setString(3, auctionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi update status auction=" + auctionId, e);
    }
  }

  /**
   * Cập nhật giá cao nhất và người dẫn đầu sau khi có bid hợp lệ.
   *
   * <p>Được gọi sau khi bid được validate và lưu vào bid_transactions.
   * Cả 2 thao tác này (BidDao.save + AuctionDao.updateHighestBid) phải
   * thực hiện cùng nhau — sẽ xử lý transaction ở tuần 7.
   *
   * @param auctionId  UUID của phiên đấu giá
   * @param amount     giá bid mới (phải > current_highest_bid)
   * @param bidderId   UUID của người đặt giá
   */
  public void updateHighestBid(String auctionId, double amount, String bidderId) {
    String sql = """
        UPDATE auctions
        SET current_highest_bid = ?, highest_bidder_id = ?, updated_at = ?
        WHERE id = ?
        """;
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setDouble(1, amount);
      ps.setString(2, bidderId);
      ps.setString(3, LocalDateTime.now().toString());
      ps.setString(4, auctionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi update highest bid auction=" + auctionId, e);
    }
  }

  /**
   * Cập nhật thời điểm kết thúc phiên — dùng bởi Anti-Sniping (tuần 8).
   *
   * @param auctionId  UUID của phiên đấu giá
   * @param newEndTime thời điểm kết thúc mới (phải sau thời điểm cũ)
   */
  public void updateEndTime(String auctionId, LocalDateTime newEndTime) {
    String sql = "UPDATE auctions SET end_time = ?, updated_at = ? WHERE id = ?";
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newEndTime.toString());
      ps.setString(2, LocalDateTime.now().toString());
      ps.setString(3, auctionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi update end_time auction=" + auctionId, e);
    }
  }

  /**
   * Map ResultSet hàng hiện tại sang đối tượng {@link Auction}.
   *
   * @param rs ResultSet đang trỏ vào hàng auction
   * @return Auction object đầy đủ
   */
  private Auction mapRow(ResultSet rs) throws SQLException {
    String highestBidderId = rs.getString("highest_bidder_id"); // Có thể null
    return new Auction(
        rs.getString("id"),
        rs.getString("item_id"),
        LocalDateTime.parse(rs.getString("start_time")),
        LocalDateTime.parse(rs.getString("end_time")),
        rs.getDouble("starting_price"),
        rs.getDouble("current_highest_bid"),
        highestBidderId, // null nếu chưa có bid
        AuctionStatus.valueOf(rs.getString("status")),
        rs.getDouble("minimum_increment"),
        LocalDateTime.parse(rs.getString("created_at")),
        LocalDateTime.parse(rs.getString("updated_at"))
    );
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/dao/AuctionDao.java
git commit -m "feat: thêm AuctionDao với save, findById, findActiveAuctions, updateHighestBid, updateStatus"
```

---

#### `BidDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.BidTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho {@link BidTransaction} — truy xuất bảng {@code bid_transactions}.
 *
 * <p>Các method quan trọng với concurrency (tuần 7):
 * <ul>
 *   <li>{@link #save(BidTransaction)} — ghi bid hợp lệ vào DB</li>
 *   <li>{@link #getHighestBid(String)} — lấy bid cao nhất của phiên để verify khi đóng phiên</li>
 *   <li>{@link #findByAuctionId(String)} — lấy lịch sử để hiển thị chart (tuần 8)</li>
 * </ul>
 */
public class BidDao {

  private final DbConnectionProvider dbProvider;

  /**
   * Constructor nhận DbConnectionProvider qua DI.
   *
   * @param dbProvider không được null
   */
  public BidDao(DbConnectionProvider dbProvider) {
    if (dbProvider == null) {
      throw new IllegalArgumentException("dbProvider không được null");
    }
    this.dbProvider = dbProvider;
  }

  /**
   * Lưu giao dịch bid vào database.
   *
   * <p>Gọi sau khi bid được validate bởi {@code BidValidator}.
   * Trong production (tuần 7), cần atomic với {@code AuctionDao.updateHighestBid()}.
   *
   * @param bid giao dịch bid cần lưu, không được null
   */
  public void save(BidTransaction bid) {
    if (bid == null) {
      throw new IllegalArgumentException("bid không được null");
    }
    String sql = """
        INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, bid_time)
        VALUES (?, ?, ?, ?, ?)
        """;
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, bid.getId());
      ps.setString(2, bid.getAuctionId());
      ps.setString(3, bid.getBidderId());
      ps.setDouble(4, bid.getBidAmount());
      ps.setString(5, bid.getBidTime().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi lưu BidTransaction id=" + bid.getId(), e);
    }
  }

  /**
   * Lấy bid có giá cao nhất trong một phiên đấu giá.
   *
   * <p>Dùng khi lifecycle task kết thúc phiên để xác định winner cuối cùng.
   * Dùng SQL {@code ORDER BY bid_amount DESC LIMIT 1} thay vì đọc hết và so sánh trong Java.
   *
   * @param auctionId UUID của phiên đấu giá
   * @return {@link Optional} chứa BidTransaction cao nhất, hoặc empty nếu chưa có bid
   */
  public Optional<BidTransaction> getHighestBid(String auctionId) {
    String sql = """
        SELECT * FROM bid_transactions
        WHERE auction_id = ?
        ORDER BY bid_amount DESC
        LIMIT 1
        """;
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi lấy highest bid auction=" + auctionId, e);
    }
    return Optional.empty();
  }

  /**
   * Lấy lịch sử tất cả bid của một phiên, sắp xếp theo thời gian tăng dần.
   *
   * <p>Dùng để hiển thị biểu đồ giá (tuần 8) và danh sách lịch sử bid.
   *
   * @param auctionId UUID của phiên đấu giá
   * @return danh sách BidTransaction sắp xếp theo bid_time ASC, có thể rỗng
   */
  public List<BidTransaction> findByAuctionId(String auctionId) {
    String sql = """
        SELECT * FROM bid_transactions
        WHERE auction_id = ?
        ORDER BY bid_time ASC
        """;
    List<BidTransaction> result = new ArrayList<>();
    Connection conn = dbProvider.getConnection();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Lỗi lấy bids cho auction=" + auctionId, e);
    }
    return result;
  }

  /**
   * Map ResultSet hàng hiện tại sang {@link BidTransaction}.
   *
   * @param rs ResultSet đang trỏ vào hàng bid_transactions
   * @return BidTransaction object
   */
  private BidTransaction mapRow(ResultSet rs) throws SQLException {
    return new BidTransaction(
        rs.getString("id"),
        rs.getString("auction_id"),
        rs.getString("bidder_id"),
        rs.getDouble("bid_amount"),
        LocalDateTime.parse(rs.getString("bid_time"))
    );
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/dao/BidDao.java
git commit -m "feat: thêm BidDao với save, getHighestBid (ORDER BY DESC LIMIT 1), findByAuctionId"
```

---

#### `AuctionDaoTest.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.config.MigrationRunner;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import com.bidhub.server.model.Bidder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Unit test cho {@link AuctionDao} + {@link BidDao} */
@DisplayName("AuctionDao & BidDao — CRUD với in-memory SQLite")
class AuctionDaoTest {

  private DbConnectionProvider dbProvider;
  private AuctionDao auctionDao;
  private BidDao bidDao;
  private UserDao userDao;

  @BeforeEach
  void setUp() {
    dbProvider = DbConnectionProvider.createInMemoryForTesting();
    MigrationRunner.run(dbProvider.getConnection());
    auctionDao = new AuctionDao(dbProvider);
    bidDao = new BidDao(dbProvider);
    userDao = new UserDao(dbProvider);
  }

  @AfterEach
  void tearDown() {
    dbProvider.closeConnection();
  }

  // Auction fixture helper
  private Auction createTestAuction(String itemId) {
    return new Auction(
        itemId,
        LocalDateTime.now().minusMinutes(1),
        LocalDateTime.now().plusHours(1),
        10_000_000.0,
        500_000.0
    );
  }

  @Test
  @DisplayName("save() rồi findById() trả về đúng Auction với status ban đầu là OPEN")
  void testSave_FindById_ReturnsAuction() {
    // Arrange
    Auction auction = createTestAuction("item-id-1");

    // Act
    auctionDao.save(auction);
    Optional<Auction> found = auctionDao.findById(auction.getId());

    // Assert
    assertTrue(found.isPresent(), "Phải tìm thấy auction sau khi save");
    assertEquals(auction.getId(), found.get().getId());
    assertEquals(AuctionStatus.OPEN, found.get().getStatus());
    assertEquals(10_000_000.0, found.get().getStartingPrice(), 0.01);
  }

  @Test
  @DisplayName("findById() với id không tồn tại → Optional.empty()")
  void testFindById_NotExist_ReturnsEmpty() {
    // Act
    Optional<Auction> found = auctionDao.findById("id-khong-ton-tai");

    // Assert
    assertTrue(found.isEmpty());
  }

  @Test
  @DisplayName("findActiveAuctions() chỉ trả về auction có status RUNNING")
  void testFindActiveAuctions_OnlyRunning() {
    // Arrange — tạo 1 OPEN, 1 RUNNING, 1 FINISHED
    Auction open = createTestAuction("item-1");
    Auction running = createTestAuction("item-2");
    Auction finished = createTestAuction("item-3");

    auctionDao.save(open);
    auctionDao.save(running);
    auctionDao.save(finished);

    auctionDao.updateStatus(running.getId(), AuctionStatus.RUNNING);
    auctionDao.updateStatus(finished.getId(), AuctionStatus.FINISHED);

    // Act
    List<Auction> actives = auctionDao.findActiveAuctions();

    // Assert
    assertEquals(1, actives.size(), "Chỉ có 1 phiên RUNNING");
    assertEquals(running.getId(), actives.get(0).getId());
    assertEquals(AuctionStatus.RUNNING, actives.get(0).getStatus());
  }

  @Test
  @DisplayName("updateHighestBid() cập nhật đúng giá và bidderId trong DB")
  void testUpdateHighestBid_UpdatesCorrectly() {
    // Arrange
    Auction auction = createTestAuction("item-id-2");
    auctionDao.save(auction);
    Bidder bidder = new Bidder("alice_test", "hash", "alice@test.com");
    userDao.save(bidder); // Cần save bidder vì có FK constraint

    // Act
    auctionDao.updateHighestBid(auction.getId(), 12_000_000.0, bidder.getId());

    // Assert
    Optional<Auction> updated = auctionDao.findById(auction.getId());
    assertTrue(updated.isPresent());
    assertEquals(12_000_000.0, updated.get().getCurrentHighestBid(), 0.01);
    assertEquals(bidder.getId(), updated.get().getHighestBidderId());
  }

  @Test
  @DisplayName("updateStatus() thay đổi trạng thái đúng trong DB")
  void testUpdateStatus_ChangesStatus() {
    // Arrange
    Auction auction = createTestAuction("item-id-3");
    auctionDao.save(auction);
    assertEquals(AuctionStatus.OPEN, auctionDao.findById(auction.getId()).get().getStatus());

    // Act
    auctionDao.updateStatus(auction.getId(), AuctionStatus.RUNNING);

    // Assert
    assertEquals(AuctionStatus.RUNNING,
        auctionDao.findById(auction.getId()).get().getStatus());
  }

  @Test
  @DisplayName("updateEndTime() cập nhật endTime đúng trong DB")
  void testUpdateEndTime_UpdatesEndTime() {
    // Arrange
    Auction auction = createTestAuction("item-id-4");
    auctionDao.save(auction);
    LocalDateTime newEndTime = LocalDateTime.now().plusHours(2);

    // Act
    auctionDao.updateEndTime(auction.getId(), newEndTime);

    // Assert
    Optional<Auction> updated = auctionDao.findById(auction.getId());
    assertTrue(updated.isPresent());
    // So sánh đến giây (bỏ qua nano)
    assertEquals(newEndTime.withNano(0),
        updated.get().getEndTime().withNano(0));
  }

  // ===================== BidDao Tests =====================

  @Test
  @DisplayName("BidDao.save() rồi getHighestBid() trả về bid cao nhất")
  void testBidDao_GetHighestBid() {
    // Arrange
    Auction auction = createTestAuction("item-bid-1");
    auctionDao.save(auction);
    Bidder bidder = new Bidder("bid_user", "hash", "bid@test.com");
    userDao.save(bidder);

    BidTransaction bid1 = new BidTransaction(auction.getId(), bidder.getId(), 11_000_000.0);
    BidTransaction bid2 = new BidTransaction(auction.getId(), bidder.getId(), 13_000_000.0);
    BidTransaction bid3 = new BidTransaction(auction.getId(), bidder.getId(), 12_000_000.0);

    bidDao.save(bid1);
    bidDao.save(bid2);
    bidDao.save(bid3);

    // Act
    Optional<BidTransaction> highest = bidDao.getHighestBid(auction.getId());

    // Assert
    assertTrue(highest.isPresent());
    assertEquals(13_000_000.0, highest.get().getBidAmount(), 0.01);
  }

  @Test
  @DisplayName("BidDao.findByAuctionId() trả về danh sách sort theo bid_time ASC")
  void testBidDao_FindByAuctionId_SortedByTime() {
    // Arrange
    Auction auction = createTestAuction("item-bid-2");
    auctionDao.save(auction);
    Bidder bidder = new Bidder("sort_user", "hash", "sort@test.com");
    userDao.save(bidder);

    BidTransaction bid1 = new BidTransaction(auction.getId(), bidder.getId(), 11_000_000.0);
    BidTransaction bid2 = new BidTransaction(auction.getId(), bidder.getId(), 12_000_000.0);

    bidDao.save(bid1);
    // Đợi 1ms để đảm bảo bid_time khác nhau
    try { Thread.sleep(1); } catch (InterruptedException ignored) {}
    bidDao.save(bid2);

    // Act
    List<BidTransaction> bids = bidDao.findByAuctionId(auction.getId());

    // Assert
    assertEquals(2, bids.size());
    // Bid sớm hơn đứng trước (ASC)
    assertTrue(bids.get(0).getBidTime().isBefore(bids.get(1).getBidTime())
        || bids.get(0).getBidTime().isEqual(bids.get(1).getBidTime()),
        "Phải sort theo bid_time ASC");
  }

  @Test
  @DisplayName("BidDao.getHighestBid() khi chưa có bid → Optional.empty()")
  void testBidDao_GetHighestBid_NoBids_ReturnsEmpty() {
    // Arrange
    Auction auction = createTestAuction("item-empty");
    auctionDao.save(auction);

    // Act
    Optional<BidTransaction> highest = bidDao.getHighestBid(auction.getId());

    // Assert
    assertTrue(highest.isEmpty(), "Chưa có bid → phải trả về Optional.empty()");
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/dao/BidDao.java
git add bidhub-server/src/test/java/com/bidhub/server/dao/AuctionDaoTest.java
git commit -m "test: thêm AuctionDaoTest và BidDaoTest 9 cases kiểm tra CRUD, findActive, getHighestBid"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Khoa

```bash
# Test 1: Compile
mvn compile -pl bidhub-server
# ✅ PASS: BUILD SUCCESS

# Test 2: Chạy AuctionDaoTest
mvn test -pl bidhub-server -Dtest="AuctionDaoTest"
# ✅ PASS: "Tests run: 9, Failures: 0, Errors: 0"

# Test 3: Test findActiveAuctions cụ thể
mvn test -pl bidhub-server -Dtest="AuctionDaoTest#testFindActiveAuctions_OnlyRunning"
# ✅ PASS

# Test 4: Test getHighestBid
mvn test -pl bidhub-server -Dtest="AuctionDaoTest#testBidDao_GetHighestBid"
# ✅ PASS: Bid 13 triệu được trả về

# Test 5: Kiểm tra KHÔNG dùng Statement (chỉ PreparedStatement)
grep "new Statement\|createStatement()" \
  bidhub-server/src/main/java/com/bidhub/server/dao/AuctionDao.java \
  bidhub-server/src/main/java/com/bidhub/server/dao/BidDao.java
# ✅ PASS: Không có dòng nào (chỉ được dùng createStatement trong MigrationRunner)

# Test 6: Đếm tổng test toàn project
mvn test
# ✅ PASS: ≥ 65 tests, 0 failures
# (tuần 1: 21 + tuần 2: ≥ 25 + tuần 3: ≥ 22 = ≥ 65)

# Test 7: Kiểm tra Optional được dùng đúng
grep "Optional<" bidhub-server/src/main/java/com/bidhub/server/dao/AuctionDao.java
# ✅ PASS: Thấy Optional<Auction> trong findById và findByStatus
```

### ❌ FAIL nếu:
- `findActiveAuctions()` trả về tất cả auction thay vì chỉ RUNNING → query sai điều kiện WHERE
- `getHighestBid()` dùng Java để tìm max thay vì SQL `ORDER BY DESC LIMIT 1` → không dùng được khi dữ liệu lớn
- Test không dùng `createInMemoryForTesting()` → để lại file `.db` hoặc conflict với test khác
- `save()` không xử lý `highestBidderId` null → `NullPointerException` khi auction chưa có bid
- Tổng test toàn project < 65 → cần bổ sung thêm cases

---

## 🔄 QUY TRÌNH KIỂM TRA CHÉO — CHỦ NHẬT 26/04/2026

### Phân công review

| Người làm | Reviewer 1 | Reviewer 2 |
|-----------|------------|------------|
| **Đăng** (DbConnectionProvider + Schema) | Quốc Minh | Khoa |
| **Quốc Minh** (UserDao + ItemDao) | Khoa | Công Minh |
| **Công Minh** (ViewRouter + FXML) | Đăng | Quốc Minh |
| **Khoa** (AuctionDao + BidDao + Tests) | Công Minh | Đăng |

### Quy trình review (mỗi người ~30 phút)

#### Bước 1: Pull code về (5 phút)
```bash
git fetch origin
git checkout feature/tuan-3-[ten-nguoi-can-review]
# Ví dụ: git checkout feature/tuan-3-dang-database-setup
```

#### Bước 2: Chạy test đầu ra (10 phút)
Chạy từng lệnh trong phần "TEST ĐẦU RA" của người đó. Ghi lại ✅/❌.

#### Bước 3: Đọc code và chuẩn bị câu hỏi (10 phút)

**Câu hỏi bắt buộc hỏi Đăng:**
1. Tại sao `getConnection()` được đánh dấu `synchronized`? Nếu bỏ `synchronized` thì điều gì có thể xảy ra?
2. `resetForTesting()` tồn tại để làm gì? Nếu không có nó, có thể xảy ra vấn đề gì giữa các test method?
3. Tại sao `PRAGMA foreign_keys=ON` quan trọng? Nếu không có, có thể lưu `bid_transaction` với `auction_id` không tồn tại được không?
4. `createInMemoryForTesting()` vs `getInstance()` — khi nào dùng cái nào? Tại sao test không nên dùng `getInstance()`?

**Câu hỏi bắt buộc hỏi Quốc Minh:**
1. `INSERT OR IGNORE` trong `UserDao.save()` nghĩa là gì? Tại sao không dùng `INSERT`?
2. Tại sao `ResultSet` phải đóng trong `try-with-resources` riêng, không đóng cùng `PreparedStatement`?
3. `mapRow()` của `UserDao` đọc cột `role` rồi `switch(role)` để tạo đúng subclass — đây là ví dụ của pattern gì?
4. `MAPPER` trong `ItemDao` là `static final` — tại sao? Nếu tạo `new ObjectMapper()` trong mỗi method thì ảnh hưởng thế nào?

**Câu hỏi bắt buộc hỏi Công Minh:**
1. `ViewRouter.navigateTo()` có `if (controller instanceof ContextAware ctx)` — đây là pattern gì? Java 16+ pattern matching for instanceof.
2. Tại sao `ViewRouter` Singleton không cần `volatile` và `synchronized` như `DbConnectionProvider`?
3. `currentScene.setRoot(root)` vs `primaryStage.setScene(new Scene(...))` — khác nhau thế nào? Tại sao prefer `setRoot`?
4. Nếu `ViewRouter.initialize()` không được gọi và ai đó gọi `navigateTo()` — điều gì xảy ra? Code hiện tại xử lý case này như thế nào?

**Câu hỏi bắt buộc hỏi Khoa:**
1. `getHighestBid()` dùng `ORDER BY bid_amount DESC LIMIT 1` — tại sao không đọc tất cả rồi dùng `stream().max()` trong Java?
2. `findByAuctionId()` sort `ORDER BY bid_time ASC` — tại sao cần ASC ở đây? AuctionDetailView và Price Chart cần gì?
3. Test `testUpdateHighestBid_UpdatesCorrectly` cần `userDao.save(bidder)` trước khi `updateHighestBid()` — tại sao? Bỏ đi thì sao?
4. Tổng test của tuần 3 là bao nhiêu? Cộng dồn cả 3 tuần là bao nhiêu?

#### Bước 4: Comment PR (5 phút)

```markdown
## Review — [Tên reviewer] → [Tên người code]
**Branch:** `feature/tuan-3-[ten]-...`

### ✅ Test Results
- [ ] `mvn compile -pl bidhub-[module]` → ✅/❌
- [ ] `mvn test -pl bidhub-[module] -Dtest="[TestClass]"` → ✅/❌ | Tests: ___ / Failures: ___
- [ ] `mvn test` (tổng toàn project) → Tests: ___ (≥ 65?) / Failures: ___
- [ ] Cấu trúc file đúng yêu cầu → ✅/❌

### 🔍 Code Quality
- PreparedStatement (không nối chuỗi SQL): ✅ / ❌ Tìm thấy chỗ nào?
- Optional thay vì null: ✅ / ⚠️ Method nào trả về null?
- Javadoc đầy đủ: ✅ / ⚠️ Thiếu ở đâu?
- Singleton đúng cách (private constructor): ✅ / ⚠️

### ❓ Câu hỏi khi đọc code (thể hiện đã đọc thật sự)
1. [Câu hỏi 1]
2. [Câu hỏi 2]

### 📝 Góp ý cải thiện (nếu có)
-

### Verdict
- [ ] ✅ Approve
- [ ] 🔄 Request changes: [mô tả cần sửa gì cụ thể]
```

---

## 📝 CHECKLIST TỔNG HỢP — KIỂM TRA TRƯỚC KHI NỘP

### Cho tất cả mọi người:
- [ ] Đã đọc xong 4 bài tự học (JDBC, Singleton, DAO, JavaFX Navigation)
- [ ] Có thể giải thích code của người khác khi được hỏi
- [ ] Branch của mình: `mvn compile` pass, `mvn test` pass trên máy local
- [ ] PR đã tạo đúng format `[Tuần 3] Tên - Mô tả ngắn`, gán đúng 2 reviewers

### Checklist Đăng (DbConnectionProvider + Schema):
- [ ] `DbConnectionProvider` constructor là `private` — không thể `new DbConnectionProvider()` từ ngoài
- [ ] `instance` field có `volatile` keyword
- [ ] `getConnection()` có `synchronized` keyword
- [ ] `schema.sql` có đủ 4 bảng với `CREATE TABLE IF NOT EXISTS`
- [ ] `schema.sql` có `FOREIGN KEY` constraints đúng
- [ ] `MigrationRunner` đọc file từ classpath (không hardcode đường dẫn)
- [ ] `createInMemoryForTesting()` tạo instance mới với `jdbc:sqlite::memory:`
- [ ] `DbConnectionProviderTest` có ≥ 5 test cases, đều pass

### Checklist Quốc Minh (UserDao + ItemDao):
- [ ] `UserDao.save()` dùng `INSERT OR IGNORE` — không throw khi duplicate
- [ ] `UserDao.findByUsername()` trả về `Optional<User>` (không phải null)
- [ ] `UserDao.mapRow()` tạo đúng `Bidder`/`Seller`/`Admin` theo cột `role`
- [ ] `ItemDao` dùng Jackson `ObjectMapper` static final (không tạo mới mỗi lần)
- [ ] `ItemDao.serializeExtraData()` serialize đúng cho 3 loại Item
- [ ] KHÔNG có SQL nối chuỗi trực tiếp với biến (SQL Injection)
- [ ] `UserDaoTest` có ≥ 8 test cases, đều pass

### Checklist Công Minh (ViewRouter + FXML):
- [ ] `ViewRouter.getInstance()` gọi 2 lần trả về cùng instance
- [ ] `ViewRouter` constructor là `private`
- [ ] `ViewRouter.navigateTo()` kiểm tra `primaryStage == null` → throw
- [ ] `ViewRouter.navigateTo()` inject params nếu controller là `ContextAware`
- [ ] `BidHubApp.start()` gọi `ViewRouter.getInstance().initialize(primaryStage)`
- [ ] 3 FXML mới: `AuctionListView`, `AuctionDetailView`, `CreateItemView`
- [ ] Tất cả 3 FXML mở được trong Scene Builder không lỗi
- [ ] `AuctionListController` và `AuctionDetailController` implements `ContextAware`

### Checklist Khoa (AuctionDao + BidDao + Tests):
- [ ] `AuctionDao.findActiveAuctions()` dùng `WHERE status = 'RUNNING'`
- [ ] `AuctionDao.save()` xử lý đúng `highestBidderId = null`
- [ ] `BidDao.getHighestBid()` dùng `ORDER BY bid_amount DESC LIMIT 1` (không sort trong Java)
- [ ] `BidDao.findByAuctionId()` sort theo `bid_time ASC`
- [ ] `AuctionDaoTest` có ≥ 6 test cases cho AuctionDao
- [ ] `AuctionDaoTest` có ≥ 3 test cases cho BidDao
- [ ] Tổng test toàn project `mvn test` → ≥ 65 cases, 0 failures

---

## ⏰ TIMELINE GỢI Ý

| Ngày | Việc cần làm |
|------|-------------|
| **T2 20/04 (Tối)** | Kick-off: phân công rõ ràng, Đăng bắt đầu schema.sql + DbConnectionProvider ngay tối đó |
| **T3 21/04** | **Đăng push lên branch** — cả nhóm checkout về; Khoa bắt đầu AuctionDao/BidDao |
| **T3-T4 21-22/04** | Đọc tài liệu tự học (JDBC, Singleton, DAO, JavaFX Navigation) |
| **T4 22/04** | Quốc Minh bắt đầu UserDao/ItemDao; Công Minh bắt đầu ViewRouter + FXML |
| **T5 23/04 (Tối)** | Mid-week check-in: demo `mvn test` xanh, hỏi nếu bị stuck với JDBC |
| **T6 24/04** | Hoàn thiện code, tự test theo checklist, thêm Javadoc còn thiếu |
| **T7 25/04** | **DEADLINE 23:59** — final push, tạo PR format đúng, gán reviewers |
| **CN 26/04 (Sáng)** | Review chéo (mỗi người ~30 phút), hỏi miệng bắt buộc, merge vào `develop` |

> [!TIP]
> **Dùng `jdbc:sqlite::memory:` cho TẤT CẢ test DAO.** Không bao giờ dùng `getInstance()` trong test. Lý do: in-memory DB là sạch và nhanh, không để lại file `.db` trong repo, không conflict giữa các test method.

> [!TIP]
> **Thứ tự implement đúng cho DAO:** (1) Viết `save()` → (2) Test `save()` + `findById()` → (3) Viết các method query phức tạp hơn → (4) Test từng method. Đừng viết hết rồi mới test — rất khó debug.

> [!WARNING]
> **Không push code sau 23:59 Thứ 7.** Code push sau deadline không được tính vào review Chủ nhật.

> [!CAUTION]
> **Tuần 4 sẽ dùng ngay các DAO này.** `RequestHandler` sẽ gọi `UserDao` khi xử lý LOGIN, `ItemDao` khi xử lý CREATE_ITEM. Nếu tuần 3 làm sai → tuần 4 bị sai theo. Merge chỉ khi `mvn test` xanh hoàn toàn.
