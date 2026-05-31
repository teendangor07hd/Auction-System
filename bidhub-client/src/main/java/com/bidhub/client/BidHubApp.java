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

/**
 * Điểm khởi động chính của ứng dụng BidHub phía client.
 *
 * <p>Class này kế thừa {@link Application} của JavaFX và chịu trách nhiệm:
 * <ul>
 *   <li>Khởi tạo {@link ViewRouter} và nạp màn hình chính (HomeView).</li>
 *   <li>Áp dụng stylesheet CSS toàn cục cho ứng dụng.</li>
 *   <li>Kết nối đến {@link ServerGateway} trước khi cho phép người dùng thao tác.</li>
 * </ul>
 */
public class BidHubApp extends Application {

    private static final int WINDOW_WIDTH = 1024;
    private static final int WINDOW_HEIGHT = 720;
    private static final String APP_TITLE = "BidHub — Hệ thống đấu giá trực tuyến";

    /**
     * Khởi tạo và hiển thị cửa sổ chính của ứng dụng.
     *
     * <p>Quy trình thực hiện:
     * <ol>
     *   <li>Khởi tạo {@link ViewRouter} với {@code primaryStage}.</li>
     *   <li>Nạp giao diện từ {@code /fxml/HomeView.fxml}.</li>
     *   <li>Vô hiệu hóa toàn bộ giao diện (UI Guard) cho đến khi kết nối Server thành công.</li>
     *   <li>Áp dụng stylesheet CSS nếu tìm thấy {@code /css/styles.css}.</li>
     *   <li>Gọi {@link #connectToServer(Parent)} để thiết lập kết nối bất đồng bộ.</li>
     * </ol>
     *
     * @param primaryStage Stage chính do JavaFX runtime cung cấp.
     * @throws IOException           nếu không thể nạp file FXML.
     * @throws IllegalStateException nếu {@code /fxml/HomeView.fxml} không tồn tại trong resources.
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        ViewRouter.getInstance().initialize(primaryStage);

        URL fxmlUrl = getClass().getResource("/fxml/HomeView.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("Không tìm thấy /fxml/HomeView.fxml trong resources.");
        }

        Parent root = FXMLLoader.load(fxmlUrl);

        // Vô hiệu hóa toàn bộ giao diện (UI Guard) ngay khi khởi động,
        // tránh người dùng tương tác trước khi kết nối Server hoàn tất.
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

    /**
     * Kết nối bất đồng bộ đến Server và cập nhật trạng thái giao diện theo kết quả.
     *
     * <p>Tác vụ kết nối chạy trên một Thread riêng thông qua {@link NetworkTask}.
     * Khi kết nối thành công, UI Guard được gỡ bỏ để người dùng có thể thao tác.
     * Khi kết nối thất bại, hiển thị thông báo lỗi và thoát ứng dụng.
     *
     * @param root Node gốc của Scene; được dùng để bật/tắt UI Guard sau khi có kết quả kết nối.
     */
    private void connectToServer(Parent root) {
        NetworkTask<Void> connectTask = new NetworkTask<>(() -> {
            ServerGateway gw = ServerGateway.getInstance();
            gw.connect(gw.getServerHost(), gw.getServerPort());
            return null;
        });

        // Kết nối thành công: gỡ bỏ UI Guard, cho phép người dùng thao tác bình thường.
        connectTask.setOnSucceeded(e -> {
            // Gỡ bỏ UI Guard: Mở khóa giao diện cho người dùng thao tác
            root.setDisable(false);
            System.out.println("[Client] Đã kết nối thành công tới Server!");
        });

        // Kết nối thất bại: hiển thị thông báo lỗi chỉ tiết rồi thoát ứng dụng.
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