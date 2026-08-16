# Demo Runbook

1. Generate the project with the desired feature flags.
2. Build with `mvn clean install` and deploy to an AEM Forms Author instance.
3. Open the seeded form journey and capture the correlation ID from the response.
4. Submit a low-risk scenario and a review-required scenario.
5. Approve and reject the review-required scenario, then inspect the audit trail.
6. Verify `/bin/bmad/observability/health` and `/bin/bmad/observability/metrics`.

The demo must label seeded/mock behavior separately from live AEM workflow,
signature, and Document of Record integrations.
