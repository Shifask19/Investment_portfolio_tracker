package com.portfolio.tracker.exception;

/**
 * Thrown when a requested asset symbol or ID does not exist.
 */
public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(Long assetId) {
        super("Asset not found with id: " + assetId);
    }

    public AssetNotFoundException(String symbol) {
        super("Asset not found with symbol: " + symbol);
    }
}
