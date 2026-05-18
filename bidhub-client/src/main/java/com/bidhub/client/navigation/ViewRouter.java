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
import java.util.Set;

/**
 * Quản lý điều hướng màn hình JavaFX. Singleton đảm bảo chỉ 1 Stage được dùng.
 *
 * <p>Dùng {@link #initialize(Stage)} trong {@code BidHubApp.start()} trước khi gọi
 * bất kỳ {@code navigateTo()} nào.
 *
 * <p>// 📌 [B1] Gọi {@link Navigable#onNavigateAway()} trên controller cũ trước khi navigate
 * để dọn dẹp socket, thread, timeline — tránh resource leak.
 * <p>// 📌 [B2] AUTH_VIEWS là Set hằng số — không hardcode string list nữa.
 */
public final class ViewRouter {

    // Tiêu chí : Singleton Pattern - 1đ
    private static volatile ViewRouter instance;

    private Stage primaryStage;
    private BorderPane mainLayout;

    /** Controller hiện đang hiển thị (có thể null). */
    private Object currentController;

    /**
     * [B2] Tập hợp các view KHÔNG yêu cầu auth — dùng convention-based lookup.
     * Nếu sau này thêm view auth mới thì chỉ cần xóa khỏi set này (mặc định yêu cầu auth).
     */
    private static final Set<String> AUTH_FREE_VIEWS = Set.of(
            "LoginView", "RegisterView"
    );

    private ViewRouter() {}

    /** Trả về instance duy nhất của ViewRouter (thread-safe). */
    public static ViewRouter getInstance() {
        if (instance == null) {
            synchronized (ViewRouter.class) {
                if (instance == null) {
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
    public void initialize(Stage stage) {
        this.primaryStage = Objects.requireNonNull(stage, "Stage không được null");
    }

    /**
     * Chuyển sang màn hình có tên {@code viewName} (không truyền params).
     *
     * @param viewName tên màn hình, khớp với tên file FXML (ví dụ {@code "AuctionListView"})
     */
    public void navigateTo(String viewName) {
        navigateTo(viewName, Collections.emptyMap());
    }

    /**
     * Chuyển màn hình và inject params vào Controller nếu implement {@link ContextAware}.
     *
     * <p>// 📌 [B1] Trước khi load view mới, gọi {@code onNavigateAway()} trên controller cũ
     * nếu nó implement {@link Navigable} — dọn dẹp resource (socket, thread, timeline).
     *
     * @param viewName tên FXML (không có đuôi .fxml)
     * @param params   dữ liệu truyền sang Controller, có thể rỗng
     */
    public void navigateTo(String viewName, Map<String, Object> params) {
        if (primaryStage == null) {
            throw new IllegalStateException("ViewRouter chưa được initialize(stage) — gọi trong BidHubApp");
        }

        // [B1] Gọi cleanup trên controller cũ trước khi navigate
        if (currentController instanceof Navigable navigable) {
            try {
                navigable.onNavigateAway();
            } catch (Exception ex) {
                System.err.println("[ViewRouter] onNavigateAway() lỗi: " + ex.getMessage());
            }
        }

        try {
            boolean isAuthView = viewName.equals(Views.LOGIN) || viewName.equals(Views.REGISTER) || viewName.equals(Views.HOME);

            String fxmlPath = "/fxml/" + viewName + ".fxml";
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource(fxmlPath),
                            "Không tìm thấy FXML: " + fxmlPath));
            Parent root = loader.load();

            // Lưu controller mới làm "current"
            currentController = loader.getController();

            // Inject params nếu controller implement ContextAware
            if (currentController instanceof ContextAware ca && !params.isEmpty()) {
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

    /**
     * [B2] Kiểm tra view có phải view không yêu cầu xác thực không.
     *
     * @param viewName tên view
     * @return true nếu view không yêu cầu auth
     */
    public boolean isAuthFreeView(String viewName) {
        return AUTH_FREE_VIEWS.contains(viewName);
    }
}
