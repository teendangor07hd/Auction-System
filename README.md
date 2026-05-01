# BidHub — Hệ thống đấu giá trực tuyến

---

## Thành viên nhóm

| Ký hiệu | Họ tên | Vai trò chính |
|---------|--------|---------------|
| **Đăng** | Vũ Tiến Đăng | Server Core & Database |
| **Quốc Minh** | Nguyễn Quốc Minh | Networking & Protocol |
| **Công Minh** | Nguyễn Công Minh | Client GUI (JavaFX) |
| **Khoa** | Trần Đăng Khoa | Business Logic & Testing |

---

## Bảng phân chia công việc — Tuần 1 & Tuần 2

### 🗓️ Tuần 1 · Thiết lập môi trường & Infrastructure

| Thành viên | Công việc | Tác dụng với dự án |
|------------|-----------|-------------------|
| **Vũ Tiến Đăng** | Khởi tạo Maven multi-module project (`bidhub-parent` → `bidhub-server` + `bidhub-client`) | Tạo khung xương chung cho toàn bộ project, đảm bảo cả nhóm clone về là build được ngay |
| **Vũ Tiến Đăng** | Tạo file `ConfigLoader` | Đọc cấu hình hệ thống (port, đường dẫn DB…) từ file `.properties`, tránh hardcode trong code |
| **Vũ Tiến Đăng** | Thiết lập `.gitignore` | Loại bỏ các file rác (`target/`, `.idea/`, `*.db`) khỏi Git, giữ repo sạch |
| **Nguyễn Quốc Minh** | Thiết lập GitHub Actions CI/CD (`ci.yml`) | Tự động chạy toàn bộ test mỗi khi có người push code, phát hiện lỗi sớm |
| **Nguyễn Quốc Minh** | Viết `CONTRIBUTING.md` | Quy định cách tạo branch, viết commit, tạo PR — giúp cả nhóm làm việc thống nhất |
| **Nguyễn Quốc Minh** | Tạo PR template (`.github/pull_request_template.md`) | Mỗi Pull Request tự động hiện checklist, nhắc reviewer không bỏ sót bước kiểm tra |
| **Nguyễn Quốc Minh** | Viết `docs/API_PROTOCOL.md` | Định nghĩa sẵn format JSON giao tiếp giữa Client và Server, để các tuần sau code không bị lệch chuẩn |
| **Nguyễn Quốc Minh** | Viết `README.md` | Hướng dẫn người mới clone về và chạy được project ngay, không cần hỏi lại |
| **Nguyễn Công Minh** | Tạo JavaFX skeleton — cửa sổ ứng dụng BidHub | Khung giao diện desktop khởi động được, là nền để gắn tất cả màn hình về sau |
| **Nguyễn Công Minh** | Tạo màn hình `LoginView` (file FXML + Controller) | Màn hình đăng nhập đầu tiên người dùng thấy khi mở app |
| **Nguyễn Công Minh** | Tạo file `Views.java` (constants tên màn hình) | Quản lý tập trung tên tất cả màn hình, tránh lỗi typo khi điều hướng |
| **Trần Đăng Khoa** | Kích hoạt JUnit 5 và cấu hình `maven-surefire-plugin` | Cho phép chạy lệnh `mvn test` để kiểm tra toàn bộ code tự động |
| **Trần Đăng Khoa** | Viết bộ test `CalculatorTest` (≥ 15 test cases) | Xác nhận JUnit đã chạy được, đồng thời làm mẫu cách viết test cho cả nhóm |
| **Trần Đăng Khoa** | Viết `docs/STYLE_GUIDE.md` | Quy tắc đặt tên, thụt lề, độ dài dòng — giúp toàn bộ code nhóm nhìn đồng nhất |

---

### 🗓️ Tuần 2 · OOP Domain Model & Exception Hierarchy

| Thành viên | Công việc | Tác dụng với dự án |
|------------|-----------|-------------------|
| **Vũ Tiến Đăng** | Thêm module `bidhub-common` vào project | Tạo nơi chứa các class dùng chung giữa server và client, tránh duplicate code |
| **Vũ Tiến Đăng** | Tạo `Entity` — abstract base class | Lớp gốc của toàn bộ domain model, cung cấp `id` (UUID), `createdAt`, `updatedAt` cho mọi đối tượng |
| **Vũ Tiến Đăng** | Tạo cây kế thừa `User`: `User` → `Bidder`, `Seller`, `Admin` | Mô hình hóa 3 loại người dùng trong hệ thống với quyền hạn và thông tin khác nhau |
| **Vũ Tiến Đăng** | Tạo enum `UserRole` (BIDDER, SELLER, ADMIN) | Định nghĩa rõ vai trò người dùng, dùng để phân quyền trong hệ thống |
| **Nguyễn Quốc Minh** | Tạo cây kế thừa `Item`: `Item` → `Electronics`, `Art`, `Vehicle` | Mô hình hóa 3 loại sản phẩm đấu giá, mỗi loại có thuộc tính riêng |
| **Nguyễn Quốc Minh** | Tạo enum `ItemType` và interface `Displayable` | `ItemType` phân loại sản phẩm; `Displayable` ép mọi loại sản phẩm phải có cách hiển thị thông tin |
| **Nguyễn Quốc Minh** | Tạo Factory Method Pattern: `ItemCreator` → `ElectronicsCreator`, `ArtCreator`, `VehicleCreator` | Chuẩn hóa cách tạo sản phẩm theo từng loại; muốn thêm loại mới chỉ cần tạo thêm file, không sửa code cũ |
| **Nguyễn Quốc Minh** | Viết bộ test `ItemCreatorTest` (≥ 14 test cases) | Đảm bảo Factory tạo đúng loại sản phẩm và bắt được lỗi đầu vào sai |
| **Nguyễn Công Minh** | Tạo `Auction` và enum `AuctionStatus` với state machine | Quản lý vòng đời phiên đấu giá qua 5 trạng thái: `OPEN → RUNNING → FINISHED → PAID / CANCELED` |
| **Nguyễn Công Minh** | Tạo `BidTransaction` | Lưu lại lịch sử mỗi lần đặt giá (ai đặt, bao nhiêu tiền, lúc mấy giờ) |
| **Trần Đăng Khoa** | Tạo `BidHubException` và 7 exception subclass | Cây exception riêng cho dự án, giúp server trả về mã lỗi rõ ràng thay vì crash |
| **Trần Đăng Khoa** | Tạo `ValidationException` với danh sách lỗi | Gom nhiều lỗi validate lại một lần và trả về client cùng lúc, thay vì báo từng lỗi một |
| **Trần Đăng Khoa** | Viết bộ test exception hierarchy + integration test domain | Đảm bảo cây exception hoạt động đúng; xác nhận các domain class phối hợp được với nhau; cộng dồn ≥ 40 test cases toàn project |
