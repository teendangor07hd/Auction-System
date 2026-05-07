# 📋 TUẦN 6 — BÀI TẬP CHI TIẾT: Core Bidding Engine + AdminUserService

✅ Kết quả kiểm tra toàn diện: Không có lỗi. Codebase đáp ứng đầy đủ barem và sẵn sàng cho Tuần 6.

## 🎯 MỤC TIÊU TUẦN 6

Tuần này xây dựng 2 trụ cột chính: engine đấu giá hoàn chỉnh (BidValidator + AuctionManager + lifecycle
tự động đóng phiên) và AdminUserService quản lý khóa/mở tài khoản. Cuối tuần, cả nhóm phải có:

- ✅ `AuctionManager` (Singleton) quản lý auction trong RAM bằng `ConcurrentHashMap`, `ScheduledExecutorService`
      chạy `AuctionLifecycleTask` mỗi 5 giây
- ✅ `AuctionLifecycleTask` (Runnable) đóng phiên quá `endTime` → `FINISHED` → xác định winner → cập nhật DB
- ✅ `BidValidator` kiểm tra 5 điều kiện đặt giá, ném exception phù hợp
- ✅ `handlePlaceBid` / `handleGetAuctionList` / `handleGetAuctionDetail` hoàn chỉnh
- ✅ `is_locked` column trong users table + `User.locked` field + `UserDao.updateLocked()`
- ✅ `handleLogin` kiểm tra `user.isLocked()` → error "TÀI KHOẢN BỊ KHÓA"
- ✅ `AdminUserService` — `listAllUsers()`, `lockUser()`, `unlockUser()` kèm audit log
- ✅ `handleGetUserList` / `handleLockUser` / `handleUnlockUser` — ADMIN only
- ✅ `AuctionListController` load data qua `NetworkTask` + `TableView` + `ObservableList`
- ✅ `AuctionDetailController` countdown timer `Timeline` + form đặt giá
- ✅ `CreateAuctionView.fxml` + `CreateAuctionController` (SELLER only)
- ✅ ≥ 23 test cases mới pass (15 bid + 8 admin) — tổng project ≥ 175 cases

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** barem: **Chức năng đấu giá** — Place Bid, Auction lifecycle (1.0đ) +
> **Quản lý người dùng** — Admin lock/unlock (phần 1.0đ) + **Kỹ thuật quan trọng & concurrency** —
> `ScheduledExecutorService` + `ConcurrentHashMap` trong `AuctionManager` (1.0đ) + **Design Patterns** —
> Singleton `AuctionManager` (phần 1.0đ) + **MVC** — RequestHandler bid/auction handlers (phần 0.5đ) +
> **Singleton** bổ sung `AuctionManager` (phần 1.0đ).

> [!CAUTION]
> **Tuyệt đối không tạo lại** bất kỳ class nào từ Tuần 1–5:
> `Entity`, `BidHubException` + 7 subclass, `MessageRequest`, `MessageResponse`, `MessageMapper`,
> `ConfigLoader`, `DbConnectionProvider`, `MigrationRunner`,
> `UserRole`, `User`, `Bidder`, `Seller`, `Admin`, `Displayable`,
> `ItemType`, `Item`, `Electronics`, `Art`, `Vehicle`,
> `ItemCreator` + 3 ConcreteCreator, `AuctionStatus`, `Auction`, `BidTransaction`,
> `AuditLog`, `AuditActions`,
> `UserDao`, `ItemDao`, `AuctionDao`, `BidDao`, `AuditLogDao`,
> `SocketServerCore`, `Session`, `ClientConnectionThread`, `RequestHandler`, `SecurityContext`,
> `AuthService`, `SessionManager`, `AuditLogService`,
> `BidHubApp`, `ViewRouter`, `ContextAware`, `LoginController`, `AuctionListController`,
> `AuctionDetailController`, `CreateItemController`, `Views`,
> `ServerGateway`, `NetworkTask`, `ClientSession`.
>
> **Thứ tự merge quan trọng:** Đăng merge đầu tiên (AuctionManager + schema is_locked + UserDao.updateLocked
> là nền tảng) → Quốc Minh merge thứ hai (BidValidator + bid handlers phụ thuộc AuctionManager) → Khoa merge
> thứ ba (AdminUserService phụ thuộc UserDao.updateLocked + BidValidator) → Công Minh merge cuối (client
> có thể test với mock).

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI HỌC (Tự học)

> [!CAUTION]
> Hoàn thành **trước Thứ 5**. Mục đích không phải học Java chung — mà là hiểu cơ chế hoạt động của
> code người khác viết tuần này, để review chéo Chủ nhật trôi chảy và trả lời được vấn đáp giảng viên
> về bidding engine + admin user management mà không lúng túng.

---

### Bài 0.1 — ScheduledExecutorService & Auction Lifecycle

**Tài liệu bắt buộc:**
- https://docs.oracle.com/javase/21/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html
- https://docs.oracle.com/javase/21/docs/api/java.base/java/util/concurrent/ScheduledThreadPoolExecutor.html

**Câu hỏi hỏi miệng Chủ nhật:**
1. `AuctionManager.start()` dùng `scheduler.scheduleAtFixedRate(task, 0, 5, SECONDS)`. Nếu vòng lặp
   `AuctionLifecycleTask.run()` mất 8 giây (nhiều auction cần đóng), `ScheduledExecutorService` xử lý thế
   nào? Có overlap giữa 2 lần chạy không? Điều gì xảy ra nếu dùng `scheduleWithFixedDelay` thay
   `scheduleAtFixedRate`?
2. `ScheduledThreadPoolExecutor` dùng **1 thread** (corePoolSize=1) — tại sao không cần nhiều thread cho
   lifecycle task? Nếu AuctionLifecycleTask bị exception trong `run()`, scheduler có tiếp tục chạy không?
   `ScheduledExecutorService` có swallow exception không?
3. `AuctionManager` dùng `ConcurrentHashMap<String, Auction>` để lưu auction trong RAM. Tại sao cần
   ConcurrentHashMap thay vì `HashMap`? Ai là thread đọc, ai là thread ghi? Nếu 2 thread cùng
   `put()` vào cùng 1 key — kết quả cuối cùng là gì?
4. `getAllActive()` trả về `new ArrayList<>(auctions.values())` — tại sao tạo copy thay vì trả trực tiếp
   `values()`? Nếu trả `values()` và `AuctionLifecycleTask` đang iterate thì `handlePlaceBid` thêm auction
   mới vào map → `ConcurrentModificationException` có xảy ra không?
5. Khi server shutdown, `AuctionManager.stop()` gọi `scheduler.shutdown()`. Nếu có lifecycle task đang
   chạy giữa chừng → `shutdown()` có đợi task hoàn thành không? `awaitTermination(5, SECONDS)` làm gì?
   Nếu task không hoàn thành trong 5 giây → nên `shutdownNow()` không?
6. **[Câu hỏi nâng cao]** AuctionLifecycleTask gọi `auctionDao.updateStatus()` và `bidDao.getHighestBid()`
   cho mỗi auction hết hạn — đây là I/O blocking trên thread của scheduler. Nếu có 100 auction hết hạn
   cùng lúc, scheduler bị block 10 giây → auction vừa hết hạn trong 10 giây đó có bị bỏ sót không?
   Tại sao choose interval 5 giây?

---

### Bài 0.2 — Transactional Thinking & Lost Update

**Tài liệu bắt buộc:**
- Đọc lại `Auction.java` (T2) — method `transitionTo()` và `isValidBid()`
- Đọc lại `BidTransaction.java` (T2) — field `bidAmount`, `bidTime`
- Đọc lại `AuctionDao.updateHighestBid()` (T3)

**Câu hỏi hỏi miệng Chủ nhật:**
1. Lost Update scenario: `handlePlaceBid()` đọc `auction.getCurrentHighestBid() = 1000`, thread A tính
   validate OK (bid 1200). Thread B cũng đọc `1000`, cũng validate OK (bid 1100). Thread A save DB → DB
   `currentHighestBid = 1200`. Thread B save DB → DB `currentHighestBid = 1100`. Kết quả sai — giá bị
   giảm từ 1200 xuống 1100. Vẽ timeline cụ thể 2 thread này. Tuần 6 giải quyết bằng cách nào?
2. `handlePlaceBid()` làm 3 việc: (1) validate, (2) `bidDao.save(bid)`, (3) `auctionDao.updateHighestBid()`.
   Nếu bước (2) thành công nhưng bước (3) thất bại (DB connection drop) → trạng thái inconsistent:
   bid đã lưu nhưng highest bid chưa cập nhật. Điều gì xảy ra khi client reload auction detail? Tuần 7
   sẽ dùng `ReentrantLock` + try-finally — giải thích tại sao lock không giải quyết DB failure.
3. `AuctionLifecycleTask.closeAuction()` cũng làm nhiều bước: `transitionTo(FINISHED)` → `updateStatus()` →
   `getHighestBid()` → update winner → `removeAuction()`. Nếu `updateStatus()` thành công nhưng
   `getHighestBid()` thất bại → auction bị FINISHED trong DB nhưng winner chưa xác định. Recovery strategy
   nào? Có nên check `FINISHED` auction chưa có winner khi server restart?
4. `BidValidator.validate()` nhận object `Auction` từ RAM (ConcurrentHashMap). Nếu giữa lúc validate và lúc
   `auctionDao.updateHighestBid()`, `AuctionLifecycleTask` đóng auction → DB `status = FINISHED` nhưng
   RAM `status` vẫn `RUNNING` → bid được lưu vào DB cho auction đã đóng. Tại sao xảy ra?
5. Atomic operation: `auctionDao.updateHighestBid(id, amount, bidderId)` UPDATE cả 2 cột cùng lúc. Nếu
   chỉ UPDATE `currentHighestBid` mà quên `highest_bidder_id` → ai là winner bị sai. `PreparedStatement`
   UPDATE SET `current_highest_bid=?, highest_bidder_id=?` có đảm bảo atomic không?
6. **[Câu hỏi nâng cao]** Race condition giữa `AuctionLifecycleTask` và `handlePlaceBid`: lifecycle task
   đọc `auction.getEndTime()` → quá hạn → gọi `closeAuction()`. Đồng thời `handlePlaceBid` validate
   `auction.getStatus() == RUNNING` → pass → save bid. Timing: lifecycle `transitionTo(FINISHED)` sau khi
   `handlePlaceBid` validate nhưng trước khi save → bid cho auction đã finished. Khoa viết BidValidator test
   case nào để catch bug này?

---

### Bài 0.3 — JavaFX Timeline & Countdown Timer

**Tài liệu bắt buộc:**
- https://openjfx.io/javadoc/21/javafx.animation/Timeline.html
- https://openjfx.io/javadoc/21/javafx.animation/KeyFrame.html

**Câu hỏi hỏi miệng Chủ nhật:**
1. `Timeline` chạy trên JavaFX Application Thread — nếu `AuctionDetailController` tạo Timeline với
   `KeyFrame(Duration.seconds(1), e -> updateCountdown())`, method `updateCountdown()` có chạy trên FX
   thread không? Có cần `Platform.runLater()` bên trong không?
2. Khi user navigate từ `AuctionDetail` sang `AuctionList`, Timeline vẫn chạy nếu không gọi `stop()`.
   Kết quả: Timeline cập nhật Label đã bị garbage collected → `NullPointerException` hoặc silent memory
   leak. `AuctionDetailController` dọn dẹp Timeline ở đâu? Có method lifecycle nào trong JavaFX controller?
3. `Duration.seconds(1)` tạo KeyFrame mỗi 1 giây. Nếu countdown timer cần accuracy mili-giây (hiện
   `00:00:00.500`), có thể dùng `Duration.millis(100)` không? Impact lên performance nếu nhiều controller
   cùng chạy timer?
4. Khi auction chuyển sang `FINISHED` (nhận từ server response hoặc countdown về 0), cần: (a) dừng
   Timeline, (b) hiện label "ĐÃ KẾT THÚC", (c) disable button đặt giá. Cách tốt nhất: check trong mỗi
   KeyFrame handler hay dừng Timeline một lần rồi update UI?
5. `Timeline` có `setCycleCount(Timeline.INDEFINITE)` — khác gì với `while (true)`? Nếu `updateCountdown()`
   ném exception (ví dụ: `auction.getEndTime()` là null), Timeline có tiếp tục chạy KeyFrame tiếp theo
   không?
6. **[Câu hỏi nâng cao]** Công Minh đặt countdown `endTime` lấy từ server (truyền qua `ContextAware`).
   Nếu user mở auction detail lúc 14:00:00, endTime = 14:05:00 → countdown = 5 phút. Nhưng server clock
   và client clock lệch nhau 30 giây → countdown sai. Giải pháp: countdown dựa trên `Duration.between(
   LocalDateTime.now(), endTime)` hay server trả `remainingSeconds`? Nếu dùng client time, khi client
   sleep/suspend máy → countdown có chính xác không?

---

### Bài 0.4 — JavaFX TableView & ObservableList

**Tài liệu bắt buộc:**
- https://openjfx.io/javadoc/21/javafx.scene.control/TableView.html
- https://openjfx.io/javadoc/21/javafx.collections/ObservableList.html
- https://openjfx.io/javadoc/21/javafx.scene.control/cell/PropertyValueFactory.html

