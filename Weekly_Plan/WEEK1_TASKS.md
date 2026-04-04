# 📋 TUẦN 1 — BÀI TẬP CHI TIẾT: Setup & Infrastructure

> **Kick-off meeting:** Thứ 2 06/04/2026 (tối)
> **Mid-week check-in:** Thứ 5 09/04/2026 (tối)
> **Deadline nộp bài:** Thứ 7 11/04/2026, 23:59
> **Review & Merge:** Chủ nhật 12/04/2026 (sáng)

---

## 🎯 MỤC TIÊU TUẦN 1

Tuần này là nền móng của toàn bộ dự án. Mục tiêu không phải là code nhiều, mà là **thiết lập đúng** để các tuần sau không mất thời gian sửa lại. Cuối tuần 1, cả nhóm phải có:

- ✅ Maven multi-module project `bidhub-server` + `bidhub-client` build được không lỗi
- ✅ CI/CD (GitHub Actions) chạy tự động khi push code
- ✅ `ConfigLoader` đọc được file `.properties`
- ✅ JavaFX `LoginView` mở được trên màn hình
- ✅ JUnit 5 chạy được, ≥ 15 test cases xanh

> [!IMPORTANT]
> Tuần này **trực tiếp phục vụ** tiêu chí barem: **Sử dụng Maven/Gradle, coding convention tốt, mã nguồn sạch (0.5đ)** và **Thiết lập CI/CD cơ bản (0.5đ)**. Làm đúng tuần này = 1.0đ trong tay ngay từ đầu.

---

## 📌 PHẦN CHUNG — AI CŨNG PHẢI LÀM (Tự học)

> [!CAUTION]
> Mỗi người **BẮT BUỘC** phải hoàn thành phần tự học này. Sẽ hỏi bất kỳ ai bất kỳ câu nào trong buổi review Chủ nhật — **không được xem tài liệu khi trả lời**.
>
> Lý do phần tự học tuần 1 được thiết kế để **ai cũng hiểu công việc của nhau**: Đăng cần hiểu Maven để tạo project, nhưng Công Minh cũng phải hiểu Maven để chạy JavaFX. Quốc Minh setup CI, nhưng cả nhóm phải hiểu pipeline đó làm gì.

---

### Bài 0.1 — Cài đặt môi trường

**Yêu cầu cụ thể:**
1. Cài **JDK 21** (Eclipse Temurin): https://adoptium.net → chọn "Temurin 21 (LTS)"
2. Cài **IntelliJ IDEA** (Community hoặc Ultimate): https://jetbrains.com/idea
3. Cài **Scene Builder** cho JavaFX: https://gluonhq.com/products/scene-builder/ → chọn phiên bản 21.x
4. Cài **Git**: https://git-scm.com
5. Cài **Maven** (nếu chưa bundled trong IntelliJ): https://maven.apache.org/download.cgi

**Đầu ra kiểm tra (chụp ảnh screenshots gửi nhóm trước Thứ 4):**
```bash
# Chạy trong terminal/cmd, chụp kết quả gửi nhóm:
java -version      # → openjdk version "21.x.x"
mvn -version       # → Apache Maven 3.9.x
git --version      # → git version 2.x.x
```
- Mở IntelliJ → New Project → Java → chạy được "Hello World"
- Mở Scene Builder → kéo 1 Button vào canvas → Save thành `test.fxml`

---

### Bài 0.2 — Git cơ bản & workflow nhóm

**Tài liệu bắt buộc:**
- Chơi game Git: https://learngitbranching.js.org (hoàn thành Main > Introduction Sequence)
- Đọc Conventional Commits: https://www.conventionalcommits.org/en/v1.0.0/

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. `git add` vs `git commit` khác nhau thế nào?
2. `git pull` vs `git fetch` — khi nào dùng cái nào an toàn hơn?
3. Làm sao tạo branch mới, push lên GitHub, tạo Pull Request?
4. Conflict xảy ra khi nào? Cách giải quyết từng bước?
5. Tại sao **không** push thẳng lên `main`? Branch protection là gì?
6. Conventional Commits: `feat:`, `fix:`, `docs:`, `test:` — cái nào dùng khi nào? Ví dụ thực tế?
7. Trong project này, tại sao có cả `main` và `develop`? Merge vào đâu khi code xong?

**Bài thực hành Git (mỗi người tự làm trước Thứ 4):**
```bash
# 1. Clone repo nhóm (Quốc Minh sẽ gửi URL sau khi setup)
git clone https://github.com/[your-org]/BidHub.git

# 2. Tạo branch thực hành cá nhân
git checkout -b practice/tuan-1-[TEN-BAN]
# Ví dụ: practice/tuan-1-dang

# 3. Tạo file members/[TEN-BAN].md với nội dung:
#    - Họ tên đầy đủ, MSSV, vai trò trong nhóm
#    - Công cụ đã cài (JDK version, OS)
#    - 1 câu tự giới thiệu

# 4. Commit đúng format
git add members/[TEN-BAN].md
git commit -m "docs: thêm thông tin thành viên [TEN]"

# 5. Push và tạo PR
git push origin practice/tuan-1-[TEN-BAN]
# Vào GitHub → New Pull Request → gán 2 reviewers
```

---

### Bài 0.3 — Maven multi-module project

**Tài liệu bắt buộc:**
- https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html
- https://maven.apache.org/guides/introduction/introduction-to-the-pom.html

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. `pom.xml` là gì? `groupId`, `artifactId`, `version` nghĩa là gì?
2. `<dependencyManagement>` khác `<dependencies>` thế nào? Tại sao dùng trong parent POM?
3. Tại sao project này cần 2 module (`bidhub-server` + `bidhub-client`)? Không tách thì sao?
4. `mvn compile`, `mvn test`, `mvn package`, `mvn clean` — mỗi lệnh làm gì?
5. Maven standard layout: `src/main/java` vs `src/test/java` — sự khác biệt?
6. **[Câu hỏi nâng cao]** Tại sao `bidhub-client` cần `javafx-maven-plugin` mà `bidhub-server` không cần?

---

### Bài 0.4 — JavaFX cơ bản & FXML

**Tài liệu bắt buộc:**
- https://openjfx.io/openjfx-docs/ (phần "Getting Started")
- Xem 1 video "JavaFX FXML tutorial" trên YouTube (~20 phút)

**Đầu ra kiểm tra (hỏi miệng Chủ nhật):**
1. JavaFX `Stage`, `Scene`, `Node` là gì? Mối quan hệ giữa chúng?
2. `FXML` là gì? Tại sao dùng FXML thay vì layout bằng Java code thuần?
3. `@FXML` annotation hoạt động như thế nào? Kết nối với controller qua cơ chế gì?
4. `initialize()` method trong controller được gọi khi nào? Dùng để làm gì?
5. **[CỰC KỲ QUAN TRỌNG]** Tại sao KHÔNG được cập nhật UI từ background thread? Phải làm gì thay thế (`Platform.runLater()`)?
6. `FXMLLoader.load()` trả về gì? Làm sao set controller cho FXML?

---

## 🔨 PHẦN CÁ NHÂN — NHIỆM VỤ RIÊNG

> [!IMPORTANT]
> Mỗi người code trên **branch riêng**, KHÔNG push thẳng vào `main` hay `develop`. Đến Chủ nhật mới tạo PR để review và merge.
>
> **Phụ thuộc tuần này:** Đăng tạo project structure trước → push lên `develop` sớm nhất có thể (Thứ 3) → Công Minh và Quốc Minh mới clone về và bắt đầu task của mình được.

---

## 👤 ĐĂNG — Maven Multi-Module Setup & ConfigLoader

```
Branch: feature/tuan-1-dang-maven-setup
Phụ thuộc: Không phụ thuộc ai — LÀM TRƯỚC, push sớm nhất có thể
Deadline: Thứ 4 08/04 (để cả nhóm có project mà clone)
```

📌 **[Tiêu chí điểm: Sử dụng Maven/Gradle, coding convention tốt — 0.5đ] + [CI/CD GitHub Actions — 0.5đ]**

### 📝 Mô tả bài tập

Bạn là người tạo **nền móng** cho toàn bộ dự án. Nhiệm vụ là khởi tạo Maven multi-module project với cấu trúc chuẩn, sao cho tất cả thành viên clone về và `mvn compile` thành công ngay lập tức mà không cần cấu hình thêm.

### 📁 Cấu trúc file cần tạo

