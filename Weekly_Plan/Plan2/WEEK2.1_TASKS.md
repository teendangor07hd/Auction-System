# 📋 TUẦN 2 — BÀI TẬP CHI TIẾT: OOP Domain Model & Exception Hierarchy

> **Kick-off meeting:** Thứ 2 13/04/2026 (tối)
> **Mid-week check-in:** Thứ 5 16/04/2026 (tối)
> **Deadline nộp bài:** Thứ 7 18/04/2026, 23:59
> **Review & Merge:** Chủ nhật 19/04/2026 (sáng)

---

## 🎯 MỤC TIÊU TUẦN 2

Tuần này xây dựng **bộ xương domain** — toàn bộ các class và cây kế thừa mà tất cả các tuần sau đều phụ thuộc vào. Code tuần này phải đúng ngay từ đầu vì sửa model sau là sửa dây chuyền. Cuối tuần 2, cả nhóm phải có:

- ✅ Module `bidhub-common` mới được thêm vào parent POM (Đăng setup)
- ✅ Cây kế thừa `Entity → User → Bidder/Seller/Admin` hoàn chỉnh (Đăng)
- ✅ Cây kế thừa `Entity → Item → Electronics/Art/Vehicle` + `ItemCreator` hierarchy (Quốc Minh)
- ✅ `Auction`, `AuctionStatus`, `BidTransaction` với state machine (Công Minh)
- ✅ `BidHubException` hierarchy 7 subclass + `ValidationException` (Khoa)
- ✅ Tổng ≥ 25 test cases xanh mới (cộng dồn ≥ 40 với tuần 1)

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** các tiêu chí barem: **Thiết kế lớp & cây kế thừa (0.5đ)** + **4 trụ cột OOP — Encapsulation, Inheritance, Polymorphism, Abstraction (1.0đ)** + **Xử lý lỗi & ngoại lệ (1.0đ)**. Làm đúng tuần này = **2.5đ** trong tay.

> [!CAUTION]
> **Tuyệt đối không tạo lại các class đã có từ tuần 1** (`ConfigLoader`, `ServerApp`, `BidHubApp`, `LoginController`, `Views`). Mọi class tuần 2 là class mới, đặt trong package mới. Nếu cần dùng `ConfigLoader` — import thẳng, không copy.

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI HỌC (Tự học)

> [!CAUTION]
> Mỗi người **BẮT BUỘC** phải hoàn thành phần tự học này trước Thứ 5. Chủ nhật sẽ hỏi bất kỳ ai bất kỳ câu nào — **không được xem tài liệu khi trả lời**.
>
> Lý do phần tự học được thiết kế để **ai cũng hiểu công việc của nhau**: Đăng làm User hierarchy, Quốc Minh làm ItemFactory, Công Minh làm AuctionStatus — nhưng tất cả đều dùng `Entity` (Đăng viết) và `BidHubException` (Khoa viết). Không hiểu nền tảng = không review được code của nhau.

---

### Bài 0.1 — Abstract class vs Interface

**Tài liệu bắt buộc đọc:**
- https://docs.oracle.com/en/java/docs/books/tutorial/java/IandI/abstract.html
- https://docs.oracle.com/en/java/docs/books/tutorial/java/IandI/createinterface.html

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. `abstract class` và `interface` khác nhau ở điểm nào cốt lõi nhất? (gợi ý: trạng thái, đa kế thừa)
2. Khi nào nên dùng `abstract class`? Khi nào nên dùng `interface`? Cho ví dụ từ BidHub.
3. Trong BidHub, `Entity` là `abstract class` — tại sao không phải `interface`? Nếu đổi thành interface thì điều gì bị phá vỡ?
4. `Item` extends `Entity` và implements `Displayable` — đây là dạng kế thừa gì? Java có cho phép không?
5. Tại sao `abstract class` không thể dùng `new Entity()` trực tiếp? JVM xử lý thế nào?
6. **[Câu hỏi nâng cao]** `abstract` method trong `abstract class` khác `default` method trong `interface` thế nào?

---

### Bài 0.2 — Factory Method Pattern

**Tài liệu bắt buộc đọc:**
- https://refactoring.guru/design-patterns/factory-method (đọc phần "Structure" và "Pseudocode")

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. Factory Method Pattern giải quyết vấn đề gì? Không có pattern này, code sẽ trông như thế nào?
2. Phân biệt **Static Factory** (ví dụ: `ItemFactory.create(type, ...)` — cũ) và **Factory Method Pattern** chuẩn GoF (`ItemCreator` abstract + `ElectronicsCreator` concrete). Tại sao BidHub chuyển sang dùng Creator hierarchy?
3. Nếu sau này thêm `ItemType.JEWELRY` — phải tạo những file nào? Không cần sửa file nào? Tại sao đây là minh chứng cho **Open/Closed Principle**?
4. `ItemCreator.forType(ItemType)` là static method — tại sao không làm `ItemCreator` là Singleton? So sánh 2 approach.
5. **[Câu hỏi nâng cao]** 4 thành phần của Factory Method Pattern (Creator, ConcreteCreator, Product, ConcreteProduct) ánh xạ vào code BidHub như thế nào? Vẽ sơ đồ UML.

---

### Bài 0.3 — Java Enum với method & State Machine

**Tài liệu bắt buộc đọc:**
- https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html (phần "Enum Types")

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. Java `enum` có phải là một class không? Nó được compile thành gì trong bytecode?
2. Tại sao `AuctionStatus.RUNNING.canBid()` trả `true` còn `FINISHED.canBid()` trả `false`? Giải thích cơ chế abstract method trong enum.
3. State machine là gì? Vẽ diagram transitions của `AuctionStatus` (5 trạng thái, 5 mũi tên).
4. `isTerminal()` trong `AuctionStatus` nghĩa là gì? Tại sao cần method này?
5. **[Câu hỏi nâng cao]** Tại sao `transitionTo()` trong `Auction` ném `IllegalStateException` (không phải custom exception tuần này)? Từ tuần nào trở đi mới nên dùng `AuctionClosedException`?

---

### Bài 0.4 — Custom Exception Hierarchy

**Tài liệu bắt buộc đọc:**
- https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html
- Đọc JavaDoc của `RuntimeException` vs `Exception` trong JDK

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. `RuntimeException` (unchecked) vs `Exception` (checked) — khác nhau ở đâu? Tại sao BidHub dùng `RuntimeException`?
2. Tại sao cần `BidHubException` chung thay vì ném thẳng `RuntimeException`? Lợi ích ở đâu?
3. `errorCode` trong `BidHubException` dùng để làm gì? Khi nào phía client cần nó?
4. `ValidationException` có thêm `List<String> errors` — tại sao cần list thay vì 1 message?
5. Khi `catch (BidHubException e)` — bạn bắt được những exception nào? Giải thích tính đa hình trong exception.
6. **[Câu hỏi nâng cao]** `getCause()` trong Exception là gì? Khi nào nên wrap exception gốc?

---

## 🔨 PHẦN CÁ NHÂN — NHIỆM VỤ RIÊNG

> [!IMPORTANT]
> Mỗi người code trên **branch riêng**, KHÔNG push thẳng vào `main` hay `develop`. Đến Chủ nhật mới tạo PR để review và merge.
>
> **Phụ thuộc tuần này (rất quan trọng):**
> - **Đăng phải push trước** (Thứ 3 14/04): vì `Entity.java` là base class mà Quốc Minh và Công Minh đều kế thừa. Nếu Đăng chậm → cả nhóm bị block.
> - Khoa có thể làm song song vì `BidHubException` không phụ thuộc ai.
> - Quốc Minh và Công Minh bắt đầu sau khi Đăng push Entity (hoặc tự copy tạm vào branch của mình để không bị block).

---

## 👤 ĐĂNG — Module `bidhub-common` + Entity Base + User Hierarchy

```
Branch: feature/tuan-2-dang-entity-user
Phụ thuộc: Kế thừa parent pom.xml từ branch feature/tuan-1-dang-maven-setup (đã merge)
Deadline: Thứ 3 14/04 (PHẢI push sớm để cả nhóm không bị block)
```

📌 **[Tiêu chí điểm: Thiết kế lớp & cây kế thừa — 0.5đ] + [OOP: Encapsulation + Inheritance + Abstraction — 1.0đ]**

### 📝 Mô tả bài tập

Bạn là người tạo **nền tảng domain** cho toàn bộ hệ thống. Task tuần này gồm 2 việc:
1. **Thêm module `bidhub-common`** vào parent POM — module này chứa các class dùng chung giữa server và client.
2. **Tạo cây kế thừa User** hoàn chỉnh: `Entity (abstract) → User (abstract) → Bidder / Seller / Admin`.

### 📁 Cấu trúc file cần tạo

```
BidHub/
├── pom.xml                                ← CẬP NHẬT: thêm module bidhub-common
│
├── bidhub-common/                         ← MỚI: module dùng chung cho server + client
│   ├── pom.xml                            ← Module POM, không có dependency nặng
│   └── src/
│       ├── main/java/com/bidhub/common/
│       │   └── model/
│       │       └── Entity.java            ← Abstract base class: id, timestamps, equals/hashCode
│       └── test/java/com/bidhub/common/
│           └── model/
│               └── EntityTest.java        ← Test Entity qua anonymous subclass
│
└── bidhub-server/
    ├── pom.xml                            ← CẬP NHẬT: thêm dependency bidhub-common
    └── src/
        ├── main/java/com/bidhub/server/
        │   └── model/                     ← MỚI: package chứa User hierarchy
        │       ├── UserRole.java          ← Enum: BIDDER, SELLER, ADMIN
        │       ├── User.java              ← Abstract: username, passwordHash, email, role
        │       ├── Bidder.java            ← Concrete: thêm totalBidsPlaced
        │       ├── Seller.java            ← Concrete: thêm totalItemsListed
        │       └── Admin.java             ← Concrete: thêm adminLevel
        └── test/java/com/bidhub/server/
            └── model/
                └── UserHierarchyTest.java ← Test polymorphism, encapsulation, UUID
```

### 📋 Yêu cầu chi tiết

#### Bước 1 — Cập nhật Parent `pom.xml` (thêm `bidhub-common`)

```xml
<!-- BidHub/pom.xml — chỉ thêm module bidhub-common, giữ nguyên phần còn lại -->
<modules>
  <module>bidhub-common</module>   <!-- THÊM DÒNG NÀY — phải đứng TRƯỚC server và client -->
  <module>bidhub-server</module>
  <module>bidhub-client</module>
</modules>
```

> 💡 **Tại sao `bidhub-common` phải đứng trước?** Maven build theo thứ tự khai báo. Nếu `bidhub-server` khai báo phụ thuộc vào `bidhub-common` mà `bidhub-common` chưa được build → lỗi `Could not resolve artifact`. Thứ tự: common → server → client.

```bash
git add pom.xml
git commit -m "build: thêm module bidhub-common vào parent pom"
```

---

#### Bước 2 — `bidhub-common/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.bidhub</groupId>
    <artifactId>bidhub-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>

  <artifactId>bidhub-common</artifactId>
  <name>BidHub Common</name>
  <description>Các class dùng chung giữa server và client: Entity, Exception hierarchy</description>

  <!--
    Module này CỐ Ý không có dependency nặng nào (không Jackson, không SQLite).
    Chỉ dùng Java standard library.
    Lý do: client cần import module này, nếu kéo theo SQLite → client bị thừa dependency.
  -->
  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

```bash
git add bidhub-common/pom.xml
git commit -m "build: khởi tạo module bidhub-common với pom không dependency nặng"
```

---

#### Bước 3 — `Entity.java`

```java
package com.bidhub.common.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Lớp cơ sở trừu tượng cho tất cả các thực thể trong hệ thống BidHub.
 *
 * <p>Mỗi entity trong hệ thống ({@code User}, {@code Item}, {@code Auction},
 * {@code BidTransaction}) đều kế thừa lớp này để đảm bảo tính nhất quán:
 * mỗi object đều có ID duy nhất và timestamp tạo/cập nhật.
 *
 * <p><b>Tại sao abstract?</b> Entity không thể tồn tại độc lập — không ai
 * "tạo một Entity chung chung". Chỉ có thể tạo {@code Bidder}, {@code Auction}, v.v.
 * Dùng {@code abstract} để JVM ngăn chặn {@code new Entity()} trực tiếp.
 *
 * <p><b>equals/hashCode dựa trên id:</b> 2 entity cùng id = cùng 1 thực thể
 * trong domain, dù là 2 object khác nhau trong bộ nhớ. Quan trọng khi
 * dùng trong {@code Set}, {@code Map} hoặc so sánh sau khi load từ DB.
 *
 * <p>Ví dụ:
 * <pre>{@code
 * // Entity là abstract — không thể new trực tiếp
 * // Entity e = new Entity(); // ← compile error
 *
 * // Phải dùng subclass
 * Bidder bidder = new Bidder("alice", "hash", "alice@mail.com");
 * System.out.println(bidder.getId()); // → UUID như "550e8400-e29b-41d4..."
 * }</pre>
 */
public abstract class Entity {

  /** ID duy nhất, dạng UUID string. Bất biến sau khi tạo. */
  private final String id;

  /** Thời điểm tạo entity. Bất biến sau khi tạo. */
  private final LocalDateTime createdAt;

  /** Thời điểm cập nhật gần nhất. Thay đổi khi gọi {@link #markUpdated()}. */
  private LocalDateTime updatedAt;

  /**
   * Constructor duy nhất — tự động gán UUID và timestamp khi tạo entity mới.
   *
   * <p>Gọi bởi constructor của subclass qua {@code super()}.
   */
  protected Entity() {
    this.id = UUID.randomUUID().toString();
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  /**
   * Constructor dùng khi load entity từ database (id và timestamps đã có sẵn).
   *
   * <p>Quan trọng: khi đọc từ DB, phải dùng constructor này để giữ nguyên id gốc,
   * không tạo UUID mới.
   *
   * @param id        id đã tồn tại (từ DB)
   * @param createdAt thời điểm tạo gốc
   * @param updatedAt thời điểm cập nhật gần nhất
   */
  protected Entity(String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
    Objects.requireNonNull(id, "id không được null");
    Objects.requireNonNull(createdAt, "createdAt không được null");
    Objects.requireNonNull(updatedAt, "updatedAt không được null");
    this.id = id;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /**
   * Cập nhật {@code updatedAt} về thời điểm hiện tại.
   *
   * <p>Gọi trong subclass bất cứ khi nào field quan trọng thay đổi.
   * Ví dụ: {@code Auction.updateHighestBid()} nên gọi {@code markUpdated()}.
   */
  protected final void markUpdated() {
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Trả về ID duy nhất của entity.
   *
   * @return chuỗi UUID, không bao giờ null
   */
  public final String getId() {
    return id;
  }

  /**
   * Trả về thời điểm tạo entity.
   *
   * @return {@code LocalDateTime} thời điểm tạo, không bao giờ null
   */
  public final LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Trả về thời điểm cập nhật gần nhất.
   *
   * @return {@code LocalDateTime} thời điểm cập nhật, không bao giờ null
   */
  public final LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * So sánh 2 entity dựa trên {@code id}.
   *
   * <p>2 entity cùng id = cùng 1 thực thể trong hệ thống, bất kể class hay trạng thái.
   *
   * @param o object cần so sánh
   * @return {@code true} nếu cùng class và cùng id
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Entity other)) {
      return false;
    }
    return Objects.equals(id, other.id);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  /**
   * Trả về biểu diễn chuỗi gồm class name và id (7 ký tự đầu của UUID).
   *
   * <p>Dùng để debug. Subclass nên override để thêm thông tin có nghĩa hơn.
   *
   * @return chuỗi dạng {@code "Bidder[550e840]"}
   */
  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + id.substring(0, 7) + "]";
  }
}
```

```bash
git add bidhub-common/src/
git commit -m "feat: thêm lớp abstract Entity với UUID, timestamps và equals/hashCode"
```

---

#### Bước 4 — `UserRole.java`

```java
package com.bidhub.server.model;

/**
 * Vai trò của người dùng trong hệ thống BidHub.
 *
 * <p>Mỗi {@link User} có đúng 1 vai trò, xác định quyền thực hiện thao tác:
 * <ul>
 *   <li>{@link #BIDDER} — đặt giá, xem đấu giá</li>
 *   <li>{@link #SELLER} — tạo sản phẩm, tạo phiên đấu giá</li>
 *   <li>{@link #ADMIN} — quản lý toàn hệ thống</li>
 * </ul>
 *
 * <p>Enum được lưu vào database dưới dạng chuỗi (tên enum).
 * Đọc lại: {@code UserRole.valueOf(dbString)}.
 */
public enum UserRole {

  /** Người đặt giá — chỉ xem và đặt, không tạo sản phẩm. */
  BIDDER("Người đặt giá"),

  /** Người bán — tạo sản phẩm và khởi tạo phiên đấu giá. */
  SELLER("Người bán"),

  /** Quản trị viên — toàn quyền. */
  ADMIN("Quản trị viên");

  /** Tên hiển thị tiếng Việt — dùng trên UI và log. */
  private final String displayName;

  UserRole(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Trả về tên hiển thị tiếng Việt của vai trò.
   *
   * @return chuỗi tên hiển thị (ví dụ: {@code "Người đặt giá"})
   */
  public String getDisplayName() {
    return displayName;
  }
}
```

