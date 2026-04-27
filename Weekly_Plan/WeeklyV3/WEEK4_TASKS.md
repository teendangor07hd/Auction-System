# 📋 TUẦN 4 — BÀI TẬP CHI TIẾT: Socket Server + RequestHandler + Audit Log DAO

## 🎯 MỤC TIÊU TUẦN 4

Tuần này thiết lập hai trụ cột song song: tầng mạng Server↔Client hoạt động end-to-end, và hạ tầng
Audit Log sẵn sàng cho Tuần 5 tích hợp. Cuối tuần, cả nhóm phải có:

- ✅ `SocketServerCore` khởi động cổng 9090, chấp nhận nhiều client song song qua thread pool 30
- ✅ `Session` đại diện 1 kết nối TCP — `sendMessage()` thread-safe, cleanup khi ngắt
- ✅ `ClientConnectionThread` đọc từng dòng JSON, dispatch `RequestHandler`, gửi response ngay
- ✅ `schema.sql` cập nhật bảng thứ 5 `audit_logs`, `MigrationRunner` tự chạy đủ 5 bảng
- ✅ `MessageRequest` / `MessageResponse` / `MessageMapper` — protocol JSON chuẩn dùng toàn server
- ✅ `RequestHandler` xử lý PING trả pong, auth-guard route cần đăng nhập, malformed JSON không crash
- ✅ `ServerGateway` (Singleton client) connect/sendRequest/disconnect — `NetworkTask<T>` không block UI
- ✅ `ClientSession` (Singleton client) lưu token + userId + role sau login thành công
- ✅ `AuditLog` model + `AuditActions` interface + `AuditLogDao` đầy đủ 5 method
- ✅ ≥ 15 test cases mới pass (protocol + AuditLogDao) — tổng project ≥ 77 cases

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** barem: **Kiến trúc Client–Server** (0.5đ) + **MVC — RequestHandler
> tầng server** (phần 0.5đ) + **Singleton** bổ sung `ServerGateway` + `ClientSession` (phần 1.0đ
> Design Patterns). `AuditLog` là class domain thứ 6 phục vụ **Thiết kế lớp** (0.5đ). Ngoài ra
> `AuditLogDao` là nền bắt buộc cho `AuditLogService` Tuần 5 — Quốc Minh cần inject log ngay vào
> `handleLogin`/`handleRegister`.

> [!CAUTION]
> **Tuyệt đối không tạo lại** bất kỳ class nào từ Tuần 1–3:
> `Entity`, `BidHubException` + 7 subclass, `ConfigLoader`, `MigrationRunner`, `DbConnectionProvider`,
> `UserRole`, `User`, `Bidder`, `Seller`, `Admin`, `Item`, `Electronics`, `Art`, `Vehicle`,
> `ItemCreator` + 3 ConcreteCreator, `AuctionStatus`, `Auction`, `BidTransaction`,
> `UserDao`, `ItemDao`, `AuctionDao`, `BidDao`,
> `BidHubApp`, `ViewRouter`, `ContextAware`, `LoginController`, 3 Controller skeleton, `Views`.
>
> **Đăng** phải **MỞ** (không tạo lại) `ServerApp.java` và `schema.sql` từ T3.
> **Thứ tự merge quan trọng:** Đăng merge schema trước → Khoa rebase để có bảng `audit_logs` khi viết test.

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI HỌC (Tự học)

> [!CAUTION]
> Hoàn thành **trước Thứ 5**. Mục đích không phải học Java chung — mà là hiểu cơ chế hoạt động của
> code người khác viết tuần này, để review chéo Chủ nhật trôi chảy và trả lời được vấn đáp giảng viên
> về tầng networking + audit log mà không lúng túng.

---

### Bài 0.1 — Java ServerSocket & Thread Pool trong BidHub

**Tài liệu bắt buộc:**
- https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
- https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executors.html

**Câu hỏi hỏi miệng Chủ nhật:**
1. `ServerSocket.accept()` là blocking call — nó block cái gì và unblock khi nào? Nếu `SocketServerCore`
   gọi `accept()` trực tiếp trên main thread mà không dùng thread pool, Client B kết nối trong lúc
   Client A đang xử lý thì xảy ra vấn đề gì?
2. `Executors.newFixedThreadPool(30)` — client thứ 31 kết nối khi 30 thread đều bận thì điều gì xảy
   ra? Task bị dropped hay vào queue? Queue đó có giới hạn không?
3. Tại sao `Session.sendMessage()` phải dùng `synchronized`? Không có `synchronized`, 2 thread cùng
   gọi `sendMessage()` trên cùng 1 `PrintWriter` dẫn đến kết quả gì phía client nhận?
4. `ClientConnectionThread.run()` dùng `BufferedReader.readLine()` — method này trả về `null` khi nào?
   Không check `null`, vòng lặp while chạy mãi hay crash? Code đúng cần điều kiện thoát nào?
5. `volatile boolean running` — tại sao cần `volatile`? Không có `volatile`, `shutdown()` từ thread
   khác set `running = false` có đảm bảo vòng lặp `accept()` thấy ngay không?
6. **[Câu hỏi nâng cao]** Thread pool 30 thread — Tuần 7 có 50 client `PLACE_BID` đồng thời vào cùng
   1 auction. 20 request chờ trong queue. Nếu mỗi handler giữ lock `ReentrantLock` 200ms, thời gian
   chờ tối đa của request thứ 50 là bao nhiêu? Nâng pool lên 50 giải quyết được không?

---

### Bài 0.2 — Jackson: JsonNode, @JsonIgnoreProperties & MessageMapper

**Tài liệu bắt buộc:**
- https://fasterxml.github.io/jackson-databind/javadoc/2.17/com/fasterxml/jackson/databind/JsonNode.html
- https://github.com/FasterXML/jackson-databind#1-minute-tutorial

**Câu hỏi hỏi miệng Chủ nhật:**
1. `MessageRequest.payload` dùng `JsonNode` thay vì `Map<String,Object>` — khi handler cần đọc
   `payload.get("username").asText()`, điều gì xảy ra nếu client không gửi field `username`?
   `JsonNode.get()` trả về gì, và cách viết null-safe đúng là thế nào?
2. `@JsonIgnoreProperties(ignoreUnknown = true)` — không có annotation này, client gửi thêm field
   `"clientVersion":"1.0"` thì `fromJson()` ném exception gì? Tại sao annotation này quan trọng
   khi cần nâng cấp protocol mà không breaking client cũ?
3. `@JsonInclude(Include.NON_NULL)` trên `MessageResponse` — field `payload = null` trong error response
   có được serialize vào JSON không? Output phía client nhận trông như thế nào?
4. `MessageMapper.MAPPER` khai báo `static final` — nếu tạo `new ObjectMapper()` trong mỗi lần gọi
   `toJson()`, hệ quả về performance và thread-safety là gì?
5. `MessageMapper.fromJson("broken json", MessageRequest.class)` ném exception gì? `RequestHandler`
   phải bắt tại đâu để không crash `ClientConnectionThread`?
6. **[Câu hỏi nâng cao]** Tuần 7, `BidUpdateEvent` chứa `LocalDateTime bidTime`. Jackson mặc định
   không serialize `LocalDateTime`. Cần register module gì vào `ObjectMapper`? Trade-off giữa dùng
   `JavaTimeModule` và dùng `@JsonSerialize` trực tiếp trên field là gì?

---

### Bài 0.3 — JavaFX Task<T>, Platform.runLater() và ServerGateway

