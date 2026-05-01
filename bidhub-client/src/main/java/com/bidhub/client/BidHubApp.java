package com.bidhub.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Entry point của BidHub Client — JavaFX Application.
 *
 * <p>Chịu trách nhiệm:
 * <ul>
 *   <li>Khởi tạo JavaFX Application lifecycle</li>
 *   <li>Load màn hình đầu tiên (LoginView)</li>
 *   <li>Cài đặt kích thước và tiêu đề cửa sổ</li>
 * </ul>
 *
 * <p>Networking (kết nối server) sẽ được thêm vào Tuần 4.
 */
public class BidHubApp extends Application {

    /** Kích thước mặc định của cửa sổ */
    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 720;

    /** Tiêu đề hiển thị trên thanh title bar */
    private static final String APP_TITLE = "BidHub — Hệ thống đấu giá trực tuyến";

    /**
     * JavaFX gọi method này khi Application khởi động.
     * Đây là nơi setup Stage và load màn hình đầu tiên.
     *
     * @param primaryStage Stage chính, được JavaFX tạo sẵn
     * @throws IOException nếu không load được file FXML
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load LoginView.fxml từ resources/fxml/
        URL fxmlUrl = getClass().getResource("/fxml/LoginView.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException(
                    "Không tìm thấy /fxml/LoginView.fxml trong resources. "
                            + "Kiểm tra bidhub-client/src/main/resources/fxml/");
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        // Tạo Scene với kích thước mặc định
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Gắn CSS (file có thể trống — sẽ style sau)
        URL cssUrl = getClass().getResource("/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    /**
     * Điểm khởi động của JVM.
     *
     * @param args tham số dòng lệnh (không dùng ở tuần 1)
     */
    public static void main(String[] args) {
        launch(args);
    }
}