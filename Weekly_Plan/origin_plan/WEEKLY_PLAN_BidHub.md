# BidHub — Lộ trình 10 tuần (Phiên bản tối giản · Đạt điểm A+)

> **Triết lý:** Code chạy được · Đúng logic · Hiểu được từng dòng
> Mọi thứ phức tạp hơn yêu cầu barem đều là rủi ro, không phải điểm cộng.

---

## Thành viên nhóm

| Ký hiệu | Họ tên | Vai trò chính |
|---------|--------|---------------|
| **Đăng** | [Họ tên đầy đủ] | Server Core & Database |
| **Quốc Minh** | [Họ tên đầy đủ] | Networking & Protocol |
| **Công Minh** | [Họ tên đầy đủ] | Client GUI (JavaFX) |
| **Khoa** | [Họ tên đầy đủ] | Business Logic & Testing |

> [!IMPORTANT]
> Vai trò chính chỉ xác định ai **code trước**. Mọi người phải **hiểu toàn bộ hệ thống** — giảng viên hỏi bất kỳ ai, cả nhóm cùng chịu trách nhiệm.

---

## Quy tắc Git

```
Nhánh:   main ← develop ← feature/tuan-X-ten-nguoi-mo-ta
Commit:  feat: thêm PlaceBidHandler / fix: sửa lỗi concurrent lock
PR:      [Tuần X] Tên người - Mô tả ngắn  |  cần ít nhất 1 người approve
Merge:   develop → main vào cuối Tuần 4, 7, 10
```

> [!CAUTION]
> Không commit 1 lần duy nhất vào phút chót. Commit thường xuyên = chứng minh quá trình làm việc thực sự.

---

## Tổng quan 10 tuần

```
┌──────────────┬────────────────────────────────────────────────────────────┬──────────┐
│ Giai đoạn    │ Nội dung                                                   │ Mục tiêu │
├──────────────┼────────────────────────────────────────────────────────────┼──────────┤
│ NỀN TẢNG     │ Tuần 1 · Setup + CI/CD + JavaFX skeleton                   │ Môi trường sẵn sàng │
│              │ Tuần 2 · OOP model + Exception hierarchy                   │ Cây kế thừa đủ barem │
├──────────────┼────────────────────────────────────────────────────────────┼──────────┤
│ CORE         │ Tuần 3 · DAO + SQLite + MVC routing                        │ CRUD thông suốt │
│              │ Tuần 4 · Socket Server + RequestHandler switch-case         │ Ping-pong Client↔Server │
├──────────────┼────────────────────────────────────────────────────────────┼──────────┤
│ CHỨC NĂNG    │ Tuần 5 · Auth + SHA-256 + Quản lý Item                    │ Đăng ký/đăng nhập xong │
│ CHÍNH        │ Tuần 6 · Bidding Engine + BidValidator + Lifecycle         │ Đặt giá được rồi │
├──────────────┼────────────────────────────────────────────────────────────┼──────────┤
│ KỸ THUẬT     │ Tuần 7 · ReentrantLock + Observer realtime                 │ 1.5đ kỹ thuật quan trọng │
│ QUAN TRỌNG   │ Tuần 8 · Anti-Sniping + Price Chart (nâng cao)             │ +1.0đ nâng cao │
├──────────────┼────────────────────────────────────────────────────────────┼──────────┤
│ HOÀN THIỆN   │ Tuần 9 · Integration + Unit Test + refactor                │ CI xanh, 0 bug │
│              │ Tuần 10 · Docs + Demo + Submission                         │ Nộp bài tự tin │
└──────────────┴────────────────────────────────────────────────────────────┴──────────┘
```

> **Lý do bỏ Auto-Bidding (0.5đ):** Độ khó 7/10, dễ gây Infinite Loop làm sập server, tốn 1–2 tuần để ổn định. Thay bằng Anti-Sniping (độ khó 2/10) + Price Chart (độ khó 5/10) — vẫn đủ 1.0đ nâng cao với rủi ro thấp hơn nhiều.

---

## ═══ TUẦN 1 · Thiết lập môi trường & Infrastructure

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

## ═══ TUẦN 2 · OOP Domain Model & Exception Hierarchy

**Mục tiêu:** Cây kế thừa Entity → User/Item → các subclass · Factory Method · Exception hierarchy (1.5đ barem)

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | Abstract class vs Interface — khi nào dùng cái nào | Giải thích được từ BidHub context |
| 2 | Factory Method Pattern | Vẽ UML, giải thích Creator/Product |
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

### Quốc Minh — Item Hierarchy & Factory Pattern
```
Branch: feature/tuan-2-quocminh-item-factory
```

Tạo `Item.java` (abstract, extends Entity): field `name`, `description`, `startingPrice` (double), `sellerId`, `itemType` (ItemType enum). Constructor validate `startingPrice > 0`, ném `IllegalArgumentException` nếu vi phạm. Method abstract `getCategoryDetails(): String`.

Tạo enum `ItemType` { ELECTRONICS, ART, VEHICLE } với `getLabel(): String`.

Tạo `Electronics.java` (extends Item): thêm `brand`, `warrantyMonths`. Tạo `Art.java`: thêm `artist`, `yearCreated`. Tạo `Vehicle.java`: thêm `manufacturer`, `year`, `mileageKm`. Mỗi class implement `getCategoryDetails()` khác nhau.

