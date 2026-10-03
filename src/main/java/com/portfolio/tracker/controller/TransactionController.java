package com.portfolio.tracker.controller;

import com.portfolio.tracker.dto.TransactionRequestDto;
import com.portfolio.tracker.model.Transaction;
import com.portfolio.tracker.model.enums.TxnType;
import com.portfolio.tracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for transactions.
 *
 * Endpoints:
 *   POST /api/transactions
 *     — submit a BUY or SELL order
 *
 *   GET  /api/transactions?clientId=&assetId=&type=&from=&to=
 *     — fetch transaction history with optional filters
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ── POST /api/transactions ────────────────────────────────────

    /**
     * Submits a BUY or SELL.
     * @Valid fires Bean Validation; failures produce a 400 automatically.
     * Business rule failures (inactive client, oversell) throw exceptions
     * caught by GlobalExceptionHandler.
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @Valid @RequestBody TransactionRequestDto dto) {

        Transaction saved = transactionService.processTransaction(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── GET /api/transactions ─────────────────────────────────────

    /**
     * Fetches transaction history with optional filters.
     * All parameters are optional — omit any to skip that filter.
     *
     * Example:
     *   GET /api/transactions?clientId=1&type=BUY&from=2024-01-01T00:00:00
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) TxnType type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<Transaction> result = transactionService.getTransactions(
            clientId, assetId, type, from, to);

        return ResponseEntity.ok(result);
    }
}
