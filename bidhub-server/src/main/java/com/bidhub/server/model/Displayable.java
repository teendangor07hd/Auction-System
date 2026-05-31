package com.bidhub.server.model;

/**
 * Interface đánh dấu các đối tượng có thể hiển thị thông tin ra màn hình.
 *
 * <p>Áp dụng cho tất cả {@link Item} subclasses. Đây là ví dụ về
 * <b>Abstraction</b>: client code chỉ cần biết đối tượng "có thể in info",
 * không cần biết cụ thể là Electronics hay Art.
 *
 * <p>Ví dụ:
 * <pre>{@code
 * List<Displayable> items = List.of(new Electronics(...), new Art(...));
 * items.forEach(Displayable::printInfo);
 * }</pre>
 */
public interface Displayable {

    /**
     * In thông tin chỉ tiết của đối tượng ra {@code System.out}.
     *
     * <p>Mỗi class implement theo cách riêng, thể hiện polymorphism qua interface.
     */
    void printInfo();
}