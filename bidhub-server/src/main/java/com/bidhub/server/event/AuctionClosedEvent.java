package com.bidhub.server.event;

import java.time.LocalDateTime;

/**
 * Event thong bao auction da dong — gui realtime den tat ca client subscribe.
 *
 * <p>// 📌 [Tieu chi: Realtime update — push AUCTION_CLOSED event]
 */
public final class AuctionClosedEvent {

    private final String auctionId;
    private final String winnerId;
    private final double winningBid;
    private final LocalDateTime timestamp;

    /**
     * Tao AuctionClosedEvent.
     *
     * @param auctionId  id auction da dong
     * @param winnerId   id nguoi thang (null neu khong co)
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

    /** @return id nguoi thang (null neu khong co bid) */
    public String getWinnerId() { return winnerId; }

    /** @return gia thang */
    public double getWinningBid() { return winningBid; }

    /** @return thoi gian dong */
    public LocalDateTime getTimestamp() { return timestamp; }

    /** Event type cho client phan biet. */
    public String getEventType() { return "AUCTION_CLOSED"; }

    @Override
    public String toString() {
        return "AuctionClosedEvent{auctionId='" + auctionId
                + "', winnerId='" + winnerId + "', winningBid=" + winningBid + '}';
    }
}