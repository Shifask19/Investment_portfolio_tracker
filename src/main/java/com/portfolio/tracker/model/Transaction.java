package com.portfolio.tracker.model;

import com.portfolio.tracker.model.enums.TxnType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable ledger entry for a single BUY or SELL event.
 *
 * Design Note:
 *   Transactions are never modified or deleted. They form an
 *   append-only audit trail. The holdings table holds the
 *   derived current state.
 */
public class Transaction {

    private Long          txnId;
    private Long          clientId;
    private Long          assetId;
    private TxnType       txnType;
    private BigDecimal    quantity;
    private BigDecimal    price;
    private LocalDateTime txnDate;

    // ── Constructors ─────────────────────────────────────────────

    public Transaction() {}

    public Transaction(Long txnId, Long clientId, Long assetId, TxnType txnType,
                       BigDecimal quantity, BigDecimal price, LocalDateTime txnDate) {
        this.txnId    = txnId;
        this.clientId = clientId;
        this.assetId  = assetId;
        this.txnType  = txnType;
        this.quantity = quantity;
        this.price    = price;
        this.txnDate  = txnDate;
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getTxnId()                          { return txnId;    }
    public void setTxnId(Long txnId)                { this.txnId = txnId; }

    public Long getClientId()                       { return clientId; }
    public void setClientId(Long clientId)          { this.clientId = clientId; }

    public Long getAssetId()                        { return assetId;  }
    public void setAssetId(Long assetId)            { this.assetId = assetId; }

    public TxnType getTxnType()                     { return txnType;  }
    public void setTxnType(TxnType txnType)         { this.txnType = txnType; }

    public BigDecimal getQuantity()                 { return quantity; }
    public void setQuantity(BigDecimal quantity)    { this.quantity = quantity; }

    public BigDecimal getPrice()                    { return price;    }
    public void setPrice(BigDecimal price)          { this.price = price; }

    public LocalDateTime getTxnDate()               { return txnDate;  }
    public void setTxnDate(LocalDateTime txnDate)   { this.txnDate = txnDate; }

    @Override
    public String toString() {
        return "Transaction{txnId=" + txnId + ", clientId=" + clientId
               + ", type=" + txnType + ", qty=" + quantity + ", price=" + price + "}";
    }
}
