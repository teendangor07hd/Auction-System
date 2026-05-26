# 🎬 BidHub — Kịch bản Demo 10 phút

## Tổng quan
- **Thời gian:** 10 phút
- **Người demo:** cả nhóm (mỗi người 1-2 bước)
- **Mục tiêu:** trình diễn đầy đủ chức năng chính, smooth flow, không crash

## Chuẩn bị trước demo

1. Chạy `demo-data.sql` để insert demo data
2. Start server: `java -jar bidhub-server/target/bidhub-server-1.0.0.jar`
3. Start 2 client instances (2 terminal)
4. Verify health check: `echo '{"type":"HEALTH_CHECK"}' | nc localhost 8080`

---

## Bước 1: Đăng ký & Đăng nhập (1.5 phút)

**Người demo:** Công Minh

1. Mở Client 1 — màn hình Login
2. Click "Đăng ký" — nhập thông tin:
   - Username: `demo_bidder`
   - Password: `Demo@123`
   - Email: `bidder@bidhub.com`
3. Click "Đăng ký" → "Đăng ký thành công!" → quay lại Login
4. Đăng nhập với `demo_bidder` / `Demo@123` → chuyển sang AuctionList
5. **Chú thích:** Đăng ký dùng `handleRegister` → hash password → lưu DB → redirect Login

## Bước 2: Tạo Item & Tạo Auction (2 phút)

**Người demo:** Công Minh

1. Đăng nhập bằng account SELLER: `seller01` / `Seller@123` (từ demo-data.sql)
2. Click "Tạo sản phẩm" → nhập:
   - Name: "MacBook Pro M3"
   - Type: Electronics
   - Brand: Apple
   - Warranty: 12 tháng
   - Base Price: 30,000,000 VND
3. Click "Tạo" → "Tạo sản phẩm thành công!"
4. Click "Tạo phiên đấu giá" → chọn item vừa tạo:
   - Starting Price: 30,000,000 VND
   - Minimum Increment: 500,000 VND
   - Duration: 10 phút
5. Click "Tạo phiên" → "Tạo phiên đấu giá thành công!" → Auction xuất hiện trong danh sách
6. **Chú thích:** Factory Method Pattern — `ElectronicsCreator.createItem()`

## Bước 3: Đặt giá đấu (2 người cùng lúc) (2 phút)

**Người demo:** Quốc Minh

1. Client 1: đăng nhập `bidder01` / `Bidder@123`
2. Client 2: đăng nhập `bidder02` / `Bidder@123`
3. Cả 2 mở auction "MacBook Pro M3"
4. Client 1 đặt giá 31,000,000 → "Đặt giá thành công!"
5. Client 2 đặt giá 31,500,000 → "Đặt giá thành công!"
6. **QUAN TRỌNG:** Client 1 nhận realtime notification "Giá mới: 31,500,000"
7. **Chú thích:** Observer Pattern — `NotificationBroker.publish()` → `Session.sendMessage()`
   + ReentrantLock — lock auction trước validate+save, chống lost update

## Bước 4: Anti-Sniping (1.5 phút)

**Người demo:** Quốc Minh

1. Đợi auction còn < 1 phút (hoặc chỉnh endTime trong DB cho gần)
2. Client 1 đặt giá ở giây cuối → "Đã gia hạn thêm 60 giây!"
3. Client 2 nhận notification endTime mới
4. Countdown timer trên cả 2 client tự cập nhật
5. **Chú thích:** Anti-Sniping logic trong `handlePlaceBid` — check endTime - now < 60s → extend

## Bước 5: Auction kết thúc tự động (1 phút)

**Người demo:** Đăng

1. Đợi auction hết giờ (hoặc chạy nhanh bằng cách giảm endTime trong DB)
2. Client nhận `AUCTION_CLOSED` event → countdown → "ĐÃ KẾT THÚC"
3. Hiển thị người thắng: `bidder02` với giá `31,500,000 VND`
4. **Chú thích:** `AuctionLifecycleTask` — ScheduledExecutorService chạy mỗi 5s, kiểm tra endTime
   → closeAuction() → transitionTo(FINISHED) → xác định winner

## Bước 6: Admin Panel (1.5 phút)

**Người demo:** Khoa

1. Đăng nhập bằng account ADMIN: `admin01` / `Admin@123`
2. Thấy nút "Admin Panel" (ADMIN only — không hiện cho BIDDER/SELLER)
3. Click "Admin Panel" → TableView hiển thị danh sách users
4. Chọn user `bidder01` → Click "Khóa tài khoản" → Alert confirm → "Đã khóa!"
5. Đăng xuất, đăng nhập `bidder01` → "TÀI KHOẢN BỊ KHÓA"
6. Quay lại Admin → Mở khóa `bidder01` → đăng nhập lại thành công
7. **Chú thích:** `AdminUserService.lockUser()` → `UserDao.updateLocked()` → audit log

---

## Backup Plan

- Nếu client crash → restart client, đăng nhập lại
- Nếu server crash → restart server, `AuctionManager.start()` load RUNNING auctions → tiếp tục
- Nếu realtime notification không nhận → explain: EventListenerThread cần reconnect
- Nếu DB lỗi → xóa `bidhub.db`, chạy `demo-data.sql` lại

## Lời kết demo

"Đây là BidHub — hệ thống đấu giá trực tuyến xây dựng bằng Java 21, JavaFX, Socket programming.
Sử dụng 3 Design Patterns chính: Singleton, Factory Method, Observer.
Hỗ trợ concurrency với ReentrantLock và ScheduledExecutorService.
Cảm ơn thầy/cô đã xem demo!"