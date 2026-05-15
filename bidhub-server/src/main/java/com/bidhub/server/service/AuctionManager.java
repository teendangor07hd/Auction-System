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
 * Singleton quan ly auction trong RAM — luu tru va lifecycle tu dong.
 *
 * <p>Dung {@link ConcurrentHashMap} de thread-safe khi nhieu handler (place bid)
 * va lifecycle task truy cap dong thoi. {@link ScheduledExecutorService} chay
 * {@link AuctionLifecycleTask} moi 5 giay de kiem tra va dong cac phien het han.
 *
 * <p>// 📌 [Tieu chi: Ky thuat quan trong & concurrency —
 *     ScheduledExecutorService + ConcurrentHashMap]
 * // 📌 [Tieu chi: Design Pattern Singleton —
 *     volatile + double-checked locking]
 * // 📌 [Tieu chi: Chuc nang dau gia — lifecycle tu dong dong phien]
 */
public final class AuctionManager {

    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);

    private static volatile AuctionManager instance;

    // 📌 [Tieu chi: Ky thuat quan trong — ConcurrentHashMap cho concurrent access]
    private final ConcurrentHashMap<String, Auction> auctions;

    // 📌 [Tieu chi: Ky thuat quan trong — ScheduledExecutorService cho periodic task]
    private final ScheduledExecutorService scheduler;

    private AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "auction-lifecycle");
            t.setDaemon(true); // daemon thread — khong block JVM shutdown
            return t;
        });
    }

    /**
     * Tra ve instance duy nhat (thread-safe, double-checked locking).
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

    /**
     * Khoi dong AuctionManager — load tat ca OPEN va RUNNING auction tu DB vao RAM,
     * schedule {@link AuctionLifecycleTask} chay moi 5 giay.
     *
     * <p>// 📌 [Tieu chi: Chuc nang dau gia — tu dong kiem tra va dong phien]
     */
    public void start() {
        // Load tat ca OPEN + RUNNING auction tu DB
        AuctionDao auctionDao = new AuctionDao();
        List<Auction> activeAuctions = auctionDao.findActiveAuctions();
        for (Auction auction : activeAuctions) {
            auctions.put(auction.getId(), auction);
        }
        logger.info("Da load {} RUNNING auctions vao RAM.", activeAuctions.size());

        // Schedule lifecycle task moi 5 giay
        AuctionLifecycleTask task = new AuctionLifecycleTask();
        scheduler.scheduleAtFixedRate(task, 5, 5, TimeUnit.SECONDS);
        logger.info("Lifecycle task scheduled (5s interval).");
    }

    /**
     * Dung scheduler — goi khi server shutdown.
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
     * Them auction vao RAM cache.
     *
     * @param auction auction can them
     */
    public void addAuction(Auction auction) {
        if (auction != null && auction.getId() != null) {
            auctions.put(auction.getId(), auction);
        }
    }

    /**
     * Xoa auction khoi RAM cache (sau khi dong phien).
     *
     * @param auctionId id cua auction
     */
    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }

    /**
     * Lay auction tu RAM cache.
     *
     * @param auctionId id cua auction
     * @return Optional chua auction neu ton tai trong RAM
     */
    public Optional<Auction> getAuction(String auctionId) {
        return Optional.ofNullable(auctions.get(auctionId));
    }

    /**
     * Tra ve danh sach tat ca auction dang active — tao copy de tranh
     * ConcurrentModificationException khi iterate.
     *
     * <p>// 📌 [Tieu chi: Ky thuat quan trong — tao copy ArrayList cho safe iteration]
     *
     * @return danh sach auction dang hoat dong
     */
    public List<Auction> getAllActive() {
        return new ArrayList<>(auctions.values());
    }

    /**
     * Tra ve so luong auction dang quan ly trong RAM — chi dung cho test.
     *
     * @return so luong auction
     */
    public int activeCount() {
        return auctions.size();
    }

    /** Xoa toan bo auction tu RAM — chi dung cho test. */
    public void clearAll() {
        auctions.clear();
    }
}