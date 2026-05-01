package com.bidhub.server.network;

import com.bidhub.common.exception.BidHubException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;

/**
 * Dispatcher chính: nhận JSON thô → parse → auth-guard → switch type → gọi handler.
 *
 * <p>Switch-case mở rộng từng tuần mà không refactor:
 * Tuần 4: PING · Tuần 5: LOGIN / REGISTER / LOGOUT / CREATE_ITEM / GET_ITEM_LIST ·
 * Tuần 6: PLACE_BID / LIST_AUCTIONS · Tuần 7+: chỉ thêm case.
 */
public final class RequestHandler {

    // 📌 [Tiêu chí: MVC — RequestHandler là tầng điều phối server]
    private static final Set<String> AUTH_REQUIRED = Set.of(
            "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
            "LIST_MY_ITEMS", "PLACE_BID", "GET_AUCTION_DETAIL"
    );

    /**
     * Xử lý 1 request JSON từ client, trả về JSON response string.
     *
     * <p>Không ném exception ra ngoài — mọi lỗi wrap thành error response.
     *
     * @param jsonLine dòng JSON thô từ socket
     * @param session  session của client
     * @return chuỗi JSON response
     */
    public String handle(String jsonLine, Session session) {
        MessageRequest req;
        try {
            req = MessageMapper.fromJson(jsonLine, MessageRequest.class);
        } catch (Exception e) {
            return MessageMapper.toJson(
                    MessageResponse.error("UNKNOWN", "JSON không hợp lệ: " + e.getMessage()));
        }

        String type = req.getType() != null ? req.getType().toUpperCase() : "UNKNOWN";

        // 📌 [Tiêu chí: Kiến trúc Client–Server — auth guard]
        if (AUTH_REQUIRED.contains(type) && !session.isAuthenticated()) {
            return MessageMapper.toJson(
                    MessageResponse.error(type, "Bạn chưa đăng nhập. Vui lòng LOGIN trước."));
        }

        try {
            return switch (type) {
                case "PING"     -> handlePing(session);
                case "LOGIN"    -> MessageMapper.toJson(
                        MessageResponse.error("LOGIN", "Chưa implement — sẽ có ở Tuần 5"));
                case "REGISTER" -> MessageMapper.toJson(
                        MessageResponse.error("REGISTER", "Chưa implement — sẽ có ở Tuần 5"));
                default         -> MessageMapper.toJson(
                        MessageResponse.error(type, "Lệnh không xác định: " + type));
            };
        } catch (BidHubException e) {
            return MessageMapper.toJson(MessageResponse.error(type, e.getMessage()));
        } catch (Exception e) {
            System.err.println("[RequestHandler] Lỗi xử lý " + type + ": " + e.getMessage());
            return MessageMapper.toJson(MessageResponse.error(type, "Lỗi hệ thống nội bộ."));
        }
    }

    private String handlePing(Session session) {
        Map<String, String> payload = Map.of(
                "message", "pong",
                "serverTime", LocalDateTime.now().toString(),
                "sessionId", session.getSessionId()
        );
        return MessageMapper.toJson(MessageResponse.ok("PING", payload));
    }
}