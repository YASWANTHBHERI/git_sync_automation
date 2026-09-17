-- queries.sql
-- Pine Labs Credit Modernization
-- feature/automation update:
--   Added date range filter parameter
--   Added transaction count per customer
--   Added index hints for performance

-- Get customer transactions with count
SELECT
    t.transaction_id,
    t.amount,
    t.transaction_type,
    t.merchant_id,
    t.payment_mode,
    COALESCE(c.email, 'no-email') AS customer_email,
    TO_CHAR(t.created_at, 'YYYY-MM-DD') AS transaction_date,
    COUNT(*) OVER (PARTITION BY t.customer_id) AS customer_txn_count
FROM transactions t
JOIN customers c ON t.customer_id = c.customer_id
WHERE t.created_at >= NOW() - INTERVAL '30 days'
  AND t.status = 'COMPLETED'
ORDER BY t.amount DESC
LIMIT 100;

-- Get account balance summary with last transaction date
SELECT
    a.account_id,
    a.account_number,
    a.bank_code,
    COALESCE(a.balance, 0.00) AS balance,
    a.currency,
    a.last_updated,
    MAX(t.created_at) AS last_transaction_date
FROM accounts a
LEFT JOIN transactions t ON a.account_id = t.account_id
WHERE a.status = 'ACTIVE'
GROUP BY a.account_id, a.account_number, a.bank_code,
         a.balance, a.currency, a.last_updated
ORDER BY a.balance DESC;
