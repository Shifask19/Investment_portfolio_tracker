-- ============================================================
-- procedures.sql  –  Stored Procedures & Triggers
-- Run AFTER schema.sql and data.sql.
-- ============================================================

USE portfolio_db;

-- ============================================================
-- TRIGGER: trg_before_sell
-- Purpose: Block a SELL transaction BEFORE it is inserted if
--          the client does not hold enough quantity.
-- Why a trigger and not just application code?
--   The trigger is the LAST line of defence. Even if someone
--   bypasses the API and runs SQL directly, the database will
--   reject the oversell. This is defense-in-depth.
-- ============================================================

DROP TRIGGER IF EXISTS trg_before_sell;

DELIMITER $$

CREATE TRIGGER trg_before_sell
BEFORE INSERT ON transactions
FOR EACH ROW
BEGIN
    DECLARE v_held_qty DECIMAL(18,4) DEFAULT 0;

    -- Only run this check for SELL orders
    IF NEW.txn_type = 'SELL' THEN

        -- Find how many units the client currently holds
        SELECT COALESCE(quantity, 0)
        INTO   v_held_qty
        FROM   holdings
        WHERE  client_id = NEW.client_id
          AND  asset_id  = NEW.asset_id;

        -- If the sell quantity exceeds held quantity, raise an error.
        -- SIGNAL SQLSTATE '45000' is the standard way to raise a user-defined
        -- error in MySQL (like throwing an exception in Java).
        IF NEW.quantity > v_held_qty THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'SELL quantity exceeds held quantity';
        END IF;

    END IF;
END$$

DELIMITER ;


-- ============================================================
-- STORED PROCEDURE: sp_get_clients_below_pnl_threshold
-- Purpose: Returns all clients whose total P/L % is below
--          a given threshold (e.g., to flag underperformers).
--
-- Usage example:
--   CALL sp_get_clients_below_pnl_threshold(-5.0);
-- ============================================================

DROP PROCEDURE IF EXISTS sp_get_clients_below_pnl_threshold;

DELIMITER $$

CREATE PROCEDURE sp_get_clients_below_pnl_threshold(IN p_threshold DECIMAL(10,2))
BEGIN
    /*
     * This query re-uses the aggregation logic from vw_client_summary.
     * We cannot reference a view inside a procedure easily with HAVING on
     * computed aliases in all MySQL versions, so we inline the subquery.
     */
    SELECT
        client_id,
        client_name,
        total_invested,
        total_market_value,
        total_pnl,
        total_pnl_pct
    FROM vw_client_summary
    WHERE total_pnl_pct < p_threshold
    ORDER BY total_pnl_pct ASC;
END$$

DELIMITER ;


-- ============================================================
-- STORED PROCEDURE: sp_top_assets_by_market_value
-- Purpose: Returns the top N assets ranked by total market
--          value held across ALL clients.
--
-- Usage example:
--   CALL sp_top_assets_by_market_value(3);
-- ============================================================

DROP PROCEDURE IF EXISTS sp_top_assets_by_market_value;

DELIMITER $$

CREATE PROCEDURE sp_top_assets_by_market_value(IN p_top_n INT)
BEGIN
    SELECT
        a.asset_id,
        a.symbol,
        a.name                                     AS asset_name,
        a.asset_type,
        a.current_price,
        ROUND(SUM(h.quantity), 4)                  AS total_units_held,
        ROUND(SUM(h.quantity * a.current_price), 2) AS total_market_value
    FROM holdings h
    INNER JOIN assets a ON a.asset_id = h.asset_id
    GROUP BY a.asset_id, a.symbol, a.name, a.asset_type, a.current_price
    ORDER BY total_market_value DESC
    LIMIT p_top_n;
END$$

DELIMITER ;
