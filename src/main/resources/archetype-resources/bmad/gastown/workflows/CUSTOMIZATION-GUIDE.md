# Production Readiness Workflow - Customization Guide

## Overview

The `production-readiness.yaml` workflow is fully customizable for any AEM project. This guide explains how to adapt it to your specific requirements.

---

## Quick Start

### 1. Copy and Rename

```bash
cp bmad/gastown/workflows/production-readiness.yaml \
   bmad/gastown/workflows/my-project-prod-readiness.yaml
```

### 2. Update Project Config

```yaml
config:
  project:
    name: "my-aem-project"
    group_id: "com.mycompany.aem"
    package_base: "com.mycompany.aem.core"
    resource_type_prefix: "myproject"
```

### 3. Enable/Disable Integrations

```yaml
config:
  integrations:
    llm:
      enabled: false  # Disable if not using AI
    email:
      enabled: true
      provider: mailchimp  # Change provider
    analytics:
      enabled: false  # Disable if not using Adobe Analytics
```

### 4. Run Workflow

```bash
# Using Claude Code as Mayor AI
@workspace Execute the production-readiness workflow from
bmad/gastown/workflows/my-project-prod-readiness.yaml
```

---

## Configuration Reference

### Project Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `project.name` | Maven artifact name | aem-bmad-showcase |
| `project.group_id` | Maven group ID | com.example.aem.bmad |
| `project.package_base` | Java package root | com.example.aem.bmad.core |
| `project.resource_type_prefix` | Component resource type prefix | bmad-showcase |

### Build Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `build.java_version` | Java version | 21 |
| `build.node_version` | Node.js version | 18.19.0 |
| `build.npm_version` | npm version | 9.8.1 |

### Integration Settings

#### LLM Integration

```yaml
integrations:
  llm:
    enabled: true
    providers:
      - name: claude
        class: ClaudeServiceImpl
        secret_key: CLAUDE_API_KEY
      - name: openai
        class: OpenAIServiceImpl
        secret_key: OPENAI_API_KEY
      # Add more providers:
      - name: gemini
        class: GeminiServiceImpl
        secret_key: GEMINI_API_KEY
```

#### Email Integration

```yaml
integrations:
  email:
    enabled: true
    provider: sendgrid  # sendgrid, mailchimp, ses, smtp
    class: EmailServiceImpl
    secret_key: EMAIL_API_KEY
```

#### Analytics Integration

```yaml
integrations:
  analytics:
    enabled: true
    provider: adobe_analytics  # adobe_analytics, google_analytics, mixpanel
```

### Testing Settings

```yaml
testing:
  unit_coverage_target: 80      # Minimum coverage %
  integration_tests: true        # Enable integration tests
  e2e_tests: true               # Enable Playwright tests
  accessibility_tests: true      # Enable a11y tests
  performance_tests: true        # Enable k6 tests
```

### Resilience Settings

```yaml
resilience:
  circuit_breaker:
    enabled: true
    failure_threshold: 50        # % failures to open circuit
    wait_duration_seconds: 30    # Time before half-open
  retry:
    max_attempts: 3
    backoff_multiplier: 2
  rate_limiting:
    enabled: true
```

---

## Customizing Tasks

### Disable a Task

```yaml
tasks:
  - id: PROD-007
    name: Circuit Breaker
    enabled: false  # Skip this task
```

### Modify Task Actions

```yaml
tasks:
  - id: PROD-003
    name: Service Unit Tests
    actions:
      - create_service_tests
      - create_mock_factories
      # Remove or add actions:
      # - add_error_scenario_tests  # Removed
      - add_performance_tests       # Added
```

### Add Custom Validation Rules

```yaml
tasks:
  - id: PROD-004
    validation_rules:
      urls:
        patterns: ["^/", "^https://"]
        max_length: 2048
        # Add custom patterns:
        allowed_domains:
          - "example.com"
          - "cdn.example.com"
      text:
        max_length: 10000
        # Add custom rules:
        forbidden_words:
          - "password"
          - "secret"
```

### Add Custom Services to Test

```yaml
tasks:
  - id: PROD-003
    services_to_test:
      # Add your custom services:
      - name: PaymentService
        enabled: true
        test_scenarios:
          - process_payment_success
          - payment_declined
          - timeout_handling
          - refund_processing
```

---

## Adding New Phases

### Example: Add Compliance Phase

```yaml
phases:
  - name: compliance
    description: Regulatory compliance checks
    depends_on: [infrastructure]
    tasks:
      - id: CUSTOM-001
        name: GDPR Compliance
        agent: aem-component-coder
        type: automated
        actions:
          - add_consent_management
          - add_data_export_endpoint
          - add_data_deletion_endpoint
        files:
          - "${config.package_base}/gdpr/*.java"

      - id: CUSTOM-002
        name: Cookie Consent
        agent: aem-component-coder
        type: automated
        actions:
          - create_cookie_banner_component
          - add_consent_storage
          - integrate_with_analytics
```

---

## Environment Variables

The workflow supports environment variables for sensitive or environment-specific values:

```bash
# Set before running workflow
export PROJECT_NAME="my-aem-project"
export GROUP_ID="com.mycompany.aem"
export MONITORING_PROVIDER="datadog"
export SLACK_WEBHOOK="https://hooks.slack.com/..."
```

Or use a `.env` file:

```bash
# .env
PROJECT_NAME=my-aem-project
GROUP_ID=com.mycompany.aem
MONITORING_PROVIDER=datadog
```

---

## Conditional Execution

### Based on Integration Status

```yaml
tasks:
  - id: PROD-018
    name: Analytics Testing
    condition: "${config.integrations.analytics.enabled}"
    # Only runs if analytics is enabled
```

