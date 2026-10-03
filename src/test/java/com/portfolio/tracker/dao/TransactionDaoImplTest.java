package com.portfolio.tracker.dao;

import com.portfolio.tracker.dao.impl.ClientDaoImpl;
import com.portfolio.tracker.dao.impl.AssetDaoImpl;
import com.portfolio.tracker.dao.impl.TransactionDaoImpl;
import com.portfolio.tracker.model.Asset;
import com.portfolio.tracker.model.Client;
import com.portfolio.tracker.model.Transaction;
import com.portfolio.tracker.model.enums.AssetType;
import com.portfolio.tracker.model.enums.ClientStatus;
import com.portfolio.tracker.model.enums.RiskProfile;
import com.portfolio.tracker.model.enums.TxnType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for TransactionDaoImpl using H2 in-memory DB.
 *
 * We import all three DAOs we need because TransactionDao inserts
 * require FK references to clients and assets.
 */
@JdbcTest
@Import({TransactionDaoImpl.class, ClientDaoImpl.class, AssetDaoImpl.class})
class TransactionDaoImplTest {

    @Autowired private TransactionDao transactionDao;
    @Autowired private ClientDao      clientDao;
    @Autowired private AssetDao       assetDao;
    @Autowired private DataSource     dataSource;

    private Client savedClient;
    private Asset  savedAsset;

    @BeforeEach
    void setUp() {
        // Insert a client and asset so FK constraints are satisfied
        Client c = new Client();
        c.setName("Test Client");
        c.setEmail("testclient@dao.com");
        c.setPhone("9000000001");
        c.setRiskProfile(RiskProfile.MODERATE);
        c.setStatus(ClientStatus.ACTIVE);
        savedClient = clientDao.save(c);

        Asset a = new Asset();
        a.setSymbol("TSTOCK");
        a.setName("Test Stock");
        a.setAssetType(AssetType.STOCK);
        a.setCurrentPrice(new BigDecimal("100.00"));
        savedAsset = assetDao.save(a);
    }

    // ─────────────────────────────────────────────────────────────
    // INSERT test
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("insert: persists a BUY transaction and returns with generated ID")
    void insert_persistsTransactionAndReturnsId() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            Transaction txn = buildTxn(savedClient.getClientId(), savedAsset.getAssetId(),
                                       TxnType.BUY, "10", "100.00");
            Transaction saved = transactionDao.insert(txn, conn);

            conn.commit();

            assertThat(saved.getTxnId()).isNotNull().isPositive();
            assertThat(saved.getTxnType()).isEqualTo(TxnType.BUY);
            assertThat(saved.getQuantity()).isEqualByComparingTo("10");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY CLIENT ID test
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByClientId: returns all transactions for a client")
    void findByClientId_returnsTransactionsForClient() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            transactionDao.insert(buildTxn(savedClient.getClientId(), savedAsset.getAssetId(),
                                           TxnType.BUY, "5", "90.00"), conn);
            transactionDao.insert(buildTxn(savedClient.getClientId(), savedAsset.getAssetId(),
                                           TxnType.BUY, "3", "95.00"), conn);
            conn.commit();
        }

        List<Transaction> txns = transactionDao.findByClientId(savedClient.getClientId());

        assertThat(txns).hasSize(2);
    }

    // ─────────────────────────────────────────────────────────────
    // FIND WITH FILTERS test
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findWithFilters: filters by txn type correctly")
    void findWithFilters_filtersByType() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            // Insert one BUY and one SELL
            transactionDao.insert(buildTxn(savedClient.getClientId(), savedAsset.getAssetId(),
                                           TxnType.BUY, "10", "100.00"), conn);
            transactionDao.insert(buildTxn(savedClient.getClientId(), savedAsset.getAssetId(),
                                           TxnType.SELL, "3", "110.00"), conn);
            conn.commit();
        }

        // Filter: only BUYs
        List<Transaction> buys = transactionDao.findWithFilters(
            savedClient.getClientId(), null, TxnType.BUY, null, null);

        assertThat(buys).allMatch(t -> t.getTxnType() == TxnType.BUY);

        // Filter: only SELLs
        List<Transaction> sells = transactionDao.findWithFilters(
            savedClient.getClientId(), null, TxnType.SELL, null, null);

        assertThat(sells).allMatch(t -> t.getTxnType() == TxnType.SELL);
    }

    @Test
    @DisplayName("findWithFilters: date range filter works correctly")
    void findWithFilters_dateRangeFilter() throws Exception {
        LocalDateTime pastDate  = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime midDate   = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            transactionDao.insert(buildTxn(savedClient.getClientId(), savedAsset.getAssetId(),
                                           TxnType.BUY, "5", "100.00"), conn);
            conn.commit();
        }

        // Should find the transaction (it was just created, so it falls in [midDate, futureDate])
        List<Transaction> found = transactionDao.findWithFilters(
            savedClient.getClientId(), null, null, midDate, futureDate);

        assertThat(found).isNotEmpty();

        // Should NOT find anything with a past-only range
        List<Transaction> notFound = transactionDao.findWithFilters(
            savedClient.getClientId(), null, null, pastDate,
            LocalDateTime.of(2023, 12, 31, 23, 59));

        assertThat(notFound).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────

    private Transaction buildTxn(Long clientId, Long assetId, TxnType type,
                                   String qty, String price) {
        Transaction t = new Transaction();
        t.setClientId(clientId);
        t.setAssetId(assetId);
        t.setTxnType(type);
        t.setQuantity(new BigDecimal(qty));
        t.setPrice(new BigDecimal(price));
        t.setTxnDate(LocalDateTime.now());
        return t;
    }
}
