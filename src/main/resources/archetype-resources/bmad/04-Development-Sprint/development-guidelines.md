# Development Guidelines

This document contains the coding standards, branching strategy, and other development guidelines for the project. These guidelines are crucial for maintaining code quality, ensuring consistency, and meeting project requirements such as design system adherence, accessibility, and multi-lingual support.

## Coding Standards

- **Java**: Follow the Google Java Style Guide.
- **HTL**: Use the official AEM HTL style guide.
- **CSS**: Use the BEM naming convention, leveraging design system tokens for styling.

## Design System Adherence

- All new components and modifications to existing ones must strictly adhere to the project's [Design System Integration](../02-Model-Definition/design-system.md) guidelines.
- Developers should utilize provided design tokens (colors, fonts, spacing) and component patterns to maintain visual consistency.

## Accessibility Guidelines (WCAG 2.1 AA)

- All UI components and rendered content must be developed with accessibility in mind.
- **Semantic HTML**: Use appropriate HTML5 semantic elements (e.g., `<header>`, `<nav>`, `<main>`, `<aside>`, `<footer>`, `<article>`, `<section>`).
- **Keyboard Navigation**: Ensure all interactive elements are keyboard-navigable and have clear focus indicators.
- **ARIA Attributes**: Use WAI-ARIA attributes where necessary to enhance accessibility for assistive technologies.
- **Color Contrast**: Verify sufficient color contrast for all text and interactive elements.
- **Alt Text**: All meaningful images must have descriptive `alt` attributes.
- **Form Labels**: All form fields must have properly associated labels.

## Internationalization (i18n) Guidelines

- All user-facing text and messages must be externalized using AEM's i18n framework.
- Avoid hardcoding text directly into HTL templates or Sling Models.
- Ensure that components support right-to-left (RTL) languages if required by the project.

## Branching Strategy

- **main**: This branch is for production releases and is protected.
- **develop**: This is the main development branch. All feature branches are merged into this branch.
- **feature/{ticket-number}**: Each new feature or bug fix should be developed in its own feature branch.

## Code Reviews

- All code must be reviewed by at least one other developer before being merged into the `develop` branch.
- The reviewer should check for correctness, adherence to coding standards, accessibility, i18n readiness, and test coverage.

## Local Development Environment

For local development, developers should use the AEM as a Cloud Service SDK. This allows developers to emulate the cloud environment on their local machines.

### Setup

1.  **Download the AEM SDK**: Download the AEM SDK from the Adobe Software Distribution portal.
2.  **Install the SDK**: Follow the instructions to install the SDK on your local machine.
3.  **Start the SDK**: Start the AEM author and publish instances.
4.  **Install the code**: Deploy the project code to the local SDK using the following command:

    ```bash
    mvn clean install -PautoInstallPackage
    ```

### Local vs. Cloud Environment

While the local SDK provides a good emulation of the cloud environment, there are some differences. Developers should be aware of these differences and should regularly deploy their code to a cloud development environment to ensure that it works as expected.

## AI Agent Development with BEAD

When AI agents are involved in development tasks as part of the BMAD framework, they will utilize the BEAD system for enhanced task management, context persistence, and dependency tracking.

-   **Task Breakdown**: AI agents will break down larger BMAD tasks (e.g., "Develop Hero Component") into granular, manageable issues within BEAD.
-   **Persistent Context**: BEAD will serve as the agent's persistent memory, storing relevant design decisions, code snippets, requirements, and execution logs. This prevents context loss over long-running tasks.
-   **Dependency Management**: AI agents will track dependencies between their BEAD issues, ensuring tasks are executed in the correct order and preventing conflicts.
-   **Git-backed Traceability**: All agent actions and task states managed by BEAD are version-controlled in Git, providing full traceability and an audit trail for human oversight and debugging.

## Unit Testing

- All new Java code must have corresponding JUnit tests.
- The test coverage should be at least 80%.
