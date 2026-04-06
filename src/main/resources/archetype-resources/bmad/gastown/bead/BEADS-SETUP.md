# Beads CLI Setup Guide - ${appName}

This guide covers how to install and use the Beads (`bd`) CLI for AI agent task tracking in your AEM Forms project.

## What is Beads?

Beads is a distributed graph issue tracker for AI coding agents, powered by [Dolt](https://github.com/dolthub/dolt). It provides persistent memory for AI agents to track work across sessions.

## Installation

### macOS / Linux (Homebrew)

```bash
brew install beads
```

### Node.js

```bash
npm install -g @beads/bd
```

### Go

```bash
go install github.com/steveyegge/beads/cmd/bd@latest
```

### Windows

```powershell
irm https://raw.githubusercontent.com/steveyegge/beads/main/scripts/install.ps1 | iex
```

### Quick Install Script (All Platforms)

```bash
curl -fsSL https://raw.githubusercontent.com/steveyegge/beads/main/scripts/install.sh | bash
```

Verify installation:

```bash
bd --version
```

## Initialize Beads in Your Project

```bash
# Navigate to your project
cd my-forms-project

# Initialize Beads (creates .beads/ directory)
bd init
```

This initializes Beads with embedded Dolt database mode by default.

## Essential Commands

### Create Issues

```bash
# Create a new task
bd create "Implement address lookup component" -p 0 -t task

# With description
bd create "Fix form submission bug" -p 1 -t bug --description="Form data not persisting to FDM"
```

### Find Work to Do

```bash
# List tasks with no open blockers (RECOMMENDED)
bd ready

# List all open issues
bd list
```

### Work on Issues

```bash
# Claim an issue (sets assignee + in_progress)
bd update <issue-id> --claim

# Update status
bd update <issue-id> --status in_progress
bd update <issue-id> --status completed
```

### Manage Dependencies

```bash
# Add dependency (blocks child until parent complete)
bd dep add <child-id> <parent-id>

# List dependencies
bd dep list <issue-id>

# Remove dependency
bd dep remove <child-id> <parent-id>
```

### View Issue Details

```bash
bd show <issue-id>
```

### Close Issue

```bash
bd close <issue-id> "Fixed the submission bug"
```

## Issue ID Format

Beads uses hash-based IDs:
- `aem-forms-bmad-archetype-1ec` (epic)
- `aem-forms-bmad-archetype-1ec.1` (subtask)

The prefix is auto-generated from your project name.

## Issue Types

| Type | Description |
|------|-------------|
| `task` | General task |
| `bug` | Bug fix |
| `story` | User story |
| `epic` | Large feature |
| `spike` | Research task |

## Priority Levels

| Priority | Description |
|----------|-------------|
| P0 | Critical - must do |
| P1 | High - important |
| P2 | Medium - should do |
| P3 | Low - nice to have |

## Workflow Example

```bash
# 1. Start your work session
bd ready

# 2. Claim a task
bd update aem-forms-bmad-archetype-1ec --claim

# 3. Create subtasks
bd create "Create Sling Model" -p 1 -t task --parent=aem-forms-bmad-archetype-1ec
bd create "Create HTL template" -p 1 -t task --parent=aem-forms-bmad-archetype-1ec
bd create "Write unit tests" -p 1 -t task --parent=aem-forms-bmad-archetype-1ec

# 4. Link dependencies
bd dep add aem-forms-bmad-archetype-1ec.2 aem-forms-bmad-archetype-1ec.1

# 5. Work on subtasks
bd update aem-forms-bmad-archetype-1ec.1 --claim
# ... do work ...
bd close aem-forms-bmad-archetype-1ec.1 "Created AccordionModel.java"

# 6. Check what's next
bd ready

# 7. Complete parent
bd close aem-forms-bmad-archetype-1ec "Completed address lookup component"
```

## Integration with Claude Code

Add to your `AGENTS.md`:

```markdown
# Agent Instructions

Use 'bd' for task tracking. Run 'bd ready' to find work.
```

Claude Code will automatically read Beads issues and track progress.

## Integration with GasTown

GasTown uses Beads for work tracking:

```bash
# Initialize Beads first
bd init

# Then set up GasTown
gt install ~/gt --git
gt rig add myproject /path/to/your/project
gt mayor attach
```

## Storage Modes

### Embedded Mode (Default)

```bash
bd init
```

Data in `.beads/embeddeddolt/`. Single-writer only.

### Server Mode

```bash
bd init --server
```

Connects to external dolt sql-server. Supports multiple writers.

### Stealth Mode (No Git)

```bash
bd init --stealth
```

For non-git VCS or CI/CD environments.

## Backup & Restore

```bash
# Set up backup
bd backup init /path/to/backup
bd backup sync

# Restore
bd init
bd backup restore --force /path/to/backup
```

## Troubleshooting

```bash
# Check health
bd doctor

# Fix common issues
bd doctor --fix

# View configuration
bd config list
```

## Graph Relationships

Beads supports linking issues:

```bash
# Relates to (bidirectional)
bd relate <issue1> <issue2>

# Duplicates
bd dup <duplicate> <original>

# Supersedes
bd super <new-issue> <old-issue>
```

## Best Practices

1. **Run `bd ready`** - Always check ready tasks at session start
2. **Claim early** - Use `--claim` to atomically claim and start
3. **Link deps** - Use `bd dep add` for dependencies
4. **Close with message** - Always explain completion
5. **Use priorities** - P0 for blockers

## See Also

- [Beads Documentation](https://gastownhall.github.io/beads/)
- [Beads GitHub](https://github.com/steveyegge/beads)
- [BMAD Methodology](../BMAD-README.md)
