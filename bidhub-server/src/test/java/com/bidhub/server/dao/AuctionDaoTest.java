package com.bidhub.server.dao;

import com.bidhub.server.model.*;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test AuctionDao và BidDao qua in-memory SQLite.
 * Dùng constructor inject(Connection) — xem phần UserDao để biết cách setup.
 */
class AuctionDaoTest {

    private Connection conn;
    private AuctionDao auctionDao;
    private BidDao bidDao;

    @BeforeEach
    void setup() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement s = conn.createStatement()) {
            s.execute("""
          CREATE TABLE auctions (
            id TEXT PRIMARY KEY, item_id TEXT, start_time TEXT, end_time TEXT,
            starting_price REAL, current_highest_bid REAL, highest_bidder_id TEXT,
            status TEXT, minimum_increment REAL DEFAULT 1.0,
            created_at TEXT, updated_at TEXT)
          """);
            s.execute("""
          CREATE TABLE bid_transactions (
            id TEXT PRIMARY KEY, auction_id TEXT, bidder_id TEXT,
            bid_amount REAL, bid_time TEXT)
          """);
        }
        auctionDao = new AuctionDao(conn); // inject in-memory connection
        bidDao    = new BidDao(conn);
    }

    @AfterEach
    void teardown() throws SQLException { conn.close(); }

    @Test
    @DisplayName("save Auction → findById trả về đúng status OPEN")
    void auction_saveAndFind_correctStatus() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction("item-1", now, now.plusHours(2), 1_000_000.0, 50_000.0);

        auctionDao.save(auction);                        // thực sự persist vào DB

        Optional<Auction> found = auctionDao.findById(auction.getId());
        assertTrue(found.isPresent(), "findById phải trả về Optional không rỗng");
        assertEquals(AuctionStatus.OPEN, found.get().getStatus());
        assertEquals(1_000_000.0, found.get().getCurrentHighestBid(), 0.01);
    }

    @Test
    @DisplayName("findActiveAuctions chỉ trả về phiên có status RUNNING")
    void findActiveAuctions_onlyReturnsRunning() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        Auction running = new Auction("item-2", now, now.plusHours(1), 500_000.0, 10_000.0);
        running.transitionTo(AuctionStatus.RUNNING);
        Auction open = new Auction("item-3", now, now.plusHours(3), 200_000.0, 5_000.0);

        auctionDao.save(running);
        auctionDao.save(open);

        List<Auction> active = auctionDao.findActiveAuctions();
        assertEquals(1, active.size(), "Chỉ 1 phiên RUNNING trong DB");
        assertEquals(running.getId(), active.get(0).getId());
    }

    @Test
    @DisplayName("updateHighestBid → getCurrentHighestBid phản ánh giá mới sau khi load lại")
    void updateHighestBid_reflectsNewAmountInDb() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction("item-4", now, now.plusHours(1), 1_000_000.0, 50_000.0);
        auctionDao.save(auction);

        auctionDao.updateHighestBid(auction.getId(), 1_500_000.0, "bidder-xyz");

        Optional<Auction> reloaded = auctionDao.findById(auction.getId());
        assertTrue(reloaded.isPresent());
        assertEquals(1_500_000.0, reloaded.get().getCurrentHighestBid(), 0.01,
                "Giá cao nhất phải là 1_500_000 sau khi updateHighestBid");
        assertEquals("bidder-xyz", reloaded.get().getHighestBidderId());
    }

    @Test
    @DisplayName("BidDao.save + getHighestBid → trả về bid có bidAmount lớn nhất")
    void bidDao_getHighestBid_returnsMaxAmount() throws SQLException {
        String auctionId = "auction-test-1";
        BidTransaction b1 = new BidTransaction(auctionId, "bidder-1", 1_000_000.0);
        BidTransaction b2 = new BidTransaction(auctionId, "bidder-2", 2_000_000.0);
        BidTransaction b3 = new BidTransaction(auctionId, "bidder-1", 1_500_000.0);

        bidDao.save(b1);
        bidDao.save(b2);
        bidDao.save(b3);

        Optional<BidTransaction> highest = bidDao.getHighestBid(auctionId);
        assertTrue(highest.isPresent());
        assertEquals(2_000_000.0, highest.get().getBidAmount(), 0.01,
                "b2 có bidAmount lớn nhất phải được trả về");
    }

    @Test
    @DisplayName("BidDao.findByAuctionId → kết quả sắp xếp theo bid_time ASC")
    void bidDao_findByAuctionId_sortedByTimeAsc() throws SQLException {
        String auctionId = "auction-test-2";
        // Tạo 3 bid thủ công với bidTime cố định (tránh flaky vì LocalDateTime.now())
        LocalDateTime t1 = LocalDateTime.of(2026, 4, 21, 10, 0, 0);
        LocalDateTime t2 = t1.plusMinutes(1);
        LocalDateTime t3 = t1.plusMinutes(2);

        // Dùng constructor 5-param DB-load để set bidTime cố định
        BidTransaction bt1 = new BidTransaction(
                "bid-1", auctionId, "bidder-A", 1_000_000.0, t1);
        BidTransaction bt2 = new BidTransaction(
                "bid-2", auctionId, "bidder-B", 1_200_000.0, t3); // insert ngược thứ tự
        BidTransaction bt3 = new BidTransaction(
                "bid-3", auctionId, "bidder-C", 1_100_000.0, t2);

        bidDao.save(bt1);
        bidDao.save(bt2);
        bidDao.save(bt3);

        List<BidTransaction> result = bidDao.findByAuctionId(auctionId);
        assertEquals(3, result.size());
        // Kiểm tra thứ tự ASC theo bid_time: t1 < t2 < t3
        assertTrue(result.get(0).getBidTime().isBefore(result.get(1).getBidTime()),
                "result[0].bidTime phải trước result[1].bidTime");
        assertTrue(result.get(1).getBidTime().isBefore(result.get(2).getBidTime()),
                "result[1].bidTime phải trước result[2].bidTime");
    }

    @Test
    @DisplayName("AuctionDao.mapRow() không NPE khi highest_bidder_id là NULL trong DB")
    void auctionDao_mapRow_nullHighestBidderIdNotCrash() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        // Auction mới tạo chưa có ai đặt giá → highestBidderId = null
        Auction auction = new Auction("item-null", now, now.plusHours(1), 500_000.0, 10_000.0);
        auctionDao.save(auction); // persist — highest_bidder_id = NULL trong DB

        Optional<Auction> found = auctionDao.findById(auction.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getHighestBidderId(),
                "highestBidderId phải là null khi chưa ai đặt giá — không được NPE");
    }
}