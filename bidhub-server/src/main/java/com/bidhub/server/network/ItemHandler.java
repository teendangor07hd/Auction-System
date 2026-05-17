package com.bidhub.server.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.server.model.*;
import com.bidhub.server.service.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

class ItemHandler {
    private final RequestHandler handler;

    ItemHandler(RequestHandler handler) {
        this.handler = handler;
    }

    String handleCreateItem(Session session, JsonNode payload) {
        String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);

        String name = RequestHandler.getTextSafe(payload, "name");
        String description = RequestHandler.getTextSafe(payload, "description");
        String priceStr = RequestHandler.getTextSafe(payload, "startingPrice");
        String itemTypeStr = RequestHandler.getTextSafe(payload, "itemType");

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

        JsonNode extrasNode = payload.has("extras") ? payload.get("extras") : null;
        java.util.Map<String, Object> extras = handler.parseExtras(extrasNode);

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

        handler.itemDao.save(item);

        handler.auditLogService.log(sellerId, AuditActions.ITEM_CREATED,
                "{\"itemId\":\"" + item.getId() + "\"}");

        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("itemId", item.getId());
        result.put("name", item.getName());
        result.put("itemType", item.getItemType().name());
        result.put("startingPrice", String.valueOf(item.getStartingPrice()));

        return MessageMapper.toJson(MessageResponse.ok("CREATE_ITEM", result));
    }

    String handleGetItemList() {
        java.util.List<Item> items = handler.itemDao.findAll();
        java.util.Map<String, String> itemAuctionStatus = handler.auctionDao.getItemAuctionStatusMap();

        java.util.List<User> allUsers = handler.userDao.findAll();
        java.util.Map<String, String> userNames = new HashMap<>();
        for (User u : allUsers) {
            userNames.put(u.getId(), u.getUsername());
        }

        java.util.List<Map<String, Object>> result = enrichItems(items, itemAuctionStatus, userNames, true, false);
        return MessageMapper.toJson(MessageResponse.ok("GET_ITEM_LIST", result));
    }

    String handleGetItemDetail(JsonNode payload) {
        String itemId = RequestHandler.getTextSafe(payload, "itemId");

        if (itemId == null || itemId.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("GET_ITEM_DETAIL", "itemId la bat buoc."));
        }

        java.util.Optional<Item> itemOpt = handler.itemDao.findById(itemId);
        if (itemOpt.isEmpty()) {
            return MessageMapper.toJson(
                    MessageResponse.error("GET_ITEM_DETAIL", "San pham khong ton tai."));
        }

        return MessageMapper.toJson(
                MessageResponse.ok("GET_ITEM_DETAIL", itemOpt.get()));
    }

    String handleDeleteItem(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String itemId = RequestHandler.getTextSafe(payload, "itemId");

        if (itemId == null || itemId.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("DELETE_ITEM", "itemId la bat buoc."));
        }

        java.util.Optional<Item> itemOpt = handler.itemDao.findById(itemId);
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

        handler.itemDao.deleteById(itemId);

        handler.auditLogService.log(userId, AuditActions.ITEM_DELETED,
                "{\"itemId\":\"" + itemId + "\"}");

        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("message", "Xoa san pham thanh cong.");
        result.put("itemId", itemId);

        return MessageMapper.toJson(MessageResponse.ok("DELETE_ITEM", result));
    }

    String handleListMyItems(Session session, JsonNode payload) {
        String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);
        java.util.List<Item> items = handler.itemDao.findBySellerId(sellerId);
        java.util.Map<String, String> itemAuctionStatus = handler.auctionDao.getItemAuctionStatusMap();

        java.util.List<Map<String, Object>> result = enrichItems(items, itemAuctionStatus, null, false, true);
        return MessageMapper.toJson(MessageResponse.ok("LIST_MY_ITEMS", result));
    }

    String handleUpdateItem(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String itemId = RequestHandler.getTextSafe(payload, "itemId");
        if (itemId == null || itemId.isBlank()) {
            return MessageMapper.toJson(MessageResponse.error("UPDATE_ITEM", "itemId không được để trống."));
        }

        java.util.Optional<Item> itemOpt = handler.itemDao.findById(itemId);
        if (itemOpt.isEmpty()) {
            return MessageMapper.toJson(MessageResponse.error("UPDATE_ITEM", "Sản phẩm không tồn tại."));
        }
        Item item = itemOpt.get();
        if (!item.getSellerId().equals(userId)) {
            return MessageMapper.toJson(MessageResponse.error("UPDATE_ITEM", "Bạn không có quyền sửa sản phẩm này."));
        }

        String newName = RequestHandler.getTextSafe(payload, "name");
        if (newName != null && !newName.isBlank()) item.setName(newName);

        String newDesc = RequestHandler.getTextSafe(payload, "description");
        if (newDesc != null) item.setDescription(newDesc);

        Double newPrice = null;
        if (payload.has("startingPrice") && payload.get("startingPrice").isNumber()) {
            newPrice = payload.get("startingPrice").asDouble();
        }

        String newImageUrl = RequestHandler.getTextSafe(payload, "imageUrl");
        if (newImageUrl != null && !newImageUrl.isBlank()) item.setImageUrl(newImageUrl);

        handler.itemDao.updateItem(itemId,
                newName != null && !newName.isBlank() ? newName : null,
                newDesc,
                newPrice,
                newImageUrl);

        handler.auditLogService.log(userId, "ITEM_UPDATED",
                "{\"itemId\":\"" + itemId + "\",\"newName\":\"" + item.getName() + "\"}");

        return MessageMapper.toJson(MessageResponse.ok("UPDATE_ITEM",
                Map.of("message", "Đã cập nhật sản phẩm.", "itemId", itemId)));
    }

    private java.util.List<Map<String, Object>> enrichItems(java.util.List<Item> items, java.util.Map<String, String> statusMap, java.util.Map<String, String> userNames, boolean includeSeller, boolean includeItemIdField) {
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Item item : items) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", item.getId());
            if (includeItemIdField) {
                info.put("itemId", item.getId());
            }
            info.put("name", item.getName());
            info.put("description", item.getDescription());
            info.put("itemType", item.getItemType().name());
            info.put("startingPrice", item.getStartingPrice());
            info.put("imageUrl", item.getImageUrl());

            if (includeSeller) {
                String sellerName = userNames != null ? userNames.getOrDefault(item.getSellerId(), "Không rõ") : "Không rõ";
                info.put("sellerName", sellerName);
            }

            String rawStatus = statusMap.get(item.getId());
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
        return result;
    }
}
