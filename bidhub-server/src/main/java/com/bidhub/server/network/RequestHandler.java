package com.bidhub.server.network;

import com.bidhub.common.exception.*;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.model.*;
import com.bidhub.server.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatcher chinh: nhan JSON tho → parse → auth-guard → switch type → goi delegate handler.
 */
public final class RequestHandler {

    static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private static final Set<String> AUTH_REQUIRED = Set.of(
            "LOGOUT", "CREATE_ITEM", "DELETE_ITEM",
            "LIST_MY_ITEMS", "CREATE_AUCTION", "PLACE_BID",
            "GET_USER_LIST", "LOCK_USER", "UNLOCK_USER",
            "GET_BID_HISTORY_REPORT", "GET_AUDIT_LOG", "RUN_INTEGRITY_CHECK",
            "SEND_NOTIFICATION", "GET_NOTIFICATIONS",
            "GET_MY_AUCTIONS", "UPDATE_ITEM", "CANCEL_AUCTION",
            "MARK_NOTIFICATION_READ",
            "ADMIN_STOP_AUCTION", "ADMIN_DELETE_AUCTION",
            "GET_WON_AUCTIONS",
            "MARK_PAID",
            "SELLER_CANCEL_FINISHED"
    );

    static final java.util.Map<String, java.util.Set<String>> userReadNotifications = new java.util.concurrent.ConcurrentHashMap<>();

    final ItemDao itemDao;
    final AuditLogService auditLogService;
    final UserDao userDao;
    final AdminUserService adminUserService;
    final AuctionDao auctionDao;
    final BidDao bidDao;
    final BidValidator bidValidator;
    final ReportService reportService;
    final DataIntegrityService dataIntegrityService;
    final AntiSnipingEngine antiSnipingEngine;

    private final AuthHandler authHandler;
    private final ItemHandler itemHandler;
    private final AuctionHandler auctionHandler;
    private final AdminHandler adminHandler;
    private final ReportHandler reportHandler;

    public RequestHandler() {
        this(new UserDao(), new ItemDao(), new AuditLogService());
    }

    public RequestHandler(UserDao userDao, ItemDao itemDao) {
        this(userDao, itemDao, new AuditLogService());
    }

    public RequestHandler(UserDao userDao, ItemDao itemDao, AuditLogService auditLogService) {
        this.userDao = userDao;
        this.itemDao = itemDao;
        this.auditLogService = auditLogService;
        this.auctionDao = new AuctionDao();
        this.bidDao = new BidDao();
        this.adminUserService = new AdminUserService();
        this.bidValidator = new BidValidator(itemDao);
        this.reportService = new ReportService();
        this.dataIntegrityService = new DataIntegrityService();
        this.antiSnipingEngine = new AntiSnipingEngine();

        this.authHandler = new AuthHandler(this);
        this.itemHandler = new ItemHandler(this);
        this.auctionHandler = new AuctionHandler(this);
        this.adminHandler = new AdminHandler(this);
        this.reportHandler = new ReportHandler(this);
    }

