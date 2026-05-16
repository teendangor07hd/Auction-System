package com.bidhub.client.navigation;

import com.bidhub.client.util.Views;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Quản lý điều hướng màn hình JavaFX. Singleton đảm bảo chỉ 1 Stage được dùng.
 *
 * <p>Dùng {@link #initialize(Stage)} trong {@code BidHubApp.start()} trước khi gọi
 * bất kỳ {@code navigateTo()} nào.
 */

public final class ViewRouter {
    // Tiêu chí : Singleton Pattern - 1đ
    private static volatile ViewRouter instance;

    private Stage primaryStage;
    private BorderPane mainLayout;

    private ViewRouter() {}

    /** Trả về instance duy nhất của ViewRouter (thread-safe). */
    public static ViewRouter getInstance(){
        if (instance == null){
            synchronized (ViewRouter.class){
                if (instance == null){
                    instance = new ViewRouter();
                }
            }

        }
        return instance;
    }

    /**
     * Gán Stage chính. Phải gọi 1 lần duy nhất trong {@code BidHubApp.start()}.
     *
     * @param stage Stage chính của ứng dụng
     */
    public void initialize(Stage stage){
        this.primaryStage = Objects.requireNonNull(stage,"Stage không được null");
    }

    /**
     * Chuyển sang màn hình có tên {@code viewName} (không truyền params).
     *
     * @param viewName tên màn hình, khớp với tên file FXML (ví dụ {@code "AuctionListView"})
     */
    public void navigateTo(String viewName){
        navigateTo(viewName, Collections.emptyMap());
    }


    /**
     * Chuyển màn hình và inject params vào Controller nếu implement {@link ContextAware}.
     *
     * @param viewName tên FXML (không có đuôi .fxml)
     * @param params   dữ liệu truyền sang Controller, có thể rỗng
     */
    public void navigateTo(String viewName, Map<String, Object> params) {
        if (primaryStage == null) {
            throw new IllegalStateException("ViewRouter chưa được initialize(stage) — gọi trong BidHubApp");
        }
        try {
            boolean isAuthView = viewName.equals(Views.LOGIN) || viewName.equals(Views.REGISTER);

            String fxmlPath = "/fxml/" + viewName + ".fxml";
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource(fxmlPath),
                            "Không tìm thấy FXML: " + fxmlPath));
            Parent root = loader.load();

            // Inject params nếu controller implement ContextAware
            Object controller = loader.getController();
            if (controller instanceof ContextAware ca && !params.isEmpty()) {
                ca.setContext(params);
            }

            Scene currentScene = primaryStage.getScene();
            if (currentScene == null) {
                currentScene = new Scene(new Region(), 1024, 720);
                primaryStage.setScene(currentScene);
            }

            if (isAuthView) {
                currentScene.setRoot(root);
                mainLayout = null;
            } else {
                if (mainLayout == null) {
                    FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/fxml/MainLayout.fxml"));
                    mainLayout = mainLoader.load();
                    currentScene.setRoot(mainLayout);
                }
                mainLayout.setCenter(root);
            }
            primaryStage.show();

        } catch (IOException e) {
            throw new RuntimeException("ViewRouter không load được " + viewName + ": " + e.getMessage(), e);
        }
    }

}
