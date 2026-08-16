# Residuality Design for AEM Forms

Residuality design complements risk analysis by asking what happens when an
unexpected stressor hits the system, what survives, and which failure state the
system naturally approaches. Use this with the resilience and chaos beads; it is
not a prediction exercise or a replacement for threat modeling.

## The design vocabulary

- **Stressor**: an unexpected technical, operational, business, or regulatory
  change.
- **Residue**: a capability that survives stress and lets the journey continue,
  recover, or be safely resumed.
- **Attractor**: the state the system naturally falls toward under stress.
- **Incidence matrix**: a map of stressors against components and journeys that
  exposes coupling and cascading failure.

## Method

1. Choose a journey and its business invariant, such as “never lose an accepted
   application” or “never sign without an accountable decision.”
2. Select known and random stressors from `RESIDUALITY-STRESSORS.csv`.
3. Mark the affected components in `RESIDUALITY-MATRIX.csv`.
4. Name the attractor and define the residues that must survive.
5. Run a synthetic chaos experiment with no real PHI, credentials, or signatures.
6. Record the result as a bead and feed failures into continuous improvement.

## AEM Forms invariants

- Accepted submissions remain traceable by correlation ID and idempotency key.
- A provider outage does not silently lose data or create duplicate agreements.
- A signature cannot bypass a required human decision.
- A signed agreement and its Document of Record have an auditable relationship.
- Degraded mode tells the applicant and operator what happened.
- Observability failure does not expose form payloads or prevent safe recovery.

The goal is graceful degradation and independent failure, not the impossible
promise that no component will fail.
