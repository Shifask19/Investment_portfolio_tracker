package com.portfolio.tracker.controller;

import com.portfolio.tracker.dto.ClientSummaryDto;
import com.portfolio.tracker.dto.PortfolioRowDto;
import com.portfolio.tracker.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for portfolio reporting endpoints.
 *
 * Endpoints:
 *   GET /api/clients/{id}/portfolio  — per-holding breakdown
 *   GET /api/clients/{id}/summary    — aggregated totals + P/L
 */
@RestController
@RequestMapping("/api/clients")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    // ── GET /api/clients/{id}/portfolio ───────────────────────────

    /**
     * Returns a list of holdings for the client, each enriched with
     * current price, market value, and unrealized P/L.
     */
    @GetMapping("/{id}/portfolio")
    public ResponseEntity<List<PortfolioRowDto>> getPortfolio(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.getPortfolio(id));
    }

    // ── GET /api/clients/{id}/summary ─────────────────────────────

    /**
     * Returns a single aggregated summary DTO:
     * total invested, total market value, overall P/L and P/L%.
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<ClientSummaryDto> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.getSummary(id));
    }
}
