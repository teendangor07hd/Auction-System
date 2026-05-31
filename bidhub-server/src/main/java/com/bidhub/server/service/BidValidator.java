package com.bidhub.server.service;

import com.bidhub.common.exception.AuctionClosedException;
import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.server.dao.ItemDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.Item;
import java.util.Optional;

/**
 * Validator kiểm tra điều kiện đặt giá — đảm bảo luật đấu giá được thực thi.
 *
 * <p>5 điều kiện validate:
 * <ol>
 *   <li>Auction phải đang RUNNING</li>
 *   <li>Bidder không được là người dẫn đầu hiện tại</li>
 *   <li>Bidder không được là seller của sản phẩm</li>
 *   <li>Giá đặt phải cao hơn giá hiện tại</li>
 *   <li>Bước giá phải đạt minimumIncrement</li>
 * </ol>
 *
 */
public final class BidValidator {

    private final ItemDao itemDao;

    /** Constructor mặc định — tạo ItemDao dùng trong môi trường thực tế. */
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
     * Kiểm tra 5 điều kiện đặt giá. Ném exception nếu vi phạm bất kỳ điều kiện nào.
     *
     * @param auction   Auction cần kiểm tra
     * @param bidderId  ID của người đấu giá
     * @param bidAmount Số tiền đấu giá
     * @throws AuctionClosedException nếu auction không đang RUNNING
     * @throws InvalidBidException   nếu vi phạm các điều kiện đấu giá
     */
    public void validate(Auction auction, String bidderId, double bidAmount) {
        if (auction.getStatus() == AuctionStatus.OPEN) {
            throw new InvalidBidException("Phiên đấu giá chưa bắt đầu. Vui lòng chờ đến giờ.");
        } else if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException(
                    "Phiên đấu giá đã kết thúc. Trạng thái: " + auction.getStatus().name());
        }

        // Kiểm tra: bidder không được là người dẫn đầu hiện tại
        if (auction.getHighestBidderId() != null
                && auction.getHighestBidderId().equals(bidderId)) {
            throw new InvalidBidException("Ban dang la nguoi dan dau.");
        }

        // Kiểm tra: bidder không được là seller của sản phẩm
        String itemOwnerId = getItemOwnerId(auction.getItemId());
        if (itemOwnerId != null && itemOwnerId.equals(bidderId)) {
            throw new InvalidBidException("Seller khong the tu dau gia san pham cua minh.");
        }

        // Kiểm tra: giá đặt phải cao hơn giá hiện tại
        if (bidAmount <= auction.getCurrentHighestBid()) {
            throw new InvalidBidException(
                    "Gia dat phai cao hon gia hien tai (" + auction.getCurrentHighestBid() + ").");
        }

        // Kiểm tra: bước giá phải đạt minimumIncrement
        double increment = bidAmount - auction.getCurrentHighestBid();
        if (increment < auction.getMinimumIncrement()) {
            throw new InvalidBidException(
                    "Buoc gia toi thieu la " + auction.getMinimumIncrement()
                            + ". Ban dat thieu " + (auction.getMinimumIncrement() - increment) + ".");
        }
    }

    /**
     * Lấy ID người sở hữu sản phẩm từ ItemDao.
     *
     * @param itemId ID sản phẩm cần tra cứu
     * @return sellerId hoặc null nếu không tìm thấy
     */
    private String getItemOwnerId(String itemId) {
        if (itemId == null) {
            return null;
        }
        Optional<Item> itemOpt = itemDao.findById(itemId);
        return itemOpt.map(Item::getSellerId).orElse(null);
    }
}