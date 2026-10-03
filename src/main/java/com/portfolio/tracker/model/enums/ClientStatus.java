package com.portfolio.tracker.model.enums;

/**
 * Lifecycle status of a client account.
 * We never hard-delete clients because transactions reference them.
 * Instead, we deactivate them (soft-delete pattern).
 */
public enum ClientStatus {
    ACTIVE,
    INACTIVE
}
