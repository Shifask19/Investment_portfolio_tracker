package com.portfolio.tracker.model;

import com.portfolio.tracker.model.enums.AssetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Concrete subclass of Asset representing a mutual fund.
 *
 * OOP Principle – Inheritance & Polymorphism:
 *   Mutual funds are priced by NAV (Net Asset Value), not a live market price.
 *   getPricingDescription() is overridden to reflect that.
 */
public class MutualFund extends Asset {

    /** Fund house managing this mutual fund, e.g. "Vanguard", "Fidelity" */
    private String fundHouse;

    public MutualFund() {
        super();
        setAssetType(AssetType.MUTUAL_FUND);
    }

    public MutualFund(Long assetId, String symbol, String name, BigDecimal currentPrice,
                      LocalDateTime updatedAt, String fundHouse) {
        super(assetId, symbol, name, AssetType.MUTUAL_FUND, currentPrice, updatedAt);
        this.fundHouse = fundHouse;
    }

    public String getFundHouse()               { return fundHouse;  }
    public void setFundHouse(String fundHouse) { this.fundHouse = fundHouse; }

    @Override
    public String getPricingDescription() {
        return "Mutual Fund by " + fundHouse + " | NAV: " + getCurrentPrice();
    }
}
