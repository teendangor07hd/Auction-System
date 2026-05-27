# BidHub — Hệ thống đấu giá trực tuyến

> Dự án môn **Lập trình Nâng cao** — Hệ thống đấu giá trực tuyến theo mô hình Client–Server sử dụng Java Socket, JavaFX và SQLite.

---

## Thành viên nhóm

| Ký hiệu | Họ tên | Vai trò chính |
|---------|--------|---------------|
| **Đăng** | Vũ Tiến Đăng | Server Core & Database |
| **Quốc Minh** | Nguyễn Quốc Minh | Networking & Protocol |
| **Công Minh** | Nguyễn Công Minh | Client GUI (JavaFX) |
| **Khoa** | Trần Đăng Khoa | Business Logic & Testing |

---

## Mô tả bài toán & Phạm vi hệ thống

**BidHub** là hệ thống đấu giá trực tuyến cho phép nhiều người dùng kết nối đồng thời qua mạng để tham gia mua bán hàng hóa theo hình thức đấu giá.

**Phạm vi hệ thống:**
- **Người dùng** có thể đăng ký, đăng nhập theo ba vai trò: **Bidder** (người đặt giá), **Seller** (người bán), **Admin** (quản trị viên).
- **Seller** đăng sản phẩm (Electronics, Art, Vehicle) và tạo phiên đấu giá.
- **Bidder** tham gia đặt giá realtime; phiên đấu giá cập nhật tức thì cho tất cả người dùng đang theo dõi.
- **Admin** quản lý người dùng, sản phẩm, phiên đấu giá và xem báo cáo thống kê.
- Server tự động kết thúc phiên đấu giá, áp dụng cơ chế **Anti-Sniping** (gia hạn thời gian khi có bid vào phút cuối).

---

## Công nghệ sử dụng

| Thành phần | Công nghệ / Thư viện |
|------------|----------------------|
| Ngôn ngữ | Java 21 |
| Giao diện | JavaFX 21.0.3 (FXML) |
| Giao tiếp mạng | Java Socket (TCP) |
| Serialization | Jackson 2.17.1 (JSON) |
| Cơ sở dữ liệu | SQLite 3.45.3.0 (via `sqlite-jdbc`) |
| Logging | SLF4J 2.0.9 + Logback 1.4.14 |
| Testing | JUnit 5.10.2 |
| Build | Apache Maven 3.x (multi-module) |
| CI/CD | GitHub Actions |

---

## Yêu cầu môi trường

| Yêu cầu | Phiên bản tối thiểu |
|---------|---------------------|
| JDK | 21 trở lên |
| Maven | 3.8 trở lên |
| Hệ điều hành | Windows / macOS / Linux |

> **Lưu ý:** JavaFX đã được đóng gói sẵn vào file `.jar` (fat-jar). Không cần cài JavaFX riêng.

---

## Cấu trúc thư mục

```
Auction-System/
├── bidhub-parent/              ← Root Maven project (pom.xml tổng)
│
├── bidhub-common/              ← Module dùng chung giữa Server và Client
│   └── src/main/java/com/bidhub/common/
│       ├── model/              ← Entity.java (abstract base class)
│       ├── network/            ← MessageRequest, MessageResponse, MessageMapper
│       └── exception/          ← BidHubException + 7 exception subclass
│
├── bidhub-server/              ← Module Server
│   └── src/main/java/com/bidhub/server/
│       ├── ServerApp.java      ← Entry point của Server
│       ├── config/             ← ConfigLoader, DbConnectionProvider, MigrationRunner
│       ├── model/              ← Domain models: User, Item, Auction, BidTransaction, AuditLog...
│       ├── dao/                ← UserDao, ItemDao, AuctionDao, BidDao, AuditLogDao
│       ├── service/            ← AuthService, AuctionManager, BidValidator, NotificationBroker...
│       ├── network/            ← SocketServerCore, ClientConnectionThread, RequestHandler, Handlers
│       ├── event/              ← Event system
│       └── utils/              ← Utility classes
│
├── bidhub-client/              ← Module Client (JavaFX)
│   └── src/main/java/com/bidhub/client/
│       ├── Launcher.java       ← Entry point của Client
│       ├── BidHubApp.java      ← Khởi động JavaFX Application
│       ├── controller/         ← 13 màn hình (Login, Register, Home, AuctionList, ...)
│       ├── network/            ← ServerGateway, EventListenerThread, ClientSession
│       ├── service/            ← BidChartService
│       ├── navigation/         ← Điều hướng màn hình
│       └── util/               ← Utility classes
│
├── docs/                       ← Tài liệu dự án
│   ├── API_PROTOCOL.md         ← Đặc tả giao thức JSON Client–Server
│   ├── STYLE_GUIDE.md          ← Quy tắc viết code
│   └── DEMO_SCRIPT.md          ← Kịch bản demo
│
├── bidhub.db                   ← File cơ sở dữ liệu SQLite (tự tạo khi chạy Server)
├── pom.xml                     ← Maven parent POM
└── CONTRIBUTING.md             ← Hướng dẫn đóng góp & quy trình Git
```

---

## Vị trí các file `.jar`

Sau khi build thành công, các file fat-jar (đã đóng gói đầy đủ dependencies) nằm tại:

