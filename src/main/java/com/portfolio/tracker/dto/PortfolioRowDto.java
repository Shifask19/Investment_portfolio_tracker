package com.portfolio.tracker.dto;

import java.math.BigDecimal;

/**
 * Read-only DTO representing one holding in a client's portfolio report.
 * Populated by PortfolioService from a JOIN across holdings + assets.
 */
public class PortfolioRowDto {

    private Long       assetId;
    private String     symbol;
    private String     assetName;
    private String     assetType;
    private BigDecimal quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;        // quantity * currentPrice
    private BigDecimal investedAmount;     // quantity * avgBuyPrice
    private BigDecimal unrealizedPnl;      // marketValue - investedAmount
    private BigDecimal pnlPercent;         // (unrealizedPnl / investedAmount) * 100

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getAssetId()                          { return assetId;       }
    public void setAssetId(Long assetId)              { this.assetId = assetId; }

    public String getSymbol()                         { return symbol;        }
    public void setSymbol(String symbol)              { this.symbol = symbol; }

    public String getAssetName()                      { return assetName;     }
    public void setAssetName(String assetName)        { this.assetName = assetName; }

    public String getAssetType()                      { return assetType;     }
    public void setAssetType(String assetType)        { this.assetType = assetType; }

    public BigDecimal getQuantity()                   { return quantity;      }
    public void setQuantity(BigDecimal quantity)      { this.quantity = quantity; }

    public BigDecimal getAvgBuyPrice()                { return avgBuyPrice;   }
    public void setAvgBuyPrice(BigDecimal avgBuyPrice){ this.avgBuyPrice = avgBuyPrice; }

    public BigDecimal getCurrentPrice()               { return currentPrice;  }
    public void setCurrentPrice(BigDecimal p)         { this.currentPrice = p; }

    public BigDecimal getMarketValue()                { return marketValue;   }
    public void setMarketValue(BigDecimal v)          { this.marketValue = v; }

    public BigDecimal getInvestedAmount()             { return investedAmount; }
    public void setInvestedAmount(BigDecimal v)       { this.investedAmount = v; }

    public BigDecimal getUnrealizedPnl()              { return unrealizedPnl; }
    public void setUnrealizedPnl(BigDecimal v)        { this.unrealizedPnl = v; }

    public BigDecimal getPnlPercent()                 { return pnlPercent;    }
    public void setPnlPercent(BigDecimal v)           { this.pnlPercent = v; }
}
