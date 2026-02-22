# BEAD Task Library

## Overview

This library contains pre-defined BEAD task breakdowns for common AEM development patterns. Use these as starting points for sprint planning and task creation.

## Library Index

| Category | Tasks | Description |
|----------|-------|-------------|
| [Components](#components) | 15 | Standard AEM component development |
| [Integrations](#integrations) | 8 | Third-party service integration |
| [Infrastructure](#infrastructure) | 6 | DevOps and infrastructure tasks |
| [Content](#content) | 5 | Content migration and management |
| [Testing](#testing) | 7 | QA and testing tasks |
| [Security](#security) | 5 | Security hardening tasks |

## How to Use

1. Find the relevant task pattern below
2. Copy the BEAD structure
3. Customize for your specific requirements
4. Create issues in your `.bead/issues/` directory

---

## Components

### BEAD-LIB-COMP-001: New Content Component (Full)

```yaml
id: ${project}-comp-${name}-001
type: component-development
priority: medium
estimated_effort: 3-5 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 2h
    deliverables:
      - Component specification reviewed
      - Dependencies identified
      - Acceptance criteria confirmed

  - id: ${id}-model
    type: implementation
    agent: coder
    effort: 4h
    depends_on: [${id}-plan]
    deliverables:
      - Sling Model with ComponentExporter
      - Model unit tests (>80% coverage)
      - JavaDoc documentation

  - id: ${id}-dialog
    type: implementation
    agent: coder
    effort: 3h
    depends_on: [${id}-plan]
    deliverables:
      - Touch UI dialog
      - Field validations
      - Design dialog (if needed)

  - id: ${id}-htl
    type: implementation
    agent: coder
    effort: 3h
    depends_on: [${id}-model]
    deliverables:
      - HTL template
      - Proper context escaping
      - i18n externalization

  - id: ${id}-css
    type: implementation
    agent: coder
    effort: 4h
    depends_on: [${id}-htl]
    deliverables:
      - SCSS styles (BEM naming)
      - Responsive breakpoints
      - Design token usage

  - id: ${id}-js
    type: implementation
    agent: coder
    effort: 2h
    depends_on: [${id}-htl]
    deliverables:
      - JavaScript behavior (if needed)
      - Event handling
      - Accessibility enhancements

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 4h
    depends_on: [${id}-css, ${id}-js]
    deliverables:
      - Functional test cases
      - Cross-browser testing
      - Accessibility audit
      - Responsive testing

  - id: ${id}-review
    type: review
    agent: reviewer
    effort: 2h
    depends_on: [${id}-test]
    deliverables:
      - Code review approved
      - Security review passed
      - Performance review passed

  - id: ${id}-docs
    type: documentation
    agent: documenter
    effort: 2h
    depends_on: [${id}-review]
    deliverables:
      - Authoring guide
      - Component README
      - Storybook entry (if applicable)

acceptance_criteria:
  - Component renders correctly on Author and Publish
  - Dialog allows full content authoring
  - Responsive across all breakpoints
  - WCAG 2.1 AA compliant
  - Unit test coverage ≥80%
  - No security vulnerabilities
  - Documentation complete
```

### BEAD-LIB-COMP-002: Container Component

```yaml
id: ${project}-container-${name}-001
type: component-development
priority: medium
estimated_effort: 2-3 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 1h

  - id: ${id}-model
    type: implementation
    agent: coder
    effort: 3h
    notes: |
      Container model should:
      - Extend AbstractContainerImpl
      - Implement allowed components policy
      - Support responsive grid

  - id: ${id}-policy
    type: implementation
    agent: coder
    effort: 2h
    deliverables:
      - Template policy definition
      - Allowed components configuration
      - Default layout settings

  - id: ${id}-htl
    type: implementation
    agent: coder
    effort: 2h

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 2h

  - id: ${id}-review
    type: review
    agent: reviewer
    effort: 1h

acceptance_criteria:
  - Child components can be added/removed
  - Layout modes work correctly
  - Policy restrictions enforced
  - Drag-and-drop authoring works
```

### BEAD-LIB-COMP-003: Experience Fragment Component

```yaml
id: ${project}-xf-${name}-001
type: component-development
priority: medium
estimated_effort: 1-2 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 1h

  - id: ${id}-impl
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - XF template
      - XF component
      - Variation support

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 2h

acceptance_criteria:
  - XF renders in page context
  - Variations selectable
  - Cross-page updates work
  - No layout issues
```

### BEAD-LIB-COMP-004: Form Component

```yaml
id: ${project}-form-${name}-001
type: component-development
priority: high
estimated_effort: 5-8 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 2h
    deliverables:
      - Form fields specification
      - Validation rules
      - Submission handling design
      - Security review (CSRF, injection)

  - id: ${id}-model
    type: implementation
    agent: coder
    effort: 6h
    deliverables:
      - Form Sling Model
      - Field models
      - Validation service

  - id: ${id}-servlet
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Form submission servlet
      - CSRF token handling
      - Input sanitization
      - Response handling

  - id: ${id}-dialog
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Form builder dialog
      - Field type selection
      - Validation configuration

  - id: ${id}-frontend
    type: implementation
    agent: coder
    effort: 6h
    deliverables:
      - Client-side validation
      - Error display
      - Submit handling
      - Success/error states

  - id: ${id}-security
    type: review
    agent: reviewer
    effort: 3h
    deliverables:
      - OWASP review
      - Injection testing
      - CSRF verification

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 4h
    deliverables:
      - Field validation tests
      - Submission tests
      - Error handling tests
      - Accessibility audit

acceptance_criteria:
  - All field types work
  - Validation client and server side
  - CSRF protection active
  - Accessible form controls
  - Spam protection (if applicable)
  - Email/integration working
```

### BEAD-LIB-COMP-005: Search Component

```yaml
id: ${project}-search-001
type: component-development
priority: high
estimated_effort: 5-7 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 2h

  - id: ${id}-index
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Oak index definition
      - Index optimization
      - Property indexes

  - id: ${id}-service
    type: implementation
    agent: coder
    effort: 6h
    deliverables:
      - Search service
      - Query builder integration
      - Facet support
      - Pagination

  - id: ${id}-servlet
    type: implementation
    agent: coder
    effort: 3h
    deliverables:
      - Search API endpoint
      - JSON response format
      - Rate limiting

  - id: ${id}-frontend
    type: implementation
    agent: coder
    effort: 6h
    deliverables:
      - Search input component
      - Results display
      - Autocomplete
      - Filters UI

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 4h

acceptance_criteria:
  - Search returns relevant results
  - Response time <500ms
  - Facets/filters work
  - Pagination functional
  - No query injection vulnerabilities
```

---

## Integrations

### BEAD-LIB-INT-001: REST API Integration

```yaml
id: ${project}-api-${service}-001
type: integration
priority: high
estimated_effort: 3-5 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 2h
    deliverables:
      - API documentation review
      - Authentication method
      - Error handling strategy
      - Rate limiting approach

  - id: ${id}-config
    type: implementation
    agent: coder
    effort: 2h
    deliverables:
      - OSGi configuration
      - Environment-specific settings
      - Secrets management

  - id: ${id}-client
    type: implementation
    agent: coder
    effort: 6h
    deliverables:
      - HTTP client service
      - Request/response models
      - Error handling
      - Retry logic
      - Circuit breaker

  - id: ${id}-service
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Business logic service
      - Data transformation
      - Caching (if applicable)

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 4h
    deliverables:
      - Unit tests with mocks
      - Integration tests
      - Error scenario tests

  - id: ${id}-docs
    type: documentation
    agent: documenter
    effort: 2h

acceptance_criteria:
  - API calls successful
  - Error handling graceful
  - Timeouts configured
  - Retry logic working
  - Secrets not in code
  - Monitoring in place
```

### BEAD-LIB-INT-002: LLM/AI Integration

```yaml
id: ${project}-llm-${provider}-001
type: integration
priority: high
estimated_effort: 5-7 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 3h
    deliverables:
      - Provider selection (OpenAI/Claude/etc)
      - Use case definition
      - Prompt engineering strategy
      - Cost estimation
      - Security review (prompt injection)

  - id: ${id}-config
    type: implementation
    agent: coder
    effort: 2h
    deliverables:
      - OSGi configuration
      - API key management
      - Model selection
      - Token limits

  - id: ${id}-service
    type: implementation
    agent: coder
    effort: 8h
    deliverables:
      - LLM client service
      - Prompt templates
      - Response parsing
      - Error handling
      - Rate limiting
      - Token tracking

  - id: ${id}-security
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Prompt injection prevention
      - Output sanitization
      - Input validation
      - Audit logging

  - id: ${id}-component
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Author-facing component
      - Content generation UI
      - Preview functionality

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 4h
    deliverables:
      - Mock service tests
      - Security tests
      - Rate limit tests

acceptance_criteria:
  - Content generation works
  - Prompt injection prevented
  - Rate limits enforced
  - Token usage tracked
  - Graceful degradation on failure
  - Cost within budget
```

### BEAD-LIB-INT-003: Email Service Integration

```yaml
id: ${project}-email-001
type: integration
priority: medium
estimated_effort: 2-3 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 1h

  - id: ${id}-config
    type: implementation
    agent: coder
    effort: 2h
    deliverables:
      - SMTP/API configuration
      - Template path configuration
      - From address settings

  - id: ${id}-service
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Email service
      - Template rendering
      - Attachment support
      - Queue handling

  - id: ${id}-templates
    type: implementation
    agent: coder
    effort: 3h
    deliverables:
      - Email templates (HTL)
      - Responsive email CSS
      - i18n support

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 2h

acceptance_criteria:
  - Emails send successfully
  - Templates render correctly
  - Attachments work
  - Bounce handling configured
```

### BEAD-LIB-INT-004: Analytics Integration

```yaml
id: ${project}-analytics-001
type: integration
priority: medium
estimated_effort: 2-4 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: mayor
    effort: 2h
    deliverables:
      - Analytics requirements
      - Event taxonomy
      - Data layer design

  - id: ${id}-datalayer
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Data layer implementation
      - Page data population
      - Event tracking

  - id: ${id}-launch
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - Adobe Launch configuration
      - Rules and data elements
      - Extension setup

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 3h
    deliverables:
      - Data layer validation
      - Event firing tests
      - Cross-browser verification

acceptance_criteria:
  - Page views tracked
  - Custom events firing
  - Data layer populated correctly
  - No PII in analytics
  - Consent respected
```

---

## Infrastructure

### BEAD-LIB-INFRA-001: New Environment Setup

```yaml
id: ${project}-env-${name}-001
type: infrastructure
priority: high
estimated_effort: 2-3 days

tasks:
  - id: ${id}-provision
    type: implementation
    agent: devops
    effort: 4h
    deliverables:
      - Cloud Manager environment
      - Pipeline configuration
      - Domain setup

  - id: ${id}-config
    type: implementation
    agent: devops
    effort: 4h
    deliverables:
      - Environment variables
      - Secrets configuration
      - OSGi configs

  - id: ${id}-dispatcher
    type: implementation
    agent: devops
    effort: 4h
    deliverables:
      - Dispatcher rules
      - Cache configuration
      - Rewrite rules

  - id: ${id}-ssl
    type: implementation
    agent: devops
    effort: 2h
    deliverables:
      - SSL certificate
      - Domain verification
      - HTTPS redirect

  - id: ${id}-test
    type: testing
    agent: tester
    effort: 2h

acceptance_criteria:
  - Environment accessible
  - Deployments working
  - SSL configured
  - Caching effective
  - Monitoring active
```

### BEAD-LIB-INFRA-002: CI/CD Pipeline Setup

```yaml
id: ${project}-cicd-001
type: infrastructure
priority: high
estimated_effort: 3-5 days

tasks:
  - id: ${id}-build
    type: implementation
    agent: devops
    effort: 4h
    deliverables:
      - Build pipeline
      - Unit test execution
      - Code quality gates

  - id: ${id}-deploy-stage
    type: implementation
    agent: devops
    effort: 4h
    deliverables:
      - Stage deployment pipeline
      - Automated testing
      - Approval gates

  - id: ${id}-deploy-prod
    type: implementation
    agent: devops
    effort: 4h
    deliverables:
      - Production pipeline
      - Manual approval
      - Rollback capability

  - id: ${id}-notifications
    type: implementation
    agent: devops
    effort: 2h
    deliverables:
      - Slack notifications
      - Email alerts
      - Status badges

acceptance_criteria:
  - Pipelines trigger on push
  - Tests run automatically
  - Quality gates enforced
  - Notifications working
  - Rollback tested
```

---

## Testing

### BEAD-LIB-TEST-001: Regression Test Suite

```yaml
id: ${project}-regression-001
type: testing
priority: high
estimated_effort: 5-7 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: tester
    effort: 4h
    deliverables:
      - Test scope definition
      - Test case prioritization
      - Environment requirements

  - id: ${id}-smoke
    type: implementation
    agent: tester
    effort: 4h
    deliverables:
      - Smoke test suite
      - Critical path coverage
      - Automated execution

  - id: ${id}-functional
    type: implementation
    agent: tester
    effort: 8h
    deliverables:
      - Component test cases
      - Integration test cases
      - API test cases

  - id: ${id}-visual
    type: implementation
    agent: tester
    effort: 4h
    deliverables:
      - Visual regression baseline
      - Screenshot comparison
      - Responsive snapshots

  - id: ${id}-accessibility
    type: implementation
    agent: tester
    effort: 4h
    deliverables:
      - Axe integration
      - Keyboard navigation tests
      - Screen reader tests

  - id: ${id}-automation
    type: implementation
    agent: tester
    effort: 8h
    deliverables:
      - Playwright test scripts
      - CI integration
      - Parallel execution

acceptance_criteria:
  - All critical paths covered
  - Automation >60%
  - Execution time <30 min
  - Reports generated
  - CI integrated
```

### BEAD-LIB-TEST-002: Performance Test Suite

```yaml
id: ${project}-perftest-001
type: testing
priority: medium
estimated_effort: 3-5 days

tasks:
  - id: ${id}-plan
    type: planning
    agent: tester
    effort: 2h
    deliverables:
      - Performance targets
      - Test scenarios
      - Load profiles

  - id: ${id}-scripts
    type: implementation
    agent: tester
    effort: 6h
    deliverables:
      - JMeter/k6 scripts
      - User journeys
      - Data parameterization

  - id: ${id}-baseline
    type: testing
    agent: tester
    effort: 4h
    deliverables:
      - Baseline measurements
      - Bottleneck identification
      - Recommendations

  - id: ${id}-load
    type: testing
    agent: tester
    effort: 4h
    deliverables:
      - Load test execution
      - Results analysis
      - Performance report

acceptance_criteria:
  - Baseline established
  - SLAs validated
  - Bottlenecks identified
  - Recommendations documented
```

---

## Security

### BEAD-LIB-SEC-001: Security Hardening

```yaml
id: ${project}-security-hardening-001
type: security
priority: critical
estimated_effort: 3-5 days

tasks:
  - id: ${id}-audit
    type: review
    agent: security
    effort: 4h
    deliverables:
      - Current state assessment
      - Gap analysis
      - Priority list

  - id: ${id}-dispatcher
    type: implementation
    agent: devops
    effort: 4h
    deliverables:
      - Filter rules hardening
      - Sensitive path blocking
      - Security headers

  - id: ${id}-osgi
    type: implementation
    agent: coder
    effort: 4h
    deliverables:
      - CSRF filter configuration
      - Referrer filter
      - Service user mappings

  - id: ${id}-code
    type: implementation
    agent: coder
    effort: 6h
    deliverables:
      - Input validation
      - Output encoding
      - Dependency updates

  - id: ${id}-test
    type: testing
    agent: security
    effort: 4h
    deliverables:
      - DAST scan
      - OWASP verification
      - Penetration test

acceptance_criteria:
  - No critical vulnerabilities
  - OWASP Top 10 addressed
  - Security headers present
  - DAST scan clean
```

---

## Usage Example

### Sprint Planning with BEAD Library

```markdown
## Sprint 5 Planning

### Features
1. New Product Card Component → Use BEAD-LIB-COMP-001
2. Wishlist API Integration → Use BEAD-LIB-INT-001
3. Search Enhancement → Use BEAD-LIB-COMP-005

### Generated Tasks

From BEAD-LIB-COMP-001 (Product Card):
- SPRINT5-001-plan: Component planning
- SPRINT5-001-model: Sling Model
- SPRINT5-001-dialog: Touch UI dialog
- SPRINT5-001-htl: HTL template
- SPRINT5-001-css: SCSS styles
- SPRINT5-001-test: QA testing
- SPRINT5-001-review: Code review
- SPRINT5-001-docs: Documentation

From BEAD-LIB-INT-001 (Wishlist API):
- SPRINT5-002-plan: Integration planning
- SPRINT5-002-config: OSGi config
- SPRINT5-002-client: HTTP client
- SPRINT5-002-service: Business service
- SPRINT5-002-test: Integration tests

Total Story Points: 34
```

---

## Contributing

To add new patterns to this library:
1. Create YAML in appropriate category
2. Follow existing format
3. Include all standard fields
4. Add acceptance criteria
5. Submit PR for review
