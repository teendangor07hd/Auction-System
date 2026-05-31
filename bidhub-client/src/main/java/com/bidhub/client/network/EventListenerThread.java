package com.bidhub.client.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Background thread đọc event realtime từ server — Observer Pattern (client side).
 *
 * <p>Vòng lặp đọc tung đóng JSON từ socket input stream, phân loại event dua tren
 * truong {@code eventType}, dispatch đến {@link BidUpdateCallback}.
 *
 */
public class EventListenerThread implements Runnable {

    private final BufferedReader reader;
    private final BidUpdateCallback callback;
    private final ObjectMapper mapper;
    private volatile boolean stopRequested;

    /**
     * Tạo EventListenerThread.
     *
     * @param reader   BufferedReader từ socket input stream
     * @param callback callback xử lý event
     */
    public EventListenerThread(BufferedReader reader, BidUpdateCallback callback) {
        this.reader = reader;
        this.callback = callback;
        this.mapper = new ObjectMapper();
        this.stopRequested = false;
    }

    @Override
    public void run() {
        try {
            while (!stopRequested) {
                String line = reader.readLine();
                if (line == null) {
                    System.out.println("[EventListenerThread] Server dong ket noi.");
                    break;
                }

                try {
                    JsonNode json = mapper.readTree(line);
                    String eventType = json.path("eventType").asText("");

                    if (!eventType.isEmpty()) {
                        callback.onBidUpdate(line);
                    }
                    // Response thuong (status: OK/ERROR) — bo qua
                } catch (Exception e) {
                    System.err.println("[EventListenerThread] Parse event loi: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (!stopRequested) {
                System.err.println("[EventListenerThread] Loi doc socket: " + e.getMessage());
            }
        }
        System.out.println("[EventListenerThread] Da dung.");
    }

    /**
     * Yeu cau đúng thread — goi khi navigate roi khoi AuctionDetail.
     */
    public void stop() {
        this.stopRequested = true;
    }

    /** Kiem tra thread đang chạy không. */
    public boolean isRunning() {
        return !stopRequested;
    }
}