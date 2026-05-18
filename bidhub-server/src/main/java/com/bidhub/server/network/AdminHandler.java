package com.bidhub.server.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.model.*;
import com.bidhub.server.service.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

class AdminHandler {
    private final RequestHandler handler;

    AdminHandler(RequestHandler handler) {
        this.handler = handler;
    }

    String handleGetUserList(Session session, JsonNode payload) {
        SecurityContext.requireRole(session, UserRole.ADMIN);

        java.util.List<User> users = handler.adminUserService.listAllUsers();
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

    String handleLockUser(Session session, JsonNode payload) {
        String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
        String targetUserId = payload.path("targetUserId").asText("");
        if (targetUserId.isBlank()) {
            targetUserId = payload.path("userId").asText("");
        }

        if (targetUserId.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException(
                "targetUserId khong duoc de trong");
        }

        handler.adminUserService.lockUser(targetUserId, adminId);

        return MessageMapper.toJson(MessageResponse.ok("LOCK_USER",
            Map.of("message", "Da khoa tai khoan.")));
    }

    String handleUnlockUser(Session session, JsonNode payload) {
        String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
        String targetUserId = payload.path("targetUserId").asText("");
        if (targetUserId.isBlank()) {
            targetUserId = payload.path("userId").asText("");
        }

        if (targetUserId.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException(
                "targetUserId khong duoc de trong");
        }

        handler.adminUserService.unlockUser(targetUserId, adminId);

        return MessageMapper.toJson(MessageResponse.ok("UNLOCK_USER",
            Map.of("message", "Da mo khoa tai khoan.")));
    }

    String handleGetAuditLog(Session session, JsonNode payload) {
        SecurityContext.requireRole(session, UserRole.ADMIN);
        int limit = payload.path("limit").asInt(50);
        if (limit <= 0 || limit > 500) {
            limit = 50;
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_AUDIT_LOG",
                handler.reportService.exportAuditLog(limit)));
    }

    String handleRunIntegrityCheck(Session session, JsonNode payload) {
        String userId = SecurityContext.requireRole(session, UserRole.ADMIN);

        handler.auditLogService.log(
            userId,
            AuditActions.RUN_INTEGRITY_CHECK,
            "{}");

        Map<String, Object> result = handler.dataIntegrityService.runFullCheck();
        return MessageMapper.toJson(MessageResponse.ok("RUN_INTEGRITY_CHECK", result));
    }

    String handleSendNotification(Session session, JsonNode payload) {
        String adminId = SecurityContext.requireRole(session, UserRole.ADMIN);
        String title = RequestHandler.getTextSafe(payload, "title");
        String message = RequestHandler.getTextSafe(payload, "message");
        String type = RequestHandler.getTextSafe(payload, "type");

        if (title == null || title.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException("Tiêu đề thông báo không được để trống.");
        }
        if (message == null || message.isBlank()) {
            throw new com.bidhub.common.exception.ValidationException("Nội dung thông báo không được để trống.");
        }

        ObjectNode notifNode = JsonNodeFactory.instance.objectNode();
        notifNode.put("title", title);
        notifNode.put("message", message);
        notifNode.put("type", type != null ? type : "SYSTEM");
        String notifJson = MessageMapper.toJson(notifNode);

        handler.auditLogService.log(adminId, "BROADCAST_NOTIFICATION", notifJson);

        return MessageMapper.toJson(MessageResponse.ok("SEND_NOTIFICATION",
                Map.of("message", "Đã gửi thông báo đến toàn bộ người dùng.",
                        "title", title)));
    }

