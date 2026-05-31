package com.bidhub.client;

/**
 * Lớp khởi động (entry point) thay thế cho môi trường không hỗ trợ JavaFX trực tiếp.
 *
 * <p>Trong một số trường hợp (ví dụ: chạy từ JAR thuần, không có JavaFX trên module-path),
 * JVM yêu cầu class chứa {@code main} không được kế thừa {@link javafx.application.Application}.
 * {@code Launcher} đóng vai trò trung gian, ủy quyền việc khởi động sang {@link BidHubApp}.
 */
public class Launcher {
    public static void main(String[] args) {
        // Gọi thẳng đến hàm main của class JavaFX chính để tránh hạn chế module của JVM
        BidHubApp.main(args);
    }
}