# Evaluation Task

## Contract

- Input: normalized form data and use-case identifier.
- Output: deterministic score, band, recommendation, policy reasons, and version.
- No PII or raw form payloads in logs.
- Every decision is linked to a correlation ID and bead/task ID.

## Acceptance criteria

- [ ] low-risk and high-risk fixtures exist
- [ ] policy decision is explainable to a reviewer
- [ ] automated tests cover boundary values
- [ ] human override is recorded separately from the machine recommendation
