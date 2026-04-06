# Monitoring Tier - Agent Health & Recovery System

> **Status**: This document describes the three-tier watchdog system for agent health monitoring.

## Architecture Overview

GasTown uses a three-tier watchdog chain to keep agents healthy at scale:

```
Daemon (Go process) ← heartbeat every 3 min
    └── Boot (AI agent) ← intelligent triage
        └── Deacon (AI agent) ← continuous patrol
            └── Witnesses & Refineries ← per-rig agents
```

## Tier 1: Witness (Per-Rig)

Each **rig** (project) has a Witness that monitors its agents.

### Responsibilities

- **Health monitoring**: Track agent session status
- **Stuck detection**: Identify agents not making progress
- **Recovery**: Trigger nudge or handoff when stuck
- **Session cleanup**: Manage dead sessions
- **Completion tracking**: Track work completion

### Example: Witness Workflow

```yaml
# .gastown/witness.yaml
rig: bmad-showcase
check_interval: 5m
stuck_threshold: 30m
recovery_actions:
  - nudge
  - handoff
```

### Commands

```bash
# Attach to witness
gt witness attach

# Check witness status
gt witness status

# Manual health check
gt witness health <agent-id>
```

## Tier 2: Deacon (Cross-Rig)

The Deacon runs **continuous patrol cycles** across all rigs.

### Responsibilities

- **Cross-rig monitoring**: Check health across all projects
- **Dog dispatch**: Send Dogs for maintenance tasks
- **Escalation routing**: Route issues to appropriate handlers
- **Pattern detection**: Identify recurring issues

### Example: Deacon Patrol

```bash
# Start patrol
gt patrol start

# Check patrol status
gt patrol status

# Manual patrol run
gt patrol run
```

### Dog Tasks

The Deacon dispatches **Dogs** (maintenance workers):

| Dog | Purpose |
|-----|---------|
| `Boot` | Triage stuck agents |
| `Groomer` | Clean up dead sessions |
| `Messenger` | Deliver handoffs |
| `Sweeper` | Archive completed work |

```bash
# Dispatch a dog
gt dog dispatch Boot --agent <agent-id>
```

## Tier 3: Daemon (System)

The Daemon is the Go process maintaining the overall system.

### Responsibilities

- **Heartbeat**: Monitor all components every 3 minutes
- **Startup**: Initialize all agents on boot
- **Shutdown**: Graceful cleanup on exit
- **Event logging**: Track all system events

### Commands

```bash
# Check daemon status
gt daemon status

# Restart daemon
gt daemon restart

# View logs
gt daemon logs
```

## Escalation System

When agents hit blockers, they can **escalate** rather than waiting:

```bash
# High priority escalation
gt escalate -s HIGH "Need API key for testing"

# Critical escalation
gt escalate -s CRITICAL "Production down"

# List open escalations
gt escalate list

# Acknowledge escalation
gt escalate ack <bead-id>
```

### Severity Levels

| Level | Description | Route |
|-------|-------------|-------|
| CRITICAL (P0) | Production down, immediate action | Overseer → Mayor → Deacon |
| HIGH (P1) | Blocker, work stopped | Mayor → Deacon |
| MEDIUM (P2) | Need help, can continue | Deacon only |

## Agent Health States

### States

| State | Condition | Action |
|-------|-----------|--------|
| `working` | Active, progressing normally | None |
| `idle` | No work assigned | Assign work |
| `stalled` | Reduced progress | Nudge |
| `zombie` | Dead session | Cleanup |
| `gupp` | Hooked, no progress | Intervention |

### Interpreting States

```
gt feed --problems
```

Shows agents grouped by health state:

- **GUPP Violation**: Hooked work with no progress for extended period
- **Stalled**: Hooked work with reduced progress
- **Zombie**: Dead tmux session
- **Working**: Active, progressing normally
- **Idle**: No hooked work

## Recovery Actions

### 1. Nudge

```bash
# Nudge stuck agent
gt nudge <agent-id> "Make progress on current task"
```

### 2. Handoff

```bash
# Refresh context via handoff
gt handoff <agent-id>
```

### 3. Escalate

```bash
# Escalate blocker
gt escalate -s HIGH "Need design approval"
```

### 4. Cancel & Respawn

```bash
# Cancel stuck agent
gt agent cancel <agent-id>

# Respawn new agent
gt agent spawn <issue-id>
```

## Monitoring Dashboard

```bash
# Start dashboard
gt dashboard --port 8080

# Open in browser
gt dashboard --open

# View agent tree
gt feed

# View problems
gt feed --problems
```

## Integration with Beads

The monitoring tier works with Beads for tracking:

```bash
# Create escalation issue
bd create "Agent stuck on X" -p 0 -t task --description="Agent needs help"

# Link to escalation
gt escalate -s HIGH "Agent stuck" --link bd-xxx
```

## BMAD Integration

### Sprint Monitoring

Each sprint can be monitored:

```bash
# Monitor sprint progress
gt feed --sprint sprint-1

# Check for blocked agents
gt feed --problems --sprint sprint-1
```

### Phase Monitoring

BMAD phases can have health checks:

```bash
# Check Phase 3 health
gt witness health --phase architecture
```

## Best Practices

1. **Run `gt feed`** regularly to monitor activity
2. **Check `gt feed --problems`** for stuck agents
3. **Use escalation** for blockers, don't wait
4. **Set up alerts** for critical escalations
5. **Review patterns** to identify recurring issues

## Migration from BMAD v5

In BMAD v5, health monitoring was manual. The monitoring tier provides:

- **Automated detection**: Stuck agents identified automatically
- **Recovery actions**: Nudge, handoff, respawn
- **Escalation routing**: Blockers routed to right person
- **Dashboard visibility**: Real-time system health

## See Also

- [GasTown Architecture](https://github.com/steveyegge/gastown/blob/main/docs/design/architecture.md)
- [Witness Design](https://github.com/steveyegge/gastown/blob/main/docs/design/witness-at-team-lead.md)
- [Escalation Design](https://github.com/steveyegge/gastown/blob/main/docs/design/escalation.md)
- [BEADS-SETUP.md](./BEADS-SETUP.md)
- [BMAD Methodology](../BMAD-README.md)
