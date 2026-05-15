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
     * Entry point chinh. Doc port tu config va in ra.
     *
     * @param args tham so dong lenh (khong dung o tuan 1)
     */
    public static void main(String[] args)  throws IOException {
        MigrationRunner.run();
        logger.info(getWelcomeMessage());
        int port = ConfigLoader.getInt("server.port");
        logger.info("Cong lang nghe: {}", port);
        logger.info("Database: {}", ConfigLoader.getString("db.path"));
        logger.info("Server san sang. Socket server se implement tuan 4.");
        SocketServerCore server = new SocketServerCore();

        // 📌 [Tieu chi: Singleton + Ky thuat quan trong — AuctionManager lifecycle]
        com.bidhub.server.service.AuctionManager.getInstance().start();
        logger.info("[ServerApp] AuctionManager da khoi dong.");

        server.start(port); // dung bien port da lay o tren de nhat quan
    }
}