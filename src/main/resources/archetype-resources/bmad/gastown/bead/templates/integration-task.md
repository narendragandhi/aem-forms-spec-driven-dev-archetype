---
id: ${workflow_id}-int-${sequence}
workflow_id: ${workflow_id}
type: integration
agent: coder
status: pending
priority: ${priority}
created: ${timestamp}
updated: ${timestamp}
depends_on: [${planning_issue_id}]
blocks: [${test_issue_id}, ${dispatcher_issue_id}]
---

# Implement ${integration_name} Integration

## Context

Develop integration with ${integration_name} for AEM as a Cloud Service.

**Integration Type**: ${integration_type}
**Spec Reference**: bmad/06-Integrations/${integration_name}-integration.md

## Acceptance Criteria

- [ ] Service interface defined
- [ ] Service implementation complete
- [ ] OSGi configuration created
- [ ] Cloud Manager secrets referenced (no hardcoded credentials)
- [ ] Error handling implemented
- [ ] Timeout handling implemented
- [ ] Stateless design verified
- [ ] Code compiles without errors
- [ ] Logging at appropriate levels

## Technical Details

### AEMaaCS Considerations

- **Stateless**: No server-side session dependencies
- **Secrets**: Use `$[secret:VAR]` pattern
- **CDN**: Consider caching implications
- **Timeouts**: Configure appropriate connection/read timeouts

### Service Interface

```java
package com.example.aem.bmad.core.services;

public interface ${IntegrationName}Service {
    // Define methods
}
```

### OSGi Configuration

```java
@ObjectClassDefinition(name = "${integration_name} Configuration")
public @interface ${IntegrationName}Config {
    @AttributeDefinition(name = "Enabled")
    boolean enabled() default true;

    @AttributeDefinition(name = "API Endpoint")
    String apiEndpoint();

    @AttributeDefinition(name = "Timeout (ms)")
    int timeout() default 5000;
}
```

### File Locations

| File Type | Path |
|-----------|------|
| Service Interface | `core/src/main/java/.../services/${IntegrationName}Service.java` |
| Service Implementation | `core/src/main/java/.../services/impl/${IntegrationName}ServiceImpl.java` |
| OSGi Config | `core/src/main/java/.../config/${IntegrationName}Config.java` |
| Dev Config | `ui.config/.../config.dev/...${IntegrationName}Config.cfg.json` |

### Environment Configs

| Environment | API Endpoint | Secrets |
|-------------|--------------|---------|
| Local | Mock service | N/A |
| Dev | ${dev_endpoint} | Cloud Manager Dev |
| Stage | ${stage_endpoint} | Cloud Manager Stage |
| Prod | ${prod_endpoint} | Cloud Manager Prod |

## Progress Log

### ${timestamp}
Issue created by Mayor during integration development workflow.

## Handoff Notes

<!-- For Tester: Mock service details, test scenarios -->
<!-- For Dispatcher: Caching requirements, filter rules needed -->

## Files Changed

<!-- Updated as work progresses -->

## Related Issues

- Planning: #${planning_issue_id}
- Testing: #${test_issue_id}
- Dispatcher: #${dispatcher_issue_id}
- Documentation: #${docs_issue_id}
