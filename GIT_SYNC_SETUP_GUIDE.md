# Git Sync Automation — Setup Guide

## Overview

This automation keeps two branches in sync by detecting when changes land on a
source branch and automatically merging them into a target branch. If the merge
is clean it happens fully automatically. If there are conflicts a PR is raised
and the team is notified.

```
PR merged into FROM_BRANCH
        ↓
GitHub Actions triggers
        ↓
Simulate merge (git merge-tree)
        ↓
    Clean?              Conflict?
      ↓                     ↓
Merge directly        Raise conflict PR
into TO_BRANCH        notify team
      ↓                     ↓
Commit CSV            Commit CSV
Send notification     Send notification
(sync-success)        (sync-conflict)
```

---

## Repository File Structure

```
your-repo/
├── .github/
│   ├── workflows/
│   │   └── sync.yml              ← GitHub Actions workflow (must be on FROM_BRANCH)
│   └── sync-diffs/               ← diff CSVs saved here (auto-created by automation)
│       └── PR-*-diff_summary.csv
└── scripts/
    ├── sync_branches.sh          ← fetches, simulates, generates diff
    └── parse_diff.py             ← parses raw diff into CSV
```

---

## Prerequisites

Before setting up, make sure you have:

- [ ] A GitHub repository with at least two branches to sync
- [ ] Admin access to the repository (for Settings)
- [ ] Git installed locally
- [ ] Python 3.11+ available (used by GitHub Actions runner — no local install needed)

---

## Step 1: Copy the Automation Files into Your Repo

Copy these three files into your repository maintaining the exact folder structure:

```
.github/workflows/sync.yml
scripts/sync_branches.sh
scripts/parse_diff.py
```

### Important
`sync.yml` **must** be committed to the **default branch** of your repo (usually `master` or `main`). GitHub Actions only reads workflow files from the default branch.

```bash
git add .github/workflows/sync.yml
git add scripts/sync_branches.sh
git add scripts/parse_diff.py
git commit -m "feat: add git sync automation"
git push origin master
```

---

## Step 2: Configure GitHub Actions Permissions

Go to your repository on GitHub:

```
Settings → Actions → General
```

Enable both of these:

- [x] **Read and write permissions** (under Workflow permissions)
- [x] **Allow GitHub Actions to create and approve pull requests**

Without these the workflow cannot push to branches or create PRs/issues.

---

## Step 3: Set Up Repository Variables

All configuration is done through GitHub Repository Variables — no hardcoded
values in the workflow file. This means you can change branches without touching
any code.

Go to:

```
Settings → Secrets and variables → Actions → Variables tab → New repository variable
```

Add these four variables:

| Variable Name | Description | Example |
|---|---|---|
| `FROM_BRANCH` | Branch changes come FROM | `master` |
| `TO_BRANCH` | Branch changes go INTO | `mod-release` |
| `CSV_BRANCH` | Branch where diff CSV is saved | `mod-release` |
| `NOTIFY_USERS` | GitHub usernames to notify (comma-separated) | `user1, user2` |

### Notes on NOTIFY_USERS
- Multiple usernames are supported — separate with commas
- Spaces after commas are allowed: `user1, user2` works fine
- These users will be @mentioned in the issue body and assigned to the issue
- They will receive a GitHub email notification automatically

### To change configuration later
Just update the variable value on GitHub — no code change, no push to master needed.

---

## Step 4: Create Required Labels

The workflow creates these labels automatically on first run. But if you want to
create them manually upfront:

```
GitHub repo → Issues → Labels → New label
```

| Label | Color | Description |
|---|---|---|
| `sync-success` | `#0075ca` (blue) | Branch sync completed successfully |
| `sync-conflict` | `#e4e669` (yellow) | Branch sync conflict — manual review needed |

---

## Step 5: Verify Your Branch Setup

Make sure the branches you configured exist on remote:

```bash
git branch -a | grep -E "FROM_BRANCH|TO_BRANCH"
```

If a branch doesn't exist, create and push it:

```bash
git checkout -b mod-release
git push origin mod-release
```

---

## How the Automation Triggers

### Automatic trigger
The workflow fires automatically when a PR is merged into `FROM_BRANCH`.

```
Developer raises PR → PR gets merged into FROM_BRANCH → workflow triggers
```

The workflow only runs when:
- The PR was actually **merged** (not just closed)
- The PR's **base branch** matches `FROM_BRANCH` variable

This prevents the workflow from triggering on PRs merged into other branches
(e.g. merging into `TO_BRANCH` will not trigger the workflow).

### Manual trigger
You can also trigger the workflow manually for any branch combination:

```
GitHub repo → Actions tab → Git Sync Automation → Run workflow
```

In the form that appears:
- Leave all fields **blank** to use your configured repo variables
- Type a value to **override** for that single run only

