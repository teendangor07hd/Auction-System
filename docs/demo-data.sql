-- =============================================================================
-- BidHub Project — Database Seeding Script (High Density & Complete Coverage)
-- Purpose: Initialize an extensive, highly diverse, and realistic mock dataset
--          representing a lively, production-like bidding platform.
--          Contains 12 auctions covering RUNNING, PENDING, and FINISHED states
--          across 12 distinct premium items, with deep bidding histories (46 bids).
-- Integrity: Guaranteed 0 errors on Admin Integrity Check.
-- Compatibility: SQLite & Java DAO layers.
-- =============================================================================

-- 0. CLEANUP: Clear old data to prevent Duplicate Primary Key or constraint violations
DELETE FROM audit_logs;
DELETE FROM bid_transactions;
DELETE FROM auctions;
DELETE FROM items;
DELETE FROM users;

-- =============================================================================
-- 1. USERS (9 columns per UserDao)
-- Password hashes matched to AuthService.java using SHA-256:
--   - Admin@123  => e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7
--   - Seller@123 => bd28c94800c2be055b3329f8dd63a3d5a4137c0def2517bf4fce85eb11e62853
--   - Bidder@123 => dd320d7626b4ea475e3efd2c227e76fb9ac5311c3d26c89757911f6294e3ce1e
-- =============================================================================

INSERT INTO users (id, username, password_hash, email, role, extra_int, is_locked, created_at, updated_at)
VALUES
-- Admins
('u-adm-01', 'admin01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', 'admin@bidhub.com', 'ADMIN', 1, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),

-- Sellers
('u-sel-01', 'seller01', 'bd28c94800c2be055b3329f8dd63a3d5a4137c0def2517bf4fce85eb11e62853', 'seller01@bidhub.com', 'SELLER', 10, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),
('u-sel-02', 'seller02', 'bd28c94800c2be055b3329f8dd63a3d5a4137c0def2517bf4fce85eb11e62853', 'seller02@bidhub.com', 'SELLER', 5, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),
('u-sel-03', 'seller03', 'bd28c94800c2be055b3329f8dd63a3d5a4137c0def2517bf4fce85eb11e62853', 'seller03@bidhub.com', 'SELLER', 8, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),

