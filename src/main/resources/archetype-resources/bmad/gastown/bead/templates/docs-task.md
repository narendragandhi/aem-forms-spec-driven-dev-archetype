---
id: ${workflow_id}-docs-${sequence}
workflow_id: ${workflow_id}
type: documentation
agent: docs
status: pending
priority: ${priority}
created: ${timestamp}
updated: ${timestamp}
depends_on: [${review_issue_id}]
blocks: []
---

# Document ${target_name}

## Context

Create documentation for ${target_name} following review approval.

**Documentation Type**: ${doc_type}
**Target**: ${target_name}
**Review Issue**: #${review_issue_id}

## Acceptance Criteria

- [ ] Component/feature documentation created
- [ ] Author guide updated (if applicable)
- [ ] Developer guide updated (if applicable)
- [ ] API documentation complete
- [ ] Usage examples provided
- [ ] README updated
- [ ] CHANGELOG updated
- [ ] All links verified
- [ ] Spelling/grammar checked

## Documentation Requirements

### Source Materials

| Source | Location |
|--------|----------|
| Implementation | ${impl_files} |
| Tests | ${test_files} |
| Review Report | #${review_issue_id} |
| Design Spec | ${design_spec} |

### Documentation Structure

```
docs/
├── components/
│   └── ${target_name}.md
├── services/
│   └── ${target_name}.md (if service)
└── guides/
    └── author-guide.md (update)
```

### Required Sections

#### For Components
- [ ] Overview
- [ ] Author Experience (dialog usage)
- [ ] Properties table
- [ ] Technical details (resource type, model, HTL)
- [ ] Accessibility notes
- [ ] Usage examples
- [ ] Dependencies

#### For Services
- [ ] Purpose
- [ ] Interface definition
- [ ] Configuration (OSGi properties, environment variables)
- [ ] Usage examples (injection, method calls)
- [ ] Error handling
- [ ] Monitoring/logging

#### For Integrations
- [ ] Overview
- [ ] Architecture diagram
- [ ] Configuration by environment
- [ ] Implementation guide
- [ ] Testing approach
- [ ] Troubleshooting

## Review Notes

<!-- From reviewer handoff -->
${review_handoff_notes}

## Progress Log

### ${timestamp}
Issue created. Code review approved, ready for documentation.

## Documentation Created

| Document | Path | Status |
|----------|------|--------|
| Component Doc | docs/components/${target_name}.md | Pending |
| README Update | README.md | Pending |
| CHANGELOG | CHANGELOG.md | Pending |

## Handoff Notes

<!-- Final notes for Mayor/completion phase -->

## Files Changed

<!-- Updated as work progresses -->

## Related Issues

- Implementation: #${impl_issue_id}
- Testing: #${test_issue_id}
- Review: #${review_issue_id}
