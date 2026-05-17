# BidHub Refactor & Debug Plan v2 — refactor1.md

> **Mục đích**: AI đọc file này → hiểu luật → tự refactor/debug toàn bộ dự án mà KHÔNG thay đổi công nghệ, luồng hoạt động, hay cách sử dụng.
> **Người thực hiện**: 2 người (A và B) — chia theo module, có thể làm song song.
> **Đầu ra**: Code sạch, đúng logic, không sót file.
> **Cập nhật từ v1**: Thêm 45+ vấn đề mới, sửa 2 vấn đề sai (GET_BID_HISTORY_REPORT "ALL" hoạt động đúng, AuctionStatus chỉ có OPEN không có PENDING), thêm client-backend gap analysis.

---

## PHẦN 1: LUẬT CHO AI TRƯỚC KHI REFACTOR

### 🔒 NGUYÊN TẮC BẮT BUỘC

```
1. KHÔNG thay đổi công nghệ:
   - Giữ JavaFX + FXML cho client
   - Giữ TCP Socket + JSON cho network
   - Giữ SQLite + JDBC cho database
   - Giữ Jackson cho JSON
   - Giữ Maven cho build
   - Giữ SLF4J + Logback cho logging

2. KHÔNG thay đổi luồng hoạt động:
   - Giữ nguyên 27 API commands (+ 2 admin commands)
   - Giữ nguyên request → response flow
   - Giữ nguyên event push flow (NotificationBroker → Session)
   - Giữ nguyên auction lifecycle (OPEN → RUNNING → FINISHED → PAID/CANCELED)
   - Giữ nguyên anti-sniping mechanism
   - Giữ nguyên 2-socket client (1 request + 1 event)
   - Giữ nguyên ViewRouter navigation

3. KHÔNG thay đổi giao diện người dùng:
   - Giữ nguyên cấu trúc FXML
   - Giữ nguyên các màn hình và flow điều hướng
   - Giữ nguyên dark theme + indigo accent
   - Chỉ sửa lỗi hiển thị (màu sai, nút thiếu, CSS conflict)

4. KHÔNG thêm thư viện mới:
   - Không thêm framework DI (Spring, Guice)
   - Không thêm ORM (Hibernate, JPA)
   - Không thêm thư viện validation
   - Chỉ dùng Java standard library + các thư viện đã có

5. GIỮ NGUYÊN signature của DAO methods public:
   - Không đổi tên method public
   - Không đổi tham số method public
   - Có thể thêm method mới nếu cần
   - Có thể đổi method private/protected

6. GIỮ NGUYÊN database schema (5 bảng):
   - Chỉ ĐƯỢC THÊM index, constraint, UNIQUE
   - KHÔNG xóa/cột/đổi tên cột
   - KHÔNG thêm bảng mới

7. Test phải pass:
   - Sau refactor, tất cả test hiện có phải vẫn pass
   - Không xóa test, chỉ có thể sửa test nếu logic sửa yêu cầu
```

### ✅ NHỮNG GÌ ĐƯỢC LÀM

```
1.  Sửa bug logic, null pointer, race condition
2.  Sửa resource leak (socket, connection, thread, timeline)
3.  Thêm validation thiếu
4.  Thêm synchronized/volatile cho thread safety
5.  Xóa dead code, duplicate code
6.  Tách method dài, gộp code lặp
7.  Sửa CSS conflict, FXML thiếu
8.  Thêm index cho database
9.  Đổi tên biến/method cho dễ hiểu (chỉ private/protected)
10. Thêm null check, error handling
11. Sửa exception hierarchy (errorCode bị mất)
12. Thêm cleanup lifecycle cho controller
13. Thêm shutdown hook cho server
14. Sửa floating-point cho tiền tệ (dùng long/BigDecimal ở logic, không đổi DB)
15. Tách God Class (RequestHandler 1391 dòng → dispatcher + sub-handlers)
16. Thêm API call thiếu từ client (RUN_INTEGRITY_CHECK, GET_ITEM_DETAIL)
17. Sửa client-backend mismatch (CANCEL_AUCTION dùng "PENDING" thay vì OPEN)
```

---

## PHẦN 2: CHIA VIỆC — NGƯỜI A VÀ NGƯỜI B

### 👤 NGƯỜI A — SERVER + COMMON + DB

**Trách nhiệm**: Toàn bộ server module + common module + database + server-side bug

---

#### MODULE: bidhub-common

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| A1 | `Entity.java` | `equals()` dùng `instanceof` không check `getClass()` → Bidder id="x" equals Auction id="x" nếu cùng id | CRITICAL | Thêm `getClass() != o.getClass()` check |
| A2 | `Entity.java` | `toString()` gọi `id.substring(0,7)` → crash nếu id < 7 ký tự | LOW | `id.substring(0, Math.min(7, id.length()))` |
| A3 | `Entity.java` | `markUpdated()` không thread-safe — `updatedAt` không volatile | MEDIUM | Thêm volatile hoặc document lock requirement |
| A4 | `MessageMapper.java` | `toJson()` catch block xây JSON thủ công → leak thông tin + injection nếu message chứa `"` | HIGH | Dùng `MAPPER.writeValueAsString()` cho error response |
| A5 | `MessageMapper.java` | `fromJson()` ném `throws Exception` quá rộng | MEDIUM | Ném `JsonProcessingException` cụ thể |
| A6 | `MessageMapper.java` | `getMapper()` expose shared ObjectMapper mutable | MEDIUM | Trả về unmodifiable hoặc che method |
| A7 | `MessageResponse.java` | Status "OK"/"ERROR" là string literal → dễ typo | HIGH | Tạo constant `STATUS_OK = "OK"`, `STATUS_ERROR = "ERROR"` |
| A8 | `BidHubException.java` | Constructor 1 tham số set `errorCode = "UNKNOWN_ERROR"` → subclass vô tình gọi super(message) sẽ sai error code | MEDIUM | Xóa hoặc làm protected constructor 1 tham số |
| A9 | `AuctionClosedException.java` | Constructor 1 tham số gọi `super(message)` → errorCode = "UNKNOWN_ERROR" thay vì "AUCTION_CLOSED" | CRITICAL | Đổi thành `super(message, ERROR_CODE)` |
| A10 | `ValidationException.java` | `Objects.requireNonNull` gọi SAU `super()` → NPE từ super xảy ra trước, message không rõ | HIGH | Move null-check trước super() hoặc bỏ |
| A11 | `MessageRequest.java` | Không validate field sau Jackson deserialize → type/token/payload có thể null | LOW | Thêm `validate()` method hoặc `@NotNull` |

