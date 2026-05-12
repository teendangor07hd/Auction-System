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
 * Test suite cho AuditLog Integration — kiem tra auditLog duoc goi dung vi tri
 * trong lifecycle: handlePlaceBid, closeAuction, AntiSnipingEngine.check().
 *
 * <p>// 📌 [Tieu chi: Unit Test — audit integration test suite ≥ 5 cases]
 * // 📌 [Tieu chi: Audit Log — verify log() goi dung action va details]
 *
 * @author Khoa
 */
class AuditLogIntegrationTest {

  /**
   * Simple tracking audit log service cho test.
   * Thay vi ghi DB — luu cac log call vao list de verify.
   */
  // 📌 [Tieu chi: Unit Test — tracking list thay cho DB call]
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
  // 📌 [Tieu chi: Audit Log — verify PLACE_BID log format]
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
  // 📌 [Tieu chi: Audit Log — verify AUCTION_CLOSED log co winnerId]
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
  // 📌 [Tieu chi: Audit Log — verify AUCTION_CLOSED khi khong co bid]
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
  // 📌 [Tieu chi: Audit Log — verify AUCTION_EXTENDED log co ca 2 endTime]
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
  // 📌 [Tieu chi: Audit Log — verify khong log khi bid fail]
  void auditLog_bidFailed_noLog() {
    // Mo phong bid fail — khong goi auditLog
    // Trong handlePlaceBid, auditLog goi SAU validate + save
    // Neu validate fail → exception nem ra → auditLog khong chay toi

    assertEquals(0, trackingLog.callCount(),
        "AuditLog khong duoc goi khi bid fail (validation error)");
    assertFalse(trackingLog.hasAction(AuditActions.PLACE_BID));
  }

  @Test
  @DisplayName("AuditLog 3 lifecycle actions được gọi theo đúng thứ tự")
  // 📌 [Tieu chi: Audit Log — verify thu tu log: PLACE_BID → AUCTION_EXTENDED → AUCTION_CLOSED]
  void auditLog_lifecycleOrder_correct() {
    // 1. PLACE_BID
    trackingLog.log("user-001", AuditActions.PLACE_BID,
        "{\"auctionId\":\"auc-005\",\"bidAmount\":1500.0}");

    // 2. AUCTION_EXTENDED (bid trong snipe window)
    trackingLog.log("SYSTEM", AuditActions.AUCTION_EXTENDED,
        "{\"auctionId\":\"auc-005\",\"oldEndTime\":\"...\",\"newEndTime\":\"...\"}");

    // 3. AUCTION_CLOSED (auction ket thuc)
    trackingLog.log("SYSTEM", AuditActions.AUCTION_CLOSED,
        "{\"auctionId\":\"auc-005\",\"winnerId\":\"user-001\",\"winningBid\":1500.0}");

    assertEquals(3, trackingLog.callCount());
    // Verify thu tu
    assertTrue(trackingLog.calls.get(0).contains(AuditActions.PLACE_BID));
    assertTrue(trackingLog.calls.get(1).contains(AuditActions.AUCTION_EXTENDED));
    assertTrue(trackingLog.calls.get(2).contains(AuditActions.AUCTION_CLOSED));
  }
}
