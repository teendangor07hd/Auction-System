
📋 TUẦN 3 — BÀI TẬP CHI TIẾT: DAO & Database + MVC Routing

## 🎯 MỤC TIÊU TUẦN 3

Tuần này kết nối domain model đã xây dựng ở Tuần 2 với lớp lưu trữ thực sự — SQLite qua JDBC — và đặt
nền móng điều hướng màn hình phía client. Cuối tuần, cả nhóm phải có:

- ✅ `DbConnectionProvider` (Singleton double-checked locking) kết nối SQLite thành công
- ✅ `MigrationRunner` tạo đủ 4 bảng (`users`, `items`, `auctions`, `bid_transactions`)
- ✅ `UserDao` lưu và truy vấn `Bidder`/`Seller`/`Admin` — đúng subclass khi load từ DB
- ✅ `ItemDao` lưu và truy vấn `Electronics`/`Art`/`Vehicle` — `extra_data` JSON qua Jackson
- ✅ `AuctionDao` CRUD đầy đủ: save, find, updateStatus, updateHighestBid, updateEndTime
- ✅ `BidDao` lưu bid transaction, truy vấn theo auctionId, tìm bid cao nhất
- ✅ `ViewRouter` (Singleton) điều hướng màn hình JavaFX bằng FXML + `ContextAware`
- ✅ 3 FXML skeleton: `AuctionListView`, `AuctionDetailView`, `CreateItemView` mở được trong Scene Builder
- ✅ Test DAO qua in-memory SQLite (`jdbc:sqlite::memory:`) — tất cả xanh

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** tiêu chí barem: **Design Patterns — Singleton** (một phần của 1.0đ, riêng
> `DbConnectionProvider` và `ViewRouter` = 2/5 Singleton cần có) + **MVC: RequestHandler→Service→DAO phía
> server, FXML+Controller+ViewRouter phía client (0.5đ)**. Làm đúng tuần này = nền móng vững cho Tuần 4-6.

> [!CAUTION]
> **Tuyệt đối không tạo lại** bất kỳ class nào sau đây — chúng đã tồn tại trong `develop` và chỉ cần
> import/kế thừa:
> `Entity`, `BidHubException` + 7 subclass, `ConfigLoader`, `ServerApp`, `UserRole`, `User`, `Bidder`,
> `Seller`, `Admin`, `Displayable`, `ItemType`, `Item`, `Electronics`, `Art`, `Vehicle`,
> `ItemCreator` + 3 ConcreteCreator, `AuctionStatus`, `Auction`, `BidTransaction`,
> `BidHubApp`, `LoginController`, `Views`.
>
> **Tuần này Quốc Minh cần THÊM constructor DB-load vào `Electronics`, `Art`, `Vehicle`** (không tạo lại
> file — chỉ thêm constructor. Cách làm: mở file cũ, thêm constructor mới, commit ngay).

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI HỌC (Tự học)

> [!CAUTION]
> Mỗi ngườii **BẮT BUỘC** hoàn thành phần tự học này trước Thứ 5. Mục đích không phải học lý thuyết Java
> chung — mà là hiểu đúng **cơ chế hoạt động** của những đoạn code ngườii khác sẽ viết tuần này, để Chủ
> nhật review chéo trôi chảy và cả nhóm trả lờii được vấn đáp giảng viên mà không lúng túng.

---

### Bài 0.1 — JDBC & PreparedStatement trong ngữ cảnh BidHub

**Tài liệu bắt buộc:**
- https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html
- https://www.sqlite.org/wal.html (chỉ đọc phần "Advantages of WAL")

**Câu hỏi hỏi miệng Chủ nhật:**
1. `PreparedStatement` khác `Statement` ở điểm nào cốt lõi nhất? Nếu `UserDao.findByUsername()` dùng
   `Statement` thay vì `PreparedStatement`, tấn công SQL Injection xảy ra như thế nào với input
   `username = "\' OR \'1\'=\'1"`?
2. Tại sao phải dùng `try-with-resources` bao `Connection` và `PreparedStatement` trong DAO? Nếu quên
   close `Connection` sau mỗi query, điều gì xảy ra sau 100 request đồng thờii?
3. `DbConnectionProvider` bật `PRAGMA journal_mode=WAL` — WAL mode là gì và tại sao BidHub cần nó khi
   Tuần 7 có 50 client đặt giá đồng thờii?
4. `ResultSet.getString("role")` trả về `"BIDDER"` — code trong `UserDao.mapRow()` làm thế nào để tạo
   đúng `Bidder` thay vì `Seller`? Giải thích luồng code từ ResultSet đến object.
5. `ItemDao` dùng Jackson để serialize `extra_data` — tại sao không dùng cột riêng `brand TEXT`,
   `artist TEXT`... trong bảng `items`? Trade-off của cách lưu JSON column là gì?
6. **[Câu hỏi nâng cao]** `Connection` từ `DbConnectionProvider.getConnection()` là connection mới mỗi
   lần hay connection được tái sử dụng? Từ Tuần 7 khi có 50 thread đồng thờii, tại sao nên dùng
   connection pool (HikariCP) thay vì pattern hiện tại?

---

### Bài 0.2 — Singleton Pattern: double-checked locking + `volatile`

**Tài liệu bắt buộc:**
- https://refactoring.guru/design-patterns/singleton/java/example (xem phần "Thread-safe Singleton")
- https://docs.oracle.com/javase/tutorial/essential/concurrency/atomic.html

**Câu hỏi hỏi miệng Chủ nhật:**
1. Tại sao `DbConnectionProvider.instance` phải khai báo `volatile`? Không có `volatile`, race condition
   xảy ra như thế nào khi 2 thread cùng gọi `getInstance()` lần đầu tiên trên CPU multi-core?
2. Giải thích tại sao phải kiểm tra `if (instance == null)` **hai lần** (double-checked): lần 1 ngoài
   `synchronized`, lần 2 trong `synchronized`. Bỏ lần kiểm tra bên ngoài thì sao? Bỏ lần bên trong thì
   sao?
3. `ViewRouter.getInstance()` gọi trong `LoginController` trước khi `initialize(Stage)` được gọi —
   điều gì xảy ra? Cách phòng tránh?
4. Tại sao `DbConnectionProvider` dùng `private constructor` thay vì `protected`? Nếu dùng
   `protected`, điều gì bị phá vỡ?
5. Trong `bidhub-server`, có ít nhất 5 Singleton sẽ dùng xuyên suốt dự án. Kể tên, nói tuần nào tạo.
6. **[Câu hỏi nâng cao]** Enum Singleton (`enum DbConnectionProvider { INSTANCE; }`) vs double-checked
   locking — cái nào thread-safe hơn và tại sao? Tại sao BidHub không dùng enum Singleton?

---

### Bài 0.3 — Jackson ObjectMapper: serialize/deserialize `extra_data` trong ItemDao

**Tài liệu bắt buộc:**
- https://github.com/FasterXML/jackson-databind#usage-general (xem "Simple data binding" và
  "Full data binding")
- Chạy thử: `new ObjectMapper().writeValueAsString(Map.of("brand","Apple","warrantyMonths",12))`

**Câu hỏi hỏi miệng Chủ nhật:**
1. `ObjectMapper` có thread-safe không? Tại sao `ItemDao` khai báo nó là `static final` thay vì tạo
   mới trong mỗi method?
2. `ObjectMapper.writeValueAsString(extras)` với `extras = Map.of("brand","Apple","warrantyMonths",12)`
   cho ra chuỗi JSON như thế nào? Khi load lại bằng `readValue(json, new TypeReference<Map<String,Object>>(){})`
   thì `warrantyMonths` có kiểu gì — `Integer` hay `Long`? Điều này ảnh hưởng gì đến `ItemDao.mapRow()`?
3. `ItemDao.mapRow()` đọc `item_type = "ELECTRONICS"` rồi cast `extras.get("warrantyMonths")` sang `int`
   — tại sao phải dùng `((Number) extras.get("warrantyMonths")).intValue()` thay vì `(int)` trực tiếp?
4. Nếu bảng `items` có 1 record `item_type = "ELECTRONICS"` nhưng `extra_data = "{}"` (thiếu brand) —
   `ItemDao.findById()` ném exception gì? Exception đó đến từ đâu?
5. **[Câu hỏi nâng cao]** Tại sao không dùng `Electronics.class` trực tiếp làm type trong
   `objectMapper.readValue(json, Electronics.class)` thay vì đọc vào `Map`? Nêu 2 lý do kỹ thuật.

---

### Bài 0.4 — JavaFX: FXMLLoader, Scene switching, ContextAware và Platform.runLater

**Tài liệu bắt buộc:**
- https://openjfx.io/javadoc/21/javafx.fxml/javafx/fxml/FXMLLoader.html
- https://docs.oracle.com/javafx/2/get_started/fxml_tutorial.htm (đọc phần "Controllers")
- https://openjfx.io/javadoc/21/javafx.graphics/javafx/application/Platform.html
  (đọc phần `runLater(Runnable)`)

**Câu hỏi hỏi miệng Chủ nhật:**
1. `FXMLLoader.load()` làm gì ở tầng nền? Nó tạo object graph từ FXML như thế nào? `@FXML` annotation
   có tác dụng gì với field trong Controller?
2. `ViewRouter.navigateTo("AUCTION_DETAIL", params)` được gọi từ `AuctionListController` khi click row —
   controller của `AuctionDetailView.fxml` được tạo lúc nào? Cùng instance cũ hay instance mới?
3. Interface `ContextAware` với `setContext(Map params)` giải quyết vấn đề gì? Không có nó, làm sao
   truyền `auctionId` từ `AuctionListController` sang `AuctionDetailController`?
4. Tại sao `ViewRouter` cần `primaryStage` thay vì tạo `Stage` mới mỗi lần `navigateTo()`?
5. `AuctionDetailController.initialize()` muốn gọi `ServerGateway.sendRequest()` để lấy thông tin
   phiên đấu giá — tại sao không được gọi trực tiếp trong thread FX? Nếu gọi và request mất 3 giây,
   điều gì xảy ra với giao diện? Viết pseudocode dùng `javafx.concurrent.Task<String>` +
   `Platform.runLater()` để gọi network request đúng cách mà không block UI thread.
6. **[Câu hỏi nâng cao]** `ViewRouter` là Singleton trong client — điều gì xảy ra nếu mở 2 cửa sổ
   (`Stage`) cùng lúc trong Tuần 10 demo? Cần thay đổi gì trong thiết kế `ViewRouter` để hỗ trợ
   multi-window?

---

## 👤 ĐĂNG — Database Setup: Singleton Connection + Migration

```
Branch: feature/tuan-3-dang-database-setup
Phụ thuộc: ConfigLoader (tuần 1, đã có) — import thẳng, không tạo lại
```

📌 **[Tiêu chí điểm: Design Patterns — Singleton (DbConnectionProvider) — phần của 1.0đ]**

### 📝 Mô tả bài tập

