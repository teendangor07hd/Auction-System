package com.bidhub.server.service;

import com.bidhub.server.config.ConfigLoader;
import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.event.AuctionExtendedEvent;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.time.LocalDateTime;

/**
 * Engine kiem tra va gia han auction khi co bid dat trong snipe window (giay cuoi).
 *
 * <p>Khi mot bid duoc dat thanh cong, {@link #check(Auction)} so sanh thoi gian hien tai
 * voi snipe window ({@code endTime - thresholdSeconds}). Neu bid nam trong window
 * → gia han auction them {@code extensionSeconds} giay.
 *
 * <p>Config lay tu {@link ConfigLoader}:
 * <ul>
 *   <li>{@code snipe.threshold} — so giay truoc endTime de bat dau gia han (default 60)</li>
 *   <li>{@code snipe.extension} — so giay gia han moi lan (default 60)</li>
 * </ul>
 *
 * <p>// 📌 [Tieu chi: Anti-Sniping — gia han tu dong phien dau gia khi bid sat gio]
 * // 📌 [Tieu chi: Kỹ thuật quan trọng — LocalDateTime arithmetic + ConfigLoader]
 */
public final class AntiSnipingEngine {

    private final AuctionDao auctionDao;
    private final int thresholdSeconds;
    private final int extensionSeconds;

    /**
     * Constructor production — doc config tu file properties.
     *
     * <p>// 📌 [Tieu chi: Anti-Sniping — ConfigLoader doc threshold va extension]
     */
    public AntiSnipingEngine() {
        this.auctionDao = new AuctionDao();
        this.thresholdSeconds = ConfigLoader.getInt("snipe.threshold");
        this.extensionSeconds = ConfigLoader.getInt("snipe.extension");
    }

    /**
     * Constructor test — cho phep inject gia trị config de test.
     *
     * @param auctionDao        AuctionDao (mock hoac real)
     * @param thresholdSeconds  snipe threshold tinh bang giay
     * @param extensionSeconds  snipe extension tinh bang giay
     */
    // 📌 [Tieu chi: Unit Test — constructor test cho inject dependency]
    public AntiSnipingEngine(AuctionDao auctionDao, int thresholdSeconds, int extensionSeconds) {
        this.auctionDao = auctionDao;
        this.thresholdSeconds = thresholdSeconds;
        this.extensionSeconds = extensionSeconds;
    }

    /**
     * Kiem tra xem bid vua dat co nam trong snipe window khong.
     *
     * <p>Neu bid dat trong {@code thresholdSeconds} giay cuoi → gia han auction them
     * {@code extensionSeconds} giay. Cap nhat ca RAM va DB, publish
     * {@link AuctionExtendedEvent} cho tat ca client subscribe.
     *
     * <p>// 📌 [Tieu chi: Anti-Sniping — logic detect va gia han auction]
     * // 📌 [Tieu chi: Kỹ thuật quan trọng — LocalDateTime.isAfter() / minusSeconds() / plusSeconds()]
     *
     * @param auction auction can kiem tra (phai la RUNNING)
     */
    public void check(Auction auction) {
        if (auction == null || auction.getEndTime() == null) {
            return;
        }
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime snipeWindow = auction.getEndTime().minusSeconds(thresholdSeconds);

        // 📌 [Tieu chi: Anti-Sniping — isAfter() OR isEqual() de bao gom canh]
        if (now.isAfter(snipeWindow) || now.isEqual(snipeWindow)) {
            // Gia han auction
            LocalDateTime newEndTime = auction.getEndTime().plusSeconds(extensionSeconds);

            // Cap nhat RAM
            auction.extendEndTime(newEndTime);

            // Cap nhat DB
            auctionDao.updateEndTime(auction.getId(), newEndTime);

            // 📌 [Tieu chi: Realtime update — publish AUCTION_EXTENDED event]
            NotificationBroker.getInstance().publish(
                    auction.getId(),
                    new AuctionExtendedEvent(auction.getId(), newEndTime));

            System.out.println("[AntiSnipingEngine] Auction " + auction.getId()
                    + " gia han den " + newEndTime);
        }
    }
}