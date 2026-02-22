# Cloud Manager Best Practices

This document outlines best practices for using Cloud Manager to ensure a smooth and efficient development and deployment process for AEM as a Cloud Service.

## Pipeline Configuration

- **Branching**: Use a Git branching strategy that aligns with the Cloud Manager pipelines. Typically, this is `main` for production, `develop` for staging, and feature branches for development.
- **Quality Gates**: Do not bypass the default quality gates. These are in place to ensure the quality and stability of your application.
- **Notifications**: Configure notifications to be sent to your team's communication channel (e.g., Slack or email) to be alerted of pipeline status changes.

## Code Quality

- **Run the Optimizer**: Always run the AEM Optimizer tool on your code before committing to ensure that it meets the Cloud Manager code quality standards.
- **Address Issues Early**: Address code quality issues as soon as they are identified by the pipeline. Do not let them accumulate.
- **Custom Rules**: If you have custom code quality rules, ensure they are compatible with the Cloud Manager pipeline.

## Deployment

- **Non-production Pipelines**: Regularly run non-production pipelines to test your code in a cloud environment. This helps to catch issues early that may not be apparent in the local SDK.
- **Production Pipelines**: Schedule production deployments during off-peak hours to minimize the impact on users.
- **Zero Downtime**: AEM as a Cloud Service provides zero-downtime deployments, so there is no need to plan for a maintenance window.

## Testing

- **Automated Tests**: Leverage the automated testing capabilities of the Cloud Manager pipeline to ensure that your application is working as expected.
- **Custom Tests**: You can add custom functional tests to your pipeline to test specific business requirements.
- **Performance Tests**: The pipeline includes performance testing, but you should also perform your own performance testing for key user journeys.
