package com.portfolio.tracker.service;

import com.portfolio.tracker.dao.AssetDao;
import com.portfolio.tracker.dao.ClientDao;
import com.portfolio.tracker.dao.HoldingDao;
import com.portfolio.tracker.dao.TransactionDao;
import com.portfolio.tracker.dto.TransactionRequestDto;
import com.portfolio.tracker.exception.*;
import com.portfolio.tracker.model.Client;
import com.portfolio.tracker.model.Holding;
import com.portfolio.tracker.model.Transaction;
import com.portfolio.tracker.model.enums.ClientStatus;
import com.portfolio.tracker.model.enums.TxnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core business logic for processing BUY and SELL transactions.
 *
 * IMPORTANT — JDBC Transaction Management:
 * ─────────────────────────────────────────
 * A BUY or SELL must atomically update TWO tables:
 *   1. INSERT into transactions
 *   2. INSERT or UPDATE holdings
 *
 * If step 2 fails after step 1, the data would be inconsistent
 * (a transaction row with no matching holding update).
 *
 * Solution:
 *   - We obtain a single Connection from the DataSource here.
 *   - We call conn.setAutoCommit(false) to start a manual transaction.
 *   - We pass the same Connection to both DAOs.
 *   - On success: conn.commit()
 *   - On failure: conn.rollback() — both operations are undone.
 *   - Finally: conn.close() returns it to the HikariCP pool.
 *
 * This keeps transaction boundaries in the SERVICE layer, not scattered in DAOs.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final DataSource      dataSource;
    private final ClientDao       clientDao;
    private final AssetDao        assetDao;
    private final HoldingDao      holdingDao;
    private final TransactionDao  transactionDao;

    public TransactionService(DataSource dataSource, ClientDao clientDao, AssetDao assetDao,
                               HoldingDao holdingDao, TransactionDao transactionDao) {
        this.dataSource     = dataSource;
        this.clientDao      = clientDao;
        this.assetDao       = assetDao;
        this.holdingDao     = holdingDao;
        this.transactionDao = transactionDao;
    }

    // ─────────────────────────────────────────────────────────────
    // PROCESS TRANSACTION  (main entry point)
    // ─────────────────────────────────────────────────────────────

    /**
     * Processes a BUY or SELL order end-to-end:
     *   Layer 1: Bean Validation already ran in the controller (@Valid).
     *   Layer 2: Business rules validated here (client active, asset exists, no oversell).
     *   Layer 3: DB trigger blocks oversell if Layer 2 is somehow bypassed.
     *
     * @param dto  the validated request DTO from the controller
     * @return the saved Transaction entity with its generated ID
     */
    public Transaction processTransaction(TransactionRequestDto dto) {

        // ── Layer 2 Validation ─────────────────────────────────────

        // 1. Client must exist
        Client client = clientDao.findById(dto.getClientId())
            .orElseThrow(() -> new ClientNotFoundException(dto.getClientId()));

        // 2. Client must be ACTIVE
        if (client.getStatus() == ClientStatus.INACTIVE) {
            throw new ClientInactiveException(dto.getClientId());
        }

        // 3. Asset must exist
        assetDao.findById(dto.getAssetId())
            .orElseThrow(() -> new AssetNotFoundException(dto.getAssetId()));

        // 4. For SELL: client must hold enough units
        if (dto.getTxnType() == TxnType.SELL) {
            validateSellQuantity(dto);
        }

        // ── Atomic JDBC Transaction ────────────────────────────────
        // Obtain a connection from HikariCP manually so we can control commit/rollback.
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);  // begin manual transaction

            Transaction saved;
            if (dto.getTxnType() == TxnType.BUY) {
                saved = processBuy(dto, conn);
            } else {
                saved = processSell(dto, conn);
            }

            conn.commit();  // both inserts/updates succeeded
            log.info("Transaction committed: {}", saved);
            return saved;

        } catch (Exception e) {
            // Roll back BOTH the transaction insert AND the holding update
            if (conn != null) {
                try {
                    conn.rollback();
                    log.warn("Transaction rolled back due to: {}", e.getMessage());
                } catch (SQLException rollbackEx) {
                    log.error("Rollback failed: {}", rollbackEx.getMessage());
                }
            }
            // Re-throw so the GlobalExceptionHandler returns the right HTTP status
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Transaction processing failed: " + e.getMessage(), e);

        } finally {
            // Always return the connection to the HikariCP pool
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);  // reset for safety before returning to pool
                    conn.close();
                } catch (SQLException closeEx) {
                    log.error("Failed to close connection: {}", closeEx.getMessage());
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY TRANSACTIONS
    // ─────────────────────────────────────────────────────────────

    public List<Transaction> getTransactions(Long clientId, Long assetId,
                                              TxnType txnType,
                                              LocalDateTime from, LocalDateTime to) {
        return transactionDao.findWithFilters(clientId, assetId, txnType, from, to);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — BUY LOGIC
    // ─────────────────────────────────────────────────────────────

    private Transaction processBuy(TransactionRequestDto dto, Connection conn) throws SQLException {

        // Step 1: Insert the transaction record
        Transaction txn = buildTransaction(dto);
        transactionDao.insert(txn, conn);

        // Step 2: Update holdings
        Optional<Holding> existingOpt = holdingDao.findByClientAndAsset(
            dto.getClientId(), dto.getAssetId(), conn);

        if (existingOpt.isEmpty()) {
            // First time this client is buying this asset — create a new holding row
            Holding newHolding = new Holding(
                null,
                dto.getClientId(),
                dto.getAssetId(),
                dto.getQuantity(),
                dto.getPrice()  // avg price = buy price for first purchase
            );
            holdingDao.insert(newHolding, conn);

        } else {
            // Already holds this asset — recalculate weighted average buy price
            Holding existing  = existingOpt.get();
            BigDecimal oldQty  = existing.getQuantity();
            BigDecimal oldAvg  = existing.getAvgBuyPrice();
            BigDecimal buyQty  = dto.getQuantity();
            BigDecimal buyPrice = dto.getPrice();

            /*
             * Weighted average formula:
             *   newAvg = (oldQty * oldAvg + buyQty * buyPrice) / (oldQty + buyQty)
             *
             * Example: hold 10 @ $150, buying 5 @ $160
             *   newAvg = (10*150 + 5*160) / 15 = 2300/15 = $153.33
             */
            BigDecimal totalCost   = oldQty.multiply(oldAvg).add(buyQty.multiply(buyPrice));
            BigDecimal totalQty    = oldQty.add(buyQty);
            BigDecimal newAvgPrice = totalCost.divide(totalQty, 4, RoundingMode.HALF_UP);

            holdingDao.updateAfterBuy(dto.getClientId(), dto.getAssetId(),
                                      totalQty, newAvgPrice, conn);
        }
        return txn;
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — SELL LOGIC
    // ─────────────────────────────────────────────────────────────

    private Transaction processSell(TransactionRequestDto dto, Connection conn) throws SQLException {

        // Step 1: Insert the transaction record
        // (The DB trigger trg_before_sell also checks quantity — final safety net)
        Transaction txn = buildTransaction(dto);
        transactionDao.insert(txn, conn);

        // Step 2: Reduce the holding quantity
        Holding existing = holdingDao.findByClientAndAsset(
            dto.getClientId(), dto.getAssetId(), conn).get(); // safe: validated in validateSellQuantity

        BigDecimal newQty = existing.getQuantity().subtract(dto.getQuantity());

        // HoldingDaoImpl.updateAfterSell() removes the row if newQty == 0
        holdingDao.updateAfterSell(dto.getClientId(), dto.getAssetId(), newQty, conn);

        return txn;
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Checks that the client holds enough units before a SELL.
     * Throws InsufficientHoldingsException if they don't.
     * Uses a fresh Connection (not the transaction one) — this is a read-only pre-check.
     */
    private void validateSellQuantity(TransactionRequestDto dto) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            Optional<Holding> holding = holdingDao.findByClientAndAsset(
                dto.getClientId(), dto.getAssetId(), conn);

            if (holding.isEmpty()
                || holding.get().getQuantity().compareTo(dto.getQuantity()) < 0) {

                BigDecimal held = holding.map(Holding::getQuantity).orElse(BigDecimal.ZERO);
                throw new InsufficientHoldingsException(
                    dto.getClientId(), dto.getAssetId(), dto.getQuantity(), held);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate sell quantity: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /** Converts a TransactionRequestDto into a Transaction domain object. */
    private Transaction buildTransaction(TransactionRequestDto dto) {
        Transaction txn = new Transaction();
        txn.setClientId(dto.getClientId());
        txn.setAssetId(dto.getAssetId());
        txn.setTxnType(dto.getTxnType());
        txn.setQuantity(dto.getQuantity());
        txn.setPrice(dto.getPrice());
        txn.setTxnDate(LocalDateTime.now());
        return txn;
    }
}
