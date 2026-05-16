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
  private final com.bidhub.server.dao.UserDao userDao;
  private final com.bidhub.server.dao.ItemDao itemDao;

  /** Constructor production. */
  public ReportService() {
    this.auctionDao = new AuctionDao();
    this.bidDao = new BidDao();
    this.auditLogDao = new AuditLogDao();
    this.userDao = new com.bidhub.server.dao.UserDao();
    this.itemDao = new com.bidhub.server.dao.ItemDao();
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
    this.userDao = new com.bidhub.server.dao.UserDao();
    this.itemDao = new com.bidhub.server.dao.ItemDao();
  }

  /**
   * Xuat bao cao tat ca auction — moi auction là 1 Map flat.
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
      
      String itemId = auction.getItemId();
      String itemName = itemDao.findById(itemId).map(item -> item.getName()).orElse("Item " + itemId);
      row.put("itemName", itemName);

      row.put("status", auction.getStatus().name());
      row.put("startingPrice", auction.getStartingPrice());
      row.put("currentHighestBid", auction.getCurrentHighestBid());
      
      String winnerId = auction.getHighestBidderId();
      String winnerName = "N/A";
      if (winnerId != null && !winnerId.isEmpty()) {
          winnerName = userDao.findById(winnerId).map(u -> u.getUsername()).orElse("User " + winnerId);
      }
      row.put("highestBidderId", winnerId != null ? winnerId : "N/A");
      row.put("winnerName", winnerName);

      row.put("startTime", formatDateTime(auction.getStartTime()));
      row.put("endTime", formatDateTime(auction.getEndTime()));
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
    List<BidTransaction> bids;
    if ("ALL".equalsIgnoreCase(auctionId)) {
        bids = bidDao.findAll();
    } else {
        bids = bidDao.findByAuctionId(auctionId);
    }
    for (BidTransaction bid : bids) {
      Map<String, Object> row = new HashMap<>();
      row.put("bidId", bid.getId());
      row.put("auctionId", bid.getAuctionId());
      row.put("bidderId", bid.getBidderId());
      
      String bidderId = bid.getBidderId();
      String bidderName = "N/A";
      if (bidderId != null && !bidderId.isEmpty()) {
          bidderName = userDao.findById(bidderId).map(u -> u.getUsername()).orElse("User " + bidderId);
      }
      row.put("bidderName", bidderName);

      row.put("bidAmount", bid.getBidAmount());
      row.put("bidTime", formatDateTime(bid.getBidTime()));
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
      
      String userId = log.getUserId();
      String userName = "SYSTEM";
      if (userId != null && !userId.isEmpty() && !userId.equalsIgnoreCase("SYSTEM")) {
          userName = userDao.findById(userId).map(u -> u.getUsername()).orElse("User " + userId);
      }
      row.put("userName", userName);

      row.put("action", log.getAction());
      row.put("details", log.getDetails());
      row.put("createdAt", formatDateTime(log.getCreatedAt()));
      result.add(row);
    }
    return result;
  }

  private String formatDateTime(java.time.LocalDateTime dt) {
      if (dt == null) return "N/A";
      try {
          return dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
      } catch (Exception e) {
          return dt.toString();
      }
  }
}
