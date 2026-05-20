package com.bidhub.server.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.common.network.MessageResponse;
import com.bidhub.common.exception.ValidationException;
import com.bidhub.server.model.*;
import com.bidhub.server.service.*;
import java.util.Optional;

class ReportHandler {
    private final RequestHandler handler;

    ReportHandler(RequestHandler handler) {
        this.handler = handler;
    }

    String handleGetAuctionReport(Session session, JsonNode payload) {
        String userId = SecurityContext.requireAuthenticated(session);
        
        com.bidhub.server.model.UserRole role = session.getUserRole();
        if (role == null) {
            java.util.Optional<com.bidhub.server.model.User> uOpt = handler.userDao.findById(userId);
            if (uOpt.isPresent()) {
                role = uOpt.get().getRole();
                session.setUserRole(role);
            }
        }
        
        if (role != com.bidhub.server.model.UserRole.SELLER && role != com.bidhub.server.model.UserRole.ADMIN) {
            throw new com.bidhub.common.exception.AuthenticationException("Khong du quyen. Ban phai la SELLER hoac ADMIN.");
        }

        return MessageMapper.toJson(MessageResponse.ok("GET_AUCTION_REPORT",
                handler.reportService.exportAuctionReport()));
    }

    String handleGetBidHistoryReport(Session session, JsonNode payload) {
        SecurityContext.requireAuthenticated(session);
        String auctionId = payload.path("auctionId").asText("");
        if (auctionId.isBlank()) {
            throw new ValidationException("auctionId khong duoc de trong");
        }
        return MessageMapper.toJson(MessageResponse.ok("GET_BID_HISTORY_REPORT",
                handler.reportService.exportBidHistory(auctionId)));
    }
}