This is useful for:
- Testing the automation
- One-off syncs between branches not in the default config
- Re-running after a failure

---

## What Happens on Each Run

### Step-by-step breakdown

| Step | What it does |
|---|---|
| Resolve inputs | Reads repo variables or dispatch inputs, validates all required values |
| Checkout repository | Clones repo with full git history (`fetch-depth: 0`) — required for merge-tree |
| Set up Python | Installs Python 3.11 for CSV generation |
| Ensure labels exist | Creates `sync-success` and `sync-conflict` labels if missing |
| Run sync script | Fetches branches, simulates merge, generates CSV and conflict flag |
| Check results | Determines if there are changes and whether conflicts exist |
| Merge (clean path) | Directly merges `FROM_BRANCH` into `TO_BRANCH` |
| Create conflict PR (conflict path) | Raises PR from `FROM_BRANCH` → `TO_BRANCH` |
| Commit CSV | Saves `diff_summary.csv` to `CSV_BRANCH/.github/sync-diffs/` |
| Send notifications | Creates GitHub issue with `sync-success` or `sync-conflict` label |
| Print summary | Logs final run summary to Actions console |

---

## Output Artifacts

For every sync event a CSV file is saved to `CSV_BRANCH` under `.github/sync-diffs/`:

```
.github/sync-diffs/
  PR-{number}-{from_branch}-to-{to_branch}-diff_summary.csv
```

### CSV columns

| Column | Description | Example |
|---|---|---|
| `file` | File path that changed | `src/dao/UserDao.java` |
| `change_type` | Type of change | `ADDITION` or `REMOVAL` |
| `line_number` | Line number in the file | `42` |
| `content` | The actual line content | `SELECT * FROM users LIMIT 10` |
| `conflict` | Whether this file has a conflict | `true` or `false` |

---

## Clean Path — What Happens

When `git merge-tree` detects no conflicts:

1. `FROM_BRANCH` is merged directly into `TO_BRANCH`
2. Commit message: `sync: merge {FROM} into {TO} (PR #{number}) [auto]`
3. CSV committed to `CSV_BRANCH/.github/sync-diffs/`
4. GitHub issue created with `sync-success` label
5. Configured users notified and assigned to the issue

**No human intervention required.**

---

## Conflict Path — What Happens

When `git merge-tree` detects conflicts:

1. `TO_BRANCH` is **NOT modified** — nothing is auto-merged
2. A PR is raised directly from `FROM_BRANCH` → `TO_BRANCH`
   - Title: `⚠️ CONFLICT: {FROM} → {TO} (PR #{number})`
   - Body contains: conflicting files list, steps to resolve, @mentions
   - If a conflict PR already exists between the same branches it is skipped (no duplicate)
3. CSV committed to `CSV_BRANCH/.github/sync-diffs/`
4. GitHub issue created with `sync-conflict` label
5. Configured users notified and assigned

**Human intervention required to resolve and merge.**

---

## How to Resolve Conflicts Manually

When a conflict PR is raised, follow these steps:

```bash
# 1. Switch to the target branch and pull latest
git checkout <TO_BRANCH>
git pull origin <TO_BRANCH>

# 2. Fetch the source branch
git fetch origin <FROM_BRANCH>

# 3. Attempt the merge — conflicts will appear here
git merge origin/<FROM_BRANCH>

# 4. Open conflicting files and resolve
# Look for conflict markers:
# <<<<<<< HEAD           ← your TO_BRANCH version
# ... your changes ...
# =======                ← separator
# ... incoming changes ...
# >>>>>>> origin/FROM    ← FROM_BRANCH version

# Edit the files to keep the correct version, then:

# 5. Stage resolved files
git add <resolved-file-1> <resolved-file-2>

# 6. Complete the merge commit
git commit -m "fix: resolve merge conflicts from <FROM_BRANCH>"

# 7. Push the resolution
git push origin <TO_BRANCH>

# 8. Close the conflict PR on GitHub (it no longer needs merging)
```

---

## Notifications

Notifications are sent as GitHub issues.

### sync-success issue (clean merge)
```
Title:  ✅ Sync: master → mod-release (PR #42)
Label:  sync-success
Body:   Merge completed. Diff summary location. @mentions.
```

### sync-conflict issue (conflict detected)
```
Title:  ⚠️ Conflict: master → mod-release (PR #42)
Label:  sync-conflict
Body:   Conflict details. Action required. @mentions.
```

All users listed in `NOTIFY_USERS` are:
- @mentioned in the issue body
- Assigned to the issue
- Sent a GitHub email notification (based on their GitHub notification settings)

---

## Multi-Repo Usage

The same three files work across any number of repositories. Each repo gets its
own independent set of variables.

