package com.bidhub.server.utils;

/**
 * Lớp tính toán số học cơ bản.
 *
 * <p>Lớp này được tạo ra để kiểm tra JUnit 5 hoạt động đúng trong project
 * và xây dựng template testing convention cho cả nhóm.
 *
 * <p>Các operation: cộng, trừ, nhân, chia, giai thừa, kiểm tra số nguyên tố.
 */
public class Calculator {

    /**
     * Tính tổng hai số nguyên.
     *
     * @param a số hạng thứ nhất
     * @param b số hạng thứ hai
     * @return tổng a + b
     */
    public int add(int a, int b) {
        return a + b;
    }

    /**
     * Tính hiệu hai số nguyên.
     *
     * @param a số bị trừ
     * @param b số trừ
     * @return hiệu a - b
     */
    public int subtract(int a, int b) {
        return a - b;
    }

    /**
     * Tính tích hai số nguyên.
     *
     * @param a thừa số thứ nhất
     * @param b thừa số thứ hai
     * @return tích a × b
     */
    public int multiply(int a, int b) {
        return a * b;
    }

    /**
     * Tính thương hai số nguyên (chia nguyên).
     *
     * @param a số bị chia
     * @param b số chia
     * @return thương a / b
     * @throws ArithmeticException nếu b == 0 (chia cho 0)
     */
    public int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Không thể chia cho 0");
        }
        return a / b;
    }

    /**
     * Tính giai thừa của số nguyên không âm.
     *
     * <p>Quy ước: 0! = 1 (giai thừa của 0 bằng 1 theo định nghĩa toán học)
     *
     * @param n số nguyên cần tính giai thừa (n ≥ 0)
     * @return n!
     * @throws IllegalArgumentException nếu n < 0
     */
    public long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Không tính được giai thừa của số âm: " + n);
        }
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Kiểm tra số nguyên tố.
     *
     * <p>Số nguyên tố là số tự nhiên lớn hơn 1, chỉ chia hết cho 1 và chính nó.
     *
     * @param n số cần kiểm tra
     * @return {@code true} nếu n là số nguyên tố
     * @throws IllegalArgumentException nếu n ≤ 1 (không xác định theo định nghĩa)
     */
    public boolean isPrime(int n) {
        if (n <= 1) {
            throw new IllegalArgumentException(
                    "Số nguyên tố được định nghĩa với n > 1. Nhận được: " + n);
        }
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; (long) i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }
}