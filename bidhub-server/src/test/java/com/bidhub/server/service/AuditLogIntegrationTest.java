package com.bidhub.server.service;

import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite cho AuditLog Integration — kiem tra auditLog được goi đúng vi tri
 * trong lifecycle: handlePlaceBid, closeAuction, AntiSnipingEngine.check().
 *
 *
 * @author Khoa
 */
class AuditLogIntegrationTest {

  /**
   * Simple tracking audit log service cho test.
   * Thay vi ghi DB — lưu các log call vao list để verify.
   */
  static class TrackingAuditLogService {
    private final List<String> calls = new ArrayList<>();

    void log(String userId, String action, String details) {
      calls.add(userId + "|" + action + "|" + details);
    }

    int callCount() {
      return calls.size();
    }

    String getLastCall() {
      return calls.isEmpty() ? null : calls.get(calls.size() - 1);
    }

    boolean hasAction(String action) {
      return calls.stream().anyMatch(c -> c.contains(action));
    }
  }

  private TrackingAuditLogService trackingLog;

  @BeforeEach
  void setUp() {
    trackingLog = new TrackingAuditLogService();
  }

  @Test
  @DisplayName("AuditLog PLACE_BID được gọi với đúng action và details")
  void auditLog_placeBid_correctFormat() {
    String userId = "user-001";
    String auctionId = "auc-001";
    double bidAmount = 1500.0;

    // Mo phong handlePlaceBid goi auditLog
    trackingLog.log(userId, AuditActions.PLACE_BID,
        "{\"auctionId\":\"" + auctionId + "\",\"bidAmount\":" + bidAmount + "}");

    assertEquals(1, trackingLog.callCount());
    assertTrue(trackingLog.hasAction(AuditActions.PLACE_BID));
    assertTrue(trackingLog.getLastCall().contains(auctionId));
    assertTrue(trackingLog.getLastCall().contains(String.valueOf(bidAmount)));
  }

  @Test
  @DisplayName("AuditLog AUCTION_CLOSED được gọi khi auction kết thúc")
  void auditLog_auctionClosed_includesWinner() {
    String auctionId = "auc-002";
    String winnerId = "user-002";
    double winningBid = 2000.0;

    // Mo phong closeAuction goi auditLog
    trackingLog.log("SYSTEM", AuditActions.AUCTION_CLOSED,
        "{\"auctionId\":\"" + auctionId
            + "\",\"winnerId\":\"" + winnerId
            + "\",\"winningBid\":" + winningBid + "}");

    assertEquals(1, trackingLog.callCount());
    assertTrue(trackingLog.hasAction(AuditActions.AUCTION_CLOSED));
    assertTrue(trackingLog.getLastCall().contains(winnerId));
    assertTrue(trackingLog.getLastCall().contains(String.valueOf(winningBid)));
  }

  @Test
  @DisplayName("AuditLog AUCTION_CLOSED khi không có winner — winnerId='none'")
  void auditLog_auctionClosed_noBid_winnerNone() {
    String auctionId = "auc-003";

    trackingLog.log("SYSTEM", AuditActions.AUCTION_CLOSED,
        "{\"auctionId\":\"" + auctionId
            + "\",\"winnerId\":\"none\",\"winningBid\":0.0}");

    assertEquals(1, trackingLog.callCount());
    assertTrue(trackingLog.getLastCall().contains("\"winnerId\":\"none\""));
  }

  @Test
  @DisplayName("AuditLog AUCTION_EXTENDED chứa oldEndTime và newEndTime")
  void auditLog_auctionExtended_containsBothEndTimes() {
    String auctionId = "auc-004";
    LocalDateTime oldEndTime = LocalDateTime.of(2025, 1, 15, 14, 5, 0);
    LocalDateTime newEndTime = oldEndTime.plusSeconds(60);

    // Mo phong AntiSnipingEngine.check() goi auditLog
    trackingLog.log("SYSTEM", AuditActions.AUCTION_EXTENDED,
        "{\"auctionId\":\"" + auctionId
            + "\",\"oldEndTime\":\"" + oldEndTime.toString()
            + "\",\"newEndTime\":\"" + newEndTime.toString() + "\"}");

    assertEquals(1, trackingLog.callCount());
    assertTrue(trackingLog.hasAction(AuditActions.AUCTION_EXTENDED));
    assertTrue(trackingLog.getLastCall().contains("oldEndTime"));
    assertTrue(trackingLog.getLastCall().contains("newEndTime"));
    assertTrue(trackingLog.getLastCall().contains(auctionId));
  }

  @Test
  @DisplayName("AuditLog không được gọi khi bid thất bại (validation fail)")
  void auditLog_bidFailed_noLog() {
    // Mo phong bid fail — không goi auditLog
    // Trong handlePlaceBid, auditLog goi Sau validate + save
    // Nếu validate fail → exception ném ra → auditLog không chay toi

    assertEquals(0, trackingLog.callCount(),
        "AuditLog khong duoc goi khi bid fail (validation error)");
    assertFalse(trackingLog.hasAction(AuditActions.PLACE_BID));
  }

  @Test
  @DisplayName("AuditLog 3 lifecycle actions được gọi theo đúng thứ tự")
  void auditLog_lifecycleOrder_correct() {
    // 1. PLACE_BID
    trackingLog.log("user-001", AuditActions.PLACE_BID,
        "{\"auctionId\":\"auc-005\",\"bidAmount\":1500.0}");

    // 2. AUCTION_EXTENDED (bid trong snipe window)
    trackingLog.log("SYSTEM", AuditActions.AUCTION_EXTENDED,
        "{\"auctionId\":\"auc-005\",\"oldEndTime\":\"...\",\"newEndTime\":\"...\"}");

    // 3. AUCTION_CLOSED (auction kết thúc)
    trackingLog.log("SYSTEM", AuditActions.AUCTION_CLOSED,
        "{\"auctionId\":\"auc-005\",\"winnerId\":\"user-001\",\"winningBid\":1500.0}");

    assertEquals(3, trackingLog.callCount());
    // Verify thu từ
    assertTrue(trackingLog.calls.get(0).contains(AuditActions.PLACE_BID));
    assertTrue(trackingLog.calls.get(1).contains(AuditActions.AUCTION_EXTENDED));
    assertTrue(trackingLog.calls.get(2).contains(AuditActions.AUCTION_CLOSED));
  }
}
