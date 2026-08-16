# Observability Task

## Contract

Every request receives or generates a safe `X-Correlation-ID`. Logs use structured
event names, metrics count requests/submissions/failures/status polls, and health
checks remain read-only. Redact payloads, tokens, signatures, and personal data.

## Acceptance criteria

- [ ] correlation ID is returned to the caller
- [ ] health and metrics endpoints are documented
- [ ] failure logs include operation and error type, not payload contents
- [ ] dashboard or probe thresholds are defined