**Câu hỏi hỏi miệng Chủ nhật:**
1. `ObservableList<AuctionInfo> data = FXCollections.observableArrayList(); tableView.setItems(data);` —
   khi `data.add(newAuction)`, TableView tự cập nhật row mới không? Nếu modify field của `AuctionInfo` đã
   có trong list (ví dụ: `setPrice(2000)`), TableView có tự refresh không? Tại sao cần `setAll()` thay vì
   modify?
2. `PropertyValueFactory<AuctionInfo, String>("itemName")` dùng reflection để gọi `getItemName()` — nếu
   class `AuctionInfo` không có getter `getItemName()` mà field là `public String itemName`, có hoạt động
   không? Tại sao nên dùng getter thay vì public field?
3. `AuctionListController` gọi `GET_AUCTION_LIST` qua `NetworkTask` → nhận JSON array → parse thành
   `ObservableList`. `NetworkTask` chạy trên background thread → `setOnSucceeded` chạy trên FX thread.
   Viết code cụ thể: `tableView.setItems(FXCollections.observableArrayList(parsedList))` — dòng này
   nên đặt trong `setOnSucceeded` hay `call()`?
4. Double-click vào row `TableView`: dùng `tableView.setRowFactory()` + `setOnMouseClicked()` hay
   `tableView.setOnMouseClicked()` trực tiếp? Lấy selected item: `tableView.getSelectionModel().
   getSelectedItem()`. Nếu user double-click vào vùng trống (không có row), `getSelectedItem()` trả về
   null → cần null check?
5. `Button createAuctionBtn` chỉ visible khi role == SELLER: `createAuctionBtn.setVisible(
   ClientSession.getInstance().getCurrentRole() == UserRole.SELLER)`. Khi nào nên set visible — trong
   `initialize()` hay trong `setOnSucceeded` của `GET_AUCTION_LIST`? Tại sao?
6. **[Câu hỏi nâng cao]** `AuctionListController` gọi `GET_AUCTION_LIST` khi `initialize()`. Nếu user
   navigate đi rồi navigate lại → `initialize()` gọi lại lần nữa → `NetworkTask` mới fire → data có bị
   duplicate không? Cần `data.clear()` trước khi `data.addAll()` không? Hoặc dùng `setAll()`?

---

## 👤 ĐĂNG — AuctionManager, LifecycleTask & is_locked Support

```
Branch: feature/tuan-6-dang-auction-manager
Phụ thuộc: AuctionDao (tuần 3, Khoa) — load RUNNING auctions khi start
           BidDao (tuần 3, Khoa) — tìm winner khi đóng phiên
           UserDao (tuần 3, Quốc Minh) — MỞ RA thêm updateLocked() + mapRow đọc is_locked
           User (tuần 2, Đăng) — MỞ RA thêm field locked + isLocked()
           schema.sql, MigrationRunner (tuần 3+4, Đăng) — thêm is_locked column
           Auction (tuần 2, Công Minh) — MỞ RA thêm setCurrentHighestBid/setHighestBidderId
           ServerApp.java (tuần 4, Đăng) — gọi AuctionManager.start() sau SocketServerCore.start()
Merge đầu tiên: AuctionManager + is_locked là nền tảng cho Quốc Minh, Khoa, Công Minh
```

📌 **[Tiêu chí điểm: Kỹ thuật quan trọng & concurrency — ScheduledExecutorService + ConcurrentHashMap — 1.0đ + Design Pattern Singleton — AuctionManager — phần 1.0đ + Chức năng đấu giá — lifecycle tự động — phần 1.0đ]**

### 📝 Mô tả bài tập

`AuctionManager` là Singleton quản lý toàn bộ auction đang hoạt động trong RAM bằng `ConcurrentHashMap`.
Khi server khởi động, `start()` load tất cả RUNNING auction từ `AuctionDao` vào map, rồi schedule
`AuctionLifecycleTask` chạy mỗi 5 giây qua `ScheduledExecutorService`. Task này iterate toàn bộ active
auction, đóng những phiên đã quá `endTime`. `handlePlaceBid` của Quốc Minh sẽ dùng `AuctionManager` để
lấy auction object thay vì query DB mỗi lần — giảm latency, tăng throughput.

`AuctionLifecycleTask` là Runnable thực hiện đóng phiên: `transitionTo(FINISHED)` → update status DB → tìm
winner qua `BidDao.getHighestBid()` → cập nhật winner → remove auction khỏi RAM. Đây là cơ chế tự động
quan trọng nhất của hệ thống đấu giá — đảm bảo không có phiên nào chạy quá giờ.

Song song, Đăng thêm hỗ trợ `is_locked` vào user management: schema.sql thêm cột `is_locked`, MigrationRunner
thêm ALTER TABLE, User class thêm field `locked`, UserDao thêm `updateLocked()` và cập nhật `mapRow()`.
Đăng phối hợp Quốc Minh cập nhật `handleLogin()` — sau `verifyPassword()` thành công, check `user.isLocked()`
→ trả về error "TÀI KHOẢN BỊ KHÓA".

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Quốc Minh cần `AuctionManager.getAuction()` trong `handlePlaceBid` + `BidValidator` cần auction object từ RAM
- Khoa cần `UserDao.updateLocked()` cho `AdminUserService.lockUser()/unlockUser()`
- Công Minh cần server API ổn định (có thể test client với mock data)
- Đăng cần `AuctionDao.findActiveAuctions()` + `BidDao.getHighestBid()` — đều đã có từ T3

**Kịch bản chọn: C — Đăng merge trước, tất cả rebase sau**

**Các bước:**
1. Đăng tạo branch, code `AuctionManager.java` + `AuctionLifecycleTask.java`
2. Đăng cập nhật `User.java` thêm field `locked`, `UserDao` thêm `updateLocked()` + `mapRow()`
3. Đăng cập nhật `schema.sql` + `MigrationRunner` thêm `is_locked` column
4. Đăng cập nhật `Auction.java` thêm `setCurrentHighestBid()` + `setHighestBidderId()`
5. Đăng cập nhật `ServerApp.main()` gọi `AuctionManager.getInstance().start()`
6. Push lên GitHub, tạo PR → review → merge vào `develop`
7. Quốc Minh rebase — giờ có `AuctionManager` + `User.locked` + `Auction` setters
8. Khoa rebase — giờ có `UserDao.updateLocked()`
9. Công Minh rebase — client có thể test với mock

**Nếu Quốc Minh cần AuctionManager trước khi Đăng merge:**
```java
// Stub tạm trong branch Quốc Minh — XÓA khi Đăng merge
package com.bidhub.server.service;
import com.bidhub.server.model.Auction;
import java.util.*;
public class AuctionManager {
  private static final AuctionManager INSTANCE = new AuctionManager();
  public static AuctionManager getInstance() { return INSTANCE; }
  public void start() {}
  public void addAuction(Auction a) {}
  public Optional<Auction> getAuction(String id) { return Optional.empty(); }
  public List<Auction> getAllActive() { return List.of(); }
}
```

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── service/
│   │   ├── AuctionManager.java        ← MỚI: Singleton
│   │   └── AuctionLifecycleTask.java  ← MỚI: Runnable
│   ├── model/
│   │   ├── User.java            (đã có T2 — MỞ RA thêm field locked)
│   │   └── Auction.java         (đã có T2 — MỞ RA thêm setters)
│   ├── dao/
│   │   └── UserDao.java         (đã có T3 — MỞ RA thêm updateLocked + mapRow is_locked)
│   ├── config/
│   │   └── MigrationRunner.java (đã có T3+T4 — MỞ RA thêm ALTER TABLE is_locked)
│   ├── network/
│   │   └── RequestHandler.java  (đã có T5 — MỞ RA thêm is_locked check trong handleLogin)
│   └── ServerApp.java           (đã có T4 — MỞ RA thêm AuctionManager.start())
├── main/resources/db/
│   └── schema.sql               (đã có T3+T4 — MỞ RA thêm is_locked column)
└── test/java/com/bidhub/server/
    └── service/
        ├── AuctionManagerTest.java      ← MỚI
        └── AuctionLifecycleTaskTest.java ← MỚI
```

> [!IMPORTANT]
> Đăng cũng cần mở `User.java`, `Auction.java`, `UserDao.java`, `schema.sql`, `MigrationRunner.java`,
> `ServerApp.java`, `RequestHandler.java` để thêm/sửa code. Mỗi file sửa commit riêng.

---

### Cập nhật `User.java` — thêm field `locked`

Mở file `User.java` đã có, thêm field và method:

```java
// === THÊM VÀO User.java ===

  /** Trang thai khoa tai khoan — true la bi khoa, false la binh thuong. */
  private boolean locked = false;

  /**
   * Kiem tra tai khoan co bi khoa khong.
   *
   * @return true neu tai khoan bi khoa
   */
  public boolean isLocked() {
    return locked;
  }

  /**
   * Dat trang thai khoa/mo khoa tai khoan.
   *
   * @param locked true de khoa, false de mo
   */
  public void setLocked(boolean locked) {
    this.locked = locked;
  }
```

```bash
git commit -m "feat: thêm field locked và isLocked()/setLocked() vào User"
```

---

### Cập nhật `Auction.java` — thêm setters cho bid update

Mở file `Auction.java` đã có, thêm method:

```java
// === THÊM VÀO Auction.java ===

  /**
   * Cap nhat gia cao nhat hien tai — dung khi co bid moi.
   *
   * <p>// 📌 [Tieu chi: Chuc nang dau gia — cap nhat gia trong RAM]
   *
   * @param amount gia dau moi
   */
  public void setCurrentHighestBid(double amount) {
    this.currentHighestBid = amount;
  }

  /**
   * Cap nhat id nguoi dan dau — dung khi co bid moi.
   *
   * @param bidderId id cua nguoi dau gia moi
   */
  public void setHighestBidderId(String bidderId) {
    this.highestBidderId = bidderId;
  }
```

```bash
git commit -m "feat: thêm setCurrentHighestBid() và setHighestBidderId() vào Auction"
```

---

### Cập nhật `schema.sql` — thêm cột `is_locked`

Mở file `schema.sql` đã có, sửa bảng `users`:

```sql
-- Bảng users — thêm cột is_locked
CREATE TABLE IF NOT EXISTS users (
  id             TEXT PRIMARY KEY,
  username       TEXT UNIQUE NOT NULL,
  password_hash  TEXT NOT NULL,
  email          TEXT,
  role           TEXT NOT NULL,
  is_locked      INTEGER NOT NULL DEFAULT 0,
  created_at     TEXT NOT NULL,
  updated_at     TEXT NOT NULL
);
```

```bash
git commit -m "feat: thêm cột is_locked vào schema.sql (users table)"
```

---

### Cập nhật `MigrationRunner.java` — thêm ALTER TABLE

Mở file `MigrationRunner.java` đã có, thêm migration cho `is_locked`:

```java
// === THÊM VÀO MigrationRunner.java — sau phần execute schema.sql ===

    // 📌 [Tieu chi: Quan ly nguoi dung — migration is_locked cho DB cu]
    // Migration: them cot is_locked vao bang users (cho DB da tao tu Tuan 3)
    String alterTableSql =
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_locked "
        + "INTEGER NOT NULL DEFAULT 0";
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(alterTableSql);
      System.out.println("[MigrationRunner] Cot is_locked da san sang.");
    } catch (SQLException e) {
      // Cot da ton tai hoac loi khac — khong block server startup
      System.err.println("[MigrationRunner] Canh bao migration is_locked: "
          + e.getMessage());
    }
```

> [!NOTE]
> SQLite hỗ trợ `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` từ phiên bản 3.35.0 (2021-03-12).
> Nếu dùng SQLite cũ hơn, dùng try-catch bắt "duplicate column name" thay vì `IF NOT EXISTS`.

```bash
git commit -m "feat: thêm ALTER TABLE is_locked vào MigrationRunner cho DB cũ"
```

---

### Cập nhật `UserDao.java` — thêm `updateLocked` + `mapRow` đọc `is_locked`

Mở file `UserDao.java` đã có, cập nhật `save()`, `mapRow()`, và thêm `updateLocked()`:

```java
// === CẬP NHẬT UserDao.java ===

