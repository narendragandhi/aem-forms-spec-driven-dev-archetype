---
id: ${workflow_id}-test-${sequence}
workflow_id: ${workflow_id}
type: testing
agent: tester
status: pending
priority: ${priority}
created: ${timestamp}
updated: ${timestamp}
depends_on: [${impl_issue_id}]
blocks: [${review_issue_id}]
---

# Create Tests for ${target_name}

## Context

Write comprehensive tests for ${target_name} implementation.

**Test Type**: ${test_type}
**Target Files**: ${target_files}

## Acceptance Criteria

- [ ] Unit tests created
- [ ] Test coverage >= 80%
- [ ] Edge cases covered
- [ ] Error scenarios tested
- [ ] Test content/fixtures created
- [ ] All tests pass
- [ ] Tests are deterministic (no flaky tests)

## Technical Details

### Implementation Handoff Notes

<!-- From implementing agent -->
${handoff_notes}

### Key Files to Test

| File | Focus Areas |
|------|-------------|
| ${file1} | ${focus1} |

### Test Scenarios

#### Happy Path
- [ ] ${happy_path_1}
- [ ] ${happy_path_2}

#### Edge Cases
- [ ] ${edge_case_1}
- [ ] ${edge_case_2}

#### Error Scenarios
- [ ] ${error_scenario_1}
- [ ] ${error_scenario_2}

### Test File Locations

| Test Type | Path |
|-----------|------|
| Unit Tests | `core/src/test/java/.../models/${TargetName}ModelTest.java` |
| Test Content | `core/src/test/resources/${target_name}/test-content.json` |

### Testing Patterns

```java
@ExtendWith(AemContextExtension.class)
class ${TargetName}ModelTest {

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @BeforeEach
    void setUp() {
        context.load().json("/com/example/${target_name}/test-content.json", "/content");
        context.addModelsForClasses(${TargetName}Model.class);
    }

    @Test
    void testBasicFunctionality() {
        // Test implementation
    }
}
```

### Mocking Requirements

| Dependency | Mock Strategy |
|------------|---------------|
| ResourceResolver | AemContext JCR_MOCK |
| External Service | Mockito mock |

## Progress Log

### ${timestamp}
Issue created. Implementation complete, ready for testing.

## Handoff Notes

<!-- For Reviewer: Test coverage report, any known limitations -->

## Files Changed

<!-- Updated as work progresses -->

## Related Issues

- Implementation: #${impl_issue_id}
- Review: #${review_issue_id}
