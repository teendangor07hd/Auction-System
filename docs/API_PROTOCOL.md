# BidHub — Tài liệu Đặc tả Giao thức API (API Protocol Specification)

Tài liệu này định nghĩa giao thức trao đổi dữ liệu định dạng JSON giữa Client và Server thông qua giao tiếp Socket trong hệ thống đấu giá trực tuyến **BidHub**.

---

## 1. Cấu trúc Gói tin Chung (Common Message Format)

Tất cả các gói tin trao đổi qua Socket giữa Client và Server đều sử dụng định dạng JSON thô (raw JSON) gửi đi theo từng dòng (kết thúc bằng ký tự xuống dòng `\n`).

### 1.1. Yêu cầu từ Client gửi đi (Client → Server Request)

```json
{
  "type": "TÊN_COMMAND",
  "token": "uuid-token-xac-thuc",
  "payload": {
    // Dữ liệu tham số cụ thể của từng command
  }
}
```

*   **`type`**: Tên lệnh (command), viết HOA, sử dụng dấu gạch dưới `_` để phân cách các từ (ví dụ: `PLACE_BID`).
*   **`token`**: Chuỗi UUID nhận được sau khi đăng nhập thành công. Đối với các lệnh không yêu cầu đăng nhập, trường này có giá trị `null` hoặc chuỗi rỗng.
*   **`payload`**: Một JSON Object chứa các tham số tương ứng với command.

### 1.2. Phản hồi thành công từ Server (Server → Client Response - OK)

```json
{
  "status": "OK",
  "type": "TÊN_COMMAND",
  "payload": {
    // Kết quả trả về cụ thể của command
  },
  "message": null
}
```

### 1.3. Phản hồi lỗi từ Server (Server → Client Response - ERROR)

```json
{
  "status": "ERROR",
  "type": "TÊN_COMMAND",
  "payload": null,
  "message": "Mô tả chi tiết lỗi để hiển thị cho người dùng"
}
```

---

## 2. Phân loại Quyền truy cập (Authorization Levels)

Các API Command trong hệ thống được bảo vệ và phân quyền dựa trên trạng thái đăng nhập và vai trò (role) của người dùng:

1.  **GUEST (Không yêu cầu đăng nhập)**: Bất kỳ client nào cũng có thể gọi.
2.  **AUTHENTICATED (Đăng nhập bất kỳ)**: Yêu cầu Token hợp lệ (có thể là BIDDER, SELLER hoặc ADMIN).
3.  **BIDDER**: Người tham gia đặt giá đấu giá.
4.  **SELLER**: Người đăng bán và tạo phiên đấu giá.
5.  **ADMIN**: Quản trị viên hệ thống.

---

## 3. Chi tiết các API Commands

### 3.1. Hệ thống & Kết nối (System & Connection)

#### 3.1.1. `PING` — Kiểm tra kết nối
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Kiểm tra tình trạng kết nối tới server và đồng bộ hóa thời gian hệ thống.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    {
      "message": "pong",
      "serverTime": "2026-05-27T22:15:30.123456",
      "sessionId": "session-uuid-here"
    }
    ```

#### 3.1.2. `GET_HOME_STATS` — Lấy thống kê trang chủ
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Lấy các số liệu thống kê tổng quát phục vụ cho giao diện trang chủ.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    {
      "activeAuctions": 5,      // Số lượng phiên đang chạy
      "totalParticipants": 12,  // Tổng số người tham gia đặt giá
      "totalVolume": 185000000.0 // Tổng số tiền giao dịch
    }
    ```

---

### 3.2. Quản lý Xác thực (Authentication)

#### 3.2.1. `REGISTER` — Đăng ký tài khoản
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Đăng ký người dùng mới. Không cho phép đăng ký trực tiếp vai trò `ADMIN`.
*   **Request Payload**:
    ```json
    {
      "username": "nguyen_van_a",
      "password": "matkhau12345 (tối thiểu 8 ký tự)",
      "email": "vana@gmail.com",
      "role": "BIDDER" // Hoặc "SELLER"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "userId": "user-uuid",
      "username": "nguyen_van_a",
      "role": "BIDDER"
    }
    ```

