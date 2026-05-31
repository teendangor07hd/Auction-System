package com.bidhub.server.service;

import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AuctionLifecycleTaskTest {

    private AuctionManager auctionManager;
    private AuctionLifecycleTask task;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
        auctionManager.clearAll();
        task = new AuctionLifecycleTask();
    }

    @AfterEach
    void tearDown() {
        auctionManager.clearAll();
    }

    /**
     * Tạo auction đã hết hạn (endTime = quá khứ) để test đóng phiên.
     */
    private Auction createExpiredAuction(String id) {
        LocalDateTime now = LocalDateTime.now();
        return new Auction(
                id,                        // id
                now.minusHours(2),         // createdAt (quá khứ)
                now.minusMinutes(10),      // updatedAt
                "item-" + id,
                now.minusHours(2),         // startTime
                now.minusMinutes(10),      // endTime (đã quá hạn 10 phút)
                1000.0,
                1000.0,
                null,
                AuctionStatus.RUNNING,
                100.0
        );
    }

    /**
     * Tạo auction còn hạn (endTime = tương lai).
     */
    private Auction createFutureAuction(String id) {
        LocalDateTime now = LocalDateTime.now();
        return new Auction(
                id,
                now,
                now,
                "item-" + id,
                now,
                now.plusHours(1),          // endTime trong tương lai
                1000.0,
                1000.0,
                null,
                AuctionStatus.RUNNING,
                100.0
        );
    }

    @Test
    @DisplayName("run() khong crash khi danh sach rong")
    void run_emptyList_noCrash() {
        assertDoesNotThrow(() -> task.run());
    }

    @Test
    @DisplayName("run() khong crash khi auction con han — khong dong phien")
    void run_futureAuction_noClose() {
        auctionManager.addAuction(createFutureAuction("auc-future"));
        task.run(); // Không crash, auction van trong RAM vi chua het han
        // Không kiem tra DB update vi test không có DB — nhung logic kiem tra endTime se skip.
        // Auction van con trong RAM (nếu lifecycle task không remove)
        // Có the kiem tra activeCount = 1 (tuy vao cai dat remove hay không)
        assertEquals(1, auctionManager.activeCount(),
                "Auction chua het han khong bi remove");
    }

    @Test
    @DisplayName("run() voi auction het han — goi closeAuction (can DB, nen test logic co ban)")
    void run_expiredAuction_triggersCloseLogic() {
        // Test này kiem tra rang run() không crash khi gap auction het han.
        // Vi test không có DB, closeAuction() se that bai, nhung AuctionLifecycleTask da
        // được thiet ke để bat exception và van tiep tuc.
        auctionManager.addAuction(createExpiredAuction("auc-expired"));
        assertDoesNotThrow(() -> task.run(),
                "Task phai xu ly exception DB, khong lam crash toan bo");
    }
}