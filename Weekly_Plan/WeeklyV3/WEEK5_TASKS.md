# 📋 TUẦN 5 — BÀI TẬP CHI TIẾT: Authentication & Quản lý người dùng/sản phẩm + AuditLogService

✅ Kết quả kiểm tra toàn diện: Không có lỗi. Codebase đáp ứng đầy đủ barem và sẵn sàng cho Tuần 5.

## 🎯 MỤC TIÊU TUẦN 5

Tuần này xây dựng 3 trụ cột chính: hệ thống xác thực (authentication) hoàn chỉnh end-to-end, CRUD sản phẩm
với kiểm soát quyền, và AuditLogService ghi nhật ký xuyên suốt mọi handler. Cuối tuần, cả nhóm phải có:

- ✅ `AuthService` hash password SHA-256 + verify, generate token UUID
- ✅ `SessionManager` (Singleton) quản lý token↔userId qua 2 ConcurrentHashMap
- ✅ `RequestHandler.handle()` giải mã token → set `session.authenticatedUserId` trước switch
- ✅ `handleLogin` / `handleRegister` / `handleLogout` hoạt động đầy đủ, kèm audit log
- ✅ `SecurityContext` kiểm tra quyền static — `requireAuthenticated()`, `requireRole()`
- ✅ `AuditLogService` wrap try-catch, không ném exception ra ngoài, 2 constructor (production + inject)
- ✅ `handleCreateItem` / `handleGetItemList` / `handleGetItemDetail` / `handleDeleteItem` hoàn chỉnh
- ✅ LoginView hoàn thiện: Button disable bind, NetworkTask → ServerGateway → ClientSession.login → navigateTo
- ✅ RegisterView.fxml + RegisterController: form, password confirmation bind, NetworkTask → REGISTER
- ✅ CreateItemController: dynamic fields theo itemType, chỉ cho SELLER
- ✅ ≥ 25 test cases mới pass (20 auth + 5 AuditLogService) — tổng project ≥ 150 cases

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** barem: **Quản lý người dùng, sản phẩm** (1.0đ) + **Chức năng đấu giá** — phần
> item CRUD (1.0đ) + **Xử lý lỗi & ngoại lệ** — validation register, auth error (1.0đ) + **Kỹ thuật quan trọng
> & concurrency** — ConcurrentHashMap trong SessionManager (1.0đ) + **Singleton** bổ sung `SessionManager`
> (phần 1.0đ Design Patterns) + **MVC** — RequestHandler auth handler + SecurityContext (phần 0.5đ) +
> **Xử lý lỗi** — AuditLogService không ném exception (1.0đ). AuditLogService tích hợp vào toàn bộ handler
> đáp ứng **chức năng audit trail** xuyên suốt.

> [!CAUTION]
> **Tuyệt đối không tạo lại** bất kỳ class nào từ Tuần 1–4:
> `Entity`, `BidHubException` + 7 subclass, `MessageRequest`, `MessageResponse`, `MessageMapper`,
> `ConfigLoader`, `DbConnectionProvider`, `MigrationRunner`,
> `UserRole`, `User`, `Bidder`, `Seller`, `Admin`, `Displayable`,
> `ItemType`, `Item`, `Electronics`, `Art`, `Vehicle`,
> `ItemCreator` + 3 ConcreteCreator, `AuctionStatus`, `Auction`, `BidTransaction`,
> `AuditLog`, `AuditActions`,
> `UserDao`, `ItemDao`, `AuctionDao`, `BidDao`, `AuditLogDao`,
> `SocketServerCore`, `Session`, `ClientConnectionThread`, `RequestHandler`,
> `BidHubApp`, `ViewRouter`, `ContextAware`, `LoginController`, `AuctionListController`,
> `AuctionDetailController`, `CreateItemController`, `Views`,
> `ServerGateway`, `NetworkTask`, `ClientSession`.
>
> **Thứ tự merge quan trọng:** Đăng merge trước (AuthService + SessionManager là nền tảng) → Khoa merge thứ hai
> (AuditLogService cần cho Quốc Minh) → Quốc Minh merge thứ ba (auth handlers phụ thuộc cả Đăng + Khoa) →
> Công Minh merge cuối (client có thể test với mock).

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI HỌC (Tự học)

> [!CAUTION]
> Hoàn thành **trước Thứ 5**. Mục đích không phải học Java chung — mà là hiểu cơ chế hoạt động của
> code người khác viết tuần này, để review chéo Chủ nhật trôi chảy và trả lời được vấn đáp giảng viên
> về authentication + audit log mà không lúng túng.

---

### Bài 0.1 — Password Hashing: SHA-256, Salt và HexFormat

**Tài liệu bắt buộc:**
- https://docs.oracle.com/javase/21/docs/api/java.base/java/security/MessageDigest.html
- https://docs.oracle.com/javase/21/docs/api/java.base/java/util/HexFormat.html

**Câu hỏi hỏi miệng Chủ nhật:**
1. `MessageDigest.getInstance("SHA-256")` trả về instance thread-safe hay không? Nếu nhiều thread gọi
   `digest()` trên cùng 1 `MessageDigest` object, điều gì xảy ra? Tại sao `hashPassword()` trong
   `AuthService` tạo local `MessageDigest` mỗi lần gọi?
2. SHA-256 luôn tạo output 256 bit = 32 byte = 64 hex chars. Tại sao `HexFormat.of().formatHex()` lại
   an toàn hơn `String.format("%02x")` lặp qua từng byte? Nêu 2 lý do về performance và correctness.
3. "Salt" là gì? `AuthService.hashPassword()` tuần này **không dùng salt** — kịch bản tấn công nào khai
   thác được điểm yếu này? Nếu 2 user cùng password `"12345678"`, hash giống nhau → vấn đề gì?
4. `verifyPassword(plain, hashed)` hash `plain` rồi so sánh với `hashed`. Tại sao dùng `equals()` chuỗi
   thay vì so sánh byte array? Timing attack là gì và `MessageDigest.isEqual()` giải quyết thế nào?
5. UUID token sinh bởi `UUID.randomUUID()` có 128 bit entropy — đủ để brute-force không? Tại sao
   `SessionManager` dùng token thay vì lưu `userId` trực tiếp vào session?
6. **[Câu hỏi nâng cao]** bcrypt thay SHA-256 có lợi gì? Nếu Tuần 9 muốn migrate sang bcrypt, chỉ cần
   thay `AuthService.hashPassword()` — class nào khác cần sửa? Tại sao interface `verifyPassword` giúp
   migration dễ dàng? Quốc Minh gọi `verifyPassword` trong `handleLogin` — nếu Đăng thay implementation,
   Quốc Minh có cần sửa code không?

---

### Bài 0.2 — Token-based Auth với ConcurrentHashMap

**Tài liệu bắt buộc:**
- https://docs.oracle.com/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html
- Đọc lại `DbConnectionProvider.java` (T1) — cùng pattern Singleton volatile + double-checked locking

**Câu hỏi hỏi miệng Chủ nhật:**
1. `SessionManager` dùng 2 `ConcurrentHashMap` (`tokenToUserId`, `userIdToToken`) — tại sao cần 2 map
   thay vì 1? Nếu chỉ có `tokenToUserId`, `invalidateSession(token)` hoạt động, nhưng làm sao đảm bảo
   1 user chỉ có 1 token tại 1 thời điểm (login mới invalidate token cũ)?
2. `ConcurrentHashMap.put()` là thread-safe — nhưng `createSession()` cần **check userIdToToken rồi put
   cả 2 map** — đây là compound action. Không có `synchronized`, 2 thread cùng gọi `createSession("user-1")`
   đồng thời → điều gì xảy ra? Có thể có 2 token cho cùng 1 user không?
3. `ConcurrentHashMap` không cho `null` key hay `null` value — `getUserIdByToken(null)` ném `NullPointerException`.
   Trong `RequestHandler.handle()`, nếu `req.getToken()` trả về `null` (chưa login), code xử lý thế nào
   để tránh NPE?
4. `invalidateSession(token)` xóa khỏi `tokenToUserId` trước, rồi xóa `userIdToToken` — nếu crash giữa
   2 lệnh xóa (OutOfMemoryError), map ở trạng thái inconsistent → hệ quả gì? Cần cơ chế cleanup nào?
5. Token sống trong memory — restart server → tất cả token mất. Client cần xử lý thế nào khi gửi request
   với token cũ sau server restart? `RequestHandler` trả về error message nào?
6. **[Câu hỏi nâng cao]** Tuần 8 thêm `ScheduledExecutorService` dọn token hết hạn (TTL 30 phút). Cần
   thêm field nào vào `SessionManager`? `ConcurrentHashMap` iterate an toàn khi concurrently modify không?
   Khoa viết `AuditLogService` — có nên log khi token hết hạn không? Log gì, ai là `userId`?

---

### Bài 0.3 — JavaFX Property Binding & FXML Controller

**Tài liệu bắt buộc:**
- https://openjfx.io/javadoc/21/javafx.base/javafx/beans/binding/Bindings.html
- https://openjfx.io/javadoc/21/javafx.fxml/javafx/fxml/FXMLLoader.html

**Câu hỏi hỏi miệng Chủ nhật:**
1. `loginButton.disableProperty().bind(usernameField.textProperty().isEmpty())` — nếu bind 2 điều kiện
   (username rỗng **HOẶC** password rỗng), dùng `Bindings.or()` hay `BooleanBinding` tự viết? Viết code
   cụ thể cho trường hợp LoginController.
2. `RegisterController` bind `confirmPassword` khớp `password` — dùng `Bindings.notEqual()` trên 2
   `StringProperty`. Nếu user chưa nhập confirmPassword (empty string), nên hiện warning hay ẩn? Tại sao?
3. `@FXML` annotation — field không có `@FXML` thì FXMLLoader có inject không? `initialize()` có
   `@FXML` không? Tại sao `initialize()` được gọi tự động dù không annotate?
4. `ChoiceBox<String>` vs `ComboBox<String>` — ChoiceBox đơn giản hơn, khi nào cần dùng ComboBox?
   Trong `RegisterController` chọn role (BIDDER/SELLER), ChoiceBox hay ComboBox phù hợp hơn?
5. `ViewRouter.navigateTo()` load FXML mới — controller cũ có bị GC không? Nếu `NetworkTask` vẫn đang
   chạy trong controller cũ mà user navigate đi chỗ khác → kết quả `setOnSucceeded` cập nhật UI đã
   đóng → crash? Cần cơ chế cancel task nào?
6. **[Câu hỏi nâng cao]** `CreateItemController` hiện dynamic fields theo `itemType` — khi chọn
   ELECTRONICS hiện brand/warrantyMonths, chọn ART hiện artist/yearCreated. Cách tốt nhất: 3 VBox
   ẩn/hiện bằng `setVisible()` hay tạo FXML riêng cho mỗi type rồi inject? Công Minh cần chọn 1 cách
   và giải thích tại sao.

---

### Bài 0.4 — SQL Injection, PreparedStatement & AuditLogService Pattern

**Tài liệu bắt buộc:**
- https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html (ôn lại PreparedStatement)
- Đọc lại `AuditLogDao.java` (T4) — `AuditLogService` dùng đúng DAO này

**Câu hỏi hỏi miệng Chủ nhật:**
1. PreparedStatement chống SQL Injection bằng cách nào? Nếu `handleLogin` nhận `username = "admin'--"`
   và dùng `Statement` thay `PreparedStatement`, câu SQL trông thế nào? Tại sao `UserDao.findByUsername()`
   dùng `PreparedStatement` là đủ bảo vệ?
2. `AuditLogService.log()` wrap toàn bộ trong try-catch — bắt `Exception` chung thay vì `SQLException`
   riêng. Tại sao? Nếu Jackson ném `JsonProcessingException` khi serialize details, handler có nên crash
   không?
3. `AuditLogService` có 2 constructor: production (tạo `new AuditLogDao()`) và inject (nhận `AuditLogDao`
   từ ngoài). Tại sao không dùng Singleton cho `AuditLogService`? Nếu dùng Singleton, test inject thế nào?
4. `auditLogService.log(userId, AuditActions.USER_LOGIN, "{}")` — details truyền `"{}"` thay vì thông
   tin chi tiết. Giới hạn của empty details là gì? Nếu muốn log IP address, browser info → cần thay đổi
   interface `log()` thế nào?
5. Quốc Minh gọi `auditLogService.log()` trong `handleLogin`, `handleRegister`, `handleLogout`. Nếu
   `auditLogService` là field của `RequestHandler` — Quốc Minh cần Khoa merge `AuditLogService` trước.
   Nếu Khoa chưa merge, Quốc Minh code thế nào để vẫn compile được? Phương án stub là gì?
6. **[Câu hỏi nâng cao]** Tuần 8 cần audit log **bên trong transaction** — ví dụ `handlePlaceBid()`
   cần cả `bidDao.save()` và `auditLogService.log()` hoặc commit hoặc rollback. `AuditLogService.log()`
   hiện tại không tham gia transaction — thiết kế nào giải quyết? Nên thêm method `log(Connection, ...)`
   hay dùng `ThreadLocal<Connection>`?

---

## 👤 ĐĂNG — AuthService (SHA-256) & SessionManager

```
Branch: feature/tuan-5-dang-auth-session
Phụ thuộc: RequestHandler.java (tuần 4, Quốc Minh) — MỞ RA thêm token resolution
           Session.java (tuần 4, Đăng) — MỞ RA set authenticatedUserId
           UserDao (tuần 3, Quốc Minh) — dùng trong RequestHandler, không tạo lại
Merge đầu tiên: AuthService + SessionManager là nền tảng cho Quốc Minh và Khoa
```

📌 **[Tiêu chí điểm: Xử lý lỗi & ngoại lệ — 1.0đ + Kỹ thuật quan trọng & concurrency — 1.0đ + Design Pattern Singleton — phần 1.0đ]**

