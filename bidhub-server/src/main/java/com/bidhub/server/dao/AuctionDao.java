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

    // Helper để DRY các UPDATE 3-param đơn giản
    private void runUpdate(String sql, String p1, String p2, String p3) {
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p1);
                ps.setString(2, p2);
                ps.setString(3, p3);
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
        double startingPrice = rs.getDouble("starting_price");
        double currentHighestBid = rs.getDouble("current_highest_bid");

        // getString trả về null khi cột NULL trong DB — null-safe cho phiên chưa có bid
        String highestBidderId = rs.getString("highest_bidder_id");

        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        double minimumIncrement = rs.getDouble("minimum_increment");

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

  /**
   * Lay tat ca auction voi thong tin bid — dung cho DataIntegrityService.
   *
   * <p>Tra ve List<Map> thay vi List<Auction> de lay ca currentHighestBid
   * va highestBidderId dang raw cho so sanh.
   *
   * @return danh sach map chua thong tin auction
   */
  public List<Map<String, Object>> findAllWithBidInfo() {
    List<Map<String, Object>> result = new ArrayList<>();
    String sql = "SELECT id, current_highest_bid, highest_bidder_id FROM auctions";
    Connection conn = null;
    try {
      conn = acquireConnection();
      try (PreparedStatement ps = conn.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> row = new HashMap<>();
          row.put("id", rs.getString("id"));
          row.put("currentHighestBid", rs.getDouble("current_highest_bid"));
          row.put("highestBidderId", rs.getString("highest_bidder_id"));
          result.add(row);
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