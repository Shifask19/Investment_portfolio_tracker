package com.portfolio.tracker.dao;

import com.portfolio.tracker.model.Asset;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Asset persistence.
 */
public interface AssetDao {

    /** Persist a new asset and return it with the generated ID. */
    Asset save(Asset asset);

    /** Find asset by primary key. */
    Optional<Asset> findById(Long assetId);

    /** Find asset by ticker symbol (e.g. "AAPL"). */
    Optional<Asset> findBySymbol(String symbol);

    /** Return all assets in the master list. */
    List<Asset> findAll();

    /** Update only the current_price and updated_at columns. */
    void updatePrice(Long assetId, BigDecimal newPrice);

    /** Full update of mutable fields. */
    void update(Asset asset);
}
