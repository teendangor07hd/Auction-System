# 🔧 BẢN VÁ TUẦN 4 — Tổng hợp toàn bộ lỗi sau khi hoàn thành WEEK4_TASKS.md

> [!IMPORTANT]
> Tài liệu này tổng hợp **6 lỗi** phát hiện sau khi cả nhóm hoàn thành code theo WEEK4_TASKS.md.
> Áp dụng tuần tự từ Lỗi 1 → Lỗi 6. Lỗi 1 đã được nhóm phát hiện độc lập — ghi lại ở đây cho đầy đủ.
> Toàn bộ bản vá phải hoàn thành **trước khi bắt đầu Tuần 5** vì Tuần 5 phụ thuộc trực tiếp vào
> `ServerGateway`, `NetworkTask`, `RequestHandler` và `AuditLogDao`.

---

## Lỗi 1 — `MessageRequest` / `MessageResponse` / `MessageMapper` sai module (đã biết)

**Mức độ:** 🔴 Nghiêm trọng — phá vỡ kiến trúc Client–Server, `bidhub-client` buộc phụ thuộc `bidhub-server`

**Triệu chứng:** `ServerGateway.java` import `com.bidhub.server.network.MessageMapper/Request/Response`.
`bidhub-client/pom.xml` phải thêm `bidhub-server` làm dependency để compile — vi phạm nguyên tắc
"client không được biết về server internals".

**Nguyên nhân gốc:** Ba class protocol được đặt trong `bidhub-server` thay vì `bidhub-common`.

### Các bước vá

**Bước 1.1** — Tạo package mới trong `bidhub-common`:
```
bidhub-common/src/main/java/com/bidhub/common/network/
```

**Bước 1.2** — Di chuyển 3 file (git mv để giữ lịch sử):
```bash
git mv bidhub-server/src/main/java/com/bidhub/server/network/MessageRequest.java \
       bidhub-common/src/main/java/com/bidhub/common/network/MessageRequest.java

git mv bidhub-server/src/main/java/com/bidhub/server/network/MessageResponse.java \
       bidhub-common/src/main/java/com/bidhub/common/network/MessageResponse.java

git mv bidhub-server/src/main/java/com/bidhub/server/network/MessageMapper.java \
       bidhub-common/src/main/java/com/bidhub/common/network/MessageMapper.java
```

**Bước 1.3** — Sửa dòng `package` trong cả 3 file vừa di chuyển:
```java
// Sửa từ:
package com.bidhub.server.network;

// Thành:
package com.bidhub.common.network;
```

**Bước 1.4** — Thêm Jackson vào `bidhub-common/pom.xml`:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

**Bước 1.5** — Cập nhật import trong `bidhub-server` (tìm và thay toàn bộ):

| File | Import cũ | Import mới |
|---|---|---|
| `RequestHandler.java` | `import com.bidhub.server.network.MessageMapper;` | `import com.bidhub.common.network.MessageMapper;` |
| `RequestHandler.java` | `import com.bidhub.server.network.MessageRequest;` | `import com.bidhub.common.network.MessageRequest;` |
| `RequestHandler.java` | `import com.bidhub.server.network.MessageResponse;` | `import com.bidhub.common.network.MessageResponse;` |
| `RequestHandlerTest.java` | (tương tự) | (tương tự) |
| `MessageMapperTest.java` | (tương tự) | (tương tự) |

**Bước 1.6** — Cập nhật import trong `bidhub-client`:

Trong `ServerGateway.java`, sửa 3 dòng import:
```java
// Sửa từ:
import com.bidhub.server.network.MessageMapper;
import com.bidhub.server.network.MessageRequest;
import com.bidhub.server.network.MessageResponse;

// Thành:
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
```

**Bước 1.7** — Xóa dependency `bidhub-server` khỏi `bidhub-client/pom.xml` nếu đã thêm vào.

**Bước 1.8** — Xóa package rỗng trên server:
```bash
rm -rf bidhub-server/src/main/java/com/bidhub/server/network/
# Chỉ xóa thư mục này khi đã chuyển hết 3 file đi và không còn class nào trong đó.
# Thực tế RequestHandler, Session, SocketServerCore, ClientConnectionThread vẫn còn
# → KHÔNG xóa thư mục. Chỉ xóa 3 file đã di chuyển đi.
```

