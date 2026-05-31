package com.bidhub.server.service;

import com.bidhub.server.event.AuctionClosedEvent;
import com.bidhub.server.event.BidUpdateEvent;
import com.bidhub.common.network.MessageMapper;
import com.bidhub.server.network.Session;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton Observer Pattern — quản lý subscribe/publish event realtime cho auction.
 *
 * <p>Dùng {@link ConcurrentHashMap} key=auctionId,
 * value={@link CopyOnWriteArrayList} session.
 * CopyOnWriteArrayList cho phép safe iteration khi concurrently modify.
 *
 */
public final class NotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(NotificationBroker.class);

    private static volatile NotificationBroker instance;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Session>> subscribers;

    private NotificationBroker() {
        this.subscribers = new ConcurrentHashMap<>();
    }

    /**
     * Trả về instance duy nhất (thread-safe, double-checked locking).
     *
     * @return NotificationBroker instance
     */
    public static NotificationBroker getInstance() {
        if (instance == null) {
            synchronized (NotificationBroker.class) {
                if (instance == null) {
                    instance = new NotificationBroker();
                }
            }
        }
        return instance;
    }

    /**
     * Thêm session vào danh sách lắng nghe của auction — nhận tất cả event của auction này.
     *
     *
     * @param auctionId id auction cần theo dõi
     * @param session   session của client
     */
    public void subscribe(String auctionId, Session session) {
        if (auctionId == null || session == null) {
            return;
        }
        CopyOnWriteArrayList<Session> list = subscribers.computeIfAbsent(
                auctionId, k -> new CopyOnWriteArrayList<>());
        if (!list.contains(session)) {
            list.add(session);
        }
        logger.debug("Session subscribe auction: {} (total: {})", auctionId, list.size());
    }

    /**
     * Xóa session khỏi danh sách lắng nghe của auction.
     *
     *
     * @param auctionId id auction
     * @param session   session cần xóa
     */
    public void unsubscribe(String auctionId, Session session) {
        if (auctionId == null || session == null) {
            return;
        }
        CopyOnWriteArrayList<Session> list = subscribers.get(auctionId);
        if (list != null) {
            list.remove(session);
            if (list.isEmpty()) {
                subscribers.remove(auctionId, list);
            }
        }
    }

    /**
     * Xóa session khỏi tất cả auction — gọi khi session ngắt kết nối.
     *
     * @param session session cần xóa
     */
    public void unsubscribeAll(Session session) {
        if (session == null) {
            return;
        }
        for (CopyOnWriteArrayList<Session> list : subscribers.values()) {
            list.remove(session);
        }
        logger.debug("UnsubscribeAll session completed.");
    }

    /**
     * Publish event đến tất cả session subscribe auction — Observer notify().
     *
     * <p>Serialize event thành JSON, gửi qua session.sendMessage(). Bắt IOException
     * để không 1 session lỗi block tất cả session khác.
     *
     *
     * @param auctionId id auction
     * @param event     event object (BidUpdateEvent hoặc AuctionClosedEvent)
     */
    public void publish(String auctionId, Object event) {
        if (auctionId == null || event == null) {
            return;
        }
        CopyOnWriteArrayList<Session> list = subscribers.get(auctionId);
        if (list == null || list.isEmpty()) {
            return;
        }

        String eventJson;
        try {
            eventJson = MessageMapper.toJson(event);
        } catch (Exception e) {
            logger.error("Lỗi serialize event thành JSON: {}", e.getMessage(), e);
            return;
        }

        // Thu thập các session lỗi để xóa batch sau vòng lặp, tránh cải biến CopyOnWriteArrayList ngay trong quá trình duyệt
        java.util.List<Session> failedSessions = new java.util.ArrayList<>();
        for (Session session : list) {
            try {
                session.sendMessage(eventJson);
            } catch (Exception e) {
                logger.error("Lỗi gửi event cho session: {}", e.getMessage(), e);
                failedSessions.add(session);
            }
        }
        // Xóa batch session da mat kết nối — chỉ ghi 1 lan vao CopyOnWriteArrayList
        if (!failedSessions.isEmpty()) {
            list.removeAll(failedSessions);
            // Xóa key nếu không con subscriber nao
            if (list.isEmpty()) {
                subscribers.remove(auctionId, list);
            }
        }
    }

    /** Trả về số subscriber hiện tại của auction — chỉ dùng cho mục đích test. */
    public int getSubscriberCount(String auctionId) {
        CopyOnWriteArrayList<Session> list = subscribers.get(auctionId);
        return list != null ? list.size() : 0;
    }

    /** Xóa toàn bộ subscriber — chỉ dùng cho mục đích test. */
    public void clearAll() {
        subscribers.clear();
    }
}