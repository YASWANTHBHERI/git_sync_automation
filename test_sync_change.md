# Test Sync Change

This file was added on the `main` branch to test the Git Sync Automation pipeline.

## Purpose
- Trigger a PR from `main` → `master`
- After merge, GitHub Actions should automatically sync `master` → `mod-release`
- Verify the full automation flow end to end

## MOD-RELEASE TEAM NOTES
- Postgres migration in progress
- Schema version: v2.0
- Target completion: Sprint 5
- DB host updated to: mod-release-db:5432
- All ROWNUM replaced with LIMIT
- All NVL replaced with COALESCE
- All SYSDATE replaced with NOW()

## Status
- [x] JDBC driver updated
- [x] Hibernate dialect updated
- [x] Query syntax converted
- [ ] Integration tests pending
