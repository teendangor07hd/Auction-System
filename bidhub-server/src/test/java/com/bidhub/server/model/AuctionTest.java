package com.bidhub.server.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử Auction: transitionTo(), isValidBid(), và BidTransaction. */
@DisplayName("Auction — transitionTo() và isValidBid()")
class AuctionTest {

    private Auction auction;
    private static final String ITEM_ID = "item-uuid-001";
    private static final String BIDDER_ID = "bidder-uuid-001";

    @BeforeEach
    void setUp() {
        // Tạo auction mới trước mỗi test: giá khởi điểm 1_000_000, tăng tối thiểu 100_000
        auction = new Auction(
                ITEM_ID,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1),
                1_000_000.0,
                100_000.0);
    }

    @Nested
    @DisplayName("transitionTo() — State Machine")
    class TransitionTests {

        @Test
        @DisplayName("Auction mới tạo có status OPEN")
        void testNewAuction_StatusIsOpen() {
            assertEquals(AuctionStatus.OPEN, auction.getStatus());
        }

        @Test
        @DisplayName("OPEN → RUNNING thành công")
        void testTransition_OpenToRunning_Success() {
            // Act
            auction.transitionTo(AuctionStatus.RUNNING);

            // Assert
            assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        }

        @Test
        @DisplayName("RUNNING → OPEN ném IllegalStateException")
        void testTransition_RunningToOpen_ThrowsException() {
            // Arrange
            auction.transitionTo(AuctionStatus.RUNNING);

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> auction.transitionTo(AuctionStatus.OPEN),
                    "Chuyển ngược từ RUNNING về OPEN phải ném IllegalStateException");
        }

        @Test
        @DisplayName("OPEN → FINISHED (bỏ qua RUNNING) ném IllegalStateException")
        void testTransition_OpenToFinished_ThrowsException() {
            assertThrows(IllegalStateException.class,
                    () -> auction.transitionTo(AuctionStatus.FINISHED),
                    "Bỏ qua RUNNING phải ném IllegalStateException");
        }

        @Test
        @DisplayName("RUNNING → FINISHED → PAID: chuỗi transition hợp lệ")
        void testTransition_FullLifecycle_Success() {
            auction.transitionTo(AuctionStatus.RUNNING);
            auction.transitionTo(AuctionStatus.FINISHED);
            auction.transitionTo(AuctionStatus.PAID);
            assertEquals(AuctionStatus.PAID, auction.getStatus());
        }
    }

    @Nested
    @DisplayName("isValidBid() — Kiểm tra bid hợp lệ")
    class BidValidationTests {

        @Test
        @DisplayName("Status OPEN → isValidBid() luôn false dù bidAmount cao hơn")
        void testIsValidBid_WhenOpen_ReturnsFalse() {
            // Auction đang OPEN (mặc định), không ai được đặt giá
            assertFalse(auction.isValidBid(2_000_000.0),
                    "OPEN không cho phép đặt giá dù bidAmount hợp lệ");
        }

        @Test
        @DisplayName("Status RUNNING, bidAmount > currentHighestBid → isValidBid() = true")
        void testIsValidBid_WhenRunning_HigherBid_ReturnsTrue() {
            // Arrange
            auction.transitionTo(AuctionStatus.RUNNING);

            // Act + Assert
            assertTrue(auction.isValidBid(1_100_000.0),
                    "RUNNING + bid cao hơn phải hợp lệ");
        }

        @Test
        @DisplayName("Status RUNNING, bidAmount = currentHighestBid → isValidBid() = false")
        void testIsValidBid_WhenRunning_EqualBid_ReturnsFalse() {
            auction.transitionTo(AuctionStatus.RUNNING);
            // bidAmount bằng chính xác currentHighestBid (= startingPrice = 1_000_000)
            assertFalse(auction.isValidBid(1_000_000.0),
                    "Bid bằng currentHighestBid không hợp lệ (phải lớn HƠN)");
        }

        @Test
        @DisplayName("Status RUNNING, bidAmount < currentHighestBid → isValidBid() = false")
        void testIsValidBid_WhenRunning_LowerBid_ReturnsFalse() {
            auction.transitionTo(AuctionStatus.RUNNING);
            assertFalse(auction.isValidBid(500_000.0),
                    "Bid thấp hơn currentHighestBid phải false");
        }

        @Test
        @DisplayName("updateHighestBid() cập nhật giá và bidderId đúng")
        void testUpdateHighestBid_UpdatesCorrectly() {
            // Arrange
            auction.transitionTo(AuctionStatus.RUNNING);

            // Act
            auction.updateHighestBid(1_500_000.0, BIDDER_ID);

            // Assert
            assertEquals(1_500_000.0, auction.getCurrentHighestBid(), 0.01);
            assertEquals(BIDDER_ID, auction.getHighestBidderId());
        }
    }

    @Nested
    @DisplayName("BidTransaction — bất biến và UUID")
    class BidTransactionTests {

        @Test
        @DisplayName("BidTransaction mới tạo có bidTime không null")
        void testBidTransaction_BidTime_NotNull() {
            BidTransaction tx = new BidTransaction("auction-1", "bidder-1", 2_000_000.0);
            assertNotNull(tx.getBidTime(), "bidTime không được null");
            assertNotNull(tx.getId(), "id không được null");
        }

        @Test
        @DisplayName("BidTransaction với bidAmount = 0 → IllegalArgumentException")
        void testBidTransaction_ZeroAmount_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new BidTransaction("a", "b", 0),
                    "bidAmount = 0 phải ném exception");
        }
    }
}