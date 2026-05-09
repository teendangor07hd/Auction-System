package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.AuditLogDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuditLog;
import com.bidhub.server.model.BidTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dich vu xuat bao cao — chuyen du lieu tu DAO sang dang flat cho client/serializer.
 *
 * <p>Tra ve {@code List<Map<String, Object>>} — linh hoat, de serialize JSON.
 * 2 constructor: production (tao DAO moi) va test (inject DAO).
 *
 * <p>// 📌 [Tieu chi: MVC — Service layer chuyen du lieu cho Controller]
 */
public class ReportService {

  private final AuctionDao auctionDao;
  private final BidDao bidDao;
  private final AuditLogDao auditLogDao;

  /** Constructor production. */
  public ReportService() {
    this.auctionDao = new AuctionDao();
    this.bidDao = new BidDao();
    this.auditLogDao = new AuditLogDao();
  }

  /**
   * Constructor test — inject cac DAO.
   *
   * @param auctionDao  AuctionDao inject
   * @param bidDao      BidDao inject
   * @param auditLogDao AuditLogDao inject
   */
  public ReportService(AuctionDao auctionDao, BidDao bidDao,
      AuditLogDao auditLogDao) {
    this.auctionDao = auctionDao;
    this.bidDao = bidDao;
    this.auditLogDao = auditLogDao;
  }

  /**
   * Xuat bao cao tat ca auction — moi auction la 1 Map flat.
   *
   * <p>// 📌 [Tieu chi: MVC — ReportService lay du lieu tu DAO layer]
   *
   * @return danh sach map chua thong tin auction
   */
  public List<Map<String, Object>> exportAuctionReport() {
    List<Map<String, Object>> result = new ArrayList<>();
    List<Auction> auctions = auctionDao.findAll();
    for (Auction auction : auctions) {
      Map<String, Object> row = new HashMap<>();
      row.put("auctionId", auction.getId());
      row.put("itemId", auction.getItemId());
      row.put("status", auction.getStatus().name());
      row.put("startingPrice", auction.getStartingPrice());
      row.put("currentHighestBid", auction.getCurrentHighestBid());
      row.put("highestBidderId",
          auction.getHighestBidderId() != null ? auction.getHighestBidderId() : "N/A");
      row.put("startTime",
          auction.getStartTime() != null ? auction.getStartTime().toString() : "N/A");
      row.put("endTime",
          auction.getEndTime() != null ? auction.getEndTime().toString() : "N/A");
      result.add(row);
    }
    return result;
  }

  /**
   * Xuat lich su bid cua auction — sorted ASC theo bidTime.
   *
   * @param auctionId id auction
   * @return danh sach map chua thong tin bid
   */
  public List<Map<String, Object>> exportBidHistory(String auctionId) {
    List<Map<String, Object>> result = new ArrayList<>();
    List<BidTransaction> bids = bidDao.findByAuctionId(auctionId);
    for (BidTransaction bid : bids) {
      Map<String, Object> row = new HashMap<>();
      row.put("bidId", bid.getId());
      row.put("bidderId", bid.getBidderId());
      row.put("bidAmount", bid.getBidAmount());
      row.put("bidTime",
          bid.getBidTime() != null ? bid.getBidTime().toString() : "N/A");
      result.add(row);
    }
    return result;
  }

  /**
   * Xuat audit log gan day — goi auditLogDao.findRecent(limit).
   *
   * @param limit so ban ghi toi da (default 50)
   * @return danh sach map chua thong tin audit log
   */
  public List<Map<String, Object>> exportAuditLog(int limit) {
    List<Map<String, Object>> result = new ArrayList<>();
    List<AuditLog> logs = auditLogDao.findRecent(limit);
    for (AuditLog log : logs) {
      Map<String, Object> row = new HashMap<>();
      row.put("id", log.getId());
      row.put("userId",
          log.getUserId() != null ? log.getUserId() : "SYSTEM");
      row.put("action", log.getAction());
      row.put("details", log.getDetails());
      row.put("createdAt",
          log.getCreatedAt() != null ? log.getCreatedAt().toString() : "N/A");
      result.add(row);
    }
    return result;
  }
}
