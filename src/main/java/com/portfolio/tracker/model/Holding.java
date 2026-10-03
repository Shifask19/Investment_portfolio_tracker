package com.portfolio.tracker.model;

import java.math.BigDecimal;

/**
 * Represents a client's current position in a single asset.
 * This is the live snapshot — updated every time a BUY or SELL occurs.
 *
 * Key business field: avgBuyPrice
 *   Recalculated on each BUY using the weighted average formula:
 *   newAvg = (oldQty * oldAvg + buyQty * buyPrice) / (oldQty + buyQty)
 */
public class Holding {

    private Long       holdingId;
    private Long       clientId;
    private Long       assetId;
    private BigDecimal quantity;
    private BigDecimal avgBuyPrice;

    // ── Constructors ─────────────────────────────────────────────

    public Holding() {}

    public Holding(Long holdingId, Long clientId, Long assetId,
                   BigDecimal quantity, BigDecimal avgBuyPrice) {
        this.holdingId   = holdingId;
        this.clientId    = clientId;
        this.assetId     = assetId;
        this.quantity    = quantity;
        this.avgBuyPrice = avgBuyPrice;
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getHoldingId()                       { return holdingId;   }
    public void setHoldingId(Long holdingId)         { this.holdingId = holdingId; }

    public Long getClientId()                        { return clientId;    }
    public void setClientId(Long clientId)           { this.clientId = clientId; }

    public Long getAssetId()                         { return assetId;     }
    public void setAssetId(Long assetId)             { this.assetId = assetId; }

    public BigDecimal getQuantity()                  { return quantity;    }
    public void setQuantity(BigDecimal quantity)     { this.quantity = quantity; }

    public BigDecimal getAvgBuyPrice()               { return avgBuyPrice; }
    public void setAvgBuyPrice(BigDecimal avgBuyPrice) { this.avgBuyPrice = avgBuyPrice; }

    @Override
    public String toString() {
        return "Holding{clientId=" + clientId + ", assetId=" + assetId
               + ", quantity=" + quantity + ", avgBuyPrice=" + avgBuyPrice + "}";
    }
}