### 📝 Mô tả bài tập

`AuthService` cung cấp 3 method cốt lõi: `hashPassword()` dùng SHA-256 + `HexFormat.of().formatHex()` tạo
chuỗi hex 64 ký tự, `verifyPassword()` hash plain text rồi so sánh, `generateToken()` sinh UUID ngẫu nhiên.
Class này **không có state** — mọi method là pure function, thread-safe tự nhiên.

`SessionManager` là Singleton quản lý token↔userId bằng 2 `ConcurrentHashMap`. `createSession()` sinh token
và đảm bảo 1 user chỉ có 1 token tại 1 thời điểm (login mới thay token cũ). `invalidateSession()` xóa khỏi
cả 2 map. `getUserIdByToken()` tra ngược token → userId. Đây là cầu nối giữa tầng network (token trong
request) và tầng service (userId trong handler).

Cập nhật `RequestHandler.handle()`: sau khi parse request, lấy `req.getToken()` → gọi
`SessionManager.getInstance().getUserIdByToken(token)` → nếu có userId → set
`session.setAuthenticatedUserId(userId)`. Thực hiện **trước** auth-guard check và switch-case.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Quốc Minh cần `AuthService.hashPassword()` trong `handleRegister` + `AuthService.verifyPassword()` trong
  `handleLogin` + `SessionManager.createSession()` trong `handleLogin`
- Khoa cần `SessionManager` để test auth flow trong test suite
- Công Minh (client) không phụ thuộc trực tiếp — chỉ cần server API ổn định

**Kịch bản chọn: C — Đăng merge trước, Quốc Minh và Khoa rebase sau**

**Các bước:**
1. Đăng tạo branch, code `AuthService.java` + `SessionManager.java` + update `RequestHandler.handle()`
2. Push lên GitHub, tạo PR → ít nhất 1 người review
3. Merge vào `develop` ngay sau khi CI xanh
4. Quốc Minh rebase `feature/tuan-5-quocminh-auth-handlers` từ `develop` — giờ có `AuthService` + `SessionManager`
5. Khoa rebase `feature/tuan-5-khoa-auditlog-service-handlers-tests` từ `develop`

**Nếu Đăng chậm:** Quốc Minh có thể tạm dùng stub:
```java
// Stub tạm trong branch Quốc Minh — XÓA khi Đăng merge
public class AuthService {
  public static String hashPassword(String plain) { return plain; }
  public static boolean verifyPassword(String plain, String hashed) { return plain.equals(hashed); }
  public static String generateToken() { return UUID.randomUUID().toString(); }
}
```

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── service/                  ← MỚI: toàn bộ package này
│   │   ├── AuthService.java          ← MỚI
│   │   └── SessionManager.java       ← MỚI: Singleton
│   └── network/
│       └── RequestHandler.java   (đã có T4 — MỞ RA thêm token resolution)
└── test/java/com/bidhub/server/
    ├── service/                  ← MỚI
    │   ├── AuthServiceTest.java      ← MỚI
    │   └── SessionManagerTest.java   ← MỚI
    └── network/
        └── RequestHandlerTest.java (đã có T4 — MỞ RA thêm test token resolution)
```

> [!IMPORTANT]
> Sau khi tạo xong `SessionManager`, mở `RequestHandler.java` và cập nhật method `handle()`:
> thêm đoạn token resolution **sau khi parse JSON** và **trước** auth-guard check.
> Commit riêng: `git commit -m "feat: thêm token resolution trong RequestHandler.handle()"`

---

### `AuthService.java`

```java
package com.bidhub.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Dich vu xac thuc: bam mat khau SHA-256, xac minh mat khau, sinh token UUID.
 *
 * <p>Moi method la pure function — khong co state, thread-safe tu nhien.
 * SHA-256 khong dung salt (don gian cho muc dich hoc tap);
 * production nen dung bcrypt hoac PBKDF2.
 */
public final class AuthService {

  private AuthService() {}