**Tài liệu bắt buộc:**
- https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html
- https://openjfx.io/javadoc/21/javafx.graphics/javafx/application/Platform.html#runLater(java.lang.Runnable)

**Câu hỏi hỏi miệng Chủ nhật:**
1. `ServerGateway.sendRequest()` gọi `socket.getOutputStream()` — I/O blocking. Nếu gọi thẳng trong
   `LoginController` trên FX thread, server mất 3 giây → giao diện phản ứng thế nào? Tại sao đây là
   lỗi nghiêm trọng cần tránh tuyệt đối?
2. `NetworkTask<T>` extends `Task<T>`, method `call()` chạy trên thread nào? Handler `setOnSucceeded`
   chạy trên thread nào? Có cần `Platform.runLater()` bên trong `setOnSucceeded` không?
3. Viết pseudocode đầy đủ cho `LoginController.handleLogin()`: tạo `NetworkTask`, set onSucceeded,
   set onFailed, start bằng `new Thread(task).start()`. Chỉ rõ `Platform.runLater()` được gọi ở đâu.
4. `ServerGateway.sendRequest()` dùng `synchronized` — nếu 2 `NetworkTask` gọi `sendRequest()` đồng
   thời, điều gì xảy ra với thứ tự request/response trên cùng 1 TCP stream?
5. `ClientSession` lưu `token` dạng String — tại sao không lưu toàn bộ `User` object? Nếu server
   lock user ngay sau login, client có biết ngay không?
6. **[Câu hỏi nâng cao]** Tuần 7 thêm `EventListenerThread` — luôn chạy nền, đọc push event từ
   server. `sendRequest()` và `EventListenerThread` cùng dùng 1 `BufferedReader` — race condition
   nào xảy ra? Thiết kế nào giải quyết (2 connection riêng biệt, hay 1 connection với message type)?

---

### Bài 0.4 — Audit Log: thiết kế, AuditLog model, AuditLogDao pattern

**Tài liệu bắt buộc:**
- https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html (ôn PreparedStatement + null handling)
- Đọc lại `BidTransaction.java` (T2) — `AuditLog` dùng cùng pattern không có `updatedAt`

**Câu hỏi hỏi miệng Chủ nhật:**
1. `AuditLog` không có setter và không có `updatedAt` — vì sao audit log phải là immutable? Nếu muốn
   "sửa" 1 sự kiện đã log, hệ thống nên xử lý thế nào thay vì UPDATE record cũ?
2. `AuditActions` là interface constant, không phải enum — trade-off so với dùng enum là gì? Khi nào
   nên chọn interface constant, khi nào nên chọn enum cho trường hợp action code?
3. `AuditLogDao.save()` truyền `ps.setString(2, log.getUserId())` — nếu `getUserId()` trả về `null`
   thì JDBC có ghi NULL vào cột `user_id` không? Cần kiểm tra gì thêm không?
4. `AuditLogService.log()` (Tuần 5) phải **không ném exception ra ngoài** — nếu `auditLogDao.save()`
   ném `SQLException` mà không bắt, điều gì xảy ra với `handleLogin()` đang chờ kết quả?
5. Bảng `audit_logs` không có `updated_at` nhưng `Entity` có field `updatedAt` — constructor DB-load
   truyền `createdAt` cho cả 2 tham số. Tại sao đây là acceptable thay vì thêm cột vào schema?
6. **[Câu hỏi nâng cao]** Tuần 8 gọi `auditLogService.log()` bên trong `try { lock.lock(); ... }`.
   Nên gọi log **bên trong** hay **sau khi giải lock**? Ảnh hưởng đến throughput concurrent bidding
   thế nào? Nếu `save()` mất 10ms, tác động đến session đang chờ lock ra sao?

---

## 👤 ĐĂNG — SocketServerCore, Session & Schema Update

```
Branch: feature/tuan-4-dang-socket-server
Phụ thuộc: ConfigLoader (tuần 1), MigrationRunner + schema.sql (tuần 3) — MỞ RA thêm, không tạo lại
           RequestHandler (tuần 4, Quốc Minh) — tạo stub class để ClientConnectionThread compile được
```

📌 **[Tiêu chí điểm: Kiến trúc Client–Server — 0.5đ]**

### 📝 Mô tả bài tập

`SocketServerCore` là cổng vào duy nhất của server — không có nó, không client nào kết nối được và
mọi chức năng từ Tuần 5 đến 8 đều vô nghĩa. Thread pool 30 luồng fixed đảm bảo server phục vụ nhiều
client đồng thời mà không tạo thread không giới hạn — đây là nền tảng Tuần 7 cần khi 50 client đặt
giá cùng lúc.

`Session` nắm giữ `PrintWriter` của mỗi socket và bảo vệ bằng `synchronized` — quan trọng vì từ Tuần
7, `NotificationBroker` gọi `session.sendMessage()` từ thread khác để push realtime event. Field
`authenticatedUserId` là cầu nối network↔service — Tuần 5 `AuthService` set sau login thành công.

Cập nhật `schema.sql` thêm bảng `audit_logs` là nhiệm vụ nền tuần này để Khoa viết `AuditLogDao`
trên đúng schema, và Tuần 5 tích hợp `AuditLogService` ngay lập tức.

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── ServerApp.java           (đã có T3 — MỞ RA thêm SocketServerCore, không xóa gì)
│   └── network/                 ← MỚI: toàn bộ package này
│       ├── SocketServerCore.java     ← MỚI
│       ├── Session.java              ← MỚI
│       ├── ClientConnectionThread.java ← MỚI
│       └── RequestHandler.java       ← STUB (Quốc Minh replace bằng impl đầy đủ)
├── resources/db/
│   └── schema.sql               (đã có T3 — APPEND thêm bảng audit_logs vào cuối)
└── test/java/com/bidhub/server/network/
    └── SessionTest.java          ← MỚI