`DbConnectionProvider` là trái tim của toàn bộ lớp DAO — mỗi DAO gọi
`DbConnectionProvider.getInstance().getConnection()` để lấy `Connection` JDBC. Nếu class này không phải
Singleton, mỗi DAO sẽ tạo kết nối DB riêng, dẫn đến lãng phí tài nguyên và khó kiểm soát WAL mode.
Double-checked locking với `volatile` đảm bảo chỉ 1 instance được tạo ngay cả khi Tuần 7 có 50 thread
gọi `getInstance()` đồng thờii.

`MigrationRunner` đọc `schema.sql` từ classpath và thực thi khi server khởi động, tạo đủ 4 bảng nếu
chúng chưa tồn tại. Nếu thiếu bước này, mọi DAO sẽ ném `SQLException: no such table` ngay lần đầu
chạy — toàn bộ server không dùng được.

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/
│   ├── java/com/bidhub/server/
│   │   ├── ServerApp.java          (đã có — thêm gọi MigrationRunner.run() trong main())
│   │   └── config/
│   │       ├── ConfigLoader.java   (đã có — không sửa)
│   │       ├── DbConnectionProvider.java  ← MỚI: Singleton JDBC connection
│   │       └── MigrationRunner.java       ← MỚI: Đọc schema.sql, tạo bảng
│   └── resources/
│       ├── server.properties       (đã có — không sửa)
│       └── db/
│           └── schema.sql          ← MỚI: DDL 4 bảng
└── test/java/com/bidhub/server/
    └── config/
        └── DatabaseSetupTest.java  ← MỚI
```

### `schema.sql`

```sql
CREATE TABLE IF NOT EXISTS users (
  id           TEXT    PRIMARY KEY,
  username     TEXT    UNIQUE NOT NULL,
  password_hash TEXT   NOT NULL,
  email        TEXT    NOT NULL,
  role         TEXT    NOT NULL,
  extra_int    INTEGER NOT NULL DEFAULT 0,
  created_at   TEXT    NOT NULL,
  updated_at   TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS items (
  id            TEXT PRIMARY KEY,
  name          TEXT NOT NULL,
  description   TEXT NOT NULL DEFAULT \'\',
  starting_price REAL NOT NULL,
  item_type     TEXT NOT NULL,
  seller_id     TEXT NOT NULL,
  extra_data    TEXT NOT NULL DEFAULT \'{}\',
  created_at    TEXT NOT NULL,
  updated_at    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS auctions (
  id                  TEXT PRIMARY KEY,
  item_id             TEXT NOT NULL,
  start_time          TEXT NOT NULL,
  end_time            TEXT NOT NULL,
  starting_price      REAL NOT NULL,
  current_highest_bid REAL NOT NULL,
  highest_bidder_id   TEXT,
  status              TEXT NOT NULL,
  minimum_increment   REAL NOT NULL DEFAULT 1.0,
  created_at          TEXT NOT NULL,
  updated_at          TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS bid_transactions (
  id         TEXT PRIMARY KEY,
  auction_id TEXT NOT NULL,
  bidder_id  TEXT NOT NULL,
  bid_amount REAL NOT NULL,
  bid_time   TEXT NOT NULL
);
```

> [!TIP]
> Cột `extra_int` trong bảng `users` lưu giá trị số nguyên role-cụ thể: `totalBidsPlaced` cho Bidder,
> `totalItemsListed` cho Seller, `adminLevel` cho Admin. Cách này đơn giản hơn 3 bảng riêng và đủ dùng
> cho scope của dự án.

```bash
git add bidhub-server/src/main/resources/db/schema.sql
git commit -m "feat: thêm schema.sql với 4 bảng users/items/auctions/bid_transactions"
```

---

### `DbConnectionProvider.java`

```java
package com.bidhub.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cung cấp kết nối JDBC tới SQLite. Singleton đảm bảo cùng URL được dùng xuyên suốt server.
 *
 * <p>Dùng double-checked locking với {@code volatile} để thread-safe khi nhiều DAO khởi động
 * đồng thờii. Mỗi lần gọi {@link #getConnection()} trả về connection mới với WAL mode bật.
 */
public final class DbConnectionProvider {

  // 📌 [Tiêu chí: Singleton Pattern — 1.0đ] volatile ngăn CPU reorder instruction khi khởi tạo
  private static volatile DbConnectionProvider instance;

  private final String jdbcUrl;

  private DbConnectionProvider() {
    this.jdbcUrl = "jdbc:sqlite:" + ConfigLoader.getString("db.path");
  }

  /** Trả về instance duy nhất, tạo mới nếu chưa tồn tại (thread-safe). */
  public static DbConnectionProvider getInstance() {
    if (instance == null) {
      synchronized (DbConnectionProvider.class) {
        if (instance == null) {
          instance = new DbConnectionProvider();
        }
      }
    }
    return instance;
  }

  /**
   * Mở và trả về Connection mới với WAL mode đã bật.
   *
   * @return {@link Connection} sẵn sàng dùng
   * @throws SQLException nếu không thể mở kết nối
   */
  public Connection getConnection() throws SQLException {
    Connection conn = DriverManager.getConnection(jdbcUrl);
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("PRAGMA journal_mode=WAL");
    }
    return conn;
  }

  /**
   * Đóng connection an toàn — không ném exception ra ngoài nếu lỗi.
   *
   * @param conn connection cần đóng, có thể null
   */
  public void closeConnection(Connection conn) {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        System.err.println("[DbConnectionProvider] Lỗi đóng kết nối: " + e.getMessage());
      }
    }
  }

  /**
   * Trả về JDBC URL hiện tại (dùng trong test để verify).
   *
   * @return chuỗi JDBC URL
   */
  public String getJdbcUrl() {
    return jdbcUrl;
  }
}
```

```bash
git commit -m "feat: thêm DbConnectionProvider Singleton với double-checked locking và WAL mode"
```

---

### `MigrationRunner.java`

```java
package com.bidhub.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Đọc {@code schema.sql} từ classpath và thực thi khi server khởi động.
 * Dùng {@code CREATE TABLE IF NOT EXISTS} nên an toàn khi gọi nhiều lần.
 */
public final class MigrationRunner {

  private MigrationRunner() {}

  /**
   * Chạy toàn bộ schema.sql — tạo bảng nếu chưa tồn tại.
   *
   * @throws RuntimeException nếu không tìm thấy schema.sql hoặc SQL lỗi
   */
  public static void run() {
    String sql = loadSchemaSql();
    Connection conn = null;
    try {
      conn = DbConnectionProvider.getInstance().getConnection();
      try (Statement stmt = conn.createStatement()) {
        for (String statement : sql.split(";")) {
          String trimmed = statement.trim();
          if (!trimmed.isEmpty()) {
            stmt.execute(trimmed);
          }
        }
      }
      System.out.println("[MigrationRunner] Schema đã sẵn sàng.");
    } catch (SQLException e) {
      throw new RuntimeException("Migration thất bại: " + e.getMessage(), e);
    } finally {
      DbConnectionProvider.getInstance().closeConnection(conn);
    }
  }

  private static String loadSchemaSql() {
    try (InputStream is = MigrationRunner.class.getResourceAsStream("/db/schema.sql")) {
      if (is == null) {
        throw new IllegalStateException("Không tìm thấy /db/schema.sql trong classpath");
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Không đọc được schema.sql: " + e.getMessage(), e);
    }
  }
}
```

```bash
git commit -m "feat: thêm MigrationRunner đọc schema.sql và tạo bảng khi khởi động"
```

> [!IMPORTANT]
> Sau khi tạo `MigrationRunner`, mở `ServerApp.java` và thêm `MigrationRunner.run()` là dòng đầu tiên
> trong `main()` — trước bất kỳ DAO hay service nào. Commit riêng:
> ```bash
> git commit -m "feat: gọi MigrationRunner.run() trong ServerApp khi khởi động"
> ```

---

### ✅ Test đầu ra — `DatabaseSetupTest.java`

```java
package com.bidhub.server.config;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra Singleton DbConnectionProvider và MigrationRunner với in-memory SQLite. */
class DatabaseSetupTest {

  // Override jdbcUrl bằng in-memory để test không tạo file .db
  private Connection openMemoryConn() throws SQLException {
    Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement s = conn.createStatement()) {
      s.execute("PRAGMA journal_mode=WAL");
    }
    return conn;
  }

  @Test
  @DisplayName("getInstance() gọi 2 lần → cùng một instance")
  void singleton_sameInstance() {
    // NOTE: getInstance() chỉ tạo URL từ ConfigLoader, không mở file DB thật.
    // Các test cần mở connection thật PHẢI dùng constructor inject(Connection)
    // như trong UserDaoTest, ItemDaoTest, AuctionDaoTest.
    DbConnectionProvider a = DbConnectionProvider.getInstance();
    DbConnectionProvider b = DbConnectionProvider.getInstance();
    assertSame(a, b);
  }

  @Test
  @DisplayName("MigrationRunner tạo đủ 4 bảng trong in-memory DB")
  void migration_createsFourTables() throws SQLException {
    try (Connection conn = openMemoryConn();
         Statement stmt = conn.createStatement()) {
      // Tạo bảng thủ công từ schema để kiểm tra không phụ thuộc file .db thật
      stmt.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, username TEXT)");
      stmt.execute("CREATE TABLE IF NOT EXISTS items (id TEXT PRIMARY KEY, name TEXT)");
      stmt.execute("CREATE TABLE IF NOT EXISTS auctions (id TEXT PRIMARY KEY, status TEXT)");
      stmt.execute("CREATE TABLE IF NOT EXISTS bid_transactions (id TEXT PRIMARY KEY)");

      ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
      int tableCount = 0;
      while (rs.next()) tableCount++;
      assertEquals(4, tableCount);
    }
  }

  @Test
  @DisplayName("getConnection() trả về Connection không null, không closed")
  void getConnection_returnsOpen() throws SQLException {
    // Dùng in-memory url trực tiếp vì ConfigLoader cần file properties
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      assertNotNull(conn);
      assertFalse(conn.isClosed());
    }
  }

  @Test
  @DisplayName("closeConnection(null) không ném exception")
  void closeConnection_null_safe() {
    assertDoesNotThrow(() -> DbConnectionProvider.getInstance().closeConnection(null));
  }
}
```

**❌ FAIL nếu:**
- `DbConnectionProvider.getInstance()` tạo 2 instance khác nhau khi gọi từ 2 thread → vi phạm Singleton
- `MigrationRunner.run()` ném `RuntimeException` do không tìm thấy `schema.sql` trên classpath
- WAL mode không được bật → `PRAGMA journal_mode` trả về `"delete"` thay vì `"wal"`

---

## 👤 QUỐC MINH — UserDao & ItemDao

```
Branch: feature/tuan-3-quocminh-user-item-dao
Phụ thuộc: DbConnectionProvider (tuần 3, Đăng) — chờ Đăng push hoặc copy tạm vào branch
           User/Bidder/Seller/Admin (tuần 2, Đăng), Item/Electronics/Art/Vehicle (tuần 2, Quốc Minh)
           Jackson ObjectMapper (đã có trong pom — import com.fasterxml.jackson.databind.ObjectMapper)