#### 3.2.2. `LOGIN` — Đăng nhập
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Xác thực tài khoản và cấp Token phiên làm việc.
*   **Request Payload**:
    ```json
    {
      "username": "nguyen_van_a",
      "password": "matkhau12345"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "token": "token-uuid-dung-de-goi-api-sau",
      "userId": "user-uuid",
      "username": "nguyen_van_a",
      "role": "BIDDER" // "BIDDER", "SELLER" hoặc "ADMIN"
    }
    ```

#### 3.2.3. `LOGOUT` — Đăng xuất
*   **Quyền yêu cầu**: AUTHENTICATED
*   **Mô tả**: Hủy Token phiên làm việc trên hệ thống server.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Dang xuat thanh cong."
    }
    ```

---

### 3.3. Quản lý Sản phẩm (Item Management)

#### 3.3.1. `CREATE_ITEM` — Tạo mới sản phẩm
*   **Quyền yêu cầu**: SELLER
*   **Mô tả**: Người bán đăng ký sản phẩm mới vào kho lưu trữ (chưa đấu giá).
*   **Request Payload**:
    ```json
    {
      "name": "Bức tranh sơn dầu Phố Cổ",
      "description": "Tranh vẽ tay năm 2024",
      "startingPrice": 5000000.0,
      "itemType": "ART", // ELECTRONICS, ART, FASHION, OTHER
      "imageUrl": "https://link-to-image.jpg",
      "extras": {
        // Thuộc tính mở rộng tùy theo loại sản phẩm
        "artist": "Bùi Xuân Phái",
        "medium": "Oil on Canvas"
      }
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "itemId": "item-uuid",
      "name": "Bức tranh sơn dầu Phố Cổ",
      "itemType": "ART",
      "startingPrice": "5000000.0"
    }
    ```

#### 3.3.2. `GET_ITEM_LIST` — Lấy danh sách sản phẩm
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Xem danh sách tất cả sản phẩm hiện có trong hệ thống và trạng thái đấu giá hiện tại.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    [
      {
        "id": "item-uuid",
        "name": "Bức tranh sơn dầu Phố Cổ",
        "description": "Tranh vẽ tay năm 2024",
        "itemType": "ART",
        "startingPrice": 5000000.0,
        "imageUrl": "https://link-to-image.jpg",
        "sellerName": "nguyen_van_a",
        "auctionStatus": "AVAILABLE" // AVAILABLE, AUCTIONING, SOLD
      }
    ]
    ```

#### 3.3.3. `GET_ITEM_DETAIL` — Chi tiết sản phẩm
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Lấy thông tin chi tiết đầy đủ của một sản phẩm.
*   **Request Payload**:
    ```json
    {
      "itemId": "item-uuid"
    }
    ```
*   **Response Payload (OK)**: Trả về toàn bộ thuộc tính của Object sản phẩm, bao gồm thông tin chi tiết các thuộc tính động mở rộng (`extras`).

#### 3.3.4. `UPDATE_ITEM` — Cập nhật sản phẩm
*   **Quyền yêu cầu**: SELLER (Chủ sản phẩm)
*   **Mô tả**: Cập nhật thông tin chi tiết cho sản phẩm chưa tham gia đấu giá nào.
*   **Request Payload**:
    ```json
    {
      "itemId": "item-uuid",
      "name": "Tên sản phẩm mới", // Tùy chọn
      "description": "Mô tả mới", // Tùy chọn
      "startingPrice": 5500000.0, // Tùy chọn
      "imageUrl": "https://link-to-image-new.jpg" // Tùy chọn
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Đã cập nhật sản phẩm.",
      "itemId": "item-uuid"
    }
    ```

#### 3.3.5. `DELETE_ITEM` — Xóa sản phẩm
*   **Quyền yêu cầu**: SELLER (Chủ sản phẩm)
*   **Mô tả**: Xóa sản phẩm khỏi kho lưu trữ (chỉ cho phép khi sản phẩm chưa từng được tạo phiên đấu giá).
*   **Request Payload**:
    ```json
    {
      "itemId": "item-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Xoa san pham thanh cong.",
      "itemId": "item-uuid"
    }
    ```

