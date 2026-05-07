package com.bidhub.server.service;

import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.Item;
import java.util.Optional;

// ╔═══════════════════════════════════════════════════════════════════════╗
// ║  ⚠️⚠️⚠️ TOÀN BỘ FILE NÀY LÀ STUB — QUỐC MINH SẼ MERGE ⚠️⚠️⚠️          ║
// ║                                                                       ║
// ║  Branch gốc: feature/tuan-6-quocminh-bid-handler                      ║
// ║  XÓA TOÀN BỘ FILE NÀY khi rebase từ develop sau khi Quốc Minh merge  ║
// ║  Lý do: Khoa cần BidValidator để chạy BidValidatorTest                ║
// ║                                                                       ║
// ║  Nếu conflict khi rebase → giữ version của Quốc Minh, xóa file này   ║
// ╚═══════════════════════════════════════════════════════════════════════╝

/**
 * Validator kiem tra dieu kien dat gia — dam bao luat dau gia duoc thuc thi.
 *
 * <p>5 dieu kien validate:
 * <ol>
 *   <li>Auction phai dang RUNNING</li>
 *   <li>Bidder khong duoc la nguoi dan dau hien tai</li>
 *   <li>Bidder khong duoc la seller cua san pham</li>
 *   <li>Gia dat phai cao hon gia hien tai</li>
 *   <li>Buoc gia phai dat minimumIncrement</li>
 * </ol>
 *
 * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — tung dieu kien nem exception phu hop]
 * // 📌 [Tieu chi: Chuc nang dau gia — kiem tra luat truoc khi cho dat gia]
 */
public final class BidValidator {

  private final ItemDao itemDao;

  /** Constructor mac dinh — tao ItemDao production. */
  public BidValidator() {
    this.itemDao = new ItemDao();
  }

  /**
   * Constructor cho test — inject ItemDao.
   *
   * @param itemDao ItemDao inject
   */
  public BidValidator(ItemDao itemDao) {
    this.itemDao = itemDao;
  }

  /**
   * Kiem tra 5 dieu kien dat gia. Nem exception neu vi pham bat ky dieu kien nao.
   *
   * <p>// 📌 [Tieu chi: Xu ly loi & ngoai le — nem AuctionClosedException /
   *     InvalidBidException tuy theo loi]
   *
   * @param auction   auction can kiem tra
   * @param bidderId  id cua nguoi dau gia
   * @param bidAmount so tien dau gia
   * @throws AuctionClosedException neu auction khong dang RUNNING
   * @throws InvalidBidException   neu vi pham cac dieu kien dau gia
   */
  public void validate(Auction auction, String bidderId, double bidAmount) {
    // 1. Auction phai dang RUNNING
    // 📌 [Tieu chi: Chuc nang dau gia — chi cho dat gia khi RUNNING]
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      throw new AuctionClosedException(auction.getId(), auction.getStatus().name());
    }

    // 2. Bidder khong duoc la nguoi dan dau hien tai
    // 📌 [Tieu chi: Xu ly loi & ngoai le — kiem tra nguoi dan dau]
    if (auction.getHighestBidderId() != null
            && auction.getHighestBidderId().equals(bidderId)) {
      throw new InvalidBidException("Ban dang la nguoi dan dau.");
    }

    // 3. Bidder khong duoc la seller cua san pham
    // 📌 [Tieu chi: Xu ly loi & ngoai le — seller khong tu dau gia]
    String itemOwnerId = getItemOwnerId(auction.getItemId());
    if (itemOwnerId != null && itemOwnerId.equals(bidderId)) {
      throw new InvalidBidException("Seller khong the tu dau gia san pham cua minh.");
    }

    // 4. Gia dat phai cao hon gia hien tai
    // 📌 [Tieu chi: Xu ly loi & ngoai le — gia phai cao hon]
    if (bidAmount <= auction.getCurrentHighestBid()) {
      throw new InvalidBidException(
              "Gia dat phai cao hon gia hien tai (" + auction.getCurrentHighestBid() + ").");
    }

    // 5. Buoc gia phai dat minimumIncrement
    // 📌 [Tieu chi: Xu ly loi & ngoai le — kiem tra buoc gia]
    double increment = bidAmount - auction.getCurrentHighestBid();
    if (increment < auction.getMinimumIncrement()) {
      throw new InvalidBidException(
              "Buoc gia toi thieu la " + auction.getMinimumIncrement()
                      + ". Ban dat thieu " + (auction.getMinimumIncrement() - increment) + ".");
    }
  }

  /**
   * Lay owner id cua san pham tu ItemDao.
   *
   * @param itemId id san pham
   * @return sellerId hoac null neu khong tim thay
   */
  private String getItemOwnerId(String itemId) {
    if (itemId == null) {
      return null;
    }
    if (itemDao == null) {
      return null; // test mode — khong co ItemDao
    }
    Optional<Item> itemOpt = itemDao.findById(itemId);
    return itemOpt.map(Item::getSellerId).orElse(null);
  }
}