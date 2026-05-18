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
 * Singleton Observer Pattern — quan ly subscribe/publish event realtime cho auction.
 *
 * <p>Dung {@link ConcurrentHashMap} key=auctionId,
 * value={@link CopyOnWriteArrayList} session.
 * CopyOnWriteArrayList cho phep safe iteration khi concurrently modify.
 *
 * <p>// 📌 [Tieu chi: Design Pattern Observer — Subject (GoF)]
 * // 📌 [Tieu chi: Singleton — volatile + double-checked locking]
 * // 📌 [Tieu chi: Realtime update — push event qua socket]
 */
public final class NotificationBroker {

    private static final Logger logger = LoggerFactory.getLogger(NotificationBroker.class);

    private static volatile NotificationBroker instance;

    // 📌 [Tieu chi: Ky thuat quan trong — ConcurrentHashMap + CopyOnWriteArrayList]
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Session>> subscribers;

    private NotificationBroker() {
        this.subscribers = new ConcurrentHashMap<>();
    }

    /**
     * Tra ve instance duy nhat (thread-safe, double-checked locking).
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
     * Subscribe session vao auction — nhan tat ca event cua auction nay.
     *
     * <p>// 📌 [Tieu chi: Observer Pattern — attach()]
     *
     * @param auctionId id auction can theo doi
     * @param session   session cua client
     */
    public void subscribe(String auctionId, Session session) {
        if (auctionId == null || session == null) {
            return;
        }
        // 📌 [Tieu chi: Ky thuat quan trong — computeIfAbsent atomic, tranh TOCTOU race]
        CopyOnWriteArrayList<Session> list = subscribers.computeIfAbsent(
                auctionId, k -> new CopyOnWriteArrayList<>());
        if (!list.contains(session)) {
            list.add(session);
        }
        logger.debug("Session subscribe auction: {} (total: {})", auctionId, list.size());
    }

    /**
     * Unsubscribe session khoi auction.
     *
     * <p>// 📌 [Tieu chi: Observer Pattern — detach()]
     *
     * @param auctionId id auction
     * @param session   session can xoa
     */
    public void unsubscribe(String auctionId, Session session) {
        if (auctionId == null || session == null) {
            return;
        }
        CopyOnWriteArrayList<Session> list = subscribers.get(auctionId);
        if (list != null) {
            list.remove(session);
            // 📌 [Tieu chi: Ky thuat quan trong — xoa key khi list rong de tranh memory leak]
            if (list.isEmpty()) {
                subscribers.remove(auctionId, list);
            }
        }
    }

    /**
     * Unsubscribe session khoi tat ca auction — goi khi session ngat ket noi.
     *
     * @param session session can xoa
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
     * Publish event den tat ca session subscribe auction — Observer notify().
     *
     * <p>Serialize event thanh JSON, gui qua session.sendMessage(). Bat IOException
     * de khong 1 session loi block tat ca session khac.
     *
     * <p>// 📌 [Tieu chi: Observer Pattern — notify()]
     * // 📌 [Tieu chi: Realtime update — push event qua socket]
     *
     * @param auctionId id auction
     * @param event     event object (BidUpdateEvent hoac AuctionClosedEvent)
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
            logger.error("Serialize event loi: {}", e.getMessage(), e);
            return;
        }

        // 📌 [Tieu chi: Ky thuat quan trong — thu thap session loi, xoa sau vong lap
        //    tranh O(n²) khi xoa truc tiep tu CopyOnWriteArrayList trong vong lap]
        java.util.List<Session> failedSessions = new java.util.ArrayList<>();
        for (Session session : list) {
            try {
                session.sendMessage(eventJson);
            } catch (Exception e) {
                // 📌 [Tieu chi: Xu ly loi — 1 session loi khong block cac session khac]
                logger.error("Gui event loi cho session: {}", e.getMessage(), e);
                failedSessions.add(session);
            }
        }
        // Xoa batch session da mat ket noi — chi ghi 1 lan vao CopyOnWriteArrayList
        if (!failedSessions.isEmpty()) {
            list.removeAll(failedSessions);
            // Xoa key neu khong con subscriber nao
            if (list.isEmpty()) {
                subscribers.remove(auctionId, list);
            }
        }
    }

    /** Lay so subscriber cua auction — chi dung cho test. */
    public int getSubscriberCount(String auctionId) {
        CopyOnWriteArrayList<Session> list = subscribers.get(auctionId);
        return list != null ? list.size() : 0;
    }

    /** Xoa toan bo subscriber — chi dung cho test. */
    public void clearAll() {
        subscribers.clear();
    }
}