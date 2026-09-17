-- queries.sql
-- Pine Labs Credit Modernization
-- PINE LABS update (via main → master)

-- Get customer transactions
-- PINE LABS: added merchant_id, payment_mode, ORDER BY amount DESC
-- CONFLICTS with mod-release: credit_score, credit_limit, ORDER BY credit_score
SELECT
    t.transaction_id,
    t.amount,
    t.transaction_type,
    t.merchant_id,
    t.payment_mode,
    COALESCE(c.email, 'no-email') AS customer_email,
    TO_CHAR(t.created_at, 'YYYY-MM-DD') AS transaction_date
FROM transactions t
JOIN customers c ON t.customer_id = c.customer_id
WHERE t.created_at >= NOW() - INTERVAL '30 days'
ORDER BY t.amount DESC
LIMIT 100;

-- Get account balance summary
-- PINE LABS: added account_number, bank_code, currency
-- CONFLICTS with mod-release: credit_limit, ORDER BY credit_limit
SELECT
    account_id,
    account_number,
    bank_code,
    COALESCE(balance, 0.00) AS balance,
    currency,
    last_updated
FROM accounts
WHERE status = 'ACTIVE'
ORDER BY balance DESC;