---

#### MODULE: bidhub-server — CONFIG

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| A12 | `MigrationRunner.java` | Split SQL bằng `;` → sai nếu string value chứa dấu `;` | CRITICAL | Dùng delimiter-aware parser hoặc loại comment trước split |
| A13 | `DbConnectionProvider.java` | `PRAGMA journal_mode=WAL` chạy trên MỖI connection → thừa | MEDIUM | Chạy 1 lần khi khởi tạo |
| A14 | `DbConnectionProvider.java` | Không có method reset singleton cho test | MEDIUM | Thêm `@VisibleForTesting static void reset()` |
| A15 | `DbConnectionProvider.java` | Không tạo thư mục cha cho `db.path` → SQLite fail nếu `data/` không tồn tại | LOW | Thêm `Files.createDirectories()` trong init |

---

#### MODULE: bidhub-server — MODEL

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| A16 | `Auction.java` | No-arg constructor để `itemId=null` → NPE trong `transitionTo()` | CRITICAL | Xóa no-arg constructor hoặc thêm null-guard |
| A17 | `Auction.java` | `setCurrentHighestBid()`/`setHighestBidderId()` KHÔNG gọi `markUpdated()` → updatedAt không cập nhật sau bid | CRITICAL | Thêm `markUpdated()` hoặc làm private, ép dùng `updateHighestBid()` |
| A18 | `Auction.java` | `isValidBid()` không kiểm tra `minimumIncrement` → cho phép bid tăng 1đ | HIGH | Thêm check `bidAmount - currentHighestBid >= minimumIncrement` |
| A19 | `Auction.java` | DB-load constructor không validate startingPrice/endTime → corrupt DB data im lặng chấp nhận | MEDIUM | Thêm validation |
| A20 | `Auction.java` | `transitionTo()` gọi `getId().substring(0,7)` → crash nếu id ngắn | MEDIUM | Thêm guard |
| A21 | `Auction.java` | ReentrantLock không transient → 2 Auction cùng ID có lock khác nhau, nhưng AuctionManager giữ instance canonical nên OK | LOW | Document rõ |
| A22 | `AuctionStatus.java` | `handleCancelAuction` check `"PENDING"` nhưng enum chỉ có `OPEN` → CANCEL AUCTION LUÔN FAIL | CRITICAL | Đổi check thành `auc.getStatus() == AuctionStatus.OPEN` + thêm `OPEN → CANCELED` vào canTransitionTo() |
| A23 | `AuctionStatus.java` | `isTerminal()` trả true cho FINISHED nhưng FINISHED → PAID/CANCELED là transition hợp lệ → mâu thuẫn | MEDIUM | Bỏ FINISHED khỏi isTerminal() hoặc đổi định nghĩa |
| A24 | `BidTransaction.java` | Constructor 7 tham số (DB-load) dead code — không ai gọi | CRITICAL | Xóa constructor thừa |
| A25 | `BidTransaction.java` | DB-load constructor không validate auctionId/bidderId null | MEDIUM | Thêm null check |
| A26 | `BidTransaction.java` | `toString()` substring → crash nếu id ngắn | LOW | Thêm guard |
| A27 | `User.java` | `setLocked()` không gọi `markUpdated()` | HIGH | Thêm `markUpdated()` |
| A28 | `User.java` | DB-load constructor không set `locked` field → mặc định false, mapRow() fix sau nhưng fragile | MEDIUM | Thêm `locked` parameter |
| A29 | `User.java` | `getPasswordHash()` public → security smell | LOW | Giảm visibility |
| A30 | `Item.java` | `printInfo()` dùng `{:,.0f}` → SLF4J không hỗ trợ format specifier, in ra raw | HIGH | Dùng `String.format()` trước rồi truyền logger |
| A31 | `Item.java` | DB-load constructor không validate | MEDIUM | Thêm validation |
| A32 | `Art/Electronics/Vehicle` | DB-load constructor ném `IllegalArgumentException` nhưng normal constructor ném `NullPointerException` cho cùng lỗi | MEDIUM | Thống nhất dùng `Objects.requireNonNull()` |

---

#### MODULE: bidhub-server — DAO

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| A33 | `AuctionDao.java` | `double` cho tiền tệ → sai lệch floating-point | CRITICAL | Dùng `long` (VND nguyên) hoặc `BigDecimal` trong logic; DB giữ REAL |
| A34 | `AuctionDao.java` | `mapRow` catch generic Exception → mặc định FINISHED khi parse status lỗi | HIGH | Log warning hoặc ném exception |
| A35 | `AuctionDao.java` | `runUpdate` dùng `setString` cho tất cả param → mất type safety | MEDIUM | Tạo overload với proper type binding |
| A36 | `AuctionDao.java` | `findAllWithBidInfo()` trả `List<Map>` → anti-pattern | MEDIUM | Tạo typed DTO |
| A37 | `ItemDao.java` | `updateItem()` dùng sentinel value `< 0` cho "không cập nhật" → nhầm với giá 0 | HIGH | Dùng null + COALESCE pattern |
| A38 | `ItemDao.java` | `updateItem()` gọi `findById()` trên 1 connection rồi `acquireConnection()` trên connection khác → lost update | HIGH | Dùng 1 connection cho cả read + write |
| A39 | `UserDao.java` | `mapRow()` catch SQLException bỏ qua → che lỗi thật | HIGH | Dùng `hasColumn` pattern hoặc try-catch cụ thể |
| A40 | `AuditLogDao.java` | `queryList` với nullable param → thiếu param cho PreparedStatement | MEDIUM | Thêm overloaded method hoặc validate |
| A41 | `BidDao.java` | `getHighestBid()` không có tiebreaker khi 2 bid cùng giá | MEDIUM | Thêm `ORDER BY bid_time ASC` |
| A42 | Tất cả DAO | MỖI method mở connection mới → không có connection pooling → overhead cao | MEDIUM | Implement simple connection pool hoặc reuse connection trong request |
| A43 | Tất cả DAO | Không có transaction support → handlePlaceBid làm 3 DB op trên 3 connection riêng, partial failure để DB inconsistent | HIGH | Thêm transaction support (Connection parameter) |

