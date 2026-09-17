# Test Sync Change

This file was added on the `main` branch to test the Git Sync Automation pipeline.

## Purpose
- Trigger a PR from `main` → `master`
- After merge, GitHub Actions should automatically sync `master` → `mod-release`
- Verify the full automation flow end to end

## Expected flow after PR is merged into master
1. GitHub Actions triggers
2. `sync_branches.sh` runs — fetches branches, simulates merge
3. `diff_summary.csv` generated
4. Clean merge → changes applied to `mod-release` automatically
5. CSV committed to `mod-release/.github/sync-diffs/`
6. Notification sent to YASWANTHBHERI via GitHub issue
