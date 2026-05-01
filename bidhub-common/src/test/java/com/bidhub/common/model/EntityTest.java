package com.bidhub.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    /**
     * Dummy class (Lớp giả mạo): Vì Entity là abstract class, ta không thể
     * instantiate (khởi tạo) nó trực tiếp. Ta cần tạo ra một subclass (lớp con)
     * dùng riêng cho mục đích testing.
     */
    private static class ConcreteEntity extends Entity {
        public ConcreteEntity() {
            super();
        }

        public ConcreteEntity(String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
            super(id, createdAt, updatedAt);
        }

        // Expose (Mở rộng quyền truy cập) hàm protected để test
        public void triggerUpdate() {
            this.markUpdated();
        }
    }

    // Một class khác để test trường hợp so sánh 2 object khác class nhưng cùng ID
    private static class AnotherEntity extends Entity {
        public AnotherEntity(String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
            super(id, createdAt, updatedAt);
        }
    }

    @Test
    @DisplayName("Default constructor should initialize valid UUID and timestamps")
    void defaultConstructor_GeneratesIdAndTimestamps() {
        // Arrange (Chuẩn bị)
        ConcreteEntity entity = new ConcreteEntity();

        // Assert (Xác minh)
        assertNotNull(entity.getId(), "ID must not be null");
        assertDoesNotThrow(() -> UUID.fromString(entity.getId()), "ID should be a valid UUID format");

        assertNotNull(entity.getCreatedAt(), "Creation timestamp is missing");
        assertNotNull(entity.getUpdatedAt(), "Update timestamp is missing");

        // Ban đầu, thời gian tạo và cập nhật phải bằng nhau
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Test
    @DisplayName("Parameterized constructor should map properties correctly")
    void parameterizedConstructor_SetsValuesCorrectly() {
        // Arrange
        String mockId = "custom-id-123";
        LocalDateTime past = LocalDateTime.now().minusDays(5);
        LocalDateTime present = LocalDateTime.now();

        // Act (Thực thi)
        ConcreteEntity entity = new ConcreteEntity(mockId, past, present);

        // Assert
        assertEquals(mockId, entity.getId());
        assertEquals(past, entity.getCreatedAt());
        assertEquals(present, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("Parameterized constructor should throw NullPointerException for null arguments")
    void parameterizedConstructor_ThrowsExceptionWhenNull() {
        LocalDateTime time = LocalDateTime.now();

        // Verify exception handling (Kiểm tra xử lý ngoại lệ)
        assertThrows(NullPointerException.class, () -> new ConcreteEntity(null, time, time));
        assertThrows(NullPointerException.class, () -> new ConcreteEntity("id", null, time));
        assertThrows(NullPointerException.class, () -> new ConcreteEntity("id", time, null));
    }

    @Test
    @DisplayName("markUpdated() should change the updatedAt timestamp")
    void markUpdated_ChangesUpdatedAt() throws InterruptedException {
        // Arrange
        ConcreteEntity entity = new ConcreteEntity();
        LocalDateTime originalUpdatedAt = entity.getUpdatedAt();

        // Act
        // Thread.sleep để đảm bảo thời gian hệ thống trôi qua ít nhất vài mili-giây
        Thread.sleep(10);
        entity.triggerUpdate();

        // Assert
        assertTrue(entity.getUpdatedAt().isAfter(originalUpdatedAt),
                "The new updatedAt timestamp should be after the original one");
        assertEquals(entity.getCreatedAt(), originalUpdatedAt,
                "CreatedAt should remain unchanged");
    }

    @Test
    @DisplayName("equals() and hashCode() should evaluate equality based on ID")
    void equalsAndHashCode_BehaveCorrectly() {
        String sharedId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        ConcreteEntity entity1 = new ConcreteEntity(sharedId, now, now);
        ConcreteEntity entity2 = new ConcreteEntity(sharedId, now.minusDays(1), now.minusDays(1));
        ConcreteEntity entityDiffId = new ConcreteEntity("different", now, now);
        AnotherEntity diffClassEntity = new AnotherEntity(sharedId, now, now);

        // 1. Reflexive (Tính phản xạ): x.equals(x) phải trả về true
        assertEquals(entity1, entity1);

        // 2. Symmetric (Tính đối xứng): x.equals(y) == true thì y.equals(x) == true
        // Dù timestamps khác nhau, nhưng cùng ID thì vẫn là 1 entity
        assertEquals(entity1, entity2);
        assertEquals(entity2, entity1);

        // hashCode của 2 object bằng nhau phải giống nhau
        assertEquals(entity1.hashCode(), entity2.hashCode());

        // 3. Khác ID -> Không bằng nhau
        assertNotEquals(entity1, entityDiffId);

        // 4. Cùng ID, khác Class kế thừa -> VẪN BẰNG NHAU (Theo đúng thiết kế của Entity: "bất kể class")
        // FIXED BUG HERE: Changed assertNotEquals to assertEquals
        assertEquals(entity1, diffClassEntity, "Entities with the same ID should be equal regardless of their subclass");

        // 5. So sánh với null
        assertNotEquals(entity1, null);
    }

    @Test
    @DisplayName("toString() should return class name and first 7 characters of ID")
    void toString_FormatsCorrectly() {
        String longId = "abcdefgh-1234-5678";
        ConcreteEntity entity = new ConcreteEntity(longId, LocalDateTime.now(), LocalDateTime.now());

        String result = entity.toString();

        // Expected output (Kết quả mong đợi)
        assertEquals("ConcreteEntity[abcdefg]", result);
    }
}