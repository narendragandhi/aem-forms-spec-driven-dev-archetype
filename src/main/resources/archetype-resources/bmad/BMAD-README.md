# Breakthrough Method for Agile Development (BMAD) v6 for AEM Forms

This document outlines the BMAD v6 methodology, tailored for AEM Forms projects. BMAD provides a structured, AI-driven, and agile framework for developing AEM Forms solutions using Agent-as-Code patterns.

## What's New in v6

- **Agent-as-Code**: All agents use YAML frontmatter with `mode: primary` for IDE integration
- **Direct Workflow Invocation**: Use slash commands like `/create-prd`, `/component-dev`
- **BMAD Help Agent**: Intelligent orchestration replacing legacy workflow-init
- **Cross-Platform Support**: Works with Claude Code, GitHub Copilot, and other AI tools

## Quick Start

### Using Slash Commands
```bash
/create-prd          # Create a Product Requirements Document
/domain-research     # Research domain knowledge
/technical-research  # Research technical solutions
/component-dev       # Start component development
/bug-fix             # Start bug fix workflow
```

### Using BMAD Help
```
@workspace Use the BMAD Help agent from bmad/gastown/agents/bmad-help.md.
I need help getting started with my project.
```

## Phases of BMAD

### Phase 01: Business Discovery & Requirements
- **Objective:** Define the form-filling journey, data submission process, and any associated approval workflows.
- **Artifacts:** User stories, process flows, and high-level requirements.
- **Slash Command:** `/create-prd`, `/domain-research`

### Phase 02: Model Definition
- **Objective:** Define the data structures and visual presentation of the forms.
- **Artifacts:**
    - **JSON Schemas:** Located in the `/specs` directory, these define the data model for each form.
    - **Adaptive Form Templates & Themes:** These define the overall look and feel of the forms.

### Phase 03: Architecture & Design
- **Objective:** Design the technical architecture, including any custom components or integrations.
- **Artifacts:** Technical design documents, API specifications.
- **Slash Command:** `/technical-research`

### Phase 04: Development Sprint
- **Objective:** Build and test the AEM Forms solution.
- **Activities:**
    - Create custom React components for Adaptive Forms in `/ui.frontend.react.forms.af`.
    - Build workflows and services in the `/core` module.
    - Assemble and test the final solution.
- **Slash Command:** `/component-dev`, `/bug-fix`

## GasTown Multi-Agent System

GasTown orchestrates multiple AI agents for collaborative AEM development:

| Agent | Purpose | Trigger |
|-------|---------|---------|
| **Mayor AI** | Central orchestrator | "orchestrate", "coordinate" |
| **BMAD Help** | Intelligent guidance | "help", "how do I" |
| **AEM Component Coder** | Component development | "create component" |
| **AEM Test Writer** | Test creation | "write tests" |
| **AEM Code Reviewer** | Code quality review | "review code" |
| **AEM Dispatcher Config** | Dispatcher setup | "configure dispatcher" |
| **AEM Documentation** | Documentation | "write docs" |

## Directory Structure

```
bmad/
├── 00-Project-Initialization/   # Getting started guides
├── 01-Business-Discovery/       # Requirements and user stories
├── 02-Model-Definition/         # Data models and design system
├── 03-Architecture-Design/      # Technical architecture
├── 04-Development-Sprint/       # Development guidelines
├── 05-Testing-and-Deployment/   # Testing and deployment
├── 06-Integrations/             # Integration guides
├── 07-Operations/               # Operational runbooks
├── gastown/                     # Multi-agent orchestration
│   ├── agents/                  # Agent definitions (v6 frontmatter)
│   ├── workflows/               # Workflow YAML files
│   └── bead/                    # BEAD issue tracking
├── tutorials/                   # Role-specific guides
└── BMAD-README.md              # This file
```

## Version Information

- **BMAD Version:** 6.0
- **Archetype Version:** See parent pom.xml
- **AEM Compatibility:** AEM as a Cloud Service 2025+
