package com.portfolio.tracker.service;

import com.portfolio.tracker.dao.AssetDao;
import com.portfolio.tracker.dao.ClientDao;
import com.portfolio.tracker.dao.HoldingDao;
import com.portfolio.tracker.dto.ClientSummaryDto;
import com.portfolio.tracker.dto.PortfolioRowDto;
import com.portfolio.tracker.exception.ClientNotFoundException;
import com.portfolio.tracker.model.Asset;
import com.portfolio.tracker.model.Client;
import com.portfolio.tracker.model.Holding;
import com.portfolio.tracker.model.enums.AssetType;
import com.portfolio.tracker.model.enums.ClientStatus;
import com.portfolio.tracker.model.enums.RiskProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioService.
 *
 * Uses Mockito to replace real DAOs with mocks.
 * This means tests run WITHOUT a database — fast and isolated.
 *
 * @ExtendWith(MockitoExtension.class) initialises @Mock and @InjectMocks automatically.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    // @Mock creates a Mockito mock (a fake object whose behaviour we control)
    @Mock private ClientDao  clientDao;
    @Mock private HoldingDao holdingDao;
    @Mock private AssetDao   assetDao;

    // @InjectMocks creates the real service and injects the mocks above into it
    @InjectMocks
    private PortfolioService portfolioService;

    // Shared test data — set up before each test
    private Client  sampleClient;
    private Asset   sampleAsset;
    private Holding sampleHolding;

    @BeforeEach
    void setUp() {
        sampleClient = new Client(1L, "Alice", "alice@example.com", "9876543210",
                                  RiskProfile.AGGRESSIVE, ClientStatus.ACTIVE, LocalDateTime.now());

        sampleAsset = new Asset(1L, "AAPL", "Apple Inc.", AssetType.STOCK,
                                new BigDecimal("189.50"), LocalDateTime.now());

        // Alice holds 12 units of AAPL bought at avg $153.33
        sampleHolding = new Holding(1L, 1L, 1L,
                                    new BigDecimal("12"),
                                    new BigDecimal("153.3333"));
    }

    // ─────────────────────────────────────────────────────────────
    // getPortfolio() tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPortfolio: returns enriched rows when client has holdings")
    void getPortfolio_returnsEnrichedRows() {
        // Arrange: tell the mocks what to return when called
        when(clientDao.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(holdingDao.findByClientId(1L)).thenReturn(List.of(sampleHolding));
        when(assetDao.findById(1L)).thenReturn(Optional.of(sampleAsset));

        // Act
        List<PortfolioRowDto> rows = portfolioService.getPortfolio(1L);

        // Assert
        assertThat(rows).hasSize(1);
        PortfolioRowDto row = rows.get(0);

        assertThat(row.getSymbol()).isEqualTo("AAPL");
        assertThat(row.getQuantity()).isEqualByComparingTo("12");

        // Market value = 12 * 189.50 = 2274.00
        assertThat(row.getMarketValue()).isEqualByComparingTo("2274.00");

        // Invested = 12 * 153.3333 = 1840.00 (approx)
        assertThat(row.getInvestedAmount()).isEqualByComparingTo("1840.00");

        // Unrealized P/L = 2274 - 1840 = 434.00
        assertThat(row.getUnrealizedPnl()).isEqualByComparingTo("434.00");

        // P/L% = (189.50 - 153.3333) / 153.3333 * 100 ≈ 23.58%
        assertThat(row.getPnlPercent()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getPortfolio: returns empty list when client has no holdings")
    void getPortfolio_emptyWhenNoHoldings() {
        when(clientDao.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(holdingDao.findByClientId(1L)).thenReturn(Collections.emptyList());

        List<PortfolioRowDto> rows = portfolioService.getPortfolio(1L);

        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("getPortfolio: throws ClientNotFoundException for unknown client")
    void getPortfolio_throwsWhenClientNotFound() {
        when(clientDao.findById(99L)).thenReturn(Optional.empty());

        // assertThatThrownBy is the AssertJ way to test exception throwing
        assertThatThrownBy(() -> portfolioService.getPortfolio(99L))
            .isInstanceOf(ClientNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ─────────────────────────────────────────────────────────────
    // getSummary() tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSummary: correctly aggregates totals")
    void getSummary_aggregatesCorrectly() {
        when(clientDao.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(holdingDao.findByClientId(1L)).thenReturn(List.of(sampleHolding));
        when(assetDao.findById(1L)).thenReturn(Optional.of(sampleAsset));

        ClientSummaryDto summary = portfolioService.getSummary(1L);

        assertThat(summary.getClientId()).isEqualTo(1L);
        assertThat(summary.getClientName()).isEqualTo("Alice");
        assertThat(summary.getTotalMarketValue()).isEqualByComparingTo("2274.00");
        assertThat(summary.getTotalPnl()).isPositive();           // in profit
        assertThat(summary.getTotalPnlPercent()).isPositive();
    }

    @Test
    @DisplayName("getSummary: P/L percent is zero when no holdings")
    void getSummary_zeroPnlWithNoHoldings() {
        when(clientDao.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(holdingDao.findByClientId(1L)).thenReturn(Collections.emptyList());

        ClientSummaryDto summary = portfolioService.getSummary(1L);

        assertThat(summary.getTotalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalPnlPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────
    // updateClient() tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateClient: updates mutable fields and calls dao.update()")
    void updateClient_updatesFieldsAndPersists() {
        when(clientDao.findById(1L)).thenReturn(Optional.of(sampleClient));

        Client updateData = new Client();
        updateData.setName("Alice Updated");
        updateData.setPhone("1111111111");
        updateData.setRiskProfile(RiskProfile.MODERATE);

        Client result = portfolioService.updateClient(1L, updateData);

        assertThat(result.getName()).isEqualTo("Alice Updated");
        assertThat(result.getRiskProfile()).isEqualTo(RiskProfile.MODERATE);

        // Verify that dao.update() was called exactly once
        verify(clientDao, times(1)).update(any(Client.class));
    }

    @Test
    @DisplayName("deactivateClient: calls dao.deactivate() for existing client")
    void deactivateClient_callsDeactivate() {
        when(clientDao.findById(1L)).thenReturn(Optional.of(sampleClient));

        portfolioService.deactivateClient(1L);

        verify(clientDao, times(1)).deactivate(1L);
    }
}
