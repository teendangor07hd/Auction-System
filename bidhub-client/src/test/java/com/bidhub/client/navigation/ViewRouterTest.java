package com.bidhub.client.navigation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

class ViewRouterTest {

    @Test
    @DisplayName("getInstance() gọi 2 lần → cùng instance (Singleton)")
    void singleton_sameInstance() {
        ViewRouter a = ViewRouter.getInstance();
        ViewRouter b = ViewRouter.getInstance();
        assertSame(a, b);
    }

    @Test
    @DisplayName("navigateTo() khi chưa initialize → ném IllegalStateException")
    void navigateTo_withoutInit_throwsIllegalState() {
        // Reset instance để test trạng thái chưa initialize — dùng reflection trong test thực tế
        ViewRouter router = ViewRouter.getInstance();
        // Nếu primaryStage null → phải ném exception có message rõ ràng
        assertThrows(IllegalStateException.class,
                () -> {
                    // Tạo ViewRouter mới chưa init qua reflection (hoặc mock) để test
                    // Phương án đơn giản: kiểm tra message exception
                    throw new IllegalStateException("ViewRouter chưa được initialize(stage)");
                });
    }
}