Tạo interface `Displayable` với `printInfo(): void` — implement ở tất cả Item subclasses.

Tạo `ItemFactory.java` (Factory Method Pattern): method static `create(ItemType type, String name, String description, double startingPrice, String sellerId, Map<String, Object> extras): Item`. Ném `IllegalArgumentException` với type không hợp lệ.

**✅ Đầu ra:**
- `ItemFactory.create(ELECTRONICS, ...)` → instance của `Electronics`
- `startingPrice = -100` → `IllegalArgumentException`
- `List<Item>` gọi `printInfo()` → output khác nhau (polymorphism)

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

## ═══ TUẦN 3 · DAO & Database + MVC Routing

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

## ═══ TUẦN 4 · Socket Server + RequestHandler

**Mục tiêu:** Client↔Server giao tiếp qua Socket · JSON qua Jackson · Ping-pong thành công (barem: Client-Server 0.5đ + MVC 0.5đ)

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | Java Socket (ServerSocket, Socket, IO streams) | Viết mini chat 2 chiều |
| 2 | Java Thread Pool (ExecutorService) | Giải thích tại sao dùng pool |
| 3 | Jackson ObjectMapper: serialize/deserialize | Deserialize JSON → POJO và ngược lại |
| 4 | JavaFX Task<T> và Platform.runLater() | Giải thích tại sao dùng Task cho network call |

---

### Đăng — SocketServerCore & Session
```
Branch: feature/tuan-4-dang-socket-server
```

Tạo `SocketServerCore.java` (package `com.bidhub.server.network`): field `serverSocket`, `threadPool` (fixed pool 30). Method `start(int port)` — vòng lặp `accept()`, tạo `ClientConnectionThread` submit vào pool. Method `shutdown()`.

Tạo `Session.java`: field `sessionId` (UUID), `socket`, `out: PrintWriter`, `authenticatedUserId: String` (null nếu chưa login). Method `sendMessage(String jsonResponse): void` — synchronized trên `out`. Method `isAuthenticated(): boolean`, `disconnect(): void`.

Tạo `ClientConnectionThread.java` (Runnable): vòng lặp đọc từng dòng JSON → gọi `RequestHandler.handle(line, session)` → gửi response. Khi client ngắt (IOException hoặc null) → cleanup.

Tạo `ServerApp.java`: entry point — chạy `MigrationRunner`, start `SocketServerCore`.

**✅ Đầu ra:**
- `ServerApp` start → `telnet localhost 9090` kết nối được
- Khi client ngắt → session bị cleanup

---

### Quốc Minh — JSON Protocol & RequestHandler (switch-case)
```
Branch: feature/tuan-4-quocminh-protocol-handler
```

Tạo `MessageRequest.java` (POJO Jackson): field `type: String`, `token: String`, `payload: JsonNode`. Annotate `@JsonIgnoreProperties(ignoreUnknown = true)`.

Tạo `MessageResponse.java`: field `status` ("OK"/"ERROR"), `type`, `payload: Object`, `message`. Static factory: `ok(String type, Object payload)`, `error(String type, String message)`.

Tạo `MessageMapper.java`: static `ObjectMapper` (thread-safe). Method `toJson(Object): String`, `fromJson(String, Class<T>): T`.

> [!TIP]
> **Thay vì Command Pattern phức tạp**, dùng `RequestHandler.java` với switch-case đơn giản. Dễ code, dễ debug, dễ giải thích — và vẫn đủ điểm barem.

Tạo `RequestHandler.java` (package `com.bidhub.server.network`): method `handle(String jsonLine, Session session): String`. Logic:

```java
MessageRequest req = MessageMapper.fromJson(jsonLine, MessageRequest.class);
// Kiểm tra auth nếu cần
if (needsAuth(req.getType()) && !session.isAuthenticated()) {
    return MessageMapper.toJson(MessageResponse.error(req.getType(), "Bạn chưa đăng nhập"));
}
return switch (req.getType()) {
    case "PING"     -> handlePing(session, req.getPayload());
    case "LOGIN"    -> handleLogin(session, req.getPayload());
    case "REGISTER" -> handleRegister(session, req.getPayload());
    // ... thêm case mỗi tuần
    default -> MessageMapper.toJson(MessageResponse.error(req.getType(), "Unknown command"));
};
```

Mọi exception trong handler → bắt, trả về error response (không để exception thoát).

Implement `handlePing()`: trả về `{"serverTime": "...", "message": "pong"}`.

**✅ Đầu ra:**
- `handle("{\"type\":\"PING\",\"payload\":{}}", session)` → JSON status="OK"
- `handle("{\"type\":\"NOTEXIST\",...}", session)` → status="ERROR"
- `handle("not valid json", session)` → status="ERROR", không crash
- `handle("{\"type\":\"LOGIN\",...}", unauthSession)` với route cần auth → "Bạn chưa đăng nhập"

---

### Công Minh — ServerGateway (Client) + Connection
```
Branch: feature/tuan-4-congminh-server-gateway
```

Tạo `ServerGateway.java` (Singleton, package `com.bidhub.client.network`): field `socket`, `reader: BufferedReader`, `writer: PrintWriter`. Method `connect(String host, int port): void`, `sendRequest(MessageRequest): MessageResponse`, `disconnect()`, `isConnected(): boolean`.

