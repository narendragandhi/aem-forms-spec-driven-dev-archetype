# GasTown: AI Agent Orchestration for AEM Development

GasTown is the orchestration layer in the BMAD methodology, coordinating multiple AI agents to collaboratively work on AEM development tasks.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           BMAD Strategic Layer                               │
│                    (PM Agent, Architect Agent, Human Team)                   │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      GasTown Orchestration Layer                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                           Mayor AI                                       ││
│  │  • Receives tasks from BMAD                                             ││
│  │  • Decomposes into agent-specific work                                  ││
│  │  • Monitors progress and dependencies                                   ││
│  │  • Aggregates results                                                   ││
│  └───────────────────┬───────────────────┬───────────────────┬─────────────┘│
│                      │                   │                   │              │
│         ┌────────────▼────────┐ ┌───────▼───────┐ ┌─────────▼────────┐     │
│         │ AEM Component       │ │ AEM Test      │ │ AEM Code         │     │
│         │ Coder Agent         │ │ Writer Agent  │ │ Reviewer Agent   │     │
│         └────────────┬────────┘ └───────┬───────┘ └─────────┬────────┘     │
└──────────────────────┼──────────────────┼───────────────────┼──────────────┘
                       │                  │                   │
                       ▼                  ▼                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        BEAD Agent Memory Layer                               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │ .issues/coder/   │  │ .issues/tester/  │  │ .issues/reviewer/│          │
│  │ - task-001.md    │  │ - task-001.md    │  │ - task-001.md    │          │
│  │ - context.json   │  │ - context.json   │  │ - context.json   │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
gastown/
├── README.md                    # This file
├── agents/                      # AI agent definitions
│   ├── mayor.md                 # Mayor orchestrator persona
│   ├── aem-component-coder.md   # Component development agent
│   ├── aem-test-writer.md       # Test authoring agent
│   ├── aem-code-reviewer.md     # Code review agent
│   ├── aem-dispatcher-config.md # Dispatcher configuration agent
│   └── aem-documentation.md     # Documentation agent
├── workflows/                   # Executable workflow definitions
│   ├── component-development.yaml
│   ├── integration-development.yaml
│   ├── bug-fix.yaml
│   └── content-migration.yaml
├── bead/                        # BEAD issue tracking
│   ├── .issues/                 # Git-backed issue store
│   ├── README.md                # BEAD overview
│   ├── BEADS-SETUP.md           # Beads CLI setup guide
│   ├── ID-FORMAT.md             # Issue ID format migration
│   └── templates/               # Issue templates
├── CONVOY.md                    # Convoy system for work tracking
├── MOLECULES.md                 # Molecules & Formulas for workflow templates
├── MONITORING.md               # Monitoring tier (Witness/Deacon/Dogs)
├── SEANCE.md                   # Session discovery & continuation
├── TELEMETRY.md                # OpenTelemetry observability
├── DASHBOARD.md                # Web dashboard for workspace monitoring
├── config/                      # GasTown configuration
│   └── gastown.yaml             # Main configuration
└── scripts/                     # Helper scripts
    ├── init-workflow.sh
    └── status.sh
```

## Quick Start

### 1. Initialize GasTown for a Task

```bash
# From project root
./bmad/gastown/scripts/init-workflow.sh component-development "Develop Hero Component"
```

### 2. Using Claude Code as Mayor AI

When using Claude Code, invoke the Mayor persona:

```
@workspace Use the Mayor AI persona from bmad/gastown/agents/mayor.md to orchestrate
the development of a new Hero component following the component-development workflow.
```

### 3. Agent Handoff

Each agent handoff is tracked in BEAD:

```bash
# Check current workflow status
./bmad/gastown/scripts/status.sh
```

## Integration with Claude Code

GasTown is designed to work with Claude Code as the AI backbone:

1. **Mayor AI**: Claude Code reads `agents/mayor.md` for orchestration persona
2. **Specialist Agents**: Claude Code switches context using agent definition files
3. **BEAD Tracking**: Issues stored in `.issues/` directory, committed to Git
4. **Workflow Execution**: YAML workflows guide the orchestration sequence

## Workflow Execution Model

```yaml
# Example: component-development.yaml
name: Component Development
trigger: manual
agents:
  - aem-component-coder
  - aem-test-writer
  - aem-code-reviewer

phases:
  - name: design
    agent: mayor
    actions:
      - decompose_task
      - assign_agents

  - name: implementation
    agent: aem-component-coder
    inputs:
      - bmad/03-Architecture-Design/component-design.md
    outputs:
      - core/src/main/java/**/*Model.java
      - ui.apps/src/main/content/**/components/**

  - name: testing
    agent: aem-test-writer
    depends_on: implementation
    outputs:
      - core/src/test/java/**/*Test.java

  - name: review
    agent: aem-code-reviewer
    depends_on: [implementation, testing]
    outputs:
      - .issues/reviews/*.md
```

## Traceability

| Component | Purpose | Location |
|-----------|---------|----------|
| Mayor AI | Orchestration | `agents/mayor.md` |
| Agent Definitions | Specialist personas | `agents/*.md` |
| Workflows | Execution sequences | `workflows/*.yaml` |
| BEAD Issues | Task tracking | `bead/.issues/` |
| Config | Settings | `config/gastown.yaml` |
