package com.bidhub.client.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Background thread doc event realtime tu server — Observer Pattern (client side).
 *
 * <p>Vong lap doc tung dong JSON tu socket input stream, phan loai event dua tren
 * truong {@code eventType}, dispatch den {@link BidUpdateCallback}.
 *
 * <p>// 📌 [Tieu chi: Realtime update — client nhan event qua socket]
 * // 📌 [Tieu chi: MVC — Observer pattern tren client]
 */
public class EventListenerThread implements Runnable {

    private final BufferedReader reader;
    private final BidUpdateCallback callback;
    private final ObjectMapper mapper;
    private volatile boolean stopRequested;

    /**
     * Tao EventListenerThread.
     *
     * @param reader   BufferedReader tu socket input stream
     * @param callback callback xu ly event
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
     * Yeu cau dung thread — goi khi navigate roi khoi AuctionDetail.
     */
    public void stop() {
        this.stopRequested = true;
    }

    /** Kiem tra thread dang chay khong. */
    public boolean isRunning() {
        return !stopRequested;
    }
}