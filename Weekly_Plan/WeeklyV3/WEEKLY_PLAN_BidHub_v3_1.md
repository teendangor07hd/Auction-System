# BidHub — Lộ trình 10 tuần 

> **Triết lý:** Code chạy được · Đúng logic · Hiểu được từng dòng
> Mọi thứ phức tạp hơn yêu cầu barem đều là rủi ro, không phải điểm cộng.


---

## Thành viên nhóm

| Ký hiệu | Họ tên | Vai trò chính |
|---------|--------|---------------|
| **Đăng** | [Họ tên đầy đủ] | Server Core & Database |
| **Quốc Minh** | [Họ tên đầy đủ] | Networking & Protocol |
| **Công Minh** | [Họ tên đầy đủ] | Client GUI (JavaFX) |
| **Khoa** | [Họ tên đầy đủ] | Business Logic, Testing & Backend Features |

> [!IMPORTANT]
> Vai trò chính chỉ xác định ai **code trước**. Mọi người phải **hiểu toàn bộ hệ thống** — giảng viên hỏi bất kỳ ai, cả nhóm cùng chịu trách nhiệm.

---

## Quy tắc Git

```
Nhánh:   main ← develop ← feature/tuan-X-ten-nguoi-mo-ta
Commit:  feat: thêm AuditLogDao / fix: sửa is_locked migration
PR:      [Tuần X] Tên người - Mô tả ngắn  |  cần ít nhất 1 người approve
Merge:   develop → main vào cuối Tuần 4, 7, 10
```

> [!CAUTION]
> Không commit 1 lần duy nhất vào phút chót. Commit thường xuyên = chứng minh quá trình làm việc thực sự.

---

## Tổng quan 10 tuần

```
┌──────────────┬────────────────────────────────────────────────────────────────────────┬──────────────────────────────┐
│ Giai đoạn    │ Nội dung                                                               │ Mục tiêu                     │
├──────────────┼────────────────────────────────────────────────────────────────────────┼──────────────────────────────┤
│ NỀN TẢNG     │ Tuần 1 · Setup + CI/CD + JavaFX skeleton                  ✅ DONE      │ Môi trường sẵn sàng          │
│              │ Tuần 2 · OOP model + Exception hierarchy                   ✅ DONE      │ Cây kế thừa đủ barem         │
├──────────────┼────────────────────────────────────────────────────────────────────────┼──────────────────────────────┤
│ CORE         │ Tuần 3 · DAO + SQLite + MVC routing                        ✅ DONE      │ CRUD thông suốt              │
│              │ Tuần 4 · Socket Server + RequestHandler + AuditLog DAO                 │ Ping-pong + audit foundation │
├──────────────┼────────────────────────────────────────────────────────────────────────┼──────────────────────────────┤
│ CHỨC NĂNG    │ Tuần 5 · Auth + SHA-256 + Item CRUD + AuditLogService tích hợp        │ Đăng ký/đăng nhập + log      │
│ CHÍNH        │ Tuần 6 · Bidding Engine + BidValidator + AdminUserService              │ Đặt giá + quản trị user      │
├──────────────┼────────────────────────────────────────────────────────────────────────┼──────────────────────────────┤
│ KỸ THUẬT     │ Tuần 7 · ReentrantLock + Observer realtime + ReportService             │ 1.5đ kỹ thuật + xuất BC      │
│ QUAN TRỌNG   │ Tuần 8 · Anti-Sniping + Price Chart + AuditLog tích hợp toàn hệ thống │ +1.0đ nâng cao + audit trail │
├──────────────┼────────────────────────────────────────────────────────────────────────┼──────────────────────────────┤
│ HOÀN THIỆN   │ Tuần 9 · Integration + Refactor + DataIntegrityService                │ CI xanh, hệ thống hoàn chỉnh │
│              │ Tuần 10 · Docs + AdminView UI + Demo + Submission                      │ Nộp bài tự tin               │
└──────────────┴────────────────────────────────────────────────────────────────────────┴──────────────────────────────┘
```

> **Lý do bỏ Auto-Bidding (0.5đ):** Độ khó 7/10, dễ gây Infinite Loop làm sập server, tốn 1–2 tuần để ổn định. Thay bằng Anti-Sniping (độ khó 2/10) + Price Chart (độ khó 5/10) — vẫn đủ 1.0đ nâng cao với rủi ro thấp hơn nhiều.

---

## ═══ TUẦN 1 · Thiết lập môi trường & Infrastructure ✅ DONE

**Mục tiêu:** Toàn nhóm có thể `mvn compile`, push lên GitHub, CI xanh, JavaFX chạy được.

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | Cài JDK 21, IntelliJ, Scene Builder, Maven | `java -version` → JDK 21 |
| 2 | Git cơ bản: branch, commit, PR, merge | Tạo branch, push, tạo PR thành công |
| 3 | Maven multi-module (`pom.xml` cha-con) | `mvn compile` không lỗi cả 2 module |
| 4 | JavaFX Hello World + FXML | Hiểu Stage/Scene/Controller, chạy cửa sổ |
| 5 | Java Optional cơ bản | Giải thích tại sao dùng `Optional` thay null |

---

### Đăng — Maven Multi-Module Setup & ConfigLoader
```
Branch: feature/tuan-1-dang-maven-setup
```

Khởi tạo Maven multi-module project `bidhub-parent → bidhub-server + bidhub-client`. `pom.xml` cha khai báo dependency dùng chung: JUnit 5, Jackson (`jackson-databind`). Module server thêm: `sqlite-jdbc`. Module client thêm: JavaFX controls + fxml.

Tạo `ConfigLoader.java` (package `com.bidhub.server.config`): đọc `server.properties` từ classpath dùng `Properties`. Method: `getString(String key)`, `getInt(String key)`. Config mẫu: `server.port=9090`, `db.path=data/bidhub.db`, `snipe.threshold=60`, `snipe.extension=60`.

Thêm `.gitignore` chuẩn (loại `target/`, `.idea/`, `*.iml`, `*.db`).

**✅ Đầu ra:**
- `mvn compile` và `mvn clean package` không lỗi ở cả 2 module
- `ConfigLoader.getString("server.port")` → `"9090"`
- `ConfigLoader.getInt("server.port")` → `9090`

---

### Quốc Minh — Git Workflow & CI/CD
```
Branch: feature/tuan-1-quocminh-git-cicd
```

Tạo `.github/workflows/ci.yml`: trigger khi push/PR vào `develop` và `main`, chạy `mvn test` trên Java 21. Thêm bước upload test report.

Tạo `CONTRIBUTING.md`: quy trình tạo branch, commit Conventional Commits, tạo PR và chờ approve. Tạo `.github/pull_request_template.md` với checklist.

Tạo `docs/API_PROTOCOL.md` (skeleton): định nghĩa format JSON sẽ dùng từ Tuần 4. Request: `{"type":"...", "token":"...", "payload":{}}`. Response: `{"status":"OK"|"ERROR", "type":"...", "payload":{}, "message":"..."}`.

**✅ Đầu ra:**
- Push lên GitHub → CI trigger → build pass ✅
- PR template hiển thị checklist khi tạo PR mới
- `docs/API_PROTOCOL.md` có ≥ 3 ví dụ request/response mẫu

---

### Công Minh — JavaFX App Skeleton
```
Branch: feature/tuan-1-congminh-javafx-skeleton
```

Tạo `BidHubApp.java` (extends `Application`): load `LoginView.fxml`, hiển thị Stage title "BidHub — Hệ thống đấu giá trực tuyến", kích thước 1024×720.

Tạo `LoginView.fxml` bằng Scene Builder (chỉ layout, chưa có logic): TextField username, PasswordField password, Button "Đăng nhập", Label lỗi ẩn. Tạo `LoginController.java` tương ứng với `initialize()` rỗng.

Tạo `Views.java` (constants): `LOGIN`, `AUCTION_LIST`, `AUCTION_DETAIL`, `CREATE_ITEM`.

**✅ Đầu ra:**
- `mvn javafx:run -pl bidhub-client` → cửa sổ BidHub hiện ra với LoginView
- FXML mở được trong Scene Builder không lỗi

---

### Khoa — JUnit 5 Setup & Coding Convention
```
Branch: feature/tuan-1-khoa-junit-convention
```

Kích hoạt `maven-surefire-plugin` (version 3.x). Tạo `CalculatorTest.java` với ≥ 15 test cases (bao gồm edge cases: chia 0, số âm) để verify JUnit chạy được.

Tạo `docs/STYLE_GUIDE.md` tóm tắt Google Java Style Guide cho nhóm (≥ 6 mục, mỗi mục có ví dụ ✅/❌): đặt tên class/method/field, thụt lề 2 spaces, độ dài dòng 100 ký tự.

**✅ Đầu ra:**
- `mvn test` → ≥ 15 tests pass, 0 failures
- Test chia 0 → `assertThrows(ArithmeticException.class, ...)`

---

## ═══ TUẦN 2 · OOP Domain Model & Exception Hierarchy ✅ DONE

**Mục tiêu:** Cây kế thừa Entity → User/Item → các subclass · Factory Method Pattern (chuẩn GoF) · Exception hierarchy (1.5đ barem)

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | Abstract class vs Interface — khi nào dùng cái nào | Giải thích được từ BidHub context |
| 2 | Factory Method Pattern — Creator, ConcreteCreator, Product, ConcreteProduct | Vẽ UML 4 thành phần, ánh xạ vào `ItemCreator`/`ElectronicsCreator`/`Item`/`Electronics` |
| 3 | Java Enum với method | Viết `AuctionStatus` với `canBid()`, `isTerminal()` |
| 4 | UUID trong Java | Dùng `UUID.randomUUID()` tạo ID cho Entity |

---

### Đăng — Entity Base & User Hierarchy
```
Branch: feature/tuan-2-dang-entity-user
```

Tạo `Entity.java` (abstract class, package `com.bidhub.common.model`): field `id` (String), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime). Constructor khởi tạo `id = UUID.randomUUID().toString()`. Override `equals()`/`hashCode()` dựa trên `id`.

Tạo `User.java` (abstract, extends Entity, package `com.bidhub.server.model`): field `username`, `passwordHash`, `email`, `role` (UserRole enum). Method abstract `getInfo(): String`.

Tạo enum `UserRole` { BIDDER, SELLER, ADMIN } với `getDisplayName(): String` (tiếng Việt).

Tạo `Bidder.java`, `Seller.java`, `Admin.java` — mỗi class extends User, override `getInfo()`. `Bidder` thêm field `totalBidsPlaced: int`. `Seller` thêm `totalItemsListed: int`.

**✅ Đầu ra:**
- `new Bidder(...)` → `getRole()` == `UserRole.BIDDER`
- `List<User>` gọi `getInfo()` → output khác nhau (polymorphism)
- 2 entity mới tạo → `id` khác nhau

---

### Quốc Minh — Item Hierarchy & Factory Method Pattern (chuẩn GoF)
```
Branch: feature/tuan-2-quocminh-item-factory
```

> [!IMPORTANT]
> **Static Factory ≠ Factory Method Pattern (GoF).** `ItemFactory.create()` (utility class với `private` constructor + static method) là **Static Factory Method** — không có kế thừa, không có polymorphism. Barem yêu cầu **Factory Method Pattern** (1.0đ), tức phải có Creator hierarchy. File này implement đúng 4 thành phần GoF.

Tạo cây kế thừa `Item` (Product) và áp dụng **Factory Method Pattern** chuẩn GoF:

**Product hierarchy:**
- `Item.java` (abstract, extends Entity, implements Displayable): field `name`, `description`, `startingPrice` (double), `sellerId`, `itemType` (ItemType enum). Constructor validate `startingPrice > 0`, ném `IllegalArgumentException` nếu vi phạm. Method abstract `getCategoryDetails(): String`.
- `Electronics.java` (extends Item, `final`): thêm `brand`, `warrantyMonths`.
- `Art.java` (extends Item, `final`): thêm `artist`, `yearCreated`.
- `Vehicle.java` (extends Item, `final`): thêm `manufacturer`, `year`, `mileageKm`.
- Mỗi class implement `getCategoryDetails()` và `printInfo()` khác nhau (Polymorphism).