> [!WARNING]
> Package `com.bidhub.server.network` vẫn tồn tại vì `Session.java`, `SocketServerCore.java`,
> `ClientConnectionThread.java`, `RequestHandler.java` nằm trong đó. Chỉ 3 file protocol mới
> được di chuyển sang common. Package server.network vẫn giữ nguyên tên.

**Bước 1.9** — Verify build:
```bash
mvn clean compile
mvn test
```

```bash
git commit -m "refactor: di chuyển MessageRequest/Response/Mapper sang bidhub-common.network"
```

---

## Lỗi 2 — `NetworkTask` Javadoc ví dụ dùng `Platform.runLater()` sai chỗ

**Mức độ:** 🟡 Trung bình — code chạy được nhưng Javadoc sai → Công Minh và Tuần 5 sẽ viết code thừa
không cần thiết, gây hiểu nhầm về threading model của JavaFX

**Nguyên nhân gốc:** Javadoc trong `NetworkTask.java` viết:
```java
task.setOnSucceeded(e -> Platform.runLater(() -> handleSuccess(task.getValue())));
```
Điều này sai — `Task.setOnSucceeded` handler đã chạy trên **FX thread** theo JavaFX spec. Bọc thêm
`Platform.runLater()` bên trong là dư thừa và tạo 1 level trễ thêm không cần thiết. Các Controller
từ Tuần 5 trở đi nếu làm theo Javadoc sẽ viết code thừa mà không hiểu tại sao.

**Tham chiếu:** https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html —
"The onSucceeded event handler is called on the JavaFX Application Thread."

### Bước vá

Mở `NetworkTask.java` trong `bidhub-client/src/main/java/com/bidhub/client/network/`, sửa Javadoc:

```java
/**
 * Wrapper {@link Task} cho mọi network call — đảm bảo không block FX thread.
 *
 * <p>Cách dùng đúng trong Controller:
 * <pre>
 *   NetworkTask&lt;MessageResponse&gt; task = new NetworkTask&lt;&gt;(() -&gt;
 *       ServerGateway.getInstance().sendRequest(req));
 *
 *   // setOnSucceeded chạy trên FX thread — KHÔNG cần Platform.runLater() thêm bên trong
 *   task.setOnSucceeded(e -&gt; handleSuccess(task.getValue()));
 *
 *   // setOnFailed cũng chạy trên FX thread
 *   task.setOnFailed(e -&gt; showError(task.getException().getMessage()));
 *
 *   new Thread(task).start();
 * </pre>
 *
 * <p>Chỉ dùng {@code Platform.runLater()} khi cập nhật UI từ một background thread thông thường
 * (không phải từ bên trong handler của Task). Ví dụ: EventListenerThread (Tuần 7) cần
 * {@code Platform.runLater()} vì nó là luồng nền tự quản, không phải Task.
 *
 * @param <T> kiểu kết quả trả về
 */
public final class NetworkTask<T> extends Task<T> {
  // ... (phần còn lại giữ nguyên)
}
```

```bash
git commit -m "fix: sửa Javadoc NetworkTask — setOnSucceeded đã trên FX thread, không cần Platform.runLater()"
```

---

## Lỗi 3 — `ServerGateway.connect()` không đóng kết nối cũ, gây resource leak

**Mức độ:** 🟡 Trung bình — không crash ngay nhưng gây socket leak khi kết nối bị gián đoạn và app
thử reconnect (Tuần 8 sẽ cần reconnect logic)

**Nguyên nhân gốc:** `connect(String host, int port)` hiện tại viết thẳng vào `socket`, `writer`,
`reader` mà không kiểm tra xem có kết nối cũ đang mở không:

```java
// Code hiện tại — SAI
public void connect(String host, int port) throws IOException {
  socket = new Socket(host, port);  // ← overwrite socket cũ mà không đóng
  writer = new PrintWriter(socket.getOutputStream(), true);
  reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
}
```

Nếu `connect()` được gọi lần 2 (ví dụ: mạng mất kết nối, app thử reconnect), socket cũ bị
overwrite mà không được `close()` → file descriptor bị leak. Trên Linux, mỗi process có giới hạn
file descriptor (~1024 mặc định) — sau nhiều lần reconnect sẽ ném `Too many open files`.

### Bước vá

Trong `ServerGateway.java`, sửa method `connect()`:

