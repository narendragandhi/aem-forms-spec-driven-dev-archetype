---
id: ${bug_id}-fix-${sequence}
workflow_id: ${workflow_id}
type: bugfix
agent: coder
status: pending
priority: ${severity}
created: ${timestamp}
updated: ${timestamp}
depends_on: [${investigation_issue_id}]
blocks: [${test_issue_id}]
---

# Fix: ${bug_title}

## Context

**Bug ID**: ${bug_id}
**Severity**: ${severity}
**Affected Component**: ${affected_component}
**Reported**: ${reported_date}

### Reproduction Steps

${reproduction_steps}

### Expected Behavior

${expected_behavior}

### Actual Behavior

${actual_behavior}

## Root Cause Analysis

<!-- From investigation phase -->
${root_cause_analysis}

## Acceptance Criteria

- [ ] Bug no longer reproducible
- [ ] Regression test passes
- [ ] All existing tests still pass
- [ ] No new warnings introduced
- [ ] Fix does not introduce side effects
- [ ] Code follows project standards

## Technical Details

### Proposed Fix

${fix_proposal}

### Affected Files

| File | Change Type | Description |
|------|-------------|-------------|
| ${file1} | Modify | ${description1} |

### Risk Assessment

**Impact Scope**: ${impact_scope}
**Regression Risk**: ${regression_risk}
**Testing Required**: ${testing_required}

## Progress Log

### ${timestamp}
Issue created. Root cause identified in investigation phase.

## Handoff Notes

<!-- For Tester: Describe what changed and edge cases to verify -->

## Files Changed

<!-- Updated as work progresses -->

## Related Issues

- Investigation: #${investigation_issue_id}
- Regression Test: #${test_issue_id}
- Original Bug Report: ${bug_ticket_url}