**Creator hierarchy:**
- `ItemCreator.java` (abstract Creator): abstract factory method `createItem(String name, String description, double startingPrice, String sellerId, Map<String, Object> extras): Item`. Chứa `protected final` helpers `requireString(Map, String)` và `requireInt(Map, String)` dùng chung cho các ConcreteCreator. Static method `ItemCreator.forType(ItemType): ItemCreator` trả về đúng ConcreteCreator lúc runtime.
- `ElectronicsCreator.java` (extends ItemCreator, `final`): override `createItem()` → tạo `Electronics`.
- `ArtCreator.java` (extends ItemCreator, `final`): override `createItem()` → tạo `Art`.
- `VehicleCreator.java` (extends ItemCreator, `final`): override `createItem()` → tạo `Vehicle`.

**Các file hỗ trợ:**
- `Displayable.java` (interface): method `printInfo(): void`.
- `ItemType.java` (enum): { ELECTRONICS, ART, VEHICLE } với `getLabel(): String`.

**✅ Đầu ra:**
- `new ElectronicsCreator().createItem(...)` → `instanceof Electronics`
- `ItemCreator.forType(ItemType.ART)` → `instanceof ArtCreator`
- `List<Item>` gọi `getCategoryDetails()` → 3 output khác nhau
- `startingPrice = -100` → `IllegalArgumentException`
- `ElectronicsCreator` thiếu `extras["brand"]` → `IllegalArgumentException`

---

### Công Minh — Auction & BidTransaction
```
Branch: feature/tuan-2-congminh-auction-bid
```

Tạo `Auction.java` (extends Entity): field `itemId`, `startTime`, `endTime` (LocalDateTime), `startingPrice`, `currentHighestBid`, `highestBidderId`, `status` (AuctionStatus), `minimumIncrement` (default 1.0).

Tạo enum `AuctionStatus` { OPEN, RUNNING, FINISHED, PAID, CANCELED } với:
- `canBid(): boolean` — chỉ RUNNING trả true
- `isTerminal(): boolean` — FINISHED/PAID/CANCELED trả true

Method `Auction.transitionTo(AuctionStatus newStatus): void` — kiểm tra transition hợp lệ (OPEN→RUNNING, RUNNING→FINISHED, FINISHED→PAID/CANCELED), ném `IllegalStateException` nếu sai.

Method `Auction.isValidBid(double bidAmount): boolean` — `bidAmount > currentHighestBid && status.canBid()`.

Tạo `BidTransaction.java` (extends Entity): field `auctionId`, `bidderId`, `bidAmount` (double), `bidTime` (LocalDateTime).

**✅ Đầu ra:**
- `OPEN.transitionTo(RUNNING)` → hợp lệ
- `RUNNING.transitionTo(OPEN)` → `IllegalStateException`
- `isValidBid(currentBid + 100)` → true; `isValidBid(currentBid - 1)` → false

---

### Khoa — Exception Hierarchy
```
Branch: feature/tuan-2-khoa-exception
```

Tạo `BidHubException.java` (extends RuntimeException, package `com.bidhub.common.exception`): field `errorCode: String`. Constructor `(String message, String errorCode)`.

Tạo các subclass:
- `InvalidBidException` (errorCode="BID_INVALID")
- `AuctionNotFoundException` (errorCode="AUCTION_NOT_FOUND")
- `AuctionClosedException` (errorCode="AUCTION_CLOSED")
- `UserNotFoundException` (errorCode="USER_NOT_FOUND")
- `DuplicateUsernameException` (errorCode="USERNAME_TAKEN")
- `AuthenticationException` (errorCode="AUTH_FAILED")
- `ValidationException` (errorCode="VALIDATION_ERROR") — thêm field `errors: List<String>`

> [!TIP]
> **Validation đơn giản:** Không cần Fluent API. Chỉ cần: `if (username == null || username.isBlank()) throw new ValidationException("username không được để trống");`. Đủ điểm, dễ hiểu, không lỗi.

**✅ Đầu ra:**
- `new InvalidBidException("...")` instanceof `BidHubException` → true
- `ValidationException.getErrors()` trả về `List<String>` không null
- Test exception hierarchy: 5 subclass, catch bằng `BidHubException` được

---

## ═══ TUẦN 3 · DAO & Database + MVC Routing ✅ DONE

**Mục tiêu:** CRUD 4 bảng qua JDBC · SQLite · ViewRouter điều hướng màn hình (barem: MVC + kiến trúc)

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | JDBC cơ bản (Connection, PreparedStatement, ResultSet) | SELECT từ 1 bảng SQLite |
| 2 | Singleton Pattern (double-checked locking) | Giải thích `volatile` + double-checked |
| 3 | JavaFX: FXMLLoader, chuyển Scene | Code được màn hình A → B khi click button |
| 4 | SQL cơ bản | Viết được schema.sql cho bảng `users` |

---

### Đăng — DbConnectionProvider & Schema
```
Branch: feature/tuan-3-dang-database-setup
```

Tạo `DbConnectionProvider.java` (Singleton, package `com.bidhub.server.config`): double-checked locking, `volatile`. Đường dẫn DB từ `ConfigLoader`. Method: `getInstance()`, `getConnection()`, `closeConnection(Connection)`. Enable WAL mode (`PRAGMA journal_mode=WAL`).

Tạo `schema.sql` (resources/db/) với `CREATE TABLE IF NOT EXISTS`:
- `users` (id TEXT PK, username TEXT UNIQUE, password_hash TEXT, email TEXT, role TEXT, created_at TEXT, updated_at TEXT)
- `items` (id TEXT PK, name TEXT, description TEXT, starting_price REAL, item_type TEXT, seller_id TEXT, extra_data TEXT, created_at TEXT, updated_at TEXT)
- `auctions` (id TEXT PK, item_id TEXT, start_time TEXT, end_time TEXT, starting_price REAL, current_highest_bid REAL, highest_bidder_id TEXT, status TEXT, minimum_increment REAL, created_at TEXT, updated_at TEXT)
- `bid_transactions` (id TEXT PK, auction_id TEXT, bidder_id TEXT, bid_amount REAL, bid_time TEXT)

Tạo `MigrationRunner.java`: đọc `schema.sql`, execute từng statement. Gọi trong server startup.

**✅ Đầu ra:**
- `DbConnectionProvider.getInstance()` gọi 2 lần → `assertSame`
- Sau `MigrationRunner.run()` → 4 bảng tồn tại

---

### Quốc Minh — UserDao & ItemDao
```
Branch: feature/tuan-3-quocminh-user-item-dao
```

> [!TIP]
> **DAO đơn giản:** Không cần Generic AbstractDao phức tạp. Chỉ cần class thẳng `UserDao.java`, `ItemDao.java` với các method JDBC thuần. Giảng viên chấm theo **chức năng đúng**, không chấm kiến trúc DAO cao siêu.

Tạo `UserDao.java` (package `com.bidhub.server.dao`): các method dùng `PreparedStatement` thuần:
- `save(User user): void`
- `findById(String id): Optional<User>`
- `findByUsername(String username): Optional<User>`
- `existsByUsername(String username): boolean`
- `findAll(): List<User>`

`findByUsername()` đọc cột `role` để khởi tạo đúng `Bidder`/`Seller`/`Admin`.

Tạo `ItemDao.java`: `save`, `findById`, `findBySellerId(String sellerId): List<Item>`, `findAll`, `deleteById`. Cột `extra_data` lưu JSON string (dùng Jackson `ObjectMapper`) cho brand/artist/manufacturer.

> [!NOTE]
> **Tuần 3 — ItemDao không cần biết Creator.** `ItemDao` làm việc trực tiếp với `Electronics`, `Art`, `Vehicle` (concrete Product). Việc tạo Item là trách nhiệm của Creator hierarchy ở Tuần 2; ItemDao chỉ persist/retrieve.

**✅ Đầu ra:**
- `userDao.save(bidder)` → `findById(id)` trả về `Optional<Bidder>`
- `findByUsername("nonexistent")` → `Optional.empty()`
- `findAll()` sau save 3 users → size == 3
- Role SELLER → instance of `Seller`

---

### Công Minh — ViewRouter & FXML Skeleton
```
Branch: feature/tuan-3-congminh-viewrouter-fxml
```

Tạo `ViewRouter.java` (Singleton, package `com.bidhub.client.navigation`): field `primaryStage: Stage`. Method `initialize(Stage stage)`, `navigateTo(String viewName)`, `navigateTo(String viewName, Map<String, Object> params)`. Cơ chế: load FXML từ `"/fxml/{viewName}.fxml"`, inject params vào Controller qua interface `ContextAware`.

Tạo interface `ContextAware`: `setContext(Map<String, Object> params): void`.

Tạo FXML skeleton (chưa networking) cho:
- `AuctionListView.fxml` + `AuctionListController.java`: TableView 4 cột, Button tạo phiên mới
- `AuctionDetailView.fxml` + `AuctionDetailController.java`: Labels info, TextField bid, Button Đặt giá
- `CreateItemView.fxml` + `CreateItemController.java`: form nhập item

**✅ Đầu ra:**
- `ViewRouter.getInstance()` gọi 2 lần → cùng instance
- `navigateTo(Views.LOGIN)` → LoginView hiện trên Stage
- Tất cả FXML mở được trong Scene Builder không lỗi

---

### Khoa — AuctionDao & BidDao
```
Branch: feature/tuan-3-khoa-auction-bid-dao
```

Tạo `AuctionDao.java`: `save`, `findById`, `findActiveAuctions(): List<Auction>` (WHERE status='RUNNING'), `updateStatus(String auctionId, AuctionStatus status)`, `updateHighestBid(String auctionId, double amount, String bidderId)`, `updateEndTime(String auctionId, LocalDateTime newEndTime)`.

Tạo `BidDao.java`: `save`, `findByAuctionId(String auctionId): List<BidTransaction>` (sort by bid_time ASC), `getHighestBid(String auctionId): Optional<BidTransaction>`.

**✅ Đầu ra:**
- `auctionDao.save(auction)` → `findById(id)` đúng status
- `findActiveAuctions()` chỉ trả về RUNNING
- `updateHighestBid(id, 2000.0, bidderId)` → `findById(id).getCurrentHighestBid()` == 2000.0
- `bidDao.getHighestBid(id)` → BidTransaction có bidAmount lớn nhất

---

## ═══ TUẦN 4 · Socket Server + RequestHandler + Audit Log DAO

**Mục tiêu:** Client↔Server giao tiếp qua Socket · JSON qua Jackson · Ping-pong thành công · Khởi động Audit Log infrastructure

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | Java Socket (ServerSocket, Socket, IO streams) | Viết mini chat 2 chiều |
| 2 | Java Thread Pool (ExecutorService) | Giải thích tại sao dùng pool |
| 3 | Jackson ObjectMapper: serialize/deserialize | Deserialize JSON → POJO và ngược lại |
| 4 | JavaFX Task<T> và Platform.runLater() | Giải thích tại sao dùng Task cho network call |

---

### Đăng — SocketServerCore, Session & Schema Update
```
Branch: feature/tuan-4-dang-socket-server
```

Tạo `SocketServerCore.java` (package `com.bidhub.server.network`): field `serverSocket`, `threadPool` (ExecutorService fixed pool 30). Method `start(int port)` — vòng lặp `accept()` blocking, mỗi kết nối tạo `ClientConnectionThread` và submit vào pool. Method `shutdown()` đóng serverSocket và shutdown pool.

Tạo `Session.java` (package `com.bidhub.server.network`): field `sessionId` (UUID), `socket`, `out: PrintWriter`, `authenticatedUserId: String` (null nếu chưa login). Method `sendMessage(String jsonResponse): void` — synchronized trên `out` để thread-safe. Method `isAuthenticated(): boolean`, `disconnect(): void` đóng socket sạch sẽ.

Tạo `ClientConnectionThread.java` (implements Runnable): nhận `Session`, vòng lặp đọc từng dòng JSON qua `BufferedReader` → gọi `RequestHandler.handle(line, session)` → lấy response String → gọi `session.sendMessage(response)`. Khi `readLine()` trả về null hoặc IOException → thoát vòng lặp, gọi `session.disconnect()` và dọn dẹp.

Tạo `ServerApp.java`: entry point `main()` — đọc port từ `ConfigLoader`, chạy `MigrationRunner.run()`, tạo và start `SocketServerCore`.

**[MỚI v3.1]** Cập nhật `schema.sql` — thêm bảng thứ 5 để hỗ trợ Audit Log của Khoa:

```sql
CREATE TABLE IF NOT EXISTS audit_logs (
  id         TEXT PRIMARY KEY,
  user_id    TEXT,
  action     TEXT NOT NULL,
  details    TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
```

Cập nhật `MigrationRunner` để đọc và execute đủ 5 bảng. Bảng `audit_logs` không có cột `updated_at` — chỉ có `created_at` (giống `bid_transactions`).

