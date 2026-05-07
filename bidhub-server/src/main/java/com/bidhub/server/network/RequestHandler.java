package com.bidhub.server.network;

import com.bidhub.common.exception.BidHubException;
import com.bidhub.common.exception.DuplicateUsernameException;
import com.bidhub.common.exception.AuthenticationException;
import com.bidhub.common.exception.UserNotFoundException;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.*;
import com.bidhub.server.service.AdminUserService;
import com.bidhub.server.service.AuditLogService;
import com.bidhub.server.service.SessionManager;
import com.bidhub.server.dao.ItemDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.Bidder;
import com.bidhub.server.model.Seller;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import com.bidhub.server.service.AuthService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dispatcher chinh: nhan JSON tho → parse → auth-guard → switch type → goi handler.
 *
 * <p>Switch-case mo rong tung tuan ma khong refactor:
 * Tuan 4: PING · Tuan 5: LOGIN / REGISTER / LOGOUT / CREATE_ITEM / GET_ITEM_LIST ·
 * Tuan 6: PLACE_BID / LIST_AUCTIONS · Tuan 7+: chi them case.
 */
public final class RequestHandler {

    // 📌 [Tieu chi: MVC — RequestHandler la tang dieu phoi server]
    private static final Set<String> AUTH_REQUIRED = Set.of(
            "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
            "LIST_MY_ITEMS", "PLACE_BID", "GET_AUCTION_DETAIL",
            "GET_USER_LIST", "LOCK_USER", "UNLOCK_USER"
    );

    // ← THÊM: field DAO (null ở T4 — sẽ được gán thực sự ở T5)
    // Giữ package-private để test inject được mà không cần reflection
    final Object injectedUserDao;   // type Object tạm — T5 sẽ đổi thành UserDao
    final Object injectedItemDao;   // type Object tạm — T5 sẽ đổi thành ItemDao
    private final ItemDao itemDao;
    private final AuditLogService auditLogService;
    private final UserDao userDao;
    private final AdminUserService adminUserService;

    public RequestHandler() {
        this.injectedUserDao = null;
        this.injectedItemDao = null;
        this.auditLogService = new AuditLogService();
        this.userDao = new UserDao();
        this.itemDao = new ItemDao();
        this.adminUserService = new AdminUserService();
    }

    RequestHandler(Object injectedUserDao, Object injectedItemDao) {
        this.injectedUserDao = injectedUserDao;
        this.injectedItemDao = injectedItemDao;
        this.auditLogService = new AuditLogService();
        this.userDao = injectedUserDao instanceof UserDao
                ? (UserDao) injectedUserDao : new UserDao();
        this.itemDao = injectedItemDao instanceof ItemDao
                ? (ItemDao) injectedItemDao : new ItemDao();
        this.adminUserService = new AdminUserService();
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
        this.adminUserService = new AdminUserService();
    }