#### 3.3.6. `LIST_MY_ITEMS` — Danh sách sản phẩm của tôi
*   **Quyền yêu cầu**: SELLER
*   **Mô tả**: Lấy danh sách toàn bộ sản phẩm do chính Seller đang đăng nhập quản lý.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**: Định dạng danh sách tương tự như `GET_ITEM_LIST` nhưng có thêm thuộc tính `"itemId"`.

---

### 3.4. Quản lý Phiên đấu giá (Auction Management)

#### 3.4.1. `CREATE_AUCTION` — Tạo phiên đấu giá
*   **Quyền yêu cầu**: SELLER (Chủ sản phẩm)
*   **Mô tả**: Mở phiên đấu giá cho sản phẩm của mình, cấu hình thời gian chạy và bước giá tối thiểu.
*   **Request Payload**:
    ```json
    {
      "itemId": "item-uuid",
      "startingPrice": 5000000.0,
      "minimumIncrement": 200000.0,
      "startTime": "2026-05-28T09:00:00",
      "endTime": "2026-05-28T18:00:00"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "auctionId": "auction-uuid",
      "itemId": "item-uuid",
      "startingPrice": 5000000.0,
      "status": "OPEN" // OPEN (Chờ bắt đầu) hoặc RUNNING (Đang diễn ra)
    }
    ```

#### 3.4.2. `GET_AUCTION_LIST` — Lấy danh sách phiên đấu giá
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Xem danh sách tất cả các phiên đấu giá (ở các trạng thái khác nhau).
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    [
      {
        "id": "auction-uuid",
        "itemId": "item-uuid",
        "itemName": "Bức tranh sơn dầu Phố Cổ",
        "imageUrl": "https://link-to-image.jpg",
        "sellerName": "nguyen_van_a",
        "itemType": "ART",
        "startingPrice": 5000000.0,
        "currentHighestBid": 5400000.0,
        "highestBidderId": "bidder-uuid",
        "startTime": "2026-05-28T09:00:00",
        "endTime": "2026-05-28T18:00:00",
        "status": "RUNNING", // OPEN, RUNNING, FINISHED, PAID, CANCELED
        "minimumIncrement": 200000.0
      }
    ]
    ```

#### 3.4.3. `GET_AUCTION_DETAIL` — Chi tiết phiên đấu giá
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Lấy thông tin chi tiết một phiên đấu giá kèm theo danh sách toàn bộ lịch sử đấu giá (từ thấp đến cao).
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "auction": {
        "id": "auction-uuid",
        "itemId": "item-uuid",
        "itemName": "Bức tranh sơn dầu Phố Cổ",
        "description": "Mô tả sản phẩm...",
        "imageUrl": "https://link-to-image.jpg",
        "startingPrice": 5000000.0,
        "currentHighestBid": 5400000.0,
        "highestBidderId": "bidder-uuid",
        "highestBidderName": "tran_van_b",
        "startTime": "2026-05-28T09:00:00",
        "endTime": "2026-05-28T18:00:00",
        "status": "RUNNING",
        "minimumIncrement": 200000.0
      },
      "bidHistory": [
        {
          "id": "bid-tx-uuid",
          "bidAmount": 5200000.0,
          "bidTime": "2026-05-28T10:15:30",
          "bidderId": "other-bidder-uuid",
          "bidderName": "le_thi_c"
        },
        {
          "id": "bid-tx-uuid-2",
          "bidAmount": 5400000.0,
          "bidTime": "2026-05-28T11:20:45",
          "bidderId": "bidder-uuid",
          "bidderName": "tran_van_b"
        }
      ]
    }
    ```