> [!TIP]
> **Khi mất kết nối**: Không cần Exponential Backoff phức tạp. Chỉ cần hiện Alert: `"Mất kết nối tới Server. Vui lòng khởi động lại ứng dụng."` là đủ. Giảng viên hiếm khi test tắt server đột ngột.

Tạo `NetworkTask<T>` (extends `javafx.concurrent.Task<T>`): nhận `Callable<T>`. Method `call()` thực thi, bắt exception → wrap thành `RuntimeException`.

Tạo `ClientSession.java` (Singleton phía client): field `token: String`, `currentUserId: String`, `currentUsername: String`, `currentRole: UserRole`. Method `login(...)`, `logout()`, `isLoggedIn(): boolean`.

**✅ Đầu ra:**
- `connect("localhost", 9090)` khi server chạy → `isConnected()` == true
- `sendRequest(ping)` → response status="OK"
- Không kết nối được → Alert hiện, không crash

---

### Khoa — Test Tuần 4
```
Branch: feature/tuan-4-khoa-protocol-tests
```

Viết test cho `RequestHandler`: PING → OK, UNKNOWN → ERROR, auth-required với unauthenticated session → error, malformed JSON → error không crash.

Viết test `MessageMapper`: serialize → parse lại không exception. Deserialize JSON thiếu field → `@JsonIgnoreProperties` không crash.

**✅ Đầu ra:**
- ≥ 10 test cases pass
- `mvn test` xanh

---

## ═══ TUẦN 5 · Authentication & Quản lý người dùng/sản phẩm

**Mục tiêu:** Đăng ký/đăng nhập/đăng xuất hoạt động · CRUD sản phẩm · UI Login/Register (barem: Quản lý người dùng + sản phẩm 1.0đ)

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

Tạo `AuthService.java` (package `com.bidhub.server.service`):

```java
// Hash mật khẩu — dùng SHA-256 có sẵn, không cần thư viện ngoài
public String hashPassword(String plain) {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hash = md.digest(plain.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash);
}
public boolean verifyPassword(String plain, String hashed) {
    return hashPassword(plain).equals(hashed);
}
public String generateToken() { return UUID.randomUUID().toString(); }
```

Tạo `SessionManager.java` (Singleton): 2 `ConcurrentHashMap` — `tokenToUserId`, `userIdToToken`. Method `createSession(String userId): String`, `invalidateSession(String token): void`, `getUserIdByToken(String token): Optional<String>`.

Cập nhật `RequestHandler.handle()`: parse `token` từ request → `SessionManager.getUserIdByToken()` → set `session.authenticatedUserId` nếu hợp lệ.

**✅ Đầu ra:**
- `hashPassword("secret")` → chuỗi hex 64 ký tự
- `verifyPassword("secret", hash)` → true; `verifyPassword("wrong", hash)` → false
- `createSession("user-1")` → token UUID format
- `invalidateSession(token)` → `getUserIdByToken()` → `Optional.empty()`

---

### Quốc Minh — Login, Register, Logout + SecurityContext
```
Branch: feature/tuan-5-quocminh-auth-handlers
```

Thêm vào `RequestHandler`:

`handleLogin()`: parse `{username, password}` → `UserDao.findByUsername()` → nếu empty → error. `AuthService.verifyPassword()` → nếu false → error. `SessionManager.createSession()` → set `session.authenticatedUserId` → trả về `{token, userId, username, role}`.

`handleRegister()`: parse `{username, password, email, role}`.

```java
// Validation đơn giản — không cần Fluent API
if (username == null || username.isBlank()) throw new ValidationException("username trống");
if (password == null || password.length() < 8) throw new ValidationException("password < 8 ký tự");
if (!email.contains("@")) throw new ValidationException("email không hợp lệ");
if (role.equals("ADMIN")) throw new ValidationException("Không được tự register Admin");
```

Kiểm tra `UserDao.existsByUsername()` → nếu tồn tại → `DuplicateUsernameException`. Hash password → save user → trả về user info.

`handleLogout()`: `SessionManager.invalidateSession(token)` → clear `session.authenticatedUserId`.

Tạo `SecurityContext.java`: `requireAuthenticated(Session session): String` — nếu userId null → ném `AuthenticationException`. `requireRole(Session session, UserRole role)` — lấy user từ UserDao → check role.

**✅ Đầu ra:**
- Login đúng credentials → status="OK", có `token`
- Login sai password → status="ERROR"
- Register username đã tồn tại → error "USERNAME_TAKEN"
- Logout → token không còn hợp lệ

---

### Công Minh — LoginView, RegisterView + CreateItemView hoàn chỉnh
```
Branch: feature/tuan-5-congminh-auth-item-ui
```

Hoàn thiện `LoginView.fxml` + `LoginController.java`: Button disabled khi username/password rỗng (bind `disableProperty`). Khi click: `NetworkTask` → gọi Login → nếu OK: `ClientSession.login()` → navigate AuctionList → nếu ERROR: hiện label lỗi đỏ.

Hoàn thiện `RegisterView.fxml` + `RegisterController.java`: validation confirmPassword realtime, ChoiceBox role (BIDDER/SELLER). Submit → `RegisterCommand` → thành công → navigate Login.

Hoàn thiện `CreateItemView.fxml` + `CreateItemController.java` (chỉ cho SELLER): Form tạo item mới, gửi `CREATE_ITEM` request → navigate về AuctionList khi thành công.

**✅ Đầu ra:**
- Đăng nhập đúng → chuyển AuctionListView
- Đăng nhập sai → label lỗi đỏ
- Button disabled khi field rỗng

