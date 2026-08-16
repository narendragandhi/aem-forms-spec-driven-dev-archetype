# AEM Forms Project Playbook

Use this playbook for every new AEM Forms project generated from the archetype.
It defines the minimum delivery story for healthcare, financial services,
government, insurance, or internal operations.

## 1. Start with a project profile

Record these decisions in the first planning bead: business journey, actors,
data class, form strategy, data integration, evaluation policy, HITL gate,
signature/DoR provider, deployment topology, and public-versus-private evidence
boundary.

## 2. Generate the foundation

```bash
mvn -B archetype:generate \
  -DarchetypeGroupId=com.example.aem.archetype \
  -DarchetypeArtifactId=aem-forms-bmad-archetype \
  -DarchetypeVersion=1.0.0-SNAPSHOT \
  -DgroupId=com.example.forms \
  -DartifactId=my-forms-project \
  -DappName=MyFormsApp \
  -DinteractiveMode=false
```

Review `bmad/foundation-config.yaml`. Keep demo defaults until the real
environment, identities, providers, and data protections are approved.

## 3. Deliver in small, traceable beads

Create linked beads for planning, implementation, integration, testing, review,
documentation, compliance, and operations. A capability is complete only when
its code/config, test, evidence, and documentation agree.

## 4. Implement the end-to-end journey

```text
discovery -> authored form -> rules/data -> submit -> evaluate
          -> human review when required -> sign -> DoR -> observe -> evidence
```

Clearly label seeded behavior. A live claim requires environment, timestamp,
identity, request/response evidence, and a corresponding bead.

## 5. Test four layers

1. Unit: rules, services, state transitions, and failure paths.
2. Integration: AEM model JSON, FDM/provider contracts, workflow, signature, DoR.
3. Browser: keyboard journey, validation, HITL decisions, and responsive UI.
4. Operational: health, metrics, correlation IDs, retries, circuit breaker,
   alerting, backup/restore, and incident exercise.

## 6. Present and hand off

Use `PRESENTATION-RUNBOOK.md` only after the seeded happy path and a visible
failure or human-review path work. Leave behind the project profile, architecture,
acceptance matrix, prerequisites, runbook, tests, evidence register, security
review, and open beads so another team can regenerate and tailor the project.
