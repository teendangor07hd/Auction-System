package com.bidhub.server.event;

import java.time.LocalDateTime;

/**
 * Event thông báo bid moi — gửi realtime đến tat ca client subscribe auction.
 *
 */
public final class BidUpdateEvent {

    private final String auctionId;
    private final String bidderId;
    private final String bidderName;
    private final double bidAmount;
    private final LocalDateTime timestamp;

    /**
     * Tạo BidUpdateEvent.
     *
     * @param auctionId id cua auction
     * @param bidderId  id cua nguoi đặt giá
     * @param bidderName ten cua nguoi đặt giá
     * @param bidAmount so tien dat
     */
    public BidUpdateEvent(String auctionId, String bidderId, String bidderName, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    /** @return id auction */
    public String getAuctionId() { return auctionId; }

    /** @return id nguoi đặt giá */
    public String getBidderId() { return bidderId; }

    /** @return ten nguoi đặt giá */
    public String getBidderName() { return bidderName; }

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