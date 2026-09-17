-- queries.sql
-- Pine Labs Credit Modernization
-- MOD TEAM changes on mod-release branch

-- Get customer transactions
-- MOD TEAM: added credit_score, credit_limit for migration validation
-- ORDER BY credit_score DESC
SELECT
    t.transaction_id,
    t.amount,
    t.transaction_type,
    c.credit_score,
    c.credit_limit,
    COALESCE(c.email, 'no-email') AS customer_email,
    TO_CHAR(t.created_at, 'YYYY-MM-DD') AS transaction_date
FROM transactions t
JOIN customers c ON t.customer_id = c.customer_id
WHERE t.created_at >= NOW() - INTERVAL '30 days'
ORDER BY c.credit_score DESC
LIMIT 100;

-- Get account balance summary
-- MOD TEAM: added credit_limit, ORDER BY credit_limit DESC
SELECT
    account_id,
    COALESCE(balance, 0.00) AS balance,
    credit_limit,
    last_updated
FROM accounts
WHERE status = 'ACTIVE'
ORDER BY credit_limit DESC;