---

#### Bước 5 — `User.java` (Abstract)

```java
package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lớp trừu tượng đại diện cho người dùng trong hệ thống BidHub.
 *
 * <p>Mọi người dùng đều có: {@code username}, {@code passwordHash},
 * {@code email}, và {@code role}. Subclass cụ thể ({@link Bidder},
 * {@link Seller}, {@link Admin}) thêm các field riêng của từng loại.
 *
 * <p><b>Tại sao passwordHash thay vì password?</b> Mật khẩu KHÔNG BAO GIỜ
 * được lưu dưới dạng plaintext. Tuần 5 sẽ implement SHA-256 hashing.
 * Ở tuần 2, truyền vào chuỗi bất kỳ (stub) để test.
 *
 * <p><b>Encapsulation:</b> Tất cả field đều {@code private}, chỉ expose qua
 * getter. Setter chỉ cung cấp cho field thực sự cần thay đổi.
 */
public abstract class User extends Entity {

  /** Tên đăng nhập — duy nhất trong hệ thống, không đổi sau khi tạo. */
  private final String username;

  /** Hash mật khẩu (SHA-256). Không lưu plaintext. */
  private String passwordHash;

  /** Địa chỉ email. */
  private String email;

  /** Vai trò người dùng — xác định quyền hạn trong hệ thống. */
  private final UserRole role;

  /**
   * Constructor tạo User mới — gọi bởi subclass qua {@code super(...)}.
   *
   * @param username     tên đăng nhập, không null, không rỗng
   * @param passwordHash hash mật khẩu, không null
   * @param email        địa chỉ email, không null
   * @param role         vai trò người dùng, không null
   * @throws IllegalArgumentException nếu username hoặc email rỗng/null
   */
  protected User(String username, String passwordHash, String email, UserRole role) {
    super(); // gọi Entity() để tạo UUID và timestamps
    validateUsername(username);
    Objects.requireNonNull(passwordHash, "passwordHash không được null");
    Objects.requireNonNull(email, "email không được null");
    Objects.requireNonNull(role, "role không được null");
    this.username = username;
    this.passwordHash = passwordHash;
    this.email = email;
    this.role = role;
  }

  /**
   * Constructor dùng khi load User từ database (id và timestamps đã có).
   *
   * @param id           id từ DB
   * @param createdAt    thời điểm tạo từ DB
   * @param updatedAt    thời điểm cập nhật từ DB
   * @param username     tên đăng nhập
   * @param passwordHash hash mật khẩu
   * @param email        email
   * @param role         vai trò
   */
  protected User(
      String id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String username,
      String passwordHash,
      String email,
      UserRole role) {
    super(id, createdAt, updatedAt);
    validateUsername(username);
    this.username = username;
    this.passwordHash = passwordHash;
    this.email = email;
    this.role = role;
  }

  /**
   * Validate username: không null, không rỗng, độ dài 3-50 ký tự.
   *
   * @param username giá trị cần kiểm tra
   * @throws IllegalArgumentException nếu vi phạm
   */
  private static void validateUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username không được null hoặc rỗng");
    }
    if (username.length() < 3 || username.length() > 50) {
      throw new IllegalArgumentException(
          "Username phải có độ dài từ 3 đến 50 ký tự, hiện tại: " + username.length());
    }
  }

  /**
   * Trả về thông tin mô tả ngắn gọn về người dùng này.
   *
   * <p>Mỗi subclass override để trả về thông tin đặc trưng của mình —
   * đây là demo tính <b>Polymorphism</b> của OOP.
   *
   * @return chuỗi mô tả không null
   */
  public abstract String getInfo();

  // =========================================================================
  // Getters — các field đều private, chỉ expose qua getter (Encapsulation)
  // =========================================================================

  /**
   * Trả về tên đăng nhập.
   *
   * @return username, không bao giờ null
   */
  public String getUsername() {
    return username;
  }

  /**
   * Trả về hash mật khẩu.
   *
   * @return passwordHash, không bao giờ null
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Trả về địa chỉ email.
   *
   * @return email, không bao giờ null
   */
  public String getEmail() {
    return email;
  }

  /**
   * Trả về vai trò người dùng.
   *
   * @return {@link UserRole}, không bao giờ null
   */
  public UserRole getRole() {
    return role;
  }

  /**
   * Cập nhật hash mật khẩu (khi người dùng đổi mật khẩu).
   *
   * @param newPasswordHash hash mới, không null
   */
  public void setPasswordHash(String newPasswordHash) {
    Objects.requireNonNull(newPasswordHash, "passwordHash mới không được null");
    this.passwordHash = newPasswordHash;
    markUpdated();
  }

  /**
   * Cập nhật địa chỉ email.
   *
   * @param newEmail email mới, không null
   */
  public void setEmail(String newEmail) {
    Objects.requireNonNull(newEmail, "email mới không được null");
    this.email = newEmail;
    markUpdated();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "[id=" + getId().substring(0, 7)
        + ", username=" + username
        + ", role=" + role.name() + "]";
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/model/
git commit -m "feat: thêm abstract class User kế thừa Entity với encapsulation đầy đủ"
```

---

#### Bước 6 — `Bidder.java`, `Seller.java`, `Admin.java`

```java
package com.bidhub.server.model;

import java.time.LocalDateTime;

/**
 * Người đặt giá trong hệ thống BidHub.
 *
 * <p>Kế thừa {@link User}, thêm field {@code totalBidsPlaced} theo dõi
 * tổng số lần đã đặt giá. Field này được cập nhật mỗi khi đặt giá thành công.
 *
 * <p>Override {@link #getInfo()} để thể hiện tính <b>Polymorphism</b>:
 * cùng lời gọi {@code user.getInfo()}, mỗi loại user trả về thông tin khác nhau.
 */
public final class Bidder extends User {

  /** Tổng số lần đã đặt giá — dùng cho thống kê. */
  private int totalBidsPlaced;

  /**
   * Tạo Bidder mới với totalBidsPlaced = 0.
   *
   * @param username     tên đăng nhập
   * @param passwordHash hash mật khẩu
   * @param email        địa chỉ email
   */
  public Bidder(String username, String passwordHash, String email) {
    super(username, passwordHash, email, UserRole.BIDDER);
    this.totalBidsPlaced = 0;
  }

  /**
   * Constructor load từ database.
   *
   * @param id              id từ DB
   * @param createdAt       thời điểm tạo từ DB
   * @param updatedAt       thời điểm cập nhật từ DB
   * @param username        tên đăng nhập
   * @param passwordHash    hash mật khẩu
   * @param email           email
   * @param totalBidsPlaced tổng số lần đặt giá
   */
  public Bidder(
      String id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String username,
      String passwordHash,
      String email,
      int totalBidsPlaced) {
    super(id, createdAt, updatedAt, username, passwordHash, email, UserRole.BIDDER);
    this.totalBidsPlaced = Math.max(0, totalBidsPlaced);
  }

  /**
   * Tăng tổng số lần đặt giá lên 1.
   *
   * <p>Gọi mỗi khi Bidder đặt giá thành công (từ Tuần 6).
   */
  public void incrementBidsPlaced() {
    this.totalBidsPlaced++;
    markUpdated();
  }

  /**
   * Trả về tổng số lần đã đặt giá.
   *
   * @return số lần đặt giá (≥ 0)
   */
  public int getTotalBidsPlaced() {
    return totalBidsPlaced;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Bidder trả về thông tin: username, vai trò, tổng bids.
   */
  @Override
  public String getInfo() {
    return "Người đặt giá: " + getUsername()
        + " | Tổng lần đặt giá: " + totalBidsPlaced;
  }
}
```

```java
package com.bidhub.server.model;

import java.time.LocalDateTime;

/**
 * Người bán hàng trong hệ thống BidHub.
 *
 * <p>Kế thừa {@link User}, thêm field {@code totalItemsListed} theo dõi
 * tổng số sản phẩm đã đăng bán.
 */
public final class Seller extends User {

  /** Tổng số sản phẩm đã đăng bán — dùng cho thống kê. */
  private int totalItemsListed;

  /**
   * Tạo Seller mới với totalItemsListed = 0.
   *
   * @param username     tên đăng nhập
   * @param passwordHash hash mật khẩu
   * @param email        địa chỉ email
   */
  public Seller(String username, String passwordHash, String email) {
    super(username, passwordHash, email, UserRole.SELLER);
    this.totalItemsListed = 0;
  }

  /**
   * Constructor load từ database.
   *
   * @param id               id từ DB
   * @param createdAt        thời điểm tạo
   * @param updatedAt        thời điểm cập nhật
   * @param username         tên đăng nhập
   * @param passwordHash     hash mật khẩu
   * @param email            email
   * @param totalItemsListed tổng sản phẩm đã đăng
   */
  public Seller(
      String id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String username,
      String passwordHash,
      String email,
      int totalItemsListed) {
    super(id, createdAt, updatedAt, username, passwordHash, email, UserRole.SELLER);
    this.totalItemsListed = Math.max(0, totalItemsListed);
  }

  /**
   * Tăng tổng sản phẩm đã đăng lên 1.
   *
   * <p>Gọi khi Seller tạo thêm một sản phẩm mới (từ Tuần 5).
   */
  public void incrementItemsListed() {
    this.totalItemsListed++;
    markUpdated();
  }

  /**
   * Trả về tổng số sản phẩm đã đăng bán.
   *
   * @return số sản phẩm (≥ 0)
   */
  public int getTotalItemsListed() {
    return totalItemsListed;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Seller trả về thông tin: username, vai trò, tổng sản phẩm.
   */
  @Override
  public String getInfo() {
    return "Người bán: " + getUsername()
        + " | Tổng sản phẩm đã đăng: " + totalItemsListed;
  }
}
```

```java
package com.bidhub.server.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Quản trị viên của hệ thống BidHub.
 *
 * <p>Kế thừa {@link User}, thêm {@code adminLevel} (cấp độ quản trị: 1-3).
 * Level 1: admin thường. Level 3: superadmin (quyền cao nhất).
 */
public final class Admin extends User {

  /** Cấp độ quản trị. Giá trị hợp lệ: 1, 2, hoặc 3. */
  private int adminLevel;

  /**
   * Tạo Admin mới.
   *
   * @param username     tên đăng nhập
   * @param passwordHash hash mật khẩu
   * @param email        địa chỉ email
   * @param adminLevel   cấp độ (1-3)
   * @throws IllegalArgumentException nếu adminLevel không hợp lệ
   */
  public Admin(String username, String passwordHash, String email, int adminLevel) {
    super(username, passwordHash, email, UserRole.ADMIN);
    this.adminLevel = validateAdminLevel(adminLevel);
  }

  /**
   * Constructor load từ database.
   *
   * @param id           id từ DB
   * @param createdAt    thời điểm tạo
   * @param updatedAt    thời điểm cập nhật
   * @param username     tên đăng nhập
   * @param passwordHash hash mật khẩu
   * @param email        email
   * @param adminLevel   cấp độ quản trị
   */
  public Admin(
      String id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String username,
      String passwordHash,
      String email,
      int adminLevel) {
    super(id, createdAt, updatedAt, username, passwordHash, email, UserRole.ADMIN);
    this.adminLevel = validateAdminLevel(adminLevel);
  }

  /**
   * Validate admin level phải trong khoảng [1, 3].
   *
   * @param level giá trị cần kiểm tra
   * @return level nếu hợp lệ
   * @throws IllegalArgumentException nếu level ngoài khoảng cho phép
   */
  private static int validateAdminLevel(int level) {
    if (level < 1 || level > 3) {
      throw new IllegalArgumentException(
          "adminLevel phải trong khoảng [1, 3], nhận được: " + level);
    }
    return level;
  }

  /**
   * Trả về cấp độ quản trị.
   *
   * @return giá trị từ 1 đến 3
   */
  public int getAdminLevel() {
    return adminLevel;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Admin trả về thông tin: username, vai trò, cấp độ quản trị.
   */
  @Override
  public String getInfo() {
    return "Quản trị viên: " + getUsername()
        + " | Cấp độ: " + adminLevel;
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/model/
git commit -m "feat: thêm Bidder, Seller, Admin kế thừa User với polymorphism getInfo()"
```

---

#### Bước 7 — Cập nhật `bidhub-server/pom.xml` để phụ thuộc vào `bidhub-common`

```xml
<!-- Thêm vào <dependencies> trong bidhub-server/pom.xml -->
<dependency>
  <groupId>com.bidhub</groupId>
  <artifactId>bidhub-common</artifactId>
  <version>${project.version}</version>
</dependency>
```

```bash
git add bidhub-server/pom.xml
git commit -m "build: bidhub-server phụ thuộc bidhub-common để dùng Entity"
```

---

#### Bước 8 — `UserHierarchyTest.java`

📌 **[Tiêu chí điểm: OOP — Polymorphism + Encapsulation + Inheritance — 1.0đ]**

```java
package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử cây kế thừa User: Bidder, Seller, Admin.
 *
 * <p>Các test này trực tiếp chứng minh 4 trụ cột OOP:
 * <ul>
 *   <li>Encapsulation: field private, getter/setter</li>
 *   <li>Inheritance: Bidder is-a User is-a Entity</li>
 *   <li>Polymorphism: getInfo() khác nhau mỗi subclass</li>
 *   <li>Abstraction: không thể new User() trực tiếp</li>
 * </ul>
 */
@DisplayName("UserHierarchy — Kiểm thử cây kế thừa User")
class UserHierarchyTest {

  // =========================================================================
  // Test Entity base (qua Bidder vì Entity là abstract)
  // =========================================================================

  @Nested
  @DisplayName("Entity — ID và timestamps")
  class EntityTests {

    @Test
    @DisplayName("Hai entity mới tạo phải có ID khác nhau (UUID không trùng)")
    void testEntity_TwoNewInstances_HaveDifferentIds() {
      // Arrange + Act
      Bidder b1 = new Bidder("user1", "hash1", "u1@mail.com");
      Bidder b2 = new Bidder("user2", "hash2", "u2@mail.com");

      // Assert
      assertNotNull(b1.getId(), "ID không được null");
      assertNotNull(b2.getId(), "ID không được null");
      assertNotEquals(b1.getId(), b2.getId(), "Hai entity mới phải có UUID khác nhau");
    }

    @Test
    @DisplayName("Entity có ID thì equals/hashCode nhất quán theo id")
    void testEntity_SameId_EqualsByContract() {
      // Arrange
      Bidder b1 = new Bidder("alice", "hash", "a@mail.com");

      // Tạo Bidder với cùng id (load từ DB)
      Bidder b2 = new Bidder(
          b1.getId(), b1.getCreatedAt(), b1.getUpdatedAt(),
          "alice", "hash", "a@mail.com", 0);

      // Assert — cùng id → equals và hashCode phải khớp
      assertEquals(b1, b2, "Entity cùng id phải equals()");
      assertEquals(b1.hashCode(), b2.hashCode(), "Entity cùng id phải cùng hashCode()");
    }

    @Test
    @DisplayName("createdAt không null sau khi tạo entity mới")
    void testEntity_CreatedAt_NotNullOnCreation() {
      // Arrange + Act
      Seller seller = new Seller("bob", "hash", "bob@mail.com");

      // Assert
      assertNotNull(seller.getCreatedAt(), "createdAt không được null");
      assertNotNull(seller.getUpdatedAt(), "updatedAt không được null");
    }
  }

  // =========================================================================
  // Test Inheritance: Bidder is-a User is-a Entity
  // =========================================================================

  @Nested
  @DisplayName("Inheritance — Bidder is-a User is-a Entity")
  class InheritanceTests {

    @Test
    @DisplayName("Bidder instanceof User và instanceof Entity (kiểm tra kế thừa)")
    void testBidder_IsInstanceOf_UserAndEntity() {
      // Arrange + Act
      Bidder bidder = new Bidder("charlie", "hash", "c@mail.com");

      // Assert
      assertInstanceOf(User.class, bidder, "Bidder phải là User");
      assertInstanceOf(Entity.class, bidder, "Bidder phải là Entity");
    }

    @Test
    @DisplayName("Bidder.getRole() trả về BIDDER")
    void testBidder_GetRole_ReturnsBidder() {
      // Arrange + Act
      Bidder bidder = new Bidder("dave", "hash", "d@mail.com");

      // Assert
      assertEquals(UserRole.BIDDER, bidder.getRole());
    }

    @Test
    @DisplayName("Seller.getRole() trả về SELLER")
    void testSeller_GetRole_ReturnsSeller() {
      Seller seller = new Seller("eve", "hash", "e@mail.com");
      assertEquals(UserRole.SELLER, seller.getRole());
    }

    @Test
    @DisplayName("Admin.getRole() trả về ADMIN")
    void testAdmin_GetRole_ReturnsAdmin() {
      Admin admin = new Admin("frank", "hash", "f@mail.com", 1);
      assertEquals(UserRole.ADMIN, admin.getRole());
    }
  }

  // =========================================================================
  // Test Polymorphism: getInfo() khác nhau mỗi subclass
  // =========================================================================

  @Nested
  @DisplayName("Polymorphism — getInfo() hành vi khác nhau")
  class PolymorphismTests {

    @Test
    @DisplayName("List<User> gọi getInfo() → mỗi phần tử trả về nội dung khác nhau")
    void testPolymorphism_GetInfo_DifferentOutputPerSubclass() {
      // Arrange
      List<User> users = List.of(
          new Bidder("alice", "h", "a@x.com"),
          new Seller("bob", "h", "b@x.com"),
          new Admin("carol", "h", "c@x.com", 2));

      // Act
      List<String> infos = users.stream().map(User::getInfo).toList();

      // Assert — mỗi info phải khác nhau
      assertNotEquals(infos.get(0), infos.get(1), "Bidder và Seller info phải khác nhau");
      assertNotEquals(infos.get(1), infos.get(2), "Seller và Admin info phải khác nhau");
      // Kiểm tra nội dung có ý nghĩa
      assertTrue(infos.get(0).contains("Người đặt giá"), "Bidder getInfo phải đề cập vai trò");
      assertTrue(infos.get(1).contains("Người bán"), "Seller getInfo phải đề cập vai trò");
      assertTrue(infos.get(2).contains("Quản trị viên"), "Admin getInfo phải đề cập vai trò");
    }
  }

  // =========================================================================
  // Test Encapsulation: validate input, không thể set trực tiếp
  // =========================================================================

  @Nested
  @DisplayName("Encapsulation & Validation")
  class EncapsulationTests {

    @Test
    @DisplayName("Username null → IllegalArgumentException")
    void testUser_NullUsername_ThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Bidder(null, "hash", "x@mail.com"),
          "Username null phải ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Username quá ngắn (< 3 ký tự) → IllegalArgumentException")
    void testUser_ShortUsername_ThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Bidder("ab", "hash", "x@mail.com"),
          "Username < 3 ký tự phải ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Admin với adminLevel = 0 → IllegalArgumentException")
    void testAdmin_InvalidLevel_ThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Admin("admin", "hash", "a@mail.com", 0),
          "adminLevel = 0 phải ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Admin với adminLevel = 4 → IllegalArgumentException")
    void testAdmin_LevelTooHigh_ThrowsException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Admin("admin", "hash", "a@mail.com", 4),
          "adminLevel = 4 phải ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Bidder.incrementBidsPlaced() tăng đúng 1 mỗi lần gọi")
    void testBidder_IncrementBids_IncreasesCountByOne() {
      // Arrange
      Bidder bidder = new Bidder("helen", "hash", "h@mail.com");

      // Act
      bidder.incrementBidsPlaced();
      bidder.incrementBidsPlaced();

      // Assert
      assertEquals(2, bidder.getTotalBidsPlaced(), "Sau 2 lần increment phải bằng 2");
    }
  }
}
```