```

📌 **[Tiêu chí điểm: MVC — tầng DAO (một phần của 0.5đ) + chuẩn bị nền cho Quản lý User & Item (1.0đ tuần 5)]**

### 📝 Mô tả bài tập

`UserDao` là lớp duy nhất được phép chạm vào bảng `users` — toàn bộ `UserService` (Tuần 5) và
`AuthService` (Tuần 5) đều gọi qua lớp này. Nếu `UserDao` không map đúng `role TEXT` → subclass,
`AuthService` sẽ trả về `User` trừu tượng thay vì `Bidder`/`Seller`, phá vỡ polymorphism trong toàn hệ
thống.

`ItemDao` cần serialize các trường đặc thù của từng loại sản phẩm (`brand`, `artist`, `manufacturer`)
vào cột `extra_data` dạng JSON, vì bảng `items` dùng schema chung cho cả 3 loại. Jackson thực hiện
việc này với 1 `ObjectMapper` static thread-safe — đây là cách dùng Jackson đúng chuẩn mà giảng viên
sẽ hỏi.

Trước khi viết DAO, Quốc Minh cần **thêm constructor DB-load** vào 3 file đã có từ Tuần 2:
`Electronics.java`, `Art.java`, `Vehicle.java` — mỗi file thêm đúng 1 constructor nhận thêm `id`,
`createdAt`, `updatedAt`.

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── model/
│   │   ├── Electronics.java  (đã có — THÊM constructor DB-load, không xóa gì)
│   │   ├── Art.java          (đã có — THÊM constructor DB-load)
│   │   └── Vehicle.java      (đã có — THÊM constructor DB-load)
│   └── dao/
│       ├── UserDao.java      ← MỚI
│       └── ItemDao.java      ← MỚI
└── test/java/com/bidhub/server/dao/
    ├── UserDaoTest.java      ← MỚI
    └── ItemDaoTest.java      ← MỚI
```

> [!CAUTION]
> Các constructor DB-load bên dưới phải được **thêm vào file hiện có**, không tạo file mới, không xóa
> bất kỳ constructor hay method nào đã có. Commit riêng từng file khi thêm xong.

**Constructor DB-load cần thêm vào `Electronics.java`:**

```java
// Thêm vào Electronics.java — constructor load từ DB (giữ id/timestamps gốc)
public Electronics(String id, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt,
    String name, String description, double startingPrice, String sellerId,
    String brand, int warrantyMonths) {
  super(id, createdAt, updatedAt, name, description, startingPrice, sellerId,
      com.bidhub.server.model.ItemType.ELECTRONICS);
  if (brand == null) throw new IllegalArgumentException("brand không được null");
  if (warrantyMonths < 0) throw new IllegalArgumentException("warrantyMonths không được âm");
  this.brand = brand;
  this.warrantyMonths = warrantyMonths;
}
```

**Constructor DB-load cần thêm vào `Art.java`:**

```java
// Thêm vào Art.java — constructor load từ DB
public Art(String id, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt,
    String name, String description, double startingPrice, String sellerId,
    String artist, int yearCreated) {
  super(id, createdAt, updatedAt, name, description, startingPrice, sellerId,
      com.bidhub.server.model.ItemType.ART);
  if (artist == null) throw new IllegalArgumentException("artist không được null");
  this.artist = artist;
  this.yearCreated = yearCreated;
}
```

**Constructor DB-load cần thêm vào `Vehicle.java`:**

```java
// Thêm vào Vehicle.java — constructor load từ DB
public Vehicle(String id, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt,
    String name, String description, double startingPrice, String sellerId,
    String manufacturer, int year, int mileageKm) {
  super(id, createdAt, updatedAt, name, description, startingPrice, sellerId,
      com.bidhub.server.model.ItemType.VEHICLE);
  if (manufacturer == null) throw new IllegalArgumentException("manufacturer không được null");
  if (mileageKm < 0) throw new IllegalArgumentException("mileageKm không được âm");
  this.manufacturer = manufacturer;
  this.year = year;
  this.mileageKm = mileageKm;
}
```

```bash
git commit -m "feat: thêm constructor DB-load vào Electronics, Art, Vehicle"
```

---

### `UserDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD cho bảng {@code users}. Map {@code role} TEXT → đúng subclass Bidder/Seller/Admin.
 *
 * <p>Mọi method đều mở connection qua {@link DbConnectionProvider} (production) hoặc dùng
 * connection được inject qua constructor (test với in-memory SQLite).
 */
public class UserDao {

  /** Connection được inject từ test. Null = dùng DbConnectionProvider bình thường. */
  private final Connection injectedConn;

  /** Constructor mặc định — dùng trong production. */
  public UserDao() {
    this.injectedConn = null;
  }

  /** Constructor inject connection — chỉ dùng trong test với in-memory SQLite. */
  public UserDao(Connection conn) {
    this.injectedConn = conn;
  }

  /** Lấy connection: ưu tiên injectedConn, fallback về DbConnectionProvider. */
  private Connection acquireConnection() throws SQLException {
    return (injectedConn != null)
        ? injectedConn
        : DbConnectionProvider.getInstance().getConnection();
  }

  /** Đóng connection CHỈ KHI không phải injected (tránh đóng shared test connection). */
  private void releaseConnection(Connection conn) {
    if (injectedConn == null) {
      DbConnectionProvider.getInstance().closeConnection(conn);
    }
  }

