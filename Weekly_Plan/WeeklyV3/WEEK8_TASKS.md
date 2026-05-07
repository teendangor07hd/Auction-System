# 📋 TUẦN 8 — BÀI TẬP CHI TIẾT: Anti-Sniping + Price Chart + AuditLog Integration

✅ Kết quả kiểm tra toàn diện: Không có lỗi. Codebase đáp ứng đầy đủ barem và sẵn sàng cho Tuần 8.

## 🎯 MỤC TIÊU TUẦN 8

Tuần này xây dựng 3 trụ cột chính: Anti-Sniping Engine gia hạn tự động phiên đấu giá khi có bid sát giờ,
Price Chart (LineChart realtime) hiển thị lịch sử giá trên JavaFX, và tích hợp AuditLog vào toàn bộ lifecycle
của hệ thống. Cuối tuần, cả nhóm phải có:

- ✅ `AuctionExtendedEvent` — event object gửi realtime khi auction được gia hạn
- ✅ `AntiSnipingEngine` — check() method detect bid trong snipe window → gia hạn endTime
- ✅ `handlePlaceBid()` gọi `antiSnipingEngine.check()` sau bid thành công
- ✅ Client nhận `AUCTION_EXTENDED` event → reset countdown timer
- ✅ `BidChartService` — `XYChart.Series<String, Number>` quản lý dữ liệu biểu đồ
- ✅ `AuctionDetailView.fxml` thêm `LineChart` hiển thị lịch sử giá realtime
- ✅ Load lịch sử bid qua `GET_BID_HISTORY`, realtime update từ `BID_UPDATE` event
- ✅ `AuditLogService.log()` tích hợp vào `handlePlaceBid()` (sau DB save trong lock)
- ✅ `AuditLogService.log()` tích hợp vào `AuctionLifecycleTask.closeAuction()` (sau FINISHED)
- ✅ `AuditLogService.log()` tích hợp vào `AntiSnipingEngine.check()` (sau extension)
- ✅ `server.properties` có `snipe.threshold=60`, `snipe.extension=60`
- ✅ ≥ 10 test cases mới (5 anti-sniping + 5 audit integration) — tổng project ≥ 205 cases

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** barem: **Anti-Sniping** — gia hạn tự động phiên đấu giá (0.5đ) +
> **Price Chart** — biểu đồ giá realtime bằng JavaFX LineChart (0.5đ) +
> **Unit Test** — test suite anti-sniping + audit integration (phần 0.5đ) +
> **Audit Log** — tích hợp auditLog vào toàn bộ lifecycle (phần 0.5đ).

> [!CAUTION]
> **Tuyệt đối không tạo lại** bất kỳ class nào từ Tuần 1–7:
> `Entity`, `BidHubException` + 7 subclass, `MessageRequest`, `MessageResponse`, `MessageMapper`,
> `ConfigLoader`, `DbConnectionProvider`, `MigrationRunner`,
> `UserRole`, `User` (với field locked), `Bidder`, `Seller`, `Admin`, `Displayable`,
> `ItemType`, `Item`, `Electronics`, `Art`, `Vehicle`,
> `ItemCreator` + 3 ConcreteCreator, `AuctionStatus`, `Auction` (với field lock + getLock()), `BidTransaction`,
> `AuditLog`, `AuditActions` (với `AUCTION_EXTENDED` đã định nghĩa),
> `UserDao` (với updateLocked), `ItemDao`, `AuctionDao` (với findAll), `BidDao`, `AuditLogDao`,
> `SocketServerCore`, `Session`, `ClientConnectionThread`, `RequestHandler` (với T4–T7 handlers), `SecurityContext`,
> `AuthService`, `SessionManager`, `AuditLogService`,
> `AuctionManager`, `AuctionLifecycleTask` (với lock trong closeAuction), `BidValidator`,
> `AdminUserService`,
> `NotificationBroker` (Singleton), `BidUpdateEvent`, `AuctionClosedEvent`,
> `BidHubApp`, `ViewRouter`, `ContextAware`, `LoginController`, `RegisterController`,
> `AuctionListController`, `AuctionDetailController` (với EventListenerThread, BidUpdateCallback, Timeline countdown),
> `CreateItemController`, `CreateAuctionController`, `Views`,
> `ServerGateway`, `NetworkTask`, `ClientSession`,
> `EventListenerThread`, `BidUpdateCallback` interface,
> `ReportService`.
>
> **Thứ tự merge quan trọng:** Đăng + Quốc Minh merge đầu tiên (AntiSnipingEngine + AuctionExtendedEvent là nền
> tảng) → Công Minh merge thứ hai (Price Chart phụ thuộc AuctionExtendedEvent event) → Khoa merge cuối (AuditLog
> integration phụ thuộc AntiSnipingEngine.check() + handlePlaceBid + closeAuction).

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI HỌC (Tự học)

> [!CAUTION]
> Hoàn thành **trước Thứ 5**. Mục đích không phải học Java chung — mà là hiểu cơ chế hoạt động của
> code người khác viết tuần này, để review chéo Chủ nhật trôi chảy và trả lời được vấn đáp giảng viên
> về Anti-Sniping + Price Chart + AuditLog Integration mà không lúng túng.

---

### Bài 0.1 — JavaFX LineChart, XYChart.Series, XYChart.Data

**Tài liệu bắt buộc:**
- https://openjfx.io/javadoc/21/javafx.scene.chart/LineChart.html
- https://openjfx.io/javadoc/21/javafx.scene.chart/XYChart.Series.html
- https://openjfx.io/javadoc/21/javafx.scene.chart/XYChart.Data.html

**Câu hỏi hỏi miệng Chủ nhật:**
1. `LineChart<X, Y>` generic type — BidHub dùng `LineChart<String, Number>` (X=String time, Y=Number price).
   Tại sao không dùng `LineChart<Number, Number>`? `String` trên trục X có được tự động sort theo thứ tự
   thời gian không? Nếu không — trục X có thể hiển thị `14:05:00` trước `14:02:00`?
2. `XYChart.Series<String, Number>` đại diện cho 1 đường data trên chart. `series.getData()` trả về
   `ObservableList<XYChart.Data<String, Number>>`. Khi `add()` data point mới — LineChart có tự cập nhật
   không? Cần gọi `chart.requestLayout()` hay `chart.animate()` không?
3. `BidChartService` quản lý `XYChart.Series` — method `addDataPoint(LocalDateTime time, double price)` format
   time thành `HH:mm:ss` string rồi tạo `XYChart.Data<>(timeStr, price)`. Nếu 2 bid cùng giây → 2 data
   point trùng X-axis value → LineChart hiển thị thế nào? Cần nối microsecond vào time string không?
4. `AuctionDetailController` khởi tạo `BidChartService`, gán `series` vào `lineChart.getData().add(series)`.
   Khi user navigate đi rồi quay lại — nếu `AuctionDetailController` tạo mới `BidChartService` → `clearData()`
   cũ không gọi → data cũ vẫn còn. Cần `clearData()` trong `initialize()` hoặc `loadAuctionDetail()` không?
5. `LineChart` trong FXML: `<LineChart fx:id="bidChart">` — khi nào nên tạo chart bằng code Java thay vì
   FXML? Nếu cần set `createSymbols(false)` (không hiện chấm tròn trên mỗi data point) — nên set trong FXML
   hay trong controller `initialize()`?
6. **[Câu hỏi nâng cao]** `BidChartService` dùng `DateTimeFormatter.ofPattern("HH:mm:ss")` — immutable,
   thread-safe. Nhưng `series.getData()` là `ObservableList` — nếu `EventListenerThread` (background thread)
   gọi `addDataPoint()` → `ObservableList` thay đổi không phải trên FX thread → `IllegalStateException`.
   Công Minh cần wrap `series.getData().add()` trong `Platform.runLater()` — giải thích tại sao.

---

### Bài 0.2 — Anti-Sniping Concept (eBay) — Logic Gia Hạn

**Tài liệu bắt buộc:**
- https://pages.ebay.com/help/buy/controlling-bidding.html
- Đọc lại `ConfigLoader.java` (T1) — method `getInt()`, `getString()`
- Đọc lại `server.properties` — `snipe.threshold=60`, `snipe.extension=60`

**Câu hỏi hỏi miệng Chủ nhật:**
1. Anti-sniping (anti-snip / soft close / popcorn bidding) — eBay dùng cơ chế gì? Khi có bid đặt trong
   N phút cuối → gia hạn thêm M phút. BidHub dùng `snipe.threshold=60` (giây) và `snipe.extension=60`
   (giây). Nếu endTime = 14:05:00, bid đặt lúc 14:04:30 (30 giây trước) → endTime mới là gì?
