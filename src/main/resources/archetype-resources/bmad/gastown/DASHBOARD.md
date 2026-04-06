# GasTown Dashboard - Workspace Monitoring

> **Status**: This document describes the GasTown web dashboard for workspace monitoring.

## What is the Dashboard?

The GasTown dashboard is a **web-based interface** for monitoring your entire workspace. It provides real-time visibility into agents, convoys, hooks, queues, and issues.

## Why Use the Dashboard?

| Without Dashboard | With Dashboard |
|-------------------|----------------|
| Command-line only | Visual overview |
| Manual status checks | Real-time updates |
| No trend visibility | Historical trends |
| Hard to spot issues | Problem highlighting |

## Starting the Dashboard

### Basic Start

```bash
# Start on default port 8080
gt dashboard

# Check status
gt dashboard status
```

### Custom Port

```bash
# Start on custom port
gt dashboard --port 3000

# Start and auto-open browser
gt dashboard --open
```

## Dashboard Views

### 1. Overview

The default view shows everything in your workspace:

- Active agents
- In-progress convoys
- Recent events
- System health

### 2. Agent Tree

Hierarchical view of all agents grouped by rig and role:

```
bmad-showcase/
├── Mayor (1)
├── Witnesses (1)
├── Coder (2)
│   ├── polecat-abc
│   └── polecat-def
└── Tester (1)
```

### 3. Convoy Panel

Shows in-progress and recently-landed convoys:

- Convoy name
- Progress (X/Y complete)
- Status
- Assigned agents

### 4. Event Stream

Chronological feed of operations:

- Creates
- Completions
- Slings
- Nudges
- And more...

### 5. Problems View

Shows agents needing human intervention:

- GUPP Violations (hooked, no progress)
- Stalled agents
- Zombie sessions (dead)

## Navigation

| Key | Action |
|-----|--------|
| `j/k` | Scroll |
| `Tab` | Switch panels |
| `1/2/3` | Jump to panel |
| `?` | Help |
| `q` | Quit |

## Activity Feed TUI

The activity feed (`gt feed`) is a terminal UI alternative:

```bash
# Launch TUI dashboard
gt feed

# Start in problems view
gt feed --problems

# Plain text output
gt feed --plain

# Open in tmux window
gt feed --window

# Events from last hour
gt feed --since 1h
```

## Features

### Real-time Updates

- Auto-refresh via htmx
- WebSocket for live events
- Manual refresh with `r`

### Command Palette

Run GT commands directly from browser:

```
Ctrl+K
```

Type commands like:
- `bd ready`
- `gt convoy list`
- `gt agents`

### Filtering

Filter by:
- Rig
- Agent type
- Status
- Time range

## Integration with OpenTelemetry

The dashboard shows OTEL data:

```bash
# View metrics
http://localhost:8080/metrics

# View traces
http://localhost:8080/traces
```

## BMAD Integration

### Sprint Dashboard

View sprint-specific metrics:

```bash
# View sprint overview
gt dashboard --sprint sprint-1

# View sprint problems
gt feed --problems --sprint sprint-1
```

### Phase Dashboard

View BMAD phase progress:

```bash
# Architecture phase
gt dashboard --phase architecture

# Development phase
gt dashboard --phase development
```

## Best Practices

1. **Run dashboard** alongside work
2. **Check problems view** regularly
3. **Use command palette** for quick actions
4. **Monitor trends** for productivity insights

## See Also

- [MONITORING.md](./MONITORING.md)
- [TELEMETRY.md](./TELEMETRY.md)
- [GasTown Dashboard Documentation](https://github.com/steveyegge/gastown)