```

> [!IMPORTANT]
> Sau khi tạo xong `SocketServerCore`, mở `ServerApp.java` thêm vào cuối `main()`:
> ```java
> SocketServerCore server = new SocketServerCore();
> server.start(ConfigLoader.getInt("server.port"));
> ```
> Commit riêng: `git commit -m "feat: khởi động SocketServerCore trong ServerApp.main()"`

> [!WARNING]
> Cập nhật `schema.sql` bằng cách **APPEND** vào **cuối file** — không xóa 4 bảng cũ.
> `MigrationRunner` split theo `;` và execute từng statement, tự xử lý bảng mới mà không cần
> sửa thêm bất kỳ dòng code nào trong `MigrationRunner.java`.

---

**Append vào cuối `schema.sql`:**

```sql
CREATE TABLE IF NOT EXISTS audit_logs (
  id         TEXT PRIMARY KEY,
  user_id    TEXT,
  action     TEXT NOT NULL,
  details    TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
```

```bash
git commit -m "feat: thêm bảng audit_logs vào schema.sql (bảng thứ 5)"
```

---

### `SocketServerCore.java`

```java
package com.bidhub.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Lõi server TCP — lắng nghe kết nối mới, submit mỗi kết nối vào thread pool.
 *
 * <p>Fixed pool 30: phục vụ tải concurrent Tuần 7 mà không spawn thread không giới hạn.
 * Gọi {@link #start(int)} cuối {@code ServerApp.main()} — blocking cho đến khi {@link #shutdown()}.
 */
public final class SocketServerCore {

  // 📌 [Tiêu chí: Kiến trúc Client–Server — 0.5đ] Fixed pool tránh OOM khi nhiều client đồng thời
  private final ExecutorService threadPool = Executors.newFixedThreadPool(30);
  private ServerSocket serverSocket;
  private volatile boolean running = false; // volatile: shutdown() từ thread khác thấy ngay

  /**
   * Bắt đầu lắng nghe — blocking. Gọi từ main thread sau khi tất cả setup xong.
   *
   * @param port cổng lắng nghe, đọc từ ConfigLoader
   * @throws IOException nếu không bind được cổng
   */
  public void start(int port) throws IOException {
    serverSocket = new ServerSocket(port);
    running = true;
    System.out.println("[SocketServerCore] Đang lắng nghe cổng " + port);

    while (running) {
      try {
        Socket clientSocket = serverSocket.accept();
        Session session = new Session(clientSocket);
        threadPool.submit(new ClientConnectionThread(session));
        System.out.println("[SocketServerCore] Client mới: "
            + clientSocket.getInetAddress() + " | session=" + session.getSessionId());
      } catch (IOException e) {
        if (running) {
          System.err.println("[SocketServerCore] Lỗi accept: " + e.getMessage());
        }
        // !running → ServerSocket đã đóng bởi shutdown() → thoát vòng lặp bình thường
      }
    }
  }

  /** Dừng server — đóng ServerSocket, shutdown pool, chờ tối đa 5 giây. */
  public void shutdown() {
    running = false;
    if (serverSocket != null && !serverSocket.isClosed()) {
      try {
        serverSocket.close();
      } catch (IOException ignored) {}
    }
    threadPool.shutdown();
    try {
      if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
        threadPool.shutdownNow();
      }
    } catch (InterruptedException e) {
      threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }
    System.out.println("[SocketServerCore] Server đã dừng.");
  }

  public boolean isRunning() {
    return running;
  }
}
```

```bash
git commit -m "feat: thêm SocketServerCore với fixed thread pool 30 và vòng lặp accept()"
```

---

### `Session.java`

```java
package com.bidhub.server.network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

/**
 * Đại diện 1 kết nối TCP đang sống. Mỗi client có đúng 1 Session suốt vòng đời kết nối.
 *
 * <p>{@link #sendMessage(String)} synchronized — Tuần 7 NotificationBroker gọi từ thread khác.
 * {@code authenticatedUserId} null khi chưa login, được set bởi AuthService Tuần 5.
 */
public final class Session {

  private final String sessionId;
  private final Socket socket;
  private final PrintWriter out;
  private String authenticatedUserId;

  /**
   * Tạo Session từ socket của ServerSocket.accept().
   *
   * @param socket socket từ {@code ServerSocket.accept()}
   * @throws IOException nếu không lấy được OutputStream
   */
  public Session(Socket socket) throws IOException {
    this.sessionId = UUID.randomUUID().toString();
    this.socket = socket;
    // autoFlush=true: println() gửi ngay, không cần flush() thủ công
    this.out = new PrintWriter(socket.getOutputStream(), true);
  }

  /**
   * Gửi 1 dòng JSON response tới client — thread-safe.
   *
   * @param jsonResponse chuỗi JSON đã serialize
   */
  public synchronized void sendMessage(String jsonResponse) {
    out.println(jsonResponse);
  }

  /** Đóng socket và dọn dẹp. Gọi từ finally của ClientConnectionThread. */
  public void disconnect() {
    try {
      socket.close();
    } catch (IOException ignored) {}
  }

  public boolean isAuthenticated() {
    return authenticatedUserId != null;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getAuthenticatedUserId() {
    return authenticatedUserId;
  }

  public void setAuthenticatedUserId(String userId) {
    this.authenticatedUserId = userId;
  }

  public Socket getSocket() {
    return socket;
  }
}
```

```bash
git commit -m "feat: thêm Session với sendMessage() synchronized, authenticatedUserId"
```

---

### `ClientConnectionThread.java`

```java
package com.bidhub.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Runnable xử lý 1 client: đọc JSON → RequestHandler → sendMessage.
 *
 * <p>Cleanup session trong finally — socket luôn được đóng dù có exception.
 */
public final class ClientConnectionThread implements Runnable {

  private final Session session;
  private final RequestHandler handler;

  public ClientConnectionThread(Session session) {
    this.session = session;
    this.handler = new RequestHandler();
  }

  @Override
  public void run() {
    System.out.println("[ClientThread] Session bắt đầu: " + session.getSessionId());
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(session.getSocket().getInputStream()))) {

      String line;
      // readLine() → null khi client đóng connection (EOF) → thoát vòng lặp sạch
      while ((line = reader.readLine()) != null) {
        String response = handler.handle(line, session);
        session.sendMessage(response);
      }

    } catch (IOException e) {
      // Client ngắt đột ngột (Ctrl+C, kill process...) — không phải lỗi server
      System.out.println("[ClientThread] Client ngắt: " + session.getSessionId());
    } finally {
      session.disconnect();
      System.out.println("[ClientThread] Cleanup xong: " + session.getSessionId());
    }
  }
}
```

### `RequestHandler.java` (stub — Quốc Minh replace)

```java
package com.bidhub.server.network;

/**
 * Stub để ClientConnectionThread compile được. Quốc Minh replace bằng implementation đầy đủ.
 */
public final class RequestHandler {

  public String handle(String jsonLine, Session session) {
    return "{\"status\":\"ERROR\",\"type\":\"UNKNOWN\",\"message\":\"Handler chưa implement\"}";
  }
}
```

```bash
git commit -m "feat: thêm ClientConnectionThread, Session, SocketServerCore, RequestHandler stub"
```

---

### ✅ Test đầu ra — `SessionTest.java`

```java
package com.bidhub.server.network;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

  @Test
  @DisplayName("Session mới → chưa authenticated, sessionId không null")
  void newSession_notAuthenticated() throws Exception {
    try (ServerSocket srv = new ServerSocket(0)) {
      Socket client = new Socket("localhost", srv.getLocalPort());
      Session session = new Session(srv.accept());

      assertFalse(session.isAuthenticated());
      assertNotNull(session.getSessionId());
      assertNull(session.getAuthenticatedUserId());

      client.close();
      session.disconnect();
    }
  }

  @Test
  @DisplayName("setAuthenticatedUserId → isAuthenticated() = true")
  void setUserId_authenticated() throws Exception {
    try (ServerSocket srv = new ServerSocket(0)) {
      Socket client = new Socket("localhost", srv.getLocalPort());
      Session session = new Session(srv.accept());

      session.setAuthenticatedUserId("user-001");

      assertTrue(session.isAuthenticated());
      assertEquals("user-001", session.getAuthenticatedUserId());

      client.close();
      session.disconnect();
    }
  }

  @Test
  @DisplayName("sendMessage() → client đọc được đúng nội dung")
  void sendMessage_clientReceives() throws Exception {
    try (ServerSocket srv = new ServerSocket(0)) {
      Socket client = new Socket("localhost", srv.getLocalPort());
      Session session = new Session(srv.accept());

      session.sendMessage("{\"status\":\"OK\"}");

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(client.getInputStream()));
      assertEquals("{\"status\":\"OK\"}", reader.readLine());

      client.close();
      session.disconnect();
    }
  }

  @Test
  @DisplayName("2 Session → sessionId luôn unique")
  void sessionIds_unique() throws Exception {
    try (ServerSocket srv = new ServerSocket(0)) {
      Socket c1 = new Socket("localhost", srv.getLocalPort());
      Session s1 = new Session(srv.accept());
      Socket c2 = new Socket("localhost", srv.getLocalPort());
      Session s2 = new Session(srv.accept());

      assertNotEquals(s1.getSessionId(), s2.getSessionId());

      c1.close(); s1.disconnect();
      c2.close(); s2.disconnect();
    }
  }
}
```

**Kiểm tra manual:**
```bash
# Terminal 1: Khởi động server
mvn compile -pl bidhub-server -q && mvn exec:java -pl bidhub-server \
  -Dexec.mainClass="com.bidhub.server.ServerApp"
