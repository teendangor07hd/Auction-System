package com.bidhub.client.navigation;

/**
 * Lifecycle hook cho Controller — gọi khi ViewRouter navigate rời khỏi màn hình.
 *
 * <p>Implement interface này để dọn dẹp tài nguyên (socket, thread, timeline)
 * trước khi Controller bị thay thế bởi Controller mới.
 *
 * <p>// 📌 [Tieu chi: Resource Management — B1 fix ViewRouter leak]
 */
public interface Navigable {
    /**
     * Được gọi tự động bởi {@link ViewRouter} trước khi chuyển sang màn hình mới.
     * Controller phải dừng tất cả timeline, thread, socket tại đây.
     */
    void onNavigateAway();
}
