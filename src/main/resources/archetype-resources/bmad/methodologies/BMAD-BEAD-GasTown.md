# BMAD, BEAD, and Gas Town: A Multi-Layered AI-Augmented Development Ecosystem for AEM Forms

This document provides a comprehensive comparison and integration strategy for three key concepts in AI-augmented software development, specifically tailored for an **AEM Forms** project: BMAD (Breakthrough Method for Agile Development), BEAD (Beads) system, and Gas Town (AI Agent Orchestration System).

## 1. BMAD (Breakthrough Method for Agile Development) Overview

BMAD is an open-source, AI-driven agile development *framework*. For AEM Forms, it provides a structured workflow for managing the entire lifecycle, from defining form requirements to deploying complex, data-driven Adaptive Forms.

## 2. BEAD (Beads) System Overview

BEAD is a distributed, Git-backed graph issue tracker designed *for individual AI agents*. In a Forms project, it's crucial for an agent tasked with creating a custom React form component. It allows the agent to manage context, remember data binding requirements, and handle the complexities of the AEM Forms framework without losing track of its progress.

## 3. Gas Town (AI Agent Orchestration System) Overview

Gas Town is an AI agent orchestration system that manages and coordinates *multiple* specialized AI agents. For AEM Forms, this means orchestrating a team that might include an agent for coding the React component, another for writing its unit tests, and a third for creating the documentation.

## 4. Comparison and Contrast

| Feature | BMAD (for AEM Forms) | BEAD (for a Forms Coder) | Gas Town (for a Forms Team) |
| :--- | :--- | :--- | :--- |
| **Type** | Comprehensive Agile Development *Framework* | *Tool/System* for *Individual* AI Agent Task & Context Management | *System* for *Multi-Agent* Orchestration |
| **Primary Focus** | End-to-end form delivery, from requirements to workflows | AI agent memory for a single custom form component, its data bindings, and styling | Orchestration of a team of AI agents (coder, tester, reviewer) to build a suite of forms |
| **AEM Application** | Guides the entire AEM Forms project lifecycle (FDM, templates, themes, workflows) | Used by an "AEM Forms React Coder" agent to build a single custom component | Orchestrates the team building an entire Adaptive Form application |

## 5. Multi-Layered Synergy in an AEM Forms Project

The true power emerges when these systems are layered together to build and deploy an AEM Forms application.

```mermaid
graph TD
    subgraph BMAD Layer (Strategic Oversight & Human-AI Collaboration)
        BMAD_PM[PM Agent: Define High-level Task (e.g., "Develop Loan Application Form")] --> BMAD_ARCH[Architect Agent: Design Form Data Model & Custom Component Specs]
        BMAD_ARCH --> BMAD_DEV_TASK(BMAD Task: "Develop Address Lookup Component" with specs from content-models.md)
    end

    subgraph Gastown Layer (Multi-Agent Orchestration)
        GT_MAYOR[Mayor AI: Orchestrate Team for "Develop Address Lookup Component"]
        BMAD_DEV_TASK --> GT_MAYOR

        GT_MAYOR -- Directs & Monitors --> AICoder(AEM Forms React Coder)
        GT_MAYOR -- Directs & Monitors --> AITest(AEM Forms Test Writer)
        GT_MAYOR -- Directs & Monitors --> AICReview(AI Code Reviewer Agent)
    end

    subgraph BEAD Layer (Individual AI Agent Task Management & Persistent Context)
        AICoder -- Manages Tasks & Context via --> BEADCoder[BEAD: Coder's Tasks]
        AITest -- Manages Tasks & Context via --> BEADTest[BEAD: Tester's Tasks]

        BEADCoder --> REACT_COMP[Task: Create React Component (for Address Lookup)]
        BEADCoder --> DIALOG_JSON[Task: Create Authoring Dialog (dialog.json)]
        BEADCoder --> FDM_BINDING[Task: Integrate with Form Data Model]

        BEADTest --> JEST_TEST[Task: Write Jest/RTL Unit Test (for React component)]
        BEADTest --> CYPRESS_TEST[Task: Write Cypress UI Test (for form interaction)]
    end

    REACT_COMP --> DIALOG_JSON
    DIALOG_JSON --> FDM_BINDING
    FDM_BINDING -- Code Ready --> JEST_TEST
    JEST_TEST -- Tests Pass --> CYPRESS_TEST
    CYPRESS_TEST -- Reports Status to --> AITest
    AITest -- Reports Status to --> GT_MAYOR
    AICoder -- Reports Status to --> GT_MAYOR

    GT_MAYOR -- Aggregates & Reports Completion --> BMAD_DEV_TASK
    BMAD_DEV_TASK --> AEM_DEPLOY(AEM Cloud Manager Deployment Trigger)
    AEM_DEPLOY --> FINAL_PRODUCT(Deployed AEM Adaptive Form)
```

1.  **BMAD as the Strategic Layer**: The BMAD PM Agent identifies the business need for a "Loan Application Form". The BMAD Architect Agent designs the required Form Data Model (FDM) and specifies the need for a custom "Address Lookup" component.

2.  **Gas Town as the Orchestration Layer**: A Gas Town "Mayor" AI receives the task to "Develop Address Lookup Component". It orchestrates a team of specialized agents:
    *   An **"AEM Forms React Coder"** AI.
    *   An **"AEM Forms Test Writer"** AI, expert in Jest and React Testing Library.
    *   An **"AEM Code Reviewer"** AI.

3.  **BEAD as the Agent Memory Layer**: Each AI agent uses BEAD to manage its work:
    *   The **"AEM Forms React Coder"** uses its BEAD to break down the task: create the React component, define the `dialog.json` for authoring, and handle the data binding to the FDM. BEAD allows it to remember the specific data fields (`street`, `city`, `zip`) that need to be populated.
    *   The **"AEM Forms Test Writer"** uses its BEAD to manage the creation of Jest unit tests for the React component and Cypress tests for the end-user interaction within the form.

This multi-layered approach ensures that high-level business requirements for a form are translated into a deployable, high-quality AEM Adaptive Form with maximum efficiency and automation.
