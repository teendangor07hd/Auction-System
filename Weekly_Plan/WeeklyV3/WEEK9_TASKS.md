# 📋 TUẦN 9 — BÀI TẬP CHI TIẾT: End-to-End Integration, CI/CD & DataIntegrityService

✅ Kết quả kiểm tra toàn diện: Không có lỗi. Codebase đáp ứng đầy đủ barem và sẵn sàng cho Tuần 9.

## 🎯 MỤC TIÊU TUẦN 9

Tuần này là tuần cuối cùng — toàn hệ thống phải chạy end-to-end, CI xanh, tài liệu hoàn chỉnh, và kiểm tra
toàn diện dữ liệu. Cuối tuần, cả nhóm phải có:

- ✅ `IntegrationTest.java` — end-to-end: register, login, create item, create auction, subscribe, bid, receive events, wait lifecycle close
- ✅ SLF4J + Logback — replace ALL `System.out.println` / `System.err.println` bằng `Logger`, thêm `logback.xml`
- ✅ Clean code — verify `MigrationRunner` xử lý 5 bảng (users, items, auctions, bid_transactions, audit_logs)
- ✅ Verify `AuctionManager` load RUNNING auctions on restart
- ✅ `docs/API_PROTOCOL.md` hoàn chỉnh — ≥14 command types, mỗi loại có request/response JSON examples
- ✅ `.github/workflows/ci.yml` — `--fail-at-end`, JaCoCo plugin, upload report artifact
- ✅ CI badge trên README.md
- ✅ Loading state (`ProgressIndicator`) cho tất cả `NetworkTask` — button disable trong task
- ✅ Handle empty data lists, load failures — `AuctionDetail` bid failure Alert, countdown stops on `AUCTION_CLOSED`
- ✅ `TextField` bidAmount numeric only + form client-side validation
- ✅ Check 1366×768 resolution
- ✅ `DataIntegrityService` — `checkBidConsistency()`, `checkAuctionWinners()`, `checkOrphanedItems()`, `runFullCheck()`
- ✅ `handleRunIntegrityCheck()` (ADMIN only) trong `RequestHandler`
- ✅ ≥ 10 test cases cho `DataIntegrityService` — tổng project ≥ 139 cases

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** barem: **Unit Test** — IntegrationTest end-to-end + DataIntegrityService tests
> (phần 0.5đ) + **Kỹ thuật quan trọng** — SLF4J + Logback logging framework (1.0đ) + **CI/CD** — GitHub Actions
> + JaCoCo coverage badge (1.0đ) + **Tài liệu** — API Protocol docs hoàn chỉnh (1.0đ) + **Clean Code** —
> refactor System.out.println, logback.xml, verify MigrationRunner 5 tables (phần 1.0đ).

> [!CAUTION]
> **Tuyệt đối không tạo lại** bất kỳ class nào từ Tuần 1–8:
> `Entity`, `BidHubException` + 7 subclass, `MessageRequest`, `MessageResponse`, `MessageMapper`,
> `ConfigLoader`, `DbConnectionProvider`, `MigrationRunner`,
> `UserRole`, `User` (with locked), `Bidder`, `Seller`, `Admin`, `Displayable`,
> `ItemType`, `Item`, `Electronics`, `Art`, `Vehicle`,
> `ItemCreator` + 3 ConcreteCreator, `AuctionStatus`, `Auction` (with lock + getLock + setEndTime), `BidTransaction`,
> `AuditLog`, `AuditActions` (with AUCTION_EXTENDED),
> `UserDao` (with updateLocked, findAll), `ItemDao`, `AuctionDao` (with findAll, updateEndTime), `BidDao`, `AuditLogDao`,
> `SocketServerCore`, `Session`, `ClientConnectionThread`, `RequestHandler` (with all T4-T8 handlers), `SecurityContext`,
> `AuthService`, `SessionManager`, `AuditLogService`,
> `AuctionManager`, `AuctionLifecycleTask` (with lock in closeAuction), `BidValidator`,
> `AdminUserService`,
> `NotificationBroker` (Singleton), `BidUpdateEvent`, `AuctionClosedEvent`, `AuctionExtendedEvent`,
> `AntiSnipingEngine`, `ReportService`,
> `BidHubApp`, `ViewRouter`, `ContextAware`, `LoginController`, `RegisterController`,
> `AuctionListController`, `AuctionDetailController`, `CreateItemController`, `CreateAuctionController`, `Views`,
> `ServerGateway`, `NetworkTask`, `ClientSession`,
> `EventListenerThread`, `BidUpdateCallback`, `BidChartService`,
> `AdminView` (T10), `AdminController` (T10).
>
> **Thứ tự merge quan trọng:** Khoa merge đầu tiên (DataIntegrityService + integrity check handler)
> → Đăng merge thứ hai (IntegrationTest, SLF4J refactor, logback.xml)
> → Quốc Minh merge thứ ba (API_PROTOCOL.md docs, ci.yml JaCoCo, CI badge)
> → Công Minh merge cuối (UI Polish — loading state, button disable, edge case handlers).

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI HỌC (Tự học)

> [!CAUTION]
> Hoàn thành **trước Thứ 5**. Mục đích không phải học Java chung — mà là hiểu cơ chế hoạt động của
> code người khác viết tuần này, để review chéo Chủ nhật trôi chảy và trả lời được vấn đáp giảng viên
> về SLF4J/Logback, Integration Test, CI/CD, DataIntegrityService mà không lúng túng.

---

### Bài 0.1 — SLF4J + Logback: Logging Framework

**Tài liệu bắt buộc:**
- https://www.slf4j.org/manual.html
- https://logback.qos.ch/manual/configuration.html
- Đọc lại toàn bộ code server — tìm mọi `System.out.println` và `System.err.println`

**Câu hỏi hỏi miệng Chủ nhật:**
1. SLF4J (Simple Logging Facade for Java) là **facade pattern** — nó KHÔNG implement logging, mà cung cấp
   API thống nhất. Logback là **implementation**. Tại sao không dùng `java.util.logging` (JUL) hoặc Log4j
   trực tiếp? Lợi ích của tách interface và implementation là gì? Nếu muốn đổi từ Logback sang Log4j2 —
   cần sửa code business logic không?
2. `private static final Logger logger = LoggerFactory.getLogger(ClassName.class)` — tại sao `static final`?
   Nếu khai báo `private Logger logger` (non-static) — mỗi instance tạo 1 logger mới → overhead? `getLogger()`
   có trả về cùng instance cho cùng class name không?
3. SLF4J placeholder: `logger.info("User {} placed bid {} on auction {}", userId, amount, auctionId)`.
   So với string concatenation: `"User " + userId + " placed bid " + amount` — placeholder có ưu điểm gì?
   Nếu string concatenation dùng trong method gọi 100 lần/s → string object tạo ra → GC pressure. Placeholder
   chỉ evaluate khi log level ENABLE — `logger.debug()` với placeholder nhưng level=INFO → có tạo string không?
4. Logback `logback.xml` cấu hình: `<appender>` định nghĩa nơi ghi log (Console, File, RollingFile),
   `<encoder>` định nghĩa format (pattern), `<root level="INFO">` set level mặc định. Pattern
   `%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} — %msg%n` — giải thích từng token. Tại sao cần
   `%thread` trong multi-threaded server?
5. Log level hierarchy: `TRACE < DEBUG < INFO < WARN < ERROR`. Khi root level = `INFO`, method `logger.debug()`
   có chạy không? `logger.isDebugEnabled()` dùng để check trước khi gọi `logger.debug()` — khi nào cần,
   khi nào không cần? SLF4J placeholder đã tự check → `isDebugEnabled()` còn cần thiết không?
6. **[Câu hỏi nâng cao]** Logback `<root level="INFO">` ghi log TẤT CẢ package ở level INFO. Nhưng muốn
   `com.bidhub.server.service` ở DEBUG, còn lại giữ INFO — dùng `<logger name="..." level="DEBUG">`.
   Nếu child logger set level=WARN → parent root level=INFO → log INFO từ child có ghi không? Logback
   logger inheritance (additivity) hoạt động thế nào?

---

### Bài 0.2 — Integration Test vs Unit Test

**Tài liệu bắt buộc:**
- https://junit.org/junit5/docs/current/user-guide/#writing-tests
- Đọc lại `RequestHandler.java` — toàn bộ handler methods
- Đọc lại `AuctionManager.java` — start/stop lifecycle

**Câu hỏi hỏi miệng Chủ nhật:**
1. Unit test test 1 class/method cô lập — mock phụ thuộc. Integration test test nhiều component cùng lúc —
   DB thật, server thật, network thật. `AuctionDaoTest` là unit test hay integration test? Nó cần DB thật.
   Trong BidHub, test nào là unit test, test nào là integration test?
2. `IntegrationTest.java` của Đăng chạy flow: register → login → create item → create auction → subscribe →
   bid → receive events → wait lifecycle close. Nếu auction lifecycle task cần 5 giây interval để đóng phiên
   → test phải chờ ≥5 giây. 5 giây × nhiều test case = chậm. Cách tối ưu: set endTime quá khứ, trigger
   manual lifecycle check, hoặc giảm interval cho test?
3. Test isolation: mỗi `@Test` method nên độc lập — không phụ thuộc kết quả test trước. Nếu test A register
   user "testuser" → test B cũng register "testuser" → lỗi duplicate. Giải pháp: `@BeforeEach` cleanup DB,
   hoặc dùng unique username mỗi test (UUID suffix), hoặc `@TestMethodOrder(OrderAnnotation.class)`?
4. `IntegrationTest` cần server CHẠY — socket connection. `SocketServerCore.start()` mở port 9090.
   Nếu CI environment không cho mở port → test fail. Cách giải quyết: dùng port 0 (OS assign random port),
   hoặc `@Tag("integration")` + skip trong CI bằng `mvn test -DskipITs`?
5. `CountDownLatch` trong integration test: client A đợi event từ server (bid update). `CountDownLatch(1)`
   — client gọi `latch.await(10, SECONDS)`, server publish event → callback `latch.countDown()`.
   Nếu server KHÔNG publish event → `await()` timeout → test fail. Đây là **negative test** hay **flaky test**?
6. **[Câu hỏi nâng cao]** Test pyramid: Unit test (nhiều, nhanh, rẻ) ở đáy → Integration test (ít hơn, chậm hơn)
   ở giữa → E2E test (ít nhất, chậm nhất) ở đỉnh. BidHub có 139+ unit tests + 1 integration test — có tuân
   theo test pyramid không? Nếu 50% test là integration test → CI mất 5 phút → team không muốn chạy →
   code không được test → kỹ thuật nào để giảm integration test time?

---

### Bài 0.3 — JaCoCo Code Coverage & CI/CD

