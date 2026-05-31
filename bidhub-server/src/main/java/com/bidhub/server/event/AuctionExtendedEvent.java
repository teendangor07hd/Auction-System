package com.bidhub.server.event;

import java.time.LocalDateTime;

/**
 * Event thông báo auction được gia hạn — gửi realtime đến tat ca client subscribe.
 *
 * <p>Khi {@link com.bidhub.server.service.AntiSnipingEngine} detect bid trong snipe window,
 * auction được gia hạn endTime và event này được publish qua NotificationBroker.
 *
 */
public final class AuctionExtendedEvent {

    private final String auctionId;
    private final LocalDateTime newEndTime;

    /**
     * Tạo AuctionExtendedEvent.
     *
     * @param auctionId  id cua auction được gia hạn
     * @param newEndTime thoi gian kết thúc moi sau gia hạn
     */
    public AuctionExtendedEvent(String auctionId, LocalDateTime newEndTime) {
        this.auctionId = auctionId;
        this.newEndTime = newEndTime;
    }

    /** @return id cua auction được gia hạn */
    public String getAuctionId() {
        return auctionId;
    }

    /** @return thoi gian kết thúc moi */
    public LocalDateTime getNewEndTime() {
        return newEndTime;
    }

    /** Event type cho client phan biet auction được gia hạn. */
    public String getEventType() {
        return "AUCTION_EXTENDED";
    }

    @Override
    public String toString() {
        return "AuctionExtendedEvent{auctionId='" + auctionId
                + "', newEndTime=" + newEndTime + '}';
    }
}