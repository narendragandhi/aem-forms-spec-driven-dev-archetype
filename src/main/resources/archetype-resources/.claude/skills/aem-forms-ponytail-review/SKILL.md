---
name: aem-forms-ponytail-review
description: Audits existing AEM Forms code (a diff, a component, a submission handler) against the aem-forms-ponytail ladder to find places where custom Java/JS was written for something the Adaptive Forms Rule Editor, Form Data Model, Document Services, FormSubmitActionService, or DataProvider already provides. Use after implementing an AEM Forms feature, or when asked to review AEM Forms code for over-engineering.
---

# AEM Forms Ponytail Review

Runs [`aem-forms-ponytail`](../aem-forms-ponytail/SKILL.md)'s ladder
backwards: instead of guiding new code, it inspects code that already
exists and flags what a lazier senior AEM Forms architect would have
deleted.

## When to use

- Right after implementing an Adaptive Form component, submission handler,
  or prefill integration — before opening the PR.
- When asked to review an AEM Forms diff for bloat.
- When a form's supporting Java/JS feels bigger than the form logic it
  implements.

## What to look for

For each new/changed file, ask which rung of the ladder it actually needed:

1. **Unnecessary existence** — custom JS for field logic an author could
   express with a Rule Editor rule instead.
2. **Duplicated capability** — a new OSGi service re-fetching external data
   that a Form Data Model integration already sources declaratively.
3. **Reinvented extension point** — a raw servlet handling submission where
   `FormSubmitActionService` already covers it, or a hand-rolled data
   lookup where `DataProvider` already covers it. (These two are
   **audit-confirmed real extension points** — see
   `aem-forms-ponytail`'s Provenance section.)
4. **Reinvented document generation** — hand-rolled PDF rendering where
   Output Service / Document Services already does this.
5. **Duplicated content** — legal/boilerplate text copy-pasted into
   templates where a Correspondence Management fragment already exists.
6. **Reinvented workflow** — a hand-rolled status field + transition logic
   where an AEM Forms Workflow model already covers approval/routing.
7. **Method safety collapsed for convenience** — same as the base ladder:
   never let a GET carry a side effect (a submission, a signature request,
   a billed API call).

## What NOT to flag

Never suggest cutting: XSS-safe HTL context attributes, ACL/permission
checks, dispatcher cache-safety logic, accessibility attributes/behavior,
replication/migration safety code, or PII/PHI redaction in logs. Findings
that would remove these are false positives — drop them even if they
technically shrink the diff.

## Output format

Report each finding as: file/location → what's there → what rung of the
ladder it should have stopped at → the specific platform capability
(Rule Editor rule, FDM, `FormSubmitActionService`, `DataProvider`, Output
Service, Correspondence Management fragment, or Workflow model) that
already covers it. Skip files where the code already sits at the correct
rung.

## Honesty note

This review skill's Forms-specific trap table (rows 3+ in
`aem-forms-ponytail`) has not yet been through the two-run audit process
that validated the original AEM Sites ladder. If you run this against a
real AEM Forms codebase, report back whether the findings held up — that's
exactly how the original ladder's evidence file was built.