### Based on Environment

```yaml
tasks:
  - id: PROD-013
    name: Performance Testing
    condition: "${ENV:-dev} == 'stage'"
    # Only runs in stage environment
```

---

## Human Tasks Customization

### Mark Task as Manual

```yaml
human_tasks:
  - id: CUSTOM-MANUAL-001
    name: PCI Compliance Audit
    type: manual
    reason: Requires certified auditor
    instructions:
      - Schedule audit with QSA
      - Provide system access
      - Complete SAQ questionnaire
    verification:
      - Receive AOC (Attestation of Compliance)
      - Update compliance documentation
```

### Convert Manual to Automated

If you have tooling that allows automation of a "manual" task:

```yaml
# Move from human_tasks to phases
phases:
  - name: security
    tasks:
      - id: PROD-014
        name: Security Scanning
        type: automated  # Changed from manual
        actions:
          - run_dependency_check
          - run_sonar_scan
          - generate_report
        # Now requires the tools to be installed
```

---

## Multi-Project Setup

For organizations with multiple AEM projects:

### 1. Create Base Workflow

```yaml
# workflows/base-prod-readiness.yaml
config:
  # Shared settings
  build:
    java_version: 21
    node_version: "18.19.0"

  resilience:
    circuit_breaker:
      enabled: true
      failure_threshold: 50

# Common phases...
```

### 2. Create Project-Specific Overrides

```yaml
# workflows/project-a-prod-readiness.yaml
extends: base-prod-readiness.yaml

config:
  project:
    name: "project-a"
    group_id: "com.company.project.a"

  integrations:
    llm:
      enabled: true
    analytics:
      enabled: false  # Project A doesn't use analytics
```

```yaml
# workflows/project-b-prod-readiness.yaml
extends: base-prod-readiness.yaml

config:
  project:
    name: "project-b"
    group_id: "com.company.project.b"

  integrations:
    llm:
      enabled: false  # Project B doesn't use LLM
    analytics:
      enabled: true
```

---

## Execution Options

### Full Workflow

```bash
# Run all phases
gastown execute production-readiness.yaml
```

### Single Phase

```bash
# Run only testing phase
gastown execute production-readiness.yaml --phase service_testing
```

### Single Task

```bash
# Run only circuit breaker task
gastown execute production-readiness.yaml --task PROD-007
```

### Dry Run

```bash
# Preview without executing
gastown execute production-readiness.yaml --dry-run
```

### Skip Review

```bash
# Auto-merge without human review (use carefully)
gastown execute production-readiness.yaml --no-review
```

---

## Output Customization

### Change Report Location

```yaml
output:
  report_path: docs/production-readiness-report.md
```

### Disable BEAD Tracking

```yaml
execution:
  create_bead_issues: false
```

### Custom Branch Names

```yaml
execution:
  branching:
    base: develop
    feature_prefix: "feature/prod-hardening/"
```

---

## Integration with CI/CD

### Cloud Manager Integration

```yaml
# Add to .cloudmanager/maven/settings.xml
execution:
  ci_integration:
    cloud_manager:
      enabled: true
      pipeline: "prod-readiness-validation"
```

### GitHub Actions Integration

```yaml
# .github/workflows/prod-readiness.yml
name: Production Readiness
on:
  workflow_dispatch:
    inputs:
      phase:
        description: 'Phase to run'
        required: false
        default: 'all'

jobs:
  execute:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run GasTown Workflow
        run: |
          npx gastown execute \
            bmad/gastown/workflows/production-readiness.yaml \
            --phase ${{ github.event.inputs.phase }}
```

---

## Troubleshooting

### Task Fails

1. Check BEAD issue for error details:
   ```bash
   cat bmad/gastown/bead/.issues/prod-ready/PROD-XXX.md
   ```

2. Re-run single task:
   ```bash
   gastown execute production-readiness.yaml --task PROD-XXX --retry
   ```

### Dependency Conflict

```yaml
# Adjust phase dependencies
phases:
  - name: my_phase
    depends_on: []  # Remove dependencies to run independently
```

### Missing Files

```yaml
# Add file creation to task
tasks:
  - id: PROD-XXX
    actions:
      - create_directory: "${config.package_base}/newpackage"
      - create_file: "..."
```

---

## Best Practices

1. **Version Your Workflow**: Keep workflow file in version control
2. **Use Environment Variables**: Don't hardcode secrets or environment-specific values
3. **Start Small**: Enable only what you need, add more later
4. **Review Generated Code**: Always review before merging
5. **Test in Stage First**: Run workflow against stage before production
6. **Document Customizations**: Add comments explaining why you changed defaults

---

## Examples

### Minimal E-commerce Project

```yaml
config:
  project:
    name: "ecommerce-site"
  integrations:
    llm:
      enabled: false
    email:
      enabled: true
      provider: ses
    analytics:
      enabled: true
      provider: google_analytics
  testing:
    unit_coverage_target: 70
    e2e_tests: true
```

### Content-Heavy Publishing Site

```yaml
config:
  project:
    name: "news-publisher"
  integrations:
    llm:
      enabled: true  # For content generation
      providers:
        - name: claude
    analytics:
      enabled: true
  resilience:
    circuit_breaker:
      enabled: false  # Less critical for content sites
```

### Enterprise B2B Platform

```yaml
config:
  project:
    name: "enterprise-portal"
  integrations:
    llm:
      enabled: true
    email:
      enabled: true
    analytics:
      enabled: true
    personalization:
      enabled: true
  testing:
    unit_coverage_target: 90  # Higher standards
  resilience:
    circuit_breaker:
      failure_threshold: 30  # More sensitive
```
