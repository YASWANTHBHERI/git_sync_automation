# Meeting Minutes

**Topic:** Git Sync POC Automation
**Project:** Pine Labs Credit / Modernization

## 1. Meeting Objective

The primary goal of the discussion is to explore and implement an automated process for merging code between Git branches. This automation aims to keep the team's working branches synchronized with the primary base branch (master) with minimal manual intervention.

## 2. Background & Context

- **Branch Dynamics:** The master branch is actively maintained and updated by the Pine Labs team (e.g., adding features, fixing bugs).
- **Working Branch:** The local team is working on a separate "mod" (modernization) branch (e.g., migrating from Oracle to PostgreSQL).
- **The Challenge:** Continuous changes in the master branch need to be regularly synced to the mod branch to ensure the modernization effort remains up-to-date with production features.

## 3. Proposed Automation Workflow

- **Difference Extraction:** The automation must identify the exact differences between a source branch and a target branch.
- **Export Format (CSV/Text):** The system should export these differences into a highly structured, readable format, such as a CSV file.
  - The file must detail exactly what was added and what was removed.
  - It must include specific line numbers corresponding to the changes in the source code.
- **Integration with 'Kiro':** The generated CSV diff file will be fed as an input to a "Kiro skill" (an automation tool/bot). Kiro will read the differences, perform the actual code merge, and automatically raise a Pull Request (PR).

## 4. Technical Requirements & Considerations

- **Dynamic Branch Selection:** The automation script should not have hardcoded branch names (like "master" or "mod"). It must use dynamic parameters (`from_branch` and `to_branch`) to remain flexible across different repositories (both Application and Database repos).
- **Trigger Mechanisms:**
  - **Automated:** Ideally triggered automatically whenever a change is pushed to the master branch.
  - **Manual (On-Demand):** Must also support a manual, one-click trigger to generate the difference file at any given time.
- **Authentication:** Need to determine if standard GitHub personal accounts are sufficient to run these automations (like GitHub Actions), or if a dedicated GitHub Service Account should be created for this specific pipeline.
- **Tooling:** Exploration should focus on tools like GitHub Actions and raw `git diff` commands to extract the necessary data.

## 5. Action Items

| Task Description | Assignee | Deadline |
|---|---|---|
| Investigate GitHub Actions capabilities for automating the diff generation between two branches. | BY / Assignee | Next Day ("Tomorrow") |
| Formulate a method to export git diff results into a structured CSV format (additions, deletions, line numbers). | BY / Assignee | Next Day |
| Evaluate account requirements (Personal vs. Service Account) for executing the GitHub Actions. | BY / Assignee | Next Day |
| Leverage 365 Copilot for accelerated research and draft a proposed solution. | BY / Assignee | Next Day |

*End of Document*
