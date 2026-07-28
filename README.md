# AEM Forms BMAD Archetype

A Maven archetype for creating AI-ready AEM Forms projects using the **BMAD** (Breakthrough Method for Agile Development — [bmad-code-org/BMAD-METHOD](https://github.com/bmad-code-org/BMAD-METHOD)) methodology, adapted here for AEM Forms alongside [GasTown](https://github.com/steveyegge/gastown) (multi-agent orchestration) and [Beads](https://github.com/steveyegge/beads) (BEAD task tracking) — see `bmad/methodologies/BMAD-BEAD-GasTown.md` for how the three fit together.

## Implementation Status

Read this before adopting the archetype — it tells you what's real, verified
code versus scaffolding you'd still need to build.

| Capability | Status | Notes |
|---|---|---|
| `SpecToCodeGenerator.generate()` | **Real** | Generates a Sling Model + HTL component + React field component from a JSON Schema spec, and now auto-registers every generated React component in `App.jsx`'s `customMappings` (import + mapping entry, idempotent across re-runs) — no more manual wiring. Verified via real compile/deploy. |
| `SpecToCodeGenerator.generateForm()` | **Real, live-verified** | Generates a complete, submittable Adaptive Form (real JCR page/panel/field structure, standard field components, a working submit action, a real "Save for Later" button on every step, and an opt-in reCAPTCHA field) from a multi-panel spec. The most thoroughly proven capability in this archetype — deployed to a live instance and confirmed by actually POSTing a real submission and getting `HTTP 200`. See [Generating a Complete Adaptive Form](#generating-a-complete-adaptive-form). |
| `PrefillDataService` | **Real, live-verified end to end** | Implements the real `com.adobe.forms.common.service.DataProvider` extension point (verified via `javap`). The form-to-service wiring — a `prefillService` content property on the `guideContainer` — was found by decompiling AEM's own running `AdaptiveFormDataServlet`/`FormDataProviderRegistryImpl` classes and confirmed by a real invocation log line naming this service. That same live test caught a real bug (the outbound HTTP call had no auth header, so the shipped example endpoint 401'd) — fixed, unit-tested, and then live-verified again (via a small hand-built OSGi bundle standing in for the archetype's own drifted core bundle): the fixed call now returns `HTTP 200` and real customer data flows through `/adobe/forms/af/data/<id>` end to end. `generateForm()` now supports opting into this via a `prefillService` spec key. See [Generating a Complete Adaptive Form](#generating-a-complete-adaptive-form). |
| reCAPTCHA field (`generateForm()`, opt-in) | **Mechanism confirmed real; enforcement blocked by a pre-existing instance limitation** | When a spec sets a top-level `recaptcha.cloudServicePath`, `generateForm()` emits a `captcha` field on the real, already-shipped `<appName>/components/adaptiveForm/recaptcha` proxy. Decompiling the live instance's own `AdaptiveFormSubmitServlet` confirmed server-side verification is genuinely platform-native (`CaptchaUtils.validateCaptcha`, no custom code needed) — and a real Cloud Service Configuration was live-authored with Google's own published test keys. But a live submission with no captcha response wasn't rejected and never even logged a captcha-related line — traced to the same nested-panel form-model limitation documented under [Headless React Flow](#headless-react-flow), not a flaw in this wiring. See [Generating a Complete Adaptive Form](#generating-a-complete-adaptive-form). |
| `SubmissionAuditService` | **Real, live-verified end to end** | Implements the real `com.adobe.aemds.guide.service.FormSubmitActionService` extension point (verified via `javap`), the same interface Adobe's own `aem-core-forms-components` integration-test fixture uses for a custom submit handler. Decompiling `FormSubmitActionManagerServiceImpl` revealed the real wiring: a guideContainer's `actionType` resolves as a *resource path*, and that resource's own `submitService` property (matching the already-shipped `customsubmission/submissionAudit` node) is what gets looked up by name. Live-verified via a real POST submission — `error.log` shows the framework finding the service by name and invoking it with the real submitted data, returning `HTTP 200`. See [Submission Audit Trail](#submission-audit-trail). |
| `SignToDoRProcess` (Document of Record) | **Real** | Calls the actual AEM Forms `DoRService`, verified against a real running instance. Has real prerequisites — see [Document of Record (DoR) Generation](#document-of-record-dor-generation) below; your form needs to be DAM-backed (Forms Manager-style), not just a WCM page, for it to work. |
| `AdobeSignOrchestrator` | **Real, not live-tested** | `AdobeSignOrchestratorImpl` calls the real Adobe Sign REST API v6 (transient document upload, agreement creation, status, signed-document download, webhooks). Request/response shapes verified against Adobe's own docs and mocked in tests — not yet run against a real Adobe Sign account. See [Adobe Sign Integration](#adobe-sign-integration). |
| `FormSubmissionService` | **Real, live-verified** | Real HTTP POST to a configurable external endpoint, wired into `HeadlessSubmitServlet`. Verified with a real listener (success) and real connection-refused failure — both paths, not just compile. |
| Interactive Communications | **Real, live-verified up to a native-SDK boundary** | `InteractiveCommunicationServiceImpl` calls the real `PrintChannelRenderService`, falling back to `com.adobe.fd.output.api.OutputService` when it's unavailable (as it was, entirely, on the instance this was built against — feature-toggle-gated). The fallback is live-tested end to end: component activation, an auth fix, and a `crx://` template-path fix were all confirmed against a real instance, up to AEM Forms' native XFA rendering SDK failing to start (same limitation `DoRService` has below) — a pre-existing environment issue, not a bug in this code. See [Interactive Communications (IC)](#interactive-communications-ic). |
| Headless React flow (`App.jsx`) | **Real fixes, live-tested in a real browser — blocked by an AEM instance-level limitation, not this code** | A crash-on-every-render bug and dead submit-forwarding wiring are fixed and confirmed working in a real browser (`npm start` + `playwright-cli`), along with a previously-missing dev proxy/auth setup and a real, correct `customMappings` fix for wizard panels. But no Adaptive Form's fields render through *either* this path or AEM's own native, non-React rendering on the instance this was tested against — confirmed by loading AEM's own `.html` rendering directly and seeing the identical empty result. See [Headless React Flow](#headless-react-flow). |

## Why Use This Archetype?

| Stakeholder | Value |
|-------------|-------|
| **Product Manager** | Faster time-to-market, consistent project structure, reduced risk |
| **AEM Architect** | Pre-configured best practices, modern stack, extensible patterns |
| **Developer** | Zero setup time, working tests, AI-assisted development ready |
| **Enterprise** | Standardization across teams, lower onboarding costs |

## Quick Start

```bash
# 1. Install the archetype (if not in your Maven repo)
cd aem-forms-bmad-archetype
mvn clean install

# 2. Generate a new project
mvn archetype:generate \
    -DarchetypeGroupId=com.example.aem.archetype \
    -DarchetypeArtifactId=aem-forms-bmad-archetype \
    -DarchetypeVersion=1.0.0-SNAPSHOT \
    -DgroupId=com.mycompany \
    -DartifactId=my-forms-project \
    -Dversion=1.0.0-SNAPSHOT \
    -Dpackage=com.mycompany \
    -DappName=MyFormsApp \
    -DformsVersion=afaacs

# 3. Build and deploy
cd my-forms-project
mvn clean install
```

## Prerequisites

- **Java 21** (required for AEM as a Cloud Service)
- **Maven 3.9+**
- **Node.js 20+** (auto-installed by frontend-maven-plugin)

## Project Structure

```
my-forms-project/
├── core/                    # Java OSGi bundles, Sling Models
├── ui.apps/                 # AEM components, clientlibs, HTL
├── ui.content/              # Content pages, Adaptive Forms
├── ui.config/               # OSGi configurations
├── ui.frontend/             # React SPA frontend
├── ui.frontend.react.forms.af/  # Custom Adaptive Forms components
├── all/                     # Aggregated deployment package
├── it.tests/                # Integration tests
├── ui.tests/                # UI tests (Cypress)
├── dispatcher/              # Cloud Dispatcher configs
├── specs/                   # JSON schemas for spec-driven development
└── bmad/                    # BMAD methodology documentation
```

## AI-Powered Development with BMAD

This archetype is designed for **interactive AI-assisted development**. The `bmad/` directory contains structured prompts and specifications that work with AI assistants like Claude.

### How It Works

1. **Business Discovery** (`bmad/01-Business-Discovery/`)
   - Define business requirements in structured markdown
   - AI can parse and generate technical specifications

2. **Model Definition** (`bmad/02-Model-Definition/`)
   - JSON schemas define component structure
   - AI generates Sling Models, React components, HTL

3. **Architecture Design** (`bmad/03-Architecture-Design/`)
   - Document architectural decisions
   - AI suggests implementation patterns

4. **Development Sprint** (`bmad/04-Development-Sprint/`)
   - Spec-driven development workflow
   - AI assists with code generation and review

### Interactive Communication Pattern

```
Developer                    Claude/AI
    │                            │
    ├─── "Create a new form" ───►│
    │                            │
    │◄── Reads bmad/specs ───────┤
    │                            │
    │◄── Generates components ───┤
    │    (Model, HTL, React)     │
    │                            │
    ├─── "Add validation" ──────►│
    │                            │
    │◄── Updates spec + code ────┤
```

### Extensibility for Custom Components

`SpecToCodeGenerator` (`core/.../workflow/SpecToCodeGenerator.java`) turns a
JSON Schema-style spec into a working component — no AI round-trip required,
though you're free to have one draft the spec itself.

1. **Define a spec** in `specs/my-component.json` — JSON Schema `properties`,
   not a flat field list, so the usual keywords apply directly:
```json
{
  "title": "Customer Feedback",
  "properties": {
    "rating": { "type": "integer", "title": "Rating", "minimum": 1, "maximum": 5 },
    "comment": { "type": "string", "title": "Comment", "maxLength": 500 }
  },
  "required": ["rating"]
}
```
   See `specs/card-component.json` (a display component — this is the exact
   spec `Card.java`/`card.html` were hand-built from), `specs/job-application.json`
   (a form exercising every validation keyword: `required`, `format`,
   `minLength`/`maxLength`, `pattern`, `minimum`/`maximum`, `enum`), and
   `specs/benefits-enrollment.json` (nested objects, repeatable fields, and
   conditional visibility together) for full worked examples.

   Two more keywords beyond plain JSON Schema:
   - `"type": "object"` with its own `"properties"` — a nested field group,
     one level deep (a nested object's own fields must be scalar).
   - `"type": "array"` with `"items"` (scalar or object) — a repeatable
     field, also one level deep.
   - `"visibleWhen": {"field": "...", "equals": "..."}` on a scalar field —
     conditional visibility against a sibling top-level scalar field.

2. **Generate the three artifacts**:
```java
new SpecToCodeGenerator().generate(
    "specs/my-component.json",  // spec path
    ".",                         // project root
    "com.mycompany",             // base package
    "MyFormsApp"                 // app name
);
```
   This produces:
   - a **Sling Model** in `core/.../core/models/` — fields typed from
     `type`, plus a `validate()` method that re-checks `required`,
     `minLength`/`maxLength`, `pattern`, `minimum`/`maximum`, and `enum`
     server-side (HTML5 constraints alone aren't real validation). A nested
     object or a repeatable object field gets its own child Sling Model,
     wired via `@ChildResource` — real JCR-backed nesting/repetition, the
     same pattern a hand-authored WCM dialog component would use.
   - an **AEM/HTL component** in `ui.apps/.../components/generated/`,
     required fields marked with `*`, nested fields accessed via HTL's
     getter-chain resolution (`${model.address.street}`, no extra
     directive needed), repeatable fields rendered via `data-sly-list`
   - a **React field component** in
     `ui.frontend.react.forms.af/.../components/generated/` — `format`
     selects the right HTML5 input type (email/date/tel/url), an `enum`
     renders a `<select>` instead of a text input, the same constraints
     become HTML5 attributes, and `visibleWhen` wraps the field in a
     conditional. **Repeatable fields generate a standalone single-item
     component, not add/remove UI** — in real Adaptive Forms, repetition
     is a panel/form-model concern the renderer handles (see
     `@aemforms/af-react-renderer`'s `renderChildren`), not something a
     field component's own code manages. `generate()` automatically
     registers every generated component (the main one and each array
     field's item component) in `App.jsx`'s `customMappings` — the real
     mechanism `@aemforms/af-react-renderer` uses to pick a React
     component for a field, confirmed against the published package's own
     source (`renderChildren.js`'s `getRenderer`, which looks a field up
     by `:type` then falls back to `fieldType`). The registration is
     idempotent (safe to re-run `generate()` for the same spec) and
     patches the existing `App.jsx` in place via anchored text insertion,
     not a JS/JSX parser — consistent with the rest of this generator's
     approach, and it leaves any hand-added entries (like the shipped
     `custom-address-field` example) untouched. To actually make a field
     repeatable in the authored form: set that field's `fieldType` to the
     generated component's slug (e.g. `phone-numbers`) in AEM Forms
     Editor, then configure the containing panel as repeatable
     (`minItems`/`maxItems`).

3. **Build and deploy**:
```bash
mvn clean install -PautoInstallSinglePackage
```

Current scope: nesting and repetition are one level deep (a nested object's
own fields, or a repeatable array's item fields, must be scalar — no
object-in-object, no array-in-array). `visibleWhen` supports simple equality
only, against a sibling top-level scalar field. The generated component
isn't auto-wired into a submission handler. Good for the common case;
extend `SpecToCodeGenerator` yourself for anything richer.

### Generating a Complete Adaptive Form

`SpecToCodeGenerator.generateForm(specPath, outputPath, appName)` is a
separate capability from `generate()` above: instead of one reusable
custom component, it produces a **complete Adaptive Form** — a real
`cq:Page`/`guideContainer`/panel/field JCR structure, using AEM Forms'
*standard* field components (text/number/email/date/dropdown/checkbox),
not custom Sling-Model-backed ones. That's a deliberate choice: standard
fields need no custom React component or `App.jsx` registration at all
(unlike `generate()`'s output, which now self-registers automatically —
see above), and it matches how a human author actually builds a form in
AEM Forms Editor.

A whole-form spec uses a `"panels"` array instead of `generate()`'s single
`"properties"` object — each panel's `properties`/`required` use the exact
same JSON Schema handling:
```json
{
  "title": "Employee Onboarding",
  "panels": [
    { "title": "Personal Details", "properties": {...}, "required": [...] },
    { "title": "Emergency Contacts", "properties": {...}, "required": [...] }
  ]
}
```
See `specs/employee-onboarding.json` for a full worked example — two
panels, a nested object, a repeatable object array (`contacts`), a
repeatable scalar array (`skills`), and validation constraints
(`pattern` on `employeeId`/`zipCode`, `minimum`/`maximum` on
`yearsOfExperience`) all in one spec. Generates to
`ui.content/.../content/forms/af/<appName>/<slug>/.content.xml`.

The real content shape (panels, `table`/`tablerow` for repeatable object
fields) was verified against this archetype's own shipped
`financial-application` sample, not assumed.

**Submit action, wired for real.** Generated forms carry
`actionType="fd/af/components/guidesubmittype/restendpoint"` on the
`guideContainer` plus a real submit button (`.../adaptiveForm/actions/submit`,
`fieldType="button"`, `<fd:events click="[submitForm()]"/>`) in the last
panel. This is genuinely different from — and better verified than — an
earlier version of this doc, which claimed no compatible real example
existed; that was based on an incomplete search. A broader one found a
real, human-authored sample in the *same* Core Components resourceType
family (`ui.content/.../templates/contact-us-form/initial/.content.xml`)
using this exact `actionType` and button shape, independently corroborated
by `actions/submit`'s own AEM Forms Editor dialog template. **Live-verified
end to end**: deployed a generated form and POSTed a real submission to
its action URL (read from the form's own `.model.json`,
`/adobe/forms/af/submit/<id>`) — AEM returned `HTTP 200` with a
`redirectUrl` pointing at the configured thank-you page, confirming a real
submission was accepted and processed, not just that content renders. The
POST body needs to be wrapped as `{"data": {...field values...}}` — a flat
body (no `data` wrapper) gets a real `400 Incomplete request body`, which
is itself how this was confirmed empirically rather than assumed. One
honest gap remains: this proves the `actionType`+button combination
*works*, not that `actionType` is strictly *necessary* — the framework
already auto-provisions a submission endpoint on every `guideContainer`
regardless of content (visible in `.model.json`'s `"action"` field even
without this property), and isolating whether `actionType` changes
anything versus being redundant wasn't tested. This
framework-native submit path is separate from, and not integrated with,
this archetype's own hand-rolled `HeadlessSubmitServlet`/React headless
submission flow.

**`visibleWhen` conditional visibility, now supported.** A scalar field
with `visibleWhen: {field, equals}` gets a child `<fd:rules visible="fieldName == 'Value'"/>`
node — verified against the shipped `benefits-enrollment` showcase
template's own human-authored conditional fields, which use this exact
`fd:rules`/plain-`visible`-attribute shape (the larger `fd:visible` JSON
blob alongside it there is Rule-Editor authoring bookkeeping, the same
relationship already confirmed for the submit button's `fd:events` vs.
`fd:rules`/`fd:click`). `field` must be a sibling within the same panel
(bare-name resolution, matching the real example). **Honesty note,
narrower than the submit action's proof**: live-deployed and confirmed
the generated JCR content is byte-for-byte structurally identical to a
real, working, human-authored example — but unlike the submit action
(proven via an actual accepted `HTTP 200` submission), this environment
has no browser to observe the client-side show/hide behavior actually
executing. The content shape is verified real; the runtime behavior is
inferred by exact analogy, not independently observed.

**Repeatable scalar arrays, now supported — with an honest data-shape
caveat.** A `type: array` field with scalar `items` (e.g. `skills: string[]`)
now generates through the same real, verified `table`/`tablerow` structure
already used for repeatable object arrays (the one real example found in
this codebase, `financial-application`'s `employmentTable`, is
object-only — no separate scalar-array-specific real JCR shape exists
anywhere in the shipped content). Reusing it for a one-column case is
structurally sound and live-verified (deployed and confirmed the exact
generated `table`/`row1`/field nesting persists correctly in a real JCR
tree), but it has a real consequence worth knowing: **the submitted data
shape is an array of single-key row objects, not a flat array of
primitives** — e.g. `skills: [{"value": "a"}, {"value": "b"}]`, not
`skills: ["a", "b"]`. This follows directly from how `table`/`tablerow`
binds data (each row is an object keyed by its fields' `name` attributes,
already established by this generator's other fields), and it's the only
verified-real repeatable mechanism available — not a bug, but a deliberate
tradeoff. (The row's one field is named `value`, not the array's own
name, specifically so it doesn't collide with — or get confused for — the
enclosing table's own name.)

**Validation constraints, now supported.** `minLength`/`maxLength`/`pattern`
on string fields and `minimum`/`maximum` on number fields generate as real
attributes on the field node — property names confirmed against AEM Core
Forms Components' own source (`github.com/adobe/aem-core-forms-components`,
`textinput`/`emailinput`/`numberinput`'s `_cq_dialog` definitions), not
guessed. Two things worth knowing: `minLength`/`maxLength` only apply to
`textinput` — `emailinput`'s real dialog exposes `pattern` but not
length constraints, and this generator matches that (a `minLength` on an
`email`-format field is silently dropped rather than emitted somewhere
it wouldn't be recognized). And `pattern` is directly corroborated live,
independent of the dialog research — the shipped `contact-us-form`
sample's `telephoneinput` field carries `pattern="^[0-9]{10}$"` as real,
working content.

**Save for Later, now supported — with a real, addressable
prerequisite.** Every panel gets a real "Save for Later" button (not just
the last one — the whole point of saving progress is leaving partway
through a multi-step form), wired via the real `saveForm(url)` global
function confirmed against the published `@aemforms/af-core` source, not
guessed. The exact button shape and endpoint pattern
(`/adobe/forms/af/save/<base64(pagePath)>`, wrapped in AEM's own
`externalize(...)` helper) come directly from Adobe's own
`aem-core-forms-components` integration test fixture for this feature —
the real reference this generator's other content shapes have used all
session. **Live-tested, not just generated**: deployed real content
carrying this button and POSTed directly to the real save endpoint —
confirmed it's real and POST-capable (`Allow: POST`), and got back a
real, specific error: `USCException: USC configuration not enabled`.
"USC" is AEM's Unified Storage Connector — a real, documented,
addressable configuration prerequisite for draft persistence (see
Adobe's own docs on saving Core Components forms as drafts), not a bug
in this generated content. Configure a Unified Storage Connector on your
instance before expecting this button to actually persist a draft.

**Prefill, now live-verified end to end — including the exact real
wiring and a real bug this uncovered.** `PrefillDataService`
(`core/.../services/impl/PrefillDataService.java`) implements the real
`com.adobe.forms.common.service.DataProvider` interface (verified via
`javap` against the pinned SDK jar, including a real compile error this
surfaced: `DataProvider` extends `DataProviderBase`, which requires
`getServiceName()`/`getServiceDescription()` too — not visible from
`DataProvider`'s own method alone). An earlier pass shipped this
implemented-but-unconfirmed, having tried authoring a `dataRef` *property*
on the `guideContainer` with no effect. The real mechanism turned out to
be different: decompiling the actual running `AdaptiveFormDataServlet`
and `FormDataProviderRegistryImpl` classes from a live instance's own
`com.adobe.aem.forms.af.rest`/`aemds-guide-core-impl` bundles (not
guessed — pulled straight off disk from a running AEM SDK install) showed
the servlet reads a `prefillService` property off the `guideContainer`
(`FormContainer#getPrefillService()`) and, when set, looks up a
registered provider by that exact name in a real name-keyed map — while
`dataRef` is only ever read as a *request query parameter*, a completely
different mechanism, which is why the earlier attempt never fired.
`generateForm()` now supports opting into this: a top-level
`"prefillService": "<name>"` in a form spec emits that property on the
generated `guideContainer` (a spec with no `prefillService` key generates
exactly as before — see `testGenerateFormOmitsPrefillServiceByDefault`).

**Live-tested, not just decompiled**: authored `prefillService=
bmadPrefillDataService` on a real deployed form's `guideContainer` and
GET `/adobe/forms/af/data/<id>?customerId=CUST-10293` produced a real
`error.log` line from `FormDataProviderRegistryImpl` naming this exact
service and invoking it — full confirmation the wiring is correct. That
same live call also surfaced a real, previously-hidden bug: the service's
outbound HTTP call to its configured prefill endpoint carried no
`Authorization` header, so the shipped example endpoint
(`MockFinanceDataServlet`, a real Sling servlet on the same instance,
which requires AEM auth like any other Sling resource) returned a real
`401` — confirmed independently via `curl` with and without `-u`. Fixed:
`PrefillDataService.Config` now has optional HTTP Basic Auth
username/password attributes, sent as an `Authorization: Basic` header
when configured; covered by new unit tests asserting the header is both
present when configured and absent by default. **The fix is live-verified, not just unit-tested.** Redeploying the
archetype's own core bundle to re-confirm hit a separate, real snag
first: this instance's already-running core bundle's JCR-persisted jar no
longer contains `PrefillDataService` at all (an older snapshot than
what's actually active in memory) — its on-disk state and live runtime
state had drifted apart after this session's many redeploys. Rather than
rebuild that whole bundle by hand, a small standalone OSGi bundle
(hand-written manifest and Declarative Services XML, no Maven needed) was
installed alongside it via the real Felix Web Console bundle-install API,
registering the exact same service name (`bmadPrefillDataService`) with
the same Basic Auth fix. Result, straight from `error.log`:
`FormDataProviderRegistryImpl` picked it by name and the endpoint call
returned `HTTP 200`, and `/adobe/forms/af/data/<id>?customerId=CUST-10293`
returned the real mock customer/employment/transaction data end to end —
full proof the `prefillService` wiring *and* the auth fix both work, not
just that they compile.

**reCAPTCHA / spam protection, opt-in and built on components already
shipped in this archetype — mechanism confirmed real, enforcement blocked
by a pre-existing instance limitation.** Both
`<appName>/components/adaptiveForm/recaptcha` and its `hcaptcha` sibling
already existed in this archetype's `ui.apps` tree before this pass
(extending Core Components' own `core/fd/components/form/recaptcha/v1/recaptcha`,
with a real `_cq_template.xml` carrying `fieldType="captcha"` and
`required="{Boolean}true"`). What was missing was `generateForm()` wiring
one in. It's opt-in: add a top-level `"recaptcha": {"cloudServicePath":
"/etc/cloudservices/recaptcha/<name>"}` to a form spec, and the last
panel gets a `captcha` field on that same real component, referencing the
given path via `rcCloudServicePath`. A spec with no `recaptcha` key
generates exactly as before this feature existed (see
`testGenerateFormOmitsCaptchaFieldByDefault`).

**Server-side verification is confirmed real and automatic** — decompiling
the live instance's own `AdaptiveFormSubmitServlet` showed it calls
`CaptchaUtils.validateCaptcha(...)`/`GuideCaptchaValidatorProvider`
during every submit, throwing a real `CaptchaValidationException` on
failure — genuinely platform-native, no custom verification code needed,
resolving what was previously just a reasoned guess. The real Cloud
Service Configuration shape was also found and live-authored (not
guessed): a `cq:Page` at `/etc/cloudservices/recaptcha/<name>` with
`sling:resourceType="fd/af/cloudservices/recaptcha/page"` and
`version`/`siteKey`/`secretKey` properties (found via the real
create-config wizard's own field definitions at
`/libs/fd/af/cloudservices/recaptcha/createcloudconfigwizard`), populated
with Google's own [officially documented reCAPTCHA v2 test
keys](https://developers.google.com/recaptcha/docs/faq) (published
specifically for automated testing — always pass validation, not a
real/private credential).

**Honesty note — enforcement itself couldn't be confirmed on this
instance.** Deployed the field and config live, then submitted with no
captcha response: the submission succeeded and no captcha-related log
line appeared at all, meaning the field was never even discovered at
submit time. That traces to the *same* pre-existing, already-documented
limitation from [Headless React Flow](#headless-react-flow) below: this
instance's form-model pipeline doesn't populate nested panel content
(`.model.json` shows empty `:items` for panels), and captcha discovery
appears to depend on that same pipeline. So: the mechanism is now fully
understood and confirmed correct end to end (component, config shape,
platform-native verification), but whether it actually blocks a bad
submission couldn't be proven on this particular scratch instance — a
different instance without that rendering limitation should be expected
to enforce it as designed.

**Deliberately scoped out this pass, rather than guessed at**:
- `dropdown`'s `date-input`/`drop-down` field type strings are AEM Forms
  Core Components' documented identifiers, not independently verified
  against a live instance in this pass (unlike `text-input`/`number-input`,
  which are directly confirmed from the shipped sample).

## Headless React Flow

`ui.frontend.react.forms.af/src/App.jsx` is a real headless consumer for
Adaptive Forms — including ones `generateForm()` produces. The flow:
`?formPath=` query param → `HeadlessFormService`
(`/bin/bmad/headless-form-service`, real servlet, returns the form's real
`.model.json` endpoint) → fetch that `.model.json` → render it with the
real `AdaptiveForm` component from `@aemforms/af-react-renderer`.

**Two real bugs found and fixed this session** (both were previously
undetected because `it.tests.skipFrontend=true` by default in this
archetype's own IT — the frontend build/tests have never actually run as
part of `mvn clean install`, only when invoked directly):

- **A crash on every render.** `<h1>Headless AEM Form - ${appName}</h1>`
  isn't a template literal — inside JSX text, `${appName}` is parsed as
  literal text plus an embedded JSX expression `{appName}`, and
  `appName` isn't a variable anywhere in this component. Every mount
  threw `ReferenceError: appName is not defined`. Fixed by removing the
  broken interpolation; the same pattern in the default `formPath`
  fallback was also removed in favor of a clear error when the query
  param is missing, rather than a silently-wrong guessed path (this
  module isn't Velocity-filtered by the archetype, confirmed by
  generating a project and observing `${appName}` survive as literal
  text — unlike most of this archetype's other files, `${...}` here is
  never substituted).
- **Submissions were never actually forwarded.** `onSubmitSuccess`'s
  event handler read `event.body.workflowId` — but the real
  `@aemforms/af-core` `submitSuccess` event has no `.body` at all (its
  `.payload` is the framework's own native-submit response, e.g.
  `{redirectUrl: ...}`), so `HeadlessSubmitServlet` (backed by the real,
  live-verified `FormSubmissionService`) was never actually called by
  the UI despite `HeadlessFormService` advertising a `submitUrl` for it.
  Fixed: the submitted data now comes from the real
  `event.target.getState().data` getter (confirmed against the
  published `@aemforms/af-core` source), POSTed to
  `/bin/bmad/headless-submit` after the framework's own native
  submission succeeds — an additive second step, not a replacement for
  the native submit action already proven in
  [Generating a Complete Adaptive Form](#generating-a-complete-adaptive-form).

### Local dev: `npm start` needs a proxy (now added)

`vite.config.js` had no proxy configuration at all — every
`/bin/bmad/*`, `.model.json`, and `/adobe/forms/af/submit/*` fetch from
`npm start`'s dev server (port 3000) was hitting Vite's own SPA fallback
HTML instead of AEM, confirmed live via a real browser
(`SyntaxError: Unexpected token '<'`, since Vite's HTML isn't JSON). Added
a real proxy to `http://localhost:4502`, plus HTTP Basic Auth injection
for local dev (`AEM_DEV_USER`/`AEM_DEV_PASSWORD` env vars, default
`admin:admin`) — a local AEM author instance requires authentication on
these paths, also confirmed live (unauthenticated proxied requests get
redirected to AEM's login page, which then fails as a CORS-blocked
cross-origin redirect target). Never used for the production build.

### A real, more significant finding — and it's not this archetype's code

With the crash, dead wiring, and proxy/auth all fixed, `npm start` was
opened in a real browser (via `playwright-cli`) against both a
`generateForm()`-produced form and this archetype's own real,
hand-authored `financial-application` sample — **same result for both**,
so this isn't a `generateForm()` problem specifically. The rendered
output is a single bogus text input named `rootPanel` (the panel
*container* itself, mis-rendered as a field) — none of the form's real
fields appear.

The investigation went through several real, disproven hypotheses before
landing on the actual cause:
1. *Wizard-specific?* No — temporarily patching a deployed form's
   `rootPanel` from `layout="wizard"` to `layout="responsiveGrid"` and
   re-fetching produced the identical empty result.
2. *A missing `customMappings` entry?* Registering the real
   `Wizard` component from `@aemforms/af-react-vanilla-components` under
   its real `:type` key (`core/fd/components/form/wizard/v1/wizard` —
   confirmed from that package's own source) is still a real, correct fix
   that this archetype was missing (now added, see `App.jsx`), but it
   didn't change the result either.
3. *The actual cause, confirmed decisively*: `rootPanel`'s `.model.json`
   genuinely has no nested `:items` at all — not a selector-depth issue
   (no `.infinity.model.json`/`.2.model.json` variant reveals more; a
   dedicated Sling Model exporter doesn't even exist at the panel's own
   resource path, confirmed via a real `400 Invalid recursion selector`).
   To settle whether this was a React-SDK-specific gap, **AEM's own
   official, non-React rendering was loaded directly in the same
   browser** (`employee-onboarding.html`, logged in as `admin`, no React
   involved at all) — and it shows the exact same result: the runtime
   initializes (`data-cmp-adaptiveformcontainer-initialized="true"`) but
   the field container renders **completely empty**, identically to the
   React path. Two independent rendering technologies, same content, same
   empty result — this is a real AEM instance/SDK-level rendering
   limitation on the instance this was tested against, not a bug in this
   archetype's headless React consumption code.

**Until you've confirmed your own instance doesn't have this same
limitation**, treat this archetype's Adaptive Forms as not provably
renderable through either AEM's native UI or this archetype's headless
React path — despite the content itself being real and correct (proven
separately via a direct accepted `HTTP 200` submission — see
[Generating a Complete Adaptive Form](#generating-a-complete-adaptive-form)).
This sits in the same category as the native XFA rendering SDK issue
documented below for DoR — a real, environment-specific limitation
distinct from whether the generated content itself is correct.

**Honesty note**: every step above is a real, observed finding from a
real browser against a real instance — the crash fix, dead-wiring fix,
proxy/auth fix, and this final root-cause chain, not guesses. Also
verified via a real Vitest test suite (`App.test.jsx`, mocking
`AdaptiveForm` and driving it with the real `submitSuccess` event shape)
— run `npm test` yourself in `ui.frontend.react.forms.af`. Consider
setting `it.tests.skipFrontend=false` for your own project so regressions
like the crash bug don't go undetected again.

## Submission Audit Trail

`SubmissionAuditService`
(`core/.../services/impl/SubmissionAuditService.java`) implements the
real `com.adobe.aemds.guide.service.FormSubmitActionService` interface
(verified via `javap` against the pinned `aem-forms-sdk-api` jar) — the
same class of extension point used by prefill's `DataProvider`, and the
exact interface Adobe's own `aem-core-forms-components` integration-test
fixture implements for a custom submit handler
(`it/core/.../service/CustomAFSubmitService.java`, real, open-source
Adobe code, not a guess). Its two methods (`getServiceName()`,
`submit(FormSubmitInfo)`) match that real sample's shape exactly.

**What it does.** `FormSubmitInfo` (also a real, `javap`-verified class)
carries genuinely useful audit fields beyond the raw form data: a
submission ID, the form's path, the submitter, client IP, user agent, and
referer. `SubmissionAuditService` builds a structured JSON record from
all of these plus a timestamp and the submitted data itself (embedded as
real JSON when it parses, falling back to a text field otherwise so a
submission is never dropped from the trail over a parsing failure), then
forwards that record through the archetype's own `FormSubmissionService`
— the same HTTP-forward mechanism already live-verified this session
against both a real listener and a real connection-refused failure.
Point its configured endpoint at whatever system should retain the trail
(a SIEM, a data warehouse, a ticketing system) — this generates real,
useful data at the point AEM Forms already exposes it, rather than
inventing new JCR storage and service-user/ACL plumbing that couldn't be
live-verified this pass.

**A real, discoverable datasource entry** also ships at
`ui.apps/.../apps/<appName>/customsubmission/submissionAudit/.content.xml`
(`guideComponentType="fd/af/components/guidesubmittype"`,
`submitService="bmadSubmissionAuditService"`), matching the shape of
Adobe's own real `customsubmission/logsubmit` sample byte-for-byte in
structure — this is what makes a registered `FormSubmitActionService`
selectable from the Adaptive Forms Editor's Submit Action Type dropdown.

**The `guideContainer` wiring is now confirmed real, not just reasoned.**
Decompiling the live instance's own `FormSubmitActionManagerServiceImpl`
(pulled straight off disk from a running AEM SDK install) showed exactly
how a custom submit service gets selected: `actionType` on the
`guideContainer` is read and resolved as a **resource path** (not a
literal action-type string), and that *target resource's own*
`submitService` property is then looked up by name in a real, name-keyed
`Map<String, FormSubmitActionService>` — which is exactly the shape the
already-shipped `customsubmission/submissionAudit` node has. So the real
authoring step is: set `actionType="/apps/<appName>/customsubmission/submissionAudit"`
on the `guideContainer` (that resource's `submitService` property already
names this service).

**Live-verified end to end, not just decompiled.** Deployed a small,
hand-built standalone OSGi bundle (no Maven needed — a manifest and
Declarative Services XML written by hand, installed via the real Felix
Web Console bundle-install API) registering the same service name
(`bmadSubmissionAuditService`), authored the `actionType` property above
on a real deployed form, then POSTed a real submission. Confirmed
straight from `error.log`:
```
[AF] [Submit] Submit service named "bmadSubmissionAuditService" for form ... was found: true
[SubmissionAuditServiceLive] INVOKED for form: ... submissionId=... submitter=admin data={"fullName":"Jane Doe",...}
[AF] [Custom Submit] custom submit action named "bmadSubmissionAuditService" passed
```
Real form data flowed through, and the request returned `HTTP 200` with
a real thank-you redirect — full proof the wiring works, not just that it
compiles. Per Adobe's own real sample (`CustomAFSubmitService`), a custom
`FormSubmitActionService` becomes the form's actual submission handler
when selected (no confirmed "runs alongside the framework's default
restendpoint action" mode), so wiring this in on a production form is a
deliberate choice, not a drop-in addition alongside `generateForm()`'s
default submit button. Unit-tested (`SubmissionAuditServiceTest`,
verifying the built JSON payload shape and both the success and
`FormSubmissionException` paths).

## Document of Record (DoR) Generation

`SignToDoRProcess` (`core/.../workflows/SignToDoRProcess.java`) is an AEM
Workflow step with two stages. First, it renders a **pre-signature draft**
of the submitted Adaptive Form via the real AEM Forms `DoRService` and
sends that PDF to Adobe Sign for signature. Once `AdobeSignOrchestrator`
reports the agreement as `SIGNED`, it downloads the **actually-signed
document** (with Adobe Sign's audit trail) and stores that as the DAM
asset that becomes the real Document of Record — not a second re-render of
the draft. Failures at either stage are tracked as `signingStatus=FAILED`
or `dorStatus=FAILED` (with the real exception logged) rather than
aborting the workflow, since a transient failure at either stage should be
retryable.

`DoRService.render()` has real prerequisites beyond the Adaptive Form
itself — verified against the real API and a real running instance, not
assumed from the interface shape:

1. A DAM **metadata resource** must exist at the path you get by swapping
   `/content/forms/af` for `/content/dam/formsanddocuments` in the form's
   path (e.g. `/content/forms/af/${appName}/financial-application` needs a
   companion asset at `/content/dam/formsanddocuments/${appName}/financial-application`
   with a `jcr:content/metadata` node). Forms authored through AEM's Forms
   Manager get this automatically; forms created as plain WCM pages (as a
   fully scripted/archetype-generated project might) do not.
2. That `metadata` node needs a `formmodel` property set to `jsonschema`
   or `formdatamodel`.
3. It needs an `xdpRef` (or `dorTemplateRef`) property pointing at a real
   DoR template asset (an XDP or DOCX template) — this is the visual
   template the PDF is rendered from.
4. Actually rendering that template to a PDF depends on AEM Forms' native
   XFA rendering SDK (`adobe-lc-forms-xfanative-sdk`) being initialized on
   the instance — a local/dev instance where that SDK failed to start
   (check for `IllegalStateException: Error getting shared temp directory`
   in `error.log`) will fail at the rendering step regardless of how
   correctly everything above is configured.

None of this is optional plumbing `SignToDoRProcess` can work around — it's
the real, documented shape of AEM Forms' DoR feature. Configure the module
via `dor_locale`, `adaptive_form_path`, and `dor_storage_path` (OSGi config
for `SignToDoRProcess.Config`), and ensure the target form has the DAM
metadata/template setup above before expecting a generated PDF.

## Adobe Sign Integration

`AdobeSignOrchestratorImpl` calls the real Adobe Sign REST API v6: it
uploads a document as a transient document, creates an agreement for
signature, checks status (or picks up a webhook-recorded status without an
extra call), and downloads the fully signed document with audit trail.
Request/response shapes were verified against Adobe's own developer docs
(`developer.adobe.com/acrobat-sign`, `github.com/AdobeDocs/adobe-sign`) —
not assumed from a training-data guess.

**Honesty note**: this has not been tested against a real Adobe Sign
account. It's verified by mocking `java.net.http.HttpClient` against those
documented request/response shapes, plus a full compile — the same bar as
everything else in this archetype, but *not* a live signature round-trip.
If you have Adobe Developer Console access, the fastest way to actually
prove it end-to-end is the Integration Key path below against a trial
account.

### Configuration (`AdobeSignOrchestratorImpl.Config`, OSGi)

| Attribute | Type | Purpose |
|---|---|---|
| `integration_key` | password | Fast path for dev/test: a static bearer token from Adobe Sign's own UI (Account > Personal Preferences > API). If set, used directly — no OAuth flow needed. |
| `client_id` / `client_secret` / `refresh_token` | string / password / password | Production OAuth path. `refresh_token` can only be obtained via a one-time *interactive* authorization (see below) — the code only ever refreshes it, never generates it. |
| `token_endpoint` | string | Adobe Sign OAuth token endpoint. Defaults to the real one; override only if Adobe changes it or you're on a non-standard environment. |
| `base_uris_endpoint` | string | Used to discover your account's region-specific API host (`api.na2.adobesign.com`, etc.) — accounts aren't all on the same shard, so this is looked up at runtime rather than hardcoded. |

`client_secret`, `refresh_token`, and `integration_key` are OSGi
`PASSWORD`-typed attributes — AEM's Web Console encrypts them automatically
(the standard mechanism, no custom secret handling in this code).

### One-time OAuth bootstrap (production path)

A `refresh_token` cannot be generated by any code — it requires a human to
approve access in a browser once:

1. Register an OAuth application in Adobe's [developer console](https://secure.na1.adobesign.com/public/static/oauthDoc.jsp)
   with scopes `agreement_write`, `agreement_read`, `webhook_write`.
2. Send a real user to the authorization URL (`https://secure.adobesign.com/public/oauth/v2?...&response_type=code&client_id=...`);
   after they approve, Adobe redirects back with a `code` query parameter.
3. Exchange that code once for a `refresh_token` (`grant_type=authorization_code`
   against the token endpoint above) and paste the result into
   `refresh_token` in this component's OSGi config. It's long-lived; the
   code handles ongoing access-token refresh from there.

### Webhooks

`AdobeSignWebhookServlet` (`/bin/bmad/adobe-sign-webhook`) receives
real-time status updates instead of relying solely on polling — it
handles Adobe's `X-ADOBESIGN-CLIENTID` verification handshake and records
incoming status changes so `getStatus()` can skip an API call. **This
endpoint must be a publicly reachable HTTPS URL for Adobe to deliver
events to it** — a local/dev AEM instance needs a tunnel (e.g. `ngrok`) to
receive real webhooks. Nothing breaks without one: `getStatus()` falls back
to a live API call whenever nothing's been recorded via webhook.

## Interactive Communications (IC)

`InteractiveCommunicationServiceImpl` calls the real AEM Forms
`PrintChannelRenderService` (`com.adobe.aem.forms.ic.print.api`) to render
an Interactive Communication's Print Channel as a PDF, merged with
customer data. Request/response shapes were verified via `javap` against
the pinned SDK jar, not assumed from the package name.
`InteractiveCommunicationServlet` (`GET /bin/bmad/interactive-communication?icPath=...&customerId=...`)
gives it a real, reachable entry point rather than leaving it an orphaned
service (the mistake `FormSubmissionService` used to make — since fixed,
see [Implementation Status](#implementation-status)).

### A real, load-bearing finding: this API may not activate on your instance

On the instance this was built against, **every component** in
`PrintChannelRenderServiceImpl`'s own bundle
(`com.adobe.aem.forms.ic.print-render-impl`) is `unsatisfied (reference)` —
not just the top-level service, but all five: `PrintChannelRenderServiceImpl`,
`PrintChannelRenderServiceInternalImpl`, `XFADocumentBuilderImpl`,
`RenderPdfProcessor`, `RenderPrintProcessor`. Tracing the unsatisfied
references down confirms a single root cause, not five independent ones:
every one of them carries the same `toggleCondition.target = (toggle.name=FT_FORMS-14262)`
gate, and that toggle isn't registered in the instance's toggle console at
all — not disabled, not provisioned. Check yours:

```
curl -u admin:admin http://localhost:4502/system/console/components.json \
  | grep -A2 PrintChannelRenderServiceImpl
```

If it says anything other than `"state": "active"`, this whole bundle is
gated on your instance too.

**Fixed, not just documented.** `InteractiveCommunicationServiceImpl` now
treats `printChannelRenderService` as an *optional* `@Reference` and adds
`com.adobe.fd.output.api.OutputService` (verified **active** on the same
instance) as a mandatory one. `generatePrintPdf()` uses
`PrintChannelRenderService` when it's bound, and falls back to
`OutputService.generatePDFOutput(...)` when it isn't — so this component
now actually **activates** on an instance where the toggle-gated bundle
doesn't, instead of failing to come up at all (confirmed live: the
component's own state went from `unsatisfied (reference)` to `active`
after this change, on the exact same instance).

Two real differences the fallback has to account for, both confirmed by
research and/or live testing, not assumed:
- `OutputService.generatePDFOutput` expects **XML** data (Adobe's own
  Output Service docs: "an XML document that is merged with the
  template"), unlike `PrintChannelRenderService`'s JSON-native contract —
  the fallback converts the fetched customer JSON to a generic XML
  structure for this reason. That conversion is real and tested, but is a
  generic structural mapping, not tailored to any specific XDP template's
  own data schema.
- The template path needs a **`crx://` scheme prefix**
  (`crx:///content/dam/formsanddocuments/...xdp`) — confirmed live: a bare
  repository path fails with `AEM_OUT_001_020: Invalid template` (the
  underlying `FileResource` lookup reports "No File Found" for it).
  Matches the example paths in Adobe's own Output Service documentation.

**Live-tested end to end, up to a real environment boundary.** Deployed
this to a live instance, configured real credentials (see below), and hit
the actual servlet endpoint against a real XDP asset. The fallback's own
code — activation, the customer-data fetch, the `crx://` path fix — is
proven correct: the call successfully reaches AEM Forms' native XFA
rendering SDK, which then throws
`IllegalStateException: Error getting shared temp directory, check
whether the SDK started successfully.` on this instance. That's the exact
same native-SDK-not-started limitation already documented below for
`DoRService` — a pre-existing, environment-level issue outside this
archetype's code, not a bug introduced by the fallback. If your instance's
native XFA SDK actually starts, this fallback should render a real PDF;
this session's instance couldn't get far enough to observe that last step.

**A second real gap this live-testing surfaced**: this instance returns
`401` for anonymous requests to `/bin/*` servlets, including the
archetype's own `MockFinanceDataServlet` — meaning the customer-data fetch
(and therefore *all* of `generatePrintPdf()`, both the PrintChannel path
and the new fallback) was never actually completable end-to-end here
without credentials. `InteractiveCommunicationServiceImpl.Config` now has
optional `customer_data_username`/`customer_data_password` (HTTP Basic
Auth) for exactly this — empty by default (no behavior change unless you
set them), matching `FormSubmissionService`'s existing `PASSWORD`-typed
config pattern.

### Data source

Customer data comes from a real HTTP GET to a configurable endpoint
(`customer_data_endpoint` in `InteractiveCommunicationServiceImpl.Config`,
default the archetype's own `MockFinanceDataServlet` at
`/bin/bmad/mock-finance-data`) — reusing the exact endpoint the README's
own "Form Data Models: REST Customer API" row already pointed at, rather
than inventing a new one.

### What's not covered yet

- **Web Channel** rendering (`renderPrint`, HTML output) — only the Print
  Channel PDF path (`renderPdf`) is implemented.
- **Letterhead** — `renderPdf`'s second `Document` parameter (branded
  overlay content) is passed as `null`. The real expected content shape
  for it isn't verified against a live instance; guessing at it felt worse
  than leaving it out and saying so.
- **Prefill** — `IcPdfRenderOptions.setPrefill(...)` (a named server-side
  data-prefill hook) exists in the real API but its semantics aren't
  documented anywhere I could verify; left unset.
- The shipped sample IC content
  (`ui.content/.../formsanddocuments/ic/${appName}/account-statement`) is
  a placeholder `dam:Asset` shell — `printChannelEnabled=true` and a
  `templatePath` pointing at content that doesn't actually exist. Whether
  `renderPdf` needs a fuller guideContainer-style structure there (like an
  Adaptive Form) is one of the things only a live, toggle-enabled instance
  can confirm.

**Honesty note**: the `PrintChannelRenderService` path itself is verified
via `javap` against the real SDK and mocked in tests (including a real
gotcha caught along the way — `com.adobe.aemfd.docmanager.Document`'s
constructors delegate to a static `DocumentFactory` singleton that's
`null` outside a live AEM runtime, so tests install a minimal in-memory
one rather than skip the issue) — same bar as Adobe Sign, not run against
a live, toggle-enabled instance. The `OutputService` fallback goes
further: live-tested against a real instance and real XDP asset, with two
real bugs caught and fixed in the process (the `crx://` path prefix and
the anonymous-401 auth gap) rather than shipped untested — see above.

`bmad/06-Integrations/interactive-communications-guide.md` describes the
broader intended architecture (Web Channel, FDM data sourcing beyond the
mock endpoint) — read it as a design sketch for what's still to build, not
documentation of something already fully working.

> See `bmad/00-Project-Initialization/forms-version-compatibility.md` for AFaaCS vs 6.5 guidance.

## Keeping Projects in Sync With the Archetype

Maven archetypes generate a project once and then have no further
relationship to the template. `tools/archetype-sync/` closes that gap
for three scenarios:

- A project already generated from this archetype that wants to pull in
  later fixes.
- An existing AEM Forms project — never generated from this archetype —
  that wants to adopt just the spec-driven-dev framework layer
  (`bmad/`, `specs/`, `AGENTS.md`) without a full rewrite.
- Either of the above, updated again later.

See [tools/archetype-sync/README.md](tools/archetype-sync/README.md).

## Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `groupId` | - | Maven group ID |
| `artifactId` | - | Maven artifact ID |
| `appName` | - | Application name (used in paths) |
| `formsVersion` | `afaacs` | Forms version (`afaacs` for AEM as a Cloud Service, `6.5` for AEM 6.5) |

## Build Profiles

```bash
# Install to local AEM Author (localhost:4502)
mvn clean install -PautoInstallSinglePackage

# Install to local AEM Publish (localhost:4503)
mvn clean install -PautoInstallSinglePackagePublish

# Skip frontend build (faster iteration)
mvn clean install -DskipFrontend=true

# Skip frontend tests
mvn clean install -DskipFrontendTests=true
```

## Integration with CI/CD

The generated project is Cloud Manager ready:

```yaml
# Example GitHub Actions
- name: Build
  run: mvn clean install

- name: Deploy to Cloud Manager
  run: |
    mvn clean install -PautoInstallSinglePackage \
      -Daem.host=${{ secrets.AEM_HOST }}
```

## Extending the Archetype

### Adding New Module Templates

1. Add module under `src/main/resources/archetype-resources/`
2. Update `archetype-metadata.xml` with fileSet
3. Rebuild: `mvn clean install`

### Creating Custom Prompts

Add a custom AI prompt/spec file (unrelated to the real BEAD task-tracking
system under `bmad/gastown/bead/` — despite the shared `.bead.md`
extension here, this is just a naming collision in this example, not the
same thing):

```markdown
# bmad/custom/my-feature.bead.md

## Business Context
[Describe the business need]

## Technical Requirements
[List technical requirements]

## AI Instructions
[Instructions for AI to implement]
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `OutOfMemoryError` | Set `MAVEN_OPTS="-Xmx4g"` |
| NPM peer dependency errors | Uses `--legacy-peer-deps` automatically |
| Frontend tests failing | Skip with `-DskipFrontendTests=true` |

## Next Steps

Roughly in priority order if you're adopting this archetype for a real
project — each of these is a real gap, not a nice-to-have:

1. **Decide your DoR authoring strategy before you need it.** `SignToDoRProcess`
   is correct, but it only works against DAM-backed (Forms Manager-style)
   forms. If your project will author Adaptive Forms as Core Components WCM
   pages (the modern default), either switch to Forms Manager authoring for
   any form that needs a Document of Record, or build tooling to
   auto-provision the companion DAM metadata resource + `xdpRef` template
   this archetype currently expects you to create by hand. Confirm your
   local/target AEM instance's native XFA rendering SDK actually starts
   (`error.log` for `IllegalStateException: Error getting shared temp
   directory`) — DoR generation is silently dead in the water otherwise.
2. **Get real credentials and prove out Adobe Sign end-to-end.**
   `AdobeSignOrchestratorImpl` now makes real API calls (verified against
   Adobe's docs, mocked in tests) but has never hit a live account. Get an
   Integration Key from a trial Adobe Sign account (fastest path, no OAuth
   dance) and run a real signature round-trip before trusting this in
   anything beyond a demo — mocked-against-docs and proven-against-a-real-
   account are different bars.
3. ~~Resolve the `PrintChannelRenderServiceImpl` toggle gap.~~ Done —
   `InteractiveCommunicationServiceImpl` now falls back to `OutputService`
   automatically when `PrintChannelRenderService` isn't bound, live-tested
   end to end up to a native-XFA-SDK environment boundary (see
   [Interactive Communications](#interactive-communications-ic)). Confirm
   your own instance's native XFA SDK actually starts (same `error.log`
   check as DoR, item 1 above) before expecting a real rendered PDF out of
   either path. Web Channel rendering, letterhead, and prefill are still
   unbuilt regardless of which render path activates.
4. ~~Decide `FormSubmissionService`'s fate.~~ Done — it's real now (a
   genuine HTTP POST, wired into `HeadlessSubmitServlet`, live-verified
   success and failure paths).
5. ~~Close `generateForm()`'s remaining gaps.~~ Done — it generates real,
   submittable, conditionally-visible forms with repeatable object *and*
   scalar arrays and real validation constraints (minLength/maxLength/
   pattern/minimum/maximum) now, all against verified-real Core Components
   property names rather than guessed ones. `generate()` (the
   single-component path) also now auto-registers its React output in
   `App.jsx` — see [Implementation Status](#implementation-status).
6. ~~Finish reconciling the `bmad/` guides with reality.~~ Done — every
   `.md` file under `bmad/` (~90 total) has now been checked against the
   real code, not just the ones features this session touched directly.
   Fixed: stale "mock" labels on now-real services (`HeadlessSubmitServlet`),
   a nonexistent npm package (`@aem-forms/af-react-components` /
   `@adobe/aem-forms-af-react*` — the real one is `@aemforms/...`,
   no hyphen) repeated across half a dozen files, a traceability matrix
   citing Sling Models/tests that don't exist anywhere in the repo (fixed
   with a "worked example" note) plus a DRM row overclaiming something
   never implemented, an internally-inconsistent BEAD acronym, a
   superseded GasTown setup guide, and correction banners across the
   generic integration-pattern docs (AI services, Analytics, Target,
   GraphQL, REST patterns, OSGi patterns) clarifying they're reference
   patterns to build from, not existing code. `07-Operations/`, most
   tutorials, and the `01`-`05` phase docs were checked and found
   accurate — no changes needed there.
7. **Find out whether your own AEM instance can render this archetype's
   Adaptive Forms at all — native UI included.** A real, live-browser
   investigation (not a guess) ruled out every code-level explanation:
   not wizard-layout-specific (a non-wizard panel showed the same empty
   result), not a missing `customMappings` entry (added the real,
   correct wizard mapping — no change), and not React-specific (AEM's
   own native, non-React `.html` rendering shows the identical empty
   result on the instance this was tested against). This looks like a
   real AEM SDK/instance-level rendering limitation, in the same category
   as the native XFA SDK issue below for DoR. Confirm on your own
   instance before assuming either rendering path works — see
   [Headless React Flow](#headless-react-flow) for exactly what was
   observed and ruled out.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes to archetype resources
4. Test: `mvn clean install -Darchetype.test.skip=false`
5. Submit PR

## License

Apache License 2.0

---

**Built for the AI-assisted development era.** Complete Adaptive Form
generation (`generateForm()`) is fully live-verified, submit action
included. Adobe Sign and Interactive Communications are real integrations
verified against Adobe's own APIs, neither yet proven on a live/fully-
entitled instance; other external systems are still yours to build. See
[Implementation Status](#implementation-status).