---

#### MODULE: bidhub-server — NETWORK

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| A44 | `RequestHandler.java` | Token trên MỖI request ghi đè `session.authenticatedUserId` → session hijack | CRITICAL | Chỉ set khi session chưa auth HOẶC khi login |
| A45 | `RequestHandler.java` | `handleCancelAuction` check `"PENDING"` thay vì `AuctionStatus.OPEN` → CANCEL LUÔN FAIL | CRITICAL | Đổi thành `auc.getStatus() != AuctionStatus.OPEN` |
| A46 | `RequestHandler.java` | Tạo `new AntiSnipingEngine()` mỗi bid → tốn resource | HIGH | Tạo 1 instance field |
| A47 | `RequestHandler.java` | `handleGetItemList` và `handleListMyItems` lặp logic ~40 dòng | MEDIUM | Tách method chung `buildItemAuctionStatusMap()` + `enrichItemInfo()` |
| A48 | `RequestHandler.java` | N+1 query: mỗi item/auction gọi `userDao.findById()` | HIGH | Batch load sellers |
| A49 | `RequestHandler.java` | `handleGetItemList`/`handleListMyItems` load ALL auctions chỉ để build status map | HIGH | Tạo targeted DAO query |
| A50 | `RequestHandler.java` | Dead fields: `injectedUserDao`/`injectedItemDao` kiểu Object + 3 constructor lặp | MEDIUM | Xóa dead fields, gộp 3 constructor → 1 + DI |
| A51 | `RequestHandler.java` | Import `ItemDao` 2 lần + nhiều import thừa | LOW | Xóa import thừa |
| A52 | `RequestHandler.java` | `handleGetHomeStats()` load ALL bids chỉ để count distinct bidders → chậm với data lớn | MEDIUM | Tạo `bidDao.countDistinctBidders()` query |
| A53 | `RequestHandler.java` | `handleSendNotification()` xây JSON thủ công bằng string concatenation → injection | MEDIUM | Dùng ObjectMapper |
| A54 | `RequestHandler.java` | `handleGetAuctionReport()` không restrict theo SELLER → BIDDER cũng xem được | MEDIUM | Thêm role check hoặc filter |
| A55 | `RequestHandler.java` | `ADMIN_STOP_AUCTION`/`ADMIN_DELETE_AUCTION` không update RAM cache → auction vẫn nhận bid sau khi stop/delete | HIGH | Gọi `AuctionManager.getInstance().removeAuction(auctionId)` |
| A56 | `RequestHandler.java` | `ADMIN_STOP_AUCTION`/`ADMIN_DELETE_AUCTION` không trong AUTH_REQUIRED set | LOW | Thêm vào AUTH_REQUIRED |
| A57 | `RequestHandler.java` | 1391 dòng, 27 handler → God Class anti-pattern | MEDIUM | Tách thành AuthHandler + ItemHandler + AuctionHandler + AdminHandler + ReportHandler (không đổi luồng, chỉ delegate) |
| A58 | `SecurityContext.java` | Tạo `new UserDao()` mỗi lần gọi `requireRole()` → connection churn | HIGH | Inject hoặc cache UserDao |
| A59 | `SecurityContext.java` | `requireRole` query DB mỗi request → có thể cache role trong Session | MEDIUM | Cache role trong Session hoặc SessionManager |
| A60 | `Session.java` | `authenticatedUserId` không volatile/synchronized → race condition | HIGH | Thêm volatile hoặc synchronized getter/setter |
| A61 | `Session.java` | `disconnect()` nuốt IOException + không close PrintWriter | MEDIUM | Close theo thứ tự: writer → reader → socket + log DEBUG |
| A62 | `SocketServerCore.java` | Không có shutdown hook → resource leak khi kill process | MEDIUM | Thêm shutdown hook |
| A63 | `SocketServerCore.java` | Thread pool size 30 hardcoded | LOW | Đọc từ ConfigLoader |
| A64 | `ClientConnectionThread.java` | Tạo `new RequestHandler()` mỗi connection → 10+ DAO instances mới | MEDIUM | Tạo 1 RequestHandler trong SocketServerCore, pass vào thread |

---

#### MODULE: bidhub-server — SERVICE

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| A65 | `AuctionLifecycleTask.java` | `closeAuction` query winner 2 LẦN (trong lock và ngoài lock) → race condition, winner không nhất quán | CRITICAL | Lưu winner từ trong lock, dùng lại cho event + audit |
| A66 | `AuctionLifecycleTask.java` | `activateAuction` dùng `System.out.println` thay vì logger | HIGH | Đổi thành logger |
| A67 | `AuctionLifecycleTask.java` | Tạo `new AuctionDao()`/`new BidDao()` mỗi lần chạy → connection churn | MEDIUM | Tạo instance fields |
| A68 | `AntiSnipingEngine.java` | Tạo `new AuditLogService()` mỗi lần `check()` → connection churn | HIGH | Tạo instance field |
| A69 | `AntiSnipingEngine.java` | `ConfigLoader.getInt()` ném exception nếu thiếu key | MEDIUM | Dùng `getOrDefault` với default value |
| A70 | `AuctionManager.java` | Gap 5s giữa load và lifecycle check đầu tiên → auction có thể hết hạn trong gap | MEDIUM | Chạy lifecycle task ngay sau load |
| A71 | `AuctionManager.java` | `start()` có thể gọi nhiều lần → duplicate scheduled tasks | MEDIUM | Thêm `started` boolean guard |
| A72 | `AuctionManager.java` | `stop()` không bao giờ được gọi | MEDIUM | Thêm shutdown hook |
| A73 | `NotificationBroker.java` | `publish()` remove session trong lúc iterate → O(n²) | HIGH | Collect failed sessions, xóa sau iteration |
| A74 | `NotificationBroker.java` | `subscribe()` có TOCTOU race condition | MEDIUM | Dùng `computeIfAbsent` + lambda thêm session |
| A75 | `NotificationBroker.java` | Không cleanup empty subscriber lists → memory leak | LOW | Xóa key khi list rỗng |
| A76 | `SessionManager.java` | synchronized write nhưng KHÔNG synchronized read → partial thread safety | HIGH | Đồng bộ tất cả method hoặc dùng cùng lock |
| A77 | `SessionManager.java` | `clearAll()` không synchronized → race với create/invalidate | MEDIUM | Thêm synchronized |
| A78 | `AuthService.java` | SHA-256 không salt → dễ rainbow table | MEDIUM | (Acknowledge — không fix trong scope này) |
| A79 | `AuthService.java` | `verifyPassword` dùng `String.equals()` → timing attack | MEDIUM | Dùng `MessageDigest.isEqual()` |
| A80 | `BidValidator.java` | `getItemOwnerId` query DB trong khi giữ auction lock → giảm throughput | MEDIUM | Cache item owner hoặc validate ngoài lock |
| A81 | `DataIntegrityService.java` | `checkBidConsistency` load ALL users vào memory nhưng không dùng | MEDIUM | Xóa unused user loading |
| A82 | `ReportService.java` | N+1 query trong `exportAuctionReport()` — mỗi auction query item + user riêng | HIGH | Batch fetch items + users |
| A83 | `ReportService.java` | N+1 query trong `exportBidHistory()` — mỗi bid query auction + item + user riêng | HIGH | Batch fetch |
| A84 | `ReportService.java` | N+1 query trong `exportAuditLog()` — mỗi log entry query user riêng | HIGH | Batch fetch |