# Output: [MigrationRunner] Schema đã sẵn sàng.  ← 5 bảng
#         [SocketServerCore] Đang lắng nghe cổng 9090

# Terminal 2: Verify 5 bảng tồn tại
sqlite3 data/bidhub.db "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;"
# audit_logs
# auctions
# bid_transactions
# items
# users
```

**❌ FAIL nếu:**
- `telnet localhost 9090` không kết nối → `SocketServerCore.start()` chưa được gọi trong `ServerApp`
- Client ngắt → server crash thay vì log cleanup → `ClientConnectionThread` thiếu try-finally
- `sqlite3` chỉ thấy 4 bảng → `audit_logs` chưa được append vào `schema.sql`

---

## 👤 QUỐC MINH — JSON Protocol & RequestHandler

```
Branch: feature/tuan-4-quocminh-protocol-handler
Phụ thuộc: Session (tuần 4, Đăng), BidHubException + 7 subclass (tuần 2)
           Rebase sau khi Đăng merge Session và ClientConnectionThread
```

📌 **[Tiêu chí điểm: MVC — RequestHandler tầng server (phần 0.5đ) + Kiến trúc Client–Server 0.5đ]**

### 📝 Mô tả bài tập

`MessageRequest`, `MessageResponse`, `MessageMapper` là ngôn ngữ chung của toàn bộ server — mọi handler
từ Tuần 5 đến 8 đều nhận `MessageRequest` và trả `MessageResponse`. Ba class này sống trong
`com.bidhub.server.network` và được dùng xuyên suốt server.

`RequestHandler` là dispatcher chính: nhận raw JSON string và quyết định handler nào chịu trách nhiệm.
Switch-case đơn giản là lựa chọn đúng — không cần Command Pattern, giảng viên hỏi đến đâu giải thích
đến đó. Tuần 5 chỉ cần thêm `case "LOGIN"`, `case "REGISTER"` vào cùng switch — không refactor gì.

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/network/
│   ├── SocketServerCore.java  (đã có T4, Đăng — không sửa)
│   ├── Session.java           (đã có T4, Đăng — không sửa)
│   ├── ClientConnectionThread.java (đã có T4, Đăng — không sửa)
│   ├── MessageRequest.java    ← MỚI
│   ├── MessageResponse.java   ← MỚI
│   ├── MessageMapper.java     ← MỚI
│   └── RequestHandler.java    ← REPLACE stub của Đăng
└── docs/
    └── API_PROTOCOL.md        (đã có T1 — cập nhật thêm ví dụ PING + auth-guard)
```

---

### `MessageRequest.java`

```java
package com.bidhub.server.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * POJO cho request từ client → server.
 *
 * <p>Format: {@code {"type":"PING","token":"uuid-or-null","payload":{}}}
 * {@code @JsonIgnoreProperties} đảm bảo field thêm từ client cũ không gây crash.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageRequest {

  private String type;
  private String token;
  private JsonNode payload;

  /** No-arg constructor bắt buộc cho Jackson. */
  public MessageRequest() {}

  public MessageRequest(String type, String token, JsonNode payload) {
    this.type = type;
    this.token = token;
    this.payload = payload;
  }

  public String getType() {
    return type;
  }

  public String getToken() {
    return token;
  }

  /** Trả về payload JsonNode — kiểm tra null trước khi gọi .get("field") trong handler. */
  public JsonNode getPayload() {
    return payload;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setPayload(JsonNode payload) {
    this.payload = payload;
  }
}
```

---

### `MessageResponse.java`

```java
package com.bidhub.server.network;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * POJO cho response từ server → client.
 *
 * <p>OK: {@code {"status":"OK","type":"PING","payload":{...}}}
 * ERROR: {@code {"status":"ERROR","type":"LOGIN","message":"Sai mật khẩu"}}
 * {@code @JsonInclude.NON_NULL} loại bỏ field null khỏi JSON output.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {

  private String status;
  private String type;
  private Object payload;
  private String message;

  /** No-arg constructor cho Jackson. */
  public MessageResponse() {}

  /**
   * Response thành công với payload bất kỳ.
   *
   * @param type    type tương ứng với request
   * @param payload object được serialize thành JSON
   */
  public static MessageResponse ok(String type, Object payload) {
    MessageResponse r = new MessageResponse();
    r.status = "OK";
    r.type = type;
    r.payload = payload;
    return r;
  }

  /**
   * Response lỗi với message tiếng Việt hiển thị cho client.
   *
   * @param type    type request gây lỗi
   * @param message thông báo lỗi
   */
  public static MessageResponse error(String type, String message) {
    MessageResponse r = new MessageResponse();
    r.status = "ERROR";
    r.type = type;
    r.message = message;
    return r;
  }

  public boolean isOk() {
    return "OK".equals(status);
  }

  public String getStatus() {
    return status;
  }

  public String getType() {
    return type;
  }

  public Object getPayload() {
    return payload;
  }

  public String getMessage() {
    return message;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setPayload(Object payload) {
    this.payload = payload;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
```

---

### `MessageMapper.java`

```java
package com.bidhub.server.network;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility serialize/deserialize JSON cho toàn bộ server protocol.
 *
 * <p>ObjectMapper là thread-safe sau khi cấu hình — khai báo {@code static final},
 * không tạo mới mỗi lần gọi (ObjectMapper tốn ~65ms khởi tạo).
 */
public final class MessageMapper {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MessageMapper() {}

  /**
   * Serialize object thành JSON string. Không ném exception ra ngoài — trả về fallback JSON.
   *
   * @param obj object cần serialize
   * @return chuỗi JSON
   */
  public static String toJson(Object obj) {
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (Exception e) {
      return "{\"status\":\"ERROR\",\"type\":\"SYSTEM\","
          + "\"message\":\"Serialization error: " + e.getMessage() + "\"}";
    }
  }

  /**
   * Deserialize JSON string thành object. Ném exception nếu JSON không hợp lệ.
   *
   * @param json  chuỗi JSON từ socket
   * @param clazz class đích
   * @param <T>   kiểu kết quả
   * @return object đã parse
   * @throws Exception nếu JSON malformed
   */
  public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
    return MAPPER.readValue(json, clazz);
  }

  /** Trả về ObjectMapper gốc — dùng khi cần register module (ví dụ JavaTimeModule Tuần 7). */
  public static ObjectMapper getMapper() {
    return MAPPER;
  }
}
```

```bash
git commit -m "feat: thêm MessageRequest, MessageResponse, MessageMapper"
```

---

### `RequestHandler.java` (replace stub của Đăng)

