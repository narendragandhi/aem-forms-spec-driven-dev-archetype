# OpenTelemetry - Observability for AI Agents

> **Status**: This document describes OpenTelemetry support for monitoring agent operations.

## What is OpenTelemetry?

OpenTelemetry (OTEL) provides **structured logging and metrics** for all agent operations. It enables observability into how AI agents are working.

## Why Use OpenTelemetry?

| Without OTEL | With OTEL |
|--------------|-----------|
| No visibility into agent work | Structured logs of all operations |
| Manual metrics | Automatic metrics collection |
| No alerting | Configurable alerts |
| Hard to debug issues | Full trace context |

## Events Emitted

GasTown emits structured events for all operations:

### Session Lifecycle

```json
{
  "event": "session.start",
  "session_id": "abc123",
  "rig": "bmad-showcase",
  "timestamp": "2026-04-06T10:00:00Z"
}
```

### Agent State Changes

```json
{
  "event": "agent.state_change",
  "agent_id": "polecat-xyz",
  "old_state": "working",
  "new_state": "stalled",
  "timestamp": "2026-04-06T10:30:00Z"
}
```

### Beads Operations

```json
{
  "event": "bd.call",
  "command": "bd create",
  "duration_ms": 45,
  "issue_id": "aem-archetype-abc",
  "timestamp": "2026-04-06T10:15:00Z"
}
```

### Convoy Operations

```json
{
  "event": "convoy.create",
  "convoy_id": "convoy-123",
  "bead_count": 5,
  "timestamp": "2026-04-06T10:00:00Z"
}
```

## Metrics Collected

| Metric | Description |
|--------|-------------|
| `gastown.session.starts.total` | Total sessions started |
| `gastown.bd.calls.total` | Total Beads CLI calls |
| `gastown.polecat.spawns.total` | Total agents spawned |
| `gastown.done.total` | Total work items completed |
| `gastown.convoy.creates.total` | Total convoys created |
| `gastown.daemon.restarts.total` | Daemon restarts |

## Configuration

### Environment Variables

```bash
# Logs endpoint (VictoriaLogs by default)
export GT_OTEL_LOGS_URL="http://localhost:9428/insert/jsonline"

# Metrics endpoint (VictoriaMetrics by default)
export GT_OTEL_METRICS_URL="http://localhost:8428/api/v1/write"
```

### Custom Backend

```bash
# Jaeger
export GT_OTEL_TRACES_URL="http://localhost:14268/api/traces"

# Prometheus
export GT_OTEL_METRICS_URL="http://localhost:9090/api/v1/write"
```

## Viewing Data

### VictoriaLogs (Logs)

```bash
# Query logs
curl "http://localhost:9428/select/logsql/query" \
  -d "query=event=session.start"
```

### VictoriaMetrics (Metrics)

```bash
# Query metrics
curl "http://localhost:8428/api/v1/query" \
  -d "query=gastown_session_starts_total"
```

## Integration with Dashboard

The GasTown dashboard shows OTEL data:

```bash
# Start dashboard
gt dashboard --port 8080

# View metrics in browser
# http://localhost:8080/metrics
```

## BMAD Integration

### Track Sprint Metrics

```bash
# View sprint metrics
gt dashboard --sprint sprint-1 --view metrics

# Query completion rate
curl "http://localhost:8428/api/v1/query" \
  -d "query=rate(gastown_done_total[5m])"
```

### Track Agent Productivity

```bash
# Query agent completion rate
curl "http://localhost:8428/api/v1/query" \
  -d "query=sum by (agent) (gastown_done_total)"
```

## Best Practices

1. **Set up early** - Configure OTEL at project start
2. **Use VictoriaStack** - Default backend is easy to run
3. **Check dashboard** - Regular monitoring
4. **Set alerts** - Configure for critical events

## See Also

- [GasTown OTEL Data Model](https://github.com/steveyegge/gastown/blob/main/docs/otel-data-model.md)
- [OTEL Architecture](https://github.com/steveyegge/gastown/blob/main/docs/design/otel)
- [MONITORING.md](./MONITORING.md)
