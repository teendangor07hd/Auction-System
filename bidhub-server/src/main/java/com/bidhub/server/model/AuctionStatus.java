package com.bidhub.server.model;
/**
 * Trạng thái của phiên đấu giá — tạo thành một State Machine.
 *
 * <p>Sơ đồ chuyển trạng thái (State Machine):
 * <pre>
 *   OPEN ──────────→ RUNNING ──────────→ FINISHED
 *                                         │       │
 *                                         ↓       ↓
 *                                        PAID  CANCELED
 * </pre>
 *
 * <p>Mỗi trạng thái có hành vi riêng thông qua abstract method —
 * đây là ví dụ nâng cao về <b>enum với abstract method</b> trong Java.
 *
 * <p>Quy tắc chuyển trạng thái:
 * <ul>
 *   <li>OPEN → RUNNING: phiên đấu giá bắt đầu</li>
 *   <li>RUNNING → FINISHED: hết thời gian hoặc admin đóng</li>
 *   <li>FINISHED → PAID: người thắng đã thanh toán</li>
 *   <li>FINISHED → CANCELED: hủy sau khi kết thúc</li>
 * </ul>
 */


public enum AuctionStatus {
    /**
     * Phiên đã tạo, chờ bắt đầu
     *
     * <p>Chưa ai đặt giá được, status này chỉ để khởi tạo
     */
    OPEN {
        @Override
        public boolean canBid(){
            return false;
        }

        @Override
        public boolean isTerminal() {
            return false;
        }
    },
    /**
     * Đang diễn ra — người dùng có thể đặt giá.
     */
    RUNNING {
        @Override
        public boolean canBid() {
            return true; // CHỈ RUNNING mới cho phép đặt giá
        }

        @Override
        public boolean isTerminal() {
            return false;
        }
    },

    /**
     * Đã kết thúc — chờ thanh toán hoặc hủy.
     */
    FINISHED {
        @Override
        public boolean canBid() {
            return false;
        }

        @Override
        public boolean isTerminal() {
            return true;
        }
    },

    /**
     * Đã thanh toán — phiên hoàn tất.
     */
    PAID {
        @Override
        public boolean canBid() {
            return false;
        }

        @Override
        public boolean isTerminal() {
            return true;
        }
    },

    /**
     * Đã hủy — không có giao dịch nào được thực hiện.
     */
    CANCELED {
        @Override
        public boolean canBid() {
            return false;
        }

        @Override
        public boolean isTerminal() {
            return true;
        }
    };

    /**
     * Kiểm tra xem trạng thái này có cho phép đặt giá không.
     *
     * <p>Chỉ {@link #RUNNING} trả về {@code true}.
     * Được gọi trong {@link Auction#isValidBid(double)} trước khi chấp nhận bid.
     *
     * @return {@code true} nếu có thể đặt giá
     */
    public abstract boolean canBid();

    /**
     * Kiểm tra xem trạng thái này là trạng thái cuối cùng (không thể đổi tiếp).
     *
     * <p>{@link #FINISHED}, {@link #PAID}, {@link #CANCELED} trả về {@code true}.
     *
     * @return {@code true} nếu là terminal state
     */
    public abstract boolean isTerminal();

    /**
     * Kiểm tra xem có thể chuyển sang {@code targetStatus} không.
     *
     * <p>Quy tắc:
     * <ul>
     *   <li>OPEN → RUNNING ✅</li>
     *   <li>RUNNING → FINISHED ✅</li>
     *   <li>FINISHED → PAID ✅</li>
     *   <li>FINISHED → CANCELED ✅</li>
     *   <li>Mọi chuyển đổi khác ❌</li>
     * </ul>
     *
     * @param targetStatus trạng thái muốn chuyển tới
     * @return {@code true} nếu chuyển đổi hợp lệ
     */
    public boolean canTransitionTo(AuctionStatus targetStatus) {
        return switch (this) {
            case OPEN -> targetStatus == RUNNING;
            case RUNNING -> targetStatus == FINISHED;
            case FINISHED -> targetStatus == PAID || targetStatus == CANCELED;
            case PAID, CANCELED -> false; // terminal — không thể chuyển tiếp
        };
    }
}
