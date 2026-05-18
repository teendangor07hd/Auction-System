# BidHub — API Protocol Specification

> Tài liệu này định nghĩa format JSON trao đổi giữa Client và Server qua Socket.
> **Cả nhóm phải tuân theo format này từ Tuần 4.** Nếu muốn thay đổi, phải thảo luận nhóm trước.

## Format chung

### Request (Client → Server)

```json
{
  "type": "TÊN_COMMAND",
  "token": "uuid-token-sau-khi-login",
  "payload": {
    // Dữ liệu cụ thể của từng command
  }
}
```

- `type`: Tên command, viết HOA, dùng UNDERSCORE (ví dụ: `PLACE_BID`)
- `token`: UUID nhận được sau khi login. Để `null` nếu chưa đăng nhập
- `payload`: Object JSON chứa tham số của command

### Response (Server → Client)

```json
{
  "status": "OK",
  "type": "TÊN_COMMAND",
  "payload": {
    // Dữ liệu trả về
  },
  "message": null
}
```

```json
{
  "status": "ERROR",
  "type": "TÊN_COMMAND",
  "payload": null,
  "message": "Mô tả lỗi cho người dùng"
}
```

## Danh sách Commands (cập nhật dần theo tiến độ)

### PING — Kiểm tra kết nối (Tuần 4)

**Request:**
```json
{ "type": "PING", "token": null, "payload": {} }
```

**Response OK:**
```json
{
  "status": "OK",
  "type": "PING",
  "payload": { "message": "pong", "serverTime": "2026-04-06T10:30:00" },
  "message": null
}
```

---

### LOGIN — Đăng nhập (Tuần 5)

**Request:**
```json
{
  "type": "LOGIN",
  "token": null,
  "payload": {
    "username": "nguyen_van_a",
    "password": "matkhau123"
  }
}
```

**Response OK:**
```json
{
  "status": "OK",
  "type": "LOGIN",
  "payload": {
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-uuid-here",
    "username": "nguyen_van_a",
    "role": "BIDDER"
  },
  "message": null
}
```

**Response ERROR:**
```json
{
  "status": "ERROR",
  "type": "LOGIN",
  "payload": null,
  "message": "Sai tên đăng nhập hoặc mật khẩu"
}
```

---

### PLACE_BID — Đặt giá (Tuần 6)

**Request:**
```json
{
  "type": "PLACE_BID",
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "payload": {
    "auctionId": "auction-uuid",
    "bidAmount": 5500000.0
  }
}
```

**Response OK:**
```json
{
  "status": "OK",
  "type": "PLACE_BID",
  "payload": {
    "transactionId": "bid-tx-uuid",
    "auctionId": "auction-uuid",
    "newHighestBid": 5500000.0,
    "highestBidderId": "user-uuid",
    "bidTime": "2026-04-10T14:25:30"
  },
  "message": null
}
```

---

### BID_UPDATE — Server push realtime (Tuần 7, không phải command từ client)

Khi có bid mới, server tự động push JSON này đến tất cả client đang xem phiên:

```json
{
  "status": "OK",
  "type": "BID_UPDATE",
  "payload": {
    "auctionId": "auction-uuid",
    "newHighestBid": 5500000.0,
    "highestBidderId": "user-uuid",
    "bidTime": "2026-04-10T14:25:30",
    "totalBids": 12
  },
  "message": null
}
```

> **Lưu ý cho Client:** Khi nhận JSON có `"type": "BID_UPDATE"`, đây không phải response cho request nào cả — đây là push từ server. `EventListenerThread` phía client phải nhận và xử lý (Tuần 7).

## Commands sẽ implement (cập nhật sau)

| Command | Tuần | Người làm |
|---------|------|-----------|
| PING | 4 | Quốc Minh |
| REGISTER | 5 | Quốc Minh |
| LOGIN | 5 | Quốc Minh |
| LOGOUT | 5 | Quốc Minh |
| CREATE_ITEM | 5 | Khoa |
| GET_ITEM_LIST | 5 | Khoa |
| CREATE_AUCTION | 6 | Đăng |
| GET_AUCTION_LIST | 6 | Quốc Minh |
| GET_AUCTION_DETAIL | 6 | Quốc Minh |
| PLACE_BID | 6 | Quốc Minh |
| SUBSCRIBE_AUCTION | 7 | Quốc Minh |
| GET_BID_HISTORY | 8 | Công Minh |

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