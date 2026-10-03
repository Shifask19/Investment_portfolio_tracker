package com.portfolio.tracker.exception;

import java.math.BigDecimal;

/**
 * Thrown when a SELL order requests more units than the client currently holds.
 * This is the Layer 2 (service-level) check; the trigger in procedures.sql is Layer 3.
 */
public class InsufficientHoldingsException extends RuntimeException {

    public InsufficientHoldingsException(Long clientId, Long assetId,
                                          BigDecimal requested, BigDecimal held) {
        super(String.format(
            "Client %d cannot sell %.4f units of asset %d; only holds %.4f units.",
            clientId, requested, assetId, held
        ));
    }
}
