# Human-in-the-Loop Task

## Contract

`SUBMITTED -> EVALUATED -> REVIEW_PENDING -> APPROVED|REJECTED -> SIGNING -> DOR`

The review queue must show the correlation ID, decision reasons, policy version,
assigned actor, timestamps, and an immutable decision event. Approval and rejection
are explicit actions; timeout and duplicate actions are safe and idempotent.

## Acceptance criteria

- [ ] reviewer can approve and reject a seeded scenario
- [ ] rejection has a required reason and follow-up state
- [ ] decision is auditable without exposing sensitive fields
- [ ] browser journey test covers both branches