```java
package com.bidhub.server.network;

import com.bidhub.common.exception.BidHubException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Dispatcher chính: nhận JSON thô → parse → auth-guard → switch type → gọi handler.
 *
 * <p>Switch-case mở rộng từng tuần mà không refactor:
 * Tuần 4: PING · Tuần 5: LOGIN / REGISTER / LOGOUT / CREATE_ITEM / GET_ITEM_LIST ·
 * Tuần 6: PLACE_BID / LIST_AUCTIONS · Tuần 7+: chỉ thêm case.
 */
public final class RequestHandler {

  // 📌 [Tiêu chí: MVC — RequestHandler là tầng điều phối server]
  private static final Set<String> AUTH_REQUIRED = Set.of(
      "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
      "LIST_MY_ITEMS", "PLACE_BID", "GET_AUCTION_DETAIL"
  );

  /**
   * Xử lý 1 request JSON từ client, trả về JSON response string.
   *
   * <p>Không ném exception ra ngoài — mọi lỗi wrap thành error response.
   *
   * @param jsonLine dòng JSON thô từ socket
   * @param session  session của client
   * @return chuỗi JSON response
   */
  public String handle(String jsonLine, Session session) {
    MessageRequest req;
    try {
      req = MessageMapper.fromJson(jsonLine, MessageRequest.class);
    } catch (Exception e) {
      return MessageMapper.toJson(
          MessageResponse.error("UNKNOWN", "JSON không hợp lệ: " + e.getMessage()));
    }

    String type = req.getType() != null ? req.getType().toUpperCase() : "UNKNOWN";

    // 📌 [Tiêu chí: Kiến trúc Client–Server — auth guard]
    if (AUTH_REQUIRED.contains(type) && !session.isAuthenticated()) {
      return MessageMapper.toJson(
          MessageResponse.error(type, "Bạn chưa đăng nhập. Vui lòng LOGIN trước."));
    }

    try {
      return switch (type) {
        case "PING"     -> handlePing(session);
        case "LOGIN"    -> MessageMapper.toJson(
            MessageResponse.error("LOGIN", "Chưa implement — sẽ có ở Tuần 5"));
        case "REGISTER" -> MessageMapper.toJson(
            MessageResponse.error("REGISTER", "Chưa implement — sẽ có ở Tuần 5"));
        default         -> MessageMapper.toJson(
            MessageResponse.error(type, "Lệnh không xác định: " + type));
      };
    } catch (BidHubException e) {
      return MessageMapper.toJson(MessageResponse.error(type, e.getMessage()));
    } catch (Exception e) {
      System.err.println("[RequestHandler] Lỗi xử lý " + type + ": " + e.getMessage());
      return MessageMapper.toJson(MessageResponse.error(type, "Lỗi hệ thống nội bộ."));
    }
  }

  private String handlePing(Session session) {
    Map<String, String> payload = Map.of(
        "message", "pong",
        "serverTime", LocalDateTime.now().toString(),
        "sessionId", session.getSessionId()
    );
    return MessageMapper.toJson(MessageResponse.ok("PING", payload));
  }
}
```

```bash
git commit -m "feat: RequestHandler đầy đủ — PING, auth-guard, malformed JSON safe"
```

---

### ✅ Test đầu ra

**Lệnh bash manual:**
```bash
# Gửi PING
echo '{"type":"PING","payload":{}}' | nc -q1 localhost 9090
# Output: {"status":"OK","type":"PING","payload":{"message":"pong","serverTime":"...","sessionId":"..."}}

# Gửi lệnh không tồn tại
echo '{"type":"GHOST","payload":{}}' | nc -q1 localhost 9090
# Output: {"status":"ERROR","type":"GHOST","message":"Lệnh không xác định: GHOST"}

# Gửi JSON lỗi
echo 'not json' | nc -q1 localhost 9090
# Output: {"status":"ERROR","type":"UNKNOWN","message":"JSON không hợp lệ:..."}
```

**❌ FAIL nếu:**
- `handle("broken json", session)` ném exception → `ClientConnectionThread` crash → thiếu try-catch trong `handle()`
- `AUTH_REQUIRED` route với session chưa auth trả `"OK"` → auth-guard bị skip
- `type = null` trong JSON → `NullPointerException` trong switch → thiếu null-check trên `getType()`

---

## 👤 CÔNG MINH — ServerGateway + NetworkTask + ClientSession

```
Branch: feature/tuan-4-congminh-server-gateway
Phụ thuộc: MessageRequest, MessageResponse, MessageMapper (tuần 4, Quốc Minh) — rebase sau QM merge
           ViewRouter, Views, BidHubApp (tuần 3) — không tạo lại
           javafx.concurrent.Task (có sẵn trong pom)
```

📌 **[Tiêu chí điểm: Singleton — ServerGateway + ClientSession (phần 1.0đ Design Patterns)]**

### 📝 Mô tả bài tập

`ServerGateway` là cổng giao tiếp duy nhất phía client — toàn bộ Controller từ Tuần 5 trở đi chỉ
gọi server qua đây. Singleton đảm bảo chỉ có 1 TCP connection, tránh hàng chục kết nối song song gây
lẫn response. `synchronized sendRequest()` đảm bảo thứ tự request/response trên cùng 1 TCP stream.

`NetworkTask<T>` giải quyết vấn đề cốt lõi của JavaFX: không bao giờ block FX thread. Pattern này
tái sử dụng nguyên vẹn từ Tuần 5 đến 8 — mỗi Controller chỉ cần thay đổi lambda bên trong NetworkTask.

### 📁 Cấu trúc file

```
bidhub-client/src/
├── main/java/com/bidhub/client/
│   ├── BidHubApp.java        (đã có T3 — MỞ RA thêm connect ServerGateway)
│   └── network/              ← MỚI: toàn bộ package này
│       ├── ServerGateway.java  ← MỚI: Singleton TCP client
│       ├── NetworkTask.java    ← MỚI: Task<T> wrapper
│       └── ClientSession.java  ← MỚI: Singleton lưu token
└── resources/
    └── client.properties     (đã có — thêm server.host + server.port nếu chưa có)
```

> [!IMPORTANT]
> Mở `client.properties`, thêm 2 dòng nếu chưa có:
> ```properties
> server.host=localhost
> server.port=9090
> ```

---

### `ClientSession.java`

```java
package com.bidhub.client.network;

/**
 * Singleton lưu trạng thái đăng nhập phía client.
 *
 * <p>Set bởi LoginController sau khi nhận response OK từ server.
 * Mọi Controller kiểm tra {@link #isLoggedIn()} trước khi thực hiện thao tác cần auth.
 */
public final class ClientSession {

  // 📌 [Tiêu chí: Singleton Pattern — 1.0đ Design Patterns]
  private static volatile ClientSession instance;

  private String token;
  private String currentUserId;
  private String currentUsername;
  private String currentRole; // "BIDDER" / "SELLER" / "ADMIN" — String, độc lập với enum server

  private ClientSession() {}

  /** Trả về instance duy nhất (thread-safe, double-checked locking). */
  public static ClientSession getInstance() {
    if (instance == null) {
      synchronized (ClientSession.class) {
        if (instance == null) {
          instance = new ClientSession();
        }
      }
    }
    return instance;
  }

  /**
   * Lưu thông tin sau khi đăng nhập thành công.
   *
   * @param token    token UUID từ server
   * @param userId   id người dùng
   * @param username tên đăng nhập
   * @param role     "BIDDER" / "SELLER" / "ADMIN"
   */
  public void login(String token, String userId, String username, String role) {
    this.token = token;
    this.currentUserId = userId;
    this.currentUsername = username;
    this.currentRole = role;
  }

  /** Reset toàn bộ. Gọi khi logout hoặc nhận lỗi auth từ server. */
  public void logout() {
    this.token = null;
    this.currentUserId = null;
    this.currentUsername = null;
    this.currentRole = null;
  }

  public boolean isLoggedIn() {
    return token != null;
  }

  public String getToken() {
    return token;
  }

  public String getCurrentUserId() {
    return currentUserId;
  }

  public String getCurrentUsername() {
    return currentUsername;
  }

  public String getCurrentRole() {
    return currentRole;
  }
}
```

