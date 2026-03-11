# Core Module

This module contains the core OSGi bundle for the AEM Forms BMAD project. It includes all the backend logic, such as Sling Models, Servlets, and Services.

## Main Dependencies

*   **AEM SDK API:** Provides the standard AEM APIs.
*   **AEM Forms SDK API:** Provides APIs specific to AEM Forms.
*   **Core WCM Components:** Provides the Core Components for WCM.
*   **AEM Mocks and JUnit:** Used for unit testing.

## Building

This module is built as part of the main project build. To build it individually, you can run the following command from the root of the project:

```bash
mvn clean install -pl core -am
```

## Contents

This module contains the following:

*   **Sling Models:** For encapsulating business logic for AEM components.
*   **Servlets:** For handling custom Sling requests.
*   **OSGi Services:** For providing reusable services to other parts of the application.
*   **Filters:** For intercepting requests and responses.
