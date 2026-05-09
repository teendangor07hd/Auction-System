package com.bidhub.client.network;

/**
 * Callback nhan event realtime tu server (Observer Pattern — client side).
 *
 * <p>Controller implement interface nay de nhan BID_UPDATE, AUCTION_CLOSED event.
 * Callback chay tren background thread — phai dung {@code Platform.runLater()}
 * khi cap nhat JavaFX UI.
 *
 * <p>// 📌 [Tieu chi: Observer Pattern — Observer (client side)]
 */
@FunctionalInterface
public interface BidUpdateCallback {

    /**
     * Xu ly event realtime tu server.
     *
     * <p>Chay tren background thread — dung {@code Platform.runLater()} cho UI update.
     *
     * @param eventJson chuoi JSON chua event (BidUpdateEvent hoac AuctionClosedEvent)
     */
    void onBidUpdate(String eventJson);
}