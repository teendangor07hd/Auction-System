package com.bidhub.client.network;

/**
 * Callback nhan event realtime từ server (Observer Pattern — client side).
 *
 * <p>Controller implement interface này để nhan BID_UPDATE, AUCTION_CLOSED event.
 * Callback chay tren background thread — phai đúng {@code Platform.runLater()}
 * khi cập nhật JavaFX UI.
 *
 */
@FunctionalInterface
public interface BidUpdateCallback {

    /**
     * Xử lý event realtime từ server.
     *
     * <p>Chay tren background thread — đúng {@code Platform.runLater()} cho UI update.
     *
     * @param eventJson chuoi JSON chua event (BidUpdateEvent hoac AuctionClosedEvent)
     */
    void onBidUpdate(String eventJson);
}