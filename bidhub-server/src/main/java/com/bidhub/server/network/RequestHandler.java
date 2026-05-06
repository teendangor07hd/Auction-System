package com.bidhub.server.network;

import com.bidhub.common.exception.BidHubException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.*;
import com.bidhub.server.service.AuditLogService;
import com.bidhub.server.service.SessionManager;
import com.bidhub.server.dao.ItemDao;
import com.fasterxml.jackson.databind.JsonNode;

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

    // ← THÊM: field DAO (null ở T4 — sẽ được gán thực sự ở T5)
    // Giữ package-private để test inject được mà không cần reflection
    final Object injectedUserDao;   // type Object tạm — T5 sẽ đổi thành UserDao
    final Object injectedItemDao;   // type Object tạm — T5 sẽ đổi thành ItemDao
    private final ItemDao itemDao;

    /** Constructor production — T4 và T5 đều dùng. */
    public RequestHandler() {
        this.injectedUserDao = null;
        this.injectedItemDao = null;
        this.auditLogService = new AuditLogService();
        this.userDao = new UserDao();
        this.itemDao = new ItemDao();
    }

    /**
     * Constructor inject — dùng trong test để truyền in-memory DAO.
     * T5 sẽ bổ sung đầy đủ tham số khi cần.
     *
     * @param injectedUserDao DAO inject từ test (Object để T4 compile không cần UserDao)
     * @param injectedItemDao DAO inject từ test
     */
    RequestHandler(Object injectedUserDao, Object injectedItemDao) {
        this.injectedUserDao = injectedUserDao;
        this.injectedItemDao = injectedItemDao;
        this.auditLogService = new AuditLogService();
        this.userDao = injectedUserDao instanceof UserDao
                ? (UserDao) injectedUserDao : new UserDao();
        this.itemDao = injectedItemDao instanceof ItemDao
                ? (ItemDao) injectedItemDao : new ItemDao();
    }
    RequestHandler(Object injectedUserDao, Object injectedItemDao,
                   AuditLogService injectedAuditService) {
        this.injectedUserDao = injectedUserDao;
        this.injectedItemDao = injectedItemDao;
        this.auditLogService = injectedAuditService;
        this.userDao = injectedUserDao instanceof UserDao
                ? (UserDao) injectedUserDao : new UserDao();
        this.itemDao = injectedItemDao instanceof ItemDao
                ? (ItemDao) injectedItemDao : new ItemDao();
    }

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
        // === THÊM VÀO method handle() — sau phần parse JSON, trước auth-guard check ===

        // 📌 [Tieu chi: Kien truc Client–Server — giai ma token truoc khi xu ly request]
        // Giai ma token → set authenticatedUserId vao session
        String token = req.getToken();
        if (token != null && !token.isBlank()) {
            SessionManager.getInstance().getUserIdByToken(token)
                    .ifPresent(session::setAuthenticatedUserId);
        }
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
                case "CREATE_ITEM"    -> handleCreateItem(session, payload);
                case "GET_ITEM_LIST"  -> handleGetItemList();
                case "GET_ITEM_DETAIL" -> handleGetItemDetail(payload);
                case "DELETE_ITEM"    -> handleDeleteItem(session, payload);
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
    // === ITEM HANDLERS ===

    /**
     * Tao san pham moi — yeu cau role SELLER.
     *
     * <p>// 📌 [Tieu chi: Quan ly san pham — tao san pham voi Factory Method + audit log]
     *
     * @param session session hien tai
     * @param payload chua {name, description, startingPrice, itemType, extras}
     * @return JSON response voi item info
     */
    private String handleCreateItem(Session session, JsonNode payload) {
        // 📌 [Tieu chi: Quan ly nguoi dung — kiem tra role SELLER]
        String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);

        String name = getTextSafe(payload, "name");
        String description = getTextSafe(payload, "description");
        String priceStr = getTextSafe(payload, "startingPrice");
        String itemTypeStr = getTextSafe(payload, "itemType");

        if (name == null || name.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_ITEM", "Ten san pham khong duoc de trong."));
        }
        if (priceStr == null) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_ITEM", "Gia khoi diem la bat buoc."));
        }

        double startingPrice;
        try {
            startingPrice = Double.parseDouble(priceStr);
            if (startingPrice <= 0) {
                return MessageMapper.toJson(
                        MessageResponse.error("CREATE_ITEM", "Gia khoi diem phai lon hon 0."));
            }
        } catch (NumberFormatException e) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_ITEM", "Gia khoi diem khong hop le."));
        }

        if (itemTypeStr == null) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_ITEM", "Loai san pham la bat buoc."));
        }

        ItemType itemType;
        try {
            itemType = ItemType.valueOf(itemTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_ITEM",
                            "Loai san pham khong hop le: " + itemTypeStr));
        }

        // 📌 [Tieu chi: Design Pattern — Factory Method ItemCreator.forType()]
        JsonNode extrasNode = payload.has("extras") ? payload.get("extras") : null;
        java.util.Map<String, Object> extras = parseExtras(extrasNode);

        Item item;
        try {
            ItemCreator creator = ItemCreator.forType(itemType);
            item = creator.createItem(name, description, startingPrice, sellerId, extras);
        } catch (Exception e) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_ITEM",
                            "Loi tao san pham: " + e.getMessage()));
        }

        itemDao.save(item);

        // 📌 [Tieu chi: Quan ly san pham — audit log tao san pham]
        auditLogService.log(sellerId, AuditActions.ITEM_CREATED,
                "{\"itemId\":\"" + item.getId() + "\"}");

        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("itemId", item.getId());
        result.put("name", item.getName());
        result.put("itemType", item.getItemType().name());
        result.put("startingPrice", String.valueOf(item.getStartingPrice()));

        return MessageMapper.toJson(MessageResponse.ok("CREATE_ITEM", result));
    }

    /**
     * Lay danh sach tat ca san pham — khong can auth.
     *
     * <p>// 📌 [Tieu chi: Quan ly san pham — danh sach san pham cong khai]
     *
     * @return JSON response voi danh sach items
     */
    private String handleGetItemList() {
        java.util.List<Item> items = itemDao.findAll();
        return MessageMapper.toJson(MessageResponse.ok("GET_ITEM_LIST", items));
    }

    /**
     * Lay chi tiet 1 san pham — khong can auth.
     *
     * @param payload chua {itemId}
     * @return JSON response voi item detail
     */
    private String handleGetItemDetail(JsonNode payload) {
        String itemId = getTextSafe(payload, "itemId");

        if (itemId == null || itemId.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("GET_ITEM_DETAIL", "itemId la bat buoc."));
        }

        java.util.Optional<Item> itemOpt = itemDao.findById(itemId);
        if (itemOpt.isEmpty()) {
            return MessageMapper.toJson(
                    MessageResponse.error("GET_ITEM_DETAIL", "San pham khong ton tai."));
        }

        return MessageMapper.toJson(
                MessageResponse.ok("GET_ITEM_DETAIL", itemOpt.get()));
    }

    /**
     * Xoa san pham — yeu cau auth + chi seller cua item moi duoc xoa.
     *
     * <p>// 📌 [Tieu chi: Quan ly san pham — xoa san pham + kiem tra quyen]
     *
     * @param session session hien tai
     * @param payload chua {itemId}
     * @return JSON response
     */
    private String handleDeleteItem(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String itemId = getTextSafe(payload, "itemId");

        if (itemId == null || itemId.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("DELETE_ITEM", "itemId la bat buoc."));
        }

        java.util.Optional<Item> itemOpt = itemDao.findById(itemId);
        if (itemOpt.isEmpty()) {
            return MessageMapper.toJson(
                    MessageResponse.error("DELETE_ITEM", "San pham khong ton tai."));
        }

        Item item = itemOpt.get();
        if (!item.getSellerId().equals(userId)) {
            return MessageMapper.toJson(
                    MessageResponse.error("DELETE_ITEM",
                            "Ban khong co quyen xoa san pham nay."));
        }

        itemDao.deleteById(itemId);

        // 📌 [Tieu chi: Quan ly san pham — audit log xoa san pham]
        auditLogService.log(userId, AuditActions.ITEM_DELETED,
                "{\"itemId\":\"" + itemId + "\"}");

        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("message", "Xoa san pham thanh cong.");
        result.put("itemId", itemId);

        return MessageMapper.toJson(MessageResponse.ok("DELETE_ITEM", result));
    }

    // === HELPER ===

    /**
     * Parse extras JsonNode thanh Map<String, Object> cho ItemCreator.
     */
    private java.util.Map<String, Object> parseExtras(JsonNode extrasNode) {
        java.util.Map<String, Object> extras = new java.util.HashMap<>();
        if (extrasNode == null || !extrasNode.isObject()) {
            return extras;
        }
        extrasNode.fields().forEachRemaining(entry -> {
            JsonNode val = entry.getValue();
            if (val.isInt()) {
                extras.put(entry.getKey(), val.asInt());
            } else if (val.isDouble()) {
                extras.put(entry.getKey(), val.asDouble());
            } else {
                extras.put(entry.getKey(), val.asText());
            }
        });
        return extras;
    }
}