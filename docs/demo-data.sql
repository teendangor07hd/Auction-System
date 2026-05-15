-- =============================================================================
-- BidHub Project — Database Seeding Script (Industry Standard)
-- Purpose: Initialize professional mock data for final demo.
-- Compatibility: Matches Java DAO implementation (Week 10).
-- =============================================================================

-- 0. CLEANUP: Xóa dữ liệu cũ để tránh lỗi Duplicate Primary Key khi chạy lại script.
DELETE FROM audit_logs;
DELETE FROM bid_transactions;
DELETE FROM auctions;
DELETE FROM items;
DELETE FROM users;

-- =============================================================================
-- 1. USERS (9 columns per UserDao.save)
-- Fields: id, username, password_hash, email, role, extra_int, is_locked, created_at, updated_at
-- =============================================================================

-- Cập nhật mật khẩu theo chuẩn SHA-256 để khớp với AuthService.java
-- DELETE FROM users; -- Bạn có thể chạy lệnh này trước nếu muốn làm sạch bảng
INSERT INTO users (id, username, password_hash, email, role, extra_int, is_locked, created_at, updated_at)
VALUES
-- Mật khẩu: Admin@123
('u-adm-01', 'admin01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', 'admin@bidhub.com', 'ADMIN', 1, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),

-- Mật khẩu: Seller@123
('u-sel-01', 'seller01', 'bd28c94800c2be055b3329f8dd63a3d5a4137c0def2517bf4fce85eb11e62853', 'seller@bidhub.com', 'SELLER', 5, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),

-- Mật khẩu: Bidder@123
('u-bid-01', 'bidder01', 'dd320d7626b4ea475e3efd2c227e76fb9ac5311c3d26c89757911f6294e3ce1e', 'bidder1@bidhub.com', 'BIDDER', 20, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),
('u-bid-02', 'bidder02', 'dd320d7626b4ea475e3efd2c227e76fb9ac5311c3d26c89757911f6294e3ce1e', 'bidder2@bidhub.com', 'BIDDER', 15, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00');
-- =============================================================================
-- 2. ITEMS (9 columns per ItemDao.save)
-- Fields: id, name, description, starting_price, item_type, seller_id, extra_data, created_at, updated_at
-- Note: extra_data uses JSON string format to match Jackson serialization logic.
-- =============================================================================

INSERT INTO items (id, name, description, starting_price, item_type, seller_id, extra_data, created_at, updated_at)
VALUES
    (
        'i-mcbook-01', 'MacBook Pro M3 Max', '14-inch, 36GB RAM, 1TB SSD. Space Black.',
        60000000.0, 'ELECTRONICS', 'u-sel-01', '{"brand":"Apple","warrantyMonths":12}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-iphone-01', 'iPhone 15 Pro Max', 'Natural Titanium, 256GB, Like New.',
        28000000.0, 'ELECTRONICS', 'u-sel-01', '{"brand":"Apple","warrantyMonths":6}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-art-01', 'Sơn mài "Hạ Long"', 'Tranh sơn mài truyền thống, nghệ nhân ký tên.',
        15000000.0, 'ART', 'u-sel-01', '{"artist":"Trần Văn B","yearCreated":2024}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    );

-- =============================================================================
-- 3. AUCTIONS (11 columns per AuctionDao.save)
-- Fields: id, item_id, start_time, end_time, starting_price, current_highest_bid,
--         highest_bidder_id, status, minimum_increment, created_at, updated_at
-- =============================================================================

INSERT INTO auctions (id, item_id, start_time, end_time, starting_price, current_highest_bid, highest_bidder_id, status, minimum_increment, created_at, updated_at)
VALUES
    (
        'auc-001', 'i-mcbook-01', '2026-05-15T00:00:00', '2026-05-15T02:00:00',
        60000000.0, 61500000.0, 'u-bid-02', 'RUNNING', 500000.0,
        '2026-05-15T00:00:00', '2026-05-15T00:15:00'
    );

-- =============================================================================
-- 4. BID TRANSACTIONS (5 columns per BidDao.save)
-- Fields: id, auction_id, bidder_id, bid_amount, bid_time
-- =============================================================================

INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, bid_time)
VALUES
    ('bid-tx-01', 'auc-001', 'u-bid-01', 60500000.0, '2026-05-15T00:10:00'),
    ('bid-tx-02', 'auc-001', 'u-bid-02', 61500000.0, '2026-05-15T00:15:00');

-- =============================================================================
-- 5. AUDIT LOGS (5 columns per AuditLogDao.save)
-- Fields: id, user_id, action, details, created_at
-- =============================================================================

INSERT INTO audit_logs (id, user_id, action, details, created_at)
VALUES
    ('log-01', 'u-adm-01', 'LOGIN', 'Admin login successful from 127.0.0.1', '2026-05-15T00:00:01'),
    ('log-02', 'u-bid-02', 'PLACE_BID', 'Placed bid 61,500,000 on auction auc-001', '2026-05-15T00:15:00');

-- =============================================================================
-- End of Script
-- =============================================================================