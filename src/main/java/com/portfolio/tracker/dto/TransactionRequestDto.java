package com.portfolio.tracker.dto;

import com.portfolio.tracker.model.enums.TxnType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO for submitting a new BUY or SELL transaction.
 * All fields are required and must pass validation before the service layer runs.
 */
public class TransactionRequestDto {

    @NotNull(message = "clientId is required")
    private Long clientId;

    @NotNull(message = "assetId is required")
    private Long assetId;

    @NotNull(message = "txnType is required (BUY or SELL)")
    private TxnType txnType;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "price is required")
    @Positive(message = "price must be greater than 0")
    private BigDecimal price;

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getClientId()                   { return clientId;  }
    public void setClientId(Long clientId)      { this.clientId = clientId; }

    public Long getAssetId()                    { return assetId;   }
    public void setAssetId(Long assetId)        { this.assetId = assetId; }

    public TxnType getTxnType()                 { return txnType;   }
    public void setTxnType(TxnType txnType)     { this.txnType = txnType; }

    public BigDecimal getQuantity()             { return quantity;  }
    public void setQuantity(BigDecimal quantity){ this.quantity = quantity; }

    public BigDecimal getPrice()                { return price;     }
    public void setPrice(BigDecimal price)      { this.price = price; }
}
