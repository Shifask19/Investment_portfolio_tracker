package com.portfolio.tracker.dao;

import com.portfolio.tracker.model.Transaction;
import com.portfolio.tracker.model.enums.TxnType;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Access Object interface for Transaction persistence.
 */
public interface TransactionDao {

    /**
     * Insert a new transaction record.
     * Accepts a caller-supplied Connection so the insert participates in the
     * same JDBC transaction as the corresponding holding update.
     *
     * @return the saved Transaction with its generated txn_id populated.
     */
    Transaction insert(Transaction transaction, Connection conn);

    /**
     * Flexible query method for the transaction history endpoint.
     * All filters are optional (pass null to skip a filter).
     *
     * @param clientId   filter by client  (nullable)
     * @param assetId    filter by asset   (nullable)
     * @param txnType    filter by BUY/SELL (nullable)
     * @param from       start of date range (nullable)
     * @param to         end   of date range (nullable)
     */
    List<Transaction> findWithFilters(Long clientId, Long assetId, TxnType txnType,
                                      LocalDateTime from, LocalDateTime to);

    /** Return all transactions for a single client, newest first. */
    List<Transaction> findByClientId(Long clientId);
}
