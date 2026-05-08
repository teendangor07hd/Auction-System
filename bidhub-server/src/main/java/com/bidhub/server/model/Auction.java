package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.time.LocalDateTime;
import java.util.Objects;
/**
 * Phiên đấu giá trong hệ thống bidhub
 * <p>Kế thừa {@link Entity} để có id và timestamps.
 *Chứa đầy đủ thông tin về một phiên đấu giá: item, giá cả, trạng thái.
 * <p><b>State Machine:</b> Trạng thái chuyển theo sơ đồ {@link AuctionStatus}.
 * Gọi {@link #transitionTo(AuctionStatus)} để thay đổi trạng thái.
 */
public class Auction extends Entity {

    /**
     * ID của sản phẩm đang được đấu giá.
     */
    private final String itemId;

    /**
     * Thời điểm bắt đầu phiên.
     */
    private final LocalDateTime startTime;

    /**
     * Thời điểm kết thúc. Có thể được gia hạn nếu Anti-Sniping kích hoạt.
     */
    private LocalDateTime endTime;

    /**
     * Giá khởi điểm — không thay đổi sau khi tạo.
     */
    private final double startingPrice;

    /**
     * Giá cao nhất hiện tại. Khởi tạo = startingPrice.
     */
    private double currentHighestBid;

    /**
     * ID của người đặt giá cao nhất. Null nếu chưa ai đặt.
     */
    private String highestBidderId;

    /**
     * Trạng thái phiên. Mặc định OPEN khi tạo.
     */
    private AuctionStatus status;

    /**
    * Mức tăng tối thiểu giữa 2 lần đặt giá (VND).
    */
    private final double minimumIncrement;

    /**
     * Tạo phiên đấu giá mới.
     *
     * @param itemId         id sản phẩm, không null
     * @param startTime      thời điểm bắt đầu, không null
     * @param endTime        thời điểm kết thúc, không null, sau startTime
     * @param startingPrice  giá khởi điểm, phải > 0
     * @param minimumIncrement mức tăng tối thiểu, phải >= 0
     * @throws IllegalArgumentException nếu vi phạm điều kiện
     */