```bash
git add bidhub-server/src/test/
git commit -m "test: thêm UserHierarchyTest kiểm tra Entity, Inheritance, Polymorphism, Encapsulation"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Đăng

```bash
# Test 1: Build toàn bộ project (bidhub-common → bidhub-server → bidhub-client)
mvn compile
# ✅ PASS: "[INFO] BUILD SUCCESS" — cả 3 modules
# ❌ FAIL: "[ERROR] Could not resolve artifact com.bidhub:bidhub-common" → bidhub-common chưa đúng thứ tự trong parent pom

# Test 2: Chạy test User hierarchy
mvn test -pl bidhub-common,bidhub-server
# ✅ PASS: "Tests run: 10+, Failures: 0, Errors: 0"
# ❌ FAIL: Failures > 0

# Test 3: Kiểm tra Bidder instanceof Entity qua quick main (manual test)
# Thêm tạm vào ServerApp.main() hoặc tạo file test nhanh:
# Bidder b = new Bidder("test", "hash", "t@mail.com");
# System.out.println(b instanceof Entity); // phải in: true
# System.out.println(b.getInfo());         // phải in: "Người đặt giá: test | Tổng lần đặt giá: 0"

# Test 4: Kiểm tra UUID khác nhau
# Bidder b1 = new Bidder("u1","h","e1@x.com"), b2 = new Bidder("u2","h","e2@x.com");
# System.out.println(b1.getId().equals(b2.getId())); // phải in: false

# Test 5: Đếm test cases
grep -r "@Test" bidhub-server/src/test/java/com/bidhub/server/model/ | wc -l
# ✅ PASS: ≥ 10

# Test 6: Kiểm tra bidhub-common không kéo theo dependency nặng
mvn dependency:tree -pl bidhub-common | grep -E "jackson|sqlite"
# ✅ PASS: không có output (bidhub-common không có jackson/sqlite)
# ❌ FAIL: xuất hiện jackson hoặc sqlite → dependency bị rò rỉ
```

### ❌ FAIL nếu:
- `mvn compile` lỗi `"Could not resolve artifact bidhub-common"` → thứ tự module trong parent pom sai
- `b1.getId().equals(b2.getId())` = `true` → Entity không dùng `UUID.randomUUID()`
- `new Bidder(null, "hash", "e@x.com")` không ném exception → thiếu validation
- `new Admin("x", "h", "e@x.com", 0)` không ném exception → thiếu validate adminLevel
- `bidder.getInfo()` và `seller.getInfo()` trả về cùng chuỗi → chưa override đúng (polymorphism chưa có)
- `bidhub-common/pom.xml` khai báo `sqlite-jdbc` dependency → vi phạm nguyên tắc module dùng chung

---

## 👤 QUỐC MINH — Item Hierarchy & Factory Method Pattern

```
Branch: feature/tuan-2-quocminh-item-factory
Phụ thuộc: Cần Entity.java từ branch của Đăng (pull sau khi Đăng push Thứ 3)
           Nếu Đăng chưa push → tự copy Entity.java vào branch của mình, replace sau
```

📌 **[Tiêu chí điểm: Design Patterns — Factory Method chuẩn GoF (1.0đ)] + [OOP — Abstraction + Polymorphism (1.0đ)]**

### 📝 Mô tả bài tập

Xây dựng cây kế thừa `Item` (Product hierarchy) và áp dụng **Factory Method Pattern chuẩn GoF** với Creator hierarchy. Thay vì dùng Static Factory (`ItemFactory` utility class), ta tạo `ItemCreator` abstract class và các ConcreteCreator riêng — đây là cách GoF định nghĩa đúng Factory Method Pattern.

### 📁 Cấu trúc file cần tạo

```
bidhub-server/
└── src/
    ├── main/java/com/bidhub/server/model/
    │   ├── Displayable.java            ← Interface: printInfo()
    │   ├── ItemType.java               ← Enum: ELECTRONICS, ART, VEHICLE với getLabel()
    │   ├── Item.java                   ← Abstract Product: kế thừa Entity + Displayable
    │   ├── Electronics.java            ← ConcreteProduct: brand, warrantyMonths
    │   ├── Art.java                    ← ConcreteProduct: artist, yearCreated
    │   ├── Vehicle.java                ← ConcreteProduct: manufacturer, year, mileageKm
    │   ├── ItemCreator.java            ← Abstract Creator: factory method createItem() + forType()
    │   ├── ElectronicsCreator.java     ← ConcreteCreator: tạo Electronics
    │   ├── ArtCreator.java             ← ConcreteCreator: tạo Art
    │   └── VehicleCreator.java         ← ConcreteCreator: tạo Vehicle
    └── test/java/com/bidhub/server/model/
        └── ItemCreatorTest.java        ← Test Factory Method Pattern, polymorphism, validation
```

> ⚠️ **Không có `ItemFactory.java`** — đã thay hoàn toàn bằng Creator hierarchy.

### 📋 Yêu cầu chi tiết

#### Bước 1 — `Displayable.java` (Interface)

```java
package com.bidhub.server.model;

/**
 * Interface đánh dấu các đối tượng có thể hiển thị thông tin ra màn hình.
 *
 * <p>Áp dụng cho tất cả {@link Item} subclasses. Đây là ví dụ về
 * <b>Abstraction</b>: client code chỉ cần biết đối tượng "có thể in info",
 * không cần biết cụ thể là Electronics hay Art.
 *
 * <p>Ví dụ:
 * <pre>{@code
 * List<Displayable> items = List.of(new Electronics(...), new Art(...));
 * items.forEach(Displayable::printInfo);
 * }</pre>
 */
public interface Displayable {

  /**
   * In thông tin chi tiết của đối tượng ra {@code System.out}.
   *
   * <p>Mỗi class implement theo cách riêng, thể hiện polymorphism qua interface.
   */
  void printInfo();
}
```

---

#### Bước 2 — `ItemType.java` (Enum)

```java
package com.bidhub.server.model;

/**
 * Loại sản phẩm trong hệ thống BidHub.
 *
 * <p>Mỗi loại tương ứng với một ConcreteCreator và ConcreteProduct:
 * <ul>
 *   <li>{@link #ELECTRONICS} → {@link ElectronicsCreator} → {@link Electronics}</li>
 *   <li>{@link #ART} → {@link ArtCreator} → {@link Art}</li>
 *   <li>{@link #VEHICLE} → {@link VehicleCreator} → {@link Vehicle}</li>
 * </ul>
 *
 * <p>Enum được lưu vào cột {@code item_type} trong database dưới dạng tên enum.
 */
public enum ItemType {

  /** Hàng điện tử: laptop, điện thoại, thiết bị gia dụng. */
  ELECTRONICS("Đồ điện tử"),

  /** Tác phẩm nghệ thuật: tranh, tượng, thủ công mỹ nghệ. */
  ART("Tác phẩm nghệ thuật"),

  /** Phương tiện giao thông: ô tô, xe máy. */
  VEHICLE("Phương tiện");

  /** Tên hiển thị tiếng Việt dùng trên UI. */
  private final String label;

  ItemType(String label) {
    this.label = label;
  }

  /**
   * Trả về tên hiển thị tiếng Việt của loại sản phẩm.
   *
   * @return chuỗi nhãn (ví dụ: {@code "Đồ điện tử"})
   */
  public String getLabel() {
    return label;
  }
}
```

---

#### Bước 3 — `Item.java` (Abstract Product)

```java
package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lớp trừu tượng đại diện cho sản phẩm đấu giá trong BidHub.
 *
 * <p>Kế thừa {@link Entity} (để có id, timestamps) và implement
 * {@link Displayable} (để có thể in thông tin). Subclass cụ thể thêm
 * field đặc thù của từng danh mục.
 *
 * <p><b>Vai trò trong Factory Method Pattern:</b> đây là {@code Product} —
 * interface chung mà Creator khai báo factory method trả về.
 *
 * <p>Quan trọng: Constructor validate {@code startingPrice > 0}.
 * Sản phẩm với giá khởi điểm <= 0 không hợp lệ về mặt nghiệp vụ.
 */
public abstract class Item extends Entity implements Displayable {

  /** Tên sản phẩm — bắt buộc, không rỗng. */
  private String name;

  /** Mô tả chi tiết sản phẩm. */
  private String description;

  /**
   * Giá khởi điểm (VND) — phải > 0.
   *
   * <p>Đây là mức giá tối thiểu để bắt đầu đấu giá.
   */
  private final double startingPrice;

  /** ID của Seller đã đăng sản phẩm này. */
  private final String sellerId;

  /** Loại sản phẩm — xác định subclass cụ thể. */
  private final ItemType itemType;

  /**
   * Constructor tạo Item mới.
   *
   * @param name          tên sản phẩm, không null, không rỗng
   * @param description   mô tả sản phẩm
   * @param startingPrice giá khởi điểm, phải > 0
   * @param sellerId      id của Seller, không null
   * @param itemType      loại sản phẩm, không null
   * @throws IllegalArgumentException nếu startingPrice <= 0 hoặc name rỗng
   */
  protected Item(
      String name,
      String description,
      double startingPrice,
      String sellerId,
      ItemType itemType) {
    super();
    validateName(name);
    Objects.requireNonNull(sellerId, "sellerId không được null");
    Objects.requireNonNull(itemType, "itemType không được null");
    if (startingPrice <= 0) {
      throw new IllegalArgumentException(
          "Giá khởi điểm phải > 0, nhận được: " + startingPrice);
    }
    this.name = name;
    this.description = (description == null) ? "" : description;
    this.startingPrice = startingPrice;
    this.sellerId = sellerId;
    this.itemType = itemType;
  }

  /**
   * Constructor load từ database.
   *
   * @param id            id từ DB
   * @param createdAt     thời điểm tạo
   * @param updatedAt     thời điểm cập nhật
   * @param name          tên sản phẩm
   * @param description   mô tả
   * @param startingPrice giá khởi điểm
   * @param sellerId      id người bán
   * @param itemType      loại sản phẩm
   */
  protected Item(
      String id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String name,
      String description,
      double startingPrice,
      String sellerId,
      ItemType itemType) {
    super(id, createdAt, updatedAt);
    this.name = name;
    this.description = (description == null) ? "" : description;
    this.startingPrice = startingPrice;
    this.sellerId = sellerId;
    this.itemType = itemType;
  }

  /**
   * Trả về thông tin đặc trưng theo danh mục của sản phẩm.
   *
   * <p>Ví dụ: Electronics trả về "Thương hiệu: Apple | Bảo hành: 12 tháng".
   * Đây là abstract method — bắt buộc subclass phải implement (Abstraction).
   *
   * @return chuỗi mô tả danh mục, không null
   */
  public abstract String getCategoryDetails();

  /** Validate tên sản phẩm: không null, không rỗng. */
  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Tên sản phẩm không được null hoặc rỗng");
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>In đầy đủ thông tin sản phẩm gồm: tên, loại, giá khởi điểm,
   * và thông tin danh mục riêng từ {@link #getCategoryDetails()}.
   */
  @Override
  public void printInfo() {
    System.out.println("=== Thông tin sản phẩm ===");
    System.out.println("Tên     : " + name);
    System.out.println("Loại    : " + itemType.getLabel());
    System.out.printf("Giá KĐ  : %,.0f VND%n", startingPrice);
    System.out.println("Chi tiết: " + getCategoryDetails());
    System.out.println("Mô tả   : " + description);
  }

  // Getters
  /** Trả về tên sản phẩm. */
  public String getName() { return name; }
  /** Trả về mô tả sản phẩm. */
  public String getDescription() { return description; }
  /** Trả về giá khởi điểm. */
  public double getStartingPrice() { return startingPrice; }
  /** Trả về id người bán. */
  public String getSellerId() { return sellerId; }
  /** Trả về loại sản phẩm. */
  public ItemType getItemType() { return itemType; }

  /** Cập nhật tên sản phẩm. */
  public void setName(String name) {
    validateName(name);
    this.name = name;
    markUpdated();
  }

  /** Cập nhật mô tả sản phẩm. */
  public void setDescription(String description) {
    this.description = (description == null) ? "" : description;
    markUpdated();
  }
}
```

---

#### Bước 4 — `Electronics.java`, `Art.java`, `Vehicle.java` (ConcreteProduct — giữ nguyên)

```java
package com.bidhub.server.model;

import java.util.Objects;

/**
 * Sản phẩm điện tử (Electronics) — ConcreteProduct trong Factory Method Pattern.
 *
 * <p>Thêm 2 field đặc trưng: thương hiệu và thời gian bảo hành.
 * Được tạo bởi {@link ElectronicsCreator}.
 */
public final class Electronics extends Item {

  /** Thương hiệu (ví dụ: "Apple", "Samsung", "Sony"). */
  private final String brand;

  /** Thời gian bảo hành tính bằng tháng (0 = không bảo hành). */
  private final int warrantyMonths;

  public Electronics(
      String name, String description, double startingPrice,
      String sellerId, String brand, int warrantyMonths) {
    super(name, description, startingPrice, sellerId, ItemType.ELECTRONICS);
    Objects.requireNonNull(brand, "brand không được null");
    if (warrantyMonths < 0) {
      throw new IllegalArgumentException("warrantyMonths không được âm: " + warrantyMonths);
    }
    this.brand = brand;
    this.warrantyMonths = warrantyMonths;
  }

  @Override
  public String getCategoryDetails() {
    return "Thương hiệu: " + brand + " | Bảo hành: " + warrantyMonths + " tháng";
  }

  public String getBrand() { return brand; }
  public int getWarrantyMonths() { return warrantyMonths; }
}
```

```java
package com.bidhub.server.model;

import java.util.Objects;

/** Art — ConcreteProduct. Được tạo bởi {@link ArtCreator}. */
public final class Art extends Item {

  private final String artist;
  private final int yearCreated;

  public Art(String name, String description, double startingPrice,
             String sellerId, String artist, int yearCreated) {
    super(name, description, startingPrice, sellerId, ItemType.ART);
    Objects.requireNonNull(artist, "artist không được null");
    this.artist = artist;
    this.yearCreated = yearCreated;
  }

  @Override
  public String getCategoryDetails() {
    return "Nghệ sĩ: " + artist + " | Năm sáng tác: " + yearCreated;
  }

