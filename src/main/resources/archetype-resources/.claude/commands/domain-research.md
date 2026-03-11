---
description: Research domain knowledge for AEM Forms project requirements
---

# Domain Research Workflow

You are initiating the **Domain Research** workflow from BMAD v6.

## Purpose
Conduct thorough domain research to inform project requirements and architecture decisions for AEM Forms implementations.

## Research Areas

### 1. Industry Domain
- Industry-specific regulations and compliance
- Common workflows and processes
- Standard terminology and concepts
- Integration patterns typical in this domain

### 2. AEM Forms Domain
- Adaptive Forms best practices
- Form Data Model (FDM) patterns
- Document Services capabilities
- Interactive Communications use cases

### 3. User Domain
- Target user personas
- User journey mapping
- Accessibility requirements
- Multi-language/locale needs

### 4. Technical Domain
- Integration endpoints and APIs
- Data storage requirements
- Security and compliance needs
- Performance expectations

## Output Structure

Generate research findings in:
```
bmad/01-Business-Discovery/domain-research/
├── industry-analysis.md
├── user-research.md
├── technical-landscape.md
└── recommendations.md
```

## Key Questions to Answer

1. What forms/documents does the organization need?
2. What data sources will forms integrate with?
3. What approval workflows are required?
4. What compliance requirements exist (GDPR, HIPAA, etc.)?
5. What is the expected form submission volume?

## References
- `bmad/06-Integrations/` for integration patterns
- `bmad/07-Operations/06-gdpr-privacy-compliance-guide.md` for compliance

Begin by asking about the project domain and industry context.