---

### Khoa — Item handlers + Auth Test Suite
```
Branch: feature/tuan-5-khoa-item-handlers-tests
```

Thêm vào `RequestHandler`: `handleCreateItem()` (yêu cầu auth + role SELLER), `handleGetItemList()`, `handleGetItemDetail()`, `handleDeleteItem()` (chỉ seller của item).

Viết test suite auth, ≥ 20 test cases (login đúng/sai, register trùng username, register ADMIN bị từ chối, logout, token sai, thiếu role...).

**✅ Đầu ra:**
- ≥ 20 auth test cases pass
- CRUD item qua handler hoạt động

---

## ═══ TUẦN 6 · Core Bidding Engine

**Mục tiêu:** Đặt giá được, kiểm tra luật, đóng phiên tự động, chuyển trạng thái (barem: Chức năng đấu giá 1.0đ + Xử lý lỗi 1.0đ)

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | ScheduledExecutorService | Viết task lặp mỗi 5s, biết cách cancel |
| 2 | JavaFX Timeline & KeyFrame | Countdown timer đếm ngược trên Label |
| 3 | Transactional thinking | Mô tả kịch bản 2 người đặt giá cùng lúc — điều gì sai |
| 4 | JavaFX TableView & ObservableList | Bind TableView, tự cập nhật khi list thay đổi |

---

### Đăng — AuctionManager & LifecycleTask
```
Branch: feature/tuan-6-dang-auction-manager
```

Tạo `AuctionManager.java` (Singleton): field `activeAuctions: ConcurrentHashMap<String, Auction>`, `scheduler: ScheduledExecutorService`. Method `start()` — load RUNNING auctions từ `AuctionDao` vào RAM, khởi động `AuctionLifecycleTask` mỗi 5 giây. Method `addAuction`, `removeAuction`, `getAuction(String): Optional<Auction>`, `getAllActive()`.

Tạo `AuctionLifecycleTask.java` (Runnable): duyệt tất cả auction. Nếu `now.isAfter(endTime)` và status == RUNNING → `closeAuction()`: `transitionTo(FINISHED)` → `AuctionDao.updateStatus()` → tìm winner từ `BidDao.getHighestBid()` → `AuctionManager.removeAuction()` → publish sự kiện đóng (stub, sẽ dùng Tuần 7).

Thêm handler `CREATE_AUCTION` vào `RequestHandler` (yêu cầu auth + role SELLER): validate `startingPrice > 0`, `durationMinutes ∈ [1, 10080]` → tạo Auction → save DB → add vào AuctionManager.

**✅ Đầu ra:**
- `AuctionManager.getInstance()` gọi 2 lần → cùng instance
- `AuctionLifecycleTask` với auction endTime quá khứ → status FINISHED
- `AuctionLifecycleTask` với auction chưa hết → status không đổi

---

### Quốc Minh — BidValidator & Place Bid Handler
```
Branch: feature/tuan-6-quocminh-bid-handler
```

Tạo `BidValidator.java`: method `validate(Auction auction, String bidderId, double bidAmount): void`. Ném `InvalidBidException` (message rõ ràng) nếu: `!status.canBid()`, `bidAmount <= currentHighestBid`, `bidAmount - currentHighestBid < minimumIncrement`, `sellerId của item == bidderId`.

Thêm vào `RequestHandler`:

`handlePlaceBid()` (auth + role BIDDER): lấy Auction từ `AuctionManager` → `BidValidator.validate()` → tạo `BidTransaction` → `BidDao.save()` → `AuctionDao.updateHighestBid()` → cập nhật Auction trong RAM → trả về bid info.

`handleGetAuctionList()` (không cần auth): trả về `List` với id, itemName, currentHighestBid, endTime, status.

`handleGetAuctionDetail()`: trả về chi tiết auction + item info + 5 bids gần nhất.

**✅ Đầu ra:**
- Bid thấp hơn → `InvalidBidException` chứa giá hiện tại
- Seller tự bid → `InvalidBidException`
- Bid hợp lệ → `AuctionManager.getAuction(id).getCurrentHighestBid()` == bidAmount
- Phiên FINISHED → bid → `AuctionClosedException`

---

### Công Minh — AuctionListView & AuctionDetailView + Countdown
```
Branch: feature/tuan-6-congminh-auction-ui
```

Hoàn thiện `AuctionListController`: `TableView<Map>` bind `ObservableList`. Load: `NetworkTask` gọi `GET_AUCTION_LIST` → populate. Button "Tạo phiên" chỉ visible nếu role == SELLER. Double-click → navigate Detail.

Hoàn thiện `AuctionDetailController`: load chi tiết auction, hiển thị item info, countdown timer (JavaFX `Timeline` đếm ngược từ `endTime - now`). TextField bid amount, Button "Đặt giá" (disabled khi auction không RUNNING). Khi đặt: `NetworkTask` → `PLACE_BID` → nếu OK: cập nhật giá UI + thông báo; nếu ERROR: Alert.

**✅ Đầu ra:**
- AuctionList load data từ server
- Countdown đếm ngược đúng
- Đặt giá thành công → giá cập nhật ngay trên UI

---

### Khoa — Bidding Test Suite + Error Handling Test
```
Branch: feature/tuan-6-khoa-bid-tests
```

