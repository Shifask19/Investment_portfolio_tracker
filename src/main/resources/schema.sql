-- ============================================================
-- schema.sql  –  Investment Portfolio Tracker
-- Run this once to create all tables, indexes, and constraints.
-- Database: portfolio_db (MySQL 8)
-- ============================================================

CREATE DATABASE IF NOT EXISTS portfolio_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE portfolio_db;

-- ============================================================
-- TABLE: clients
-- Stores individual investor profiles.
-- ============================================================
CREATE TABLE IF NOT EXISTS clients (
    client_id    BIGINT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100)    NOT NULL,
    email        VARCHAR(150)    NOT NULL,
    phone        VARCHAR(15),
    -- CONSERVATIVE, MODERATE, AGGRESSIVE - stored as string enum
    risk_profile ENUM('CONSERVATIVE','MODERATE','AGGRESSIVE') NOT NULL DEFAULT 'MODERATE',
    -- ACTIVE / INACTIVE - soft-delete instead of hard-delete to preserve history
    status       ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (client_id),
    UNIQUE KEY uq_clients_email (email)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: assets
-- Master list of investable instruments.
-- ============================================================
CREATE TABLE IF NOT EXISTS assets (
    asset_id      BIGINT          NOT NULL AUTO_INCREMENT,
    symbol        VARCHAR(20)     NOT NULL,
    name          VARCHAR(100)    NOT NULL,
    -- STOCK, MUTUAL_FUND, BOND, ETF
    asset_type    ENUM('STOCK','MUTUAL_FUND','BOND','ETF') NOT NULL,
    current_price DECIMAL(15,4)   NOT NULL,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (asset_id),
    UNIQUE KEY uq_assets_symbol (symbol),

    -- Price must always be positive
    CONSTRAINT chk_assets_price CHECK (current_price > 0)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: holdings
-- Tracks how much of each asset a client currently owns.
-- Quantities and prices here are always the CURRENT state
-- (updated after every BUY/SELL transaction).
-- ============================================================
CREATE TABLE IF NOT EXISTS holdings (
    holding_id    BIGINT          NOT NULL AUTO_INCREMENT,
    client_id     BIGINT          NOT NULL,
    asset_id      BIGINT          NOT NULL,
    quantity      DECIMAL(18,4)   NOT NULL,
    avg_buy_price DECIMAL(15,4)   NOT NULL,

    PRIMARY KEY (holding_id),

    -- One row per client-asset pair
    UNIQUE KEY uq_holdings_client_asset (client_id, asset_id),

    -- ON DELETE RESTRICT: we do NOT want to silently remove holdings if a client
    -- is deleted. Deactivate the client instead (soft-delete pattern).
    CONSTRAINT fk_holdings_client
        FOREIGN KEY (client_id) REFERENCES clients(client_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- ON DELETE RESTRICT: removing an asset that has open holdings is dangerous.
    -- Force the operator to resolve holdings first.
    CONSTRAINT fk_holdings_asset
        FOREIGN KEY (asset_id) REFERENCES assets(asset_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT chk_holdings_quantity  CHECK (quantity      >= 0),
    CONSTRAINT chk_holdings_avg_price CHECK (avg_buy_price >  0),

    -- Index FK columns so JOIN/lookup queries stay fast
    INDEX idx_holdings_client_id (client_id),
    INDEX idx_holdings_asset_id  (asset_id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: transactions
-- Immutable ledger of every BUY and SELL event.
-- Never update or delete rows here — it's the audit trail.
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    txn_id     BIGINT          NOT NULL AUTO_INCREMENT,
    client_id  BIGINT          NOT NULL,
    asset_id   BIGINT          NOT NULL,
    txn_type   ENUM('BUY','SELL') NOT NULL,
    quantity   DECIMAL(18,4)   NOT NULL,
    price      DECIMAL(15,4)   NOT NULL,
    txn_date   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (txn_id),

    -- ON DELETE RESTRICT: same reasoning as holdings — preserve the audit trail.
    CONSTRAINT fk_txn_client
        FOREIGN KEY (client_id) REFERENCES clients(client_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_txn_asset
        FOREIGN KEY (asset_id) REFERENCES assets(asset_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- Business rule: you cannot transact zero or negative units / price
    CONSTRAINT chk_txn_quantity CHECK (quantity > 0),
    CONSTRAINT chk_txn_price    CHECK (price    > 0),

    -- Indexes that support the most common query filters
    INDEX idx_txn_client_id  (client_id),
    INDEX idx_txn_asset_id   (asset_id),
    INDEX idx_txn_date       (txn_date),
    INDEX idx_txn_type       (txn_type)
) ENGINE=InnoDB;
