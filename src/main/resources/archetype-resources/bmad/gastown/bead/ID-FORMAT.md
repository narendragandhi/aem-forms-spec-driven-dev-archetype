# Issue ID Format Migration

> **Status**: This document explains the migration from legacy ID format to Beads format.

## Old Format (BMAD v5)

The old format used descriptive file-based IDs:

```
{workflow-id}-{task-type}-{sequence}.md
```

Examples:
- `comp-001-impl-001.md` - Implementation task 001
- `bug-123-fix-001.md` - Bug fix task
- `DEMO-001-test-001.md` - Test task

## New Format (Beads)

Beads uses hash-based IDs with project prefix:

```
{project-prefix}-{hash}
```

Examples:
- `aem-archetype-1ec` - Main task
- `aem-archetype-1ec.1` - Subtask
- `aem-archetype-bb1` - Another task

### ID Structure

```
aem-forms-bmad-archetype-a3f2dd
└───── project prefix ─────┘└ hash │
                                └─ unique identifier
```

### Subtask Hierarchy

```
aem-forms-bmad-archetype-abc   (parent)
aem-forms-bmad-archetype-abc.1 (child 1)
aem-forms-bmad-archetype-abc.2 (child 2)
aem-forms-bmad-archetype-abc.2.1 (grandchild)
```

## ID Prefix

The prefix is auto-generated from your project name during `bd init`. You can customize:

```bash
# Set custom prefix
bd config set issue_prefix myproject
```

## Migration Guide

### For Archetype Templates

The markdown files in this archetype are **examples/templates**, not actual issues. They demonstrate the format and structure for AI agents to understand.

### For Generated Projects

Projects generated from this archetype use the Beads CLI:

```bash
# Initialize Beads (auto-sets prefix)
bd init

# Create issues (auto-generates IDs)
bd create "My task" -p 1 -t task
# Output: aem-forms-bmad-archetype-xyz created
```

### ID Mapping

| Old Format | New Format | Notes |
|------------|------------|-------|
| `comp-001-impl-001` | `aem-archetype-abc` | Epic-level |
| `comp-001-impl-001.1` | `aem-archetype-abc.1` | Subtask |
| `DEMO-001-test-001` | `aem-archetype-def` | New issue |

## Using the New Format

### Find Ready Work

```bash
bd ready
```

Lists all open issues with no blockers - all using new format.

### Work with Issues

```bash
# Show issue
bd show aem-archetype-abc

# Update status
bd update aem-archetype-abc --status in_progress

# Add dependency
bd dep add aem-archetype-abc aem-archetype-def

# Close issue
bd close aem-archetype-abc "Completed"
```

## Best Practices

1. **Don't manually create IDs** - Let Beads generate them
2. **Use `bd ready`** - Find unblocked work quickly
3. **Claim with `--claim`** - Atomic claim + in_progress
4. **Use meaningful titles** - IDs are for reference, titles for humans

## See Also

- [BEADS-SETUP.md](./BEADS-SETUP.md)
- [Beads Documentation](https://gastownhall.github.io/beads/)
