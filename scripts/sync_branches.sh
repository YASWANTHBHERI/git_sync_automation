#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# sync_branches.sh
#
# Fetches two branches, simulates the merge using git merge-tree (dry run),
# generates a raw diff, and produces:
#   - diff_summary.csv   : structured line-by-line change breakdown
#   - conflict_flag.txt  : conflict=true or conflict=false
#
# Usage  : bash sync_branches.sh <from_branch> <to_branch>
# Example: bash sync_branches.sh master release
# ─────────────────────────────────────────────────────────────────────────────
set -e

# ── Validate arguments ────────────────────────────────────────────────────────
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "❌ ERROR: Both from_branch and to_branch are required."
  echo "   Usage: bash sync_branches.sh <from_branch> <to_branch>"
  exit 1
fi

FROM_BRANCH=$1
TO_BRANCH=$2

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " Git Sync — sync_branches.sh"
echo " From : ${FROM_BRANCH}"
echo " To   : ${TO_BRANCH}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ── Step 1: Fetch both branches from remote ───────────────────────────────────
echo ""
echo "→ [1/4] Fetching branches..."
git fetch origin ${FROM_BRANCH}
echo "  ✓ origin/${FROM_BRANCH}"
git fetch origin ${TO_BRANCH}
echo "  ✓ origin/${TO_BRANCH}"

# ── Step 2: Simulate merge using git merge-tree ───────────────────────────────
# Performs a dry-run 3-way merge entirely in memory.
# Nothing is written to the working directory or any branch.
# Exit code 0 = clean merge is possible
# Exit code 1 = conflicts exist
echo ""
echo "→ [2/4] Simulating merge (git merge-tree dry run)..."

set +e
git merge-tree origin/${TO_BRANCH} origin/${FROM_BRANCH} > merge_result.txt 2>&1
MERGE_EXIT_CODE=$?
set -e

if [ ${MERGE_EXIT_CODE} -eq 0 ]; then
  echo "  ✓ Clean — no conflicts detected"
  echo "conflict=false" > conflict_flag.txt
else
  echo "  ⚠️  Conflicts detected"
  echo "conflict=true" > conflict_flag.txt
fi

# ── Step 3: Generate raw diff between the two branches ───────────────────────
echo ""
echo "→ [3/4] Generating diff..."

set +e
git diff origin/${TO_BRANCH} origin/${FROM_BRANCH} > raw_diff.patch
set -e

if [ ! -s raw_diff.patch ]; then
  echo "  ℹ️  No differences — branches are already in sync"
  # Write header-only CSV so downstream steps can detect no changes
  echo "file,change_type,line_number,content,conflict" > diff_summary.csv
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " Result: Already in sync. Nothing to do."
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 0
fi

echo "  ✓ Diff generated ($(wc -l < raw_diff.patch) lines)"

# ── Step 4: Parse raw diff into structured CSV ────────────────────────────────
echo ""
echo "→ [4/4] Parsing diff into CSV..."

CONFLICT_VAL=$(cat conflict_flag.txt | cut -d'=' -f2)
python3 scripts/parse_diff.py raw_diff.patch diff_summary.csv ${CONFLICT_VAL}

echo "  ✓ CSV rows: $(($(wc -l < diff_summary.csv) - 1))"

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " Done"
echo "   diff_summary.csv  — change breakdown"
echo "   conflict_flag.txt — conflict=${CONFLICT_VAL}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