```java
/**
 * Mở kết nối TCP đến server. Đóng kết nối cũ nếu đang tồn tại trước khi mở mới.
 *
 * @param host hostname server
 * @param port cổng server
 * @throws IOException nếu không kết nối được
 */
public synchronized void connect(String host, int port) throws IOException {
  // Đóng kết nối cũ nếu còn sống — tránh resource leak khi reconnect
  if (socket != null && !socket.isClosed()) {
    try {
      socket.close();
    } catch (IOException ignored) {}
  }
  socket = new Socket(host, port);
  writer = new PrintWriter(socket.getOutputStream(), true);
  reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  System.out.println("[ServerGateway] Kết nối: " + host + ":" + port);
}
```

> [!NOTE]
> Thêm `synchronized` vào `connect()` để đồng bộ với `sendRequest()` (cũng `synchronized`).
> Nếu 2 NetworkTask cùng gọi `connect()` đồng thời (edge case cực hiếm), chỉ 1 kết nối được tạo.

```bash
git commit -m "fix: ServerGateway.connect() đóng kết nối cũ trước khi mở mới, thêm synchronized"
```

---

## Lỗi 4 — `AuditLogDao.findRecent(-1)` trả về toàn bộ bảng thay vì ném lỗi

**Mức độ:** 🟡 Trung bình — hành vi ngầm gây khó debug; nếu caller truyền nhầm `-1`, log console
bị dump toàn bộ mà không có cảnh báo

**Nguyên nhân gốc:** Trong SQLite, `LIMIT -1` tương đương "không giới hạn" — trả về **toàn bộ bảng**.
`findRecent(-1)` hiện không validate, âm thầm bỏ qua limit và trả về tất cả rows.

```java
// Code hiện tại — thiếu validation
public List<AuditLog> findRecent(int limit) {
  String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ?";
  // ... ps.setInt(1, limit); ← -1 hoặc 0 không bị reject
}
```

### Bước vá

Trong `AuditLogDao.java`, thêm validation đầu method `findRecent()`:

```java
/**
 * Trả về N bản ghi mới nhất.
 *
 * @param limit số lượng tối đa cần lấy — phải > 0
 * @return danh sách tối đa {@code limit} bản ghi
 * @throws IllegalArgumentException nếu {@code limit} ≤ 0
 */
public List<AuditLog> findRecent(int limit) {
  if (limit <= 0) {
    throw new IllegalArgumentException("limit phải > 0, nhận được: " + limit);
  }
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
```

Thêm test case vào `AuditLogDaoTest.java`:

```java
@Test
@DisplayName("findRecent(0) → ném IllegalArgumentException")
void findRecent_zeroLimit_throwsException() {
  assertThrows(IllegalArgumentException.class, () -> dao.findRecent(0));
}

@Test
@DisplayName("findRecent(-1) → ném IllegalArgumentException (không trả về toàn bảng)")
void findRecent_negativeLimit_throwsException() {
  // Đảm bảo LIMIT -1 không âm thầm trả về tất cả rows
  for (int i = 0; i < 5; i++) {
    dao.save(new AuditLog("u" + i, AuditActions.PLACE_BID, "{}"));
  }
  assertThrows(IllegalArgumentException.class, () -> dao.findRecent(-1));
}
```

```bash
git commit -m "fix: AuditLogDao.findRecent() validate limit > 0, thêm test case edge case"
```

---

## Lỗi 5 — `BidHubApp` connect code thiếu `setOnSucceeded` và không có UI guard

**Mức độ:** 🟠 Cao — gây `IOException: Chưa kết nối server` khi user nhấn login ngay lập tức trước
khi `NetworkTask<Void>` connect hoàn tất; xảy ra đặc biệt khi server khởi động chậm (> 500ms)

**Nguyên nhân gốc:** TIP block trong WEEK4 chỉ có `setOnFailed` mà thiếu `setOnSucceeded`. Không có
`setOnSucceeded` → sau khi connect xong, app không làm gì để signal "đã sẵn sàng". Không có UI guard
→ Button "Đăng nhập" trong `LoginController` không biết connection đã xong chưa.

```java
// Code TIP block T4 — THIẾU setOnSucceeded
NetworkTask<Void> connectTask = new NetworkTask<>(() -> { ... gw.connect(...); return null; });
connectTask.setOnFailed(e -> Platform.runLater(() -> { ... Platform.exit(); }));
// ← không có setOnSucceeded!
new Thread(connectTask).start();
// ← LoginView load ngay sau đây, user có thể click login trước khi connect xong
```

