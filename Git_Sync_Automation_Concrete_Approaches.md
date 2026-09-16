# Git Sync Automation — Concrete Approaches

Each approach below picks one option from Stage 1 (diff extraction), Stage 2 (export format), and Stage 3 (trigger/automation), and walks through it as a complete, working pipeline — not just a list of tools, but how they'd actually talk to each other.

---

## Approach 1: git diff + CSV + GitHub Actions

Think of this as the most "do it with what git already gives you" option.

Here's how it would actually play out. Someone on the Pine Labs team pushes a commit to `master` — maybe a bug fix, maybe a new feature. That push is the trigger. A GitHub Actions workflow is sitting there watching `master` for exactly this, and it wakes up.

The first thing the workflow does is check out the repo, making sure it has both branches available — `master` and `mod`. Then it runs a plain `git diff` between the two, something like `git diff mod...master`, which gives it every line that's different between where the mod branch currently is and where master now stands. This isn't a special tool — it's the same diff command anyone would run locally, just running inside an automated job instead of on someone's laptop.

That diff output is just text though — lines with `+` and `-` in front of them, file paths, hunk headers. Not something you'd want to hand to a bot and expect clean behavior. So the next step in the workflow takes that raw diff and walks through it line by line, pulling out three things for every change: which file it's in, whether it was an addition or a removal, and what line number it happened at. All of that gets written into a CSV — one row per change. That CSV is the deliverable of this stage: a flat, readable table anyone (or any bot) can open and understand at a glance.

Once the CSV exists, the workflow uploads it somewhere Kiro can grab it — could be a workflow artifact, could be pushed to a specific folder in a "diffs" repo, could be sent via webhook. Kiro picks it up, reads through the rows, and uses that as its instruction set for what needs to move from master into mod. It performs the merge and opens a PR.

For the manual side, the same workflow also has a `workflow_dispatch` trigger — meaning someone can go into the Actions tab, click "Run workflow," type in whatever `from_branch` and `to_branch` they want (not hardcoded to master/mod), and get the same CSV generated on demand.

**The flow, step by step:**
1. Push happens on `master` (or someone manually triggers the workflow)
2. GitHub Actions checks out the repo with both branches
3. `git diff from_branch...to_branch` runs
4. A parsing step turns the raw diff into a CSV (file, change type, line number)
5. CSV gets stored/uploaded somewhere accessible
6. Kiro reads the CSV, performs the merge, opens a PR

**Where this is weakest:** if the diff is complex — files renamed, binary files touched, big blocks moved around — hand-parsing raw `git diff` text into CSV rows gets messy fast, and Kiro is working purely off of line numbers and add/remove flags with none of the surrounding context a real merge tool would use.

---

## Approach 2: GitHub Compare API + JSON + GitHub Actions

This one swaps out the "manually parse git diff text" step for something that gives you clean data from the start.

Same trigger as before — a push to `master`, or someone kicking off the workflow manually. But instead of checking out the repo and running `git diff` locally inside the runner, the workflow makes an API call straight to GitHub: `GET /repos/{owner}/{repo}/compare/mod...master`. GitHub itself computes the diff on its servers and hands back a JSON response that already has the structure you'd otherwise have to build by hand — a list of files changed, whether each one was added, modified, removed, or renamed, the actual patch text for each file, and stats like how many lines were added or removed.

Because this comes back as JSON rather than raw diff text, the "turn this into something structured" step becomes much lighter. The workflow just reshapes the JSON into whatever the team wants downstream — could still be a CSV for a human-readable summary, but the underlying data itself is already reliable and doesn't need fragile text parsing. Renames are labeled as renames. Binary files are flagged as binary. None of that gets lost or misread the way it can with raw diff parsing.

That JSON (or a CSV built from it) then gets handed to Kiro the same way as before — as an artifact, a webhook payload, whatever integration point Kiro exposes. Kiro reads it and does the merge + PR.

**The flow, step by step:**
1. Push happens on `master` (or manual trigger via `workflow_dispatch`)
2. GitHub Actions calls the Compare API instead of running git locally
3. GitHub returns structured JSON: files changed, change type, patch content, stats
4. Workflow optionally reshapes this into CSV for readability, but the JSON is the reliable source of truth
5. Kiro consumes the JSON/CSV, performs the merge, opens the PR

**Why this is a step up from Approach 1:** you're not writing your own diff parser and hoping it handles every edge case. GitHub has already done that work for you, and the output is something a program can trust without extra validation.

---

## Approach 3: git merge-tree + Patch file & CSV summary + GitHub App

This is the approach to reach for if the real worry isn't just "what changed" but "will this actually merge cleanly."

Here's the thing the first two approaches don't really deal with: a plain diff tells you what's different between two branches, but it doesn't tell you what happens when you actually try to combine them. Since the `mod` branch is doing a real structural migration (Oracle to PostgreSQL), conflicts with ongoing master changes are pretty likely — someone touching a SQL query in master could easily collide with a table that's already been rewritten for Postgres in mod.

So this approach uses `git merge-tree` instead of a plain diff. It's still a git command, but instead of just comparing two branches, it simulates the merge itself, in memory, without ever touching anyone's working directory. The output tells you not just what's different, but whether combining the two branches would actually succeed cleanly or produce a conflict, and where.

From there, the workflow produces two things, not one: a real patch file (the actual git-native diff, usable directly with `git apply`), and a CSV summary alongside it that's purely for humans and for Kiro's reporting — a readable index of what changed, file by file. The patch file is the thing that actually gets applied; the CSV is there so anyone glancing at the PR (or Kiro itself, when deciding what to say in the PR description) has an easy summary to point to.

Kiro's job here is simpler than in the earlier approaches: it takes the patch file and applies it with `git apply` (leaning on git's own merge logic instead of reconstructing anything from scratch), then opens the PR. If `git merge-tree` flagged a conflict earlier in the pipeline, Kiro doesn't even try to auto-merge — it opens the PR with the conflict markers left in place and flags a human to resolve it, along with the CSV summary attached so the reviewer has context without digging through the raw diff.

On the automation side, instead of a plain GitHub Actions workflow acting under a personal or shared account, this is run through a **GitHub App** — a dedicated bot identity with narrowly scoped permissions (only the repos it needs, nothing more), so it's not tied to any one person's account and survives someone leaving the team.

**The flow, step by step:**
1. Push happens on `master` (webhook triggers the GitHub App), or someone triggers it manually
2. `git merge-tree` simulates the merge between `mod` and `master` — no working directory changes
3. If it's clean: a patch file is generated, plus a CSV summary for readability
4. If it's not clean: the conflict is flagged right here, before Kiro ever touches it
5. Kiro applies the patch file with `git apply`, attaches the CSV summary to the PR description, and opens the PR
6. If a conflict was flagged in step 3, Kiro opens the PR with conflicts marked and tags a human reviewer instead of auto-merging

**Why this is the most resilient option:** it's the only one of the three that actually plans for what happens when the merge *doesn't* go cleanly — which, given the nature of an Oracle-to-Postgres migration running in parallel with active master development, is going to happen regularly, not occasionally.

---

## Rough sense of trade-offs across the three

Approach 1 is the fastest to stand up since it only uses tools already sitting in every git installation, but it's the most fragile once the diffs get messy. Approach 2 fixes the fragile-parsing problem by leaning on GitHub's own API instead of reinventing diff parsing, and it's not much more work than Approach 1 to build. Approach 3 takes more upfront setup — a GitHub App instead of a simple workflow, plus handling two output formats instead of one — but it's the only one that treats conflicts as a first-class part of the design instead of something that gets discovered later when a PR fails to merge.
