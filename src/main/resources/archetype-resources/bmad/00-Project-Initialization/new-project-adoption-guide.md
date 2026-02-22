# New Project Adoption Guide

## Overview

This guide explains how to use the AEM BMAD Showcase as a foundation for new AEM development projects. It covers the adoption process, customization steps, BEAD task integration, and a productization checklist.

---

## Table of Contents

1. [Adoption Decision](#1-adoption-decision)
2. [Project Setup](#2-project-setup)
3. [BEAD Task Integration](#3-bead-task-integration)
4. [Customization Checklist](#4-customization-checklist)
5. [Team Onboarding](#5-team-onboarding)
6. [Productization Checklist](#6-productization-checklist)

---

## 1. Adoption Decision

### 1.1 When to Use This Showcase

| Use Case | Recommendation |
|----------|----------------|
| New AEM as a Cloud Service project | **Use** - Full adoption |
| AEM 6.5 on-premise project | **Partial** - Adapt methodology, not infra |
| Brownfield AEM project (existing) | **Reference** - Adopt patterns incrementally |
| Non-AEM project | **Methodology only** - BMAD methodology transfers |

### 1.2 What You Get

```
AEM BMAD Showcase Includes:
├── BMAD Methodology Documentation (35+ docs)
│   ├── Business discovery templates
│   ├── Architecture patterns
│   ├── Development guidelines
│   └── Testing strategies
├── GasTown AI Agent Framework
│   ├── Agent definitions
│   ├── BEAD task templates
│   └── Workflow orchestration
├── Reference Implementation
│   ├── 5 core components (Hero, Card, Carousel, etc.)
│   ├── Sling Models with unit tests
│   ├── HTL templates
│   └── Frontend build (Webpack/SCSS)
├── Operations Documentation
│   ├── Runbooks
│   ├── Monitoring strategy
│   ├── Security hardening
│   └── DR procedures
└── Role-based Tutorials
    ├── Developer Guide
    ├── Architect Guide
    ├── QA Engineer Guide
    └── Product Manager Guide
```

### 1.3 Time to Value

| Activity | Estimated Time |
|----------|----------------|
| Repository setup | 1 day |
| Customization | 1-2 weeks |
| Team training | 1 week |
| First sprint | 2 weeks |
| Full velocity | 4-6 weeks |

---

## 2. Project Setup

### 2.1 Fork/Clone the Repository

```bash
# Option 1: Fork (recommended for ongoing updates)
# Fork via GitHub UI, then:
git clone https://github.com/YOUR-ORG/aem-bmad-showcase.git
cd aem-bmad-showcase
git remote add upstream https://github.com/ORIGINAL/aem-bmad-showcase.git

# Option 2: Fresh copy (clean start)
git clone https://github.com/ORIGINAL/aem-bmad-showcase.git my-new-project
cd my-new-project
rm -rf .git
git init
git add .
git commit -m "Initial commit from AEM BMAD Showcase"
```

### 2.2 Rename Project

```bash
# 1. Update project name in all POMs
find . -name "pom.xml" -exec sed -i 's/aem-bmad-showcase/your-project-name/g' {} \;

# 2. Update package names
find core/src -name "*.java" -exec sed -i 's/com.example.aem.bmad/com.yourorg.aem.project/g' {} \;

# 3. Update resource types
find ui.apps -name "*.xml" -exec sed -i 's/bmad-showcase/your-project/g' {} \;

# 4. Update content paths
find ui.content -name "*.xml" -exec sed -i 's/bmad-showcase/your-project/g' {} \;

# 5. Update clientlib categories
find ui.frontend -name "*.json" -exec sed -i 's/bmad-showcase/your-project/g' {} \;
```

### 2.3 Configure Cloud Manager

```yaml
# 1. Create program in Cloud Manager
# 2. Connect repository
# 3. Configure pipelines:

pipelines:
  - name: CI Build
    type: ci-build
    branch: develop

  - name: Stage Deploy
    type: full-stack
    branch: main
    environments: [stage]

  - name: Production Deploy
    type: full-stack
    branch: main
    environments: [production]
    approval: required
```

### 2.4 Update Documentation

| File | Update |
|------|--------|
| `README.md` | Project name, description, team |
| `CLAUDE.md` | Project-specific AI instructions |
| `bmad/00-*/README.md` | Project initialization details |
| `bmad/01-*/` | Business requirements |
| `bmad/02-*/` | Content models for your project |

---

## 3. BEAD Task Integration

### 3.1 Understanding BEAD Structure

```
BEAD (Beads) = Git-backed Issue Tracking for AI Agents

Structure:
.bead/
├── config.yaml              # BEAD configuration
└── issues/
    ├── {hash-prefix}/
    │   └── {issue-id}.yaml  # Individual issue files
    └── ...

Issue Types:
├── component-task    # New component development
├── integration-task  # Third-party integration
├── test-task         # Testing tasks
├── review-task       # Code review tasks
├── bug-fix           # Bug fixes
└── docs-task         # Documentation tasks
```

### 3.2 BEAD Configuration

```yaml
# .bead/config.yaml
project:
  name: your-project-name
  type: aem-cloud

agents:
  mayor:
    role: orchestrator
    delegates_to: [coder, tester, reviewer, documenter]

  coder:
    role: implementation
    templates: [component-task, integration-task, bug-fix]

  tester:
    role: quality-assurance
    templates: [test-task]

  reviewer:
    role: code-review
    templates: [review-task]

  documenter:
    role: documentation
    templates: [docs-task]

workflows:
  new-component:
    steps:
      - plan: mayor
      - implement: coder
      - test: tester
      - review: reviewer
      - document: documenter
```

### 3.3 Creating BEAD Tasks

**Example: New Component Workflow**

```yaml
# issues/abc1/abc12345.yaml
---
id: abc12345
workflow_id: FEAT-001
type: component
agent: mayor
status: active
priority: high
created: 2024-01-15T10:00:00Z
---

# Develop Contact Form Component

## Overview
Create a contact form component with validation and LLM-assisted response suggestions.

## Child Tasks

| Task ID | Type | Agent | Status |
|---------|------|-------|--------|
| abc12346 | planning | mayor | complete |
| abc12347 | implementation | coder | in-progress |
| abc12348 | test | tester | pending |
| abc12349 | review | reviewer | pending |
| abc12350 | docs | documenter | pending |

## Acceptance Criteria
- [ ] Form renders with configurable fields
- [ ] Client-side validation works
- [ ] Server-side validation secure
- [ ] LLM suggestions functional
- [ ] Unit tests pass (>80% coverage)
- [ ] Accessibility compliant
- [ ] Documentation complete
```

### 3.4 BEAD Task Templates Location

| Template | Location | Purpose |
|----------|----------|---------|
| Component | `bmad/gastown/bead/templates/component-task.md` | New AEM components |
| Integration | `bmad/gastown/bead/templates/integration-task.md` | API/service integration |
| Bug Fix | `bmad/gastown/bead/templates/bug-fix.md` | Defect resolution |
| Test | `bmad/gastown/bead/templates/test-task.md` | QA tasks |
| Review | `bmad/gastown/bead/templates/review-task.md` | Code reviews |
| Docs | `bmad/gastown/bead/templates/docs-task.md` | Documentation |

### 3.5 Sample Sprint BEAD Breakdown

```markdown
## Sprint 1: Foundation Components

### BEAD-001: Hero Component
├── BEAD-001-PLAN: Design review (Mayor)
├── BEAD-001-IMPL: Sling Model + HTL (Coder)
├── BEAD-001-TEST: Unit tests + manual QA (Tester)
├── BEAD-001-REVIEW: Code review (Reviewer)
└── BEAD-001-DOCS: Component documentation (Documenter)

### BEAD-002: Navigation Component
├── BEAD-002-PLAN: IA review (Mayor)
├── BEAD-002-IMPL: Mega-menu implementation (Coder)
├── BEAD-002-TEST: Cross-browser testing (Tester)
├── BEAD-002-REVIEW: Code review (Reviewer)
└── BEAD-002-DOCS: Authoring guide (Documenter)

### BEAD-003: LLM Integration
├── BEAD-003-PLAN: API design (Mayor)
├── BEAD-003-IMPL: OSGi service (Coder)
├── BEAD-003-TEST: Integration tests (Tester)
├── BEAD-003-REVIEW: Security review (Reviewer)
└── BEAD-003-DOCS: Integration guide (Documenter)
```

---

## 4. Customization Checklist

### 4.1 Business Discovery (Phase 01)

- [ ] Update personas for your project
- [ ] Define your user stories
- [ ] Create your PRD (Product Requirements Document)
- [ ] Map business requirements to components

### 4.2 Model Definition (Phase 02)

- [ ] Define content models for your domain
- [ ] Update information architecture
- [ ] Customize design system tokens
- [ ] Define component hierarchy

### 4.3 Architecture Design (Phase 03)

- [ ] Review and update system architecture
- [ ] Customize component designs
- [ ] Update dispatcher rules for your domains
- [ ] Define integration points

### 4.4 Development (Phase 04)

- [ ] Remove showcase components not needed
- [ ] Add project-specific components
- [ ] Customize LLM prompts for your domain
- [ ] Update build configurations

### 4.5 Testing (Phase 05)

- [ ] Update test data for your content
- [ ] Customize regression suite
- [ ] Define project-specific performance baselines
- [ ] Update security testing scope

### 4.6 Operations (Phase 07)

- [ ] Update runbooks with your contacts
- [ ] Configure monitoring for your SLAs
- [ ] Update DR procedures for your infrastructure
- [ ] Customize security checklist for compliance needs

---

## 5. Team Onboarding

### 5.1 Role-Based Onboarding Paths

```
┌─────────────────────────────────────────────────────────────────┐
│                    New Team Member                              │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   Developer   │    │   Architect   │    │   QA Engineer │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ Read:         │    │ Read:         │    │ Read:         │
│ Developer     │    │ Architect     │    │ QA Engineer   │
│ Guide         │    │ Guide         │    │ Guide         │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ Review:       │    │ Review:       │    │ Review:       │
│ Components    │    │ Architecture  │    │ Testing       │
│ Codebase      │    │ Docs          │    │ Strategy      │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
                             ▼
                    ┌───────────────┐
                    │ First Task    │
                    │ (Mentored)    │
                    └───────────────┘
```

### 5.2 Onboarding Checklist

**Week 1: Foundation**
- [ ] Environment access (AEM, Cloud Manager, Git)
- [ ] Read project README and CLAUDE.md
- [ ] Review BMAD methodology overview
- [ ] Complete role-specific tutorial
- [ ] Set up local development environment

**Week 2: Deep Dive**
- [ ] Review architecture documentation
- [ ] Study existing components
- [ ] Understand BEAD task workflow
- [ ] Shadow experienced team member
- [ ] Complete first mentored task

**Week 3: Contributing**
- [ ] Complete independent BEAD task
- [ ] Participate in code review
- [ ] Join sprint planning
- [ ] Contribute to documentation

### 5.3 Training Resources

| Topic | Resource | Duration |
|-------|----------|----------|
| AEM Basics | Adobe Experience League | 8 hours |
| BMAD Methodology | `bmad/methodologies/BMAD-BEAD-GasTown.md` | 2 hours |
| GasTown Agents | `bmad/gastown/README.md` | 1 hour |
| Component Development | `bmad/tutorials/Developer-Guide.md` | 4 hours |
| Testing Strategy | `bmad/05-Testing-and-Deployment/` | 2 hours |

---

## 6. Productization Checklist

### 6.1 Pre-Production Gate

**Documentation Complete:**
- [ ] All BMAD phases documented
- [ ] API documentation generated
- [ ] User guides written
- [ ] Operations runbooks ready
- [ ] Training materials prepared

**Quality Assured:**
- [ ] Unit test coverage ≥ 70%
- [ ] Integration tests passing
- [ ] E2E tests covering critical paths
- [ ] Performance baselines established
- [ ] Security scan clean

**Operations Ready:**
- [ ] Monitoring dashboards configured
- [ ] Alerting rules defined
- [ ] Logging strategy implemented
- [ ] DR procedures tested
- [ ] On-call rotation established

### 6.2 Production Readiness Review

```markdown
## Production Readiness Checklist

### Architecture
- [ ] Load testing completed at 2x expected traffic
- [ ] Auto-scaling configured and tested
- [ ] CDN caching optimized
- [ ] Database connections pooled

### Security
- [ ] Penetration test completed
- [ ] OWASP Top 10 addressed
- [ ] Secrets management implemented
- [ ] SSL certificates valid 90+ days

### Compliance
- [ ] GDPR requirements met
- [ ] Accessibility audit passed (WCAG 2.1 AA)
- [ ] Cookie consent implemented
- [ ] Privacy policy published

### Operations
- [ ] Runbooks reviewed and tested
- [ ] Incident response tested
- [ ] Backup/restore tested
- [ ] Rollback procedure validated

### Team
- [ ] On-call rotation scheduled
- [ ] Escalation paths documented
- [ ] All team members trained
- [ ] Knowledge transfer complete
```

### 6.3 Go-Live Checklist

```markdown
## Go-Live Day Checklist

### T-24 Hours
- [ ] Final deployment to Stage
- [ ] Full regression pass
- [ ] Stakeholder sign-off
- [ ] War room scheduled
- [ ] Rollback artifacts ready

### T-2 Hours
- [ ] Team assembled
- [ ] Communication channels active
- [ ] Monitoring dashboards open
- [ ] Status page updated

### Deployment
- [ ] Execute deployment
- [ ] Smoke tests pass
- [ ] Key journeys verified
- [ ] Performance within baseline

### T+1 Hour
- [ ] Monitor error rates
- [ ] Check analytics
- [ ] Verify CDN caching
- [ ] Customer feedback channels monitored

### T+24 Hours
- [ ] Extended monitoring period complete
- [ ] No critical issues
- [ ] Status: GO-LIVE SUCCESSFUL
- [ ] Schedule retrospective
```

---

## Quick Start Summary

```bash
# 1. Clone/Fork
git clone https://github.com/ORG/aem-bmad-showcase.git my-project

# 2. Rename (run customization scripts)
./scripts/rename-project.sh my-project com.myorg.aem

# 3. Configure
# - Update Cloud Manager settings
# - Configure environment variables
# - Set up team access

# 4. Train
# - Assign tutorials to team members
# - Schedule knowledge transfer sessions

# 5. Sprint 0
# - Customize Phase 01-02 documentation
# - Create first BEAD tasks
# - Start development!
```

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Platform Team | Initial version |

**Review Cycle:** Per major release
**Owner:** Technical Lead
