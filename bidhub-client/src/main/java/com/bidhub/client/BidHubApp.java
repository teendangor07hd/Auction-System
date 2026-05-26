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
        ViewRouter.getInstance().initialize(primaryStage);

        URL fxmlUrl = getClass().getResource("/fxml/HomeView.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("Không tìm thấy /fxml/HomeView.fxml trong resources.");
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        // [UI GUARD] - Khóa toàn bộ giao diện ngay từ đầu
        // Người dùng sẽ thấy giao diện hơi mờ đi và không thể click được
        root.setDisable(true);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        URL cssUrl = getClass().getResource("/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();

        // Truyền root vào hàm connect để có thể mở khóa sau khi kết nối xong
        connectToServer(root);
    }

    private void connectToServer(Parent root) {
        NetworkTask<Void> connectTask = new NetworkTask<>(() -> {
            ServerGateway gw = ServerGateway.getInstance();
            gw.connect(gw.getServerHost(), gw.getServerPort());
            return null;
        });

        // [THÀNH CÔNG] - Server đã kết nối
        connectTask.setOnSucceeded(e -> {
            // Gỡ bỏ UI Guard: Mở khóa giao diện cho người dùng thao tác
            root.setDisable(false);
            System.out.println("[Client] Đã kết nối thành công tới Server!");
        });

        // [THẤT BẠI] - Không kết nối được
        connectTask.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Không kết nối được Server tại "
                            + ServerGateway.getInstance().getServerHost() + ":"
                            + ServerGateway.getInstance().getServerPort()
                            + "\nKiểm tra server đang chạy rồi thử lại.");
            alert.showAndWait();
            Platform.exit();
        });

        new Thread(connectTask).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}