#### 3.4.4. `PLACE_BID` — Đặt giá
*   **Quyền yêu cầu**: BIDDER
*   **Mô tả**: Đặt một giá thầu mới cho phiên đấu giá đang trong trạng thái `RUNNING`.
    *   *Quy tắc nghiệp vụ*: Giá đặt phải lớn hơn giá cao nhất hiện tại cộng với bước giá tối thiểu (`minimumIncrement`).
    *   *Anti-Sniping*: Nếu lượt đặt giá diễn ra trong thời gian ngắn cuối cùng trước khi hết giờ (ví dụ: 30 giây cuối), thời gian kết thúc phiên sẽ tự động được kéo dài thêm nhằm đảm bảo công bằng.
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid",
      "bidAmount": 5600000.0
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "auctionId": "auction-uuid",
      "currentHighestBid": 5600000.0,
      "highestBidderId": "bidder-uuid"
    }
    ```

#### 3.4.5. `SUBSCRIBE_AUCTION` — Đăng ký quan sát thời gian thực
*   **Quyền yêu cầu**: GUEST
*   **Mô tả**: Đăng ký Socket hiện tại để lắng nghe các cập nhật biến động giá thầu thời gian thực của một phiên đấu giá.
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "auctionId": "auction-uuid",
      "message": "Da subscribe thanh cong"
    }
    ```

#### 3.4.6. `GET_MY_AUCTIONS` — Danh sách đấu giá của tôi
*   **Quyền yêu cầu**: SELLER
*   **Mô tả**: Người bán lấy danh sách phiên do mình tạo. Khi phiên có trạng thái `FINISHED` hoặc `PAID`, Server sẽ trả về thêm thông tin chi tiết của người thắng đấu giá để phục vụ khâu giao dịch liên hệ.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    [
      {
        "id": "auction-uuid",
        "itemId": "item-uuid",
        "itemName": "Bức tranh sơn dầu Phố Cổ",
        "imageUrl": "https://link-to-image.jpg",
        "startingPrice": 5000000.0,
        "currentHighestBid": 5600000.0,
        "status": "FINISHED",
        "startTime": "2026-05-28T09:00:00",
        "endTime": "2026-05-28T18:00:00",
        "winnerName": "tran_van_b",
        "winnerEmail": "bidder@gmail.com"
      }
    ]
    ```

#### 3.4.7. `CANCEL_AUCTION` — Hủy phiên đấu giá
*   **Quyền yêu cầu**: SELLER (Chủ phiên đấu giá)
*   **Mô tả**: Hủy phiên đấu giá đang ở trạng thái `OPEN` (Chờ bắt đầu). Phiên sẽ bị xóa bỏ.
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Đã hủy phiên đấu giá.",
      "auctionId": "auction-uuid"
    }
    ```

#### 3.4.8. `GET_WON_AUCTIONS` — Danh sách phiên đấu giá thắng cuộc
*   **Quyền yêu cầu**: BIDDER
*   **Mô tả**: Người mua lấy danh sách các phiên đấu giá mà mình đã thắng đấu giá thành công (trạng thái `FINISHED` hoặc `PAID`).
*   **Request Payload**: `{}`
*   **Response Payload (OK)**: Danh sách các Object phiên đấu giá kèm tên người bán (`sellerName`) và thông tin sản phẩm.

#### 3.4.9. `MARK_PAID` — Xác nhận thanh toán
*   **Quyền yêu cầu**: SELLER (Chủ phiên đấu giá)
*   **Mô tả**: Người bán xác nhận đã nhận được tiền từ người mua thắng đấu giá, chuyển trạng thái phiên từ `FINISHED` sang `PAID` (Đã bán).
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "auctionId": "auction-uuid",
      "message": "Da xac nhan thanh toan."
    }
    ```

#### 3.4.10. `SELLER_CANCEL_FINISHED` — Hủy phiên kết thúc (Không thanh toán)
*   **Quyền yêu cầu**: SELLER (Chủ phiên đấu giá)
*   **Mô tả**: Người bán hủy phiên đấu giá đã kết thúc (`FINISHED`) vì lý do người mua thắng cuộc không thực hiện thanh toán. Trạng thái phiên chuyển sang `CANCELED`, sản phẩm được giải phóng về lại kho để đấu giá lại.
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "auctionId": "auction-uuid",
      "message": "Da huy phien. San pham co the dua len dau gia lai."
    }
    ```

---

### 3.5. Báo cáo & Thống kê (Reports & Statistics)