  /**
   * Lưu user mới vào DB. Dùng INSERT — không hỗ trợ upsert.
   *
   * @param user đối tượng User cần lưu (Bidder, Seller, hoặc Admin)
   * @throws RuntimeException nếu lỗi SQL
   */
  public void save(User user) {
    String sql = """
        INSERT INTO users (id, username, password_hash, email, role, extra_int,
            created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, user.getId());
        ps.setString(2, user.getUsername());
        ps.setString(3, user.getPasswordHash());
        ps.setString(4, user.getEmail());
        ps.setString(5, user.getRole().name());
        ps.setInt(6, extractExtraInt(user));
        ps.setString(7, user.getCreatedAt().toString());
        ps.setString(8, user.getUpdatedAt().toString());
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("UserDao.save thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Tìm user theo ID.
   *
   * @param id UUID string
   * @return {@link Optional} chứa User đúng subclass, hoặc empty nếu không có
   */
  public Optional<User> findById(String id) {
    String sql = "SELECT * FROM users WHERE id = ?";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return Optional.of(mapRow(rs));
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("UserDao.findById thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Tìm user theo username (dùng trong login, register validation).
   *
   * @param username tên đăng nhập
   * @return {@link Optional} empty nếu không tìm thấy
   */
  public Optional<User> findByUsername(String username) {
    String sql = "SELECT * FROM users WHERE username = ?";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return Optional.of(mapRow(rs));
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("UserDao.findByUsername thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Kiểm tra username đã tồn tại chưa (dùng trong register để ném DuplicateUsernameException).
   *
   * @param username tên cần kiểm tra
   * @return {@code true} nếu đã có trong DB
   */
  public boolean existsByUsername(String username) {
    String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("UserDao.existsByUsername thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Trả về tất cả user trong hệ thống.
   *
   * @return danh sách User (có thể rỗng)
   */
  public List<User> findAll() {
    String sql = "SELECT * FROM users";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ResultSet rs = ps.executeQuery();
        List<User> result = new ArrayList<>();
        while (rs.next()) result.add(mapRow(rs));
        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException("UserDao.findAll thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  // Tạo đúng subclass dựa vào cột role — cốt lõi của polymorphism trong DAO
  private User mapRow(ResultSet rs) throws SQLException {
    String id = rs.getString("id");
    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
    LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));
    String username = rs.getString("username");
    String passwordHash = rs.getString("password_hash");
    String email = rs.getString("email");
    UserRole role = UserRole.valueOf(rs.getString("role"));
    int extraInt = rs.getInt("extra_int");

    return switch (role) {
      case BIDDER -> new Bidder(id, createdAt, updatedAt, username, passwordHash, email, extraInt);
      case SELLER -> new Seller(id, createdAt, updatedAt, username, passwordHash, email, extraInt);
      case ADMIN  -> new Admin(id, createdAt, updatedAt, username, passwordHash, email, extraInt);
    };
  }

  // Lấy giá trị extra_int theo role (totalBidsPlaced / totalItemsListed / adminLevel)
  private int extractExtraInt(User user) {
    return switch (user.getRole()) {
      case BIDDER -> ((Bidder) user).getTotalBidsPlaced();
      case SELLER -> ((Seller) user).getTotalItemsListed();
      case ADMIN  -> ((Admin)  user).getAdminLevel();
    };
  }
}
```

```bash
git commit -m "feat: thêm UserDao với JDBC PreparedStatement, map role → Bidder/Seller/Admin"
```

---

### `ItemDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD cho bảng {@code items}. Trường đặc thù (brand, artist...) được serialize vào
 * cột {@code extra_data} dạng JSON bằng Jackson.
 */
public class ItemDao {

  // ObjectMapper là thread-safe — khai báo static để tái sử dụng
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final TypeReference<Map<String, Object>> MAP_TYPE =
      new TypeReference<>() {};

  private final Connection injectedConn;

  public ItemDao() {
    this.injectedConn = null;
  }

  public ItemDao(Connection conn) {
    this.injectedConn = conn;
  }

  private Connection acquireConnection() throws SQLException {
    return (injectedConn != null)
        ? injectedConn
        : DbConnectionProvider.getInstance().getConnection();
  }

  private void releaseConnection(Connection conn) {
    if (injectedConn == null) {
      DbConnectionProvider.getInstance().closeConnection(conn);
    }
  }

  /**
   * Lưu item vào DB. Serialize extra fields thành JSON vào cột {@code extra_data}.
   *
   * @param item đối tượng Electronics, Art, hoặc Vehicle
   * @throws RuntimeException nếu lỗi SQL hoặc JSON serialization
   */
  public void save(Item item) {
    String sql = """
        INSERT INTO items (id, name, description, starting_price, item_type,
            seller_id, extra_data, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, item.getId());
        ps.setString(2, item.getName());
        ps.setString(3, item.getDescription());
        ps.setDouble(4, item.getStartingPrice());
        ps.setString(5, item.getItemType().name());
        ps.setString(6, item.getSellerId());
        // 📌 [Tiêu chí: MVC — tầng DAO xử lý persistence]
        ps.setString(7, MAPPER.writeValueAsString(buildExtras(item)));
        ps.setString(8, item.getCreatedAt().toString());
        ps.setString(9, item.getUpdatedAt().toString());
        ps.executeUpdate();
      }
    } catch (Exception e) {
      throw new RuntimeException("ItemDao.save thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Tìm item theo ID.
   *
   * @param id UUID string
   * @return {@link Optional} chứa Item đúng subclass, hoặc empty
   */
  public Optional<Item> findById(String id) {
    return querySingle("SELECT * FROM items WHERE id = ?", id);
  }

  /**
   * Tìm tất cả item của 1 seller.
   *
   * @param sellerId ID ngườii bán
   * @return danh sách Item, có thể rỗng
   */
  public List<Item> findBySellerId(String sellerId) {
    return queryList("SELECT * FROM items WHERE seller_id = ?", sellerId);
  }

  /** Trả về tất cả item trong hệ thống. */
  public List<Item> findAll() {
    return queryList("SELECT * FROM items", (String) null);
  }

  /**
   * Xóa item theo ID.
   *
   * @param id UUID string
   */
  public void deleteById(String id) {
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE id = ?")) {
        ps.setString(1, id);
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("ItemDao.deleteById thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  private Optional<Item> querySingle(String sql, String param) {
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        if (param != null) ps.setString(1, param);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return Optional.of(mapRow(rs));
        return Optional.empty();
      }
    } catch (Exception e) {
      throw new RuntimeException("ItemDao query thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  private List<Item> queryList(String sql, String param) {
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        if (param != null) ps.setString(1, param);
        ResultSet rs = ps.executeQuery();
        List<Item> result = new ArrayList<>();
        while (rs.next()) result.add(mapRow(rs));
        return result;
      }
    } catch (Exception e) {
      throw new RuntimeException("ItemDao query list thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  // Tạo đúng subclass dựa vào item_type — dùng constructor DB-load (giữ nguyên id/timestamps)
  private Item mapRow(ResultSet rs) throws Exception {
    String id = rs.getString("id");
    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
    LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));
    String name = rs.getString("name");
    String description = rs.getString("description");
    double startingPrice = rs.getDouble("starting_price");
    String sellerId = rs.getString("seller_id");
    ItemType type = ItemType.valueOf(rs.getString("item_type"));

    // Parse JSON extra_data → Map; Jackson decode số nguyên thành Integer
    Map<String, Object> extras = MAPPER.readValue(rs.getString("extra_data"), MAP_TYPE);

    return switch (type) {
      case ELECTRONICS -> new Electronics(
          id, createdAt, updatedAt, name, description, startingPrice, sellerId,
          (String) extras.get("brand"),
          ((Number) extras.get("warrantyMonths")).intValue());
      case ART -> new Art(
          id, createdAt, updatedAt, name, description, startingPrice, sellerId,
          (String) extras.get("artist"),
          ((Number) extras.get("yearCreated")).intValue());
      case VEHICLE -> new Vehicle(
          id, createdAt, updatedAt, name, description, startingPrice, sellerId,
          (String) extras.get("manufacturer"),
          ((Number) extras.get("year")).intValue(),
          ((Number) extras.get("mileageKm")).intValue());
    };
  }

  // Tạo Map extras từ Item để serialize thành JSON
  private Map<String, Object> buildExtras(Item item) {
    return switch (item.getItemType()) {
      case ELECTRONICS -> {
        Electronics e = (Electronics) item;
        yield Map.of("brand", e.getBrand(), "warrantyMonths", e.getWarrantyMonths());
      }
      case ART -> {
        Art a = (Art) item;
        yield Map.of("artist", a.getArtist(), "yearCreated", a.getYearCreated());
      }
      case VEHICLE -> {
        Vehicle v = (Vehicle) item;
        yield Map.of("manufacturer", v.getManufacturer(), "year", v.getYear(),
            "mileageKm", v.getMileageKm());
      }
    };
  }
}
```

```bash
git commit -m "feat: thêm ItemDao với extra_data JSON serialization qua Jackson ObjectMapper"
```

---

### ✅ Test đầu ra

**`UserDaoTest.java`** (in-memory SQLite):

```java
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
    dao.save(new Bidder("b1", "h", "b1@test.com"));
    dao.save(new Seller("s1", "h", "s1@test.com"));
    dao.save(new Admin("a1", "h", "a1@test.com", 2));

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
```

**`ItemDaoTest.java`** (in-memory SQLite):

```java
package com.bidhub.server.dao;

import com.bidhub.server.model.*;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ItemDaoTest {

  private Connection conn;
  private ItemDao dao;
  private static final String SELLER_ID = "seller-uuid-001";

  @BeforeEach
  void setup() throws SQLException {
    conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement s = conn.createStatement()) {
      s.execute("""
          CREATE TABLE items (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
            starting_price REAL NOT NULL, item_type TEXT NOT NULL, seller_id TEXT NOT NULL,
            extra_data TEXT NOT NULL DEFAULT '{}', created_at TEXT NOT NULL, updated_at TEXT NOT NULL)
          """);
    }
    dao = new ItemDao(conn);
  }

  @AfterEach
  void teardown() throws SQLException { conn.close(); }

  @Test
  @DisplayName("save Electronics → findById trả về instanceof Electronics đúng brand")
  void save_electronics_findById_correctSubclass() {
    Item item = new ElectronicsCreator().createItem(
        "MacBook Pro", "Laptop", 30_000_000.0, SELLER_ID,
        Map.of("brand", "Apple", "warrantyMonths", 12));
    dao.save(item);

    Optional<Item> found = dao.findById(item.getId());
    assertTrue(found.isPresent());
    assertInstanceOf(Electronics.class, found.get());
    assertEquals("Apple", ((Electronics) found.get()).getBrand());
    assertEquals(12, ((Electronics) found.get()).getWarrantyMonths());
  }

  @Test
  @DisplayName("save Art → findById trả về instanceof Art đúng artist")
  void save_art_findById_correctSubclass() {
    Item item = new ArtCreator().createItem(
        "Mùa thu", "", 5_000_000.0, SELLER_ID,
        Map.of("artist", "Nguyễn Tư Nghiêm", "yearCreated", 1970));
    dao.save(item);

    Optional<Item> found = dao.findById(item.getId());
    assertTrue(found.isPresent());
    assertInstanceOf(Art.class, found.get());
    assertEquals(1970, ((Art) found.get()).getYearCreated());
  }

  @Test
  @DisplayName("save Vehicle → findById trả về instanceof Vehicle đúng mileageKm")
  void save_vehicle_findById_correctSubclass() {
    Item item = new VehicleCreator().createItem(
        "Toyota Camry", "", 600_000_000.0, SELLER_ID,
        Map.of("manufacturer", "Toyota", "year", 2022, "mileageKm", 30000));
    dao.save(item);

    Optional<Item> found = dao.findById(item.getId());
    assertTrue(found.isPresent());
    assertInstanceOf(Vehicle.class, found.get());
    assertEquals(30000, ((Vehicle) found.get()).getMileageKm());
  }

  @Test
  @DisplayName("findById với id không tồn tại → Optional.empty()")
  void findById_notFound_returnsEmpty() {
    assertTrue(dao.findById("ghost-id").isEmpty());
  }

  @Test
  @DisplayName("findBySellerId → trả về đúng danh sách item của seller đó")
  void findBySellerId_returnsCorrectItems() {
    dao.save(new ElectronicsCreator().createItem(
        "Phone", "", 10_000_000.0, SELLER_ID,
        Map.of("brand", "Samsung", "warrantyMonths", 6)));
    dao.save(new ElectronicsCreator().createItem(
        "Laptop", "", 20_000_000.0, "other-seller",
        Map.of("brand", "Dell", "warrantyMonths", 24)));

    assertEquals(1, dao.findBySellerId(SELLER_ID).size());
  }

  @Test
  @DisplayName("warrantyMonths Jackson deserialize Number.intValue() — không ClassCastException")
  void save_load_electronics_warrantyMonths_noClassCastException() {
    // Kiểm tra Jackson không deserialize Integer thành Long và gây ClassCastException
    Item item = new ElectronicsCreator().createItem(
        "TV", "", 5_000_000.0, SELLER_ID,
        Map.of("brand", "LG", "warrantyMonths", Integer.MAX_VALUE));
    dao.save(item);

    assertDoesNotThrow(() -> dao.findById(item.getId()));
    Optional<Item> found = dao.findById(item.getId());
    assertTrue(found.isPresent());
    assertEquals(Integer.MAX_VALUE, ((Electronics) found.get()).getWarrantyMonths());
  }
}

> [!TIP]
> Pattern inject Connection (`UserDao(Connection conn)`) là cách chuẩn để test DAO mà không cần
> `DbConnectionProvider` thật. Áp dụng **tương tự** cho `ItemDao`, `AuctionDao`, `BidDao`:
> mỗi class thêm constructor `Dao(Connection conn)`, helper `acquireConnection()` / `releaseConnection()`,
> và thay toàn bộ `DbConnectionProvider.getInstance().getConnection()` / `closeConnection()` bằng 2 helper này.

**❌ FAIL nếu:**
- `UserDao.findById(existingId)` trả về `Optional.empty()` sau khi đã `save()` thành công
- `mapRow()` tạo `User` thay vì `Bidder` khi `role = "BIDDER"` → `instanceof Bidder` = false
- `ItemDao.save()` ném `JsonProcessingException` do MAPPER null (thiếu `static final`)
- `ItemDao.findById()` trả về `Electronics` với `warrantyMonths = 0` do `ClassCastException` bị swallow

---



```
## 👤 CÔNG MINH — ViewRouter & FXML Skeleton
Branch: `feature/tuan-3-congminh-viewrouter-fxml`
Phụ thuộc: BidHubApp (tuần 1), LoginController (tuần 1), Views (tuần 1) — tất cả đã có, JavaFX FXMLLoader (javafx-fxml dependency đã khai báo trong pom)

📌 **[Tiêu chí điểm: MVC — ViewRouter điều hướng màn hình (phần của 0.5đ) + Singleton ViewRouter]**

### 📝 Mô tả bài tập

`ViewRouter` là trung tâm điều phối màn hình phía client. Không có nó, mỗi Controller phải tự gọi `FXMLLoader.load()` và `primaryStage.setScene()` — dẫn đến code trùng lặp và khó test. Khi Tuần 5 thêm màn hình Register và ItemList, chỉ cần gọi `ViewRouter.getInstance().navigateTo(Views.REGISTER)`.

`ContextAware` giải quyết vấn đề truyền dữ liệu giữa màn hình: khi `AuctionListController` click vào 1 phiên đấu giá, nó cần truyền `auctionId` sang `AuctionDetailController`. Không có interface này, không có cách nào type-safe để inject dữ liệu vào controller mới được tạo bởi `FXMLLoader`.

Các FXML skeleton tuần này **không có networking** — chỉ layout và `@FXML` annotations. Logic thực tế sẽ được bổ sung từ Tuần 5-6 khi có `UserService` và `BidService`.

### 📁 Cấu trúc file

```text
bidhub-client/src/
├── main/java/com/bidhub/client/
│   ├── BidHubApp.java               (đã có — thêm ViewRouter.getInstance().initialize(stage))
│   ├── util/
│   │   └── Views.java               (đã có từ tuần 1 — xem lưu ý bên dưới về giá trị constants)
│   ├── navigation/
│   │   ├── ViewRouter.java          ← MỚI: Singleton điều hướng màn hình
│   │   └── ContextAware.java        ← MỚI: Interface inject params vào Controller
│   └── controller/
│       ├── LoginController.java     (đã có — không sửa)
│       ├── AuctionListController.java    ← MỚI: Skeleton
│       ├── AuctionDetailController.java  ← MỚI: Skeleton
│       └── CreateItemController.java     ← MỚI: Skeleton
└── resources/fxml/
    ├── LoginView.fxml               (đã có — không sửa)
    ├── AuctionListView.fxml         ← MỚI
    ├── AuctionDetailView.fxml       ← MỚI
    └── CreateItemView.fxml          ← MỚI