### Bước vá

Mở `BidHubApp.java`, thay toàn bộ đoạn connect bằng:

```java
package com.bidhub.client;

import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class BidHubApp extends Application {

  @Override
  public void start(Stage stage) {
    ViewRouter.getInstance().initialize(stage);

    // Hiện màn hình "Đang kết nối..." trong khi connect chạy nền
    ProgressIndicator spinner = new ProgressIndicator(-1);
    spinner.setPrefSize(50, 50);
    Label lblStatus = new Label("Đang kết nối tới server...");
    lblStatus.setTextAlignment(TextAlignment.CENTER);
    VBox loadingPane = new VBox(16, spinner, lblStatus);
    loadingPane.setAlignment(javafx.geometry.Pos.CENTER);
    loadingPane.setPrefSize(300, 200);
    stage.setScene(new Scene(loadingPane));
    stage.setTitle("BidHub");
    stage.show();

    ServerGateway gw = ServerGateway.getInstance();
    NetworkTask<Void> connectTask = new NetworkTask<>(() -> {
      gw.connect(gw.getServerHost(), gw.getServerPort());
      return null;
    });

    // setOnSucceeded chạy trên FX thread — không cần Platform.runLater()
    connectTask.setOnSucceeded(e -> {
      // Kết nối thành công → chuyển sang LoginView
      ViewRouter.getInstance().navigateTo(Views.LOGIN);
    });

    connectTask.setOnFailed(e -> {
      // Kết nối thất bại → hiện lỗi rõ ràng + thoát
      javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
          javafx.scene.control.Alert.AlertType.ERROR,
          "Không kết nối được Server tại "
              + gw.getServerHost() + ":" + gw.getServerPort()
              + "\n\nKiểm tra:\n"
              + "  1. Server đang chạy chưa (mvn exec:java -pl bidhub-server)\n"
              + "  2. Đúng host/port trong client.properties\n"
              + "  3. Tường lửa không chặn cổng " + gw.getServerPort());
      alert.setTitle("Lỗi kết nối");
      alert.showAndWait();
      Platform.exit();
    });

    new Thread(connectTask).start();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
```

> [!NOTE]
> Màn hình loading "Đang kết nối..." là UX bắt buộc để tránh race condition. Trước khi `setOnSucceeded`
> chạy, không có màn hình login nào được hiển thị → user không thể click gì → không có race condition.

```bash
git commit -m "fix: BidHubApp hiện loading screen khi connect, chuyển LoginView sau setOnSucceeded"
```

---

## Lỗi 6 — `RequestHandler` khai báo `final` nhưng T5 cần thêm DAO injection constructor

**Mức độ:** 🟡 Trung bình — không crash hiện tại nhưng `RequestHandlerTest` ở T4 tạo `handler = new RequestHandler()` và không thể inject DAO, dẫn đến test phụ thuộc vào `DbConnectionProvider` thật → test T5 sẽ fail nếu không có DB thật ở classpath

**Nguyên nhân gốc:** `RequestHandler` được khai báo `final` với chỉ 1 constructor không có tham số.
Khi Tuần 5 `RequestHandler` cần `UserDao`, `ItemDao`, `AuditLogService`, test không thể inject
in-memory DAO — buộc dùng DB thật hoặc DbConnectionProvider → test trở thành integration test thay
vì unit test.

Điều này chưa gây lỗi ở T4 vì `RequestHandler` T4 không dùng DAO. Nhưng cần sửa **ngay** trước
Tuần 5 để Khoa có thể viết `AuthHandlerTest` với in-memory SQLite theo đúng pattern đã thiết lập.

### Bước vá

Mở `RequestHandler.java`, thêm 2 field và constructor inject (giữ constructor no-arg cho production):