#### 3.5.1. `GET_AUCTION_REPORT` — Xuất báo cáo đấu giá
*   **Quyền yêu cầu**: SELLER hoặc ADMIN
*   **Mô tả**: Xuất báo cáo phẳng dạng bảng của toàn bộ phiên đấu giá có trong hệ thống, bao gồm tên sản phẩm và tên người chiến thắng.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    [
      {
        "auctionId": "auction-uuid",
        "itemId": "item-uuid",
        "itemName": "Bức tranh sơn dầu Phố Cổ",
        "status": "FINISHED",
        "startingPrice": 5000000.0,
        "currentHighestBid": 5600000.0,
        "highestBidderId": "bidder-uuid",
        "winnerName": "tran_van_b",
        "startTime": "28/05/2026 09:00:00",
        "endTime": "28/05/2026 18:00:00"
      }
    ]
    ```

#### 3.5.2. `GET_BID_HISTORY_REPORT` — Xuất báo cáo lịch sử đặt giá
*   **Quyền yêu cầu**: AUTHENTICATED
*   **Mô tả**: Xuất danh sách toàn bộ các lượt đặt giá đã ghi nhận của một phiên chỉ định (truyền `"ALL"` để xuất báo cáo lịch sử đặt giá trên toàn hệ thống).
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid" // Hoặc "ALL"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    [
      {
        "bidId": "bid-tx-uuid",
        "auctionId": "auction-uuid",
        "bidderId": "bidder-uuid",
        "itemName": "Bức tranh sơn dầu Phố Cổ",
        "bidderName": "tran_van_b",
        "bidAmount": 5600000.0,
        "bidTime": "28/05/2026 11:20:45"
      }
    ]
    ```

---

### 3.6. Hệ thống Thông báo (Notification System)

#### 3.6.1. `SEND_NOTIFICATION` — Gửi thông báo hệ thống
*   **Quyền yêu cầu**: ADMIN
*   **Mô tả**: Admin phát sóng một thông báo rộng rãi tới toàn bộ người dùng trong hệ thống.
*   **Request Payload**:
    ```json
    {
      "title": "Bảo trì định kỳ",
      "message": "Server sẽ bảo trì trong vòng 1 tiếng kể từ 23:00 hôm nay.",
      "type": "SYSTEM"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Đã gửi thông báo đến toàn bộ người dùng.",
      "title": "Bảo trì định kỳ"
    }
    ```

#### 3.6.2. `GET_NOTIFICATIONS` — Lấy danh sách thông báo
*   **Quyền yêu cầu**: AUTHENTICATED
*   **Mô tả**: Lấy toàn bộ danh sách thông báo của hệ thống kèm theo cờ đánh dấu trạng thái đã đọc (`isRead`) riêng cho từng người dùng.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    [
      {
        "id": "audit-log-id",
        "title": "Bảo trì định kỳ",
        "message": "Server sẽ bảo trì trong vòng 1 tiếng kể từ 23:00 hôm nay.",
        "type": "SYSTEM",
        "createdAt": "28/05/2026 21:00:00",
        "isRead": false
      }
    ]
    ```

#### 3.6.3. `MARK_NOTIFICATION_READ` — Đánh dấu thông báo đã đọc
*   **Quyền yêu cầu**: AUTHENTICATED
*   **Mô tả**: Đánh dấu đã xem đối với một thông báo cụ thể.
*   **Request Payload**:
    ```json
    {
      "notificationId": "audit-log-id"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Đã đánh dấu đọc"
    }
    ```

---

### 3.7. Quản trị hệ thống (Admin Console)

#### 3.7.1. `GET_USER_LIST` — Lấy danh sách tài khoản
*   **Quyền yêu cầu**: ADMIN
*   **Mô tả**: Xem danh sách toàn bộ người dùng và tình trạng khóa tài khoản của họ.
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    [
      {
        "id": "user-uuid",
        "username": "nguyen_van_a",
        "email": "vana@gmail.com",
        "role": "BIDDER",
        "isLocked": false
      }
    ]
    ```

#### 3.7.2. `LOCK_USER` — Khóa tài khoản
*   **Quyền yêu cầu**: ADMIN
*   **Mô tả**: Khóa tài khoản người dùng vi phạm. Người dùng bị khóa sẽ không thể tiếp tục đăng nhập hoặc gọi các API cần quyền hạn.
*   **Request Payload**:
    ```json
    {
      "targetUserId": "user-uuid" // Hoặc "userId": "user-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Da khoa tai khoan."
    }
    ```