  public String getArtist() { return artist; }
  public int getYearCreated() { return yearCreated; }
}
```

```java
package com.bidhub.server.model;

import java.util.Objects;

/** Vehicle — ConcreteProduct. Được tạo bởi {@link VehicleCreator}. */
public final class Vehicle extends Item {

  private final String manufacturer;
  private final int year;
  private final int mileageKm;

  public Vehicle(String name, String description, double startingPrice,
                 String sellerId, String manufacturer, int year, int mileageKm) {
    super(name, description, startingPrice, sellerId, ItemType.VEHICLE);
    Objects.requireNonNull(manufacturer, "manufacturer không được null");
    if (mileageKm < 0) {
      throw new IllegalArgumentException("mileageKm không được âm: " + mileageKm);
    }
    this.manufacturer = manufacturer;
    this.year = year;
    this.mileageKm = mileageKm;
  }

  @Override
  public String getCategoryDetails() {
    return "Nhà SX: " + manufacturer + " | Năm: " + year
        + " | Km đã đi: " + String.format("%,d", mileageKm);
  }

  public String getManufacturer() { return manufacturer; }
  public int getYear() { return year; }
  public int getMileageKm() { return mileageKm; }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/model/
git commit -m "feat: thêm Item abstract và 3 ConcreteProduct Electronics, Art, Vehicle"
```

---

#### Bước 5 — Creator Hierarchy (Factory Method Pattern chuẩn GoF)

📌 **[Tiêu chí điểm: Design Patterns — Factory Method — 1.0đ]**

```java
package com.bidhub.server.model;

import java.util.Map;
import java.util.Objects;

/**
 * Abstract Creator trong Factory Method Pattern.
 *
 * <p><b>4 thành phần GoF ánh xạ vào BidHub:</b>
 * <ul>
 *   <li>Creator (abstract): {@code ItemCreator} — khai báo factory method {@link #createItem}</li>
 *   <li>ConcreteCreator: {@link ElectronicsCreator}, {@link ArtCreator}, {@link VehicleCreator}</li>
 *   <li>Product (abstract): {@link Item}</li>
 *   <li>ConcreteProduct: {@link Electronics}, {@link Art}, {@link Vehicle}</li>
 * </ul>
 *
 * <p><b>Open/Closed Principle:</b> Khi thêm {@code ItemType.JEWELRY}, chỉ tạo
 * {@code Jewelry extends Item} và {@code JewelryCreator extends ItemCreator}.
 * Không sửa {@code ElectronicsCreator}, {@code ArtCreator}, {@code VehicleCreator},
 * hay client code đang dùng {@code ItemCreator.forType()}.
 *
 * <p>Ví dụ sử dụng:
 * <pre>{@code
 * // Cách 1: dùng thẳng ConcreteCreator
 * Item laptop = new ElectronicsCreator()
 *     .createItem("MacBook Pro", "Máy tính mạnh", 25_000_000.0, "seller-id",
 *                 Map.of("brand", "Apple", "warrantyMonths", 12));
 *
 * // Cách 2: dùng static dispatcher (runtime type)
 * ItemCreator creator = ItemCreator.forType(ItemType.ELECTRONICS);
 * Item item = creator.createItem(...);
 * }</pre>
 */
public abstract class ItemCreator {

  /**
   * Factory method — subclass bắt buộc phải implement để tạo ConcreteProduct riêng.
   *
   * @param name          tên sản phẩm, không null, không rỗng
   * @param description   mô tả sản phẩm (có thể null)
   * @param startingPrice giá khởi điểm, phải > 0
   * @param sellerId      id người bán, không null
   * @param extras        map thông tin bổ sung theo loại (brand, warrantyMonths, v.v.)
   * @return instance {@link Item} cụ thể (runtime type tùy ConcreteCreator)
   * @throws IllegalArgumentException nếu dữ liệu không hợp lệ hoặc extras thiếu field
   */
  public abstract Item createItem(
      String name,
      String description,
      double startingPrice,
      String sellerId,
      Map<String, Object> extras);

  /**
   * Static dispatcher — trả về đúng ConcreteCreator tương ứng với {@link ItemType} tại runtime.
   *
   * <p>Client code dùng method này khi chỉ biết type ở runtime, không biết lúc compile.
   * Ví dụ: đọc {@code itemType} từ request JSON → gọi {@code forType(itemType)} để lấy Creator.
   *
   * @param type loại sản phẩm, không null
   * @return ConcreteCreator phù hợp (instanceof của từng ConcreteCreator tương ứng)
   * @throws IllegalArgumentException nếu type null
   */
  public static ItemCreator forType(ItemType type) {
    Objects.requireNonNull(type, "type không được null");
    return switch (type) {
      case ELECTRONICS -> new ElectronicsCreator();
      case ART -> new ArtCreator();
      case VEHICLE -> new VehicleCreator();
    };
  }

  /**
   * Helper dùng chung: lấy giá trị String từ extras, ném exception nếu thiếu.
   *
   * @param extras map extras
   * @param key    khóa cần lấy
   * @return giá trị String
   * @throws IllegalArgumentException nếu key không có hoặc không phải String
   */
  protected final String requireString(Map<String, Object> extras, String key) {
    Object value = (extras != null) ? extras.get(key) : null;
    if (!(value instanceof String str)) {
      throw new IllegalArgumentException(
          getClass().getSimpleName() + ": extras[\"" + key + "\"] phải là String, nhận: "
              + (value == null ? "null" : value.getClass().getSimpleName()));
    }
    return str;
  }

  /**
   * Helper dùng chung: lấy giá trị int từ extras, ném exception nếu thiếu.
   *
   * @param extras map extras
   * @param key    khóa cần lấy
   * @return giá trị int
   * @throws IllegalArgumentException nếu key không có hoặc không phải Integer
   */
  protected final int requireInt(Map<String, Object> extras, String key) {
    Object value = (extras != null) ? extras.get(key) : null;
    if (!(value instanceof Integer intVal)) {
      throw new IllegalArgumentException(
          getClass().getSimpleName() + ": extras[\"" + key + "\"] phải là Integer, nhận: "
              + (value == null ? "null" : value.getClass().getSimpleName()));
    }
    return intVal;
  }
}
```

```java
package com.bidhub.server.model;

import java.util.Map;

/**
 * ConcreteCreator tạo {@link Electronics} — implements factory method của {@link ItemCreator}.
 */
public final class ElectronicsCreator extends ItemCreator {

  /**
   * {@inheritDoc}
   *
   * <p>Yêu cầu extras: {@code brand} (String), {@code warrantyMonths} (Integer).
   */
  @Override
  public Item createItem(
      String name, String description, double startingPrice,
      String sellerId, Map<String, Object> extras) {
    String brand = requireString(extras, "brand");
    int warrantyMonths = requireInt(extras, "warrantyMonths");
    return new Electronics(name, description, startingPrice, sellerId, brand, warrantyMonths);
  }
}
```

```java
package com.bidhub.server.model;

import java.util.Map;

/**
 * ConcreteCreator tạo {@link Art} — implements factory method của {@link ItemCreator}.
 */
public final class ArtCreator extends ItemCreator {

  /**
   * {@inheritDoc}
   *
   * <p>Yêu cầu extras: {@code artist} (String), {@code yearCreated} (Integer).
   */
  @Override
  public Item createItem(
      String name, String description, double startingPrice,
      String sellerId, Map<String, Object> extras) {
    String artist = requireString(extras, "artist");
    int yearCreated = requireInt(extras, "yearCreated");
    return new Art(name, description, startingPrice, sellerId, artist, yearCreated);
  }
}
```

```java
package com.bidhub.server.model;

import java.util.Map;

/**
 * ConcreteCreator tạo {@link Vehicle} — implements factory method của {@link ItemCreator}.
 */
public final class VehicleCreator extends ItemCreator {

