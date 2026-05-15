package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.dao.UserDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import com.bidhub.server.model.Item;
import com.bidhub.server.model.User;
import com.bidhub.server.model.UserRole;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class DataIntegrityServiceTest {

  private DataIntegrityService service;

  @BeforeEach
  void setUp() {
    // Service with no-arg constructor — uses real DAO (needs DB for integration)
    // For unit test, use constructor with injected DAOs
    service = new DataIntegrityService();
  }

  @Test
  @DisplayName("Constructor inject khong crash")
  void constructor_inject_noCrash() {
    assertDoesNotThrow(() ->
        new DataIntegrityService(null, null, null, null));
  }

  @Test
  @DisplayName("checkBidConsistency() tra ve List khong null")
  void checkBidConsistency_notNull() {
    List<String> errors = service.checkBidConsistency();
    assertNotNull(errors, "checkBidConsistency() khong duoc tra ve null");
  }

  @Test
  @DisplayName("checkAuctionWinners() tra ve List khong null")
  void checkAuctionWinners_notNull() {
    List<String> errors = service.checkAuctionWinners();
    assertNotNull(errors, "checkAuctionWinners() khong duoc tra ve null");
  }

  @Test
  @DisplayName("checkOrphanedItems() tra ve List khong null")
  void checkOrphanedItems_notNull() {
    List<String> errors = service.checkOrphanedItems();
    assertNotNull(errors, "checkOrphanedItems() khong duoc tra ve null");
  }

  @Test
  @DisplayName("runFullCheck() tra ve Map voi 5 key bat buoc")
  void runFullCheck_hasRequiredKeys() {
    Map<String, Object> result = service.runFullCheck();
    assertNotNull(result, "runFullCheck() khong duoc tra ve null");
    assertTrue(result.containsKey("bidConsistencyErrors"),
        "Thieu key bidConsistencyErrors");
    assertTrue(result.containsKey("auctionWinnerErrors"),
        "Thieu key auctionWinnerErrors");
    assertTrue(result.containsKey("orphanedItemErrors"),
        "Thieu key orphanedItemErrors");
    assertTrue(result.containsKey("totalErrors"),
        "Thieu key totalErrors");
    assertTrue(result.containsKey("status"),
        "Thieu key status");
  }

  @Test
  @DisplayName("runFullCheck() totalErrors = tong 3 loai errors")
  void runFullCheck_totalErrorsCorrect() {
    Map<String, Object> result = service.runFullCheck();
    @SuppressWarnings("unchecked")
    List<String> bidErrors = (List<String>) result.get("bidConsistencyErrors");
    @SuppressWarnings("unchecked")
    List<String> winnerErrors = (List<String>) result.get("auctionWinnerErrors");
    @SuppressWarnings("unchecked")
    List<String> orphanErrors = (List<String>) result.get("orphanedItemErrors");
    int expected = bidErrors.size() + winnerErrors.size() + orphanErrors.size();
    assertEquals(expected, result.get("totalErrors"),
        "totalErrors khong bang tong 3 loai errors");
  }

  @Test
  @DisplayName("runFullCheck() status OK khi khong co errors")
  void runFullCheck_statusOk_whenNoErrors() {
    Map<String, Object> result = service.runFullCheck();
    int total = (int) result.get("totalErrors");
    if (total == 0) {
      assertEquals("OK", result.get("status"),
          "status phai la OK khi totalErrors = 0");
    } else {
      assertEquals("ERRORS_FOUND", result.get("status"),
          "status phai la ERRORS_FOUND khi totalErrors > 0");
    }
  }

  @Test
  @DisplayName("checkBidConsistency() moi error la String khong rong")
  void checkBidConsistency_errorsNotEmpty() {
    List<String> errors = service.checkBidConsistency();
    for (String error : errors) {
      assertNotNull(error, "Error description khong duoc null");
      assertFalse(error.isBlank(), "Error description khong duoc rong");
    }
  }

  @Test
  @DisplayName("runFullCheck() result la LinkedHashMap (keep order)")
  void runFullCheck_orderedResult() {
    Map<String, Object> result = service.runFullCheck();
    // Verify result maintains insertion order (LinkedHashMap behavior)
    List<String> keyList = new ArrayList<>(result.keySet());
    assertEquals("bidConsistencyErrors", keyList.get(0));
    assertEquals("auctionWinnerErrors", keyList.get(1));
    assertEquals("orphanedItemErrors", keyList.get(2));
    assertEquals("totalErrors", keyList.get(3));
    assertEquals("status", keyList.get(4));
  }

  @Test
  @DisplayName("DataIntegrityService la final class")
  void dataIntegrityService_isFinal() {
    assertTrue(java.lang.reflect.Modifier.isFinal(DataIntegrityService.class.getModifiers()),
        "DataIntegrityService phai la final class");
  }
}
