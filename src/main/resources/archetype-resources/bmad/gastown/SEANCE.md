# Seance - Session Discovery & Continuation

> **Status**: This document describes the Seance system for discovering and querying previous agent sessions.

## What is Seance?

Seance enables agents to **discover previous sessions** and query their predecessors for context and decisions from earlier work. It solves the "forgetting" problem when agents restart.

## Why Use Seance?

| Without Seance | With Seance |
|----------------|-------------|
| Agent forgets context on restart | Agent discovers predecessor sessions |
| Must re-read entire codebase | Query predecessor for context |
| Lost decisions and reasoning | Audit trail of decisions |

## How It Works

```
Session N starts
     │
     ▼
Seance discovers predecessor sessions
     │
     ▼
Agent queries predecessor: "What did you find?"
     │
     ▼
Agent gets context and continues work
```

## Using Seance

### List Discoverable Sessions

```bash
gt seance
```

Lists all sessions that can be discovered (stored in `.events.jsonl` logs).

### Query a Predecessor

```bash
# Full context conversation
gt seance --talk <session-id>

# One-shot question
gt seance --talk <session-id> -p "What did you find?"
```

### Example

```bash
# 1. Start session
claude --resume

# 2. Seance auto-discovers previous sessions
gt seance
# Output: Found 3 predecessor sessions
#   - session-abc (2026-04-06 10:00)
#   - session-def (2026-04-06 09:00)
#   - session-ghi (2026-04-05 16:00)

# 3. Query predecessor
gt seance --talk session-abc -p "What was the blocker?"
# Output: The API endpoint requires OAuth. 
#         I was working on getting credentials.
```

## Session Discovery

Seance discovers sessions via `.events.jsonl` logs:

```json
{"session_id": "abc123", "started": "2026-04-06T10:00:00Z", "ended": "2026-04-06T11:30:00Z"}
{"session_id": "def456", "started": "2026-04-06T09:00:00Z", "ended": "2026-04-06T09:45:00Z"}
```

### Discovery Criteria

- Session must have `.events.jsonl` log file
- Session must be in same project
- Session must have ended (not active)

## Integration with Beads

Seance works with Beads for full context:

```bash
# Query predecessor about a specific issue
gt seance --talk session-abc -p "What's the status of issue abc1?"

# Get issue context from predecessor
bd show abc1 --history
```

## BMAD Integration

### Sprint Continuity

Use Seance to continue from where previous sprints left off:

```bash
# After weekend break, query predecessor
gt seance --talk last-sprint -p "What was the focus?"

# Get active issues
bd list --status in_progress
```

### Phase Handoff

Use Seance between BMAD phases:

```bash
# Architecture phase complete, start Development
gt seance --talk arch-session -p "What decisions were made?"

# Continue with that context
bd ready
```

## Best Practices

1. **Use `--resume`** with Claude Code for auto-discovery
2. **Query specifically** - Ask targeted questions
3. **Check before starting** - Run `gt seance` at session start
4. **Document decisions** - Agents should log decisions to events

## See Also

- [GasTown Seance Documentation](https://github.com/steveyegge/gastown/blob/main/docs/design/seance.md)
- [BEADS-SETUP.md](./BEADS-SETUP.md)
- [CONVOY.md](./CONVOY.md)