---

#### MODULE: bidhub-server — APP + RESOURCES + POM

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| A85 | `ServerApp.java` | Không có shutdown hook → resource leak khi kill process | HIGH | Thêm `Runtime.getRuntime().addShutdownHook()` |
| A86 | `ServerApp.java` | `AuctionManager.start()` trước `server.start()` → nếu port fail, scheduler không dừng được | MEDIUM | Wrap try-catch hoặc start AuctionManager sau socket bound |
| A87 | `ServerApp.java` | Log message outdated "tuần 4" | LOW | Cập nhật log message |
| A88 | `schema.sql` | THIẾU index → mọi query full scan | HIGH | Thêm index: auctions(status), auctions(item_id), items(seller_id), bid_transactions(auction_id), bid_transactions(bidder_id), audit_logs(user_id), audit_logs(created_at) |
| A89 | `schema.sql` | `users.email` thiếu UNIQUE constraint → cho phép duplicate email | HIGH | Thêm UNIQUE constraint |
| A90 | `schema.sql` | `items.seller_id` thiếu FK constraint → orphan items | MEDIUM | Thêm FOREIGN KEY |
| A91 | `schema.sql` | `auctions.highest_bidder_id` thiếu FK constraint | MEDIUM | Thêm FOREIGN KEY |
| A92 | `bidhub-server/pom.xml` | `maven-surefire-plugin` khai báo 2 LẦN → cái đầu dead code | HIGH | Gộp thành 1 khai báo |
| A93 | `bidhub-server/pom.xml` | `jackson-databind` khai báo thừa (đã có qua common) | MEDIUM | Xóa |
| A94 | `pom.xml` (parent) | `jackson-datatype-jsr310` chỉ trong dependencyManagement, server không khai báo explicit → fragile transitive | LOW | Thêm explicit dependency trong server pom |

---

### 👤 NGƯỜI B — CLIENT + FXML + CSS

**Trách nhiệm**: Toàn bộ client module + FXML + CSS + client-side bug + client-backend gap

---

#### MODULE: bidhub-client — CORE NETWORK

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| B1 | `ViewRouter.java` | KHÔNG cleanup controller cũ khi navigate → leak socket, thread, timeline | CRITICAL | Thêm lifecycle hook (interface `Navigable` với `onNavigateAway()`) |
| B2 | `ViewRouter.java` | `isAuthView` check hardcoded 3 view name → dễ sót khi thêm view | MEDIUM | Dùng Set<String> hoặc convention |
| B3 | `ClientSession.java` | `login()`/`logout()` KHÔNG synchronized → race condition | CRITICAL | Thêm synchronized hoặc volatile cho tất cả field |
| B4 | `ClientSession.java` | `getCurrentRole()` trả null → `String.valueOf(null)` = "null" → hiện "Vai trò: null" trên UI | HIGH | Thêm null check ở mọi nơi dùng role |
| B5 | `ServerGateway.java` | `connect()`/`disconnect()` KHÔNG synchronized → race với `sendRequest()` | CRITICAL | Thêm synchronized cho tất cả method |
| B6 | `ServerGateway.java` | `sendRequest()` giữ lock khi blocking I/O → treo nếu server không phản hồi | HIGH | Thêm socket timeout `setSoTimeout(30000)` |
| B7 | `ServerGateway.java` | `disconnect()` không close writer/reader trước khi set null | HIGH | Close theo thứ tự: writer → reader → socket |
| B8 | `ServerGateway.java` | `getSocket()` expose internal state | MEDIUM | Xóa method hoặc làm package-private |
| B9 | `ServerGateway.java` | `new Socket(host, port)` không có connect timeout → block mãi nếu server unreachable | MEDIUM | Dùng `socket.connect(new InetSocketAddress(host, port), 5000)` |
| B10 | `EventListenerThread.java` | `stop()` KHÔNG interrupt thread → thread block mãi trên `readLine()` | CRITICAL | Thêm `interrupt()` hoặc close socket/reader trong stop() |
| B11 | `BidHubApp.java` | Không có shutdown hook → ServerGateway.disconnect() không gọi khi close window | MEDIUM | `primaryStage.setOnCloseRequest(e -> { disconnect(); Platform.exit(); })` |
| B12 | `BidHubApp.java` | UI disabled khi connect fail, không có retry → phải restart app | MEDIUM | Thêm "Retry" button trong error dialog |

---

