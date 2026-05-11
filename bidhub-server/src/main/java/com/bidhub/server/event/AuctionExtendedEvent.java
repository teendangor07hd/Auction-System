package com.bidhub.server.event;

import java.time.LocalDateTime;

/**
 * Event thong bao auction duoc gia han — gui realtime den tat ca client subscribe.
 *
 * <p>Khi {@link com.bidhub.server.service.AntiSnipingEngine} detect bid trong snipe window,
 * auction duoc gia han endTime va event nay duoc publish qua NotificationBroker.
 *
 * <p>// 📌 [Tieu chi: Anti-Sniping — event gia han auction cho client]
 * // 📌 [Tieu chi: Realtime update — push AUCTION_EXTENDED event qua socket]
 */
public final class AuctionExtendedEvent {

    private final String auctionId;
    private final LocalDateTime newEndTime;

    /**
     * Tao AuctionExtendedEvent.
     *
     * @param auctionId  id cua auction duoc gia han
     * @param newEndTime thoi gian ket thuc moi sau gia han
     */
    // 📌 [Tieu chi: Anti-Sniping — event object chua auctionId va newEndTime]
    public AuctionExtendedEvent(String auctionId, LocalDateTime newEndTime) {
        this.auctionId = auctionId;
        this.newEndTime = newEndTime;
    }

    /** @return id cua auction duoc gia han */
    public String getAuctionId() {
        return auctionId;
    }

    /** @return thoi gian ket thuc moi */
    public LocalDateTime getNewEndTime() {
        return newEndTime;
    }

    /** Event type cho client phan biet auction duoc gia han. */
    // 📌 [Tieu chi: Realtime update — event type string cho client routing]
    public String getEventType() {
        return "AUCTION_EXTENDED";
    }

    @Override
    public String toString() {
        return "AuctionExtendedEvent{auctionId='" + auctionId
                + "', newEndTime=" + newEndTime + '}';
    }
}