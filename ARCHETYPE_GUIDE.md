### AEM Forms BMAD Archetype Quickstart Guide

This guide provides a quick introduction to generating a new AEM project based on the AEM Forms BMAD (BeAD, GasTown) methodology using this Maven Archetype.

#### 1. Prerequisites

Before you begin, ensure you have the following installed:

*   **Java Development Kit (JDK):** Version 11 or higher (Java 21 for AEM as a Cloud Service projects).
*   **Apache Maven:** Version 3.8.6 or higher.
*   **AEM SDK:** Downloaded and configured (if targeting AEM as a Cloud Service).

#### 2. Generating a New Project

To generate a new AEM Forms BMAD project, navigate to your desired parent directory and execute the following Maven command. You will be prompted to enter values for `groupId`, `artifactId`, `version`, `package`, and `appName`.

```bash
mvn archetype:generate 
  -DarchetypeGroupId=com.example.aem.archetype 
  -DarchetypeArtifactId=aem-forms-bmad-archetype
  -DarchetypeVersion=1.0.0-SNAPSHOT 
  -DgroupId=com.mycompany 
  -DartifactId=my-forms-project 
  -Dversion=1.0.0-SNAPSHOT 
  -Dpackage=com.mycompany.core 
  -DappName=MyFormsApp 
  -DinteractiveMode=true
```

**Explanation of Properties:**

*   `-DgroupId`: The Maven Group ID for your new project (e.g., `com.mycompany`).
*   `-DartifactId`: The Maven Artifact ID for your new project (e.g., `my-forms-project`).
*   `-Dversion`: The version of your new project (e.g., `1.0.0-SNAPSHOT`).
*   `-Dpackage`: The base Java package for your project's Java code (e.g., `com.mycompany.core`). This will affect your `core` bundle's package structure.
*   `-DappName`: A short, descriptive name for your application (e.g., `MyFormsApp`). This is used in various AEM paths and display labels.

#### 3. Generated Project Structure Overview

The archetype will generate a multi-module Maven project with a structure similar to a standard AEM project, tailored for AEM Forms BMAD development:

*   `pom.xml`: Parent POM for the entire project.
*   `core/`: Contains OSGi bundles with Java services, servlets, and models.
*   `ui.apps/`: Contains AEM component definitions, clientlibs, and HTL scripts.
*   `ui.content/`: Contains mutable content, initial pages, and Adaptive Forms structures.
*   `ui.config/`: OSGi configurations for various services.
*   `ui.frontend/`: Frontend build setup (React-based SPA).
*   `all/`: An "all" content package that aggregates all other content packages and OSGi bundles for easy deployment.
*   `it.tests/`: Integration tests for the core logic.
*   `ui.tests/`: UI tests (e.g., Cypress) for the frontend.
*   `dispatcher/`: Apache/Dispatcher configurations for AEM as a Cloud Service.

#### 4. Building the Generated Project

After generating the project, navigate into the new project's root directory (`cd my-forms-project`) and build it using Maven:

```bash
mvn clean install
```

This command will compile all modules, run unit tests, and assemble the content packages and OSGi bundles.

#### 5. Deploying to AEM

Once the project is built successfully, you can deploy the `all` package to your AEM instance (local or Cloud Service).

*   **Local AEM SDK:**
    ```bash
    # From the project root (my-forms-project)
    mvn clean install -PautoInstallSinglePackage
    ```
*   **AEM as a Cloud Service:** The generated `all` package is ready for deployment via Cloud Manager.

#### 6. Next Steps

*   **Explore Complex Examples:** The archetype includes advanced examples to jumpstart development:
    *   **Financial Services Form:** A multi-step wizard with dynamic tables for employment history (`/content/forms/af/MyFormsApp/financial-application`).
    *   **Annual Account Statement:** A dynamic Interactive Communication template with auto-expanding transaction tables (`/content/forms/ic/MyFormsApp/annual-statement`).
    *   **Mock Data Source:** A pre-configured servlet at `/bin/bmad/mock-finance-data` to simulate external API responses for FDM.
    *   **Headless Orchestration:** A "BFF" service at `/bin/bmad/headless-form-service` to bridge AEM forms with React/EDS headless consumers.
*   Customize existing components or create new ones in `core/` and `ui.apps/`.
*   Develop your frontend in `ui.frontend/`.
*   Create and manage your Adaptive Forms and content in `ui.content/`.
### 7. Current Limitations and Known Issues

This archetype is functional and successfully generates a compilable AEM project. However, it's important to be aware of the following points:

#### 7.1. FileVault Validation Bypass

During the archetype's own integration tests, FileVault package validation for the `ui.apps` and `all` content packages is explicitly skipped using `<skipValidation>true</skipValidation>`.

*   **Implication:** This was a necessary workaround to achieve a successful build in the archetype's integration tests due to strict validation rules conflicting with the archetype's generation process. It means that the `filter.xml` configurations and potentially other `.content.xml` structures within the archetype might still have underlying issues that would cause validation failures if `skipValidation` were removed.
*   **Recommendation:** For a production-ready archetype, these validation issues should be properly addressed by refining the `filter.xml`s and content structures so that `skipValidation` can be removed.

#### 7.2. Outdated Dependencies and Build Tool Warnings

The archetype currently generates projects that may contain references to outdated versions of certain tools and dependencies:

*   **AEM Analyser Plugin:** Warnings about outdated `aemanalyser-maven-plugin` versions may appear during the build of the generated project.
*   **AEM SDK API:** Warnings about outdated `aem-sdk-api` versions may appear, indicating that the generated project is not immediately aligned with the very latest AEM SDK.
*   **NPM Warnings & Vulnerabilities:** The `ui.frontend` module's `npm ci` step reports deprecated packages and security vulnerabilities (low, moderate, high, and critical).
    *   **Implication:** This introduces known security risks and technical debt into any project generated by the archetype from day one.
    *   **Recommendation:** It is highly recommended to update the `ui.frontend/package.json` and associated configurations within the archetype resources to address these warnings and vulnerabilities by using up-to-date versions of frontend dependencies.

### 8. Future Considerations and Improvements

*   **Refine FileVault Filters:** Properly configure `filter.xml` files in `ui.apps` and `all` modules to pass FileVault validation without skipping it.
*   **Update Dependencies:** Ensure all Maven and NPM dependencies within the archetype are updated to their latest stable and secure versions.
*   **Comprehensive Testing:** Expand integration tests to cover deployment scenarios and basic functional validation on a running AEM instance (if feasible within an archetype IT setup).
*   **Documentation for BMAD/BEAD/GasTown Patterns:** Provide more in-depth documentation within the generated project explaining how the BMAD, BEAD, and GasTown patterns are implemented and how to extend them.
*   **Configurability Options:** Explore adding more archetype properties to allow users to configure aspects of the generated project related to the BMAD/BEAD/GasTown patterns.