#### MODULE: bidhub-client — CONTROLLERS

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| B13 | `AuctionDetailController.java` | Event socket + listener thread leak khi navigate qua sidebar (không gọi cleanup) | CRITICAL | Đăng ký cleanup hook với ViewRouter (liên quan B1) |
| B14 | `AuctionDetailController.java` | `subscribeRealtimeEvents()` tạo TCP socket TRÊN FX thread → freeze UI | CRITICAL | Chạy trong background task / NetworkTask |
| B15 | `AuctionDetailController.java` | Flash Timeline không lưu reference → leak trên mỗi bid update | HIGH | Lưu reference, stop trong cleanup() hoặc reuse Timeline |
| B16 | `AuctionDetailController.java` | Không validate bid-against-self ở client → gửi request rồi bị server reject, UX kém | MEDIUM | So sánh userId với highestBidderId/sellerId trước khi gửi |
| B17 | `AuctionListController.java` | Timeline leak — `activeTimelines` không stop khi navigate away | CRITICAL | Thêm cleanup() + gọi từ ViewRouter |
| B18 | `AuctionListController.java` | Price filter dùng `startingPrice` nhưng hiển thị `currentHighestBid` → inconsistent | HIGH | Dùng cùng giá cho filter và display |
| B19 | `AuctionListController.java` | Silent exception swallowing khi parse price | MEDIUM | Hiển thị lỗi hoặc dùng default |
| B20 | `CreateItemController.java` | `Integer.parseInt()` trên field có decimal filter → crash khi nhập "12.5" | CRITICAL | Tạo `applyIntegerFilter()` hoặc parse bằng Double rồi cast |
| B21 | `CreateItemController.java` | Image URL dùng `file:///` URI → chỉ hiện trên máy local, người khác thấy broken image | HIGH | Upload server/CDN hoặc warn chỉ hỗ trợ URL |
| B22 | `CreateItemController.java` | Chỉ cho SELLER tạo item, ADMIN cũng nên được tạo | MEDIUM | Thêm ADMIN vào role check |
| B23 | `CreateAuctionController.java` | KHÔNG validate start time < end time | HIGH | Thêm validation |
| B24 | `CreateAuctionController.java` | KHÔNG validate thời gian trong tương lai | HIGH | So sánh với `LocalDateTime.now()` |
| B25 | `CreateAuctionController.java` | Spinner `editable=true` nhưng không validate giá trị nhập | HIGH | Thêm commit listener hoặc TextFormatter |
| B26 | `CreateAuctionController.java` | 6 Spinner setup giống nhau → DRY violation | MEDIUM | Extract helper `createSpinner(min, max, default)` |
| B27 | `RegisterController.java` | Password match binding KHÔNG watch visible text fields → stale validation | CRITICAL | Thêm visible field listeners vào binding |
| B28 | `RegisterController.java` | `registerButton.disableProperty().unbind()` không re-bind → nút không disable lại | HIGH | Re-bind sau khi task hoàn thành |
| B29 | `RegisterController.java` | Email validation chỉ check `contains("@")` → chấp nhận "@@@" | MEDIUM | Dùng regex cơ bản |
| B30 | `RegisterController.java` | Role buttons nên dùng ToggleGroup thay vì manual style toggle | MEDIUM | Đổi thành ToggleButton + ToggleGroup |
| B31 | `RegisterController.java` | Không validate username length/characters | MEDIUM | Thêm min-length + character restrictions |
| B32 | `LoginController.java` | Redundant `Platform.runLater()` trong `setOnSucceeded` | HIGH | Xóa — đã chạy trên FX thread |
| B33 | `LoginController.java` | ClassCastException risk khi cast payload | HIGH | Dùng safe cast + null check |
| B34 | `LoginController.java` | Không có ENTER key handler → phải click button | LOW | Thêm `setOnAction` |
| B35 | `MainLayoutController.java` | `String.valueOf(null)` hiện "null" cho role | HIGH | Null check trước khi display |
| B36 | `MainLayoutController.java` | Username/role chỉ load 1 lần → không refresh khi đổi user | MEDIUM | Thêm refresh method hoặc listener |
| B37 | `AdminController.java` | NPE khi `payload` null trong `loadAuctionReport()` | CRITICAL | Thêm null check trước khi gọi `isArray()` |
| B38 | `AdminController.java` | `String.valueOf(null)` cho role check | HIGH | Null check |
| B39 | `AdminController.java` | KHÔNG có confirmation dialog cho stopAuction/deleteAuction/lock/unlock | HIGH | Thêm Alert(AlertType.CONFIRMATION) |
| B40 | `AdminController.java` | `executeRequest` serialize response → JSON → re-parse → waste | MEDIUM | Work với MessageResponse trực tiếp |
| B41 | `AdminController.java` | Tab onSelectionChanged fire cả select AND deselect → load data 2 lần | MEDIUM | Check `Tab.isSelected()` trước khi load |
| B42 | `AdminController.java` | `RUN_INTEGRITY_CHECK` khai báo constant nhưng KHÔNG BAO GIỜ GỌI → dead code | CRITICAL | Thêm nút UI + handler gọi API |
| B43 | `NotificationController.java` | `handleMarkAllRead()` chỉ local → không persist server | HIGH | Gửi request lên server (cần API hoặc dùng PATCH) |
| B44 | `NotificationController.java` | `String.valueOf(null)` hiện "null" cho role | HIGH | Null check |
| B45 | `NotificationController.java` | Admin panel có thể hiện cho non-admin | HIGH | Thêm `else` ẩn admin panel |
| B46 | `NotificationController.java` | Demo notifications trộn lẫn với real data → nhầm lẫn | MEDIUM | Label rõ "demo" hoặc không trộn |
| B47 | `NotificationController.java` | Hover effect dùng fragile string replacement | MEDIUM | Dùng CSS pseudo-classes |
| B48 | `NotificationController.java` | `sendMarkReadRequest()` tạo raw thread không quản lý | MEDIUM | Dùng shared ExecutorService |
| B49 | `SellerDashboardController.java` | `setLoading(false)` gọi bởi 2 task song song → loading biến mất sớm | HIGH | Dùng counter hoặc AtomicInteger |
| B50 | `SellerDashboardController.java` | Fallback filter bằng `sellerName` thay vì `sellerId` → sai nếu trùng tên | HIGH | Dùng sellerId |
| B51 | `SellerDashboardController.java` | Không có role check → BIDDER có thể truy cập seller dashboard | HIGH | Check role + redirect |
| B52 | `HomeController.java` | Polling `Thread.sleep()` đợi connection → fragile | HIGH | Dùng listener/callback pattern |
| B53 | `HomeController.java` | ClassCastException risk khi parse stats | MEDIUM | Safe parse với try-catch |
| B54 | `HomeController.java` | Placeholder image phụ thuộc URL ngoài → fail nếu placehold.co down | MEDIUM | Bundle local placeholder |
| B55 | `ItemCatalogController.java` | Không gọi `GET_ITEM_DETAIL` khi click item → xem data cũ từ list | HIGH | Gọi GET_ITEM_DETAIL API |
| B56 | `ItemCatalogController.java` | Dialog hiển thị không apply stylesheet → light theme trên dark app | MEDIUM | Thêm stylesheet vào dialog scene |

