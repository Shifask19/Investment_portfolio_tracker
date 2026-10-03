package com.portfolio.tracker.dao.impl;

import com.portfolio.tracker.dao.HoldingDao;
import com.portfolio.tracker.model.Holding;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of HoldingDao.
 *
 * IMPORTANT DESIGN NOTE — Why do methods accept a Connection?
 * ──────────────────────────────────────────────────────────
 * A BUY/SELL must atomically:
 *   1. Insert a row into transactions
 *   2. INSERT or UPDATE a row in holdings
 *
 * Both operations MUST be in the same database transaction.
 * The TransactionService opens one Connection, calls setAutoCommit(false),
 * then passes that Connection to both TransactionDaoImpl.insert() and
 * HoldingDaoImpl.updateAfterBuy/Sell().
 *
 * Only the service layer commits or rolls back — the DAO never
 * calls commit() or rollback(). This keeps transaction management
 * in the service, not scattered across DAOs.
 */
@Repository
public class HoldingDaoImpl implements HoldingDao {

    private final DataSource dataSource;

    public HoldingDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY CLIENT ID  (uses its own connection — read-only)
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<Holding> findByClientId(Long clientId) {
        String sql = "SELECT holding_id, client_id, asset_id, quantity, avg_buy_price "
                   + "FROM holdings WHERE client_id = ?";

        List<Holding> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, clientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find holdings for client: " + e.getMessage(), e);
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY CLIENT AND ASSET  (uses caller's connection)
    // ─────────────────────────────────────────────────────────────

    @Override
    public Optional<Holding> findByClientAndAsset(Long clientId, Long assetId, Connection conn) {
        String sql = "SELECT holding_id, client_id, asset_id, quantity, avg_buy_price "
                   + "FROM holdings WHERE client_id = ? AND asset_id = ?";

        // NOTE: we do NOT close 'conn' here — the caller owns and will close it.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, clientId);
            ps.setLong(2, assetId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find holding: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ─────────────────────────────────────────────────────────────
    // INSERT  (uses caller's connection)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void insert(Holding holding, Connection conn) {
        String sql = "INSERT INTO holdings (client_id, asset_id, quantity, avg_buy_price) "
                   + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, holding.getClientId());
            ps.setLong(2, holding.getAssetId());
            ps.setBigDecimal(3, holding.getQuantity());
            ps.setBigDecimal(4, holding.getAvgBuyPrice());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert holding: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE AFTER BUY  (uses caller's connection)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void updateAfterBuy(Long clientId, Long assetId,
                                BigDecimal newQuantity, BigDecimal newAvgBuyPrice,
                                Connection conn) {
        String sql = "UPDATE holdings SET quantity = ?, avg_buy_price = ? "
                   + "WHERE client_id = ? AND asset_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newQuantity);
            ps.setBigDecimal(2, newAvgBuyPrice);
            ps.setLong(3, clientId);
            ps.setLong(4, assetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update holding after buy: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE AFTER SELL  (uses caller's connection)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void updateAfterSell(Long clientId, Long assetId, BigDecimal newQuantity, Connection conn) {
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // Quantity hit zero — remove the row entirely (no ghost holdings)
            String sql = "DELETE FROM holdings WHERE client_id = ? AND asset_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, clientId);
                ps.setLong(2, assetId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete zero holding: " + e.getMessage(), e);
            }
        } else {
            // Reduce the quantity; avg_buy_price is unchanged on a SELL
            String sql = "UPDATE holdings SET quantity = ? WHERE client_id = ? AND asset_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, newQuantity);
                ps.setLong(2, clientId);
                ps.setLong(3, assetId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update holding after sell: " + e.getMessage(), e);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────

    private Holding mapRow(ResultSet rs) throws SQLException {
        return new Holding(
            rs.getLong("holding_id"),
            rs.getLong("client_id"),
            rs.getLong("asset_id"),
            rs.getBigDecimal("quantity"),
            rs.getBigDecimal("avg_buy_price")
        );
    }
}
