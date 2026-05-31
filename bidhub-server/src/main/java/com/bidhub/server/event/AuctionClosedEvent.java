package com.bidhub.server.event;

import java.time.LocalDateTime;

/**
 * Event thông báo auction da đóng — gửi realtime đến tat ca client subscribe.
 *
 */
public final class AuctionClosedEvent {

    private final String auctionId;
    private final String winnerId;
    private final double winningBid;
    private final LocalDateTime timestamp;

    /**
     * Tạo AuctionClosedEvent.
     *
     * @param auctionId  id auction da đóng
     * @param winnerId   id nguoi thang (null nếu không có)
     * @param winningBid gia thang
     */
    public AuctionClosedEvent(String auctionId, String winnerId, double winningBid) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.winningBid = winningBid;
        this.timestamp = LocalDateTime.now();
    }

    /** @return id auction */
    public String getAuctionId() { return auctionId; }

    /** @return id nguoi thang (null nếu không có bid) */
    public String getWinnerId() { return winnerId; }

    /** @return gia thang */
    public double getWinningBid() { return winningBid; }

    /** @return thoi gian đóng */
    public LocalDateTime getTimestamp() { return timestamp; }

    /** Event type cho client phan biet. */
    public String getEventType() { return "AUCTION_CLOSED"; }

    @Override
    public String toString() {
        return "AuctionClosedEvent{auctionId='" + auctionId
                + "', winnerId='" + winnerId + "', winningBid=" + winningBid + '}';
    }
}