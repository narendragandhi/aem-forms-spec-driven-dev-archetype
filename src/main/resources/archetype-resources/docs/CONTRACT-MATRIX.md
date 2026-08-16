# Foundation Contract Matrix

The archetype is the reusable source for the AEM Forms showcase foundation.
Generated projects may specialize the UI and provider implementation, but they
must preserve the following contracts:

| Contract | Required evidence |
|---|---|
| foundation flags and provider mode | `bmad/foundation-config.yaml` |
| evaluation and HITL | deterministic fixtures, reviewer decision, policy version |
| observability | correlation ID, health, metrics, redacted event logs |
| resilience | timeout/retry/circuit-breaker policy and tests |
| Adobe Sign/DoR | provider configuration plus live environment evidence |
| SOC readiness | control mapping, evidence register, owner, review period |
| continuous improvement | signal bead, regression fixture, approval, canary evidence |

The showcase repository contains the reference implementation and its own
contract matrix. Demo behavior must always be labeled separately from live
provider behavior.
