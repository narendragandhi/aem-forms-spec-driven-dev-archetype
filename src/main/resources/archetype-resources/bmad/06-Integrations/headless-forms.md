# Headless AEM Forms with BMAD - ${appName}

This document describes the implementation of headless Adaptive Forms within the ${appName} project.

## Architecture

The headless forms architecture consists of:

1.  **AEM Adaptive Form**: The source of truth for the form model.
2.  **Headless Form Service (BFF)**: An OSGi servlet at `/bin/bmad/headless-form-service`.
3.  **Headless Submit Servlet**: A mock servlet at `/bin/bmad/headless-submit`.
4.  **React Frontend Module**: Located in `ui.frontend.react.forms.af`.

## Components

### 1. Backend Services (Core)

*   **`${package}.services.HeadlessFormService`**: Metadata orchestration for forms.
*   **`${package}.services.HeadlessSubmitServlet`**: Mock submission handler.

### 2. Frontend Module (React)

The `ui.frontend.react.forms.af` module uses Adobe's Headless Forms SDK.

### 3. Custom Components

Custom React components are developed in `ui.frontend.react.forms.af/src/main/webpack/components`.

Example: `CustomAddressField.js`

## How to use

1.  **Generate a Form**: Create an Adaptive Form in AEM.
2.  **Access Headless View**: Navigate to the React app with the `formPath` parameter.

## BMAD Benefits for Headless

*   **Model-First Design**: Unified form structure.
*   **Metadata Orchestration**: Versioning and agent hints.
*   **Decoupled Development**: Independent React development.
