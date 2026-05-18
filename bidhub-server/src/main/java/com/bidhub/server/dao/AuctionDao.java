package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD cho bảng {@code auctions}. Cung cấp 3 UPDATE method riêng biệt để tránh
 * overwrite toàn bộ record khi chỉ cần thay đổi 1 trường.
 */
public class AuctionDao {

    private final Connection injectedConn;

    public AuctionDao() {
        this.injectedConn = null;
    }

    public AuctionDao(Connection conn) {
        this.injectedConn = conn;
    }

    private Connection acquireConnection() throws SQLException {
        return (injectedConn != null)
                ? injectedConn
                : DbConnectionProvider.getInstance().getConnection();
    }

    private void releaseConnection(Connection conn) {
        if (injectedConn == null) {
            DbConnectionProvider.getInstance().closeConnection(conn);
        }
    }

    /**
     * Lưu phiên đấu giá mới vào DB.
     *
     * @param auction đối tượng cần lưu (status mặc định OPEN)
     */
    public void save(Auction auction) {
        String sql = """
        INSERT INTO auctions (id, item_id, start_time, end_time, starting_price,
            current_highest_bid, highest_bidder_id, status, minimum_increment,
            created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, auction.getId());
                ps.setString(2, auction.getItemId());
                ps.setString(3, auction.getStartTime().toString());
                ps.setString(4, auction.getEndTime().toString());
                ps.setDouble(5, auction.getStartingPrice());
                ps.setDouble(6, auction.getCurrentHighestBid());
                ps.setString(7, auction.getHighestBidderId());
                ps.setString(8, auction.getStatus().name());
                ps.setDouble(9, auction.getMinimumIncrement());
                ps.setString(10, auction.getCreatedAt().toString());
                ps.setString(11, auction.getUpdatedAt().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.save thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Tìm phiên đấu giá theo ID.
     *
     * @param id UUID string
     * @return {@link Optional} chứa Auction, hoặc empty nếu không có
     */
    public Optional<Auction> findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.findById thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Trả về tất cả phiên có status = OPEN hoặc RUNNING.
     * Dùng trong AuctionListController để hiển thị danh sách đang chờ và đang diễn ra.
     *
     * @return danh sách phiên OPEN + RUNNING, có thể rỗng
     */
    public List<Auction> findActiveAuctions() {
        String sql = "SELECT * FROM auctions WHERE status IN ('OPEN', 'RUNNING')";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                List<Auction> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.findActiveAuctions thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }


    /**
     * Cập nhật trạng thái phiên (OPEN→RUNNING, RUNNING→FINISHED...).
     *
     * @param auctionId ID phiên
     * @param status    trạng thái mới
     */
    public void updateStatus(String auctionId, AuctionStatus status) {
        runUpdate("UPDATE auctions SET status = ?, updated_at = ? WHERE id = ?",
                status.name(), LocalDateTime.now().toString(), auctionId);
    }

    /**
     * Cập nhật giá cao nhất và ngườii đặt cao nhất (gọi sau khi bid hợp lệ).
     *
     * @param auctionId ID phiên
     * @param amount    giá mới
     * @param bidderId  ID ngườii đặt
     */
    public void updateHighestBid(String auctionId, double amount, String bidderId) {
        // 📌 [Tiêu chí: MVC — DAO xử lý persistence cho Bidding Engine tuần 6]
        String sql = """
        UPDATE auctions
        SET current_highest_bid = ?, highest_bidder_id = ?, updated_at = ?
        WHERE id = ?
        """;
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setString(2, bidderId);
                ps.setString(3, LocalDateTime.now().toString());
                ps.setString(4, auctionId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.updateHighestBid thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Cập nhật end_time (dùng cho Anti-Sniping tuần 8 — method này đã sẵn sàng).
     *
     * @param auctionId  ID phiên
     * @param newEndTime thờii điểm kết thúc mới (phải sau endTime hiện tại)
     */
    public void updateEndTime(String auctionId, LocalDateTime newEndTime) {
        runUpdate("UPDATE auctions SET end_time = ?, updated_at = ? WHERE id = ?",
                newEndTime.toString(), LocalDateTime.now().toString(), auctionId);
    }

    private void runUpdate(String sql, Object... params) {
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    Object p = params[i];
                    if (p instanceof String s) ps.setString(i + 1, s);
                    else if (p instanceof Double d) ps.setDouble(i + 1, d);
                    else if (p instanceof Integer in) ps.setInt(i + 1, in);
                    else if (p == null) ps.setNull(i + 1, Types.NULL);
                    else ps.setObject(i + 1, p);
                }
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao update thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    private Auction mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));
        String itemId = rs.getString("item_id");
        LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
        LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"));
        // Lam tron so thap phan sang VND (so nguyen) de tranh sai lech float
        double startingPrice = Math.round(rs.getDouble("starting_price"));
        double currentHighestBid = Math.round(rs.getDouble("current_highest_bid"));

        // getString trả về null khi cột NULL trong DB — null-safe cho phiên chưa có bid
        String highestBidderId = rs.getString("highest_bidder_id");

        String statusStr = rs.getString("status");
        AuctionStatus status;
        try {
            status = AuctionStatus.valueOf(statusStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("Loi map status: " + statusStr, e);
        }
        double minimumIncrement = Math.round(rs.getDouble("minimum_increment"));

        return new Auction(id, createdAt, updatedAt, itemId, startTime, endTime,
                startingPrice, currentHighestBid, highestBidderId, status, minimumIncrement);
    }
    /**
     * Lay tat ca auction — ORDER BY created_at DESC (moi nhat truoc).
     *
     * <p>Dung cho ReportService.exportAuctionReport().
     *
     * @return danh sach tat ca auction
     */
    public List<Auction> findAll() {
        List<Auction> result = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY created_at DESC";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.findAll that bai: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
        return result;
    }

  public static class AuctionBidDto {
      public final String id;
      public final double currentHighestBid;
      public final String highestBidderId;
      public final String itemName;

      public AuctionBidDto(String id, double currentHighestBid, String highestBidderId, String itemName) {
          this.id = id;
          this.currentHighestBid = currentHighestBid;
          this.highestBidderId = highestBidderId;
          this.itemName = itemName;
      }
  }

  /**
   * Lay tat ca auction voi thong tin bid — dung cho DataIntegrityService.
   *
   * @return danh sach chua thong tin auction
   */
  public List<AuctionBidDto> findAllWithBidInfo() {
    List<AuctionBidDto> result = new ArrayList<>();
    String sql = "SELECT a.id, a.current_highest_bid, a.highest_bidder_id, i.name as item_name FROM auctions a LEFT JOIN items i ON a.item_id = i.id";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(new AuctionBidDto(
              rs.getString("id"),
              Math.round(rs.getDouble("current_highest_bid")),
              rs.getString("highest_bidder_id"),
              rs.getString("item_name") != null ? rs.getString("item_name") : "Không rõ tên"
          ));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("AuctionDao.findAllWithBidInfo that bai: "
          + e.getMessage(), e);
    } finally {
      releaseConnection(conn);
    }
    return result;
  }

    public int countByStatus(AuctionStatus status) {
        String sql = "SELECT COUNT(*) FROM auctions WHERE status = ?";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status.name());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.countByStatus that bai: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    public double sumHighestBids() {
        String sql = "SELECT SUM(current_highest_bid) FROM auctions";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
                return 0.0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.sumHighestBids that bai: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    public Map<String, String> getItemAuctionStatusMap() {
        Map<String, String> result = new HashMap<>();
        String sql = "SELECT item_id, status FROM auctions";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String itemId = rs.getString("item_id");
                    String status = rs.getString("status");
                    String existing = result.get(itemId);
                    if (existing == null) {
                        result.put(itemId, status);
                    } else if ("RUNNING".equals(status)) {
                        result.put(itemId, "RUNNING");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.getItemAuctionStatusMap that bai: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
        return result;
    }

    /**
     * Xóa phiên đấu giá theo ID — dùng để hủy phiên PENDING.
     *
     * @param auctionId UUID của phiên cần hủy
     */
    public void deleteById(String auctionId) {
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM auctions WHERE id = ?")) {
                ps.setString(1, auctionId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("AuctionDao.deleteById thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }
}