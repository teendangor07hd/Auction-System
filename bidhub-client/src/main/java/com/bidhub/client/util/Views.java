package com.bidhub.client.util;

/**
 * Hằng số tên các màn hình (FXML views) trong ứng dụng.
 *
 * <p>Sử dụng constants thay vì hardcode string để tránh typo
 * khi gọi {@code ViewRouter.navigateTo()}.
 *
 * <p>Quy ước: Tên constant = tên file FXML (không có đuôi .fxml).
 * File FXML đặt trong {@code resources/fxml/}.
 */
public final class Views {

    /** Màn hình đăng nhập — màn hình đầu tiên khi mở app */
    public static final String LOGIN = "LoginView";

    /** Danh sách tất cả phiên đấu giá đang mở */
    public static final String AUCTION_LIST = "AuctionListView";

    /** Chi tiết phiên đấu giá + realtime bidding */
    public static final String AUCTION_DETAIL = "AuctionDetailView";

    /** Form tạo item mới — chỉ dành cho Seller */
    public static final String CREATE_ITEM = "CreateItemView";

    /** Form tạo phiên đấu giá — chỉ dành cho Seller */
    public static final String CREATE_AUCTION = "CreateAuctionView";

    /** Màn hình đăng ký tài khoản mới */
    public static final String REGISTER = "RegisterView";

    /** Ngăn khởi tạo class này */
    private Views() {}
}