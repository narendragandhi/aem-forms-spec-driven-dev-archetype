# Convoy System - Work Tracking for Multi-Agent Workflows

> **Status**: This document describes the Convoy system for grouping related Beads issues.

## What is a Convoy?

Convoys bundle multiple Beads issues together for coordinated work tracking. They provide visibility across agents working on related tasks and enable batch operations.

## Why Use Convoys?

| Without Convoy | With Convoy |
|----------------|-------------|
| Individual issues tracked separately | Grouped under single convoy |
| Hard to see overall progress | Single view of all related work |
| Manual coordination | Automated dependency tracking |

## Convoy Concepts

```
┌─────────────────────────────────────────────────────────────┐
│                    CONVOY: Feature X                         │
│  Created: 2026-04-06    Status: in_progress                 │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Beads:                                               │   │
│  │   ├── aem-archetype-abc1 (Implementation) ← in_progress │
│  │   ├── aem-archetype-abc2 (Testing)                  │   │
│  │   └── aem-archetype-abc3 (Review) ← blocked         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Progress: 1/3 complete                                     │
└─────────────────────────────────────────────────────────────┘
```

## Convoy Workflow

### Creating a Convoy

```bash
# Create convoy with multiple beads
gt convoy create "Feature X" aem-archetype-abc1 aem-archetype-abc2 aem-archetype-abc3

# Or create first, then add beads
gt convoy create "Feature X"
gt convoy add aem-archetype-abc1 aem-archetype-abc2
```

### Managing Convoys

```bash
# List all convoys
gt convoy list

# Show convoy details
gt convoy show <convoy-id>

# Add beads to convoy
gt convoy add <convoy-id> <bead-id>

# Remove bead from convoy
gt convoy remove <convoy-id> <bead-id>
```

## Integration with BMAD

Convoys integrate with the BMAD methodology:

### Sprint Convoy

Each BMAD sprint can be tracked as a convoy:

```bash
# Create sprint convoy
gt convoy create "Sprint 1 - Core Forms" \
  aem-archetype-f1k \
  aem-archetype-f2k \
  aem-archetype-f3k
```

### Phase Convoy

BMAD phases can be grouped:

```bash
# Architecture phase
gt convoy create "BMAD Phase 3: Architecture" \
  aem-archetype-3a1 \
  aem-archetype-3a2
```

## Manual Convoy (Without GasTown CLI)

For projects using only Beads:

### YAML Definition

Create a convoy definition file:

```yaml
# .beads/convoys/my-feature.yaml
name: "My Feature"
description: "Complete feature implementation"
beads:
  - id: aem-archetype-abc1
    role: implementation
  - id: aem-archetype-abc2
    role: testing
    depends_on: abc1
  - id: aem-archetype-abc3
    role: review
    depends_on: abc2
```

### Tracking Progress

```bash
# Simple progress check
grep -r "status:.*in_progress" .beads/issues/*/*.md | wc -l

# Check convoy status
cat .beads/convoys/my-feature.yaml
```

## Convoy States

| State | Description |
|-------|-------------|
| `planning` | Convoy being planned |
| `in_progress` | Work actively underway |
| `blocked` | Waiting on external dependency |
| `review` | Awaiting review |
| `completed` | All beads closed |
| `cancelled` | Cancelled |

## Best Practices

1. **Cohesive grouping**: Group related beads (same feature/component)
2. **Size limits**: 3-10 beads per convoy (manageable batch)
3. **Clear naming**: Use descriptive convoy names
4. **Regular updates**: Update status as work progresses

## Example: Complete Feature Convoy

```bash
# 1. Create the convoy
gt convoy create "Address Lookup Feature"

# 2. Add beads to it
bd create "Create AddressLookup Sling Model" -p 1 -t task
bd create "Create HTL template" -p 1 -t task
bd create "Write unit tests" -p 1 -t task

# 3. Link dependencies
bd dep add model test
bd dep add template test
bd dep add test review

# 4. Add to convoy
gt convoy add address-lookup model template test review
```

## Migration from BMAD v5

In BMAD v5, workflows were tracked manually. Convoys provide:

- **Structured grouping**: Automatic based on dependencies
- **Progress visibility**: Real-time completion tracking
- **Batch operations**: Move all related issues together

## See Also

- [GasTown Convoy Documentation](https://github.com/steveyegge/gastown/blob/main/docs/design/convoy/)
- [BEADS-SETUP.md](./BEADS-SETUP.md)
- [BMAD Methodology](../BMAD-README.md)