---

### `NetworkTask.java`

```java
package com.bidhub.client.network;

import java.util.concurrent.Callable;
import javafx.concurrent.Task;

/**
 * Wrapper {@link Task} cho mọi network call — đảm bảo không block FX thread.
 *
 * <p>Cách dùng trong Controller:
 * <pre>
 *   NetworkTask&lt;MessageResponse&gt; task = new NetworkTask&lt;&gt;(() -&gt;
 *       ServerGateway.getInstance().sendRequest(req));
 *   task.setOnSucceeded(e -&gt; Platform.runLater(() -&gt; handleSuccess(task.getValue())));
 *   task.setOnFailed(e -&gt; Platform.runLater(() -&gt; showError(task.getException())));
 *   new Thread(task).start();
 * </pre>
 *
 * @param <T> kiểu kết quả trả về
 */
public final class NetworkTask<T> extends Task<T> {

  private final Callable<T> callable;

  /** @param callable logic cần chạy ngoài FX thread */
  public NetworkTask(Callable<T> callable) {
    this.callable = callable;
  }

  @Override
  protected T call() throws Exception {
    return callable.call();
  }
}
```

---

### `ServerGateway.java`

```java
package com.bidhub.client.network;

import com.bidhub.server.network.MessageMapper;
import com.bidhub.server.network.MessageRequest;
import com.bidhub.server.network.MessageResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

/**
 * Cổng giao tiếp duy nhất từ client đến server. Singleton đảm bảo chỉ có 1 TCP connection.
 *
 * <p>Mọi network call phải bọc trong {@link NetworkTask} để không block FX thread.
 * {@code sendRequest()} dùng {@code synchronized} để đảm bảo thứ tự request/response.
 */
public final class ServerGateway {

  // 📌 [Tiêu chí: Singleton Pattern — 1.0đ Design Patterns]
  private static volatile ServerGateway instance;

  private Socket socket;
  private PrintWriter writer;
  private BufferedReader reader;
  private String serverHost;
  private int serverPort;

  private ServerGateway() {
    loadConfig();
  }

  /** Trả về instance duy nhất (thread-safe). */
  public static ServerGateway getInstance() {
    if (instance == null) {
      synchronized (ServerGateway.class) {
        if (instance == null) {
          instance = new ServerGateway();
        }
      }
    }
    return instance;
  }

  private void loadConfig() {
    try (var is = getClass().getResourceAsStream("/client.properties")) {
      Properties props = new Properties();
      if (is != null) props.load(is);
      serverHost = props.getProperty("server.host", "localhost");
      serverPort = Integer.parseInt(props.getProperty("server.port", "9090"));
    } catch (Exception e) {
      serverHost = "localhost";
      serverPort = 9090;
    }
  }

  /**
   * Mở kết nối TCP đến server. Gọi từ BidHubApp.start() qua NetworkTask.
   *
   * @param host hostname server
   * @param port cổng server
   * @throws IOException nếu không kết nối được
   */
  public void connect(String host, int port) throws IOException {
    socket = new Socket(host, port);
    writer = new PrintWriter(socket.getOutputStream(), true);
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    System.out.println("[ServerGateway] Kết nối: " + host + ":" + port);
  }

  /**
   * Gửi request và chờ response — blocking, PHẢI gọi từ background thread (NetworkTask).
   *
   * @param request request đã cấu trúc
   * @return response từ server
   * @throws IOException nếu mất kết nối trong khi giao tiếp
   */
  public synchronized MessageResponse sendRequest(MessageRequest request) throws IOException {
    if (!isConnected()) {
      throw new IOException("Chưa kết nối server. Gọi connect() trước.");
    }
    writer.println(MessageMapper.toJson(request));

    String responseLine = reader.readLine();
    if (responseLine == null) {
      throw new IOException("Server đóng kết nối bất ngờ.");
    }
    try {
      return MessageMapper.fromJson(responseLine, MessageResponse.class);
    } catch (Exception e) {
      throw new IOException("Response không parse được: " + responseLine, e);
    }
  }

  /** Đóng kết nối và giải phóng tài nguyên. */
  public void disconnect() {
    if (socket != null && !socket.isClosed()) {
      try {
        socket.close();
      } catch (IOException ignored) {}
    }
    socket = null;
    writer = null;
    reader = null;
  }

  public boolean isConnected() {
    return socket != null && socket.isConnected() && !socket.isClosed();
  }

  public String getServerHost() {
    return serverHost;
  }

  public int getServerPort() {
    return serverPort;
  }
}
```

```bash
git commit -m "feat: thêm ServerGateway Singleton, NetworkTask<T>, ClientSession Singleton"
```

> [!TIP]
> Mở `BidHubApp.java`, trong `start()` sau `ViewRouter.getInstance().initialize(stage)`, thêm:
> ```java
> NetworkTask<Void> connectTask = new NetworkTask<>(() -> {
>   ServerGateway gw = ServerGateway.getInstance();
>   gw.connect(gw.getServerHost(), gw.getServerPort());
>   return null;
> });
> connectTask.setOnFailed(e -> Platform.runLater(() -> {
>   Alert alert = new Alert(Alert.AlertType.ERROR,
>       "Không kết nối được Server tại "
>       + ServerGateway.getInstance().getServerHost() + ":"
>       + ServerGateway.getInstance().getServerPort()
>       + "\nKiểm tra server đang chạy rồi thử lại.");
>   alert.showAndWait();
>   Platform.exit();
> }));
> new Thread(connectTask).start();
> ```
> Commit: `git commit -m "feat: connect ServerGateway khi BidHubApp khởi động"`

---

### ✅ Test đầu ra — `ClientSessionTest.java`

```java
package com.bidhub.client.network;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientSessionTest {

  @BeforeEach
  void reset() {
    ClientSession.getInstance().logout();
  }

  @Test
  @DisplayName("getInstance() gọi 2 lần → cùng instance (Singleton)")
  void singleton_sameInstance() {
    assertSame(ClientSession.getInstance(), ClientSession.getInstance());
  }

  @Test
  @DisplayName("Khởi tạo → chưa đăng nhập, token null")
  void initial_notLoggedIn() {
    assertFalse(ClientSession.getInstance().isLoggedIn());
    assertNull(ClientSession.getInstance().getToken());
  }

  @Test
  @DisplayName("login() → isLoggedIn() = true, token và username đúng")
  void login_setsState() {
    ClientSession.getInstance().login("tok-123", "uid-1", "alice", "BIDDER");

    assertTrue(ClientSession.getInstance().isLoggedIn());
    assertEquals("tok-123", ClientSession.getInstance().getToken());
    assertEquals("alice", ClientSession.getInstance().getCurrentUsername());
    assertEquals("BIDDER", ClientSession.getInstance().getCurrentRole());
  }

  @Test
  @DisplayName("logout() → isLoggedIn() = false, tất cả field null")
  void logout_clearsAll() {
    ClientSession.getInstance().login("tok-abc", "uid-2", "bob", "SELLER");
    ClientSession.getInstance().logout();

    assertFalse(ClientSession.getInstance().isLoggedIn());
    assertNull(ClientSession.getInstance().getToken());
    assertNull(ClientSession.getInstance().getCurrentUserId());
  }
}
```

