package com.bidhub.client.service;

import java.time.LocalDateTime;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite nâng cao cho BidChartService.
 * Bao gồm Happy Path (Luồng lý tưởng) và Edge Cases (Trường hợp ngoại lệ).
 *
 * @author Công Minh
 */
class BidChartServiceTest {

    private BidChartService service;

    @BeforeEach
    void setUp() {
        service = new BidChartService();
    }

    // ==========================================
    // 1. HAPPY PATH TESTS (Các test cơ bản)
    // ==========================================

    @Test
    @DisplayName("Constructor khởi tạo series với tên 'Lịch sử giá'")
    void constructor_seriesNameCorrect() {
        XYChart.Series<String, Number> series = service.getSeries();
        assertEquals("Lịch sử giá", series.getName(),
                "Series name phai la 'Lịch sử giá'");
    }

    @Test
    @DisplayName("addDataPoint thêm đúng 1 data point vào series và tăng count")
    void addDataPoint_incrementsCount() {
        service.addDataPoint(LocalDateTime.now(), 1000.0);
        assertEquals(1, service.getDataPointCount());

        service.addDataPoint(LocalDateTime.now().plusSeconds(5), 1200.0);
        assertEquals(2, service.getDataPointCount());
    }

    @Test
    @DisplayName("addDataPoint format time thành HH:mm:ss string chính xác")
    void addDataPoint_formatsTimeCorrectly() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 15, 14, 30, 45);
        service.addDataPoint(time, 500.0);

        XYChart.Data<String, Number> data = service.getSeries().getData().get(0);
        assertEquals("14:30:45", data.getXValue(),
                "Time format phải chuẩn HH:mm:ss");
        assertEquals(500.0, data.getYValue().doubleValue(), 0.001,
                "Price phải match chính xác");
    }

    @Test
    @DisplayName("clearData xóa sạch toàn bộ data point hiện có")
    void clearData_removesAllPoints() {
        service.addDataPoint(LocalDateTime.now(), 1000.0);
        service.addDataPoint(LocalDateTime.now().plusSeconds(1), 1100.0);
        assertEquals(2, service.getDataPointCount());

        service.clearData();
        assertEquals(0, service.getDataPointCount(),
                "Data phải trống sau khi gọi clearData");
    }

    @Test
    @DisplayName("getSeries luôn trả về cùng một instance (Singleton behavior for the list)")
    void getSeries_returnsSameInstance() {
        assertSame(service.getSeries(), service.getSeries(),
                "Phải trả về cùng một tham chiếu bộ nhớ (reference)");
    }

    // ==========================================
    // 2. EDGE CASES & ROBUSTNESS TESTS (Test ngoại lệ & Tính bền bỉ)
    // ==========================================

    @Test
    @DisplayName("addDataPoint ném ra NullPointerException khi time là null")
    void addDataPoint_nullTime_throwsException() {
        // Dùng assertThrows để đảm bảo chương trình ném ra đúng loại lỗi
        assertThrows(NullPointerException.class, () -> {
            service.addDataPoint(null, 1500.0);
        }, "Phải ném ra NullPointerException nếu time bị null");
    }

    @Test
    @DisplayName("addDataPoint xử lý tốt các giá trị price ở mức biên (Boundary values)")
    void addDataPoint_boundaryPrices_handledCorrectly() {
        LocalDateTime time = LocalDateTime.now();

        // Test với giá trị 0 (Zero value) - Giả sử đấu giá bắt đầu từ 0
        service.addDataPoint(time, 0.0);
        assertEquals(0.0, service.getSeries().getData().get(0).getYValue().doubleValue(), 0.001);

        // Test với giá trị cực lớn (Large value) - Tránh lỗi overflow
        service.addDataPoint(time.plusSeconds(1), Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, service.getSeries().getData().get(1).getYValue().doubleValue(), 0.001);
    }
}