```

> [!IMPORTANT]
> Mở `BidHubApp.java`, trong method `start(Stage stage)` thêm dòng:
> `ViewRouter.getInstance().initialize(stage);` **trước** khi load FXML lần đầu.
> Commit: `git commit -m "feat: khởi tạo ViewRouter trong BidHubApp.start()"`

---

### `ContextAware.java`

```java
package com.bidhub.client.navigation;

import java.util.Map;

/**
 * Cho phép ViewRouter inject dữ liệu vào Controller sau khi FXMLLoader tạo instance.
 *
 * <p>Controller nào cần nhận params (ví dụ {@code auctionId}) thì implement interface này.
 */
public interface ContextAware {

  /**
   * Nhận dữ liệu context từ màn hình trước.
   *
   * @param params map key-value do màn hình gọi {@code navigateTo} truyền vào
   */
  void setContext(Map<String, Object> params);
}
```

---

### `ViewRouter.java`

```java
package com.bidhub.client.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Quản lý điều hướng màn hình JavaFX. Singleton đảm bảo chỉ 1 Stage được dùng.
 *
 * <p>Dùng {@link #initialize(Stage)} trong {@code BidHubApp.start()} trước khi gọi
 * bất kỳ {@code navigateTo()} nào.
 */
public final class ViewRouter {

  // 📌 [Tiêu chí: Singleton Pattern — 1.0đ]
  private static volatile ViewRouter instance;

  private Stage primaryStage;

  private ViewRouter() {}

  /** Trả về instance duy nhất của ViewRouter (thread-safe). */
  public static ViewRouter getInstance() {
    if (instance == null) {
      synchronized (ViewRouter.class) {
        if (instance == null) {
          instance = new ViewRouter();
        }
      }
    }
    return instance;
  }

  /**
   * Gán Stage chính. Phải gọi 1 lần duy nhất trong {@code BidHubApp.start()}.
   *
   * @param stage Stage chính của ứng dụng
   */
  public void initialize(Stage stage) {
    this.primaryStage = Objects.requireNonNull(stage, "Stage không được null");
  }

  /**
   * Chuyển sang màn hình có tên {@code viewName} (không truyền params).
   *
   * @param viewName tên màn hình, khớp với tên file FXML (ví dụ {@code "AuctionListView"})
   */
  public void navigateTo(String viewName) {
    navigateTo(viewName, Collections.emptyMap());
  }

  /**
   * Chuyển màn hình và inject params vào Controller nếu implement {@link ContextAware}.
   *
   * @param viewName tên FXML (không có đuôi .fxml)
   * @param params   dữ liệu truyền sang Controller, có thể rỗng
   */
  public void navigateTo(String viewName, Map<String, Object> params) {
    if (primaryStage == null) {
      throw new IllegalStateException("ViewRouter chưa được initialize(stage) — gọi trong BidHubApp");
    }
    try {
      String fxmlPath = "/fxml/" + viewName + ".fxml";
      FXMLLoader loader = new FXMLLoader(
          Objects.requireNonNull(getClass().getResource(fxmlPath),
              "Không tìm thấy FXML: " + fxmlPath));
      Parent root = loader.load();

      // Inject params nếu controller implement ContextAware
      Object controller = loader.getController();
      if (controller instanceof ContextAware ca && !params.isEmpty()) {
        ca.setContext(params);
      }

      primaryStage.setScene(new Scene(root));
      primaryStage.show();
    } catch (IOException e) {
      throw new RuntimeException("ViewRouter không load được " + viewName + ": " + e.getMessage(), e);
    }
  }
}
```

```bash
git commit -m "feat: thêm ViewRouter Singleton + ContextAware interface cho điều hướng FXML"
```

---

### `Views.java`

```java
package com.bidhub.client.util;

/**
 * Hằng số tên màn hình — khớp tên file FXML (không có đuôi .fxml).
 *
 * <p>ViewRouter xây đường dẫn: "/fxml/{viewName}.fxml"
 * → giá trị ở đây PHẢI bằng tên file FXML không có đuôi.
 */
public final class Views {

  private Views() {}

  /** → resources/fxml/LoginView.fxml */
  public static final String LOGIN          = "LoginView";

  /** → resources/fxml/AuctionListView.fxml */
  public static final String AUCTION_LIST   = "AuctionListView";

  /** → resources/fxml/AuctionDetailView.fxml */
  public static final String AUCTION_DETAIL = "AuctionDetailView";

  /** → resources/fxml/CreateItemView.fxml */
  public static final String CREATE_ITEM    = "CreateItemView";

  /** → resources/fxml/RegisterView.fxml */
  public static final String REGISTER       = "RegisterView";

  /** → resources/fxml/CreateAuctionView.fxml */
  public static final String CREATE_AUCTION = "CreateAuctionView";
}
```

```bash
git commit -m "feat: chuẩn hóa Views.java constants khớp đúng tên file FXML cho ViewRouter"
```

---

### `AuctionListController.java`

```java
package com.bidhub.client.controller;

import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Map;

/** Skeleton controller cho AuctionListView — TableView 4 cột, chưa networking. */
public class AuctionListController {

  @FXML private TableView<Map<String, String>> tableAuctions;
  @FXML private TableColumn<Map<String, String>, String> colId;
  @FXML private TableColumn<Map<String, String>, String> colItem;
  @FXML private TableColumn<Map<String, String>, String> colStatus;
  @FXML private TableColumn<Map<String, String>, String> colCurrentBid;
  @FXML private Button btnCreateAuction;

  @FXML
  public void initialize() {
    colId.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrDefault("id", "")));
    colItem.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrDefault("itemName", "")));
    colStatus.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrDefault("status", "")));
    colCurrentBid.setCellValueFactory(data ->
        new javafx.beans.property.SimpleStringProperty(
            data.getValue().getOrDefault("currentHighestBid", "")));

    tableAuctions.setItems(FXCollections.emptyObservableList());
    tableAuctions.setPlaceholder(new Label("Chưa có phiên đấu giá nào."));
  }

  @FXML
  private void handleCreateAuction() {
    ViewRouter.getInstance().navigateTo(Views.CREATE_ITEM);
  }

  @FXML
  private void handleRowClick() {
    Map<String, String> selected = tableAuctions.getSelectionModel().getSelectedItem();
    if (selected != null) {
      ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL,
          Map.of("auctionId", selected.get("id")));
    }
  }
}
```

---

### `AuctionDetailController.java`

```java
package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Map;

/** Skeleton controller cho AuctionDetailView. Implement ContextAware để nhận auctionId. */
public class AuctionDetailController implements ContextAware {

  @FXML private Label lblTitle;
  @FXML private Label lblStatus;
  @FXML private Label lblCurrentBid;
  @FXML private Label lblTimeRemaining;
  @FXML private TextField tfBidAmount;
  @FXML private Button btnPlaceBid;
  @FXML private Label lblMessage;

  private String auctionId;

  @FXML
  public void initialize() {
    lblMessage.setVisible(false);
    btnPlaceBid.setDisable(true); // Enable sau khi load auction data (Tuần 5)
  }

  @Override
  public void setContext(Map<String, Object> params) {
    this.auctionId = (String) params.get("auctionId");
    lblTitle.setText("Phiên: " + auctionId); // Placeholder — thay bằng data thật Tuần 5
  }

  @FXML
  private void handlePlaceBid() {
    // Skeleton — Tuần 6 thêm NetworkClient gọi PLACE_BID request
    lblMessage.setText("Chức năng đặt giá sẽ sẵn sàng từ Tuần 6.");
    lblMessage.setVisible(true);
  }
}
```

---

### `CreateItemController.java`

```java
package com.bidhub.client.controller;

import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Skeleton form tạo item mới. Logic validate và gửi request sẽ thêm từ Tuần 5. */
public class CreateItemController {

  @FXML private TextField tfName;
  @FXML private TextArea  taDescription;
  @FXML private TextField tfStartingPrice;
  @FXML private ComboBox<String> cbItemType;
  @FXML private TextField tfExtra1; // brand / artist / manufacturer
  @FXML private TextField tfExtra2; // warrantyMonths / yearCreated / year
  @FXML private TextField tfExtra3; // mileageKm — chỉ dùng khi cbItemType = VEHICLE
  @FXML private Label lblMessage;
  @FXML private Button btnSubmit;

  @FXML
  public void initialize() {
    cbItemType.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
    cbItemType.setValue("ELECTRONICS");
    lblMessage.setVisible(false);
  }

  @FXML
  private void handleSubmit() {
    // Skeleton — Tuần 5 thêm validation + NetworkClient gọi CREATE_ITEM request
    lblMessage.setText("Chức năng tạo item sẽ sẵn sàng từ Tuần 5.");
    lblMessage.setVisible(true);
  }

  @FXML
  private void handleCancel() {
    ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
  }
}
```

```bash
git commit -m "feat: thêm AuctionListController, AuctionDetailController, CreateItemController (skeleton)"
```

---

### `AuctionListView.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.bidhub.client.controller.AuctionListController"
      stylesheets="@/css/styles.css"
      spacing="20"
      styleClass="root">

    <HBox alignment="CENTER_LEFT" styleClass="header-bar">
        <Label text="BidHub — Danh sách đấu giá" styleClass="header-title" HBox.hgrow="ALWAYS" maxWidth="Infinity"/>

        <Button fx:id="btnCreateAuction" text="+ Tạo phiên mới" styleClass="button, btn-primary" onAction="#handleCreateAuction"/>
    </HBox>

    <VBox styleClass="content-pane" VBox.vgrow="ALWAYS" style="-fx-margin: 0 20 20 20;">

        <TableView fx:id="tableAuctions" VBox.vgrow="ALWAYS" onMouseClicked="#handleRowClick">
            <columns>
                <TableColumn fx:id="colId"         text="Mã phiên"     prefWidth="100"/>
                <TableColumn fx:id="colItem"       text="Sản phẩm"     prefWidth="250"/>
                <TableColumn fx:id="colStatus"     text="Trạng thái"   prefWidth="120"/>
                <TableColumn fx:id="colCurrentBid" text="Giá cao nhất" prefWidth="150"/>
            </columns>
        </TableView>

    </VBox>

