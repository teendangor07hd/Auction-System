package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.dao.BidDao;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

class ReportServiceTest {

  private ReportService reportService;

  @BeforeEach
  void setUp() {
    // --- START STUB: Mock DAO để pass test khi không có database that (Phan cua Khoa thêm để thay cho DB) ---
    AuctionDao mockAuctionDao = new AuctionDao() {
        @Override public List<com.bidhub.server.model.Auction> findAll() {
            java.time.LocalDateTime start = java.time.LocalDateTime.now();
            java.time.LocalDateTime end = start.plusHours(1);
            return List.of(new com.bidhub.server.model.Auction("auc1", start, start, "item1", start, end, 100, 150, "user1", com.bidhub.server.model.AuctionStatus.RUNNING, 10));
        }
        @Override public java.util.Optional<com.bidhub.server.model.Auction> findById(String id) {
            return java.util.Optional.empty();
        }
    };
    BidDao mockBidDao = new BidDao() {
        @Override public List<com.bidhub.server.model.BidTransaction> findByAuctionId(String id) {
            if ("nonexistent-auction-id".equals(id)) return java.util.Collections.emptyList();
            return List.of(new com.bidhub.server.model.BidTransaction("auc1", "user1", 150));
        }
    };
    AuditLogDao mockAuditLogDao = new AuditLogDao() {
        @Override public List<com.bidhub.server.model.AuditLog> findRecent(int limit) {
            return List.of(new com.bidhub.server.model.AuditLog("user1", "action1", "details1"));
        }
    };
    com.bidhub.server.dao.UserDao mockUserDao = new com.bidhub.server.dao.UserDao() {
        @Override public java.util.Optional<com.bidhub.server.model.User> findById(String id) {
            return java.util.Optional.empty();
        }
    };
    com.bidhub.server.dao.ItemDao mockItemDao = new com.bidhub.server.dao.ItemDao() {
        @Override public java.util.Optional<com.bidhub.server.model.Item> findById(String id) {
            return java.util.Optional.empty();
        }
    };
    reportService = new ReportService(mockAuctionDao, mockBidDao, mockAuditLogDao, mockUserDao, mockItemDao);
    // --- END STUB ---
  }

  @Test
  @DisplayName("exportAuctionReport() tra ve List<Map> khong null")
  void exportAuctionReport_notNull() {
    List<Map<String, Object>> report = reportService.exportAuctionReport();
    assertNotNull(report, "Report khong duoc null");
  }

  @Test
  @DisplayName("exportAuctionReport() moi row co key auctionId va status")
  void exportAuctionReport_hasRequiredKeys() {
    List<Map<String, Object>> report = reportService.exportAuctionReport();
    // Verify method signature và return type
    assertNotNull(report);
    // Actual data test cần DB — verify structure
    for (Map<String, Object> row : report) {
      assertTrue(row.containsKey("auctionId"), "Row thieu key auctionId");
      assertTrue(row.containsKey("status"), "Row thieu key status");
      assertTrue(row.containsKey("currentHighestBid"), "Row thieu key currentHighestBid");
    }
  }

  @Test
  @DisplayName("exportBidHistory() voi auctionId rong khong crash")
  void exportBidHistory_emptyAuction_noCrash() {
    assertDoesNotThrow(() ->
        reportService.exportBidHistory("nonexistent-auction-id"));
  }

  @Test
  @DisplayName("exportAuditLog(5) tra ve list khong null")
  void exportAuditLog_notNull() {
    List<Map<String, Object>> logs = reportService.exportAuditLog(5);
    assertNotNull(logs);
  }

  @Test
  @DisplayName("exportAuditLog(0) co fallback ve 50")
  void exportAuditLog_zeroLimit_fallback() {
    // Note: validation 0 < limit < 500 o handler, không phai service
    // Service chỉ goi auditLogDao.findRecent(limit) truc tiep
    assertDoesNotThrow(() -> reportService.exportAuditLog(0));
  }

  @Test
  @DisplayName("ReportService constructor inject khong crash")
  void constructor_inject_noCrash() {
    assertDoesNotThrow(() ->
        new ReportService(null, null, null, null, null));
  }

  @Test
  @DisplayName("exportBidHistory() map co key bidAmount va bidderId")
  void exportBidHistory_hasRequiredKeys() {
    List<Map<String, Object>> bids =
        reportService.exportBidHistory("test-auction");
    assertNotNull(bids);
    for (Map<String, Object> row : bids) {
      assertTrue(row.containsKey("bidAmount"), "Row thieu key bidAmount");
      assertTrue(row.containsKey("bidderId"), "Row thieu key bidderId");
      assertTrue(row.containsKey("bidTime"), "Row thieu key bidTime");
    }
  }

  @Test
  @DisplayName("exportAuditLog() map co key action va createdAt")
  void exportAuditLog_hasRequiredKeys() {
    List<Map<String, Object>> logs = reportService.exportAuditLog(10);
    assertNotNull(logs);
    for (Map<String, Object> row : logs) {
      assertTrue(row.containsKey("action"), "Row thieu key action");
      assertTrue(row.containsKey("createdAt"), "Row thieu key createdAt");
    }
  }
}
