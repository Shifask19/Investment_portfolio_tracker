-- ============================================================
-- data.sql  –  Seed data for Investment Portfolio Tracker
-- Run AFTER schema.sql.
-- ============================================================

USE portfolio_db;

-- ============================================================
-- Clients (5 rows)
-- ============================================================
INSERT INTO clients (name, email, phone, risk_profile, status) VALUES
('Alice Johnson',  'alice@example.com',  '9876543210', 'AGGRESSIVE',   'ACTIVE'),
('Bob Martinez',   'bob@example.com',    '9123456780', 'MODERATE',     'ACTIVE'),
('Carol White',    'carol@example.com',  '9988776655', 'CONSERVATIVE', 'ACTIVE'),
('David Lee',      'david@example.com',  '9001122334', 'MODERATE',     'ACTIVE'),
('Eva Chen',       'eva@example.com',    '9871234560', 'AGGRESSIVE',   'INACTIVE');

-- ============================================================
-- Assets (8 rows)
-- ============================================================
INSERT INTO assets (symbol, name, asset_type, current_price) VALUES
('AAPL',   'Apple Inc.',              'STOCK',       189.5000),
('GOOGL',  'Alphabet Inc.',           'STOCK',       175.3000),
('TSLA',   'Tesla Inc.',              'STOCK',        95.2000),
('NIFTY',  'Nifty 50 Index Fund',     'MUTUAL_FUND',  18.4500),
('USGOV',  'US Treasury Bond 10Y',    'BOND',         97.8000),
('QQQETF', 'Invesco QQQ ETF',         'ETF',         430.1000),
('MSFT',   'Microsoft Corporation',   'STOCK',       415.6000),
('GOLDBND','Gold Bond 2025',          'BOND',         54.2000);

-- ============================================================
-- Transactions (25 rows — single INSERT for valid SQL)
-- NOTE: Holdings are maintained by the application layer.
--       These seed transactions are recorded for history.
--       We insert matching holdings separately below.
-- ============================================================
INSERT INTO transactions (client_id, asset_id, txn_type, quantity, price, txn_date) VALUES
-- Alice (client_id = 1): aggressive investor, trades stocks & ETF
(1, 1, 'BUY',  10, 150.00, '2024-01-10 09:30:00'),
(1, 1, 'BUY',   5, 160.00, '2024-03-15 10:00:00'),
(1, 1, 'SELL',  3, 175.00, '2024-05-20 14:00:00'),
(1, 2, 'BUY',   8, 140.00, '2024-02-01 11:00:00'),
(1, 6, 'BUY',   4, 400.00, '2024-04-10 09:00:00'),
-- Bob (client_id = 2): moderate investor, diversified
(2, 1, 'BUY',   6, 145.00, '2024-01-20 09:00:00'),
(2, 4, 'BUY', 100,  15.00, '2024-02-10 10:30:00'),
(2, 5, 'BUY',  20,  98.00, '2024-03-05 11:00:00'),
(2, 7, 'BUY',   3, 380.00, '2024-04-22 13:00:00'),
(2, 7, 'SELL',  1, 410.00, '2024-06-01 15:30:00'),
-- Carol (client_id = 3): conservative investor, bonds & mutual funds
(3, 5, 'BUY',  50,  99.00, '2024-01-15 10:00:00'),
(3, 8, 'BUY',  30,  50.00, '2024-02-20 09:30:00'),
(3, 4, 'BUY', 200,  14.50, '2024-03-10 10:00:00'),
(3, 5, 'BUY',  20,  97.50, '2024-05-05 11:00:00'),
(3, 8, 'SELL', 10,  53.00, '2024-06-10 14:00:00'),
-- David (client_id = 4): moderate, interested in tech
(4, 3, 'BUY',  15,  80.00, '2024-01-25 09:30:00'),
(4, 3, 'BUY',   5,  88.00, '2024-03-18 10:30:00'),
(4, 3, 'SELL',  8,  92.00, '2024-05-15 13:00:00'),
(4, 7, 'BUY',   4, 370.00, '2024-02-28 10:00:00'),
(4, 1, 'BUY',   7, 155.00, '2024-04-05 09:00:00'),
-- Eva (client_id = 5): inactive account, historical transactions only
(5, 1, 'BUY',  20, 130.00, '2023-11-01 09:00:00'),
(5, 2, 'BUY',  10, 125.00, '2023-11-15 09:30:00'),
(5, 1, 'SELL', 10, 150.00, '2023-12-20 14:00:00'),
(5, 2, 'SELL',  5, 140.00, '2023-12-22 14:30:00'),
(5, 6, 'BUY',   2, 395.00, '2023-12-01 10:00:00');

-- ============================================================
-- Holdings  (current state derived from the transactions above)
-- avg_buy_price formula: total_cost / total_quantity_held
-- ============================================================

-- Alice: AAPL qty=12, avg=( (10*150)+(5*160) ) / 15 = 2300/15 = 153.3333; sold 3 -> qty=12
--        GOOGL qty=8, avg=140.00
--        QQQETF qty=4, avg=400.00
INSERT INTO holdings (client_id, asset_id, quantity, avg_buy_price) VALUES
(1, 1, 12.0000, 153.3333),
(1, 2,  8.0000, 140.0000),
(1, 6,  4.0000, 400.0000);

-- Bob: AAPL qty=6, avg=145.00
--      NIFTY qty=100, avg=15.00
--      USGOV qty=20, avg=98.00
--      MSFT qty=2, avg=380.00 (bought 3, sold 1)
INSERT INTO holdings (client_id, asset_id, quantity, avg_buy_price) VALUES
(2, 1,   6.0000, 145.0000),
(2, 4, 100.0000,  15.0000),
(2, 5,  20.0000,  98.0000),
(2, 7,   2.0000, 380.0000);

-- Carol: USGOV qty=70, avg=( (50*99)+(20*97.5) ) / 70 = 6900/70 = 98.5714
--        GOLDBND qty=20, avg=50.00 (bought 30, sold 10)
--        NIFTY qty=200, avg=14.50
INSERT INTO holdings (client_id, asset_id, quantity, avg_buy_price) VALUES
(3, 5,  70.0000,  98.5714),
(3, 8,  20.0000,  50.0000),
(3, 4, 200.0000,  14.5000);

-- David: TSLA qty=12, avg=( (15*80)+(5*88) ) / 20 = 1640/20 = 82.00; sold 8 -> qty=12
--        MSFT qty=4, avg=370.00
--        AAPL qty=7, avg=155.00
INSERT INTO holdings (client_id, asset_id, quantity, avg_buy_price) VALUES
(4, 3,  12.0000,  82.0000),
(4, 7,   4.0000, 370.0000),
(4, 1,   7.0000, 155.0000);

-- Eva: AAPL qty=10, avg=130.00 (bought 20, sold 10)
--      GOOGL qty=5, avg=125.00 (bought 10, sold 5)
--      QQQETF qty=2, avg=395.00
INSERT INTO holdings (client_id, asset_id, quantity, avg_buy_price) VALUES
(5, 1, 10.0000, 130.0000),
(5, 2,  5.0000, 125.0000),
(5, 6,  2.0000, 395.0000);