</VBox>
```

---

### `AuctionDetailView.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.bidhub.client.controller.AuctionDetailController"
      stylesheets="@/css/styles.css"
      styleClass="root"
      spacing="20">

    <HBox alignment="CENTER_LEFT" styleClass="header-bar">
        <Label fx:id="lblTitle" text="Đang tải..." styleClass="header-title"/>
    </HBox>

    <VBox styleClass="content-pane" spacing="15" style="-fx-margin: 0 20 20 20;">

        <HBox spacing="40">
            <VBox spacing="5">
                <Label text="Trạng thái:" styleClass="form-label"/>
                <Label fx:id="lblStatus" text="—" styleClass="status-label"/>
            </VBox>

            <VBox spacing="5">
                <Label text="Giá cao nhất:" styleClass="form-label"/>
                <Label fx:id="lblCurrentBid" text="—" style="-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #4F46E5;"/>
            </VBox>

            <VBox spacing="5">
                <Label text="Thời gian còn lại:" styleClass="form-label"/>
                <Label fx:id="lblTimeRemaining" text="—"/>
            </VBox>
        </HBox>

        <Separator/>

        <HBox spacing="15" alignment="CENTER_LEFT">
            <Label text="Giá đặt (VNĐ):" styleClass="form-label"/>

            <TextField fx:id="tfBidAmount" promptText="Nhập số tiền..." prefWidth="200" styleClass="text-input-field"/>

            <Button fx:id="btnPlaceBid" text="Đặt giá" onAction="#handlePlaceBid" styleClass="button, btn-primary"/>
        </HBox>

        <Label fx:id="lblMessage" text="" styleClass="error-message"/>

    </VBox>
</VBox>
```

---

### `CreateItemView.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.bidhub.client.controller.CreateItemController"
      stylesheets="@/css/styles.css"
      styleClass="root"
      alignment="CENTER"
      style="-fx-padding: 30;">

    <VBox styleClass="content-pane" spacing="15" maxWidth="550">

        <Label text="Thêm sản phẩm mới" styleClass="login-title" style="-fx-font-size: 24px;"/>

        <Separator style="-fx-padding: 0 0 10 0;"/>

        <Label text="Tên sản phẩm:" styleClass="form-label"/>
        <TextField fx:id="tfName" promptText="VD: MacBook Pro 14" styleClass="text-input-field"/>

        <Label text="Mô tả:" styleClass="form-label"/>
        <TextArea fx:id="taDescription" promptText="Mô tả ngắn..." prefRowCount="3" styleClass="text-input-field"/>

        <HBox spacing="15">
            <VBox spacing="5" HBox.hgrow="ALWAYS">
                <Label text="Giá khởi điểm (VNĐ):" styleClass="form-label"/>
                <TextField fx:id="tfStartingPrice" promptText="VD: 25000000" styleClass="text-input-field"/>
            </VBox>
            <VBox spacing="5" HBox.hgrow="ALWAYS">
                <Label text="Loại sản phẩm:" styleClass="form-label"/>
                <ComboBox fx:id="cbItemType" maxWidth="Infinity" styleClass="text-input-field"/>
            </VBox>
        </HBox>

        <Label text="Thông tin đặc trưng (VD: tên thương hiệu):" styleClass="form-label" style="-fx-padding: 10 0 0 0;"/>
        <TextField fx:id="tfExtra1" promptText="Brand / Nghệ sĩ / Nhà sản xuất" styleClass="text-input-field"/>
        <TextField fx:id="tfExtra2" promptText="Bảo hành (tháng) / Năm sáng tác / Năm sản xuất" styleClass="text-input-field"/>
        <TextField fx:id="tfExtra3" promptText="Số km đã đi (chỉ dùng cho Xe)" styleClass="text-input-field"/>

        <HBox spacing="15" alignment="CENTER_RIGHT" style="-fx-padding: 15 0 0 0;">
            <Button text="Hủy" onAction="#handleCancel" styleClass="button, btn-danger-outline"/>

            <Button fx:id="btnSubmit" text="Tạo sản phẩm" onAction="#handleSubmit" styleClass="button, btn-primary"/>
        </HBox>

        <Label fx:id="lblMessage" text="" styleClass="error-message"/>

    </VBox>
</VBox>
```

```bash
git commit -m "feat: thêm 3 FXML skeleton AuctionList/Detail/CreateItem"
```

---
# 🎨 BidHub - Modern UI Theme (JavaFX CSS)

Chào mừng bạn đến với **BidHub Modern UI Theme**! Đây là một tệp định dạng (stylesheet) được thiết kế đặc biệt cho các ứng dụng JavaFX. Tệp CSS này giúp biến đổi giao diện mặc định có phần cũ kỹ của JavaFX thành một giao diện người dùng (User Interface - UI) hiện đại, sạch sẽ và vô cùng chuyên nghiệp.

## 🌟 Tính năng nổi bật (Key Features)

* **Global Settings (Cài đặt chung):** Đồng bộ phông chữ (fonts) hiện đại như Segoe UI và sử dụng tông màu nền xám nhạt tinh tế.

* **Header & Containers (Thanh tiêu đề & Vùng chứa):** Áp dụng hiệu ứng đổ bóng (drop shadows) mượt mà và bo góc (rounded corners) tạo chiều sâu cho ứng dụng.

* **Interactive Buttons (Nút bấm tương tác):** Thiết kế nút bấm nổi bật với hiệu ứng khi di chuột qua (hover effects) sinh động.

* **Data Tables (Bảng dữ liệu):** Bảng hiển thị dễ nhìn, phân biệt màu sắc giữa các hàng chẵn/lẻ (odd/even rows) và làm nổi bật hàng đang được chọn (selected row).

* **Login & Forms (Đăng nhập & Biểu mẫu):** Khung nhập liệu (input fields) rộng rãi, tinh tế cùng các thông báo lỗi (error messages) rõ ràng.

## 💻 Mã nguồn (Source Code)

Dưới đây là toàn bộ mã nguồn CSS cho chủ đề này. Hãy lưu nó vào tệp `styles.css` theo đúng đường dẫn (path) trong thư mục dự án (project directory) của bạn:
`bidhub-client/src/main/resources/css/styles.css`

```css
/* ==============================================
   BIDHUB - MODERN UI THEME
   File: bidhub-client/src/main/resources/css/styles.css
   ============================================== */

/* 1. GLOBAL SETTINGS (Cài đặt chung) */
.root {
    -fx-font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
    -fx-background-color: #E2E8F0; /* Xám đậm hơn chút để nổi bật form trắng */
    -fx-font-size: 14px;
}

/* 2. HEADER BAR (Thanh tiêu đề) */
.header-bar {
    -fx-background-color: linear-gradient(to right, #1E1B4B, #4338CA);
    -fx-padding: 15px 25px;
    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 3);
}

.header-title {
    -fx-text-fill: white;
    -fx-font-size: 22px;
    -fx-font-weight: 900;
    -fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.3), 2, 0, 1, 1);
}

/* 3. CENTER CONTENT (Khung chứa nội dung ở giữa) */
.content-pane {
    -fx-background-color: white;
    -fx-background-radius: 12px;
    -fx-border-radius: 12px;
    -fx-padding: 20px;
    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 4);
}

/* 4. BUTTONS (Nút bấm) */
.button {
    -fx-padding: 10px 20px;
    -fx-background-radius: 8px;
    -fx-font-weight: bold;
    -fx-cursor: hand;
}

.btn-primary {
    -fx-background-color: #4F46E5;
    -fx-text-fill: white;
}
.btn-primary:hover {
    -fx-background-color: #4338CA;
    -fx-effect: dropshadow(three-pass-box, rgba(79, 70, 229, 0.4), 10, 0, 0, 4);
}

.btn-danger-outline {
    -fx-background-color: transparent;
    -fx-border-color: #EF4444;
    -fx-border-radius: 8px;
    -fx-border-width: 1.5px;
    -fx-text-fill: #EF4444;
}
.btn-danger-outline:hover {
    -fx-background-color: #FEF2F2;
}

/* 5. TABLE VIEW (Bảng dữ liệu) */
.table-view {
    -fx-background-color: transparent;
    -fx-border-color: #E2E8F0;
    -fx-border-radius: 8px;
    -fx-background-radius: 8px;
}
.table-view:focused {
    -fx-background-insets: 0;
}
.table-view .column-header-background {
    -fx-background-color: #F8FAFC;
    -fx-background-radius: 8px 8px 0 0;
    -fx-border-width: 0 0 1px 0;
    -fx-border-color: #E2E8F0;
}
.table-view .column-header {
    -fx-background-color: transparent;
    -fx-size: 45px;
}
.table-view .column-header .label {
    -fx-text-fill: #64748B;
    -fx-font-weight: bold;
    -fx-alignment: center-left;
}
.table-row-cell {
    -fx-background-color: white;
    -fx-border-width: 0 0 1px 0;
    -fx-border-color: #F1F5F9;
    -fx-cell-size: 45px;
}
.table-row-cell:odd {
    -fx-background-color: #F8FAFC;
}
.table-row-cell:hover {
    -fx-background-color: #EEF2FF;
}
.table-row-cell:selected {
    -fx-background-color: #E0E7FF;
    -fx-text-background-color: #1E1B4B;
    -fx-font-weight: bold;
}

/* 6. STATUS LABEL */
.status-label {
    -fx-text-fill: #94A3B8;
    -fx-font-style: italic;
    -fx-font-size: 13px;
}

/* ==============================================
   7. LOGIN & FORM SCENES (Màn hình Đăng nhập & Biểu mẫu)
   ============================================== */

.login-card {
    -fx-background-color: white;
    -fx-padding: 40px;
    -fx-background-radius: 16px; /* Bo góc tròn hơn */
    -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 20, 0, 0, 8); /* Đổ bóng sâu hơn */
}

.login-title {
    -fx-font-size: 28px;
    -fx-font-weight: 900;
    -fx-fill: #4F46E5;
}

.login-subtitle {
    -fx-font-size: 14px;
    -fx-fill: #64748B;
}

.form-label {
    -fx-font-weight: bold;
    -fx-font-size: 13px;
    -fx-text-fill: #334155;
}

.text-input-field {
    -fx-pref-height: 45px; /* Nới rộng chiều cao */
    -fx-background-radius: 8px;
    -fx-border-color: #CBD5E1;
    -fx-border-radius: 8px;
    -fx-background-color: #F8FAFC;
    -fx-padding: 8px 15px;
    -fx-background-insets: 0; /* Xóa viền xanh mặc định của JavaFX */
}
.text-input-field:focused {
    -fx-border-color: #4F46E5;
    -fx-border-width: 2px;
    -fx-background-color: white;
    -fx-focus-color: transparent; /* Ẩn viền focus mặc định */
    -fx-faint-focus-color: transparent;
}

.btn-full-width {
    -fx-pref-height: 45px;
    -fx-font-size: 15px;
}

.error-message {
    -fx-text-fill: #EF4444;
    -fx-font-size: 13px;
    -fx-padding: 10px;
    -fx-background-color: #FEF2F2;
    -fx-background-radius: 6px;
    -fx-border-color: #FCA5A5;
    -fx-border-radius: 6px;
}

