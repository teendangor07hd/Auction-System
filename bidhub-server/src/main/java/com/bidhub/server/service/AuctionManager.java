package com.bidhub.server.service;

import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton quản lý auction trong RAM — lưu trữ và lifecycle từ đóng.
 *
 * <p>Đúng {@link ConcurrentHashMap} để thread-safe khi nhieu handler (place bid)
 * và lifecycle task truy cap đóng thoi. {@link ScheduledExecutorService} chay
 * {@link AuctionLifecycleTask} moi 5 giay để kiem tra và đóng các phien het han.
 *
 *     ScheduledExecutorService + ConcurrentHashMap]
 *     volatile + double-checked locking]
 */
public final class AuctionManager {

    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);

    private static volatile AuctionManager instance;

    private final ConcurrentHashMap<String, Auction> auctions;

    private final ScheduledExecutorService scheduler;

    private AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auction-lifecycle");
            t.setDaemon(true); // daemon thread — không block JVM shutdown
            return t;
        });
    }

    /**
     * Trả về instance duy nhat (thread-safe, double-checked locking).
     *
     * @return AuctionManager instance
     */
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    private volatile boolean started = false;

    /**
     * Khởi động AuctionManager — load tat ca OPEN và RUNNING auction từ DB vao RAM,
     * schedule {@link AuctionLifecycleTask} chay moi 5 giay.
     *
     */
    public void start() {
        // Chong goi start() nhieu lan → duplicate scheduled tasks
        if (started) {
            logger.warn("AuctionManager.start() da duoc goi truoc do — bo qua.");
            return;
        }
        started = true;

        // Load tat ca OPEN + RUNNING auction từ DB
        AuctionDao auctionDao = new AuctionDao();
        List<Auction> activeAuctions = auctionDao.findActiveAuctions();
        for (Auction auction : activeAuctions) {
            auctions.put(auction.getId(), auction);
        }
        logger.info("Da load {} RUNNING auctions vao RAM.", activeAuctions.size());

        //    ngay khi khởi động, tranh gap 5s để auction expire trong khoang trong]
        AuctionLifecycleTask task = new AuctionLifecycleTask();
        scheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.SECONDS);
        logger.info("Lifecycle task scheduled (5s interval, chay ngay).");
    }

    /**
     * Đúng scheduler — goi khi server shutdown.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Da dung lifecycle scheduler.");
    }

    /**
     * Thêm auction vao RAM cache.
     *
     * @param auction auction cần thêm
     */
    public void addAuction(Auction auction) {
        if (auction != null && auction.getId() != null) {
            auctions.put(auction.getId(), auction);
        }
    }

    /**
     * Xóa auction khoi RAM cache (sau khi đóng phien).
     *
     * @param auctionId id cua auction
     */
    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }

    /**
     * Lấy auction từ RAM cache.
     *
     * @param auctionId id cua auction
     * @return Optional chua auction nếu ton tai trong RAM
     */
    public Optional<Auction> getAuction(String auctionId) {
        return Optional.ofNullable(auctions.get(auctionId));
    }

    /**
     * Trả về danh sach tat ca auction dang active — tạo copy để tranh
     * ConcurrentModificationException khi iterate.
     *
     *
     * @return danh sach auction đang hoạt động
     */
    public List<Auction> getAllActive() {
        return new ArrayList<>(auctions.values());
    }

    /**
     * Trả về so luong auction dang quản lý trong RAM — chỉ đúng cho test.
     *
     * @return so luong auction
     */
    public int activeCount() {
        return auctions.size();
    }

    /** Xóa toàn bộ auction từ RAM — chỉ đúng cho test. */
    public void clearAll() {
        auctions.clear();
    }
}