**✅ Đầu ra:**
- `ServerApp` start → `telnet localhost 9090` kết nối được
- Khi client ngắt kết nối đột ngột (Ctrl+C) → không crash server, session dọn dẹp sạch
- Sau `MigrationRunner.run()` → 5 bảng tồn tại (kiểm tra bằng `SELECT name FROM sqlite_master WHERE type='table'`)

---

### Quốc Minh — JSON Protocol & RequestHandler
```
Branch: feature/tuan-4-quocminh-protocol-handler
```

Tạo `MessageRequest.java` (POJO Jackson, package `com.bidhub.server.network`): field `type: String`, `token: String`, `payload: JsonNode`. Annotate `@JsonIgnoreProperties(ignoreUnknown = true)` để parse an toàn JSON dư trường.

Tạo `MessageResponse.java` (POJO): field `status: String` ("OK" hoặc "ERROR"), `type: String`, `payload: Object`, `message: String`. Thêm 2 static factory method: `ok(String type, Object payload)` → trả về MessageResponse status="OK"; `error(String type, String message)` → trả về status="ERROR".

Tạo `MessageMapper.java` (package `com.bidhub.server.network`): field `private static final ObjectMapper MAPPER` — khởi tạo một lần (thread-safe). Method `toJson(Object obj): String` — serialize, ném `RuntimeException` nếu lỗi. Method `fromJson(String json, Class<T> clazz): T` — deserialize.

Tạo `RequestHandler.java` (package `com.bidhub.server.network`): method `handle(String jsonLine, Session session): String`. Logic:

```
1. parse jsonLine → MessageRequest (nếu parse lỗi → trả error ngay)
2. kiểm tra needsAuth(req.getType()) && !session.isAuthenticated() → trả error "Bạn chưa đăng nhập"
3. switch(req.getType()): PING → handlePing(); LOGIN → handleLogin(); ... default → error Unknown
4. mọi BidHubException trong handler → bắt → trả error response với message + errorCode
5. mọi Exception khác → bắt → trả error "Lỗi hệ thống"
```

Implement `handlePing(Session session, JsonNode payload): String` trả về payload `{"serverTime": "<ISO timestamp>", "message": "pong"}`.

Thêm constant `private static final Set<String> AUTH_REQUIRED` chứa các type cần auth: `"CREATE_ITEM"`, `"PLACE_BID"`, `"LOGOUT"`, và các type mới thêm ở tuần sau.

Cập nhật `docs/API_PROTOCOL.md`: bổ sung ví dụ PING request/response, mô tả format token, error response.

**✅ Đầu ra:**
- `handle("{\"type\":\"PING\",\"payload\":{}}", session)` → JSON status="OK", message="pong"
- `handle("{\"type\":\"NOTEXIST\",\"payload\":{}}", session)` → status="ERROR"
- `handle("not valid json", session)` → status="ERROR", không throw exception ra ngoài
- Request type cần auth gửi với session chưa login → "Bạn chưa đăng nhập"

---

### Công Minh — ServerGateway (Client-side) + NetworkTask + ClientSession
```
Branch: feature/tuan-4-congminh-server-gateway
```

Tạo `ServerGateway.java` (Singleton, package `com.bidhub.client.network`): field `socket`, `reader: BufferedReader`, `writer: PrintWriter`. Method `connect(String host, int port): void` — tạo Socket, khởi tạo reader/writer. Method `sendRequest(MessageRequest request): MessageResponse` — serialize request → gửi qua writer → `writer.flush()` → đọc 1 dòng response → deserialize → trả về. Method `disconnect()` đóng tài nguyên. Method `isConnected(): boolean`.

> [!TIP]
> **Khi mất kết nối:** Không cần Exponential Backoff phức tạp. Chỉ cần hiện Alert: `"Mất kết nối tới Server. Vui lòng khởi động lại ứng dụng."` là đủ.

Tạo `NetworkTask<T>` (extends `javafx.concurrent.Task<T>`, package `com.bidhub.client.network`): nhận `Callable<T>` trong constructor. Override `call()` — thực thi callable, mọi exception được wrap thành `RuntimeException`. Dùng trong Controller: `new Thread(task).start()` và `task.setOnSucceeded / setOnFailed` để update UI trên JavaFX thread.

Tạo `ClientSession.java` (Singleton, package `com.bidhub.client.network`): field `token: String`, `currentUserId: String`, `currentUsername: String`, `currentRole: UserRole`. Method `login(String token, String userId, String username, UserRole role)`, `logout()` reset tất cả field về null, `isLoggedIn(): boolean` kiểm tra token != null.

Cập nhật `BidHubApp.java`: trong `start()`, sau khi show stage, gọi `ServerGateway.getInstance().connect(host, port)` trong `NetworkTask`. Nếu connect thất bại → hiện Alert, đóng app.

**✅ Đầu ra:**
- `connect("localhost", 9090)` khi server đang chạy → `isConnected()` == true
- `sendRequest(ping request)` → response status="OK", message="pong"
- Khi server không chạy → Alert "Mất kết nối" hiện, không crash

---

### Khoa — AuditLog Model + AuditLogDao + Protocol Tests
```
Branch: feature/tuan-4-khoa-auditlog-dao-tests
```

**[MỚI v3.1] AuditLog infrastructure:**

Tạo `AuditLog.java` (package `com.bidhub.server.model`, kế thừa `Entity`): field `userId: String` (nullable — null nếu là system action), `action: String` (mã SCREAMING_SNAKE_CASE), `details: String` (JSON string). Constructor mới nhận `(String userId, String action, String details)` — tự generate `id` và `createdAt` qua Entity. Constructor load từ DB nhận đủ 5 tham số bao gồm `id` và `createdAt`. Cung cấp getters, không có setters (immutable sau khi tạo).

Tạo interface `AuditActions` (package `com.bidhub.server.model`): tập hợp hằng số String kiểu SCREAMING_SNAKE_CASE — `USER_LOGIN`, `USER_LOGOUT`, `USER_REGISTER`, `USER_LOCKED`, `USER_UNLOCKED`, `PLACE_BID`, `AUCTION_CLOSED`, `AUCTION_EXTENDED`, `ITEM_CREATED`, `ITEM_DELETED`. Mục đích: tập trung tất cả action code một chỗ, tránh hardcode string rải rác.

Tạo `AuditLogDao.java` (package `com.bidhub.server.dao`), theo đúng pattern DAO đã có từ Tuần 3:

| Method | Return | Mô tả |
|--------|--------|-------|
| `AuditLogDao()` | — | Constructor production — dùng `DbConnectionProvider` |
| `AuditLogDao(Connection conn)` | — | Constructor test — inject connection in-memory |
| `save(AuditLog log)` | void | INSERT vào `audit_logs` |
| `findAll()` | `List<AuditLog>` | SELECT tất cả, ORDER BY created_at DESC |
| `findByUserId(String userId)` | `List<AuditLog>` | WHERE user_id = ? |
| `findByAction(String action)` | `List<AuditLog>` | WHERE action = ? |
| `findRecent(int limit)` | `List<AuditLog>` | ORDER BY created_at DESC LIMIT ? |

> [!TIP]
> Dùng lại đúng pattern DAO từ Tuần 3. `AuditLog` không có `updatedAt` riêng — dùng `createdAt` (giống `BidTransaction`).

**[GIỮ ĐỦ] Protocol tests (≥ 10 cases):** Test `RequestHandler` với các scenario: PING → OK, type không tồn tại → ERROR, gửi type cần auth khi session chưa login → error message "Bạn chưa đăng nhập", JSON sai format → error không crash. Test `MessageMapper`: serialize object → parse lại → không exception; deserialize JSON thiếu trường → `@JsonIgnoreProperties` không crash.

**[MỚI v3.1] Test cho AuditLogDao (≥ 5 cases):**
- `save()` → `findAll()` trả về đúng bản ghi vừa lưu
- `findByUserId()` chỉ trả về log của đúng user đó, không lẫn user khác
- `findByAction("USER_LOGIN")` → chỉ trả về log có action="USER_LOGIN"
- `findRecent(3)` với 10 bản ghi trong DB → tối đa 3 kết quả
- `save()` với `userId = null` (system action) → không crash, bản ghi lưu được

**✅ Đầu ra:**
- ≥ 15 test cases pass (10 protocol + 5 AuditLogDao)
- `AuditLog` kế thừa `Entity` → `getId()`, `getCreatedAt()` hoạt động
- `auditLogDao.save(log)` → `findByUserId(userId)` trả về đúng bản ghi
- `findAll()` trả về ORDER BY created_at DESC (mới nhất lên đầu)

---

## ═══ TUẦN 5 · Authentication & Quản lý người dùng/sản phẩm + AuditLogService

**Mục tiêu:** Đăng ký/đăng nhập/đăng xuất hoạt động · CRUD sản phẩm · AuditLogService ghi nhật ký xuyên suốt handlers

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | Password hashing — tại sao không lưu plaintext, SHA-256 là gì | Giải thích salt và hash |
| 2 | Token-based auth (UUID token) | Vẽ flow: Login → nhận token → gửi trong mỗi request |
| 3 | JavaFX Property Binding | Bind label text với StringProperty |
| 4 | SQL Injection — tại sao dùng PreparedStatement | Demo kịch bản injection |

---

### Đăng — AuthService (SHA-256) & SessionManager
```
Branch: feature/tuan-5-dang-auth-session
```

Tạo `AuthService.java` (package `com.bidhub.server.service`): method `hashPassword(String plain): String` — dùng `MessageDigest` SHA-256, kết quả là hex string 64 ký tự (dùng `HexFormat.of().formatHex()`). Method `verifyPassword(String plain, String hashed): boolean` — hash plain rồi `equals` hashed. Method `generateToken(): String` — `UUID.randomUUID().toString()`.

Tạo `SessionManager.java` (Singleton, package `com.bidhub.server.service`): 2 field `ConcurrentHashMap` — `tokenToUserId: Map<String, String>` và `userIdToToken: Map<String, String>`. Method `createSession(String userId): String` — generate token → lưu vào cả 2 map → trả về token. Method `invalidateSession(String token): void` — xóa khỏi cả 2 map. Method `getUserIdByToken(String token): Optional<String>` — tra `tokenToUserId`.

Cập nhật `RequestHandler.handle()`: sau khi parse request, lấy `token` từ `req.getToken()` → gọi `SessionManager.getUserIdByToken(token)` → nếu có → set `session.setAuthenticatedUserId(userId)`. Thực hiện trước khi vào switch-case.

**✅ Đầu ra:**
- `hashPassword("secret")` → chuỗi hex 64 ký tự
- `verifyPassword("secret", hash)` → true; `verifyPassword("wrong", hash)` → false
- `createSession("user-1")` → token UUID format
- `invalidateSession(token)` → `getUserIdByToken(token)` → `Optional.empty()`

---

### Quốc Minh — Login, Register, Logout + SecurityContext (+ 3 dòng AuditLog)
```
Branch: feature/tuan-5-quocminh-auth-handlers
```

Thêm vào `RequestHandler` các handler sau, mỗi cái trong một private method riêng:

`handleLogin(Session session, JsonNode payload)`: parse `{username, password}` từ payload → `UserDao.findByUsername()` → nếu empty → error. `AuthService.verifyPassword()` → nếu sai → error. Kiểm tra `user.isLocked()` (sẽ có sau Tuần 6 — Tuần 5 tạm bỏ qua hoặc thêm điều kiện rỗng). `SessionManager.createSession(userId)` → set `session.authenticatedUserId` → trả về payload `{token, userId, username, role}`. **[MỚI v3.1]** Ngay sau login thành công: `auditLogService.log(userId, AuditActions.USER_LOGIN, "{}");`.

`handleRegister(Session session, JsonNode payload)`: parse `{username, password, email, role}`. Validation đơn giản: `username` không blank, `password` ≥ 8 ký tự, `email` chứa "@", `role` không được là "ADMIN". `UserDao.existsByUsername()` → nếu tồn tại → `DuplicateUsernameException`. Hash password → tạo đúng subclass (Bidder/Seller) → save → trả về user info. **[MỚI v3.1]** Sau register thành công: `auditLogService.log(userId, AuditActions.USER_REGISTER, "{}");`.

`handleLogout(Session session, JsonNode payload)`: lấy token từ session → `SessionManager.invalidateSession(token)` → xóa `session.authenticatedUserId`. **[MỚI v3.1]** Trước khi invalidate: `auditLogService.log(userId, AuditActions.USER_LOGOUT, "{}");`.

