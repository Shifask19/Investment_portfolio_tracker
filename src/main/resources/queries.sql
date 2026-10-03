-- ============================================================
-- queries.sql  –  8 Analytical Queries with Comments
-- Run AFTER schema.sql, data.sql, views.sql, procedures.sql.
-- ============================================================

USE portfolio_db;

-- ============================================================
-- Q1. Full portfolio snapshot for a specific client
-- Uses: INNER JOIN, filtering, view
-- ============================================================
SELECT
    client_name,
    symbol,
    asset_name,
    asset_type,
    quantity,
    avg_buy_price,
    current_price,
    market_value,
    invested_amount,
    unrealized_pnl,
    pnl_pct
FROM vw_client_portfolio
WHERE client_id = 1        -- change to any client_id
ORDER BY market_value DESC;


-- ============================================================
-- Q2. Summary P/L across all active clients
-- Uses: LEFT JOIN, GROUP BY, HAVING
-- ============================================================
SELECT
    client_name,
    risk_profile,
    total_invested,
    total_market_value,
    total_pnl,
    total_pnl_pct
FROM vw_client_summary
WHERE status = 'ACTIVE'
ORDER BY total_pnl_pct DESC;


-- ============================================================
-- Q3. Clients whose portfolio is DOWN more than 2 %
-- Uses: HAVING on an aggregate, subquery
-- ============================================================
SELECT
    client_name,
    total_pnl_pct
FROM vw_client_summary
WHERE total_pnl_pct < -2.0
ORDER BY total_pnl_pct ASC;


-- ============================================================
-- Q4. Most traded asset by total transaction volume (BUY+SELL)
-- Uses: GROUP BY, ORDER BY, aggregate SUM
-- ============================================================
SELECT
    a.symbol,
    a.name                          AS asset_name,
    COUNT(t.txn_id)                 AS total_transactions,
    SUM(t.quantity)                 AS total_units_traded,
    ROUND(SUM(t.quantity * t.price),2) AS total_traded_value
FROM transactions t
INNER JOIN assets a ON a.asset_id = t.asset_id
GROUP BY a.asset_id, a.symbol, a.name
ORDER BY total_traded_value DESC
LIMIT 5;


-- ============================================================
-- Q5. Transaction history for a specific client in a date range
-- Uses: INNER JOIN, BETWEEN, ORDER BY
-- ============================================================
SELECT
    txn_id,
    txn_date,
    symbol,
    asset_name,
    txn_type,
    quantity,
    price,
    txn_value
FROM vw_transaction_history
WHERE client_id = 2
  AND txn_date BETWEEN '2024-01-01' AND '2024-12-31'
ORDER BY txn_date DESC;


-- ============================================================
-- Q6. Assets currently NOT held by any client (LEFT JOIN pattern)
-- Uses: LEFT JOIN, IS NULL — great pattern to demonstrate
-- ============================================================
SELECT
    a.asset_id,
    a.symbol,
    a.name,
    a.asset_type,
    a.current_price
FROM assets a
LEFT JOIN holdings h ON h.asset_id = a.asset_id
WHERE h.holding_id IS NULL;    -- assets with no holdings row


-- ============================================================
-- Q7. Average buy price vs current price by asset_type
-- Uses: GROUP BY on enum column, ROUND, computed columns
-- ============================================================
SELECT
    a.asset_type,
    COUNT(DISTINCT h.client_id)         AS num_clients_holding,
    ROUND(AVG(h.avg_buy_price), 2)      AS avg_cost_basis,
    ROUND(AVG(a.current_price), 2)      AS avg_current_price,
    ROUND(
        AVG((a.current_price - h.avg_buy_price) / h.avg_buy_price * 100),
        2
    )                                    AS avg_pnl_pct
FROM holdings h
INNER JOIN assets a ON a.asset_id = h.asset_id
GROUP BY a.asset_type
ORDER BY avg_pnl_pct DESC;


-- ============================================================
-- Q8. Top 5 holdings by unrealized P/L across the entire platform
-- Uses: INNER JOIN across 3 tables, computed column, LIMIT
-- ============================================================
SELECT
    c.name                                                       AS client_name,
    a.symbol,
    h.quantity,
    h.avg_buy_price,
    a.current_price,
    ROUND((a.current_price - h.avg_buy_price) * h.quantity, 2)  AS unrealized_pnl
FROM holdings h
INNER JOIN clients c ON c.client_id = h.client_id
INNER JOIN assets  a ON a.asset_id  = h.asset_id
ORDER BY unrealized_pnl DESC
LIMIT 5;


-- ============================================================
-- Q9. Monthly BUY volume per client (pivot-friendly output)
-- Uses: DATE_FORMAT, GROUP BY multiple columns
-- ============================================================
SELECT
    c.name                              AS client_name,
    DATE_FORMAT(t.txn_date, '%Y-%m')    AS month,
    SUM(t.quantity * t.price)           AS monthly_buy_value
FROM transactions t
INNER JOIN clients c ON c.client_id = t.client_id
WHERE t.txn_type = 'BUY'
GROUP BY c.client_id, c.name, DATE_FORMAT(t.txn_date, '%Y-%m')
ORDER BY c.client_id, month;


-- ============================================================
-- Q10. Clients holding more than 2 different asset types
-- Uses: COUNT DISTINCT with HAVING
-- ============================================================
SELECT
    c.client_id,
    c.name                          AS client_name,
    COUNT(DISTINCT a.asset_type)    AS distinct_asset_types,
    COUNT(DISTINCT h.asset_id)      AS total_assets_held
FROM holdings h
INNER JOIN clients c ON c.client_id = h.client_id
INNER JOIN assets  a ON a.asset_id  = h.asset_id
GROUP BY c.client_id, c.name
HAVING COUNT(DISTINCT a.asset_type) > 2
ORDER BY distinct_asset_types DESC;
