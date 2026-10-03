package com.portfolio.tracker.model;

import com.portfolio.tracker.model.enums.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Concrete subclass of Asset representing a fixed-income bond.
 *
 * OOP Principle – Inheritance & Polymorphism:
 *   Bonds have a maturityDate and couponRate not found in other assets.
 *   getPricingDescription() is overridden to surface bond-specific info.
 */
public class Bond extends Asset {

    private LocalDate  maturityDate;
    /** Annual coupon rate, e.g. 6.5 means 6.5% */
    private BigDecimal couponRate;

    public Bond() {
        super();
        setAssetType(AssetType.BOND);
    }

    public Bond(Long assetId, String symbol, String name, BigDecimal currentPrice,
                LocalDateTime updatedAt, LocalDate maturityDate, BigDecimal couponRate) {
        super(assetId, symbol, name, AssetType.BOND, currentPrice, updatedAt);
        this.maturityDate = maturityDate;
        this.couponRate   = couponRate;
    }

    public LocalDate getMaturityDate()                    { return maturityDate;  }
    public void setMaturityDate(LocalDate maturityDate)   { this.maturityDate = maturityDate; }

    public BigDecimal getCouponRate()                     { return couponRate;   }
    public void setCouponRate(BigDecimal couponRate)      { this.couponRate = couponRate; }

    @Override
    public String getPricingDescription() {
        return "Bond | Coupon: " + couponRate + "% | Matures: " + maturityDate
               + " | Current price: " + getCurrentPrice();
    }
}
