package com.bidhub.server.model;

import com.bidhub.common.model.Item;
import com.bidhub.common.model.ItemFactory;
import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.common.exception.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test tích hợp domain: kết hợp User, Auction, Item, Exception.
 *
 * <p>Mỗi test mô phỏng một luồng nghiệp vụ thực tế để đảm bảo
 * các class hoạt động đúng khi tương tác với nhau.
 */
@DisplayName("DomainIntegration — Test tích hợp domain Tuần 2")
class DomainIntegrationTest {

    @Test
    @DisplayName("Kịch bản: Bidder đặt giá hợp lệ vào phiên RUNNING")
    void testScenario_BidderPlacesValidBid() {
        // Arrange
        Bidder bidder = new Bidder("alice", "hash_alice", "alice@mail.com");
        Auction auction = new Auction(
                "item-001",
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusMinutes(30),
                5_000_000.0, 500_000.0);
        auction.transitionTo(AuctionStatus.RUNNING);

        // Act — kiểm tra bid hợp lệ
        double bidAmount = 6_000_000.0;
        boolean valid = auction.isValidBid(bidAmount);

        // Assert
        assertTrue(valid, "Bid 6 triệu vào phiên RUNNING với giá KĐ 5 triệu phải hợp lệ");

        // Cập nhật highest bid
        auction.updateHighestBid(bidAmount, bidder.getId());
        assertEquals(bidAmount, auction.getCurrentHighestBid(), 0.01);
        assertEquals(bidder.getId(), auction.getHighestBidderId());
    }

    @Test
    @DisplayName("Kịch bản: Validation user registration — nhiều lỗi cùng lúc")
    void testScenario_ValidateUserRegistration_MultipleErrors() {
        // Arrange — mô phỏng validation logic (tuần 5 sẽ implement thật)
        String username = "ab";    // quá ngắn
        String password = "123";   // quá ngắn
        String email = "";         // rỗng

        List<String> errors = new ArrayList<>();
        if (username.length() < 3) errors.add("Username phải ≥ 3 ký tự");
        if (password.length() < 6) errors.add("Password phải ≥ 6 ký tự");
        if (email.isBlank()) errors.add("Email không được để trống");

        // Act
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> { if (!errors.isEmpty()) throw new ValidationException(errors); });

        // Assert
        assertEquals(3, ex.getErrorCount(), "Phải có đúng 3 lỗi validation");
        assertTrue(ex.getMessage().contains("3 lỗi validation"));
    }

    @Test
    @DisplayName("Kịch bản: Exception hierarchy trong luồng đặt giá")
    void testScenario_BidOnClosedAuction_ThrowsAuctionClosedException() {
        // Arrange
        Auction auction = new Auction("item-002",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                1_000_000.0, 0.0);
        auction.transitionTo(AuctionStatus.RUNNING);
        auction.transitionTo(AuctionStatus.FINISHED);

        // Act — mô phỏng logic sẽ có trong BidService (Tuần 6)
        AuctionClosedException ex = assertThrows(
                AuctionClosedException.class,
                () -> {
                    if (auction.getStatus().isTerminal()) {
                        throw new AuctionClosedException(auction.getId(), auction.getStatus().name());
                    }
                });

        // Assert
        assertEquals("AUCTION_CLOSED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("FINISHED"));
    }

    @Test
    @DisplayName("Kịch bản: ItemFactory + Auction + Bidder hoạt động cùng nhau")
    void testScenario_FullDomainObjectsInteraction() {
        // Arrange — tạo đầy đủ objects domain
        Seller seller = new Seller("bob_seller", "hash_bob", "bob@mail.com");
        Item laptop = ItemFactory.create(
                ItemType.ELECTRONICS, "Dell XPS 15", "Laptop cao cấp",
                30_000_000.0, seller.getId(),
                java.util.Map.of("brand", "Dell", "warrantyMonths", 24));

        Auction auction = new Auction(
                laptop.getId(),
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(2),
                laptop.getStartingPrice(), 1_000_000.0);
        auction.transitionTo(AuctionStatus.RUNNING);

        Bidder bidder = new Bidder("carol_bidder", "hash_carol", "carol@mail.com");

        // Act
        double bidAmount = laptop.getStartingPrice() + 2_000_000.0; // 32 triệu
        assertTrue(auction.isValidBid(bidAmount));
        auction.updateHighestBid(bidAmount, bidder.getId());

        BidTransaction tx = new BidTransaction(auction.getId(), bidder.getId(), bidAmount);

        // Assert — kiểm tra toàn bộ domain kết nối đúng
        assertEquals(bidAmount, auction.getCurrentHighestBid(), 0.01);
        assertEquals(bidder.getId(), auction.getHighestBidderId());
        assertEquals(auction.getId(), tx.getAuctionId());
        assertEquals(bidder.getId(), tx.getBidderId());
        assertNotNull(tx.getBidTime());

        // Seller, laptop, auction, bidder đều có UUID riêng
        assertNotEquals(seller.getId(), laptop.getId());
        assertNotEquals(laptop.getId(), auction.getId());
    }
}