**❌ FAIL nếu:**
- `ServerGateway.getInstance()` trả 2 instance khác nhau → double-checked locking sai
- `sendRequest()` không `synchronized` → 2 NetworkTask đồng thời đọc lẫn response → data corruption
- `connect()` thất bại → crash app thay vì hiện Alert → thiếu `setOnFailed` trong `BidHubApp`

---

## 👤 KHOA — AuditLog Model + AuditLogDao + Protocol Tests

```
Branch: feature/tuan-4-khoa-auditlog-dao-tests
Phụ thuộc: Entity (tuần 2, bidhub-common), DbConnectionProvider (tuần 3, Đăng)
           schema.sql đã có bảng audit_logs (tuần 4, Đăng — rebase sau khi Đăng merge)
           RequestHandler, MessageMapper, Session (tuần 4, Quốc Minh + Đăng — rebase sau merge)
```

📌 **[Tiêu chí điểm: Thiết kế lớp — AuditLog class thứ 6 (phần 0.5đ) + Unit Test JUnit (0.5đ)]**

### 📝 Mô tả bài tập

`AuditLog` là class domain thứ 6 trong hệ thống, trực tiếp phục vụ tiêu chí "Thiết kế lớp" của barem.
Nó là record bất biến — không có setter, không có `updatedAt` riêng — vì audit log về bản chất là lịch
sử không được sửa. Nếu thiếu class này và `AuditLogDao`, Tuần 5 không có `AuditLogService` để inject
vào `handleLogin`, hệ thống mất toàn bộ khả năng truy vết và Quốc Minh không thể log event.

`AuditLogDao` theo đúng pattern từ Tuần 3: 2 constructor, `acquireConnection()` / `releaseConnection()`,
mọi query dùng `PreparedStatement`. Khoa rebase từ branch Đăng sau khi Đăng merge bảng `audit_logs`
vào `schema.sql` — không tự thêm bảng riêng.

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── model/
│   │   ├── AuditLog.java       ← MỚI
│   │   └── AuditActions.java   ← MỚI: interface hằng số action codes
│   └── dao/
│       └── AuditLogDao.java    ← MỚI
└── test/java/com/bidhub/server/
    ├── dao/
    │   └── AuditLogDaoTest.java    ← MỚI
    └── network/
        ├── RequestHandlerTest.java ← MỚI (mở rộng từ skeleton QM viết)
        └── MessageMapperTest.java  ← MỚI
```

---

### `AuditActions.java`

```java
package com.bidhub.server.model;

/**
 * Hằng số mã hành động audit — tập trung một chỗ, tránh hardcode string rải rác trong handlers.
 *
 * <p>Interface constant thay vì enum: action code là String thô để lưu thẳng vào DB
 * và dùng trong WHERE clause SQL mà không cần valueOf() conversion.
 */
public interface AuditActions {

  String USER_LOGIN      = "USER_LOGIN";
  String USER_LOGOUT     = "USER_LOGOUT";
  String USER_REGISTER   = "USER_REGISTER";
  String USER_LOCKED     = "USER_LOCKED";
  String USER_UNLOCKED   = "USER_UNLOCKED";
  String PLACE_BID       = "PLACE_BID";
  String AUCTION_CLOSED  = "AUCTION_CLOSED";
  String AUCTION_EXTENDED = "AUCTION_EXTENDED";
  String ITEM_CREATED    = "ITEM_CREATED";
  String ITEM_DELETED    = "ITEM_DELETED";
}
```

---

### `AuditLog.java`

```java
package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;

/**
 * Bản ghi audit — lịch sử hành động người dùng và hệ thống.
 *
 * <p>Immutable sau khi tạo: không có setter. Không có {@code updatedAt} riêng.
 * Constructor DB-load dùng {@code createdAt} cho cả 2 tham số của Entity.
 */
public final class AuditLog extends Entity {

  private final String userId;  // null nếu là system action
  private final String action;  // mã từ AuditActions
  private final String details; // JSON string context, tối thiểu là "{}"

  /**
   * Tạo bản ghi audit mới — tự sinh id và createdAt qua Entity.
   *
   * @param userId  id người thực hiện, null nếu system action
   * @param action  mã từ {@link AuditActions}
   * @param details JSON string context
   */
  public AuditLog(String userId, String action, String details) {
    super(); // Entity() → tự sinh UUID + createdAt = now
    this.userId = userId;
    this.action = action != null ? action : "";
    this.details = details != null ? details : "{}";
  }

  /**
   * Constructor load từ DB — giữ nguyên id và createdAt gốc.
   *
   * <p>Bảng audit_logs không có updated_at → truyền createdAt cho cả 2 tham số Entity.
   *
   * @param id        id từ DB
   * @param createdAt thời điểm tạo từ DB
   * @param userId    có thể null (system action)
   * @param action    mã hành động
   * @param details   JSON string
   */
  public AuditLog(String id, LocalDateTime createdAt,
      String userId, String action, String details) {
    super(id, createdAt, createdAt); // dùng createdAt cho updatedAt — audit log không thay đổi
    this.userId = userId;
    this.action = action != null ? action : "";
    this.details = details != null ? details : "{}";
  }

  public String getUserId() {
    return userId;
  }

  public String getAction() {
    return action;
  }

  public String getDetails() {
    return details;
  }
}
```

```bash
git commit -m "feat: thêm AuditLog model (immutable) và AuditActions interface"
```

---

### `AuditLogDao.java`

```java
package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.AuditLog;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD cho bảng {@code audit_logs}.
 *
 * <p>Theo đúng pattern DAO Tuần 3: 2 constructor, acquireConnection/releaseConnection,
 * PreparedStatement cho mọi query.
 */
public class AuditLogDao {

  private final Connection injectedConn;

  /** Constructor production. */
  public AuditLogDao() {
    this.injectedConn = null;
  }