    /**
     * Xử lý 1 request JSON từ client, trả về JSON response string.
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

        String token = req.getToken();
        if (token != null && !token.isBlank() && !session.isAuthenticated()) {
            SessionManager.getInstance().getUserIdByToken(token)
                    .ifPresent(session::setAuthenticatedUserId);
        }

        if (AUTH_REQUIRED.contains(type) && !session.isAuthenticated()) {
            return MessageMapper.toJson(
                    MessageResponse.error(type, "Ban chua dang nhap. Vui long LOGIN truoc."));
        }

        try {
            JsonNode payload = req.getPayload() != null
                    ? req.getPayload() : JsonNodeFactory.instance.objectNode();

            return switch (type) {
                case "PING"                  -> handlePing(session);
                case "LOGIN"                 -> authHandler.handleLogin(session, payload);
                case "REGISTER"              -> authHandler.handleRegister(session, payload);
                case "LOGOUT"                -> authHandler.handleLogout(session, req.getToken());
                case "CREATE_ITEM"           -> itemHandler.handleCreateItem(session, payload);
                case "GET_ITEM_LIST"         -> itemHandler.handleGetItemList();
                case "GET_ITEM_DETAIL"       -> itemHandler.handleGetItemDetail(payload);
                case "DELETE_ITEM"           -> itemHandler.handleDeleteItem(session, payload);
                case "LIST_MY_ITEMS"         -> itemHandler.handleListMyItems(session, payload);
                case "CREATE_AUCTION"        -> auctionHandler.handleCreateAuction(session, payload);
                case "GET_USER_LIST"         -> adminHandler.handleGetUserList(session, payload);
                case "LOCK_USER"             -> adminHandler.handleLockUser(session, payload);
                case "UNLOCK_USER"           -> adminHandler.handleUnlockUser(session, payload);
                case "PLACE_BID"             -> auctionHandler.handlePlaceBid(session, payload);
                case "GET_AUCTION_LIST"      -> auctionHandler.handleGetAuctionList(session, payload);
                case "GET_AUCTION_DETAIL"    -> auctionHandler.handleGetAuctionDetail(session, payload);
                case "SUBSCRIBE_AUCTION"     -> auctionHandler.handleSubscribeAuction(session, payload);
                case "GET_AUCTION_REPORT"    -> reportHandler.handleGetAuctionReport(session, payload);
                case "GET_BID_HISTORY_REPORT"-> reportHandler.handleGetBidHistoryReport(session, payload);
                case "GET_AUDIT_LOG"         -> adminHandler.handleGetAuditLog(session, payload);
                case "RUN_INTEGRITY_CHECK"   -> adminHandler.handleRunIntegrityCheck(session, payload);
                case "GET_HOME_STATS"        -> handleGetHomeStats();
                case "SEND_NOTIFICATION"     -> adminHandler.handleSendNotification(session, payload);
                case "GET_NOTIFICATIONS"     -> adminHandler.handleGetNotifications(session, payload);
                case "GET_MY_AUCTIONS"       -> auctionHandler.handleGetMyAuctions(session, payload);
                case "UPDATE_ITEM"           -> itemHandler.handleUpdateItem(session, payload);
                case "CANCEL_AUCTION"        -> auctionHandler.handleCancelAuction(session, payload);
                case "MARK_NOTIFICATION_READ"-> adminHandler.handleMarkNotificationRead(session, payload);
                case "ADMIN_STOP_AUCTION"    -> adminHandler.handleAdminStopAuction(session, payload);
                case "ADMIN_DELETE_AUCTION"  -> adminHandler.handleAdminDeleteAuction(session, payload);
                case "GET_WON_AUCTIONS"      -> auctionHandler.handleGetWonAuctions(session, payload);
                case "MARK_PAID"             -> auctionHandler.handleMarkPaid(session, payload);
                case "SELLER_CANCEL_FINISHED" -> auctionHandler.handleSellerCancelFinished(session, payload);
                default                      -> MessageMapper.toJson(
                        MessageResponse.error(type, "Lệnh không xác định: " + type));
            };
        } catch (BidHubException e) {
            return MessageMapper.toJson(MessageResponse.error(type, e.getMessage()));
        } catch (Exception e) {
            logger.error("Loi xu ly {}: {}", type, e.getMessage(), e);
            return MessageMapper.toJson(MessageResponse.error(type, "Loi he thong noi bo."));
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

    private String handleGetHomeStats() {
        long activeCount = auctionDao.countByStatus(AuctionStatus.RUNNING);
        double totalVolume = auctionDao.sumHighestBids();
        long participants = bidDao.countDistinctBidders();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeAuctions", activeCount);
        stats.put("totalParticipants", participants);
        stats.put("totalVolume", totalVolume);

        return MessageMapper.toJson(MessageResponse.ok("GET_HOME_STATS", stats));
    }

    java.util.Map<String, Object> parseExtras(JsonNode extrasNode) {
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

    static String getTextSafe(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}