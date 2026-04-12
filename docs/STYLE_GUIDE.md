# BidHub — Coding Convention (Google Java Style Guide)

> Tất cả code trong project PHẢI tuân thủ tài liệu này.
> Nguồn gốc: https://google.github.io/styleguide/javaguide.html

---

## 1. Đặt tên (Naming)

| Loại | Quy tắc | ✅ Đúng | ❌ Sai |
|------|---------|---------|--------|
| Class/Interface | UpperCamelCase | `AuctionService`, `UserDao` | `auction_service`, `userDAO` |
| Method | lowerCamelCase | `placeBid()`, `getUserById()` | `PlaceBid()`, `place_bid()` |
| Field | lowerCamelCase | `currentHighestBid`, `userId` | `CurrentHighestBid`, `user_id` |
| Constant | UPPER_SNAKE_CASE | `MAX_CONNECTIONS`, `DEFAULT_PORT` | `MaxConnections`, `default_port` |
| Package | lowercase, không dấu | `com.bidhub.server.dao` | `com.bidhub.Server.DAO` |
| Test method | lowerCamelCase hoặc dùng @DisplayName | `testPlaceBid_BidTooLow_Throws()` | `Test_place_bid()` |

---

## 2. Thụt lề và khoảng cách (Indentation & Spacing)

```java
// ✅ ĐÚNG: 2 spaces thụt lề (không phải tab)
public class Auction {
  private String id;         // 2 spaces

  public void placeBid(double amount) {
    if (amount <= 0) {       // 4 spaces (2 lần thụt lề)
      throw new IllegalArgumentException("...");
    }
  }
}

// ❌ SAI: dùng tab hoặc 4 spaces
public class Auction {
    private String id;    // 4 spaces → sai
```

---

## 3. Độ dài dòng (Line Length)

- Tối đa **100 ký tự** mỗi dòng
- Nếu dài hơn → xuống dòng, thụt lề thêm 4 spaces so với dòng trên

```java
// ✅ ĐÚNG: Xuống dòng đúng cách
MessageResponse response = MessageResponse.error(
    request.getType(),
    "Số tiền đặt giá phải lớn hơn giá hiện tại: " + currentBid);

// ❌ SAI: Dòng quá dài (> 100 ký tự)
MessageResponse response = MessageResponse.error(request.getType(), "Số tiền đặt giá phải lớn hơn giá hiện tại: " + currentBid);
```

---

## 4. Javadoc — Khi nào bắt buộc

Bắt buộc có Javadoc cho:
- Tất cả `public class` và `public interface`
- Tất cả `public` và `protected` methods
- Field phức tạp hoặc có ý nghĩa đặc biệt

```java
// ✅ ĐÚNG: Javadoc đầy đủ
/**
 * Đặt giá mới cho phiên đấu giá.
 *
 * @param auctionId ID của phiên đấu giá
 * @param bidAmount  số tiền đặt giá (phải > giá hiện tại)
 * @throws InvalidBidException  nếu bidAmount ≤ currentHighestBid
 * @throws AuctionClosedException nếu phiên đã kết thúc
 */
public BidTransaction placeBid(String auctionId, double bidAmount) { ... }

// ❌ SAI: Không có Javadoc cho public method
public BidTransaction placeBid(String auctionId, double bidAmount) { ... }
```

---

## 5. Thứ tự access modifier trong class

```java
public class MyClass {
  // Thứ tự: public → protected → package-private → private
  // Trong mỗi nhóm: static fields → instance fields → constructors → methods

  public static final int MAX = 100;     // 1. public static field
  private static int count = 0;          // 2. private static field
  private String name;                   // 3. instance field

  public MyClass(String name) { ... }    // 4. constructor
  public String getName() { ... }        // 5. public method
  private void helper() { ... }          // 6. private method
}
```

---

## 6. Những điều KHÔNG được làm

```java
// ❌ Magic number — không dùng số literal không rõ nghĩa
if (status == 2) { ... }          // 2 là gì? FINISHED? PAID?

// ✅ ĐÚNG: Dùng Enum hoặc constant
if (status == AuctionStatus.FINISHED) { ... }

// ❌ Swallow exception — không bắt exception rồi bỏ trống
try {
  dao.save(user);
} catch (Exception e) { }       // Bug âm thầm, không bao giờ làm thế này!

// ✅ ĐÚNG: Ít nhất log hoặc rethrow
try {
  dao.save(user);
} catch (SQLException e) {
  throw new RuntimeException("Lỗi lưu user vào DB", e);
}

// ❌ System.out.println cho debug production code
System.out.println("Bid placed: " + amount);   // Từ Tuần 9: dùng Logger

// ❌ Tên biến vô nghĩa
int x = getHighestBid();    // x là gì?
String s = user.getName();  // s là gì?

// ✅ ĐÚNG: Tên biến mô tả đúng nội dung
int highestBid = getHighestBid();
String username = user.getUsername();
```

---

## 7. Utility class — Ngăn khởi tạo

```java
// ✅ ĐÚNG: Class chỉ có static methods → private constructor
public final class ConfigLoader {
  private ConfigLoader() {}   // Không ai tạo được: new ConfigLoader() → compile error
  public static String getString(String key) { ... }
}

// ❌ SAI: Để constructor mặc định public → có thể tạo instance vô nghĩa
public class ConfigLoader {
  public static String getString(String key) { ... }
  // Không có private constructor → new ConfigLoader() hợp lệ nhưng vô nghĩa
}
```