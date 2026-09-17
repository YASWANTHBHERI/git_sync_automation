#!/usr/bin/env python3
"""
parse_diff.py
─────────────────────────────────────────────────────────────────
Parses a raw git diff (unified diff format) into a structured CSV
file that Kiro can read and understand.

Usage:
    python3 parse_diff.py <patch_file> <output_csv> <conflict_flag>

Arguments:
    patch_file    : Path to the raw git diff file (e.g. raw_diff.patch)
    output_csv    : Path to write the output CSV (e.g. diff_summary.csv)
    conflict_flag : 'true' or 'false' — from conflict_flag.txt

Output CSV columns:
    file          : File path that changed
    change_type   : ADDITION or REMOVAL
    line_number   : Line number in the file
    content       : The actual line content
    conflict      : true or false (from merge-tree simulation)
─────────────────────────────────────────────────────────────────
"""

import re
import csv
import sys
import os


def parse_diff_to_csv(patch_file: str, output_csv: str, conflict_flag: str) -> None:
    rows = []
    current_file = None
    line_old = 0
    line_new = 0
    is_conflict = conflict_flag.strip().lower() == 'true'

    if not os.path.exists(patch_file):
        print(f"ERROR: Patch file not found: {patch_file}")
        sys.exit(1)

    with open(patch_file, 'r', encoding='utf-8', errors='replace') as f:
        for line in f:
            line = line.rstrip('\n')

            # ── Detect file being changed ─────────────────────────
            if line.startswith('diff --git'):
                match = re.search(r' b/(.+)$', line)
                current_file = match.group(1) if match else None
                line_old = 0
                line_new = 0

            # ── Skip binary files ─────────────────────────────────
            elif line.startswith('Binary files'):
                rows.append({
                    'file': current_file,
                    'change_type': 'BINARY',
                    'line_number': 0,
                    'content': line,
                    'conflict': str(is_conflict).lower()
                })

            # ── Parse hunk header to get starting line numbers ────
            elif line.startswith('@@'):
                match = re.match(r'@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@', line)
                if match:
                    line_old = int(match.group(1))
                    line_new = int(match.group(2))

            # ── Addition line ─────────────────────────────────────
            elif line.startswith('+') and not line.startswith('+++'):
                rows.append({
                    'file': current_file,
                    'change_type': 'ADDITION',
                    'line_number': line_new,
                    'content': line[1:],
                    'conflict': str(is_conflict).lower()
                })
                line_new += 1

            # ── Removal line ──────────────────────────────────────
            elif line.startswith('-') and not line.startswith('---'):
                rows.append({
                    'file': current_file,
                    'change_type': 'REMOVAL',
                    'line_number': line_old,
                    'content': line[1:],
                    'conflict': str(is_conflict).lower()
                })
                line_old += 1

            # ── Context line (unchanged, for reference) ───────────
            elif line.startswith(' '):
                line_old += 1
                line_new += 1

    # ── Write to CSV ──────────────────────────────────────────
    with open(output_csv, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(
            f,
            fieldnames=['file', 'change_type', 'line_number', 'content', 'conflict']
        )
        writer.writeheader()
        writer.writerows(rows)

    # ── Print summary stats ───────────────────────────────────
    additions = sum(1 for r in rows if r['change_type'] == 'ADDITION')
    removals  = sum(1 for r in rows if r['change_type'] == 'REMOVAL')
    files     = len(set(r['file'] for r in rows if r['file']))

    print(f"  Files changed : {files}")
    print(f"  Additions     : {additions}")
    print(f"  Removals      : {removals}")
    print(f"  Total rows    : {len(rows)}")
    print(f"  Conflict flag : {conflict_flag}")
    print(f"  Output        : {output_csv}")


if __name__ == '__main__':
    if len(sys.argv) != 4:
        print("Usage: python3 parse_diff.py <patch_file> <output_csv> <conflict_flag>")
        print("Example: python3 parse_diff.py raw_diff.patch diff_summary.csv false")
        sys.exit(1)

    parse_diff_to_csv(
        patch_file=sys.argv[1],
        output_csv=sys.argv[2],
        conflict_flag=sys.argv[3]
    )
