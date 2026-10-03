package com.portfolio.tracker.model;

import com.portfolio.tracker.model.enums.AssetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Concrete subclass of Asset representing an equity stock.
 *
 * OOP Principle – Inheritance & Polymorphism:
 *   Inherits all fields from Asset, overrides getPricingDescription()
 *   to show stock-specific info (exchange-listed market price).
 *
 *   This demonstrates the "is-a" relationship and method overriding.
 */
public class Stock extends Asset {

    /** Exchange where this stock is listed, e.g. "NYSE", "NASDAQ" */
    private String exchange;

    public Stock() {
        super();
        setAssetType(AssetType.STOCK);
    }

    public Stock(Long assetId, String symbol, String name,
                 BigDecimal currentPrice, LocalDateTime updatedAt, String exchange) {
        super(assetId, symbol, name, AssetType.STOCK, currentPrice, updatedAt);
        this.exchange = exchange;
    }

    public String getExchange()               { return exchange; }
    public void setExchange(String exchange)  { this.exchange = exchange; }

    /**
     * Polymorphic override: describes how stocks are priced.
     */
    @Override
    public String getPricingDescription() {
        return "Listed on " + exchange + " | Last traded price: " + getCurrentPrice();
    }
}
