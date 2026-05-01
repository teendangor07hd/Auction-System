package com.bidhub.common.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * POJO cho request từ client → server.
 *
 * <p>Format: {@code {"type":"PING","token":"uuid-or-null","payload":{}}}
 * {@code @JsonIgnoreProperties} đảm bảo field thêm từ client cũ không gây crash.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageRequest {

    private String type;
    private String token;
    private JsonNode payload;

    /** No-arg constructor bắt buộc cho Jackson. */
    public MessageRequest() {}

    public MessageRequest(String type, String token, JsonNode payload) {
        this.type = type;
        this.token = token;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    /** Trả về payload JsonNode — kiểm tra null trước khi gọi .get("field") trong handler. */
    public JsonNode getPayload() {
        return payload;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}