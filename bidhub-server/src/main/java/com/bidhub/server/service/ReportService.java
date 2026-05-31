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
 * Dich vu xuat bao cao — chuyen du lieu từ DAO sang dang flat cho client/serializer.
 *
 * <p>Trả về {@code List<Map<String, Object>>} — linh hoat, để serialize JSON.
 * 2 constructor: production (tạo DAO moi) và test (inject DAO).
 *
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
   * Constructor test — inject các DAO.
   *
   * @param auctionDao  AuctionDao inject
   * @param bidDao      BidDao inject
   * @param auditLogDao AuditLogDao inject
   */
  public ReportService(AuctionDao auctionDao, BidDao bidDao,
      AuditLogDao auditLogDao, com.bidhub.server.dao.UserDao userDao, com.bidhub.server.dao.ItemDao itemDao) {
    this.auctionDao = auctionDao;
    this.bidDao = bidDao;
    this.auditLogDao = auditLogDao;
    this.userDao = userDao;
    this.itemDao = itemDao;
  }

  /**
   * Xuat bao cao tat ca auction — moi auction là 1 Map flat.
   *
   *    Truoc day moi auction goi itemDao.findById() và userDao.findById() rieng
   *    → O(n) DB round-trip. Này load 1 lan, lookup từ Map → O(1)]
   *
   * @return danh sach map chua thông tin auction
   */
  public List<Map<String, Object>> exportAuctionReport() {
    List<Map<String, Object>> result = new ArrayList<>();
    List<Auction> auctions = auctionDao.findAll();

    // Batch fetch items và users để tranh N+1 query
    Map<String, String> itemNameCache = buildItemNameCache();
    Map<String, String> userNameCache = buildUserNameCache();

    for (Auction auction : auctions) {
      Map<String, Object> row = new HashMap<>();
      row.put("auctionId", auction.getId());
      row.put("itemId", auction.getItemId());
      row.put("itemName", itemNameCache.getOrDefault(auction.getItemId(), "Item " + auction.getItemId()));
      row.put("status", auction.getStatus().name());
      row.put("startingPrice", auction.getStartingPrice());
      row.put("currentHighestBid", auction.getCurrentHighestBid());
      
      String winnerId = auction.getHighestBidderId();
      row.put("highestBidderId", winnerId != null ? winnerId : "N/A");
      row.put("winnerName", winnerId != null ? userNameCache.getOrDefault(winnerId, "User " + winnerId) : "N/A");

      row.put("startTime", formatDateTime(auction.getStartTime()));
      row.put("endTime", formatDateTime(auction.getEndTime()));
      result.add(row);
    }
    return result;
  }

  /**
   * Xuat lich su bid cua auction — sorted ASC theo bidTime.
   *
   *
   * @param auctionId id auction
   * @return danh sach map chua thông tin bid
   */
  public List<Map<String, Object>> exportBidHistory(String auctionId) {
    List<Map<String, Object>> result = new ArrayList<>();
    List<BidTransaction> bids;
    if ("ALL".equalsIgnoreCase(auctionId)) {
        bids = bidDao.findAll();
    } else {
        bids = bidDao.findByAuctionId(auctionId);
    }

    // Batch fetch — load 1 lan thay vi query tung row
    Map<String, String> userNameCache = buildUserNameCache();
    Map<String, String> auctionItemCache = buildAuctionItemCache();

    for (BidTransaction bid : bids) {
      Map<String, Object> row = new HashMap<>();
      row.put("bidId", bid.getId());
      row.put("auctionId", bid.getAuctionId());
      row.put("bidderId", bid.getBidderId());
      row.put("itemName", auctionItemCache.getOrDefault(bid.getAuctionId(), "Sản phẩm " + bid.getAuctionId()));

      String bidderId = bid.getBidderId();
      row.put("bidderName", bidderId != null ? userNameCache.getOrDefault(bidderId, "User " + bidderId) : "N/A");

      row.put("bidAmount", bid.getBidAmount());
      row.put("bidTime", formatDateTime(bid.getBidTime()));
      result.add(row);
    }
    return result;
  }

  /**
   * Xuat audit log gan day — goi auditLogDao.findRecent(limit).
   *
   *
   * @param limit so ban ghi toi da (default 50)
   * @return danh sach map chua thông tin audit log
   */
  public List<Map<String, Object>> exportAuditLog(int limit) {
    List<Map<String, Object>> result = new ArrayList<>();
    List<AuditLog> logs = auditLogDao.findRecent(limit);

    // Batch fetch users — moi log entry truoc day goi userDao.findById() rieng
    Map<String, String> userNameCache = buildUserNameCache();

    for (AuditLog log : logs) {
      Map<String, Object> row = new HashMap<>();
      row.put("id", log.getId());
      row.put("userId",
          log.getUserId() != null ? log.getUserId() : "SYSTEM");
      
      String userId = log.getUserId();
      String userName = "SYSTEM";
      if (userId != null && !userId.isEmpty() && !userId.equalsIgnoreCase("SYSTEM")) {
          userName = userNameCache.getOrDefault(userId, "User " + userId);
      }
      row.put("userName", userName);

      row.put("action", log.getAction());
      row.put("details", log.getDetails());
      row.put("createdAt", formatDateTime(log.getCreatedAt()));
      result.add(row);
    }
    return result;
  }

  // =========================================================================
  // Helper methods — batch cache builders
  // =========================================================================

  /**
   * Load tat ca items vao Map(itemId → itemName) — 1 query duy nhat.
   *
   */
  private Map<String, String> buildItemNameCache() {
      Map<String, String> cache = new HashMap<>();
      if (itemDao != null) {
          try {
              itemDao.findAll().forEach(item -> cache.put(item.getId(), item.getName()));
          } catch (Exception e) { /* DAO có the null trong test */ }
      }
      return cache;
  }

  /**
   * Load tat ca users vao Map(userId → username) — 1 query duy nhat.
   */
  private Map<String, String> buildUserNameCache() {
      Map<String, String> cache = new HashMap<>();
      if (userDao != null) {
          try {
              userDao.findAll().forEach(user -> cache.put(user.getId(), user.getUsername()));
          } catch (Exception e) { /* DAO có the null trong test */ }
      }
      return cache;
  }

  /**
   * Load tat ca auctions → map (auctionId → itemName) — tranh query chain auction→item.
   */
  private Map<String, String> buildAuctionItemCache() {
      Map<String, String> cache = new HashMap<>();
      Map<String, String> itemNameCache = buildItemNameCache();
      if (auctionDao != null) {
          try {
              auctionDao.findAll().forEach(auction ->
                  cache.put(auction.getId(),
                      itemNameCache.getOrDefault(auction.getItemId(), "Sản phẩm " + auction.getItemId())));
          } catch (Exception e) { /* DAO có the null trong test */ }
      }
      return cache;
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
