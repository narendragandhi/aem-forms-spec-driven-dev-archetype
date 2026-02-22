# Level of Effort (LoE) Estimation: WordPress to AEM as a Cloud Service "Lift and Shift" Migration

This document provides a high-level Level of Effort (LoE) estimation for a "lift and shift" migration from a WordPress site to AEM as a Cloud Service, leveraging AEM WCM Core Components. This approach prioritizes re-implementing existing functionality using standard AEM features and Core Components, with minimal custom development.

## Assumptions:

*   **WordPress Site Complexity**: Medium complexity (e.g., 50-200 pages, standard blog functionality, contact forms, image galleries, 2-3 custom post types). Not a highly integrated e-commerce site or a custom application.
*   **Target AEM Platform**: AEM as a Cloud Service.
*   **AEM Components**: Maximum utilization of AEM WCM Core Components. Customization primarily via styling (CSS/Sass) and component proxy/overlay. Minimal custom Java/Sling Model development.
*   **Content Volume**: Manageable for semi-automated or automated migration scripts.
*   **Team Experience**: Project team has prior AEM development experience; some familiarity with AEMaaCS is beneficial.
*   **Design System/Accessibility/i18n**: Basic implementation to meet core requirements, not extensive enterprise-level solutions.
*   **Scope**: Focus on core website functionality; complex integrations beyond standard analytics and basic forms are considered out of scope for this LoE.

## LoE Breakdown by Phase (Approximate Person-Weeks)

The following estimates are "person-weeks," meaning the effort one person would expend in one week.

### Phase 1: Discovery & Planning (Total: 2 - 4 Person-Weeks)
*   **WordPress Content Audit & Analysis**: 1-2 weeks (Content Strategist, PM)
    *   Inventory content types, templates, plugins, assets.
    *   Identify content hierarchies, relationships, and metadata.
    *   Assess content quality and identify redundant/stale content.
*   **AEM Target State Definition (IA & Content Model Mapping)**: 1-2 weeks (AEM Architect, PM)
    *   Define target AEM site structure and information architecture.
    *   Map WordPress content types/fields to AEM Content Fragments/Pages with Core Components.
*   **Migration Strategy & Roadmap**: 0.5-1 week (PM, AEM Architect)
    *   Develop a high-level migration plan, approach (e.g., phased), and initial timelines.

### Phase 2: Content Migration Strategy & Execution (Total: 4 - 8 Person-Weeks)
*   **Content Extraction (WordPress)**: 1-2 weeks (Developer, DevOps)
    *   Export content via WordPress tools (XML) or develop custom scripts for specific data.
    *   Extract media assets.
*   **Content Transformation & Cleansing**: 2-4 weeks (AEM Developer, Architect)
    *   Develop scripts/rules to transform WordPress data into AEM-compatible formats (e.g., JSON, XML).
    *   Map fields, clean HTML, handle internal links, re-point media URLs.
*   **Content Ingestion (AEM)**: 1-2 weeks (AEM Developer, DevOps)
    *   Develop AEM ingestion routines (e.g., custom Sling servlets, AEM content package upload automation) to import transformed content.
    *   Initial ingestion into AEM development environments.

### Phase 3: Template & Component Development (Total: 6 - 10 Person-Weeks)
*   **Core Component Usage & Styling**: 3-5 weeks (AEM Developer, Frontend Developer)
    *   Identify relevant AEM Core Components for each WordPress element.
    *   Develop CSS/SCSS to apply WordPress theme's look and feel to Core Components.
    *   Customize Core Components via policies and configuration.
*   **AEM Editable Template Setup**: 1-2 weeks (AEM Developer)
    *   Create AEM Editable Templates using Core Components, defining initial page structures and allowed components.
*   **Minor Custom Component Development (if required)**: 2-3 weeks (AEM Developer)
    *   For very specific WordPress functionalities not covered by Core Components. (Minimize this for "lift and shift").
*   **Accessibility & i18n Implementation**: 1-2 weeks (AEM Developer, Frontend Developer)
    *   Ensure all components and templates meet WCAG 2.1 AA standards and are ready for multi-lingual content.

### Phase 4: Testing & Validation (Total: 4 - 6 Person-Weeks)
*   **Migration Verification**: 1-2 weeks (QA, Content Editor, PM)
    *   Verify content fidelity, links, images post-migration.
    *   Content freeze coordination.
*   **Functional & UI Testing**: 2-3 weeks (QA, AEM Developer)
    *   Automated (unit, integration) and manual testing of all new AEM components and migrated pages.
    *   Responsive design testing.
*   **Performance & Security Testing**: 1 week (QA, DevOps)
    *   Basic performance benchmarks and security scans (leveraging Cloud Manager).
*   **User Acceptance Testing (UAT)**: 1-2 weeks (Business Stakeholders, PM)
    *   Validation by key business users in a staging environment.

### Phase 5: Deployment & Post-Migration (Total: 2 - 3 Person-Weeks)
*   **Cloud Manager Pipeline Validation**: 0.5-1 week (DevOps, AEM Architect)
    *   Ensure CI/CD pipelines are robust and quality gates are met.
*   **Go-Live & Cutover Execution**: 0.5-1 week (DevOps, All team members)
    *   Final content synchronization, DNS changes, cache invalidation.
*   **Post-Migration Monitoring & Stabilization**: 1-2 weeks (DevOps, AEM Developer)
    *   Monitor site health, performance, logs, and address immediate post-launch issues.

## Overall Estimated Effort:

**Total Person-Weeks**: Approximately **18 - 31 person-weeks**.

## Translation to Calendar Time (Example):

*   **Small Team (3 people)**: Approximately 6 - 10 months.
*   **Medium Team (5 people)**: Approximately 4 - 6 months.

## Considerations for AI-Augmented Approach (BMAD, BEAD, Gastown):

The above estimates are for a human-led project. Integrating BMAD, BEAD, and Gastown could potentially **reduce the lower end of these estimates** by:

*   **Automating Content Transformation**: AI agents (Gastown orchestrated, BEAD managed) could significantly reduce manual effort and time in Phase 2.
*   **Accelerating Component Development**: AI agents could assist in code generation, styling, and basic test writing in Phase 3.
*   **Enhancing Testing Efficiency**: AI agents could generate more comprehensive test cases, perform automated content validation, and identify discrepancies faster in Phase 4.
*   **Improved Traceability & Context**: Reduced overhead from better project knowledge management.

However, the initial setup and fine-tuning of AI agents and orchestration systems would introduce a **new overhead (e.g., 2-4 weeks)**, which needs to be factored in. For a first-time implementation, this overhead might mean the overall calendar time doesn't drastically decrease initially, but subsequent projects or complex aspects of the migration would see significant gains.