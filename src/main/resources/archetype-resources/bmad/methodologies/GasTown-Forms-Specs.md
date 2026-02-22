# Gas Town Orchestration Specifications for AEM Forms Implementation

This document refines the Gas Town AI agent orchestration system's specifications, detailing its role and agent interactions specifically within an AEM Forms development context. Building upon the `BMAD-BEAD-GasTown.md` overview, this provides a more practical blueprint for implementing a Gas Town-like system for Adaptive Forms.

## 1. Mayor AI's Orchestration Role in AEM Forms Projects

The **Mayor AI** is the central orchestrator, translating high-level BMAD tasks into actionable, multi-agent workflows. For AEM Forms, its key responsibilities include:

*   **Task Interpretation:** Decomposing BMAD tasks (e.g., "Develop Loan Application Form") into smaller, specialized sub-tasks.
*   **Agent Selection & Delegation:** Identifying the most suitable specialized AI agents (e.g., Forms React Coder, FDM Architect) for each sub-task based on their defined expertise.
*   **Context Management:** Ensuring each agent receives the necessary input and context (e.g., FDM definition, component spec) and that their outputs are integrated correctly.
*   **Progress Monitoring:** Tracking the status of delegated tasks and managing dependencies between agents.
*   **Error Handling & Retry Logic:** Implementing strategies for when an agent fails or produces unsatisfactory output.
*   **Artifact Aggregation:** Collecting and validating outputs from all agents to form the final deliverable.

## 2. Forms-Specific Agent Interfaces (Inputs & Outputs)

Each specialized AI agent within the Gas Town ecosystem will have well-defined interfaces for interaction.

### 2.1. AEM Forms FDM Architect Agent

*   **Responsibility:** Designs Form Data Models (FDM) and related schemas based on business requirements and available data sources.
*   **Input:**
    *   `task_id`: Unique identifier for the current Gas Town task.
    *   `form_business_requirements`: Detailed business requirements for the form (e.g., "capture applicant's personal info," "integrate with CRM for pre-fill").
    *   `data_source_spec`: Available data sources (e.g., "CRM REST API at `api.crm.com/users`," "SQL Database `loan_db`").
    *   `existing_fdm_path` (Optional): Path to an existing FDM to extend or modify.
*   **Output:**
    *   `fdm_definition_xml`: JCR `.content.xml` for the FDM (e.g., `/conf/app/settings/fdm/loan-fdm/.content.xml`).
    *   `json_schema_file`: JSON Schema file for the FDM (e.g., `/conf/app/settings/fdm/loan-fdm/schemas/loan.schema.json`).
    *   `service_integration_specs`: Details for services to be implemented (e.g., "REST service to `api.crm.com/users/get`").
    *   `fdm_details_markdown`: Human-readable summary of the FDM structure and rationale.

### 2.2. AEM Forms React Coder Agent

*   **Responsibility:** Develops custom React components for Adaptive Forms, including the React code, authoring dialogs, and clientlib configurations.
*   **Input:**
    *   `task_id`: Unique identifier.
    *   `component_spec`: Detailed component design (from BMAD Architect, e.g., for "Address Lookup Component"). Includes UI/UX requirements, data binding, and expected behavior.
    *   `fdm_schema_context`: Relevant parts of the FDM schema for data binding.
    *   `theme_context`: Current theme styling guidelines.
*   **Output:**
    *   `react_component_code`: React component source (`.js` file for `ui.frontend.react.forms.af`).
    *   `dialog_content_xml`: JCR `.content.xml` for the authoring dialog (`ui.apps`).
    *   `clientlib_config_xml`: Clientlib configuration for the component (`ui.apps`).
    *   `component_docs_markdown`: Markdown documentation for the component's usage.

### 2.3. AEM Forms Workflow Designer Agent

*   **Responsibility:** Designs and implements AEM Workflows for form submission processing (e.g., approvals, external system integration).
*   **Input:**
    *   `task_id`: Unique identifier.
    *   `form_submission_process`: Business process flow for form submission (e.g., "Form submitted -> Manager approves -> Data written to CRM").
    *   `fdm_context`: Relevant FDM details for data access.
    *   `integration_point_specs`: Details of external systems to integrate with.
*   **Output:**
    *   `workflow_model_xml`: Workflow model XML definition (e.g., `/var/workflow/models/loan-approval.xml`).
    *   `workflow_process_java_code`: Java source for custom workflow process steps (`core`).
    *   `workflow_process_docs`: Documentation for the workflow and its steps.

### 2.4. AEM Forms Test Writer Agent

*   **Responsibility:** Creates automated tests for custom components, FDM integrations, and workflow steps.
*   **Input:**
    *   `task_id`: Unique identifier.
    *   `component_code`: React component code to test.
    *   `fdm_definition`: FDM definition and schema.
    *   `workflow_model`: Workflow model definition.
    *   `business_requirements`: Original requirements for the feature.
*   **Output:**
    *   `jest_tests_js`: Jest/React Testing Library tests for React components (`ui.frontend.react.forms.af`).
    *   `cypress_tests_js`: Cypress tests for end-to-end UI validation of forms (`ui.tests`).
    *   `junit_tests_java`: JUnit tests for Java services and workflow processes (`core`).
    *   `test_report_summary`: Summary of test coverage and results.

### 2.5. AEM Forms DoR Agent

*   **Responsibility:** Orchestrates the generation of Document of Record (DoR) PDFs and their archiving.
*   **Input:**
    *   `task_id`: Unique identifier.
    *   `form_data`: Submitted form data for the DoR.
    *   `xdp_template_path`: Path to the XDP template for DoR generation.
    *   `archival_policy`: Requirements for where and how to archive the DoR (e.g., Customer Managed Storage).