Tạo `SecurityContext.java` (package `com.bidhub.server.network`): static method `requireAuthenticated(Session session): String` — lấy `authenticatedUserId` từ session, nếu null → ném `AuthenticationException("Bạn chưa đăng nhập")`, nếu có → trả về userId. Static method `requireRole(Session session, UserRole required): String` — gọi `requireAuthenticated()` → load user từ `UserDao` → kiểm tra role → nếu sai → ném `AuthenticationException("Không đủ quyền")`.

> [!NOTE]
> `AuditLogService` do Khoa viết trong cùng Tuần 5. Quốc Minh chỉ gọi `auditLogService.log(...)` — không cần biết implementation bên trong. Cần phối hợp với Khoa để merge `AuditLogService` vào branch trước khi Quốc Minh tạo PR.

**✅ Đầu ra:**
- Login đúng credentials → status="OK", payload có `token`
- Login sai password → status="ERROR"
- Register username đã tồn tại → error "USERNAME_TAKEN"
- Register role ADMIN → error bị từ chối
- Logout → token không còn hợp lệ trong SessionManager

---

### Công Minh — LoginView, RegisterView + CreateItemView hoàn chỉnh
```
Branch: feature/tuan-5-congminh-auth-item-ui
```

Hoàn thiện `LoginView.fxml` + `LoginController.java`: Button "Đăng nhập" bind `disableProperty` với `BooleanBinding` — disable khi username hoặc password rỗng. Khi click: tạo `NetworkTask` → `ServerGateway.sendRequest(loginRequest)` → nếu status="OK": `ClientSession.login(token, userId, username, role)` → `ViewRouter.navigateTo(Views.AUCTION_LIST)` → nếu "ERROR": hiện Label lỗi màu đỏ với message từ server.

Hoàn thiện `RegisterView.fxml` + `RegisterController.java`: form gồm TextField username, PasswordField password, PasswordField confirmPassword, TextField email, ChoiceBox role (BIDDER/SELLER). Bind realtime: Label cảnh báo hiện khi `password != confirmPassword`. Submit → `NetworkTask` gọi REGISTER → thành công → navigate về LoginView.

Hoàn thiện `CreateItemView.fxml` + `CreateItemController.java` (chỉ cho SELLER — nếu role khác → navigate về AuctionList): form gồm TextField name, TextArea description, TextField startingPrice, ChoiceBox itemType (ELECTRONICS/ART/VEHICLE). Khi chọn itemType → hiện thêm form phụ (brand/warrantyMonths hoặc artist/yearCreated hoặc manufacturer/year/mileage). Submit → `NetworkTask` gọi `CREATE_ITEM` → thành công → navigate về AuctionList.

Cập nhật `Views.java` thêm `REGISTER`, `CREATE_ITEM` nếu chưa có.

**✅ Đầu ra:**
- Đăng nhập đúng → chuyển AuctionListView
- Đăng nhập sai → Label lỗi đỏ
- Register confirmPassword không khớp → cảnh báo realtime trước khi submit
- CreateItem từ tài khoản SELLER → form hiện đúng trường phụ theo itemType

---

### Khoa — AuditLogService + Item Handlers + Auth Test Suite
```
Branch: feature/tuan-5-khoa-auditlog-service-handlers-tests
```

**[MỚI v3.1] AuditLogService:**

Tạo `AuditLogService.java` (package `com.bidhub.server.service`): field `auditLogDao: AuditLogDao`. Hai constructor: production (tạo `new AuditLogDao()`) và test (inject `AuditLogDao` từ ngoài). Method `log(String userId, String action, String details): void` — wrap toàn bộ trong try-catch, tạo `new AuditLog(userId, action, details)` → `auditLogDao.save(entry)`. Nếu exception → in `System.err`, không ném ra ngoài.

> [!IMPORTANT]
> `AuditLogService.log()` **không bao giờ được ném exception ra ngoài**. Audit log là chức năng phụ trợ — exception ở đây không được cản trở business logic chính (login, place bid...).

**[GIỮ ĐỦ] Item Handlers trong `RequestHandler`:**

`handleCreateItem(Session session, JsonNode payload)` (yêu cầu SELLER): `SecurityContext.requireRole(session, UserRole.SELLER)` → parse `{name, description, startingPrice, itemType, extras}` → `ItemCreator.forType(itemType).createItem(...)` → `itemDao.save(item)` → trả về item info. Sau khi save thành công: `auditLogService.log(sellerId, AuditActions.ITEM_CREATED, "{\"itemId\":\"" + item.getId() + "\"}")`.

`handleGetItemList(Session session, JsonNode payload)` (không cần auth): `itemDao.findAll()` → serialize → trả về list.

`handleGetItemDetail(Session session, JsonNode payload)`: parse `{itemId}` → `itemDao.findById()` → nếu empty → `AuctionNotFoundException` → trả về item detail.

`handleDeleteItem(Session session, JsonNode payload)` (yêu cầu auth + chỉ seller của item): `SecurityContext.requireAuthenticated()` → `itemDao.findById()` → kiểm tra `item.getSellerId().equals(userId)` → `itemDao.deleteById()` → `auditLogService.log(sellerId, AuditActions.ITEM_DELETED, ...)`.

Cập nhật `AUTH_REQUIRED` set trong `RequestHandler` thêm: `"CREATE_ITEM"`, `"DELETE_ITEM"`.

**[GIỮ ĐỦ] Auth Test Suite (≥ 20 cases):** login đúng, login sai password, login username không tồn tại, register mới thành công, register trùng username, register role ADMIN bị từ chối, register password < 8 ký tự, register email không hợp lệ, logout thành công → token hết hạn, token giả → rejected, type cần auth không có token → error.

**[MỚI v3.1] AuditLogService tests (≥ 5 cases):**
- `log()` với action hợp lệ → `auditLogDao.findAll()` có bản ghi mới
- `log()` khi DAO ném exception (inject mock) → không ném exception ra ngoài, không crash
- `log()` với `userId = null` (system action) → lưu được
- `log()` nhiều lần → `findAll()` trả về đúng số lượng
- `log()` với `details = "{}"` → bản ghi lưu đúng details

**✅ Đầu ra:**
- ≥ 25 test cases pass (20 auth + 5 AuditLogService)
- `handleCreateItem()` hoạt động: request CREATE_ITEM từ SELLER → item tạo được trong DB
- `AuditLogService.log()` gọi khi DB có sự cố → không crash handler
- `auditLogDao.findByAction("USER_LOGIN")` sau login thành công → có bản ghi

---

## ═══ TUẦN 6 · Core Bidding Engine + AdminUserService

**Mục tiêu:** Đặt giá được, kiểm tra luật, đóng phiên tự động · Admin quản lý khóa/mở tài khoản người dùng

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | ScheduledExecutorService | Viết task lặp mỗi 5s, biết cách cancel |
| 2 | JavaFX Timeline & KeyFrame | Countdown timer đếm ngược trên Label |
| 3 | Transactional thinking | Mô tả kịch bản 2 người đặt giá cùng lúc — điều gì sai |
| 4 | JavaFX TableView & ObservableList | Bind TableView, tự cập nhật khi list thay đổi |

---

### Đăng — AuctionManager, LifecycleTask & is_locked Support
```
Branch: feature/tuan-6-dang-auction-manager
```

Tạo `AuctionManager.java` (Singleton, package `com.bidhub.server.service`): field `auctions: ConcurrentHashMap<String, Auction>`, `scheduler: ScheduledExecutorService`. Method `getInstance()`, `start()` — schedule `AuctionLifecycleTask` chạy mỗi 5 giây, `addAuction(Auction a)`, `removeAuction(String auctionId)`, `getAuction(String auctionId): Optional<Auction>`, `getAllActive(): List<Auction>`. Khi server start, load tất cả RUNNING auction từ `AuctionDao` vào `auctions` map.

Tạo `AuctionLifecycleTask.java` (implements Runnable): duyệt `AuctionManager.getAllActive()`, với mỗi auction đã quá `endTime` → gọi `closeAuction(auction)`. Method `closeAuction(Auction a)`: gọi `a.transitionTo(AuctionStatus.FINISHED)` → `auctionDao.updateStatus(id, FINISHED)` → `bidDao.getHighestBid(id)` để tìm winner → nếu có winner → cập nhật `highestBidderId` trong DB → `AuctionManager.removeAuction(id)`.

**[MỚI v3.1] Thêm `is_locked` support vào `users` table và `UserDao`:**

Cập nhật `schema.sql` — thêm cột `is_locked INTEGER NOT NULL DEFAULT 0` vào bảng `users`. Cập nhật `MigrationRunner` thêm câu `ALTER TABLE users ADD COLUMN IF NOT EXISTS is_locked INTEGER NOT NULL DEFAULT 0` để tương thích nếu DB file đã tồn tại từ Tuần 3.

Cập nhật `UserDao.save()`: thêm cột `is_locked` vào câu INSERT (ghi `0` cho tài khoản mới). Cập nhật `UserDao.mapRow()`, `findById()`, `findByUsername()`, `findAll()`: đọc cột `is_locked` → gán vào field `locked: boolean` của User. Thêm method `UserDao.updateLocked(String userId, boolean locked): void` — `UPDATE users SET is_locked=?, updated_at=? WHERE id=?`. Gọi `user.markUpdated()` sau khi update.

Cập nhật `handleLogin()` trong `RequestHandler` (phối hợp Quốc Minh): sau khi `verifyPassword()` thành công, thêm: `if (user.isLocked()) return MessageMapper.toJson(MessageResponse.error("LOGIN", "TÀI KHOẢN BỊ KHÓA"));`.

**✅ Đầu ra:**
- `AuctionManager.getInstance()` gọi 2 lần → cùng instance
- `AuctionLifecycleTask` với auction có endTime = quá khứ → status chuyển thành FINISHED trong DB
- `userDao.updateLocked("userId", true)` → `findByUsername().isLocked()` == true
- User bị khóa cố login → response error "TÀI KHOẢN BỊ KHÓA"

---

### Quốc Minh — BidValidator & PlaceBid/AuctionList/Detail Handlers
```
Branch: feature/tuan-6-quocminh-bid-handler
```

Tạo `BidValidator.java` (package `com.bidhub.server.service`): method `validate(Auction auction, String bidderId, double bidAmount): void` — ném exception nếu vi phạm:
- `auction.getStatus() != AuctionStatus.RUNNING` → `AuctionClosedException`
- `bidderId.equals(auction.getHighestBidderId())` → `InvalidBidException("Bạn đang là người dẫn đầu")`
- `auction.getItemOwnerId()` (lấy từ ItemDao) == bidderId → `InvalidBidException("Seller không thể tự đấu giá")`
- `bidAmount <= auction.getCurrentHighestBid()` → `InvalidBidException("Giá phải cao hơn giá hiện tại")`
- `bidAmount - auction.getCurrentHighestBid() < auction.getMinimumIncrement()` → `InvalidBidException("Bước giá không đủ")`

Thêm vào `RequestHandler`:

`handlePlaceBid(Session session, JsonNode payload)`: `SecurityContext.requireAuthenticated()` → parse `{auctionId, bidAmount}` → `AuctionManager.getAuction(auctionId)` → nếu empty → error → `bidValidator.validate(...)` → tạo `BidTransaction` → `bidDao.save()` → cập nhật `auction.setCurrentHighestBid / setHighestBidderId` trong RAM → `auctionDao.updateHighestBid()` trong DB → publish `BidUpdateEvent` (Tuần 7 sẽ kết nối, Tuần 6 tạm stub) → trả về updated auction info.

`handleGetAuctionList(Session session, JsonNode payload)`: `auctionDao.findActiveAuctions()` → serialize → trả về list (kèm thông tin item tóm tắt).

`handleGetAuctionDetail(Session session, JsonNode payload)`: parse `{auctionId}` → `AuctionManager.getAuction()` hoặc `auctionDao.findById()` → trả về đầy đủ thông tin auction + item + bidHistory gần nhất.

Thêm các case vào switch trong `RequestHandler`: `"PLACE_BID"`, `"GET_AUCTION_LIST"`, `"GET_AUCTION_DETAIL"`. Cập nhật `AUTH_REQUIRED` thêm `"PLACE_BID"`.

Cập nhật `docs/API_PROTOCOL.md`: bổ sung examples cho PLACE_BID, GET_AUCTION_LIST, GET_AUCTION_DETAIL.

