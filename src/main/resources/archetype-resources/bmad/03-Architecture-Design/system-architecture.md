# System Architecture

This document provides a high-level overview of the system architecture for the AEM as a Cloud Service project.

## AEM as a Cloud Service

- **Author Tier**: Used by content authors to create and manage content.
- **Publish Tier**: Delivers the content to the website visitors.
- **Preview Tier**: Used to preview content before it is published.

## CDN

- **AEM Managed CDN**: The built-in CDN will be used to cache content at the edge and improve performance.

## Cloud Manager CI/CD Pipeline

The Cloud Manager CI/CD pipeline is the required and only way to build and deploy code to AEM as a Cloud Service. The pipeline automates the build, test, and deployment process, ensuring that all code is of high quality and that the deployment process is reliable.

- **Source Code Analysis**: Scans the codebase for security vulnerabilities, code quality issues, and other potential problems.
- **Build & Unit Testing**: Compiles the code and runs unit tests.
- **Integration & UI Testing**: Deploys the code to a staging environment and runs automated integration and UI tests.
- **Deployment**: Deploys the code to the production environment.

## Data Flow Diagram

```
[Developer] -> [Git Repository] -> [Cloud Manager Pipeline] -> [AEM Author/Publish]
      ^                                                              |
      |______________________________________________________________| (feedback loop)

[Website Visitor] -> [AEM Managed CDN] -> [AEM Publish Tier]
                                               ^
                                               |
                                          [CRM API]
                                               ^
                                               |
                                       [Translation API]
```

## Integrations

- **CRM**: AEM will be integrated with the company's CRM (e.g., Salesforce) to capture leads from the "Contact Us" form. This will be a server-side integration using AEM's workflow engine.
- **Analytics**: Adobe Analytics will be used to track user behavior on the website. The integration will be done using the Adobe Launch.
- **Translation**: AEM will be integrated with a third-party translation service to manage multilingual content.

