---
id: ${workflow_id}-review-${sequence}
workflow_id: ${workflow_id}
type: review
agent: reviewer
status: pending
priority: ${priority}
created: ${timestamp}
updated: ${timestamp}
depends_on: [${impl_issue_id}, ${test_issue_id}]
blocks: [${docs_issue_id}]
---

# Code Review: ${target_name}

## Context

Review implementation and tests for ${target_name}.

**Review Type**: ${review_type}
**Implementation Issue**: #${impl_issue_id}
**Test Issue**: #${test_issue_id}

## Files to Review

| File | Type | Lines |
|------|------|-------|
| ${file1} | ${type1} | ${lines1} |

## Acceptance Criteria

- [ ] Code quality review complete
- [ ] Security review complete
- [ ] Performance review complete
- [ ] Accessibility review complete (if applicable)
- [ ] AEMaaCS compatibility verified
- [ ] All critical issues addressed
- [ ] Review report generated

## Review Checklist

### Code Quality
- [ ] Clean code principles followed
- [ ] SOLID principles applied
- [ ] Appropriate design patterns
- [ ] Consistent naming conventions
- [ ] No code duplication

### Security
- [ ] No XSS vulnerabilities
- [ ] No injection risks (SQL/JCR)
- [ ] No hardcoded credentials
- [ ] Proper input validation
- [ ] Authorization checks in place

### Performance
- [ ] No N+1 query patterns
- [ ] Proper caching considerations
- [ ] Efficient resource handling
- [ ] No memory leaks
- [ ] Appropriate lazy loading

### AEMaaCS Compatibility
- [ ] Stateless design
- [ ] Cloud Manager secrets used
- [ ] No deprecated APIs
- [ ] CDN/Dispatcher friendly

### Testing
- [ ] Adequate test coverage (>= 80%)
- [ ] Edge cases covered
- [ ] Error scenarios tested
- [ ] Tests are maintainable

## Progress Log

### ${timestamp}
Issue created. Implementation and tests complete, ready for review.

## Review Findings

### Critical (Must Fix)
<!-- Blocking issues that must be resolved -->

### Major (Should Fix)
<!-- Significant issues that should be addressed -->

### Minor (Consider)
<!-- Suggestions for improvement -->

### Positive Observations
<!-- What was done well -->

## Review Decision

- [ ] Approved
- [ ] Approved with minor changes
- [ ] Request changes (major issues)
- [ ] Reject (critical issues)

## Handoff Notes

<!-- For Docs Agent: Key features to document, any notable patterns -->
<!-- For Coder: Specific changes required if not approved -->

## Files Changed

<!-- If any changes made during review -->

## Related Issues

- Implementation: #${impl_issue_id}
- Testing: #${test_issue_id}
- Documentation: #${docs_issue_id}
