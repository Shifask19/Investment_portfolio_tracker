package com.portfolio.tracker.dao.impl;

import com.portfolio.tracker.dao.TransactionDao;
import com.portfolio.tracker.model.Transaction;
import com.portfolio.tracker.model.enums.TxnType;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of TransactionDao.
 *
 * The insert() method takes a caller-supplied Connection so it can
 * participate in the BUY/SELL JDBC transaction managed by TransactionService.
 *
 * findWithFilters() builds a dynamic query safely using a StringBuilder
 * and parameter list — never by concatenating user input directly.
 */
@Repository
public class TransactionDaoImpl implements TransactionDao {

    private final DataSource dataSource;

    public TransactionDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ─────────────────────────────────────────────────────────────
    // INSERT  (uses caller's connection)
    // ─────────────────────────────────────────────────────────────

    @Override
    public Transaction insert(Transaction txn, Connection conn) {
        String sql = "INSERT INTO transactions (client_id, asset_id, txn_type, quantity, price, txn_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        // Note: we do NOT close 'conn' here — TransactionService owns it.
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, txn.getClientId());
            ps.setLong(2, txn.getAssetId());
            ps.setString(3, txn.getTxnType().name());
            ps.setBigDecimal(4, txn.getQuantity());
            ps.setBigDecimal(5, txn.getPrice());

            // Default txn_date to NOW if the caller didn't provide one
            LocalDateTime date = txn.getTxnDate() != null ? txn.getTxnDate() : LocalDateTime.now();
            ps.setTimestamp(6, Timestamp.valueOf(date));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    txn.setTxnId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert transaction: " + e.getMessage(), e);
        }
        return txn;
    }

    // ─────────────────────────────────────────────────────────────
    // FIND WITH FILTERS (dynamic query, safe parameterization)
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<Transaction> findWithFilters(Long clientId, Long assetId, TxnType txnType,
                                              LocalDateTime from, LocalDateTime to) {

        // Build the WHERE clause dynamically, collecting parameters in order
        StringBuilder sql = new StringBuilder(
            "SELECT txn_id, client_id, asset_id, txn_type, quantity, price, txn_date "
          + "FROM transactions WHERE 1=1 ");  // 1=1 lets us append AND clauses uniformly

        List<Object> params = new ArrayList<>();

        if (clientId != null) {
            sql.append("AND client_id = ? ");
            params.add(clientId);
        }
        if (assetId != null) {
            sql.append("AND asset_id = ? ");
            params.add(assetId);
        }
        if (txnType != null) {
            sql.append("AND txn_type = ? ");
            params.add(txnType.name());
        }
        if (from != null) {
            sql.append("AND txn_date >= ? ");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append("AND txn_date <= ? ");
            params.add(Timestamp.valueOf(to));
        }

        sql.append("ORDER BY txn_date DESC");

        List<Transaction> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Bind parameters positionally — safe, no string injection risk
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch transactions: " + e.getMessage(), e);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY CLIENT ID
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<Transaction> findByClientId(Long clientId) {
        return findWithFilters(clientId, null, null, null, null);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTxnId(rs.getLong("txn_id"));
        t.setClientId(rs.getLong("client_id"));
        t.setAssetId(rs.getLong("asset_id"));
        t.setTxnType(TxnType.valueOf(rs.getString("txn_type")));
        t.setQuantity(rs.getBigDecimal("quantity"));
        t.setPrice(rs.getBigDecimal("price"));
        Timestamp ts = rs.getTimestamp("txn_date");
        if (ts != null) {
            t.setTxnDate(ts.toLocalDateTime());
        }
        return t;
    }
}
