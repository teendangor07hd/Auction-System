package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Một lần đặt giá trong hệ thống BidHub.
 *
 * <p>Kế thừa {@link Entity} — mỗi BidTransaction có id riêng để truy vết.
 * Không thể thay đổi sau khi tạo ({@code final} fields) — lịch sử đặt giá
 * là bất biến: không ai được phép sửa lại một giao dịch đã thực hiện.
 *
 * <p>Một phiên đấu giá ({@link Auction}) có thể có nhiều {@code BidTransaction}.
 * Mối quan hệ: Auction (1) ——— (*) BidTransaction
 */
public final class BidTransaction extends Entity {

        /**
         * ID phiên đấu giá mà bid này thuộc về.
         */
        private final String auctionId;

        /**
         * ID người đặt giá.
         */
        private final String bidderId;

        /**
         * Mức giá đặt (VND).
         */
        private final double bidAmount;

        /**
         */
        private final LocalDateTime bidTime;

        /**
         * Tạo một BidTransaction mới.
         *
         * @param auctionId id phiên đấu giá, không null
         * @param bidderId  id người đặt giá, không null
         * @param bidAmount mức giá, phải > 0
         * @throws IllegalArgumentException nếu bidAmount <= 0
         */
        public BidTransaction(String auctionId, String bidderId, double bidAmount) {
            super();
            Objects.requireNonNull(auctionId, "auctionId không được null");
            Objects.requireNonNull(bidderId, "bidderId không được null");
            if (bidAmount <= 0) {
                throw new IllegalArgumentException("bidAmount phải > 0: " + bidAmount);
            }
            this.auctionId = auctionId;
            this.bidderId = bidderId;
            this.bidAmount = bidAmount;
            this.bidTime = LocalDateTime.now();
        }



        // Getters — tất cả fields là final, không có setter (immutable record of bid)
        /** Trả về id phiên đấu giá. */
        public String getAuctionId() { return auctionId; }
        /** Trả về id người đặt giá. */
        public String getBidderId() { return bidderId; }
        /** Trả về mức giá đã đặt. */
        public double getBidAmount() { return bidAmount; }
        /** Trả về thời điểm đặt giá. */
        public LocalDateTime getBidTime() { return bidTime; }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "BidTransaction[id=" + (getId() != null ? getId().substring(0, Math.min(7, getId().length())) : "null")
                    + ", auctionId=" + (auctionId != null ? auctionId.substring(0, Math.min(7, auctionId.length())) : "null")
                    + ", bidderId=" + (bidderId != null ? bidderId.substring(0, Math.min(7, bidderId.length())) : "null")
                    + String.format(", amount=%,.0f VND", bidAmount)
                    + ", time=" + bidTime + "]";
        }

    /**
     * Constructor load từ database — dành cho BidDao.mapRow().
     *
     * <p>Bảng bid_transactions không lưu created_at/updated_at riêng;
     * dùng bidTime cho cả hai để giữ nguyên contract của Entity.
     *
     * @param id        id từ DB
     * @param auctionId id phiên đấu giá
     * @param bidderId  id ngườii đặt giá
     * @param bidAmount mức giá
     * @param bidTime   thờii điểm đặt giá từ DB (cũng dùng làm createdAt/updatedAt)
     */
    public BidTransaction(
            String id,
            String auctionId,
            String bidderId,
            double bidAmount,
            LocalDateTime bidTime) {
        // Dùng bidTime cho createdAt và updatedAt vì schema không lưu chúng riêng
        super(id, bidTime, bidTime);
        Objects.requireNonNull(auctionId, "auctionId không được null");
        Objects.requireNonNull(bidderId, "bidderId không được null");
        Objects.requireNonNull(bidTime, "bidTime không được null");
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("bidAmount phải > 0: " + bidAmount);
        }
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }
}