    /**
     * Xu ly 1 request JSON tu client, tra ve JSON response string.
     *
     * <p>Khong nem exception ra ngoai — moi loi wrap thanh error response.
     *
     * @param jsonLine dong JSON tho tu socket
     * @param session  session cua client
     * @return chuoi JSON response
     */
    public String handle(String jsonLine, Session session) {
        MessageRequest req;
        try {
            req = MessageMapper.fromJson(jsonLine, MessageRequest.class);
        } catch (Exception e) {
            return MessageMapper.toJson(
                    MessageResponse.error("UNKNOWN", "JSON khong hop le: " + e.getMessage()));
        }

        String type = req.getType() != null ? req.getType().toUpperCase() : "UNKNOWN";

        // 📌 [Tieu chi: Kien truc Client–Server — giai ma token truoc khi xu ly request]
        String token = req.getToken();
        if (token != null && !token.isBlank()) {
            SessionManager.getInstance().getUserIdByToken(token)
                    .ifPresent(session::setAuthenticatedUserId);
        }

        // 📌 [Tieu chi: Kien truc Client–Server — auth guard]
        if (AUTH_REQUIRED.contains(type) && !session.isAuthenticated()) {
            return MessageMapper.toJson(
                    MessageResponse.error(type, "Ban chua dang nhap. Vui long LOGIN truoc."));
        }

        try {
            JsonNode payload = req.getPayload() != null
                    ? req.getPayload() : JsonNodeFactory.instance.objectNode();

            return switch (type) {
                case "PING"          -> handlePing(session);
                case "LOGIN"         -> handleLogin(session, payload);
                case "REGISTER"      -> handleRegister(session, payload);
                case "LOGOUT"        -> handleLogout(session, req.getToken());
                // Khoa se them cac case sau
                case "CREATE_ITEM"   -> MessageMapper.toJson(
                        MessageResponse.error("CREATE_ITEM", "Chua implement — Khoa se them"));
                case "GET_ITEM_LIST" -> MessageMapper.toJson(
                        MessageResponse.error("GET_ITEM_LIST", "Chua implement — Khoa se them"));
                case "GET_ITEM_DETAIL" -> MessageMapper.toJson(
                        MessageResponse.error("GET_ITEM_DETAIL", "Chua implement — Khoa se them"));
                case "DELETE_ITEM"   -> MessageMapper.toJson(
                        MessageResponse.error("DELETE_ITEM", "Chua implement — Khoa se them"));
                case "GET_USER_LIST"  -> handleGetUserList(session, payload);
                case "LOCK_USER"      -> handleLockUser(session, payload);
                case "UNLOCK_USER"    -> handleUnlockUser(session, payload);
                default              -> MessageMapper.toJson(
                        MessageResponse.error(type, "Lenh khong xac dinh: " + type));
            };
        } catch (BidHubException e) {
            return MessageMapper.toJson(MessageResponse.error(type, e.getMessage()));
        } catch (Exception e) {
            System.err.println("[RequestHandler] Loi xu ly " + type + ": " + e.getMessage());
            return MessageMapper.toJson(MessageResponse.error(type, "Loi he thong noi bo."));
        }
    }

    // === AUTH HANDLERS ===

