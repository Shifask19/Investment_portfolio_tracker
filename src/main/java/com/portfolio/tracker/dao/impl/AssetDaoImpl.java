package com.portfolio.tracker.dao.impl;

import com.portfolio.tracker.dao.AssetDao;
import com.portfolio.tracker.model.Asset;
import com.portfolio.tracker.model.enums.AssetType;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of AssetDao.
 * All SQL uses PreparedStatement with parameterized queries (no string concat).
 */
@Repository
public class AssetDaoImpl implements AssetDao {

    private final DataSource dataSource;

    public AssetDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ─────────────────────────────────────────────────────────────
    // SAVE
    // ─────────────────────────────────────────────────────────────

    @Override
    public Asset save(Asset asset) {
        String sql = "INSERT INTO assets (symbol, name, asset_type, current_price) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, asset.getSymbol());
            ps.setString(2, asset.getName());
            ps.setString(3, asset.getAssetType().name());
            ps.setBigDecimal(4, asset.getCurrentPrice());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    asset.setAssetId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save asset: " + e.getMessage(), e);
        }
        return asset;
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY ID
    // ─────────────────────────────────────────────────────────────

    @Override
    public Optional<Asset> findById(Long assetId) {
        String sql = "SELECT asset_id, symbol, name, asset_type, current_price, updated_at "
                   + "FROM assets WHERE asset_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, assetId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find asset by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY SYMBOL
    // ─────────────────────────────────────────────────────────────

    @Override
    public Optional<Asset> findBySymbol(String symbol) {
        String sql = "SELECT asset_id, symbol, name, asset_type, current_price, updated_at "
                   + "FROM assets WHERE symbol = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, symbol);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find asset by symbol: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ─────────────────────────────────────────────────────────────
    // FIND ALL
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<Asset> findAll() {
        String sql = "SELECT asset_id, symbol, name, asset_type, current_price, updated_at "
                   + "FROM assets ORDER BY symbol";

        List<Asset> assets = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                assets.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all assets: " + e.getMessage(), e);
        }
        return assets;
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE PRICE
    // ─────────────────────────────────────────────────────────────

    @Override
    public void updatePrice(Long assetId, BigDecimal newPrice) {
        // updated_at is auto-updated by MySQL's ON UPDATE CURRENT_TIMESTAMP
        String sql = "UPDATE assets SET current_price = ? WHERE asset_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, newPrice);
            ps.setLong(2, assetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update asset price: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE (full)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void update(Asset asset) {
        String sql = "UPDATE assets SET symbol = ?, name = ?, asset_type = ?, current_price = ? "
                   + "WHERE asset_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, asset.getSymbol());
            ps.setString(2, asset.getName());
            ps.setString(3, asset.getAssetType().name());
            ps.setBigDecimal(4, asset.getCurrentPrice());
            ps.setLong(5, asset.getAssetId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update asset: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────

    private Asset mapRow(ResultSet rs) throws SQLException {
        Asset a = new Asset();
        a.setAssetId(rs.getLong("asset_id"));
        a.setSymbol(rs.getString("symbol"));
        a.setName(rs.getString("name"));
        a.setAssetType(AssetType.valueOf(rs.getString("asset_type")));
        a.setCurrentPrice(rs.getBigDecimal("current_price"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) {
            a.setUpdatedAt(ts.toLocalDateTime());
        }
        return a;
    }
}