---

#### MODULE: bidhub-client — SERVICE + UTILS

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| B57 | `BidChartService.java` | Không giới hạn data points → chart chậm sau ~200 bids | HIGH | Cap tại 100 points, xóa cũ nhất |
| B58 | `UiUtils.java` | `applyNumericFilter()` cho phép decimal nhưng dùng cho integer field → crash | HIGH | Thêm `applyIntegerFilter()` |
| B59 | `UiUtils.java` | `validatePositiveNumber()` không check empty trước → crash | MEDIUM | Gọi `validateNotEmpty()` trước |
| B60 | `UiUtils.java` | `showError()` stacking modal dialogs | MEDIUM | Dùng `show()` thay vì `showAndWait()` |
| B61 | Nhiều controllers | `translateType()` lặp trong 3 controllers | MEDIUM | Tách vào `UiUtils` |
| B62 | Nhiều controllers | `loadImageSafely()` lặp trong 3 controllers | MEDIUM | Tách vào `UiUtils` |
| B63 | Nhiều controllers | Card style strings (styleNormal/styleHover) lặp trong 4 controllers | MEDIUM | Tách vào CSS class hoặc `UiUtils` |
| B64 | Nhiều controllers | `ObjectMapper` tạo mới mỗi controller | MEDIUM | Tạo shared static instance |
| B65 | Nhiều controllers | `new Thread(task).start()` everywhere → không có thread pool, không giới hạn | MEDIUM | Tạo shared ExecutorService |
| B66 | 2 controllers | `formatDateTime()` lặp | LOW | Tách vào `UiUtils` |
| B67 | 2 controllers | Password toggle logic lặp (Login + Register) | LOW | Extract PasswordToggleHelper |

---