**✅ Đầu ra:**
- Bid thấp hơn giá hiện tại → `InvalidBidException`, response status="ERROR"
- Bid hợp lệ → `AuctionManager.getAuction(id).getCurrentHighestBid()` == bidAmount trong RAM
- `bidDao.findByAuctionId(id)` → có BidTransaction mới với đúng amount
- Seller tự bid → error bị từ chối

---

### Công Minh — AuctionListView, AuctionDetailView & Countdown Timer
```
Branch: feature/tuan-6-congminh-auction-ui
```

Hoàn thiện `AuctionListController.java`: trong `initialize()`, tạo `NetworkTask` gọi `GET_AUCTION_LIST` → nhận list → chuyển thành `ObservableList` → bind vào `TableView`. Cột: Tên sản phẩm, Giá hiện tại, Thời gian kết thúc, Trạng thái. Button "Tạo phiên đấu giá" chỉ visible khi `ClientSession.currentRole == SELLER`. Double-click vào hàng → `ViewRouter.navigateTo(Views.AUCTION_DETAIL, {auctionId})`. Button "Tạo sản phẩm" → navigate `CREATE_ITEM` (SELLER only).

Hoàn thiện `AuctionDetailController.java` (implements `ContextAware`): nhận `auctionId` từ context → `NetworkTask` gọi `GET_AUCTION_DETAIL` → populate các Label (tên item, mô tả, giá khởi điểm, giá hiện tại, người dẫn đầu). Countdown timer dùng `javafx.animation.Timeline` + `KeyFrame` mỗi 1 giây → cập nhật Label "Còn lại: HH:mm:ss". Khi auction FINISHED → hiện Label "ĐÃ KẾT THÚC", vô hiệu hóa nút đặt giá. Form đặt giá: TextField bidAmount, Button "Đặt giá" → `NetworkTask` gọi `PLACE_BID` → nếu OK: reload detail; nếu ERROR: hiện Alert lỗi.

Tạo `CreateAuctionView.fxml` + `CreateAuctionController.java` (cho SELLER): form chọn Item từ danh sách (ChoiceBox), nhập startingPrice, chọn startTime + endTime (DatePicker + Spinner giờ), minimumIncrement → Submit → gọi handler `CREATE_AUCTION` (Tuần 6 implement phía server hoặc để Tuần 7 — Công Minh tạo UI trước).

**✅ Đầu ra:**
- AuctionList load data từ server, hiển thị đúng thông tin
- Countdown đếm ngược đúng theo endTime của auction
- Button "Tạo phiên" chỉ hiện với SELLER, ẩn với BIDDER
- Đặt giá thành công → giá hiện tại trên AuctionDetail cập nhật

---

### Khoa — AdminUserService + Handlers + Bid Test Suite
```
Branch: feature/tuan-6-khoa-admin-user-service-bid-tests
```

**[MỚI v3.1] AdminUserService:**

Tạo `AdminUserService.java` (package `com.bidhub.server.service`): field `userDao: UserDao`, `auditLogService: AuditLogService`. Hai constructor: production và test (inject). Các method:

| Method | Logic |
|--------|-------|
| `listAllUsers(): List<User>` | `userDao.findAll()` — trả về toàn bộ danh sách |
| `lockUser(String targetId, String adminId)` | findById → nếu không tìm thấy → `UserNotFoundException` · nếu target là ADMIN → `ValidationException("Không thể khóa Admin")` · `userDao.updateLocked(targetId, true)` · log `USER_LOCKED` |
| `unlockUser(String targetId, String adminId)` | findById → nếu không tìm thấy → `UserNotFoundException` · `userDao.updateLocked(targetId, false)` · log `USER_UNLOCKED` |

Thêm vào `RequestHandler` 3 handler mới (đều yêu cầu role ADMIN):

`handleGetUserList()`: `SecurityContext.requireRole(session, UserRole.ADMIN)` → `adminUserService.listAllUsers()` → serialize list (mỗi user: id, username, email, role, isLocked) → trả về.

`handleLockUser()`: parse `{targetUserId}` → `adminUserService.lockUser(targetUserId, adminUserId)` → trả về `{"message": "Đã khóa tài khoản"}`.

`handleUnlockUser()`: parse `{targetUserId}` → `adminUserService.unlockUser(targetUserId, adminUserId)` → trả về `{"message": "Đã mở khóa tài khoản"}`.

Thêm các case vào switch: `"GET_USER_LIST"`, `"LOCK_USER"`, `"UNLOCK_USER"`. Cập nhật `AUTH_REQUIRED`.

Cập nhật `docs/API_PROTOCOL.md` (phối hợp Quốc Minh review): bổ sung 3 example mới cho GET_USER_LIST, LOCK_USER, UNLOCK_USER.

**[GIỮ ĐỦ] Bid Test Suite (≥ 15 cases):** bid hợp lệ tăng giá, bid thấp hơn → exception, bid đúng bằng giá hiện tại → exception, bid khi auction FINISHED → exception, seller tự bid → exception, bidAmount âm → exception, auctionId không tồn tại → exception, người đang dẫn đầu bid lại → exception, bid với minimumIncrement không đủ → exception.

**[MỚI v3.1] AdminUserService tests (≥ 8 cases):**
- `listAllUsers()` sau save 3 users → trả về 3 user
- `lockUser()` → `userDao.findByUsername().isLocked()` == true
- `unlockUser()` sau `lockUser()` → `isLocked()` == false
- `lockUser()` với ID không tồn tại → `UserNotFoundException`
- `lockUser()` trên tài khoản Admin → `ValidationException`
- `lockUser()` → `auditLogDao.findByAction("USER_LOCKED")` có bản ghi mới
- User bị lock, thử login → response error "TÀI KHOẢN BỊ KHÓA"
- `handleGetUserList()` từ session BIDDER (non-Admin) → `AuthenticationException`

**✅ Đầu ra:**
- ≥ 23 test cases pass (15 bid + 8 admin)
- `handleGetUserList()` từ ADMIN → danh sách đầy đủ users với isLocked status
- `handleLockUser()` → user bị khóa không thể login nữa
- Admin không thể bị khóa bởi chính tính năng này

---

## ═══ TUẦN 7 · Concurrency & Realtime Observer + ReportService

**Mục tiêu:** ReentrantLock chống lost update · Observer push bid realtime → 1.5đ kỹ thuật · ReportService xuất báo cáo

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | ReentrantLock vs synchronized | Giải thích 2 điểm khác biệt |
| 2 | Race condition & Lost Update | Vẽ timeline 2 thread cùng bid, chỉ ra vấn đề |
| 3 | Observer / Pub-Sub Pattern | Vẽ UML, phân biệt Observer và Pub-Sub |
| 4 | Platform.runLater() | Điều gì xảy ra nếu cập nhật UI từ background thread |

---

### Đăng — ReentrantLock Granular Locking
```
Branch: feature/tuan-7-dang-reentrantlock
```

Cập nhật `Auction.java`: thêm field `lock: ReentrantLock` (khởi tạo `new ReentrantLock()`). Thêm method `getLock(): ReentrantLock`.

Cập nhật `handlePlaceBid()` trong `RequestHandler` (phối hợp Quốc Minh): toàn bộ logic validate + save + update phải nằm trong khối:

```
auction.getLock().lock();
try {
    bidValidator.validate(...);
    // tạo BidTransaction → save DB → cập nhật RAM
} finally {
    auction.getLock().unlock();
}
```

> [!WARNING]
> ReentrantLock **PHẢI** trong try-finally. Quên `unlock()` trong finally → deadlock khi exception → toàn bộ auction bị treo mãi.

Đảm bảo `handlePlaceBid()` vẫn gọi `NotificationBroker.publish(BidUpdateEvent)` sau khi unlock (Quốc Minh viết NotificationBroker trong cùng tuần). Phối hợp thứ tự merge: Đăng merge trước, Quốc Minh rebase.

**✅ Đầu ra:**
- 2 threads cùng bid vào auction → chỉ 1 thắng, `currentHighestBid` nhất quán giữa RAM và DB
- Không deadlock: test dùng try-finally đúng chỗ

---

### Quốc Minh — NotificationBroker & Observer Events
```
Branch: feature/tuan-7-quocminh-notification-broker
```

Tạo `BidUpdateEvent.java` (package `com.bidhub.server.event`): field `auctionId`, `bidderId`, `bidAmount`, `timestamp`. Constructor, getters.

Tạo `AuctionClosedEvent.java`: field `auctionId`, `winnerId` (nullable), `winningBid`.

Tạo `NotificationBroker.java` (Singleton, Observer Pattern): field `subscribers: ConcurrentHashMap<String, CopyOnWriteArrayList<Session>>` — key là `auctionId`. Method `subscribe(String auctionId, Session session)` thêm session vào list. Method `unsubscribe(String auctionId, Session session)` xóa session. Method `unsubscribeAll(Session session)` xóa session khỏi tất cả auction — gọi khi session ngắt kết nối. Method `publish(String auctionId, Object event)` — duyệt list, serialize event thành JSON, gọi `session.sendMessage()` từng session (bắt IOException nếu session đã mất).

Thêm handler `handleSubscribeAuction(Session session, JsonNode payload)` vào `RequestHandler`: parse `{auctionId}` → `NotificationBroker.subscribe(auctionId, session)` → trả về OK. Thêm case `"SUBSCRIBE_AUCTION"` vào switch.

Cập nhật `handlePlaceBid()` sau khi unlock: `NotificationBroker.getInstance().publish(auctionId, new BidUpdateEvent(...))`.

Cập nhật `AuctionLifecycleTask.closeAuction()`: sau khi FINISHED → `NotificationBroker.getInstance().publish(auctionId, new AuctionClosedEvent(...))`.

Cập nhật `ClientConnectionThread` cleanup: khi session ngắt → `NotificationBroker.getInstance().unsubscribeAll(session)`.

**✅ Đầu ra:**
- Client subscribe → server publish → client nhận message JSON qua socket
- 2 sessions cùng subscribe cùng auctionId → cả 2 đều nhận khi có bid mới
- Session mất kết nối → `unsubscribeAll` không crash server

---

### Công Minh — EventListenerThread & Realtime UI Update
```
Branch: feature/tuan-7-congminh-realtime-client
```

Tạo interface `BidUpdateCallback` (package `com.bidhub.client.network`): method `onBidUpdate(String eventJson): void`.

Tạo `EventListenerThread.java` (implements Runnable, package `com.bidhub.client.network`): nhận riêng `Socket` thứ 2 (hoặc tái dùng socket chính trên luồng khác), field `callback: BidUpdateCallback`. Vòng lặp đọc từng dòng JSON → phân loại event (dựa theo trường `type` hoặc `eventType`) → gọi `callback.onBidUpdate(json)`.

> [!NOTE]
> Cách đơn giản: ServerGateway dùng 1 socket và phân biệt response từ PUSH event bằng trường `eventType`. EventListenerThread chạy trong thread riêng, đọc liên tục, phân loại rồi dispatch đến callback.

Cập nhật `AuctionDetailController.java`: khi mở màn hình → gửi request `SUBSCRIBE_AUCTION` → khởi động `EventListenerThread` với callback xử lý `BID_UPDATE` event: dùng `Platform.runLater(() -> { updateCurrentBidLabel(event); updateBidderLabel(event); })`. Khi nhận `AUCTION_CLOSED` event → `Platform.runLater(() -> { showFinishedLabel(); disableBidButton(); })`. Khi rời màn hình (navigate away) → dừng EventListenerThread.

**✅ Đầu ra:**
- Client A đặt giá → Client B đang xem cùng auction nhận BID_UPDATE → Label giá cập nhật không cần refresh
- Client B nhận AUCTION_CLOSED → nút đặt giá bị vô hiệu hóa tự động
- `Platform.runLater()` đúng chỗ — không có `IllegalStateException` từ JavaFX thread

---

### Khoa — ReportService + Handlers + Stress Test
```
Branch: feature/tuan-7-khoa-report-service-stress-test
```

**[GIỮ ĐỦ] Stress Test Concurrent Bidding:**

Tạo `ConcurrentBidTest.java` (package test): 50 threads, dùng `CountDownLatch` để đồng bộ thời điểm bắt đầu bid, `ExecutorService.awaitTermination()` chờ xong. Mỗi thread gọi `handlePlaceBid()` với bidAmount tăng dần. Assert: `auction.getCurrentHighestBid()` trong RAM == highest bid trong DB (qua `bidDao.getHighestBid()`), không xảy ra `NullPointerException` hay deadlock, tổng số BidTransaction trong DB đúng số bid hợp lệ (chỉ bid cao hơn bid trước mới được lưu).

**[MỚI v3.1] ReportService:**

