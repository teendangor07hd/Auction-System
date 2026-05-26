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
        // Static initializer: chạy duy nhất 1 lần khi class được load vào JVM
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

    /**
     * Đọc giá trị int từ config, trả về giá trị mặc định nếu key không tồn tại.
     *
     * @param key          khóa trong file .properties
     * @param defaultValue giá trị mặc định nếu key không có
     * @return giá trị int từ config hoặc defaultValue
     */
    public static int getIntOrDefault(String key, int defaultValue) {
        String value = PROPS.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}