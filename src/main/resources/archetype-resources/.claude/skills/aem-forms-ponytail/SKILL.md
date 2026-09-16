---
name: aem-forms-ponytail
description: Makes an AEM Forms coding agent check for the Adaptive Forms platform's own declarative mechanisms — the Rule Editor, Form Data Model, Document/Output Services, FormSubmitActionService, DataProvider — before writing custom Java, servlets, or JS for form logic, prefill, submission handling, or PDF generation. Use this whenever implementing, extending, or scaffolding anything in an AEM Forms project (Adaptive Form components, form-submission handlers, prefill integrations, PDF/document generation).
---

# AEM Forms Ponytail

An AEM-Forms-specific adaptation of [`aem-ponytail`](https://github.com/narendragandhi/aem-ponytail)
(itself a port of [ponytail](https://github.com/DietrichGebert/ponytail) by
Dietrich Gebert, MIT-licensed). Same ladder, re-grounded in what the
**Adaptive Forms** platform already does declaratively before a form
project reaches for custom Java or JS.

## Provenance and honesty about validation state

AEM Sites' `aem-ponytail` earned its trap table from two audited codebases
(see its own `EVIDENCE.md`). This Forms-specific version reuses one of
those two audits directly — **Run 1** in that evidence file audited an AEM
Forms archetype (Adobe Sign integration, headless submission,
observability) and confirmed two real extension points were used
idiomatically: `FormSubmitActionService` and `DataProvider`. That's real,
audited evidence, not invented.

The rest of this ladder's Forms-specific rows (Rule Editor, Form Data
Model, Document/Output Services, Correspondence Management) are grounded
in documented AEM Forms platform capabilities, but **have not yet been
through the same two-run audit process** the original ladder was. Treat
rows 3 onward in the trap table below as a reasoned starting point, not a
validated finding, until someone runs `aem-forms-ponytail-review` against
a real Forms codebase and reports back — the same bar the original project
holds itself to.

## The ladder

Before writing any AEM Forms code, stop at the first rung that holds:

```
1. Does this need to exist?           → could a form author solve it via
                                          the Adaptive Forms Rule Editor
                                          (visibility/validation/calculation
                                          rules), a Form Data Model, or a
                                          theme/layout change — no deploy?
2. Already in this repo?              → grep the project's Sling Models,
                                          servlets, and OSGi services before
                                          writing a new one for prefill,
                                          submission, or document generation.
3. Does a real AEM Forms extension
   point cover it?                    → FormSubmitActionService (submission
                                          handling), DataProvider (prefill/
                                          data sourcing) — confirmed real,
                                          idiomatic extension points by
                                          audit (see Provenance above).
4. Does Document Services / Output
   Service / Correspondence Mgmt
   cover it?                          → PDF generation, document assembly,
                                          reusable content fragments — don't
                                          hand-roll PDF rendering or
                                          duplicate content blocks in code.
5. Does AEM Forms Workflow cover it?  → approval/routing/state machines —
                                          use a workflow model, don't
                                          hand-roll a status field + custom
                                          transition logic.
6. Does a Form Data Model (FDM)
   integration cover it instead of
   a custom OSGi service?            → FDM is the declarative mechanism
                                          for external data source/schema
                                          integration — check before writing
                                          a bespoke service for the same job.
7. One HTL expression, one Rule
   Editor rule, or a one-line
   Sling Model getter?                → write that.
8. Only then: the minimum new
   servlet/service/Sling Model that
   works.                             → and only for the part the platform
                                          genuinely doesn't cover.
```

The ladder runs *after* understanding the problem — read the Adaptive Form
model, the rule set already configured on it, and the submission/workflow
path it flows through, before picking a rung.

## AEM-Forms-specific over-build traps

| You're about to... | Check first | Usually means |
| --- | --- | --- |
| Write custom JS for field visibility/validation/calculation | Does the Adaptive Forms Rule Editor already express this declaratively? | Use a Rule Editor rule, not custom JS |
| Write a new OSGi service to fetch/prefill external data | Does a Form Data Model (FDM) integration already cover this data source? | Configure an FDM, don't hand-roll a prefill service |
| Write a custom servlet to handle form submission | Does `FormSubmitActionService` already cover this? — **confirmed real, idiomatic extension point by audit** | Implement `FormSubmitActionService`, don't write a raw servlet |
| Write a custom OSGi service to source prefill/reference data | Does `DataProvider` already cover this? — **confirmed real, idiomatic extension point by audit** | Implement `DataProvider`, don't hand-roll the lookup |
| Write custom code to generate a PDF from form data | Does Output Service / Document Services already cover this? | Use the platform's document generation, don't hand-roll PDF rendering |
| Duplicate a block of legal/boilerplate text across templates | Does Correspondence Management / a document fragment already cover this? | Reference the shared fragment, don't copy-paste content into code |
| Hand-roll an approval/routing state machine with a status field | Does an AEM Forms Workflow model already cover this? | Use a workflow model, don't reinvent state transitions |
| Write a custom e-signature capture flow | Does the existing Adobe Sign integration already cover this? | Reuse the integration, don't build a second signature mechanism |

## Never on the chopping block

Same non-negotiables as AEM Sites' `aem-ponytail`, plus one Forms-specific
addition:

- **XSS-safe HTL output contexts, ACL/permission checks, dispatcher/CDN
  cache safety, accessibility, content/replication safety, HTTP method
  safety** — identical to the base ladder; see
  [`aem-ponytail`](https://github.com/narendragandhi/aem-ponytail) for the
  full reasoning on each.
- **PII/PHI handling in form submissions** — never log full submission
  payloads containing personal data to reduce debugging effort; redact
  before writing to any log, trace, or error report. Laziness that leaks
  personal data through a debug log is the same class of mistake as
  laziness that drops an ACL check.

## Before scaffolding new Forms code, run

```bash
# does a Rule Editor rule already exist for this field's behavior?
grep -ril "guideBridge\|rule" ui.apps/src/main/content/jcr_root -l 2>/dev/null

# does an existing FormSubmitActionService or DataProvider implementation cover this?
grep -rl "implements FormSubmitActionService\|implements DataProvider" core/src/main/java 2>/dev/null

# is Document/Output Services already wired for PDF generation?
grep -ril "OutputService\|DocAssuranceService" core/src/main/java 2>/dev/null
```
