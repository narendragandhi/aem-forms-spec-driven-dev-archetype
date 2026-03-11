---
description: Research technical solutions and patterns for AEM Forms implementation
---

# Technical Research Workflow

You are initiating the **Technical Research** workflow from BMAD v6.

## Purpose
Research technical solutions, patterns, and best practices for AEM Forms implementation decisions.

## Research Categories

### 1. AEM Forms Patterns
- Adaptive Forms vs HTML5 Forms
- Form Data Model (FDM) design patterns
- Prefill service strategies
- Custom submit actions
- Form fragments and templates

### 2. Integration Patterns
- REST API integration
- SOAP web services
- Adobe Sign integration
- Document Services integration
- Third-party system connectors

### 3. Performance Patterns
- Form caching strategies
- Lazy loading for complex forms
- CDN and Dispatcher optimization
- Form Data Model caching

### 4. Security Patterns
- CSRF protection
- Input validation
- Data encryption
- PII handling
- Compliance (GDPR, HIPAA)

### 5. Accessibility Patterns
- WCAG 2.1 AA compliance
- Screen reader compatibility
- Keyboard navigation
- Color contrast and visual design

## Output Structure

Generate research in:
```
bmad/03-Architecture-Design/technical-research/
├── pattern-analysis.md
├── integration-options.md
├── security-considerations.md
├── performance-recommendations.md
└── decision-log.md
```

## Decision Record Template

```markdown
## Decision: {Title}

**Date**: {date}
**Status**: Proposed | Accepted | Deprecated

### Context
{Why is this decision needed?}

### Options Considered
1. **Option A**: {description}
   - Pros: {list}
   - Cons: {list}
2. **Option B**: {description}
   - Pros: {list}
   - Cons: {list}

### Decision
{Which option was chosen and why}

### Consequences
{What are the implications}
```

## References
- `bmad/06-Integrations/` for integration guides
- `bmad/03-Architecture-Design/system-architecture.md` for architecture context

What technical area would you like to research?
