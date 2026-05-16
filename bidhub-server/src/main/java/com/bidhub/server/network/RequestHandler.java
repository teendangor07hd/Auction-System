package com.bidhub.server.network;

import com.bidhub.common.exception.*;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.*;
import com.bidhub.server.service.*;
import com.bidhub.server.dao.ItemDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.Bidder;
import com.bidhub.server.model.Seller;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;

import java.util.List;
import com.bidhub.server.event.BidUpdateEvent;
import com.bidhub.server.event.AuctionClosedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatcher chinh: nhan JSON tho → parse → auth-guard → switch type → goi handler.
 *
 * <p>Switch-case mo rong tung tuan ma khong refactor:
 * Tuan 4: PING · Tuan 5: LOGIN / REGISTER / LOGOUT / CREATE_ITEM / GET_ITEM_LIST ·
 * Tuan 6: PLACE_BID / LIST_AUCTIONS · Tuan 7+: chi them case.
 */
public final class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    // 📌 [Tieu chi: MVC — RequestHandler la tang dieu phoi server]
    private static final Set<String> AUTH_REQUIRED = Set.of(
            "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
            "LIST_MY_ITEMS", "CREATE_AUCTION", "PLACE_BID", "GET_AUCTION_DETAIL",
            "GET_USER_LIST", "LOCK_USER", "UNLOCK_USER",
            "GET_BID_HISTORY_REPORT", "GET_AUDIT_LOG", "RUN_INTEGRITY_CHECK",
            "SEND_NOTIFICATION", "GET_NOTIFICATIONS",
            "GET_MY_AUCTIONS", "UPDATE_ITEM", "CANCEL_AUCTION"
    );

    // ← THÊM: field DAO (null ở T4 — sẽ được gán thực sự ở T5)
    // Giữ package-private để test inject được mà không cần reflection
    final Object injectedUserDao;   // type Object tạm — T5 sẽ đổi thành UserDao
    final Object injectedItemDao;   // type Object tạm — T5 sẽ đổi thành ItemDao
    private final ItemDao itemDao;
    private final AuditLogService auditLogService;
    private final UserDao userDao;
    private final AdminUserService adminUserService;
    private final AuctionDao auctionDao;
    private final BidDao bidDao;
    private final BidValidator bidValidator;
    private final ReportService reportService;
    private final DataIntegrityService dataIntegrityService;

    public RequestHandler() {
        this.injectedUserDao = null;
        this.injectedItemDao = null;
        this.auditLogService = new AuditLogService();
        this.userDao = new UserDao();
        this.auctionDao = new AuctionDao();
        this.bidDao = new BidDao();
        this.itemDao = new ItemDao();
        this.adminUserService = new AdminUserService();
        this.bidValidator = new BidValidator(itemDao);
        this.reportService = new ReportService();
        this.dataIntegrityService = new DataIntegrityService();
    }

    RequestHandler(Object injectedUserDao, Object injectedItemDao) {
        this.injectedUserDao = injectedUserDao;
        this.injectedItemDao = injectedItemDao;
        this.auditLogService = new AuditLogService();
        this.userDao = injectedUserDao instanceof UserDao
                ? (UserDao) injectedUserDao : new UserDao();
        this.itemDao = injectedItemDao instanceof ItemDao
                ? (ItemDao) injectedItemDao : new ItemDao();
        this.auctionDao = new AuctionDao();
        this.bidDao = new BidDao();
        this.adminUserService = new AdminUserService();
        this.bidValidator = new BidValidator(itemDao);
        this.reportService = new ReportService();
        this.dataIntegrityService = new DataIntegrityService();
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
        this.auctionDao = new AuctionDao();
        this.bidDao = new BidDao();
        this.adminUserService = new AdminUserService();
        this.bidValidator = new BidValidator(itemDao);
        this.reportService = new ReportService();
        this.dataIntegrityService = new DataIntegrityService();
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
                case "CREATE_ITEM"   -> handleCreateItem(session, payload);
                case "GET_ITEM_LIST" -> handleGetItemList();
                case "GET_ITEM_DETAIL" -> handleGetItemDetail(payload);
                case "DELETE_ITEM"   -> handleDeleteItem(session, payload);
                case "LIST_MY_ITEMS" -> handleListMyItems(session, payload);
                case "CREATE_AUCTION" -> handleCreateAuction(session, payload);
                case "GET_USER_LIST"  -> handleGetUserList(session, payload);
                case "LOCK_USER"      -> handleLockUser(session, payload);
                case "UNLOCK_USER"    -> handleUnlockUser(session, payload);
                case "PLACE_BID"        -> handlePlaceBid(session, payload);
                case "GET_AUCTION_LIST"  -> handleGetAuctionList(session, payload);
                case "GET_AUCTION_DETAIL" -> handleGetAuctionDetail(session, payload);
                case "SUBSCRIBE_AUCTION" -> handleSubscribeAuction(session, payload);
                case "GET_AUCTION_REPORT"     -> handleGetAuctionReport(session, payload);
                case "GET_BID_HISTORY_REPORT" -> handleGetBidHistoryReport(session, payload);
                case "GET_AUDIT_LOG"          -> handleGetAuditLog(session, payload);
                case "RUN_INTEGRITY_CHECK"    -> handleRunIntegrityCheck(session, payload);
                case "GET_HOME_STATS"         -> handleGetHomeStats();
                case "SEND_NOTIFICATION"      -> handleSendNotification(session, payload);
                case "GET_NOTIFICATIONS"      -> handleGetNotifications(session, payload);
                case "GET_MY_AUCTIONS"        -> handleGetMyAuctions(session, payload);
                case "UPDATE_ITEM"            -> handleUpdateItem(session, payload);
                case "CANCEL_AUCTION"         -> handleCancelAuction(session, payload);
                default              -> MessageMapper.toJson(
                        MessageResponse.error(type, "Lệnh không xác định: " + type));
            };
        } catch (BidHubException e) {
            return MessageMapper.toJson(MessageResponse.error(type, e.getMessage()));
        } catch (Exception e) {
            logger.error("Loi xu ly {}: {}", type, e.getMessage(), e);
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
        // 📌 [Tieu chi: Quan ly nguoi dung — kiem tra tai khoan bi khoa]
        if (user.isLocked()) {
            auditLogService.log(user.getId(),
                    AuditActions.USER_LOGIN, "{\"blocked\":true}");
            return MessageMapper.toJson(
                    MessageResponse.error("LOGIN", "TAI KHOAN BI KHOA"));
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
            if (payload.has("imageUrl")) {
                item.setImageUrl(payload.get("imageUrl").asText(""));
            }
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
        java.util.List<Auction> allAuctions = auctionDao.findAll();

        // Build map itemId -> auction status
        java.util.Map<String, String> itemAuctionStatus = new HashMap<>();
        for (Auction auc : allAuctions) {
            String existing = itemAuctionStatus.get(auc.getItemId());
            // ưu tiên RUNNING > PENDING > CLOSED
            if (existing == null) {
                itemAuctionStatus.put(auc.getItemId(), auc.getStatus().name());
            } else if ("RUNNING".equals(auc.getStatus().name())) {
                itemAuctionStatus.put(auc.getItemId(), "RUNNING");
            }
        }

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Item item : items) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", item.getId());
            info.put("name", item.getName());
            info.put("description", item.getDescription());
            info.put("itemType", item.getItemType().name());
            info.put("startingPrice", item.getStartingPrice());
            info.put("imageUrl", item.getImageUrl());

            // Người bán
            String sellerName = "Không rõ";
            java.util.Optional<com.bidhub.server.model.User> sellerOpt = userDao.findById(item.getSellerId());
            if (sellerOpt.isPresent()) sellerName = sellerOpt.get().getUsername();
            info.put("sellerName", sellerName);

            // Trạng thái đấu giá
            String rawStatus = itemAuctionStatus.get(item.getId());
            String auctionStatus;
            if (rawStatus == null) {
                auctionStatus = "AVAILABLE";
            } else if ("RUNNING".equals(rawStatus)) {
                auctionStatus = "AUCTIONING";
            } else if ("CLOSED".equals(rawStatus) || "FINISHED".equals(rawStatus)) {
                auctionStatus = "SOLD";
            } else {
                auctionStatus = "AVAILABLE";
            }
            info.put("auctionStatus", auctionStatus);

            result.add(info);
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_ITEM_LIST", result));
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

    /**
     * Lay danh sach item cua seller hien tai.
     *
     * <p>// 📌 [Tieu chi: Quan ly san pham — seller xem item cua minh]
     *
     * @param session session cua client
     * @param payload payload rong
     * @return JSON response voi danh sach item cua seller
     */
    private String handleListMyItems(Session session, JsonNode payload) {
        String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);
        java.util.List<Item> items = itemDao.findBySellerId(sellerId);
        java.util.List<Auction> allAuctions = auctionDao.findAll();

        // Build map itemId -> auction status (giống handleGetItemList)
        java.util.Map<String, String> itemAuctionStatus = new HashMap<>();
        for (Auction auc : allAuctions) {
            String ex = itemAuctionStatus.get(auc.getItemId());
            if (ex == null || "RUNNING".equals(auc.getStatus().name())) {
                itemAuctionStatus.put(auc.getItemId(), auc.getStatus().name());
            }
        }

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Item item : items) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", item.getId());
            info.put("itemId", item.getId()); // giữ lại vì SellerDashboard có thể dùng cả 2
            info.put("name", item.getName());
            info.put("description", item.getDescription());
            info.put("itemType", item.getItemType().name());
            info.put("startingPrice", item.getStartingPrice());
            info.put("imageUrl", item.getImageUrl());

            String rawStatus = itemAuctionStatus.get(item.getId());
            String auctionStatus;
            if (rawStatus == null) {
                auctionStatus = "AVAILABLE";
            } else if ("RUNNING".equals(rawStatus)) {
                auctionStatus = "AUCTIONING";
            } else if ("CLOSED".equals(rawStatus) || "FINISHED".equals(rawStatus)) {
                auctionStatus = "SOLD";
            } else {
                auctionStatus = "AVAILABLE";
            }
            info.put("auctionStatus", auctionStatus);

            result.add(info);
        }
        return MessageMapper.toJson(MessageResponse.ok("LIST_MY_ITEMS", result));
    }

    // === AUCTION HANDLERS ===

    /**
     * Tao phien dau gia moi — yeu cau role SELLER.
     *
     * <p>// 📌 [Tieu chi: Chuc nang dau gia — tao phien moi + audit log]
     *
     * @param session session cua client
     * @param payload chua {itemId, startingPrice, minimumIncrement, startTime, endTime}
     * @return JSON response voi auction info
     */
    private String handleCreateAuction(Session session, JsonNode payload) {
        String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);

        String itemId = getTextSafe(payload, "itemId");
        if (itemId == null || itemId.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION", "itemId khong duoc de trong."));
        }

        // Kiem tra item ton tai va thuoc ve seller
        java.util.Optional<Item> itemOpt = itemDao.findById(itemId);
        if (itemOpt.isEmpty()) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION", "San pham khong ton tai."));
        }
        Item item = itemOpt.get();
        if (!item.getSellerId().equals(sellerId)) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION",
                            "Ban khong co quyen tao phien cho san pham nay."));
        }

        // Parse gia khoi diem
        double startingPrice;
        if (payload.has("startingPrice") && payload.get("startingPrice").isNumber()) {
            startingPrice = payload.get("startingPrice").asDouble();
        } else {
            String priceStr = getTextSafe(payload, "startingPrice");
            if (priceStr == null) {
                return MessageMapper.toJson(
                        MessageResponse.error("CREATE_AUCTION", "Gia khoi diem la bat buoc."));
            }
            try {
                startingPrice = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                return MessageMapper.toJson(
                        MessageResponse.error("CREATE_AUCTION", "Gia khoi diem khong hop le."));
            }
        }
        if (startingPrice <= 0) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION", "Gia khoi diem phai lon hon 0."));
        }

        // Parse minimum increment
        double minimumIncrement = 1.0;
        if (payload.has("minimumIncrement")) {
            if (payload.get("minimumIncrement").isNumber()) {
                minimumIncrement = payload.get("minimumIncrement").asDouble();
            } else {
                String incStr = getTextSafe(payload, "minimumIncrement");
                if (incStr != null && !incStr.isBlank()) {
                    try {
                        minimumIncrement = Double.parseDouble(incStr);
                    } catch (NumberFormatException e) {
                        return MessageMapper.toJson(
                                MessageResponse.error("CREATE_AUCTION",
                                        "Buoc gia khong hop le."));
                    }
                }
            }
        }
        if (minimumIncrement < 0) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION",
                            "Buoc gia khong duoc am."));
        }

        // Parse thoi gian
        String startTimeStr = getTextSafe(payload, "startTime");
        String endTimeStr = getTextSafe(payload, "endTime");
        if (startTimeStr == null || endTimeStr == null) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION",
                            "Thoi gian bat dau va ket thuc la bat buoc."));
        }

        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            startTime = LocalDateTime.parse(startTimeStr);
            endTime = LocalDateTime.parse(endTimeStr);
        } catch (Exception e) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION",
                            "Dinh dang thoi gian khong hop le (yyyy-MM-ddTHH:mm:ss)."));
        }

        if (!endTime.isAfter(startTime)) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION",
                            "Thoi gian ket thuc phai sau thoi gian bat dau."));
        }

        // Tao Auction object
        Auction auction = new Auction(itemId, startTime, endTime, startingPrice, minimumIncrement);

        // Luu vao DB
        auctionDao.save(auction);

        // Them vao RAM cache (AuctionManager)
        AuctionManager.getInstance().addAuction(auction);

        // Audit log
        auditLogService.log(sellerId, AuditActions.AUCTION_CREATED,
                "{\"auctionId\":\"" + auction.getId()
                        + "\",\"itemId\":\"" + itemId + "\"}");

        Map<String, Object> result = new HashMap<>();
        result.put("auctionId", auction.getId());
        result.put("itemId", itemId);
        result.put("startingPrice", startingPrice);
        result.put("status", auction.getStatus().name());

        return MessageMapper.toJson(MessageResponse.ok("CREATE_AUCTION", result));
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
            targetUserId = payload.path("userId").asText("");
        }

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
            targetUserId = payload.path("userId").asText("");
        }

        if (targetUserId.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException(
                "targetUserId khong duoc de trong");
        }

        adminUserService.unlockUser(targetUserId, adminId);

        return MessageMapper.toJson(MessageResponse.ok("UNLOCK_USER",
            Map.of("message", "Da mo khoa tai khoan.")));
    }

    // --- 4. Handler method ---

    /**
     * Xu ly dat gia — validate, luu bid, cap nhat RAM va DB.
     *
     * <p>// 📌 [Tieu chi: Chuc nang dau gia — place bid flow day du]
     * // 📌 [Tieu chi: MVC — handler la tang dieu phoi]
     *
     * @param session session cua client
     * @param payload {auctionId, bidAmount}
     * @return JSON response
     */
    private String handlePlaceBid(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);

        String auctionId = payload.path("auctionId").asText("");
        double bidAmount = payload.path("bidAmount").asDouble(0.0);

        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }
        if (bidAmount <= 0) {
            throw new InvalidBidException("Gia dat phai lon hon 0.");
        }

        // Lay auction tu RAM (AuctionManager)
        Auction auction = AuctionManager.getInstance().getAuction(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(
                        "Phien dau gia khong ton tai: " + auctionId));

        // 📌 [Tieu chi: Ky thuat quan trọng — ReentrantLock granular locking]
        // Lock toan bo logic bid de chong lost update va race condition
        auction.getLock().lock();
        try {
            // Validate 5 dieu kien
            bidValidator.validate(auction, userId, bidAmount);

            // Tao BidTransaction
            BidTransaction bid = new BidTransaction(auctionId, userId, bidAmount);

            // Luu bid vao DB
            bidDao.save(bid);

            // Cap nhat RAM
            auction.setCurrentHighestBid(bidAmount);
            auction.setHighestBidderId(userId);

            // Cap nhat DB
            auctionDao.updateHighestBid(auctionId, bidAmount, userId);
            // 📌 [Tieu chi: Anti-Sniping — kiem tra va gia han neu bid trong snipe window]
            // Chay trong lock block → thread-safe, khong race voi lifecycle task
            AntiSnipingEngine antiSnipingEngine = new AntiSnipingEngine();
            antiSnipingEngine.check(auction);

            // 📌 [Tieu chi: Audit Log — log PLACE_BID trong lock block, sau DB save thanh cong]
            // Dam bao audit log duoc ghi truoc khi unlock cho bid khac
            auditLogService.log(userId, AuditActions.PLACE_BID,
                    "{\"auctionId\":\"" + auctionId
                            + "\",\"bidAmount\":" + bidAmount + "}");
        } finally {
            auction.getLock().unlock();
        }

        // Lay ten nguoi dung
        String bidderName = userId;
        java.util.Optional<com.bidhub.server.model.User> userOpt = userDao.findById(userId);
        if (userOpt.isPresent()) {
            bidderName = userOpt.get().getUsername();
        }

        // 📌 [Tieu chi: Realtime update — publish BID_UPDATE sau unlock]
        NotificationBroker.getInstance().publish(auctionId,
                new BidUpdateEvent(auctionId, userId, bidderName, bidAmount));

        // NotificationBroker publish (sau khi unlock — Week 7, Quốc Minh them)
        // NotificationBroker.getInstance().publish(auctionId, new BidUpdateEvent(...));

        return MessageMapper.toJson(MessageResponse.ok("PLACE_BID",
                Map.of("auctionId", auctionId,
                        "currentHighestBid", bidAmount,
                        "highestBidderId", userId)));
    }

    /**
     * Xu ly lay danh sach auction dang active.
     *
     * <p>// 📌 [Tieu chi: MVC — handler truy xuat du lieu tu DAO]
     *
     * @param session session cua client
     * @param payload payload rong
     * @return JSON response voi danh sach auction
     */
    private String handleGetAuctionList(Session session, JsonNode payload) {
        List<Auction> auctions = auctionDao.findActiveAuctions();

        // Enrich voi ten san pham tu ItemDao
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Auction auction : auctions) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", auction.getId());
            info.put("itemId", auction.getItemId());
            info.put("startingPrice", auction.getStartingPrice());
            info.put("currentHighestBid", auction.getCurrentHighestBid());
            info.put("highestBidderId", auction.getHighestBidderId());
            info.put("startTime", auction.getStartTime() != null ? auction.getStartTime().toString() : "");
            info.put("endTime", auction.getEndTime() != null ? auction.getEndTime().toString() : "");
            info.put("status", auction.getStatus().name());
            info.put("minimumIncrement", auction.getMinimumIncrement());

            // Tra cuu ten san pham, anh san pham, ten nguoi ban tu ItemDao va UserDao
            String itemName = "San pham khong xac dinh";
            String imageUrl = null;
            String sellerName = "Khong xac dinh";
            java.util.Optional<Item> itemOpt = itemDao.findById(auction.getItemId());
            if (itemOpt.isPresent()) {
                Item item = itemOpt.get();
                itemName = item.getName();
                imageUrl = item.getImageUrl();
                java.util.Optional<com.bidhub.server.model.User> sellerOpt = userDao.findById(item.getSellerId());
                if (sellerOpt.isPresent()) {
                    sellerName = sellerOpt.get().getUsername();
                }
            }
            info.put("itemName", itemName);
            info.put("imageUrl", imageUrl);
            info.put("sellerName", sellerName);
            if (itemOpt.isPresent()) {
                info.put("itemType", itemOpt.get().getItemType().name());
            } else {
                info.put("itemType", "");
            }

            result.add(info);
        }

        return MessageMapper.toJson(
                MessageResponse.ok("GET_AUCTION_LIST", result));
    }

    /**
     * Xu ly lay chi tiet 1 auction.
     *
     * <p>// 📌 [Tieu chi: MVC — handler truy xuat chi tiet tu DAO + BidDao]
     *
     * @param session session cua client
     * @param payload {auctionId}
     * @return JSON response voi chi tiet auction + bid history
     */
    private String handleGetAuctionDetail(Session session, JsonNode payload) {
        String auctionId = payload.path("auctionId").asText("");
        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }

        // Thu lay tu RAM truoc, neu khong co thi lay tu DB
        Auction auction = AuctionManager.getInstance().getAuction(auctionId)
                .orElseGet(() -> auctionDao.findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(
                                "Phien dau gia khong ton tai: " + auctionId)));

        // Lay lich su bid
        List<BidTransaction> bids = bidDao.findByAuctionId(auctionId);
        List<Map<String, Object>> bidHistory = new java.util.ArrayList<>();
        for (BidTransaction b : bids) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("bidAmount", b.getBidAmount());
            map.put("bidTime", b.getBidTime().toString());
            map.put("bidderId", b.getBidderId());
            
            java.util.Optional<com.bidhub.server.model.User> uOpt = userDao.findById(b.getBidderId());
            map.put("bidderName", uOpt.map(com.bidhub.server.model.User::getUsername).orElse(b.getBidderId()));
            bidHistory.add(map);
        }

        // Enrich auction data voi ten san pham
        Map<String, Object> auctionInfo = new HashMap<>();
        auctionInfo.put("id", auction.getId());
        auctionInfo.put("itemId", auction.getItemId());
        auctionInfo.put("startingPrice", auction.getStartingPrice());
        auctionInfo.put("currentHighestBid", auction.getCurrentHighestBid());
        auctionInfo.put("highestBidderId", auction.getHighestBidderId());

        String bidderId = auction.getHighestBidderId();
        if (bidderId != null) {
            java.util.Optional<com.bidhub.server.model.User> userOpt = userDao.findById(bidderId);
            auctionInfo.put("highestBidderName", userOpt.map(com.bidhub.server.model.User::getUsername).orElse(bidderId));
        } else {
            auctionInfo.put("highestBidderName", "Chưa có");
        }

        auctionInfo.put("startTime", auction.getStartTime() != null ? auction.getStartTime().toString() : "");
        auctionInfo.put("endTime", auction.getEndTime() != null ? auction.getEndTime().toString() : "");
        auctionInfo.put("status", auction.getStatus().name());
        auctionInfo.put("minimumIncrement", auction.getMinimumIncrement());

        // Tra cuu ten va mo ta san pham
        java.util.Optional<Item> itemOpt = itemDao.findById(auction.getItemId());
        if (itemOpt.isPresent()) {
            Item item = itemOpt.get();
            auctionInfo.put("itemName", item.getName());
            auctionInfo.put("description", item.getDescription());
            auctionInfo.put("imageUrl", item.getImageUrl());
        } else {
            auctionInfo.put("itemName", "San pham khong xac dinh");
            auctionInfo.put("description", "");
            auctionInfo.put("imageUrl", null);
        }

        return MessageMapper.toJson(MessageResponse.ok("GET_AUCTION_DETAIL",
                Map.of("auction", auctionInfo, "bidHistory", bidHistory)));
    }

    /**
     * Xu ly subscribe realtime event cho auction.
     *
     * <p>// 📌 [Tieu chi: Realtime update — Observer Pattern subscribe]
     *
     * @param session session cua client
     * @param payload {auctionId}
     * @return JSON response
     */
    private String handleSubscribeAuction(Session session, JsonNode payload) {
        String auctionId = payload.path("auctionId").asText("");
        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }
        NotificationBroker.getInstance().subscribe(auctionId, session);
        return MessageMapper.toJson(
                MessageResponse.ok("SUBSCRIBE_AUCTION",
                        Map.of("auctionId", auctionId, "message", "Da subscribe thanh cong")));
    }

  /**
   * Xu ly kiem tra toan ven du lieu (ADMIN only).
   *
   * <p>// 📌 [Tieu chi: Clean Code — Admin kiem tra data consistency]
   *
   * @param session session cua client
   * @param payload payload rong
   * @return JSON response voi ket qua integrity check
   */
  private String handleRunIntegrityCheck(Session session, JsonNode payload) {
    String userId = SecurityContext.requireRole(session, UserRole.ADMIN);

    // Audit log
    auditLogService.log(
        userId,
        AuditActions.RUN_INTEGRITY_CHECK,
        "{}");

    Map<String, Object> result = dataIntegrityService.runFullCheck();
    return MessageMapper.toJson(MessageResponse.ok("RUN_INTEGRITY_CHECK", result));
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

    /**
     * Xu ly lay bao cao auction (SELLER hoac ADMIN).
     *
     * <p>// 📌 [Tieu chi: MVC — handler truy xuat bao cao tu Service]
     *
     * @param session session cua client
     * @param payload payload rong
     * @return JSON response voi danh sach auction report
     */
    private String handleGetAuctionReport(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        // Cho phep SELLER va ADMIN xem auction report
        return MessageMapper.toJson(MessageResponse.ok("GET_AUCTION_REPORT",
                reportService.exportAuctionReport()));
    }

    /**
     * Xu ly lay lich su bid cua auction.
     *
     * @param session session cua client
     * @param payload {auctionId}
     * @return JSON response voi danh sach bid
     */
    private String handleGetBidHistoryReport(Session session, JsonNode payload) {
        SecurityContext.requireAuthenticated(session);
        String auctionId = payload.path("auctionId").asText("");
        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_BID_HISTORY_REPORT",
                reportService.exportBidHistory(auctionId)));
    }

    /**
     * Xu ly lay audit log (ADMIN only).
     *
     * <p>// 📌 [Tieu chi: Quan ly nguoi dung — Admin xem audit log]
     *
     * @param session session cua client
     * @param payload {limit} (default 50)
     * @return JSON response voi danh sach audit log
     */
    private String handleGetAuditLog(Session session, JsonNode payload) {
        SecurityContext.requireRole(session, UserRole.ADMIN);
        int limit = payload.path("limit").asInt(50);
        if (limit <= 0 || limit > 500) {
            limit = 50;
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_AUDIT_LOG",
                reportService.exportAuditLog(limit)));
    }

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

    private String handleGetHomeStats() {
        List<Auction> allAuctions = auctionDao.findAll();
        long activeCount = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .count();
        
        double totalVolume = allAuctions.stream()
                .mapToDouble(Auction::getCurrentHighestBid)
                .sum();

        // Unique bidders
        long participants = bidDao.findAll().stream()
                .map(BidTransaction::getBidderId)
                .distinct()
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeAuctions", activeCount);
        stats.put("totalParticipants", participants);
        stats.put("totalVolume", totalVolume);

        return MessageMapper.toJson(MessageResponse.ok("GET_HOME_STATS", stats));
    }

    /**
     * Gửi thông báo toàn server — chỉ ADMIN.
     * Lưu thông báo vào AuditLog để có thể truy vấn lại.
     *
     * @param session session của admin
     * @param payload {title, message, type}
     * @return JSON response
     */
    private String handleSendNotification(Session session, JsonNode payload) {
        String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
        String title = getTextSafe(payload, "title");
        String message = getTextSafe(payload, "message");
        String type = getTextSafe(payload, "type");

        if (title == null || title.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException("Tiêu đề thông báo không được để trống.");
        }
        if (message == null || message.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException("Nội dung thông báo không được để trống.");
        }

        String notifJson = "{\"title\":\"" + title.replace("\"", "\\\"") +
                "\",\"message\":\"" + message.replace("\"", "\\\"") +
                "\",\"type\":\"" + (type != null ? type : "SYSTEM") + "\"}";

        // Lưu thông báo dưới dạng audit log với action BROADCAST_NOTIFICATION
        auditLogService.log(adminId, "BROADCAST_NOTIFICATION", notifJson);

        return MessageMapper.toJson(MessageResponse.ok("SEND_NOTIFICATION",
                Map.of("message", "Đã gửi thông báo đến toàn bộ người dùng.",
                        "title", title)));
    }

    /**
     * Lấy danh sách thông báo hệ thống.
     *
     * @param session session của người dùng
     * @param payload payload rỗng
     * @return JSON response với danh sách thông báo
     */
    private String handleGetNotifications(Session session, JsonNode payload) {
        SecurityContext.requireAuthenticated(session);
        // Lấy các broadcast notification từ audit log
        List<Map<String, Object>> notifications = new java.util.ArrayList<>();
        try {
            // Truy vấn audit log entries có action = BROADCAST_NOTIFICATION
            List<Map<String, Object>> auditLogs = reportService.exportAuditLog(100);
            for (Map<String, Object> log : auditLogs) {
                String action = String.valueOf(log.getOrDefault("action", ""));
                if ("BROADCAST_NOTIFICATION".equals(action)) {
                    String detail = String.valueOf(log.getOrDefault("details", "{}"));
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                        JsonNode detailNode = om.readTree(detail);
                        Map<String, Object> notif = new HashMap<>();
                        notif.put("id", log.getOrDefault("id", ""));
                        notif.put("title", detailNode.path("title").asText("Thông báo"));
                        notif.put("message", detailNode.path("message").asText(""));
                        notif.put("type", detailNode.path("type").asText("SYSTEM"));
                        notif.put("createdAt", log.getOrDefault("createdAt", ""));
                        notif.put("isRead", false);
                        notifications.add(notif);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            logger.warn("Không thể lấy danh sách thông báo: {}", e.getMessage());
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_NOTIFICATIONS", notifications));
    }

    /**
     * Lấy danh sách phiên đấu giá của seller hiện tại.
     *
     * @param session session của seller
     * @param payload payload rỗng
     * @return JSON response với danh sách phiên
     */
    private String handleGetMyAuctions(Session session, JsonNode payload) {
        String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);
        List<Auction> allAuctions = auctionDao.findAll();

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Auction auc : allAuctions) {
            // Chỉ lấy auction của seller này
            java.util.Optional<Item> itemOpt = itemDao.findById(auc.getItemId());
            if (itemOpt.isEmpty()) continue;
            Item item = itemOpt.get();
            if (!item.getSellerId().equals(sellerId)) continue;

            Map<String, Object> info = new HashMap<>();
            info.put("id", auc.getId());
            info.put("itemId", auc.getItemId());
            info.put("itemName", item.getName());
            info.put("imageUrl", item.getImageUrl());
            info.put("startingPrice", auc.getStartingPrice());
            info.put("currentHighestBid", auc.getCurrentHighestBid());
            info.put("status", auc.getStatus().name());
            info.put("startTime", auc.getStartTime() != null ? auc.getStartTime().toString() : "");
            info.put("endTime", auc.getEndTime() != null ? auc.getEndTime().toString() : "");
            result.add(info);
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_MY_AUCTIONS", result));
    }

    /**
     * Cập nhật thông tin sản phẩm — chỉ seller sở hữu mới được sửa.
     *
     * @param session session của seller
     * @param payload {itemId, name, description, startingPrice}
     * @return JSON response
     */
    private String handleUpdateItem(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String itemId = getTextSafe(payload, "itemId");
        if (itemId == null || itemId.isBlank()) {
            return MessageMapper.toJson(MessageResponse.error("UPDATE_ITEM", "itemId không được để trống."));
        }

        java.util.Optional<Item> itemOpt = itemDao.findById(itemId);
        if (itemOpt.isEmpty()) {
            return MessageMapper.toJson(MessageResponse.error("UPDATE_ITEM", "Sản phẩm không tồn tại."));
        }
        Item item = itemOpt.get();
        if (!item.getSellerId().equals(userId)) {
            return MessageMapper.toJson(MessageResponse.error("UPDATE_ITEM", "Bạn không có quyền sửa sản phẩm này."));
        }

        // Cập nhật tên
        String newName = getTextSafe(payload, "name");
        if (newName != null && !newName.isBlank()) item.setName(newName);

        // Cập nhật mô tả
        String newDesc = getTextSafe(payload, "description");
        if (newDesc != null) item.setDescription(newDesc);

        // Cập nhật giá
        double newPrice = -1;
        if (payload.has("startingPrice") && payload.get("startingPrice").isNumber()) {
            newPrice = payload.get("startingPrice").asDouble();
        }

        // Dùng updateItem() (UPDATE SQL) thay vì save() (INSERT SQL) để tránh lỗi duplicate key
        itemDao.updateItem(itemId,
                newName != null && !newName.isBlank() ? newName : null,
                newDesc,
                newPrice);

        auditLogService.log(userId, "ITEM_UPDATED",
                "{\"itemId\":\"" + itemId + "\",\"newName\":\"" + item.getName() + "\"}");

        return MessageMapper.toJson(MessageResponse.ok("UPDATE_ITEM",
                Map.of("message", "Đã cập nhật sản phẩm.", "itemId", itemId)));
    }

    /**
     * Hủy phiên đấu giá — chỉ seller sở hữu và chỉ khi PENDING mới được hủy.
     *
     * @param session session của seller
     * @param payload {auctionId}
     * @return JSON response
     */
    private String handleCancelAuction(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String auctionId = getTextSafe(payload, "auctionId");
        if (auctionId == null || auctionId.isBlank()) {
            return MessageMapper.toJson(MessageResponse.error("CANCEL_AUCTION", "auctionId không được để trống."));
        }

        java.util.Optional<Auction> aucOpt = auctionDao.findById(auctionId);
        if (aucOpt.isEmpty()) {
            return MessageMapper.toJson(MessageResponse.error("CANCEL_AUCTION", "Phiên đấu giá không tồn tại."));
        }
        Auction auc = aucOpt.get();

        // Kiểm tra quyền sở hữu
        java.util.Optional<Item> itemOpt = itemDao.findById(auc.getItemId());
        if (itemOpt.isEmpty() || !itemOpt.get().getSellerId().equals(userId)) {
            return MessageMapper.toJson(MessageResponse.error("CANCEL_AUCTION", "Bạn không có quyền hủy phiên này."));
        }

        // Chỉ cho phép hủy khi chưa bắt đầu
        if (!"PENDING".equals(auc.getStatus().name())) {
            return MessageMapper.toJson(MessageResponse.error("CANCEL_AUCTION",
                    "Chỉ có thể hủy phiên đang ở trạng thái Chờ bắt đầu."));
        }

        auctionDao.deleteById(auctionId);
        auditLogService.log(userId, "AUCTION_CANCELLED",
                "{\"auctionId\":\"" + auctionId + "\"}");

        return MessageMapper.toJson(MessageResponse.ok("CANCEL_AUCTION",
                Map.of("message", "Đã hủy phiên đấu giá.", "auctionId", auctionId)));
    }
}