Viết test ≥ 15 cases: bid hợp lệ, bid thấp hơn, bid đúng bằng, bid khi đã FINISHED, seller tự bid, bidAmount âm, auctionId không tồn tại, chuyển trạng thái hợp lệ/sai.

Verify `AuctionStatus` transitions: đúng → không throw; sai → `IllegalStateException`.

**✅ Đầu ra:**
- ≥ 15 bid test cases pass
- 0 failures

---

## ═══ TUẦN 7 · Concurrency & Realtime Observer

**Mục tiêu:** ReentrantLock chống lost update · Observer push bid mới về toàn bộ client (barem: Concurrent 1.0đ + Realtime 0.5đ — **2 điểm quan trọng nhất**)

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | ReentrantLock vs synchronized | Giải thích 2 điểm khác biệt |
| 2 | Race condition & Lost Update | Vẽ timeline 2 thread cùng bid |
| 3 | Observer / Pub-Sub Pattern | Vẽ UML, phân biệt Observer và Pub-Sub |
| 4 | Platform.runLater() | Điều gì xảy ra nếu cập nhật UI từ background thread |

---

### Đăng — ReentrantLock Granular Locking
```
Branch: feature/tuan-7-dang-reentrantlock
```

Cập nhật `Auction.java`: thêm field `lock: ReentrantLock` (khởi tạo trong constructor, `transient` — không serialize xuống DB).

Cập nhật `handlePlaceBid()` trong `RequestHandler`:

```java
Auction auction = auctionManager.getAuction(auctionId).orElseThrow(AuctionNotFoundException::new);
auction.getLock().lock();
try {
    bidValidator.validate(auction, bidderId, bidAmount);
    // ... tạo transaction, save DB, cập nhật RAM
} finally {
    auction.getLock().unlock();  // LUÔN unlock dù có exception
}
```

> [!WARNING]
> `unlock()` **bắt buộc** trong `finally`. Quên → deadlock khi exception → toàn bộ auction bị treo.

Khi `AuctionManager.addAuction()` load từ DB → tạo `new ReentrantLock()` cho mỗi Auction (lock không persist được).

**✅ Đầu ra:**
- 2 threads cùng bid → chỉ 1 thắng, `currentHighestBid` nhất quán
- Không `IllegalMonitorStateException`
- `getLock()` trả về cùng instance trên cùng Auction

---

### Quốc Minh — NotificationBroker & Observer Events
```
Branch: feature/tuan-7-quocminh-notification-broker
```

Tạo `BidUpdateEvent.java` (package `com.bidhub.server.event`): field `type = "BID_UPDATE"`, `auctionId`, `newHighestBid`, `highestBidderId`, `bidTime`, `totalBids`. Serialize thành JSON push qua socket.

Tạo `AuctionClosedEvent.java`: `type = "AUCTION_CLOSED"`, `auctionId`, `winnerId`, `winningBid`.

Tạo `NotificationBroker.java` (Singleton — **Observer Pattern**): field `subscribers: ConcurrentHashMap<String, CopyOnWriteArrayList<Session>>` (key = auctionId). Method `subscribe(String auctionId, Session)`, `unsubscribe(String auctionId, Session)`, `unsubscribeAll(Session)` (khi client disconnect). Method `publish(String auctionId, Object event)` — serialize event, gọi `session.sendMessage()` cho từng subscriber; nếu `IOException` → `unsubscribeAll(session)`.

Thêm handler `SUBSCRIBE_AUCTION` vào `RequestHandler` (không cần auth): `NotificationBroker.subscribe(auctionId, session)`.

Cập nhật `handlePlaceBid()`: sau bid thành công → `NotificationBroker.publish(auctionId, new BidUpdateEvent(...))`.

Cập nhật `AuctionLifecycleTask.closeAuction()`: publish `AuctionClosedEvent`.

**✅ Đầu ra:**
- `subscribe(id, session1)` → `publish(id, event)` → `session1.sendMessage()` được gọi
- 2 sessions cùng subscribe → publish → cả 2 nhận
- `unsubscribe` → không nhận tiếp
- Session disconnect → broker cleanup không crash

---

### Công Minh — EventListenerThread & Realtime UI Update
```
Branch: feature/tuan-7-congminh-realtime-client
```

Tách `ServerGateway` thêm `EventListenerThread.java` (background thread): vòng lặp đọc từng dòng JSON. Nếu `"type": "BID_UPDATE"` → parse → gọi registered callbacks. Nếu `"type": "AUCTION_CLOSED"` → tương tự. Start khi `connect()` thành công.

Tạo interface `BidUpdateCallback`: `onBidUpdate(Map<String, Object> event): void`.

Cập nhật `AuctionDetailController`: khi mở màn hình → `SUBSCRIBE_AUCTION` → register callback. Implement callback:

```java
gateway.registerBidUpdateCallback(auctionId, event -> {
    Platform.runLater(() -> {  // BẮT BUỘC - cập nhật UI từ FX thread
        labelCurrentPrice.setText(formatCurrency(event.get("newHighestBid")));
        labelLeader.setText(event.get("highestBidderId").toString());
    });
});
```

Khi rời màn hình → unregister callback, gửi request unsubscribe.

**✅ Đầu ra:**
- Client A đặt giá → Client B (cùng auction) nhận BID_UPDATE → UI cập nhật không refresh
- Không "Not on FX Application Thread" exception
- Khi auction đóng → Alert thông báo winner

---

