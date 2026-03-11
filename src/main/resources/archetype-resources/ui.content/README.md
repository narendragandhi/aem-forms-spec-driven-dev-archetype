# UI.content Module

This module contains the `ui.content` content package for the AEM Forms BMAD project. It includes all the mutable sample content for the application, such as pages, and Adaptive Forms.

## Main Dependencies

*   **UI.apps Module:** This module depends on the `ui.apps` module for the application's components and templates.

## Building

This module is built as part of the main project build. To build it individually, you can run the following command from the root of the project:

```bash
mvn clean install -pl ui.content -am
```

## Contents

This module contains the following:

*   **Sample Pages:** Sample pages for the application are located under `/content/<appName>`.
*   **Adaptive Forms:** Sample Adaptive Forms are located under `/content/forms/af/<appName>`.
*   **DAM Assets:** Sample assets for the application are located under `/content/dam/<appName>`.
*   **Content Fragments:** Sample content fragments are located under `/content/dam/<appName>/content-fragments`.
