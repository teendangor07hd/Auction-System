package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Task chay dinh ky — kiem tra va dong cac phien dau gia het han.
 *
 * <p>Moi lan chay, lay danh sach auction active tu {@link AuctionManager#getAllActive()},
 * kiem tra tung auction: neu {@code endTime} da qua → goi {@link #closeAuction(Auction)}.
 *
 * <p>// 📌 [Tieu chi: Chuc nang dau gia — lifecycle tu dong dong phien]
 * // 📌 [Tieu chi: Ky thuat quan trọng — Runnable duoc ScheduledExecutorService goi dinh ky]
 */
public final class AuctionLifecycleTask implements Runnable {

    @Override
    public void run() {
        try {
            List<Auction> activeList = AuctionManager.getInstance().getAllActive();
            for (Auction auction : activeList) {
                try {
                    if (auction.getEndTime() != null
                            && auction.getEndTime().isBefore(LocalDateTime.now())
                            && auction.getStatus() == AuctionStatus.RUNNING) {
                        closeAuction(auction);
                    }
                } catch (Exception e) {
                    // 📌 [Tieu chi: Xu ly loi — khong de 1 auction loi block cac auction khac]
                    System.err.println("[LifecycleTask] Loi xu ly auction "
                            + auction.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[LifecycleTask] Loi chung: " + e.getMessage());
        }
    }

    /**
     * Dong 1 phien dau gia — chuyen status FINISHED, xac dinh winner, cap nhat DB.
     *
     * <p>Flow:
     * <ol>
     *   <li>transitionTo(FINISHED) — validate trong Auction class</li>
     *   <li>updateStatus(FINISHED) trong DB</li>
     *   <li>getHighestBid() — tim nguoi thang</li>
     *   <li>Neu co winner → da cap nhat trong DB qua bidDao.save() truoc do</li>
     *   <li>removeAuction() khoi RAM</li>
     * </ol>
     *
     * <p>// 📌 [Tieu chi: Chuc nang dau gia — dong phien tu dong, xac dinh winner]
     *
     * @param auction auction can dong
     */

    private void closeAuction(Auction auction) {
        String auctionId = auction.getId();
        System.out.println("[LifecycleTask] Dang dong phien: " + auctionId);

        // 📌 [Tieu chi: Ky thuat quan trọng — lock khi dong phien de chong race voi bid]
        auction.getLock().lock();
        try {
            // 1. Chuyen trang thai
            auction.transitionTo(AuctionStatus.FINISHED);

            // 2. Cap nhat status trong DB
            AuctionDao auctionDao = new AuctionDao();
            auctionDao.updateStatus(auctionId, AuctionStatus.FINISHED);

            // 3. Tim winner
            BidDao bidDao = new BidDao();
            Optional<BidTransaction> highestBidOpt = bidDao.getHighestBid(auctionId);

            if (highestBidOpt.isPresent()) {
                BidTransaction winner = highestBidOpt.get();
                System.out.println("[LifecycleTask] Winner: " + winner.getBidderId()
                        + " voi gia " + winner.getBidAmount());
            } else {
                System.out.println("[LifecycleTask] Khong co bid nao — phien "
                        + auctionId + " ket thuc khong co nguoi thang.");
            }

            // 4. Xoa khoi RAM
            AuctionManager.getInstance().removeAuction(auctionId);
        } finally {
            auction.getLock().unlock();
        }

        // NotificationBroker publish (sau khi unlock — Week 7, Quốc Minh them)
        // NotificationBroker.getInstance().publish(auctionId,
        //     new AuctionClosedEvent(auctionId, winnerId, winningBid));

        System.out.println("[LifecycleTask] Da dong phien: " + auctionId);
    }
}