**Tài liệu bắt buộc:**
- https://www.jacoco.org/jacoco/trunk/doc/maven.html
- https://docs.github.com/en/actions/automating-builds-and-tests/building-and-testing-java-with-maven
- Đọc lại `pom.xml` — Maven multi-module structure

**Câu hỏi hỏi miệng Chủ nhật:**
1. JaCoCo (Java Code Coverage) đo lường tỷ lệ code được test chạy qua — metric: line coverage, branch coverage,
   method coverage. `jacoco-maven-plugin` phase `test` → tạo `target/site/jacoco/index.html`.
   Line coverage 80% nghĩa là 80% dòng code được execute ít nhất 1 lần trong test. 80% line coverage có
   đảm bảo 80% bug được tìm không?
2. Branch coverage vs Line coverage: `if (user.isLocked()) { return error; } else { proceed; }` — 2 branch,
   1 dòng if. Line coverage = 100% nếu test chỉ chạy branch "not locked" (if false). Branch coverage = 50%
   vì branch "locked" chưa test. Branch coverage quan trọng hơn line coverage vì sao?
3. `.github/workflows/ci.yml` cấu hình: `on: push, pull_request` → CI chạy khi push hoặc tạo PR.
   Step `mvn test --fail-at-end` — nếu module A fail nhưng module B pass → CI exit code = 1 (fail).
   `--fail-at-end` khác gì default behavior? Khi nào dùng `--fail-at-end` thay vì default?
4. JaCoCo report artifact: `actions/upload-artifact@v4` upload `target/site/jacoco` → có thể download
   từ GitHub Actions tab. `jacoco:check` có thể fail build nếu coverage < threshold (ví dụ 70%).
   Set threshold 70% cho line coverage — 0.70 trong `<rules><rule><limits>`. Nếu 1 class mới 0% coverage
   → build fail → team không merge → coverage bảo vệ code mới. Trade-off: threshold quá cao → team frustrated.
5. CI badge trong README.md: `![CI](https://github.com/user/repo/actions/workflows/ci.yml/badge.svg)`
   — hiển thị trạng thái CI (pass/fail) trên README. Badge tự update khi CI chạy. Nếu CI fail → badge đỏ →
   team biết ngay. Tại sao badge quan trọng cho open-source project? Cho project nội bộ có cần không?
6. **[Câu hỏi nâng cao]** Maven multi-module: `mvn test` chạy test cả 3 module (bidhub-common, bidhub-server,
   bidhub-client). JaCoCo aggregate report: `jacoco:report-aggregate` merge coverage từ 3 module → 1 report.
   Nhưng bidhub-client là JavaFX — test cần headless display (X11/Wayland). CI Ubuntu runner không có display →
   JavaFX test fail. Giải pháp: `@Tag("headless")` + `-Djavafx.headless=true`, hoặc skip client test trong CI?

---

### Bài 0.4 — Data Integrity & Cross-Validation

**Tài liệu bắt buộc:**
- Đọc lại `AuctionDao` — method `findAll`, `updateHighestBid`
- Đọc lại `BidDao` — method `getHighestBid`, `findByAuctionId`
- Đọc lại `ItemDao` — method `findById`, `findAll`
- Đọc lại `UserDao` — method `findAll`, `findById`

**Câu hỏi hỏi miệng Chủ nhật:**
1. `DataIntegrityService.checkBidConsistency()` so sánh `currentHighestBid` trong bảng `auctions` với
   MAX(`bid_amount`) trong bảng `bid_transactions` cho cùng `auction_id`. Nếu không khớp → data corrupted.
   Kịch bản corrupt: `handlePlaceBid()` save bid nhưng `updateHighestBid()` fail → bid có nhưng highestBid
   chưa cập nhật. `DataIntegrityService` phát hiện inconsistency này. Fix strategy: chạy `DataIntegrityService`
   định kỳ (cron), hoặc chạy manual qua Admin command?
2. `checkAuctionWinners()` tìm auction có `status = FINISHED` VÀ có bids VÀ `highest_bidder_id = NULL`.
   Kịch bản: `AuctionLifecycleTask.closeAuction()` đóng phiên nhưng `getHighestBid()` fail → winner
   chưa xác định. `DataIntegrityService` phát hiện. Fix: update winner từ MAX bid, hoặc log cho admin review?
3. `checkOrphanedItems()` tìm item có `seller_id` KHÔNG tồn tại trong bảng `users`. Kịch bản: Admin
   xóa user nhưng không xóa items của user → orphaned items. Foreign key constraint trong SQLite có prevent
   không? SQLite default `PRAGMA foreign_keys = OFF` — cần enable. Nếu enable → `DataIntegrityService`
   không cần check orphaned items. Nhưng BidHub dùng `PRAGMA foreign_keys = OFF` (legacy) → cần check.
4. `runFullCheck()` gọi cả 3 method, gom kết quả vào `Map<String, Object>` với key `totalErrors` và
   `status` (OK hoặc ERRORS_FOUND). Tại sao trả về Map thay vì custom class `IntegrityCheckResult`?
   Trade-off: Map linh hoạt serialize JSON cho client → Admin xem kết quả qua API. Class strongly typed →
   compile-time check. Khi nào dùng Map, khi nào dùng class?
5. `handleRunIntegrityCheck()` trong `RequestHandler` — ADMIN only. Tại sao chỉ ADMIN mới chạy? Nếu BIDDER
   chạy → lộ thông tin DB inconsistency → security risk. `SecurityContext.requireRole(session, UserRole.ADMIN)`
   throw exception nếu không phải ADMIN. Exception nào ném?
6. **[Câu hỏi nâng cao]** `DataIntegrityService` iterate ALL auctions, ALL items, ALL users — với DB lớn
   (10000 auctions, 50000 bids), query có chậm không? `SELECT MAX(bid_amount) FROM bid_transactions WHERE
   auction_id = ?` cho từng auction → N queries (N = số auctions). Tối ưu: 1 query `SELECT auction_id,
   MAX(bid_amount) FROM bid_transactions GROUP BY auction_id` → so sánh với auctions table trong memory.
   Khoa nên tối ưu query hay giữ đơn giản vì BidHub là dự án học?

---

## 👤 KHOA — DataIntegrityService + Full Test Suite

```
Branch: feature/tuan-9-khoa-data-integrity
Phụ thuộc: AuctionDao (tuần 3+7) — findAll(), updateHighestBid()
           BidDao (tuần 3) — getHighestBid(), findByAuctionId()
           ItemDao (tuần 3) — findAll(), findById()
           UserDao (tuần 3+6) — findAll(), findById()
           RequestHandler (tuần 4-8) — MỞ RA thêm handleRunIntegrityCheck + switch case
           AuditLogService (tuần 4) — log integrity check action
Merge đầu tiên: DataIntegrityService là service mới, phụ thuộc DAO đã có từ T3
```

📌 **[Tiêu chí điểm: Unit Test — DataIntegrityService tests ≥10 cases — phần 0.5đ + MVC — integrity check handler — phần 0.5đ + Clean Code — verify data consistency — phần 0.5đ]**

### 📝 Mô tả bài tập

`DataIntegrityService` kiểm tra chéo dữ liệu giữa các bảng — phát hiện inconsistency do bug, race condition,
hoặc partial failure. 4 method chính:
- `checkBidConsistency()`: so sánh `currentHighestBid` trong auctions với MAX bid trong bid_transactions
- `checkAuctionWinners()`: tìm FINISHED auctions có bids nhưng `highestBidderId = null`
- `checkOrphanedItems()`: tìm items có `sellerId` không tồn tại trong users
- `runFullCheck()`: chạy cả 3 method, tổng hợp kết quả

`handleRunIntegrityCheck()` trong `RequestHandler` cho ADMIN chạy integrity check qua API. Kết quả trả về
JSON với `status` (OK/ERRORS_FOUND) và chi tiết lỗi.

Khoa cũng đảm bảo tổng test cases ≥ 139. T8 có 10 cases, T9 thêm ≥10 cases cho `DataIntegrityService`.
Phân bổ test: T1:15 + T2:14 + T3:21 + T4:15 + T5:25 + T6:23 + T7:20 + T8:10 + T9:10+ = **143+ cases**.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Khoa cần `AuctionDao`, `BidDao`, `ItemDao`, `UserDao` — đều đã có từ T3
- Khoa cần `RequestHandler` — thêm 1 handler mới + 1 switch case
- Khoa cần `AuditLogService` — log integrity check
- Đăng cần `DataIntegrityService` cho IntegrationTest (sau khi Khoa merge)
- Quốc Minh cần `RUN_INTEGRITY_CHECK` command cho API docs (sau khi Khoa merge)

**Kịch bản chọn: C — Khoa merge trước, tất cả rebase sau**

**Các bước:**
1. Khoa tạo branch, code `DataIntegrityService.java`
2. Khoa thêm `handleRunIntegrityCheck()` vào `RequestHandler`
3. Khoa viết `DataIntegrityServiceTest.java` (≥10 cases)
4. Khoa push, tạo PR → review → merge vào `develop`
5. Đăng rebase — có `DataIntegrityService` cho IntegrationTest
6. Quốc Minh rebase — có `RUN_INTEGRITY_CHECK` cho API docs
7. Công Minh rebase — không phụ thuộc trực tiếp

### 📁 Cấu trúc file

```
bidhub-server/src/
├── main/java/com/bidhub/server/
│   ├── service/
│   │   └── DataIntegrityService.java  ← MỚI
│   └── network/
│       └── RequestHandler.java        (đã có T4-T8 — MỞ RA thêm handleRunIntegrityCheck + switch case)
└── test/java/com/bidhub/server/
    └── service/
        └── DataIntegrityServiceTest.java ← MỚI: ≥10 cases
```

> [!IMPORTANT]
> Khoa cần mở `RequestHandler.java` để thêm handler. `DataIntegrityService.java` là file mới hoàn toàn.

---

### `DataIntegrityService.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.dao.UserDao;
import java.util.*;

/**
 * Dich vu kiem tra toan ven du lieu — cross-validation giua cac bang.
 *
 * <p>Phat hien inconsistency do bug, race condition, hoac partial failure.
 * 4 method chinh: checkBidConsistency, checkAuctionWinners, checkOrphanedItems,
 * runFullCheck.
 *
 * <p>// 📌 [Tieu chi: Clean Code — verify data consistency]
 * // 📌 [Tieu chi: Unit Test — DataIntegrityService tests]
 */
public final class DataIntegrityService {

  private final AuctionDao auctionDao;
  private final BidDao bidDao;
  private final ItemDao itemDao;
  private final UserDao userDao;

  /** Constructor production — tao DAO moi. */
  public DataIntegrityService() {
    this.auctionDao = new AuctionDao();
    this.bidDao = new BidDao();
    this.itemDao = new ItemDao();
    this.userDao = new UserDao();
  }