  /**
   * {@inheritDoc}
   *
   * <p>Yêu cầu extras: {@code manufacturer} (String), {@code year} (Integer),
   * {@code mileageKm} (Integer).
   */
  @Override
  public Item createItem(
      String name, String description, double startingPrice,
      String sellerId, Map<String, Object> extras) {
    String manufacturer = requireString(extras, "manufacturer");
    int year = requireInt(extras, "year");
    int mileageKm = requireInt(extras, "mileageKm");
    return new Vehicle(name, description, startingPrice, sellerId, manufacturer, year, mileageKm);
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/model/
git commit -m "feat: thêm ItemCreator abstract + ElectronicsCreator/ArtCreator/VehicleCreator (Factory Method GoF)"
```

---

#### Bước 6 — `ItemCreatorTest.java` (14+ test cases)

```java
package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử Factory Method Pattern: ItemCreator hierarchy và Item hierarchy.
 *
 * <p>Test chứng minh đầy đủ 4 thành phần GoF:
 * <ul>
 *   <li>Creator: {@link ItemCreator} và {@code forType()} dispatcher</li>
 *   <li>ConcreteCreator: {@link ElectronicsCreator}, {@link ArtCreator}, {@link VehicleCreator}</li>
 *   <li>Product: {@link Item} (polymorphism qua {@code getCategoryDetails()})</li>
 *   <li>ConcreteProduct: đúng runtime type trả về</li>
 * </ul>
 */
@DisplayName("ItemCreator — Kiểm thử Factory Method Pattern (GoF)")
class ItemCreatorTest {

  private static final String SELLER_ID = "seller-uuid-001";

  // =========================================================================
  // Test forType() dispatcher
  // =========================================================================

  @Nested
  @DisplayName("forType() — dispatcher trả về đúng ConcreteCreator")
  class ForTypeTests {

    @Test
    @DisplayName("forType(ELECTRONICS) → instanceof ElectronicsCreator")
    void testForType_Electronics_ReturnsElectronicsCreator() {
      ItemCreator creator = ItemCreator.forType(ItemType.ELECTRONICS);
      assertInstanceOf(ElectronicsCreator.class, creator);
    }

    @Test
    @DisplayName("forType(ART) → instanceof ArtCreator")
    void testForType_Art_ReturnsArtCreator() {
      ItemCreator creator = ItemCreator.forType(ItemType.ART);
      assertInstanceOf(ArtCreator.class, creator);
    }

    @Test
    @DisplayName("forType(VEHICLE) → instanceof VehicleCreator")
    void testForType_Vehicle_ReturnsVehicleCreator() {
      ItemCreator creator = ItemCreator.forType(ItemType.VEHICLE);
      assertInstanceOf(VehicleCreator.class, creator);
    }

    @Test
    @DisplayName("forType(null) → IllegalArgumentException")
    void testForType_Null_ThrowsException() {
      assertThrows(IllegalArgumentException.class,
          () -> ItemCreator.forType(null),
          "forType(null) phải ném IllegalArgumentException");
    }
  }

  // =========================================================================
  // Test createItem() trả về đúng ConcreteProduct
  // =========================================================================

  @Nested
  @DisplayName("createItem() — ConcreteCreator tạo đúng ConcreteProduct")
  class CreateItemTests {

    @Test
    @DisplayName("ElectronicsCreator.createItem() → instanceof Electronics")
    void testElectronicsCreator_CreateItem_ReturnsElectronics() {
      Item item = new ElectronicsCreator().createItem(
          "iPhone 15", "Điện thoại", 25_000_000.0, SELLER_ID,
          Map.of("brand", "Apple", "warrantyMonths", 12));
      assertInstanceOf(Electronics.class, item);
    }

    @Test
    @DisplayName("ElectronicsCreator.createItem() → kết quả instanceof Item")
    void testElectronicsCreator_CreateItem_IsInstanceOfItem() {
      Item item = new ElectronicsCreator().createItem(
          "Laptop", "", 10_000_000.0, SELLER_ID,
          Map.of("brand", "Dell", "warrantyMonths", 24));
      assertInstanceOf(Item.class, item);
    }

    @Test
    @DisplayName("ElectronicsCreator.createItem() → kết quả instanceof Entity (kế thừa 2 tầng)")
    void testElectronicsCreator_CreateItem_IsInstanceOfEntity() {
      Item item = new ElectronicsCreator().createItem(
          "TV", "", 5_000_000.0, SELLER_ID,
          Map.of("brand", "Samsung", "warrantyMonths", 0));
      assertInstanceOf(Entity.class, item,
          "Electronics → Item → Entity: phải instanceof Entity");
    }

    @Test
    @DisplayName("ArtCreator.createItem() → instanceof Art")
    void testArtCreator_CreateItem_ReturnsArt() {
      Item item = new ArtCreator().createItem(
          "Mùa thu", "", 3_000_000.0, SELLER_ID,
          Map.of("artist", "Nguyễn A", "yearCreated", 2020));
      assertInstanceOf(Art.class, item);
    }

    @Test
    @DisplayName("VehicleCreator.createItem() → instanceof Vehicle")
    void testVehicleCreator_CreateItem_ReturnsVehicle() {
      Item item = new VehicleCreator().createItem(
          "Toyota Camry", "", 600_000_000.0, SELLER_ID,
          Map.of("manufacturer", "Toyota", "year", 2022, "mileageKm", 30000));
      assertInstanceOf(Vehicle.class, item);
    }
  }

  // =========================================================================
  // Test Polymorphism: getCategoryDetails() khác nhau mỗi ConcreteProduct
  // =========================================================================

  @Nested
  @DisplayName("Polymorphism — getCategoryDetails() khác nhau")
  class PolymorphismTests {

    @Test
    @DisplayName("List<Item> gọi getCategoryDetails() → 3 output khác nhau")
    void testPolymorphism_GetCategoryDetails_ThreeDifferentOutputs() {
      List<Item> items = List.of(
          new ElectronicsCreator().createItem("Laptop", "", 10_000_000, SELLER_ID,
              Map.of("brand", "Dell", "warrantyMonths", 24)),
          new ArtCreator().createItem("Tranh lụa", "", 3_000_000, SELLER_ID,
              Map.of("artist", "Trần B", "yearCreated", 1995)),
          new VehicleCreator().createItem("Honda", "", 50_000_000, SELLER_ID,
              Map.of("manufacturer", "Honda", "year", 2019, "mileageKm", 15000)));

      List<String> details = items.stream().map(Item::getCategoryDetails).toList();

      assertNotEquals(details.get(0), details.get(1));
      assertNotEquals(details.get(1), details.get(2));
      assertTrue(details.get(0).contains("Dell"));
      assertTrue(details.get(1).contains("Trần B"));
      assertTrue(details.get(2).contains("Honda"));
    }

    @Test
    @DisplayName("Item implements Displayable — printInfo() không ném exception")
    void testItem_PrintInfo_DoesNotThrow() {
      Item item = new ElectronicsCreator().createItem(
          "TV", "", 5_000_000, SELLER_ID,
          Map.of("brand", "LG", "warrantyMonths", 0));
      assertInstanceOf(Displayable.class, item);
      assertDoesNotThrow(item::printInfo);
    }
  }

  // =========================================================================
  // Test Validation
  // =========================================================================

  @Nested
  @DisplayName("Validation — từ chối input không hợp lệ")
  class ValidationTests {

    @Test
    @DisplayName("startingPrice âm → IllegalArgumentException")
    void testCreateItem_NegativePrice_ThrowsException() {
      assertThrows(IllegalArgumentException.class,
          () -> new ArtCreator().createItem("Tranh", "", -100.0, SELLER_ID,
              Map.of("artist", "X", "yearCreated", 2000)));
    }

    @Test
    @DisplayName("startingPrice = 0 → IllegalArgumentException")
    void testCreateItem_ZeroPrice_ThrowsException() {
      assertThrows(IllegalArgumentException.class,
          () -> new ArtCreator().createItem("Tranh", "", 0, SELLER_ID,
              Map.of("artist", "X", "yearCreated", 2000)));
    }

    @Test
    @DisplayName("ElectronicsCreator thiếu extras[brand] → IllegalArgumentException")
    void testElectronicsCreator_MissingBrand_ThrowsException() {
      assertThrows(IllegalArgumentException.class,
          () -> new ElectronicsCreator().createItem("Phone", "", 5_000_000, SELLER_ID,
              Map.of("warrantyMonths", 12)),
          "Thiếu extras[brand] phải ném IllegalArgumentException");
    }
  }

  // =========================================================================
  // Test UUID từ Entity base
  // =========================================================================

  @Test
  @DisplayName("Hai item tạo từ cùng Creator có UUID khác nhau")
  void testCreator_TwoItems_HaveDifferentIds() {
    ElectronicsCreator creator = new ElectronicsCreator();
    Item i1 = creator.createItem("A", "", 1000, SELLER_ID,
        Map.of("brand", "X", "warrantyMonths", 0));
    Item i2 = creator.createItem("B", "", 1000, SELLER_ID,
        Map.of("brand", "Y", "warrantyMonths", 0));
    assertNotEquals(i1.getId(), i2.getId());
  }
}
```

```bash
git add bidhub-server/src/test/
git commit -m "test: thêm ItemCreatorTest 14 cases kiểm tra Factory Method GoF, polymorphism, validation"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Quốc Minh

```bash
# Test 1: Compile
mvn compile -pl bidhub-common,bidhub-server
# ✅ PASS: BUILD SUCCESS
# ❌ FAIL: "cannot find symbol Entity" → chưa thêm dependency bidhub-common vào bidhub-server pom

# Test 2: Chạy test
mvn test -pl bidhub-server -Dtest="ItemCreatorTest"
# ✅ PASS: "Tests run: 14, Failures: 0, Errors: 0"

# Test 3: Kiểm tra forType() dispatcher
# ItemCreator.forType(ItemType.ELECTRONICS) instanceof ElectronicsCreator → true ✅
# ItemCreator.forType(ItemType.ART) instanceof ArtCreator → true ✅

# Test 4: Kiểm tra ConcreteCreator tạo đúng ConcreteProduct
# new ElectronicsCreator().createItem(...) instanceof Electronics → true ✅
# item instanceof Entity → true (kế thừa 2 tầng) ✅

# Test 5: Kiểm tra polymorphism
# List<Item> 3 loại → getCategoryDetails() trả về 3 chuỗi khác nhau ✅

# Test 6: Đếm test cases
grep "@Test" bidhub-server/src/test/java/com/bidhub/server/model/ItemCreatorTest.java | wc -l
# ✅ PASS: 14

# Test 7: Kiểm tra KHÔNG có file ItemFactory.java
ls bidhub-server/src/main/java/com/bidhub/server/model/ItemFactory.java 2>&1
# ✅ PASS: "No such file or directory" — đã xóa, thay bằng Creator hierarchy
```

### ❌ FAIL nếu:
- `ItemCreator.forType(ELECTRONICS)` không trả về `instanceof ElectronicsCreator` → forType() sai
- `new ElectronicsCreator().createItem(...)` trả về `Item` không phải `instanceof Electronics` → override sai
- `ItemFactory.java` vẫn còn tồn tại → chưa refactor sang Creator hierarchy
- `item instanceof Entity` = `false` → kế thừa `Electronics → Item → Entity` bị đứt
- `getCategoryDetails()` của Electronics và Art trả về cùng chuỗi → override chưa đúng
- `ItemCreator` là `final class` thay vì `abstract` → ConcreteCreator không extend được

---

## 👤 CÔNG MINH — Auction, AuctionStatus & BidTransaction

```
Branch: feature/tuan-2-congminh-auction-bid
Phụ thuộc: Cần Entity.java từ branch của Đăng (tương tự Quốc Minh)
```

📌 **[Tiêu chí điểm: Chức năng đấu giá — transitions, isValidBid (1.0đ)] + [OOP — Abstraction/Encapsulation]**

### 📝 Mô tả bài tập

Xây dựng `Auction` và `BidTransaction` — trái tim của nghiệp vụ đấu giá. Tuần này chỉ tạo model (chưa có concurrency). Logic khóa `ReentrantLock` sẽ thêm ở Tuần 7.

### 📁 Cấu trúc file cần tạo

```
bidhub-server/src/main/java/com/bidhub/server/model/
    ├── AuctionStatus.java         ← Enum với canBid(), isTerminal(), và abstract method
    ├── Auction.java               ← Extends Entity: state machine, isValidBid()
    └── BidTransaction.java        ← Extends Entity: ghi lại từng lần đặt giá

bidhub-server/src/test/java/com/bidhub/server/model/
    ├── AuctionStatusTest.java     ← Test enum state machine
    └── AuctionTest.java           ← Test transitionTo(), isValidBid()
```

### 📋 Yêu cầu chi tiết

#### Bước 1 — `AuctionStatus.java` (Enum với State Machine)

```java
package com.bidhub.server.model;

/**
 * Trạng thái của phiên đấu giá — tạo thành một State Machine.
 *
 * <p>Sơ đồ chuyển trạng thái (State Machine):
 * <pre>
 *   OPEN ──────────→ RUNNING ──────────→ FINISHED
 *                                         │       │
 *                                         ↓       ↓
 *                                        PAID  CANCELED
 * </pre>
 *
 * <p>Mỗi trạng thái có hành vi riêng thông qua abstract method —
 * đây là ví dụ nâng cao về <b>enum với abstract method</b> trong Java.
 *
 * <p>Quy tắc chuyển trạng thái:
 * <ul>
 *   <li>OPEN → RUNNING: phiên đấu giá bắt đầu</li>
 *   <li>RUNNING → FINISHED: hết thời gian hoặc admin đóng</li>
 *   <li>FINISHED → PAID: người thắng đã thanh toán</li>
 *   <li>FINISHED → CANCELED: hủy sau khi kết thúc</li>
 * </ul>
 */
public enum AuctionStatus {

  /**
   * Phiên đã tạo, chờ bắt đầu.
   *
   * <p>Chưa ai đặt giá được — status này chỉ để khởi tạo.
   */
  OPEN {
    @Override
    public boolean canBid() {
      return false;
    }

    @Override
    public boolean isTerminal() {
      return false;
    }
  },

  /**
   * Đang diễn ra — người dùng có thể đặt giá.
   */
  RUNNING {
    @Override
    public boolean canBid() {
      return true; // CHỈ RUNNING mới cho phép đặt giá
    }

    @Override
    public boolean isTerminal() {
      return false;
    }
  },

  /**
   * Đã kết thúc — chờ thanh toán hoặc hủy.
   */
  FINISHED {
    @Override
    public boolean canBid() {
      return false;
    }

    @Override
    public boolean isTerminal() {
      return true;
    }
  },

  /**
   * Đã thanh toán — phiên hoàn tất.
   */
  PAID {
    @Override
    public boolean canBid() {
      return false;
    }

    @Override
    public boolean isTerminal() {
      return true;
    }
  },

  /**
   * Đã hủy — không có giao dịch nào được thực hiện.
   */
  CANCELED {
    @Override
    public boolean canBid() {
      return false;
    }

    @Override
    public boolean isTerminal() {
      return true;
    }
  };

  /**
   * Kiểm tra xem trạng thái này có cho phép đặt giá không.
   *
   * <p>Chỉ {@link #RUNNING} trả về {@code true}.
   * Được gọi trong {@link Auction#isValidBid(double)} trước khi chấp nhận bid.
   *
   * @return {@code true} nếu có thể đặt giá
   */
  public abstract boolean canBid();

  /**
   * Kiểm tra xem trạng thái này là trạng thái cuối cùng (không thể đổi tiếp).
   *
   * <p>{@link #FINISHED}, {@link #PAID}, {@link #CANCELED} trả về {@code true}.
   *
   * @return {@code true} nếu là terminal state
   */
  public abstract boolean isTerminal();

  /**
   * Kiểm tra xem có thể chuyển sang {@code targetStatus} không.
   *
   * <p>Quy tắc:
   * <ul>
   *   <li>OPEN → RUNNING ✅</li>
   *   <li>RUNNING → FINISHED ✅</li>
   *   <li>FINISHED → PAID ✅</li>
   *   <li>FINISHED → CANCELED ✅</li>
   *   <li>Mọi chuyển đổi khác ❌</li>
   * </ul>
   *
   * @param targetStatus trạng thái muốn chuyển tới
   * @return {@code true} nếu chuyển đổi hợp lệ
   */
  public boolean canTransitionTo(AuctionStatus targetStatus) {
    return switch (this) {
      case OPEN -> targetStatus == RUNNING;
      case RUNNING -> targetStatus == FINISHED;
      case FINISHED -> targetStatus == PAID || targetStatus == CANCELED;
      case PAID, CANCELED -> false; // terminal — không thể chuyển tiếp
    };
  }
}
```

---

#### Bước 2 — `Auction.java`

```java
package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Phiên đấu giá trong hệ thống BidHub.
 *
 * <p>Kế thừa {@link Entity} để có id và timestamps.
 * Chứa đầy đủ thông tin về một phiên đấu giá: item, giá cả, trạng thái.
 *
 * <p><b>State Machine:</b> Trạng thái chuyển theo sơ đồ {@link AuctionStatus}.
 * Gọi {@link #transitionTo(AuctionStatus)} để thay đổi trạng thái.
 *
 * <p><b>Lưu ý Concurrency (Tuần 7):</b> Hiện tại class này CHƯA thread-safe.
 * Tuần 7 sẽ thêm {@code ReentrantLock} vào {@link #updateHighestBid(double, String)}.
 * KHÔNG tự ý thêm {@code synchronized} ở đây — đợi đến tuần 7.
 */
public class Auction extends Entity {

  /** ID của sản phẩm đang được đấu giá. */
  private final String itemId;

  /** Thời điểm bắt đầu phiên. */
  private final LocalDateTime startTime;

  /** Thời điểm kết thúc. Có thể được gia hạn nếu Anti-Sniping kích hoạt. */
  private LocalDateTime endTime;

  /** Giá khởi điểm — không thay đổi sau khi tạo. */
  private final double startingPrice;

  /** Giá cao nhất hiện tại. Khởi tạo = startingPrice. */
  private double currentHighestBid;

  /** ID của người đặt giá cao nhất. Null nếu chưa ai đặt. */
  private String highestBidderId;

  /** Trạng thái phiên. Mặc định OPEN khi tạo. */
  private AuctionStatus status;

  /**
   * Mức tăng tối thiểu giữa 2 lần đặt giá (VND).
   *
   * <p>Ví dụ: minimumIncrement = 100_000 → bid mới phải > currentHighestBid + 100_000.
   * Tuy nhiên, trong tuần 2 này, kiểm tra minimumIncrement sẽ implement ở Tuần 6
   * (BidValidator). Hiện tại field này chỉ lưu giá trị.
   */
  private final double minimumIncrement;

  /**
   * Tạo phiên đấu giá mới.
   *
   * @param itemId         id sản phẩm, không null
   * @param startTime      thời điểm bắt đầu, không null
   * @param endTime        thời điểm kết thúc, không null, sau startTime
   * @param startingPrice  giá khởi điểm, phải > 0
   * @param minimumIncrement mức tăng tối thiểu, phải >= 0
   * @throws IllegalArgumentException nếu vi phạm điều kiện
   */
  public Auction(
      String itemId,
      LocalDateTime startTime,
      LocalDateTime endTime,
      double startingPrice,
      double minimumIncrement) {
    super();
    Objects.requireNonNull(itemId, "itemId không được null");
    Objects.requireNonNull(startTime, "startTime không được null");
    Objects.requireNonNull(endTime, "endTime không được null");
    if (startingPrice <= 0) {
      throw new IllegalArgumentException("startingPrice phải > 0: " + startingPrice);
    }
    if (!endTime.isAfter(startTime)) {
      throw new IllegalArgumentException("endTime phải sau startTime");
    }
    if (minimumIncrement < 0) {
      throw new IllegalArgumentException("minimumIncrement không được âm: " + minimumIncrement);
    }
    this.itemId = itemId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.startingPrice = startingPrice;
    this.currentHighestBid = startingPrice;
    this.highestBidderId = null;
    this.status = AuctionStatus.OPEN;
    this.minimumIncrement = minimumIncrement;
  }

  /**
   * Constructor load từ database.
   *
   * @param id               id từ DB
   * @param createdAt        thời điểm tạo từ DB
   * @param updatedAt        thời điểm cập nhật từ DB
   * @param itemId           id sản phẩm
   * @param startTime        thời điểm bắt đầu
   * @param endTime          thời điểm kết thúc
   * @param startingPrice    giá khởi điểm
   * @param currentHighestBid giá cao nhất hiện tại
   * @param highestBidderId  id người đặt cao nhất (null nếu chưa ai)
   * @param status           trạng thái hiện tại
   * @param minimumIncrement mức tăng tối thiểu
   */
  public Auction(
      String id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String itemId,
      LocalDateTime startTime,
      LocalDateTime endTime,
      double startingPrice,
      double currentHighestBid,
      String highestBidderId,
      AuctionStatus status,
      double minimumIncrement) {
    super(id, createdAt, updatedAt);
    this.itemId = itemId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.startingPrice = startingPrice;
    this.currentHighestBid = currentHighestBid;
    this.highestBidderId = highestBidderId;
    this.status = status;
    this.minimumIncrement = minimumIncrement;
  }

  /**
   * Chuyển trạng thái phiên đấu giá sang trạng thái mới.
   *
   * <p>Kiểm tra tính hợp lệ qua {@link AuctionStatus#canTransitionTo(AuctionStatus)}.
   * Nếu không hợp lệ → ném {@link IllegalStateException}.
   *
   * <p>Ví dụ hợp lệ: {@code OPEN → RUNNING}, {@code RUNNING → FINISHED}.
   * Ví dụ không hợp lệ: {@code RUNNING → OPEN} → ném exception.
   *
   * @param newStatus trạng thái mới muốn chuyển tới
   * @throws IllegalStateException nếu transition không hợp lệ
   */
  public void transitionTo(AuctionStatus newStatus) {
    Objects.requireNonNull(newStatus, "newStatus không được null");
    if (!status.canTransitionTo(newStatus)) {
      throw new IllegalStateException(
          "Không thể chuyển từ " + status.name() + " sang " + newStatus.name()
              + " [auctionId=" + getId().substring(0, 7) + "]");
    }
    this.status = newStatus;
    markUpdated();
  }

  /**
   * Kiểm tra xem một mức giá có hợp lệ để đặt không.
   *
   * <p>Điều kiện:
   * <ol>
   *   <li>Trạng thái phải là {@link AuctionStatus#RUNNING} ({@code status.canBid() == true})</li>
   *   <li>{@code bidAmount > currentHighestBid}</li>
   * </ol>
   *
   * <p><b>Tuần 6:</b> {@code BidValidator} sẽ bổ sung kiểm tra minimumIncrement.
   *
   * @param bidAmount mức giá muốn đặt
   * @return {@code true} nếu hợp lệ để đặt
   */
  public boolean isValidBid(double bidAmount) {
    return status.canBid() && bidAmount > currentHighestBid;
  }

  /**
   * Cập nhật giá cao nhất (sau khi bid được chấp nhận).
   *
   * <p><b>QUAN TRỌNG:</b> Method này CHƯA thread-safe (Tuần 7 sẽ thêm ReentrantLock).
   * Gọi method này chỉ trong môi trường single-thread (tuần 2-6).
   *
   * @param newHighestBid mức giá mới (đã kiểm tra hợp lệ trước khi gọi)
   * @param bidderId      id người đặt
   */
  public void updateHighestBid(double newHighestBid, String bidderId) {
    this.currentHighestBid = newHighestBid;
    this.highestBidderId = bidderId;
    markUpdated();
  }

  /**
   * Gia hạn thời gian kết thúc (Anti-Sniping — Tuần 8).
   *
   * @param newEndTime thời điểm kết thúc mới, phải sau endTime hiện tại
   * @throws IllegalArgumentException nếu newEndTime không sau endTime hiện tại
   */
  public void extendEndTime(LocalDateTime newEndTime) {
    Objects.requireNonNull(newEndTime, "newEndTime không được null");
    if (!newEndTime.isAfter(this.endTime)) {
      throw new IllegalArgumentException(
          "newEndTime phải sau endTime hiện tại (" + this.endTime + ")");
    }
    this.endTime = newEndTime;
    markUpdated();
  }

  // Getters
  /** Trả về id sản phẩm. */
  public String getItemId() { return itemId; }
  /** Trả về thời điểm bắt đầu. */
  public LocalDateTime getStartTime() { return startTime; }
  /** Trả về thời điểm kết thúc (có thể đã gia hạn). */
  public LocalDateTime getEndTime() { return endTime; }
  /** Trả về giá khởi điểm. */
  public double getStartingPrice() { return startingPrice; }
  /** Trả về giá cao nhất hiện tại. */
  public double getCurrentHighestBid() { return currentHighestBid; }
  /** Trả về id người đặt giá cao nhất (null nếu chưa ai đặt). */
  public String getHighestBidderId() { return highestBidderId; }
  /** Trả về trạng thái hiện tại của phiên. */
  public AuctionStatus getStatus() { return status; }
  /** Trả về mức tăng tối thiểu. */
  public double getMinimumIncrement() { return minimumIncrement; }
}
```

---

#### Bước 3 — `BidTransaction.java`

```java
package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Một lần đặt giá trong hệ thống BidHub.
 *
 * <p>Kế thừa {@link Entity} — mỗi BidTransaction có id riêng để truy vết.
 * Không thể thay đổi sau khi tạo ({@code final} fields) — lịch sử đặt giá
 * là bất biến: không ai được phép sửa lại một giao dịch đã thực hiện.
 *
 * <p>Một phiên đấu giá ({@link Auction}) có thể có nhiều {@code BidTransaction}.
 * Mối quan hệ: Auction (1) ——— (*) BidTransaction
 */
public final class BidTransaction extends Entity {

  /** ID phiên đấu giá mà bid này thuộc về. */
  private final String auctionId;

  /** ID người đặt giá. */
  private final String bidderId;

  /** Mức giá đặt (VND). */
  private final double bidAmount;

  /** Thời điểm đặt giá — ghi lại chính xác để phân tích Anti-Sniping (Tuần 8). */
  private final LocalDateTime bidTime;

  /**
   * Tạo một BidTransaction mới.
   *
   * @param auctionId id phiên đấu giá, không null
   * @param bidderId  id người đặt giá, không null
   * @param bidAmount mức giá, phải > 0
   * @throws IllegalArgumentException nếu bidAmount <= 0
   */
  public BidTransaction(String auctionId, String bidderId, double bidAmount) {
    super();
    Objects.requireNonNull(auctionId, "auctionId không được null");
    Objects.requireNonNull(bidderId, "bidderId không được null");
    if (bidAmount <= 0) {
      throw new IllegalArgumentException("bidAmount phải > 0: " + bidAmount);
    }
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.bidAmount = bidAmount;
    this.bidTime = LocalDateTime.now();
  }

  /**
   * Constructor load từ database.
   *
   * @param id        id từ DB
   * @param createdAt thời điểm tạo từ DB
   * @param updatedAt thời điểm cập nhật
   * @param auctionId id phiên đấu giá
   * @param bidderId  id người đặt giá
   * @param bidAmount mức giá
   * @param bidTime   thời điểm đặt giá từ DB
   */
  public BidTransaction(
      String id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String auctionId,
      String bidderId,
      double bidAmount,
      LocalDateTime bidTime) {
    super(id, createdAt, updatedAt);
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.bidAmount = bidAmount;
    this.bidTime = Objects.requireNonNull(bidTime, "bidTime không được null");
  }

  // Getters — tất cả fields là final, không có setter (immutable record of bid)
  /** Trả về id phiên đấu giá. */
  public String getAuctionId() { return auctionId; }
  /** Trả về id người đặt giá. */
  public String getBidderId() { return bidderId; }
  /** Trả về mức giá đã đặt. */
  public double getBidAmount() { return bidAmount; }
  /** Trả về thời điểm đặt giá. */
  public LocalDateTime getBidTime() { return bidTime; }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "BidTransaction[id=" + getId().substring(0, 7)
        + ", auctionId=" + auctionId.substring(0, 7)
        + ", bidderId=" + bidderId.substring(0, 7)
        + String.format(", amount=%,.0f VND", bidAmount)
        + ", time=" + bidTime + "]";
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/model/
git commit -m "feat: thêm AuctionStatus enum state machine, Auction, và BidTransaction"
```

---

#### Bước 4 — Test classes

```java
package com.bidhub.server.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử AuctionStatus enum và state machine. */
@DisplayName("AuctionStatus — State machine và canBid()/isTerminal()")
class AuctionStatusTest {

  @Test
  @DisplayName("Chỉ RUNNING.canBid() = true, các status khác false")
  void testCanBid_OnlyRunningReturnsTrue() {
    assertTrue(AuctionStatus.RUNNING.canBid(), "RUNNING phải cho phép đặt giá");
    assertFalse(AuctionStatus.OPEN.canBid(), "OPEN chưa được đặt giá");
    assertFalse(AuctionStatus.FINISHED.canBid(), "FINISHED không còn đặt giá được");
    assertFalse(AuctionStatus.PAID.canBid(), "PAID là terminal");
    assertFalse(AuctionStatus.CANCELED.canBid(), "CANCELED là terminal");
  }

  @Test
  @DisplayName("FINISHED, PAID, CANCELED là terminal states")
  void testIsTerminal_CorrectStatuses() {
    assertFalse(AuctionStatus.OPEN.isTerminal());
    assertFalse(AuctionStatus.RUNNING.isTerminal());
    assertTrue(AuctionStatus.FINISHED.isTerminal());
    assertTrue(AuctionStatus.PAID.isTerminal());
    assertTrue(AuctionStatus.CANCELED.isTerminal());
  }

  @Test
  @DisplayName("OPEN → RUNNING: transition hợp lệ")
  void testTransition_OpenToRunning_Valid() {
    assertTrue(AuctionStatus.OPEN.canTransitionTo(AuctionStatus.RUNNING));
  }

  @Test
  @DisplayName("RUNNING → OPEN: transition KHÔNG hợp lệ (không đi ngược)")
  void testTransition_RunningToOpen_Invalid() {
    assertFalse(AuctionStatus.RUNNING.canTransitionTo(AuctionStatus.OPEN),
        "Không thể quay lại OPEN từ RUNNING");
  }

  @Test
  @DisplayName("FINISHED → PAID: hợp lệ")
  void testTransition_FinishedToPaid_Valid() {
    assertTrue(AuctionStatus.FINISHED.canTransitionTo(AuctionStatus.PAID));
  }

  @Test
  @DisplayName("FINISHED → CANCELED: hợp lệ")
  void testTransition_FinishedToCanceled_Valid() {
    assertTrue(AuctionStatus.FINISHED.canTransitionTo(AuctionStatus.CANCELED));
  }

  @Test
  @DisplayName("PAID → bất kỳ: đều không hợp lệ (terminal)")
  void testTransition_FromPaid_AlwaysInvalid() {
    for (AuctionStatus target : AuctionStatus.values()) {
      assertFalse(AuctionStatus.PAID.canTransitionTo(target),
          "PAID là terminal, không thể chuyển sang " + target);
    }
  }
}
```

```java
package com.bidhub.server.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử Auction: transitionTo(), isValidBid(), và BidTransaction. */
@DisplayName("Auction — transitionTo() và isValidBid()")
class AuctionTest {

  private Auction auction;
  private static final String ITEM_ID = "item-uuid-001";
  private static final String BIDDER_ID = "bidder-uuid-001";

  @BeforeEach
  void setUp() {
    // Tạo auction mới trước mỗi test: giá khởi điểm 1_000_000, tăng tối thiểu 100_000
    auction = new Auction(
        ITEM_ID,
        LocalDateTime.now().minusMinutes(5),
        LocalDateTime.now().plusHours(1),
        1_000_000.0,
        100_000.0);
  }

  @Nested
  @DisplayName("transitionTo() — State Machine")
  class TransitionTests {

    @Test
    @DisplayName("Auction mới tạo có status OPEN")
    void testNewAuction_StatusIsOpen() {
      assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    @DisplayName("OPEN → RUNNING thành công")
    void testTransition_OpenToRunning_Success() {
      // Act
      auction.transitionTo(AuctionStatus.RUNNING);

      // Assert
      assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    @DisplayName("RUNNING → OPEN ném IllegalStateException")
    void testTransition_RunningToOpen_ThrowsException() {
      // Arrange
      auction.transitionTo(AuctionStatus.RUNNING);

      // Act & Assert
      assertThrows(IllegalStateException.class,
          () -> auction.transitionTo(AuctionStatus.OPEN),
          "Chuyển ngược từ RUNNING về OPEN phải ném IllegalStateException");
    }

    @Test
    @DisplayName("OPEN → FINISHED (bỏ qua RUNNING) ném IllegalStateException")
    void testTransition_OpenToFinished_ThrowsException() {
      assertThrows(IllegalStateException.class,
          () -> auction.transitionTo(AuctionStatus.FINISHED),
          "Bỏ qua RUNNING phải ném IllegalStateException");
    }

    @Test
    @DisplayName("RUNNING → FINISHED → PAID: chuỗi transition hợp lệ")
    void testTransition_FullLifecycle_Success() {
      auction.transitionTo(AuctionStatus.RUNNING);
      auction.transitionTo(AuctionStatus.FINISHED);
      auction.transitionTo(AuctionStatus.PAID);
      assertEquals(AuctionStatus.PAID, auction.getStatus());
    }
  }

  @Nested
  @DisplayName("isValidBid() — Kiểm tra bid hợp lệ")
  class BidValidationTests {

    @Test
    @DisplayName("Status OPEN → isValidBid() luôn false dù bidAmount cao hơn")
    void testIsValidBid_WhenOpen_ReturnsFalse() {
      // Auction đang OPEN (mặc định), không ai được đặt giá
      assertFalse(auction.isValidBid(2_000_000.0),
          "OPEN không cho phép đặt giá dù bidAmount hợp lệ");
    }

    @Test
    @DisplayName("Status RUNNING, bidAmount > currentHighestBid → isValidBid() = true")
    void testIsValidBid_WhenRunning_HigherBid_ReturnsTrue() {
      // Arrange
      auction.transitionTo(AuctionStatus.RUNNING);

      // Act + Assert
      assertTrue(auction.isValidBid(1_100_000.0),
          "RUNNING + bid cao hơn phải hợp lệ");
    }

    @Test
    @DisplayName("Status RUNNING, bidAmount = currentHighestBid → isValidBid() = false")
    void testIsValidBid_WhenRunning_EqualBid_ReturnsFalse() {
      auction.transitionTo(AuctionStatus.RUNNING);
      // bidAmount bằng chính xác currentHighestBid (= startingPrice = 1_000_000)
      assertFalse(auction.isValidBid(1_000_000.0),
          "Bid bằng currentHighestBid không hợp lệ (phải lớn HƠN)");
    }

    @Test
    @DisplayName("Status RUNNING, bidAmount < currentHighestBid → isValidBid() = false")
    void testIsValidBid_WhenRunning_LowerBid_ReturnsFalse() {
      auction.transitionTo(AuctionStatus.RUNNING);
      assertFalse(auction.isValidBid(500_000.0),
          "Bid thấp hơn currentHighestBid phải false");
    }

    @Test
    @DisplayName("updateHighestBid() cập nhật giá và bidderId đúng")
    void testUpdateHighestBid_UpdatesCorrectly() {
      // Arrange
      auction.transitionTo(AuctionStatus.RUNNING);

      // Act
      auction.updateHighestBid(1_500_000.0, BIDDER_ID);

      // Assert
      assertEquals(1_500_000.0, auction.getCurrentHighestBid(), 0.01);
      assertEquals(BIDDER_ID, auction.getHighestBidderId());
    }
  }

  @Nested
  @DisplayName("BidTransaction — bất biến và UUID")
  class BidTransactionTests {

    @Test
    @DisplayName("BidTransaction mới tạo có bidTime không null")
    void testBidTransaction_BidTime_NotNull() {
      BidTransaction tx = new BidTransaction("auction-1", "bidder-1", 2_000_000.0);
      assertNotNull(tx.getBidTime(), "bidTime không được null");
      assertNotNull(tx.getId(), "id không được null");
    }

    @Test
    @DisplayName("BidTransaction với bidAmount = 0 → IllegalArgumentException")
    void testBidTransaction_ZeroAmount_ThrowsException() {
      assertThrows(IllegalArgumentException.class,
          () -> new BidTransaction("a", "b", 0),
          "bidAmount = 0 phải ném exception");
    }
  }
}
```

```bash
git add bidhub-server/src/test/
git commit -m "test: thêm AuctionStatusTest và AuctionTest kiểm tra state machine và isValidBid"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Công Minh

```bash
# Test 1: Compile
mvn compile -pl bidhub-common,bidhub-server
# ✅ PASS: BUILD SUCCESS

# Test 2: Chạy toàn bộ test Auction
mvn test -pl bidhub-server -Dtest="AuctionStatusTest,AuctionTest"
# ✅ PASS: "Tests run: 14+, Failures: 0, Errors: 0"

# Test 3: Chạy cụ thể test transition không hợp lệ
mvn test -pl bidhub-server -Dtest="AuctionTest#testTransition_RunningToOpen_ThrowsException"
# ✅ PASS: Test PASSED

# Test 4: Manual test state machine (thêm vào main tạm thời để kiểm tra)
# Auction a = new Auction("item-1", LocalDateTime.now(), LocalDateTime.now().plusHours(1), 1000, 100);
# a.transitionTo(RUNNING);
# System.out.println(a.isValidBid(1500));  // true ✅
# System.out.println(a.isValidBid(500));   // false ✅
# a.transitionTo(OPEN);                    // → IllegalStateException ✅

# Test 5: Đếm test cases
grep "@Test" bidhub-server/src/test/java/com/bidhub/server/model/AuctionTest.java | wc -l
# ✅ PASS: ≥ 9

# Test 6: Kiểm tra BidTransaction không có setter (bất biến)
# Xem BidTransaction.java → không có setAuctionId(), setBidAmount() v.v.
grep "public void set" bidhub-server/src/main/java/com/bidhub/server/model/BidTransaction.java
# ✅ PASS: không có output → BidTransaction đúng là immutable
```

### ❌ FAIL nếu:
- `auction.transitionTo(OPEN)` từ trạng thái RUNNING không ném exception
- `AuctionStatus.FINISHED.canBid()` = `true` → enum logic sai
- `isValidBid(currentHighestBid)` trả `true` → điều kiện phải là `>` không phải `>=`
- `BidTransaction` có setter → vi phạm tính bất biến của lịch sử giao dịch
- `Auction.extendEndTime(newEndTime)` không kiểm tra `newEndTime.isAfter(endTime)` → Anti-Sniping tuần 8 sẽ bị sai

---

## 👤 KHOA — Exception Hierarchy & Integration Tests

```
Branch: feature/tuan-2-khoa-exception
Phụ thuộc: KHÔNG phụ thuộc ai — làm song song ngay từ Thứ 2
```

📌 **[Tiêu chí điểm: Xử lý lỗi & ngoại lệ — 1.0đ] + [Unit Test JUnit ≥ 40 cases — 0.5đ]**

### 📝 Mô tả bài tập

Xây dựng hệ thống exception hoàn chỉnh cho BidHub. Đây là nền tảng cho toàn bộ error handling từ Tuần 3 đến 10. Ngoài ra, tổng hợp và bổ sung test cases để đạt mục tiêu ≥ 25 test mới trong tuần 2 (tổng cộng ≥ 40 tests bao gồm cả tuần 1).

### 📁 Cấu trúc file cần tạo

```
bidhub-common/src/main/java/com/bidhub/common/exception/
    ├── BidHubException.java           ← Base: RuntimeException + errorCode
    ├── InvalidBidException.java       ← "BID_INVALID"
    ├── AuctionNotFoundException.java  ← "AUCTION_NOT_FOUND"
    ├── AuctionClosedException.java    ← "AUCTION_CLOSED"
    ├── UserNotFoundException.java     ← "USER_NOT_FOUND"
    ├── DuplicateUsernameException.java← "USERNAME_TAKEN"
    ├── AuthenticationException.java   ← "AUTH_FAILED"
    └── ValidationException.java       ← "VALIDATION_ERROR" + errors: List<String>

bidhub-common/src/test/java/com/bidhub/common/exception/
    └── ExceptionHierarchyTest.java    ← ≥ 10 test cases exception

bidhub-server/src/test/java/com/bidhub/server/model/
    └── DomainIntegrationTest.java     ← Test tích hợp Entity + User + Auction
```

### 📋 Yêu cầu chi tiết

#### Bước 1 — `BidHubException.java` (Base Exception)

```java
package com.bidhub.common.exception;

/**
 * Base exception của hệ thống BidHub.
 *
 * <p>Tất cả exception trong BidHub đều kế thừa lớp này, cho phép:
 * <ul>
 *   <li>Catch gọn: {@code catch (BidHubException e)} bắt được mọi lỗi nghiệp vụ</li>
 *   <li>Phân loại lỗi: mỗi subclass có {@code errorCode} riêng → client
 *       xử lý đúng case (ví dụ: "BID_INVALID" hiện "Giá đặt không hợp lệ")</li>
 *   <li>Tương thích JSON: {@code errorCode} sẽ được đưa vào response
 *       JSON từ Tuần 4 ({@code {"status":"ERROR","errorCode":"BID_INVALID"}})</li>
 * </ul>
 *
 * <p><b>Tại sao extends RuntimeException (unchecked)?</b>
 * Checked exception bắt buộc try-catch ở mọi nơi → code rối, khó đọc.
 * RuntimeException không bắt buộc khai báo trong signature nhưng vẫn
 * có thể catch khi cần xử lý cụ thể.
 *
 * <p>Ví dụ sử dụng (từ Tuần 5):
 * <pre>{@code
 * // Ném exception khi bid không hợp lệ
 * if (!auction.isValidBid(amount)) {
 *     throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại: " + amount);
 * }
 *
 * // Bắt tại tầng Request Handler
 * try {
 *     bidService.placeBid(auctionId, amount, userId);
 * } catch (BidHubException e) {
 *     sendError(session, e.getErrorCode(), e.getMessage());
 * }
 * }</pre>
 */
public class BidHubException extends RuntimeException {

  /**
   * Mã lỗi ngắn gọn để client xử lý logic.
   *
   * <p>Ví dụ: {@code "BID_INVALID"}, {@code "AUCTION_NOT_FOUND"}.
   * Quy ước: SCREAMING_SNAKE_CASE.
   */
  private final String errorCode;

  /**
   * Tạo BidHubException với message và errorCode.
   *
   * @param message   thông báo lỗi đọc được bởi con người
   * @param errorCode mã lỗi ngắn gọn cho client (SCREAMING_SNAKE_CASE)
   */
  public BidHubException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Tạo BidHubException với message, errorCode, và nguyên nhân gốc.
   *
   * <p>Dùng khi wrap exception từ tầng dưới (ví dụ: SQLException).
   *
   * @param message   thông báo lỗi
   * @param errorCode mã lỗi
   * @param cause     nguyên nhân gốc
   */
  public BidHubException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * Trả về mã lỗi ngắn gọn.
   *
   * @return errorCode (ví dụ: {@code "BID_INVALID"}), không bao giờ null
   */
  public String getErrorCode() {
    return errorCode;
  }
}
```

---

#### Bước 2 — 6 Subclass đơn giản

```java
package com.bidhub.common.exception;

/**
 * Ném khi bid không hợp lệ (thấp hơn giá hiện tại, không đủ increment, v.v.).
 *
 * <p>Sử dụng từ Tuần 6 trong {@code BidValidator}.
 */
public class InvalidBidException extends BidHubException {

  /** Mã lỗi cố định của exception này. */
  public static final String ERROR_CODE = "BID_INVALID";

  /**
   * Tạo InvalidBidException.
   *
   * @param message mô tả lý do bid không hợp lệ
   */
  public InvalidBidException(String message) {
    super(message, ERROR_CODE);
  }
}
```

```java
package com.bidhub.common.exception;

/**
 * Ném khi tìm kiếm phiên đấu giá không tồn tại trong hệ thống.
 *
 * <p>Sử dụng từ Tuần 4 trong {@code RequestHandler}.
 */
public class AuctionNotFoundException extends BidHubException {

  /** Mã lỗi cố định. */
  public static final String ERROR_CODE = "AUCTION_NOT_FOUND";

  /**
   * Tạo AuctionNotFoundException.
   *
   * @param auctionId id của phiên đấu giá không tìm thấy
   */
  public AuctionNotFoundException(String auctionId) {
    super("Không tìm thấy phiên đấu giá với id: " + auctionId, ERROR_CODE);
  }
}
```

```java
package com.bidhub.common.exception;

/**
 * Ném khi cố đặt giá vào phiên đấu giá đã đóng (FINISHED/PAID/CANCELED).
 *
 * <p>Khác với {@link InvalidBidException}: exception này về trạng thái phiên,
 * không phải về giá trị bid.
 */
public class AuctionClosedException extends BidHubException {

  /** Mã lỗi cố định. */
  public static final String ERROR_CODE = "AUCTION_CLOSED";

  /**
   * Tạo AuctionClosedException.
   *
   * @param auctionId id phiên đã đóng
   * @param status    trạng thái hiện tại của phiên
   */
  public AuctionClosedException(String auctionId, String status) {
    super("Phiên đấu giá " + auctionId + " đã đóng (status: " + status + ")", ERROR_CODE);
  }
}
```

```java
package com.bidhub.common.exception;

/**
 * Ném khi tìm kiếm người dùng không tồn tại.
 *
 * <p>Sử dụng từ Tuần 5 trong {@code UserService}.
 */
public class UserNotFoundException extends BidHubException {

  /** Mã lỗi cố định. */
  public static final String ERROR_CODE = "USER_NOT_FOUND";

  /**
   * Tạo UserNotFoundException.
   *
   * @param identifier username hoặc id của người dùng không tìm thấy
   */
  public UserNotFoundException(String identifier) {
    super("Không tìm thấy người dùng: " + identifier, ERROR_CODE);
  }
}
```

```java
package com.bidhub.common.exception;

/**
 * Ném khi đăng ký với username đã tồn tại trong hệ thống.
 *
 * <p>Sử dụng từ Tuần 5 trong {@code UserService.register()}.
 */
public class DuplicateUsernameException extends BidHubException {

  /** Mã lỗi cố định. */
  public static final String ERROR_CODE = "USERNAME_TAKEN";

  /**
   * Tạo DuplicateUsernameException.
   *
   * @param username tên đăng nhập đã bị trùng
   */
  public DuplicateUsernameException(String username) {
    super("Tên đăng nhập '" + username + "' đã tồn tại trong hệ thống", ERROR_CODE);
  }
}
```

```java
package com.bidhub.common.exception;

/**
 * Ném khi xác thực thất bại (sai mật khẩu, token không hợp lệ, chưa đăng nhập).
 *
 * <p>Sử dụng từ Tuần 5 trong {@code AuthService.login()} và
 * từ Tuần 4 trong các handler yêu cầu token.
 */
public class AuthenticationException extends BidHubException {

  /** Mã lỗi cố định. */
  public static final String ERROR_CODE = "AUTH_FAILED";

  /**
   * Tạo AuthenticationException.
   *
   * @param message mô tả lý do xác thực thất bại
   */
  public AuthenticationException(String message) {
    super(message, ERROR_CODE);
  }
}
```

---

#### Bước 3 — `ValidationException.java` (exception đặc biệt với nhiều lỗi)

📌 **[Tiêu chí điểm: Xử lý lỗi & ngoại lệ — ValidationException với errors list — 1.0đ]**

```java
package com.bidhub.common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Exception khi form nhập liệu vi phạm nhiều quy tắc validation cùng lúc.
 *
 * <p>Khác các exception còn lại, {@code ValidationException} chứa
 * {@link List} các lỗi cụ thể — cho phép client hiển thị tất cả vấn đề
 * một lúc thay vì chỉ lỗi đầu tiên.
 *
 * <p>Ví dụ sử dụng (từ Tuần 5):
 * <pre>{@code
 * List<String> errors = new ArrayList<>();
 * if (username == null || username.isBlank()) {
 *     errors.add("Username không được để trống");
 * }
 * if (username != null && username.length() < 3) {
 *     errors.add("Username phải ≥ 3 ký tự");
 * }
 * if (password == null || password.length() < 6) {
 *     errors.add("Mật khẩu phải ≥ 6 ký tự");
 * }
 * if (!errors.isEmpty()) {
 *     throw new ValidationException(errors);
 * }
 * }</pre>
 */
public class ValidationException extends BidHubException {

  /** Mã lỗi cố định. */
  public static final String ERROR_CODE = "VALIDATION_ERROR";

  /**
   * Danh sách lỗi validation cụ thể.
   *
   * <p>Unmodifiable để tránh thay đổi ngoài ý muốn sau khi exception được tạo.
   */
  private final List<String> errors;

  /**
   * Tạo ValidationException với một lỗi duy nhất.
   *
   * <p>Convenience constructor cho trường hợp đơn giản có 1 lỗi.
   *
   * @param errorMessage mô tả lỗi, không null
   */
  public ValidationException(String errorMessage) {
    super(errorMessage, ERROR_CODE);
    Objects.requireNonNull(errorMessage, "errorMessage không được null");
    this.errors = List.of(errorMessage);
  }

  /**
   * Tạo ValidationException với nhiều lỗi.
   *
   * @param errors danh sách lỗi, không null, không rỗng
   * @throws IllegalArgumentException nếu errors null hoặc rỗng
   */
  public ValidationException(List<String> errors) {
    super(buildMessage(errors), ERROR_CODE);
    if (errors == null || errors.isEmpty()) {
      throw new IllegalArgumentException(
          "ValidationException cần ít nhất 1 lỗi trong danh sách");
    }
    // Tạo defensive copy và wrap bằng unmodifiableList
    this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
  }

  /**
   * Trả về danh sách lỗi validation (unmodifiable).
   *
   * @return list lỗi, không bao giờ null, không bao giờ rỗng
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Trả về số lượng lỗi validation.
   *
   * @return số lỗi (≥ 1)
   */
  public int getErrorCount() {
    return errors.size();
  }

  /**
   * Tạo message tổng hợp từ danh sách lỗi.
   *
   * @param errors danh sách lỗi
   * @return message tổng hợp
   */
  private static String buildMessage(List<String> errors) {
    if (errors == null || errors.isEmpty()) {
      return "Dữ liệu không hợp lệ";
    }
    return errors.size() + " lỗi validation: " + String.join("; ", errors);
  }
}
```

```bash
git add bidhub-common/src/main/java/com/bidhub/common/exception/
git commit -m "feat: thêm BidHubException hierarchy: 6 subclass + ValidationException với errors list"
```

---

#### Bước 4 — `ExceptionHierarchyTest.java`

```java
package com.bidhub.common.exception;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử hệ thống exception của BidHub.
 *
 * <p>Tests này chứng minh:
 * <ul>
 *   <li>Kế thừa đúng: mọi exception là instanceof BidHubException</li>
 *   <li>errorCode đúng cho từng subclass</li>
 *   <li>Polymorphism: catch BidHubException bắt được tất cả</li>
 *   <li>ValidationException chứa đúng errors list</li>
 * </ul>
 */
@DisplayName("ExceptionHierarchy — Kiểm thử BidHubException và 7 subclass")
class ExceptionHierarchyTest {

  // =========================================================================
  // Test Inheritance: mọi exception là BidHubException
  // =========================================================================

  @Nested
  @DisplayName("Inheritance — tất cả là instanceof BidHubException")
  class InheritanceTests {

    @Test
    @DisplayName("InvalidBidException instanceof BidHubException và RuntimeException")
    void testInvalidBid_IsInstanceOfBidHubException() {
      // Arrange + Act
      InvalidBidException ex = new InvalidBidException("Giá quá thấp");

      // Assert
      assertInstanceOf(BidHubException.class, ex);
      assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("Tất cả 6 subclass đều instanceof BidHubException")
    void testAllSubclasses_AreInstanceOfBidHubException() {
      // Arrange — tạo 1 instance mỗi loại
      List<BidHubException> exceptions = List.of(
          new InvalidBidException("test"),
          new AuctionNotFoundException("auction-1"),
          new AuctionClosedException("auction-2", "FINISHED"),
          new UserNotFoundException("user-1"),
          new DuplicateUsernameException("alice"),
          new AuthenticationException("Sai mật khẩu"),
          new ValidationException("field rỗng"));

      // Assert — mỗi exception phải đúng kiểu
      for (BidHubException ex : exceptions) {
        assertInstanceOf(BidHubException.class, ex,
            ex.getClass().getSimpleName() + " phải là BidHubException");
      }
    }

    @Test
    @DisplayName("Polymorphism: catch BidHubException bắt được InvalidBidException")
    void testPolymorphism_CatchBidHubException_CatchesSubclass() {
      // Arrange
      BidHubException caught = null;

      // Act — throw InvalidBidException, catch BidHubException
      try {
        throw new InvalidBidException("Giá đặt thấp hơn giá hiện tại");
      } catch (BidHubException e) {
        caught = e;
      }

      // Assert
      assertNotNull(caught, "BidHubException phải bắt được InvalidBidException");
      assertInstanceOf(InvalidBidException.class, caught);
    }
  }

  // =========================================================================
  // Test errorCode đúng theo từng subclass
  // =========================================================================

  @Nested
  @DisplayName("ErrorCode — mỗi subclass có errorCode đúng")
  class ErrorCodeTests {

    @Test
    @DisplayName("InvalidBidException có errorCode = BID_INVALID")
    void testInvalidBid_ErrorCode() {
      assertEquals("BID_INVALID", new InvalidBidException("x").getErrorCode());
    }

    @Test
    @DisplayName("AuctionNotFoundException có errorCode = AUCTION_NOT_FOUND")
    void testAuctionNotFound_ErrorCode() {
      assertEquals("AUCTION_NOT_FOUND", new AuctionNotFoundException("id-1").getErrorCode());
    }

    @Test
    @DisplayName("AuctionClosedException có errorCode = AUCTION_CLOSED")
    void testAuctionClosed_ErrorCode() {
      assertEquals("AUCTION_CLOSED",
          new AuctionClosedException("id-1", "FINISHED").getErrorCode());
    }

    @Test
    @DisplayName("DuplicateUsernameException có errorCode = USERNAME_TAKEN")
    void testDuplicateUsername_ErrorCode() {
      assertEquals("USERNAME_TAKEN", new DuplicateUsernameException("alice").getErrorCode());
    }

    @Test
    @DisplayName("AuthenticationException có errorCode = AUTH_FAILED")
    void testAuthentication_ErrorCode() {
      assertEquals("AUTH_FAILED", new AuthenticationException("Sai mật khẩu").getErrorCode());
    }

    @Test
    @DisplayName("ValidationException có errorCode = VALIDATION_ERROR")
    void testValidation_ErrorCode() {
      assertEquals("VALIDATION_ERROR", new ValidationException("lỗi").getErrorCode());
    }
  }

  // =========================================================================
  // Test ValidationException đặc biệt
  // =========================================================================

  @Nested
  @DisplayName("ValidationException — errors list")
  class ValidationExceptionTests {

    @Test
    @DisplayName("ValidationException(String) → getErrors() trả về list 1 phần tử")
    void testValidation_SingleError_ErrorsListHasOneItem() {
      // Arrange + Act
      ValidationException ex = new ValidationException("Username rỗng");

      // Assert
      assertNotNull(ex.getErrors(), "getErrors() không được null");
      assertFalse(ex.getErrors().isEmpty(), "getErrors() không được rỗng");
      assertEquals(1, ex.getErrorCount());
      assertEquals("Username rỗng", ex.getErrors().get(0));
    }

    @Test
    @DisplayName("ValidationException(List) → getErrors() trả về đúng số lượng")
    void testValidation_MultipleErrors_ErrorsListCorrect() {
      // Arrange
      List<String> errors = List.of(
          "Username không được rỗng",
          "Password phải ≥ 6 ký tự",
          "Email không hợp lệ");

      // Act
      ValidationException ex = new ValidationException(errors);

      // Assert
      assertEquals(3, ex.getErrorCount(), "Phải có đúng 3 lỗi");
      assertTrue(ex.getErrors().contains("Username không được rỗng"));
      assertTrue(ex.getMessage().contains("3 lỗi validation"));
    }

    @Test
    @DisplayName("ValidationException.getErrors() là unmodifiable (không thể thêm lỗi sau)")
    void testValidation_ErrorsList_IsUnmodifiable() {
      // Arrange
      ValidationException ex = new ValidationException(List.of("Lỗi 1", "Lỗi 2"));

      // Act + Assert — cố thêm vào list → phải ném exception
      assertThrows(UnsupportedOperationException.class,
          () -> ex.getErrors().add("Lỗi hacker thêm vào"),
          "getErrors() phải là unmodifiable list");
    }

    @Test
    @DisplayName("ValidationException với list rỗng → IllegalArgumentException")
    void testValidation_EmptyList_ThrowsException() {
      assertThrows(IllegalArgumentException.class,
          () -> new ValidationException(List.of()),
          "List lỗi rỗng phải ném IllegalArgumentException");
    }

    @Test
    @DisplayName("ValidationException với null list → IllegalArgumentException")
    void testValidation_NullList_ThrowsException() {
      assertThrows(IllegalArgumentException.class,
          () -> new ValidationException((List<String>) null),
          "null list phải ném IllegalArgumentException");
    }
  }
}
```

```bash
git add bidhub-common/src/test/
git commit -m "test: thêm ExceptionHierarchyTest 10+ cases kiểm tra inheritance, errorCode, ValidationException"
```

---

#### Bước 5 — `DomainIntegrationTest.java` (test tích hợp domain)

```java
package com.bidhub.server.model;

import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.common.exception.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test tích hợp domain: kết hợp User, Auction, Item, Exception.
 *
 * <p>Mỗi test mô phỏng một luồng nghiệp vụ thực tế để đảm bảo
 * các class hoạt động đúng khi tương tác với nhau.
 */
@DisplayName("DomainIntegration — Test tích hợp domain Tuần 2")
class DomainIntegrationTest {

  @Test
  @DisplayName("Kịch bản: Bidder đặt giá hợp lệ vào phiên RUNNING")
  void testScenario_BidderPlacesValidBid() {
    // Arrange
    Bidder bidder = new Bidder("alice", "hash_alice", "alice@mail.com");
    Auction auction = new Auction(
        "item-001",
        LocalDateTime.now().minusMinutes(10),
        LocalDateTime.now().plusMinutes(30),
        5_000_000.0, 500_000.0);
    auction.transitionTo(AuctionStatus.RUNNING);

    // Act — kiểm tra bid hợp lệ
    double bidAmount = 6_000_000.0;
    boolean valid = auction.isValidBid(bidAmount);

    // Assert
    assertTrue(valid, "Bid 6 triệu vào phiên RUNNING với giá KĐ 5 triệu phải hợp lệ");

    // Cập nhật highest bid
    auction.updateHighestBid(bidAmount, bidder.getId());
    assertEquals(bidAmount, auction.getCurrentHighestBid(), 0.01);
    assertEquals(bidder.getId(), auction.getHighestBidderId());
  }

  @Test
  @DisplayName("Kịch bản: Validation user registration — nhiều lỗi cùng lúc")
  void testScenario_ValidateUserRegistration_MultipleErrors() {
    // Arrange — mô phỏng validation logic (tuần 5 sẽ implement thật)
    String username = "ab";    // quá ngắn
    String password = "123";   // quá ngắn
    String email = "";         // rỗng

    List<String> errors = new ArrayList<>();
    if (username.length() < 3) errors.add("Username phải ≥ 3 ký tự");
    if (password.length() < 6) errors.add("Password phải ≥ 6 ký tự");
    if (email.isBlank()) errors.add("Email không được để trống");

    // Act
    ValidationException ex = assertThrows(
        ValidationException.class,
        () -> { if (!errors.isEmpty()) throw new ValidationException(errors); });

    // Assert
    assertEquals(3, ex.getErrorCount(), "Phải có đúng 3 lỗi validation");
    assertTrue(ex.getMessage().contains("3 lỗi validation"));
  }

  @Test
  @DisplayName("Kịch bản: Exception hierarchy trong luồng đặt giá")
  void testScenario_BidOnClosedAuction_ThrowsAuctionClosedException() {
    // Arrange
    Auction auction = new Auction("item-002",
        LocalDateTime.now().minusHours(2),
        LocalDateTime.now().minusHours(1),
        1_000_000.0, 0.0);
    auction.transitionTo(AuctionStatus.RUNNING);
    auction.transitionTo(AuctionStatus.FINISHED);

    // Act — mô phỏng logic sẽ có trong BidService (Tuần 6)
    AuctionClosedException ex = assertThrows(
        AuctionClosedException.class,
        () -> {
          if (auction.getStatus().isTerminal()) {
            throw new AuctionClosedException(auction.getId(), auction.getStatus().name());
          }
        });

    // Assert
    assertEquals("AUCTION_CLOSED", ex.getErrorCode());
    assertTrue(ex.getMessage().contains("FINISHED"));
  }

  @Test
  @DisplayName("Kịch bản: ItemFactory + Auction + Bidder hoạt động cùng nhau")
  void testScenario_FullDomainObjectsInteraction() {
    // Arrange — tạo đầy đủ objects domain
    Seller seller = new Seller("bob_seller", "hash_bob", "bob@mail.com");
    Item laptop = ItemFactory.create(
        ItemType.ELECTRONICS, "Dell XPS 15", "Laptop cao cấp",
        30_000_000.0, seller.getId(),
        java.util.Map.of("brand", "Dell", "warrantyMonths", 24));

    Auction auction = new Auction(
        laptop.getId(),
        LocalDateTime.now().minusMinutes(5),
        LocalDateTime.now().plusHours(2),
        laptop.getStartingPrice(), 1_000_000.0);
    auction.transitionTo(AuctionStatus.RUNNING);

    Bidder bidder = new Bidder("carol_bidder", "hash_carol", "carol@mail.com");

    // Act
    double bidAmount = laptop.getStartingPrice() + 2_000_000.0; // 32 triệu
    assertTrue(auction.isValidBid(bidAmount));
    auction.updateHighestBid(bidAmount, bidder.getId());

    BidTransaction tx = new BidTransaction(auction.getId(), bidder.getId(), bidAmount);

    // Assert — kiểm tra toàn bộ domain kết nối đúng
    assertEquals(bidAmount, auction.getCurrentHighestBid(), 0.01);
    assertEquals(bidder.getId(), auction.getHighestBidderId());
    assertEquals(auction.getId(), tx.getAuctionId());
    assertEquals(bidder.getId(), tx.getBidderId());
    assertNotNull(tx.getBidTime());

    // Seller, laptop, auction, bidder đều có UUID riêng
    assertNotEquals(seller.getId(), laptop.getId());
    assertNotEquals(laptop.getId(), auction.getId());
  }
}
```

```bash
git add bidhub-server/src/test/
git commit -m "test: thêm DomainIntegrationTest kiểm tra tương tác User, Auction, Item, Exception"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Khoa

```bash
# Test 1: Compile bidhub-common (exceptions nằm ở đây)
mvn compile -pl bidhub-common
# ✅ PASS: BUILD SUCCESS
# ❌ FAIL: compile error → kiểm tra import, package name

# Test 2: Chạy test exception hierarchy
mvn test -pl bidhub-common -Dtest="ExceptionHierarchyTest"
# ✅ PASS: "Tests run: 12+, Failures: 0, Errors: 0"

# Test 3: Chạy test tích hợp domain
mvn test -pl bidhub-server -Dtest="DomainIntegrationTest"
# ✅ PASS: "Tests run: 4, Failures: 0, Errors: 0"

# Test 4: Chạy TOÀN BỘ tests (tuần 1 + tuần 2)
mvn test
# ✅ PASS: "Tests run: ≥ 40, Failures: 0, Errors: 0"
# ❌ FAIL: < 40 tests → cần bổ sung thêm test cases

# Test 5: Kiểm tra ValidationException.getErrors() là unmodifiable
mvn test -pl bidhub-common -Dtest="ExceptionHierarchyTest#testValidation_ErrorsList_IsUnmodifiable"
# ✅ PASS

# Test 6: Kiểm tra tổng test mỗi module
mvn test --fail-at-end 2>&1 | grep "Tests run"
# ✅ PASS khi thấy 3 dòng (bidhub-common, bidhub-server, bidhub-client) đều "Failures: 0"

# Test 7: Đếm tổng @Test annotations toàn project
grep -r "@Test" */src/test/ | wc -l
# ✅ PASS: ≥ 40 (bao gồm cả tuần 1)

# Test 8: Kiểm tra không có exception nào thiếu errorCode
grep -r "public.*extends BidHubException" bidhub-common/src/main/
# ✅ PASS: xuất hiện 7 dòng (7 subclass)
```

### ❌ FAIL nếu:
- `new ValidationException("x").getErrors()` trả về `null` → vi phạm contract
- `ex.getErrors().add(...)` không ném `UnsupportedOperationException` → List chưa unmodifiable
- `new InvalidBidException("x").getErrorCode()` không phải `"BID_INVALID"` → hardcode sai
- `new ValidationException(List.of())` không ném exception → thiếu validate
- `mvn test` cho thấy tổng < 40 test cases → cần bổ sung
- `catch (BidHubException e)` không bắt được `AuctionNotFoundException` → exception chưa extends đúng

---

## 🔄 QUY TRÌNH KIỂM TRA CHÉO — CHỦ NHẬT 19/04/2026

### Phân công review

| Người làm | Reviewer 1 | Reviewer 2 |
|-----------|------------|------------|
| **Đăng** (Entity + User hierarchy) | Quốc Minh | Khoa |
| **Quốc Minh** (Item hierarchy + Factory) | Công Minh | Đăng |
| **Công Minh** (Auction + BidTransaction) | Khoa | Quốc Minh |
| **Khoa** (Exception hierarchy + Tests) | Đăng | Công Minh |

### Quy trình review (mỗi người ~30 phút)

#### Bước 1: Pull code về (5 phút)
```bash
git fetch origin
git checkout feature/tuan-2-[ten-nguoi-can-review]
# Ví dụ: git checkout feature/tuan-2-dang-entity-user

# Nếu branch của người đó phụ thuộc bidhub-common chưa merge:
# Tạm thời cherry-pick hoặc checkout cả 2 branches
```

#### Bước 2: Chạy test đầu ra (10 phút)
Chạy từng lệnh trong phần "TEST ĐẦU RA" của người đó. Ghi lại: ✅ PASS hoặc ❌ FAIL.

#### Bước 3: Đọc code và chuẩn bị câu hỏi (10 phút)

**Câu hỏi bắt buộc hỏi Đăng:**
1. Tại sao `Entity` có 2 constructor (1 không có id, 1 có id)? Khi nào dùng cái nào?
2. `markUpdated()` là `protected final` — tại sao lại là `final`? Nếu subclass override `markUpdated()` để không làm gì thì sẽ xảy ra vấn đề gì?
3. `equals()` và `hashCode()` trong Entity dựa trên `id` — nếu không override chúng, điều gì xảy ra khi dùng `Set<User>`?
4. Tại sao `bidhub-common/pom.xml` không có dependency `sqlite-jdbc`? Hậu quả nếu thêm vào là gì?

**Câu hỏi bắt buộc hỏi Quốc Minh:**
1. Phân biệt **Static Factory** (kiểu cũ `ItemFactory.create(type, ...)`) với **Factory Method Pattern GoF** (`ItemCreator` abstract + ConcreteCreator). Tại sao BidHub chuyển sang dùng Creator hierarchy?
2. `ItemCreator.forType(ItemType)` là `static` — có mâu thuẫn với Factory Method Pattern không? Giải thích vai trò của method này.
3. Nếu sau này thêm `ItemType.JEWELRY` — phải tạo những file nào? Không cần sửa những file nào? Đây là Open/Closed Principle thể hiện ở đâu?
4. `ElectronicsCreator`, `ArtCreator`, `VehicleCreator` đều là `final class` — tại sao? Nếu bỏ `final` thì có vấn đề thiết kế gì?

**Câu hỏi bắt buộc hỏi Công Minh:**
1. `AuctionStatus` có abstract method trong enum — Java cho phép điều này không? Giải thích cơ chế.
2. `isValidBid()` kiểm tra `bidAmount > currentHighestBid` — tại sao là `>` không phải `>=`?
3. `BidTransaction` không có setter — tại sao? Có phải mọi class đều nên bất biến không?
4. `extendEndTime()` kiểm tra `newEndTime.isAfter(endTime)` — quan trọng như thế nào với Anti-Sniping tuần 8?

**Câu hỏi bắt buộc hỏi Khoa:**
1. `ValidationException` có 2 constructor — design quyết định này có hợp lý không?
2. Tại sao `getErrors()` trả về `unmodifiableList`? Nếu trả về `ArrayList` thì có vấn đề gì?
3. `DomainIntegrationTest` test cái gì mà `ExceptionHierarchyTest` không test? Tại sao cần cả 2?
4. Tổng số test cases là bao nhiêu? Cách tính để đảm bảo đạt ≥ 40 từ tuần 1 + tuần 2?

#### Bước 4: Comment PR (5 phút)

```markdown
## Review — [Tên reviewer] → [Tên người code]
**Branch:** `feature/tuan-2-[ten]-...`

### ✅ Test Results
- [ ] `mvn compile` → ✅/❌
- [ ] `mvn test -pl bidhub-[module]` → ✅/❌  |  Tests: ___ / Failures: ___ / Errors: ___
- [ ] Tổng test cả project `mvn test` → Tests: ___ (≥ 40?)
- [ ] Kiểm tra cấu trúc file đúng như yêu cầu → ✅/❌

### 🔍 Code Quality
- Naming convention (Google Style): ✅ OK / ⚠️ Cần sửa (chỗ nào?)
- Javadoc tiếng Việt đầy đủ: ✅ / ⚠️ Thiếu (method nào?)
- Validation input trong constructor: ✅ / ⚠️ Thiếu (class nào?)
- Encapsulation — field đều private: ✅ / ⚠️ Có field public?

### ❓ Câu hỏi khi đọc code (thể hiện đã đọc thật sự)
1. [Câu hỏi 1]
2. [Câu hỏi 2]

### 📝 Góp ý cải thiện (nếu có)
-

### Verdict
- [ ] ✅ Approve — code tốt, merge được
- [ ] 🔄 Request changes: [mô tả cần sửa gì cụ thể]
```

---

## 📝 CHECKLIST TỔNG HỢP — KIỂM TRA TRƯỚC KHI NỘP

### Cho tất cả mọi người:
- [ ] Đã đọc xong 4 bài tự học (Abstract class, Factory Pattern, Enum, Exception)
- [ ] Có thể giải thích được code của người khác (ít nhất nắm concept cơ bản)
- [ ] Branch của mình: `mvn compile` pass, `mvn test` pass trên branch local
- [ ] PR đã tạo đúng format `[Tuần 2] Tên - Mô tả ngắn`, gán đúng 2 reviewers

### Checklist Đăng (Entity + User hierarchy):
- [ ] `bidhub-common` module được thêm vào parent `pom.xml` và build thành công
- [ ] `Entity.java` là `abstract class`, KHÔNG thể `new Entity()` trực tiếp
- [ ] `Entity.getId()` là `final` — không subclass nào override được
- [ ] Hai `Bidder` mới tạo có `getId()` khác nhau (UUID không trùng)
- [ ] `new Bidder(null, "hash", "e@x.com")` → `IllegalArgumentException`
- [ ] `new Admin("x", "h", "e@x.com", 0)` → `IllegalArgumentException`
- [ ] `List<User>` với 3 subclass → `getInfo()` trả về 3 chuỗi khác nhau (polymorphism)
- [ ] `bidhub-common/pom.xml` KHÔNG có `sqlite-jdbc` hay `jackson-databind`
- [ ] `bidhub-server/pom.xml` có dependency `bidhub-common` (không khai báo version riêng)

### Checklist Quốc Minh (Item hierarchy + Creator hierarchy):
- [ ] `ItemCreator.forType(ELECTRONICS)` trả về `instanceof ElectronicsCreator`
- [ ] `ItemCreator.forType(ART)` trả về `instanceof ArtCreator`
- [ ] `ItemCreator.forType(VEHICLE)` trả về `instanceof VehicleCreator`
- [ ] `new ElectronicsCreator().createItem(...)` trả về `instanceof Electronics`
- [ ] `new ArtCreator().createItem(...)` trả về `instanceof Art`
- [ ] `new VehicleCreator().createItem(...)` trả về `instanceof Vehicle`
- [ ] `startingPrice = -1` → `IllegalArgumentException`
- [ ] `startingPrice = 0` → `IllegalArgumentException`
- [ ] `ElectronicsCreator` thiếu extras[brand] → `IllegalArgumentException`
- [ ] `ElectronicsCreator`, `ArtCreator`, `VehicleCreator` đều là `final class`
- [ ] `ItemCreator` là `abstract class` — KHÔNG thể `new ItemCreator()` trực tiếp
- [ ] **Không có** `ItemFactory.java` trong codebase
- [ ] `List<Item>` gọi `printInfo()` → 3 output khác nhau (polymorphism qua interface)
- [ ] `Item` là `abstract` — KHÔNG thể `new Item(...)` trực tiếp
- [ ] `item instanceof Entity` = `true` (kế thừa 2 tầng)
- [ ] `ItemCreatorTest.java` có ≥ 14 test cases, tất cả xanh

### Checklist Công Minh (Auction + BidTransaction):
- [ ] `Auction` mới tạo có `status = OPEN`
- [ ] `auction.transitionTo(RUNNING)` thành công
- [ ] `auction.transitionTo(OPEN)` từ RUNNING → `IllegalStateException`
- [ ] `RUNNING.canBid()` = `true`, `FINISHED.canBid()` = `false`
- [ ] `FINISHED.isTerminal()` = `true`, `OPEN.isTerminal()` = `false`
- [ ] `isValidBid(currentHighestBid)` = `false` (phải `>`, không phải `>=`)
- [ ] `isValidBid(currentHighestBid + 1)` khi RUNNING = `true`
- [ ] `BidTransaction` không có setter — fields là `final`
- [ ] `new BidTransaction("a", "b", 0)` → `IllegalArgumentException`
- [ ] `extendEndTime(newEndTime)` check `newEndTime.isAfter(endTime)`

### Checklist Khoa (Exception hierarchy + Tests):
- [ ] `BidHubException` extends `RuntimeException`
- [ ] 7 exception subclass đều `instanceof BidHubException`
- [ ] Mỗi subclass có `errorCode` đúng (7 constants)
- [ ] `ValidationException.getErrors()` không bao giờ null, không rỗng
- [ ] `ValidationException.getErrors().add(...)` → `UnsupportedOperationException`
- [ ] `new ValidationException(List.of())` → `IllegalArgumentException`
- [ ] `catch (BidHubException e)` bắt được tất cả 7 subclass
- [ ] `mvn test` tổng toàn project ≥ 40 test cases, 0 failures
- [ ] Mỗi test method có `@DisplayName` tiếng Việt mô tả rõ hành vi

---

## ⏰ TIMELINE GỢI Ý

| Ngày | Việc cần làm |
|------|-------------|
| **T2 13/04 (Tối)** | Kick-off: phân công, mọi người tạo branch, Đăng bắt đầu Entity + bidhub-common ngay |
| **T3 14/04** | **Đăng push Entity.java lên branch sớm nhất có thể** — cả nhóm clone về, Khoa bắt đầu exceptions |
| **T3-T4 14-15/04** | Đọc tài liệu tự học 4 bài (Abstract class, Factory, Enum, Exception) |
| **T4 15/04** | Quốc Minh + Công Minh bắt đầu task (đã có Entity để kế thừa) |
| **T5 16/04 (Tối)** | Mid-week check-in: demo progress, hỏi nếu stuck, kiểm tra `mvn compile` |
| **T6 17/04** | Hoàn thiện code, tự test theo checklist, xem lại Javadoc |
| **T7 18/04** | **DEADLINE 23:59** — final push, tạo PR format đúng, gán reviewers |
| **CN 19/04 (Sáng)** | Review chéo (mỗi người ~30 phút), trả lời câu hỏi miệng, merge vào `develop` |

> [!TIP]
> **Đăng phải push Entity sớm nhất có thể (Thứ 3 14/04)** — Quốc Minh và Công Minh đều phụ thuộc. Nếu chưa merge được vào develop, tạo tạm một branch `hotfix/entity-base` và gửi link cho cả nhóm checkout.

> [!TIP]
> **Thứ tự làm trong branch:** (1) Viết class → (2) Compile check → (3) Viết test → (4) Chạy test → (5) Commit. Không commit code chưa compile được.

> [!WARNING]
> **Không push code sau 23:59 Thứ 7.** Code push sau deadline không được tính vào review Chủ nhật.

> [!CAUTION]
> **Tuần 3 sẽ dùng thẳng các class tuần này** (UserDao dùng User, Bidder, Seller; AuctionDao dùng Auction, AuctionStatus). Nếu tuần 2 làm sai → toàn bộ tuần 3 bị sai theo. Đây là lý do review Chủ nhật tuần này đặc biệt quan trọng: **merge chỉ khi test xanh hoàn toàn**.