```java
public final class RequestHandler {

  private static final Set<String> AUTH_REQUIRED = Set.of(
      "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
      "LIST_MY_ITEMS", "PLACE_BID", "GET_AUCTION_DETAIL"
  );

  // ← THÊM: field DAO (null ở T4 — sẽ được gán thực sự ở T5)
  // Giữ package-private để test inject được mà không cần reflection
  final Object injectedUserDao;   // type Object tạm — T5 sẽ đổi thành UserDao
  final Object injectedItemDao;   // type Object tạm — T5 sẽ đổi thành ItemDao

  /** Constructor production — T4 và T5 đều dùng. */
  public RequestHandler() {
    this.injectedUserDao = null;
    this.injectedItemDao = null;
  }

  /**
   * Constructor inject — dùng trong test để truyền in-memory DAO.
   * T5 sẽ bổ sung đầy đủ tham số khi cần.
   *
   * @param injectedUserDao DAO inject từ test (Object để T4 compile không cần UserDao)
   * @param injectedItemDao DAO inject từ test
   */
  RequestHandler(Object injectedUserDao, Object injectedItemDao) {
    this.injectedUserDao = injectedUserDao;
    this.injectedItemDao = injectedItemDao;
  }

  // ... phần còn lại giữ nguyên
}
```

> [!TIP]
> Khi Tuần 5 viết `handleLogin()`, thay `Object injectedUserDao` bằng `UserDao userDao` thực sự
> và thêm `AuditLogService auditLogService`. Constructor inject cũng được mở rộng tương ứng.
> Cách này tránh phải refactor lại test infrastructure sau T5.

```bash
git commit -m "fix: thêm inject constructor vào RequestHandler để test T5 inject in-memory DAO"
```

---

## Tổng kết bản vá

| # | Lỗi | Mức độ | File cần sửa | Ảnh hưởng nếu không vá |
|---|-----|--------|--------------|------------------------|
| 1 | MessageRequest/Response/Mapper sai module | 🔴 Nghiêm trọng | `bidhub-common/pom.xml`, 3 file di chuyển, `RequestHandler`, `ServerGateway` | `bidhub-client` không compile nếu bỏ dependency `bidhub-server` |
| 2 | Javadoc `NetworkTask` misleading `Platform.runLater()` | 🟡 Trung bình | `NetworkTask.java` | Controller T5–8 viết code thừa, hiểu sai threading model |
| 3 | `ServerGateway.connect()` không đóng kết nối cũ | 🟡 Trung bình | `ServerGateway.java` | Socket leak khi reconnect, crash sau nhiều lần mất mạng |
| 4 | `AuditLogDao.findRecent(-1)` trả về tất cả rows | 🟡 Trung bình | `AuditLogDao.java` + `AuditLogDaoTest.java` | Debug khó, silent bug khi caller truyền nhầm tham số |
| 5 | `BidHubApp` thiếu `setOnSucceeded`, không có UI guard | 🟠 Cao | `BidHubApp.java` | `IOException` ngẫu nhiên khi user click login trước khi connect xong |
| 6 | `RequestHandler` thiếu inject constructor cho test T5 | 🟡 Trung bình | `RequestHandler.java` | Test T5 phải dùng DB thật, không thể unit test isolated |

### Thứ tự commit gợi ý

```bash
# Theo thứ tự ưu tiên — Lỗi 1 và 5 phải làm trước khi merge vào develop
git commit -m "refactor: di chuyển MessageRequest/Response/Mapper sang bidhub-common.network"
git commit -m "fix: BidHubApp hiện loading screen khi connect, chuyển LoginView sau setOnSucceeded"
git commit -m "fix: ServerGateway.connect() đóng kết nối cũ trước khi mở mới, thêm synchronized"
git commit -m "fix: AuditLogDao.findRecent() validate limit > 0, thêm test case edge case"
git commit -m "fix: thêm inject constructor vào RequestHandler để test T5 inject in-memory DAO"
git commit -m "fix: sửa Javadoc NetworkTask — setOnSucceeded đã trên FX thread, không cần Platform.runLater()"
```

### Kiểm tra sau khi vá

```bash
# Đảm bảo build sạch toàn bộ 3 module
mvn clean compile

# Đảm bảo test không có regression
mvn test

# Kiểm tra client KHÔNG import bidhub-server
grep -r "bidhub.server" bidhub-client/src/
# → Output phải rỗng hoàn toàn

# Kiểm tra common/network có đủ 3 file
ls bidhub-common/src/main/java/com/bidhub/common/network/
# → MessageMapper.java  MessageRequest.java  MessageResponse.java
```

> [!IMPORTANT]
> Sau khi áp dụng toàn bộ bản vá, chạy lại toàn bộ test suite (`mvn test`) và đảm bảo **không có
> test nào bị regression** so với trước khi vá. Nếu `RequestHandlerTest` hoặc `MessageMapperTest`
> fail sau khi di chuyển package, kiểm tra lại import trong từng test file.
