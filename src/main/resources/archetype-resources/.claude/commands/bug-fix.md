---
description: Start bug fix workflow with diagnosis and resolution
---

# Bug Fix Workflow

You are initiating the **Bug Fix** workflow using GasTown orchestration.

## Workflow Overview

This workflow systematically diagnoses and fixes bugs in AEM components:

1. **Mayor AI** - Coordinates the fix process
2. **AEM Component Coder** - Implements the fix
3. **AEM Test Writer** - Adds regression tests
4. **AEM Code Reviewer** - Verifies fix quality

## Bug Fix Process

### Step 1: Bug Triage (Mayor)
Gather information:
- Bug description and reproduction steps
- Affected component(s)
- Severity and priority
- Related BEAD issues

### Step 2: Root Cause Analysis (Coder)
- Identify the source of the bug
- Understand the expected vs actual behavior
- Document the root cause

### Step 3: Fix Implementation (Coder)
- Implement minimal fix
- Avoid scope creep
- Follow existing patterns

### Step 4: Regression Testing (Tester)
- Create test case that reproduces the bug
- Verify fix resolves the issue
- Ensure no regression in related functionality

### Step 5: Review (Reviewer)
- Verify fix addresses root cause
- Check for side effects
- Ensure code quality

## BEAD Tracking

```
bmad/gastown/bead/.issues/
├── coder/{bug-id}-fix-001.md
├── tester/{bug-id}-test-001.md
└── reviewer/{bug-id}-review-001.md
```

## Bug Report Template

```markdown
## Bug Report

**ID**: BUG-{number}
**Component**: {component name}
**Severity**: Critical | High | Medium | Low

### Description
{What is happening}

### Expected Behavior
{What should happen}

### Reproduction Steps
1. {Step 1}
2. {Step 2}
3. {Step 3}

### Environment
- AEM Version: {version}
- Browser: {browser}
- OS: {os}

### Logs/Screenshots
{Attach relevant logs or screenshots}
```

## Invocation

To start, adopt the Mayor AI persona:

```
Please read bmad/gastown/agents/mayor.md and adopt the Mayor AI persona.
Initialize a bug-fix workflow for: {Bug Description}
```

Describe the bug you need to fix.
