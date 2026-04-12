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