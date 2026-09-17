-- queries.sql
-- Pine Labs Credit Modernization
-- Updated by Pine Labs team on master

-- Get customer transactions with date filter
-- Added new columns: transaction_type, reference_id
SELECT
    t.transaction_id,
    t.amount,
    t.transaction_type,
    t.reference_id,
    COALESCE(c.email, 'no-email') AS customer_email,
    TO_CHAR(t.created_at, 'YYYY-MM-DD') AS transaction_date
FROM transactions t
JOIN customers c ON t.customer_id = c.customer_id
WHERE t.created_at >= NOW() - INTERVAL '30 days'
ORDER BY t.created_at DESC
LIMIT 100;

-- Get account balance summary
-- Added currency column
SELECT
    account_id,
    COALESCE(balance, 0.00) AS balance,
    currency,
    last_updated
FROM accounts
WHERE status = 'ACTIVE'
ORDER BY balance DESC;