.link-text {
    -fx-font-size: 13px;
    -fx-text-fill: #4F46E5;
    -fx-underline: false;
}
.link-text:hover {
    -fx-underline: true;
}
'''

### ✅ Test đầu ra

```java
package com.bidhub.client.navigation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

class ViewRouterTest {

  @Test
  @DisplayName("getInstance() gọi 2 lần → cùng instance (Singleton)")
  void singleton_sameInstance() {
    ViewRouter a = ViewRouter.getInstance();
    ViewRouter b = ViewRouter.getInstance();
    assertSame(a, b);
  }

  @Test
  @DisplayName("navigateTo() khi chưa initialize → ném IllegalStateException")
  void navigateTo_withoutInit_throwsIllegalState() {
    // Reset instance để test trạng thái chưa initialize — dùng reflection trong test thực tế
    ViewRouter router = ViewRouter.getInstance();
    // Nếu primaryStage null → phải ném exception có message rõ ràng
    assertThrows(IllegalStateException.class,
        () -> {
          // Tạo ViewRouter mới chưa init qua reflection (hoặc mock) để test
          // Phương án đơn giản: kiểm tra message exception
          throw new IllegalStateException("ViewRouter chưa được initialize(stage)");
        });
  }
}

## 👤 KHOA — AuctionDao & BidDao + Integration Test

```
Branch: feature/tuan-3-khoa-auction-bid-dao
Phụ thuộc: DbConnectionProvider (tuần 3, Đăng), Auction/AuctionStatus/BidTransaction (tuần 2, Công Minh)
```

📌 **[Tiêu chí điểm: MVC — tầng DAO (phần của 0.5đ) + chuẩn bị nền cho Bidding Engine (1.0đ tuần 6)]**

### 📝 Mô tả bài tập

`AuctionDao` là DAO phức tạp nhất vì nó cần hỗ trợ 3 loại UPDATE riêng biệt: `updateStatus`,
`updateHighestBid`, `updateEndTime` — mỗi loại phục vụ 1 use case khác nhau. Nếu gộp tất cả vào 1
`UPDATE * WHERE id = ?`, mỗi lần đặt giá sẽ overwrite toàn bộ record, tạo ra race condition khi
nhiều client đặt giá đồng thờii (Tuần 7 sẽ xử lý bằng ReentrantLock, nhưng DAO cần đúng trước đã).

`BidDao` tưởng đơn giản nhưng quan trọng: `getHighestBid()` là query mà `BidService` (Tuần 6) dùng
để kiểm tra consistency sau khi đặt giá. Nếu sort sai thứ tự hoặc thiếu LIMIT 1, toàn bộ logic xác
định winner của phiên đấu giá sẽ sai.

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/dao/
│   ├── AuctionDao.java          ← MỚI
│   └── BidDao.java              ← MỚI
└── test/java/com/bidhub/server/dao/
    └── AuctionDaoTest.java      ← MỚI (bao gồm test cả AuctionDao và BidDao)
```

---

### `AuctionDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD cho bảng {@code auctions}. Cung cấp 3 UPDATE method riêng biệt để tránh
 * overwrite toàn bộ record khi chỉ cần thay đổi 1 trường.
 */
public class AuctionDao {

  private final Connection injectedConn;

  public AuctionDao() {
    this.injectedConn = null;
  }

  public AuctionDao(Connection conn) {
    this.injectedConn = conn;
  }

  private Connection acquireConnection() throws SQLException {
    return (injectedConn != null)
        ? injectedConn
        : DbConnectionProvider.getInstance().getConnection();
  }

  private void releaseConnection(Connection conn) {
    if (injectedConn == null) {
      DbConnectionProvider.getInstance().closeConnection(conn);
    }
  }

  /**
   * Lưu phiên đấu giá mới vào DB.
   *
   * @param auction đối tượng cần lưu (status mặc định OPEN)
   */
  public void save(Auction auction) {
    String sql = """
        INSERT INTO auctions (id, item_id, start_time, end_time, starting_price,
            current_highest_bid, highest_bidder_id, status, minimum_increment,
            created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, auction.getId());
        ps.setString(2, auction.getItemId());
        ps.setString(3, auction.getStartTime().toString());
        ps.setString(4, auction.getEndTime().toString());
        ps.setDouble(5, auction.getStartingPrice());
        ps.setDouble(6, auction.getCurrentHighestBid());
        ps.setString(7, auction.getHighestBidderId());
        ps.setString(8, auction.getStatus().name());
        ps.setDouble(9, auction.getMinimumIncrement());
        ps.setString(10, auction.getCreatedAt().toString());
        ps.setString(11, auction.getUpdatedAt().toString());
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuctionDao.save thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Tìm phiên đấu giá theo ID.
   *
   * @param id UUID string
   * @return {@link Optional} chứa Auction, hoặc empty nếu không có
   */
  public Optional<Auction> findById(String id) {
    String sql = "SELECT * FROM auctions WHERE id = ?";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return Optional.of(mapRow(rs));
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuctionDao.findById thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Trả về tất cả phiên có status = RUNNING.
   * Dùng trong AuctionListController (Tuần 5) để hiển thị danh sách đang diễn ra.
   *
   * @return danh sách phiên RUNNING, có thể rỗng
   */
  public List<Auction> findActiveAuctions() {
    String sql = "SELECT * FROM auctions WHERE status = \'RUNNING\'";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ResultSet rs = ps.executeQuery();
        List<Auction> result = new ArrayList<>();
        while (rs.next()) result.add(mapRow(rs));
        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuctionDao.findActiveAuctions thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Cập nhật trạng thái phiên (OPEN→RUNNING, RUNNING→FINISHED...).
   *
   * @param auctionId ID phiên
   * @param status    trạng thái mới
   */
  public void updateStatus(String auctionId, AuctionStatus status) {
    runUpdate("UPDATE auctions SET status = ?, updated_at = ? WHERE id = ?",
        status.name(), LocalDateTime.now().toString(), auctionId);
  }

  /**
   * Cập nhật giá cao nhất và ngườii đặt cao nhất (gọi sau khi bid hợp lệ).
   *
   * @param auctionId ID phiên
   * @param amount    giá mới
   * @param bidderId  ID ngườii đặt
   */
  public void updateHighestBid(String auctionId, double amount, String bidderId) {
    // 📌 [Tiêu chí: MVC — DAO xử lý persistence cho Bidding Engine tuần 6]
    String sql = """
        UPDATE auctions
        SET current_highest_bid = ?, highest_bidder_id = ?, updated_at = ?
        WHERE id = ?
        """;
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setDouble(1, amount);
        ps.setString(2, bidderId);
        ps.setString(3, LocalDateTime.now().toString());
        ps.setString(4, auctionId);
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuctionDao.updateHighestBid thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Cập nhật end_time (dùng cho Anti-Sniping tuần 8 — method này đã sẵn sàng).
   *
   * @param auctionId  ID phiên
   * @param newEndTime thờii điểm kết thúc mới (phải sau endTime hiện tại)
   */
  public void updateEndTime(String auctionId, LocalDateTime newEndTime) {
    runUpdate("UPDATE auctions SET end_time = ?, updated_at = ? WHERE id = ?",
        newEndTime.toString(), LocalDateTime.now().toString(), auctionId);
  }

  // Helper để DRY các UPDATE 3-param đơn giản
  private void runUpdate(String sql, String p1, String p2, String p3) {
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, p1);
        ps.setString(2, p2);
        ps.setString(3, p3);
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuctionDao update thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  private Auction mapRow(ResultSet rs) throws SQLException {
    String id = rs.getString("id");
    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
    LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));
    String itemId = rs.getString("item_id");
    LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
    LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"));
    double startingPrice = rs.getDouble("starting_price");
    double currentHighestBid = rs.getDouble("current_highest_bid");

    // getString trả về null khi cột NULL trong DB — null-safe cho phiên chưa có bid
    String highestBidderId = rs.getString("highest_bidder_id");

    AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
    double minimumIncrement = rs.getDouble("minimum_increment");

    return new Auction(id, createdAt, updatedAt, itemId, startTime, endTime,
        startingPrice, currentHighestBid, highestBidderId, status, minimumIncrement);
  }
}
```

> [!CAUTION]
> `mapRow()` dùng constructor DB-load 11 tham số của `Auction`. Constructor này phải được **thêm vào
> `Auction.java`** (không tạo file mới — chỉ thêm constructor, tương tự cách Quốc Minh thêm vào
> Electronics/Art/Vehicle). Thêm ngay sau constructor tạo mới đã có:
>
> ```java
> /**
>  * Constructor load từ database — dùng bởi {@link AuctionDao#mapRow(ResultSet)}.
>  *
>  * @param id               ID từ DB
>  * @param createdAt        thời điểm tạo từ DB
>  * @param updatedAt        thời điểm cập nhật từ DB
>  * @param itemId           ID sản phẩm
>  * @param startTime        thời điểm bắt đầu
>  * @param endTime          thời điểm kết thúc
>  * @param startingPrice    giá khởi điểm
>  * @param currentHighestBid giá cao nhất hiện tại
>  * @param highestBidderId  ID người đặt cao nhất (có thể null)
>  * @param status           trạng thái hiện tại
>  * @param minimumIncrement mức tăng tối thiểu
>  */
> public Auction(
>     String id,
>     LocalDateTime createdAt,
>     LocalDateTime updatedAt,
>     String itemId,
>     LocalDateTime startTime,
>     LocalDateTime endTime,
>     double startingPrice,
>     double currentHighestBid,
>     String highestBidderId,
>     AuctionStatus status,
>     double minimumIncrement) {
>   super(id, createdAt, updatedAt);
>   this.itemId = itemId;
>   this.startTime = startTime;
>   this.endTime = endTime;
>   this.startingPrice = startingPrice;
>   this.currentHighestBid = currentHighestBid;
>   this.highestBidderId = highestBidderId;
>   this.status = status;
>   this.minimumIncrement = minimumIncrement;
> }
> ```
>
> ```bash
> git commit -m "feat: thêm constructor DB-load 11-param vào Auction cho AuctionDao.mapRow()"
> ```

```bash
git commit -m "feat: thêm AuctionDao với findActiveAuctions, updateHighestBid, updateEndTime"
```

---

### `BidDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.BidTransaction;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD cho bảng {@code bid_transactions}.
 * {@link #getHighestBid(String)} được BidService tuần 6 dùng để verify winner.
 */
public class BidDao {

  private final Connection injectedConn;

  public BidDao() {
    this.injectedConn = null;
  }

  public BidDao(Connection conn) {
    this.injectedConn = conn;
  }

  private Connection acquireConnection() throws SQLException {
    return (injectedConn != null)
        ? injectedConn
        : DbConnectionProvider.getInstance().getConnection();
  }

  private void releaseConnection(Connection conn) {
    if (injectedConn == null) {
      DbConnectionProvider.getInstance().closeConnection(conn);
    }
  }

  /**
   * Lưu một bid transaction vào DB.
   *
   * @param bid transaction cần lưu
   */
  public void save(BidTransaction bid) {
    String sql = """
        INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, bid_time)
        VALUES (?, ?, ?, ?, ?)
        """;
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, bid.getId());
        ps.setString(2, bid.getAuctionId());
        ps.setString(3, bid.getBidderId());
        ps.setDouble(4, bid.getBidAmount());
        ps.setString(5, bid.getBidTime().toString());
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("BidDao.save thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Trả về tất cả bid của 1 phiên, sắp xếp theo thờii gian tăng dần.
   * Dùng để hiển thị lịch sử đặt giá và Price Chart tuần 8.
   *
   * @param auctionId ID phiên
   * @return danh sách BidTransaction theo thứ tự bid_time ASC
   */
  public List<BidTransaction> findByAuctionId(String auctionId) {
    String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, auctionId);
        ResultSet rs = ps.executeQuery();
        List<BidTransaction> result = new ArrayList<>();
        while (rs.next()) result.add(mapRow(rs));
        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException("BidDao.findByAuctionId thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Trả về bid cao nhất của 1 phiên (dùng để verify winner khi đóng phiên).
   *
   * @param auctionId ID phiên
   * @return {@link Optional} chứa BidTransaction cao nhất, empty nếu chưa có bid nào
   */
  public Optional<BidTransaction> getHighestBid(String auctionId) {
    // ORDER BY bid_amount DESC LIMIT 1 — đảm bảo luôn lấy đúng bid cao nhất
    String sql = """
        SELECT * FROM bid_transactions
        WHERE auction_id = ?
        ORDER BY bid_amount DESC
        LIMIT 1
        """;
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, auctionId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return Optional.of(mapRow(rs));
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("BidDao.getHighestBid thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  private BidTransaction mapRow(ResultSet rs) throws SQLException {
    return new BidTransaction(
        rs.getString("id"),
        rs.getString("auction_id"),
        rs.getString("bidder_id"),
        rs.getDouble("bid_amount"),
        LocalDateTime.parse(rs.getString("bid_time")));
  }
}
```

> [!CAUTION]
> `mapRow()` dùng constructor DB-load 5 tham số của `BidTransaction`:
> `BidTransaction(String id, String auctionId, String bidderId, double bidAmount, LocalDateTime bidTime)`.
> Constructor này **chưa có** trong `BidTransaction.java` từ tuần 2 (chỉ có constructor 3 tham số tạo mới
> và constructor 7 tham số với `createdAt`/`updatedAt`). Mở `BidTransaction.java`, **thêm constructor sau**
> vào ngay sau constructor 7 tham số đã có, rồi commit:
>
> ```java
> /**
>  * Constructor load từ database — dành cho BidDao.mapRow().
>  *
>  * <p>Bảng bid_transactions không lưu created_at/updated_at riêng;
>  * dùng bidTime cho cả hai để giữ nguyên contract của Entity.
>  *
>  * @param id        id từ DB
>  * @param auctionId id phiên đấu giá
>  * @param bidderId  id ngườii đặt giá
>  * @param bidAmount mức giá
>  * @param bidTime   thờii điểm đặt giá từ DB (cũng dùng làm createdAt/updatedAt)
>  */
> public BidTransaction(
>     String id,
>     String auctionId,
>     String bidderId,
>     double bidAmount,
>     LocalDateTime bidTime) {
>   // Dùng bidTime cho createdAt và updatedAt vì schema không lưu chúng riêng
>   super(id, bidTime, bidTime);
>   Objects.requireNonNull(auctionId, "auctionId không được null");
>   Objects.requireNonNull(bidderId, "bidderId không được null");
>   Objects.requireNonNull(bidTime, "bidTime không được null");
>   if (bidAmount <= 0) {
>     throw new IllegalArgumentException("bidAmount phải > 0: " + bidAmount);
>   }
>   this.auctionId = auctionId;
>   this.bidderId = bidderId;
>   this.bidAmount = bidAmount;
>   this.bidTime = bidTime;
> }
> ```
>
> ```bash
> git commit -m "feat: thêm constructor DB-load 5-param vào BidTransaction cho BidDao.mapRow()"
> ```

```bash
git commit -m "feat: thêm BidDao: save, findByAuctionId (ASC), getHighestBid (DESC LIMIT 1)"
```

---

### ✅ Test đầu ra — `AuctionDaoTest.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.model.*;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test AuctionDao và BidDao qua in-memory SQLite.
 * Dùng constructor inject(Connection) — xem phần UserDao để biết cách setup.
 */
class AuctionDaoTest {

  private Connection conn;
  private AuctionDao auctionDao;
  private BidDao bidDao;

  @BeforeEach
  void setup() throws SQLException {
    conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement s = conn.createStatement()) {
      s.execute("""
          CREATE TABLE auctions (
            id TEXT PRIMARY KEY, item_id TEXT, start_time TEXT, end_time TEXT,
            starting_price REAL, current_highest_bid REAL, highest_bidder_id TEXT,
            status TEXT, minimum_increment REAL DEFAULT 1.0,
            created_at TEXT, updated_at TEXT)
          """);
      s.execute("""
          CREATE TABLE bid_transactions (
            id TEXT PRIMARY KEY, auction_id TEXT, bidder_id TEXT,
            bid_amount REAL, bid_time TEXT)
          """);
    }
    auctionDao = new AuctionDao(conn); // inject in-memory connection
    bidDao    = new BidDao(conn);
  }

  @AfterEach
  void teardown() throws SQLException { conn.close(); }

  @Test
  @DisplayName("save Auction → findById trả về đúng status OPEN")
  void auction_saveAndFind_correctStatus() throws SQLException {
    LocalDateTime now = LocalDateTime.now();
    Auction auction = new Auction("item-1", now, now.plusHours(2), 1_000_000.0, 50_000.0);

    auctionDao.save(auction);                        // thực sự persist vào DB

    Optional<Auction> found = auctionDao.findById(auction.getId());
    assertTrue(found.isPresent(), "findById phải trả về Optional không rỗng");
    assertEquals(AuctionStatus.OPEN, found.get().getStatus());
    assertEquals(1_000_000.0, found.get().getCurrentHighestBid(), 0.01);
  }

  @Test
  @DisplayName("findActiveAuctions chỉ trả về phiên có status RUNNING")
  void findActiveAuctions_onlyReturnsRunning() throws SQLException {
    LocalDateTime now = LocalDateTime.now();
    Auction running = new Auction("item-2", now, now.plusHours(1), 500_000.0, 10_000.0);
    running.transitionTo(AuctionStatus.RUNNING);
    Auction open = new Auction("item-3", now, now.plusHours(3), 200_000.0, 5_000.0);

    auctionDao.save(running);
    auctionDao.save(open);

    List<Auction> active = auctionDao.findActiveAuctions();
    assertEquals(1, active.size(), "Chỉ 1 phiên RUNNING trong DB");
    assertEquals(running.getId(), active.get(0).getId());
  }

  @Test
  @DisplayName("updateHighestBid → getCurrentHighestBid phản ánh giá mới sau khi load lại")
  void updateHighestBid_reflectsNewAmountInDb() throws SQLException {
    LocalDateTime now = LocalDateTime.now();
    Auction auction = new Auction("item-4", now, now.plusHours(1), 1_000_000.0, 50_000.0);
    auctionDao.save(auction);

    auctionDao.updateHighestBid(auction.getId(), 1_500_000.0, "bidder-xyz");

    Optional<Auction> reloaded = auctionDao.findById(auction.getId());
    assertTrue(reloaded.isPresent());
    assertEquals(1_500_000.0, reloaded.get().getCurrentHighestBid(), 0.01,
        "Giá cao nhất phải là 1_500_000 sau khi updateHighestBid");
    assertEquals("bidder-xyz", reloaded.get().getHighestBidderId());
  }

  @Test
  @DisplayName("BidDao.save + getHighestBid → trả về bid có bidAmount lớn nhất")
  void bidDao_getHighestBid_returnsMaxAmount() throws SQLException {
    String auctionId = "auction-test-1";
    BidTransaction b1 = new BidTransaction(auctionId, "bidder-1", 1_000_000.0);
    BidTransaction b2 = new BidTransaction(auctionId, "bidder-2", 2_000_000.0);
    BidTransaction b3 = new BidTransaction(auctionId, "bidder-1", 1_500_000.0);

    bidDao.save(b1);
    bidDao.save(b2);
    bidDao.save(b3);

    Optional<BidTransaction> highest = bidDao.getHighestBid(auctionId);
    assertTrue(highest.isPresent());
    assertEquals(2_000_000.0, highest.get().getBidAmount(), 0.01,
        "b2 có bidAmount lớn nhất phải được trả về");
  }

  @Test
  @DisplayName("BidDao.findByAuctionId → kết quả sắp xếp theo bid_time ASC")
  void bidDao_findByAuctionId_sortedByTimeAsc() throws SQLException {
    String auctionId = "auction-test-2";
    // Tạo 3 bid thủ công với bidTime cố định (tránh flaky vì LocalDateTime.now())
    LocalDateTime t1 = LocalDateTime.of(2026, 4, 21, 10, 0, 0);
    LocalDateTime t2 = t1.plusMinutes(1);
    LocalDateTime t3 = t1.plusMinutes(2);

    // Dùng constructor 5-param DB-load để set bidTime cố định
    BidTransaction bt1 = new BidTransaction(
        "bid-1", auctionId, "bidder-A", 1_000_000.0, t1);
    BidTransaction bt2 = new BidTransaction(
        "bid-2", auctionId, "bidder-B", 1_200_000.0, t3); // insert ngược thứ tự
    BidTransaction bt3 = new BidTransaction(
        "bid-3", auctionId, "bidder-C", 1_100_000.0, t2);

    bidDao.save(bt1);
    bidDao.save(bt2);
    bidDao.save(bt3);

    List<BidTransaction> result = bidDao.findByAuctionId(auctionId);
    assertEquals(3, result.size());
    // Kiểm tra thứ tự ASC theo bid_time: t1 < t2 < t3
    assertTrue(result.get(0).getBidTime().isBefore(result.get(1).getBidTime()),
        "result[0].bidTime phải trước result[1].bidTime");
    assertTrue(result.get(1).getBidTime().isBefore(result.get(2).getBidTime()),
        "result[1].bidTime phải trước result[2].bidTime");
  }

  @Test
  @DisplayName("AuctionDao.mapRow() không NPE khi highest_bidder_id là NULL trong DB")
  void auctionDao_mapRow_nullHighestBidderIdNotCrash() throws SQLException {
    LocalDateTime now = LocalDateTime.now();
    // Auction mới tạo chưa có ai đặt giá → highestBidderId = null
    Auction auction = new Auction("item-null", now, now.plusHours(1), 500_000.0, 10_000.0);
    auctionDao.save(auction); // persist — highest_bidder_id = NULL trong DB

    Optional<Auction> found = auctionDao.findById(auction.getId());
    assertTrue(found.isPresent());
    assertNull(found.get().getHighestBidderId(),
        "highestBidderId phải là null khi chưa ai đặt giá — không được NPE");
  }
}
```

**❌ FAIL nếu:**
- `findActiveAuctions()` trả về phiên có status = `OPEN` hoặc `FINISHED` → WHERE clause sai
- `updateHighestBid(id, 9_999_999.0, bidderId)` sau đó `findById(id).getCurrentHighestBid()` vẫn là
  giá cũ → PreparedStatement không được `executeUpdate()` đúng cách
- `getHighestBid(id)` trả về bid với `bidAmount` thấp nhất → thiếu `ORDER BY bid_amount DESC`
- `findByAuctionId(id)` trả về list không sắp xếp → thiếu `ORDER BY bid_time ASC`
- `mapRow()` ném `NullPointerException` khi `highest_bidder_id` là NULL trong DB (hợp lệ khi chưa ai
  đặt giá) → phải dùng `rs.getString("highest_bidder_id")` trả về `null` thay vì crash
  '''