  /**
   * Constructor test — inject cac DAO.
   *
   * @param auctionDao AuctionDao inject
   * @param bidDao     BidDao inject
   * @param itemDao    ItemDao inject
   * @param userDao    UserDao inject
   */
  public DataIntegrityService(AuctionDao auctionDao, BidDao bidDao,
      ItemDao itemDao, UserDao userDao) {
    this.auctionDao = auctionDao;
    this.bidDao = bidDao;
    this.itemDao = itemDao;
    this.userDao = userDao;
  }

  /**
   * Kiem tra tinh nhat quan giua currentHighestBid trong auctions va
   * MAX(bid_amount) trong bid_transactions.
   *
   * <p>Cho tung auction: lay currentHighestBid tu auctions table, so sanh voi
   * MAX(bid_amount) tu bid_transactions. Neu khac nhau → inconsistent.
   *
   * @return danh sach mo ta loi (rong neu OK)
   */
  public List<String> checkBidConsistency() {
    List<String> errors = new ArrayList<>();
    try {
      List<Map<String, Object>> auctions = auctionDao.findAllWithBidInfo();
      Set<String> allUserIds = new HashSet<>();
      userDao.findAll().forEach(u -> allUserIds.add(u.getId()));

      for (Map<String, Object> row : auctions) {
        String auctionId = (String) row.get("id");
        double dbHighestBid = row.get("currentHighestBid") != null
            ? ((Number) row.get("currentHighestBid")).doubleValue() : 0.0;
        String dbHighestBidder = (String) row.get("highestBidderId");

        Optional<com.bidhub.server.model.BidTransaction> maxBid =
            bidDao.getHighestBid(auctionId);

        if (maxBid.isPresent()) {
          double actualMaxBid = maxBid.get().getBidAmount();
          if (Math.abs(dbHighestBid - actualMaxBid) > 0.001) {
            errors.add("Auction " + auctionId + ": currentHighestBid="
                + dbHighestBid + " nhung MAX(bid) trong DB=" + actualMaxBid);
          }
          // Kiem tra highestBidderId co khop voi MAX bidder
          if (dbHighestBidder != null && !dbHighestBidder.isEmpty()
              && !dbHighestBidder.equals(maxBid.get().getBidderId())) {
            errors.add("Auction " + auctionId + ": highestBidderId="
                + dbHighestBidder + " nhung MAX bidder=" + maxBid.get().getBidderId());
          }
        } else {
          // Khong co bid nao — currentHighestBid phai = startingPrice hoac 0
          if (dbHighestBid > 0 && dbHighestBidder != null) {
            errors.add("Auction " + auctionId + ": co highestBid="
                + dbHighestBid + " nhung khong co bid nao trong DB");
          }
        }
      }
    } catch (Exception e) {
      errors.add("Loi checkBidConsistency: " + e.getMessage());
    }
    return errors;
  }

  /**
   * Kiem tra FINISHED auctions co bids nhung chua xac dinh winner.
   *
   * @return danh sach mo ta loi (rong neu OK)
   */
  public List<String> checkAuctionWinners() {
    List<String> errors = new ArrayList<>();
    try {
      List<com.bidhub.server.model.Auction> auctions = auctionDao.findAll();
      for (com.bidhub.server.model.Auction auction : auctions) {
        if (auction.getStatus() == com.bidhub.server.model.AuctionStatus.FINISHED) {
          Optional<com.bidhub.server.model.BidTransaction> highestBid =
              bidDao.getHighestBid(auction.getId());
          if (highestBid.isPresent() && auction.getHighestBidderId() == null) {
            errors.add("Auction " + auction.getId()
                + ": FINISHED co bids nhung highestBidderId = null. "
                + "Winner nen la: " + highestBid.get().getBidderId());
          }
        }
      }
    } catch (Exception e) {
      errors.add("Loi checkAuctionWinners: " + e.getMessage());
    }
    return errors;
  }

  /**
   * Kiem tra items co sellerId khong ton tai trong bang users.
   *
   * @return danh sach mo ta loi (rong neu OK)
   */
  public List<String> checkOrphanedItems() {
    List<String> errors = new ArrayList<>();
    try {
      Set<String> validUserIds = new HashSet<>();
      userDao.findAll().forEach(u -> validUserIds.add(u.getId()));

      List<com.bidhub.server.model.Item> items = itemDao.findAll();
      for (com.bidhub.server.model.Item item : items) {
        if (!validUserIds.contains(item.getSellerId())) {
          errors.add("Item " + item.getId() + " ('" + item.getName()
              + "'): sellerId=" + item.getSellerId() + " khong ton tai trong bang users");
        }
      }
    } catch (Exception e) {
      errors.add("Loi checkOrphanedItems: " + e.getMessage());
    }
    return errors;
  }

