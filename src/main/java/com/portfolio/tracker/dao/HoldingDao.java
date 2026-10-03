package com.portfolio.tracker.dao;

import com.portfolio.tracker.model.Holding;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Holding persistence.
 *
 * NOTE: Several methods accept a java.sql.Connection parameter.
 * This is intentional — BUY/SELL processing spans TWO tables
 * (transactions + holdings) in a single JDBC transaction.
 * The service layer opens the connection, begins the transaction,
 * then passes the same Connection to both DAOs so both operations
 * share the same commit/rollback boundary.
 */
public interface HoldingDao {

    /**
     * Return all holdings for a client, joined with asset info.
     * The List may be empty but never null.
     */
    List<Holding> findByClientId(Long clientId);

    /**
     * Find the specific position a client holds in one asset.
     * Returns empty Optional if the client has never bought this asset.
     */
    Optional<Holding> findByClientAndAsset(Long clientId, Long assetId, Connection conn);

    /**
     * Insert a brand-new holding row (first BUY for this client-asset pair).
     * Uses the caller's Connection so it participates in the same transaction.
     */
    void insert(Holding holding, Connection conn);

    /**
     * Update quantity and average buy price after a BUY.
     * Uses the caller's Connection.
     */
    void updateAfterBuy(Long clientId, Long assetId,
                        BigDecimal newQuantity, BigDecimal newAvgBuyPrice,
                        Connection conn);

    /**
     * Decrease quantity after a SELL.
     * If quantity reaches zero, the row is deleted (no ghost holdings).
     * Uses the caller's Connection.
     */
    void updateAfterSell(Long clientId, Long assetId, BigDecimal newQuantity, Connection conn);
}
