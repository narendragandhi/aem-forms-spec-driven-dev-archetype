---
description: Start AEM component development workflow with GasTown orchestration
---

# Component Development Workflow

You are initiating the **Component Development** workflow using GasTown multi-agent orchestration.

## Workflow Overview

This workflow coordinates multiple AI agents to develop a complete AEM component:

1. **Mayor AI** - Orchestrates the workflow
2. **AEM Component Coder** - Implements the component
3. **AEM Test Writer** - Creates comprehensive tests
4. **AEM Code Reviewer** - Reviews for quality and security

## Prerequisites

Before starting, ensure you have:
- Component design specifications in `bmad/03-Architecture-Design/component-design.md`
- Design system tokens in `bmad/02-Model-Definition/design-system.md`
- Development guidelines reviewed in `bmad/04-Development-Sprint/development-guidelines.md`

## Workflow Steps

### Step 1: Task Decomposition (Mayor)
- Analyze component requirements
- Create BEAD issues for each agent
- Define dependencies

### Step 2: Implementation (Coder)
Create:
- Sling Model: `core/src/main/java/.../models/{Component}Model.java`
- HTL Template: `ui.apps/.../components/content/{component}/{component}.html`
- Dialog: `ui.apps/.../components/content/{component}/_cq_dialog/.content.xml`
- Client Library (if needed): `ui.frontend/src/components/{component}/`

### Step 3: Testing (Tester)
Create:
- Unit Tests: `core/src/test/java/.../models/{Component}ModelTest.java`
- Test Content: `core/src/test/resources/{component}/test-content.json`

### Step 4: Review (Reviewer)
Verify:
- Code quality and patterns
- Security (XSS, injection)
- Accessibility (WCAG 2.1 AA)
- AEMaaCS compatibility

## BEAD Tracking

Issues will be created in:
```
bmad/gastown/bead/.issues/
├── coder/{component}-impl-001.md
├── tester/{component}-test-001.md
└── reviewer/{component}-review-001.md
```

## Invocation

To start, adopt the Mayor AI persona:

```
Please read bmad/gastown/agents/mayor.md and adopt the Mayor AI persona.
Initialize a component-development workflow for "{Component Name}".
```

What component would you like to develop?