Tạo `ReportService.java` (package `com.bidhub.server.service`): field `auctionDao: AuctionDao`, `bidDao: BidDao`, `auditLogDao: AuditLogDao`. Hai constructor: production và test (inject). Các method:

| Method | Return | Mô tả |
|--------|--------|-------|
| `exportAuctionReport()` | `List<Map<String, Object>>` | Mỗi phần tử là 1 auction: auctionId, itemId, status, startingPrice, currentHighestBid, highestBidderId, startTime, endTime |
| `exportBidHistory(String auctionId)` | `List<Map<String, Object>>` | Mỗi phần tử là 1 BidTransaction: bidId, bidderId, bidAmount, bidTime — sắp xếp ASC theo bidTime |
| `exportAuditLog(int limit)` | `List<Map<String, Object>>` | Gọi `auditLogDao.findRecent(limit)` → map sang dạng flat (id, userId, action, details, createdAt) |

> [!NOTE]
> `AuctionDao.findAll()` chưa có trong Tuần 3. Khoa thêm method này vào `AuctionDao` (Đăng review PR).

Thêm vào `RequestHandler` 3 handler mới:

`handleGetAuctionReport()` (SELLER hoặc ADMIN): `SecurityContext.requireAuthenticated()` → kiểm tra role → `reportService.exportAuctionReport()` → trả về JSON array.

`handleGetBidHistoryReport()` (auth): parse `{auctionId}` → `reportService.exportBidHistory(auctionId)` → trả về list.

`handleGetAuditLog()` (ADMIN only): parse `{limit}` (default 50 nếu thiếu) → `reportService.exportAuditLog(limit)` → trả về list.

Thêm cases: `"GET_AUCTION_REPORT"`, `"GET_BID_HISTORY_REPORT"`, `"GET_AUDIT_LOG"`.

**[MỚI v3.1] ReportService tests (≥ 6 cases):**
- `exportAuctionReport()` sau save 3 auctions → 3 phần tử trong list
- `exportBidHistory(auctionId)` trả về đúng bids ORDER BY bidTime ASC
- `exportBidHistory(auctionId)` với auction không có bid → list rỗng, không exception
- `exportAuditLog(5)` với 10 bản ghi → tối đa 5 phần tử
- Map keys trong `exportAuctionReport()` phải có đủ: auctionId, status, currentHighestBid
- `exportAuctionReport()` sau auction chuyển FINISHED → status='FINISHED' trong kết quả

**✅ Đầu ra:**
- 50-thread stress test → không deadlock, RAM và DB nhất quán sau khi tất cả threads hoàn tất
- ≥ 6 ReportService test cases pass
- `handleGetAuctionReport()` từ ADMIN → danh sách đầy đủ auctions
- `handleGetBidHistoryReport()` với auctionId hợp lệ → bid list đúng thứ tự

---

## ═══ TUẦN 8 · Tính năng nâng cao (Anti-Sniping + Price Chart + AuditLog Integration)

**Mục tiêu:** Anti-Sniping +0.5đ · Price Chart +0.5đ · Tích hợp AuditLog vào toàn bộ lifecycle

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | JavaFX LineChart, XYChart.Series, XYChart.Data | Thêm 1 data point vào LineChart bằng code |
| 2 | Anti-sniping concept (eBay) | Giải thích logic gia hạn — tại sao cần threshold |
| 3 | LocalDateTime arithmetic | Dùng `endTime.minusSeconds(60)`, `endTime.plusSeconds(60)` |

---

### Đăng + Quốc Minh — Anti-Sniping Engine
```
Branch: feature/tuan-8-antisniping
```

Tạo `AuctionExtendedEvent.java` (package `com.bidhub.server.event`): field `auctionId`, `newEndTime: LocalDateTime`. Constructor, getters.

Tạo `AntiSnipingEngine.java` (package `com.bidhub.server.service`): method `check(Auction auction): void`. Logic: đọc `threshold = ConfigLoader.getInt("snipe.threshold")` (mặc định 60 giây) và `extension = ConfigLoader.getInt("snipe.extension")` (mặc định 60 giây). Tính `snipeWindow = auction.getEndTime().minusSeconds(threshold)`. Nếu `LocalDateTime.now().isAfter(snipeWindow)` (đang trong vùng nguy hiểm) → tính `newEndTime = auction.getEndTime().plusSeconds(extension)` → `auction.setEndTime(newEndTime)` → `auctionDao.updateEndTime(auction.getId(), newEndTime)` → `NotificationBroker.publish(auctionId, new AuctionExtendedEvent(auctionId, newEndTime))`.

Cập nhật `handlePlaceBid()` trong `RequestHandler`: sau khi bid lưu thành công và `BidUpdateEvent` publish → gọi `AntiSnipingEngine.check(auction)`.

Cập nhật `AuctionDetailController.java` (Công Minh cùng review): khi nhận `AUCTION_EXTENDED` event → lấy `newEndTime` → reset countdown timer với endTime mới → hiện thông báo "Phiên được gia hạn thêm 60 giây".

**✅ Đầu ra:**
- Bid khi còn < 60s → `auction.getEndTime()` trong RAM và DB tăng thêm 60s
- Client đang xem nhận `AUCTION_EXTENDED` event → countdown reset về giá trị mới
- Bid khi còn > 60s → endTime không thay đổi

---

### Công Minh — Price Chart (LineChart Realtime)
```
Branch: feature/tuan-8-congminh-price-chart
```

Tạo `BidChartService.java` (package `com.bidhub.client.service`): field `series: XYChart.Series<String, Number>`. Method `addDataPoint(LocalDateTime time, double price)` — tạo `XYChart.Data<String, Number>` với x = `time.format(HH:mm:ss)`, y = price → thêm vào `series`. Method `clearData()` — xóa toàn bộ data point. Method `getSeries(): XYChart.Series<String, Number>`.

Cập nhật `AuctionDetailView.fxml` (Scene Builder): thêm `LineChart<String, Number>` bên dưới thông tin auction. `CategoryAxis` (trục X, label "Thời gian"), `NumberAxis` (trục Y, label "Giá đấu (VNĐ)"). Chart `animated="false"` để tránh lag khi update liên tục.

Cập nhật `AuctionDetailController.java`: trong `initialize()` → `chartService.clearData()` → gán `series` vào `lineChart.getData()`. Load lịch sử bid ban đầu: gọi `GET_BID_HISTORY` → với mỗi BidTransaction → `Platform.runLater(() -> chartService.addDataPoint(bidTime, bidAmount))`. Cập nhật callback `BidUpdateCallback.onBidUpdate()`: khi nhận `BID_UPDATE` → `Platform.runLater(() -> chartService.addDataPoint(...))`.

**✅ Đầu ra:**
- Mở AuctionDetail → LineChart hiện đúng lịch sử bid (nếu có)
- Client đặt giá mới → data point mới xuất hiện trên chart không cần refresh
- Chart không crash khi bid đầu tiên (empty series)

---

### Khoa — AuditLog Event Integration + Test Suite
```
Branch: feature/tuan-8-khoa-auditlog-integration-tests
```

**[MỚI v3.1] Tích hợp AuditLog vào 3 điểm sự kiện quan trọng:**

Cập nhật `handlePlaceBid()` trong `RequestHandler` — thêm sau khi bid thành công (bên trong khối lock, sau khi lưu DB): `auditLogService.log(bidderId, AuditActions.PLACE_BID, "JSON với auctionId và bidAmount")`.

Cập nhật `AuctionLifecycleTask.closeAuction()` — thêm sau khi chuyển trạng thái FINISHED: `auditLogService.log(null, AuditActions.AUCTION_CLOSED, "JSON với auctionId, winnerId, winningBid")`.

Cập nhật `AntiSnipingEngine.check()` — thêm sau khi gia hạn thành công: `auditLogService.log(null, AuditActions.AUCTION_EXTENDED, "JSON với auctionId và newEndTime")`.

> [!TIP]
> Khoa chỉ thêm 2–3 dòng vào mỗi handler/task. Không thay đổi logic chính. Tạo branch riêng, PR nhỏ, dễ review.

**[GIỮ ĐỦ] Anti-Sniping Tests (≥ 5 cases):**
- Bid khi còn < threshold → `endTime` tăng đúng `extension` giây
- Bid khi còn > threshold → `endTime` không thay đổi
- Threshold và extension đọc từ ConfigLoader (kiểm tra configurable)
- `AntiSnipingEngine.check()` publish `AuctionExtendedEvent` khi trigger
- `AuctionExtendedEvent` chứa đúng `newEndTime`

**[MỚI v3.1] Audit Log Integration Tests (≥ 5 cases):**
- Sau khi `handlePlaceBid()` thành công → `auditLogDao.findByAction("PLACE_BID")` có bản ghi
- `closeAuction()` → `auditLogDao.findByAction("AUCTION_CLOSED")` có bản ghi
- Anti-sniping trigger → `auditLogDao.findByAction("AUCTION_EXTENDED")` có bản ghi
- PLACE_BID audit log chứa đúng auctionId và bidAmount trong details
- `handleGetAuditLog(admin)` → trả về list bao gồm cả PLACE_BID và AUCTION_CLOSED entries

**✅ Đầu ra:**
- ≥ 10 test cases pass (5 anti-sniping + 5 audit integration)
- Sau khi đặt giá → `auditLogDao.findByAction("PLACE_BID")` có bản ghi với đúng auctionId
- `handleGetAuditLog()` từ ADMIN → danh sách đầy đủ các sự kiện hệ thống

---

## ═══ TUẦN 9 · Integration & Chất lượng mã + DataIntegrityService

**Mục tiêu:** Toàn hệ thống chạy end-to-end · Test ≥ 139 cases · CI xanh · DataIntegrityService kiểm tra chéo dữ liệu

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | SLF4J + Logback: Logger, log levels | Thay 1 `System.out.println` bằng `logger.info()` |
| 2 | Integration test vs Unit test | Giải thích sự khác biệt trong context BidHub |
| 3 | JaCoCo code coverage | Cài plugin, chạy `mvn verify` xem report |

---

### Đăng — Integration Test & Final Refactor
```
Branch: feature/tuan-9-dang-integration
```

Tạo `IntegrationTest.java` (trong thư mục `test`): khởi động `SocketServerCore` trên port test (9091), kết nối `ServerGateway` phía client. Chạy kịch bản end-to-end đầy đủ: register user A (SELLER), register user B (BIDDER), login cả 2 → nhận token. A tạo Item → A tạo Auction với startTime = now, endTime = now + 5 phút. B subscribe auction → B place bid → kiểm tra server response OK. Đợi `BidUpdateEvent` phía B (dùng `CountDownLatch` timeout 3s). Chờ auction lifecycle (mock endTime = now + 2s) → auction chuyển FINISHED → đảm bảo B nhận `AuctionClosedEvent`. Dọn dẹp: dừng server, xóa DB test.

Refactor toàn server: thay tất cả `System.out.println` và `System.err.println` bằng `SLF4J` logger (`LoggerFactory.getLogger(ClassName.class)`). Thêm `logback.xml` trong `resources` với appender Console và FILE (level INFO). Dọn code: xóa comment thừa, đặt tên method/variable đúng Google Java Style Guide (camelCase, không abbreviation mù quáng).

Kiểm tra `MigrationRunner` xử lý đủ 5 bảng. Đảm bảo `AuctionManager` load lại RUNNING auctions từ DB khi server restart (quan trọng cho tính liên tục).

**✅ Đầu ra:**
- Integration test pass end-to-end (kịch bản register → login → bid → close)
- 0 `System.out.println` trong production code (`grep -r "System.out" src/main` → 0 kết quả)
- SLF4J logger hoạt động, log file được tạo

---

### Quốc Minh — API Protocol Docs Final + CI/CD
```
Branch: feature/tuan-9-quocminh-docs-cicd
```

Hoàn thiện `docs/API_PROTOCOL.md` với đầy đủ ≥ 14 command types, mỗi loại có ví dụ request JSON và response JSON:

