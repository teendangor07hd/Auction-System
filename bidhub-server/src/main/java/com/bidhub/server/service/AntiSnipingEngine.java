package com.bidhub.server.service;

import com.bidhub.server.config.ConfigLoader;
import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.event.AuctionExtendedEvent;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.time.LocalDateTime;
import com.bidhub.server.model.AuditActions;
import com.bidhub.server.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Engine kiểm tra và gia hạn phiên đấu giá khi có lượt đặt giá trong snipe window (những giây cuối).
 *
 * <p>Khi một bid được đặt thành công, {@link #check(Auction)} so sánh thời gian hiện tại
 * với snipe window ({@code endTime - thresholdSeconds}). Nếu bid nằm trong window
 * → gia hạn auction thêm {@code extensionSeconds} giây.
 *
 * <p>Config lấy từ {@link ConfigLoader}:
 * <ul>
 *   <li>{@code snipe.threshold} — số giây trước endTime để bắt đầu gia hạn (default 60)</li>
 *   <li>{@code snipe.extension} — số giây gia hạn mỗi lần (default 60)</li>
 * </ul>
 *
 */
public final class AntiSnipingEngine {

    private static final Logger logger = LoggerFactory.getLogger(AntiSnipingEngine.class);

    private final AuctionDao auctionDao;
    private final int thresholdSeconds;
    private final int extensionSeconds;
    private final AuditLogService auditLogService;

    /**
     * Constructor dùng trong môi trường thực tế — đọc cấu hình từ file properties.
     *
     */
    public AntiSnipingEngine() {
        this.auctionDao = new AuctionDao();
        this.auditLogService = new AuditLogService();
        this.thresholdSeconds = ConfigLoader.getIntOrDefault("snipe.threshold", 60);
        this.extensionSeconds = ConfigLoader.getIntOrDefault("snipe.extension", 60);
    }

    /**
     * Constructor dùng cho test — cho phép inject giá trị cấu hình tùy ý.
     *
     * @param auctionDao        AuctionDao (mock hoặc real)
     * @param thresholdSeconds  snipe threshold tính bằng giây
     * @param extensionSeconds  snipe extension tính bằng giây
     */
    public AntiSnipingEngine(AuctionDao auctionDao, int thresholdSeconds, int extensionSeconds) {
        this.auctionDao = auctionDao;
        this.auditLogService = new AuditLogService();
        this.thresholdSeconds = thresholdSeconds;
        this.extensionSeconds = extensionSeconds;
    }

    /**
     * Kiểm tra xem bid vừa đặt có nằm trong snipe window không.
     *
     * <p>Nếu bid được đặt trong {@code thresholdSeconds} giây cuối → gia hạn auction thêm
     * {@code extensionSeconds} giây. Cập nhật cả RAM và DB, publish
     * {@link AuctionExtendedEvent} cho tất cả client subscribe.
     *
     *
     * @param auction auction cần kiểm tra (phải là RUNNING)
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

        if (now.isAfter(snipeWindow) || now.isEqual(snipeWindow)) {
            LocalDateTime oldEndTime = auction.getEndTime();
            LocalDateTime newEndTime = oldEndTime.plusSeconds(extensionSeconds);

            // Cập nhật thời gian kết thúc trên RAM
            auction.extendEndTime(newEndTime);

            // Cập nhật thời gian kết thúc xuống cơ sở dữ liệu
            auctionDao.updateEndTime(auction.getId(), newEndTime);

            NotificationBroker.getInstance().publish(
                    auction.getId(),
                    new AuctionExtendedEvent(auction.getId(), newEndTime));

            // Chạy trong lock block của handlePlaceBid → thread-safe
            auditLogService.log("SYSTEM", AuditActions.AUCTION_EXTENDED,
                    "{\"auctionId\":\"" + auction.getId()
                            + "\",\"oldEndTime\":\"" + oldEndTime.toString()
                            + "\",\"newEndTime\":\"" + newEndTime.toString() + "\"}");

            logger.info("Auction {} gia han den {}.", auction.getId(), newEndTime);
        }
    }
}