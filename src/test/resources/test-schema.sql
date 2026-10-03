-- ============================================================
-- test-schema.sql
-- H2-compatible schema for DAO integration tests.
-- H2 doesn't support MySQL ENUM natively, so we use VARCHAR.
-- ============================================================

DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS holdings;
DROP TABLE IF EXISTS assets;
DROP TABLE IF EXISTS clients;

CREATE TABLE clients (
    client_id    BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)    NOT NULL,
    email        VARCHAR(150)    NOT NULL UNIQUE,
    phone        VARCHAR(15),
    risk_profile VARCHAR(20)     NOT NULL DEFAULT 'MODERATE',
    status       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE assets (
    asset_id      BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    symbol        VARCHAR(20)     NOT NULL UNIQUE,
    name          VARCHAR(100)    NOT NULL,
    asset_type    VARCHAR(20)     NOT NULL,
    current_price DECIMAL(15,4)   NOT NULL,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_assets_price CHECK (current_price > 0)
);

CREATE TABLE holdings (
    holding_id    BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id     BIGINT          NOT NULL,
    asset_id      BIGINT          NOT NULL,
    quantity      DECIMAL(18,4)   NOT NULL,
    avg_buy_price DECIMAL(15,4)   NOT NULL,
    UNIQUE (client_id, asset_id),
    CONSTRAINT chk_holdings_qty   CHECK (quantity      >= 0),
    CONSTRAINT chk_holdings_price CHECK (avg_buy_price >  0),
    FOREIGN KEY (client_id) REFERENCES clients(client_id),
    FOREIGN KEY (asset_id)  REFERENCES assets(asset_id)
);

CREATE TABLE transactions (
    txn_id     BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id  BIGINT          NOT NULL,
    asset_id   BIGINT          NOT NULL,
    txn_type   VARCHAR(10)     NOT NULL,
    quantity   DECIMAL(18,4)   NOT NULL,
    price      DECIMAL(15,4)   NOT NULL,
    txn_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_txn_qty   CHECK (quantity > 0),
    CONSTRAINT chk_txn_price CHECK (price    > 0),
    FOREIGN KEY (client_id) REFERENCES clients(client_id),
    FOREIGN KEY (asset_id)  REFERENCES assets(asset_id)
);