| # | Type | Mô tả |
|---|------|-------|
| 1 | PING | Health check |
| 2 | LOGIN | Đăng nhập |
| 3 | REGISTER | Đăng ký tài khoản |
| 4 | LOGOUT | Đăng xuất |
| 5 | CREATE_ITEM | Tạo sản phẩm mới (SELLER) |
| 6 | GET_ITEM_LIST | Lấy danh sách sản phẩm |
| 7 | GET_ITEM_DETAIL | Chi tiết sản phẩm |
| 8 | DELETE_ITEM | Xóa sản phẩm |
| 9 | GET_AUCTION_LIST | Danh sách auction đang chạy |
| 10 | GET_AUCTION_DETAIL | Chi tiết auction |
| 11 | PLACE_BID | Đặt giá |
| 12 | SUBSCRIBE_AUCTION | Đăng ký nhận realtime update |
| 13 | GET_USER_LIST | Danh sách người dùng (ADMIN) |
| 14 | LOCK_USER | Khóa tài khoản (ADMIN) |
| 15 | UNLOCK_USER | Mở khóa tài khoản (ADMIN) |
| 16 | GET_AUCTION_REPORT | Báo cáo auctions (ADMIN/SELLER) |
| 17 | GET_BID_HISTORY_REPORT | Lịch sử bid của 1 auction |
| 18 | GET_AUDIT_LOG | Lịch sử audit (ADMIN) |
| 19 | RUN_INTEGRITY_CHECK | Kiểm tra toàn vẹn dữ liệu (ADMIN) |

Cập nhật CI `ci.yml`: thêm bước `--fail-at-end` cho `mvn test` (tất cả test chạy dù có fail, báo cáo đầy đủ). Thêm JaCoCo Maven plugin, bước `mvn jacoco:report`, upload `target/site/jacoco` artifact. Thêm badge CI vào `README.md`.

**✅ Đầu ra:**
- `API_PROTOCOL.md` đủ ≥ 14 command types với ví dụ request/response
- CI push → build pass, JaCoCo report được upload
- Badge CI xanh hiện trên README

---

### Công Minh — UI Polish & UX
```
Branch: feature/tuan-9-congminh-ui-polish
```

Thêm loading state cho tất cả `NetworkTask`: spinner `ProgressIndicator` visible khi task đang chạy, invisible khi xong. Dùng `task.setOnRunning()` để show spinner, `task.setOnSucceeded() / setOnFailed()` để hide. Button submit disable khi task đang chạy (tránh double-submit).

Xử lý edge cases trong từng màn hình:
- `AuctionListView`: list trống → hiện Label "Chưa có phiên đấu giá nào". Load thất bại → Alert "Không thể tải dữ liệu".
- `AuctionDetailView`: bid thất bại (server trả ERROR) → hiện Alert với message lỗi từ server. Countdown dừng khi nhận `AUCTION_CLOSED`. TextField bidAmount chỉ cho phép nhập số (bind `textProperty().addListener`).
- `CreateItemView`: validate form phía client trước khi gửi (không để server validation là tuyến phòng thủ duy nhất).

Kiểm tra UI trên độ phân giải 1366×768 (laptop phổ thông): không có phần tử bị tràn hoặc bị che. Cập nhật CSS stylesheet nếu cần (font, màu sắc, padding).

**✅ Đầu ra:**
- Tất cả NetworkTask có loading state đúng cách
- Không crash khi data trống, khi server trả error
- AuctionDetail hoạt động đúng khi auction FINISHED: nút bị disable, countdown dừng

---

### Khoa — DataIntegrityService + Full Test Suite
```
Branch: feature/tuan-9-khoa-integrity-service-full-tests
```

**[MỚI v3.1] DataIntegrityService:**

Tạo `DataIntegrityService.java` (package `com.bidhub.server.service`): field `auctionDao`, `bidDao`, `itemDao`, `userDao`. Hai constructor: production và test (inject). Các method:

| Method | Return | Logic |
|--------|--------|-------|
| `checkBidConsistency()` | `List<String>` | So sánh `currentHighestBid` trong bảng `auctions` với bid cao nhất trong `bid_transactions` — phát hiện chênh lệch |
| `checkAuctionWinners()` | `List<String>` | Auction FINISHED mà có bids nhưng `highestBidderId` = null → bất thường |
| `checkOrphanedItems()` | `List<String>` | Item trong `items` mà `sellerId` không tồn tại trong `users` |
| `runFullCheck()` | `Map<String, Object>` | Chạy cả 3 kiểm tra → trả về map với keys: `bidConsistencyErrors`, `auctionWinnerErrors`, `orphanedItemErrors`, `totalErrors`, `status` ("OK" hoặc "ERRORS_FOUND") |

> [!TIP]
> `DataIntegrityService` chỉ READ dữ liệu, không ghi. An toàn để chạy bất cứ lúc nào mà không lo side effect.

Thêm vào `RequestHandler` 1 handler mới:

`handleRunIntegrityCheck()` (ADMIN only): `SecurityContext.requireRole(session, UserRole.ADMIN)` → `dataIntegrityService.runFullCheck()` → `auditLogService.log(adminId, "DATA_INTEGRITY_CHECK", "totalErrors=" + count)` → trả về report map.

Thêm case `"RUN_INTEGRITY_CHECK"` vào switch.

**[GIỮ ĐỦ] Full Test Suite ≥ 139 cases:**

Tổng hợp và xác nhận tất cả test từ các tuần:

| Tuần | Test classes | Cases ước tính |
|------|-------------|----------------|
| T1 | CalculatorTest | 15 |
| T2 | EntityTest, UserHierarchyTest, ItemCreatorTest, ExceptionTest | ≥ 14 |
| T3 | DatabaseSetupTest, UserDaoTest, ItemDaoTest, AuctionDaoTest, BidDaoTest | ≥ 21 |
| T4 | RequestHandlerTest, MessageMapperTest, AuditLogDaoTest | ≥ 15 |
| T5 | AuthTest, AuditLogServiceTest | ≥ 25 |
| T6 | BidValidatorTest, AdminUserServiceTest | ≥ 23 |
| T7 | ReportServiceTest, ConcurrentBidTest | ≥ 6 |
| T8 | AntiSnipingTest, AuditLogIntegrationTest | ≥ 10 |
| T9 | DataIntegrityServiceTest | ≥ 10 |
| **Tổng** | | **≥ 139** |

**[MỚI v3.1] DataIntegrityService tests (≥ 10 cases):**
- `checkBidConsistency()` khi auction và bids nhất quán → list rỗng
- `checkBidConsistency()` khi `currentHighestBid` trong auctions khác bid cao nhất trong bid_transactions → phát hiện lỗi
- `checkAuctionWinners()` auction FINISHED có bids nhưng `highestBidderId` = null → phát hiện lỗi
- `checkAuctionWinners()` auction FINISHED không có bids (không ai đặt) → không lỗi
- `checkOrphanedItems()` khi item có `sellerId` không tồn tại → phát hiện lỗi
- `checkOrphanedItems()` khi tất cả sellers tồn tại → list rỗng
- `runFullCheck()` khi không có lỗi → `totalErrors` = 0, `status` = "OK"
- `runFullCheck()` khi có lỗi → `status` = "ERRORS_FOUND"
- `handleRunIntegrityCheck()` từ BIDDER (non-Admin) → `AuthenticationException`
- `handleRunIntegrityCheck()` từ Admin → response chứa key `totalErrors`

**✅ Đầu ra:**
- ≥ 139 total test cases, `mvn test` → 0 failures
- `CI` push → build xanh ổn định
- `handleRunIntegrityCheck()` từ ADMIN → báo cáo tổng hợp đầy đủ
- `checkBidConsistency()` phát hiện đúng inconsistency khi dữ liệu cố ý sai

---

## ═══ TUẦN 10 · Docs, AdminView UI, Demo & Nộp bài

**Mục tiêu:** Demo trơn tru 10 phút · Docs đầy đủ · AdminView hoàn chỉnh · Mỗi người giải thích được BẤT KỲ dòng code nào

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | Mermaid sequence diagram | Vẽ được Login flow bằng Mermaid |
| 2 | Git tag và GitHub Releases | Tạo tag v1.0.0, push, tạo Release trên GitHub |

---

### Đăng — Deployment Docs & Server Final Validation
```
Branch: feature/tuan-10-dang-final
```

Tạo `docs/DEPLOYMENT.md`: hướng dẫn đầy đủ để người mới có thể chạy hệ thống từ đầu. Bao gồm: yêu cầu môi trường (JDK 21+, Maven 3.8+), bước build (`mvn clean package`), cách khởi động server (`java -jar bidhub-server.jar` hoặc `mvn exec:java`), cách khởi động client (`mvn javafx:run -pl bidhub-client`), cấu hình `server.properties` (port, db path, snipe threshold/extension), hướng dẫn tạo DB lần đầu (MigrationRunner tự động), troubleshooting phổ biến (port bị chiếm → thay port, DB bị corrupt → xóa file .db).

Xác nhận `MigrationRunner` xử lý đúng 5 bảng khi khởi động từ DB trống và từ DB đã tồn tại (cột `is_locked` cần `ALTER TABLE IF NOT EXISTS`). Kiểm tra `AuctionManager.start()` load RUNNING auctions từ DB khi server restart — đảm bảo không mất auction đang diễn ra.

Thêm handler `handleHealthCheck(Session session, JsonNode payload)`: không cần auth, trả về `{status: "OK", uptime: ..., activeAuctions: count, activeSessions: count}`. Thêm case `"HEALTH_CHECK"` vào switch. Hữu ích khi demo để giảng viên thấy server đang chạy tốt.

Tạo `README.md` hoàn chỉnh ở root: mô tả project, badge CI, sơ đồ kiến trúc tóm tắt (text-art), link tới DEPLOYMENT.md và API_PROTOCOL.md, danh sách tính năng đã implement.

**✅ Đầu ra:**
- `DEPLOYMENT.md` đủ để người ngoài nhóm chạy được hệ thống
- Server restart → RUNNING auctions vẫn tồn tại, không mất
- `HEALTH_CHECK` handler trả về thông tin server
- `README.md` có đủ badge CI và link tài liệu

---

### Quốc Minh — Sequence Diagrams & Design Pattern Docs
```
Branch: feature/tuan-10-quocminh-diagrams
```

Tạo `docs/DESIGN_PATTERNS.md` với 3 pattern đã áp dụng, mỗi pattern gồm: danh sách class áp dụng, lợi ích trong context BidHub, và sequence diagram Mermaid.

**Singleton Pattern:** Các class áp dụng: `DbConnectionProvider`, `AuctionManager`, `SessionManager`, `NotificationBroker`, `ViewRouter`, `ServerGateway`, `ClientSession`. Lợi ích: đảm bảo chỉ có 1 instance kết nối DB / 1 broker quản lý subscriber toàn hệ thống. Ghi chú: `AuditLogService` dùng Singleton cho production nhưng cho phép Dependency Injection trong test — đây là best practice.

**Factory Method Pattern (chuẩn GoF):** Creator: `ItemCreator` (abstract). ConcreteCreator: `ElectronicsCreator`, `ArtCreator`, `VehicleCreator`. Product: `Item` (abstract). ConcreteProduct: `Electronics`, `Art`, `Vehicle`. Factory Method: `ItemCreator.createItem(...)`. Lợi ích: Open/Closed Principle — thêm `ItemType.JEWELRY` chỉ cần `JewelryCreator`, không sửa code hiện có. Sequence diagram: Client → `ItemCreator.forType(ELECTRONICS)` → ElectronicsCreator → `createItem(...)` → Electronics instance.

**Observer Pattern:** Subject/Broker: `NotificationBroker`. Observer: `Session`. Events: `BidUpdateEvent`, `AuctionClosedEvent`, `AuctionExtendedEvent`. Lợi ích: push realtime đến tất cả client đang xem phiên mà không cần polling. Sequence diagram: `handlePlaceBid()` → lock → save → unlock → `NotificationBroker.publish(BidUpdateEvent)` → duyệt subscribers → `session.sendMessage()`.

Tạo ≥ 3 sequence diagram (Mermaid trong Markdown): Login flow, PlaceBid flow (với ReentrantLock + Observer), Anti-Sniping flow.

**✅ Đầu ra:**
- `DESIGN_PATTERNS.md` có đủ 3 pattern, mỗi pattern có sequence diagram
- Diagram render được trong GitHub Markdown (verify bằng GitHub preview)

---

### Công Minh — AdminView UI + Demo Script & demo-data.sql
```
Branch: feature/tuan-10-congminh-demo-admin-ui
```

**[MỚI v3.1] AdminView UI:**

Tạo `AdminView.fxml` + `AdminController.java` (chỉ accessible khi `ClientSession.currentRole == ADMIN`). Layout: `TableView` hiện danh sách users (cột: Username, Email, Role, Trạng thái — "Hoạt động" / "Đã khóa"). Hai Button: "Khóa tài khoản" (disable khi chọn Admin hoặc chưa chọn dòng), "Mở khóa" (disable khi chưa chọn dòng). Logic: khi click Khóa/Mở khóa → `NetworkTask` gọi `LOCK_USER` / `UNLOCK_USER` → nếu OK → reload danh sách → TableView cập nhật. Hiện Alert confirm trước khi khóa: "Bạn có chắc muốn khóa tài khoản [username]?".