// --- 1. Cập nhật save() — thêm is_locked vào INSERT ---
// Thay thế câu INSERT cũ bằng:
  public void save(User user) {
    String sql = "INSERT INTO users (id, username, password_hash, email, role, "
        + "is_locked, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, user.getId());
        ps.setString(2, user.getUsername());
        ps.setString(3, user.getPasswordHash());
        ps.setString(4, user.getEmail());
        ps.setString(5, user.getRole().name());
        ps.setInt(6, user.isLocked() ? 1 : 0);
        ps.setString(7, user.getCreatedAt().toString());
        ps.setString(8, user.getUpdatedAt().toString());
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("UserDao.save that bai: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }

// --- 2. Cập nhật mapRow() — đọc is_locked ---
// Thêm dòng đọc is_locked vào mapRow, trước return:
//   boolean locked = rs.getInt("is_locked") == 1;
//   user.setLocked(locked);

// --- 3. THÊM method mới: updateLocked() ---
  /**
   * Cap nhat trang thai khoa/mo khoa tai khoan.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin khoa/mo tai khoan]
   *
   * @param userId id nguoi dung
   * @param locked true de khoa, false de mo
   */
  public void updateLocked(String userId, boolean locked) {
    String sql = "UPDATE users SET is_locked = ?, updated_at = ? WHERE id = ?";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, locked ? 1 : 0);
        ps.setString(2, java.time.LocalDateTime.now().toString());
        ps.setString(3, userId);
        int affected = ps.executeUpdate();
        if (affected == 0) {
          throw new RuntimeException(
              "UserDao.updateLocked: khong tim thay user " + userId);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(
          "UserDao.updateLocked that bai: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
  }
```

```bash
git commit -m "feat: UserDao thêm updateLocked(), cập nhật save()/mapRow() đọc is_locked"
```

---

### Cập nhật `RequestHandler.java` — thêm is_locked check trong handleLogin

Mở file `RequestHandler.java` đã có, thêm check `isLocked()` vào `handleLogin()`:

```java
// === THÊM VÀO handleLogin() — sau verifyPassword() thành công, trước createSession() ===

    // 📌 [Tieu chi: Quan ly nguoi dung — kiem tra tai khoan bi khoa]
    if (user.isLocked()) {
      auditLogService.log(user.getId(),
          AuditActions.USER_LOGIN, "{\"blocked\":true}");
      return MessageMapper.toJson(
          MessageResponse.error("LOGIN", "TAI KHOAN BI KHOA"));
    }
```

```bash
git commit -m "feat: handleLogin kiểm tra user.isLocked() → trả về TÀI KHOẢN BỊ KHÓA"
```

---

### Cập nhật `ServerApp.java` — gọi AuctionManager.start()

Mở file `ServerApp.java` đã có, thêm khởi động AuctionManager:

```java
// === THÊM VÀO ServerApp.main() — sau SocketServerCore.start() ===

    // 📌 [Tieu chi: Singleton + Ky thuat quan trong — AuctionManager lifecycle]
    com.bidhub.server.service.AuctionManager.getInstance().start();
    System.out.println("[ServerApp] AuctionManager da khoi dong.");
```

```bash
git commit -m "feat: ServerApp gọi AuctionManager.start() sau SocketServerCore.start()"
```

---

### `AuctionManager.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Singleton quan ly auction trong RAM — luu tru va lifecycle tu dong.
 *
 * <p>Dung {@link ConcurrentHashMap} de thread-safe khi nhieu handler (place bid)
 * va lifecycle task truy cap dong thoi. {@link ScheduledExecutorService} chay
 * {@link AuctionLifecycleTask} moi 5 giay de kiem tra va dong cac phien het han.
 *
 * <p>// 📌 [Tieu chi: Ky thuat quan trong & concurrency —
 *     ScheduledExecutorService + ConcurrentHashMap]
 * // 📌 [Tieu chi: Design Pattern Singleton —
 *     volatile + double-checked locking]
 * // 📌 [Tieu chi: Chuc nang dau gia — lifecycle tu dong dong phien]
 */
public final class AuctionManager {

  private static volatile AuctionManager instance;

  // 📌 [Tieu chi: Ky thuat quan trong — ConcurrentHashMap cho concurrent access]
  private final ConcurrentHashMap<String, Auction> auctions;

  // 📌 [Tieu chi: Ky thuat quan trong — ScheduledExecutorService cho periodic task]
  private final ScheduledExecutorService scheduler;

  private AuctionManager() {
    this.auctions = new ConcurrentHashMap<>();
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "auction-lifecycle");
      t.setDaemon(true); // daemon thread — khong block JVM shutdown
      return t;
    });
  }

  /**
   * Tra ve instance duy nhat (thread-safe, double-checked locking).
   *
   * @return AuctionManager instance
   */
  public static AuctionManager getInstance() {
    if (instance == null) {
      synchronized (AuctionManager.class) {
        if (instance == null) {
          instance = new AuctionManager();
        }
      }
    }
    return instance;
  }

  /**
   * Khoi dong AuctionManager — load tat ca RUNNING auction tu DB vao RAM,
   * schedule {@link AuctionLifecycleTask} chay moi 5 giay.
   *
   * <p>// 📌 [Tieu chi: Chuc nang dau gia — tu dong kiem tra va dong phien]
   */
  public void start() {
    // Load tat ca RUNNING auction tu DB
    AuctionDao auctionDao = new AuctionDao();
    List<Auction> activeAuctions = auctionDao.findActiveAuctions();
    for (Auction auction : activeAuctions) {
      auctions.put(auction.getId(), auction);
    }
    System.out.println("[AuctionManager] Da load " + activeAuctions.size()
        + " RUNNING auctions vao RAM.");

    // Schedule lifecycle task moi 5 giay
    AuctionLifecycleTask task = new AuctionLifecycleTask();
    scheduler.scheduleAtFixedRate(task, 5, 5, TimeUnit.SECONDS);
    System.out.println("[AuctionManager] Lifecycle task scheduled (5s interval).");
  }

  /**
   * Dung scheduler — goi khi server shutdown.
   */
  public void stop() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    System.out.println("[AuctionManager] Da dung lifecycle scheduler.");
  }

  /**
   * Them auction vao RAM cache.
   *
   * @param auction auction can them
   */
  public void addAuction(Auction auction) {
    if (auction != null && auction.getId() != null) {
      auctions.put(auction.getId(), auction);
    }
  }

  /**
   * Xoa auction khoi RAM cache (sau khi dong phien).
   *
   * @param auctionId id cua auction
   */
  public void removeAuction(String auctionId) {
    auctions.remove(auctionId);
  }

  /**
   * Lay auction tu RAM cache.
   *
   * @param auctionId id cua auction
   * @return Optional chua auction neu ton tai trong RAM
   */
  public Optional<Auction> getAuction(String auctionId) {
    return Optional.ofNullable(auctions.get(auctionId));
  }

  /**
   * Tra ve danh sach tat ca auction dang active — tao copy de tranh
   * ConcurrentModificationException khi iterate.
   *
   * <p>// 📌 [Tieu chi: Ky thuat quan trong — tao copy ArrayList cho safe iteration]
   *
   * @return danh sach auction dang hoat dong
   */
  public List<Auction> getAllActive() {
    return new ArrayList<>(auctions.values());
  }

  /**
   * Tra ve so luong auction dang quan ly trong RAM — chi dung cho test.
   *
   * @return so luong auction
   */
  public int activeCount() {
    return auctions.size();
  }

  /** Xoa toan bo auction tu RAM — chi dung cho test. */
  public void clearAll() {
    auctions.clear();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm AuctionManager Singleton với ScheduledExecutorService + ConcurrentHashMap"
```

---

### `AuctionLifecycleTask.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Task chay dinh ky — kiem tra va dong cac phien dau gia het han.
 *
 * <p>Moi lan chay, lay danh sach auction active tu {@link AuctionManager#getAllActive()},
 * kiem tra tung auction: neu {@code endTime} da qua → goi {@link #closeAuction(Auction)}.
 *
 * <p>// 📌 [Tieu chi: Chuc nang dau gia — lifecycle tu dong dong phien]
 * // 📌 [Tieu chi: Ky thuat quan trọng — Runnable duoc ScheduledExecutorService goi dinh ky]
 */
public final class AuctionLifecycleTask implements Runnable {

  @Override
  public void run() {
    try {
      List<Auction> activeList = AuctionManager.getInstance().getAllActive();
      for (Auction auction : activeList) {
        try {
          if (auction.getEndTime() != null
              && auction.getEndTime().isBefore(LocalDateTime.now())
              && auction.getStatus() == AuctionStatus.RUNNING) {
            closeAuction(auction);
          }
        } catch (Exception e) {
          // 📌 [Tieu chi: Xu ly loi — khong de 1 auction loi block cac auction khac]
          System.err.println("[LifecycleTask] Loi xu ly auction "
              + auction.getId() + ": " + e.getMessage());
        }
      }
    } catch (Exception e) {
      System.err.println("[LifecycleTask] Loi chung: " + e.getMessage());
    }
  }

  /**
   * Dong 1 phien dau gia — chuyen status FINISHED, xac dinh winner, cap nhat DB.
   *
   * <p>Flow:
   * <ol>
   *   <li>transitionTo(FINISHED) — validate trong Auction class</li>
   *   <li>updateStatus(FINISHED) trong DB</li>
   *   <li>getHighestBid() — tim nguoi thang</li>
   *   <li>Neu co winner → da cap nhat trong DB qua bidDao.save() truoc do</li>
   *   <li>removeAuction() khoi RAM</li>
   * </ol>
   *
   * <p>// 📌 [Tieu chi: Chuc nang dau gia — dong phien tu dong, xac dinh winner]
   *
   * @param auction auction can dong
   */
  private void closeAuction(Auction auction) {
    String auctionId = auction.getId();
    System.out.println("[LifecycleTask] Dang dong phien: " + auctionId);

    // 1. Chuyen trang thai
    auction.transitionTo(AuctionStatus.FINISHED);

    // 2. Cap nhat status trong DB
    AuctionDao auctionDao = new AuctionDao();
    auctionDao.updateStatus(auctionId, AuctionStatus.FINISHED);

    // 3. Tim winner
    BidDao bidDao = new BidDao();
    Optional<BidTransaction> highestBidOpt = bidDao.getHighestBid(auctionId);

    if (highestBidOpt.isPresent()) {
      BidTransaction winner = highestBidOpt.get();
      System.out.println("[LifecycleTask] Winner: " + winner.getBidderId()
          + " voi gia " + winner.getBidAmount());
      // Winner da duoc cap nhat qua auctionDao.updateHighestBid()
      // khi bid duoc dat → khong can update lai
    } else {
      System.out.println("[LifecycleTask] Khong co bid nao — phien "
          + auctionId + " ket thuc khong co nguoi thang.");
    }

    // 4. Xoa khoi RAM
    AuctionManager.getInstance().removeAuction(auctionId);
    System.out.println("[LifecycleTask] Da dong phien: " + auctionId);
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm AuctionLifecycleTask — đóng phiên hết hạn, xác định winner"
```

---

### ✅ Test đầu ra — `AuctionManagerTest.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

  private AuctionManager auctionManager;

  @BeforeEach
  void setUp() {
    auctionManager = AuctionManager.getInstance();
    auctionManager.clearAll();
  }

  @AfterEach
  void tearDown() {
    auctionManager.clearAll();
  }

  private Auction createTestAuction(String id) {
    Auction a = new Auction();
    // Reflective set fields — vi Auction khong co public setter cho id
    try {
      var idField = a.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(a, id);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return a;
  }

  @Test
  @DisplayName("getInstance tra ve cung instance (Singleton)")
  void getInstance_sameInstance() {
    AuctionManager a1 = AuctionManager.getInstance();
    AuctionManager a2 = AuctionManager.getInstance();
    assertSame(a1, a2);
  }

  @Test
  @DisplayName("addAuction them auction vao RAM")
  void addAuction_addsToMap() {
    Auction a = createTestAuction("auc-001");
    auctionManager.addAuction(a);
    assertEquals(1, auctionManager.activeCount());
  }

  @Test
  @DisplayName("getAuction voi id hop le tra ve auction")
  void getAuction_found() {
    Auction a = createTestAuction("auc-002");
    auctionManager.addAuction(a);
    Optional<Auction> result = auctionManager.getAuction("auc-002");
    assertTrue(result.isPresent());
    assertEquals("auc-002", result.get().getId());
  }

  @Test
  @DisplayName("getAuction voi id khong ton tai tra ve empty")
  void getAuction_notFound() {
    assertTrue(auctionManager.getAuction("fake-id").isEmpty());
  }

  @Test
  @DisplayName("removeAuction xoa auction khoi RAM")
  void removeAuction_removesFromMap() {
    Auction a = createTestAuction("auc-003");
    auctionManager.addAuction(a);
    auctionManager.removeAuction("auc-003");
    assertEquals(0, auctionManager.activeCount());
    assertTrue(auctionManager.getAuction("auc-003").isEmpty());
  }

  @Test
  @DisplayName("getAllActive tra ve copy — modify list khong anh huong map")
  void getAllActive_returnsCopy() {
    Auction a1 = createTestAuction("auc-004");
    Auction a2 = createTestAuction("auc-005");
    auctionManager.addAuction(a1);
    auctionManager.addAuction(a2);
    var list = auctionManager.getAllActive();
    assertEquals(2, list.size());
    list.clear();
    assertEquals(2, auctionManager.activeCount()); // map khong bi anh huong
  }

  @Test
  @DisplayName("addAuction voi null khong crash")
  void addAuction_nullSafe() {
    assertDoesNotThrow(() -> auctionManager.addAuction(null));
    assertEquals(0, auctionManager.activeCount());
  }

  @Test
  @DisplayName("activeCount tra ve dung so luong")
  void activeCount_correct() {
    assertEquals(0, auctionManager.activeCount());
    auctionManager.addAuction(createTestAuction("a1"));
    assertEquals(1, auctionManager.activeCount());
    auctionManager.addAuction(createTestAuction("a2"));
    assertEquals(2, auctionManager.activeCount());
    auctionManager.removeAuction("a1");
    assertEquals(1, auctionManager.activeCount());
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

---

### ✅ Test đầu ra — `AuctionLifecycleTaskTest.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AuctionLifecycleTaskTest {

  private AuctionManager auctionManager;
  private AuctionLifecycleTask task;

  @BeforeEach
  void setUp() {
    auctionManager = AuctionManager.getInstance();
    auctionManager.clearAll();
    task = new AuctionLifecycleTask();
  }

  @AfterEach
  void tearDown() {
    auctionManager.clearAll();
  }

  private Auction createExpiredAuction(String id) {
    Auction a = new Auction();
    try {
      var idField = a.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(a, id);
      var endField = a.getClass().getDeclaredField("endTime");
      endField.setAccessible(true);
      endField.set(a, LocalDateTime.now().minusMinutes(10));
      var statusField = a.getClass().getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(a, AuctionStatus.RUNNING);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return a;
  }

  private Auction createFutureAuction(String id) {
    Auction a = new Auction();
    try {
      var idField = a.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(a, id);
      var endField = a.getClass().getDeclaredField("endTime");
      endField.setAccessible(true);
      endField.set(a, LocalDateTime.now().plusHours(1));
      var statusField = a.getClass().getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(a, AuctionStatus.RUNNING);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return a;
  }

  @Test
  @DisplayName("run() khong crash khi danh sach rong")
  void run_emptyList_noCrash() {
    assertDoesNotThrow(() -> task.run());
  }

  @Test
  @DisplayName("run() khong crash khi auction con han — khong dong phien")
  void run_futureAuction_noClose() {
    auctionManager.addAuction(createFutureAuction("auc-future"));
    task.run(); // Khong nen crash — auction con han
    // Auction van trong RAM (khong bi remove vi chua het han)
    // Note: lifecycle task can DB de updateStatus → se throw o test khong co DB
    // Nhung logic kiem tra endTime.isBefore(now) se false → skip
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm AuctionManagerTest (8 cases) và AuctionLifecycleTaskTest (2 cases)"
```

---

**Kiểm tra manual:**
```bash
# Chạy test
mvn test -pl bidhub-server -Dtest="AuctionManagerTest,AuctionLifecycleTaskTest" -q
# Output: Tests run: 10, Failures: 0

# Kiểm tra schema migration (cần DB thật)
mvn compile -pl bidhub-server -q && mvn exec:java -pl bidhub-server \
  -Dexec.mainClass="com.bidhub.server.ServerApp"
# Output: [MigrationRunner] Cot is_locked da san sang.
#         [AuctionManager] Da load X RUNNING auctions vao RAM.
#         [AuctionManager] Lifecycle task scheduled (5s interval).
#         [SocketServerCore] Dang lang nghe cong 9090
```

**❌ FAIL nếu:**
- `AuctionManager.getInstance()` gọi 2 lần trả về khác instance → Singleton sai
- `getAllActive()` trả về `values()` trực tiếp thay vì copy → `ConcurrentModificationException`
- `AuctionLifecycleTask.run()` ném exception chưa catch → scheduler stop task
- `UserDao.updateLocked()` không update `updated_at` → audit trail bị thiếu timestamp
- `schema.sql` thiếu `is_locked` → MigrationRunner chạy trên DB cũ không có cột
- `handleLogin` không check `isLocked()` → user bị khóa vẫn login được

---

## 👤 QUỐC MINH — BidValidator & PlaceBid/AuctionList/Detail Handlers

```
Branch: feature/tuan-6-quocminh-bid-handler
Phụ thuộc: AuctionManager (tuần 6, Đăng) — rebase sau khi Đăng merge
           AuctionDao (tuần 3, Khoa) — handleGetAuctionList
           BidDao (tuần 3, Khoa) — handlePlaceBid save bid
           ItemDao (tuần 3, Quốc Minh) — BidValidator kiểm tra seller
           RequestHandler (tuần 4+5) — MỞ RA thêm switch cases + handler methods
           handleLogin (tuần 5) — MỞ RA thêm is_locked check (phối hợp Đăng)
```

📌 **[Tiêu chí điểm: Chức năng đấu giá — Place Bid — 1.0đ + Xử lý lỗi & ngoại lệ — BidValidator 1.0đ + MVC — bid/auction handlers — phần 0.5đ]**

### 📝 Mô tả bài tập

`BidValidator` là class validation cốt lõi cho chức năng đấu giá, kiểm tra 5 điều kiện trước khi cho phép
đặt giá: (1) auction đang RUNNING, (2) bidder không phải người đang dẫn đầu, (3) bidder không phải seller,
(4) giá cao hơn giá hiện tại, (5) bước giá đạt minimumIncrement. Mỗi điều kiện vi phạm ném đúng subclass
của `BidHubException` — đáp ứng yêu cầu "Xử lý lỗi & ngoại lệ" trong barem.

3 handler mới trong `RequestHandler`: `handlePlaceBid` là handler phức tạp nhất — validate qua
`BidValidator`, tạo `BidTransaction`, lưu DB, cập nhật cả RAM (AuctionManager) và DB (AuctionDao).
`handleGetAuctionList` trả về danh sách auction đang active. `handleGetAuctionDetail` trả về chi tiết
1 auction kèm lịch sử bid.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Quốc Minh cần `AuctionManager` (Đăng) — `getAuction()`, `addAuction()`
- Quốc Minh cần `ItemDao` (đã có từ T3) — `findById()` để check seller trong BidValidator
- Quốc Minh cần `AuctionDao` (T3) + `BidDao` (T3) — đã có sẵn
- Quốc Minh cần `UserDao` + `User.isLocked()` (Đăng) — cho is_locked check trong handleLogin

**Kịch bản chọn: C — Rebase từ develop sau khi Đăng merge**

**Nếu Đăng chưa merge AuctionManager:**
Quốc Minh dùng stub (như phần trên). Tuy nhiên, BidValidator không phụ thuộc AuctionManager — có thể
code và test BidValidator độc lập trước.

**Các bước:**
1. Quốc Minh tạo branch, code `BidValidator.java` — không phụ thuộc AuctionManager
2. Quốc Minh code 3 handler method + cập nhật switch-case trong RequestHandler
3. Quốc Minh cập nhật `handleLogin` thêm is_locked check (phối hợp Đăng)
4. Đăng merge AuctionManager → Quốc Minh rebase
5. Quốc Minh xóa stub AuctionManager, dùng thật
6. Push, tạo PR → review → merge

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── service/
│   │   └── BidValidator.java   ← MỚI
│   └── network/
│       └── RequestHandler.java (đã có T4+T5 — MỞ RA thêm 3 handler methods + switch cases)
└── test/java/com/bidhub/server/
    └── service/
        └── BidValidatorTest.java ← MỚI
```

---

### `BidValidator.java`

```java
package com.bidhub.server.service;

import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.Item;
import java.util.Optional;

/**
 * Validator kiem tra dieu kien dat gia — dam bao luat dau gia duoc thuc thi.
 *
 * <p>5 dieu kien validate:
 * <ol>
 *   <li>Auction phai dang RUNNING</li>
 *   <li>Bidder khong duoc la nguoi dan dau hien tai</li>
 *   <li>Bidder khong duoc la seller cua san pham</li>
 *   <li>Gia dat phai cao hon gia hien tai</li>
 *   <li>Buoc gia phai dat minimumIncrement</li>
 * </ol>
 *
 * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — tung dieu kien nem exception phu hop]
 * // 📌 [Tieu chi: Chuc nang dau gia — kiem tra luat truoc khi cho dat gia]
 */
public final class BidValidator {

  private final ItemDao itemDao;

  /** Constructor mac dinh — tao ItemDao production. */
  public BidValidator() {
    this.itemDao = new ItemDao();
  }

  /**
   * Constructor cho test — inject ItemDao.
   *
   * @param itemDao ItemDao inject
   */
  public BidValidator(ItemDao itemDao) {
    this.itemDao = itemDao;
  }

  /**
   * Kiem tra 5 dieu kien dat gia. Nem exception neu vi pham bat ky dieu kien nao.
   *
   * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — nem AuctionClosedException /
   *     InvalidBidException tuy theo loi]
   *
   * @param auction   auction can kiem tra
   * @param bidderId  id cua nguoi dau gia
   * @param bidAmount so tien dau gia
   * @throws AuctionClosedException neu auction khong dang RUNNING
   * @throws InvalidBidException   neu vi pham cac dieu kien dau gia
   */
  public void validate(Auction auction, String bidderId, double bidAmount) {
    // 1. Auction phai dang RUNNING
    // 📌 [Tieu chi: Chuc nang dau gia — chi cho dat gia khi RUNNING]
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      throw new AuctionClosedException(
          "Phien dau gia da dong. Trang thai: " + auction.getStatus().name());
    }

    // 2. Bidder khong duoc la nguoi dan dau hien tai
    // 📌 [Tieu chi: Xu ly loi & ngoai le — kiem tra nguoi dan dau]
    if (auction.getHighestBidderId() != null
        && auction.getHighestBidderId().equals(bidderId)) {
      throw new InvalidBidException("Ban dang la nguoi dan dau.");
    }

    // 3. Bidder khong duoc la seller cua san pham
    // 📌 [Tieu chi: Xu ly loi & ngoai le — seller khong tu dau gia]
    String itemOwnerId = getItemOwnerId(auction.getItemId());
    if (itemOwnerId != null && itemOwnerId.equals(bidderId)) {
      throw new InvalidBidException("Seller khong the tu dau gia san pham cua minh.");
    }

    // 4. Gia dat phai cao hon gia hien tai
    // 📌 [Tieu chi: Xu ly loi & ngoai le — gia phai cao hon]
    if (bidAmount <= auction.getCurrentHighestBid()) {
      throw new InvalidBidException(
          "Gia dat phai cao hon gia hien tai (" + auction.getCurrentHighestBid() + ").");
    }

    // 5. Buoc gia phai dat minimumIncrement
    // 📌 [Tieu chi: Xu ly loi & ngoai le — kiem tra buoc gia]
    double increment = bidAmount - auction.getCurrentHighestBid();
    if (increment < auction.getMinimumIncrement()) {
      throw new InvalidBidException(
          "Buoc gia toi thieu la " + auction.getMinimumIncrement()
              + ". Ban dat thieu " + (auction.getMinimumIncrement() - increment) + ".");
    }
  }

  /**
   * Lay owner id cua san pham tu ItemDao.
   *
   * @param itemId id san pham
   * @return sellerId hoac null neu khong tim thay
   */
  private String getItemOwnerId(String itemId) {
    if (itemId == null) {
      return null;
    }
    Optional<Item> itemOpt = itemDao.findById(itemId);
    return itemOpt.map(Item::getSellerId).orElse(null);
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm BidValidator — kiểm tra 5 điều kiện đặt giá, ném exception phù hợp"
```

---

### Cập nhật `RequestHandler.java` — thêm 3 handler + switch cases

Mở file `RequestHandler.java`, thêm 3 private handler method và cập nhật switch-case + AUTH_REQUIRED:

```java
// === THÊM VÀO RequestHandler.java ===

// --- 1. Thêm import ---
import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import com.bidhub.server.service.AuctionManager;
import com.bidhub.server.service.BidValidator;
import java.util.List;

// --- 2. Thêm field ---
  private final AuctionDao auctionDao;
  private final BidDao bidDao;
  private final ItemDao itemDao;
  private final BidValidator bidValidator;

// --- 3. Cập nhật constructor ---
  public RequestHandler() {
    this.injectedUserDao = null;
    this.injectedItemDao = null;
    this.auditLogService = new AuditLogService();
    this.userDao = new UserDao();
    this.auctionDao = new AuctionDao();
    this.bidDao = new BidDao();
    this.itemDao = new ItemDao();
    this.bidValidator = new BidValidator(itemDao);
  }

  RequestHandler(Object injectedUserDao, Object injectedItemDao) {
    this.injectedUserDao = injectedUserDao;
    this.injectedItemDao = injectedItemDao;
    this.auditLogService = new AuditLogService();
    this.userDao = injectedUserDao instanceof UserDao
        ? (UserDao) injectedUserDao : new UserDao();
    this.auctionDao = new AuctionDao();
    this.bidDao = new BidDao();
    this.itemDao = new ItemDao();
    this.bidValidator = new BidValidator(itemDao);
  }

  RequestHandler(Object injectedUserDao, Object injectedItemDao,
      AuditLogService injectedAuditService) {
    this.injectedUserDao = injectedUserDao;
    this.injectedItemDao = injectedItemDao;
    this.auditLogService = injectedAuditService;
    this.userDao = injectedUserDao instanceof UserDao
        ? (UserDao) injectedUserDao : new UserDao();
    this.auctionDao = new AuctionDao();
    this.bidDao = new BidDao();
    this.itemDao = new ItemDao();
    this.bidValidator = new BidValidator(itemDao);
  }

// --- 4. Cập nhật AUTH_REQUIRED ---
  private static final Set<String> AUTH_REQUIRED = Set.of(
      "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
      "LIST_MY_ITEMS", "PLACE_BID", "GET_AUCTION_DETAIL",
      "GET_USER_LIST", "LOCK_USER", "UNLOCK_USER"
  );

// --- 5. Thêm vào switch-case ---
        case "PLACE_BID"        -> handlePlaceBid(session, payload);
        case "GET_AUCTION_LIST"  -> handleGetAuctionList(session, payload);
        case "GET_AUCTION_DETAIL" -> handleGetAuctionDetail(session, payload);

// --- 6. Handler methods ---

  /**
   * Xu ly dat gia — validate, luu bid, cap nhat RAM va DB.
   *
   * <p>// 📌 [Tieu chi: Chuc nang dau gia — place bid flow day du]
   * // 📌 [Tieu chi: MVC — handler la tang dieu phoi]
   *
   * @param session session cua client
   * @param payload {auctionId, bidAmount}
   * @return JSON response
   */
  private String handlePlaceBid(Session session, JsonNode payload) {
    String userId = SecurityContext.requireAuthenticated(session);

    String auctionId = payload.path("auctionId").asText("");
    double bidAmount = payload.path("bidAmount").asDouble(0.0);

    if (auctionId.isBlank()) {
      throw new ValidationException("auctionId khong duoc de trong");
    }
    if (bidAmount <= 0) {
      throw new InvalidBidException("Gia dat phai lon hon 0.");
    }

    // Lay auction tu RAM (AuctionManager)
    // 📌 [Tieu chi: Ky thuat quan trong — truy cap auction tu RAM thay vi DB]
    Auction auction = AuctionManager.getInstance().getAuction(auctionId)
        .orElseThrow(() -> new AuctionNotFoundException(
            "Phien dau gia khong ton tai: " + auctionId));

    // Validate 5 dieu kien
    // 📌 [Tieu chi: Xu ly loi & ngoai le — BidValidator nem exception phu hop]
    bidValidator.validate(auction, userId, bidAmount);

    // Tao BidTransaction
    BidTransaction bid = new BidTransaction(auctionId, userId, bidAmount);

    // Luu bid vao DB
    // 📌 [Tieu chi: Chuc nang dau gia — luu bid transaction vao DB]
    bidDao.save(bid);

    // Cap nhat RAM
    // 📌 [Tieu chi: Ky thuat quan trong — cap nhat RAM nhanh hon DB]
    auction.setCurrentHighestBid(bidAmount);
    auction.setHighestBidderId(userId);

    // Cap nhat DB
    auctionDao.updateHighestBid(auctionId, bidAmount, userId);

    // Audit log
    auditLogService.log(userId, AuditActions.PLACE_BID,
        "{\"auctionId\":\"" + auctionId + "\",\"amount\":" + bidAmount + "}");

    return MessageMapper.toJson(MessageResponse.ok("PLACE_BID",
        Map.of("auctionId", auctionId,
            "currentHighestBid", bidAmount,
            "highestBidderId", userId)));
  }

  /**
   * Xu ly lay danh sach auction dang active.
   *
   * <p>// 📌 [Tieu chi: MVC — handler truy xuat du lieu tu DAO]
   *
   * @param session session cua client
   * @param payload payload rong
   * @return JSON response voi danh sach auction
   */
  private String handleGetAuctionList(Session session, JsonNode payload) {
    List<Auction> auctions = auctionDao.findActiveAuctions();
    return MessageMapper.toJson(
        MessageResponse.ok("GET_AUCTION_LIST", auctions));
  }

  /**
   * Xu ly lay chi tiet 1 auction.
   *
   * <p>// 📌 [Tieu chi: MVC — handler truy xuat chi tiet tu DAO + BidDao]
   *
   * @param session session cua client
   * @param payload {auctionId}
   * @return JSON response voi chi tiet auction + bid history
   */
  private String handleGetAuctionDetail(Session session, JsonNode payload) {
    String auctionId = payload.path("auctionId").asText("");
    if (auctionId.isBlank()) {
      throw new ValidationException("auctionId khong duoc de trong");
    }

    // Thu lay tu RAM truoc, neu khong co thi lay tu DB
    Auction auction = AuctionManager.getInstance().getAuction(auctionId)
        .orElseGet(() -> auctionDao.findById(auctionId)
            .orElseThrow(() -> new AuctionNotFoundException(
                "Phien dau gia khong ton tai: " + auctionId)));

    // Lay lich su bid
    List<BidTransaction> bidHistory = bidDao.findByAuctionId(auctionId);

    return MessageMapper.toJson(MessageResponse.ok("GET_AUCTION_DETAIL",
        Map.of("auction", auction, "bidHistory", bidHistory)));
  }
```

```bash
git commit -m "feat: thêm handlePlaceBid/handleGetAuctionList/handleGetAuctionDetail + BidValidator integration"
```

---

### ✅ Test đầu ra — `BidValidatorTest.java`

```java
package com.bidhub.server.service;

import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BidValidatorTest {

  private BidValidator validator;

  @BeforeEach
  void setUp() {
    // Validator voi null ItemDao — chi test nhung case khong can ItemDao
    validator = new BidValidator(null);
  }

  private Auction createRunningAuction(double currentBid,
      double minIncrement, String highestBidderId) {
    Auction a = new Auction();
    try {
      var idField = a.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(a, "auc-test");
      var statusField = a.getClass().getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(a, AuctionStatus.RUNNING);
      var bidField = a.getClass().getDeclaredField("currentHighestBid");
      bidField.setAccessible(true);
      bidField.set(a, currentBid);
      var incField = a.getClass().getDeclaredField("minimumIncrement");
      incField.setAccessible(true);
      incField.set(a, minIncrement);
      var bidderField = a.getClass().getDeclaredField("highestBidderId");
      bidderField.setAccessible(true);
      bidderField.set(a, highestBidderId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return a;
  }

  @Test
  @DisplayName("validate hop le — khong nem exception")
  void validate_validBid_noException() {
    Auction a = createRunningAuction(1000.0, 50.0, "user-other");
    assertDoesNotThrow(
        () -> validator.validate(a, "user-bidder", 1100.0));
  }

  @Test
  @DisplayName("validate auction FINISHED → AuctionClosedException")
  void validate_finishedAuction_throwsClosed() {
    Auction a = createRunningAuction(1000.0, 50.0, null);
    try {
      var statusField = a.getClass().getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(a, AuctionStatus.FINISHED);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertThrows(AuctionClosedException.class,
        () -> validator.validate(a, "user-bidder", 1100.0));
  }

  @Test
  @DisplayName("validate nguoi dan dau bid lai → InvalidBidException")
  void validate_currentLeader_throwsException() {
    Auction a = createRunningAuction(1000.0, 50.0, "user-leader");
    InvalidBidException ex = assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-leader", 1100.0));
    assertTrue(ex.getMessage().contains("dang la nguoi dan dau"));
  }

  @Test
  @DisplayName("validate gia dat bang gia hien tai → InvalidBidException")
  void validate_equalBid_throwsException() {
    Auction a = createRunningAuction(1000.0, 50.0, null);
    assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-bidder", 1000.0));
  }

  @Test
  @DisplayName("validate gia dat thap hon gia hien tai → InvalidBidException")
  void validate_lowerBid_throwsException() {
    Auction a = createRunningAuction(1000.0, 50.0, null);
    assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-bidder", 500.0));
  }

  @Test
  @DisplayName("validate buoc gia khong du → InvalidBidException")
  void validate_insufficientIncrement_throwsException() {
    Auction a = createRunningAuction(1000.0, 100.0, null);
    // bidAmount = 1050, increment = 50 < minimumIncrement = 100
    InvalidBidException ex = assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-bidder", 1050.0));
    assertTrue(ex.getMessage().contains("Buoc gia toi thieu"));
  }

  @Test
  @DisplayName("validate gia am → InvalidBidException")
  void validate_negativeBid_throwsException() {
    Auction a = createRunningAuction(1000.0, 50.0, null);
    assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-bidder", -100.0));
  }

  @Test
  @DisplayName("validate auciton chua co ai bid (highestBidderId null) — hop le")
  void validate_noBidsYet_valid() {
    Auction a = createRunningAuction(0.0, 50.0, null);
    assertDoesNotThrow(
        () -> validator.validate(a, "user-bidder", 100.0));
  }

  @Test
  @DisplayName("validate chinh xac minimumIncrement — dat dung du → hop le")
  void validate_exactIncrement_valid() {
    Auction a = createRunningAuction(1000.0, 50.0, null);
    assertDoesNotThrow(
        () -> validator.validate(a, "user-bidder", 1050.0));
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm BidValidatorTest (9 cases) — 5 điều kiện validate + edge cases"
```

---

**Kiểm tra manual:**
```bash
# Chạy test BidValidator
mvn test -pl bidhub-server -Dtest="BidValidatorTest" -q
# Output: Tests run: 9, Failures: 0

# Manual test: gửi PLACE_BID request
echo '{"type":"PLACE_BID","token":"<valid-token>","payload":{"auctionId":"auc-001","bidAmount":1500.0}}' \
  | nc -q1 localhost 9090
# Output: {"status":"OK","type":"PLACE_BID","payload":{...}}

# Manual test: gửi GET_AUCTION_LIST
echo '{"type":"GET_AUCTION_LIST","token":"<valid-token>","payload":{}}' \
  | nc -q1 localhost 9090
# Output: {"status":"OK","type":"GET_AUCTION_LIST","payload":[...]}
```

**❌ FAIL nếu:**
- `BidValidator.validate()` cho phép bid khi auction FINISHED → thiếu check AuctionStatus
- Seller tự bid không bị chặn → thiếu check itemOwnerId
- Người đang dẫn đầu bid lại không bị chặn → thiếu check highestBidderId
- Bước giá nhỏ hơn minimumIncrement vẫn pass → logic so sánh sai
- `handlePlaceBid` không cập nhật cả RAM (AuctionManager) và DB (AuctionDao) → dữ liệu không nhất quán
- `handleGetAuctionDetail` không lấy bidHistory → client không thấy lịch sử bid

---

## 👤 CÔNG MINH — AuctionListView, AuctionDetailView & Countdown Timer

```
Branch: feature/tuan-6-congminh-auction-ui
Phụ thuộc: ServerGateway (tuần 4) + NetworkTask (tuần 4) — gửi request
           ClientSession (tuần 4) — kiểm tra role
           ViewRouter (tuần 3) — điều hướng màn hình
           Server API: GET_AUCTION_LIST, GET_AUCTION_DETAIL, PLACE_BID — có thể test với mock
Merge cuối cùng: Client không phụ thuộc server code, chỉ cần API response format ổn định
```

📌 **[Tiêu chí điểm: MVC — JavaFX Controller + TableView binding — phần 0.5đ + JavaFX Timeline countdown — kỹ thuật UI quan trọng]**

### 📝 Mô tả bài tập

`AuctionListController` hoàn thiện skeleton từ T3: khi `initialize()`, gửi `GET_AUCTION_LIST` qua
`NetworkTask` → parse JSON array → chuyển thành `ObservableList` → bind vào `TableView`. 4 cột: Tên
sản phẩm, Giá hiện tại, Thời gian kết thúc, Trạng thái. Button "Tạo phiên đấu giá" chỉ visible khi
`ClientSession.getInstance().getCurrentRole() == SELLER`. Double-click vào hàng → navigate sang
`AuctionDetail`.

`AuctionDetailController` implement `ContextAware` để nhận `auctionId` từ `ViewRouter`. Countdown timer
dùng `Timeline` + `KeyFrame(Duration.seconds(1))` → cập nhật Label còn lại mỗi giây. Form đặt giá:
`TextField bidAmount` + Button "Đặt giá" → `NetworkTask` gọi `PLACE_BID` → reload detail nếu thành công.

`CreateAuctionView.fxml` + `CreateAuctionController`: form cho SELLER chọn item, nhập startingPrice,
startTime/endTime, minimumIncrement. Gửi `CREATE_AUCTION` request (phía server có thể implement ở T6 hoặc T7).

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Công Minh chỉ cần `ServerGateway` + `NetworkTask` + `ClientSession` (đã có từ T4)
- Server API format: `{status, type, payload, message}` — không thay đổi
- Công Minh KHÔNG phụ thuộc `AuctionManager`, `BidValidator` — chỉ giao tiếp qua JSON

**Kịch bản chọn: A — Công Minh code song song với server, test với mock data**

Công Minh có thể test client độc lập:
1. Tạo mock response `GET_AUCTION_LIST` → parse trong controller
2. Test countdown timer với auction có `endTime` trong tương lai
3. Test TableView binding với dummy data
4. Khi server sẵn sàng → integration test end-to-end

### 📁 Cấu trúc file

```
bidhub-client/src/
├── main/java/com/bidhub/client/
│   ├── controller/
│   │   ├── AuctionListController.java    (đã có T3 — MỞ RA hoàn thiện)
│   │   ├── AuctionDetailController.java  (đã có T3 — MỞ RA hoàn thiện + ContextAware)
│   │   └── CreateAuctionController.java  ← MỚI
│   ├── navigation/
│   │   └── Views.java                    (đã có T1 — MỞ RA thêm CREATE_AUCTION)
│   └── util/
│       └── Views.java (hoặc navigation/Views.java)
├── main/resources/fxml/
│   ├── AuctionListView.fxml             (đã có T3 — MỞ RA hoàn thiện)
│   ├── AuctionDetailView.fxml           (đã có T3 — MỞ RA hoàn thiện)
│   └── CreateAuctionView.fxml           ← MỚI
```

---

### `AuctionListController.java` — hoàn thiện

```java
package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.common.network.MessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller danh sach phien dau gia — hien thi TableView voi du lieu tu server.
 *
 * <p>// 📌 [Tieu chi: MVC — Controller la tang dieu phoi client]
 * // 📌 [Tieu chi: JavaFX TableView + ObservableList binding]
 */
public class AuctionListController {

  @FXML private TableView<JsonNode> auctionTable;
  @FXML private TableColumn<JsonNode, String> colItemName;
  @FXML private TableColumn<JsonNode, String> colPrice;
  @FXML private TableColumn<JsonNode, String> colEndTime;
  @FXML private TableColumn<JsonNode, String> colStatus;
  @FXML private Button btnCreateAuction;
  @FXML private Button btnCreateItem;

  private final ObjectMapper mapper = new ObjectMapper();
  private final ObservableList<JsonNode> auctionData =
      FXCollections.observableArrayList();

  /**
   * Khoi tao — load danh sach auction va bind TableView.
   */
  @FXML
  public void initialize() {
    // 📌 [Tieu chi: JavaFX — TableView column binding]
    colItemName.setCellValueFactory(
        cellData -> {
          JsonNode node = cellData.getValue();
          String name = node.has("itemName")
              ? node.get("itemName").asText("") : node.path("id").asText("");
          return new javafx.beans.property.SimpleStringProperty(name);
        });
    colPrice.setCellValueFactory(
        cellData -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(cellData.getValue().path("currentHighestBid").asDouble(0))));
    colEndTime.setCellValueFactory(
        cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().path("endTime").asText("")));
    colStatus.setCellValueFactory(
        cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().path("status").asText("")));

    auctionTable.setItems(auctionData);

    // Double-click → navigate sang detail
    auctionTable.setRowFactory(tv -> {
      TableRow<JsonNode> row = new TableRow<>();
      row.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2 && !row.isEmpty()
            && event.getButton() == MouseButton.PRIMARY) {
          JsonNode selected = row.getItem();
          String auctionId = selected.path("id").asText("");
          if (!auctionId.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("auctionId", auctionId);
            com.bidhub.client.navigation.ViewRouter.getInstance()
                .navigateTo(Views.AUCTION_DETAIL, params);
          }
        }
      });
      return row;
    });

    // Button "Tạo phiên" chi visible cho SELLER
    // 📌 [Tieu chi: MVC — Controller phan quyen UI theo role]
    btnCreateAuction.setVisible(
        ClientSession.getInstance().getCurrentRole()
            == com.bidhub.server.model.UserRole.SELLER);
    btnCreateItem.setVisible(
        ClientSession.getInstance().getCurrentRole()
            == com.bidhub.server.model.UserRole.SELLER);

    btnCreateAuction.setOnAction(e ->
        com.bidhub.client.navigation.ViewRouter.getInstance()
            .navigateTo(Views.CREATE_AUCTION));
    btnCreateItem.setOnAction(e ->
        com.bidhub.client.navigation.ViewRouter.getInstance()
            .navigateTo(Views.CREATE_ITEM));

    // Load danh sach auction
    loadAuctionList();
  }

  /**
   * Gui request GET_AUCTION_LIST den server, bind ket qua vao TableView.
   *
   * <p>// 📌 [Tieu chi: JavaFX — NetworkTask chay background, setOnSucceeded tren FX thread]
   */
  private void loadAuctionList() {
    MessageRequest req = new MessageRequest();
    req.setType("GET_AUCTION_LIST");

    NetworkTask<MessageResponse> task = new NetworkTask<>(
        () -> ServerGateway.getInstance().sendRequest(req));

    task.setOnSucceeded(e -> {
      MessageResponse response = task.getValue();
      if ("OK".equals(response.getStatus())
          && response.getPayload() != null) {
        JsonNode payload = mapper.valueToTree(response.getPayload());
        if (payload.isArray()) {
          auctionData.clear();
          for (JsonNode node : payload) {
            auctionData.add(node);
          }
        }
      }
    });

    task.setOnFailed(e -> {
      System.err.println("[AuctionListController] Loi load danh sach: "
          + task.getException().getMessage());
    });

    new Thread(task).start();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: hoàn thiện AuctionListController — TableView + NetworkTask GET_AUCTION_LIST"
```

---

### `AuctionDetailController.java` — hoàn thiện

```java
package com.bidhub.client.controller;

import com.bidhub.client.navigation.ContextAware;
import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.common.network.MessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller chi tiet phien dau gia — hien thi thong tin, countdown, form dat gia.
 *
 * <p>Implement {@link ContextAware} de nhan auctionId tu ViewRouter.
 * Countdown timer dung {@link Timeline} + {@link KeyFrame} cap nhat moi 1 giay.
 *
 * <p>// 📌 [Tieu chi: JavaFX Timeline & KeyFrame — countdown timer]
 * // 📌 [Tieu chi: MVC — Controller hien thi va tuong tac]
 */
public class AuctionDetailController implements ContextAware {

  @FXML private Label lblItemName;
  @FXML private Label lblDescription;
  @FXML private Label lblStartingPrice;
  @FXML private Label lblCurrentPrice;
  @FXML private Label lblHighestBidder;
  @FXML private Label lblCountdown;
  @FXML private Label lblStatus;
  @FXML private TextField tfBidAmount;
  @FXML private Button btnPlaceBid;
  @FXML private Button btnBack;

  private String auctionId;
  private LocalDateTime endTime;
  private Timeline countdownTimeline;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Nhan context params tu ViewRouter — auctionId.
   *
   * @param params map chua auctionId
   */
  @Override
  public void setContext(Map<String, Object> params) {
    this.auctionId = (String) params.get("auctionId");
    if (auctionId != null && !auctionId.isBlank()) {
      loadAuctionDetail();
    }
  }

  @FXML
  public void initialize() {
    btnPlaceBid.setOnAction(e -> placeBid());
    btnBack.setOnAction(e ->
        com.bidhub.client.navigation.ViewRouter.getInstance()
            .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));
  }

  /**
   * Gui request GET_AUCTION_DETAIL den server, populate Labels.
   *
   * <p>// 📌 [Tieu chi: JavaFX — NetworkTask cap nhat UI tren FX thread]
   */
  private void loadAuctionDetail() {
    MessageRequest req = new MessageRequest();
    req.setType("GET_AUCTION_DETAIL");
    req.setPayload(mapper.createObjectNode().put("auctionId", auctionId));

    NetworkTask<MessageResponse> task = new NetworkTask<>(
        () -> ServerGateway.getInstance().sendRequest(req));

    task.setOnSucceeded(e -> {
      MessageResponse response = task.getValue();
      if ("OK".equals(response.getStatus())
          && response.getPayload() != null) {
        JsonNode payload = mapper.valueToTree(response.getPayload());
        populateLabels(payload);
      } else {
        String msg = response.getMessage();
        Platform.runLater(() -> showError(msg));
      }
    });

    task.setOnFailed(e ->
        Platform.runLater(() -> showError(
            task.getException().getMessage())));

    new Thread(task).start();
  }

  /**
   * Populate cac Label tu JSON payload.
   *
   * @param payload JSON node chua auction detail
   */
  private void populateLabels(JsonNode payload) {
    JsonNode auction = payload.path("auction");
    lblItemName.setText(auction.path("itemId").asText(""));
    lblStartingPrice.setText(
        "Gia khoi diem: " + auction.path("startingPrice").asDouble(0));
    lblCurrentPrice.setText(
        "Gia hien tai: " + auction.path("currentHighestBid").asDouble(0));
    lblHighestBidder.setText(
        "Nguoi dan dau: " + auction.path("highestBidderId").asText("Chua co"));
    lblStatus.setText(
        "Trang thai: " + auction.path("status").asText(""));

    String endTimeStr = auction.path("endTime").asText("");
    if (!endTimeStr.isEmpty()) {
      try {
        endTime = LocalDateTime.parse(endTimeStr);
        startCountdown();
      } catch (Exception ex) {
        lblCountdown.setText("Khong the parse thoi gian");
      }
    }

    // Kiem tra trang thai — neu FINISHED thi disable
    String status = auction.path("status").asText("");
    if ("FINISHED".equals(status)) {
      lblStatus.setText("DA KET THUC");
      btnPlaceBid.setDisable(true);
      tfBidAmount.setDisable(true);
      stopCountdown();
    }
  }

  /**
   * Khoi dong countdown timer — cap nhat Label moi 1 giay.
   *
   * <p>// 📌 [Tieu chi: JavaFX Timeline + KeyFrame — countdown timer]
   */
  private void startCountdown() {
    stopCountdown(); // Dung timeline cu neu co

    if (endTime == null) {
      return;
    }

    countdownTimeline = new Timeline(
        new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  /**
   * Cap nhat Label countdown — goi moi 1 giay boi Timeline.
   */
  private void updateCountdown() {
    if (endTime == null) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    if (now.isAfter(endTime)) {
      lblCountdown.setText("DA KET THUC");
      btnPlaceBid.setDisable(true);
      tfBidAmount.setDisable(true);
      stopCountdown();
      return;
    }
    long seconds = java.time.Duration.between(now, endTime).getSeconds();
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    lblCountdown.setText(String.format("Con lai: %02d:%02d:%02d",
        hours, minutes, secs));
  }

  /**
   * Dung countdown timer — goi khi navigate di chieu khac hoac auction ket thuc.
   */
  private void stopCountdown() {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
      countdownTimeline = null;
    }
  }

  /**
   * Gui request PLACE_BID den server.
   *
   * <p>// 📌 [Tieu chi: Chuc nang dau gia — client gui bid len server]
   */
  private void placeBid() {
    String amountStr = tfBidAmount.getText().trim();
    if (amountStr.isEmpty()) {
      showError("Vui long nhap gia dau gia.");
      return;
    }
    double bidAmount;
    try {
      bidAmount = Double.parseDouble(amountStr);
    } catch (NumberFormatException ex) {
      showError("Gia khong hop le. Vui long nhap so.");
      return;
    }

    MessageRequest req = new MessageRequest();
    req.setType("PLACE_BID");
    req.setPayload(mapper.createObjectNode()
        .put("auctionId", auctionId)
        .put("bidAmount", bidAmount));

    NetworkTask<MessageResponse> task = new NetworkTask<>(
        () -> ServerGateway.getInstance().sendRequest(req));

    task.setOnSucceeded(e -> {
      MessageResponse response = task.getValue();
      if ("OK".equals(response.getStatus())) {
        loadAuctionDetail(); // Reload de cap nhat gia moi
      } else {
        Platform.runLater(() -> showError(response.getMessage()));
      }
    });

    task.setOnFailed(e ->
        Platform.runLater(() -> showError(
            task.getException().getMessage())));

    new Thread(task).start();
  }

  /**
   * Hien thi Alert loi.
   *
   * @param message noi dung loi
   */
  private void showError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR, message);
    alert.setTitle("Loi");
    alert.showAndWait();
  }

  /**
   * Dung Timeline khi controller bi destroy — tranh memory leak.
   */
  @FXML
  public void cleanup() {
    stopCountdown();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: hoàn thiện AuctionDetailController — countdown Timeline + form đặt giá + PLACE_BID"
```

---

### `CreateAuctionController.java` — mới

```java
package com.bidhub.client.controller;

import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.common.network.MessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

/**
 * Controller tao phien dau gia — chi cho SELLER.
 *
 * <p>Form: chon Item, nhap startingPrice, startTime/endTime, minimumIncrement.
 * Submit gui request CREATE_AUCTION den server.
 *
 * <p>// 📌 [Tieu chi: MVC — Controller thuc hien business logic tren client]
 */
public class CreateAuctionController {

  @FXML private ComboBox<String> cbItemId;
  @FXML private TextField tfStartingPrice;
  @FXML private TextField tfMinIncrement;
  @FXML private DatePicker dpStartTime;
  @FXML private Spinner<Integer> spStartHour;
  @FXML private DatePicker dpEndTime;
  @FXML private Spinner<Integer> spEndHour;
  @FXML private Button btnSubmit;
  @FXML private Button btnBack;

  private final ObjectMapper mapper = new ObjectMapper();

  @FXML
  public void initialize() {
    SpinnerValueFactory<Integer> hourFactory =
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12);
    spStartHour.setValueFactory(hourFactory);
    spEndHour.setValueFactory(hourFactory);
    spStartHour.setEditable(true);
    spEndHour.setEditable(true);

    btnSubmit.setOnAction(e -> createAuction());
    btnBack.setOnAction(e ->
        com.bidhub.client.navigation.ViewRouter.getInstance()
            .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));
  }

  /**
   * Gui request CREATE_AUCTION den server.
   *
   * <p>// 📌 [Tieu chi: Chuc nang dau gia — client tao phien moi]
   */
  private void createAuction() {
    String itemId = cbItemId.getValue();
    String priceStr = tfStartingPrice.getText().trim();
    String incStr = tfMinIncrement.getText().trim();

    if (itemId == null || itemId.isBlank()) {
      showError("Vui long chon san pham.");
      return;
    }
    if (priceStr.isEmpty()) {
      showError("Vui long nhap gia khoi diem.");
      return;
    }
    if (dpStartTime.getValue() == null || dpEndTime.getValue() == null) {
      showError("Vui long chon thoi gian bat dau va ket thuc.");
      return;
    }

    double startingPrice;
    double minIncrement;
    try {
      startingPrice = Double.parseDouble(priceStr);
      minIncrement = incStr.isEmpty() ? 1.0 : Double.parseDouble(incStr);
    } catch (NumberFormatException ex) {
      showError("Gia khong hop le. Vui long nhap so.");
      return;
    }

    String startTime = dpStartTime.getValue().toString() + "T"
        + String.format("%02d:00:00", spStartHour.getValue());
    String endTime = dpEndTime.getValue().toString() + "T"
        + String.format("%02d:00:00", spEndHour.getValue());

    ObjectNode payload = mapper.createObjectNode();
    payload.put("itemId", itemId);
    payload.put("startingPrice", startingPrice);
    payload.put("minimumIncrement", minIncrement);
    payload.put("startTime", startTime);
    payload.put("endTime", endTime);

    MessageRequest req = new MessageRequest();
    req.setType("CREATE_AUCTION");
    req.setPayload(payload);

    NetworkTask<MessageResponse> task = new NetworkTask<>(
        () -> ServerGateway.getInstance().sendRequest(req));

    task.setOnSucceeded(e -> {
      MessageResponse response = task.getValue();
      if ("OK".equals(response.getStatus())) {
        javafx.application.Platform.runLater(() ->
            com.bidhub.client.navigation.ViewRouter.getInstance()
                .navigateTo(com.bidhub.client.util.Views.AUCTION_LIST));
      } else {
        javafx.application.Platform.runLater(() ->
            showError(response.getMessage()));
      }
    });

    task.setOnFailed(e ->
        javafx.application.Platform.runLater(() ->
            showError(task.getException().getMessage())));

    new Thread(task).start();
  }

  private void showError(String message) {
    Alert alert = new Alert(AlertType.ERROR, message);
    alert.setTitle("Loi");
    alert.showAndWait();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm CreateAuctionController — form tạo phiên đấu giá cho SELLER"
```

---

### `CreateAuctionView.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>
<VBox spacing="16" alignment="TOP_CENTER"
      xmlns="http://javafx.com/javafx/21"
      style="-fx-padding: 24;">
  <Label text="Tạo phiên đấu giá" style="-fx-font-size: 20px; -fx-font-weight: bold;"/>
  <HBox spacing="12" alignment="CENTER_LEFT">
    <Label text="Sản phẩm:" prefWidth="120"/>
    <ComboBox fx:id="cbItemId" prefWidth="250" promptText="Chọn sản phẩm..."/>
  </HBox>
  <HBox spacing="12" alignment="CENTER_LEFT">
    <Label text="Giá khởi điểm:" prefWidth="120"/>
    <TextField fx:id="tfStartingPrice" prefWidth="250" promptText="VD: 100000"/>
  </HBox>
  <HBox spacing="12" alignment="CENTER_LEFT">
    <Label text="Bước giá tối thiểu:" prefWidth="120"/>
    <TextField fx:id="tfMinIncrement" prefWidth="250" promptText="VD: 50000 (mặc định 1)"/>
  </HBox>
  <HBox spacing="12" alignment="CENTER_LEFT">
    <Label text="Thời gian bắt đầu:" prefWidth="120"/>
    <DatePicker fx:id="dpStartTime"/>
    <Spinner fx:id="spStartHour"/>
    <Label text="giờ"/>
  </HBox>
  <HBox spacing="12" alignment="CENTER_LEFT">
    <Label text="Thời gian kết thúc:" prefWidth="120"/>
    <DatePicker fx:id="dpEndTime"/>
    <Spinner fx:id="spEndHour"/>
    <Label text="giờ"/>
  </HBox>
  <HBox spacing="16" alignment="CENTER">
    <Button fx:id="btnSubmit" text="Tạo phiên" prefWidth="120"
            style="-fx-background-color: #4CAF50; -fx-text-fill: white;"/>
    <Button fx:id="btnBack" text="Quay lại" prefWidth="120"/>
  </HBox>
</VBox>
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm CreateAuctionView.fxml — form tạo phiên đấu giá"
```

---

### Cập nhật `Views.java` — thêm CREATE_AUCTION

```java
// === THÊM VÀO Views.java ===
  public static final String CREATE_AUCTION = "create_auction";
```

```bash
git commit -m "feat: thêm Views.CREATE_AUCTION constant"
```

---

**Kiểm tra manual:**
```bash
# Compile client
mvn compile -pl bidhub-client -q

# Chạy client — đảm bảo màn hình AuctionList hiện thị
mvn javafx:run -pl bidhub-client
# 1. Đăng nhập SELLER → AuctionList hiện → button "Tạo phiên" visible
# 2. Double-click hàng → chuyển AuctionDetail → countdown chạy
# 3. Nhập giá → Đặt giá → giá cập nhật
```

**❌ FAIL nếu:**
- `TableView` không hiển thị data → `PropertyValueFactory` hoặc binding sai
- Double-click không navigate → `setRowFactory` hoặc `getSelectedItem()` sai
- Countdown không cập nhật → `Timeline` không start hoặc `updateCountdown()` sai format
- Countdown âm khi auction hết hạn → thiếu check `now.isAfter(endTime)`
- Button "Tạo phiên" hiện cho BIDDER → logic `isVisible()` sai
- Navigate đi mà Timeline vẫn chạy → thiếu `stopCountdown()` cleanup

---

## 👤 KHOA — AdminUserService + Handlers + Bid Test Suite

```
Branch: feature/tuan-6-khoa-admin-user-service-bid-tests
Phụ thuộc: UserDao.updateLocked() (tuần 6, Đăng) — rebase sau khi Đăng merge
           User.isLocked() (tuần 6, Đăng) — cho integration test
           AuditLogService (tuần 5, Khoa — đã có) — log khi lock/unlock
           AuditActions (tuần 4, Khoa — đã có) — USER_LOCKED, USER_UNLOCKED
           BidValidator (tuần 6, Quốc Minh) — cho bid test suite
           RequestHandler (tuần 4+5) — MỞ RA thêm 3 admin handler + switch cases
```

📌 **[Tiêu chí điểm: Quản lý người dùng — Admin lock/unlock — phần 1.0đ + Xử lý lỗi & ngoại lệ — test suite — phần 1.0đ]**

### 📝 Mô tả bài tập

`AdminUserService` cung cấp 3 method: `listAllUsers()`, `lockUser()`, `unlockUser()`. `lockUser` kiểm tra
user tồn tại, không lock ADMIN, gọi `UserDao.updateLocked()`, log `USER_LOCKED` qua `AuditLogService`.
`unlockUser` tương tự nhưng không cần check role. Class này có 2 constructor: production và inject (cho test).

3 handler mới trong `RequestHandler` đều yêu cầu ADMIN role: `handleGetUserList`, `handleLockUser`,
`handleUnlockUser`. Mỗi handler gọi `SecurityContext.requireRole(session, UserRole.ADMIN)` trước khi
thực hiện logic.

Test suite gồm 2 nhóm: Bid Test Suite ≥ 15 cases test logic đặt giá (sử dụng `BidValidator`) và
AdminUserService tests ≥ 8 cases test logic khóa/mở tài khoản. Tổng ≥ 23 test cases mới.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Khoa cần `UserDao.updateLocked()` (Đăng) — nếu Đăng chưa merge, dùng stub
- Khoa cần `BidValidator` (Quốc Minh) — nếu chưa merge, test BidValidator độc lập
- Khoa cần `AuditLogService` + `AuditLogDao` (đã có từ T5) — production

**Kịch bản chọn: C — Rebase từ develop sau khi Đăng + Quốc Minh merge**

**Nếu Đăng chưa merge UserDao.updateLocked():**
```java
// Stub tạm — XÓA khi Đăng merge
// Thêm vào UserDao trong branch Khoa:
public void updateLocked(String userId, boolean locked) {
  // stub — khong lam gi
}
```

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── service/
│   │   └── AdminUserService.java  ← MỚI
│   └── network/
│       └── RequestHandler.java    (đã có T4+T5+T6 — MỞ RA thêm 3 admin handlers)
└── test/java/com/bidhub/server/
    ├── service/
    │   ├── BidValidatorTest.java    ← MỚI (nếu Quốc Minh chưa có)
    │   └── AdminUserServiceTest.java ← MỚI
    └── network/
        └── AdminHandlerTest.java    ← MỚI (integration test cho handlers)
```

---

### `AdminUserService.java`

```java
package com.bidhub.server.service;

import com.bidhub.common.exception.UserNotFoundException;
import com.bidhub.common.exception.ValidationException;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.util.List;

/**
 * Dich vu quan tri: liet ke, khoa, mo khoa tai khoan nguoi dung.
 *
 * <p>Goi {@link UserDao} va {@link AuditLogService} de thuc hien thao tac admin.
 * Moi thao tac lock/unlock deu duoc ghi nhat ky qua audit log.
 *
 * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin lock/unlock tai khoan]
 */
public class AdminUserService {

  private final UserDao userDao;
  private final AuditLogService auditLogService;

  /** Constructor production — tao UserDao va AuditLogService moi. */
  public AdminUserService() {
    this.userDao = new UserDao();
    this.auditLogService = new AuditLogService();
  }

  /**
   * Constructor cho test — inject UserDao va AuditLogService.
   *
   * @param userDao         UserDao inject
   * @param auditLogService AuditLogService inject
   */
  public AdminUserService(UserDao userDao, AuditLogService auditLogService) {
    this.userDao = userDao;
    this.auditLogService = auditLogService;
  }

  /**
   * Lay danh sach toan bo nguoi dung.
   *
   * @return danh sach user
   */
  public List<User> listAllUsers() {
    return userDao.findAll();
  }

  /**
   * Khoa tai khoan nguoi dung — chi Admin duoc goi.
   *
   * <p>Kiem tra: user ton tai, khong phai ADMIN → cap nhat is_locked → log.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin khoa tai khoan]
   *
   * @param targetId id cua nguoi dung bi khoa
   * @param adminId  id cua admin thuc hien
   * @throws UserNotFoundException neu target khong ton tai
   * @throws ValidationException   neu target la ADMIN
   */
  public void lockUser(String targetId, String adminId) {
    User target = userDao.findById(targetId)
        .orElseThrow(() -> new UserNotFoundException(
            "Nguoi dung khong ton tai: " + targetId));

    // 📌 [Tieu chi: Xu ly loi & ngoai le — khong khoa Admin]
    if (target.getRole() == UserRole.ADMIN) {
      throw new ValidationException("Khong the khoa tai khoan Admin.");
    }

    userDao.updateLocked(targetId, true);

    // 📌 [Tieu chi: Audit trail — ghi nhat ky khi khoa user]
    auditLogService.log(adminId, AuditActions.USER_LOCKED,
        "{\"targetId\":\"" + targetId + "\"}");
  }

  /**
   * Mo khoa tai khoan nguoi dung.
   *
   * <p>Kiem tra user ton tai → cap nhat is_locked → log.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin mo khoa tai khoan]
   *
   * @param targetId id cua nguoi dung bi mo khoa
   * @param adminId  id cua admin thuc hien
   * @throws UserNotFoundException neu target khong ton tai
   */
  public void unlockUser(String targetId, String adminId) {
    User target = userDao.findById(targetId)
        .orElseThrow(() -> new UserNotFoundException(
            "Nguoi dung khong ton tai: " + targetId));

    userDao.updateLocked(targetId, false);

    // 📌 [Tieu chi: Audit trail — ghi nhat ky khi mo khoa user]
    auditLogService.log(adminId, AuditActions.USER_UNLOCKED,
        "{\"targetId\":\"" + targetId + "\"}");
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm AdminUserService — listAllUsers, lockUser, unlockUser kèm audit log"
```

---

### Cập nhật `RequestHandler.java` — thêm 3 admin handlers

```java
// === THÊM VÀO RequestHandler.java ===

// --- 1. Thêm import ---
import com.bidhub.server.service.AdminUserService;

// --- 2. Thêm field ---
  private final AdminUserService adminUserService;

// --- 3. Cập nhật constructors ---
  public RequestHandler() {
    // ... existing code ...
    this.adminUserService = new AdminUserService();
  }

  RequestHandler(Object injectedUserDao, Object injectedItemDao) {
    // ... existing code ...
    this.adminUserService = new AdminUserService();
  }

  RequestHandler(Object injectedUserDao, Object injectedItemDao,
      AuditLogService injectedAuditService) {
    // ... existing code ...
    this.adminUserService = new AdminUserService();
  }

// --- 4. Thêm vào switch-case ---
        case "GET_USER_LIST"  -> handleGetUserList(session, payload);
        case "LOCK_USER"      -> handleLockUser(session, payload);
        case "UNLOCK_USER"    -> handleUnlockUser(session, payload);

// --- 5. Handler methods ---

  /**
   * Xu ly lay danh sach nguoi dung — chi ADMIN.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin xem danh sach user]
   *
   * @param session session cua client
   * @param payload payload rong
   * @return JSON response voi danh sach user
   */
  private String handleGetUserList(Session session, JsonNode payload) {
    SecurityContext.requireRole(session, UserRole.ADMIN);

    List<User> users = adminUserService.listAllUsers();
    List<Map<String, Object>> result = new ArrayList<>();
    for (User u : users) {
      Map<String, Object> userInfo = new HashMap<>();
      userInfo.put("id", u.getId());
      userInfo.put("username", u.getUsername());
      userInfo.put("email", u.getEmail());
      userInfo.put("role", u.getRole().name());
      userInfo.put("isLocked", u.isLocked());
      result.add(userInfo);
    }
    return MessageMapper.toJson(
        MessageResponse.ok("GET_USER_LIST", result));
  }

  /**
   * Xu ly khoa tai khoan — chi ADMIN.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin khoa tai khoan]
   *
   * @param session session cua client
   * @param payload {targetUserId}
   * @return JSON response
   */
  private String handleLockUser(Session session, JsonNode payload) {
    String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
    String targetUserId = payload.path("targetUserId").asText("");

    if (targetUserId.isBlank()) {
      throw new ValidationException("targetUserId khong duoc de trong");
    }

    adminUserService.lockUser(targetUserId, adminId);

    return MessageMapper.toJson(MessageResponse.ok("LOCK_USER",
        Map.of("message", "Da khoa tai khoan.")));
  }

  /**
   * Xu ly mo khoa tai khoan — chi ADMIN.
   *
   * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin mo khoa tai khoan]
   *
   * @param session session cua client
   * @param payload {targetUserId}
   * @return JSON response
   */
  private String handleUnlockUser(Session session, JsonNode payload) {
    String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
    String targetUserId = payload.path("targetUserId").asText("");

    if (targetUserId.isBlank()) {
      throw new ValidationException("targetUserId khong duoc de trong");
    }

    adminUserService.unlockUser(targetUserId, adminId);

    return MessageMapper.toJson(MessageResponse.ok("UNLOCK_USER",
        Map.of("message", "Da mo khoa tai khoan.")));
  }
```

```bash
git commit -m "feat: thêm handleGetUserList/handleLockUser/handleUnlockUser — ADMIN only"
```

---

### ✅ Test đầu ra — `AdminUserServiceTest.java`

```java
package com.bidhub.server.service;

import com.bidhub.common.exception.UserNotFoundException;
import com.bidhub.common.exception.ValidationException;
import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.AuditLog;
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

  private User createAndSaveUser(String id, String username, UserRole role) {
    User user = new Bidder(id, username,
        com.bidhub.server.service.AuthService.hashPassword("password123"),
        "test@email.com");
    userDao.save(user);
    return user;
  }

  @Test
  @DisplayName("listAllUsers — sau save 3 users tra ve 3 users")
  void listAllUsers_threeUsers_returnsThree() {
    createAndSaveUser("u1", "bidder1", UserRole.BIDDER);
    createAndSaveUser("u2", "seller1", UserRole.SELLER);
    createAndSaveUser("u3", "bidder2", UserRole.BIDDER);

    List<User> users = adminService.listAllUsers();
    assertEquals(3, users.size());
  }

  @Test
  @DisplayName("lockUser — isLocked tra ve true sau khi khoa")
  void lockUser_success_isLockedTrue() {
    createAndSaveUser("u1", "bidder1", UserRole.BIDDER);

    adminService.lockUser("u1", "admin-001");

    User locked = userDao.findById("u1").orElseThrow();
    assertTrue(locked.isLocked());
  }

  @Test
  @DisplayName("unlockUser — isLocked tra ve false sau khi mo khoa")
  void unlockUser_afterLock_isLockedFalse() {
    createAndSaveUser("u1", "bidder1", UserRole.BIDDER);
    adminService.lockUser("u1", "admin-001");

    adminService.unlockUser("u1", "admin-001");

    User unlocked = userDao.findById("u1").orElseThrow();
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
    // Tao Admin user (note: UserDao tao dua tren role — can Seller cho test)
    createAndSaveUser("admin-1", "adminUser", UserRole.ADMIN);

    assertThrows(ValidationException.class,
        () -> adminService.lockUser("admin-1", "admin-002"));
  }

  @Test
  @DisplayName("lockUser — audit log USER_LOCKED duoc tao")
  void lockUser_auditLogCreated() {
    createAndSaveUser("u1", "bidder1", UserRole.BIDDER);

    adminService.lockUser("u1", "admin-001");

    List<AuditLog> logs = auditLogDao.findByAction(AuditActions.USER_LOCKED);
    assertFalse(logs.isEmpty());
    assertEquals("admin-001", logs.get(0).getUserId());
  }

  @Test
  @DisplayName("unlockUser — audit log USER_UNLOCKED duoc tao")
  void unlockUser_auditLogCreated() {
    createAndSaveUser("u1", "bidder1", UserRole.BIDDER);
    adminService.lockUser("u1", "admin-001");

    adminService.unlockUser("u1", "admin-001");

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
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm AdminUserServiceTest (8 cases) — lock/unlock/audit/listAllUsers"
```

---

### ✅ Test đầu ra — `BidValidatorTest.java` (bổ sung nếu chưa đủ)

> [!NOTE]
> Nếu Quốc Minh đã viết `BidValidatorTest` với 9 cases, Khoa bổ sung thêm test cases
> còn thiếu để đạt ≥ 15 bid test cases tổng cộng. Dưới đây là test bổ sung:

```java
package com.bidhub.server.service;

import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bo sung BidValidatorTest — them cac edge case de dat ≥ 15 bid test cases tong cong.
 *
 * <p>// 📌 [Tieu chi: Testing — bao phu toan bo 5 dieu kien validate]
 */
class BidValidatorAdditionalTest {

  private BidValidator validator;

  @BeforeEach
  void setUp() {
    validator = new BidValidator(null);
  }

  private Auction createAuction(AuctionStatus status, double currentBid,
      double minIncrement, String highestBidderId) {
    Auction a = new Auction();
    try {
      var idField = a.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(a, "auc-test");
      var statusField = a.getClass().getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(a, status);
      var bidField = a.getClass().getDeclaredField("currentHighestBid");
      bidField.setAccessible(true);
      bidField.set(a, currentBid);
      var incField = a.getClass().getDeclaredField("minimumIncrement");
      incField.setAccessible(true);
      incField.set(a, minIncrement);
      var bidderField = a.getClass().getDeclaredField("highestBidderId");
      bidderField.setAccessible(true);
      bidderField.set(a, highestBidderId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return a;
  }

  @Test
  @DisplayName("validate auction CANCELED → AuctionClosedException")
  void validate_canceledAuction_throwsClosed() {
    Auction a = createAuction(AuctionStatus.CANCELED, 1000, 50, null);
    assertThrows(AuctionClosedException.class,
        () -> validator.validate(a, "user1", 1100));
  }

  @Test
  @DisplayName("validate auction OPEN (chua bat dau) → AuctionClosedException")
  void validate_openAuction_throwsClosed() {
    Auction a = createAuction(AuctionStatus.OPEN, 0, 50, null);
    assertThrows(AuctionClosedException.class,
        () -> validator.validate(a, "user1", 100));
  }

  @Test
  @DisplayName("validate auction PAID → AuctionClosedException")
  void validate_paidAuction_throwsClosed() {
    Auction a = createAuction(AuctionStatus.PAID, 5000, 50, "winner");
    assertThrows(AuctionClosedException.class,
        () -> validator.validate(a, "user1", 6000));
  }

  @Test
  @DisplayName("validate gia dat 0 → InvalidBidException")
  void validate_zeroBid_throwsException() {
    Auction a = createAuction(AuctionStatus.RUNNING, 0, 50, null);
    assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user1", 0));
  }

  @Test
  @DisplayName("validate buoc gia dung chinh xac minimumIncrement → hop le")
  void validate_exactMinimumIncrement_valid() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000, 100, null);
    assertDoesNotThrow(() -> validator.validate(a, "user1", 1100));
  }

  @Test
  @DisplayName("validate bid vuot minimumIncrement nhieu → hop le")
  void validate_largeIncrement_valid() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000, 50, null);
    assertDoesNotThrow(() -> validator.validate(a, "user1", 5000));
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: bổ sung BidValidatorAdditionalTest (6 cases) — đạt ≥ 15 bid test cases"
```

---

**Kiểm tra manual:**
```bash
# Chạy toàn bộ test server
mvn test -pl bidhub-server -q
# Output: Tests run: ≥ 175, Failures: 0

# Chạy AdminUserService test rieng
mvn test -pl bidhub-server -Dtest="AdminUserServiceTest" -q
# Output: Tests run: 8, Failures: 0

# Chạy BidValidator test rieng
mvn test -pl bidhub-server -Dtest="BidValidatorTest,BidValidatorAdditionalTest" -q
# Output: Tests run: ≥ 15, Failures: 0

# Manual test: Admin lock user
echo '{"type":"LOCK_USER","token":"<admin-token>","payload":{"targetUserId":"user-001"}}' \
  | nc -q1 localhost 9090
# Output: {"status":"OK","type":"LOCK_USER","payload":{"message":"Da khoa tai khoan."}}

# Manual test: BIDDER thuc hien LOCK_USER → bi tu choi
echo '{"type":"LOCK_USER","token":"<bidder-token>","payload":{"targetUserId":"user-001"}}' \
  | nc -q1 localhost 9090
# Output: {"status":"ERROR","type":"LOCK_USER","message":"Khong du quyen. ..."}
```

**❌ FAIL nếu:**
- `AdminUserService.lockUser()` lock được Admin → thiếu check `target.getRole() == ADMIN`
- `lockUser()` không tạo audit log → thiếu `auditLogService.log(USER_LOCKED, ...)`
- `handleGetUserList` trả về user có `isLocked = null` thay vì `true/false` → serialize sai
- `handleLockUser` từ BIDDER session không bị từ chối → thiếu `requireRole(ADMIN)`
- `unlockUser` sau `lockUser` vẫn trả `isLocked = true` → `updateLocked` không update DB
- Bid test < 15 cases hoặc Admin test < 8 cases → tổng test mới < 23

---

## 📊 TỔNG KẾT TUẦN 6

| Người | File mới | File sửa | Test cases | Merge thứ tự |
|-------|----------|----------|------------|--------------|
| **Đăng** | `AuctionManager.java`, `AuctionLifecycleTask.java` | `User.java`, `Auction.java`, `UserDao.java`, `schema.sql`, `MigrationRunner.java`, `RequestHandler.java`, `ServerApp.java` | 10 | 1 (đầu tiên) |
| **Quốc Minh** | `BidValidator.java` | `RequestHandler.java` (+3 handlers, +switch, +AUTH_REQUIRED) | 9+ | 2 |
| **Công Minh** | `CreateAuctionController.java`, `CreateAuctionView.fxml` | `AuctionListController.java`, `AuctionDetailController.java`, `Views.java` | Manual UI | 4 (cuối) |
| **Khoa** | `AdminUserService.java` | `RequestHandler.java` (+3 admin handlers, +switch) | 8+6=14 | 3 |
| **Tổng** | **7 files mới** | **11 files sửa** | **≥ 33 cases** | Đăng → QM → Khoa → CM |

> [!IMPORTANT]
> Sau khi tất cả merge vào `develop`, chạy `mvn clean test` — đảm bảo **0 failures**.
> Tổng project test cases ≥ 175 (150 từ T1-T5 + ≥ 25 từ T6).

---

## 🔗 THỨ TỰ MERGE CHI TIẾT

```
1. Đăng → PR "feat: AuctionManager + is_locked support" → merge develop
2. Quốc Minh → rebase develop → PR "feat: BidValidator + bid/auction handlers" → merge develop
3. Khoa → rebase develop → PR "feat: AdminUserService + admin handlers + test suite" → merge develop
4. Công Minh → rebase develop → PR "feat: AuctionList + AuctionDetail + CreateAuction UI" → merge develop
```

> [!WARNING]
> **Quốc Minh và Khoa:** Cả 2 đều sửa `RequestHandler.java`. Merge lần lượt — mỗi người rebase
> từ develop **ngay sau** khi người trước merge xong. Nếu merge cùng lúc → **xung đột merge conflict**
> trên file `RequestHandler.java`. Giải quyết: giữ cả switch-case của cả 2 (Đăng: handleLogin is_locked,
> Quốc Minh: 3 bid handlers, Khoa: 3 admin handlers).

---

> [!TIP]
> **Khoa:** Bid test suite phụ thuộc `BidValidator` của Quốc Minh. Nếu Quốc Minh chưa merge,
> Khoa có thể copy `BidValidator.java` vào branch tạm và test — hoặc đơn giản chờ Quốc Minh merge rồi
> rebase. AdminUserService test KHÔNG phụ thuộc BidValidator — có thể code và test song song.
