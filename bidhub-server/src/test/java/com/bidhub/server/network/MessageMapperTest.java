package com.bidhub.server.network;

import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import org.junit.jupiter.api.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MessageMapperTest {

    @Test
    @DisplayName("MessageResponse.ok serialize → có status=OK và type đúng")
    void ok_serializes() throws Exception {
        MessageResponse resp = MessageResponse.ok("PING", Map.of("message", "pong"));
        String json = MessageMapper.toJson(resp);
        assertTrue(json.contains("\"status\":\"OK\""));
        assertTrue(json.contains("\"type\":\"PING\""));
    }

    @Test
    @DisplayName("MessageResponse.error → status=ERROR, field payload không xuất hiện")
    void error_noPayloadField() {
        MessageResponse resp = MessageResponse.error("LOGIN", "Sai mật khẩu");
        String json = MessageMapper.toJson(resp);
        assertTrue(json.contains("\"status\":\"ERROR\""));
        assertTrue(json.contains("Sai mật khẩu"));
        assertFalse(json.contains("\"payload\"")); // @JsonInclude.NON_NULL
    }

    @Test
    @DisplayName("fromJson thêm field lạ → @JsonIgnoreProperties không crash")
    void fromJson_extraFields_ignored() {
        String json = "{\"type\":\"PING\",\"payload\":{},\"clientVersion\":\"2.0\"}";
        assertDoesNotThrow(() -> MessageMapper.fromJson(json, MessageRequest.class));
    }

    @Test
    @DisplayName("fromJson chuỗi rỗng → ném exception (không im lặng)")
    void fromJson_empty_throwsException() {
        assertThrows(Exception.class,
                () -> MessageMapper.fromJson("", MessageRequest.class));
    }

    @Test
    @DisplayName("fromJson JSON thiếu field → type null, payload null, không crash")
    void fromJson_minimal_doesNotCrash() throws Exception {
        MessageRequest req = MessageMapper.fromJson("{}", MessageRequest.class);
        assertNull(req.getType());
        assertNull(req.getPayload());
    }
}