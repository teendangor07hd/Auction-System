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
 * Task chay dinh ky — kiem tra và đóng các phien đấu giá het han.
 *
 * <p>Moi lan chay, lấy danh sach auction active từ {@link AuctionManager#getAllActive()},
 * kiem tra tung auction: nếu {@code endTime} da qua → goi {@link #closeAuction(Auction)}.
 *
 */
public final class AuctionLifecycleTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AuctionLifecycleTask.class);

    private final AuctionDao auctionDao = new AuctionDao();
    private final BidDao bidDao = new BidDao();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    public void run() {
        try {
            List<Auction> activeList = AuctionManager.getInstance().getAllActive();
            for (Auction auction : activeList) {
                try {
                    LocalDateTime now = LocalDateTime.now();

                    if (auction.getStatus() == AuctionStatus.OPEN
                            && auction.getStartTime() != null
                            && !auction.getStartTime().isAfter(now)) {
                        activateAuction(auction);
                    }

                    if (auction.getEndTime() != null
                            && auction.getEndTime().isBefore(now)
                            && auction.getStatus() == AuctionStatus.RUNNING) {
                        closeAuction(auction);
                    }
                } catch (Exception e) {
                    logger.error("Loi xu ly auction {}: {}", auction.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Loi chung AuctionLifecycleTask: {}", e.getMessage(), e);
        }
    }

    /**
     * Kich hoat phien đấu giá — chuyen status OPEN → RUNNING khi đến startTime.
     *
     *
     * @param auction auction cần kich hoat
     */
    private void activateAuction(Auction auction) {
        String auctionId = auction.getId();
        logger.info("[LifecycleTask] Kich hoat phien: {}", auctionId);

        auction.getLock().lock();
        try {
            // Chuyen trạng thái OPEN → RUNNING
            auction.transitionTo(AuctionStatus.RUNNING);

            // Cập nhật status trong DB
            auctionDao.updateStatus(auctionId, AuctionStatus.RUNNING);

            logger.info("[LifecycleTask] Da kich hoat phien: {}", auctionId);
        } finally {
            auction.getLock().unlock();
        }
    }

    private void closeAuction(Auction auction) {
        String auctionId = auction.getId();
        logger.info("Dang dong phien: {}", auctionId);

        String winnerId = null;
        double winningBid = 0.0;

        auction.getLock().lock();
        try {
            // 1. Chuyen trạng thái
            auction.transitionTo(AuctionStatus.FINISHED);

            // 2. Cập nhật status trong DB
            auctionDao.updateStatus(auctionId, AuctionStatus.FINISHED);

            // 3. Tìm winner
            Optional<BidTransaction> highestBidOpt = bidDao.getHighestBid(auctionId);

            if (highestBidOpt.isPresent()) {
                BidTransaction winner = highestBidOpt.get();
                winnerId = winner.getBidderId();
                winningBid = winner.getBidAmount();
                logger.info("Winner: {} voi gia {}.", winnerId, winningBid);
            } else {
                logger.info("Khong co bid nao — phien {} ket thuc khong co nguoi thang.", auctionId);
            }

            // 4. Xóa khoi RAM
            AuctionManager.getInstance().removeAuction(auctionId);

            // Log trong lock → dam bao không race với bid handler
            auditLogService.log("SYSTEM", AuditActions.AUCTION_CLOSED,
                    "{\"auctionId\":\"" + auctionId
                            + "\",\"winnerId\":\"" + (winnerId != null ? winnerId : "none")
                            + "\",\"winningBid\":" + winningBid + "}");
        } finally {
            auction.getLock().unlock();
        }

        // Goi ngoai khoi lock block để tranh block thread khi gửi socket
        NotificationBroker.getInstance().publish(auctionId,
                new AuctionClosedEvent(auctionId, winnerId, winningBid));

        logger.info("Da dong phien: {}", auctionId);
    }
}