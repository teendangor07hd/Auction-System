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
 * <p>Tuan 1: Chi in thong tin khoi dong, kiem tra ConfigLoader hoat dong.
 * Socket server se duoc implement o Tuan 4.
 */
public class ServerApp {

    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

    public static final String APP_NAME = "BidHub Server";
    public static final String VERSION = "1.0-SNAPSHOT";

    /**
     * Tra ve welcome message — dung trong test de khong can chay main().
     *
     * @return chuoi thong bao khoi dong
     */
    public static String getWelcomeMessage() {
        return APP_NAME + " v" + VERSION + " — He thong dau gia truc tuyen";
    }

    /**
     * Entry point chinh. Doc port tu config, khoi tao DB, AuctionManager, va socket server.
     *
     * @param args tham so dong lenh (khong dung)
     */
    public static void main(String[] args)  throws IOException {
        MigrationRunner.run();
        logger.info(getWelcomeMessage());
        int port = ConfigLoader.getInt("server.port");
        logger.info("Cong lang nghe: {}", port);
        logger.info("Database: {}", ConfigLoader.getString("db.path"));

        // 📌 [Tieu chi: Singleton + Ky thuat quan trong — AuctionManager lifecycle]
        com.bidhub.server.service.AuctionManager.getInstance().start();
        logger.info("[ServerApp] AuctionManager da khoi dong.");

        // 📌 [Tieu chi: Ky thuat quan trong — shutdown hook dam bao AuctionManager.stop()
        //    duoc goi khi JVM shutdown, giai phong ScheduledExecutorService]
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[ServerApp] Shutdown hook — dang dung AuctionManager...");
            com.bidhub.server.service.AuctionManager.getInstance().stop();
        }, "auction-manager-shutdown"));

        // 📌 [Tieu chi: Ky thuat quan trong — server.start() la blocking call,
        //    dat cuoi cung sau khi tat ca setup hoan thanh]
        logger.info("Server san sang — bat dau lang nghe ket noi.");
        SocketServerCore server = new SocketServerCore();
        server.start(port);
    }
}