```
BidHub/                                    ← root của repo
├── pom.xml                                ← Parent POM
├── .gitignore
├── bidhub-server/                         ← Module server
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/bidhub/server/
│       │   │   └── ServerApp.java         ← Entry point (stub)
│       │   └── resources/
│       │       └── server.properties      ← Config file
│       └── test/java/com/bidhub/server/
│           └── ServerAppTest.java
└── bidhub-client/                         ← Module client
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/bidhub/client/
        │   │   └── BidHubApp.java         ← JavaFX entry point (stub)
        │   └── resources/
        │       └── client.properties      ← Config client
        └── test/java/com/bidhub/client/
            └── (trống — test GUI khó tự động)
```

### 📋 Yêu cầu chi tiết

#### Bước 1 — Parent POM (`pom.xml` ở thư mục gốc)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.bidhub</groupId>
  <artifactId>bidhub-parent</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>BidHub Parent</name>

  <modules>
    <module>bidhub-server</module>
    <module>bidhub-client</module>
  </modules>

  <properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- Versions tập trung ở đây — không viết lại trong module con -->
    <junit.version>5.10.2</junit.version>
    <jackson.version>2.17.1</jackson.version>
    <sqlite.version>3.45.3.0</sqlite.version>
    <javafx.version>21.0.3</javafx.version>
  </properties>

  <!--
    dependencyManagement: khai báo version một lần duy nhất ở đây.
    Module con chỉ cần groupId + artifactId, không cần version.
    Tránh version conflict giữa các module.
  -->
  <dependencyManagement>
    <dependencies>
      <!-- JUnit 5 BOM — import để tự động quản lý version toàn bộ JUnit modules -->
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>${junit.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>

      <!-- Jackson — dùng cho JSON serialization/deserialization (thay Gson) -->
      <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>${jackson.version}</version>
      </dependency>

      <!-- SQLite JDBC -->
      <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>${sqlite.version}</version>
      </dependency>

      <!-- JavaFX -->
      <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>${javafx.version}</version>
      </dependency>
      <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>${javafx.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.13.0</version>
          <configuration>
            <source>${java.version}</source>
            <target>${java.version}</target>
            <encoding>UTF-8</encoding>
          </configuration>
        </plugin>
        <!-- Surefire 3.x mới chạy đúng JUnit 5 (Surefire 2.x chỉ JUnit 4) -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.2.5</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

> 💡 **Tại sao Jackson mà không phải Gson?** Jackson thread-safe với `static ObjectMapper` — rất quan trọng khi server xử lý nhiều request đồng thời (tuần 7). Gson không thread-safe, dùng là bug tiềm ẩn.

```bash
git add pom.xml
git commit -m "build: khởi tạo maven multi-module parent pom với jackson và junit 5"
```

---

#### Bước 2 — Module `bidhub-server` POM

```xml
<!-- bidhub-server/pom.xml -->
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

  <artifactId>bidhub-server</artifactId>
  <name>BidHub Server</name>

  <dependencies>
    <!-- Jackson — JSON protocol giữa client và server -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- SQLite — database nhúng, không cần cài server riêng -->
    <dependency>
      <groupId>org.xerial</groupId>
      <artifactId>sqlite-jdbc</artifactId>
    </dependency>

    <!-- JUnit 5 — chỉ dùng khi test, không đóng vào production JAR -->
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

---

#### Bước 3 — `ConfigLoader.java`

```java
package com.bidhub.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Đọc cấu hình từ file {@code server.properties} trong classpath.
 *
 * <p>Sử dụng Java standard {@link Properties} — không cần thư viện ngoài.
 * Đủ đơn giản, dễ test, và giảng viên có thể đọc hiểu ngay.
 *
 * <p>Cách dùng:
 * <pre>{@code
 * int port = ConfigLoader.getInt("server.port");          // → 9090
 * String dbPath = ConfigLoader.getString("db.path");      // → "data/bidhub.db"
 * }</pre>
 *
 * <p><b>Lưu ý:</b> Không dùng Singleton ở đây vì Properties được đọc
 * một lần khi class được load (static initializer). Đủ đơn giản và đúng.
 */
public final class ConfigLoader {

  /** Đường dẫn file config trong classpath (src/main/resources/) */
  private static final String CONFIG_FILE = "server.properties";

  /** Properties được load một lần duy nhất khi class được khởi tạo */
  private static final Properties PROPS = new Properties();

  static {
    // Static initializer: chạy một lần khi class được load vào JVM
    try (InputStream in = ConfigLoader.class
        .getClassLoader()
        .getResourceAsStream(CONFIG_FILE)) {

      if (in == null) {
        throw new IllegalStateException(
            "Không tìm thấy file config: " + CONFIG_FILE
                + " trong classpath. Kiểm tra src/main/resources/");
      }
      PROPS.load(in);

    } catch (IOException e) {
      throw new IllegalStateException("Lỗi đọc file config: " + CONFIG_FILE, e);
    }
  }

  /** Ngăn khởi tạo — class này chỉ có static methods */
  private ConfigLoader() {}

  /**
   * Đọc giá trị String từ config.
   *
   * @param key khóa trong file .properties (ví dụ: "server.port")
   * @return giá trị String tương ứng
   * @throws IllegalArgumentException nếu key không tồn tại trong file config
   */
  public static String getString(String key) {
    String value = PROPS.getProperty(key);
    if (value == null) {
      throw new IllegalArgumentException(
          "Không tìm thấy key '" + key + "' trong " + CONFIG_FILE);
    }
    return value.trim();
  }

  /**
   * Đọc giá trị int từ config.
   *
   * @param key khóa trong file .properties
   * @return giá trị int tương ứng
   * @throws IllegalArgumentException nếu key không tồn tại hoặc không phải số
   */
  public static int getInt(String key) {
    String value = getString(key);
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Giá trị của key '" + key + "' không phải số nguyên: '" + value + "'");
    }
  }

  /**
   * Đọc giá trị String từ config, trả về giá trị mặc định nếu key không tồn tại.
   *
   * @param key          khóa trong file .properties
   * @param defaultValue giá trị mặc định nếu key không có
   * @return giá trị từ config hoặc defaultValue
   */
  public static String getOrDefault(String key, String defaultValue) {
    String value = PROPS.getProperty(key);
    return (value != null) ? value.trim() : defaultValue;
  }
}
```

#### Bước 4 — `server.properties`

```properties
# server.properties — đặt trong bidhub-server/src/main/resources/
# Đây là config của SERVER (không commit thông tin nhạy cảm vào Git)

# Cổng lắng nghe kết nối từ client
server.port=9090

# Đường dẫn file SQLite database (tương đối từ thư mục chạy server)
db.path=data/bidhub.db

# Anti-Sniping: bid trong X giây cuối sẽ gia hạn thêm Y giây
snipe.threshold=60
snipe.extension=60
```

```bash
git add bidhub-server/
git commit -m "feat: khởi tạo module bidhub-server với configloader và server.properties"
```

---

#### Bước 5 — `ServerApp.java`

```java
package com.bidhub.server;

import com.bidhub.server.config.ConfigLoader;

/**
 * Entry point của BidHub Server.
 *
 * <p>Tuần 1: Chỉ in thông tin khởi động, kiểm tra ConfigLoader hoạt động.
 * Socket server sẽ được implement ở Tuần 4.
 */
public class ServerApp {

  public static final String APP_NAME = "BidHub Server";
  public static final String VERSION = "1.0-SNAPSHOT";

  /**
   * Trả về welcome message — dùng trong test để không cần chạy main().
   *
   * @return chuỗi thông báo khởi động
   */
  public static String getWelcomeMessage() {
    return APP_NAME + " v" + VERSION + " — Hệ thống đấu giá trực tuyến";
  }

  /**
   * Entry point chính. Đọc port từ config và in ra.
   *
   * @param args tham số dòng lệnh (không dùng ở tuần 1)
   */
  public static void main(String[] args) {
    System.out.println(getWelcomeMessage());
    int port = ConfigLoader.getInt("server.port");
    System.out.println("Cổng lắng nghe: " + port);
    System.out.println("Database: " + ConfigLoader.getString("db.path"));
    System.out.println("Server sẵn sàng. Socket server sẽ implement tuần 4.");
  }
}
```

#### Test: `ServerAppTest.java`

```java
package com.bidhub.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra cơ bản ServerApp và ConfigLoader */
class ServerAppTest {

  @Test
  @DisplayName("Welcome message không null và chứa đúng tên app")
  void testGetWelcomeMessage_ContainsAppName() {
    // Arrange + Act
    String message = ServerApp.getWelcomeMessage();

    // Assert
    assertNotNull(message, "Message không được null");
    assertTrue(message.contains("BidHub Server"), "Phải chứa tên app");
    assertTrue(message.contains("1.0-SNAPSHOT"), "Phải chứa version");
  }

  @Test
  @DisplayName("ConfigLoader đọc được server.port từ file properties")
  void testConfigLoader_ReadServerPort() {
    int port = ConfigLoader.getInt("server.port");
    assertEquals(9090, port, "Port mặc định phải là 9090");
  }

  @Test
  @DisplayName("ConfigLoader đọc được db.path từ file properties")
  void testConfigLoader_ReadDbPath() {
    String dbPath = ConfigLoader.getString("db.path");
    assertNotNull(dbPath);
    assertFalse(dbPath.isBlank(), "db.path không được rỗng");
    assertTrue(dbPath.contains(".db"), "db.path phải chứa đuôi .db");
  }

  @Test
  @DisplayName("ConfigLoader getOrDefault trả về default khi key không tồn tại")
  void testConfigLoader_GetOrDefault_KeyNotExist() {
    String result = ConfigLoader.getOrDefault("key.khong.ton.tai", "default-value");
    assertEquals("default-value", result);
  }

  @Test
  @DisplayName("ConfigLoader getString ném exception khi key không tồn tại")
  void testConfigLoader_GetString_ThrowsOnMissingKey() {
    assertThrows(IllegalArgumentException.class,
        () -> ConfigLoader.getString("key.khong.co.trong.file"),
        "Phải ném IllegalArgumentException với key không tồn tại");
  }

  @Test
  @DisplayName("ConfigLoader getInt ném exception khi giá trị không phải số")
  void testConfigLoader_GetInt_ThrowsOnNonNumericValue() {
    // db.path là String, không phải số → phải throw
    assertThrows(IllegalArgumentException.class,
        () -> ConfigLoader.getInt("db.path"),
        "Phải ném exception khi value không phải số nguyên");
  }
}
```

```bash
git add bidhub-server/src/
git commit -m "test: thêm ServerAppTest kiểm tra configloader getString và getInt"
```

---

#### Bước 6 — Module `bidhub-client` POM

```xml
<!-- bidhub-client/pom.xml -->
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

  <artifactId>bidhub-client</artifactId>
  <name>BidHub Client</name>

  <dependencies>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
    </dependency>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-fxml</artifactId>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- Plugin bắt buộc để chạy: mvn javafx:run -pl bidhub-client -->
      <plugin>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>0.0.8</version>
        <configuration>
          <mainClass>com.bidhub.client.BidHubApp</mainClass>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

```bash
git add bidhub-client/pom.xml
git commit -m "build: thêm module bidhub-client với javafx và jackson"
```

---

#### Bước 7 — `.gitignore`

```gitignore
# Maven build output
target/
*.class

# IntelliJ IDEA
.idea/
*.iml
*.iws
*.ipr
out/

# OS files
.DS_Store
Thumbs.db

# SQLite database files (không commit data vào Git)
*.db
*.db-shm
*.db-wal
data/

# Log files
*.log
logs/
```

```bash
git add .gitignore
git commit -m "chore: thêm gitignore chuẩn cho maven + intellij + sqlite"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Đăng

```bash
# Test 1: Compile toàn bộ project từ thư mục gốc
mvn compile
# ✅ PASS: "[INFO] BUILD SUCCESS" cho cả 2 modules
# ❌ FAIL: Bất kỳ dòng nào có "[ERROR]"

# Test 2: Chạy test ServerApp
mvn test -pl bidhub-server
# ✅ PASS: "Tests run: 6, Failures: 0, Errors: 0"
# ❌ FAIL: Tests run < 6 hoặc có Failures/Errors

# Test 3: Test một case cụ thể
mvn test -pl bidhub-server -Dtest="ServerAppTest#testConfigLoader_ReadServerPort"
# ✅ PASS: Test PASSED, in ra "9090"

# Test 4: Kiểm tra module con KHÔNG tự khai báo version
grep "<version>" bidhub-server/pom.xml
# ✅ PASS: Chỉ thấy version của parent, Jackson/SQLite/JUnit không có version riêng
# ❌ FAIL: Thấy "<version>2.17.1</version>" hoặc tương tự trong module con

# Test 5: Build package (JAR)
mvn clean package -pl bidhub-server -DskipTests
# ✅ PASS: "bidhub-server-1.0-SNAPSHOT.jar" xuất hiện trong target/

# Test 6: Chạy ServerApp (tuần 1 chỉ print, chưa socket)
mvn -pl bidhub-server exec:java -Dexec.mainClass="com.bidhub.server.ServerApp"
# ✅ PASS: In ra "BidHub Server v1.0-SNAPSHOT" và "Cổng lắng nghe: 9090"
# ❌ FAIL: Exception hoặc "Không tìm thấy file config"
```

### ❌ FAIL nếu:
- `mvn compile` lỗi ở bất kỳ module nào
- Module con (`bidhub-server/pom.xml`) tự khai báo `<version>` của Jackson/JUnit → vi phạm `dependencyManagement`
- `ConfigLoader` dùng hardcode `"server.properties"` thay vì `getResourceAsStream` → không tìm thấy file khi chạy từ CLI
- `ConfigLoader` constructor là `public` → có thể `new ConfigLoader()` → sai thiết kế
- Thiếu file `.gitignore` hoặc `target/` bị commit vào repo

---

## 👤 QUỐC MINH — Git Workflow, CI/CD & API Protocol Skeleton

```
Branch: feature/tuan-1-quocminh-git-cicd
Phụ thuộc: Đợi Đăng tạo project structure (lấy repo URL từ Đăng)
Deadline: Thứ 7 11/04 23:59
```

📌 **[Tiêu chí điểm: CI/CD GitHub Actions — 0.5đ]**

### 📝 Mô tả bài tập

Bạn setup **hạ tầng Git chuyên nghiệp**: CI tự động, PR template, workflow rõ ràng. Đây là thứ mà giảng viên nhìn vào GitHub sẽ đánh giá ngay về chất lượng nhóm. Ngoài ra, bạn viết phác thảo API Protocol — tài liệu quan trọng giúp cả nhóm code đúng format JSON từ Tuần 4.

### 📁 Cấu trúc file cần tạo

```
BidHub/
├── .github/
│   ├── workflows/
│   │   └── ci.yml                       ← Pipeline CI/CD tự động
│   ├── pull_request_template.md         ← Template khi tạo PR
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       └── feature_request.md
├── docs/
│   ├── API_PROTOCOL.md                  ← Phác thảo format JSON (quan trọng!)
│   └── screenshots/                     ← Ảnh chụp branch protection rules
├── CONTRIBUTING.md                      ← Hướng dẫn đóng góp code
└── README.md                            ← Mô tả project, hướng dẫn chạy
```

### 📋 Yêu cầu chi tiết

#### `.github/workflows/ci.yml`

```yaml
# BidHub CI Pipeline
# Trigger: tự động chạy khi push hoặc tạo PR vào develop hoặc main
# Mục tiêu: Đảm bảo code luôn compile và test pass trước khi merge

name: Java CI

on:
  push:
    branches: [ develop, main ]
  pull_request:
    branches: [ develop, main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    name: Build & Test (Java 21)

    steps:
      # Bước 1: Checkout code từ branch hiện tại
      - name: Checkout source code
        uses: actions/checkout@v4

      # Bước 2: Cài JDK 21 (Temurin — cùng version với local development)
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      # Bước 3: Cache Maven dependencies (~/.m2)
      # Cache giúp không download lại hàng chục MB mỗi lần CI chạy
      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-m2-

      # Bước 4: Build và chạy tất cả tests
      # --no-transfer-progress: tắt progress bar dài dòng trong CI log
      - name: Build and run tests
        run: mvn --no-transfer-progress verify

      # Bước 5: Upload kết quả test (Surefire reports) để xem chi tiết khi fail
      - name: Upload test reports
        uses: actions/upload-artifact@v4
        if: always()    # Upload dù pass hay fail (để debug)
        with:
          name: surefire-reports
          path: |
            bidhub-server/target/surefire-reports/
            bidhub-client/target/surefire-reports/
          retention-days: 7
```

```bash
git add .github/workflows/ci.yml
git commit -m "ci: thêm github actions pipeline build và test tự động trên java 21"
```

---

#### `CONTRIBUTING.md`

```markdown
# Hướng dẫn đóng góp code — BidHub

## Quy trình làm việc

### 1. Lấy task từ tuần hiện tại

Đọc file `WEEK[X]_TASKS.md` trong repo → tìm task của bạn → tạo branch.

### 2. Tạo branch đúng format

```bash
git checkout develop
git pull origin develop
git checkout -b feature/tuan-[X]-[ten]-[mo-ta-ngan]

# Ví dụ:
# feature/tuan-1-dang-maven-setup
# feature/tuan-3-quocminh-user-dao
# feature/tuan-6-congminh-auction-detail-view
```

### 3. Code và commit thường xuyên

Commit mỗi khi hoàn thành 1 phần nhỏ (1 class, 1 tính năng nhỏ):

```bash
git add [file cụ thể, không dùng git add .]
git commit -m "[type]: [mô tả ngắn gọn tiếng Việt/Anh]"
```

**Các loại commit (Conventional Commits):**

| Type     | Dùng khi                                      | Ví dụ                                       |
|----------|-----------------------------------------------|---------------------------------------------|
| `feat`   | Thêm tính năng mới                            | `feat: thêm class BidValidator`             |
| `fix`    | Sửa bug                                       | `fix: sửa lỗi null pointer trong UserDao`   |
| `test`   | Thêm/sửa test                                 | `test: thêm test cho AuctionStatus enum`    |
| `docs`   | Sửa tài liệu, Javadoc, README                 | `docs: cập nhật API_PROTOCOL.md`            |
| `refactor` | Refactor code không thay đổi behavior       | `refactor: tách BidService thành interface` |
| `build`  | Thay đổi build system (pom.xml, .github)      | `build: thêm dependency jackson`            |
| `chore`  | Việc lặt vặt (gitignore, cấu hình IDE)        | `chore: cập nhật .gitignore`                |

### 4. Tạo Pull Request trước deadline

```bash
git push origin feature/tuan-X-[ten]-[mo-ta]
# Vào GitHub → Compare & Pull Request
# Title: [Tuần X] Tên người - Mô tả ngắn
# Gán 2 reviewers
```

### 5. Review và merge

- Tối thiểu **1 người approve** trước khi merge
- KHÔNG merge nếu CI fail (badge đỏ)
- Merge vào `develop`, KHÔNG merge vào `main`
- Merge vào `main` chỉ vào cuối Tuần 4, 7, 10 (milestone)

## Quy tắc branch

```
main          ← production-ready, chỉ merge develop vào cuối milestone
  └── develop ← integration branch, merge feature vào đây
        ├── feature/tuan-1-dang-maven-setup
        ├── feature/tuan-1-quocminh-git-cicd
        ├── feature/tuan-1-congminh-javafx-skeleton
        └── feature/tuan-1-khoa-junit-convention
```

## Quy tắc KHÔNG được làm

- ❌ Push thẳng vào `main` hoặc `develop` (vi phạm branch protection)
- ❌ Merge PR của chính mình (phải có ít nhất 1 người khác approve)
- ❌ Commit file binary lớn (`.jar`, `.db`, ảnh, video) vào repo
- ❌ Commit `target/` (đã có trong `.gitignore`)
- ❌ Code chưa compile hoặc test đang fail → push lên → làm CI fail của cả nhóm
```

```bash
git add CONTRIBUTING.md
git commit -m "docs: thêm hướng dẫn đóng góp code với conventional commits và quy trình branch"
```

---

#### `.github/pull_request_template.md`

```markdown
## Mô tả thay đổi

<!-- Tóm tắt ngắn gọn bạn đã làm gì trong PR này -->

**Loại thay đổi:**
- [ ] ✨ feat — Tính năng mới
- [ ] 🐛 fix — Sửa bug
- [ ] ♻️ refactor — Refactor
- [ ] 📝 docs — Tài liệu
- [ ] ✅ test — Test

## Checklist trước khi review

- [ ] `mvn compile` thành công trên branch này
- [ ] `mvn test` pass (0 failures)
- [ ] Code theo Google Java Style Guide (indent 2 spaces, tên camelCase)
- [ ] Javadoc đầy đủ cho class và method public
- [ ] Không có `System.out.println()` dư thừa (debug print)
- [ ] Không commit `target/`, `.idea/`, `*.db`

## Test đã viết

<!-- Liệt kê các test case đã thêm hoặc sửa -->
- Test 1: ...
- Test 2: ...

## Ảnh chụp màn hình (nếu có UI)

<!-- Kéo thả ảnh vào đây nếu bạn thay đổi UI -->

## Liên kết issue (nếu có)

Closes #[số issue]
```

---

#### `docs/API_PROTOCOL.md` (Skeleton quan trọng)

```markdown
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
```

```bash
git add docs/ .github/ README.md CONTRIBUTING.md
git commit -m "docs: thêm api protocol skeleton, contributing guide, và pr template"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Quốc Minh

```bash
# Test 1: CI tự động kích hoạt khi push
git push origin feature/tuan-1-quocminh-git-cicd
# ✅ PASS: Vào GitHub → Actions tab → thấy workflow đang chạy (màu vàng)
# Sau 1-2 phút: workflow chuyển sang xanh lá ✅

# Test 2: PR template hoạt động
# → Vào GitHub → New Pull Request → ✅ PASS nếu ô Description tự điền template

# Test 3: Kiểm tra CI cache hoạt động
# Chờ 2 lần CI chạy → lần 2 phải nhanh hơn lần 1 (~30-60s vs ~3-5 phút)
# ✅ PASS: Log CI lần 2 có dòng "Cache restored from key: ..."

# Test 4: Kiểm tra các file tồn tại
ls .github/workflows/ci.yml CONTRIBUTING.md docs/API_PROTOCOL.md
# ✅ PASS: Không có "No such file or directory"

# Test 5: Kiểm tra API_PROTOCOL.md có đủ ví dụ
grep -c '```json' docs/API_PROTOCOL.md
# ✅ PASS: ≥ 6 (tức ≥ 3 cặp request/response JSON)

# Test 6: CI fail đúng cách khi code lỗi
# Tạo 1 file Java syntax error → push lên → CI phải đỏ (FAIL)
# Sửa lại → push lên → CI phải xanh (PASS)
# ✅ PASS: CI phát hiện được cả compile error lẫn test failure
```

### ❌ FAIL nếu:
- CI workflow không trigger khi push lên branch (kiểm tra triggers `on: push/pull_request`)
- CI không cache Maven → mỗi lần chạy mất 3-5 phút download dependencies
- `API_PROTOCOL.md` chỉ có tiêu đề rỗng, không có ví dụ JSON thực tế
- PR template không xuất hiện khi tạo PR mới (kiểm tra đường dẫn file)

---

## 👤 CÔNG MINH — JavaFX App Skeleton & LoginView

```
Branch: feature/tuan-1-congminh-javafx-skeleton
Phụ thuộc: Cần Đăng push project structure trước (để có pom.xml mà làm)
Deadline: Thứ 7 11/04 23:59
```

📌 **[Tiêu chí điểm: Giao diện người dùng GUI (JavaFX) — 0.5đ nằm trong MVC]**

### 📝 Mô tả bài tập

Bạn tạo **khung sườn GUI** cho client: entry point JavaFX, màn hình Login đầu tiên, và hệ thống điều hướng màn hình (`ViewRouter` + `Views`). Tuần này chưa có networking — chỉ cần UI hiển thị đúng và Scene Builder mở được không lỗi.

### 📁 Cấu trúc file cần tạo

```
bidhub-client/src/main/
├── java/com/bidhub/client/
│   ├── BidHubApp.java                   ← JavaFX Application entry point
│   ├── Views.java                       ← Constants tên màn hình
│   └── controller/
│       └── LoginController.java         ← Controller cho màn hình Login
└── resources/
    ├── fxml/
    │   └── LoginView.fxml               ← Layout XML cho màn hình Login
    ├── css/
    │   └── styles.css                   ← CSS cơ bản (có thể để trống)
    └── client.properties                ← Config phía client
```

### 📋 Yêu cầu chi tiết

#### `Views.java` — Constants tên màn hình

```java
package com.bidhub.client;

/**
 * Hằng số tên các màn hình (FXML views) trong ứng dụng.
 *
 * <p>Sử dụng constants thay vì hardcode string để tránh typo
 * khi gọi {@code ViewRouter.navigateTo()}.
 *
 * <p>Quy ước: Tên constant = tên file FXML (không có đuôi .fxml).
 * File FXML đặt trong {@code resources/fxml/}.
 */
public final class Views {

  /** Màn hình đăng nhập — màn hình đầu tiên khi mở app */
  public static final String LOGIN = "LoginView";

  /** Danh sách tất cả phiên đấu giá đang mở */
  public static final String AUCTION_LIST = "AuctionListView";

  /** Chi tiết phiên đấu giá + realtime bidding */
  public static final String AUCTION_DETAIL = "AuctionDetailView";

  /** Form tạo item mới — chỉ dành cho Seller */
  public static final String CREATE_ITEM = "CreateItemView";

  /** Form tạo phiên đấu giá — chỉ dành cho Seller */
  public static final String CREATE_AUCTION = "CreateAuctionView";

  /** Màn hình đăng ký tài khoản mới */
  public static final String REGISTER = "RegisterView";

  /** Ngăn khởi tạo class này */
  private Views() {}
}
```

---

#### `BidHubApp.java` — JavaFX Entry Point

```java
package com.bidhub.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Entry point của BidHub Client — JavaFX Application.
 *
 * <p>Chịu trách nhiệm:
 * <ul>
 *   <li>Khởi tạo JavaFX Application lifecycle</li>
 *   <li>Load màn hình đầu tiên (LoginView)</li>
 *   <li>Cài đặt kích thước và tiêu đề cửa sổ</li>
 * </ul>
 *
 * <p>Networking (kết nối server) sẽ được thêm vào Tuần 4.
 */
public class BidHubApp extends Application {

  /** Kích thước mặc định của cửa sổ */
  private static final int WINDOW_WIDTH = 1024;
  private static final int WINDOW_HEIGHT = 720;

  /** Tiêu đề hiển thị trên thanh title bar */
  private static final String APP_TITLE = "BidHub — Hệ thống đấu giá trực tuyến";

  /**
   * JavaFX gọi method này khi Application khởi động.
   * Đây là nơi setup Stage và load màn hình đầu tiên.
   *
   * @param primaryStage Stage chính, được JavaFX tạo sẵn
   * @throws IOException nếu không load được file FXML
   */
  @Override
  public void start(Stage primaryStage) throws IOException {
    // Load LoginView.fxml từ resources/fxml/
    URL fxmlUrl = getClass().getResource("/fxml/LoginView.fxml");
    if (fxmlUrl == null) {
      throw new IllegalStateException(
          "Không tìm thấy /fxml/LoginView.fxml trong resources. "
          + "Kiểm tra bidhub-client/src/main/resources/fxml/");
    }

    Parent root = FXMLLoader.load(fxmlUrl);

    // Tạo Scene với kích thước mặc định
    Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

    // Gắn CSS (file có thể trống — sẽ style sau)
    URL cssUrl = getClass().getResource("/css/styles.css");
    if (cssUrl != null) {
      scene.getStylesheets().add(cssUrl.toExternalForm());
    }

    primaryStage.setTitle(APP_TITLE);
    primaryStage.setScene(scene);
    primaryStage.setResizable(true);
    primaryStage.show();
  }

  /**
   * Điểm khởi động của JVM.
   *
   * @param args tham số dòng lệnh (không dùng ở tuần 1)
   */
  public static void main(String[] args) {
    launch(args);
  }
}
```

---

#### `LoginView.fxml` — Layout màn hình Login

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.text.Text?>

<!--
    LoginView.fxml — Màn hình đăng nhập BidHub
    Controller: com.bidhub.client.controller.LoginController

    Cấu trúc layout:
    VBox (root, căn giữa màn hình)
      └── VBox (form card, tối đa 360px)
            ├── Text (tiêu đề "BidHub")
            ├── TextField (username)
            ├── PasswordField (password)
            ├── Label (hiển thị lỗi, ẩn mặc định)
            ├── Button (Đăng nhập)
            └── Hyperlink (Chưa có tài khoản? Đăng ký)
-->
<VBox alignment="CENTER"
      spacing="0"
      xmlns="http://javafx.com/javafx/21"
      xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.bidhub.client.controller.LoginController">

  <VBox alignment="TOP_CENTER"
        spacing="16"
        maxWidth="360"
        style="-fx-background-color: white; -fx-padding: 40; -fx-background-radius: 8;
               -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);">

    <!-- Tiêu đề -->
    <Text text="🔨 BidHub"
          style="-fx-font-size: 28px; -fx-font-weight: bold; -fx-fill: #2563EB;"/>
    <Text text="Hệ thống đấu giá trực tuyến"
          style="-fx-font-size: 14px; -fx-fill: #6B7280;"/>

    <VBox spacing="8" VBox.vgrow="NEVER">
      <Label text="Tên đăng nhập" style="-fx-font-weight: bold; -fx-font-size: 13px;"/>
      <!-- fx:id kết nối với @FXML field trong LoginController -->
      <TextField fx:id="usernameField"
                 promptText="Nhập tên đăng nhập"
                 style="-fx-pref-height: 40px;"/>
    </VBox>

    <VBox spacing="8">
      <Label text="Mật khẩu" style="-fx-font-weight: bold; -fx-font-size: 13px;"/>
      <PasswordField fx:id="passwordField"
                     promptText="Nhập mật khẩu"
                     style="-fx-pref-height: 40px;"/>
    </VBox>

    <!-- Label lỗi: ẩn mặc định, hiện khi đăng nhập thất bại -->
    <Label fx:id="errorLabel"
           text=""
           visible="false"
           wrapText="true"
           style="-fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-padding: 8;
                  -fx-background-color: #FEF2F2; -fx-background-radius: 4;"/>

    <!-- Button đăng nhập -->
    <Button fx:id="loginButton"
            text="Đăng nhập"
            onAction="#handleLogin"
            maxWidth="Infinity"
            style="-fx-background-color: #2563EB; -fx-text-fill: white;
                   -fx-font-size: 14px; -fx-pref-height: 42px; -fx-cursor: hand;
                   -fx-background-radius: 6;"/>

    <Separator/>

    <Hyperlink text="Chưa có tài khoản? Đăng ký"
               onAction="#handleRegister"
               style="-fx-font-size: 13px;"/>

    <VBox.margin>
      <Insets top="20"/>
    </VBox.margin>
  </VBox>

  <!-- Background màu nhạt cho toàn màn hình -->
  <style>
    -fx-background-color: #F3F4F6;
  </style>
</VBox>
```

---

#### `LoginController.java` — Controller cho LoginView

```java
package com.bidhub.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller cho màn hình đăng nhập (LoginView.fxml).
 *
 * <p>Tuần 1: Chỉ có skeleton, chưa có logic kết nối server.
 * Logic thật (gọi API Login) sẽ được implement ở Tuần 5.
 *
 * <p>Pattern: JavaFX MVC
 * <ul>
 *   <li>View: LoginView.fxml</li>
 *   <li>Controller: LoginController (class này)</li>
 *   <li>Model: sẽ có User, ClientSession ở tuần 5</li>
 * </ul>
 */
public class LoginController {

  /** TextField nhập tên đăng nhập — kết nối từ FXML qua fx:id="usernameField" */
  @FXML
  private TextField usernameField;

  /** PasswordField nhập mật khẩu */
  @FXML
  private PasswordField passwordField;

  /** Label hiển thị lỗi (ẩn mặc định) */
  @FXML
  private Label errorLabel;

  /** Button đăng nhập */
  @FXML
  private Button loginButton;

  /**
   * JavaFX gọi method này SAU KHI tất cả @FXML fields đã được inject.
   * Dùng để setup bindings, event listeners, giá trị mặc định.
   *
   * <p>Tuần 1: Chỉ disable button khi fields rỗng (binding đơn giản).
   */
  @FXML
  public void initialize() {
    // Disable button khi username hoặc password đang rỗng
    // Bindings.or() trả về BooleanBinding: true khi ít nhất 1 field rỗng
    loginButton.disableProperty().bind(
        usernameField.textProperty().isEmpty()
            .or(passwordField.textProperty().isEmpty())
    );

    // Ẩn label lỗi ban đầu
    errorLabel.setVisible(false);
    errorLabel.setManaged(false); // Không chiếm không gian khi ẩn
  }

  /**
   * Xử lý khi người dùng click "Đăng nhập" hoặc nhấn Enter trong form.
   *
   * <p>Tuần 1: Chỉ in ra console để test — sẽ gọi API thật ở Tuần 5.
   * <p>onAction="#handleLogin" trong FXML kết nối đến method này.
   */
  @FXML
  private void handleLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();

    // Tuần 1: Demo validation cơ bản (chưa gọi server)
    if (username.length() < 3) {
      showError("Tên đăng nhập phải có ít nhất 3 ký tự");
      return;
    }

    // TODO Tuần 5: Gọi NetworkTask → LoginCommand → nhận token → navigate AuctionList
    System.out.println("[DEBUG] Đăng nhập với username: " + username);
    showError(""); // Xóa lỗi cũ
    System.out.println("[DEBUG] Tuần 5 sẽ implement kết nối server thật");
  }

  /**
   * Xử lý khi người dùng click "Đăng ký".
   *
   * <p>TODO Tuần 5: ViewRouter.navigateTo(Views.REGISTER)
   */
  @FXML
  private void handleRegister() {
    System.out.println("[DEBUG] Chuyển sang màn hình đăng ký — implement tuần 5");
  }

  /**
   * Hiển thị thông báo lỗi dưới form.
   *
   * @param message nội dung lỗi; truyền chuỗi rỗng để ẩn label
   */
  private void showError(String message) {
    if (message == null || message.isBlank()) {
      errorLabel.setVisible(false);
      errorLabel.setManaged(false);
    } else {
      errorLabel.setText(message);
      errorLabel.setVisible(true);
      errorLabel.setManaged(true);
    }
  }
}
```

---

#### `client.properties`

```properties
# client.properties — đặt trong bidhub-client/src/main/resources/
# Cấu hình kết nối đến server

# Địa chỉ IP của server (đổi sang IP thật khi demo với máy khác)
server.host=localhost

# Phải khớp với server.port trong bidhub-server/src/main/resources/server.properties
server.port=9090
```

```bash
git add bidhub-client/src/
git commit -m "feat: thêm javafx entry point, loginview fxml, logincontroller và views constants"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Công Minh

```bash
# Test 1: Compile module client
mvn compile -pl bidhub-client
# ✅ PASS: BUILD SUCCESS
# ❌ FAIL: Lỗi FXML class không tìm thấy (kiểm tra package đúng chưa)

# Test 2: Chạy JavaFX app (MỞ ĐƯỢC CỬA SỔ)
mvn javafx:run -pl bidhub-client
# ✅ PASS: Cửa sổ "BidHub — Hệ thống đấu giá trực tuyến" hiện ra, có LoginView
# ❌ FAIL: Exception, cửa sổ không hiện, màn hình trắng

# Test 3: Kiểm tra FXML mở được trong Scene Builder
# → Mở Scene Builder → File → Open → chọn LoginView.fxml
# ✅ PASS: Layout hiện ra đầy đủ, không có lỗi "Unknown class" hay "Could not resolve"
# ❌ FAIL: Scene Builder báo lỗi fx:controller hoặc component

# Test 4: Test binding disable button
# → Mở app → để trống username/password → button "Đăng nhập" phải màu xám (disabled)
# → Nhập text vào cả 2 field → button phải enable (màu xanh)
# ✅ PASS: Disable/enable đúng theo trạng thái field

# Test 5: Test hiển thị lỗi
# → Nhập username "ab" (< 3 ký tự) + nhập password → click Đăng nhập
# ✅ PASS: Label lỗi màu đỏ hiện ra "Tên đăng nhập phải có ít nhất 3 ký tự"
# → Nhập username hợp lệ → Label lỗi phải biến mất

# Test 6: Kiểm tra Views.java constants
grep -c "public static final String" bidhub-client/src/main/java/com/bidhub/client/Views.java
# ✅ PASS: ≥ 5 constants (LOGIN, AUCTION_LIST, AUCTION_DETAIL, CREATE_ITEM, REGISTER)
```

### ❌ FAIL nếu:
- `BidHubApp` không extends `Application` → không phải JavaFX app
- FXML dùng `fx:controller` sai package (gây `ClassNotFoundException`)
- `initialize()` không có `@FXML` annotation → không được gọi tự động
- Button không disable khi field rỗng (thiếu binding)
- `errorLabel` chiếm chỗ khi ẩn (thiếu `setManaged(false)`) → layout bị dịch

---

## 👤 KHOA — JUnit 5 Setup, CalculatorTest & STYLE_GUIDE

```
Branch: feature/tuan-1-khoa-junit-convention
Phụ thuộc: Đợi Đăng push project structure (để có pom.xml mà code)
Deadline: Thứ 7 11/04 23:59
```

📌 **[Tiêu chí điểm: Unit Test (JUnit) cho logic quan trọng — 0.5đ]**

### 📝 Mô tả bài tập

Bạn xác nhận JUnit 5 chạy đúng trong project và tạo chuẩn testing cho cả nhóm. `CalculatorTest` là bài test đầu tiên, nhưng quan trọng hơn là thiết lập **convention** mà mọi người sẽ dùng xuyên suốt 10 tuần: đặt tên test đúng, cấu trúc AAA, `@DisplayName` tiếng Việt.

### 📁 Cấu trúc file cần tạo

```
bidhub-server/src/
├── main/java/com/bidhub/server/utils/
│   └── Calculator.java              ← Class đơn giản để test JUnit
└── test/java/com/bidhub/server/utils/
    └── CalculatorTest.java          ← ≥ 15 test cases (bao gồm edge cases)

docs/
└── STYLE_GUIDE.md                   ← Coding convention của nhóm
```

### 📋 Yêu cầu chi tiết

#### `Calculator.java` — Subject Under Test

```java
package com.bidhub.server.utils;

/**
 * Lớp tính toán số học cơ bản.
 *
 * <p>Lớp này được tạo ra để kiểm tra JUnit 5 hoạt động đúng trong project
 * và xây dựng template testing convention cho cả nhóm.
 *
 * <p>Các operation: cộng, trừ, nhân, chia, giai thừa, kiểm tra số nguyên tố.
 */
public class Calculator {

  /**
   * Tính tổng hai số nguyên.
   *
   * @param a số hạng thứ nhất
   * @param b số hạng thứ hai
   * @return tổng a + b
   */
  public int add(int a, int b) {
    return a + b;
  }

  /**
   * Tính hiệu hai số nguyên.
   *
   * @param a số bị trừ
   * @param b số trừ
   * @return hiệu a - b
   */
  public int subtract(int a, int b) {
    return a - b;
  }

  /**
   * Tính tích hai số nguyên.
   *
   * @param a thừa số thứ nhất
   * @param b thừa số thứ hai
   * @return tích a × b
   */
  public int multiply(int a, int b) {
    return a * b;
  }

  /**
   * Tính thương hai số nguyên (chia nguyên).
   *
   * @param a số bị chia
   * @param b số chia
   * @return thương a / b
   * @throws ArithmeticException nếu b == 0 (chia cho 0)
   */
  public int divide(int a, int b) {
    if (b == 0) {
      throw new ArithmeticException("Không thể chia cho 0");
    }
    return a / b;
  }

  /**
   * Tính giai thừa của số nguyên không âm.
   *
   * <p>Quy ước: 0! = 1 (giai thừa của 0 bằng 1 theo định nghĩa toán học)
   *
   * @param n số nguyên cần tính giai thừa (n ≥ 0)
   * @return n!
   * @throws IllegalArgumentException nếu n < 0
   */
  public long factorial(int n) {
    if (n < 0) {
      throw new IllegalArgumentException("Không tính được giai thừa của số âm: " + n);
    }
    long result = 1;
    for (int i = 2; i <= n; i++) {
      result *= i;
    }
    return result;
  }

  /**
   * Kiểm tra số nguyên tố.
   *
   * <p>Số nguyên tố là số tự nhiên lớn hơn 1, chỉ chia hết cho 1 và chính nó.
   *
   * @param n số cần kiểm tra
   * @return {@code true} nếu n là số nguyên tố
   * @throws IllegalArgumentException nếu n ≤ 1 (không xác định theo định nghĩa)
   */
  public boolean isPrime(int n) {
    if (n <= 1) {
      throw new IllegalArgumentException(
          "Số nguyên tố được định nghĩa với n > 1. Nhận được: " + n);
    }
    if (n <= 3) return true;
    if (n % 2 == 0 || n % 3 == 0) return false;
    for (int i = 5; (long) i * i <= n; i += 6) {
      if (n % i == 0 || n % (i + 2) == 0) return false;
    }
    return true;
  }
}
```

---

#### `CalculatorTest.java` — ≥ 15 test cases với đầy đủ edge cases

```java
package com.bidhub.server.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho {@link Calculator}.
 *
 * <p>Template testing convention của nhóm BidHub:
 * <ul>
 *   <li>Cấu trúc AAA: Arrange → Act → Assert</li>
 *   <li>{@code @DisplayName} tiếng Việt mô tả rõ ràng hành vi</li>
 *   <li>{@code @Nested} nhóm test theo từng method</li>
 *   <li>Test cả happy path (đầu vào bình thường) và edge cases</li>
 * </ul>
 */
@DisplayName("Calculator — Kiểm tra các phép tính cơ bản")
class CalculatorTest {

  private Calculator calc;

  @BeforeEach
  void setUp() {
    // Arrange chung: tạo fresh instance trước mỗi test
    calc = new Calculator();
  }

  // ===================== add() =====================

  @Nested
  @DisplayName("Phép cộng add()")
  class AddTests {

    @Test
    @DisplayName("Cộng hai số dương → kết quả đúng")
    void testAdd_TwoPositives_ReturnsSum() {
      // Arrange
      int a = 5, b = 3;

      // Act
      int result = calc.add(a, b);

      // Assert
      assertEquals(8, result, "5 + 3 phải bằng 8");
    }

    @Test
    @DisplayName("Cộng số dương và số âm → kết quả đúng")
    void testAdd_PositiveAndNegative_ReturnsSum() {
      assertEquals(2, calc.add(5, -3));
    }

    @Test
    @DisplayName("Cộng hai số âm → kết quả âm")
    void testAdd_TwoNegatives_ReturnsNegativeSum() {
      assertEquals(-8, calc.add(-5, -3));
    }

    @Test
    @DisplayName("Cộng với 0 → trả về chính số đó")
    void testAdd_WithZero_ReturnsSameNumber() {
      assertEquals(42, calc.add(42, 0));
      assertEquals(42, calc.add(0, 42));
    }
  }

  // ===================== subtract() =====================

  @Nested
  @DisplayName("Phép trừ subtract()")
  class SubtractTests {

    @Test
    @DisplayName("Trừ bình thường → kết quả đúng")
    void testSubtract_Normal_ReturnsCorrect() {
      assertEquals(2, calc.subtract(5, 3));
    }

    @Test
    @DisplayName("Trừ ra số âm khi a < b")
    void testSubtract_ResultNegative_ReturnsNegative() {
      assertEquals(-3, calc.subtract(2, 5));
    }
  }

  // ===================== divide() =====================

  @Nested
  @DisplayName("Phép chia divide()")
  class DivideTests {

    @Test
    @DisplayName("Chia hai số nguyên chia hết nhau → kết quả đúng")
    void testDivide_ExactDivision_ReturnsQuotient() {
      assertEquals(3, calc.divide(9, 3));
    }

    @Test
    @DisplayName("Chia cho 0 → ném ArithmeticException")
    void testDivide_ByZero_ThrowsArithmeticException() {
      // assertThrows trả về exception object để có thể assert thêm
      ArithmeticException ex = assertThrows(
          ArithmeticException.class,
          () -> calc.divide(10, 0),
          "Chia cho 0 phải ném ArithmeticException"
      );
      assertTrue(ex.getMessage().contains("0"),
          "Message lỗi phải đề cập đến 0");
    }

    @Test
    @DisplayName("Chia số âm cho số dương → kết quả âm")
    void testDivide_NegativeByPositive_ReturnsNegative() {
      assertEquals(-3, calc.divide(-9, 3));
    }

    @Test
    @DisplayName("Chia 0 cho số bất kỳ (khác 0) → trả về 0")
    void testDivide_ZeroByNonZero_ReturnsZero() {
      assertEquals(0, calc.divide(0, 5));
    }
  }

  // ===================== factorial() =====================

  @Nested
  @DisplayName("Giai thừa factorial()")
  class FactorialTests {

    @Test
    @DisplayName("Giai thừa 0 → phải bằng 1 (theo định nghĩa toán học)")
    void testFactorial_Zero_ReturnsOne() {
      assertEquals(1L, calc.factorial(0));
    }

    @Test
    @DisplayName("Giai thừa 1 → bằng 1")
    void testFactorial_One_ReturnsOne() {
      assertEquals(1L, calc.factorial(1));
    }

    @Test
    @DisplayName("Giai thừa 5 → bằng 120")
    void testFactorial_Five_Returns120() {
      assertEquals(120L, calc.factorial(5));
    }

    @Test
    @DisplayName("Giai thừa số âm → ném IllegalArgumentException")
    void testFactorial_NegativeNumber_ThrowsIllegalArgument() {
      IllegalArgumentException ex = assertThrows(
          IllegalArgumentException.class,
          () -> calc.factorial(-1),
          "Số âm phải ném IllegalArgumentException"
      );
      assertNotNull(ex.getMessage(), "Message lỗi không được null");
      assertTrue(ex.getMessage().contains("-1"),
          "Message phải chứa giá trị nhập vào");
    }
  }

  // ===================== isPrime() =====================

  @Nested
  @DisplayName("Kiểm tra nguyên tố isPrime()")
  class IsPrimeTests {

    @Test
    @DisplayName("isPrime(2) → true (số nguyên tố nhỏ nhất)")
    void testIsPrime_Two_ReturnsTrue() {
      assertTrue(calc.isPrime(2));
    }

    @Test
    @DisplayName("isPrime(7) → true")
    void testIsPrime_Seven_ReturnsTrue() {
      assertTrue(calc.isPrime(7));
    }

    @Test
    @DisplayName("isPrime(4) → false (chia hết cho 2)")
    void testIsPrime_Four_ReturnsFalse() {
      assertFalse(calc.isPrime(4));
    }

    @Test
    @DisplayName("isPrime(1) → ném exception (1 không phải nguyên tố)")
    void testIsPrime_One_ThrowsException() {
      assertThrows(IllegalArgumentException.class,
          () -> calc.isPrime(1));
    }

    @Test
    @DisplayName("isPrime(0) và isPrime(-5) → ném exception")
    void testIsPrime_ZeroAndNegative_ThrowsException() {
      assertThrows(IllegalArgumentException.class, () -> calc.isPrime(0));
      assertThrows(IllegalArgumentException.class, () -> calc.isPrime(-5));
    }

    @Test
    @DisplayName("isPrime(97) → true (số nguyên tố lớn)")
    void testIsPrime_LargePrime_ReturnsTrue() {
      assertTrue(calc.isPrime(97));
    }

    @Test
    @DisplayName("isPrime(100) → false")
    void testIsPrime_Hundred_ReturnsFalse() {
      assertFalse(calc.isPrime(100));
    }
  }
}
```

```bash
git add bidhub-server/src/main/java/com/bidhub/server/utils/Calculator.java
git commit -m "feat: thêm class Calculator để làm subject under test cho junit 5"

git add bidhub-server/src/test/java/com/bidhub/server/utils/CalculatorTest.java
git commit -m "test: thêm calculatortest 15+ cases với aaa pattern và nested groups"
```

---

#### `docs/STYLE_GUIDE.md` — Coding Convention của nhóm

```markdown
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
```

```bash
git add docs/STYLE_GUIDE.md
git commit -m "docs: thêm style guide coding convention theo google java style"
```

---

### ✅ TEST ĐẦU RA — Cách kiểm tra bài của Khoa

```bash
# Test 1: Chạy tất cả tests
mvn test -pl bidhub-server
# ✅ PASS: "Tests run: 15+, Failures: 0, Errors: 0, Skipped: 0"
# ❌ FAIL: Failures > 0 hoặc Errors > 0

# Test 2: Đếm số test methods
grep -r "@Test" bidhub-server/src/test/java/ | wc -l
# ✅ PASS: ≥ 15

# Test 3: Chạy test chia cho 0 cụ thể
mvn test -pl bidhub-server -Dtest="CalculatorTest\$DivideTests#testDivide_ByZero_ThrowsArithmeticException"
# ✅ PASS: Test PASSED

# Test 4: Chạy test giai thừa số âm
mvn test -pl bidhub-server -Dtest="CalculatorTest\$FactorialTests#testFactorial_NegativeNumber_ThrowsIllegalArgument"
# ✅ PASS: Test PASSED

# Test 5: Kiểm tra @DisplayName tồn tại đủ
grep -c "@DisplayName" bidhub-server/src/test/java/com/bidhub/server/utils/CalculatorTest.java
# ✅ PASS: ≥ 12 (class-level + mỗi test method)

# Test 6: Kiểm tra cấu trúc AAA (có comment Arrange/Act/Assert)
grep -c "// Arrange\|// Act\|// Assert" bidhub-server/src/test/java/com/bidhub/server/utils/CalculatorTest.java
# ✅ PASS: ≥ 3 (ít nhất 1 test có đủ cả 3 comments)

# Test 7: Kiểm tra STYLE_GUIDE.md đủ nội dung
grep -c "^## " docs/STYLE_GUIDE.md
# ✅ PASS: ≥ 6 sections

# Test 8: Kiểm tra loại assertion đa dạng
grep -oE "assert(Equals|True|False|NotNull|Null|Throws|DoesNotThrow)" \
  bidhub-server/src/test/java/com/bidhub/server/utils/CalculatorTest.java | sort -u
# ✅ PASS: Phải có ≥ 4 loại assertion khác nhau
```

### ❌ FAIL nếu:
- `mvn test` có < 15 test cases
- Thiếu test `isPrime(1)` → exception (không phải nguyên tố)
- Thiếu test `factorial(-1)` → exception
- Thiếu test `divide(10, 0)` → `ArithmeticException`
- Không có `@DisplayName` tiếng Việt → reviewer phải đọc code mới hiểu test làm gì
- Thiếu cấu trúc AAA: 3 phần Arrange/Act/Assert không phân biệt rõ ràng
- `STYLE_GUIDE.md` chỉ có tiêu đề rỗng, thiếu ví dụ ✅/❌ cụ thể

---

## 🔄 QUY TRÌNH KIỂM TRA CHÉO — CHỦ NHẬT 12/04/2026

### Phân công review

| Người làm | Reviewer 1 | Reviewer 2 |
|-----------|------------|------------|
| **Đăng** (Maven + ConfigLoader) | Quốc Minh | Khoa |
| **Quốc Minh** (Git CI + API Docs) | Đăng | Công Minh |
| **Công Minh** (JavaFX Skeleton) | Khoa | Đăng |
| **Khoa** (JUnit + Style Guide) | Công Minh | Quốc Minh |

### Quy trình review (mỗi người ~30 phút)

#### Bước 1: Pull code về (5 phút)
```bash
git fetch origin
git checkout feature/tuan-1-[ten-nguoi-can-review]
# Ví dụ: git checkout feature/tuan-1-dang-maven-setup
```

#### Bước 2: Chạy test đầu ra (10 phút)
Chạy từng lệnh trong phần "TEST ĐẦU RA" của người đó. Ghi lại: ✅ PASS hoặc ❌ FAIL.

#### Bước 3: Đọc code và chuẩn bị câu hỏi (10 phút)

**Câu hỏi bắt buộc hỏi Đăng:**
1. `<dependencyManagement>` trong parent POM khác `<dependencies>` thế nào? Không có nó thì sao?
2. Tại sao `ConfigLoader` constructor là `private`? Nếu để `public` thì có vấn đề gì?
3. Vì sao dùng `getResourceAsStream` thay vì `new File()`? Khi đóng gói JAR thì cái nào chạy được?

**Câu hỏi bắt buộc hỏi Quốc Minh:**
1. CI pipeline có mấy bước? Mỗi bước làm gì?
2. Tại sao phải cache `~/.m2`? Nếu không cache thì ảnh hưởng gì?
3. Conventional Commits: `feat` vs `build` khác nhau thế nào? Ví dụ thực tế?

**Câu hỏi bắt buộc hỏi Công Minh:**
1. `@FXML` annotation làm gì? Kết nối với FXML qua cơ chế nào?
2. `initialize()` được gọi khi nào? Trước hay sau `@FXML` fields được inject?
3. Tại sao `setManaged(false)` khi ẩn `errorLabel`? Chỉ `setVisible(false)` thì sao?

**Câu hỏi bắt buộc hỏi Khoa:**
1. Cấu trúc AAA trong test là gì? Tại sao nên tách rõ 3 phần?
2. `@BeforeEach` làm gì? Tại sao tạo `new Calculator()` ở đó thay vì trong mỗi test?
3. `assertThrows` trả về gì? Tại sao dùng nó tốt hơn try-catch trong test?

#### Bước 4: Comment PR (5 phút)

```markdown
## Review — [Tên reviewer] → [Tên người code]
**Branch:** `feature/tuan-1-[ten]-...`

### ✅ Test Results
- [ ] `mvn compile` → ✅/❌
- [ ] `mvn test` → ✅/❌  |  Tests: ___ / Failures: ___ / Errors: ___
- [ ] Manual test (chạy app / kiểm tra CI) → ✅/❌
- [ ] Kiểm tra cấu trúc file đúng như yêu cầu → ✅/❌

### 🔍 Code Quality
- Naming convention (Google Style): ✅ OK / ⚠️ Cần sửa (chỗ nào?)
- Javadoc: ✅ Đầy đủ / ⚠️ Thiếu (method nào?)
- Không có code dư/hardcode: ✅ OK / ⚠️ (mô tả)
- Private constructor cho utility class: ✅ Có / ❌ Thiếu

### ❓ Câu hỏi khi đọc code
1. [Câu hỏi 1 — thể hiện bạn đã đọc thật sự]
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
- [ ] Đã cài JDK 21, IntelliJ, Scene Builder, Git, Maven (screenshot đã gửi nhóm)
- [ ] Đã hoàn thành bài thực hành Git (branch `practice/tuan-1-[TEN]` đã push)
- [ ] Có thể trả lời miệng các câu hỏi tự học (không xem tài liệu)
- [ ] Branch của mình: `mvn compile` pass, `mvn test` pass
- [ ] PR đã tạo, gán đúng 2 reviewers trước deadline

### Checklist Đăng (Maven + ConfigLoader):
- [ ] `mvn compile` thành công cả 2 module từ thư mục gốc
- [ ] `mvn test -pl bidhub-server` → ≥ 6 tests pass, 0 failures
- [ ] Module con (`bidhub-server/pom.xml`) KHÔNG tự khai báo `<version>` của Jackson/JUnit/SQLite
- [ ] `ConfigLoader` constructor là `private` (`final class`)
- [ ] `server.properties` đặt đúng trong `src/main/resources/`
- [ ] `ConfigLoader.getString("server.port")` → `"9090"`
- [ ] `ConfigLoader.getOrDefault("nonexistent", "default")` → `"default"`
- [ ] `.gitignore` loại được `target/`, `.idea/`, `*.db`

### Checklist Quốc Minh (Git CI + API Docs):
- [ ] GitHub Actions trigger khi push lên branch bất kỳ
- [ ] CI badge xuất hiện trong repo (sau khi merge vào develop)
- [ ] Cache Maven hoạt động (lần 2 CI chạy nhanh hơn lần 1)
- [ ] PR template hiện đầy đủ khi tạo PR mới
- [ ] `CONTRIBUTING.md` có đủ: quy trình branch, commit types với ví dụ, quy trình PR
- [ ] `docs/API_PROTOCOL.md` có ≥ 3 ví dụ JSON request/response
- [ ] `README.md` đủ để người mới clone về và chạy được

### Checklist Công Minh (JavaFX Skeleton):
- [ ] `mvn javafx:run -pl bidhub-client` → cửa sổ BidHub hiện ra
- [ ] LoginView hiển thị đúng: username field, password field, button, tiêu đề
- [ ] Button "Đăng nhập" disabled khi username hoặc password rỗng
- [ ] Nhập username < 3 ký tự → click → label lỗi màu đỏ hiện
- [ ] `LoginView.fxml` mở được trong Scene Builder không lỗi
- [ ] `Views.java` có ít nhất 5 constants tên màn hình
- [ ] `client.properties` có `server.host` và `server.port`

### Checklist Khoa (JUnit + Style Guide):
- [ ] `mvn test -pl bidhub-server` → ≥ 15 tests pass, 0 failures
- [ ] Có test `divide(10, 0)` → `ArithmeticException`
- [ ] Có test `factorial(-1)` → `IllegalArgumentException`
- [ ] Có test `isPrime(1)` → `IllegalArgumentException`
- [ ] Mỗi test method có `@DisplayName` tiếng Việt mô tả rõ hành vi
- [ ] Ít nhất 1 test có cấu trúc AAA đầy đủ với comment
- [ ] Dùng ≥ 4 loại assertion khác nhau (`assertEquals`, `assertTrue`, `assertFalse`, `assertThrows`, `assertNotNull`...)
- [ ] `docs/STYLE_GUIDE.md` đủ 6+ mục với ví dụ ✅/❌ cụ thể

---

## ⏰ TIMELINE GỢI Ý

| Ngày | Việc cần làm |
|------|-------------|
| **T2 06/04 (Tối)** | Kick-off meeting: phân công rõ ràng, Đăng bắt đầu ngay tối hôm đó |
| **T3 07/04** | **Đăng push project structure lên `develop`** — cả nhóm clone về |
| **T3-T4 07-08/04** | Học phần tự học (Git, Maven, JavaFX, Optional) |
| **T4 08/04** | Bắt đầu code task cá nhân trên branch riêng |
| **T5 09/04 (Tối)** | Mid-week check-in: demo progress, hỏi nếu stuck |
| **T6 10/04** | Hoàn thiện code, viết test, self-review theo checklist |
| **T7 11/04** | **DEADLINE 23:59** — final push, tạo PR gán reviewers |
| **CN 12/04 (Sáng)** | Review chéo (mỗi người 30 phút), comment PR, merge vào `develop` |

> [!TIP]
> **Đăng phải push sớm nhất có thể (Thứ 3)** để cả nhóm có project mà clone về. Nếu chưa merge được, tạm thời gửi ZIP qua Zalo/Discord để mọi người làm tạm.

> [!WARNING]
> **Không push code sau 23:59 Thứ 7.** Code nào push sau deadline sẽ không được tính vào review Chủ nhật.

> [!CAUTION]
> **Buổi review Chủ nhật sẽ hỏi miệng** theo danh sách câu hỏi ở phần "Câu hỏi bắt buộc" của từng người. Phải giải thích được code của người khác — đây là cách duy nhất để cả nhóm không bị 0 điểm khi giảng viên hỏi vấn đáp.
