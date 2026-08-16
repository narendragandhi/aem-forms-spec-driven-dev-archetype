# Continuous Improvement Loop

Generated projects use a bounded improvement loop around the AEM Forms system:

```text
signal -> bead -> regression fixture -> proposed change -> verification
       -> human approval -> canary -> evidence -> next baseline
```

## Signals

Signals may come from a failed unit/browser/accessibility test, AEM or provider
failure, reviewer override, accessibility finding, incident, SOC evidence gap,
or a measurable SLO regression. A signal is not permission to change production.

## Guardrails

- Every improvement has a bead, owner, policy/version reference, and evidence.
- The evaluator and safety controls are not changed by the system being evaluated.
- Real PHI, signer data, credentials, and provider responses are excluded from
  training fixtures and logs.
- Changes to evaluation policy, HITL routing, access, retention, or resilience
  require explicit human approval and rollback criteria.
- Canary and full regression results are retained before rollout.
- The loop stops on repeated failure, missing evidence, policy ambiguity, or a
  security/privacy finding.

## Operating sequence

1. Create an improvement bead from the signal.
2. Reproduce the issue with synthetic data and attach a regression fixture.
3. Propose the smallest change and identify affected controls.
4. Run unit, integration, browser, accessibility, security, and policy tests.
5. Obtain reviewer approval; rejected proposals remain evidence, not discarded.
6. Deploy to a demo/canary environment and compare baseline metrics.
7. Record the decision, results, and new baseline in the evidence register.

Use `bmad/gastown/bead/templates/improvement-task.md` for every proposal.