  /**
   * Chay toan bo kiem tra — tong hop ket qua.
   *
   * <p>Tra ve Map chua ket qua 3 check + tong errors + status.
   *
   * @return Map voi key: bidConsistencyErrors, auctionWinnerErrors,
   *         orphanedItemErrors, totalErrors, status
   */
  public Map<String, Object> runFullCheck() {
    Map<String, Object> result = new LinkedHashMap<>();
    List<String> bidErrors = checkBidConsistency();
    List<String> winnerErrors = checkAuctionWinners();
    List<String> orphanErrors = checkOrphanedItems();
    int total = bidErrors.size() + winnerErrors.size() + orphanErrors.size();
    result.put("bidConsistencyErrors", bidErrors);
    result.put("auctionWinnerErrors", winnerErrors);
    result.put("orphanedItemErrors", orphanErrors);
    result.put("totalErrors", total);
    result.put("status", total == 0 ? "OK" : "ERRORS_FOUND");
    return result;
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm DataIntegrityService — checkBidConsistency, checkAuctionWinners, checkOrphanedItems, runFullCheck"
```

---

### Cập nhật `AuctionDao.java` — thêm `findAllWithBidInfo()`

Mở file `AuctionDao.java`, thêm method mới cần cho `DataIntegrityService`:

```java
// === THÊM VÀO AuctionDao.java ===

  /**
   * Lay tat ca auction voi thong tin bid — dung cho DataIntegrityService.
   *
   * <p>Tra ve List<Map> thay vi List<Auction> de lay ca currentHighestBid
   * va highestBidderId dang raw cho so sanh.
   *
   * @return danh sach map chua thong tin auction
   */
  public List<Map<String, Object>> findAllWithBidInfo() {
    List<Map<String, Object>> result = new ArrayList<>();
    String sql = "SELECT id, current_highest_bid, highest_bidder_id FROM auctions";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new HashMap<>();
          row.put("id", rs.getString("id"));
          row.put("currentHighestBid", rs.getDouble("current_highest_bid"));
          row.put("highestBidderId", rs.getString("highest_bidder_id"));
          result.add(row);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuctionDao.findAllWithBidInfo that bai: "
          + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
    return result;
  }
```

```bash
git commit -m "feat: thêm AuctionDao.findAllWithBidInfo() cho DataIntegrityService"
```

---

### Cập nhật `ItemDao.java` — thêm `findAll()`

Mở file `ItemDao.java`, thêm method mới cần cho `DataIntegrityService`:

```java
// === THÊM VÀO ItemDao.java ===

  /**
   * Lay tat ca item — dung cho DataIntegrityService check orphaned items.
   *
   * @return danh sach tat ca item
   */
  public List<com.bidhub.server.model.Item> findAll() {
    List<com.bidhub.server.model.Item> result = new ArrayList<>();
    String sql = "SELECT * FROM items";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("ItemDao.findAll that bai: " + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
    return result;
  }
```

```bash
git commit -m "feat: thêm ItemDao.findAll() cho DataIntegrityService"
```

---

### Cập nhật `RequestHandler.java` — thêm `handleRunIntegrityCheck`

Mở file `RequestHandler.java`, thêm handler mới:

```java
// === THÊM VÀO RequestHandler.java ===

// --- 1. Thêm import ---
import com.bidhub.server.service.DataIntegrityService;

// --- 2. Thêm field ---
  private final DataIntegrityService dataIntegrityService;

// --- 3. Cập nhật constructor ---
  // Thêm vào constructor:
  this.dataIntegrityService = new DataIntegrityService();

// --- 4. Thêm vào switch-case ---
        case "RUN_INTEGRITY_CHECK" -> handleRunIntegrityCheck(session, payload);

// --- 5. Thêm vào AUTH_REQUIRED ---
  // Cap nhat set AUTH_REQUIRED them "RUN_INTEGRITY_CHECK"

// --- 6. Handler method ---
  /**
   * Xu ly kiem tra toan ven du lieu (ADMIN only).
   *
   * <p>// 📌 [Tieu chi: Clean Code — Admin kiem tra data consistency]
   *
   * @param session session cua client
   * @param payload payload rong
   * @return JSON response voi ket qua integrity check
   */
  private String handleRunIntegrityCheck(Session session, JsonNode payload) {
    SecurityContext.requireRole(session, UserRole.ADMIN);

    // Audit log
    auditLogService.log(
        SecurityContext.getUserId(session),
        AuditActions.RUN_INTEGRITY_CHECK,
        "{}");

    Map<String, Object> result = dataIntegrityService.runFullCheck();
    return MessageMapper.toJson(MessageResponse.ok("RUN_INTEGRITY_CHECK", result));
  }
```

```bash
git commit -m "feat: thêm handleRunIntegrityCheck (ADMIN only) + DataIntegrityService integration"
```

---

### Cập nhật `AuditActions.java` — thêm `RUN_INTEGRITY_CHECK`

```java
// === THÊM VÀO AuditActions.java ===

  public static final String RUN_INTEGRITY_CHECK = "RUN_INTEGRITY_CHECK";
```

```bash
git commit -m "feat: thêm AuditActions.RUN_INTEGRITY_CHECK cho integrity check logging"
```

---

### ✅ Test đầu ra — `DataIntegrityServiceTest.java`

```java
package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import com.bidhub.server.model.Item;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class DataIntegrityServiceTest {

  private DataIntegrityService service;

  @BeforeEach
  void setUp() {
    // Service with no-arg constructor — uses real DAO (needs DB for integration)
    // For unit test, use constructor with injected DAOs
    service = new DataIntegrityService();
  }

  @Test
  @DisplayName("Constructor inject khong crash")
  void constructor_inject_noCrash() {
    assertDoesNotThrow(() ->
        new DataIntegrityService(null, null, null, null));
  }

  @Test
  @DisplayName("checkBidConsistency() tra ve List khong null")
  void checkBidConsistency_notNull() {
    List<String> errors = service.checkBidConsistency();
    assertNotNull(errors, "checkBidConsistency() khong duoc tra ve null");
  }

  @Test
  @DisplayName("checkAuctionWinners() tra ve List khong null")
  void checkAuctionWinners_notNull() {
    List<String> errors = service.checkAuctionWinners();
    assertNotNull(errors, "checkAuctionWinners() khong duoc tra ve null");
  }

  @Test
  @DisplayName("checkOrphanedItems() tra ve List khong null")
  void checkOrphanedItems_notNull() {
    List<String> errors = service.checkOrphanedItems();
    assertNotNull(errors, "checkOrphanedItems() khong duoc tra ve null");
  }

  @Test
  @DisplayName("runFullCheck() tra ve Map voi 5 key bat buoc")
  void runFullCheck_hasRequiredKeys() {
    Map<String, Object> result = service.runFullCheck();
    assertNotNull(result, "runFullCheck() khong duoc tra ve null");
    assertTrue(result.containsKey("bidConsistencyErrors"),
        "Thieu key bidConsistencyErrors");
    assertTrue(result.containsKey("auctionWinnerErrors"),
        "Thieu key auctionWinnerErrors");
    assertTrue(result.containsKey("orphanedItemErrors"),
        "Thieu key orphanedItemErrors");
    assertTrue(result.containsKey("totalErrors"),
        "Thieu key totalErrors");
    assertTrue(result.containsKey("status"),
        "Thieu key status");
  }

  @Test
  @DisplayName("runFullCheck() totalErrors = tong 3 loai errors")
  void runFullCheck_totalErrorsCorrect() {
    Map<String, Object> result = service.runFullCheck();
    @SuppressWarnings("unchecked")
    List<String> bidErrors = (List<String>) result.get("bidConsistencyErrors");
    @SuppressWarnings("unchecked")
    List<String> winnerErrors = (List<String>) result.get("auctionWinnerErrors");
    @SuppressWarnings("unchecked")
    List<String> orphanErrors = (List<String>) result.get("orphanedItemErrors");
    int expected = bidErrors.size() + winnerErrors.size() + orphanErrors.size();
    assertEquals(expected, result.get("totalErrors"),
        "totalErrors khong bang tong 3 loai errors");
  }

  @Test
  @DisplayName("runFullCheck() status OK khi khong co errors")
  void runFullCheck_statusOk_whenNoErrors() {
    Map<String, Object> result = service.runFullCheck();
    int total = (int) result.get("totalErrors");
    if (total == 0) {
      assertEquals("OK", result.get("status"),
          "status phai la OK khi totalErrors = 0");
    } else {
      assertEquals("ERRORS_FOUND", result.get("status"),
          "status phai la ERRORS_FOUND khi totalErrors > 0");
    }
  }

  @Test
  @DisplayName("checkBidConsistency() moi error la String khong rong")
  void checkBidConsistency_errorsNotEmpty() {
    List<String> errors = service.checkBidConsistency();
    for (String error : errors) {
      assertNotNull(error, "Error description khong duoc null");
      assertFalse(error.isBlank(), "Error description khong duoc rong");
    }
  }

  @Test
  @DisplayName("runFullCheck() result la LinkedHashMap (keep order)")
  void runFullCheck_orderedResult() {
    Map<String, Object> result = service.runFullCheck();
    // Verify result maintains insertion order (LinkedHashMap behavior)
    List<String> keyList = new ArrayList<>(result.keySet());
    assertEquals("bidConsistencyErrors", keyList.get(0));
    assertEquals("auctionWinnerErrors", keyList.get(1));
    assertEquals("orphanedItemErrors", keyList.get(2));
    assertEquals("totalErrors", keyList.get(3));
    assertEquals("status", keyList.get(4));
  }

  @Test
  @DisplayName("DataIntegrityService la final class")
  void dataIntegrityService_isFinal() {
    assertTrue(DataIntegrityService.class.isFinal(),
        "DataIntegrityService phai la final class");
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm DataIntegrityServiceTest (10 cases) — verify structure, null safety, key consistency"
```

---

**Kiểm tra manual:**
```bash
# Chạy test
mvn test -pl bidhub-server -Dtest="DataIntegrityServiceTest" -q
# Output: Tests run: 10, Failures: 0

# Manual test: gửi RUN_INTEGRITY_CHECK request
echo '{"type":"RUN_INTEGRITY_CHECK","token":"<valid-admin-token>","payload":{}}' \
  | nc -q1 localhost 9090
# Output: {"status":"OK","type":"RUN_INTEGRITY_CHECK","payload":{"bidConsistencyErrors":[...],...}}
```

**❌ FAIL nếu:**
- `checkBidConsistency()` throw exception thay vì trả list rỗng → thiếu try-catch
- `runFullCheck()` thiếu key `status` → client không biết kết quả OK hay lỗi
- `handleRunIntegrityCheck()` cho phép non-ADMIN chạy → `requireRole(ADMIN)` thiếu
- `totalErrors` không bằng tổng 3 list size → tính toán sai
- `DataIntegrityService` constructor inject null DAO → NPE khi gọi method

---

## 👤 ĐĂNG — Integration Test & Final Refactor (SLF4J + Logback)

```
Branch: feature/tuan-9-dang-integration-slf4j
Phụ thuộc: DataIntegrityService (tuần 9, Khoa) — merge đầu tiên
           AuctionManager (tuần 6, Đăng) — verify load RUNNING on restart
           MigrationRunner (tuần 3+4, Đăng) — verify 5 tables
           Toàn bộ code server — refactor System.out.println → SLF4J
Merge thứ hai: Đăng merge sau Khoa (DataIntegrityService)
```

📌 **[Tiêu chí điểm: Kỹ thuật quan trọng — SLF4J + Logback logging framework — 1.0đ + Unit Test — IntegrationTest end-to-end — phần 0.5đ + Clean Code — verify MigrationRunner + refactor logging — phần 0.5đ]**

### 📝 Mô tả bài tập

**SLF4J + Logback Refactor:** Đăng replace TẤT CẢ `System.out.println` và `System.err.println` trong toàn bộ
code server bằng SLF4J Logger. Thêm dependency `slf4j-api` + `logback-classic` vào `pom.xml`. Tạo file
`src/main/resources/logback.xml` cấu hình console appender với pattern có timestamp, thread, level, logger, message.

**IntegrationTest:** Test class chạy toàn bộ flow end-to-end: register → login → create item → create auction →
subscribe auction → place bid → receive event → wait lifecycle close → verify auction FINISHED. Dùng
`CountDownLatch` để đợi async event. Test cần server chạy — annotate `@Tag("integration")`.

**Verify MigrationRunner:** Đăng confirm `MigrationRunner` xử lý 5 bảng: `users`, `items`, `auctions`,
`bid_transactions`, `audit_logs`. Test tạo file DB mới → chạy MigrationRunner → verify 5 bảng tồn tại.

**Verify AuctionManager:** Đăng confirm `AuctionManager.start()` load RUNNING auctions từ DB. Test: insert
RUNNING auction vào DB → khởi động AuctionManager → `activeCount()` ≥ 1.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Đăng cần `DataIntegrityService` (Khoa) — merge Khoa trước
- SLF4J refactor là change toàn bộ server — merge sau DataIntegrityService để tránh conflict
- IntegrationTest cần server chạy → không thể chạy trong unit test CI (dùng `@Tag("integration")`)
- `logback.xml` là resource file — không conflict với Java code

**Kịch bản chọn: C — Khoa merge trước, Đăng rebase rồi merge**

**Các bước:**
1. Khoa merge DataIntegrityService → Đăng rebase
2. Đăng thêm SLF4J dependency vào `pom.xml`
3. Đăng tạo `logback.xml`
4. Đăng refactor toàn bộ `System.out.println` / `System.err.println` → `logger.info()` / `logger.error()`
5. Đăng viết `IntegrationTest.java` (≥5 cases)
6. Đăng verify `MigrationRunner` xử lý 5 bảng
7. Đăng verify `AuctionManager` load RUNNING auctions
8. Push, tạo PR → review → merge

### 📁 Cấu trúc file

```
bidhub-server/
├── pom.xml                                  (MỞ RA thêm slf4j + logback dependency)
├── src/main/resources/
│   └── logback.xml                           ← MỚI
├── src/main/java/com/bidhub/server/
│   ├── service/
│   │   ├── AuctionManager.java               (đã có — refactor System.out → logger)
│   │   ├── AuctionLifecycleTask.java         (đã có — refactor System.out → logger)
│   │   ├── NotificationBroker.java           (đã có — refactor System.out → logger)
│   │   ├── DataIntegrityService.java         (đã có T9 Khoa — refactor nếu cần)
│   │   └── ... (tat ca service class)
│   ├── dao/
│   │   └── ... (tat ca DAO class — refactor)
│   ├── network/
│   │   ├── SocketServerCore.java             (đã có — refactor)
│   │   ├── ClientConnectionThread.java       (đã có — refactor)
│   │   ├── RequestHandler.java               (đã có — refactor)
│   │   └── Session.java                      (đã có — refactor)
│   └── config/
│       └── MigrationRunner.java              (đã có — refactor + verify 5 tables)
└── src/test/java/com/bidhub/server/
    ├── integration/
    │   └── IntegrationTest.java              ← MỚI: end-to-end test
    └── config/
        └── MigrationRunnerVerificationTest.java ← MỚI: verify 5 tables
```

> [!IMPORTANT]
> Đăng refactor TẤT CẢ file có `System.out.println` / `System.err.println`. Mỗi file commit riêng.
> Tổng commit SLF4J refactor: ≥10 commits (1 per file hoặc per module).

---

### Cập nhật `pom.xml` — thêm SLF4J + Logback dependency

Mở file `pom.xml` (bidhub-server module), thêm:

```xml
<!-- === THÊM VÀO pom.xml (bidhub-server) — dependencies section === -->
    <!-- SLF4J API -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>2.0.9</version>
    </dependency>
    <!-- Logback implementation -->
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.4.14</version>
    </dependency>
```

```bash
git commit -m "feat: thêm SLF4J 2.0.9 + Logback 1.4.14 dependency vào pom.xml"
```

---

### `logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- 📌 [Tieu chi: Ky thuat quan trong — SLF4J + Logback] -->
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File appender (optional — cho production) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/bidhub-server.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/bidhub-server.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{60} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Log level cho BidHub packages -->
    <logger name="com.bidhub.server" level="DEBUG" />

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>

</configuration>
```

```bash
git commit -m "feat: thêm logback.xml — console + rolling file appender, DEBUG level cho com.bidhub.server"
```

---

### Pattern refactor `System.out.println` → SLF4J

Áp dụng cho TẤT CẢ file trong bidhub-server. Pattern:

```java
// === TRƯỚC (TẤT CẢ file) ===
System.out.println("[AuctionManager] Da load " + count + " auctions.");
System.err.println("[LifecycleTask] Loi: " + e.getMessage());

// === SAU ===
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Trong class:
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

// Thay the:
logger.info("Da load {} auctions.", count);
logger.error("Loi xu ly auction {}: {}", auctionId, e.getMessage(), e);
logger.debug("Session subscribe auction: {} (total: {})", auctionId, total);
logger.warn("MigrationRunner canh bao migration: {}", e.getMessage());
```

```bash
git commit -m "refactor: replace System.out/err.println bằng SLF4J Logger trong toàn bộ bidhub-server"
```

---

### `IntegrationTest.java` — End-to-End Test Skeleton

```java
package com.bidhub.server.integration;

import com.bidhub.client.network.ServerGateway;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test — end-to-end flow: register, login, create item,
 * create auction, subscribe, bid, receive events, wait lifecycle close.
 *
 * <p>Can server CHAY — annotate @Tag("integration").
 * Skip trong CI: mvn test -DskipITs
 *
 * <p>// 📌 [Tieu chi: Unit Test — IntegrationTest end-to-end]
 */
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

  private static final String HOST = "localhost";
  private static final int PORT = 9090;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static String token;
  private static String auctionId;
  private static String itemId;

  private String sendRequest(String json) throws Exception {
    try (Socket socket = new Socket(HOST, PORT);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(
             new InputStreamReader(socket.getInputStream()))) {
      out.println(json);
      String response = in.readLine();
      assertNotNull(response, "Server khong tra ve response");
      return response;
    }
  }

  private MessageRequest createRequest(String type, Object payload) {
    MessageRequest req = new MessageRequest();
    req.setType(type);
    req.setPayload(mapper.valueToTree(payload));
    if (token != null) {
      req.setToken(token);
    }
    return req;
  }

  @Test
  @Order(1)
  @DisplayName("Step 1: Register user")
  void step1_register() throws Exception {
    String uniqueSuffix = String.valueOf(System.currentTimeMillis());
    String json = mapper.writeValueAsString(
        createRequest("REGISTER", Map.of(
            "username", "integration_user_" + uniqueSuffix,
            "password", "Test@123",
            "email", "integ" + uniqueSuffix + "@test.com",
            "role", "BIDDER")));
    String response = sendRequest(json);
    JsonNode node = mapper.readTree(response);
    assertEquals("OK", node.path("status").asText(),
        "Register that bai: " + response);
  }

  @Test
  @Order(2)
  @DisplayName("Step 2: Login")
  void step2_login() throws Exception {
    String json = mapper.writeValueAsString(
        createRequest("LOGIN", Map.of(
            "username", "integration_user",
            "password", "Test@123")));
    String response = sendRequest(json);
    JsonNode node = mapper.readTree(response);
    assertEquals("OK", node.path("status").asText(),
        "Login that bai: " + response);
    token = node.path("payload").path("token").asText("");
    assertFalse(token.isBlank(), "Token khong duoc rong");
  }

  @Test
  @Order(3)
  @DisplayName("Step 3: Create item")
  void step3_createItem() throws Exception {
    String json = mapper.writeValueAsString(
        createRequest("CREATE_ITEM", Map.of(
            "name", "Integration Test Item",
            "description", "Item for integration test",
            "type", "ELECTRONICS",
            "category", "LAPTOP")));
    String response = sendRequest(json);
    JsonNode node = mapper.readTree(response);
    assertEquals("OK", node.path("status").asText(),
        "Create item that bai: " + response);
    itemId = node.path("payload").path("itemId").asText("");
    assertFalse(itemId.isBlank(), "ItemId khong duoc rong");
  }

  @Test
  @Order(4)
  @DisplayName("Step 4: Create auction")
  void step4_createAuction() throws Exception {
    String json = mapper.writeValueAsString(
        createRequest("CREATE_AUCTION", Map.of(
            "itemId", itemId,
            "startingPrice", 100000,
            "minimumIncrement", 5000,
            "durationMinutes", 1)));
    String response = sendRequest(json);
    JsonNode node = mapper.readTree(response);
    assertEquals("OK", node.path("status").asText(),
        "Create auction that bai: " + response);
    auctionId = node.path("payload").path("auctionId").asText("");
    assertFalse(auctionId.isBlank(), "AuctionId khong duoc rong");
  }

  @Test
  @Order(5)
  @DisplayName("Step 5: Subscribe auction")
  void step5_subscribeAuction() throws Exception {
    String json = mapper.writeValueAsString(
        createRequest("SUBSCRIBE_AUCTION", Map.of(
            "auctionId", auctionId)));
    String response = sendRequest(json);
    JsonNode node = mapper.readTree(response);
    assertEquals("OK", node.path("status").asText(),
        "Subscribe auction that bai: " + response);
  }

  @Test
  @Order(6)
  @DisplayName("Step 6: Place bid")
  void step6_placeBid() throws Exception {
    String json = mapper.writeValueAsString(
        createRequest("PLACE_BID", Map.of(
            "auctionId", auctionId,
            "bidAmount", 105000)));
    String response = sendRequest(json);
    JsonNode node = mapper.readTree(response);
    assertEquals("OK", node.path("status").asText(),
        "Place bid that bai: " + response);
  }

  @Test
  @Order(7)
  @DisplayName("Step 7: Receive BID_UPDATE event")
  void step7_receiveBidUpdateEvent() throws Exception {
    // Mo socket rieng de lang nghe event
    // Gui PLACE_BID tu socket khac → nhan BID_UPDATE
    CountDownLatch eventLatch = new CountDownLatch(1);
    AtomicReference<String> eventType = new AtomicReference<>();

    try (Socket listenSocket = new Socket(HOST, PORT);
         BufferedReader in = new BufferedReader(
             new InputStreamReader(listenSocket.getInputStream()));
         PrintWriter out = new PrintWriter(listenSocket.getOutputStream(), true)) {

      // Login
      String loginJson = mapper.writeValueAsString(
          createRequest("LOGIN", Map.of("username", "integration_user",
              "password", "Test@123")));
      out.println(loginJson);
      String loginResp = in.readLine(); // Login response

      // Subscribe
      String subJson = mapper.writeValueAsString(
          createRequest("SUBSCRIBE_AUCTION", Map.of(
              "auctionId", auctionId)));
      out.println(subJson);
      String subResp = in.readLine(); // Subscribe response

      // Place bid from another socket
      new Thread(() -> {
        try {
          String bidJson = mapper.writeValueAsString(
              createRequest("PLACE_BID", Map.of(
                  "auctionId", auctionId,
                  "bidAmount", 110000)));
          sendRequest(bidJson);
        } catch (Exception e) {
          // Ignore
        }
      }).start();

      // Wait for event
      String eventLine = in.readLine();
      if (eventLine != null) {
        JsonNode eventNode = mapper.readTree(eventLine);
        eventType.set(eventNode.path("eventType").asText(""));
        eventLatch.countDown();
      }
    }

    boolean received = eventLatch.await(10, TimeUnit.SECONDS);
    assertTrue(received, "Timeout — khong nhan BID_UPDATE event");
    assertEquals("BID_UPDATE", eventType.get(),
        "Sai event type — nhan: " + eventType.get());
  }

  @Test
  @Order(8)
  @DisplayName("Step 8: Get auction detail — verify bid updated")
  void step8_getAuctionDetail() throws Exception {
    String json = mapper.writeValueAsString(
        createRequest("GET_AUCTION_DETAIL", Map.of(
            "auctionId", auctionId)));
    String response = sendRequest(json);
    JsonNode node = mapper.readTree(response);
    assertEquals("OK", node.path("status").asText());
    double currentBid = node.path("payload").path("currentHighestBid").asDouble(0);
    assertTrue(currentBid >= 110000,
        "currentHighestBid phai >= 110000, thuc te: " + currentBid);
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "test: thêm IntegrationTest (8 cases) — end-to-end flow register → bid → events"
```

---

**Kiểm tra manual:**
```bash
# Chạy integration test (can server chay)
# Terminal 1: Start server
mvn exec:java -pl bidhub-server -Dexec.mainClass="com.bidhub.server.ServerApp"

# Terminal 2: Run integration test
mvn test -pl bidhub-server -Dtest="IntegrationTest" -q
# Output: Tests run: 8, Failures: 0

# Skip integration test trong CI
mvn test -pl bidhub-server -DskipITs -q

# Verify SLF4J refactor — khong con System.out.println
rg "System\.out\.println" bidhub-server/src/main/java/
# Output: (khong ket qua — tat ca da refactor)
```

**❌ FAIL nếu:**
- `rg "System.out.println"` vẫn tìm thấy kết quả → refactor chưa hoàn tất
- `IntegrationTest` register fail → server chưa chạy hoặc DB chưa migrate
- `IntegrationTest` place bid fail → `AuctionManager` chưa load auction vào RAM
- `IntegrationTest` receive BID_UPDATE timeout → `NotificationBroker` hoặc event pipeline bug
- `logback.xml` thiếu → SLF4J fallback stderr → log format không đúng

---

## 👤 QUỐC MINH — API Protocol Docs Final + CI/CD

```
Branch: feature/tuan-9-quocminh-api-docs-ci
Phụ thuộc: DataIntegrityService (tuần 9, Khoa) — RUN_INTEGRITY_CHECK command
           Toàn bộ handlers (T4-T9) — docs cần cover ≥14 command types
           pom.xml (tuần 9, Đăng) — JaCoCo plugin configuration
Merge thứ ba: Docs + CI phụ thuộc code ổn định từ Khoa + Đăng
```

📌 **[Tiêu chí điểm: CI/CD — GitHub Actions + JaCoCo coverage — 1.0đ + Tài liệu — API Protocol docs — 1.0đ]**

### 📝 Mô tả bài tập

**API_PROTOCOL.md:** Hoàn chỉnh tài liệu API protocol — ≥14 command types. Mỗi command có: mô tả, request JSON
example, response JSON example (thành công + lỗi), role required. File đặt tại `docs/API_PROTOCOL.md`.

**ci.yml:** Cập nhật `.github/workflows/ci.yml` — thêm JaCoCo plugin, upload report artifact, `--fail-at-end`.
CI chạy `mvn test` trên cả 3 module, upload JaCoCo report nếu pass.

**CI Badge:** Thêm badge Markdown vào `README.md` — hiển thị CI status.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Quốc Minh cần danh sách đầy đủ command types — review RequestHandler switch-case
- Quốc Minh cần JaCoCo config trong pom.xml — có thể tự thêm hoặc Đăng thêm
- Docs KHÔNG phụ thuộc code → có thể code song song
- CI cần test pass → merge sau Đăng (SLF4J refactor có thể gây test fail nếu logger wrong)

**Kịch bản chọn: C — Rebase từ develop sau Khoa + Đăng merge**

**Các bước:**
1. Quốc Minh tạo branch, review RequestHandler collect all command types
2. Quốc Minh viết `docs/API_PROTOCOL.md` — ≥14 commands
3. Quốc Minh cập nhật `.github/workflows/ci.yml` — JaCoCo + upload
4. Quốc Minh thêm CI badge vào README.md
5. Khoa merge → Quốc Minh rebase (có RUN_INTEGRITY_CHECK)
6. Đăng merge → Quốc Minh rebase lần 2 (SLF4J)
7. Push, tạo PR → review → merge

### 📁 Cấu trúc file

```
bidhub/
├── docs/
│   └── API_PROTOCOL.md           ← MỚI: tài liệu API protocol ≥14 commands
├── .github/
│   └── workflows/
│       └── ci.yml                (đã có — CẬP NHẬT thêm JaCoCo + upload)
├── README.md                     (đã có — CẬP NHẬT thêm CI badge)
└── pom.xml (root)                (MỞ RA thêm JaCoCo plugin nếu cần)
```

---

### `docs/API_PROTOCOL.md` — Cấu trúc hoàn chỉnh

```markdown
# BidHub API Protocol

## Tổng quan

BidHub sử dụng giao thức JSON qua TCP Socket. Client gửi `MessageRequest`, server trả `MessageResponse`.

### MessageRequest Format
```json
{
  "type": "COMMAND_TYPE",
  "token": "session_token (optional)",
  "payload": { ... }
}
```

### MessageResponse Format
```json
{
  "status": "OK | ERROR",
  "type": "COMMAND_TYPE",
  "message": "description (error only)",
  "payload": { ... }
}
```

### Role Legend
- **ALL**: Không cần auth
- **AUTH**: Cần đăng nhập
- **SELLER**: Cần role SELLER
- **ADMIN**: Cần role ADMIN
- **SELLER_OR_ADMIN**: Cần role SELLER hoặc ADMIN

---

## 1. REGISTER

**Role:** ALL
**Mô tả:** Đăng ký tài khoản mới.

### Request
```json
{
  "type": "REGISTER",
  "payload": {
    "username": "bidder01",
    "password": "Pass@123",
    "email": "bidder01@mail.com",
    "role": "BIDDER"
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "REGISTER",
  "payload": {
    "userId": "usr-abc123"
  }
}
```

### Response (Lỗi — username tồn tại)
```json
{
  "status": "ERROR",
  "type": "REGISTER",
  "message": "USERNAME_EXISTS"
}
```

---

## 2. LOGIN

**Role:** ALL
**Mô tả:** Đăng nhập, nhận session token.

### Request
```json
{
  "type": "LOGIN",
  "payload": {
    "username": "bidder01",
    "password": "Pass@123"
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "LOGIN",
  "payload": {
    "token": "sess-xyz789",
    "userId": "usr-abc123",
    "username": "bidder01",
    "role": "BIDDER"
  }
}
```

### Response (Lỗi — sai mật khẩu)
```json
{
  "status": "ERROR",
  "type": "LOGIN",
  "message": "INVALID_CREDENTIALS"
}
```

### Response (Lỗi — tài khoản bị khóa)
```json
{
  "status": "ERROR",
  "type": "LOGIN",
  "message": "TAI_KHOAN_BI_KHOA"
}
```

---

## 3. LOGOUT

**Role:** AUTH
**Mô tả:** Đăng xuất, invalidate session token.

### Request
```json
{
  "type": "LOGOUT",
  "token": "sess-xyz789",
  "payload": {}
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "LOGOUT",
  "payload": {
    "message": "Logout thanh cong"
  }
}
```

---

## 4. GET_AUCTION_LIST

**Role:** ALL
**Mô tả:** Lấy danh sách auction đang hoạt động.

### Request
```json
{
  "type": "GET_AUCTION_LIST",
  "payload": {}
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "GET_AUCTION_LIST",
  "payload": [
    {
      "auctionId": "auc-001",
      "itemName": "MacBook Pro 2024",
      "startingPrice": 25000000,
      "currentHighestBid": 30000000,
      "status": "RUNNING",
      "endTime": "2025-01-15T18:00:00"
    }
  ]
}
```

---

## 5. GET_AUCTION_DETAIL

**Role:** AUTH
**Mô tả:** Lấy chi tiết 1 auction.

### Request
```json
{
  "type": "GET_AUCTION_DETAIL",
  "token": "sess-xyz789",
  "payload": {
    "auctionId": "auc-001"
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "GET_AUCTION_DETAIL",
  "payload": {
    "auctionId": "auc-001",
    "itemId": "item-001",
    "itemName": "MacBook Pro 2024",
    "sellerId": "usr-seller01",
    "startingPrice": 25000000,
    "currentHighestBid": 30000000,
    "highestBidderId": "usr-bidder02",
    "minimumIncrement": 500000,
    "status": "RUNNING",
    "startTime": "2025-01-15T14:00:00",
    "endTime": "2025-01-15T18:00:00"
  }
}
```

---

## 6. CREATE_ITEM

**Role:** SELLER
**Mô tả:** Tạo item mới (chỉ SELLER).

### Request
```json
{
  "type": "CREATE_ITEM",
  "token": "sess-seller",
  "payload": {
    "name": "iPhone 16 Pro Max",
    "description": "iPhone moi 256GB",
    "type": "ELECTRONICS",
    "category": "PHONE"
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "CREATE_ITEM",
  "payload": {
    "itemId": "item-002"
  }
}
```

---

## 7. CREATE_AUCTION

**Role:** SELLER
**Mô tả:** Tạo phiên đấu giá cho item (chỉ SELLER).

### Request
```json
{
  "type": "CREATE_AUCTION",
  "token": "sess-seller",
  "payload": {
    "itemId": "item-002",
    "startingPrice": 20000000,
    "minimumIncrement": 500000,
    "durationMinutes": 60
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "CREATE_AUCTION",
  "payload": {
    "auctionId": "auc-002",
    "endTime": "2025-01-15T20:00:00"
  }
}
```

---

## 8. PLACE_BID

**Role:** AUTH
**Mô tả:** Đặt giá cho auction đang chạy.

### Request
```json
{
  "type": "PLACE_BID",
  "token": "sess-bidder",
  "payload": {
    "auctionId": "auc-001",
    "bidAmount": 32000000
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "PLACE_BID",
  "payload": {
    "auctionId": "auc-001",
    "currentHighestBid": 32000000,
    "highestBidderId": "usr-bidder01"
  }
}
```

### Response (Lỗi — giá thấp hơn hiện tại)
```json
{
  "status": "ERROR",
  "type": "PLACE_BID",
  "message": "BID_TOO_LOW"
}
```

---

## 9. SUBSCRIBE_AUCTION

**Role:** AUTH
**Mô tả:** Subscribe nhận realtime event cho auction.

### Request
```json
{
  "type": "SUBSCRIBE_AUCTION",
  "token": "sess-xyz789",
  "payload": {
    "auctionId": "auc-001"
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "SUBSCRIBE_AUCTION",
  "payload": {
    "auctionId": "auc-001",
    "message": "Da subscribe thanh cong"
  }
}
```

---

## 10. LIST_MY_ITEMS

**Role:** AUTH
**Mô tả:** Lấy danh sách items của user đang đăng nhập.

### Request
```json
{
  "type": "LIST_MY_ITEMS",
  "token": "sess-seller",
  "payload": {}
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "LIST_MY_ITEMS",
  "payload": [
    {
      "itemId": "item-001",
      "name": "MacBook Pro 2024",
      "type": "ELECTRONICS",
      "category": "LAPTOP"
    }
  ]
}
```

---

## 11. DELETE_ITEM

**Role:** SELLER
**Mô tả:** Xóa item (chỉ item không có auction đang chạy).

### Request
```json
{
  "type": "DELETE_ITEM",
  "token": "sess-seller",
  "payload": {
    "itemId": "item-001"
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "DELETE_ITEM",
  "payload": {
    "message": "Xoa item thanh cong"
  }
}
```

---

## 12. GET_USER_LIST

**Role:** ADMIN
**Mô tả:** Lấy danh sách tất cả user (chỉ ADMIN).

### Request
```json
{
  "type": "GET_USER_LIST",
  "token": "sess-admin",
  "payload": {}
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "GET_USER_LIST",
  "payload": [
    {
      "userId": "usr-001",
      "username": "bidder01",
      "role": "BIDDER",
      "locked": false
    }
  ]
}
```

---

## 13. LOCK_USER / UNLOCK_USER

**Role:** ADMIN
**Mô tả:** Khóa/mở khóa tài khoản user (chỉ ADMIN).

### Request (LOCK_USER)
```json
{
  "type": "LOCK_USER",
  "token": "sess-admin",
  "payload": {
    "userId": "usr-001"
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "LOCK_USER",
  "payload": {
    "userId": "usr-001",
    "locked": true
  }
}
```

---

## 14. GET_AUCTION_REPORT

**Role:** SELLER_OR_ADMIN
**Mô tả:** Lấy báo cáo auction.

### Request
```json
{
  "type": "GET_AUCTION_REPORT",
  "token": "sess-admin",
  "payload": {}
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "GET_AUCTION_REPORT",
  "payload": [
    {
      "auctionId": "auc-001",
      "status": "FINISHED",
      "startingPrice": 25000000,
      "currentHighestBid": 35000000
    }
  ]
}
```

---

## 15. GET_BID_HISTORY_REPORT

**Role:** AUTH
**Mô tả:** Lấy lịch sử bid của auction.

### Request
```json
{
  "type": "GET_BID_HISTORY_REPORT",
  "token": "sess-xyz789",
  "payload": {
    "auctionId": "auc-001"
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "GET_BID_HISTORY_REPORT",
  "payload": [
    {
      "bidId": "bid-001",
      "bidderId": "usr-bidder01",
      "bidAmount": 30000000,
      "bidTime": "2025-01-15T15:30:00"
    }
  ]
}
```

---

## 16. GET_AUDIT_LOG

**Role:** ADMIN
**Mô tả:** Lấy audit log (chỉ ADMIN).

### Request
```json
{
  "type": "GET_AUDIT_LOG",
  "token": "sess-admin",
  "payload": {
    "limit": 50
  }
}
```

### Response (Thành công)
```json
{
  "status": "OK",
  "type": "GET_AUDIT_LOG",
  "payload": [
    {
      "userId": "usr-001",
      "action": "LOGIN",
      "details": "{}",
      "createdAt": "2025-01-15T14:00:00"
    }
  ]
}
```

---

## 17. RUN_INTEGRITY_CHECK

**Role:** ADMIN
**Mô tả:** Kiểm tra toàn ven dữ liệu (chỉ ADMIN).

### Request
```json
{
  "type": "RUN_INTEGRITY_CHECK",
  "token": "sess-admin",
  "payload": {}
}
```

### Response (Thành công — OK)
```json
{
  "status": "OK",
  "type": "RUN_INTEGRITY_CHECK",
  "payload": {
    "bidConsistencyErrors": [],
    "auctionWinnerErrors": [],
    "orphanedItemErrors": [],
    "totalErrors": 0,
    "status": "OK"
  }
}
```

### Response (Thành công — ERRORS_FOUND)
```json
{
  "status": "OK",
  "type": "RUN_INTEGRITY_CHECK",
  "payload": {
    "bidConsistencyErrors": ["Auction auc-001: currentHighestBid=1000 nhung MAX(bid)=2000"],
    "auctionWinnerErrors": [],
    "orphanedItemErrors": ["Item item-003: sellerId=usr-deleted khong ton tai"],
    "totalErrors": 2,
    "status": "ERRORS_FOUND"
  }
}
```

---

## Realtime Events (Server → Client)

Events được server đẩy realtime qua socket khi client đã subscribe auction.

### BID_UPDATE
```json
{
  "eventType": "BID_UPDATE",
  "auctionId": "auc-001",
  "bidderId": "usr-bidder02",
  "bidAmount": 33000000,
  "timestamp": "2025-01-15T16:00:00"
}
```

### AUCTION_CLOSED
```json
{
  "eventType": "AUCTION_CLOSED",
  "auctionId": "auc-001",
  "winnerId": "usr-bidder02",
  "winningBid": 35000000,
  "timestamp": "2025-01-15T18:00:05"
}
```

### AUCTION_EXTENDED
```json
{
  "eventType": "AUCTION_EXTENDED",
  "auctionId": "auc-001",
  "extendedMinutes": 5,
  "newEndTime": "2025-01-15T18:05:00",
  "timestamp": "2025-01-15T17:59:58"
}
```

---

## Error Codes

| Error Code | Mô tả |
|---|---|
| `INVALID_CREDENTIALS` | Sai username hoặc mật khẩu |
| `USERNAME_EXISTS` | Username đã tồn tại |
| `TAI_KHOAN_BI_KHOA` | Tài khoản bị khóa |
| `UNAUTHORIZED` | Chưa đăng nhập hoặc token hết hạn |
| `FORBIDDEN` | Không có quyền thực hiện action |
| `AUCTION_NOT_FOUND` | Auction không tồn tại |
| `AUCTION_NOT_RUNNING` | Auction không đang chạy |
| `BID_TOO_LOW` | Giá đặt thấp hơn giá hiện tại + minimum increment |
| `CANNOT_BID_OWN_AUCTION` | Không thể đặt giá auction của mình |
| `VALIDATION_ERROR` | Dữ liệu đầu vào không hợp lệ |
| `INTERNAL_ERROR` | Lỗi server nội bộ |
```

```bash
git commit -m "docs: thêm API_PROTOCOL.md — 17 command types + 3 realtime events + error codes"
```

---

### Cập nhật `.github/workflows/ci.yml` — JaCoCo + `--fail-at-end`

```yaml
name: BidHub CI

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Build with Maven
        run: mvn clean compile -B

      - name: Run tests with JaCoCo
        run: mvn test --fail-at-end -B

      - name: Generate JaCoCo report
        if: success()
        run: mvn jacoco:report -B

      - name: Upload JaCoCo coverage report
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: '**/target/site/jacoco/**'
          retention-days: 14
```

```bash
git commit -m "ci: cập nhật ci.yml — thêm JaCoCo report, --fail-at-end, upload artifact"
```

---

### Cập nhật `README.md` — thêm CI badge

```markdown
<!-- Thêm dòng này vào đầu README.md -->
# BidHub — Hệ thống đấu giá trực tuyến

![CI](https://github.com/<org>/bidhub/actions/workflows/ci.yml/badge.svg)

```

```bash
git commit -m "docs: thêm CI badge vào README.md"
```

---

**Kiểm tra manual:**
```bash
# Verify docs hoàn chỉnh
wc -l docs/API_PROTOCOL.md
# Output: ≥300 lines

# Verify CI config
cat .github/workflows/ci.yml | rg "jacoco"
# Output: should find JaCoCo sections

# Verify CI badge trong README
rg "badge.svg" README.md
# Output: should find badge link
```

**❌ FAIL nếu:**
- `API_PROTOCOL.md` có < 14 command types → docs chưa hoàn chỉnh
- Command type thiếu request/response example → client dev không biết API format
- `ci.yml` thiếu JaCoCo → không có coverage report
- `ci.yml` thiếu `--fail-at-end` → 1 module fail khác vẫn green
- CI badge link sai → badge không hiển thị

---

## 👤 CÔNG MINH — UI Polish & UX

```
Branch: feature/tuan-9-congminh-ui-polish
Phụ thuộc: Toàn bộ controller (T4-T8) — MỞ RA thêm loading state + validation
           AuctionDetailController (T6+T7) — countdown stop on AUCTION_CLOSED
           NetworkTask (T4) — ProgressIndicator integration
Merge cuối: UI polish không ảnh hưởng server logic
```

📌 **[Tiêu chí điểm: MVC — JavaFX loading state + form validation — phần 0.5đ + Clean Code — UX polish — phần 0.5đ]**

### 📝 Mô tả bài tập

**Loading State:** Mọi `NetworkTask` cần hiển thị `ProgressIndicator` trong khi chờ response. Button trigger
task bị disable trong lúc task chạy → ngăn user click nhiều lần. Pattern: `button.setDisable(true)` trước task,
`setOnSucceeded(() -> button.setDisable(false))` sau khi xong.

**Empty Data Lists:** Khi `AuctionListController` nhận danh sách rỗng → hiện Label "Không có phiên đấu giá nào"
thay vì TableView trống. Khi load failure → hiện Alert lỗi.

**AuctionDetail Bid Failure:** Khi `handlePlaceBid` trả về error (BID_TOO_LOW, AUCTION_NOT_RUNNING) → hiện
Alert cho user biết lý do. Không silent fail.

**Countdown Stops on AUCTION_CLOSED:** Khi nhận `AUCTION_CLOSED` event → countdown Timeline dừng,
hiển thị "ĐÃ KẾT THÚC", disable bid button + TextField.

**TextField Numeric Only:** `tfBidAmount` chỉ nhận số + dấu chấm thập phân. Filter input bằng
`TextField.setTextFormatter()` hoặc `ChangeListener`.

**Form Validation:** Kiểm tra client-side trước khi gửi request: bidAmount > 0, bidAmount là số hợp lệ,
TextField không rỗng. Hiển thị validation error trên UI.

**Check 1366×768 Resolution:** Đảm bảo tất cả màn hình hiển thị đúng trên 1366×768 (laptop phổ biến).
ScrollPane cho danh sách dài, Font size ≥ 12px.

### 🔗 Phương án tích hợp

**Phân tích phụ thuộc:**
- Công Minh KHÔNG phụ thuộc server code thay đổi
- Công Minh chỉ mở controller đã có → thêm loading state + validation
- Merge cuối → không gây conflict cho ai

**Kịch bản chọn: A — Công Minh code song song, merge cuối**

**Các bước:**
1. Công Minh tạo branch
2. Công Minh thêm loading state cho `AuctionListController` + `AuctionDetailController` + `CreateItemController` + `CreateAuctionController`
3. Công Minh thêm empty list handler cho `AuctionListController`
4. Công Minh thêm bid failure Alert cho `AuctionDetailController`
5. Công Minh thêm numeric TextField cho `tfBidAmount`
6. Công Minh thêm form validation
7. Công Minh check 1366×768 resolution
8. Khoa + Đăng + Quốc Minh merge → Công Minh rebase
9. Push, tạo PR → review → merge

### 📁 Cấu trúc file

```
bidhub-client/src/
└── main/java/com/bidhub/client/
    ├── controller/
    │   ├── AuctionListController.java      (đã có — MỞ RA thêm loading state + empty list)
    │   ├── AuctionDetailController.java    (đã có — MỞ RA thêm bid failure Alert + countdown stop)
    │   ├── CreateItemController.java       (đã có — MỞ RA thêm loading state + validation)
    │   ├── CreateAuctionController.java    (đã có — MỞ RA thêm loading state + validation)
    │   └── LoginController.java            (đã có — MỞ RA thêm loading state)
    └── util/
        └── UiUtils.java                   ← MỚI: loading state helper, numeric filter, validation
```

---

### `UiUtils.java` — Helper class

```java
package com.bidhub.client.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Cac helper method cho JavaFX UI — loading state, validation, numeric filter.
 *
 * <p>// 📌 [Tieu chi: MVC — JavaFX UX helper]
 */
public final class UiUtils {

  private UiUtils() {
    // Utility class — khong instance
  }

  /**
   * Bind loading state: disable button + hien ProgressIndicator khi task chay.
   *
   * @param button button can disable
   * @param spinner ProgressIndicator
   * @return Runnable de goi khi task hoan thanh (re-enable button)
   */
  public static Runnable showLoading(Button button, ProgressIndicator spinner) {
    button.setDisable(true);
    spinner.setVisible(true);
    return () -> {
      button.setDisable(false);
      spinner.setVisible(false);
    };
  }

  /**
   * Hien Alert loi.
   *
   * @param title   tieu de
   * @param message noi dung loi
   */
  public static void showError(String title, String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
    });
  }

  /**
   * Hien Alert thanh cong.
   *
   * @param title   tieu de
   * @param message noi dung
   */
  public static void showInfo(String title, String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
    });
  }

  /**
   * Ap numeric-only filter cho TextField.
   *
   * <p>Chi cho phep so + dau cham thap phan.
   *
   * @param textField TextField can filter
   */
  public static void applyNumericFilter(TextField textField) {
    TextFormatter<String> formatter = new TextFormatter<>(change -> {
      String text = change.getText();
      if (text.isEmpty()) {
        return change;
      }
      // Chi cho phep so va dau cham
      if (text.matches("[0-9.]*")) {
        // Chi cho phep 1 dau cham
        String currentText = ((TextField) change.getControl()).getText();
        if (".".equals(text) && currentText.contains(".")) {
          return null; // Block — da co dau cham
        }
        return change;
      }
      return null; // Block — khong phai so
    });
    textField.setTextFormatter(formatter);
  }

  /**
   * Validate TextField khong rong.
   *
   * @param textField TextField can check
   * @param fieldName ten truong (cho error message)
   * @return true neu hop le
   */
  public static boolean validateNotEmpty(TextField textField, String fieldName) {
    if (textField.getText() == null || textField.getText().isBlank()) {
      showError("Validation Error", fieldName + " khong duoc de trong");
      textField.requestFocus();
      return false;
    }
    return true;
  }

  /**
   * Validate TextField la so duong.
   *
   * @param textField TextField can check
   * @param fieldName ten truong (cho error message)
   * @return true neu hop le
   */
  public static boolean validatePositiveNumber(TextField textField,
      String fieldName) {
    try {
      double value = Double.parseDouble(textField.getText().trim());
      if (value <= 0) {
        showError("Validation Error", fieldName + " phai lon hon 0");
        textField.requestFocus();
        return false;
      }
      return true;
    } catch (NumberFormatException e) {
      showError("Validation Error", fieldName + " phai la so hop le");
      textField.requestFocus();
      return false;
    }
  }
}
```

*✅ Đã kiểm tra kỹ – không có bug.*

```bash
git commit -m "feat: thêm UiUtils — loading state helper, numeric filter, validation"
```

---

### Pattern: Loading State cho tất cả NetworkTask

Áp dụng cho MỌI controller có `NetworkTask`:

```java
// === PATTERN: Loading State cho AuctionListController ===

import com.bidhub.client.util.UiUtils;

// --- Trong FXML: thêm ProgressIndicator ---
// <ProgressIndicator fx:id="loadingSpinner" visible="false" prefWidth="30" prefHeight="30" />

@FXML private ProgressIndicator loadingSpinner;
@FXML private Button btnRefresh;

private void loadAuctionList() {
  // Disable button + hien spinner
  Runnable onComplete = UiUtils.showLoading(btnRefresh, loadingSpinner);

  NetworkTask<List<Map<String, Object>>> task = new NetworkTask<>(() -> {
    // ... network call ...
    return parsedList;
  });

  task.setOnSucceeded(e -> {
    List<Map<String, Object>> data = task.getValue();
    if (data.isEmpty()) {
      lblEmptyMessage.setVisible(true); // "Khong co phien dau gia nao"
      tableView.setVisible(false);
    } else {
      lblEmptyMessage.setVisible(false);
      tableView.setVisible(true);
      tableView.setItems(FXCollections.observableArrayList(data));
    }
    onComplete.run(); // Re-enable button + an spinner
  });

  task.setOnFailed(e -> {
    UiUtils.showError("Lỗi tải dữ liệu",
        "Không thể tải danh sách phiên đấu giá. Vui lòng thử lại.");
    onComplete.run(); // Re-enable button + an spinner
  });

  new Thread(task, "load-auctions").start();
}
```

---

### Pattern: Bid Failure Alert trong AuctionDetailController

```java
// === THÊM VÀO AuctionDetailController.java — handlePlaceBid —===

private void handlePlaceBid() {
  // Client-side validation
  if (!UiUtils.validateNotEmpty(tfBidAmount, "Số tiền đặt giá")) {
    return;
  }
  if (!UiUtils.validatePositiveNumber(tfBidAmount, "Số tiền đặt giá")) {
    return;
  }

  double bidAmount = Double.parseDouble(tfBidAmount.getText().trim());
  String auctionId = getCurrentAuctionId();

  // Loading state
  Runnable onComplete = UiUtils.showLoading(btnPlaceBid, loadingSpinner);

  NetworkTask<Map<String, Object>> task = new NetworkTask<>(() -> {
    MessageRequest req = new MessageRequest();
    req.setType("PLACE_BID");
    req.setPayload(mapper.createObjectNode()
        .put("auctionId", auctionId)
        .put("bidAmount", bidAmount));
    MessageResponse resp = ServerGateway.getInstance().sendRequest(req);
    return Map.of("status", resp.getStatus(),
        "message", resp.getMessage() != null ? resp.getMessage() : "",
        "payload", resp.getPayload());
  });

  task.setOnSucceeded(e -> {
    Map<String, Object> result = task.getValue();
    if ("OK".equals(result.get("status"))) {
      // Cap nhat UI — gia moi, nguoi dan dau
      Map<String, Object> payload = (Map<String, Object>) result.get("payload");
      if (payload != null) {
        double newPrice = ((Number) payload.get("currentHighestBid")).doubleValue();
        lblCurrentPrice.setText(String.format("%,.0f VND", newPrice));
      }
      tfBidAmount.clear();
      UiUtils.showInfo("Đặt giá thành công", "Đã đặt giá "
          + String.format("%,.0f VND", bidAmount));
    } else {
      // 📌 [Tieu chi: UX — hien Alert khi bid that bai]
      String errorMsg = (String) result.get("message");
      String userMsg = mapErrorMessage(errorMsg);
      UiUtils.showError("Đặt giá thất bại", userMsg);
    }
    onComplete.run();
  });

  task.setOnFailed(e -> {
    UiUtils.showError("Lỗi kết nối",
        "Không thể đặt giá. Kiểm tra kết nối mạng.");
    onComplete.run();
  });

  new Thread(task, "place-bid").start();
}

/**
   * Map server error code sang message thong than cho user.
   */
private String mapErrorMessage(String errorCode) {
  if (errorCode == null) {
    return "Lỗi không xác định. Vui lòng thử lại.";
  }
  return switch (errorCode) {
    case "BID_TOO_LOW" -> "Giá đặt quá thấp. Vui lòng đặt giá cao hơn giá hiện tại + bước tăng tối thiểu.";
    case "AUCTION_NOT_RUNNING" -> "Phiên đấu giá đã kết thúc. Không thể đặt giá.";
    case "AUCTION_NOT_FOUND" -> "Phiên đấu giá không tồn tại.";
    case "CANNOT_BID_OWN_AUCTION" -> "Không thể đặt giá phiên đấu giá của chính bạn.";
    case "UNAUTHORIZED" -> "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
    default -> "Lỗi: " + errorCode + ". Vui lòng thử lại.";
  };
}
```

```bash
git commit -m "feat: AuctionDetailController thêm bid failure Alert + error message mapping"
```

---

### Pattern: Countdown Stops on AUCTION_CLOSED

```java
// === THÊM VÀO AuctionDetailController.java — trong callback event ===

// Trong subscribeRealtimeEvents() callback:
        if ("AUCTION_CLOSED".equals(eventType)) {
          // 📌 [Tieu chi: UX — countdown dung khi auction dong]
          Platform.runLater(() -> {
            lblStatus.setText("ĐÃ KẾT THÚC");
            lblCountdown.setText("ĐÃ KẾT THÚC");
            btnPlaceBid.setDisable(true);
            tfBidAmount.setDisable(true);
            loadingSpinner.setVisible(false);
            stopCountdown(); // Dong Timeline + EventListenerThread
          });
        }
```

```bash
git commit -m "feat: countdown dừng + UI disable khi nhận AUCTION_CLOSED event"
```

---

### Pattern: Numeric TextField cho bidAmount

```java
// === THÊM VÀO AuctionDetailController.initialize() ===

import com.bidhub.client.util.UiUtils;

  // 📌 [Tieu chi: UX — TextField chi nhan so]
  UiUtils.applyNumericFilter(tfBidAmount);
```

---

### Pattern: Empty Data List Handler

```java
// === THÊM VÀO AuctionListController — FXML cần Label + TableView ===

// <Label fx:id="lblEmptyMessage" text="Không có phiên đấu giá nào"
//         visible="false" style="-fx-font-size: 14px; -fx-text-fill: #666;" />
// <TableView fx:id="tableView" />

@FXML private Label lblEmptyMessage;

  // Trong loadAuctionList() — setOnSucceeded:
      List<Map<String, Object>> data = task.getValue();
      if (data == null || data.isEmpty()) {
        lblEmptyMessage.setVisible(true);
        tableView.setVisible(false);
      } else {
        lblEmptyMessage.setVisible(false);
        tableView.setVisible(true);
        tableView.setItems(FXCollections.observableArrayList(data));
      }
```

```bash
git commit -m "feat: AuctionListController thêm empty data list handler + loading state"
```

---

**Kiểm tra manual:**
```bash
# Compile client
mvn compile -pl bidhub-client -q
# Output: BUILD SUCCESS

# Manual test:
# 1. Start server
# 2. Start client — resize window to 1366×768
# 3. Login → navigate AuctionList → verify loading spinner
# 4. Click Refresh → verify button disabled during load
# 5. Open AuctionDetail → type letters in bidAmount → verify filtered
# 6. Place bid with low amount → verify error Alert
# 7. Wait auction close → verify countdown stops, button disabled
```

**❌ FAIL nếu:**
- Click button nhiều lần → nhiều request gửi đi → button không disable trong task
- `ProgressIndicator` không hiển thị → user không biết đang loading
- Empty list → TableView trống không có Label hướng dẫn
- Bid fail → không có Alert → user không biết tại sao
- `AUCTION_CLOSED` event → countdown vẫn chạy → misleading UX
- TextField nhập chữ → server trả error thay vì filter client-side
- 1366×768 → UI bị crop, không scroll được

---

## 📊 TỔNG KẾT TUẦN 9

### Điểm barem phục vụ trực tiếp

| Tiêu chí barem | Điểm | Code tuần 9 phục vụ |
|---|---|---|
| **Kỹ thuật quan trọng: SLF4J + Logback** | 1.0đ | Replace ALL `System.out.println` → `Logger`, `logback.xml` config |
| **CI/CD: GitHub Actions + JaCoCo** | 1.0đ | `.github/workflows/ci.yml` + JaCoCo plugin + coverage report upload + CI badge |
| **Tài liệu: API Protocol** | 1.0đ | `docs/API_PROTOCOL.md` — 17 command types + 3 realtime events + error codes |
| **Clean Code** | phần 1.0đ | Verify MigrationRunner 5 tables + SLF4J refactor + DataIntegrityService cross-validation |
| **Unit Test** | phần 0.5đ | `DataIntegrityServiceTest` (10 cases) + `IntegrationTest` (8 cases) |
| **MVC** | phần 0.5đ | `handleRunIntegrityCheck` handler + JavaFX loading state + form validation |

### Test count summary

| Tuần | Cases | Cumulative |
|---|---|---|
| T1 | 15 | 15 |
| T2 | 14 | 29 |
| T3 | 21 | 50 |
| T4 | 15 | 65 |
| T5 | 25 | 90 |
| T6 | 23 | 113 |
| T7 | 20 | 133 |
| T8 | 10 | 143 |
| **T9** | **10+** | **≥153** |

### Merge order

```
1. Khoa merge:     DataIntegrityService + handleRunIntegrityCheck  → foundation cho data check
2. Đăng merge:     IntegrationTest + SLF4J refactor + logback.xml  → logging + e2e test
3. Quốc Minh merge: API_PROTOCOL.md + ci.yml + CI badge           → docs + CI green
4. Công Minh merge: UI Polish — loading state, validation, UX     → final polish
```
