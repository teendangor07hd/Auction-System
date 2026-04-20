package com.bidhub.common.exception;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử hệ thống exception của BidHub.
 *
 * <p>Tests này chứng minh:
 * <ul>
 *   <li>Kế thừa đúng: mọi exception là instanceof BidHubException</li>
 *   <li>errorCode đúng cho từng subclass</li>
 *   <li>Polymorphism: catch BidHubException bắt được tất cả</li>
 *   <li>ValidationException chứa đúng errors list</li>
 * </ul>
 */
@DisplayName("ExceptionHierarchy — Kiểm thử BidHubException và 7 subclass")
class ExceptionHierarchyTest {

    // =========================================================================
    // Test Inheritance: mọi exception là BidHubException
    // =========================================================================

    @Nested
    @DisplayName("Inheritance — tất cả là instanceof BidHubException")
    class InheritanceTests {

        @Test
        @DisplayName("InvalidBidException instanceof BidHubException và RuntimeException")
        void testInvalidBid_IsInstanceOfBidHubException() {
            // Arrange + Act
            InvalidBidException ex = new InvalidBidException("Giá quá thấp");

            // Assert
            assertInstanceOf(BidHubException.class, ex);
            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("Tất cả 6 subclass đều instanceof BidHubException")
        void testAllSubclasses_AreInstanceOfBidHubException() {
            // Arrange — tạo 1 instance mỗi loại
            List<BidHubException> exceptions = List.of(
                    new InvalidBidException("test"),
                    new AuctionNotFoundException("auction-1"),
                    new AuctionClosedException("auction-2", "FINISHED"),
                    new UserNotFoundException("user-1"),
                    new DuplicateUsernameException("alice"),
                    new AuthenticationException("Sai mật khẩu"),
                    new ValidationException("field rỗng"));

            // Assert — mỗi exception phải đúng kiểu
            for (BidHubException ex : exceptions) {
                assertInstanceOf(BidHubException.class, ex,
                        ex.getClass().getSimpleName() + " phải là BidHubException");
            }
        }

        @Test
        @DisplayName("Polymorphism: catch BidHubException bắt được InvalidBidException")
        void testPolymorphism_CatchBidHubException_CatchesSubclass() {
            // Arrange
            BidHubException caught = null;

            // Act — throw InvalidBidException, catch BidHubException
            try {
                throw new InvalidBidException("Giá đặt thấp hơn giá hiện tại");
            } catch (BidHubException e) {
                caught = e;
            }

            // Assert
            assertNotNull(caught, "BidHubException phải bắt được InvalidBidException");
            assertInstanceOf(InvalidBidException.class, caught);
        }
    }

    // =========================================================================
    // Test errorCode đúng theo từng subclass
    // =========================================================================

    @Nested
    @DisplayName("ErrorCode — mỗi subclass có errorCode đúng")
    class ErrorCodeTests {

        @Test
        @DisplayName("InvalidBidException có errorCode = BID_INVALID")
        void testInvalidBid_ErrorCode() {
            assertEquals("BID_INVALID", new InvalidBidException("x").getErrorCode());
        }

        @Test
        @DisplayName("AuctionNotFoundException có errorCode = AUCTION_NOT_FOUND")
        void testAuctionNotFound_ErrorCode() {
            assertEquals("AUCTION_NOT_FOUND", new AuctionNotFoundException("id-1").getErrorCode());
        }

        @Test
        @DisplayName("AuctionClosedException có errorCode = AUCTION_CLOSED")
        void testAuctionClosed_ErrorCode() {
            assertEquals("AUCTION_CLOSED",
                    new AuctionClosedException("id-1", "FINISHED").getErrorCode());
        }

        @Test
        @DisplayName("DuplicateUsernameException có errorCode = USERNAME_TAKEN")
        void testDuplicateUsername_ErrorCode() {
            assertEquals("USERNAME_TAKEN", new DuplicateUsernameException("alice").getErrorCode());
        }

        @Test
        @DisplayName("AuthenticationException có errorCode = AUTH_FAILED")
        void testAuthentication_ErrorCode() {
            assertEquals("AUTH_FAILED", new AuthenticationException("Sai mật khẩu").getErrorCode());
        }

        @Test
        @DisplayName("ValidationException có errorCode = VALIDATION_ERROR")
        void testValidation_ErrorCode() {
            assertEquals("VALIDATION_ERROR", new ValidationException("lỗi").getErrorCode());
        }
    }

    // =========================================================================
    // Test ValidationException đặc biệt
    // =========================================================================

    @Nested
    @DisplayName("ValidationException — errors list")
    class ValidationExceptionTests {

        @Test
        @DisplayName("ValidationException(String) → getErrors() trả về list 1 phần tử")
        void testValidation_SingleError_ErrorsListHasOneItem() {
            // Arrange + Act
            ValidationException ex = new ValidationException("Username rỗng");

            // Assert
            assertNotNull(ex.getErrors(), "getErrors() không được null");
            assertFalse(ex.getErrors().isEmpty(), "getErrors() không được rỗng");
            assertEquals(1, ex.getErrorCount());
            assertEquals("Username rỗng", ex.getErrors().get(0));
        }

        @Test
        @DisplayName("ValidationException(List) → getErrors() trả về đúng số lượng")
        void testValidation_MultipleErrors_ErrorsListCorrect() {
            // Arrange
            List<String> errors = List.of(
                    "Username không được rỗng",
                    "Password phải ≥ 6 ký tự",
                    "Email không hợp lệ");

            // Act
            ValidationException ex = new ValidationException(errors);

            // Assert
            assertEquals(3, ex.getErrorCount(), "Phải có đúng 3 lỗi");
            assertTrue(ex.getErrors().contains("Username không được rỗng"));
            assertTrue(ex.getMessage().contains("3 lỗi validation"));
        }

        @Test
        @DisplayName("ValidationException.getErrors() là unmodifiable (không thể thêm lỗi sau)")
        void testValidation_ErrorsList_IsUnmodifiable() {
            // Arrange
            ValidationException ex = new ValidationException(List.of("Lỗi 1", "Lỗi 2"));

            // Act + Assert — cố thêm vào list → phải ném exception
            assertThrows(UnsupportedOperationException.class,
                    () -> ex.getErrors().add("Lỗi hacker thêm vào"),
                    "getErrors() phải là unmodifiable list");
        }

        @Test
        @DisplayName("ValidationException với list rỗng → IllegalArgumentException")
        void testValidation_EmptyList_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ValidationException(List.of()),
                    "List lỗi rỗng phải ném IllegalArgumentException");
        }

        @Test
        @DisplayName("ValidationException với null list → IllegalArgumentException")
        void testValidation_NullList_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ValidationException((List<String>) null),
                    "null list phải ném IllegalArgumentException");
        }
    }
}