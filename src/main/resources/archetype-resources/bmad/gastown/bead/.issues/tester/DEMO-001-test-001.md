---
id: DEMO-001-test-001
workflow_id: DEMO-001
type: testing
agent: tester
status: pending
priority: high
created: 2026-02-18T10:00:00Z
updated: 2026-02-18T10:00:00Z
depends_on: [DEMO-001-impl-001]
blocks: [DEMO-001-review-001]
---

# Test Accordion Component

## Context

Write comprehensive tests for the Accordion component implementation. This includes unit tests for the Sling Model, functional tests for the HTL rendering, and accessibility tests.

**Testing Framework**: AemContext + JUnit 5
**Coverage Target**: 80%
**Accessibility Tools**: axe-core

## Acceptance Criteria

- [ ] Unit tests for AccordionModel (>80% coverage)
- [ ] Unit tests for AccordionItemModel
- [ ] HTL rendering tests with mock content
- [ ] Accessibility audit passes (WCAG 2.1 AA)
- [ ] Keyboard navigation tests
- [ ] Edge case coverage (empty, max items, special chars)
- [ ] Tests pass in CI pipeline

## Technical Details

### Test Structure

```
core/src/test/java/com/example/aem/bmad/core/models/
├── AccordionModelTest.java
└── AccordionItemModelTest.java

ui.tests/playwright/
├── accordion.spec.ts          # Functional tests
└── accordion.a11y.spec.ts     # Accessibility tests
```

### Required Test Scenarios

**Unit Tests (JUnit + AemContext):**
1. Model adapts correctly from request
2. Items collection populated from child resources
3. Empty items returns empty list (not null)
4. Single-expand mode configuration
5. Expand-all configuration
6. JSON export contains expected fields
7. Resource type matches

**Functional Tests (Playwright):**
1. Accordion renders on page
2. Click expands/collapses item
3. Single-expand mode closes previous
4. Keyboard: Tab focuses items
5. Keyboard: Enter/Space toggles
6. Keyboard: Arrow keys navigate

**Accessibility Tests:**
1. ARIA roles present
2. aria-expanded state updates
3. Focus management
4. Screen reader announcement

### Test Data Location

```
core/src/test/resources/accordion/
├── fully-configured.json     # All options set
├── minimal.json              # Required fields only
├── empty-items.json          # No child items
└── max-items.json            # 50 items
```

## Progress Log

### 2026-02-18T10:00:00Z
Issue created by Mayor during component-development workflow.
Waiting on implementation (DEMO-001-impl-001) to complete.

## Handoff Notes

<!-- For Reviewer: Document test coverage, any known limitations -->

## Files Changed

<!-- Updated as work progresses -->

## Related Issues

- Implementation: #DEMO-001-impl-001 (dependency)
- Review: #DEMO-001-review-001
