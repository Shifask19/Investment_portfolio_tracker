package com.portfolio.tracker.model.enums;

/**
 * Investor risk appetite classification.
 * Used to categorize clients and may later drive asset allocation rules.
 */
public enum RiskProfile {
    CONSERVATIVE,   // Prefers bonds, low volatility
    MODERATE,       // Balanced mix
    AGGRESSIVE      // High-risk / high-reward assets
}