    String handleGetNotifications(Session session, JsonNode payload) {
        SecurityContext.requireAuthenticated(session);
        String userId = session.getAuthenticatedUserId();
        List<Map<String, Object>> notifications = new java.util.ArrayList<>();
        try {
            List<Map<String, Object>> auditLogs = handler.reportService.exportAuditLog(100);
            for (Map<String, Object> log : auditLogs) {
                String action = String.valueOf(log.getOrDefault("action", ""));
                if ("BROADCAST_NOTIFICATION".equals(action)) {
                    String detail = String.valueOf(log.getOrDefault("details", "{}"));
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                        JsonNode detailNode = om.readTree(detail);
                        Map<String, Object> notif = new HashMap<>();
                        String notifId = String.valueOf(log.getOrDefault("id", ""));
                        notif.put("id", notifId);
                        notif.put("title", detailNode.path("title").asText("Thông báo"));
                        notif.put("message", detailNode.path("message").asText(""));
                        notif.put("type", detailNode.path("type").asText("SYSTEM"));
                        notif.put("createdAt", log.getOrDefault("createdAt", ""));
                        boolean isRead = RequestHandler.userReadNotifications.getOrDefault(userId, java.util.Collections.emptySet()).contains(notifId);
                        notif.put("isRead", isRead);
                        notifications.add(notif);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            RequestHandler.logger.warn("Không thể lấy danh sách thông báo: {}", e.getMessage());
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_NOTIFICATIONS", notifications));
    }

    String handleMarkNotificationRead(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String notifId = RequestHandler.getTextSafe(payload, "notificationId");
        if (notifId != null && !notifId.isBlank()) {
            RequestHandler.userReadNotifications.computeIfAbsent(userId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(notifId);
        }
        return MessageMapper.toJson(MessageResponse.ok("MARK_NOTIFICATION_READ", Map.of("message", "Đã đánh dấu đọc")));
    }

    String handleAdminStopAuction(Session session, JsonNode payload) {
        String userId = SecurityContext.requireRole(session, UserRole.ADMIN);
        String auctionId = RequestHandler.getTextSafe(payload, "auctionId");
        if (auctionId == null || auctionId.isBlank()) {
            return MessageMapper.toJson(MessageResponse.error("ADMIN_STOP_AUCTION", "auctionId không được để trống."));
        }
        java.util.Optional<Auction> aucOpt = handler.auctionDao.findById(auctionId);
        if (aucOpt.isEmpty()) {
            return MessageMapper.toJson(MessageResponse.error("ADMIN_STOP_AUCTION", "Phiên đấu giá không tồn tại."));
        }
        handler.auctionDao.updateStatus(auctionId, AuctionStatus.CANCELED);
        com.bidhub.server.service.AuctionManager.getInstance().removeAuction(auctionId);
        handler.auditLogService.log(userId, "ADMIN_STOP_AUCTION", "{\"auctionId\":\"" + auctionId + "\"}");
        return MessageMapper.toJson(MessageResponse.ok("ADMIN_STOP_AUCTION", Map.of("message", "Đã dừng/hủy phiên đấu giá thành công.", "auctionId", auctionId)));
    }

    String handleAdminDeleteAuction(Session session, JsonNode payload) {
        String userId = SecurityContext.requireRole(session, UserRole.ADMIN);
        String auctionId = RequestHandler.getTextSafe(payload, "auctionId");
        if (auctionId == null || auctionId.isBlank()) {
            return MessageMapper.toJson(MessageResponse.error("ADMIN_DELETE_AUCTION", "auctionId không được để trống."));
        }
        java.util.Optional<Auction> aucOpt = handler.auctionDao.findById(auctionId);
        if (aucOpt.isEmpty()) {
            return MessageMapper.toJson(MessageResponse.error("ADMIN_DELETE_AUCTION", "Phiên đấu giá không tồn tại."));
        }
        handler.auctionDao.deleteById(auctionId);
        com.bidhub.server.service.AuctionManager.getInstance().removeAuction(auctionId);
        handler.auditLogService.log(userId, "ADMIN_DELETE_AUCTION", "{\"auctionId\":\"" + auctionId + "\"}");
        return MessageMapper.toJson(MessageResponse.ok("ADMIN_DELETE_AUCTION", Map.of("message", "Đã xóa phiên đấu giá thành công.", "auctionId", auctionId)));
    }
}