| Module | Đường dẫn |
|--------|-----------|
| **Server** | `bidhub-server/target/bidhub-server-1.0-SNAPSHOT.jar` |
| **Client** | `bidhub-client/target/bidhub-client-1.0-SNAPSHOT.jar` |

> Các file `original-*.jar` là jar gốc chưa đóng gói dependencies — **không dùng để chạy trực tiếp**.

---

## Hướng dẫn Build & Chạy

### Bước 1 — Clone repository

```bash
git clone https://github.com/teendangor07hd/Auction-System.git
cd Auction-System
```

### Bước 2 — Build toàn bộ project

```bash
mvn clean package -DskipTests
```

> Lệnh này build cả 3 module theo thứ tự: `bidhub-common` → `bidhub-server` → `bidhub-client`.

---

### Bước 3 — Chạy Server *(phải chạy trước)*

```bash
java -jar bidhub-server/target/bidhub-server-1.0-SNAPSHOT.jar
```

Server sẽ khởi động và lắng nghe kết nối từ client. File `bidhub.db` sẽ được tự động tạo tại thư mục gốc nếu chưa có.

**Kết quả mong đợi:**
```
[INFO] BidHub Server starting...
[INFO] Database initialized successfully.
[INFO] Server listening on port 9090
```

---

### Bước 4 — Chạy Client *(sau khi Server đã chạy)*

Mở một terminal mới, chạy:

```bash
java -jar bidhub-client/target/bidhub-client-1.0-SNAPSHOT.jar
```

Có thể mở nhiều cửa sổ Client cùng lúc để mô phỏng nhiều người dùng đồng thời.

---

### Tóm tắt thứ tự chạy

```
1. mvn clean package -DskipTests     ← Build (chỉ cần làm 1 lần)
2. java -jar bidhub-server/...jar    ← Chạy Server TRƯỚC
3. java -jar bidhub-client/...jar    ← Chạy Client SAU (có thể mở nhiều cửa sổ)
```

---

## Danh sách chức năng đã hoàn thành

### 🔐 Xác thực & Tài khoản
- [x] Đăng ký tài khoản (Bidder / Seller)
- [x] Đăng nhập / Đăng xuất với session token (UUID)
- [x] Phân quyền theo vai trò: Bidder, Seller, Admin

### 🛒 Quản lý Sản phẩm
- [x] Thêm sản phẩm mới (Electronics, Art, Vehicle) — Factory Method Pattern
- [x] Xem danh sách & chi tiết sản phẩm
- [x] Seller xem dashboard sản phẩm của mình

### 🔨 Phiên đấu giá
- [x] Tạo phiên đấu giá cho sản phẩm
- [x] Xem danh sách phiên đấu giá (lọc theo trạng thái)
- [x] Xem chi tiết phiên đấu giá realtime
- [x] Đặt giá (PLACE_BID) với kiểm tra nghiệp vụ (BidValidator)
- [x] Cập nhật realtime khi có bid mới (Server push — BID_UPDATE)
- [x] Tự động kết thúc phiên theo thời gian (AuctionLifecycleTask)
- [x] Cơ chế **Anti-Sniping** — gia hạn thời gian khi có bid vào phút cuối
- [x] Vòng đời phiên: `OPEN → RUNNING → FINISHED → PAID / CANCELED`

### 📊 Thống kê & Báo cáo
- [x] Biểu đồ giá đặt theo thời gian (BidChartService)
- [x] Xem lịch sử đặt giá (GET_BID_HISTORY)
- [x] Báo cáo tổng hợp (ReportService)
- [x] Audit log toàn bộ hành động quan trọng

### 🛡️ Quản trị (Admin)
- [x] Quản lý người dùng (xem, vô hiệu hóa, đặt lại mật khẩu)
- [x] Quản lý sản phẩm & phiên đấu giá
- [x] Kiểm tra toàn vẹn dữ liệu (DataIntegrityService)
- [x] Xem audit log hệ thống

### 🔔 Thông báo
- [x] Nhận thông báo realtime (NotificationBroker)
- [x] Màn hình thông báo cho người dùng (NotificationController)

### ⚙️ Hạ tầng kỹ thuật
- [x] Multi-threaded server — mỗi client một thread riêng
- [x] Giao tiếp qua JSON (Jackson) trên Java Socket (TCP)
- [x] Cơ sở dữ liệu SQLite với migration tự động
- [x] Logging với SLF4J + Logback
- [x] CI/CD tự động với GitHub Actions
- [x] Cây exception riêng: `BidHubException` + 7 subclass
- [x] JUnit 5 — bộ test tự động (≥ 40 test cases)

---

## Tài liệu & Báo cáo

| Tài liệu | Liên kết |
|----------|----------|
| 📄 Báo cáo PDF | *(chèn link tại đây)* |
| 🎬 Video Demo | *(chèn link tại đây)* |
| 📋 API Protocol | [docs/API_PROTOCOL.md](docs/API_PROTOCOL.md) |
| 🎨 Style Guide | [docs/STYLE_GUIDE.md](docs/STYLE_GUIDE.md) |
| 🤝 Contributing Guide | [CONTRIBUTING.md](CONTRIBUTING.md) |
