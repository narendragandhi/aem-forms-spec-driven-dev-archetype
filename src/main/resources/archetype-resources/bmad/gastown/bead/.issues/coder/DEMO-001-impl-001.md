---
id: DEMO-001-impl-001
workflow_id: DEMO-001
type: implementation
agent: coder
status: in_progress
priority: high
created: 2026-02-18T10:00:00Z
updated: 2026-02-18T14:30:00Z
depends_on: []
blocks: [DEMO-001-test-001, DEMO-001-review-001]
---

# Implement Accordion Component

## Context

Develop a fully accessible Accordion component following the BMAD component guidelines. This component will allow content authors to create expandable/collapsible sections for FAQ pages and content organization.

**Component Type**: Content Component
**Design Reference**: bmad/03-Architecture-Design/component-design.md
**Accessibility Standard**: WCAG 2.1 AA

## Acceptance Criteria

- [x] Sling Model created with correct annotations
- [x] Model implements ComponentExporter for JSON
- [x] HTL template renders correctly
- [ ] Dialog allows author configuration
- [ ] Client library (CSS/JS) functional
- [ ] Accessibility requirements met (ARIA attributes)
- [ ] i18n ready (no hardcoded strings)
- [x] Code compiles without errors
- [ ] Follows project coding standards

## Technical Details

### Sling Model Requirements

```java
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = {AccordionModel.class, ComponentExporter.class},
    resourceType = AccordionModel.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
          extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class AccordionModel implements ComponentExporter {
    public static final String RESOURCE_TYPE = "bmad-showcase/components/content/accordion";
    // Implementation
}
```

### File Locations

| File Type | Path | Status |
|-----------|------|--------|
| Sling Model | `core/src/main/java/.../models/AccordionModel.java` | Done |
| HTL Template | `ui.apps/.../components/content/accordion/accordion.html` | Done |
| Dialog | `ui.apps/.../components/content/accordion/_cq_dialog/.content.xml` | In Progress |
| Client Library | `ui.frontend/src/components/accordion/` | Pending |

### Design System Tokens

- Background: `--color-surface-secondary`
- Border: `--color-border-subtle`
- Expanded icon: chevron-down → chevron-up rotation
- Animation: 200ms ease-in-out

## Progress Log

### 2026-02-18T10:00:00Z
Issue created by Mayor during component-development workflow.

### 2026-02-18T10:30:00Z
Started implementation. Reading component design specifications.

### 2026-02-18T11:45:00Z
Completed AccordionModel.java with the following features:
- Multi-item support via child resources
- Configurable "expand all" behavior
- Support for single-expand mode (auto-collapse others)
- JSON export for headless usage

Files created:
- `core/src/main/java/com/example/aem/bmad/core/models/AccordionModel.java`
- `core/src/main/java/com/example/aem/bmad/core/models/AccordionItemModel.java`

### 2026-02-18T13:00:00Z
Completed HTL template with proper ARIA attributes:
- `role="region"` on accordion container
- `aria-expanded` state on buttons
- `aria-controls` linking buttons to panels
- `aria-labelledby` on panels

Files created:
- `ui.apps/.../components/content/accordion/accordion.html`
- `ui.apps/.../components/content/accordion/accordionItem.html`

### 2026-02-18T14:30:00Z
Working on Touch UI dialog. Multifield for accordion items configured.
Need to add validation for minimum 1 item required.

## Handoff Notes

<!-- For Tester: Document key files, business logic locations, and edge cases -->

**Key Files for Testing:**
- `AccordionModel.java:45-120` - Core business logic
- `accordion.html:1-50` - Template rendering with ARIA
- `accordionItem.html:1-30` - Individual item rendering

**Edge Cases to Test:**
- Empty items array (should show placeholder in author mode)
- Maximum 50 items (soft limit)
- Special characters in labels (XSS prevention)
- Single-expand mode (clicking new item closes previous)
- Keyboard navigation (Tab, Enter, Space, Arrow keys)

**Mocking Required:**
- ResourceResolver (use AemContext)
- Child resource iteration

## Files Changed

- `core/src/main/java/com/example/aem/bmad/core/models/AccordionModel.java` - Main Sling Model
- `core/src/main/java/com/example/aem/bmad/core/models/AccordionItemModel.java` - Item model
- `ui.apps/.../components/content/accordion/accordion.html` - Main HTL template
- `ui.apps/.../components/content/accordion/accordionItem.html` - Item template
- `ui.apps/.../components/content/accordion/_cq_dialog/.content.xml` - In progress

## Related Issues

- Testing: #DEMO-001-test-001
- Review: #DEMO-001-review-001
