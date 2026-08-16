# Resilience Task

## Contract

External calls must have bounded timeouts, bounded retries with backoff, circuit
breaker state, bulkhead limits, idempotency keys, and a user-safe fallback. The
default archetype keeps this capability opt-in until an integration supplies its
failure policy and operational thresholds.

## Acceptance criteria

- [ ] timeout and retry budgets are explicit
- [ ] open/half-open/closed transitions are tested
- [ ] duplicate submissions are idempotent
- [ ] degraded mode is visible to operators and users
