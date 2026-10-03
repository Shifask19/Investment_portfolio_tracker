-- ============================================================
-- views.sql  –  Database Views for Investment Portfolio Tracker
-- Run AFTER schema.sql and data.sql.
-- ============================================================

USE portfolio_db;

-- ============================================================
-- VIEW: vw_client_portfolio
-- Shows each holding for every client, enriched with
-- current market price and unrealized P/L.
--
-- Unrealized P/L = (current_price - avg_buy_price) * quantity
-- ============================================================
CREATE OR REPLACE VIEW vw_client_portfolio AS
SELECT
    c.client_id,
    c.name                                                      AS client_name,
    c.risk_profile,
    a.asset_id,
    a.symbol,
    a.name                                                      AS asset_name,
    a.asset_type,
    h.quantity,
    h.avg_buy_price,
    a.current_price,
    -- Market value of this position
    ROUND(h.quantity * a.current_price, 2)                     AS market_value,
    -- Total cost paid to acquire this position
    ROUND(h.quantity * h.avg_buy_price, 2)                     AS invested_amount,
    -- Unrealized profit or loss
    ROUND((a.current_price - h.avg_buy_price) * h.quantity, 2) AS unrealized_pnl,
    -- P/L as a percentage of cost
    ROUND(
        ((a.current_price - h.avg_buy_price) / h.avg_buy_price) * 100,
        2
    )                                                           AS pnl_pct
FROM holdings  h
INNER JOIN clients c ON c.client_id = h.client_id   -- only clients that have holdings
INNER JOIN assets  a ON a.asset_id  = h.asset_id;

-- ============================================================
-- VIEW: vw_client_summary
-- One row per client: total invested, total market value,
-- overall P/L, and P/L %.
--
-- Uses LEFT JOIN so clients with ZERO holdings still appear.
-- ============================================================
CREATE OR REPLACE VIEW vw_client_summary AS
SELECT
    c.client_id,
    c.name                                                         AS client_name,
    c.email,
    c.risk_profile,
    c.status,
    -- SUM of cost basis across all positions
    ROUND(COALESCE(SUM(h.quantity * h.avg_buy_price), 0), 2)      AS total_invested,
    -- SUM of current market value
    ROUND(COALESCE(SUM(h.quantity * a.current_price), 0), 2)      AS total_market_value,
    -- Overall P/L in currency
    ROUND(
        COALESCE(SUM(h.quantity * a.current_price), 0)
        - COALESCE(SUM(h.quantity * h.avg_buy_price), 0),
        2
    )                                                              AS total_pnl,
    -- Overall P/L as percentage (handle divide-by-zero with NULLIF)
    ROUND(
        (
            (COALESCE(SUM(h.quantity * a.current_price), 0)
             - COALESCE(SUM(h.quantity * h.avg_buy_price), 0))
            / NULLIF(SUM(h.quantity * h.avg_buy_price), 0)
        ) * 100,
        2
    )                                                              AS total_pnl_pct
FROM clients c
LEFT JOIN holdings h ON h.client_id = c.client_id  -- include clients with no holdings
LEFT JOIN assets   a ON a.asset_id  = h.asset_id
GROUP BY c.client_id, c.name, c.email, c.risk_profile, c.status;

-- ============================================================
-- VIEW: vw_transaction_history
-- Human-readable join across transactions, clients, and assets.
-- ============================================================
CREATE OR REPLACE VIEW vw_transaction_history AS
SELECT
    t.txn_id,
    t.txn_date,
    c.client_id,
    c.name          AS client_name,
    a.asset_id,
    a.symbol,
    a.name          AS asset_name,
    t.txn_type,
    t.quantity,
    t.price,
    -- Total value of this transaction
    ROUND(t.quantity * t.price, 2) AS txn_value
FROM transactions t
INNER JOIN clients c ON c.client_id = t.client_id
INNER JOIN assets  a ON a.asset_id  = t.asset_id
ORDER BY t.txn_date DESC;
