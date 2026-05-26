package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử cây kế thừa User: Bidder, Seller, Admin.
 *
 * <p>Các test này trực tiếp chứng minh 4 trụ cột OOP:
 * <ul>
 *   <li>Encapsulation: field private, getter/setter</li>
 *   <li>Inheritance: Bidder is-a User is-a Entity</li>
 *   <li>Polymorphism: getInfo() khác nhau mỗi subclass</li>
 *   <li>Abstraction: không thể new User() trực tiếp</li>
 * </ul>
 */
@DisplayName("UserHierarchy — Kiểm thử cây kế thừa User")
class UserHierarchyTest {

    // =========================================================================
    // Test Entity base (qua Bidder vì Entity là abstract)
    // =========================================================================

    @Nested
    @DisplayName("Entity — ID và timestamps")
    class EntityTests {

        @Test
        @DisplayName("Hai entity mới tạo phải có ID khác nhau (UUID không trùng)")
        void testEntity_TwoNewInstances_HaveDifferentIds() {
            // Arrange + Act
            Bidder b1 = new Bidder("user1", "hash1", "u1@mail.com");
            Bidder b2 = new Bidder("user2", "hash2", "u2@mail.com");

            // Assert
            assertNotNull(b1.getId(), "ID không được null");
            assertNotNull(b2.getId(), "ID không được null");
            assertNotEquals(b1.getId(), b2.getId(), "Hai entity mới phải có UUID khác nhau");
        }

        @Test
        @DisplayName("Entity có ID thì equals/hashCode nhất quán theo id")
        void testEntity_SameId_EqualsByContract() {
            // Arrange
            Bidder b1 = new Bidder("alice", "hash", "a@mail.com");

            // Tạo Bidder với cùng id (load từ DB)
            Bidder b2 = new Bidder(
                    b1.getId(), b1.getCreatedAt(), b1.getUpdatedAt(),
                    "alice", "hash", "a@mail.com", 0, false);

            // Assert — cùng id → equals và hashCode phải khớp
            assertEquals(b1, b2, "Entity cùng id phải equals()");
            assertEquals(b1.hashCode(), b2.hashCode(), "Entity cùng id phải cùng hashCode()");
        }

        @Test
        @DisplayName("createdAt không null sau khi tạo entity mới")
        void testEntity_CreatedAt_NotNullOnCreation() {
            // Arrange + Act
            Seller seller = new Seller("bob", "hash", "bob@mail.com");

            // Assert
            assertNotNull(seller.getCreatedAt(), "createdAt không được null");
            assertNotNull(seller.getUpdatedAt(), "updatedAt không được null");
        }
    }

    // =========================================================================
    // Test Inheritance: Bidder is-a User is-a Entity
    // =========================================================================

    @Nested
    @DisplayName("Inheritance — Bidder is-a User is-a Entity")
    class InheritanceTests {

        @Test
        @DisplayName("Bidder instanceof User và instanceof Entity (kiểm tra kế thừa)")
        void testBidder_IsInstanceOf_UserAndEntity() {
            // Arrange + Act
            Bidder bidder = new Bidder("charlie", "hash", "c@mail.com");

            // Assert
            assertInstanceOf(User.class, bidder, "Bidder phải là User");
            assertInstanceOf(Entity.class, bidder, "Bidder phải là Entity");
        }

        @Test
        @DisplayName("Bidder.getRole() trả về BIDDER")
        void testBidder_GetRole_ReturnsBidder() {
            // Arrange + Act
            Bidder bidder = new Bidder("dave", "hash", "d@mail.com");

            // Assert
            assertEquals(UserRole.BIDDER, bidder.getRole());
        }

        @Test
        @DisplayName("Seller.getRole() trả về SELLER")
        void testSeller_GetRole_ReturnsSeller() {
            Seller seller = new Seller("eve", "hash", "e@mail.com");
            assertEquals(UserRole.SELLER, seller.getRole());
        }

        @Test
        @DisplayName("Admin.getRole() trả về ADMIN")
        void testAdmin_GetRole_ReturnsAdmin() {
            Admin admin = new Admin("frank", "hash", "f@mail.com", 1);
            assertEquals(UserRole.ADMIN, admin.getRole());
        }
    }

    // =========================================================================
    // Test Polymorphism: getInfo() khác nhau mỗi subclass
    // =========================================================================

    @Nested
    @DisplayName("Polymorphism — getInfo() hành vi khác nhau")
    class PolymorphismTests {

        @Test
        @DisplayName("List<User> gọi getInfo() → mỗi phần tử trả về nội dung khác nhau")
        void testPolymorphism_GetInfo_DifferentOutputPerSubclass() {
            // Arrange
            List<User> users = List.of(
                    new Bidder("alice", "h", "a@x.com"),
                    new Seller("bob", "h", "b@x.com"),
                    new Admin("carol", "h", "c@x.com", 2));

            // Act
            List<String> infos = users.stream().map(User::getInfo).toList();

            // Assert — mỗi info phải khác nhau
            assertNotEquals(infos.get(0), infos.get(1), "Bidder và Seller info phải khác nhau");
            assertNotEquals(infos.get(1), infos.get(2), "Seller và Admin info phải khác nhau");
            // Kiểm tra nội dung có ý nghĩa
            assertTrue(infos.get(0).contains("Người đặt giá"), "Bidder getInfo phải đề cập vai trò");
            assertTrue(infos.get(1).contains("Người bán"), "Seller getInfo phải đề cập vai trò");
            assertTrue(infos.get(2).contains("Quản trị viên"), "Admin getInfo phải đề cập vai trò");
        }
    }

    // =========================================================================
    // Test Encapsulation: validate input, không thể set trực tiếp
    // =========================================================================

    @Nested
    @DisplayName("Encapsulation & Validation")
    class EncapsulationTests {

        @Test
        @DisplayName("Username null → IllegalArgumentException")
        void testUser_NullUsername_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Bidder(null, "hash", "x@mail.com"),
                    "Username null phải ném IllegalArgumentException");
        }

        @Test
        @DisplayName("Username quá ngắn (< 3 ký tự) → IllegalArgumentException")
        void testUser_ShortUsername_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Bidder("ab", "hash", "x@mail.com"),
                    "Username < 3 ký tự phải ném IllegalArgumentException");
        }

        @Test
        @DisplayName("Admin với adminLevel = 0 → IllegalArgumentException")
        void testAdmin_InvalidLevel_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Admin("admin", "hash", "a@mail.com", 0),
                    "adminLevel = 0 phải ném IllegalArgumentException");
        }

        @Test
        @DisplayName("Admin với adminLevel = 4 → IllegalArgumentException")
        void testAdmin_LevelTooHigh_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Admin("admin", "hash", "a@mail.com", 4),
                    "adminLevel = 4 phải ném IllegalArgumentException");
        }

        @Test
        @DisplayName("Bidder.incrementBidsPlaced() tăng đúng 1 mỗi lần gọi")
        void testBidder_IncrementBids_IncreasesCountByOne() {
            // Arrange
            Bidder bidder = new Bidder("helen", "hash", "h@mail.com");

            // Act
            bidder.incrementBidsPlaced();
            bidder.incrementBidsPlaced();

            // Assert
            assertEquals(2, bidder.getTotalBidsPlaced(), "Sau 2 lần increment phải bằng 2");
        }
    }
}