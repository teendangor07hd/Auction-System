package com.bidhub.server.service;

import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite cho BidValidator — bao phu toan bo 5 dieu kien validate + edge cases.
 *
 * <p>Quoc Minh chua merge BidValidator → Khoa tao BidValidatorTest truoc.
 * Khi QM merge, co the ghep 2 file test hoac giu ca 2.
 *
 * <p>// 📌 [Tieu chi: Testing — bao phu toan bo 5 dieu kien validate]
 */
class BidValidatorTest {

  private BidValidator validator;

  @BeforeEach
  void setUp() {
    // Validator voi null ItemDao — chi test nhung case khong can ItemDao
    validator = new BidValidator(null);
  }

  /**
   * Tao Auction voi trang thai, gia hien tai, buoc gia, va nguoi dan dau tuy y.
   * Dung reflection de set cac field private nhu doc WEEK6_TASKS.md chi dinh.
   */
  private Auction createAuction(AuctionStatus status, double currentBid,
      double minIncrement, String highestBidderId) {
    Auction a = new Auction();
    try {
      var statusField = a.getClass().getDeclaredField("status");
      statusField.setAccessible(true);
      statusField.set(a, status);

      var bidField = a.getClass().getDeclaredField("currentHighestBid");
      bidField.setAccessible(true);
      bidField.set(a, currentBid);

      var incField = a.getClass().getDeclaredField("minimumIncrement");
      incField.setAccessible(true);
      incField.set(a, minIncrement);

      var bidderField = a.getClass().getDeclaredField("highestBidderId");
      bidderField.setAccessible(true);
      bidderField.set(a, highestBidderId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return a;
  }

  // === 9 test cases co ban ===

  @Test
  @DisplayName("validate hop le — khong nem exception")
  void validate_validBid_noException() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000.0, 50.0, "user-other");
    assertDoesNotThrow(
        () -> validator.validate(a, "user-bidder", 1100.0));
  }

  @Test
  @DisplayName("validate auction FINISHED → AuctionClosedException")
  void validate_finishedAuction_throwsClosed() {
    Auction a = createAuction(AuctionStatus.FINISHED, 1000.0, 50.0, null);
    assertThrows(AuctionClosedException.class,
        () -> validator.validate(a, "user-bidder", 1100.0));
  }

  @Test
  @DisplayName("validate nguoi dan dau bid lai → InvalidBidException")
  void validate_currentLeader_throwsException() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000.0, 50.0, "user-leader");
    InvalidBidException ex = assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-leader", 1100.0));
    assertTrue(ex.getMessage().contains("dang la nguoi dan dau"));
  }

  @Test
  @DisplayName("validate gia dat bang gia hien tai → InvalidBidException")
  void validate_equalBid_throwsException() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000.0, 50.0, null);
    assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-bidder", 1000.0));
  }

  @Test
  @DisplayName("validate gia dat thap hon gia hien tai → InvalidBidException")
  void validate_lowerBid_throwsException() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000.0, 50.0, null);
    assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-bidder", 500.0));
  }

  @Test
  @DisplayName("validate buoc gia khong du → InvalidBidException")
  void validate_insufficientIncrement_throwsException() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000.0, 100.0, null);
    // bidAmount = 1050, increment = 50 < minimumIncrement = 100
    InvalidBidException ex = assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-bidder", 1050.0));
    assertTrue(ex.getMessage().contains("Buoc gia toi thieu"));
  }

  @Test
  @DisplayName("validate gia am → InvalidBidException")
  void validate_negativeBid_throwsException() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000.0, 50.0, null);
    assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user-bidder", -100.0));
  }

  @Test
  @DisplayName("validate auction chua co ai bid (highestBidderId null) — hop le")
  void validate_noBidsYet_valid() {
    Auction a = createAuction(AuctionStatus.RUNNING, 0.0, 50.0, null);
    assertDoesNotThrow(
        () -> validator.validate(a, "user-bidder", 100.0));
  }

  @Test
  @DisplayName("validate chinh xac minimumIncrement — dat dung du → hop le")
  void validate_exactIncrement_valid() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000.0, 50.0, null);
    assertDoesNotThrow(
        () -> validator.validate(a, "user-bidder", 1050.0));
  }

  // === 6 test cases bo sung (de dat >= 15 bid test cases) ===

  @Test
  @DisplayName("validate auction CANCELED → AuctionClosedException")
  void validate_canceledAuction_throwsClosed() {
    Auction a = createAuction(AuctionStatus.CANCELED, 1000, 50, null);
    assertThrows(AuctionClosedException.class,
        () -> validator.validate(a, "user1", 1100));
  }

  @Test
  @DisplayName("validate auction OPEN (chua bat dau) → AuctionClosedException")
  void validate_openAuction_throwsClosed() {
    Auction a = createAuction(AuctionStatus.OPEN, 0, 50, null);
    assertThrows(AuctionClosedException.class,
        () -> validator.validate(a, "user1", 100));
  }

  @Test
  @DisplayName("validate auction PAID → AuctionClosedException")
  void validate_paidAuction_throwsClosed() {
    Auction a = createAuction(AuctionStatus.PAID, 5000, 50, "winner");
    assertThrows(AuctionClosedException.class,
        () -> validator.validate(a, "user1", 6000));
  }

  @Test
  @DisplayName("validate gia dat 0 → InvalidBidException")
  void validate_zeroBid_throwsException() {
    Auction a = createAuction(AuctionStatus.RUNNING, 0, 50, null);
    assertThrows(InvalidBidException.class,
        () -> validator.validate(a, "user1", 0));
  }

  @Test
  @DisplayName("validate buoc gia dung chinh xac minimumIncrement → hop le")
  void validate_exactMinimumIncrement_valid() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000, 100, null);
    assertDoesNotThrow(() -> validator.validate(a, "user1", 1100));
  }

  @Test
  @DisplayName("validate bid vuot minimumIncrement nhieu → hop le")
  void validate_largeIncrement_valid() {
    Auction a = createAuction(AuctionStatus.RUNNING, 1000, 50, null);
    assertDoesNotThrow(() -> validator.validate(a, "user1", 5000));
  }
}
