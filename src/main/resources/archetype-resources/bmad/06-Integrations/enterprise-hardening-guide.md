# Enterprise AEM Forms: Hardening & Modernization Guide

This guide outlines the "Elite" patterns for bridging modern Headless AEM Forms with industrial-strength enterprise document requirements. It incorporates strategies for mass migration, complex correspondence, and data orchestration.

## 1. Legacy Modernization: Automated Forms Conversion (AFCS)

To handle enterprises with thousands of legacy PDF/XFA forms, the blueprint utilizes the **Automated Forms Conversion Service (Sensei)**.

### The Conversion Workflow
1.  **Ingestion**: Legacy PDF/XFA files are uploaded to AEM Assets.
2.  **AI Analysis**: Adobe Sensei identifies form fields, labels, and logical groupings.
3.  **Adaptive Form Generation**: A standard Adaptive Form (AF) is generated with core components.
4.  **Headless Refinement (GasTown)**: 
    *   The `aem-component-coder` agent reviews the generated AF.
    *   It applies the **Omnichannel Design Tokens** (`--bmad-`).
    *   It ensures the `ComponentExporter` is correctly configured for Headless delivery.

### Best Practice: The "Migration Agent"
In the GasTown ecosystem, a specialized **Migration Agent** is used to orchestrate the AFCS API, ensuring that converted forms meet the project's TDD and accessibility standards from Day 1.

---

## 2. Document Automation: Interactive Communications (IC)

While a Document of Record (DoR) is a static snapshot, **Interactive Communications** (formerly Correspondence Management) supports complex, personalized document generation.

### DoR vs. Interactive Communications
| Feature | Document of Record (DoR) | Interactive Communications (IC) |
| :--- | :--- | :--- |
| **Purpose** | Submission Receipt / Audit Trail | Personalized Statements, Policies, Letters |
| **Data Sources** | Current Form Submission | Multiple Backend Systems (CRM, ERP, SQL) |
| **Agent-in-the-Loop** | Fully Automated | Supports Manual "Agent Review" before send |
| **Output** | Single PDF | Multi-channel (Web, PDF, Print) |

### Implementation Pattern
-   **XDP Source**: Use Adobe Forms Designer (XDP) for pixel-perfect, regulated PDF layouts.
*   **Data Binding**: Use **Form Data Model (FDM)** to bind the XDP template to enterprise data.
*   **Headless Trigger**: The Headless React app triggers a workflow that invokes the **Output Service** to generate the personalized IC document.

---

## 3. Data Orchestration: FDM as the Headless Middle Layer

In an enterprise environment, the **Form Data Model (FDM)** should act as the "Single Source of Truth" between the Headless React app and the Backend.

### The BFF-to-FDM Pattern
1.  **React Layer**: Calls the Headless BFF (`/bin/bmad/headless-form-service`).
2.  **BFF Layer**: Adapts the request to an **FDM Service**.
3.  **FDM Layer**: Orchestrates calls to Salesforce, SQL, or REST APIs.
4.  **Benefits**:
    *   **No-Code Configuration**: AEM Authors can update FDM mappings (e.g., changing a field source from `test_db` to `prod_db`) without a code deployment.
    *   **Unified Validation**: Server-side validation rules defined in FDM are enforced for both Headless and Traditional forms.

---

## 4. Security: Document Rights Management (DRM)

For high-compliance industries (Finance, Gov), the generated PDF must be protected after it leaves the AEM environment.

### Adobe Document Security Integration
Implement a custom **Workflow Process Step** that applies security policies:
-   **Persistent Protection**: Encryption that stays with the document (even when emailed).
*   **Dynamic Policies**: 
    *   `Validity Period`: Document becomes unreadable after 30 days.
    *   `Permission Control`: Disable printing, copying, or modification.
    *   `Revocation`: Remotely revoke access to a document even after it has been downloaded.

### Workflow Example
`Submit` -> `Generate DoR/IC` -> `Apply DRM Policy (Financial-Restricted)` -> `Secure Archive/Email`

---

## 5. Deployment & Governance (AEMaaCS)

Enterprise hardening requires strict environment governance via **Cloud Manager**.

### Hardening Checklist
- [ ] **Dispatcher Caching**: Ensure `.model.json` is cached correctly but cleared on form updates.
- [ ] **SSL/TLS**: Mandatory TLS 1.3 for all form endpoints.
- [ ] **Rate Limiting**: Apply at the Dispatcher/WAF level to prevent "Submit Spam" attacks.
- [ ] **Audit Logging**: Use `AEM Audit Log` to track every form view, submission, and document generation event for compliance.
