package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import java.sql.Connection;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.event.AuctionExtendedEvent;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.service.NotificationBroker;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite cho AntiSnipingEngine — kiem tra logic detect va gia han auction.
 *
 * <p>// 📌 [Tieu chi: Unit Test — anti-sniping test suite ≥ 5 cases]
 * // 📌 [Tieu chi: Anti-Sniping — verify gia han, khong gia han, null safe, finished auction]
 *
 * @author Đăng + Quốc Minh
 */
class AntiSnipingEngineTest {

    // KHÔNG cần mockAuctionDao kiểu cũ nữa
    private AuctionDao mockAuctionDao;
    private AntiSnipingEngine engine;

    @BeforeEach
    void setUp() {
        // Tạo một AuctionDao "giả" (mock) bằng Anonymous Class.
        // Nó không cần Connection thật, nên ta truyền null vào constructor.
        mockAuctionDao = new AuctionDao((Connection) null) {
            // Ghi đè method updateEndTime để nó KHÔNG làm gì với DB thật.
            @Override
            public void updateEndTime(String auctionId, LocalDateTime newEndTime) {
                // Ghi log ra console để xác nhận test đã gọi đến đây.
                System.out.println("[MOCK] AuctionDao.updateEndTime called for auctionId=" + auctionId + ", newEndTime=" + newEndTime);
                // Cố ý không làm gì: không gọi DB, không throw Exception.
            }
        };

        // Khởi tạo AntiSnipingEngine với mock Dao và config snipe = 60s, gia hạn = 60s.
        engine = new AntiSnipingEngine(mockAuctionDao, 60, 60);
    }

    private Auction createRunningAuction(String id, LocalDateTime endTime) {
        Auction a = new Auction();
        try {
            var idField = a.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, id);
            var endField = a.getClass().getDeclaredField("endTime");
            endField.setAccessible(true);
            endField.set(a, endTime);
            var statusField = a.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(a, AuctionStatus.RUNNING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return a;
    }

    @Test
    @DisplayName("Bid trong snipe window → auction được gia hạn 60 giây")
        // 📌 [Tieu chi: Anti-Sniping — verify endTime tang extensionSeconds khi bid trong window]
    void check_bidInSnipeWindow_extendsEndTime() {
        // endTime = now + 30 giay (trong snipe window 60 giay)
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(30);
        Auction auction = createRunningAuction("auc-snip-001", endTime);
        LocalDateTime originalEndTime = auction.getEndTime();

        // check() se thay endTime khong vi mockDao khong update DB — nhung RAM duoc cap nhat
        engine.check(auction);

        // Verify endTime trong RAM da tang 60 giay
        assertEquals(originalEndTime.plusSeconds(60), auction.getEndTime(),
                "endTime phai duoc gia han 60 giay khi bid nam trong snipe window");
    }

    @Test
    @DisplayName("Bid ngoài snipe window → auction KHÔNG được gia hạn")
        // 📌 [Tieu chi: Anti-Sniping — verify khong gia han khi bid o ngoai window]
    void check_bidOutsideSnipeWindow_noExtension() {
        // endTime = now + 120 giay (ngoài snipe window 60 giay)
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(120);
        Auction auction = createRunningAuction("auc-snip-002", endTime);
        LocalDateTime originalEndTime = auction.getEndTime();

        engine.check(auction);

        // Verify endTime khong thay doi
        assertEquals(originalEndTime, auction.getEndTime(),
                "endTime KHONG duoc gia han khi bid nam ngoai snipe window");
    }

    @Test
    @DisplayName("Bid đúng tại boundary snipe window → auction được gia hạn (isEqual)")
        // 📌 [Tieu chi: Anti-Sniping — verify boundary condition isEqual]
    void check_bidAtBoundary_extendsEndTime() {
        // endTime = now + 60 giay (đúng tại boundary)
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(60);
        Auction auction = createRunningAuction("auc-snip-003", endTime);

        engine.check(auction);

        // Boundary (isEqual) → gia han
        assertEquals(endTime.plusSeconds(60), auction.getEndTime(),
                "endTime phai duoc gia han khi bid dung tai boundary (isEqual)");
    }

    @Test
    @DisplayName("Auction null hoặc endTime null → không crash, không gia hạn")
        // 📌 [Tieu chi: Anti-Sniping — null safety]
    void check_nullAuctionOrEndTime_noException() {
        assertDoesNotThrow(() -> engine.check(null));

        Auction auctionNoEndTime = createRunningAuction("auc-snip-004", null);
        assertDoesNotThrow(() -> engine.check(auctionNoEndTime));
    }

    @Test
    @DisplayName("Auction FINISHED → không gia hạn dù bid trong snipe window")
        // 📌 [Tieu chi: Anti-Sniping — khong gia han auction da ket thuc]
    void check_finishedAuction_noExtension() {
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(30);
        Auction auction = createRunningAuction("auc-snip-005", endTime);

        // Chuyen status sang FINISHED
        try {
            var statusField = auction.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(auction, AuctionStatus.FINISHED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LocalDateTime originalEndTime = auction.getEndTime();
        engine.check(auction);

        assertEquals(originalEndTime, auction.getEndTime(),
                "Auction FINISHED khong duoc gia han");
    }
}