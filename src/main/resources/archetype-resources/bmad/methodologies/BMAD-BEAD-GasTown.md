# BMAD, BEAD, and GasTown Integration Guide

This document explains how the BMAD, BEAD, and GasTown methodologies work together to create an AI-driven development framework for AEM projects.

All three are independent open-source projects, adapted here for AEM
Forms — not invented by this archetype:

| Name | Original source |
|---|---|
| BMAD | [bmad-code-org/BMAD-METHOD](https://github.com/bmad-code-org/BMAD-METHOD) |
| GasTown | [steveyegge/gastown](https://github.com/steveyegge/gastown) |
| Beads / BEAD | [steveyegge/beads](https://github.com/steveyegge/beads) |

## Overview

- **BMAD (Breakthrough Method for Agile Development):** The primary framework that organizes the project lifecycle into distinct phases, each managed by specialized AI agents (PM, Architect, Developer, QA).

- **BEAD (Backlog for Execution by AI Developers):** A git-backed task management system for the AI agents (see `gastown/bead/README.md`). It defines how work is broken down, assigned, and tracked.

- **GasTown:** A conceptual guide and framework for setting up and orchestrating the multi-agent system. It defines the "engine" that runs the BEAD tasks within the BMAD framework.

## Workflow

1.  The **BMAD** process defines the high-level phase (e.g., "Development Sprint").
2.  Within that phase, tasks are created as **BEAD** issues.
3.  The **GasTown** multi-agent system picks up these BEAD tasks and assigns them to the appropriate AI agent for execution.
4.  The AI agent completes the task (e.g., generates code based on a spec) and updates the BEAD issue.
