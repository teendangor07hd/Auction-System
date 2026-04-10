package com.bidhub.server.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho {@link Calculator}.
 *
 * <p>Template testing convention của nhóm BidHub:
 * <ul>
 *   <li>Cấu trúc AAA: Arrange → Act → Assert</li>
 *   <li>{@code @DisplayName} tiếng Việt mô tả rõ ràng hành vi</li>
 *   <li>{@code @Nested} nhóm test theo từng method</li>
 *   <li>Test cả happy path (đầu vào bình thường) và edge cases</li>
 * </ul>
 */
@DisplayName("Calculator — Kiểm tra các phép tính cơ bản")
class CalculatorTest {

    private Calculator calc;

    @BeforeEach
    void setUp() {
        // Arrange chung: tạo fresh instance trước mỗi test
        calc = new Calculator();
    }

    // ===================== add() =====================

    @Nested
    @DisplayName("Phép cộng add()")
    class AddTests {

        @Test
        @DisplayName("Cộng hai số dương → kết quả đúng")
        void testAdd_TwoPositives_ReturnsSum() {
            // Arrange
            int a = 5, b = 3;

            // Act
            int result = calc.add(a, b);

            // Assert
            assertEquals(8, result, "5 + 3 phải bằng 8");
        }

        @Test
        @DisplayName("Cộng số dương và số âm → kết quả đúng")
        void testAdd_PositiveAndNegative_ReturnsSum() {
            assertEquals(2, calc.add(5, -3));
        }

        @Test
        @DisplayName("Cộng hai số âm → kết quả âm")
        void testAdd_TwoNegatives_ReturnsNegativeSum() {
            assertEquals(-8, calc.add(-5, -3));
        }

        @Test
        @DisplayName("Cộng với 0 → trả về chính số đó")
        void testAdd_WithZero_ReturnsSameNumber() {
            assertEquals(42, calc.add(42, 0));
            assertEquals(42, calc.add(0, 42));
        }
    }

    // ===================== subtract() =====================

    @Nested
    @DisplayName("Phép trừ subtract()")
    class SubtractTests {

        @Test
        @DisplayName("Trừ bình thường → kết quả đúng")
        void testSubtract_Normal_ReturnsCorrect() {
            assertEquals(2, calc.subtract(5, 3));
        }

        @Test
        @DisplayName("Trừ ra số âm khi a < b")
        void testSubtract_ResultNegative_ReturnsNegative() {
            assertEquals(-3, calc.subtract(2, 5));
        }
    }

    // ===================== divide() =====================

    @Nested
    @DisplayName("Phép chia divide()")
    class DivideTests {

        @Test
        @DisplayName("Chia hai số nguyên chia hết nhau → kết quả đúng")
        void testDivide_ExactDivision_ReturnsQuotient() {
            assertEquals(3, calc.divide(9, 3));
        }

        @Test
        @DisplayName("Chia cho 0 → ném ArithmeticException")
        void testDivide_ByZero_ThrowsArithmeticException() {
            // assertThrows trả về exception object để có thể assert thêm
            ArithmeticException ex = assertThrows(
                    ArithmeticException.class,
                    () -> calc.divide(10, 0),
                    "Chia cho 0 phải ném ArithmeticException"
            );
            assertTrue(ex.getMessage().contains("0"),
                    "Message lỗi phải đề cập đến 0");
        }

        @Test
        @DisplayName("Chia số âm cho số dương → kết quả âm")
        void testDivide_NegativeByPositive_ReturnsNegative() {
            assertEquals(-3, calc.divide(-9, 3));
        }

        @Test
        @DisplayName("Chia 0 cho số bất kỳ (khác 0) → trả về 0")
        void testDivide_ZeroByNonZero_ReturnsZero() {
            assertEquals(0, calc.divide(0, 5));
        }
    }

    // ===================== factorial() =====================

    @Nested
    @DisplayName("Giai thừa factorial()")
    class FactorialTests {

        @Test
        @DisplayName("Giai thừa 0 → phải bằng 1 (theo định nghĩa toán học)")
        void testFactorial_Zero_ReturnsOne() {
            assertEquals(1L, calc.factorial(0));
        }

        @Test
        @DisplayName("Giai thừa 1 → bằng 1")
        void testFactorial_One_ReturnsOne() {
            assertEquals(1L, calc.factorial(1));
        }

        @Test
        @DisplayName("Giai thừa 5 → bằng 120")
        void testFactorial_Five_Returns120() {
            assertEquals(120L, calc.factorial(5));
        }

        @Test
        @DisplayName("Giai thừa số âm → ném IllegalArgumentException")
        void testFactorial_NegativeNumber_ThrowsIllegalArgument() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> calc.factorial(-1),
                    "Số âm phải ném IllegalArgumentException"
            );
            assertNotNull(ex.getMessage(), "Message lỗi không được null");
            assertTrue(ex.getMessage().contains("-1"),
                    "Message phải chứa giá trị nhập vào");
        }
    }

    // ===================== isPrime() =====================

    @Nested
    @DisplayName("Kiểm tra nguyên tố isPrime()")
    class IsPrimeTests {

        @Test
        @DisplayName("isPrime(2) → true (số nguyên tố nhỏ nhất)")
        void testIsPrime_Two_ReturnsTrue() {
            assertTrue(calc.isPrime(2));
        }

        @Test
        @DisplayName("isPrime(7) → true")
        void testIsPrime_Seven_ReturnsTrue() {
            assertTrue(calc.isPrime(7));
        }

        @Test
        @DisplayName("isPrime(4) → false (chia hết cho 2)")
        void testIsPrime_Four_ReturnsFalse() {
            assertFalse(calc.isPrime(4));
        }

        @Test
        @DisplayName("isPrime(1) → ném exception (1 không phải nguyên tố)")
        void testIsPrime_One_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> calc.isPrime(1));
        }

        @Test
        @DisplayName("isPrime(0) và isPrime(-5) → ném exception")
        void testIsPrime_ZeroAndNegative_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> calc.isPrime(0));
            assertThrows(IllegalArgumentException.class, () -> calc.isPrime(-5));
        }

        @Test
        @DisplayName("isPrime(97) → true (số nguyên tố lớn)")
        void testIsPrime_LargePrime_ReturnsTrue() {
            assertTrue(calc.isPrime(97));
        }

        @Test
        @DisplayName("isPrime(100) → false")
        void testIsPrime_Hundred_ReturnsFalse() {
            assertFalse(calc.isPrime(100));
        }
    }
}