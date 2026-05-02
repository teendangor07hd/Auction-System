package com.bidhub.client;

import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class BidHubApp extends Application {

    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 720;
    private static final String APP_TITLE = "BidHub — Hệ thống đấu giá trực tuyến";

    @Override
    public void start(Stage primaryStage) throws IOException {
        // 1. Khởi tạo ViewRouter (Bộ định tuyến giao diện)
        ViewRouter.getInstance().initialize(primaryStage);

        // 2. Tải giao diện đầu tiên (LoginView)
        URL fxmlUrl = getClass().getResource("/fxml/LoginView.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("Không tìm thấy /fxml/LoginView.fxml trong resources.");
        }
        Parent root = FXMLLoader.load(fxmlUrl);
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Thêm CSS nếu có
        URL cssUrl = getClass().getResource("/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // 3. Cấu hình và hiển thị cửa sổ
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();

        // 4. Kết nối tới Server ở chế độ nền (Background)
        connectToServer();
    }

    private void connectToServer() {
        NetworkTask<Void> connectTask = new NetworkTask<>(() -> {
            ServerGateway gw = ServerGateway.getInstance();
            gw.connect(gw.getServerHost(), gw.getServerPort());
            return null;
        });

        // Nếu lỗi, hiển thị thông báo rồi đóng app
        connectTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Không kết nối được Server tại "
                            + ServerGateway.getInstance().getServerHost() + ":"
                            + ServerGateway.getInstance().getServerPort()
                            + "\nKiểm tra server đang chạy rồi thử lại.");
            alert.showAndWait();
            Platform.exit(); // Thoát ứng dụng
        });

        // Khởi chạy tác vụ trên một Thread riêng
        new Thread(connectTask).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}