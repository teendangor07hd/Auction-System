package com.bidhub.client.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Background thread doc event realtime tu server — Observer Pattern (client side).
 *
 * <p>Vong lap doc tung dong JSON tu socket input stream, phan loai event dua tren
 * truong {@code eventType}, dispatch den {@link BidUpdateCallback}.
 *
 * <p>// 📌 [Tieu chi: Realtime update — client nhan event qua socket]
 * // 📌 [Tieu chi: MVC — Observer pattern tren client]
 * // 📌 [B10] stop() giờ đóng reader để interrupt blocking readLine() — thread sẽ thoát ngay.
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
                String line;
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    // [B10] Nếu stop() đóng reader, readLine() ném IOException → thoát bình thường
                    if (!stopRequested) {
                        System.err.println("[EventListenerThread] Loi doc socket: " + e.getMessage());
                    }
                    break;
                }

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
                System.err.println("[EventListenerThread] Loi khong mong doi: " + e.getMessage());
            }
        }
        System.out.println("[EventListenerThread] Da dung.");
    }

    /**
     * Yeu cau dung thread.
     *
     * <p>// 📌 [B10] Đóng reader để interrupt blocking {@code readLine()} — thread thoát ngay
     * thay vì block cho đến khi server gửi dòng tiếp theo.
     */
    public void stop() {
        this.stopRequested = true;
        // [B10] Đóng reader để unblock readLine()
        try {
            reader.close();
        } catch (IOException ignored) {
            // Bỏ qua — mục tiêu là interrupt thread đang block
        }
    }

    /** Kiem tra thread dang chay khong. */
    public boolean isRunning() {
        return !stopRequested;
    }
}