package com.portfolio.tracker.model;

import com.portfolio.tracker.model.enums.AssetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for a tradable financial instrument.
 *
 * OOP Principle – Abstraction / Polymorphism:
 *   Asset is designed to be the base class in a hierarchy.
 *   Concrete subclasses (Stock, Bond, MutualFund, ETF) can override
 *   getPricingStrategy() to implement asset-type-specific pricing logic
 *   (e.g., NAV for mutual funds, yield-based for bonds).
 *   The controller/service layer always works with the Asset base type —
 *   classic Liskov Substitution Principle.
 */
public class Asset {

    private Long          assetId;
    private String        symbol;
    private String        name;
    private AssetType     assetType;
    private BigDecimal    currentPrice;
    private LocalDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────────

    public Asset() {}

    public Asset(Long assetId, String symbol, String name,
                 AssetType assetType, BigDecimal currentPrice, LocalDateTime updatedAt) {
        this.assetId      = assetId;
        this.symbol       = symbol;
        this.name         = name;
        this.assetType    = assetType;
        this.currentPrice = currentPrice;
        this.updatedAt    = updatedAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getAssetId()                       { return assetId;      }
    public void setAssetId(Long assetId)           { this.assetId = assetId; }

    public String getSymbol()                      { return symbol;  }
    public void setSymbol(String symbol)           { this.symbol = symbol; }

    public String getName()                        { return name;   }
    public void setName(String name)               { this.name = name; }

    public AssetType getAssetType()                { return assetType;  }
    public void setAssetType(AssetType assetType)  { this.assetType = assetType; }

    public BigDecimal getCurrentPrice()                        { return currentPrice;  }
    public void setCurrentPrice(BigDecimal currentPrice)       { this.currentPrice = currentPrice; }

    public LocalDateTime getUpdatedAt()                        { return updatedAt;  }
    public void setUpdatedAt(LocalDateTime updatedAt)          { this.updatedAt = updatedAt; }

    /**
     * Hook for subclasses to provide a type-specific pricing description.
     * Override in Stock, Bond, MutualFund subclasses for polymorphism.
     */
    public String getPricingDescription() {
        return "Market price: " + currentPrice;
    }

    @Override
    public String toString() {
        return "Asset{assetId=" + assetId + ", symbol='" + symbol + "', type=" + assetType + "}";
    }
}
