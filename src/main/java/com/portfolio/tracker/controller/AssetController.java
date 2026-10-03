package com.portfolio.tracker.controller;

import com.portfolio.tracker.dao.AssetDao;
import com.portfolio.tracker.dto.UpdatePriceDto;
import com.portfolio.tracker.exception.AssetNotFoundException;
import com.portfolio.tracker.model.Asset;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for asset management.
 *
 * Endpoints:
 *   GET  /api/assets           — list all assets
 *   GET  /api/assets/{id}      — get one asset
 *   POST /api/assets           — add a new asset
 *   PUT  /api/assets/{id}/price — update current market price
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetDao assetDao;

    public AssetController(AssetDao assetDao) {
        this.assetDao = assetDao;
    }

    // ── GET /api/assets ───────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Asset>> getAllAssets() {
        return ResponseEntity.ok(assetDao.findAll());
    }

    // ── GET /api/assets/{id} ──────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Asset> getAsset(@PathVariable Long id) {
        Asset asset = assetDao.findById(id)
            .orElseThrow(() -> new AssetNotFoundException(id));
        return ResponseEntity.ok(asset);
    }

    // ── POST /api/assets ──────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Asset> createAsset(@Valid @RequestBody Asset asset) {
        Asset saved = assetDao.save(asset);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── PUT /api/assets/{id}/price ────────────────────────────────

    /**
     * Updates only the current_price for an asset.
     * In a real app this would be called by a price-feed service.
     */
    @PutMapping("/{id}/price")
    public ResponseEntity<Asset> updatePrice(@PathVariable Long id,
                                             @Valid @RequestBody UpdatePriceDto dto) {
        // Verify asset exists
        Asset asset = assetDao.findById(id)
            .orElseThrow(() -> new AssetNotFoundException(id));

        assetDao.updatePrice(id, dto.getPrice());
        asset.setCurrentPrice(dto.getPrice());
        return ResponseEntity.ok(asset);
    }
}
