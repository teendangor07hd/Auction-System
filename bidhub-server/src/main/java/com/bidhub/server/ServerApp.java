package com.bidhub.server;

import com.bidhub.server.config.ConfigLoader;
import com.bidhub.server.config.MigrationRunner;
import com.bidhub.server.network.SocketServerCore;

import java.io.IOException;

/**
 * Entry point của BidHub Server.
 *
 * <p>Tuần 1: Chỉ in thông tin khởi động, kiểm tra ConfigLoader hoạt động.
 * Socket server sẽ được implement ở Tuần 4.
 */
public class ServerApp {

    public static final String APP_NAME = "BidHub Server";
    public static final String VERSION = "1.0-SNAPSHOT";

    /**
     * Trả về welcome message — dùng trong test để không cần chạy main().
     *
     * @return chuỗi thông báo khởi động
     */
    public static String getWelcomeMessage() {
        return APP_NAME + " v" + VERSION + " — Hệ thống đấu giá trực tuyến";
    }

    /**
     * Entry point chính. Đọc port từ config và in ra.
     *
     * @param args tham số dòng lệnh (không dùng ở tuần 1)
     */
    public static void main(String[] args)  throws IOException {
        MigrationRunner.run();
        System.out.println(getWelcomeMessage());
        int port = ConfigLoader.getInt("server.port");
        System.out.println("Cổng lắng nghe: " + port);
        System.out.println("Database: " + ConfigLoader.getString("db.path"));
        System.out.println("Server sẵn sàng. Socket server sẽ implement tuần 4.");
        SocketServerCore server = new SocketServerCore();
        server.start(port); // dùng biến port đã lấy ở trên để nhất quán
    }
}