package com.bidhub.server.service;

import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager auctionManager;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
        auctionManager.clearAll();
    }

    @AfterEach
    void tearDown() {
        auctionManager.clearAll();
    }

    /**
     * Tạo một Auction test với constructor đầy đủ 11 tham số (dùng trong DAO).
     * Mặc định đặt trạng thái RUNNING, thời gian kết thúc trong tương lai.
     */
    private Auction createTestAuction(String id) {
        LocalDateTime now = LocalDateTime.now();
        return new Auction(
                id,                        // id
                now,                       // createdAt
                now,                       // updatedAt
                "item-" + id,              // itemId
                now,                       // startTime
                now.plusHours(1),          // endTime (tương lai 1h)
                1000.0,                    // startingPrice
                1000.0,                    // currentHighestBid (chưa có bid)
                null,                      // highestBidderId (chưa ai bid)
                AuctionStatus.RUNNING,     // status
                100.0                      // minimumIncrement
        );
    }

    @Test
    @DisplayName("getInstance tra ve cung instance (Singleton)")
    void getInstance_sameInstance() {
        AuctionManager a1 = AuctionManager.getInstance();
        AuctionManager a2 = AuctionManager.getInstance();
        assertSame(a1, a2);
    }

    @Test
    @DisplayName("addAuction them auction vao RAM")
    void addAuction_addsToMap() {
        Auction a = createTestAuction("auc-001");
        auctionManager.addAuction(a);
        assertEquals(1, auctionManager.activeCount());
    }

    @Test
    @DisplayName("getAuction voi id hop le tra ve auction")
    void getAuction_found() {
        Auction a = createTestAuction("auc-002");
        auctionManager.addAuction(a);
        Optional<Auction> result = auctionManager.getAuction("auc-002");
        assertTrue(result.isPresent());
        assertEquals("auc-002", result.get().getId());
    }

    @Test
    @DisplayName("getAuction voi id khong ton tai tra ve empty")
    void getAuction_notFound() {
        assertTrue(auctionManager.getAuction("fake-id").isEmpty());
    }

    @Test
    @DisplayName("removeAuction xoa auction khoi RAM")
    void removeAuction_removesFromMap() {
        Auction a = createTestAuction("auc-003");
        auctionManager.addAuction(a);
        auctionManager.removeAuction("auc-003");
        assertEquals(0, auctionManager.activeCount());
        assertTrue(auctionManager.getAuction("auc-003").isEmpty());
    }

    @Test
    @DisplayName("getAllActive tra ve copy — modify list khong anh huong map")
    void getAllActive_returnsCopy() {
        Auction a1 = createTestAuction("auc-004");
        Auction a2 = createTestAuction("auc-005");
        auctionManager.addAuction(a1);
        auctionManager.addAuction(a2);
        var list = auctionManager.getAllActive();
        assertEquals(2, list.size());
        list.clear();
        assertEquals(2, auctionManager.activeCount()); // map khong bi anh huong
    }

    @Test
    @DisplayName("addAuction voi null khong crash")
    void addAuction_nullSafe() {
        assertDoesNotThrow(() -> auctionManager.addAuction(null));
        assertEquals(0, auctionManager.activeCount());
    }

    @Test
    @DisplayName("activeCount tra ve dung so luong")
    void activeCount_correct() {
        assertEquals(0, auctionManager.activeCount());
        auctionManager.addAuction(createTestAuction("a1"));
        assertEquals(1, auctionManager.activeCount());
        auctionManager.addAuction(createTestAuction("a2"));
        assertEquals(2, auctionManager.activeCount());
        auctionManager.removeAuction("a1");
        assertEquals(1, auctionManager.activeCount());
    }
}