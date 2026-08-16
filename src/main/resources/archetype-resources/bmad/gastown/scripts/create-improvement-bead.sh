#!/usr/bin/env bash
set -euo pipefail

issues_dir="${1:-bmad/gastown/bead/.issues/inbox}"
signal="${2:-unspecified-signal}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
safe_signal="$(printf '%s' "$signal" | tr -cs 'A-Za-z0-9._-' '-')"
file="${issues_dir}/improvement-${timestamp}-${safe_signal}.md"

mkdir -p "$issues_dir"
cat > "$file" <<EOF
---
type: improvement
status: pending
created: ${timestamp}
source: ${safe_signal}
---

# Continuous improvement: ${signal}

Use bmad/gastown/bead/templates/improvement-task.md. Attach synthetic
reproduction, regression tests, verification results, reviewer approval,
canary evidence, and rollback criteria before changing production.
EOF
printf '%s\n' "$file"