Thêm vào `AuctionListView` (hoặc tạo NavBar riêng) Button "Quản lý Admin" chỉ visible với ADMIN → navigate `ADMIN_VIEW`. Thêm `ADMIN_VIEW` vào `Views.java`.

**Demo Script:**

Tạo `docs/DEMO_SCRIPT.md` (kịch bản 10 phút):
- Bước 1 (1 phút): Mở 4 cửa sổ Client — A (Admin), B (Seller), C + D (Bidder). Giải thích kiến trúc Client-Server. Chỉ `ServerApp` đang chạy trên terminal.
- Bước 2 (2 phút): B (Seller) đăng nhập → tạo Item Electronics "MacBook Pro" → tạo Auction giá khởi điểm 30 triệu, thời gian 5 phút.
- Bước 3 (2 phút): C và D (Bidder) đăng nhập. C đặt 31tr → D nhận BID_UPDATE realtime → D đặt 32tr → C nhận update → Chart cập nhật theo thời gian thực.
- Bước 4 (2 phút): Chỉnh auction endTime còn 50s (update DB) → C đặt thêm → countdown gia hạn 60s (Anti-Sniping) → client reset countdown.
- Bước 5 (1.5 phút): Đợi auction kết thúc → AuctionClosedEvent → hiện winner. A (Admin) login → AdminView → xem danh sách users → lock D → D cố login → "TÀI KHOẢN BỊ KHÓA".
- Bước 6 (0.5 phút): Q&A.

Tạo `demo-data.sql`: INSERT 4 users (1 Admin, 1 Seller, 2 Bidder), 3 items (Electronics, Art, Vehicle), 1 auction RUNNING cho item Electronics với 2 bid transactions mẫu.

**✅ Đầu ra:**
- `AdminView` load danh sách users từ server
- Lock user thành công → Trạng thái cập nhật trong TableView
- Demo script chạy được đúng ~10 phút
- `demo-data.sql` chạy không lỗi

---

### Khoa — Submission Checklist & Mock Q&A
```
Branch: feature/tuan-10-khoa-submission
```

Tạo `docs/SUBMISSION_CHECKLIST.md`:

```markdown
## Bắt buộc (9.0đ)
- [x] 0.5đ Thiết kế lớp: Entity, User/Bidder/Seller/Admin, Item/Electronics/Art/Vehicle,
      Auction, BidTransaction, AuditLog
- [x] 1.0đ OOP: Encapsulation (private+getter), Inheritance (Entity→User→Bidder),
      Polymorphism (getInfo()/getCategoryDetails()), Abstraction (abstract Entity/User/Item)
- [x] 1.0đ Design Patterns:
    - Singleton: DbConnectionProvider, AuctionManager, SessionManager, NotificationBroker, ViewRouter
    - Factory Method: ItemCreator (abstract) + ElectronicsCreator / ArtCreator / VehicleCreator
    - Observer: NotificationBroker + BidUpdateEvent + AuctionClosedEvent
- [x] 1.0đ Quản lý người dùng & sản phẩm: Register, Login, Logout, CRUD Item,
      LockUser, UnlockUser (ADMIN)
- [x] 1.0đ Chức năng đấu giá: PlaceBid, BidValidator, AuctionLifecycle,
      transitions OPEN→RUNNING→FINISHED→PAID/CANCELED
- [x] 1.0đ Xử lý lỗi & ngoại lệ: BidHubException hierarchy (7 subclass),
      ValidationException với errors list, error response JSON
- [x] 1.0đ Concurrent bidding an toàn: ReentrantLock per Auction, finally block,
      stress test 50 threads, không lost update
- [x] 0.5đ Realtime update: NotificationBroker, BidUpdateEvent,
      EventListenerThread, Platform.runLater
- [x] 0.5đ Kiến trúc Client-Server: Socket, SocketServerCore, Session, ClientConnectionThread
- [x] 0.5đ MVC: FXML + Controller + ViewRouter (client); RequestHandler → Service → DAO (server)
- [x] 0.5đ Maven multi-module, Google Java Style, SLF4J+Logback
- [x] 0.5đ Unit Test JUnit ≥ 139 cases
- [x] 0.5đ CI/CD: GitHub Actions + test tự động + JaCoCo report

## Nâng cao (1.0đ)
- [x] 0.5đ Anti-Sniping: threshold 60s, gia hạn 60s, AuctionExtendedEvent push realtime,
      client reset countdown
- [x] 0.5đ Price Chart: LineChart realtime, addDataPoint từ EventListenerThread,
      lịch sử + live data

## Tính năng bổ sung v3.1 (minh họa cho giảng viên)
- [x] Audit Log System: AuditLog model, AuditLogDao, AuditLogService, tích hợp toàn hệ thống
- [x] Admin User Management: lockUser, unlockUser, listAllUsers, AdminView UI, phân quyền chặt chẽ
- [x] Report Export Service: exportAuctionReport, exportBidHistory, exportAuditLog (JSON)
- [x] Data Integrity Service: checkBidConsistency, checkAuctionWinners, checkOrphanedItems
- [x] Health Check endpoint: server uptime, active auctions, active sessions
```

Tổ chức mock Q&A 30 phút: mỗi người ngẫu nhiên giải thích 1 class do người **khác** viết. Chú ý các câu hỏi phổ biến: "Tại sao dùng ReentrantLock thay synchronized?", "Factory Method Pattern GoF có gì khác Static Factory?", "Platform.runLater() là gì?", "AuditLogService.log() tại sao không ném exception?".

Chạy `mvn test` lần cuối → xác nhận ≥ 139 tests, 0 failures. Tag release:

```
git tag v1.0.0 -m "Final submission BidHub v3.1"
git push origin v1.0.0
```

Tạo GitHub Release với changelogs tóm tắt, upload JAR nếu có.

**✅ Đầu ra:**
- `mvn test` → ≥ 139 tests, 0 failures
- `SUBMISSION_CHECKLIST.md` tick đầy đủ
- Tag `v1.0.0` xuất hiện trong GitHub Releases
- **Mỗi thành viên giải thích được bất kỳ class nào trong hệ thống**

---

## Bảng điểm dự kiến

| Tiêu chí | Điểm | Tuần xong | Người chịu trách nhiệm |
|----------|------|-----------|------------------------|
| Thiết kế lớp & cây kế thừa | 0.5 | Tuần 2 | Đăng + Quốc Minh + Công Minh |
| OOP principles (4 trụ cột) | 1.0 | Tuần 2 | Cả nhóm |
| Design Patterns (Singleton × 6, Factory Method GoF, Observer) | 1.0 | T3, T4, T7 | Đăng + Quốc Minh |
| Quản lý người dùng & sản phẩm | 1.0 | Tuần 5 | Quốc Minh + Công Minh + **Khoa** |
| Chức năng đấu giá | 1.0 | Tuần 6 | Quốc Minh + **Khoa** |
| Xử lý lỗi & ngoại lệ | 1.0 | T2, T5, T6 | Khoa + Quốc Minh |
| Concurrent bidding an toàn | 1.0 | Tuần 7 | Đăng + **Khoa** |
| Realtime update (Observer/Socket) | 0.5 | Tuần 7 | Quốc Minh + Công Minh |
| Kiến trúc Client-Server | 0.5 | Tuần 4 | Đăng + Quốc Minh |
| MVC (JavaFX+FXML / RequestHandler→DAO) | 0.5 | T3, T4 | Công Minh + Đăng |
| Maven, coding convention, SLF4J | 0.5 | T1, T9 | Đăng + Quốc Minh |
| Unit Test JUnit | 0.5 | Mỗi tuần | **Khoa** |
| CI/CD GitHub Actions | 0.5 | T1, T9 | Quốc Minh |
| **Tổng bắt buộc** | **9.0** | | |
| Anti-Sniping | 0.5 | Tuần 8 | Đăng + Quốc Minh |
| Price Chart LineChart | 0.5 | Tuần 8 | Công Minh |
| **Tổng nâng cao** | **1.0** | | |
| **TỔNG DỰ KIẾN** | **10.0** | | |

> **Tính năng v3.1 bổ sung (Audit Log, Admin Mgmt, Report, Integrity, Health Check):** Không có điểm riêng trong barem, nhưng **tăng tính thuyết phục cho giảng viên** (hệ thống hoàn chỉnh, có audit trail, quản trị admin đầy đủ với UI) và giúp nhóm tự tin hơn trong buổi bảo vệ.

---

## Lưu ý tích hợp & phụ thuộc

### Thứ tự implement (quan trọng, không đảo ngược)

```
AuditLog model (T4, Khoa)
    └→ AuditLogDao (T4, Khoa)
        └→ AuditLogService (T5, Khoa)
            ├→ auditLogService.log() trong handleLogin/Register/Logout (T5, QM gọi)
            ├→ auditLogService.log() trong handleCreateItem/DeleteItem (T5, Khoa)
            ├→ AdminUserService sử dụng (T6, Khoa)
            └→ auditLogService.log() trong PlaceBid/LifecycleTask/AntiSnipe (T8, Khoa)

UserDao.updateLocked() + cột is_locked (T6, Đăng)
    └→ AdminUserService sử dụng (T6, Khoa)
        └→ handleLockUser/UnlockUser (T6, Khoa)
            └→ handleLogin kiểm tra isLocked (T6, Đăng thêm 2 dòng)

AuctionDao.findAll() (T7, Khoa tự thêm vào AuctionDao)
    └→ ReportService.exportAuctionReport() (T7, Khoa)
    └→ DataIntegrityService.checkBidConsistency() (T9, Khoa)
```

### Các phụ thuộc cần phối hợp theo tuần

| Tuần | Khoa cần từ ai | Action |
|------|---------------|--------|
| T4 | Đăng thêm bảng `audit_logs` vào schema.sql | Merge Đăng trước, Khoa rebase |
| T5 | Quốc Minh inject `AuditLogService` vào `RequestHandler` | Khoa merge AuditLogService trước, QM rebase |
| T6 | Đăng thêm `is_locked` + `UserDao.updateLocked()` | Merge Đăng trước khi Khoa viết AdminUserService |
| T6 | Quốc Minh thêm `isLocked()` check vào `handleLogin()` | Đăng merge trước, QM thêm 2 dòng |
| T7 | Khoa tự thêm `AuctionDao.findAll()` | Đăng review PR của Khoa |

---

## Lưu ý quan trọng

> [!TIP]
> **Thứ tự implement đúng:** Model (Tuần 2) → DAO (Tuần 3) → RequestHandler (Tuần 4–6) → UI (Tuần 5–6). Không nhảy vào UI khi chưa có logic server.

> [!TIP]
> **Dùng in-memory SQLite (`jdbc:sqlite::memory:`) cho tất cả DAO test** — nhanh, không để lại file `.db` trong repo. Nhất quán cho tất cả DAO: UserDao, ItemDao, AuctionDao, BidDao, AuditLogDao.

> [!TIP]
> **Dùng Jackson** cho toàn bộ JSON (static `ObjectMapper` — thread-safe). Không dùng Gson. `AuditLog.details` là String JSON thô — không parse lại trong DAO, chỉ lưu/đọc nguyên văn.

> [!WARNING]
> **ReentrantLock PHẢI trong try-finally.** Quên `unlock()` trong finally → deadlock khi exception → toàn bộ auction bị treo mãi.

> [!WARNING]
> **`AuditLogService.log()` KHÔNG được ném exception.** Wrap toàn bộ trong try-catch. Mọi exception audit log phải im lặng (chỉ log ra stderr). Không để audit làm crash PlaceBid hay Login.

> [!CAUTION]
> **Quy tắc sống còn:** Mỗi thành viên phải giải thích được **BẤT KỲ dòng code nào**. Giảng viên hỏi bất kỳ ai — không trả lời được → **0 điểm TOÀN NHÓM**.

> [!CAUTION]
> Không commit 1 lần duy nhất vào phút chót. Commit thường xuyên = chứng minh quá trình làm việc thực sự.

### Khi gặp khó khăn
1. Google + đọc Javadoc chính thức (15 phút)
2. Hỏi nhóm trên Discord/Zalo
3. Dùng AI để **hiểu concept** — nhưng phải hiểu code trước khi commit
4. Vẫn stuck → đặt câu hỏi cụ thể trong buổi họp nhóm cuối tuần