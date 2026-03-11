# UI.apps Module

This module contains the `ui.apps` content package for the AEM Forms BMAD project. It includes all the immutable application content, such as component definitions, client-side libraries, and templates.

## Main Dependencies

*   **Core Module:** This module depends on the `core` module for the backend logic.
*   **UI.frontend Module:** This module depends on the `ui.frontend` module for the compiled frontend assets.
*   **AEM SDK API:** Provides the standard AEM APIs.
*   **AEM Forms SDK API:** Provides APIs specific to AEM Forms.

## Building

This module is built as part of the main project build. To build it individually, you can run the following command from the root of the project:

```bash
mvn clean install -pl ui.apps -am
```

## Contents

This module contains the following:

*   **Components:** All the AEM components for the application are located under `/apps/<appName>/components`.
*   **Client-side Libraries:** The compiled CSS and JavaScript from the `ui.frontend` module are embedded here.
*   **Templates:** Editable templates for creating pages and Adaptive Forms are located under `/apps/<appName>/templates`.
*   **OSGi Configurations:** OSGi configurations for the application are located under `/apps/<appName>/config`.
