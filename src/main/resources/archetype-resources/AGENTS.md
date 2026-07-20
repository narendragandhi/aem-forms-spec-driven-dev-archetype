# Agent Instructions

This project uses **Beads** (`bd`) for AI agent task tracking and **GasTown** for multi-agent orchestration.

## Task Tracking with Beads

If `bd` CLI is installed, use it for all task tracking:

```bash
# Find work with no open blockers (start here every session)
bd ready

# Claim an issue
bd update <issue-id> --claim

# Show issue details
bd show <issue-id>

# Close issue with summary
bd close <issue-id> "What was done"
```

If `bd` is not installed, use the markdown issues in `bmad/gastown/bead/.issues/`.

## Session Start Protocol

1. Run `bd ready` (or check `bmad/gastown/bead/.issues/`) for pending work
2. Check `bmad/gastown/bead/.issues/*/context.json` for agent state
3. Review active workflow via `bmad/gastown/scripts/status.sh`

## Agent Roles

| Agent | Persona File | Trigger |
|-------|-------------|---------|
| **Mayor** | `bmad/gastown/agents/mayor.md` | Orchestrating multi-agent work |
| **Coder** | `bmad/gastown/agents/aem-component-coder.md` | Building components |
| **Tester** | `bmad/gastown/agents/aem-test-writer.md` | Writing tests |
| **Reviewer** | `bmad/gastown/agents/aem-code-reviewer.md` | Reviewing code |
| **Dispatcher** | `bmad/gastown/agents/aem-dispatcher-config.md` | Dispatcher config |
| **Docs** | `bmad/gastown/agents/aem-documentation.md` | Writing docs |
| **BMAD Help** | `bmad/gastown/agents/bmad-help.md` | Methodology guidance |

## Safety Rules

- **Never run `bd init`** on an existing project — it wipes the database
- **Never run `bd init --force`** — destroys all issues
- Use `bd update --claim` instead of `bd edit` (edit opens `$EDITOR`)
- Use `BEADS_DB=test.db bd ...` for manual testing, never the production DB

## Issue ID Format

Beads uses hash-based IDs: `${artifactId}-abc1`
Old BMAD v5 format (`comp-001-impl-001`) is for legacy reference only.
See `bmad/gastown/bead/ID-FORMAT.md` for migration guide.

## Commit Convention

```
[BEAD] {Action}: {issue-id} - {description}
```

Actions: Create, Start, Progress, Complete, Blocked, Unblocked, Cancel

## Key References

- [BEADS-SETUP.md](bmad/gastown/bead/BEADS-SETUP.md) — Install and use `bd`
- [GasTown README](bmad/gastown/README.md) — Orchestration overview
- [BMAD README](bmad/BMAD-README.md) — Methodology overview
- [CONVOY.md](bmad/gastown/CONVOY.md) — Multi-bead work grouping
- [MOLECULES.md](bmad/gastown/MOLECULES.md) — Reusable workflow templates
- [SEANCE.md](bmad/gastown/SEANCE.md) — Session discovery & continuation
- [MONITORING.md](bmad/gastown/MONITORING.md) — Agent health & recovery