    /**
     * Xu ly LOGIN: xac thuc credentials → tao session → tra ve token + user info.
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — 1.0đ]
     *
     * @param session session hien tai
     * @param payload chua {username, password}
     * @return JSON response voi token neu thanh cong
     */
    private String handleLogin(Session session, JsonNode payload) {
        String username = getTextSafe(payload, "username");
        String password = getTextSafe(payload, "password");

        if (username == null || username.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "Ten dang nhap khong duoc de trong."));
        }
        if (password == null || password.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "Mat khau khong duoc de trong."));
        }

        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "Ten dang nhap hoac mat khau khong dung."));
        }

        User user = userOpt.get();
        // 📌 [Tieu chi: Xu ly loi & ngoai le — verify password khong tiet lo thong tin]
        if (!AuthService.verifyPassword(password, user.getPasswordHash())) {
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "Ten dang nhap hoac mat khau khong dung."));
        }

        // 📌 [Tieu chi: Ky thuat quan trong — tao session voi token UUID]
        String token = SessionManager.getInstance().createSession(user.getId());
        session.setAuthenticatedUserId(user.getId());

        // 📌 [Tieu chi: Quan ly nguoi dung — audit log login]
        auditLogService.log(user.getId(), AuditActions.USER_LOGIN, "{}");

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole().name());

        return MessageMapper.toJson(MessageResponse.ok("LOGIN", result));
    }

    /**
     * Xu ly REGISTER: validate input → kiem tra trung username → tao user → save.
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — 1.0đ + Xu ly loi & ngoai le — validation]
     *
     * @param session session hien tai
     * @param payload chua {username, password, email, role}
     * @return JSON response voi user info neu thanh cong
     */
    private String handleRegister(Session session, JsonNode payload) {
        String username = getTextSafe(payload, "username");
        String password = getTextSafe(payload, "password");
        String email = getTextSafe(payload, "email");
        String roleStr = getTextSafe(payload, "role");

        // 📌 [Tieu chi: Xu ly loi & ngoai le — validation dau vao]
        if (username == null || username.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Ten dang nhap khong duoc de trong."));
        }
        if (password == null || password.length() < 8) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Mat khau phai co it nhat 8 ky tu."));
        }
        if (email == null || !email.contains("@")) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Email khong hop le (phai chua @)."));
        }
        if (roleStr == null || "ADMIN".equalsIgnoreCase(roleStr)) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Khong the dang ky voi vai tro ADMIN."));
        }

        // 📌 [Tieu chi: Xu ly loi & ngoai le — trung username]
        if (userDao.existsByUsername(username)) {
            return MessageMapper.toJson(
                    MessageResponse.error("REGISTER", "Ten dang nhap da ton tai."));
        }

        // Tao user moi voi dung subclass
        UserRole role = "SELLER".equalsIgnoreCase(roleStr)
                ? UserRole.SELLER : UserRole.BIDDER;
        String hashedPassword = AuthService.hashPassword(password);

        User newUser;
        if (role == UserRole.SELLER) {
            newUser = new Seller(username, hashedPassword, email);
        } else {
            newUser = new Bidder(username, hashedPassword, email);
        }

        userDao.save(newUser);

        // 📌 [Tieu chi: Quan ly nguoi dung — audit log register]
        auditLogService.log(newUser.getId(), AuditActions.USER_REGISTER, "{}");

        Map<String, String> result = new HashMap<>();
        result.put("userId", newUser.getId());
        result.put("username", newUser.getUsername());
        result.put("role", newUser.getRole().name());

        return MessageMapper.toJson(MessageResponse.ok("REGISTER", result));
    }

    /**
     * Xu ly LOGOUT: invalidate session → clear authenticatedUserId.
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — logout + audit log]
     *
     * @param session session hien tai
     * @param token   token tu request
     * @return JSON response
     */
    private String handleLogout(Session session, String token) {
        String userId = session.getAuthenticatedUserId();

        // 📌 [Tieu chi: Quan ly nguoi dung — audit log logout truoc khi invalidate]
        if (userId != null) {
            auditLogService.log(userId, AuditActions.USER_LOGOUT, "{}");
        }

        if (token != null && !token.isBlank()) {
            SessionManager.getInstance().invalidateSession(token);
        }
        session.setAuthenticatedUserId(null);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Dang xuat thanh cong.");
        return MessageMapper.toJson(MessageResponse.ok("LOGOUT", result));
    }

    // === HELPER METHODS ===

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

    // === ADMIN HANDLERS ===

    /**
     * Xu ly lay danh sach nguoi dung — chi ADMIN.
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin xem danh sach user]
     *
     * @param session session cua client
     * @param payload payload rong
     * @return JSON response voi danh sach user
     */
    private String handleGetUserList(Session session, JsonNode payload) {
        SecurityContext.requireRole(session, UserRole.ADMIN);

        java.util.List<User> users = adminUserService.listAllUsers();
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (User u : users) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", u.getId());
            userInfo.put("username", u.getUsername());
            userInfo.put("email", u.getEmail());
            userInfo.put("role", u.getRole().name());
            userInfo.put("isLocked", u.isLocked());
            result.add(userInfo);
        }
        return MessageMapper.toJson(
            MessageResponse.ok("GET_USER_LIST", result));
    }

    /**
     * Xu ly khoa tai khoan — chi ADMIN.
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin khoa tai khoan]
     *
     * @param session session cua client
     * @param payload {targetUserId}
     * @return JSON response
     */
    private String handleLockUser(Session session, JsonNode payload) {
        String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
        String targetUserId = payload.path("targetUserId").asText("");

        if (targetUserId.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException(
                "targetUserId khong duoc de trong");
        }

        adminUserService.lockUser(targetUserId, adminId);

        return MessageMapper.toJson(MessageResponse.ok("LOCK_USER",
            Map.of("message", "Da khoa tai khoan.")));
    }

    /**
     * Xu ly mo khoa tai khoan — chi ADMIN.
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin mo khoa tai khoan]
     *
     * @param session session cua client
     * @param payload {targetUserId}
     * @return JSON response
     */
    private String handleUnlockUser(Session session, JsonNode payload) {
        String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
        String targetUserId = payload.path("targetUserId").asText("");

        if (targetUserId.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException(
                "targetUserId khong duoc de trong");
        }

        adminUserService.unlockUser(targetUserId, adminId);

        return MessageMapper.toJson(MessageResponse.ok("UNLOCK_USER",
            Map.of("message", "Da mo khoa tai khoan.")));
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

    /**
     * Doc string tu JsonNode an toan — tra ve null neu field khong ton tai.
     *
     * @param node JsonNode cha
     * @param field ten field
     * @return gia tri string hoac null
     */
    private String getTextSafe(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}