*   **Output:**
    *   `workflow_process_java_code`: Java source for the workflow step invoking Output Service (`core`).
    *   `dor_config_xml`: Configuration files for DoR generation and archiving.
    *   `dor_template_path`: Confirmed path to the XDP template.
    *   `dor_archiving_strategy`: Documentation or configuration for archiving the DoR.

## 3. Example Workflow: Orchestrating "Develop Loan Application Form"

This example illustrates how the Mayor AI would orchestrate agents to deliver a complex AEM Forms task.

**BMAD Task:** "Develop Loan Application Form" (triggered by a PM Agent based on business needs).

**Mayor AI Orchestration Steps:**

1.  **Initiate Task:** Mayor AI creates a new Gas Town task for "Develop Loan Application Form".
2.  **FDM Design:**
    *   Mayor delegates to **AEM Forms FDM Architect Agent**.
    *   *Input:* Loan application business requirements, financial data sources.
    *   *Output:* `loan-application-fdm.xml`, `loan-application.schema.json`, `loan-services.spec`.
    *   Mayor reviews and approves FDM output.
3.  **Custom Component Design & Implementation (e.g., Address Lookup):**
    *   Mayor identifies need for "Address Lookup Component" based on form fields.
    *   Mayor delegates to **AEM Forms React Coder Agent**.
    *   *Input:* Address Lookup component spec, FDM schema (applicant address details).
    *   *Output:* `CustomAddressField.js`, `dialog.json`, `component_docs.md`.
    *   Mayor delegates to **AEM Forms Test Writer Agent**.
    *   *Input:* `CustomAddressField.js`, FDM context, component spec.
    *   *Output:* `CustomAddressField.test.js` (Jest), `AddressLookup.cy.js` (Cypress).
    *   Mayor monitors test results and approves component.
4.  **Adaptive Form Template Creation:**
    *   Mayor delegates to **Adaptive Form Template Designer Agent**.
    *   *Input:* Overall form structure requirements (wizard, panels, header/footer from BMAD Architect).
    *   *Output:* `/conf/app/templates/loan-application-template/.content.xml`.
5.  **Form Implementation:**
    *   Mayor delegates to **Adaptive Form Authoring Agent**.
    *   *Input:* `loan-application-fdm.xml`, `loan-application-template.xml`, component library, all approved custom components.
    *   *Output:* `/content/forms/af/loan-application-form/.content.xml`.
6.  **Workflow Design & Implementation (incl. External API & DoR):**
    *   Mayor delegates to **AEM Forms Workflow Designer Agent**.
    *   *Input:* Form submission process (e.g., "verify credit score, send to underwriter, generate DoR").
    *   *Output:* `loan-approval-workflow.xml`, `CreditCheckProcess.java` (for credit check API), `GenerateDoRProcess.java`.
    *   Mayor delegates to **AEM Forms DoR Agent**.
    *   *Input:* Submitted form data structure, XDP template path, archival policy.
    *   *Output:* DoR generation workflow step, XDP template configuration.
    *   Mayor delegates to **AEM Forms Test Writer Agent**.
    *   *Input:* `CreditCheckProcess.java`, `GenerateDoRProcess.java`, workflow model.
    *   *Output:* `CreditCheckProcessTest.java` (JUnit), `GenerateDoRTest.java` (JUnit).
7.  **Final Review & Deployment:**
    *   Mayor aggregates all artifacts, runs final validation checks.
    *   Mayor triggers Cloud Manager pipeline for deployment.

## 4. Integration with Other Adobe Services

Gas Town orchestration extends to leveraging other Adobe services critical for AEM Forms:

### 4.1. Adobe Sign Integration

*   **Orchestration Goal:** Seamlessly integrate digital signatures into Adaptive Form workflows.
*   **Agent Role:** An **"AEM Forms Sign Agent"** (orchestrated by Gas Town) would handle:
    *   Configuring Adobe Sign services in AEM.
    *   Integrating Adaptive Forms with Adobe Sign (e.g., adding an E-Sign component, pre-filling signer details).
    *   Monitoring signature status within workflows.
*   **Workflow Example:** Post-submission, the "AEM Forms Workflow Designer Agent" creates a workflow step, which the "Sign Agent" then configures to send the submitted form data for digital signing via Adobe Sign.

### 4.2. Automated Forms Conversion Service

*   **Orchestration Goal:** Automate the conversion of static PDF forms or XDPs into Adaptive Forms.
*   **Agent Role:** An **"AEM Forms Conversion Agent"** (orchestrated by Gas Town) would:
    *   Ingest legacy form assets.
    *   Apply conversion rules and configurations.
    *   Validate the generated Adaptive Form structure and fields against business requirements.
*   **Workflow Example:** During a migration phase, the "AEM Forms Conversion Agent" could be tasked with taking a legacy form (input) and producing a draft Adaptive Form (output), which is then refined by other agents.

### 4.3. Document Services (Output, Assembler, DoR)

*   **Orchestration Goal:** Generate dynamic PDFs for various purposes (e.g., Document of Record, personalized communications).
*   **Agent Role:** The **"AEM Forms DoR Agent"** (as detailed above) is a specialized instance that leverages these services.
*   **Workflow Example:** After a form submission is finalized, the "AEM Forms DoR Agent" orchestrates the use of the Output Service to generate a PDF based on submitted data and an XDP template, and then ensures it's archived correctly.

This detailed specification provides a comprehensive roadmap for building or integrating a Gas Town-like orchestration system tailored for the unique complexities and service integrations required in AEM Forms development.
