package com.portfolio.tracker.service;

import com.portfolio.tracker.dao.AssetDao;
import com.portfolio.tracker.dao.ClientDao;
import com.portfolio.tracker.dao.HoldingDao;
import com.portfolio.tracker.dao.TransactionDao;
import com.portfolio.tracker.dto.TransactionRequestDto;
import com.portfolio.tracker.exception.ClientInactiveException;
import com.portfolio.tracker.exception.ClientNotFoundException;
import com.portfolio.tracker.exception.InsufficientHoldingsException;
import com.portfolio.tracker.model.Asset;
import com.portfolio.tracker.model.Client;
import com.portfolio.tracker.model.Holding;
import com.portfolio.tracker.model.Transaction;
import com.portfolio.tracker.model.enums.AssetType;
import com.portfolio.tracker.model.enums.ClientStatus;
import com.portfolio.tracker.model.enums.RiskProfile;
import com.portfolio.tracker.model.enums.TxnType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService.
 *
 * Testing a service that manages JDBC transactions requires mocking
 * the DataSource and Connection — a bit more setup, but it lets us
 * test all the branching logic without a real database.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    // DAOs and DataSource are all mocked
    @Mock private DataSource      dataSource;
    @Mock private Connection      connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet       resultSet;

    @Mock private ClientDao       clientDao;
    @Mock private AssetDao        assetDao;
    @Mock private HoldingDao      holdingDao;
    @Mock private TransactionDao  transactionDao;

    // We create the service manually (not via @InjectMocks) because we need
    // to inject the mocked DataSource explicitly.
    private TransactionService transactionService;

    private Client  activeClient;
    private Client  inactiveClient;
    private Asset   sampleAsset;

    @BeforeEach
    void setUp() throws Exception {
        transactionService = new TransactionService(
            dataSource, clientDao, assetDao, holdingDao, transactionDao);

        activeClient = new Client(1L, "Alice", "alice@example.com", "9876543210",
                                  RiskProfile.AGGRESSIVE, ClientStatus.ACTIVE, LocalDateTime.now());

        inactiveClient = new Client(5L, "Eva", "eva@example.com", "1234567890",
                                    RiskProfile.AGGRESSIVE, ClientStatus.INACTIVE, LocalDateTime.now());

        sampleAsset = new Asset(1L, "AAPL", "Apple Inc.", AssetType.STOCK,
                                new BigDecimal("189.50"), LocalDateTime.now());

        // Mock the DataSource to return our fake connection
        when(dataSource.getConnection()).thenReturn(connection);
        // Mocks for PreparedStatement so insert doesn't actually hit a DB
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
            .thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString()))
            .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(100L);  // generated txn_id
    }

    // ─────────────────────────────────────────────────────────────
    // BUY tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BUY: creates new holding when client has no existing position")
    void buy_createsNewHoldingWhenNoneExists() throws Exception {
        when(clientDao.findById(1L)).thenReturn(Optional.of(activeClient));
        when(assetDao.findById(1L)).thenReturn(Optional.of(sampleAsset));
        // No existing holding — first buy
        when(holdingDao.findByClientAndAsset(eq(1L), eq(1L), any(Connection.class)))
            .thenReturn(Optional.empty());

        TransactionRequestDto dto = buildDto(1L, 1L, TxnType.BUY,
                                             new BigDecimal("10"), new BigDecimal("150.00"));

        // Mock transactionDao.insert to return a transaction with an id
        when(transactionDao.insert(any(Transaction.class), any(Connection.class)))
            .thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setTxnId(100L);
                return t;
            });

        Transaction result = transactionService.processTransaction(dto);

        assertThat(result.getTxnId()).isEqualTo(100L);
        assertThat(result.getTxnType()).isEqualTo(TxnType.BUY);

        // A new holding should have been inserted
        verify(holdingDao, times(1)).insert(any(Holding.class), any(Connection.class));
        // No update should have been called
        verify(holdingDao, never()).updateAfterBuy(any(), any(), any(), any(), any());

        verify(connection, times(1)).commit();
    }

    @Test
    @DisplayName("BUY: recalculates weighted average price when adding to existing holding")
    void buy_updatesWeightedAverageForExistingHolding() throws Exception {
        // Existing holding: 10 units @ $150.00
        Holding existing = new Holding(1L, 1L, 1L,
                                       new BigDecimal("10"), new BigDecimal("150.00"));

        when(clientDao.findById(1L)).thenReturn(Optional.of(activeClient));
        when(assetDao.findById(1L)).thenReturn(Optional.of(sampleAsset));
        when(holdingDao.findByClientAndAsset(eq(1L), eq(1L), any(Connection.class)))
            .thenReturn(Optional.of(existing));
        when(transactionDao.insert(any(Transaction.class), any(Connection.class)))
            .thenAnswer(inv -> { Transaction t = inv.getArgument(0); t.setTxnId(101L); return t; });

        // BUY 5 more @ $160
        TransactionRequestDto dto = buildDto(1L, 1L, TxnType.BUY,
                                             new BigDecimal("5"), new BigDecimal("160.00"));

        transactionService.processTransaction(dto);

        // Verify updateAfterBuy was called with correct new values:
        // newQty = 15, newAvg = (10*150 + 5*160) / 15 = 2300/15 = 153.3333
        verify(holdingDao, times(1)).updateAfterBuy(
            eq(1L),
            eq(1L),
            eq(new BigDecimal("15")),
            argThat(avg -> avg.compareTo(new BigDecimal("153.3333")) == 0),
            any(Connection.class)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // SELL tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SELL: reduces holding when client has enough units")
    void sell_reducesHoldingSuccessfully() throws Exception {
        // Alice holds 12 @ 153.33
        Holding existing = new Holding(1L, 1L, 1L,
                                       new BigDecimal("12"), new BigDecimal("153.33"));

        when(clientDao.findById(1L)).thenReturn(Optional.of(activeClient));
        when(assetDao.findById(1L)).thenReturn(Optional.of(sampleAsset));
        // Called twice: once in validateSellQuantity, once inside processSell
        when(holdingDao.findByClientAndAsset(eq(1L), eq(1L), any(Connection.class)))
            .thenReturn(Optional.of(existing));
        when(transactionDao.insert(any(Transaction.class), any(Connection.class)))
            .thenAnswer(inv -> { Transaction t = inv.getArgument(0); t.setTxnId(102L); return t; });

        // SELL 3 units
        TransactionRequestDto dto = buildDto(1L, 1L, TxnType.SELL,
                                             new BigDecimal("3"), new BigDecimal("189.50"));

        transactionService.processTransaction(dto);

        // New quantity = 12 - 3 = 9
        verify(holdingDao, times(1)).updateAfterSell(
            eq(1L), eq(1L), eq(new BigDecimal("9")), any(Connection.class));
    }

    @Test
    @DisplayName("SELL: throws InsufficientHoldingsException when sell qty exceeds held qty")
    void sell_throwsWhenInsufficientHoldings() throws Exception {
        // Alice holds only 5 units
        Holding existing = new Holding(1L, 1L, 1L,
                                       new BigDecimal("5"), new BigDecimal("153.33"));

        when(clientDao.findById(1L)).thenReturn(Optional.of(activeClient));
        when(assetDao.findById(1L)).thenReturn(Optional.of(sampleAsset));
        when(holdingDao.findByClientAndAsset(eq(1L), eq(1L), any(Connection.class)))
            .thenReturn(Optional.of(existing));

        // Try to sell 10 units (more than held)
        TransactionRequestDto dto = buildDto(1L, 1L, TxnType.SELL,
                                             new BigDecimal("10"), new BigDecimal("189.50"));

        assertThatThrownBy(() -> transactionService.processTransaction(dto))
            .isInstanceOf(InsufficientHoldingsException.class)
            .hasMessageContaining("10");

        // Rollback should have been called because exception was thrown
        verify(connection, times(1)).rollback();
    }

    // ─────────────────────────────────────────────────────────────
    // Validation tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processTransaction: throws ClientNotFoundException for unknown client")
    void processTransaction_throwsWhenClientMissing() {
        when(clientDao.findById(99L)).thenReturn(Optional.empty());

        TransactionRequestDto dto = buildDto(99L, 1L, TxnType.BUY,
                                             BigDecimal.ONE, BigDecimal.TEN);

        assertThatThrownBy(() -> transactionService.processTransaction(dto))
            .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    @DisplayName("processTransaction: throws ClientInactiveException for inactive client")
    void processTransaction_throwsWhenClientInactive() {
        when(clientDao.findById(5L)).thenReturn(Optional.of(inactiveClient));

        TransactionRequestDto dto = buildDto(5L, 1L, TxnType.BUY,
                                             BigDecimal.ONE, BigDecimal.TEN);

        assertThatThrownBy(() -> transactionService.processTransaction(dto))
            .isInstanceOf(ClientInactiveException.class);
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────

    private TransactionRequestDto buildDto(Long clientId, Long assetId, TxnType type,
                                            BigDecimal qty, BigDecimal price) {
        TransactionRequestDto dto = new TransactionRequestDto();
        dto.setClientId(clientId);
        dto.setAssetId(assetId);
        dto.setTxnType(type);
        dto.setQuantity(qty);
        dto.setPrice(price);
        return dto;
    }
}
