package com.bidhub.server.event;

import java.time.LocalDateTime;

/**
 * Event thong bao bid moi — gui realtime den tat ca client subscribe auction.
 *
 * <p>// 📌 [Tieu chi: Realtime update Observer/Socket — push event qua socket]
 * // 📌 [Tieu chi: Design Pattern Observer — event object cho notify]
 */
public final class BidUpdateEvent {

    private final String auctionId;
    private final String bidderId;
    private final double bidAmount;
    private final LocalDateTime timestamp;

    /**
     * Tao BidUpdateEvent.
     *
     * @param auctionId id cua auction
     * @param bidderId  id cua nguoi dat gia
     * @param bidAmount so tien dat
     */
    public BidUpdateEvent(String auctionId, String bidderId, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    /** @return id auction */
    public String getAuctionId() { return auctionId; }

    /** @return id nguoi dat gia */
    public String getBidderId() { return bidderId; }

    /** @return so tien dat */
    public double getBidAmount() { return bidAmount; }

    /** @return thoi gian dat */
    public LocalDateTime getTimestamp() { return timestamp; }

    /** Event type cho client phan biet. */
    public String getEventType() { return "BID_UPDATE"; }

    @Override
    public String toString() {
        return "BidUpdateEvent{auctionId='" + auctionId + "', bidderId='"
                + bidderId + "', bidAmount=" + bidAmount + '}';
    }
}