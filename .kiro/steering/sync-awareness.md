---
inclusion: always
---

# Project Context — Pine Labs Git Sync Automation

## Project background
This is the Pine Labs Credit / Modernization project. The team is performing an
Oracle to PostgreSQL database migration. The codebase uses IBATIS (SQL map XML),
JDBC (JdbcTemplate, datasource config), and JPA (entities, repositories, Hibernate
dialect) frameworks in a Java application.

---

## Branch structure

| Branch | Owner | Purpose |
|---|---|---|
| `master` | Pine Labs team | Production source — never push here directly |
| `release` | Our team | Stable base — receives changes from master automatically |
| `mod` | Our developers | Active Oracle→Postgres conversion work happens here |

Branch names are fully configurable — `master` and `release` are the defaults
but any `from_branch` → `to_branch` combination can be used.

### Sync direction
```
master  ──→  release  ──→  mod
         ↑               ↑
   (this automation)  (separate sprint process)
```

This automation handles only: **master → release** (or any configured branch pair).
The release → mod sync is a separate manual process done per sprint.

---

## How the automation works

When a PR is merged into master (or triggered manually):

```
PR merged into master
        ↓
GitHub Actions triggers (sync.yml)
        ↓
sync_branches.sh runs:
  1. git fetch origin {from_branch}
  2. git fetch origin {to_branch}
  3. git merge-tree (dry run — simulate in memory)
  4. git diff → raw_diff.patch
  5. parse_diff.py → diff_summary.csv + conflict_flag.txt
        ↓
    conflict=false?          conflict=true?
        ↓                         ↓
  Merge directly            Raise a PR
  into {to_branch}          with conflict details
        ↓                         ↓
  Commit CSV to             Commit CSV to
  {csv_branch}              {csv_branch}
        ↓                         ↓
  Send notification         Send notification
  to configured users       to configured users
```

---

## Configurable inputs

All of these can be set as defaults in `sync.yml` (env section) or overridden
at runtime via `workflow_dispatch` inputs:

| Input | Description | Default |
|---|---|---|
| `from_branch` | Branch changes come FROM | `master` |
| `to_branch` | Branch changes go INTO | `release` |
| `csv_branch` | Branch where diff_summary.csv is saved | `release` |
| `notify_users` | Comma-separated GitHub usernames to notify | set in env |

To change defaults permanently, edit the `env` section at the top of `sync.yml`:
```yaml
env:
  DEFAULT_FROM_BRANCH: 'master'
  DEFAULT_TO_BRANCH: 'release'
  DEFAULT_CSV_BRANCH: 'release'
  DEFAULT_NOTIFY_USERS: 'user1,user2'
```

---

## Output artifacts

For every sync event, a CSV is saved to `{csv_branch}` under `.github/sync-diffs/`:

```
.github/sync-diffs/
  PR-{number}-{from_branch}-to-{to_branch}-diff_summary.csv
```

CSV columns:
| Column | Description |
|---|---|
| `file` | File path that changed |
| `change_type` | ADDITION or REMOVAL |
| `line_number` | Line number of the change |
| `content` | The actual line content |
| `conflict` | true or false |

---

## Clean path vs conflict path

**Clean path (conflict=false):**
- GitHub Actions merges `{from_branch}` directly into `{to_branch}`
- CSV committed to `{csv_branch}/.github/sync-diffs/`
- Notification sent to configured users via GitHub issue

**Conflict path (conflict=true):**
- GitHub Actions does NOT touch `{to_branch}`
- A PR is raised: `sync/{from_branch}-to-{to_branch}-PR-{number}` → `{to_branch}`
- PR title: `⚠️ CONFLICT: {from_branch} → {to_branch} — Manual review required`
- CSV committed to `{csv_branch}/.github/sync-diffs/`
- Notification sent to configured users via GitHub issue

---

## Notifications

Notifications are sent as GitHub issues with:
- `sync-success` label for clean merges
- `sync-conflict` label for conflicts

Required one-time setup: create these two labels in your GitHub repo:
```
sync-success  (suggested color: #0075ca)
sync-conflict (suggested color: #e4e669)
```

---

## Repository file structure

```
your-repo/
├── .github/
│   ├── workflows/
│   │   └── sync.yml              ← GitHub Actions workflow
│   └── sync-diffs/               ← diff CSVs saved here (auto-created)
│       └── PR-*-diff_summary.csv
├── .kiro/
│   └── steering/
│       └── sync-awareness.md     ← this file
└── scripts/
    ├── sync_branches.sh          ← fetch, simulate, generate diff
    └── parse_diff.py             ← parse raw diff into CSV
```

---

## Technology stack — what to look for in diffs

### IBATIS
- SQL map XML files (`*SqlMap.xml`, `*Mapper.xml`)
- Tags: `<select>`, `<insert>`, `<update>`, `<delete>`, `<resultMap>`, `<parameterMap>`

### JDBC
- `JdbcTemplate`, `NamedParameterJdbcTemplate`
- Datasource config (driver class, URL)
- `ojdbc` → `postgresql` driver changes

### JPA / Hibernate
- Entity classes (`@Entity`, `@Table`, `@Column`, `@Id`)
- Repository interfaces (`@Query`, named queries)
- `OracleDialect` → `PostgreSQLDialect` changes
- `persistence.xml` or `application.properties` dialect config

---

## Oracle→Postgres key patterns to watch for in diffs

| Oracle (leaving) | Postgres (arriving) |
|---|---|
| `ROWNUM` | `LIMIT` |
| `NVL(x, y)` | `COALESCE(x, y)` |
| `SYSDATE` | `NOW()` |
| `VARCHAR2` | `VARCHAR` |
| `NUMBER` | `NUMERIC` |
| `SEQUENCE.NEXTVAL` | `SERIAL` or sequence |
| `CONNECT BY` | Recursive CTE (`WITH RECURSIVE`) |
| `OracleDialect` | `PostgreSQLDialect` |
| `ojdbc8.jar` | `postgresql.jar` |
| `jdbc:oracle:thin:@` | `jdbc:postgresql://` |

---

## Important rules

1. **Never push directly to `master`** — Pine Labs owns this branch
2. **Branch names are configurable** — never assume fixed branch names in responses
3. **CSV files are the audit trail** — one per sync event, stored in `.github/sync-diffs/`
4. **Conflict = human required** — a PR is raised, nothing is auto-merged when conflicts exist
5. **Notifications go to configured users** — set via `notify_users` input or `DEFAULT_NOTIFY_USERS` env var