  /** Constructor test — inject in-memory connection. */
  public AuditLogDao(Connection conn) {
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
   * Lưu bản ghi audit mới.
   *
   * @param log bản ghi cần lưu
   */
  public void save(AuditLog log) {
    String sql = """
        INSERT INTO audit_logs (id, user_id, action, details, created_at)
        VALUES (?, ?, ?, ?, ?)
        """;
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, log.getId());
        ps.setString(2, log.getUserId()); // null-safe: SQLite lưu NULL khi setString(null)
        ps.setString(3, log.getAction());
        ps.setString(4, log.getDetails());
        ps.setString(5, log.getCreatedAt().toString());
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuditLogDao.save thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  /**
   * Trả về tất cả bản ghi, mới nhất trước.
   *
   * @return danh sách AuditLog, có thể rỗng
   */
  public List<AuditLog> findAll() {
    return queryList("SELECT * FROM audit_logs ORDER BY created_at DESC", null);
  }

  /**
   * Tìm tất cả log của 1 user.
   *
   * @param userId id người dùng
   * @return danh sách log, mới nhất trước
   */
  public List<AuditLog> findByUserId(String userId) {
    return queryList(
        "SELECT * FROM audit_logs WHERE user_id = ? ORDER BY created_at DESC", userId);
  }

  /**
   * Tìm log theo mã action.
   *
   * @param action mã từ {@link com.bidhub.server.model.AuditActions}
   * @return danh sách log matching, mới nhất trước
   */
  public List<AuditLog> findByAction(String action) {
    return queryList(
        "SELECT * FROM audit_logs WHERE action = ? ORDER BY created_at DESC", action);
  }

  /**
   * Trả về N bản ghi mới nhất.
   *
   * @param limit số lượng tối đa cần lấy
   * @return danh sách tối đa {@code limit} bản ghi
   */
  public List<AuditLog> findRecent(int limit) {
    String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ?";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, limit);
        ResultSet rs = ps.executeQuery();
        List<AuditLog> result = new ArrayList<>();
        while (rs.next()) result.add(mapRow(rs));
        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuditLogDao.findRecent thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  private List<AuditLog> queryList(String sql, String param) {
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        if (param != null) ps.setString(1, param);
        ResultSet rs = ps.executeQuery();
        List<AuditLog> result = new ArrayList<>();
        while (rs.next()) result.add(mapRow(rs));
        return result;
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuditLogDao query thất bại: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

  private AuditLog mapRow(ResultSet rs) throws SQLException {
    String id = rs.getString("id");
    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
    String userId = rs.getString("user_id"); // null khi system action — getString trả null, không crash
    String action = rs.getString("action");
    String details = rs.getString("details");
    return new AuditLog(id, createdAt, userId, action, details);
  }
}
```

```bash
git commit -m "feat: thêm AuditLogDao với save/findAll/findByUserId/findByAction/findRecent"
```

---

### ✅ Test đầu ra

```java
package com.bidhub.server.dao;

import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.AuditLog;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogDaoTest {

  private Connection conn;
  private AuditLogDao dao;

  @BeforeEach
  void setup() throws SQLException {
    conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    try (Statement s = conn.createStatement()) {
      s.execute("""
          CREATE TABLE audit_logs (
            id TEXT PRIMARY KEY, user_id TEXT, action TEXT NOT NULL,
            details TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL)
          """);
    }
    dao = new AuditLogDao(conn);
  }

  @AfterEach
  void teardown() throws SQLException {
    conn.close();
  }

  @Test
  @DisplayName("save → findAll trả về đúng bản ghi vừa lưu")
  void save_findAll_returnsRecord() {
    dao.save(new AuditLog("user-1", AuditActions.USER_LOGIN, "{}"));
    List<AuditLog> all = dao.findAll();

    assertEquals(1, all.size());
    assertEquals(AuditActions.USER_LOGIN, all.get(0).getAction());
    assertEquals("user-1", all.get(0).getUserId());
  }

  @Test
  @DisplayName("findByUserId chỉ trả về log của đúng user, không lẫn user khác")
  void findByUserId_filtersCorrectly() {
    dao.save(new AuditLog("user-A", AuditActions.USER_LOGIN, "{}"));
    dao.save(new AuditLog("user-B", AuditActions.USER_LOGIN, "{}"));
    dao.save(new AuditLog("user-A", AuditActions.PLACE_BID, "{}"));

    List<AuditLog> resultA = dao.findByUserId("user-A");
    assertEquals(2, resultA.size());
    assertTrue(resultA.stream().allMatch(l -> "user-A".equals(l.getUserId())));
  }

  @Test
  @DisplayName("findByAction lọc đúng action, không trả action khác")
  void findByAction_onlyMatchingAction() {
    dao.save(new AuditLog("u1", AuditActions.USER_LOGIN, "{}"));
    dao.save(new AuditLog("u2", AuditActions.PLACE_BID, "{}"));
    dao.save(new AuditLog("u3", AuditActions.USER_LOGIN, "{}"));

    List<AuditLog> logins = dao.findByAction(AuditActions.USER_LOGIN);
    assertEquals(2, logins.size());
    assertTrue(logins.stream().allMatch(l -> AuditActions.USER_LOGIN.equals(l.getAction())));
  }

  @Test
  @DisplayName("findRecent(3) với 10 bản ghi → tối đa 3 kết quả")
  void findRecent_limitsResults() {
    for (int i = 0; i < 10; i++) {
      dao.save(new AuditLog("user-" + i, AuditActions.PLACE_BID, "{}"));
    }
    List<AuditLog> recent = dao.findRecent(3);
    assertTrue(recent.size() <= 3);
  }

  @Test
  @DisplayName("save userId=null (system action) → không crash, bản ghi lưu được")
  void save_nullUserId_succeeds() {
    dao.save(new AuditLog(null, AuditActions.AUCTION_CLOSED, "{\"auctionId\":\"a-1\"}"));

    List<AuditLog> all = dao.findAll();
    assertEquals(1, all.size());
    assertNull(all.get(0).getUserId());
    assertEquals("{\"auctionId\":\"a-1\"}", all.get(0).getDetails());
  }
}
```

```java
package com.bidhub.server.network;

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
```

```java
package com.bidhub.server.network;

import org.junit.jupiter.api.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MessageMapperTest {

  @Test
  @DisplayName("MessageResponse.ok serialize → có status=OK và type đúng")
  void ok_serializes() throws Exception {
    MessageResponse resp = MessageResponse.ok("PING", Map.of("message", "pong"));
    String json = MessageMapper.toJson(resp);
    assertTrue(json.contains("\"status\":\"OK\""));
    assertTrue(json.contains("\"type\":\"PING\""));
  }

  @Test
  @DisplayName("MessageResponse.error → status=ERROR, field payload không xuất hiện")
  void error_noPayloadField() {
    MessageResponse resp = MessageResponse.error("LOGIN", "Sai mật khẩu");
    String json = MessageMapper.toJson(resp);
    assertTrue(json.contains("\"status\":\"ERROR\""));
    assertTrue(json.contains("Sai mật khẩu"));
    assertFalse(json.contains("\"payload\"")); // @JsonInclude.NON_NULL
  }

  @Test
  @DisplayName("fromJson thêm field lạ → @JsonIgnoreProperties không crash")
  void fromJson_extraFields_ignored() {
    String json = "{\"type\":\"PING\",\"payload\":{},\"clientVersion\":\"2.0\"}";
    assertDoesNotThrow(() -> MessageMapper.fromJson(json, MessageRequest.class));
  }

  @Test
  @DisplayName("fromJson chuỗi rỗng → ném exception (không im lặng)")
  void fromJson_empty_throwsException() {
    assertThrows(Exception.class,
        () -> MessageMapper.fromJson("", MessageRequest.class));
  }

  @Test
  @DisplayName("fromJson JSON thiếu field → type null, payload null, không crash")
  void fromJson_minimal_doesNotCrash() throws Exception {
    MessageRequest req = MessageMapper.fromJson("{}", MessageRequest.class);
    assertNull(req.getType());
    assertNull(req.getPayload());
  }
}
```

```bash
git commit -m "test: thêm AuditLogDaoTest, RequestHandlerTest, MessageMapperTest — 15 cases mới"
```

**❌ FAIL nếu:**
- `AuditLogDao.save()` với `userId = null` ném `NullPointerException` → `ps.setString(2, null)` JDBC
  thực ra chấp nhận null → lỗi là do code tự thêm null-check sai cách
- `findByUserId("user-A")` trả về cả log của `user-B` → WHERE clause sai hoặc param binding sai
- `findRecent(3)` với 10 bản ghi trả về 10 → thiếu LIMIT trong SQL
- `MessageMapperTest.error_noPayloadField` fail vì `"payload":null` xuất hiện → thiếu `@JsonInclude.NON_NULL`
