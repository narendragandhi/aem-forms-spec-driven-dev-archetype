---
id: ${workflow_id}-impl-${sequence}
workflow_id: ${workflow_id}
type: implementation
agent: coder
status: pending
priority: ${priority}
created: ${timestamp}
updated: ${timestamp}
depends_on: [${planning_issue_id}]
blocks: [${test_issue_id}, ${review_issue_id}]
---

# Implement ${component_name} Component

## Context

Implement the ${component_name} AEM component as specified in the component design document.

**Component Type**: ${component_type}
**Design Reference**: bmad/03-Architecture-Design/component-design.md

## Acceptance Criteria

- [ ] Sling Model created with correct annotations
- [ ] Model implements ComponentExporter for JSON
- [ ] HTL template renders correctly
- [ ] Dialog allows author configuration
- [ ] Client library (CSS/JS) functional
- [ ] Accessibility requirements met (WCAG 2.1 AA)
- [ ] i18n ready (no hardcoded strings)
- [ ] Code compiles without errors
- [ ] Follows project coding standards

## Technical Details

### Sling Model Requirements

```java
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = {${ComponentName}Model.class, ComponentExporter.class},
    resourceType = ${ComponentName}Model.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
          extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ${ComponentName}Model implements ComponentExporter {
    public static final String RESOURCE_TYPE = "aem-bmad-showcase/components/content/${component_name}";
    // Implementation
}
```

### File Locations

| File Type | Path |
|-----------|------|
| Sling Model | `core/src/main/java/.../models/${ComponentName}Model.java` |
| HTL Template | `ui.apps/.../components/content/${component_name}/${component_name}.html` |
| Dialog | `ui.apps/.../components/content/${component_name}/_cq_dialog/.content.xml` |
| Client Library | `ui.frontend/src/components/${component_name}/` |

### Design System Tokens

Reference: bmad/02-Model-Definition/design-system.md

## Progress Log

### ${timestamp}
Issue created by Mayor during ${workflow_name} workflow.

## Handoff Notes

<!-- For Tester: Document key files, business logic locations, and edge cases -->

## Files Changed

<!-- Updated as work progresses -->

## Related Issues

- Planning: #${planning_issue_id}
- Testing: #${test_issue_id}
- Review: #${review_issue_id}
