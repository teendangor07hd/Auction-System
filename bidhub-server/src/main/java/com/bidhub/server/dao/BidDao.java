package com.bidhub.server.dao;

import com.bidhub.server.config.DbConnectionProvider;
import com.bidhub.server.model.BidTransaction;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD cho bảng {@code bid_transactions}.
 * {@link #getHighestBid(String)} được BidService tuần 6 dùng để verify winner.
 */
public class BidDao {

    private final Connection injectedConn;

    public BidDao() {
        this.injectedConn = null;
    }

    public BidDao(Connection conn) {
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
     * Lưu một bid transaction vào DB.
     *
     * @param bid transaction cần lưu
     */
    public void save(BidTransaction bid) {
        String sql = """
        INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, bid_time)
        VALUES (?, ?, ?, ?, ?)
        """;
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, bid.getId());
                ps.setString(2, bid.getAuctionId());
                ps.setString(3, bid.getBidderId());
                ps.setDouble(4, bid.getBidAmount());
                ps.setString(5, bid.getBidTime().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("BidDao.save thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Trả về tất cả bid của 1 phiên, sắp xếp theo thờii gian tăng dần.
     * Dùng để hiển thị lịch sử đặt giá và Price Chart tuần 8.
     *
     * @param auctionId ID phiên
     * @return danh sách BidTransaction theo thứ tự bid_time ASC
     */
    public List<BidTransaction> findByAuctionId(String auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, auctionId);
                ResultSet rs = ps.executeQuery();
                List<BidTransaction> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("BidDao.findByAuctionId thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    /**
     * Trả về bid cao nhất của 1 phiên (dùng để verify winner khi đóng phiên).
     *
     * @param auctionId ID phiên
     * @return {@link Optional} chứa BidTransaction cao nhất, empty nếu chưa có bid nào
     */
    public Optional<BidTransaction> getHighestBid(String auctionId) {
        // ORDER BY bid_amount DESC LIMIT 1 — đảm bảo luôn lấy đúng bid cao nhất
        String sql = """
        SELECT * FROM bid_transactions
        WHERE auction_id = ?
        ORDER BY bid_amount DESC
        LIMIT 1
        """;
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, auctionId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("BidDao.getHighestBid thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    public List<BidTransaction> findAll() {
        String sql = "SELECT * FROM bid_transactions ORDER BY bid_time DESC";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<BidTransaction> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("BidDao.findAll thất bại: " + e.getMessage(), e);
        } finally {
            releaseConnection(conn);
        }
    }

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        return new BidTransaction(
                rs.getString("id"),
                rs.getString("auction_id"),
                rs.getString("bidder_id"),
                rs.getDouble("bid_amount"),
                LocalDateTime.parse(rs.getString("bid_time")));
    }
}