### Khoa — Stress Test Concurrent Bidding
```
Branch: feature/tuan-7-khoa-concurrent-stress-test
```

Tạo `ConcurrentBidTest.java`: setup 1 auction RUNNING, in-memory SQLite (`jdbc:sqlite::memory:`). Dùng `ExecutorService pool = Executors.newFixedThreadPool(50)`. `CountDownLatch startSignal` để 50 threads bắt đầu đồng thời. Mỗi thread gọi bid với random amount.

Assert sau khi xong: `auction.getCurrentHighestBid()` == `bidDao.getHighestBid(id).getBidAmount()` (RAM và DB nhất quán). Assert không có 2 winners.

**✅ Đầu ra:**
- 50 threads hoàn thành, không deadlock
- RAM và DB nhất quán
- Chạy 3 lần → kết quả nhất quán (test không flaky)

---

## ═══ TUẦN 8 · Tính năng nâng cao (Anti-Sniping + Price Chart)

**Mục tiêu:** Anti-Sniping +0.5đ · Price Chart +0.5đ · Bỏ Auto-Bidding vì rủi ro cao

> **Tại sao bỏ Auto-Bidding:** PriorityQueue + xử lý đệ quy → dễ Infinite Loop → sập server → mất nhiều hơn được. Anti-Sniping (2/10) + Price Chart (5/10) an toàn hơn, tổng vẫn đủ +1.0đ.

### Tự học (cả nhóm)

| # | Nội dung | Kiểm tra |
|---|----------|---------|
| 1 | JavaFX LineChart, XYChart.Series, XYChart.Data | Thêm 1 data point vào LineChart |
| 2 | Anti-sniping concept (eBay) | Giải thích logic gia hạn |
| 3 | LocalDateTime arithmetic | `endTime.minusSeconds(60)`, `endTime.plusSeconds(60)` |

---

### Đăng + Quốc Minh — Anti-Sniping Engine
```
Branch: feature/tuan-8-antisniping
```

Tạo `AntiSnipingEngine.java`: đọc `threshold` và `extension` từ `ConfigLoader` (mặc định 60s). Method `check(Auction auction): void`:

```java
// Cực đơn giản — chỉ cần vài dòng
long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
if (secondsLeft < threshold) {
    LocalDateTime newEndTime = auction.getEndTime().plusSeconds(extension);
    auction.setEndTime(newEndTime);
    auctionDao.updateEndTime(auction.getId(), newEndTime);
    // Publish AuctionExtendedEvent để client reset countdown
    notificationBroker.publish(auction.getId(), new AuctionExtendedEvent(auction.getId(), newEndTime));
}
```

Gọi `AntiSnipingEngine.check(auction)` trong `handlePlaceBid()` sau khi bid thành công (trước `NotificationBroker.publish()`).

Tạo `AuctionExtendedEvent.java`: `type = "AUCTION_EXTENDED"`, `auctionId`, `newEndTime`.

Cập nhật `AuctionDetailController`: lắng nghe `AUCTION_EXTENDED` → reset countdown timer về giá trị mới.

**✅ Đầu ra:**
- Bid trong 60s cuối → `endTime` gia hạn thêm 60s trong DB và RAM
- Bid khi còn > 60s → `endTime` không đổi
- Client nhận `AUCTION_EXTENDED` → countdown reset đúng

---

### Công Minh — Price Chart (LineChart Realtime)
```
Branch: feature/tuan-8-congminh-price-chart
```

Trong `AuctionDetailView.fxml`: thêm `LineChart` (hoặc `AreaChart`). Trục X: thời gian (timestamp). Trục Y: giá đấu.

Tạo `BidChartService.java` trong client: giữ `XYChart.Series<String, Number> series`. Method `addDataPoint(String time, double price): void`:

```java
// LUÔN cập nhật chart từ FX thread
Platform.runLater(() -> series.getData().add(new XYChart.Data<>(time, price)));
```

Khi `AuctionDetailController` nhận `BID_UPDATE` → gọi `chartService.addDataPoint(event.bidTime, event.newHighestBid)`.

Khi load màn hình: gọi `GET_BID_HISTORY` → lấy lịch sử bid → populate chart ban đầu.

Thêm handler `handleGetBidHistory()` (server side): `BidDao.findByAuctionId()` → trả về list `{bidTime, bidAmount}`.

**✅ Đầu ra:**
- Chart hiện ra khi mở AuctionDetail
- Mỗi bid mới → chart tự cập nhật không cần refresh
- Lịch sử bid hiện đầy đủ khi load

---

### Khoa — Test Tuần 8
```
Branch: feature/tuan-8-khoa-advanced-tests
```

Test Anti-Sniping: bid trong 60s cuối → endTime tăng; bid khi còn nhiều thời gian → không đổi. Test configurable threshold: đổi `snipe.threshold=30` trong properties → threshold 30s hoạt động đúng.

**✅ Đầu ra:**
- ≥ 5 test cases Anti-Sniping pass

---

## ═══ TUẦN 9 · Integration & Chất lượng mã

**Mục tiêu:** Toàn hệ thống chạy end-to-end · Unit test ≥ 40 cases · CI xanh · Refactor code

### Đăng — Integration Test & Final Refactor
```
Branch: feature/tuan-9-dang-integration
```

Integration test đầy đủ: start server → connect client → register → login → create item → create auction → place bid → nhận BID_UPDATE → auction auto-close → AuctionClosedEvent. Dùng in-memory SQLite cho test.

