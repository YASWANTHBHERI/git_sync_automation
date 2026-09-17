# Test Sync Change 2

Second test file added on `main` branch to re-test the Git Sync Automation pipeline.

## Changes in this test
- Verifying workflow triggers correctly after previous fixes
- mod-release branch pushed to remote
- scripts committed to master branch
- GitHub Actions write permissions confirmed

## Sample SQL change (simulating Oracle→Postgres migration context)

### Before (Oracle)
```sql
SELECT * FROM users WHERE ROWNUM <= 10;
SELECT NVL(email, 'no-email') FROM customers;
SELECT SYSDATE FROM DUAL;
```

### After (Postgres)
```sql
SELECT * FROM users LIMIT 10;
SELECT COALESCE(email, 'no-email') FROM customers;
SELECT NOW();
```
