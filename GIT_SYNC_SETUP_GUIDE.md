# Git Sync Automation — Setup Guide

## Overview

This automation keeps two branches in sync. When a PR is merged into the source branch, GitHub Actions automatically detects the changes, simulates the merge, and either applies it directly or raises a conflict PR — without any manual intervention.


---

## How It Works

```
PR merged into master (source branch)
        ↓
GitHub Actions triggers automatically
        ↓
Fetches both branches
Simulates merge in memory (git merge-tree — dry run, touches nothing)
Generates diff_summary.csv
        ↓
    No conflicts?              Conflicts detected?
        ↓                              ↓
Merges master directly         Raises a PR from
into mod-release               master → mod-release
(fully automatic)              (manual review required)
        ↓                              ↓
Commits CSV to                 Commits CSV to
configured branch              configured branch
        ↓                              ↓
Creates GitHub issue           Creates GitHub issue
with sync-success label        with sync-conflict label
Notifies configured users      Notifies configured users
```

---

## Repository File Structure

These files must exist in your repository for the automation to work:

```
your-repo/
└── .github/
    ├── workflows/
    │   └── sync.yml              ← GitHub Actions workflow (must be on default branch)
    └── scripts/
        ├── sync_branches.sh      ← fetches branches, simulates merge, generates diff
        └── parse_diff.py         ← parses raw git diff into structured CSV
```

---

## Prerequisites

Before setting up, make sure you have:

- A GitHub repository with at least two branches to sync
- Admin access to the repository (for Settings)
- Python 3.11 or higher (used by GitHub Actions runner — no local install needed)
- Git 2.38 or higher on the GitHub Actions runner (ubuntu-latest satisfies this)

---

## Step-by-Step Setup

### Step 1: Copy the automation files into your repository

Copy these three files from this project into your repository maintaining the same folder structure:

```
.github/workflows/sync.yml
.github/scripts/sync_branches.sh
.github/scripts/parse_diff.py
```

Commit and push them to your **default branch** (master):

```bash
git add .github/workflows/sync.yml
git add .github/scripts/sync_branches.sh
git add .github/scripts/parse_diff.py
git commit -m "feat: add git sync automation"
git push origin master
```

> **Important:** `sync.yml` must be on the default branch. GitHub Actions only reads workflow files from there.

---

### Step 2: Configure GitHub Actions permissions

Go to your repository on GitHub:

```
Settings → Actions → General
```

Make sure these are enabled:

- ✅ **Read and write permissions** (under Workflow permissions)
- ✅ **Allow GitHub Actions to create and approve pull requests**

Without these, the automation cannot push branches, create PRs, or create issues.

---

### Step 3: Set up Repository Variables

This is where you configure all branch names and notification settings. No hardcoded values exist in the workflow file — everything comes from here.

Go to:

```
Settings → Secrets and variables → Actions → Variables tab → New repository variable
```

Add these four variables:

| Variable Name | Description | Example Value |
|---|---|---|
| `FROM_BRANCH` | Branch changes come FROM | `master` |
| `TO_BRANCH` | Branch changes go INTO | `mod-release` |
| `CSV_BRANCH` | Branch where diff CSV is saved | `mod-release` |
| `NOTIFY_USERS` | GitHub usernames to notify (comma separated) | `user1, user2` |

> **To change branch names later:** Just update the variables here. No code changes, no push to master needed.

> **Multiple notify users:** Comma separated values are supported. Spaces after commas are allowed.
> Example: `YASWANTHBHERI, bheri-yaswanth_pinegit`

---

### Step 4: Create GitHub issue labels

The automation creates GitHub issues with labels to track sync status. These labels need to exist in your repo.

Go to:

```
Issues tab → Labels → New label
```

Create these two labels:

| Label Name | Color | Description |
|---|---|---|
| `sync-success` | `#0075ca` (blue) | Branch sync completed successfully |
| `sync-conflict` | `#e4e669` (yellow) | Branch sync conflict — manual review needed |

> **Note:** The workflow also auto-creates these labels on first run if they don't exist. But creating them manually ensures they are available from the start.

---

### Step 5: Verify the workflow appears in Actions tab

Go to your repository on GitHub:

```
Actions tab
```

You should see **Git Sync Automation** listed under workflows. If it appears, GitHub has successfully detected and registered the workflow.

---

## How Triggers Work

### Automatic trigger

The workflow fires automatically whenever a PR is merged into `FROM_BRANCH` (your configured source branch). It does not trigger for PRs merged into any other branch.

```
PR opened → PR merged into master → workflow triggers → syncs to mod-release
PR opened → PR merged into mod-release → workflow does NOT trigger
```

### Manual trigger

You can trigger the workflow manually for any branch combination without changing the repo variables:

```
GitHub → Actions tab
  → Git Sync Automation
  → Run workflow button (top right)
```

A form appears with these fields:

| Field | What to enter |
|---|---|
| Source branch | Leave blank to use `FROM_BRANCH` variable, or type a branch name |
| Target branch | Leave blank to use `TO_BRANCH` variable, or type a branch name |
| CSV branch | Leave blank to use `CSV_BRANCH` variable, or type a branch name |
| Notify users | Leave blank to use `NOTIFY_USERS` variable, or type usernames |

Leaving all fields blank uses your repo variables — useful for a quick manual sync. Typing values overrides for that single run only.

---

## What Happens After Each Run

### Clean merge (no conflicts)

```
✅ master merged into mod-release automatically
✅ diff_summary.csv committed to configured CSV branch
✅ GitHub issue created with sync-success label
✅ Configured users assigned and notified via email
```

No action required from the team.

### Conflict detected

```
⚠️ mod-release NOT modified — no automatic merge
⚠️ Conflict PR raised: master → mod-release
⚠️ diff_summary.csv committed to configured CSV branch
⚠️ GitHub issue created with sync-conflict label
⚠️ Configured users assigned and notified via email
```

A developer must manually resolve the conflicts and merge the PR.

---

## Notifications

Notifications are sent as **GitHub Issues** assigned to the configured users. GitHub automatically sends an email to each assigned user's registered GitHub email address.

- **Clean merge:** Issue titled `✅ Sync: master → mod-release (PR #X)`
- **Conflict:** Issue titled `⚠️ Conflict: master → mod-release (PR #X)`

To update who receives notifications, go to:

```
Settings → Secrets and variables → Actions → Variables tab
```

Update `NOTIFY_USERS` with the new comma-separated list of GitHub usernames. No code change needed.

---

## Reusing Across Multiple Repositories

The same three files work in any repository. Each repo has its own independent configuration.

For each new repository:

1. Copy `.github/workflows/sync.yml`, `scripts/sync_branches.sh`, `scripts/parse_diff.py`
2. Push to that repo's default branch
3. Set the four repository variables with the correct branch names for that repo
4. Configure Actions permissions (Step 2)
5. Create the two labels (Step 4)

Each repo reads its own variables independently — changing variables in one repo does not affect any other repo.

---

## Branch Structure (Pine Labs Project Reference)

| Branch | Owner | Purpose |
|---|---|---|
| `master` | Pine Labs team | Production source — never push here directly |
| `mod-release` | Our team | Stable base — receives changes from master via this automation |
| `mod` | Our developers | Active Oracle→Postgres conversion work |