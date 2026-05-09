package com.bidhub.server.service;

import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BidValidatorTest {

    private BidValidator validator;

    @BeforeEach
    void setUp() {
        // Validator voi null ItemDao — chi test nhung case khong can ItemDao
        validator = new BidValidator(null);
    }

    private Auction createRunningAuction(double currentBid,
                                         double minIncrement, String highestBidderId) {
        Auction a = new Auction();
        try {
            var idField = a.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, "auc-test");
            var statusField = a.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(a, AuctionStatus.RUNNING);
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

    @Test
    @DisplayName("validate hop le — khong nem exception")
    void validate_validBid_noException() {
        Auction a = createRunningAuction(1000.0, 50.0, "user-other");
        assertDoesNotThrow(
                () -> validator.validate(a, "user-bidder", 1100.0));
    }

    @Test
    @DisplayName("validate auction FINISHED → AuctionClosedException")
    void validate_finishedAuction_throwsClosed() {
        Auction a = createRunningAuction(1000.0, 50.0, null);
        try {
            var statusField = a.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(a, AuctionStatus.FINISHED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThrows(AuctionClosedException.class,
                () -> validator.validate(a, "user-bidder", 1100.0));
    }

    @Test
    @DisplayName("validate nguoi dan dau bid lai → InvalidBidException")
    void validate_currentLeader_throwsException() {
        Auction a = createRunningAuction(1000.0, 50.0, "user-leader");
        InvalidBidException ex = assertThrows(InvalidBidException.class,
                () -> validator.validate(a, "user-leader", 1100.0));
        assertTrue(ex.getMessage().contains("dang la nguoi dan dau"));
    }

    @Test
    @DisplayName("validate gia dat bang gia hien tai → InvalidBidException")
    void validate_equalBid_throwsException() {
        Auction a = createRunningAuction(1000.0, 50.0, null);
        assertThrows(InvalidBidException.class,
                () -> validator.validate(a, "user-bidder", 1000.0));
    }

    @Test
    @DisplayName("validate gia dat thap hon gia hien tai → InvalidBidException")
    void validate_lowerBid_throwsException() {
        Auction a = createRunningAuction(1000.0, 50.0, null);
        assertThrows(InvalidBidException.class,
                () -> validator.validate(a, "user-bidder", 500.0));
    }

    @Test
    @DisplayName("validate buoc gia khong du → InvalidBidException")
    void validate_insufficientIncrement_throwsException() {
        Auction a = createRunningAuction(1000.0, 100.0, null);
        // bidAmount = 1050, increment = 50 < minimumIncrement = 100
        InvalidBidException ex = assertThrows(InvalidBidException.class,
                () -> validator.validate(a, "user-bidder", 1050.0));
        assertTrue(ex.getMessage().contains("Buoc gia toi thieu"));
    }

    @Test
    @DisplayName("validate gia am → InvalidBidException")
    void validate_negativeBid_throwsException() {
        Auction a = createRunningAuction(1000.0, 50.0, null);
        assertThrows(InvalidBidException.class,
                () -> validator.validate(a, "user-bidder", -100.0));
    }

    @Test
    @DisplayName("validate auciton chua co ai bid (highestBidderId null) — hop le")
    void validate_noBidsYet_valid() {
        Auction a = createRunningAuction(0.0, 50.0, null);
        assertDoesNotThrow(
                () -> validator.validate(a, "user-bidder", 100.0));
    }

    @Test
    @DisplayName("validate chinh xac minimumIncrement — dat dung du → hop le")
    void validate_exactIncrement_valid() {
        Auction a = createRunningAuction(1000.0, 50.0, null);
        assertDoesNotThrow(
                () -> validator.validate(a, "user-bidder", 1050.0));
    }
}