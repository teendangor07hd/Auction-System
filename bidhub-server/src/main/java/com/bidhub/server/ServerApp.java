package com.bidhub.server;

import com.bidhub.server.config.ConfigLoader;
import com.bidhub.server.config.MigrationRunner;
import com.bidhub.server.network.SocketServerCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Entry point cua BidHub Server.
 *
 * <p>Tuan 1: Chỉ in thông tin khởi động, kiem tra ConfigLoader hoat đóng.
 * Socket server se được implement o Tuan 4.
 */
public class ServerApp {

    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

    public static final String APP_NAME = "BidHub Server";
    public static final String VERSION = "1.0-SNAPSHOT";

    /**
     * Trả về welcome message — đúng trong test để không cần chay main().
     *
     * @return chuoi thông báo khởi động
     */
    public static String getWelcomeMessage() {
        return APP_NAME + " v" + VERSION + " — He thong dau gia truc tuyen";
    }

    /**
     * Entry point chinh. Đọc port từ config, khởi tạo DB, AuctionManager, và socket server.
     *
     * @param args tham so đóng lenh (không đúng)
     */
    public static void main(String[] args)  throws IOException {
        MigrationRunner.run();
        logger.info(getWelcomeMessage());
        int port = ConfigLoader.getInt("server.port");
        logger.info("Cong lang nghe: {}", port);
        logger.info("Database: {}", ConfigLoader.getString("db.path"));

        com.bidhub.server.service.AuctionManager.getInstance().start();
        logger.info("[ServerApp] AuctionManager da khoi dong.");

        //    được goi khi JVM shutdown, giải phóng ScheduledExecutorService]
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[ServerApp] Shutdown hook — dang dung AuctionManager...");
            com.bidhub.server.service.AuctionManager.getInstance().stop();
        }, "auction-manager-shutdown"));

        //    dat cuối cung sau khi tat ca setup hoan thanh]
        logger.info("Server san sang — bat dau lang nghe ket noi.");
        SocketServerCore server = new SocketServerCore();
        server.start(port);
    }
}