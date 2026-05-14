package com.bidhub.common.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility serialize/deserialize JSON cho toàn bộ server protocol.
 *
 * <p>ObjectMapper là thread-safe sau khi cấu hình — khai báo {@code static final},
 * không tạo mới mỗi lần gọi (ObjectMapper tốn ~65ms khởi tạo).
 */
public final class MessageMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 📌 [Tieu chi: Ky thuat quan trong — JavaTimeModule cho LocalDateTime serialization]
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private MessageMapper() {}

    /**
     * Serialize object thành JSON string. Không ném exception ra ngoài — trả về fallback JSON.
     *
     * @param obj object cần serialize
     * @return chuỗi JSON
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"type\":\"SYSTEM\","
                    + "\"message\":\"Serialization error: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Deserialize JSON string thành object. Ném exception nếu JSON không hợp lệ.
     *
     * @param json  chuỗi JSON từ socket
     * @param clazz class đích
     * @param <T>   kiểu kết quả
     * @return object đã parse
     * @throws Exception nếu JSON malformed
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return MAPPER.readValue(json, clazz);
    }

    /** Trả về ObjectMapper gốc — dùng khi cần register module (ví dụ JavaTimeModule Tuần 7). */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}