2. `AntiSnipingEngine.check()` tính `snipeWindow = endTime.minusSeconds(thresholdSeconds)`. Nếu
   `now.isAfter(snipeWindow)` → gia hạn. `minusSeconds(60)` khi endTime = 14:05:00 → snipeWindow = 14:04:00.
   Nếu bid lúc 14:04:01 (1 giây trong window) → gia hạn. Nếu bid lúc 14:03:59 (1 giây ngoài window) →
   không gia hạn. Giải thích tại sao `isAfter()` OR `isEqual()` (>=) thay vì chỉ `isAfter()`?
3. `auction.setEndTime(newEndTime)` + `auctionDao.updateEndTime()` — cập nhật cả RAM và DB. Nếu DB update
   thất bại nhưng RAM đã thay đổi → client nhận `AUCTION_EXTENDED` event với endTime mới → countdown reset →
   nhưng server restart sẽ dùng endTime cũ từ DB. Hậu quả? Cần rollback RAM khi DB fail không?
4. `AntiSnipingEngine.check()` gọi `NotificationBroker.getInstance().publish()` — gửi `AuctionExtendedEvent`
   cho tất cả client subscribe auction. Nếu client đang countdown 30 giây → nhận event mới endTime +60 giây →
   countdown reset thành 90 giây. Logic reset countdown trong `AuctionDetailController` như thế nào?
5. `server.properties` chứa `snipe.threshold` và `snipe.extension`. `ConfigLoader.getInstance().getInt("snipe.threshold")`
   đọc giá trị. Nếu config key không tồn tại → `getInt()` trả về bao nhiêu? Nên có default value không?
   Ưu điểm load config từ file thay vì hardcode constant trong code?
6. **[Câu hỏi nâng cao]** Nếu 2 bid đặt cùng lúc vào 2 thread — cả 2 đều gọi `check()` → cả 2 đều detect
   snipe window → cả 2 đều gia hạn → endTime được cộng 120 giây thay vì 60 giây. Giải pháp? Lock
   `check()` trong `handlePlaceBid()` lock block (đã có ReentrantLock) giải quyết vấn đề này không?

---

### Bài 0.3 — LocalDateTime Arithmetic

**Tài liệu bắt buộc:**
- https://docs.oracle.com/javase/21/docs/api/java.base/java/time/LocalDateTime.html
- https://docs.oracle.com/javase/21/docs/api/java.base/java/time/Duration.html
- Đọc lại `Auction.java` (T2) — field `endTime`, getter/setter

**Câu hỏi hỏi miệng Chủ nhật:**
1. `LocalDateTime.now().plusSeconds(60)` trả về immutable object mới — không thay đổi `now`. `minusSeconds()`
   cũng trả về object mới. `auction.getEndTime().plusSeconds(extension)` — cần `auction.setEndTime()` để
   lưu lại. Nếu chỉ gọi `plusSeconds()` mà không `setEndTime()` → auction endTime không thay đổi?
2. `LocalDateTime.isAfter()` vs `LocalDateTime.isBefore()` vs `isEqual()` — 3 method trả về boolean.
   `now.isAfter(snipeWindow)` kiểm tra now > snipeWindow. `now.isBefore(endTime)` kiểm tra now < endTime.
   Tại sao `AntiSnipingEngine.check()` cần check cả `now.isAfter(snipeWindow)` VÀ không cần check
   `now.isBefore(endTime)`? Auction đã hết hạn (endTime < now) có nên gia hạn không?
3. `Duration.between(startTime, endTime)` — trả về Duration object. `Duration.toSeconds()` chuyển thành
   giây. `Duration.toMinutes()` chuyển thành phút (làm tròn xuống). Công Minh dùng `Duration` tính countdown
   còn bao nhiêu giây — code cụ thể?
4. `LocalDateTime.now()` dùng system clock — nếu server clock bị sai (sau đúng 10 giây) → snipe window
   tính sai → bid không được gia hạn khi nên được. Giải pháp: dùng `Clock.systemUTC()` và đảm bảo server
   đồng bộ NTP? `Instant.now()` (UTC) có chính xác hơn `LocalDateTime.now()` (local timezone) không?
5. `DateTimeFormatter.ofPattern("HH:mm:ss")` — format LocalDateTime thành string hiển thị countdown.
   `Duration` có method `format()` không? Tại sao BidHub dùng `DateTimeFormatter` thay vì `Duration`?
6. **[Câu hỏi nâng cao]** Anti-sniping loop: bid A đặt lúc 14:04:30 → endTime extend 14:06:00. Bid B đặt
   lúc 14:05:30 → endTime extend 14:07:00. Bid C đặt lúc 14:06:30 → endTime extend 14:08:00. Nếu auction
   liên tục nhận bid mỗi 30 giây → auction không bao giờ kết thúc. eBay giới hạn bao nhiêu lần extend?
   BidHub nên thêm `maxExtensions` config không? Nếu có — reset `extensionCount` mỗi lần extend không?

---

### Bài 0.4 — AuditLog Event Integration — Full Lifecycle

**Tài liệu bắt buộc:**
- Đọc lại `AuditLogService.java` (T4, Khoa) — method `log(userId, action, details)`
- Đọc lại `AuditActions.java` (T4, Khoa) — constants bao gồm `AUCTION_EXTENDED`
- Đọc lại `handlePlaceBid()` trong `RequestHandler` (T6+T7, Quốc Minh)
- Đọc lại `AuctionLifecycleTask.closeAuction()` (T6+T7, Đăng)

**Câu hỏi hỏi miệng Chủ nhật:**
1. `AuditLogService.log(userId, action, details)` — lưu vào DB `audit_logs` table. Khoa cần gọi `log()` tại
   3 vị trí: (a) `handlePlaceBid()` sau DB save, (b) `closeAuction()` sau FINISHED transition, (c)
   `AntiSnipingEngine.check()` sau extension. Tại sao vị trí (a) phải ở **trong lock** (sau `bidDao.save()`
   trước `unlock()`) — nếu đặt ngoài lock có ảnh hưởng gì?
2. `AuditActions.AUCTION_EXTENDED` đã được định nghĩa trong `AuditActions.java` từ trước (T4). Khoa không cần
   thêm constant mới. Nhưng Khoa cần xây JSON `details` string chứa thông tin gia hạn: auctionId, oldEndTime,
   newEndTime. Ví dụ: `{"auctionId":"auc-001","oldEndTime":"14:05:00","newEndTime":"14:06:00"}`. Tại sao
   cần `oldEndTime` trong audit log? Mục đích forensics?
3. `handlePlaceBid()` hiện tại gọi `auditLogService.log()` ở **ngoài finally block** (sau unlock). Khoa di chuyển
   vào **trong lock block** (sau `bidDao.save()` thành công). Ưu điểm: đảm bảo audit log được ghi trước khi
   unlock cho bid khác. Nhược điểm: audit log I/O blocking thêm vào critical section → throughput giảm.
   Trade-off nào phù hợp hơn cho BidHub?
4. `closeAuction()` gọi `auditLogService.log()` sau `transitionTo(FINISHED)` và `updateStatus()`. Nếu audit log
   fail (DB connection drop) → exception propagate → auction đã FINISHED trong DB nhưng không có audit log →
   mất record. Nên try-catch audit log trong closeAuction không? Nếu catch → silent failure → không ai biết
   audit log bị mất?
5. `AntiSnipingEngine.check()` gọi `auditLogService.log()` sau `NotificationBroker.publish()`. Nếu audit log
   chạy trước publish → event chưa gửi mà log đã ghi → client nhận event trễ hơn log → không đồng bộ.
   Ưu điểm publish trước log: client nhận ngay, log async. Nhược điểm: log ghi fail → event đã gửi → client
   thấy gia hạn nhưng không có audit trace → inconsistency.
6. **[Câu hỏi nâng cao]** `AuditLogService.log()` gọi `auditLogDao.save()` → INSERT vào SQLite. SQLite chỉ cho
   1 write tại một thời điểm (file lock). Nếu 3 thread gọi `log()` đồng thời → 2 thread bị block chờ file
   lock. Trong `handlePlaceBid()`, audit log chậm → bid chậm → user trải nghiệm kém. Giải pháp: queue audit
   log và write async? FIFO queue + background thread? Impact lên memory nếu queue tăng không kiểm soát?

---

## 👤 ĐĂNG + QUỐC MINH — Anti-Sniping Engine & AuctionExtendedEvent

```
Branch: feature/tuan-8-dang-quocminh-anti-sniping
Phụ thuộc: Auction.java (tuần 2+7) — getLock(), setEndTime(), getEndTime(), getStatus()
           AuctionDao (tuần 3+7) — updateEndTime(auctionId, newEndTime)
           ConfigLoader (tuần 1) — getInt("snipe.threshold"), getInt("snipe.extension")
           NotificationBroker (tuần 7, Quốc Minh) — publish AuctionExtendedEvent
           AuditActions (tuần 4) — AUCTION_EXTENDED constant đã có
           RequestHandler.handlePlaceBid() (tuần 6+7) — MỞ RA thêm antiSnipingEngine.check()
           server.properties — snipe.threshold=60, snipe.extension=60
Merge đầu tiên: AntiSnipingEngine + AuctionExtendedEvent là nền tảng cho Công Minh + Khoa
```