-- Bidders
('u-bid-01', 'bidder01', 'dd320d7626b4ea475e3efd2c227e76fb9ac5311c3d26c89757911f6294e3ce1e', 'bidder01@bidhub.com', 'BIDDER', 20, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),
('u-bid-02', 'bidder02', 'dd320d7626b4ea475e3efd2c227e76fb9ac5311c3d26c89757911f6294e3ce1e', 'bidder02@bidhub.com', 'BIDDER', 15, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),
('u-bid-03', 'bidder03', 'dd320d7626b4ea475e3efd2c227e76fb9ac5311c3d26c89757911f6294e3ce1e', 'bidder03@bidhub.com', 'BIDDER', 42, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),
('u-bid-04', 'bidder04', 'dd320d7626b4ea475e3efd2c227e76fb9ac5311c3d26c89757911f6294e3ce1e', 'bidder04@bidhub.com', 'BIDDER', 12, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00'),
('u-bid-05', 'bidder05', 'dd320d7626b4ea475e3efd2c227e76fb9ac5311c3d26c89757911f6294e3ce1e', 'bidder05@bidhub.com', 'BIDDER', 33, 0, '2026-05-15T00:00:00', '2026-05-15T00:00:00');

-- =============================================================================
-- 2. ITEMS (12 Distinct Premium Items with custom Unsplash images)
-- Fields: id, name, description, starting_price, item_type, seller_id, extra_data, created_at, updated_at
-- =============================================================================

INSERT INTO items (id, name, description, starting_price, item_type, seller_id, extra_data, created_at, updated_at)
VALUES
    -- Electronics
    (
        'i-elec-01', 'MacBook Pro M3 Max', '14-inch, 36GB RAM, 1TB SSD. Space Black. Like new 99%.',
        60000000.0, 'ELECTRONICS', 'u-sel-01', 
        '{"brand":"Apple","warrantyMonths":12,"imageUrl":"https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-elec-02', 'iPhone 15 Pro Max', 'Natural Titanium, 256GB, Fullbox VN/A.',
        28000000.0, 'ELECTRONICS', 'u-sel-01', 
        '{"brand":"Apple","warrantyMonths":24,"imageUrl":"https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-elec-03', 'Sony WH-1000XM5 Headset', 'Tai nghe chống ồn chủ động đỉnh cao, màu Bạc thời thượng.',
        6500000.0, 'ELECTRONICS', 'u-sel-02', 
        '{"brand":"Sony","warrantyMonths":12,"imageUrl":"https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-elec-04', 'iPad Pro M4 Ultra Thin', 'Màn hình Tandem OLED siêu mỏng nhẹ, hiệu năng M4 vượt trội.',
        32000000.0, 'ELECTRONICS', 'u-sel-02', 
        '{"brand":"Apple","warrantyMonths":12,"imageUrl":"https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    -- Art
    (
        'i-art-01', 'Tranh sơn mài "Hạ Long"', 'Tranh sơn mài truyền thống độc bản vẽ Vịnh Hạ Long, nghệ nhân ký tên.',
        15000000.0, 'ART', 'u-sel-02', 
        '{"artist":"Trần Văn B","yearCreated":2024,"imageUrl":"https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-art-02', 'Bức tranh "Phố cổ Hà Nội"', 'Tranh sơn dầu vẽ phố cổ Hà Nội sau cơn mưa thu mát mẻ.',
        22000000.0, 'ART', 'u-sel-03', 
        '{"artist":"Nguyễn Bá Cường","yearCreated":2023,"imageUrl":"https://images.unsplash.com/photo-1579783928621-7a13d66a62d1?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-art-03', 'Tượng gỗ "Quan Vân Trường"', 'Tượng điêu khắc thủ công gỗ hương ta liền khối, tinh xảo sắc nét.',
        35000000.0, 'ART', 'u-sel-03', 
        '{"artist":"Nghệ nhân Âu Lạc","yearCreated":2022,"imageUrl":"https://images.unsplash.com/photo-1513519245088-0e12902e5a38?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-art-04', 'Bình gốm Chu Đậu vẽ vàng', 'Họa tiết chim hoa đắp nổi tinh xảo, dát vàng kim sang trọng.',
        8000000.0, 'ART', 'u-sel-02', 
        '{"artist":"Nghệ nhân Hải Dương","yearCreated":2021,"imageUrl":"https://images.unsplash.com/photo-1612196808214-b8e1d6145a8c?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    -- Vehicle
    (
        'i-veh-01', 'Mercedes-Benz E300 AMG', 'Màu Đen obsidian sang trọng, lăn bánh chuẩn 18,000 km.',
        2100000000.0, 'VEHICLE', 'u-sel-01', 
        '{"manufacturer":"Mercedes-Benz","year":2022,"mileageKm":18000,"imageUrl":"https://images.unsplash.com/photo-1617531653332-bd46c24f2068?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-veh-02', 'Ducati Panigale V4 S', 'Đỏ huyền thoại thể thao đầy mã lực, odo cực lướt.',
        850000000.0, 'VEHICLE', 'u-sel-02', 
        '{"manufacturer":"Ducati","year":2023,"mileageKm":4500,"imageUrl":"https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-veh-03', 'Tesla Model Y Long Range', 'Xe điện thông minh nhập nguyên chiếc từ Mỹ, màu trắng tinh khiết.',
        1200000000.0, 'VEHICLE', 'u-sel-03', 
        '{"manufacturer":"Tesla","year":2023,"mileageKm":12000,"imageUrl":"https://images.unsplash.com/photo-1619767886558-efdc259cde1a?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    (
        'i-veh-04', 'Porsche 911 Carrera S', 'Sơn xám cá tính GT Silver Metallic, odo cực ngắn.',
        6500000000.0, 'VEHICLE', 'u-sel-01', 
        '{"manufacturer":"Porsche","year":2021,"mileageKm":9500,"imageUrl":"https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=800&q=80"}',
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    );

-- =============================================================================
-- 3. AUCTIONS (12 Sessions covering RUNNING, PENDING, FINISHED)
-- Constraints & Consistency Rules:
--   - If has bids, current_highest_bid = MAX(bid_amount) AND highest_bidder_id = bidder_id of that bid.
--   - If no bids, current_highest_bid = starting_price AND highest_bidder_id = NULL.
--   - Time zones aligned with: 2026-05-19T21:40:00
-- =============================================================================

INSERT INTO auctions (id, item_id, start_time, end_time, starting_price, current_highest_bid, highest_bidder_id, status, minimum_increment, created_at, updated_at)
VALUES
    -- ==================== RUNNING SESSIONS (6 Sessions) ====================
    -- auc-001: MacBook Pro M3 Max - Ends May 25, 2026. 6 bids.
    (
        'auc-001', 'i-elec-01', '2026-05-18T10:00:00', '2026-05-25T18:00:00',
        60000000.0, 64500000.0, 'u-bid-03', 'RUNNING', 500000.0,
        '2026-05-15T00:00:00', '2026-05-18T15:20:00'
    ),
    -- auc-002: iPhone 15 Pro Max - Ends May 25, 2026. 4 bids.
    (
        'auc-002', 'i-elec-02', '2026-05-18T12:00:00', '2026-05-25T12:00:00',
        28000000.0, 31000000.0, 'u-bid-04', 'RUNNING', 500000.0,
        '2026-05-15T00:00:00', '2026-05-18T14:15:00'
    ),
    -- auc-004: Mercedes-Benz E300 - Ends May 26, 2026. 4 bids.
    (
        'auc-004', 'i-veh-01', '2026-05-18T08:00:00', '2026-05-26T20:00:00',
        2100000000.0, 2250000000.0, 'u-bid-02', 'RUNNING', 10000000.0,
        '2026-05-15T00:00:00', '2026-05-18T13:20:00'
    ),
    -- auc-007: Sony WH-1000XM5 - Ends May 24, 2026. 7 bids.
    (
        'auc-007', 'i-elec-03', '2026-05-19T08:00:00', '2026-05-24T18:00:00',
        6500000.0, 7400000.0, 'u-bid-05', 'RUNNING', 100000.0,
        '2026-05-15T00:00:00', '2026-05-19T13:20:00'
    ),
    -- auc-008: iPad Pro M4 - Ends May 26, 2026. 5 bids.
    (
        'auc-008', 'i-elec-04', '2026-05-19T10:00:00', '2026-05-26T21:00:00',
        32000000.0, 36000000.0, 'u-bid-02', 'RUNNING', 500000.0,
        '2026-05-15T00:00:00', '2026-05-19T14:40:00'
    ),
    -- auc-011: Porsche 911 Carrera S - Ends May 27, 2026. 5 bids.
    (
        'auc-011', 'i-veh-04', '2026-05-18T09:00:00', '2026-05-27T17:00:00',
        6500000000.0, 6800000000.0, 'u-bid-01', 'RUNNING', 50000000.0,
        '2026-05-15T00:00:00', '2026-05-19T15:10:00'
    ),

    -- ==================== PENDING SESSIONS (3 Sessions) ====================
    -- auc-005: Ducati Panigale V4 S - Starts May 20, 2026. 0 bids.
    (
        'auc-005', 'i-veh-02', '2026-05-20T09:00:00', '2026-05-27T18:00:00',
        850000000.0, 850000000.0, NULL, 'OPEN', 5000000.0,
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    -- auc-010: Tesla Model Y - Starts May 21, 2026. 0 bids.
    (
        'auc-010', 'i-veh-03', '2026-05-21T10:00:00', '2026-05-28T16:00:00',
        1200000000.0, 1200000000.0, NULL, 'OPEN', 10000000.0,
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),
    -- auc-012: Tượng gỗ "Quan Vân Trường" - Starts May 20, 2026. 0 bids.
    (
        'auc-012', 'i-art-03', '2026-05-20T14:00:00', '2026-05-27T18:00:00',
        35000000.0, 35000000.0, NULL, 'OPEN', 1000000.0,
        '2026-05-15T00:00:00', '2026-05-15T00:00:00'
    ),

    -- ==================== FINISHED SESSIONS (3 Sessions) ====================
    -- auc-003: Tranh sơn mài - Finished May 16, 2026. 5 bids.
    (
        'auc-003', 'i-art-01', '2026-05-15T09:00:00', '2026-05-16T18:00:00',
        15000000.0, 18500000.0, 'u-bid-01', 'FINISHED', 500000.0,
        '2026-05-15T00:00:00', '2026-05-16T15:45:00'
    ),
    -- auc-006: Phố cổ Hà Nội - Finished May 17, 2026. 4 bids.
    (
        'auc-006', 'i-art-02', '2026-05-16T08:00:00', '2026-05-17T17:00:00',
        22000000.0, 24500000.0, 'u-bid-03', 'FINISHED', 500000.0,
        '2026-05-15T00:00:00', '2026-05-16T16:00:00'
    ),
    -- auc-009: Bình gốm Chu Đậu - Finished May 18, 2026. 6 bids.
    (
        'auc-009', 'i-art-04', '2026-05-17T09:00:00', '2026-05-18T22:00:00',
        8000000.0, 9800000.0, 'u-bid-05', 'FINISHED', 200000.0,
        '2026-05-15T00:00:00', '2026-05-18T21:45:00'
    );

-- =============================================================================
-- 4. BID TRANSACTIONS (46 Bids total representing active and diverse curves)
-- =============================================================================

INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, bid_time)
VALUES
    -- Bids for auc-001 (MacBook Pro M3 Max)
    ('bid-tx-001', 'auc-001', 'u-bid-01', 60500000.0, '2026-05-18T10:15:00'),
    ('bid-tx-002', 'auc-001', 'u-bid-02', 61000000.0, '2026-05-18T11:00:00'),
    ('bid-tx-003', 'auc-001', 'u-bid-01', 62000000.0, '2026-05-18T11:30:00'),
    ('bid-tx-004', 'auc-001', 'u-bid-03', 63000000.0, '2026-05-18T12:00:00'),
    ('bid-tx-005', 'auc-001', 'u-bid-02', 63500000.0, '2026-05-18T13:45:00'),
    ('bid-tx-006', 'auc-001', 'u-bid-03', 64500000.0, '2026-05-18T15:20:00'),

    -- Bids for auc-002 (iPhone 15 Pro Max)
    ('bid-tx-007', 'auc-002', 'u-bid-02', 28500000.0, '2026-05-18T12:10:00'),
    ('bid-tx-008', 'auc-002', 'u-bid-05', 29000000.0, '2026-05-18T12:30:00'),
    ('bid-tx-009', 'auc-002', 'u-bid-02', 30000000.0, '2026-05-18T13:00:00'),
    ('bid-tx-010', 'auc-002', 'u-bid-04', 31000000.0, '2026-05-18T14:15:00'),

    -- Bids for auc-003 (Tranh sơn mài Hạ Long - FINISHED)
    ('bid-tx-011', 'auc-003', 'u-bid-05', 15500000.0, '2026-05-15T10:00:00'),
    ('bid-tx-012', 'auc-003', 'u-bid-01', 16500000.0, '2026-05-15T12:30:00'),
    ('bid-tx-013', 'auc-003', 'u-bid-02', 17000000.0, '2026-05-15T15:00:00'),
    ('bid-tx-014', 'auc-003', 'u-bid-05', 18000000.0, '2026-05-16T11:00:00'),
    ('bid-tx-015', 'auc-003', 'u-bid-01', 18500000.0, '2026-05-16T15:45:00'),

    -- Bids for auc-004 (Mercedes-Benz E300)
    ('bid-tx-016', 'auc-004', 'u-bid-03', 2120000000.0, '2026-05-18T08:30:00'),
    ('bid-tx-017', 'auc-004', 'u-bid-02', 2150000000.0, '2026-05-18T09:15:00'),
    ('bid-tx-018', 'auc-004', 'u-bid-04', 2200000000.0, '2026-05-18T10:45:00'),
    ('bid-tx-019', 'auc-004', 'u-bid-02', 2250000000.0, '2026-05-18T13:20:00'),

    -- Bids for auc-006 (Phố cổ Hà Nội - FINISHED)
    ('bid-tx-020', 'auc-006', 'u-bid-04', 22500000.0, '2026-05-16T09:30:00'),
    ('bid-tx-021', 'auc-006', 'u-bid-03', 23500000.0, '2026-05-16T11:20:00'),
    ('bid-tx-022', 'auc-006', 'u-bid-04', 24000000.0, '2026-05-16T14:10:00'),
    ('bid-tx-023', 'auc-006', 'u-bid-03', 24500000.0, '2026-05-16T16:00:00'),

    -- Bids for auc-007 (Sony WH-1000XM5)
    ('bid-tx-024', 'auc-007', 'u-bid-01', 6600000.0, '2026-05-19T08:30:00'),
    ('bid-tx-025', 'auc-007', 'u-bid-03', 6700000.0, '2026-05-19T09:00:00'),
    ('bid-tx-026', 'auc-007', 'u-bid-02', 6900000.0, '2026-05-19T09:45:00'),
    ('bid-tx-027', 'auc-007', 'u-bid-05', 7000000.0, '2026-05-19T10:15:00'),
    ('bid-tx-028', 'auc-007', 'u-bid-01', 7150000.0, '2026-05-19T11:00:00'),
    ('bid-tx-029', 'auc-007', 'u-bid-03', 7250000.0, '2026-05-19T11:45:00'),
    ('bid-tx-030', 'auc-007', 'u-bid-05', 7400000.0, '2026-05-19T13:20:00'),

    -- Bids for auc-008 (iPad Pro M4 Ultra Thin)
    ('bid-tx-031', 'auc-008', 'u-bid-04', 32500000.0, '2026-05-19T10:20:00'),
    ('bid-tx-032', 'auc-008', 'u-bid-02', 33500000.0, '2026-05-19T11:15:00'),
    ('bid-tx-033', 'auc-008', 'u-bid-05', 34500000.0, '2026-05-19T12:00:00'),
    ('bid-tx-034', 'auc-008', 'u-bid-03', 35000000.0, '2026-05-19T13:10:00'),
    ('bid-tx-035', 'auc-008', 'u-bid-02', 36000000.0, '2026-05-19T14:40:00'),

    -- Bids for auc-009 (Bình gốm Chu Đậu - FINISHED)
    ('bid-tx-036', 'auc-009', 'u-bid-01', 8200000.0, '2026-05-17T10:00:00'),
    ('bid-tx-037', 'auc-009', 'u-bid-02', 8500000.0, '2026-05-17T12:30:00'),
    ('bid-tx-038', 'auc-009', 'u-bid-04', 8800000.0, '2026-05-17T15:40:00'),
    ('bid-tx-039', 'auc-009', 'u-bid-05', 9200000.0, '2026-05-18T09:15:00'),
    ('bid-tx-040', 'auc-009', 'u-bid-02', 9500000.0, '2026-05-18T14:30:00'),
    ('bid-tx-041', 'auc-009', 'u-bid-05', 9800000.0, '2026-05-18T21:45:00'),

    -- Bids for auc-011 (Porsche 911 Carrera S)
    ('bid-tx-042', 'auc-011', 'u-bid-01', 6550000000.0, '2026-05-18T10:30:00'),
    ('bid-tx-043', 'auc-011', 'u-bid-03', 6600000000.0, '2026-05-18T14:20:00'),
    ('bid-tx-044', 'auc-011', 'u-bid-02', 6700000000.0, '2026-05-18T16:00:00'),
    ('bid-tx-045', 'auc-011', 'u-bid-05', 6750000000.0, '2026-05-19T09:30:00'),
    ('bid-tx-046', 'auc-011', 'u-bid-01', 6800000000.0, '2026-05-19T15:10:00');

-- =============================================================================
-- 5. AUDIT LOGS (Real Action Logging)
-- =============================================================================

INSERT INTO audit_logs (id, user_id, action, details, created_at)
VALUES
    ('log-01', 'u-adm-01', 'LOGIN', 'Admin login successful from IP 127.0.0.1', '2026-05-15T00:00:01'),
    ('log-02', 'u-sel-01', 'ITEM_CREATED', 'Created new item MacBook Pro M3 Max (i-elec-01)', '2026-05-15T01:30:00'),
    ('log-03', 'u-sel-01', 'ITEM_CREATED', 'Created new item iPhone 15 Pro Max (i-elec-02)', '2026-05-15T01:35:00'),
    ('log-04', 'u-sel-01', 'AUCTION_CREATED', 'Created auction auc-001 for i-elec-01', '2026-05-15T02:00:00'),
    ('log-05', 'u-bid-01', 'PLACE_BID', 'Placed bid 60,500,000 on auction auc-001', '2026-05-18T10:15:00'),
    ('log-06', 'u-bid-02', 'PLACE_BID', 'Placed bid 61,000,000 on auction auc-001', '2026-05-18T11:00:00'),
    ('log-07', 'u-bid-01', 'PLACE_BID', 'Placed bid 62,000,000 on auction auc-001', '2026-05-18T11:30:00'),
    ('log-08', 'u-bid-03', 'PLACE_BID', 'Placed bid 63,000,000 on auction auc-001', '2026-05-18T12:00:00'),
    ('log-09', 'u-bid-02', 'PLACE_BID', 'Placed bid 63,500,000 on auction auc-001', '2026-05-18T13:45:00'),
    ('log-10', 'u-bid-03', 'PLACE_BID', 'Placed bid 64,500,000 on auction auc-001', '2026-05-18T15:20:00');

-- =============================================================================
-- End of Script
-- =============================================================================