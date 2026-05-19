package com.bidhub.server.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.common.exception.*;
import com.bidhub.server.model.*;
import com.bidhub.server.service.*;
import com.bidhub.server.event.BidUpdateEvent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

class AuctionHandler {
    private final RequestHandler handler;

    AuctionHandler(RequestHandler handler) {
        this.handler = handler;
    }

    String handleCreateAuction(Session session, JsonNode payload) {
        String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);

        String itemId = RequestHandler.getTextSafe(payload, "itemId");
        if (itemId == null || itemId.isBlank()) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION", "itemId khong duoc de trong."));
        }

        java.util.Optional<Item> itemOpt = handler.itemDao.findById(itemId);
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

        // Kiểm tra: sản phẩm đã có phiên đấu giá đang hoạt động chưa?
        boolean hasActiveAuction = AuctionManager.getInstance().getAllActive().stream()
                .anyMatch(a -> a.getItemId().equals(itemId)
                        && (a.getStatus() == AuctionStatus.OPEN
                        || a.getStatus() == AuctionStatus.RUNNING));
        if (!hasActiveAuction) {
            hasActiveAuction = handler.auctionDao.findActiveAuctions().stream()
                    .anyMatch(a -> a.getItemId().equals(itemId));
        }
        if (hasActiveAuction) {
            return MessageMapper.toJson(
                    MessageResponse.error("CREATE_AUCTION",
                            "San pham nay dang co phien dau gia hoat dong. Khong the tao phien moi."));
        }
        double startingPrice;
        if (payload.has("startingPrice") && payload.get("startingPrice").isNumber()) {
            startingPrice = payload.get("startingPrice").asDouble();
        } else {
            String priceStr = RequestHandler.getTextSafe(payload, "startingPrice");
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

        double minimumIncrement = 1.0;
        if (payload.has("minimumIncrement")) {
            if (payload.get("minimumIncrement").isNumber()) {
                minimumIncrement = payload.get("minimumIncrement").asDouble();
            } else {
                String incStr = RequestHandler.getTextSafe(payload, "minimumIncrement");
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

        String startTimeStr = RequestHandler.getTextSafe(payload, "startTime");
        String endTimeStr = RequestHandler.getTextSafe(payload, "endTime");
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

        Auction auction = new Auction(itemId, startTime, endTime, startingPrice, minimumIncrement);

        handler.auctionDao.save(auction);

        AuctionManager.getInstance().addAuction(auction);

        handler.auditLogService.log(sellerId, AuditActions.AUCTION_CREATED,
                "{\"auctionId\":\"" + auction.getId()
                        + "\",\"itemId\":\"" + itemId + "\"}");

        Map<String, Object> result = new HashMap<>();
        result.put("auctionId", auction.getId());
        result.put("itemId", itemId);
        result.put("startingPrice", startingPrice);
        result.put("status", auction.getStatus().name());

        return MessageMapper.toJson(MessageResponse.ok("CREATE_AUCTION", result));
    }

    String handlePlaceBid(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);

        String auctionId = payload.path("auctionId").asText("");
        double bidAmount = payload.path("bidAmount").asDouble(0.0);

        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }
        if (bidAmount <= 0) {
            throw new InvalidBidException("Gia dat phai lon hon 0.");
        }

        Auction auction = AuctionManager.getInstance().getAuction(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException(
                        "Phien dau gia khong ton tai: " + auctionId));

        auction.getLock().lock();
        java.sql.Connection txConn = null;
        try {
            handler.bidValidator.validate(auction, userId, bidAmount);

            txConn = com.bidhub.server.config.DbConnectionProvider.getInstance().getConnection();
            txConn.setAutoCommit(false);

            BidTransaction bid = new BidTransaction(auctionId, userId, bidAmount);

            new com.bidhub.server.dao.BidDao(txConn).save(bid);

            auction.setCurrentHighestBid(bidAmount);
            auction.setHighestBidderId(userId);

            new com.bidhub.server.dao.AuctionDao(txConn).updateHighestBid(auctionId, bidAmount, userId);
            
            // Log audit within transaction
            com.bidhub.server.model.AuditLog log = new com.bidhub.server.model.AuditLog(userId, AuditActions.PLACE_BID, 
                    "{\"auctionId\":\"" + auctionId + "\",\"bidAmount\":" + bidAmount + "}");
            new com.bidhub.server.dao.AuditLogDao(txConn).save(log);
            
            txConn.commit();

            handler.antiSnipingEngine.check(auction);
        } catch (Exception e) {
            if (txConn != null) {
                try {
                    txConn.rollback();
                } catch (java.sql.SQLException ex) {
                    // Ignore
                }
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Loi transaction: " + e.getMessage(), e);
        } finally {
            if (txConn != null) {
                try {
                    txConn.setAutoCommit(true);
                    com.bidhub.server.config.DbConnectionProvider.getInstance().closeConnection(txConn);
                } catch (java.sql.SQLException ex) {
                    // Ignore
                }
            }
            auction.getLock().unlock();
        }

        String bidderName = userId;
        java.util.Optional<com.bidhub.server.model.User> userOpt = handler.userDao.findById(userId);
        if (userOpt.isPresent()) {
            bidderName = userOpt.get().getUsername();
        }

        NotificationBroker.getInstance().publish(auctionId,
                new BidUpdateEvent(auctionId, userId, bidderName, bidAmount));

        return MessageMapper.toJson(MessageResponse.ok("PLACE_BID",
                Map.of("auctionId", auctionId,
                        "currentHighestBid", bidAmount,
                        "highestBidderId", userId)));
    }

    String handleGetAuctionList(Session session, JsonNode payload) {
        List<Auction> auctions = handler.auctionDao.findAll();

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

            String itemName = "San pham khong xac dinh";
            String imageUrl = null;
            String sellerName = "Khong xac dinh";
            java.util.Optional<Item> itemOpt = handler.itemDao.findById(auction.getItemId());
            if (itemOpt.isPresent()) {
                Item item = itemOpt.get();
                itemName = item.getName();
                imageUrl = item.getImageUrl();
                java.util.Optional<com.bidhub.server.model.User> sellerOpt = handler.userDao.findById(item.getSellerId());
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

    String handleGetAuctionDetail(Session session, JsonNode payload) {
        String auctionId = payload.path("auctionId").asText("");
        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }

        Auction auction = AuctionManager.getInstance().getAuction(auctionId)
                .orElseGet(() -> handler.auctionDao.findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(
                                "Phien dau gia khong ton tai: " + auctionId)));

        List<BidTransaction> bids = handler.bidDao.findByAuctionId(auctionId);
        List<Map<String, Object>> bidHistory = new java.util.ArrayList<>();
        for (BidTransaction b : bids) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("bidAmount", b.getBidAmount());
            map.put("bidTime", b.getBidTime().toString());
            map.put("bidderId", b.getBidderId());
            
            java.util.Optional<com.bidhub.server.model.User> uOpt = handler.userDao.findById(b.getBidderId());
            map.put("bidderName", uOpt.map(com.bidhub.server.model.User::getUsername).orElse(b.getBidderId()));
            bidHistory.add(map);
        }

        Map<String, Object> auctionInfo = new HashMap<>();
        auctionInfo.put("id", auction.getId());
        auctionInfo.put("itemId", auction.getItemId());
        auctionInfo.put("startingPrice", auction.getStartingPrice());
        auctionInfo.put("currentHighestBid", auction.getCurrentHighestBid());
        auctionInfo.put("highestBidderId", auction.getHighestBidderId());

        String bidderId = auction.getHighestBidderId();
        if (bidderId != null) {
            java.util.Optional<com.bidhub.server.model.User> userOpt = handler.userDao.findById(bidderId);
            auctionInfo.put("highestBidderName", userOpt.map(com.bidhub.server.model.User::getUsername).orElse(bidderId));
        } else {
            auctionInfo.put("highestBidderName", "Chưa có");
        }

        auctionInfo.put("startTime", auction.getStartTime() != null ? auction.getStartTime().toString() : "");
        auctionInfo.put("endTime", auction.getEndTime() != null ? auction.getEndTime().toString() : "");
        auctionInfo.put("status", auction.getStatus().name());
        auctionInfo.put("minimumIncrement", auction.getMinimumIncrement());

        java.util.Optional<Item> itemOpt = handler.itemDao.findById(auction.getItemId());
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

    String handleSubscribeAuction(Session session, JsonNode payload) {
        String auctionId = payload.path("auctionId").asText("");
        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }
        NotificationBroker.getInstance().subscribe(auctionId, session);
        return MessageMapper.toJson(
                MessageResponse.ok("SUBSCRIBE_AUCTION",
                        Map.of("auctionId", auctionId, "message", "Da subscribe thanh cong")));
    }

    String handleGetMyAuctions(Session session, JsonNode payload) {
        String sellerId = SecurityContext.requireRole(session, UserRole.SELLER);
        java.util.List<Item> items = handler.itemDao.findBySellerId(sellerId);
        java.util.Map<String, Item> itemMap = new HashMap<>();
        for (Item item : items) {
            itemMap.put(item.getId(), item);
        }

        List<Auction> allAuctions = handler.auctionDao.findAll();

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Auction auc : allAuctions) {
            Item item = itemMap.get(auc.getItemId());
            if (item == null) continue;

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

            // Nếu phiên đã kết thúc, gửi thêm thông tin liên hệ của người thắng cuộc
            if (auc.getStatus() == com.bidhub.server.model.AuctionStatus.FINISHED || auc.getStatus() == com.bidhub.server.model.AuctionStatus.PAID) {
                java.util.Optional<com.bidhub.server.model.BidTransaction> highestBidOpt = handler.bidDao.getHighestBid(auc.getId());
                if (highestBidOpt.isPresent()) {
                    String winnerId = highestBidOpt.get().getBidderId();
                    java.util.Optional<com.bidhub.server.model.User> winnerOpt = handler.userDao.findById(winnerId);
                    if (winnerOpt.isPresent()) {
                        info.put("winnerName", winnerOpt.get().getUsername());
                        info.put("winnerEmail", winnerOpt.get().getEmail());
                    }
                }
            }
            result.add(info);
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_MY_AUCTIONS", result));
    }

    String handleCancelAuction(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String auctionId = RequestHandler.getTextSafe(payload, "auctionId");
        if (auctionId == null || auctionId.isBlank()) {
            return MessageMapper.toJson(MessageResponse.error("CANCEL_AUCTION", "auctionId không được để trống."));
        }

        java.util.Optional<Auction> aucOpt = handler.auctionDao.findById(auctionId);
        if (aucOpt.isEmpty()) {
            return MessageMapper.toJson(MessageResponse.error("CANCEL_AUCTION", "Phiên đấu giá không tồn tại."));
        }
        Auction auc = aucOpt.get();

        java.util.Optional<Item> itemOpt = handler.itemDao.findById(auc.getItemId());
        if (itemOpt.isEmpty() || !itemOpt.get().getSellerId().equals(userId)) {
            return MessageMapper.toJson(MessageResponse.error("CANCEL_AUCTION", "Bạn không có quyền hủy phiên này."));
        }

        if (auc.getStatus() != AuctionStatus.OPEN) {
            return MessageMapper.toJson(MessageResponse.error("CANCEL_AUCTION",
                    "Chỉ có thể hủy phiên đang ở trạng thái Chờ bắt đầu."));
        }

        handler.auctionDao.deleteById(auctionId);
        handler.auditLogService.log(userId, "AUCTION_CANCELLED",
                "{\"auctionId\":\"" + auctionId + "\"}");

        return MessageMapper.toJson(MessageResponse.ok("CANCEL_AUCTION",
                Map.of("message", "Đã hủy phiên đấu giá.", "auctionId", auctionId)));
    }

    String handleGetWonAuctions(Session session, JsonNode payload) {
        String bidderId = SecurityContext.requireAuthenticated(session);
        List<Auction> all = handler.auctionDao.findAll();
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Auction auc : all) {
            if (auc.getStatus() != com.bidhub.server.model.AuctionStatus.FINISHED
                    && auc.getStatus() != com.bidhub.server.model.AuctionStatus.PAID) continue;
            if (!bidderId.equals(auc.getHighestBidderId())) continue;
            Map<String, Object> info = new HashMap<>();
            info.put("id", auc.getId());
            info.put("itemId", auc.getItemId());
            info.put("startingPrice", auc.getStartingPrice());
            info.put("currentHighestBid", auc.getCurrentHighestBid());
            info.put("status", auc.getStatus().name());
            info.put("startTime", auc.getStartTime() != null ? auc.getStartTime().toString() : "");
            info.put("endTime", auc.getEndTime() != null ? auc.getEndTime().toString() : "");
            java.util.Optional<com.bidhub.server.model.Item> itemOpt = handler.itemDao.findById(auc.getItemId());
            if (itemOpt.isPresent()) {
                info.put("itemName", itemOpt.get().getName());
                info.put("imageUrl", itemOpt.get().getImageUrl());
                info.put("description", itemOpt.get().getDescription());
                java.util.Optional<com.bidhub.server.model.User> sellerOpt = handler.userDao.findById(itemOpt.get().getSellerId());
                info.put("sellerName", sellerOpt.map(com.bidhub.server.model.User::getUsername).orElse("Unknown"));
            } else {
                info.put("itemName", "Sản phẩm không xác định");
                info.put("imageUrl", null);
                info.put("description", "");
                info.put("sellerName", "Unknown");
            }
            result.add(info);
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_WON_AUCTIONS", result));
    }

    String handleMarkPaid(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String auctionId = payload.path("auctionId").asText("");
        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }

        java.util.Optional<Auction> aucOpt = handler.auctionDao.findById(auctionId);
        if (aucOpt.isEmpty()) {
            return MessageMapper.toJson(MessageResponse.error("MARK_PAID", "Phien dau gia khong ton tai."));
        }
        Auction auction = aucOpt.get();

        if (auction.getStatus() != AuctionStatus.FINISHED) {
            return MessageMapper.toJson(MessageResponse.error("MARK_PAID",
                    "Chi co the danh dau da thanh toan cho phien da ket thuc."));
        }

        java.util.Optional<Item> itemOpt = handler.itemDao.findById(auction.getItemId());
        if (itemOpt.isEmpty() || !itemOpt.get().getSellerId().equals(userId)) {
            return MessageMapper.toJson(MessageResponse.error("MARK_PAID",
                    "Ban khong co quyen thuc hien thao tac nay."));
        }

        auction.transitionTo(AuctionStatus.PAID);
        handler.auctionDao.updateStatus(auctionId, AuctionStatus.PAID);

        handler.auditLogService.log(userId, "AUCTION_PAID",
                "{\"auctionId\":\"" + auctionId + "\"}");

        return MessageMapper.toJson(MessageResponse.ok("MARK_PAID",
                Map.of("auctionId", auctionId, "message", "Da xac nhan thanh toan.")));
    }

    String handleSellerCancelFinished(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        String auctionId = payload.path("auctionId").asText("");
        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }

        java.util.Optional<Auction> aucOpt = handler.auctionDao.findById(auctionId);
        if (aucOpt.isEmpty()) {
            return MessageMapper.toJson(MessageResponse.error("SELLER_CANCEL_FINISHED",
                    "Phien dau gia khong ton tai."));
        }
        Auction auction = aucOpt.get();

        if (auction.getStatus() != AuctionStatus.FINISHED) {
            return MessageMapper.toJson(MessageResponse.error("SELLER_CANCEL_FINISHED",
                    "Chi co the huy phien o trang thai da ket thuc."));
        }

        java.util.Optional<Item> itemOpt = handler.itemDao.findById(auction.getItemId());
        if (itemOpt.isEmpty() || !itemOpt.get().getSellerId().equals(userId)) {
            return MessageMapper.toJson(MessageResponse.error("SELLER_CANCEL_FINISHED",
                    "Ban khong co quyen huy phien nay."));
        }

        auction.transitionTo(AuctionStatus.CANCELED);
        handler.auctionDao.updateStatus(auctionId, AuctionStatus.CANCELED);

        handler.auditLogService.log(userId, "AUCTION_SELLER_CANCELLED",
                "{\"auctionId\":\"" + auctionId + "\",\"reason\":\"Bidder khong thanh toan\"}");

        return MessageMapper.toJson(MessageResponse.ok("SELLER_CANCEL_FINISHED",
                Map.of("auctionId", auctionId,
                        "message", "Da huy phien. San pham co the dua len dau gia lai.")));
    }
}
