# Sprint 1 Plan (AEM Forms)

This document outlines the goals, user stories, and tasks for the first development sprint, focused on AEM Forms implementation.

## Sprint Goal

- To build the foundational elements for Adaptive Forms, including a custom component and a basic form template, demonstrating data binding with Form Data Model (FDM).

## User Stories for this Sprint

- As a Form Author, I want to be able to create an "Address Lookup" custom component in an Adaptive Form.
- As a Form Author, I want to be able to use a "Standard Application Template" to create new forms.
- As an End User, I want to be able to enter my address using an "Address Lookup" component that suggests addresses as I type.

## Detailed Tasks for Sprint 1

### Custom Component: Address Lookup

- **Story**: As a developer, I need to build the "Address Lookup" custom Adaptive Form component.
    - **Task (Frontend - React)**: Create the React component `CustomAddressField.js` in `ui.frontend.react.forms.af` that renders an input field and integrates with an external address suggestion API (mocked for now).
    - **Task (Frontend - Integration)**: Wrap the React component using `@aem-forms/af-react-components` `Field` to ensure proper integration with the AEM Forms rule engine and data binding.
    - **Task (Content - Dialog)**: Create the component dialog (`_cq_dialog/.content.xml`) with fields for `label`, `description`, and `bindRef` (FDM path).
    - **Task (Backend - Service)**: (Placeholder) Define an OSGi configuration for the external address API key and create a Sling Model to expose it securely to the frontend.

### Adaptive Form Template: Standard Application

- **Story**: As a developer, I need to create a "Standard Application Template" for Adaptive Forms.
    - **Task (Content - Template)**: Create the initial template structure in `/conf/aem-forms-bmad-showcase/settings/wcm/templates/standard-application`.
    - **Task (Content - Initial Content)**: Define the initial content structure for the template, including a basic header and footer, and a panel for form fields.
    - **Task (Content - Theme)**: Ensure the template is configured to use the "Global Brand Theme".

### FDM Integration: User Profile

- **Story**: As a developer, I need to integrate the "User Profile FDM" into an Adaptive Form.
    - **Task (Backend - FDM)**: Define the "User Profile FDM" structure (if not already done) in AEM Forms, ensuring it has fields for `firstName`, `lastName`, `email`, and `address` (complex object).
    - **Task (Frontend - Data Binding)**: Bind the "Address Lookup" component and other relevant fields in the "Standard Application Template" to the appropriate paths within the "User Profile FDM".

### Testing

- **Story**: As a QA Engineer, I need to create and execute test cases for Sprint 1 deliverables.
    - **Task**: Write unit tests (Jest/React Testing Library) for the `CustomAddressField.js` React component.
    - **Task**: Manually test the "Address Lookup" component's authoring experience and functionality within a form.
    - **Task**: Write an automated UI test script using Cypress to verify the "Address Lookup" component's end-to-end behavior within a sample form.
    - **Task**: Report any bugs found in the project's bug tracking system.

### Cloud Manager Compliance

- **Story**: As a Developer, I need to ensure the project remains compliant with Cloud Manager quality gates.
    - **Task**: Run a full local build and deploy the code to it for initial validation.
    - **Task**: Trigger a non-production pipeline in Cloud Manager to verify the build, code quality, and deployment process.
    - **Task**: Investigate and fix any issues reported by the Cloud Manager pipeline.
