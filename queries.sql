-- queries.sql
-- Pine Labs Credit Modernization
-- Updated by MOD team on mod-release — added credit limit queries

-- Get customer transactions
-- MOD team added credit_score and removed reference_id
-- This CONFLICTS with master's addition of transaction_type and reference_id
SELECT
    t.transaction_id,
    t.amount,
    c.credit_score,
    COALESCE(c.email, 'no-email') AS customer_email,
    TO_CHAR(t.created_at, 'YYYY-MM-DD') AS transaction_date
FROM transactions t
JOIN customers c ON t.customer_id = c.customer_id
WHERE t.created_at >= NOW() - INTERVAL '30 days'
ORDER BY c.credit_score DESC
LIMIT 100;

-- Get account balance summary
-- MOD team added credit_limit column
-- This CONFLICTS with master's addition of currency column
SELECT
    account_id,
    COALESCE(balance, 0.00) AS balance,
    credit_limit,
    last_updated
FROM accounts
WHERE status = 'ACTIVE'
ORDER BY credit_limit DESC;