#### MODULE: bidhub-client — FXML

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| B68 | `LoginView.fxml` | Demo credentials hardcoded trong UI → lộ mật khẩu | CRITICAL | Xóa hoặc ẩn phía production |
| B69 | `AuctionListView.fxml` | 4 ToggleButton KHÔNG có ToggleGroup → chọn nhiều status cùng lúc | HIGH | Thêm `<ToggleGroup>` |
| B70 | `ItemCatalogView.fxml` | 4 ToggleButton KHÔNG có ToggleGroup → cùng lỗi B69 | HIGH | Thêm `<ToggleGroup>` |
| B71 | `AuctionDetailView.fxml` | Controller khai báo `btnZoomIn/Out/Reset` nhưng FXML KHÔNG có nút → null | HIGH | Thêm nút vào FXML hoặc xóa field trong controller |
| B72 | `CreateAuctionView.fxml` | Light theme trên app dark theme → lệch màu nghiêm trọng | HIGH | Đổi sang dark theme colors |
| B73 | `CreateItemView.fxml` | Light theme + dark text trên dark background → không nhìn thấy | HIGH | Đổi sang dark theme colors |
| B74 | `CreateItemView.fxml` | Dynamic section dùng màu sáng (#f1f3f5, #fff9db, #e7f5ff) → lệch dark theme | MEDIUM | Đổi sang dark-themed colors |
| B75 | `AdminView.fxml` | Tham chiếu CSS classes KHÔNG tồn tại (.container, .header, .btn-secondary, v.v.) | HIGH | Thêm CSS classes vào styles.css |
| B76 | `AdminView.fxml` | Không có `stylesheets` attribute → CSS không load | HIGH | Thêm `stylesheets="@../css/styles.css"` |
| B77 | `AdminView.fxml` | xmlns version khác các FXML khác (21 vs 17) | MEDIUM | Thống nhất version |
| B78 | `AdminView.fxml` | Duplicate column definitions (colAuctionItemName + colAuctionItem) | LOW | Xóa column thừa |

---

#### MODULE: bidhub-client — CSS

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| B79 | `login.css` | `.login-title` dùng `-fx-fill` thay vì `-fx-text-fill` → không áp dụng cho Label | HIGH | Đổi thành `-fx-text-fill` |
| B80 | `login.css` | Không được FXML nào tham chiếu → toàn bộ file dead code | HIGH | Thêm `stylesheets` vào LoginView.fxml + RegisterView.fxml |
| B81 | `login.css` | `.btn-primary` #8B5CF6 (violet) CONFLICT với styles.css #4F46E5 (indigo) | HIGH | Thống nhất màu primary |
| B82 | `login.css` | `.form-label` #FFFFFF (white) CONFLICT với styles.css #334155 (dark) | HIGH | Tách namespace hoặc đổi tên class |
| B83 | `styles.css` | `.root` background #E2E8F0 (light) nhưng tất cả FXML dùng #0B0E11 (dark) → dead style | HIGH | Đổi `.root` thành dark background #0B0E11 |
| B84 | `styles.css` | Thiếu nhiều CSS classes mà AdminView cần | HIGH | Thêm các classes thiếu |
| B85 | `styles.css` | `.content-pane` / `.login-card` dùng white bg → conflict với dark theme | MEDIUM | Đổi sang dark bg |

---

#### MODULE: bidhub-client — PROPERTIES

| STT | File | Vấn đề | Mức | Hướng sửa |
|-----|------|---------|-----|-----------|
| B86 | `client.properties` | Hardcoded `localhost` → chỉ chạy local | LOW | Hỗ trợ env variable override |

---

## PHẦN 3: CLIENT-BACKEND GAP ANALYSIS

### ❌ Backend có nhưng Client KHÔNG gọi

| # | Command | Chi tiết | Mức |
|---|---------|----------|-----|
| GAP1 | `RUN_INTEGRITY_CHECK` | Khai báo constant trong AdminController nhưng không invoke. Thiếu nút UI + handler. | CRITICAL |
| GAP2 | `GET_ITEM_DETAIL` | Server có handler nhưng ItemCatalogController chỉ dùng data từ GET_ITEM_LIST. Click item không lấy detail mới. | HIGH |
| GAP3 | `PING` | Server hỗ trợ nhưng client không dùng. | LOW |

### ⚠️ Client gọi nhưng có vấn đề

| # | Command | Vấn đề | Mức |
|---|---------|------|-----|
| GAP4 | `SUBSCRIBE_AUCTION` | Không gửi token qua event socket → server không biết ai subscribe | HIGH |
| GAP5 | `LOGOUT` | Ignore response → không biết server có invalidate token thật không | MEDIUM |
| GAP6 | `LOCK_USER/UNLOCK_USER` | Gửi duplicate field (targetUserId + userId cùng giá trị) | LOW |
| GAP7 | `GET_NOTIFICATIONS` | `isRead` luôn false từ server, mark-read chỉ local | HIGH |
| GAP8 | `UPDATE_ITEM` | Không gửi imageUrl → ảnh không cập nhật được | LOW |
| GAP9 | `CANCEL_AUCTION` | Client gửi đúng nhưng server check `"PENDING"` thay vì `OPEN` → luôn fail (bug server A22/A45) | CRITICAL |

---

## PHẦN 4: THỨ TỰ ƯU TIÊN

### 🔴 CRITICAL — Phải sửa trước (sẽ crash hoặc sai logic)

```
A1  - Entity.equals() cross-type equality
A9  - AuctionClosedException mất errorCode
A12 - MigrationRunner SQL split fragility
A16 - Auction no-arg constructor NPE
A17 - setCurrentHighestBid/setHighestBidderId không markUpdated
A22 - AuctionStatus: handleCancelAuction dùng "PENDING" thay vì OPEN
A24 - BidTransaction dead constructor
A33 - double cho tiền tệ
A44 - Session auth hijack
A45 - handleCancelAuction check "PENDING" → luôn fail
A65 - AuctionLifecycleTask double winner query

B1  - ViewRouter không cleanup controller cũ
B3  - ClientSession không synchronized
B5  - ServerGateway connect/disconnect không synchronized
B10 - EventListenerThread stop không interrupt
B13 - AuctionDetailController socket leak khi navigate sidebar
B14 - subscribeRealtimeEvents trên FX thread
B17 - AuctionListController timeline leak
B20 - Integer.parseInt trên decimal filter
B27 - RegisterController password binding không watch visible fields
B37 - AdminController NPE khi payload null
B42 - RUN_INTEGRITY_CHECK dead code
B68 - Demo credentials hardcoded trong UI

GAP1 - RUN_INTEGRITY_CHECK không gọi
GAP9 - CANCEL_AUCTION luôn fail (server bug)
```

### 🟠 HIGH — Sửa sau CRITICAL

```
A4, A7, A17-A18, A27, A30, A34, A37-A39, A43, A46, A48-A49, A55, A58, A60, A66, A68, A73, A76, A82-A84, A85, A88-A89, A92

B4, B6-B7, B15-B16, B18, B21-B25, B28, B32-B33, B35, B38-B39, B42-B45, B49-B52, B55, B57-B58, B69-B76, B79-B84

GAP2, GAP4, GAP7
```

### 🟡 MEDIUM — Sửa nếu còn thời gian

```
A3, A5-A6, A10, A13-A14, A19-A20, A23, A25, A28, A31-A32, A35-A36, A40-A42, A47, A50, A52-A54, A56, A59, A61, A63-A64, A67, A69-A72, A74, A77, A78-A81, A86, A90-A91, A93

B2, B9, B11-B12, B19, B22, B26, B29-B31, B36, B40-B41, B46-B48, B53-B54, B56, B59-B65, B74, B77, B85

GAP5, GAP8
```

### 🟢 LOW — Nice to have

```
A2, A11, A15, A21, A29, A51, A63, A87, A94

B8, B34, B66-B67, B78, B86

GAP3, GAP6
```

---

## PHẦN 5: FILE CHECKLIST — KIỂM TRA KHÔNG SÓT

### ✅ COMMON MODULE (12 files)

| File | Người | Issues |
|------|--------|--------|
| `Entity.java` | A | A1, A2, A3 |
| `MessageMapper.java` | A | A4, A5, A6 |
| `MessageRequest.java` | A | A11 |
| `MessageResponse.java` | A | A7 |
| `BidHubException.java` | A | A8 |
| `AuctionClosedException.java` | A | A9 |
| `AuctionNotFoundException.java` | A | (LOW) |
| `AuthenticationException.java` | A | (LOW) |
| `DuplicateUsernameException.java` | A | (LOW) |
| `InvalidBidException.java` | A | (LOW) |
| `UserNotFoundException.java` | A | (LOW) |
| `ValidationException.java` | A | A10 |

### ✅ SERVER — CONFIG (3 files)

| File | Người | Issues |
|------|--------|--------|
| `ConfigLoader.java` | A | (MEDIUM/LOW) |
| `DbConnectionProvider.java` | A | A13, A14, A15 |
| `MigrationRunner.java` | A | A12 |

### ✅ SERVER — MODEL (20 files)

| File | Người | Issues |
|------|--------|--------|
| `Auction.java` | A | A16-A21 |
| `AuctionStatus.java` | A | A22, A23 |
| `BidTransaction.java` | A | A24-A26 |
| `User.java` | A | A27-A29 |
| `Item.java` | A | A30, A31 |
| `AuditLog.java` | A | (LOW) |
| `Admin.java` | A | (no issue) |
| `Bidder.java` | A | (no issue) |
| `Seller.java` | A | (no issue) |
| `Electronics.java` | A | A32 |
| `Art.java` | A | A32 |
| `Vehicle.java` | A | A32 |
| `ElectronicsCreator.java` | A | (no issue) |
| `ArtCreator.java` | A | (no issue) |
| `VehicleCreator.java` | A | (no issue) |
| `ItemCreator.java` | A | (no issue) |
| `ItemType.java` | A | (no issue) |
| `UserRole.java` | A | (no issue) |
| `AuditActions.java` | A | (no issue) |
| `Displayable.java` | A | (no issue) |

### ✅ SERVER — DAO (5 files)

| File | Người | Issues |
|------|--------|--------|
| `AuctionDao.java` | A | A33-A36 |
| `AuditLogDao.java` | A | A40 |
| `BidDao.java` | A | A41 |
| `ItemDao.java` | A | A37, A38 |
| `UserDao.java` | A | A39 |

### ✅ SERVER — EVENT (3 files)

| File | Người | Issues |
|------|--------|--------|
| `AuctionClosedEvent.java` | A | (LOW - double cho winningBid) |
| `BidUpdateEvent.java` | A | (LOW - double cho bidAmount) |
| `AuctionExtendedEvent.java` | A | (MEDIUM - thiếu timestamp) |

### ✅ SERVER — NETWORK (5 files)

| File | Người | Issues |
|------|--------|--------|
| `RequestHandler.java` | A | A44-A57 |
| `SecurityContext.java` | A | A58, A59 |
| `Session.java` | A | A60, A61 |
| `ClientConnectionThread.java` | A | A64 |
| `SocketServerCore.java` | A | A62, A63 |

### ✅ SERVER — SERVICE (10 files)

| File | Người | Issues |
|------|--------|--------|
| `AntiSnipingEngine.java` | A | A68, A69 |
| `AuctionLifecycleTask.java` | A | A65-A67 |
| `AuctionManager.java` | A | A70-A72 |
| `AuditLogService.java` | A | (LOW) |
| `AuthService.java` | A | A78, A79 |
| `BidValidator.java` | A | A80 |
| `DataIntegrityService.java` | A | A81 |
| `NotificationBroker.java` | A | A73-A75 |
| `ReportService.java` | A | A82-A84 |
| `SessionManager.java` | A | A76, A77 |
| `AdminUserService.java` | A | (no major issue) |

### ✅ SERVER — APP + RESOURCES + POM (6 files)

| File | Người | Issues |
|------|--------|--------|
| `ServerApp.java` | A | A85-A87 |
| `Calculator.java` | A | (LOW) |
| `schema.sql` | A | A88-A91 |
| `server.properties` | A | (no issue) |
| `logback.xml` | A | (config only) |
| `bidhub-server/pom.xml` | A | A92, A93 |
| `pom.xml` (parent) | A | A94 |

### ✅ CLIENT — CORE (6 files)

| File | Người | Issues |
|------|--------|--------|
| `BidHubApp.java` | B | B11, B12 |
| `Launcher.java` | B | (no issue) |
| `ViewRouter.java` | B | B1, B2 |
| `ClientSession.java` | B | B3, B4 |
| `ServerGateway.java` | B | B5-B9 |
| `EventListenerThread.java` | B | B10 |

### ✅ CLIENT — CONTROLLERS (12 files)

| File | Người | Issues |
|------|--------|--------|
| `AuctionDetailController.java` | B | B13-B16 |
| `AuctionListController.java` | B | B17-B19 |
| `CreateItemController.java` | B | B20-B22 |
| `CreateAuctionController.java` | B | B23-B26 |
| `RegisterController.java` | B | B27-B31 |
| `LoginController.java` | B | B32-B34 |
| `MainLayoutController.java` | B | B35, B36 |
| `AdminController.java` | B | B37-B42 |
| `NotificationController.java` | B | B43-B48 |
| `SellerDashboardController.java` | B | B49-B51 |
| `HomeController.java` | B | B52-B54 |
| `ItemCatalogController.java` | B | B55, B56 |

### ✅ CLIENT — SERVICE + UTILS (4 files)

| File | Người | Issues |
|------|--------|--------|
| `BidChartService.java` | B | B57 |
| `UiUtils.java` | B | B58-B60 |
| `Views.java` | B | (no issue) |
| `NetworkTask.java` | B | (no issue) |

### ✅ CLIENT — FXML (12 files)

| File | Người | Issues |
|------|--------|--------|
| `LoginView.fxml` | B | B68 |
| `RegisterView.fxml` | B | (MEDIUM) |
| `MainLayout.fxml` | B | (MEDIUM) |
| `HomeView.fxml` | B | (MEDIUM) |
| `AuctionListView.fxml` | B | B69 |
| `AuctionDetailView.fxml` | B | B71 |
| `CreateAuctionView.fxml` | B | B72 |
| `CreateItemView.fxml` | B | B73, B74 |
| `ItemCatalogView.fxml` | B | B70 |
| `SellerDashboardView.fxml` | B | (MEDIUM) |
| `AdminView.fxml` | B | B75-B78 |
| `NotificationView.fxml` | B | (MEDIUM) |

### ✅ CLIENT — CSS (2 files)

| File | Người | Issues |
|------|--------|--------|
| `login.css` | B | B79-B82 |
| `styles.css` | B | B83-B85 |

### ✅ CLIENT — PROPERTIES (1 file)

| File | Người | Issues |
|------|--------|--------|
| `client.properties` | B | B86 |

---

## PHẦN 6: THỐNG KÊ

| Hạng mục | Số lượng |
|----------|----------|
| Tổng file đã rà | **88** |
| Tổng vấn đề (A) | **94** |
| Tổng vấn đề (B) | **86** |
| Tổng vấn đề | **180** |
| Client-Backend Gaps | **9** |
| CRITICAL | **30** |
| HIGH | **58** |
| MEDIUM | **56** |
| LOW | **36** |

---

## PHẦN 7: CÁCH AI THỰC HIỆN

```
1. Đọc file refactor1.md (chính file này)
2. Xác định người A hay B dựa trên file cần sửa
3. Đọc luật trong PHẦN 1 → tuân thủ tuyệt đối
4. Đọc issue trong PHẦN 2 → hiểu vấn đề
5. Đọc source file gốc → hiểu context
6. Sửa code theo hướng dẫn
7. Đảm bảo test vẫn pass
8. Đánh dấu ✅ trong checklist PHẦN 5 khi xong
9. Chuyển sang file tiếp theo theo thứ tự ưu tiên PHẦN 4
10. Kiểm tra GAP issues trong PHẦN 3 — sửa cả server + client nếu cần
```

**QUY TẮC QUAN TRỌNG**: Sửa 1 file → test → sang file tiếp. Không sửa 10 file cùng lúc.

**LƯU Ý ĐẶC BIỆT**:
- A22 + A45 + GAP9 là CÙNG 1 bug: `handleCancelAuction` check `"PENDING"` nhưng enum chỉ có `OPEN`. Sửa 1 lần ở server (A22 + A45) thì client GAP9 tự khắc hoạt động.
- B1 + B13 + B17 liên quan nhau: ViewRouter cleanup là root cause của nhiều leak. Sửa B1 trước.
- B42 + GAP1 liên quan nhau: Thêm nút RUN_INTEGRITY_CHECK trong AdminView.fxml + handler gọi API.
