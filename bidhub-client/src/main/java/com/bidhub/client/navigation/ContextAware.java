package com.bidhub.client.navigation;


import java.util.Map;
/**
 * Cho phép ViewRouter inject dữ liệu vào Controller sau khi FXMLLoader tạo instance.
 *
 * <p>Controller nào cần nhận params (ví dụ {@code auctionId}) thì implement interface này.
 */
public interface ContextAware {
    /**
     * Nhận dữ liệu context từ màn hình trước.
     *
     * @param params map key-value do màn hình gọi {@code navigateTo} truyền vào
     */
    void setContext(Map<String, Object> params);
}