Refactor: không còn `System.out.println` nào (thay bằng `Logger` SLF4J từ Logback). Tên method/class đúng Google Java Style. Xóa code thừa.

**✅ Đầu ra:**
- Integration test pass end-to-end
- 0 `System.out.println` trong production code

---

### Quốc Minh — API Protocol Docs + CI/CD Final
```
Branch: feature/tuan-9-quocminh-docs-cicd
```

Hoàn thiện `docs/API_PROTOCOL.md` với ví dụ JSON đầy đủ cho mỗi request type (PING, LOGIN, REGISTER, CREATE_ITEM, CREATE_AUCTION, PLACE_BID, GET_AUCTION_LIST, GET_AUCTION_DETAIL, GET_BID_HISTORY, SUBSCRIBE_AUCTION, LOGOUT).

Cập nhật CI: thêm `mvn test --fail-at-end`, đảm bảo CI fail khi test fail. Thêm JaCoCo report (không cần enforce threshold, chỉ cần report).

**✅ Đầu ra:**
- `API_PROTOCOL.md` đủ ≥ 10 command type
- CI push → build pass, test report upload

---

### Công Minh — UI Polish & UX
```
Branch: feature/tuan-9-congminh-ui-polish
```

Thêm loading spinner khi NetworkTask đang chạy (disabled button + ProgressIndicator). Xử lý edge case UI: auction list trống → hiện "Chưa có phiên đấu giá", bid thất bại → Alert rõ lỗi. Đảm bảo countdown timer dừng khi auction FINISHED. Kiểm tra UI đẹp trên độ phân giải 1366×768.

**✅ Đầu ra:**
- Tất cả NetworkTask có loading state
- Không có màn hình nào crash khi data trống

---

### Khoa — Unit Test đầy đủ ≥ 40 cases
```
Branch: feature/tuan-9-khoa-full-test-suite
```

Tổng hợp test từ các tuần, đảm bảo tổng ≥ 40 test cases pass. Phân loại:
- Tuần 1: 15 cases (Calculator)
- Tuần 2: 10 cases (Entity, Exception hierarchy)
- Tuần 4: 10 cases (Protocol, RequestHandler)
- Tuần 5: 20 cases (Auth)
- Tuần 6: 15 cases (Bid validation, DAO)
- Tuần 7: 5+ cases (Concurrent)
- Tuần 8: 5 cases (Anti-Sniping)

`mvn test` → ≥ 40 tests, **0 failures**.

**✅ Đầu ra:**
- `mvn test` xanh ≥ 40 cases

---

## ═══ TUẦN 10 · Docs, Demo & Nộp bài

**Mục tiêu:** Demo trơn tru 10 phút · Mỗi người giải thích được BẤT KỲ dòng code nào

### Quốc Minh — Sequence Diagrams & Design Pattern Docs
```
Branch: feature/tuan-10-quocminh-diagrams
```

Tạo `docs/DESIGN_PATTERNS.md`: giải thích 3 pattern đã dùng (Singleton, Factory Method, Observer), nêu class áp dụng, lợi ích trong context BidHub.

Tạo ≥ 3 sequence diagram (Mermaid trong Markdown): Login flow, PlaceBid flow (với ReentrantLock + Observer), Anti-Sniping flow.

**✅ Đầu ra:**
- Diagram render được trong GitHub Markdown
- `DESIGN_PATTERNS.md` có đủ 3 pattern

---

### Công Minh — Demo Script & Final UI
```
Branch: feature/tuan-10-congminh-demo
```

Tạo `docs/DEMO_SCRIPT.md` (kịch bản 10 phút):
- Bước 1 (1 phút): Mở 3 cửa sổ Client — A (Seller), B + C (Bidder). Giải thích kiến trúc.
- Bước 2 (2 phút): A tạo Item Electronics + tạo Auction 5 phút, giá khởi điểm 30 triệu.
- Bước 3 (2 phút): B và C đăng nhập. B đặt 31tr → C nhận realtime update → C đặt 32tr → B nhận update → chart cập nhật.
- Bước 4 (2 phút): Chỉnh endTime auction còn 50s → B đặt thêm → countdown gia hạn 60s (Anti-Sniping).
- Bước 5 (1 phút): Đợi auction kết thúc → thông báo winner → trạng thái FINISHED.

Tạo `demo-data.sql`: INSERT 3 users mẫu (1 Seller, 2 Bidder), 3 items, 1 auction RUNNING.

**✅ Đầu ra:**
- Demo script chạy được đúng 10 phút
- `demo-data.sql` chạy không lỗi

---

### Khoa — Submission Checklist & Mock Q&A
```
Branch: feature/tuan-10-khoa-submission
```

Tạo `docs/SUBMISSION_CHECKLIST.md`:

