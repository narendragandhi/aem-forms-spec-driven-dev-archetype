# 06 - Integrations

This phase is driven by the "Integration Architect Agent" of the BMAD framework. It focuses on defining integration patterns, external service connections, and API specifications for the AEM solution, ensuring seamless data flow between AEM and enterprise systems.

## Overview

Modern AEM implementations rarely exist in isolation. They must integrate with:
- **CRM Systems** (Salesforce, HubSpot, Microsoft Dynamics)
- **Marketing Platforms** (Adobe Analytics, Adobe Target, Google Analytics)
- **Commerce Backends** (Adobe Commerce, Salesforce Commerce Cloud)
- **Translation Services** (Smartling, Lionbridge, Google Translate)
- **DAM Systems** (Adobe Assets, Bynder, Aprimo)
- **AI/LLM Services** (OpenAI, Anthropic Claude, Google Gemini)
- **Custom Enterprise APIs** (internal services, legacy systems)

This phase provides comprehensive documentation for implementing these integrations following AEM best practices.

## Artifacts in this Phase

- **[rest-api-patterns.md](rest-api-patterns.md)**: Comprehensive guide to implementing REST APIs in AEM, including Sling Servlets, JSON Exporters, and API security patterns.

- **[external-services-integration.md](external-services-integration.md)**: Patterns for integrating AEM with external services including CRM, Analytics, Translation APIs, and third-party platforms.

- **[integration-best-practices.md](integration-best-practices.md)**: AEM-specific best practices for error handling, caching, circuit breakers, retry logic, and monitoring integrations.

- **[headless-graphql.md](headless-graphql.md)**: Guide to AEM's headless capabilities including Content Fragments, GraphQL API, and SPA integration patterns.
- **[headless-forms.md](headless-forms.md)**: Overview of Headless AEM Forms integration using BMAD patterns.

- **[osgi-services.md](osgi-services.md)**: OSGi service patterns for creating reusable, configurable integration services in AEM.

- **[ai-services.md](ai-services.md)**: Multi-provider LLM service integration (OpenAI, Claude, Gemini) with unified abstraction layer for AI-powered content features.

- **[ai-translation.md](ai-translation.md)**: AI-powered automatic translation service using LLMs, with change tracking, Live Copy integration, and workflow support.

## Integration Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        AEM as a Cloud Service                          │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   Sling     │  │   Content   │  │   Workflow  │  │   OSGi      │   │
│  │   Servlets  │  │   Fragment  │  │   Engine    │  │   Services  │   │
│  │   (REST)    │  │   GraphQL   │  │             │  │             │   │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘   │
│         │                │                │                │          │
│         └────────────────┴────────────────┴────────────────┘          │
│                                   │                                    │
│                          ┌────────▼────────┐                          │
│                          │  HTTP Client    │                          │
│                          │  Service Layer  │                          │
│                          └────────┬────────┘                          │
└───────────────────────────────────┼────────────────────────────────────┘
                                    │
        ┌───────────────┬───────────┼───────────┬───────────┬───────────┐
        ▼               ▼           ▼           ▼           ▼           ▼
┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐
│  Salesforce   │ │  Adobe    │ │Translation│ │  Commerce │ │  AI/LLM   │ │  Custom   │
│     CRM       │ │ Analytics │ │  Service  │ │  Backend  │ │ Services  │ │   APIs    │
└───────────────┘ └───────────┘ └───────────┘ └───────────┘ └───────────┘ └───────────┘
```

## BMAD Agent Responsibilities

### Integration Architect Agent
- Define integration patterns and standards
- Specify API contracts and data models
- Design error handling and resilience strategies
- Document security requirements

### Integration Developer Agent
- Implement Sling Servlets and OSGi services
- Create HTTP client wrappers
- Build workflow process steps for integrations
- Write integration tests

### Integration QA Agent
- Validate API contracts
- Test error scenarios and edge cases
- Verify security implementations
- Performance test integration endpoints

## Key Principles

1. **Loose Coupling**: Integrations should be decoupled from core AEM functionality
2. **Configuration-Driven**: Use OSGi configurations for environment-specific settings
3. **Resilience**: Implement circuit breakers, retries, and fallbacks
4. **Observability**: Log, monitor, and trace all integration calls
5. **Security**: Follow OAuth 2.0, API key management, and secret handling best practices