    public Auction(
            String itemId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            double startingPrice,
            double minimumIncrement) {
        super();
        Objects.requireNonNull(itemId, "itemId không được null");
        Objects.requireNonNull(startTime, "startTime không được null");
        Objects.requireNonNull(endTime, "endTime không được null");
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("startingPrice phải > 0: " + startingPrice);
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }
        if (minimumIncrement < 0) {
            throw new IllegalArgumentException("minimumIncrement không được âm: " + minimumIncrement);
        }
        this.itemId = itemId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.highestBidderId = null;
        this.status = AuctionStatus.OPEN;
        this.minimumIncrement = minimumIncrement;
    }

    /**
     * Chuyển trạng thái phiên đấu giá sang trạng thái mới.
     *
     * <p>Kiểm tra tính hợp lệ qua {@link AuctionStatus#canTransitionTo(AuctionStatus)}.
     * Nếu không hợp lệ → ném {@link IllegalStateException}.
     *
     * <p>Ví dụ hợp lệ: {@code OPEN → RUNNING}, {@code RUNNING → FINISHED}.
     * Ví dụ không hợp lệ: {@code RUNNING → OPEN} → ném exception.
     *
     * @param newStatus trạng thái mới muốn chuyển tới
     * @throws IllegalStateException nếu transition không hợp lệ
     */
    public void transitionTo(AuctionStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus không được null");
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Không thể chuyển từ " + status.name() + " sang " + newStatus.name()
                            + " [auctionId=" + getId().substring(0, 7) + "]");
        }
        this.status = newStatus;
        markUpdated();
    }

    /**
     * Kiểm tra xem một mức giá có hợp lệ để đặt không.
     *
     * <p>Điều kiện:
     * <ol>
     *   <li>Trạng thái phải là {@link AuctionStatus#RUNNING} ({@code status.canBid() == true})</li>
     *   <li>{@code bidAmount > currentHighestBid}</li>
     * </ol>
     *
     *@param bidAmount mức giá muốn đặt
     * @return {@code true} nếu hợp lệ để đặt
     */
    public boolean isValidBid(double bidAmount) {
        return status.canBid() && bidAmount > currentHighestBid;
    }

    /**
     * Cập nhật giá cao nhất (sau khi bid được chấp nhận).
     *
     *
     * @param newHighestBid mức giá mới (đã kiểm tra hợp lệ trước khi gọi)
     * @param bidderId      id người đặt
     */
    public void updateHighestBid(double newHighestBid, String bidderId) {
        this.currentHighestBid = newHighestBid;
        this.highestBidderId = bidderId;
        markUpdated();
    }
    /**
     * Gia hạn thời gian kết thúc (Anti-Sniping — Tuần 8).
     *
     * @param newEndTime thời điểm kết thúc mới, phải sau endTime hiện tại
     * @throws IllegalArgumentException nếu newEndTime không sau endTime hiện tại
     */
    public void extendEndTime(LocalDateTime newEndTime) {
        Objects.requireNonNull(newEndTime, "newEndTime không được null");
        if (!newEndTime.isAfter(this.endTime)) {
            throw new IllegalArgumentException(
                    "newEndTime phải sau endTime hiện tại (" + this.endTime + ")");
        }
        this.endTime = newEndTime;
        markUpdated();
    }

    // Getters
    /** Trả về id sản phẩm. */
    public String getItemId() { return itemId; }
    /** Trả về thời điểm bắt đầu. */
    public LocalDateTime getStartTime() { return startTime; }
    /** Trả về thời điểm kết thúc (có thể đã gia hạn). */
    public LocalDateTime getEndTime() { return endTime; }
    /** Trả về giá khởi điểm. */
    public double getStartingPrice() { return startingPrice; }
    /** Trả về giá cao nhất hiện tại. */
    public double getCurrentHighestBid() { return currentHighestBid; }
    /** Trả về id người đặt giá cao nhất (null nếu chưa ai đặt). */
    public String getHighestBidderId() { return highestBidderId; }
    /** Trả về trạng thái hiện tại của phiên. */
    public AuctionStatus getStatus() { return status; }
    /** Trả về mức tăng tối thiểu. */
    public double getMinimumIncrement() { return minimumIncrement; }

    /**
     * Constructor load từ database — dùng bởi {@link AuctionDao#mapRow(ResultSet)}.
     *
     * @param id               ID từ DB
     * @param createdAt        thời điểm tạo từ DB
     * @param updatedAt        thời điểm cập nhật từ DB
     * @param itemId           ID sản phẩm
     * @param startTime        thời điểm bắt đầu
     * @param endTime          thời điểm kết thúc
     * @param startingPrice    giá khởi điểm
     * @param currentHighestBid giá cao nhất hiện tại
     * @param highestBidderId  ID người đặt cao nhất (có thể null)
     * @param status           trạng thái hiện tại
     * @param minimumIncrement mức tăng tối thiểu
     */
    public Auction(
            String id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String itemId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            double startingPrice,
            double currentHighestBid,
            String highestBidderId,
            AuctionStatus status,
            double minimumIncrement) {
        super(id, createdAt, updatedAt);
        this.itemId = itemId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startingPrice = startingPrice;
        this.currentHighestBid = currentHighestBid;
        this.highestBidderId = highestBidderId;
        this.status = status;
        this.minimumIncrement = minimumIncrement;
    }

    /**
     * Cap nhat gia cao nhat hien tai — dung khi co bid moi.
     *
     * <p>// 📌 [Tieu chi: Chuc nang dau gia — cap nhat gia trong RAM]
     *
     * @param amount gia dau moi
     */
    public void setCurrentHighestBid(double amount) {
        this.currentHighestBid = amount;
    }

    /**
     * Cap nhat id nguoi dan dau — dung khi co bid moi.
     *
     * @param bidderId id cua nguoi dau gia moi
     */
    public void setHighestBidderId(String bidderId) {
        this.highestBidderId = bidderId;
    }
}
