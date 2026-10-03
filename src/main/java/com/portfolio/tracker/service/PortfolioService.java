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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for read-only portfolio reporting:
 *   - current portfolio snapshot (per holding)
 *   - overall summary (totals + P/L)
 *
 * This service has NO write operations — all mutations go through TransactionService.
 *
 * OOP Principle – Single Responsibility:
 *   PortfolioService only handles READ/REPORT operations.
 *   TransactionService handles WRITE operations.
 *   This separation makes each class smaller, easier to test, and reason about.
 */
@Service
public class PortfolioService {

    private final ClientDao  clientDao;
    private final HoldingDao holdingDao;
    private final AssetDao   assetDao;

    // Spring injects all three DAOs through the constructor
    public PortfolioService(ClientDao clientDao, HoldingDao holdingDao, AssetDao assetDao) {
        this.clientDao  = clientDao;
        this.holdingDao = holdingDao;
        this.assetDao   = assetDao;
    }

    // ─────────────────────────────────────────────────────────────
    // GET PORTFOLIO (list of holdings with enriched data)
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns a row-by-row breakdown of every holding for a client,
     * enriched with current price, market value, and unrealized P/L.
     *
     * @param clientId the client whose portfolio we're reporting
     * @return list of PortfolioRowDto; empty list if client holds nothing
     * @throws ClientNotFoundException if the clientId does not exist
     */
    public List<PortfolioRowDto> getPortfolio(Long clientId) {

        // Validate client exists first — service-level check (Layer 2).
        // We don't use the returned object here; we just need it to throw if absent.
        clientDao.findById(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));

        List<Holding> holdings = holdingDao.findByClientId(clientId);
        List<PortfolioRowDto> rows = new ArrayList<>();

        for (Holding h : holdings) {
            // Fetch live asset info (current price may have been updated)
            Asset asset = assetDao.findById(h.getAssetId())
                .orElse(null);  // asset could theoretically be removed; handle gracefully

            if (asset == null) continue;

            PortfolioRowDto row = buildPortfolioRow(h, asset);
            rows.add(row);
        }
        return rows;
    }

    // ─────────────────────────────────────────────────────────────
    // GET SUMMARY (aggregated totals)
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns overall portfolio totals for a client:
     * total invested, total market value, P/L in dollars, P/L percentage.
     *
     * @param clientId the client
     * @return ClientSummaryDto with aggregated numbers
     */
    public ClientSummaryDto getSummary(Long clientId) {

        Client client = clientDao.findById(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));

        List<PortfolioRowDto> rows = getPortfolio(clientId);

        BigDecimal totalInvested    = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;

        for (PortfolioRowDto row : rows) {
            totalInvested    = totalInvested.add(row.getInvestedAmount());
            totalMarketValue = totalMarketValue.add(row.getMarketValue());
        }

        BigDecimal totalPnl     = totalMarketValue.subtract(totalInvested);
        BigDecimal totalPnlPct  = BigDecimal.ZERO;

        // Avoid divide-by-zero if client hasn't bought anything yet
        if (totalInvested.compareTo(BigDecimal.ZERO) != 0) {
            totalPnlPct = totalPnl
                .divide(totalInvested, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        ClientSummaryDto summary = new ClientSummaryDto();
        summary.setClientId(client.getClientId());
        summary.setClientName(client.getName());
        summary.setTotalInvested(totalInvested.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalMarketValue(totalMarketValue.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalPnl(totalPnl.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalPnlPercent(totalPnlPct);
        return summary;
    }

    // ─────────────────────────────────────────────────────────────
    // CLIENT CRUD  (thin pass-through; controller delegates here)
    // ─────────────────────────────────────────────────────────────

    public Client saveClient(Client client) {
        return clientDao.save(client);
    }

    public Client getClient(Long clientId) {
        return clientDao.findById(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    public List<Client> getAllClients() {
        return clientDao.findAll();
    }

    public Client updateClient(Long clientId, Client updatedData) {
        // Make sure the client exists before updating
        Client existing = clientDao.findById(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));

        existing.setName(updatedData.getName());
        existing.setPhone(updatedData.getPhone());
        existing.setRiskProfile(updatedData.getRiskProfile());
        if (updatedData.getStatus() != null) {
            existing.setStatus(updatedData.getStatus());
        }
        clientDao.update(existing);
        return existing;
    }

    public void deactivateClient(Long clientId) {
        // Verify client exists before deactivating
        clientDao.findById(clientId)
            .orElseThrow(() -> new ClientNotFoundException(clientId));
        clientDao.deactivate(clientId);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Builds a single PortfolioRowDto from a Holding + Asset pair.
     * All P/L arithmetic happens here in one place.
     */
    private PortfolioRowDto buildPortfolioRow(Holding h, Asset asset) {
        BigDecimal qty          = h.getQuantity();
        BigDecimal avgBuyPrice  = h.getAvgBuyPrice();
        BigDecimal currentPrice = asset.getCurrentPrice();

        BigDecimal marketValue    = qty.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal investedAmount = qty.multiply(avgBuyPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unrealizedPnl  = marketValue.subtract(investedAmount);

        // P/L percentage = (currentPrice - avgBuyPrice) / avgBuyPrice * 100
        BigDecimal pnlPct = BigDecimal.ZERO;
        if (avgBuyPrice.compareTo(BigDecimal.ZERO) != 0) {
            pnlPct = currentPrice.subtract(avgBuyPrice)
                .divide(avgBuyPrice, new MathContext(10, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        PortfolioRowDto row = new PortfolioRowDto();
        row.setAssetId(asset.getAssetId());
        row.setSymbol(asset.getSymbol());
        row.setAssetName(asset.getName());
        row.setAssetType(asset.getAssetType().name());
        row.setQuantity(qty);
        row.setAvgBuyPrice(avgBuyPrice);
        row.setCurrentPrice(currentPrice);
        row.setMarketValue(marketValue);
        row.setInvestedAmount(investedAmount);
        row.setUnrealizedPnl(unrealizedPnl);
        row.setPnlPercent(pnlPct);
        return row;
    }
}
