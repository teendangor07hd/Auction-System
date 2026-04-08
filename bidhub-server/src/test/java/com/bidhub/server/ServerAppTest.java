package com.bidhub.server;

import com.bidhub.server.config.ConfigLoader;
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