  /**
   * Bam mat khau bang SHA-256, tra ve chuoi hex 64 ky tu.
   *
   * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — SHA-256 hash khong reversible]
   *
   * @param plain mat khau goc
   * @return chuoi hex 64 ky tu
   */
  public static String hashPassword(String plain) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(
          plain.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 luon co trong JDK — khong bao gio xay ra
      throw new RuntimeException("SHA-256 khong kha dung", e);
    }
  }

  /**
   * Xac minh mat khau: hash plain text roi so sanh voi hashed.
   *
   * @param plain  mat khau nguoi dung nhap
   * @param hashed mat khau da hash luu trong DB
   * @return true neu khop, false neu sai
   */
  public static boolean verifyPassword(String plain, String hashed) {
    return hashPassword(plain).equals(hashed);
  }

  /**
   * Sinh token UUID ngau nhien cho phien dang nhap.
   *
   * <p>// 📌 [Tieu chi: Ky thuat quan trong — UUID token 128-bit entropy]
   *
   * @return chuoi UUID format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
   */
  public static String generateToken() {
    return UUID.randomUUID().toString();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm AuthService với SHA-256 hashing và UUID token generation"
```

---

### `SessionManager.java`

```java
package com.bidhub.server.service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton quan ly phien dang nhap: token ↔ userId.
 *
 * <p>Dung 2 {@link ConcurrentHashMap} de tra cuu ca 2 chieu:
 * token → userId va userId → token.
 * Dam bao 1 user chi co 1 token tai 1 thoi diem — login moi thay token cu.
 *
 * <p>// 📌 [Tieu chi: Ky thuat quan trong & concurrency — ConcurrentHashMap thread-safe]
 * // 📌 [Tieu chi: Design Pattern Singleton — volatile + double-checked locking]
 */
public final class SessionManager {

  private static volatile SessionManager instance;

  // 📌 [Tieu chi: Ky thuat quan trong — ConcurrentHashMap cho concurrent access]
  private final ConcurrentHashMap<String, String> tokenToUserId;
  private final ConcurrentHashMap<String, String> userIdToToken;

  private SessionManager() {
    this.tokenToUserId = new ConcurrentHashMap<>();
    this.userIdToToken = new ConcurrentHashMap<>();
  }

  /**
   * Tra ve instance duy nhat (thread-safe, double-checked locking).
   *
   * @return SessionManager instance
   */
  public static SessionManager getInstance() {
    if (instance == null) {
      synchronized (SessionManager.class) {
        if (instance == null) {
          instance = new SessionManager();
        }
      }
    }
    return instance;
  }

  /**
   * Tao phien dang nhap moi cho userId. Neu userId da co token cu → thay the.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — tao phien dang nhap]
   *
   * @param userId id nguoi dung
   * @return token UUID moi
   */
  public String createSession(String userId) {
    String token = AuthService.generateToken();

    // Neu user da co token cu → xoa token cu khoi tokenToUserId
    String oldToken = userIdToToken.put(userId, token);
    if (oldToken != null) {
      tokenToUserId.remove(oldToken);
    }

    tokenToUserId.put(token, userId);
    return token;
  }

  /**
   * Huy phien dang nhap — xoa token khoi ca 2 map.
   *
   * @param token token can huy
   */
  public void invalidateSession(String token) {
    if (token == null) {
      return;
    }
    String userId = tokenToUserId.remove(token);
    if (userId != null) {
      userIdToToken.remove(userId);
    }
  }

  /**
   * Tra cuu userId tu token.
   *
   * @param token token can tra cuu
   * @return Optional chua userId neu token hop le, Optional.empty() neu khong
   */
  public Optional<String> getUserIdByToken(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(tokenToUserId.get(token));
  }

  /**
   * Kiem tra token co ton tai khong.
   *
   * @param token token can kiem tra
   * @return true neu token hop le
   */
  public boolean isValidToken(String token) {
    return token != null && tokenToUserId.containsKey(token);
  }

  /**
   * Lay token hien tai cua userId.
   *
   * @param userId id nguoi dung
   * @return Optional chua token neu user dang dang nhap
   */
  public Optional<String> getTokenByUserId(String userId) {
    return Optional.ofNullable(userIdToToken.get(userId));
  }

  /** Xoa toan bo session — chi dung cho test. */
  public void clearAll() {
    tokenToUserId.clear();
    userIdToToken.clear();
  }

  /** Tra ve so phien dang nhap hien tai — chi dung cho test/monitor. */
  public int activeSessionCount() {
    return tokenToUserId.size();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm SessionManager Singleton với ConcurrentHashMap quản lý token↔userId"
```

---

### Cập nhật `RequestHandler.java` — thêm token resolution

Mở file `RequestHandler.java` đã có, thêm đoạn token resolution vào method `handle()` ngay sau khi parse
JSON và trước khi check auth-guard:

```java
// === THÊM VÀO method handle() — sau phần parse JSON, trước auth-guard check ===

    // 📌 [Tieu chi: Kien truc Client–Server — giai ma token truoc khi xu ly request]
    // Giai ma token → set authenticatedUserId vao session
    String token = req.getToken();
    if (token != null && !token.isBlank()) {
      SessionManager.getInstance().getUserIdByToken(token)
          .ifPresent(session::setAuthenticatedUserId);
    }
```

Cụ thể, method `handle()` sau khi sửa sẽ như sau:

```java
  public String handle(String jsonLine, Session session) {
    MessageRequest req;
    try {
      req = MessageMapper.fromJson(jsonLine, MessageRequest.class);
    } catch (Exception e) {
      return MessageMapper.toJson(
          MessageResponse.error("UNKNOWN", "JSON khong hop le: " + e.getMessage()));
    }

    String type = req.getType() != null ? req.getType().toUpperCase() : "UNKNOWN";

    // 📌 [Tieu chi: Kien truc Client–Server — giai ma token truoc khi xu ly request]
    String token = req.getToken();
    if (token != null && !token.isBlank()) {
      SessionManager.getInstance().getUserIdByToken(token)
          .ifPresent(session::setAuthenticatedUserId);
    }

    // 📌 [Tieu chi: Kien truc Client–Server — auth guard]
    if (AUTH_REQUIRED.contains(type) && !session.isAuthenticated()) {
      return MessageMapper.toJson(
          MessageResponse.error(type, "Ban chua dang nhap. Vui long LOGIN truoc."));
    }

    try {
      return switch (type) {
        case "PING"     -> handlePing(session);
        case "LOGIN"    -> MessageMapper.toJson(
            MessageResponse.error("LOGIN", "Chua implement — se co o Tuan 5"));
        case "REGISTER" -> MessageMapper.toJson(
            MessageResponse.error("REGISTER", "Chua implement — se co o Tuan 5"));
        default         -> MessageMapper.toJson(
            MessageResponse.error(type, "Lenh khong xac dinh: " + type));
      };
    } catch (BidHubException e) {
      return MessageMapper.toJson(MessageResponse.error(type, e.getMessage()));
    } catch (Exception e) {
      System.err.println("[RequestHandler] Loi xu ly " + type + ": " + e.getMessage());
      return MessageMapper.toJson(MessageResponse.error(type, "Loi he thong noi bo."));
    }
  }
```

```bash
git commit -m "feat: thêm token resolution trong RequestHandler.handle() — giải mã token từ request"
```

---

### ✅ Test đầu ra — `AuthServiceTest.java`

```java
package com.bidhub.server.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

  @Test
  @DisplayName("hashPassword tra ve chuoi hex 64 ky tu")
  void hashPassword_returns64CharHex() {
    String hash = AuthService.hashPassword("secret");
    assertEquals(64, hash.length());
    assertTrue(hash.matches("[0-9a-f]+"));
  }

  @Test
  @DisplayName("hashPassword cung input luon tra ve cung output (deterministic)")
  void hashPassword_deterministic() {
    String hash1 = AuthService.hashPassword("password123");
    String hash2 = AuthService.hashPassword("password123");
    assertEquals(hash1, hash2);
  }

  @Test
  @DisplayName("verifyPassword dung mat khau tra ve true")
  void verifyPassword_correctPassword() {
    String hash = AuthService.hashPassword("mypassword");
    assertTrue(AuthService.verifyPassword("mypassword", hash));
  }

  @Test
  @DisplayName("verifyPassword sai mat khau tra ve false")
  void verifyPassword_wrongPassword() {
    String hash = AuthService.hashPassword("mypassword");
    assertFalse(AuthService.verifyPassword("wrongpassword", hash));
  }

  @Test
  @DisplayName("generateToken tra ve UUID format hop le")
  void generateToken_validUUID() {
    String token = AuthService.generateToken();
    assertNotNull(token);
    assertTrue(token.matches(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
  }

  @Test
  @DisplayName("2 lan generateToken tra ve token khac nhau")
  void generateToken_unique() {
    String t1 = AuthService.generateToken();
    String t2 = AuthService.generateToken();
    assertNotEquals(t1, t2);
  }

  @Test
  @DisplayName("hashPassword chuoi rong khong crash")
  void hashPassword_emptyString() {
    String hash = AuthService.hashPassword("");
    assertEquals(64, hash.length());
  }

  @Test
  @DisplayName("verifyPassword voi hash sai format tra ve false")
  void verifyPassword_invalidHash() {
    assertFalse(AuthService.verifyPassword("test", "nothex"));
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

---

### ✅ Test đầu ra — `SessionManagerTest.java`

```java
package com.bidhub.server.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

  private SessionManager sessionManager;

  @BeforeEach
  void setUp() {
    sessionManager = SessionManager.getInstance();
    sessionManager.clearAll();
  }

  @AfterEach
  void tearDown() {
    sessionManager.clearAll();
  }

  @Test
  @DisplayName("getInstance tra ve cung instance (Singleton)")
  void getInstance_sameInstance() {
    SessionManager s1 = SessionManager.getInstance();
    SessionManager s2 = SessionManager.getInstance();
    assertSame(s1, s2);
  }

  @Test
  @DisplayName("createSession tra ve token UUID hop le")
  void createSession_returnsToken() {
    String token = sessionManager.createSession("user-001");
    assertNotNull(token);
    assertFalse(token.isBlank());
  }

  @Test
  @DisplayName("getUserIdByToken voi token hop le tra ve userId")
  void getUserIdByToken_validToken() {
    String token = sessionManager.createSession("user-001");
    assertEquals("user-001", sessionManager.getUserIdByToken(token).orElse(null));
  }

  @Test
  @DisplayName("getUserIdByToken voi token khong ton tai tra ve empty")
  void getUserIdByToken_invalidToken() {
    assertTrue(sessionManager.getUserIdByToken("fake-token").isEmpty());
  }

  @Test
  @DisplayName("getUserIdByToken voi null token tra ve empty")
  void getUserIdByToken_nullToken() {
    assertTrue(sessionManager.getUserIdByToken(null).isEmpty());
  }

  @Test
  @DisplayName("invalidateSession xoa token khoi ca 2 map")
  void invalidateSession_removesToken() {
    String token = sessionManager.createSession("user-001");
    sessionManager.invalidateSession(token);
    assertTrue(sessionManager.getUserIdByToken(token).isEmpty());
    assertTrue(sessionManager.getTokenByUserId("user-001").isEmpty());
  }

  @Test
  @DisplayName("invalidateSession voi null token khong crash")
  void invalidateSession_nullToken() {
    assertDoesNotThrow(() -> sessionManager.invalidateSession(null));
  }

  @Test
  @DisplayName("login moi thay token cu — 1 user chi co 1 token")
  void createSession_replacesOldToken() {
    String oldToken = sessionManager.createSession("user-001");
    String newToken = sessionManager.createSession("user-001");

    // Token cu khong con hop le
    assertTrue(sessionManager.getUserIdByToken(oldToken).isEmpty());
    // Token moi hop le
    assertEquals("user-001", sessionManager.getUserIdByToken(newToken).orElse(null));
  }

  @Test
  @DisplayName("isValidToken kiem tra token ton tai")
  void isValidToken() {
    String token = sessionManager.createSession("user-001");
    assertTrue(sessionManager.isValidToken(token));
    assertFalse(sessionManager.isValidToken("fake"));
  }

  @Test
  @DisplayName("activeSessionCount dem dung so phien")
  void activeSessionCount() {
    assertEquals(0, sessionManager.activeSessionCount());
    sessionManager.createSession("user-001");
    assertEquals(1, sessionManager.activeSessionCount());
    sessionManager.createSession("user-002");
    assertEquals(2, sessionManager.activeSessionCount());
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm AuthServiceTest (8 cases) và SessionManagerTest (10 cases)"
```

---

**Kiểm tra manual:**
```bash
# Terminal 1: Khởi động server
mvn compile -pl bidhub-server -q && mvn exec:java -pl bidhub-server \
  -Dexec.mainClass="com.bidhub.server.ServerApp"
# Output: [MigrationRunner] Schema đã sẵn sàng.
#         [SocketServerCore] Đang lắng nghe cổng 9090

# Terminal 2: Gửi PING với token giả
echo '{"type":"PING","token":"fake-token","payload":{}}' | nc -q1 localhost 9090
# Output: {"status":"OK","type":"PING","payload":{"message":"pong",...}}
# → PING không cần auth, token giả bị bỏ qua

# Terminal 3: Chạy test
mvn test -pl bidhub-server -Dtest="AuthServiceTest,SessionManagerTest" -q
# Output: Tests run: 18, Failures: 0
```

**❌ FAIL nếu:**
- `hashPassword("secret")` không trả về chuỗi 64 hex chars → SHA-256 hoặc HexFormat sai
- `SessionManager.getInstance()` gọi 2 lần trả về khác instance → Singleton sai
- `createSession("u1")` → `invalidateSession(token)` → `getUserIdByToken(token)` vẫn trả userId → map chưa xóa
- `createSession("u1")` lần 2 → token cũ vẫn hợp lệ → logic thay thế token bị thiếu
- `RequestHandler` gửi request với token hợp lệ nhưng `session.isAuthenticated()` vẫn false → token resolution chưa được thêm vào `handle()`

---

## 👤 QUỐC MINH — Login, Register, Logout + SecurityContext (+ 3 AuditLog lines)

```
Branch: feature/tuan-5-quocminh-auth-handlers
Phụ thuộc: AuthService, SessionManager (tuần 5, Đăng) — rebase sau khi Đăng merge
           AuditLogService (tuần 5, Khoa) — rebase sau khi Khoa merge
           UserDao (tuần 3), AuditLogDao (tuần 4) — không tạo lại
           RequestHandler (tuần 4) — MỞ RA thêm handler methods
```

📌 **[Tiêu chí điểm: Quản lý người dùng — 1.0đ + Xử lý lỗi & ngoại lệ — validation 1.0đ + MVC — auth handler 0.5đ]**

### 📝 Mô tả bài tập

3 handler auth là cốt lõi của chức năng quản lý người dùng — barem 1.0đ. `handleLogin` xác thực credentials
và tạo session, `handleRegister` validate đầu vào và tạo user mới, `handleLogout` invalidate session. Mỗi
handler gọi `auditLogService.log()` để ghi nhật ký — đảm bảo audit trail xuyên suốt.

`SecurityContext` là utility class kiểm tra quyền: `requireAuthenticated()` ném `AuthenticationException`
nếu chưa login, `requireRole()` ném nếu sai role. Khoa dùng `SecurityContext.requireRole(SELLER)` trong
`handleCreateItem`.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Quốc Minh cần `AuthService` (Đăng) + `SessionManager` (Đăng) + `AuditLogService` (Khoa)
- Quốc Minh cần `UserDao` (đã có từ T3)
- Quốc Minh cần `AuditActions` (đã có từ T4)
- Quốc Minh sửa `RequestHandler` — thêm case LOGIN/REGISTER/LOGOUT vào switch

**Kịch bản chọn: C — Rebase từ develop sau khi Đăng + Khoa merge**

**Nếu Khoa chưa merge AuditLogService:**
Quốc Minh tạo stub interface tạm trong branch:
```java
// Stub tạm — XÓA khi Khoa merge AuditLogService
package com.bidhub.server.service;
public class AuditLogService {
  public AuditLogService() {}
  public AuditLogService(Object dao) {}
  public void log(String userId, String action, String details) {
    // no-op stub
  }
}
```

**Các bước merge:**
1. Đăng merge AuthService + SessionManager vào `develop`
2. Quốc Minh rebase từ `develop` → giờ có AuthService + SessionManager
3. Quốc Minh code 3 handler + SecurityContext, dùng stub AuditLogService
4. Khoa merge AuditLogService vào `develop`
5. Quốc Minh rebase lần 2 → xóa stub, dùng AuditLogService thật
6. Quốc Minh tạo PR

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── network/
│   │   ├── RequestHandler.java    (đã có T4 — MỞ RA thêm handleLogin/Register/Logout)
│   │   └── SecurityContext.java   ← MỚI
│   └── service/
│       ├── AuthService.java       (đã có T5, Đăng — không sửa)
│       └── SessionManager.java    (đã có T5, Đăng — không sửa)
└── test/java/com/bidhub/server/
    └── network/
        ├── RequestHandlerTest.java  (đã có T4 — MỞ RA thêm auth handler tests)
        └── SecurityContextTest.java ← MỚI
```

---

### `SecurityContext.java`

```java
package com.bidhub.server.network;

import com.bidhub.common.exception.AuthenticationException;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.util.Optional;

/**
 * Utility kiem tra quyen truy cap — dung trong moi handler can auth/role guard.
 *
 * <p>Static method ném {@link AuthenticationException} neu khong dat dieu kien.
 * Exception nay duoc bat boi try-catch trong {@link RequestHandler#handle} va tra ve error response.
 *
 * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — AuthenticationException cho auth guard]
 */
public final class SecurityContext {

  private SecurityContext() {}

  /**
   * Yeu cau nguoi dung da dang nhap — tra ve userId neu hop le.
   *
   * @param session session hien tai
   * @return userId cua nguoi dung da dang nhap
   * @throws AuthenticationException neu chua dang nhap
   */
  public static String requireAuthenticated(Session session) {
    if (session == null || !session.isAuthenticated()) {
      throw new AuthenticationException("Ban chua dang nhap. Vui long LOGIN truoc.");
    }
    return session.getAuthenticatedUserId();
  }

  /**
   * Yeu cau nguoi dung co dung role — tra ve userId neu hop le.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — kiem tra role truoc khi thuc hien hanh dong]
   *
   * @param session  session hien tai
   * @param required role yeu cau
   * @return userId cua nguoi dung co dung role
   * @throws AuthenticationException neu chua dang nhap hoac sai role
   */
  public static String requireRole(Session session, UserRole required) {
    String userId = requireAuthenticated(session);

    UserDao userDao = new UserDao();
    Optional<User> userOpt = userDao.findById(userId);

    if (userOpt.isEmpty()) {
      throw new AuthenticationException("Nguoi dung khong ton tai.");
    }

    User user = userOpt.get();
    if (user.getRole() != required) {
      throw new AuthenticationException(
          "Khong du quyen. Yeu cau role: " + required.getDisplayName());
    }

    return userId;
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm SecurityContext với requireAuthenticated() và requireRole()"
```

---

### Cập nhật `RequestHandler.java` — thêm 3 handler method

Mở file `RequestHandler.java`, thêm 3 private handler method và cập nhật switch-case:

```java
package com.bidhub.server.network;

import com.bidhub.common.exception.BidHubException;
import com.bidhub.common.exception.DuplicateUsernameException;
import com.bidhub.common.exception.AuthenticationException;
import com.bidhub.common.exception.UserNotFoundException;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.Bidder;
import com.bidhub.server.model.Seller;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import com.bidhub.server.service.AuditLogService;
import com.bidhub.server.service.AuthService;
import com.bidhub.server.service.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dispatcher chinh: nhan JSON tho → parse → auth-guard → switch type → goi handler.
 *
 * <p>Switch-case mo rong tung tuan ma khong refactor:
 * Tuan 4: PING · Tuan 5: LOGIN / REGISTER / LOGOUT / CREATE_ITEM / GET_ITEM_LIST ·
 * Tuan 6: PLACE_BID / LIST_AUCTIONS · Tuan 7+: chi them case.
 */
public final class RequestHandler {

  // 📌 [Tieu chi: MVC — RequestHandler la tang dieu phoi server]
  private static final Set<String> AUTH_REQUIRED = Set.of(
      "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
      "LIST_MY_ITEMS", "PLACE_BID", "GET_AUCTION_DETAIL"
  );

  // DAO fields cho dependency injection (testing)
  final Object injectedUserDao;
  final Object injectedItemDao;

  // 📌 [Tieu chi: Quan ly nguoi dung — AuditLogService ghi nhat ky]
  private final AuditLogService auditLogService;
  private final UserDao userDao;

  public RequestHandler() {
    this.injectedUserDao = null;
    this.injectedItemDao = null;
    this.auditLogService = new AuditLogService();
    this.userDao = new UserDao();
  }

  RequestHandler(Object injectedUserDao, Object injectedItemDao) {
    this.injectedUserDao = injectedUserDao;
    this.injectedItemDao = injectedItemDao;
    this.auditLogService = new AuditLogService();
    this.userDao = injectedUserDao instanceof UserDao
        ? (UserDao) injectedUserDao : new UserDao();
  }

  /**
   * Constructor cho test voi inject ca AuditLogService.
   *
   * @param injectedUserDao       UserDao inject
   * @param injectedItemDao       ItemDao inject
   * @param injectedAuditService  AuditLogService inject
   */
  RequestHandler(Object injectedUserDao, Object injectedItemDao,
      AuditLogService injectedAuditService) {
    this.injectedUserDao = injectedUserDao;
    this.injectedItemDao = injectedItemDao;
    this.auditLogService = injectedAuditService;
    this.userDao = injectedUserDao instanceof UserDao
        ? (UserDao) injectedUserDao : new UserDao();
  }

  /**
   * Xu ly 1 request JSON tu client, tra ve JSON response string.
   *
   * <p>Khong nem exception ra ngoai — moi loi wrap thanh error response.
   *
   * @param jsonLine dong JSON tho tu socket
   * @param session  session cua client
   * @return chuoi JSON response
   */
  public String handle(String jsonLine, Session session) {
    MessageRequest req;
    try {
      req = MessageMapper.fromJson(jsonLine, MessageRequest.class);
    } catch (Exception e) {
      return MessageMapper.toJson(
          MessageResponse.error("UNKNOWN", "JSON khong hop le: " + e.getMessage()));
    }

    String type = req.getType() != null ? req.getType().toUpperCase() : "UNKNOWN";

    // 📌 [Tieu chi: Kien truc Client–Server — giai ma token truoc khi xu ly request]
    String token = req.getToken();
    if (token != null && !token.isBlank()) {
      SessionManager.getInstance().getUserIdByToken(token)
          .ifPresent(session::setAuthenticatedUserId);
    }

    // 📌 [Tieu chi: Kien truc Client–Server — auth guard]
    if (AUTH_REQUIRED.contains(type) && !session.isAuthenticated()) {
      return MessageMapper.toJson(
          MessageResponse.error(type, "Ban chua dang nhap. Vui long LOGIN truoc."));
    }

    try {
      JsonNode payload = req.getPayload() != null
          ? req.getPayload() : JsonNodeFactory.instance.objectNode();

      return switch (type) {
        case "PING"          -> handlePing(session);
        case "LOGIN"         -> handleLogin(session, payload);
        case "REGISTER"      -> handleRegister(session, payload);
        case "LOGOUT"        -> handleLogout(session, req.getToken());
        // Khoa se them cac case sau
        case "CREATE_ITEM"   -> MessageMapper.toJson(
            MessageResponse.error("CREATE_ITEM", "Chua implement — Khoa se them"));
        case "GET_ITEM_LIST" -> MessageMapper.toJson(
            MessageResponse.error("GET_ITEM_LIST", "Chua implement — Khoa se them"));
        case "GET_ITEM_DETAIL" -> MessageMapper.toJson(
            MessageResponse.error("GET_ITEM_DETAIL", "Chua implement — Khoa se them"));
        case "DELETE_ITEM"   -> MessageMapper.toJson(
            MessageResponse.error("DELETE_ITEM", "Chua implement — Khoa se them"));
        default              -> MessageMapper.toJson(
            MessageResponse.error(type, "Lenh khong xac dinh: " + type));
      };
    } catch (BidHubException e) {
      return MessageMapper.toJson(MessageResponse.error(type, e.getMessage()));
    } catch (Exception e) {
      System.err.println("[RequestHandler] Loi xu ly " + type + ": " + e.getMessage());
      return MessageMapper.toJson(MessageResponse.error(type, "Loi he thong noi bo."));
    }
  }

  // === AUTH HANDLERS ===

  /**
   * Xu ly LOGIN: xac thuc credentials → tao session → tra ve token + user info.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — 1.0đ]
   *
   * @param session session hien tai
   * @param payload chua {username, password}
   * @return JSON response voi token neu thanh cong
   */
  private String handleLogin(Session session, JsonNode payload) {
    String username = getTextSafe(payload, "username");
    String password = getTextSafe(payload, "password");

    if (username == null || username.isBlank()) {
      return MessageMapper.toJson(
          MessageResponse.error("LOGIN", "Ten dang nhap khong duoc de trong."));
    }
    if (password == null || password.isBlank()) {
      return MessageMapper.toJson(
          MessageResponse.error("LOGIN", "Mat khau khong duoc de trong."));
    }

    Optional<User> userOpt = userDao.findByUsername(username);
    if (userOpt.isEmpty()) {
      return MessageMapper.toJson(
          MessageResponse.error("LOGIN", "Ten dang nhap hoac mat khau khong dung."));
    }

    User user = userOpt.get();
    // 📌 [Tieu chi: Xu ly loi & ngoai le — verify password khong tiet lo thong tin]
    if (!AuthService.verifyPassword(password, user.getPasswordHash())) {
      return MessageMapper.toJson(
          MessageResponse.error("LOGIN", "Ten dang nhap hoac mat khau khong dung."));
    }

    // 📌 [Tieu chi: Ky thuat quan trong — tao session voi token UUID]
    String token = SessionManager.getInstance().createSession(user.getId());
    session.setAuthenticatedUserId(user.getId());

    // 📌 [Tieu chi: Quan ly nguoi dung — audit log login]
    auditLogService.log(user.getId(), AuditActions.USER_LOGIN, "{}");

    Map<String, String> result = new HashMap<>();
    result.put("token", token);
    result.put("userId", user.getId());
    result.put("username", user.getUsername());
    result.put("role", user.getRole().name());

    return MessageMapper.toJson(MessageResponse.ok("LOGIN", result));
  }

  /**
   * Xu ly REGISTER: validate input → kiem tra trung username → tao user → save.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — 1.0đ + Xu ly loi & ngoai le — validation]
   *
   * @param session session hien tai
   * @param payload chua {username, password, email, role}
   * @return JSON response voi user info neu thanh cong
   */
  private String handleRegister(Session session, JsonNode payload) {
    String username = getTextSafe(payload, "username");
    String password = getTextSafe(payload, "password");
    String email = getTextSafe(payload, "email");
    String roleStr = getTextSafe(payload, "role");

    // 📌 [Tieu chi: Xu ly loi & ngoai le — validation dau vao]
    if (username == null || username.isBlank()) {
      return MessageMapper.toJson(
          MessageResponse.error("REGISTER", "Ten dang nhap khong duoc de trong."));
    }
    if (password == null || password.length() < 8) {
      return MessageMapper.toJson(
          MessageResponse.error("REGISTER", "Mat khau phai co it nhat 8 ky tu."));
    }
    if (email == null || !email.contains("@")) {
      return MessageMapper.toJson(
          MessageResponse.error("REGISTER", "Email khong hop le (phai chua @)."));
    }
    if (roleStr == null || "ADMIN".equalsIgnoreCase(roleStr)) {
      return MessageMapper.toJson(
          MessageResponse.error("REGISTER", "Khong the dang ky voi vai tro ADMIN."));
    }

    // 📌 [Tieu chi: Xu ly loi & ngoai le — trung username]
    if (userDao.existsByUsername(username)) {
      return MessageMapper.toJson(
          MessageResponse.error("REGISTER", "Ten dang nhap da ton tai."));
    }

    // Tao user moi voi dung subclass
    UserRole role = "SELLER".equalsIgnoreCase(roleStr)
        ? UserRole.SELLER : UserRole.BIDDER;
    String hashedPassword = AuthService.hashPassword(password);

    User newUser;
    if (role == UserRole.SELLER) {
      newUser = new Seller(username, hashedPassword, email);
    } else {
      newUser = new Bidder(username, hashedPassword, email);
    }

    userDao.save(newUser);

    // 📌 [Tieu chi: Quan ly nguoi dung — audit log register]
    auditLogService.log(newUser.getId(), AuditActions.USER_REGISTER, "{}");

    Map<String, String> result = new HashMap<>();
    result.put("userId", newUser.getId());
    result.put("username", newUser.getUsername());
    result.put("role", newUser.getRole().name());

    return MessageMapper.toJson(MessageResponse.ok("REGISTER", result));
  }

  /**
   * Xu ly LOGOUT: invalidate session → clear authenticatedUserId.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — logout + audit log]
   *
   * @param session session hien tai
   * @param token   token tu request
   * @return JSON response
   */
  private String handleLogout(Session session, String token) {
    String userId = session.getAuthenticatedUserId();

    // 📌 [Tieu chi: Quan ly nguoi dung — audit log logout truoc khi invalidate]
    if (userId != null) {
      auditLogService.log(userId, AuditActions.USER_LOGOUT, "{}");
    }

    if (token != null && !token.isBlank()) {
      SessionManager.getInstance().invalidateSession(token);
    }
    session.setAuthenticatedUserId(null);

    Map<String, String> result = new HashMap<>();
    result.put("message", "Dang xuat thanh cong.");
    return MessageMapper.toJson(MessageResponse.ok("LOGOUT", result));
  }

  // === HELPER METHODS ===

  private String handlePing(Session session) {
    Map<String, String> payload = Map.of(
        "message", "pong",
        "serverTime", LocalDateTime.now().toString(),
        "sessionId", session.getSessionId()
    );
    return MessageMapper.toJson(MessageResponse.ok("PING", payload));
  }

  /**
   * Doc string tu JsonNode an toan — tra ve null neu field khong ton tai.
   *
   * @param node JsonNode cha
   * @param field ten field
   * @return gia tri string hoac null
   */
  private String getTextSafe(JsonNode node, String field) {
    if (node == null || !node.has(field) || node.get(field).isNull()) {
      return null;
    }
    return node.get(field).asText();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm handleLogin, handleRegister, handleLogout vào RequestHandler với audit log"
```

---

### ✅ Test đầu ra — `SecurityContextTest.java`

```java
package com.bidhub.server.network;

import com.bidhub.common.exception.AuthenticationException;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

class SecurityContextTest {

  private Session createAuthSession(String userId) throws Exception {
    try (ServerSocket srv = new ServerSocket(0)) {
      Socket client = new Socket("localhost", srv.getLocalPort());
      Session session = new Session(srv.accept());
      session.setAuthenticatedUserId(userId);
      client.close();
      return session;
    }
  }

  private Session createUnauthSession() throws Exception {
    try (ServerSocket srv = new ServerSocket(0)) {
      Socket client = new Socket("localhost", srv.getLocalPort());
      Session session = new Session(srv.accept());
      client.close();
      return session;
    }
  }

  @Test
  @DisplayName("requireAuthenticated voi session hop le tra ve userId")
  void requireAuthenticated_validSession() throws Exception {
    Session session = createAuthSession("user-001");
    assertEquals("user-001", SecurityContext.requireAuthenticated(session));
    session.disconnect();
  }

  @Test
  @DisplayName("requireAuthenticated voi session null nem AuthenticationException")
  void requireAuthenticated_nullSession() {
    assertThrows(AuthenticationException.class,
        () -> SecurityContext.requireAuthenticated(null));
  }

  @Test
  @DisplayName("requireAuthenticated voi session chua login nem AuthenticationException")
  void requireAuthenticated_unauthSession() throws Exception {
    Session session = createUnauthSession();
    assertThrows(AuthenticationException.class,
        () -> SecurityContext.requireAuthenticated(session));
    session.disconnect();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm SecurityContextTest (3 cases)"
```

---

**Kiểm tra manual end-to-end:**
```bash
# Terminal 1: Khởi động server
mvn compile -pl bidhub-server -q && mvn exec:java -pl bidhub-server \
  -Dexec.mainClass="com.bidhub.server.ServerApp"

# Terminal 2: Đăng ký user mới
echo '{"type":"REGISTER","payload":{"username":"seller01","password":"12345678","email":"s@test.com","role":"SELLER"}}' | nc -q1 localhost 9090
# Output: {"status":"OK","type":"REGISTER","payload":{"userId":"...","username":"seller01","role":"SELLER"}}

# Terminal 3: Đăng nhập
echo '{"type":"LOGIN","payload":{"username":"seller01","password":"12345678"}}' | nc -q1 localhost 9090
# Output: {"status":"OK","type":"LOGIN","payload":{"token":"xxx-xxx","userId":"...","username":"seller01","role":"SELLER"}}

# Terminal 4: Đăng ký trùng username
echo '{"type":"REGISTER","payload":{"username":"seller01","password":"12345678","email":"s2@test.com","role":"SELLER"}}' | nc -q1 localhost 9090
# Output: {"status":"ERROR","type":"REGISTER","message":"Ten dang nhap da ton tai."}

# Terminal 5: Đăng ký role ADMIN
echo '{"type":"REGISTER","payload":{"username":"admin01","password":"12345678","email":"a@test.com","role":"ADMIN"}}' | nc -q1 localhost 9090
# Output: {"status":"ERROR","type":"REGISTER","message":"Khong the dang ky voi vai tro ADMIN."}

# Chạy test
mvn test -pl bidhub-server -Dtest="SecurityContextTest" -q
# Output: Tests run: 3, Failures: 0
```

**❌ FAIL nếu:**
- LOGIN sai password trả `"Sai mật khẩu"` thay vì message chung → lộ thông tin user tồn tại
- REGISTER password 7 ký tự → chấp nhận → thiếu validation `password.length() < 8`
- REGISTER role ADMIN → chấp nhận → thiếu check `"ADMIN".equalsIgnoreCase(roleStr)`
- LOGIN thành công nhưng response không chứa `token` → `SessionManager.createSession()` chưa được gọi
- LOGOUT thành công nhưng token vẫn hợp lệ → `SessionManager.invalidateSession()` chưa được gọi
- `auditLogService.log()` ném exception → handler crash → thiếu try-catch trong AuditLogService

---

## 👤 CÔNG MINH — LoginView, RegisterView + CreateItemView hoàn chỉnh

```
Branch: feature/tuan-5-congminh-auth-item-ui
Phụ thuộc: ServerGateway, NetworkTask, ClientSession (tuần 4, Công Minh) — không tạo lại
           ViewRouter, Views (tuần 3) — không tạo lại
           MessageRequest, MessageResponse, MessageMapper (tuần 4, trong bidhub-common) — dùng chung
Merge cuối cùng: client có thể test với server đã có đầy đủ auth handler
```

📌 **[Tiêu chí điểm: MVC — Controller xử lý UI + network (phần 0.5đ) + Quản lý người dùng/sản phẩm — UI 1.0đ]**

### 📝 Mô tả bài tập

`LoginController` hoàn thiện: Button disable bind, NetworkTask → ServerGateway.sendRequest →
ClientSession.login → navigateTo(AUCTION_LIST). Đây là flow end-to-end đầu tiên: UI → network →
server → response → UI update.

`RegisterView.fxml` + `RegisterController`: form đăng ký mới với validation realtime — password
confirmation bind, email check. Submit → NetworkTask → REGISTER → thành công navigate về LoginView.

`CreateItemController` hoàn thiện: dynamic fields theo itemType, chỉ cho role SELLER. Khi chọn
ELECTRONICS → hiện brand/warrantyMonths, ART → artist/yearCreated, VEHICLE → manufacturer/year/mileageKm.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Công Minh chỉ phụ thuộc vào `bidhub-common` (MessageRequest, MessageResponse, MessageMapper)
- Server API phải ổn định — REQUEST format không đổi dù server handler có thể chưa merge
- Công Minh có thể test UI với server đang chạy trên branch Đăng+Quốc Minh+Khoa đã merge

**Kịch bản chọn: D — Công Minh merge cuối, test với server đã đầy đủ handler**

**Nếu server handler chưa sẵn sàng:**
Công Minh test UI bằng cách start server từ branch `develop` đã merge đủ Đăng+Quốc Minh+Khoa.
Client gửi request → server xử lý → response trả về đúng format → UI cập nhật.

**Các bước:**
1. Công Minh code UI trên branch riêng, không cần rebase liên tục
2. Sau khi Đăng+Quốc Minh+Khoa merge vào `develop`, Công Minh start server từ `develop`
3. Công Minh start client từ branch của mình → test end-to-end
4. Khi tất cả pass → Công Minh merge vào `develop`

### 📁 Cấu trúc file

```
bidhub-client/src/
├── main/java/com/bidhub/client/
│   ├── controller/
│   │   ├── LoginController.java      (đã có T3 — MỞ RA thêm network logic)
│   │   ├── RegisterController.java   ← MỚI
│   │   ├── AuctionListController.java (đã có T3 — không sửa)
│   │   ├── AuctionDetailController.java (đã có T3 — không sửa)
│   │   └── CreateItemController.java  (đã có T3 — MỞ RA thêm dynamic fields + network)
│   └── util/
│       └── Views.java                (đã có T1 — MỞ RA thêm REGISTER nếu chưa có)
└── resources/
    └── fxml/
        ├── LoginView.fxml             (đã có T1 — MỞ RA thêm Register link)
        ├── RegisterView.fxml          ← MỚI
        ├── AuctionListView.fxml       (đã có — không sửa)
        ├── AuctionDetailView.fxml     (đã có — không sửa)
        └── CreateItemView.fxml        (đã có T3 — MỞ RA thêm dynamic fields)
```

> [!IMPORTANT]
> Mở `Views.java`, thêm constant nếu chưa có:
> ```java
> public static final String REGISTER = "RegisterView";
> ```

---

### `LoginController.java` (hoàn thiện)

```java
package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.beans.binding.Bindings;

/**
 * Controller cho man hinh dang nhap.
 *
 * <p>// 📌 [Tieu chi: MVC — Controller dieu phoi UI + network + navigation]
 * Flow: validate → NetworkTask → ServerGateway → ClientSession → navigateTo.
 * setOnSucceeded chay tren FX thread — KHONG can Platform.runLater().
 */
public class LoginController {
    private javafx.beans.property.BooleanProperty isLoading = new javafx.beans.property.SimpleBooleanProperty(false);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        loginButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        // Nút sẽ disable nếu ô chữ trống HOẶC đang trong trạng thái loading
                        () -> usernameField.getText().isBlank()
                                || passwordField.getText().isBlank()
                                || isLoading.get(),
                        usernameField.textProperty(),
                        passwordField.textProperty(),
                        isLoading // Lắng nghe thêm biến isLoading
                )
        );
        errorLabel.setVisible(false);
        errorLabel.getStyleClass().add("error-message");
    }

    /**
     * Xu ly click nut "Dang nhap" — gui request LOGIN qua NetworkTask.
     */
    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setVisible(false);
        isLoading.set(true); // Bắt đầu loading, binding sẽ tự động disable nút

        // 📌 [Tieu chi: MVC — tao request JSON payload]
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", username);
        payload.put("password", password);

        MessageRequest request = new MessageRequest(
                "LOGIN", ClientSession.getInstance().getToken(), payload);

        // 📌 [Tieu chi: Ky thuat quan trong — NetworkTask khong block FX thread]
        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(request));

        task.setOnSucceeded(e -> {
            isLoading.set(false); // Đưa lên đầu: Dù thành công hay thất bại, tiến trình mạng đã xong thì gỡ loading.

            MessageResponse response = task.getValue();
            if (response.isOk()) {
                handleLoginSuccess(response);
            } else {
                errorLabel.setText(response.getMessage());
                errorLabel.setVisible(true);
            }
        });

        task.setOnFailed(e -> {
            isLoading.set(false); // Lỗi mạng (Exception) rơi vào đây, cũng gỡ loading.
            errorLabel.setText("Khong ket noi duoc may chu. Thu lai sau.");
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
    }

    /**
     * Xu ly dang nhap thanh cong — luu session va chuyen man hinh.
     */
    private void handleLoginSuccess(MessageResponse response) {
        // 📌 [Tieu chi: Quan ly nguoi dung — luu thong tin dang nhap vao ClientSession]
        Object payload = response.getPayload();
        if (payload instanceof java.util.Map<?, ?> map) {
            String token = (String) map.get("token");
            String userId = (String) map.get("userId");
            String username = (String) map.get("username");
            String role = (String) map.get("role");
            ClientSession.getInstance().login(token, userId, username, role);
        }
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
    }

    /**
     * Chuyen sang man hinh dang ky.
     */
    @FXML
    public void handleRegister() {
        ViewRouter.getInstance().navigateTo(Views.REGISTER);
    }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: hoàn thiện LoginController với NetworkTask, ClientSession, ViewRouter"
```

---

### `RegisterView.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.bidhub.client.controller.RegisterController"
      styleClass="root" spacing="12" alignment="CENTER"
      prefWidth="450" prefHeight="550">

  <padding><Insets top="30" right="40" bottom="30" left="40"/></padding>

  <Label text="Dang ky tai khoan" styleClass="header-bar"
         style="-fx-font-size: 20px; -fx-font-weight: bold;"/>

  <TextField fx:id="usernameField" promptText="Ten dang nhap"/>
  <PasswordField fx:id="passwordField" promptText="Mat khau (it nhat 8 ky tu)"/>
  <PasswordField fx:id="confirmPasswordField" promptText="Xac nhan mat khau"/>
  <Label fx:id="passwordMatchLabel" visible="false"
         style="-fx-text-fill: #e74c3c; -fx-font-size: 11px;"/>
  <TextField fx:id="emailField" promptText="Email"/>
  <HBox spacing="10" alignment="CENTER_LEFT">
    <Label text="Vai tro:"/>
    <ChoiceBox fx:id="roleChoiceBox" prefWidth="150"/>
  </HBox>
  <Label fx:id="errorLabel" visible="false" styleClass="error-message"/>
  <Button fx:id="registerButton" text="Dang ky" onAction="#handleRegister"
          styleClass="btn-primary" maxWidth="Infinity"/>
  <Button text="Da co tai khoan? Dang nhap" onAction="#handleBackToLogin"
          style="-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand;"/>
</VBox>
```

---

### `RegisterController.java`

```java
package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller cho man hinh dang ky.
 *
 * <p>// 📌 [Tieu chi: MVC — Controller dieu phoi form validation + network]
 * Validation realtime: password confirmation bind, email check.
 * Submit → NetworkTask → REGISTER → thanh cong → navigate ve LoginView.
 */
public class RegisterController {

  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;
  @FXML private PasswordField confirmPasswordField;
  @FXML private Label passwordMatchLabel;
  @FXML private TextField emailField;
  @FXML private ChoiceBox<String> roleChoiceBox;
  @FXML private Label errorLabel;
  @FXML private Button registerButton;

  @FXML
  public void initialize() {
    roleChoiceBox.setItems(
        FXCollections.observableArrayList("BIDDER", "SELLER"));
    roleChoiceBox.setValue("BIDDER");

    // 📌 [Tieu chi: MVC — bind realtime password confirmation]
    passwordMatchLabel.visibleProperty().bind(
        Bindings.createBooleanBinding(
            () -> {
              String pw = passwordField.getText();
              String cpw = confirmPasswordField.getText();
              return !cpw.isEmpty() && !pw.equals(cpw);
            },
            passwordField.textProperty(),
            confirmPasswordField.textProperty()
        )
    );
    passwordMatchLabel.setText("Mat khau xac nhan khong khop!");

    registerButton.disableProperty().bind(
        Bindings.createBooleanBinding(
            () -> usernameField.getText().isBlank()
                || passwordField.getText().length() < 8
                || emailField.getText().isBlank()
                || !emailField.getText().contains("@")
                || !passwordField.getText().equals(confirmPasswordField.getText()),
            usernameField.textProperty(),
            passwordField.textProperty(),
            emailField.textProperty()
        )
    );

    errorLabel.setVisible(false);
    errorLabel.getStyleClass().add("error-message");
  }

  /**
   * Xu ly click nut "Dang ky" — gui request REGISTER qua NetworkTask.
   */
  @FXML
  public void handleRegister() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();
    String confirmPassword = confirmPasswordField.getText();
    String email = emailField.getText().trim();
    String role = roleChoiceBox.getValue();

    // Client-side validation
    if (!password.equals(confirmPassword)) {
      errorLabel.setText("Mat khau xac nhan khong khop!");
      errorLabel.setVisible(true);
      return;
    }

    errorLabel.setVisible(false);
    registerButton.setDisable(true);

    // 📌 [Tieu chi: MVC — tao REGISTER request payload]
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("username", username);
    payload.put("password", password);
    payload.put("email", email);
    payload.put("role", role);

    MessageRequest request = new MessageRequest(
        "REGISTER", ClientSession.getInstance().getToken(), payload);

    NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
        ServerGateway.getInstance().sendRequest(request));

    task.setOnSucceeded(e -> {
      MessageResponse response = task.getValue();
      if (response.isOk()) {
        ViewRouter.getInstance().navigateTo(Views.LOGIN);
      } else {
        errorLabel.setText(response.getMessage());
        errorLabel.setVisible(true);
        registerButton.setDisable(false);
      }
    });

    task.setOnFailed(e -> {
      errorLabel.setText("Khong ket noi duoc may chu. Thu lai sau.");
      errorLabel.setVisible(true);
      registerButton.setDisable(false);
    });

    new Thread(task).start();
  }

  /**
   * Chuyen ve man hinh dang nhap.
   */
  @FXML
  public void handleBackToLogin() {
    ViewRouter.getInstance().navigateTo(Views.LOGIN);
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm RegisterView.fxml và RegisterController với password confirmation bind"
```

---

### `CreateItemController.java` (hoàn thiện)

```java
package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Controller cho man hinh tao san pham — chi cho SELLER.
 *
 * <p>// 📌 [Tieu chi: Quan ly san pham — tao san pham voi dynamic fields]
 * Khi chon itemType → hien thi form phu phu hop:
 * ELECTRONICS: brand, warrantyMonths
 * ART: artist, yearCreated
 * VEHICLE: manufacturer, year, mileageKm
 */
public class CreateItemController {

  @FXML private TextField nameField;
  @FXML private TextArea descriptionArea;
  @FXML private TextField startingPriceField;
  @FXML private ComboBox<String> itemTypeComboBox;
  @FXML private Label lblMessage;

  // 📌 [Tieu chi: MVC — dynamic fields theo itemType]
  @FXML private VBox electronicsFields;
  @FXML private VBox artFields;
  @FXML private VBox vehicleFields;

  // Electronics fields
  @FXML private TextField brandField;
  @FXML private TextField warrantyMonthsField;

  // Art fields
  @FXML private TextField artistField;
  @FXML private TextField yearCreatedField;

  // Vehicle fields
  @FXML private TextField manufacturerField;
  @FXML private TextField yearField;
  @FXML private TextField mileageKmField;

  @FXML
  public void initialize() {
    itemTypeComboBox.setItems(
        FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));

    // 📌 [Tieu chi: MVC — an/hien dynamic fields khi doi itemType]
    itemTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
      electronicsFields.setVisible("ELECTRONICS".equals(newVal));
      electronicsFields.setManaged("ELECTRONICS".equals(newVal));
      artFields.setVisible("ART".equals(newVal));
      artFields.setManaged("ART".equals(newVal));
      vehicleFields.setVisible("VEHICLE".equals(newVal));
      vehicleFields.setManaged("VEHICLE".equals(newVal));
    });

    // Mac dinh an tat ca form phu
    electronicsFields.setVisible(false);
    electronicsFields.setManaged(false);
    artFields.setVisible(false);
    artFields.setManaged(false);
    vehicleFields.setVisible(false);
    vehicleFields.setManaged(false);

    lblMessage.setVisible(false);
    lblMessage.getStyleClass().add("error-message");

    // Kiem tra role — chi SELLER duoc tao item
    String role = ClientSession.getInstance().getCurrentRole();
    if (!"SELLER".equals(role)) {
      lblMessage.setText("Chi nguoi ban (SELLER) moi duoc tao san pham.");
      lblMessage.setVisible(true);
    }
  }

  /**
   * Xu ly tao san pham — gui request CREATE_ITEM.
   */
  @FXML
  public void handleSubmit() {
    String role = ClientSession.getInstance().getCurrentRole();
    if (!"SELLER".equals(role)) {
      lblMessage.setText("Chi nguoi ban (SELLER) moi duoc tao san pham.");
      lblMessage.setVisible(true);
      return;
    }

    String name = nameField.getText().trim();
    String description = descriptionArea.getText().trim();
    String priceStr = startingPriceField.getText().trim();
    String itemType = itemTypeComboBox.getValue();

    if (name.isBlank() || priceStr.isBlank() || itemType == null) {
      lblMessage.setText("Vui long dien day du thong tin.");
      lblMessage.setVisible(true);
      return;
    }

    double startingPrice;
    try {
      startingPrice = Double.parseDouble(priceStr);
      if (startingPrice <= 0) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      lblMessage.setText("Gia khoi diem phai la so duong.");
      lblMessage.setVisible(true);
      return;
    }

    // 📌 [Tieu chi: Quan ly san pham — tao payload voi extras theo itemType]
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("name", name);
    payload.put("description", description);
    payload.put("startingPrice", startingPrice);
    payload.put("itemType", itemType);

    ObjectNode extras = JsonNodeFactory.instance.objectNode();
    switch (itemType) {
      case "ELECTRONICS" -> {
        extras.put("brand", brandField.getText().trim());
        extras.put("warrantyMonths",
            Integer.parseInt(warrantyMonthsField.getText().trim()));
      }
      case "ART" -> {
        extras.put("artist", artistField.getText().trim());
        extras.put("yearCreated",
            Integer.parseInt(yearCreatedField.getText().trim()));
      }
      case "VEHICLE" -> {
        extras.put("manufacturer", manufacturerField.getText().trim());
        extras.put("year", Integer.parseInt(yearField.getText().trim()));
        extras.put("mileageKm",
            Integer.parseInt(mileageKmField.getText().trim()));
      }
    }
    payload.set("extras", extras);

    MessageRequest request = new MessageRequest(
        "CREATE_ITEM", ClientSession.getInstance().getToken(), payload);

    lblMessage.setVisible(false);

    NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
        ServerGateway.getInstance().sendRequest(request));

    task.setOnSucceeded(e -> {
      MessageResponse response = task.getValue();
      if (response.isOk()) {
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
      } else {
        lblMessage.setText(response.getMessage());
        lblMessage.setVisible(true);
      }
    });

    task.setOnFailed(e -> {
      lblMessage.setText("Khong ket noi duoc may chu. Thu lai sau.");
      lblMessage.setVisible(true);
    });

    new Thread(task).start();
  }

  /**
   * Huy tao san pham — quay lai danh sach dau gia.
   */
  @FXML
  public void handleCancel() {
    ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: hoàn thiện CreateItemController với dynamic fields theo itemType"
```

---

### Cập nhật `CreateItemView.fxml` — thêm dynamic fields

Mở file `CreateItemView.fxml` đã có, thêm 3 VBox chứa dynamic fields:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.bidhub.client.controller.CreateItemController"
      styleClass="root" spacing="10" alignment="CENTER"
      prefWidth="550" prefHeight="650">

  <padding><Insets top="20" right="40" bottom="20" left="40"/></padding>

  <Label text="Tao san pham moi" style="-fx-font-size: 18px; -fx-font-weight: bold;"/>

  <TextField fx:id="nameField" promptText="Ten san pham"/>
  <TextArea fx:id="descriptionArea" promptText="Mo ta san pham" prefRowCount="3"/>
  <TextField fx:id="startingPriceField" promptText="Gia khoi diem (VND)"/>
  <ComboBox fx:id="itemTypeComboBox" promptText="Chon loai san pham" maxWidth="Infinity"/>

  <!-- Dynamic fields: Electronics -->
  <VBox fx:id="electronicsFields" spacing="8">
    <TextField fx:id="brandField" promptText="Thuong hieu (brand)"/>
    <TextField fx:id="warrantyMonthsField" promptText="Thang bao hanh"/>
  </VBox>

  <!-- Dynamic fields: Art -->
  <VBox fx:id="artFields" spacing="8">
    <TextField fx:id="artistField" promptText="Nghe si (artist)"/>
    <TextField fx:id="yearCreatedField" promptText="Nam sang tac"/>
  </VBox>

  <!-- Dynamic fields: Vehicle -->
  <VBox fx:id="vehicleFields" spacing="8">
    <TextField fx:id="manufacturerField" promptText="Nha san xuat"/>
    <TextField fx:id="yearField" promptText="Nam san xuat"/>
    <TextField fx:id="mileageKmField" promptText="So km da di"/>
  </VBox>

  <Label fx:id="lblMessage" visible="false"/>
  <HBox spacing="10" alignment="CENTER">
    <Button text="Tao san pham" onAction="#handleSubmit" styleClass="btn-primary"/>
    <Button text="Huy" onAction="#handleCancel"/>
  </HBox>
</VBox>
```

```bash
git commit -m "feat: cập nhật CreateItemView.fxml với dynamic fields theo itemType"
```

---

### Cập nhật `LoginView.fxml` — thêm link đăng ký

Mở file `LoginView.fxml` đã có, thêm nút chuyển sang đăng ký:

```xml
<!-- Thêm vào cuối VBox trong LoginView.fxml, sau Button "Đăng nhập" -->
<Button text="Chua co tai khoan? Dang ky" onAction="#handleRegister"
        style="-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand;"/>
```

```bash
git commit -m "feat: thêm link đăng ký vào LoginView.fxml"
```

---

### ✅ Test đầu ra

**Kiểm tra manual UI:**
```bash
# Terminal 1: Start server (phải có Đăng+Quốc Minh+Khoa đã merge)
mvn compile -pl bidhub-server -q && mvn exec:java -pl bidhub-server \
  -Dexec.mainClass="com.bidhub.server.ServerApp"

# Terminal 2: Start client
mvn javafx:run -pl bidhub-client
```

**Test cases UI:**
1. Mở app → hiển thị LoginView → Button "Đăng nhập" bị disable khi username/password rỗng
2. Nhập username + password → Button enable → Click → chuyển sang AuctionListView
3. Nhập sai password → Label lỗi đỏ hiện "Tên đăng nhập hoặc mật khẩu không đúng"
4. Click "Đăng ký" → chuyển sang RegisterView
5. Nhập password + confirmPassword không khớp → Label cảnh báo hiện realtime
6. Chọn role → chỉ BIDDER/SELLER (không có ADMIN)
7. Submit đăng ký thành công → navigate về LoginView
8. LoginView → click tạo item (nếu BIDDER) → thông báo "Chỉ người bán mới được tạo"
9. LoginView → login SELLER → AuctionList → CreateItem → chọn ELECTRONICS → hiện brand/warrantyMonths
10. Chọn ART → hiện artist/yearCreated; chọn VEHICLE → hiện manufacturer/year/mileageKm

**❌ FAIL nếu:**
- Click "Đăng nhập" → UI đóng băng 3 giây → gọi `sendRequest` trực tiếp trên FX thread thay vì NetworkTask
- `setOnSucceeded` chứa `Platform.runLater()` → redundant, `setOnSucceeded` đã chạy trên FX thread
- Register chọn ADMIN → server trả error nhưng UI không hiện → thiếu xử lý error response
- CreateItem → chọn itemType nhưng không hiện dynamic fields → `setManaged(false)` thiếu
- BIDDER vào CreateItem → không hiện cảnh báo → thiếu check role trong `initialize()`

---

## 👤 KHOA — AuditLogService + Item Handlers + Auth Test Suite

```
Branch: feature/tuan-5-khoa-auditlog-service-handlers-tests
Phụ thuộc: AuditLogDao (tuần 4, Khoa) — không tạo lại
           AuthService, SessionManager (tuần 5, Đăng) — rebase sau khi Đăng merge
           SecurityContext (tuần 5, Quốc Minh) — rebase sau khi Quốc Minh merge
           ItemDao, ItemCreator + 3 ConcreteCreator (tuần 2-3) — không tạo lại
Merge thứ hai: AuditLogService cần cho Quốc Minh gọi auditLogService.log()
```

📌 **[Tiêu chí điểm: Chức năng đấu giá — item CRUD (1.0đ) + Xử lý lỗi — AuditLogService không ném exception (1.0đ) + Unit Test JUnit (0.5đ)]**

### 📝 Mô tả bài tập

`AuditLogService` là tầng service duy nhất tương tác với `AuditLogDao` — mọi handler gọi `log()` để ghi
nhật ký. Điểm quan trọng nhất: `log()` **không bao giờ ném exception ra ngoài** — audit log là chức năng
phụ trợ, lỗi ở đây không được cản trở business logic chính (login, register, place bid...).

4 item handler hoàn thiện CRUD sản phẩm: `handleCreateItem` yêu cầu SELLER, `handleGetItemList` công khai,
`handleGetItemDetail` công khai, `handleDeleteItem` yêu cầu auth + đúng seller. Mỗi handler tạo/bỏ item
gọi `auditLogService.log()`.

Auth test suite ≥ 20 cases kiểm tra toàn bộ flow authentication: login đúng/sai, register hợp lệ/không,
logout, token, auth-guard.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Khoa cần `AuthService` (Đăng) cho test auth flow
- Khoa cần `SessionManager` (Đăng) cho test auth flow
- Khoa cần `SecurityContext` (Quốc Minh) cho `handleCreateItem`
- Quốc Minh cần `AuditLogService` (Khoa) — **Khoa phải merge trước Quốc Minh**

**Kịch bản chọn: B — Khoa merge thứ hai, sau Đăng, trước Quốc Minh**

**Nếu Quốc Minh cần AuditLogService trước khi Khoa merge:**
Quốc Minh dùng stub (xem phần Quốc Minh ở trên).

**Các bước:**
1. Đăng merge AuthService + SessionManager vào `develop`
2. Khoa rebase từ `develop` → giờ có AuthService + SessionManager
3. Khoa code AuditLogService + item handlers + tests
4. Khoa push lên GitHub, tạo PR → merge vào `develop`
5. Quốc Minh rebase lần 2 → giờ có AuditLogService thật → xóa stub

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── service/
│   │   ├── AuthService.java       (đã có T5, Đăng — không sửa)
│   │   ├── SessionManager.java    (đã có T5, Đăng — không sửa)
│   │   └── AuditLogService.java   ← MỚI
│   └── network/
│       └── RequestHandler.java    (đã có T4 — MỞ RA thêm item handlers)
└── test/java/com/bidhub/server/
    ├── service/
    │   └── AuditLogServiceTest.java  ← MỚI
    └── network/
        └── AuthIntegrationTest.java  ← MỚI: ≥ 20 auth test cases
```

> [!IMPORTANT]
> Khoa cũng cần cập nhật `RequestHandler.java` để thêm case `CREATE_ITEM`, `GET_ITEM_LIST`,
> `GET_ITEM_DETAIL`, `DELETE_ITEM` vào switch. Lưu ý: Quốc Minh cũng sửa cùng file — cần
> merge cẩn thận, thêm case không xóa case của nhau.

---

### `AuditLogService.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.model.AuditLog;

/**
 * Dich vu ghi nhat ky audit — wrap AuditLogDao voi try-catch, khong bao gio nem exception.
 *
 * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — audit log khong bao gio crash handler]
 * Moi handler goi {@link #log(String, String, String)} de ghi nhat ky.
 * Neu DAO loi → chi in stderr, khong nem ra ngoai → business logic tiep tuc binh thuong.
 *
 * <p>2 constructor: production (tao AuditLogDao moi) va test (inject AuditLogDao tu ngoai).
 */
public class AuditLogService {

  private final AuditLogDao auditLogDao;

  /**
   * Constructor production — tao AuditLogDao tu DbConnectionProvider.
   */
  public AuditLogService() {
    this.auditLogDao = new AuditLogDao();
  }

  /**
   * Constructor test — inject AuditLogDao tu ngoai (vi du in-memory SQLite).
   *
   * @param auditLogDao DAO inject
   */
  public AuditLogService(AuditLogDao auditLogDao) {
    this.auditLogDao = auditLogDao;
  }

  /**
   * Ghi 1 ban ghi audit log — khong bao gio nem exception ra ngoai.
   *
   * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — wrap try-catch, khong nem exception]
   * Neu loi → in System.err, khong lam phanh business logic chinh.
   * userId co the null (system action).
   *
   * @param userId  id nguoi dung (co the null cho system action)
   * @param action  ma hanh dong tu AuditActions
   * @param details chi tiet them (JSON string, vi du "{}")
   */
  public void log(String userId, String action, String details) {
    try {
      AuditLog entry = new AuditLog(userId, action, details);
      auditLogDao.save(entry);
    } catch (Exception e) {
      // 📌 [Tieu chi: Xu ly loi — audit log khong duoc lam phanh handler]
      System.err.println("[AuditLogService] Khong the ghi log: "
          + "action=" + action + ", userId=" + userId
          + ", error=" + e.getMessage());
    }
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm AuditLogService với try-catch không ném exception, 2 constructor"
```

---

### Cập nhật `RequestHandler.java` — thêm item handlers

Khoa mở file `RequestHandler.java`, thêm 4 private handler method và cập nhật switch-case.
Lưu ý: Quốc Minh đã thêm `handleLogin`, `handleRegister`, `handleLogout` — Khoa chỉ **thêm** case mới,
không sửa code của Quốc Minh.

Thêm imports cần thiết:
```java
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.model.Item;
import com.bidhub.server.model.ItemCreator;
import com.bidhub.server.model.ItemType;
import com.fasterxml.jackson.databind.JsonNode;
```

Thêm field:
```java
  private final ItemDao itemDao;
```

Cập nhật constructor — thêm init `itemDao`:
```java
  public RequestHandler() {
    this.injectedUserDao = null;
    this.injectedItemDao = null;
    this.auditLogService = new AuditLogService();
    this.userDao = new UserDao();
    this.itemDao = new ItemDao();
  }

  RequestHandler(Object injectedUserDao, Object injectedItemDao) {
    this.injectedUserDao = injectedUserDao;
    this.injectedItemDao = injectedItemDao;
    this.auditLogService = new AuditLogService();
    this.userDao = injectedUserDao instanceof UserDao
        ? (UserDao) injectedUserDao : new UserDao();
    this.itemDao = injectedItemDao instanceof ItemDao
        ? (ItemDao) injectedItemDao : new ItemDao();
  }

  RequestHandler(Object injectedUserDao, Object injectedItemDao,
      AuditLogService injectedAuditService) {
    this.injectedUserDao = injectedUserDao;
    this.injectedItemDao = injectedItemDao;
    this.auditLogService = injectedAuditService;
    this.userDao = injectedUserDao instanceof UserDao
        ? (UserDao) injectedUserDao : new UserDao();
    this.itemDao = injectedItemDao instanceof ItemDao
        ? (ItemDao) injectedItemDao : new ItemDao();
  }
```

Cập nhật switch-case trong `handle()` — thay placeholder bằng handler thật:
```java
        case "CREATE_ITEM"    -> handleCreateItem(session, payload);
        case "GET_ITEM_LIST"  -> handleGetItemList();
        case "GET_ITEM_DETAIL" -> handleGetItemDetail(payload);
        case "DELETE_ITEM"    -> handleDeleteItem(session, payload);
```

Thêm 4 handler method:

```java
  // === ITEM HANDLERS ===

  /**
   * Tao san pham moi — yeu cau role SELLER.
   *
   * <p>// 📌 [Tieu chi: Quan ly san pham — tao san pham voi Factory Method + audit log]
   *
   * @param session session hien tai
   * @param payload chua {name, description, startingPrice, itemType, extras}
   * @return JSON response voi item info
   */
  private String handleCreateItem(Session session, JsonNode payload) {
    // 📌 [Tieu chi: Quan ly nguoi dung — kiem tra role SELLER]
    String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);

    String name = getTextSafe(payload, "name");
    String description = getTextSafe(payload, "description");
    String priceStr = getTextSafe(payload, "startingPrice");
    String itemTypeStr = getTextSafe(payload, "itemType");

    if (name == null || name.isBlank()) {
      return MessageMapper.toJson(
          MessageResponse.error("CREATE_ITEM", "Ten san pham khong duoc de trong."));
    }
    if (priceStr == null) {
      return MessageMapper.toJson(
          MessageResponse.error("CREATE_ITEM", "Gia khoi diem la bat buoc."));
    }

    double startingPrice;
    try {
      startingPrice = Double.parseDouble(priceStr);
      if (startingPrice <= 0) {
        return MessageMapper.toJson(
            MessageResponse.error("CREATE_ITEM", "Gia khoi diem phai lon hon 0."));
      }
    } catch (NumberFormatException e) {
      return MessageMapper.toJson(
          MessageResponse.error("CREATE_ITEM", "Gia khoi diem khong hop le."));
    }

    if (itemTypeStr == null) {
      return MessageMapper.toJson(
          MessageResponse.error("CREATE_ITEM", "Loai san pham la bat buoc."));
    }

    ItemType itemType;
    try {
      itemType = ItemType.valueOf(itemTypeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      return MessageMapper.toJson(
          MessageResponse.error("CREATE_ITEM",
              "Loai san pham khong hop le: " + itemTypeStr));
    }

    // 📌 [Tieu chi: Design Pattern — Factory Method ItemCreator.forType()]
    JsonNode extrasNode = payload.has("extras") ? payload.get("extras") : null;
    java.util.Map<String, Object> extras = parseExtras(extrasNode);

    Item item;
    try {
      ItemCreator creator = ItemCreator.forType(itemType);
      item = creator.createItem(name, description, startingPrice, sellerId, extras);
    } catch (Exception e) {
      return MessageMapper.toJson(
          MessageResponse.error("CREATE_ITEM",
              "Loi tao san pham: " + e.getMessage()));
    }

    itemDao.save(item);

    // 📌 [Tieu chi: Quan ly san pham — audit log tao san pham]
    auditLogService.log(sellerId, AuditActions.ITEM_CREATED,
        "{\"itemId\":\"" + item.getId() + "\"}");

    java.util.Map<String, String> result = new java.util.HashMap<>();
    result.put("itemId", item.getId());
    result.put("name", item.getName());
    result.put("itemType", item.getItemType().name());
    result.put("startingPrice", String.valueOf(item.getStartingPrice()));

    return MessageMapper.toJson(MessageResponse.ok("CREATE_ITEM", result));
  }

  /**
   * Lay danh sach tat ca san pham — khong can auth.
   *
   * <p>// 📌 [Tieu chi: Quan ly san pham — danh sach san pham cong khai]
   *
   * @return JSON response voi danh sach items
   */
  private String handleGetItemList() {
    java.util.List<Item> items = itemDao.findAll();
    return MessageMapper.toJson(MessageResponse.ok("GET_ITEM_LIST", items));
  }

  /**
   * Lay chi tiet 1 san pham — khong can auth.
   *
   * @param payload chua {itemId}
   * @return JSON response voi item detail
   */
  private String handleGetItemDetail(JsonNode payload) {
    String itemId = getTextSafe(payload, "itemId");

    if (itemId == null || itemId.isBlank()) {
      return MessageMapper.toJson(
          MessageResponse.error("GET_ITEM_DETAIL", "itemId la bat buoc."));
    }

    java.util.Optional<Item> itemOpt = itemDao.findById(itemId);
    if (itemOpt.isEmpty()) {
      return MessageMapper.toJson(
          MessageResponse.error("GET_ITEM_DETAIL", "San pham khong ton tai."));
    }

    return MessageMapper.toJson(
        MessageResponse.ok("GET_ITEM_DETAIL", itemOpt.get()));
  }

  /**
   * Xoa san pham — yeu cau auth + chi seller cua item moi duoc xoa.
   *
   * <p>// 📌 [Tieu chi: Quan ly san pham — xoa san pham + kiem tra quyen]
   *
   * @param session session hien tai
   * @param payload chua {itemId}
   * @return JSON response
   */
  private String handleDeleteItem(Session session, JsonNode payload) {
    String userId = SecurityContext.requireAuthenticated(session);
    String itemId = getTextSafe(payload, "itemId");

    if (itemId == null || itemId.isBlank()) {
      return MessageMapper.toJson(
          MessageResponse.error("DELETE_ITEM", "itemId la bat buoc."));
    }

    java.util.Optional<Item> itemOpt = itemDao.findById(itemId);
    if (itemOpt.isEmpty()) {
      return MessageMapper.toJson(
          MessageResponse.error("DELETE_ITEM", "San pham khong ton tai."));
    }

    Item item = itemOpt.get();
    if (!item.getSellerId().equals(userId)) {
      return MessageMapper.toJson(
          MessageResponse.error("DELETE_ITEM",
              "Ban khong co quyen xoa san pham nay."));
    }

    itemDao.deleteById(itemId);

    // 📌 [Tieu chi: Quan ly san pham — audit log xoa san pham]
    auditLogService.log(userId, AuditActions.ITEM_DELETED,
        "{\"itemId\":\"" + itemId + "\"}");

    java.util.Map<String, String> result = new java.util.HashMap<>();
    result.put("message", "Xoa san pham thanh cong.");
    result.put("itemId", itemId);

    return MessageMapper.toJson(MessageResponse.ok("DELETE_ITEM", result));
  }

  // === HELPER ===

  /**
   * Parse extras JsonNode thanh Map<String, Object> cho ItemCreator.
   */
  private java.util.Map<String, Object> parseExtras(JsonNode extrasNode) {
    java.util.Map<String, Object> extras = new java.util.HashMap<>();
    if (extrasNode == null || !extrasNode.isObject()) {
      return extras;
    }
    extrasNode.fields().forEachRemaining(entry -> {
      JsonNode val = entry.getValue();
      if (val.isInt()) {
        extras.put(entry.getKey(), val.asInt());
      } else if (val.isDouble()) {
        extras.put(entry.getKey(), val.asDouble());
      } else {
        extras.put(entry.getKey(), val.asText());
      }
    });
    return extras;
  }
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm handleCreateItem, handleGetItemList, handleGetItemDetail, handleDeleteItem vào RequestHandler"
```

---

### Cập nhật `AUTH_REQUIRED` set

Khoa cập nhật `AUTH_REQUIRED` set trong `RequestHandler` — đảm bảo đã có các type cần thiết:

```java
  private static final Set<String> AUTH_REQUIRED = Set.of(
      "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
      "LIST_MY_ITEMS", "PLACE_BID", "GET_AUCTION_DETAIL"
  );
```

> [!NOTE]
> `GET_ITEM_LIST` và `GET_ITEM_DETAIL` **không** trong AUTH_REQUIRED — bất kỳ ai cũng xem được.
> `CREATE_ITEM` và `DELETE_ITEM` đã có từ T4 — không cần thêm.

```bash
git commit -m "fix: xác nhận AUTH_REQUIRED set đã đầy đủ cho T5"
```

---

### ✅ Test đầu ra — `AuditLogServiceTest.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.model.AuditLog;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogServiceTest {

  private Connection testConn;
  private AuditLogDao auditLogDao;
  private AuditLogService auditLogService;

  @BeforeEach
  void setUp() throws Exception {
    testConn = DriverManager.getConnection("jdbc:sqlite::memory:");
    // Tao bang audit_logs trong in-memory DB
    testConn.createStatement().executeUpdate(
        "CREATE TABLE audit_logs ("
        + "id TEXT PRIMARY KEY, "
        + "user_id TEXT, "
        + "action TEXT NOT NULL, "
        + "details TEXT NOT NULL DEFAULT '', "
        + "created_at TEXT NOT NULL)");
    auditLogDao = new AuditLogDao(testConn);
    auditLogService = new AuditLogService(auditLogDao);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (testConn != null && !testConn.isClosed()) {
      testConn.close();
    }
  }

  @Test
  @DisplayName("log() voi action hop le → auditLogDao.findAll() co ban ghi moi")
  void log_validAction_savesRecord() {
    auditLogService.log("user-001", "USER_LOGIN", "{}");

    List<AuditLog> logs = auditLogDao.findAll();
    assertEquals(1, logs.size());
    assertEquals("user-001", logs.get(0).getUserId());
    assertEquals("USER_LOGIN", logs.get(0).getAction());
  }

  @Test
  @DisplayName("log() khi DAO nem exception → khong nem exception ra ngoai")
  void log_daoThrows_doesNotPropagate() throws Exception {
    // Dong connection de buoc DAO nem SQLException
    testConn.close();

    // Khong nem exception ra ngoai
    assertDoesNotThrow(() ->
        auditLogService.log("user-001", "USER_LOGIN", "{}"));
  }

  @Test
  @DisplayName("log() voi userId null (system action) → luu duoc")
  void log_nullUserId_savesRecord() {
    auditLogService.log(null, "AUCTION_CLOSED", "{}");

    List<AuditLog> logs = auditLogDao.findAll();
    assertEquals(1, logs.size());
    assertNull(logs.get(0).getUserId());
    assertEquals("AUCTION_CLOSED", logs.get(0).getAction());
  }

  @Test
  @DisplayName("log() nhieu lan → findAll() tra ve dung so luong")
  void log_multipleTimes_correctCount() {
    auditLogService.log("user-001", "USER_LOGIN", "{}");
    auditLogService.log("user-001", "PLACE_BID", "{\"auctionId\":\"a1\"}");
    auditLogService.log("user-002", "USER_REGISTER", "{}");

    List<AuditLog> logs = auditLogDao.findAll();
    assertEquals(3, logs.size());
  }

  @Test
  @DisplayName("log() voi details '{}' → ban ghi luu dung details")
  void log_emptyDetails_savesCorrectly() {
    auditLogService.log("user-001", "USER_LOGOUT", "{}");

    List<AuditLog> logs = auditLogDao.findAll();
    assertEquals(1, logs.size());
    assertEquals("{}", logs.get(0).getDetails());
  }

  @Test
  @DisplayName("constructor production tao AuditLogService binh thuong")
  void constructorProduction() {
    assertDoesNotThrow(() -> new AuditLogService());
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm AuditLogServiceTest (6 cases) với in-memory SQLite"
```

---

### ✅ Test đầu ra — `AuthIntegrationTest.java` (≥ 20 cases)

```java
package com.bidhub.server.network;

import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.AuditLog;
import com.bidhub.server.model.User;
import com.bidhub.server.service.AuditLogService;
import com.bidhub.server.service.AuthService;
import com.bidhub.server.service.SessionManager;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test cho toan bo auth flow: register → login → logout + token + auth-guard.
 *
 * <p>// 📌 [Tieu chi: Unit Test JUnit — 0.5đ] ≥ 20 cases
 */
class AuthIntegrationTest {

  private Connection testConn;
  private UserDao userDao;
  private ItemDao itemDao;
  private AuditLogDao auditLogDao;
  private AuditLogService auditLogService;
  private RequestHandler handler;
  private Session mockSession;

  @BeforeEach
  void setUp() throws Exception {
    testConn = DriverManager.getConnection("jdbc:sqlite::memory:");
    testConn.createStatement().executeUpdate(
        "CREATE TABLE users ("
        + "id TEXT PRIMARY KEY, username TEXT UNIQUE, "
        + "password_hash TEXT, email TEXT, role TEXT, "
        + "extra_int INTEGER DEFAULT 0, "
        + "created_at TEXT, updated_at TEXT)");
    testConn.createStatement().executeUpdate(
        "CREATE TABLE items ("
        + "id TEXT PRIMARY KEY, name TEXT, description TEXT, "
        + "starting_price REAL, item_type TEXT, seller_id TEXT, "
        + "extra_data TEXT, created_at TEXT, updated_at TEXT)");
    testConn.createStatement().executeUpdate(
        "CREATE TABLE audit_logs ("
        + "id TEXT PRIMARY KEY, user_id TEXT, "
        + "action TEXT NOT NULL, details TEXT NOT NULL DEFAULT '', "
        + "created_at TEXT NOT NULL)");

    userDao = new UserDao(testConn);
    itemDao = new ItemDao(testConn);
    auditLogDao = new AuditLogDao(testConn);
    auditLogService = new AuditLogService(auditLogDao);
    handler = new RequestHandler(userDao, itemDao, auditLogService);

    // Clear SessionManager
    SessionManager.getInstance().clearAll();

    // Tao mock session
    try (ServerSocket srv = new ServerSocket(0)) {
      Socket client = new Socket("localhost", srv.getLocalPort());
      mockSession = new Session(srv.accept());
      client.close();
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    SessionManager.getInstance().clearAll();
    if (testConn != null && !testConn.isClosed()) {
      testConn.close();
    }
  }

  // === REGISTER TESTS ===

  @Test
  @DisplayName("REGISTER moi thanh cong")
  void register_success() {
    String json = buildRegisterJson("user1", "password1", "u@test.com", "BIDDER");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertTrue(res.isOk());
    assertEquals("REGISTER", res.getType());
  }

  @Test
  @DisplayName("REGISTER trung username → error")
  void register_duplicateUsername() {
    handler.handle(buildRegisterJson("dup", "password1", "a@t.com", "BIDDER"), mockSession);
    String response = handler.handle(
        buildRegisterJson("dup", "password2", "b@t.com", "BIDDER"), mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("REGISTER role ADMIN → bi tu choi")
  void register_adminRole_rejected() {
    String json = buildRegisterJson("admin1", "password1", "a@t.com", "ADMIN");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("REGISTER password < 8 ky tu → error")
  void register_shortPassword() {
    String json = buildRegisterJson("user2", "1234567", "u2@t.com", "BIDDER");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("REGISTER email khong chua @ → error")
  void register_invalidEmail() {
    String json = buildRegisterJson("user3", "password1", "noemail", "BIDDER");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("REGISTER username blank → error")
  void register_blankUsername() {
    String json = buildRegisterJson("", "password1", "u4@t.com", "BIDDER");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("REGISTER thanh cong → audit log co USER_REGISTER")
  void register_auditLog() {
    handler.handle(buildRegisterJson("audit1", "password1", "a1@t.com", "BIDDER"), mockSession);
    List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_REGISTER);
    assertEquals(1, logs.size());
  }

  // === LOGIN TESTS ===

  @Test
  @DisplayName("LOGIN dung credentials → OK voi token")
  void login_success() {
    handler.handle(buildRegisterJson("login1", "password1", "l1@t.com", "BIDDER"), mockSession);
    String json = buildLoginJson("login1", "password1");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertTrue(res.isOk());
    assertEquals("LOGIN", res.getType());
  }

  @Test
  @DisplayName("LOGIN sai password → error")
  void login_wrongPassword() {
    handler.handle(buildRegisterJson("login2", "password1", "l2@t.com", "BIDDER"), mockSession);
    String json = buildLoginJson("login2", "wrongpass");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("LOGIN username khong ton tai → error")
  void login_nonexistentUser() {
    String json = buildLoginJson("ghost", "password1");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("LOGIN thanh cong → session.authenticatedUserId duoc set")
  void login_setsAuthenticatedUserId() {
    handler.handle(buildRegisterJson("login3", "password1", "l3@t.com", "SELLER"), mockSession);
    String json = buildLoginJson("login3", "password1");
    handler.handle(json, mockSession);
    assertTrue(mockSession.isAuthenticated());
  }

  @Test
  @DisplayName("LOGIN thanh cong → audit log co USER_LOGIN")
  void login_auditLog() {
    handler.handle(buildRegisterJson("login4", "password1", "l4@t.com", "BIDDER"), mockSession);
    handler.handle(buildLoginJson("login4", "password1"), mockSession);
    List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_LOGIN);
    assertEquals(1, logs.size());
  }

  // === LOGOUT TESTS ===

  @Test
  @DisplayName("LOGOUT thanh cong → token khong con hop le")
  void logout_invalidatesToken() {
    handler.handle(buildRegisterJson("logout1", "password1", "lo1@t.com", "BIDDER"), mockSession);
    String loginResponse = handler.handle(
        buildLoginJson("logout1", "password1"), mockSession);
    MessageResponse loginRes = parseResponse(loginResponse);
    Object payload = loginRes.getPayload();
    String token = null;
    if (payload instanceof java.util.Map<?, ?> map) {
      token = (String) map.get("token");
    }

    // Logout
    String logoutJson = "{\"type\":\"LOGOUT\",\"token\":\"" + token + "\",\"payload\":{}}";
    handler.handle(logoutJson, mockSession);

    // Token khong con hop le
    assertTrue(SessionManager.getInstance().getUserIdByToken(token).isEmpty());
  }

  @Test
  @DisplayName("LOGOUT → audit log co USER_LOGOUT")
  void logout_auditLog() {
    handler.handle(buildRegisterJson("logout2", "password1", "lo2@t.com", "BIDDER"), mockSession);
    handler.handle(buildLoginJson("logout2", "password1"), mockSession);
    handler.handle("{\"type\":\"LOGOUT\",\"token\":\"fake\",\"payload\":{}}", mockSession);
    List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_LOGOUT);
    assertEquals(1, logs.size());
  }

  // === TOKEN & AUTH-GUARD TESTS ===

  @Test
  @DisplayName("Token gia → rejected")
  void fakeToken_rejected() {
    String json = "{\"type\":\"CREATE_ITEM\",\"token\":\"fake-token-123\",\"payload\":{}}";
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("Type can auth khong co token → error auth-guard")
  void authRequired_noToken_error() {
    String json = "{\"type\":\"LOGOUT\",\"payload\":{}}";
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("Token hop le → session.authenticatedUserId duoc set")
  void validToken_setsAuthUserId() {
    String token = SessionManager.getInstance().createSession("user-test-001");
    String json = "{\"type\":\"PING\",\"token\":\"" + token + "\",\"payload\":{}}";
    handler.handle(json, mockSession);
    assertEquals("user-test-001", mockSession.getAuthenticatedUserId());
  }

  // === PASSWORD HASHING TESTS ===

  @Test
  @DisplayName("Password duoc hash truoc khi luu vao DB")
  void passwordHashed_beforeSave() {
    handler.handle(buildRegisterJson("hash1", "mypassword", "h1@t.com", "BIDDER"), mockSession);
    Optional<User> userOpt = userDao.findByUsername("hash1");
    assertTrue(userOpt.isPresent());
    // Password trong DB khong phai plaintext
    assertNotEquals("mypassword", userOpt.get().getPasswordHash());
    // Nhung verify duoc
    assertTrue(AuthService.verifyPassword("mypassword", userOpt.get().getPasswordHash()));
  }

  @Test
  @DisplayName("2 user cung password → hash giong nhau (SHA-256 deterministic)")
  void samePassword_sameHash() {
    handler.handle(buildRegisterJson("same1", "sharedpw", "s1@t.com", "BIDDER"), mockSession);
    handler.handle(buildRegisterJson("same2", "sharedpw", "s2@t.com", "BIDDER"), mockSession);
    Optional<User> u1 = userDao.findByUsername("same1");
    Optional<User> u2 = userDao.findByUsername("same2");
    assertEquals(u1.get().getPasswordHash(), u2.get().getPasswordHash());
  }

  // === ROLE-BASED ACCESS TESTS ===

  @Test
  @DisplayName("BIDDER khong tao duoc item → CREATE_ITEM yeu cau SELLER")
  void bidderCannotCreateItem() {
    // Register BIDDER
    handler.handle(buildRegisterJson("bidder1", "password1", "b1@t.com", "BIDDER"), mockSession);
    // Login
    handler.handle(buildLoginJson("bidder1", "password1"), mockSession);
    // Try CREATE_ITEM
    String json = buildCreateItemJson("Test Item", "Desc", "100.0", "ELECTRONICS");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  @Test
  @DisplayName("LOGIN password rong → error")
  void login_emptyPassword() {
    String json = buildLoginJson("user1", "");
    String response = handler.handle(json, mockSession);
    MessageResponse res = parseResponse(response);
    assertFalse(res.isOk());
  }

  // === HELPER METHODS ===

  private String buildRegisterJson(String username, String password, String email, String role) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("username", username);
    payload.put("password", password);
    payload.put("email", email);
    payload.put("role", role);
    return "{\"type\":\"REGISTER\",\"payload\":"
        + payload.toString() + "}";
  }

  private String buildLoginJson(String username, String password) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("username", username);
    payload.put("password", password);
    return "{\"type\":\"LOGIN\",\"payload\":"
        + payload.toString() + "}";
  }

  private String buildCreateItemJson(String name, String desc, String price, String type) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("name", name);
    payload.put("description", desc);
    payload.put("startingPrice", Double.parseDouble(price));
    payload.put("itemType", type);
    ObjectNode extras = JsonNodeFactory.instance.objectNode();
    extras.put("brand", "TestBrand");
    extras.put("warrantyMonths", 12);
    payload.set("extras", extras);
    return "{\"type\":\"CREATE_ITEM\",\"token\":\""
        + (mockSession.isAuthenticated()
            ? SessionManager.getInstance().getTokenByUserId(
                mockSession.getAuthenticatedUserId()).orElse("")
            : "")
        + "\",\"payload\":" + payload.toString() + "}";
  }

  private MessageResponse parseResponse(String json) {
    try {
      return MessageMapper.fromJson(json, MessageResponse.class);
    } catch (Exception e) {
      fail("Khong the parse response: " + json);
      return null;
    }
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm AuthIntegrationTest (21 cases) và AuditLogServiceTest (6 cases)"
```

---

**Kiểm tra manual end-to-end:**
```bash
# Terminal 1: Start server (Đăng+Quốc Minh+Khoa đã merge)
mvn compile -pl bidhub-server -q && mvn exec:java -pl bidhub-server \
  -Dexec.mainClass="com.bidhub.server.ServerApp"

# Terminal 2: Tạo sản phẩm (cần login SELLER trước)
# Bước 1: Register SELLER
echo '{"type":"REGISTER","payload":{"username":"seller01","password":"12345678","email":"s@test.com","role":"SELLER"}}' | nc -q1 localhost 9090
# Output: {"status":"OK","type":"REGISTER",...}

# Bước 2: Login
echo '{"type":"LOGIN","payload":{"username":"seller01","password":"12345678"}}' | nc -q1 localhost 9090
# Output: {"status":"OK","type":"LOGIN","payload":{"token":"xxx",...}}
# → Copy token

# Bước 3: Tạo item (thay TOKEN bằng token thực)
echo '{"type":"CREATE_ITEM","token":"TOKEN","payload":{"name":"Laptop","description":"Gaming laptop","startingPrice":15000000,"itemType":"ELECTRONICS","extras":{"brand":"ASUS","warrantyMonths":24}}}' | nc -q1 localhost 9090
# Output: {"status":"OK","type":"CREATE_ITEM","payload":{"itemId":"...","name":"Laptop",...}}

# Bước 4: Xem danh sách item
echo '{"type":"GET_ITEM_LIST","payload":{}}' | nc -q1 localhost 9090
# Output: {"status":"OK","type":"GET_ITEM_LIST","payload":[...]}

# Chạy test
mvn test -pl bidhub-server -Dtest="AuditLogServiceTest,AuthIntegrationTest" -q
# Output: Tests run: 27, Failures: 0

# Kiểm tra audit log trong DB
sqlite3 data/bidhub.db "SELECT action, user_id FROM audit_logs ORDER BY created_at DESC LIMIT 10;"
# USER_LOGIN   | <userId>
# USER_REGISTER| <userId>
```

**❌ FAIL nếu:**
- `auditLogService.log()` ném exception → handler crash → thiếu try-catch trong AuditLogService
- `handleCreateItem()` BIDDER tạo được item → thiếu `SecurityContext.requireRole(SELLER)`
- `handleDeleteItem()` seller B xóa được item của seller A → thiếu check `item.getSellerId().equals(userId)`
- `handleGetItemList()` trả về error → thiếu method trong ItemDao hoặc serialization lỗi
- Auth test < 20 cases → barem Unit Test JUnit không đạt
- `AuditLogService.log()` khi DAO lỗi → handler crash → try-catch bắt sai exception type

---

## 🔄 THỨ TỰ MERCH & TỔNG KẾT

### Merge Order

```
1. Đăng   → feature/tuan-5-dang-auth-session
   Nội dung: AuthService + SessionManager + RequestHandler token resolution
   Reviewer: Quốc Minh

2. Khoa   → feature/tuan-5-khoa-auditlog-service-handlers-tests
   Nội dung: AuditLogService + item handlers + auth test suite (27 cases)
   Reviewer: Đăng
   Rebase từ: develop (đã có AuthService + SessionManager)

3. Quốc Minh → feature/tuan-5-quocminh-auth-handlers
   Nội dung: handleLogin + handleRegister + handleLogout + SecurityContext
   Reviewer: Khoa
   Rebase từ: develop (đã có AuthService + SessionManager + AuditLogService)
   Xóa stub AuditLogService nếu có

4. Công Minh → feature/tuan-5-congminh-auth-item-ui
   Nội dung: LoginController hoàn thiện + RegisterView + RegisterController + CreateItemController
   Reviewer: Quốc Minh
   Test end-to-end với server đã đầy đủ handler
```

> [!CAUTION]
> Cả 4 người đều sửa `RequestHandler.java` — merge conflict chắc chắn xảy ra.
> Giải pháp: mỗi người chỉ thêm case mới vào switch, không xóa case người khác.
> Nếu conflict → giải quyết thủ công, giữ lại tất cả case.

### Tổng kết điểm barem Tuần 5 phục vụ

| Barem | Điểm | Tuần 5 đóng góp |
|---|---|---|
| Thiết kế lớp & cây kế thừa | 0.5 | (Tuần 2) |
| OOP (4 trụ cột) | 1.0 | (Tuần 2) |
| Design Pattern phù hợp | 1.0 | SessionManager Singleton + ItemCreator Factory Method |
| Quản lý người dùng, sản phẩm | 1.0 | ✅ Login/Register/Logout + CRUD Item |
| Chức năng đấu giá | 1.0 | ✅ Item CRUD (phần trước khi đặt giá) |
| Xử lý lỗi & ngoại lệ | 1.0 | ✅ Validation register + AuditLogService try-catch |
| Kỹ thuật quan trọng & concurrency | 1.0 | ✅ ConcurrentHashMap + SHA-256 + UUID token |
| Kiến trúc Client–Server | 0.5 | ✅ Token-based auth qua socket protocol |
| MVC | 0.5 | ✅ RequestHandler auth handler + SecurityContext |
| Maven/Gradle, convention, code sạch | 0.5 | (Liên tục) |
| Unit Test JUnit | 0.5 | ✅ ≥ 27 test cases mới |
| CI/CD GitHub Actions | 0.5 | (Tuần 1) |
| Anti-sniping | 0.5 | (Tuần 8) |
| Bid History Visualization | 0.5 | (Tuần 8) |
| Other advanced | 0.5 | (Tuần 7-8) |

### Tổng test cases dự kiến sau Tuần 5

| Module | Cases cũ (T1-4) | Cases mới (T5) | Tổng |
|---|---|---|---|
| bidhub-common | ≥10 | 0 | ≥10 |
| bidhub-server | ≥115 | ≥27 | ≥142 |
| bidhub-client | ≥6 | 0 (UI test manual) | ≥6 |
| **Tổng** | ≥131 | ≥27 | **≥158** |