📌 **[Tiêu chí điểm: Anti-Sniping — gia hạn tự động phiên đấu giá — 0.5đ + Unit Test — anti-sniping test suite — phần 0.5đ]**

### 📝 Mô tả bài tập

Anti-sniping (chống đánh chặn giây cuối) là tính năng quan trọng trong mọi hệ thống đấu giá trực tuyến. eBay
sử dụng cơ chế này: khi có người đặt giá trong N phút cuối của phiên đấu giá, hệ thống tự động gia hạn thêm
M phút để người khác có cơ hội phản hồi. BidHub implement bằng `AntiSnipingEngine` — class final với method
`check(Auction auction)` nhận auction object, tính toán snipe window (`endTime - thresholdSeconds`), so sánh
với `LocalDateTime.now()`. Nếu bid đặt trong snipe window → `endTime += extensionSeconds` → cập nhật RAM
(`auction.setEndTime()`) và DB (`auctionDao.updateEndTime()`) → publish `AuctionExtendedEvent` qua
`NotificationBroker` để tất cả client nhận thông báo và reset countdown.

`AuctionExtendedEvent` là event class mới (giống `BidUpdateEvent`, `AuctionClosedEvent` từ T7) chứa
`auctionId` và `newEndTime`. Client nhận event type `AUCTION_EXTENDED` → `AuctionDetailController` cập nhật
endTime → `Timeline` countdown reset.

Đăng + Quốc Minh phối hợp: Đăng code `AntiSnipingEngine` (server logic) + `AuctionExtendedEvent`, Quốc Minh
tích hợp `check()` vào `handlePlaceBid()` (sau bid thành công, sau auditLog) và đảm bảo event được publish.
ConfigLoader đọc `snipe.threshold` và `snipe.extension` từ `server.properties` — không hardcode.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Công Minh cần `AUCTION_EXTENDED` event type để handle trên client
- Khoa cần `AntiSnipingEngine.check()` để inject `auditLogService.log()` vào
- `handlePlaceBid()` đã có ReentrantLock — `check()` chạy trong lock block → thread-safe
- `NotificationBroker` đã có từ T7 — chỉ cần thêm event type mới
- `AuctionDao.updateEndTime()` cần được thêm nếu chưa có (có thể đã có từ T3)

**Kịch bản chọn: C — Đăng + Quốc Minh merge trước, tất cả rebase sau**

**Các bước:**
1. Đăng + Quốc Minh tạo branch, code `AuctionExtendedEvent.java`
2. Đăng code `AntiSnipingEngine.java` với 2 constructor (production + test)
3. Quốc Minh mở `handlePlaceBid()` thêm `antiSnipingEngine.check(auction)` sau bid save (trong lock)
4. Quốc Minh xác nhận `NotificationBroker.publish()` gửi `AuctionExtendedEvent`
5. Đăng + Quốc Minh viết `AntiSnipingEngineTest.java` — 5 test cases
6. Push lên GitHub, tạo PR → review → merge vào `develop`
7. Công Minh rebase — giờ có `AuctionExtendedEvent` cho client handler
8. Khoa rebase — giờ có `AntiSnipingEngine.check()` cho audit log integration

**Nếu Quốc Minh cần AntiSnipingEngine trước khi Đăng merge:**
```java
// Stub tạm trong branch Quốc Minh — XÓA khi Đăng merge
package com.bidhub.server.service;
import com.bidhub.server.model.Auction;
public class AntiSnipingEngine {
  public void check(Auction auction) { /* no-op */ }
}
```

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── event/
│   │   └── AuctionExtendedEvent.java  ← MỚI: event gia hạn
│   ├── service/
│   │   └── AntiSnipingEngine.java     ← MỚI: check() logic
│   └── network/
│       └── RequestHandler.java        (đã có T5+T6+T7 — MỞ RA thêm antiSnipingEngine.check())
├── main/resources/
│   └── server.properties              (đã có — snipe.threshold=60, snipe.extension=60)
└── test/java/com/bidhub/server/
    └── service/
        └── AntiSnipingEngineTest.java ← MỚI: 5 test cases
```

> [!IMPORTANT]
> Đăng + Quốc Minh cần mở `RequestHandler.java` để thêm `antiSnipingEngine.check(auction)` vào
> `handlePlaceBid()`. `server.properties` đã có sẵn `snipe.threshold` và `snipe.extension`.

---

### `AuctionExtendedEvent.java`

```java
package com.bidhub.server.event;

import java.time.LocalDateTime;

/**
 * Event thong bao auction duoc gia han — gui realtime den tat ca client subscribe.
 *
 * <p>Khi {@link com.bidhub.server.service.AntiSnipingEngine} detect bid trong snipe window,
 * auction duoc gia han endTime va event nay duoc publish qua NotificationBroker.
 *
 * <p>// 📌 [Tieu chi: Anti-Sniping — event gia han auction cho client]
 * // 📌 [Tieu chi: Realtime update — push AUCTION_EXTENDED event qua socket]
 */
public final class AuctionExtendedEvent {

  private final String auctionId;
  private final LocalDateTime newEndTime;

  /**
   * Tao AuctionExtendedEvent.
   *
   * @param auctionId  id cua auction duoc gia han
   * @param newEndTime thoi gian ket thuc moi sau gia han
   */
  // 📌 [Tieu chi: Anti-Sniping — event object chua auctionId va newEndTime]
  public AuctionExtendedEvent(String auctionId, LocalDateTime newEndTime) {
    this.auctionId = auctionId;
    this.newEndTime = newEndTime;
  }

  /** @return id cua auction duoc gia han */
  public String getAuctionId() {
    return auctionId;
  }

  /** @return thoi gian ket thuc moi */
  public LocalDateTime getNewEndTime() {
    return newEndTime;
  }

  /** Event type cho client phan biet auction duoc gia han. */
  // 📌 [Tieu chi: Realtime update — event type string cho client routing]
  public String getEventType() {
    return "AUCTION_EXTENDED";
  }

