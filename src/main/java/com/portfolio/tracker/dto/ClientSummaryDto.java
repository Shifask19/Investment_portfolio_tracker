package com.portfolio.tracker.dto;

import java.math.BigDecimal;

/**
 * Read-only DTO for the overall portfolio summary of a single client.
 * Aggregates totals across all holdings.
 */
public class ClientSummaryDto {

    private Long       clientId;
    private String     clientName;
    private BigDecimal totalInvested;      // sum of (quantity * avgBuyPrice)
    private BigDecimal totalMarketValue;   // sum of (quantity * currentPrice)
    private BigDecimal totalPnl;           // totalMarketValue - totalInvested
    private BigDecimal totalPnlPercent;    // (totalPnl / totalInvested) * 100

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getClientId()                           { return clientId;        }
    public void setClientId(Long clientId)              { this.clientId = clientId; }

    public String getClientName()                       { return clientName;      }
    public void setClientName(String n)                 { this.clientName = n; }

    public BigDecimal getTotalInvested()                { return totalInvested;   }
    public void setTotalInvested(BigDecimal v)          { this.totalInvested = v; }

    public BigDecimal getTotalMarketValue()             { return totalMarketValue; }
    public void setTotalMarketValue(BigDecimal v)       { this.totalMarketValue = v; }

    public BigDecimal getTotalPnl()                     { return totalPnl;        }
    public void setTotalPnl(BigDecimal v)               { this.totalPnl = v; }

    public BigDecimal getTotalPnlPercent()              { return totalPnlPercent; }
    public void setTotalPnlPercent(BigDecimal v)        { this.totalPnlPercent = v; }
}