```
repo-A                    repo-B                    repo-C
─────────────             ─────────────             ─────────────
FROM_BRANCH=master        FROM_BRANCH=main          FROM_BRANCH=develop
TO_BRANCH=staging         TO_BRANCH=release         TO_BRANCH=qa
CSV_BRANCH=staging        CSV_BRANCH=release        CSV_BRANCH=qa
NOTIFY_USERS=alice        NOTIFY_USERS=bob,carol    NOTIFY_USERS=dave
```

Same `sync.yml`, `sync_branches.sh`, `parse_diff.py` — different behavior per repo.

---

## Changing Configuration

### Change branch names
```
GitHub → Settings → Secrets and variables → Actions → Variables tab
Edit FROM_BRANCH or TO_BRANCH → Save
```
Next workflow run picks up the new values automatically.

### Add or remove notification users
```
GitHub → Settings → Secrets and variables → Actions → Variables tab
Edit NOTIFY_USERS → comma-separated usernames → Save
```
Spaces after commas are allowed: `user1, user2, user3`

### Temporarily override for one run
```
GitHub → Actions → Git Sync Automation → Run workflow
Type values in the form fields → Run workflow
```
These override the repo variables for that single run only.

---

## Troubleshooting

### Workflow not triggering on PR merge
- Verify `sync.yml` is on the default branch (master/main)
- Check that `FROM_BRANCH` variable matches the exact branch name the PR merged into
- Confirm GitHub Actions permissions are set to Read and write

### FROM_BRANCH or TO_BRANCH not set error
```
❌ ERROR: FROM_BRANCH is not set.
   Go to: Settings → Secrets and variables → Actions → Variables
   Add variable: FROM_BRANCH = <your branch name>
```
Go to Settings → Variables and add the missing variable.

### Conflict PR not created
- Check Actions logs for the "Create conflict PR" step
- If it shows "Conflict PR already exists" — a PR between those branches is already open, close it first
- Verify `GITHUB_TOKEN` has permission to create PRs (Settings → Actions → General)

### CSV not committed to CSV_BRANCH
- The CSV step uses `if: always()` so it should always run
- Check that `CSV_BRANCH` variable is set and the branch exists on remote
- Check Actions logs for the "Commit diff_summary.csv" step for error details

### Notification issue not created
- The notification step uses `if: always()` so it should always run
- Verify `sync-success` and `sync-conflict` labels exist in your repo
- Check that `GITHUB_TOKEN` has permission to create issues

### Branches already in sync — nothing happens
This is expected behavior. If `FROM_BRANCH` and `TO_BRANCH` are identical the
workflow detects no changes and exits cleanly. No merge, no CSV, no notification.

---

## Important Rules

1. **Never push directly to `FROM_BRANCH`** if it is owned by another team
2. **Never modify `TO_BRANCH` without going through the automation** or manual PR
3. **Branch names are fully dynamic** — always read from repo variables, never hardcoded
4. **CSV files are the audit trail** — one per sync event, stored in `CSV_BRANCH`
5. **Conflict = human required** — nothing is auto-merged when conflicts exist
6. **The workflow only triggers on `FROM_BRANCH` merges** — merges into other branches do not trigger it

---

## Scripts Reference

### sync_branches.sh

```
Usage: bash sync_branches.sh <from_branch> <to_branch>

What it does:
  1. git fetch origin <from_branch>
  2. git fetch origin <to_branch>
  3. git merge-tree — simulate merge in memory (dry run, touches nothing)
  4. git diff — generate raw_diff.patch
  5. python3 parse_diff.py — parse patch into diff_summary.csv

Outputs:
  diff_summary.csv    — structured change breakdown
  conflict_flag.txt   — conflict=true or conflict=false
  raw_diff.patch      — raw git diff (intermediate file)
  merge_result.txt    — merge-tree output (intermediate file)
```

### parse_diff.py

```
Usage: python3 parse_diff.py <patch_file> <output_csv> <conflict_flag>

What it does:
  Parses a unified git diff into a structured CSV with columns:
  file, change_type, line_number, content, conflict

Arguments:
  patch_file    : raw diff file (raw_diff.patch)
  output_csv    : output CSV path (diff_summary.csv)
  conflict_flag : true or false — from conflict_flag.txt
```

---

## Quick Checklist — New Repo Setup

```
☐ Copy sync.yml to .github/workflows/
☐ Copy sync_branches.sh to scripts/
☐ Copy parse_diff.py to scripts/
☐ Commit and push all three files to master/default branch
☐ Settings → Actions → General → Read and write permissions ✓
☐ Settings → Actions → General → Allow creating PRs ✓
☐ Settings → Variables → Add FROM_BRANCH
☐ Settings → Variables → Add TO_BRANCH
☐ Settings → Variables → Add CSV_BRANCH
☐ Settings → Variables → Add NOTIFY_USERS
☐ Verify both FROM_BRANCH and TO_BRANCH exist on remote
☐ Test with manual workflow_dispatch run
```
