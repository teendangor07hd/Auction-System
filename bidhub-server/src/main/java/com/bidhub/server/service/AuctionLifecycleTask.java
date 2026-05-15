package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.bidhub.server.event.AuctionClosedEvent;
import com.bidhub.server.service.NotificationBroker;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task chay dinh ky — kiem tra va dong cac phien dau gia het han.
 *
 * <p>Moi lan chay, lay danh sach auction active tu {@link AuctionManager#getAllActive()},
 * kiem tra tung auction: neu {@code endTime} da qua → goi {@link #closeAuction(Auction)}.
 *
 * <p>// 📌 [Tieu chi: Chuc nang dau gia — lifecycle tu dong dong phien]
 * // 📌 [Tieu chi: Ky thuat quan trong — Runnable duoc ScheduledExecutorService goi dinh ky]
 */
public final class AuctionLifecycleTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AuctionLifecycleTask.class);

    @Override
    public void run() {
        try {
            List<Auction> activeList = AuctionManager.getInstance().getAllActive();
            for (Auction auction : activeList) {
                try {
                    LocalDateTime now = LocalDateTime.now();

                    // 📌 [Tieu chi: Chuc nang dau gia — tu dong chuyen OPEN → RUNNING khi den startTime]
                    if (auction.getStatus() == AuctionStatus.OPEN
                            && auction.getStartTime() != null
                            && !auction.getStartTime().isAfter(now)) {
                        activateAuction(auction);
                    }

                    // 📌 [Tieu chi: Chuc nang dau gia — tu dong dong phien RUNNING khi het han]
                    if (auction.getEndTime() != null
                            && auction.getEndTime().isBefore(now)
                            && auction.getStatus() == AuctionStatus.RUNNING) {
                        closeAuction(auction);
                    }
                } catch (Exception e) {
                    // 📌 [Tieu chi: Xu ly loi — khong de 1 auction loi block cac auction khac]
                    logger.error("Loi xu ly auction {}: {}", auction.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Loi chung AuctionLifecycleTask: {}", e.getMessage(), e);
        }
    }

    /**
     * Kich hoat phien dau gia — chuyen status OPEN → RUNNING khi den startTime.
     *
     * <p>// 📌 [Tieu chi: Chuc nang dau gia — tu dong kich hoat phien khi den gio]
     *
     * @param auction auction can kich hoat
     */
    private void activateAuction(Auction auction) {
        String auctionId = auction.getId();
        System.out.println("[LifecycleTask] Kich hoat phien: " + auctionId);

        auction.getLock().lock();
        try {
            // Chuyen trang thai OPEN → RUNNING
            auction.transitionTo(AuctionStatus.RUNNING);

            // Cap nhat status trong DB
            AuctionDao auctionDao = new AuctionDao();
            auctionDao.updateStatus(auctionId, AuctionStatus.RUNNING);

            System.out.println("[LifecycleTask] Da kich hoat phien: " + auctionId);
        } finally {
            auction.getLock().unlock();
        }
    }

    private void closeAuction(Auction auction) {
        String auctionId = auction.getId();
        logger.info("Dang dong phien: {}", auctionId);

        // 📌 [Tieu chi: Ky thuat quan trong — lock khi dong phien de chong race voi bid]
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

            String winnerId = null;
            double winningBid = 0.0;
            if (highestBidOpt.isPresent()) {
                BidTransaction winner = highestBidOpt.get();
                winnerId = winner.getBidderId();
                winningBid = winner.getBidAmount();
                logger.info("Winner: {} voi gia {}.", winnerId, winningBid);
            } else {
                logger.info("Khong co bid nao — phien {} ket thuc khong co nguoi thang.", auctionId);
            }

            // 4. Xoa khoi RAM
            AuctionManager.getInstance().removeAuction(auctionId);

            // 📌 [Tieu chi: Audit Log — log AUCTION_CLOSED sau FINISHED transition]
            // Log trong lock → dam bao khong race voi bid handler
            AuditLogService auditLogService = new AuditLogService();
            auditLogService.log("SYSTEM", AuditActions.AUCTION_CLOSED,
                    "{\"auctionId\":\"" + auctionId
                            + "\",\"winnerId\":\"" + (winnerId != null ? winnerId : "none")
                            + "\",\"winningBid\":" + winningBid + "}");
        } finally {
            auction.getLock().unlock();
        }

        // Lay winner info de publish event
        BidDao bidDao = new BidDao();
        Optional<BidTransaction> highestBidOpt = bidDao.getHighestBid(auctionId);
        String winnerId = null;
        double winningBid = 0.0;
        if (highestBidOpt.isPresent()) {
            winnerId = highestBidOpt.get().getBidderId();
            winningBid = highestBidOpt.get().getBidAmount();
        }

        // 📌 [Tieu chi: Realtime update — publish AUCTION_CLOSED sau unlock]
        NotificationBroker.getInstance().publish(auctionId,
                new AuctionClosedEvent(auctionId, winnerId, winningBid));

        // NotificationBroker publish (sau khi unlock — Week 7, Quoc Minh them)
        // NotificationBroker.getInstance().publish(auctionId,
        //     new AuctionClosedEvent(auctionId, winnerId, winningBid));

        logger.info("Da dong phien: {}", auctionId);
    }
}