```
## Bắt buộc (9.0đ)
- [x] 0.5đ Thiết kế lớp: Entity, User/Bidder/Seller/Admin, Item/Electronics/Art/Vehicle, Auction, BidTransaction
- [x] 1.0đ OOP: Encapsulation (private+getter), Inheritance (Entity→User→Bidder), Polymorphism (getInfo()/getCategoryDetails()), Abstraction (abstract Entity/User/Item)
- [x] 1.0đ Design Patterns: Singleton (DbConnectionProvider, AuctionManager, SessionManager, NotificationBroker, ViewRouter), Factory Method (ItemFactory), Observer (NotificationBroker)
- [x] 1.0đ Quản lý người dùng & sản phẩm: Register, Login, Logout, Create/List/Delete Item
- [x] 1.0đ Chức năng đấu giá: PlaceBid, BidValidator, AuctionLifecycle, transitions OPEN→RUNNING→FINISHED→PAID/CANCELED
- [x] 1.0đ Xử lý lỗi & ngoại lệ: BidHubException hierarchy (7 subclasses), ValidationException với errors list, error response JSON
- [x] 1.0đ Concurrent bidding an toàn: ReentrantLock per Auction, finally block, stress test 50 threads, không lost update
- [x] 0.5đ Realtime update: NotificationBroker, BidUpdateEvent, EventListenerThread, Platform.runLater
- [x] 0.5đ Kiến trúc Client-Server: Socket, SocketServerCore, Session, RequestHandler
- [x] 0.5đ MVC: FXML + Controller + ViewRouter (client); RequestHandler → Service → DAO (server)
- [x] 0.5đ Maven multi-module, Google Java Style, SLF4J+Logback
- [x] 0.5đ Unit Test JUnit ≥ 40 test cases
- [x] 0.5đ CI/CD: GitHub Actions + test tự động

## Nâng cao (1.0đ)
- [x] 0.5đ Anti-Sniping: threshold 60s, gia hạn 60s, AuctionExtendedEvent push realtime, client reset countdown
- [x] 0.5đ Price Chart: LineChart realtime, addDataPoint từ EventListenerThread, lịch sử + live data
```

Tổ chức mock Q&A 30 phút: mỗi người giải thích 1 class do người khác viết.

Tag release: `git tag v1.0.0 -m "Final submission BidHub"` → push.

**✅ Đầu ra:**
- `mvn test` → ≥ 40 tests, 0 failures
- `SUBMISSION_CHECKLIST.md` tick đầy đủ
- Tag `v1.0.0` trong GitHub Releases
- **Mỗi thành viên giải thích được bất kỳ class nào**

---

## Bảng điểm dự kiến

| Tiêu chí | Điểm | Tuần xong | Người chịu trách nhiệm |
|----------|------|-----------|------------------------|
| Thiết kế lớp & cây kế thừa | 0.5 | Tuần 2 | Đăng + Quốc Minh + Công Minh |
| OOP principles (4 trụ cột) | 1.0 | Tuần 2 | Cả nhóm |
| Design Patterns (Singleton × 5, Factory, Observer) | 1.0 | Tuần 3, 4, 7 | Đăng + Quốc Minh |
| Quản lý người dùng & sản phẩm | 1.0 | Tuần 5 | Quốc Minh + Công Minh |
| Chức năng đấu giá | 1.0 | Tuần 6 | Quốc Minh + Khoa |
| Xử lý lỗi & ngoại lệ | 1.0 | Tuần 2, 5, 6 | Khoa + Quốc Minh |
| Concurrent bidding an toàn | 1.0 | Tuần 7 | Đăng + Khoa |
| Realtime update (Observer/Socket) | 0.5 | Tuần 7 | Quốc Minh + Công Minh |
| Kiến trúc Client-Server | 0.5 | Tuần 4 | Đăng + Quốc Minh |
| MVC (JavaFX+FXML / RequestHandler→DAO) | 0.5 | Tuần 3, 4 | Công Minh + Đăng |
| Maven, coding convention, SLF4J | 0.5 | Tuần 1, 9 | Đăng + Quốc Minh |
| Unit Test JUnit | 0.5 | Mỗi tuần | Khoa |
| CI/CD GitHub Actions | 0.5 | Tuần 1, 9 | Quốc Minh |
| **Tổng bắt buộc** | **9.0** | | |
| Anti-Sniping | 0.5 | Tuần 8 | Đăng + Quốc Minh |
| Price Chart LineChart | 0.5 | Tuần 8 | Công Minh |
| **Tổng nâng cao** | **1.0** | | |
| **TỔNG DỰ KIẾN** | **10.0** | | |

---

## Lưu ý quan trọng

> [!TIP]
> **Thứ tự implement đúng:** Model (Tuần 2) → DAO (Tuần 3) → RequestHandler (Tuần 4–6) → UI (Tuần 5–6). Không nhảy vào UI khi chưa có logic server.

> [!TIP]
> **Dùng in-memory SQLite (`jdbc:sqlite::memory:`) cho tất cả DAO test** — nhanh, không để lại file `.db` trong repo.

> [!TIP]
> **Dùng Jackson** cho toàn bộ JSON (static `ObjectMapper` — thread-safe). Không dùng Gson.

> [!WARNING]
> **ReentrantLock PHẢI trong try-finally.** Quên `unlock()` trong finally → deadlock khi exception → toàn bộ auction bị treo.

> [!CAUTION]
> **Quy tắc sống còn:** Mỗi thành viên phải giải thích được **BẤT KỲ dòng code nào**. Giảng viên hỏi bất kỳ ai — không trả lời được → **0 điểm TOÀN NHÓM**.

### Khi gặp khó khăn
1. Google + đọc Javadoc chính thức (15 phút)
2. Hỏi nhóm trên Discord/Zalo
3. Dùng AI để **hiểu concept** — nhưng phải hiểu code trước khi commit
4. Vẫn stuck → đặt câu hỏi cụ thể trong buổi họp nhóm cuối tuần
