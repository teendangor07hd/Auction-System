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

    /** Màn hình giới thiệu — màn hình đầu tiên khi mở app */
    public static final String HOME = "HomeView";

    /** Màn hình đăng nhập */
    public static final String LOGIN = "LoginView";

    /** Danh sách tất cả phiên đấu giá đang mở */
    public static final String AUCTION_LIST = "AuctionListView";

    /** Chỉ tiết phiên đấu giá + realtime bidding */
    public static final String AUCTION_DETAIL = "AuctionDetailView";

    /** Form tạo item mới — chỉ dành cho Seller */
    public static final String CREATE_ITEM = "CreateItemView";

    /** Form tạo phiên đấu giá — chỉ dành cho Seller */
    public static final String CREATE_AUCTION = "CreateAuctionView";

    /** Màn hình đăng ký tài khoản mới */
    public static final String REGISTER = "RegisterView";

    /** Admin panel — quản lý người dùng (ADMIN only) */
    public static final String ADMIN_VIEW = "AdminView";

    /** Trang thông báo — xem thông báo hệ thống */
    public static final String NOTIFICATION_VIEW = "NotificationView";

    /** Kho sản phẩm — xem tất cả sản phẩm đang/chưa đấu giá */
    public static final String ITEM_CATALOG = "ItemCatalogView";

    /** Trang quản lý của người bán — xem/sửa/xóa sản phẩm và phiên của mình */
    public static final String SELLER_DASHBOARD = "SellerDashboardView";

    /** Trang sản phẩm đã thắng — dành cho Bidder */
    public static final String BIDDER_ITEMS = "BidderItemsView";

    /** Ngăn khởi tạo class này */
    private Views() {}
}