#### 3.7.3. `UNLOCK_USER` — Mở khóa tài khoản
*   **Quyền yêu cầu**: ADMIN
*   **Mô tả**: Mở khóa tài khoản đã bị khóa trước đó.
*   **Request Payload**:
    ```json
    {
      "targetUserId": "user-uuid" // Hoặc "userId": "user-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Da mo khoa tai khoan."
    }
    ```

#### 3.7.4. `GET_AUDIT_LOG` — Lấy nhật ký hệ thống
*   **Quyền yêu cầu**: ADMIN
*   **Mô tả**: Xem lịch sử các thao tác quan trọng trên hệ thống nhằm phục vụ mục đích kiểm toán.
*   **Request Payload**:
    ```json
    {
      "limit": 50 // Giới hạn số dòng ghi nhận (1-500, mặc định là 50)
    }
    ```
*   **Response Payload (OK)**:
    ```json
    [
      {
        "id": "audit-log-id",
        "userId": "user-uuid",
        "userName": "nguyen_van_a",
        "action": "PLACE_BID",
        "details": "{\"auctionId\":\"...\",\"bidAmount\":5600000}",
        "createdAt": "28/05/2026 11:20:45"
      }
    ]
    ```

#### 3.7.5. `RUN_INTEGRITY_CHECK` — Kiểm tra toàn vẹn cơ sở dữ liệu
*   **Quyền yêu cầu**: ADMIN
*   **Mô tả**: Kích hoạt quét toàn bộ hệ thống cơ sở dữ liệu để phát hiện bất kỳ sự bất đồng nhất hoặc sai lệch dữ liệu nào (ví dụ: sai lệch số liệu giá cao nhất giữa bảng phiên đấu giá và lịch sử bid).
*   **Request Payload**: `{}`
*   **Response Payload (OK)**:
    ```json
    {
      "bidConsistencyErrors": [],
      "auctionWinnerErrors": [],
      "orphanedItemErrors": [],
      "totalErrors": 0,
      "status": "OK" // Hoặc "ERRORS_FOUND"
    }
    ```

#### 3.7.6. `ADMIN_STOP_AUCTION` — Dừng cưỡng chế phiên đấu giá
*   **Quyền yêu cầu**: ADMIN
*   **Mô tả**: Hủy/dừng khẩn cấp một phiên đấu giá đang diễn ra hoặc chuẩn bị diễn ra. Trạng thái phiên chuyển sang `CANCELED`.
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Đã dừng/hủy phiên đấu giá thành công.",
      "auctionId": "auction-uuid"
    }
    ```

#### 3.7.7. `ADMIN_DELETE_AUCTION` — Xóa phiên đấu giá
*   **Quyền yêu cầu**: ADMIN
*   **Mô tả**: Xóa hoàn toàn bản ghi phiên đấu giá khỏi cơ sở dữ liệu hệ thống.
*   **Request Payload**:
    ```json
    {
      "auctionId": "auction-uuid"
    }
    ```
*   **Response Payload (OK)**:
    ```json
    {
      "message": "Đã xóa phiên đấu giá thành công.",
      "auctionId": "auction-uuid"
    }
    ```

---

## 4. Sự kiện Đẩy từ Server (Server Push Events)

Khi các Client đang kết nối tới Server, Server có thể tự động gửi đi các gói tin mà không cần đợi yêu cầu từ phía Client. Định dạng các gói tin này tương đương cấu trúc phản hồi thành công (`status: "OK"`).

### 4.1. `BID_UPDATE` — Cập nhật giá đấu thầu thời gian thực
*   **Mô tả**: Khi bất kỳ Bidder nào đặt giá thầu mới hợp lệ cho một phiên đấu giá, Server sẽ tự động phát tin nhắn JSON này tới tất cả các Client đang theo dõi phiên đấu giá đó (thông qua lệnh đăng ký `SUBSCRIBE_AUCTION`).
*   **JSON Gói tin đẩy**:
    ```json
    {
      "status": "OK",
      "type": "BID_UPDATE",
      "payload": {
        "auctionId": "auction-uuid",
        "bidderId": "bidder-uuid",
        "bidderName": "tran_van_b",
        "bidAmount": 5600000.0
      },
      "message": null
    }
    ```