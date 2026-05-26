package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.dao.UserDao;
import java.util.*;

/**
 * Dich vu kiem tra toan ven du lieu — cross-validation giua cac bang.
 *
 * <p>Phat hien inconsistency do bug, race condition, hoac partial failure.
 * 4 method chinh: checkBidConsistency, checkAuctionWinners, checkOrphanedItems,
 * runFullCheck.
 *
 * <p>// 📌 [Tieu chi: Clean Code — verify data consistency]
 * // 📌 [Tieu chi: Unit Test — DataIntegrityService tests]
 */
public final class DataIntegrityService {

  private final AuctionDao auctionDao;
  private final BidDao bidDao;
  private final ItemDao itemDao;
  private final UserDao userDao;

  /** Constructor production — tao DAO moi. */
  public DataIntegrityService() {
    this.auctionDao = new AuctionDao();
    this.bidDao = new BidDao();
    this.itemDao = new ItemDao();
    this.userDao = new UserDao();
  }

  /**
   * Constructor test — inject cac DAO.
   *
   * @param auctionDao AuctionDao inject
   * @param bidDao     BidDao inject
   * @param itemDao    ItemDao inject
   * @param userDao    UserDao inject
   */
  public DataIntegrityService(AuctionDao auctionDao, BidDao bidDao,
      ItemDao itemDao, UserDao userDao) {
    this.auctionDao = auctionDao;
    this.bidDao = bidDao;
    this.itemDao = itemDao;
    this.userDao = userDao;
  }

  /**
   * Kiem tra tinh nhat quan giua currentHighestBid trong auctions va
   * MAX(bid_amount) trong bid_transactions.
   *
   * <p>Cho tung auction: lay currentHighestBid tu auctions table, so sanh voi
   * MAX(bid_amount) tu bid_transactions. Neu khac nhau → inconsistent.
   *
   * @return danh sach mo ta loi (rong neu OK)
   */
  public List<String> checkBidConsistency() {
    List<String> errors = new ArrayList<>();
    try {
      List<AuctionDao.AuctionBidDto> auctions = auctionDao.findAllWithBidInfo();

      for (AuctionDao.AuctionBidDto row : auctions) {
        String auctionId = row.id;
        String itemName = row.itemName;
        double dbHighestBid = row.currentHighestBid;
        String dbHighestBidder = row.highestBidderId;

        Optional<com.bidhub.server.model.BidTransaction> maxBid =
            bidDao.getHighestBid(auctionId);

        if (maxBid.isPresent()) {
          double actualMaxBid = maxBid.get().getBidAmount();
          if (Math.abs(dbHighestBid - actualMaxBid) > 0.001) {
            errors.add("Sản phẩm '" + itemName + "' (Auction " + auctionId + "): Giá cao nhất ghi nhận là "
                + String.format("%,.0f VND", dbHighestBid) + " nhưng lịch sử Bid cao nhất trong DB chỉ có " + String.format("%,.0f VND", actualMaxBid));
          }
          // Kiem tra highestBidderId co khop voi MAX bidder
          if (dbHighestBidder != null && !dbHighestBidder.isEmpty()
              && !dbHighestBidder.equals(maxBid.get().getBidderId())) {
            errors.add("Sản phẩm '" + itemName + "' (Auction " + auctionId + "): Người thắng ghi nhận là "
                + dbHighestBidder + " nhưng người đặt giá cao nhất thực tế là " + maxBid.get().getBidderId());
          }
        } else {
          // Khong co bid nao — currentHighestBid phai = startingPrice hoac 0
          if (dbHighestBid > 0 && dbHighestBidder != null) {
            errors.add("Sản phẩm '" + itemName + "' (Auction " + auctionId + "): Ghi nhận có người thắng với giá "
                + String.format("%,.0f VND", dbHighestBid) + " nhưng không tồn tại bất kỳ lượt Bid nào trong DB");
          }
        }
      }
    } catch (Exception e) {
      errors.add("Loi checkBidConsistency: " + e.getMessage());
    }
    return errors;
  }

  /**
   * Kiem tra FINISHED auctions co bids nhung chua xac dinh winner.
   *
   * @return danh sach mo ta loi (rong neu OK)
   */
  public List<String> checkAuctionWinners() {
    List<String> errors = new ArrayList<>();
    try {
      List<com.bidhub.server.model.Auction> auctions = auctionDao.findAll();
      for (com.bidhub.server.model.Auction auction : auctions) {
        if (auction.getStatus() == com.bidhub.server.model.AuctionStatus.FINISHED) {
          Optional<com.bidhub.server.model.BidTransaction> highestBid =
              bidDao.getHighestBid(auction.getId());
          if (highestBid.isPresent() && auction.getHighestBidderId() == null) {
            errors.add("Auction " + auction.getId()
                + ": FINISHED co bids nhung highestBidderId = null. "
                + "Winner nen la: " + highestBid.get().getBidderId());
          }
        }
      }
    } catch (Exception e) {
      errors.add("Loi checkAuctionWinners: " + e.getMessage());
    }
    return errors;
  }

  /**
   * Kiem tra items co sellerId khong ton tai trong bang users.
   *
   * @return danh sach mo ta loi (rong neu OK)
   */
  public List<String> checkOrphanedItems() {
    List<String> errors = new ArrayList<>();
    try {
      Set<String> validUserIds = new HashSet<>();
      userDao.findAll().forEach(u -> validUserIds.add(u.getId()));

      List<com.bidhub.server.model.Item> items = itemDao.findAll();
      for (com.bidhub.server.model.Item item : items) {
        if (!validUserIds.contains(item.getSellerId())) {
          errors.add("Item " + item.getId() + " ('" + item.getName()
              + "'): sellerId=" + item.getSellerId() + " khong ton tai trong bang users");
        }
      }
    } catch (Exception e) {
      errors.add("Loi checkOrphanedItems: " + e.getMessage());
    }
    return errors;
  }

  /**
   * Chay toan bo kiem tra — tong hop ket qua.
   *
   * <p>Tra ve Map chua ket qua 3 check + tong errors + status.
   *
   * @return Map voi key: bidConsistencyErrors, auctionWinnerErrors,
   *         orphanedItemErrors, totalErrors, status
   */
  public Map<String, Object> runFullCheck() {
    Map<String, Object> result = new LinkedHashMap<>();
    List<String> bidErrors = checkBidConsistency();
    List<String> winnerErrors = checkAuctionWinners();
    List<String> orphanErrors = checkOrphanedItems();
    int total = bidErrors.size() + winnerErrors.size() + orphanErrors.size();
    result.put("bidConsistencyErrors", bidErrors);
    result.put("auctionWinnerErrors", winnerErrors);
    result.put("orphanedItemErrors", orphanErrors);
    result.put("totalErrors", total);
    result.put("status", total == 0 ? "OK" : "ERRORS_FOUND");
    return result;
  }
}
