# BEAD Issue Examples

This directory contains example BEAD (Beads) issues that demonstrate how AI agents manage tasks within the BMAD framework.

**Important: `.bead/config.yaml` in this directory is illustrative, not a
real, usable `bd` configuration.** Its schema (`agents:`, `context_sources:`,
`gastown: report_to:`) doesn't match the real `bd` CLI's actual config
format — real `bd` config concerns Dolt storage/sync settings, not agent
role definitions. For a real, verified-working `bd` config and command
set, see `bmad/gastown/bead/BEADS-SETUP.md` — its commands were
cross-checked against a real `bd` session and are accurate. The agent-role
mapping shown here (mayor → coder/tester/reviewer/documenter) is a useful
description of the *intended workflow*, just not something you can point
`bd init` at directly.

## What is BEAD?

BEAD is a Git-backed graph issue tracker designed for AI coding agents. It provides:

- **Persistent Memory**: Context that survives across sessions
- **Dependency Tracking**: Graph-based task dependencies
- **Git Integration**: All issues stored as version-controlled files
- **AI-Optimized Format**: JSON/YAML structured for AI consumption

## Directory Structure

```
bead-examples/
├── README.md                    # This file
├── .bead/                       # BEAD configuration
│   └── config.yaml              # Agent and project settings
└── issues/                      # Issue files organized by hash prefix
    ├── a1b2/
    │   └── a1b2c3d4.yaml        # Hero component implementation
    ├── e5f6/
    │   └── e5f6g7h8.yaml        # Hero unit tests
    └── i9j0/
        └── i9j0k1l2.yaml        # HTL template development
```

## Example Workflow

1. **GasTown Mayor** delegates "Develop Hero Component" to AI Coder Agent
2. AI Coder creates BEAD parent issue with sub-tasks
3. Each sub-task tracks context, decisions, and progress
4. Upon completion, status flows back up to GasTown

## Issue Files

See the `issues/` directory for concrete examples of BEAD issues used in this project.