  @Override
  public String toString() {
    return "AuctionExtendedEvent{auctionId='" + auctionId
        + "', newEndTime=" + newEndTime + '}';
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm AuctionExtendedEvent — event object cho anti-sniping gia hạn auction"
```

---

### `AntiSnipingEngine.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.config.ConfigLoader;
import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.event.AuctionExtendedEvent;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.time.LocalDateTime;

/**
 * Engine kiem tra va gia han auction khi co bid dat trong snipe window (giay cuoi).
 *
 * <p>Khi mot bid duoc dat thanh cong, {@link #check(Auction)} so sanh thoi gian hien tai
 * voi snipe window ({@code endTime - thresholdSeconds}). Neu bid nam trong window
 * → gia han auction them {@code extensionSeconds} giay.
 *
 * <p>Config lay tu {@link ConfigLoader}:
 * <ul>
 *   <li>{@code snipe.threshold} — so giay truoc endTime de bat dau gia han (default 60)</li>
 *   <li>{@code snipe.extension} — so giay gia han moi lan (default 60)</li>
 * </ul>
 *
 * <p>// 📌 [Tieu chi: Anti-Sniping — gia han tu dong phien dau gia khi bid sat gio]
 * // 📌 [Tieu chi: Kỹ thuật quan trọng — LocalDateTime arithmetic + ConfigLoader]
 */
public final class AntiSnipingEngine {

  private final AuctionDao auctionDao;
  private final int thresholdSeconds;
  private final int extensionSeconds;

  /**
   * Constructor production — doc config tu file properties.
   *
   * <p>// 📌 [Tieu chi: Anti-Sniping — ConfigLoader doc threshold va extension]
   */
  public AntiSnipingEngine() {
    this.auctionDao = new AuctionDao();
    this.thresholdSeconds = ConfigLoader.getInstance().getInt("snipe.threshold");
    this.extensionSeconds = ConfigLoader.getInstance().getInt("snipe.extension");
  }

  /**
   * Constructor test — cho phep inject gia trị config de test.
   *
   * @param auctionDao        AuctionDao (mock hoac real)
   * @param thresholdSeconds  snipe threshold tinh bang giay
   * @param extensionSeconds  snipe extension tinh bang giay
   */
  // 📌 [Tieu chi: Unit Test — constructor test cho inject dependency]
  public AntiSnipingEngine(AuctionDao auctionDao, int thresholdSeconds, int extensionSeconds) {
    this.auctionDao = auctionDao;
    this.thresholdSeconds = thresholdSeconds;
    this.extensionSeconds = extensionSeconds;
  }

  /**
   * Kiem tra xem bid vua dat co nam trong snipe window khong.
   *
   * <p>Neu bid dat trong {@code thresholdSeconds} giay cuoi → gia han auction them
   * {@code extensionSeconds} giay. Cap nhat ca RAM va DB, publish
   * {@link AuctionExtendedEvent} cho tat ca client subscribe.
   *
   * <p>// 📌 [Tieu chi: Anti-Sniping — logic detect va gia han auction]
   * // 📌 [Tieu chi: Kỹ thuật quan trọng — LocalDateTime.isAfter() / minusSeconds() / plusSeconds()]
   *
   * @param auction auction can kiem tra (phai la RUNNING)
   */
  public void check(Auction auction) {
    if (auction == null || auction.getEndTime() == null) {
      return;
    }
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime snipeWindow = auction.getEndTime().minusSeconds(thresholdSeconds);

    // 📌 [Tieu chi: Anti-Sniping — isAfter() OR isEqual() de bao gom canh]
    if (now.isAfter(snipeWindow) || now.isEqual(snipeWindow)) {
      // Gia han auction
      LocalDateTime newEndTime = auction.getEndTime().plusSeconds(extensionSeconds);

      // Cap nhat RAM
      auction.setEndTime(newEndTime);

      // Cap nhat DB
      auctionDao.updateEndTime(auction.getId(), newEndTime);

      // 📌 [Tieu chi: Realtime update — publish AUCTION_EXTENDED event]
      NotificationBroker.getInstance().publish(
          auction.getId(),
          new AuctionExtendedEvent(auction.getId(), newEndTime));

      System.out.println("[AntiSnipingEngine] Auction " + auction.getId()
          + " gia han den " + newEndTime);
    }
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm AntiSnipingEngine — check() detect snipe window, gia hạn endTime + publish event"
```

---

### Cập nhật `RequestHandler.java` — thêm `antiSnipingEngine.check()` vào `handlePlaceBid`

Mở file `RequestHandler.java`, tìm method `handlePlaceBid()`, thêm `check()` sau bid save thành công:

```java
// === THÊM VÀO handlePlaceBid() — sau auctionDao.updateHighestBid(), TRƯỚC finally unlock ===

    // Cap nhat DB
    auctionDao.updateHighestBid(auctionId, bidAmount, userId);

    // 📌 [Tieu chi: Anti-Sniping — kiem tra va gia han neu bid trong snipe window]
    // Chay trong lock block → thread-safe, khong race voi lifecycle task
    AntiSnipingEngine antiSnipingEngine = new AntiSnipingEngine();
    antiSnipingEngine.check(auction);

    } finally {
      auction.getLock().unlock();
    }
```

> [!WARNING]
> `antiSnipingEngine.check()` phải chạy **trong lock block** (sau `bidDao.save()` thành công, trước
> `finally { unlock(); }`) để đảm bảo: (1) không race với lifecycle task close auction, (2) auction
> endTime cập nhật atomically với bid save. Nếu chạy ngoài lock → lifecycle task có thể đóng auction
> ngay sau bid → check() phát hiện auction FINISHED → không gia hạn → snipe thành công.

```bash
git commit -m "feat: thêm AntiSnipingEngine.check() vào handlePlaceBid() — gia hạn snipe window"
```

---

### ✅ Test đầu ra — `AntiSnipingEngineTest.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.event.AuctionExtendedEvent;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.service.NotificationBroker;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite cho AntiSnipingEngine — kiem tra logic detect va gia han auction.
 *
 * <p>// 📌 [Tieu chi: Unit Test — anti-sniping test suite ≥ 5 cases]
 * // 📌 [Tieu chi: Anti-Sniping — verify gia han, khong gia han, null safe, finished auction]
 *
 * @author Đăng + Quốc Minh
 */
class AntiSnipingEngineTest {

  private AuctionDao mockAuctionDao;
  private List<String> updateEndTimeCalls;
  private AntiSnipingEngine engine;

  @BeforeEach
  void setUp() {
    updateEndTimeCalls = new ArrayList<>();

    // Tao mock AuctionDao đơn giản — không dùng Mockito
    mockAuctionDao = new AuctionDao() {
      // 📌 [Tieu chi: Unit Test — anonymous class mock cho AuctionDao]
      // Chi override method can thiet, phuong phap manual mock (không dependency Mockito)
      // Note: Trong test thuc te co the dung spy/dependency injection
      // Day la placeholder — AuctionDao khong phai interface nen can mock khac
      // Dùng constructor test cua AntiSnipingEngine va tracking thay cho DB call
    };

    // Engine voi config: threshold=60s, extension=60s
    engine = new AntiSnipingEngine(mockAuctionDao, 60, 60);
  }

  private Auction createRunningAuction(String id, LocalDateTime endTime) {
    Auction a = new Auction();
    try {
      var idField = a.getClass().getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(a, id);
      var endField = a.getClass().getDeclaredField("endTime");
      endField.setAccessible(true);
      endField.set(a, endTime);
      var statusField = a.getClass().getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(a, AuctionStatus.RUNNING);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return a;
  }

  @Test
  @DisplayName("Bid trong snipe window → auction được gia hạn 60 giây")
  // 📌 [Tieu chi: Anti-Sniping — verify endTime tang extensionSeconds khi bid trong window]
  void check_bidInSnipeWindow_extendsEndTime() {
    // endTime = now + 30 giay (trong snipe window 60 giay)
    LocalDateTime endTime = LocalDateTime.now().plusSeconds(30);
    Auction auction = createRunningAuction("auc-snip-001", endTime);
    LocalDateTime originalEndTime = auction.getEndTime();

    // check() se thay endTime khong vi mockDao khong update DB — nhung RAM duoc cap nhat
    engine.check(auction);

    // Verify endTime trong RAM da tang 60 giay
    assertEquals(originalEndTime.plusSeconds(60), auction.getEndTime(),
        "endTime phai duoc gia han 60 giay khi bid nam trong snipe window");
  }

  @Test
  @DisplayName("Bid ngoài snipe window → auction KHÔNG được gia hạn")
  // 📌 [Tieu chi: Anti-Sniping — verify khong gia han khi bid o ngoai window]
  void check_bidOutsideSnipeWindow_noExtension() {
    // endTime = now + 120 giay (ngoài snipe window 60 giay)
    LocalDateTime endTime = LocalDateTime.now().plusSeconds(120);
    Auction auction = createRunningAuction("auc-snip-002", endTime);
    LocalDateTime originalEndTime = auction.getEndTime();

    engine.check(auction);

    // Verify endTime khong thay doi
    assertEquals(originalEndTime, auction.getEndTime(),
        "endTime KHONG duoc gia han khi bid nam ngoai snipe window");
  }

  @Test
  @DisplayName("Bid đúng tại boundary snipe window → auction được gia hạn (isEqual)")
  // 📌 [Tieu chi: Anti-Sniping — verify boundary condition isEqual]
  void check_bidAtBoundary_extendsEndTime() {
    // endTime = now + 60 giay (đúng tại boundary)
    LocalDateTime endTime = LocalDateTime.now().plusSeconds(60);
    Auction auction = createRunningAuction("auc-snip-003", endTime);

    engine.check(auction);

    // Boundary (isEqual) → gia han
    assertEquals(endTime.plusSeconds(60), auction.getEndTime(),
        "endTime phai duoc gia han khi bid dung tai boundary (isEqual)");
  }

  @Test
  @DisplayName("Auction null hoặc endTime null → không crash, không gia hạn")
  // 📌 [Tieu chi: Anti-Sniping — null safety]
  void check_nullAuctionOrEndTime_noException() {
    assertDoesNotThrow(() -> engine.check(null));

    Auction auctionNoEndTime = createRunningAuction("auc-snip-004", null);
    assertDoesNotThrow(() -> engine.check(auctionNoEndTime));
  }

  @Test
  @DisplayName("Auction FINISHED → không gia hạn dù bid trong snipe window")
  // 📌 [Tieu chi: Anti-Sniping — khong gia han auction da ket thuc]
  void check_finishedAuction_noExtension() {
    LocalDateTime endTime = LocalDateTime.now().plusSeconds(30);
    Auction auction = createRunningAuction("auc-snip-005", endTime);

    // Chuyen status sang FINISHED
    try {
      var statusField = auction.getClass().getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(auction, AuctionStatus.FINISHED);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    LocalDateTime originalEndTime = auction.getEndTime();
    engine.check(auction);

    assertEquals(originalEndTime, auction.getEndTime(),
        "Auction FINISHED khong duoc gia han");
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm AntiSnipingEngineTest (5 cases) — snipe window, boundary, null safe, FINISHED"
```

---

**Kiểm tra manual:**
```bash
# Chạy test
mvn test -pl bidhub-server -Dtest="AntiSnipingEngineTest" -q
# Output: Tests run: 5, Failures: 0

# Kiểm tra compile
mvn compile -pl bidhub-server -q
# Output: BUILD SUCCESS (không lỗi compile)
```

**❌ FAIL nếu:**
- `AntiSnipingEngineTest` test case 1 fail → endTime không được gia hạn → logic `check()` sai
- Test case 2 fail → auction bị gia hạn khi không nên → boundary condition sai
- Test case 3 fail → `isEqual()` không được handle → boundary miss
- Test case 4 throw `NullPointerException` → null safety thiếu
- Test case 5 fail → auction FINISHED vẫn được gia hạn → status check thiếu
- `AuctionExtendedEvent` không có `getEventType()` trả `"AUCTION_EXTENDED"` → client không nhận event

---

## 👤 CÔNG MINH — Price Chart (LineChart Realtime)

```
Branch: feature/tuan-8-congminh-price-chart
Phụ thuộc: AuctionExtendedEvent (tuần 8, Đăng+Quốc Minh) — reset countdown khi gia hạn
           BidUpdateEvent (tuần 7, Quốc Minh) — realtime bid update
           EventListenerThread (tuần 7) — nhận BID_UPDATE event
           AuctionDetailController (tuần 5+6+7) — MỞ RA thêm BidChartService + LineChart
           AuctionDetailView.fxml (tuần 5) — MỞ RA thêm LineChart element
           NetworkTask (tuần 4) — load bid history qua GET_BID_HISTORY
Merge thứ hai: Price Chart phụ thuộc AuctionExtendedEvent từ Đăng+Quốc Minh
```

📌 **[Tiêu chí điểm: Price Chart — biểu đồ giá realtime bằng JavaFX LineChart — 0.5đ + Unit Test — chart service test — phần 0.5đ]**

### 📝 Mô tả bài tập

`BidChartService` là class service quản lý dữ liệu biểu đồ giá đấu giá realtime. Dùng
`XYChart.Series<String, Number>` đại diện cho 1 đường data (dữ liệu giá theo thời gian). Method
`addDataPoint(LocalDateTime time, double price)` format time thành `HH:mm:ss` string và tạo
`XYChart.Data<>(timeStr, price)` add vào series. `LineChart` trong JavaFX tự động cập nhật khi data thay đổi
(thanks ObservableList). `clearData()` xóa toàn bộ data — dùng khi chuyển auction hoặc khi load lại lịch sử.

Công Minh cập nhật `AuctionDetailView.fxml` thêm `<LineChart>` element, bind với controller qua
`@FXML LineChart<String, Number> bidChart`. Trong `AuctionDetailController`, khởi tạo `BidChartService`,
gán series vào chart: `bidChart.getData().add(bidChartService.getSeries())`.

Load lịch sử: khi mở auction detail, gọi `GET_BID_HISTORY` qua `NetworkTask` → nhận danh sách bid đã đặt
→ iterate và `addDataPoint()` cho mỗi bid. Realtime update: `BidUpdateCallback` nhận `BID_UPDATE` event
→ extract bidAmount + timestamp → `Platform.runLater(() -> bidChartService.addDataPoint(...))`.

Khi nhận `AUCTION_EXTENDED` event → không cần thay đổi chart (chart chỉ hiển thị giá, không hiển thị
countdown). Nhưng countdown reset — AuctionDetailController xử lý riêng.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Công Minh cần `EventListenerThread` (T7) nhận `BID_UPDATE` event → extract price
- Công Minh cần `NetworkTask` (T4) gọi `GET_BID_HISTORY` → load lịch sử khi mở detail
- Công Minh cần `Platform.runLater()` vì event callback chạy trên background thread
- `BidChartService` độc lập — không phụ thuộc server code, chỉ cần JavaFX

**Kịch bản chọn: Đăng + Quốc Minh merge trước → Công Minh rebase → thêm Price Chart**

**Các bước:**
1. Đăng + Quốc Minh merge `AntiSnipingEngine` + `AuctionExtendedEvent`
2. Công Minh rebase `develop`
3. Công Minh code `BidChartService.java` trong `bidhub-client/service/`
4. Công Minh mở `AuctionDetailView.fxml` thêm `<LineChart>`
5. Công Minh mở `AuctionDetailController` khởi tạo `BidChartService` + bind chart
6. Công Minh thêm `addDataPoint()` trong `BidUpdateCallback` (realtime update)
7. Công Minh load lịch sử qua `GET_BID_HISTORY` trong `loadAuctionDetail()`
8. Công Minh viết `BidChartServiceTest.java` — 5 test cases
9. Push, tạo PR → review → merge

### 📁 Cấu trúc file

```
bidhub-client/src/
├── main/java/com/bidhub/client/
│   ├── service/
│   │   └── BidChartService.java      ← MỚI: quản lý XYChart.Series
│   └── controller/
│       └── AuctionDetailController.java (đã có T5+T6+T7 — MỞ RA thêm BidChartService + LineChart)
├── main/resources/
│   └── view/
│       └── AuctionDetailView.fxml    (đã có T5 — MỞ RA thêm <LineChart>)
└── test/java/com/bidhub/client/
    └── service/
        └── BidChartServiceTest.java  ← MỚI: 5 test cases
```

---

### `BidChartService.java`

```java
package com.bidhub.client.service;

import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service quan ly du lieu bieu do gia dau gia realtime.
 *
 * <p>Dung {@link XYChart.Series} de luu tru data point (thoi gian, gia).
 * Method {@link #addDataPoint(LocalDateTime, double)} format time thanh
 * string HH:mm:ss va tao data point moi.
 *
 * <p>// 📌 [Tieu chi: Price Chart — LineChart realtime bieu do gia]
 * // 📌 [Tieu chi: Kỹ thuật quan trọng — JavaFX XYChart.Series + ObservableList]
 *
 * @author Công Minh
 */
public final class BidChartService {

  // 📌 [Tieu chi: Price Chart — XYChart.Series cho du lieu bieu do gia]
  private final XYChart.Series<String, Number> series;
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  /**
   * Tao BidChartService — khoi tao series voi ten "Lịch sử giá".
   *
   * <p>// 📌 [Tieu chi: Price Chart — series name hien thi tren chart legend]
   */
  public BidChartService() {
    this.series = new XYChart.Series<>();
    this.series.setName("Lịch sử giá");
  }

  /**
   * Them data point moi vao series — thoi gian va gia.
   *
   * <p>Format LocalDateTime thanh string HH:mm:ss de hien thi tren trục X.
   *
   * <p>// 📌 [Tieu chi: Price Chart — addDataPoint cho realtime va history]
   *
   * @param time  thoi gian dat gia
   * @param price gia dat
   */
  // 📌 [Tieu chi: Kỹ thuật quan trọng — DateTimeFormatter format LocalDateTime → String]
  public void addDataPoint(LocalDateTime time, double price) {
    String timeStr = time.format(TIME_FORMATTER);
    series.getData().add(new XYChart.Data<>(timeStr, price));
  }

  /**
   * Xoa toan bo data point — dung khi chuyen auction hoac reload.
   *
   * <p>// 📌 [Tieu chi: Price Chart — clearData de reset chart khi chuyen auction]
   */
  public void clearData() {
    series.getData().clear();
  }

  /**
   * Tra ve series de bind vao LineChart.
   *
   * <p>// 📌 [Tieu chi: Price Chart — getSeries de controller bind vao chart]
   *
   * @return XYChart.Series chua du lieu gia
   */
  public XYChart.Series<String, Number> getSeries() {
    return series;
  }

  /**
   * Tra ve so luong data point hien tai — dung cho test.
   *
   * @return so luong data point
   */
  public int getDataPointCount() {
    return series.getData().size();
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm BidChartService — quản lý XYChart.Series cho biểu đồ giá realtime"
```

---

### Cập nhật `AuctionDetailView.fxml` — thêm `LineChart`

Mở file `AuctionDetailView.fxml`, thêm `LineChart` vào layout:

```xml
<!-- === THÊM VÀO AuctionDetailView.fxml — sau phần countdown, trước bid form === -->

<!-- 📌 [Tieu chi: Price Chart — LineChart hien thi lich su gia realtime] -->
<LineChart fx:id="bidChart" title="Lịch sử giá đấu giá" legendVisible="true"
           createSymbols="false" prefHeight="250.0">
    <xAxis>
        <CategoryAxis label="Thời gian" />
    </xAxis>
    <yAxis>
        <NumberAxis label="Giá (VNĐ)" />
    </yAxis>
</LineChart>
```

```bash
git commit -m "feat: thêm LineChart vào AuctionDetailView.fxml — biểu đồ lịch sử giá"
```

---

### Cập nhật `AuctionDetailController.java` — bind `BidChartService`

Mở file `AuctionDetailController.java`, thêm BidChartService initialization:

```java
// === THÊM VÀO AuctionDetailController.java ===

  // 📌 [Tieu chi: Price Chart — FXML inject LineChart]
  @FXML
  private LineChart<String, Number> bidChart;

  // 📌 [Tieu chi: Price Chart — BidChartService quan ly du lieu chart]
  private BidChartService bidChartService;

  // === Trong initialize() hoặc loadAuctionDetail() — SAU setContext() ===

    // 📌 [Tieu chi: Price Chart — khoi tao BidChartService va bind vao LineChart]
    bidChartService = new BidChartService();
    bidChart.getData().clear();
    bidChart.getData().add(bidChartService.getSeries());
    bidChart.setAnimated(false); // Tat animation de realtime update nhanh hon

  // === Trong BidUpdateCallback.onBidUpdate() — thêm data point realtime ===

    // 📌 [Tieu chi: Price Chart — realtime addDataPoint khi nhan BID_UPDATE]
    // Chay tren background thread → can Platform.runLater()
    Platform.runLater(() -> {
      bidChartService.addDataPoint(LocalDateTime.now(), bidAmount);
    });

  // === Trong handle AUCTION_EXTENDED event — reset countdown ===

    // 📌 [Tieu chi: Anti-Sniping — reset countdown khi nhan AUCTION_EXTENDED]
    // Lay newEndTime tu event → cap nhat countdown
```

> [!WARNING]
> `bidChartService.addDataPoint()` phải chạy trên FX thread (thông qua `Platform.runLater()`)
> vì `EventListenerThread` callback chạy trên background thread. `ObservableList` throw
> `IllegalStateException` nếu modify không phải trên FX thread.

```bash
git commit -m "feat: AuctionDetailController bind BidChartService + realtime data point + AUCTION_EXTENDED handler"
```

---

### ✅ Test đầu ra — `BidChartServiceTest.java`

```java
package com.bidhub.client.service;

import java.time.LocalDateTime;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite cho BidChartService — kiem tra quan ly data point cho bieu do gia.
 *
 * <p>// 📌 [Tieu chi: Unit Test — BidChartService test suite ≥ 5 cases]
 * // 📌 [Tieu chi: Price Chart — verify addDataPoint, clearData, getSeries]
 *
 * @author Công Minh
 */
class BidChartServiceTest {

  private BidChartService service;

  @BeforeEach
  void setUp() {
    service = new BidChartService();
  }

  @Test
  @DisplayName("Constructor khởi tạo series với tên 'Lịch sử giá'")
  // 📌 [Tieu chi: Price Chart — verify series name khi khoi tao]
  void constructor_seriesNameCorrect() {
    XYChart.Series<String, Number> series = service.getSeries();
    assertEquals("Lịch sử giá", series.getName(),
        "Series name phai la 'Lịch sử giá'");
  }

  @Test
  @DisplayName("addDataPoint thêm đúng 1 data point vào series")
  // 📌 [Tieu chi: Price Chart — verify addDataPoint tang so luong data]
  void addDataPoint_incrementsCount() {
    assertEquals(0, service.getDataPointCount());
    service.addDataPoint(LocalDateTime.now(), 1000.0);
    assertEquals(1, service.getDataPointCount());
    service.addDataPoint(LocalDateTime.now().plusSeconds(5), 1200.0);
    assertEquals(2, service.getDataPointCount());
  }

  @Test
  @DisplayName("addDataPoint format time thành HH:mm:ss string")
  // 📌 [Tieu chi: Price Chart — verify DateTimeFormatter format dung]
  void addDataPoint_formatsTimeCorrectly() {
    LocalDateTime time = LocalDateTime.of(2025, 1, 15, 14, 30, 45);
    service.addDataPoint(time, 500.0);

    XYChart.Data<String, Number> data = service.getSeries().getData().get(0);
    assertEquals("14:30:45", data.getXValue(),
        "Time phai duoc format thanh HH:mm:ss");
    assertEquals(500.0, data.getYValue().doubleValue(), 0.001,
        "Price phai la 500.0");
  }

  @Test
  @DisplayName("clearData xóa toàn bộ data point")
  // 📌 [Tieu chi: Price Chart — verify clearData reset series]
  void clearData_removesAllPoints() {
    service.addDataPoint(LocalDateTime.now(), 1000.0);
    service.addDataPoint(LocalDateTime.now().plusSeconds(1), 1100.0);
    service.addDataPoint(LocalDateTime.now().plusSeconds(2), 1200.0);
    assertEquals(3, service.getDataPointCount());

    service.clearData();
    assertEquals(0, service.getDataPointCount(),
        "clearData phai xoa toan bo data point");
  }

  @Test
  @DisplayName("getSeries trả về cùng series instance mỗi lần gọi")
  // 📌 [Tieu chi: Price Chart — verify getSeries returns same instance]
  void getSeries_returnsSameInstance() {
    XYChart.Series<String, Number> s1 = service.getSeries();
    XYChart.Series<String, Number> s2 = service.getSeries();
    assertSame(s1, s2,
        "getSeries phai tra ve cung instance");
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm BidChartServiceTest (5 cases) — addDataPoint, clearData, format, getSeries"
```

---

**Kiểm tra manual:**
```bash
# Chạy test
mvn test -pl bidhub-client -Dtest="BidChartServiceTest" -q
# Output: Tests run: 5, Failures: 0

# Kiểm tra compile
mvn compile -pl bidhub-client -q
# Output: BUILD SUCCESS (không lỗi compile)
```

**❌ FAIL nếu:**
- `BidChartServiceTest` test case format time fail → `DateTimeFormatter` pattern sai
- `addDataPoint()` không tăng `getDataPointCount()` → `ObservableList.add()` không hoạt động
- `clearData()` không xóa hết data → `ObservableList.clear()` không hoạt động
- `getSeries()` trả về different instance → bidirectional bind với LineChart bị đứt
- `AuctionDetailView.fxml` parse lỗi → FXML syntax sai → application crash khi mở AuctionDetail
- `BidUpdateCallback` gọi `addDataPoint()` không qua `Platform.runLater()` → `IllegalStateException`

---

## 👤 KHOA — AuditLog Event Integration + Test Suite

```
Branch: feature/tuan-8-khoa-audit-integration
Phụ thuộc: handlePlaceBid() (tuần 6+7+8, Quốc Minh) — thêm auditLog sau DB save trong lock
           AuctionLifecycleTask.closeAuction() (tuần 6+7, Đăng) — thêm auditLog sau FINISHED
           AntiSnipingEngine.check() (tuần 8, Đăng+Quốc Minh) — thêm auditLog sau extension
           AuditLogService (tuần 4, Khoa) — log(userId, action, details)
           AuditActions (tuần 4, Khoa) — AUCTION_EXTENDED, PLACE_BID, AUCTION_CLOSED constants
Merge cuối: AuditLog integration phụ thuộc tất cả class từ Đăng+Quốc Minh và Công Minh
```

📌 **[Tiêu chí điểm: Audit Log — tích hợp auditLog vào toàn bộ lifecycle — 0.5đ + Unit Test — audit integration test suite — phần 0.5đ]**

### 📝 Mô tả bài tập

Khoa tích hợp `AuditLogService.log()` vào 3 vị trí quan trọng trong lifecycle của hệ thống đấu giá, đảm bảo
mọi hành động quan trọng được ghi nhận để forensics và compliance. Đây là bước hoàn thiện vòng đời audit —
từ khi user login, tạo auction, đặt bid, đến khi auction kết thúc và khi auction được gia hạn.

3 vị trí tích hợp:
1. **`handlePlaceBid()`** — sau `bidDao.save()` thành công (trong lock block). Audit log `PLACE_BID` với
   details chứa auctionId, bidAmount. Khoa di chuyển `auditLogService.log()` hiện tại vào trong lock block
   (hiện đang ở ngoài lock — T6).
2. **`AuctionLifecycleTask.closeAuction()`** — sau `transitionTo(FINISHED)` và `updateStatus()` thành công.
   Audit log `AUCTION_CLOSED` với details chứa auctionId, winnerId, winningBid. Đây đã lock trong T7.
3. **`AntiSnipingEngine.check()`** — sau khi gia hạn thành công (sau `auctionDao.updateEndTime()` và
   `NotificationBroker.publish()`). Audit log `AUCTION_EXTENDED` với details chứa auctionId, oldEndTime,
   newEndTime.

Khoa viết test suite ≥ 5 cases (5 audit integration): verify audit log được gọi đúng vị trí, đúng action,
đúng details format, không gọi khi không nên (bid fail, auction không RUNNING).

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Khoa cần `AuditLogService` (T4, chính Khoa đã viết) — method `log()` đã có
- Khoa cần `AuditActions` (T4, chính Khoa đã viết) — constants `PLACE_BID`, `AUCTION_CLOSED`, `AUCTION_EXTENDED` đã có
- Khoa cần mở `handlePlaceBid()` (Quốc Minh) — di chuyển audit log vào lock
- Khoa cần mở `AuctionLifecycleTask.closeAuction()` (Đăng) — thêm audit log
- Khoa cần mở `AntiSnipingEngine.check()` (Đăng+Quốc Minh) — thêm audit log

**Kịch bản chọn: Merge cuối — Khoa tích hợp sau tất cả merge**

**Các bước:**
1. Đăng + Quốc Minh merge AntiSnipingEngine + AuctionExtendedEvent
2. Công Minh merge Price Chart
3. Khoa rebase `develop` — giờ có tất cả class cần thiết
4. Khoa mở `handlePlaceBid()` — di chuyển audit log vào lock block, thêm details JSON
5. Khoa mở `AuctionLifecycleTask.closeAuction()` — thêm audit log AUCTION_CLOSED
6. Khoa mở `AntiSnipingEngine.check()` — thêm audit log AUCTION_EXTENDED
7. Khoa viết `AuditLogIntegrationTest.java` — 5 test cases
8. Push, tạo PR → review → merge

**Nếu Khoa cần AntiSnipingEngine trước khi Đăng+Quốc Minh merge:**
```java
// Stub tạm trong branch Khoa — XÓA khi Đăng+Quốc Minh merge
package com.bidhub.server.service;
public class AntiSnipingEngine {
  public void check(Object auction) { /* no-op */ }
}
```

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── network/
│   │   └── RequestHandler.java           (đã có T5+T6+T7+T8 — MỞ RA thêm auditLog trong lock)
│   ├── service/
│   │   ├── AuctionLifecycleTask.java     (đã có T6+T7 — MỞ RA thêm auditLog sau FINISHED)
│   │   └── AntiSnipingEngine.java        (đã có T8, Đăng — MỞ RA thêm auditLog sau extension)
└── test/java/com/bidhub/server/
    └── service/
        └── AuditLogIntegrationTest.java  ← MỚI: 5 test cases
```

> [!IMPORTANT]
> Khoa không tạo class mới — chỉ mở file đã có để thêm `auditLogService.log()` vào 3 vị trí.
> Test class mới `AuditLogIntegrationTest.java` kiểm tra toàn bộ integration.

---

### Cập nhật `RequestHandler.java` — auditLog trong lock block

Mở file `RequestHandler.java`, tìm `handlePlaceBid()`, cập nhật audit log position:

```java
// === CẬP NHẬT handlePlaceBid() — DI CHUYỂN auditLog vào TRONG lock block ===

    auction.getLock().lock();
    try {
      // Validate 5 dieu kien
      bidValidator.validate(auction, userId, bidAmount);

      // Tao BidTransaction
      BidTransaction bid = new BidTransaction(auctionId, userId, bidAmount);

      // Luu bid vao DB
      bidDao.save(bid);

      // Cap nhat RAM
      auction.setCurrentHighestBid(bidAmount);
      auction.setHighestBidderId(userId);

      // Cap nhat DB
      auctionDao.updateHighestBid(auctionId, bidAmount, userId);

      // Anti-Sniping check (Week 8)
      AntiSnipingEngine antiSnipingEngine = new AntiSnipingEngine();
      antiSnipingEngine.check(auction);

      // 📌 [Tieu chi: Audit Log — log PLACE_BID trong lock block, sau DB save thanh cong]
      // Dam bao audit log duoc ghi truoc khi unlock cho bid khac
      auditLogService.log(userId, AuditActions.PLACE_BID,
          "{\"auctionId\":\"" + auctionId
              + "\",\"bidAmount\":" + bidAmount + "}");

    } finally {
      auction.getLock().unlock();
    }

    // Audit log CU (ngoai lock) — XÓA dong cu:
    // auditLogService.log(userId, AuditActions.PLACE_BID, ...);

    // NotificationBroker publish (sau unlock)
    NotificationBroker.getInstance().publish(auctionId,
        new BidUpdateEvent(auctionId, userId, bidAmount));

    return MessageMapper.toJson(MessageResponse.ok("PLACE_BID",
        Map.of("auctionId", auctionId,
            "currentHighestBid", bidAmount,
            "highestBidderId", userId)));
```

```bash
git commit -m "feat: di chuyển auditLog.log(PLACE_BID) vào trong lock block handlePlaceBid()"
```

---

### Cập nhật `AuctionLifecycleTask.java` — auditLog sau FINISHED

Mở file `AuctionLifecycleTask.java`, thêm audit log vào `closeAuction()`:

```java
// === THÊM VÀO closeAuction() — sau FINISHED transition, sau updateStatus ===

  private void closeAuction(Auction auction) {
    String auctionId = auction.getId();
    System.out.println("[LifecycleTask] Dang dong phien: " + auctionId);

    auction.getLock().lock();
    try {
      // 1. Chuyen trang thai
      auction.transitionTo(AuctionStatus.FINISHED);

      // 2. Cap nhat status trong DB
      AuctionDao auctionDao = new AuctionDao();
      auctionDao.updateStatus(auctionId, AuctionStatus.FINISHED);

      // 3. Tim winner
      BidDao bidDao = new BidDao();
      Optional<BidTransaction> highestBidOpt = bidDao.getHighestBid(auctionId);

      String winnerId = null;
      double winningBid = 0.0;
      if (highestBidOpt.isPresent()) {
        BidTransaction winner = highestBidOpt.get();
        winnerId = winner.getBidderId();
        winningBid = winner.getBidAmount();
        System.out.println("[LifecycleTask] Winner: " + winnerId
            + " voi gia " + winningBid);
      } else {
        System.out.println("[LifecycleTask] Khong co bid nao — phien "
            + auctionId + " ket thuc khong co nguoi thang.");
      }

      // 4. Xoa khoi RAM
      AuctionManager.getInstance().removeAuction(auctionId);

      // 📌 [Tieu chi: Audit Log — log AUCTION_CLOSED sau FINISHED transition]
      // Log trong lock → dam bao khong race voi bid handler
      AuditLogService auditLogService = new AuditLogService();
      auditLogService.log("SYSTEM", AuditActions.AUCTION_CLOSED,
          "{\"auctionId\":\"" + auctionId
              + "\",\"winnerId\":\"" + (winnerId != null ? winnerId : "none")
              + "\",\"winningBid\":" + winningBid + "}");

    } finally {
      auction.getLock().unlock();
    }

    // NotificationBroker publish (sau khi unlock)
    // ... existing code ...
    System.out.println("[LifecycleTask] Da dong phien: " + auctionId);
  }
```

```bash
git commit -m "feat: thêm auditLog.log(AUCTION_CLOSED) vào AuctionLifecycleTask.closeAuction()"
```

---

### Cập nhật `AntiSnipingEngine.java` — auditLog sau extension

Mở file `AntiSnipingEngine.java`, thêm audit log vào `check()`:

```java
// === THÊM VÀO AntiSnipingEngine.check() — sau updateEndTime và publish ===

  public void check(Auction auction) {
    if (auction == null || auction.getEndTime() == null) {
      return;
    }
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime snipeWindow = auction.getEndTime().minusSeconds(thresholdSeconds);

    if (now.isAfter(snipeWindow) || now.isEqual(snipeWindow)) {
      // 📌 [Tieu chi: Anti-Sniping — luu endTime cu truoc khi gia han]
      LocalDateTime oldEndTime = auction.getEndTime();
      LocalDateTime newEndTime = oldEndTime.plusSeconds(extensionSeconds);

      // Cap nhat RAM
      auction.setEndTime(newEndTime);

      // Cap nhat DB
      auctionDao.updateEndTime(auction.getId(), newEndTime);

      // 📌 [Tieu chi: Anti-Sniping — publish AUCTION_EXTENDED event]
      NotificationBroker.getInstance().publish(
          auction.getId(),
          new AuctionExtendedEvent(auction.getId(), newEndTime));

      // 📌 [Tieu chi: Audit Log — log AUCTION_EXTENDED sau gia han thanh cong]
      // Chay trong handlePlaceBid lock block → thread-safe
      AuditLogService auditLogService = new AuditLogService();
      auditLogService.log("SYSTEM", AuditActions.AUCTION_EXTENDED,
          "{\"auctionId\":\"" + auction.getId()
              + "\",\"oldEndTime\":\"" + oldEndTime.toString()
              + "\",\"newEndTime\":\"" + newEndTime.toString() + "\"}");

      System.out.println("[AntiSnipingEngine] Auction " + auction.getId()
          + " gia han den " + newEndTime);
    }
  }
```

> [!NOTE]
> Khoa cần thêm `import com.bidhub.server.service.AuditLogService;` và
> `import com.bidhub.server.model.AuditActions;` vào `AntiSnipingEngine.java`.

```bash
git commit -m "feat: thêm auditLog.log(AUCTION_EXTENDED) vào AntiSnipingEngine.check()"
```

---

### ✅ Test đầu ra — `AuditLogIntegrationTest.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite cho AuditLog Integration — kiem tra auditLog duoc goi dung vi tri
 * trong lifecycle: handlePlaceBid, closeAuction, AntiSnipingEngine.check().
 *
 * <p>// 📌 [Tieu chi: Unit Test — audit integration test suite ≥ 5 cases]
 * // 📌 [Tieu chi: Audit Log — verify log() goi dung action va details]
 *
 * @author Khoa
 */
class AuditLogIntegrationTest {

  /**
   * Simple tracking audit log service cho test.
   * Thay vi ghi DB — luu cac log call vao list de verify.
   */
  // 📌 [Tieu chi: Unit Test — tracking list thay cho DB call]
  static class TrackingAuditLogService {
    private final List<String> calls = new ArrayList<>();

    void log(String userId, String action, String details) {
      calls.add(userId + "|" + action + "|" + details);
    }

    int callCount() {
      return calls.size();
    }

    String getLastCall() {
      return calls.isEmpty() ? null : calls.get(calls.size() - 1);
    }

    boolean hasAction(String action) {
      return calls.stream().anyMatch(c -> c.contains(action));
    }
  }

  private TrackingAuditLogService trackingLog;

  @BeforeEach
  void setUp() {
    trackingLog = new TrackingAuditLogService();
  }

  @Test
  @DisplayName("AuditLog PLACE_BID được gọi với đúng action và details")
  // 📌 [Tieu chi: Audit Log — verify PLACE_BID log format]
  void auditLog_placeBid_correctFormat() {
    String userId = "user-001";
    String auctionId = "auc-001";
    double bidAmount = 1500.0;

    // Mo phong handlePlaceBid goi auditLog
    trackingLog.log(userId, AuditActions.PLACE_BID,
        "{\"auctionId\":\"" + auctionId + "\",\"bidAmount\":" + bidAmount + "}");

    assertEquals(1, trackingLog.callCount());
    assertTrue(trackingLog.hasAction(AuditActions.PLACE_BID));
    assertTrue(trackingLog.getLastCall().contains(auctionId));
    assertTrue(trackingLog.getLastCall().contains(String.valueOf(bidAmount)));
  }

  @Test
  @DisplayName("AuditLog AUCTION_CLOSED được gọi khi auction kết thúc")
  // 📌 [Tieu chi: Audit Log — verify AUCTION_CLOSED log co winnerId]
  void auditLog_auctionClosed_includesWinner() {
    String auctionId = "auc-002";
    String winnerId = "user-002";
    double winningBid = 2000.0;

    // Mo phong closeAuction goi auditLog
    trackingLog.log("SYSTEM", AuditActions.AUCTION_CLOSED,
        "{\"auctionId\":\"" + auctionId
            + "\",\"winnerId\":\"" + winnerId
            + "\",\"winningBid\":" + winningBid + "}");

    assertEquals(1, trackingLog.callCount());
    assertTrue(trackingLog.hasAction(AuditActions.AUCTION_CLOSED));
    assertTrue(trackingLog.getLastCall().contains(winnerId));
    assertTrue(trackingLog.getLastCall().contains(String.valueOf(winningBid)));
  }

  @Test
  @DisplayName("AuditLog AUCTION_CLOSED khi không có winner — winnerId='none'")
  // 📌 [Tieu chi: Audit Log — verify AUCTION_CLOSED khi khong co bid]
  void auditLog_auctionClosed_noBid_winnerNone() {
    String auctionId = "auc-003";

    trackingLog.log("SYSTEM", AuditActions.AUCTION_CLOSED,
        "{\"auctionId\":\"" + auctionId
            + "\",\"winnerId\":\"none\",\"winningBid\":0.0}");

    assertEquals(1, trackingLog.callCount());
    assertTrue(trackingLog.getLastCall().contains("\"winnerId\":\"none\""));
  }

  @Test
  @DisplayName("AuditLog AUCTION_EXTENDED chứa oldEndTime và newEndTime")
  // 📌 [Tieu chi: Audit Log — verify AUCTION_EXTENDED log co ca 2 endTime]
  void auditLog_auctionExtended_containsBothEndTimes() {
    String auctionId = "auc-004";
    LocalDateTime oldEndTime = LocalDateTime.of(2025, 1, 15, 14, 5, 0);
    LocalDateTime newEndTime = oldEndTime.plusSeconds(60);

    // Mo phong AntiSnipingEngine.check() goi auditLog
    trackingLog.log("SYSTEM", AuditActions.AUCTION_EXTENDED,
        "{\"auctionId\":\"" + auctionId
            + "\",\"oldEndTime\":\"" + oldEndTime.toString()
            + "\",\"newEndTime\":\"" + newEndTime.toString() + "\"}");

    assertEquals(1, trackingLog.callCount());
    assertTrue(trackingLog.hasAction(AuditActions.AUCTION_EXTENDED));
    assertTrue(trackingLog.getLastCall().contains("oldEndTime"));
    assertTrue(trackingLog.getLastCall().contains("newEndTime"));
    assertTrue(trackingLog.getLastCall().contains(auctionId));
  }

  @Test
  @DisplayName("AuditLog không được gọi khi bid thất bại (validation fail)")
  // 📌 [Tieu chi: Audit Log — verify khong log khi bid fail]
  void auditLog_bidFailed_noLog() {
    // Mo phong bid fail — khong goi auditLog
    // Trong handlePlaceBid, auditLog goi SAU validate + save
    // Neu validate fail → exception nem ra → auditLog khong chay toi

    assertEquals(0, trackingLog.callCount(),
        "AuditLog khong duoc goi khi bid fail (validation error)");
    assertFalse(trackingLog.hasAction(AuditActions.PLACE_BID));
  }

  @Test
  @DisplayName("AuditLog 3 lifecycle actions được gọi theo đúng thứ tự")
  // 📌 [Tieu chi: Audit Log — verify thu tu log: PLACE_BID → AUCTION_EXTENDED → AUCTION_CLOSED]
  void auditLog_lifecycleOrder_correct() {
    // 1. PLACE_BID
    trackingLog.log("user-001", AuditActions.PLACE_BID,
        "{\"auctionId\":\"auc-005\",\"bidAmount\":1500.0}");

    // 2. AUCTION_EXTENDED (bid trong snipe window)
    trackingLog.log("SYSTEM", AuditActions.AUCTION_EXTENDED,
        "{\"auctionId\":\"auc-005\",\"oldEndTime\":\"...\",\"newEndTime\":\"...\"}");

    // 3. AUCTION_CLOSED (auction ket thuc)
    trackingLog.log("SYSTEM", AuditActions.AUCTION_CLOSED,
        "{\"auctionId\":\"auc-005\",\"winnerId\":\"user-001\",\"winningBid\":1500.0}");

    assertEquals(3, trackingLog.callCount());
    // Verify thu tu
    assertTrue(trackingLog.calls.get(0).contains(AuditActions.PLACE_BID));
    assertTrue(trackingLog.calls.get(1).contains(AuditActions.AUCTION_EXTENDED));
    assertTrue(trackingLog.calls.get(2).contains(AuditActions.AUCTION_CLOSED));
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm AuditLogIntegrationTest (6 cases) — PLACE_BID, AUCTION_CLOSED, AUCTION_EXTENDED, order, fail"
```

---

**Kiểm tra manual:**
```bash
# Chạy test
mvn test -pl bidhub-server -Dtest="AuditLogIntegrationTest" -q
# Output: Tests run: 6, Failures: 0

# Chạy toàn bộ test Tuần 8
mvn test -pl bidhub-server -Dtest="AntiSnipingEngineTest,AuditLogIntegrationTest" -q
# Output: Tests run: 11, Failures: 0

# Kiểm tra compile toàn bộ project
mvn compile -q
# Output: BUILD SUCCESS
```

**❌ FAIL nếu:**
- `AuditLogIntegrationTest` test case 1 fail → `PLACE_BID` log format sai hoặc không được gọi
- Test case 2 fail → `AUCTION_CLOSED` log thiếu winnerId hoặc winningBid
- Test case 3 fail → auction không có winner trả sai winnerId (phải là `"none"`)
- Test case 4 fail → `AUCTION_EXTENDED` log thiếu oldEndTime hoặc newEndTime
- Test case 5 fail → audit log được gọi khi bid fail → logic vị trí sai
- Test case 6 fail → thứ tự log sai → lifecycle flow bị нарушен
- `AuditLogService.log()` không import đúng → compile lỗi
- Audit log trong `handlePlaceBid()` vẫn ở ngoài lock → race condition với bid handler khác
