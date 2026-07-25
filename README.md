# AEM Forms BMAD Archetype

A Maven archetype for creating AI-ready AEM Forms projects using the **BMAD** (Business-Model-Architecture-Development) methodology.

## Implementation Status

Read this before adopting the archetype — it tells you what's real, verified
code versus scaffolding you'd still need to build.

| Capability | Status | Notes |
|---|---|---|
| `SpecToCodeGenerator` | **Real** | Generates a Sling Model + HTL component + React field component from a JSON Schema spec. Verified via real compile/deploy. Scaffolds custom field *components*, not whole forms — you still assemble panels/layout/submission actions in AEM Forms Editor. |
| `SignToDoRProcess` (Document of Record) | **Real** | Calls the actual AEM Forms `DoRService`, verified against a real running instance. Has real prerequisites — see [Document of Record (DoR) Generation](#document-of-record-dor-generation) below; your form needs to be DAM-backed (Forms Manager-style), not just a WCM page, for it to work. |
| `AdobeSignOrchestrator` | **Real, not live-tested** | `AdobeSignOrchestratorImpl` calls the real Adobe Sign REST API v6 (transient document upload, agreement creation, status, signed-document download, webhooks). Request/response shapes verified against Adobe's own docs and mocked in tests — not yet run against a real Adobe Sign account. See [Adobe Sign Integration](#adobe-sign-integration). |
| `FormSubmissionService` | **Stub** | Logs and returns; the `// TODO: Replace with a real HTTP client call` in the source is accurate. Not currently referenced by `SignToDoRProcess`. |
| Interactive Communications | **Not implemented** | No `InteractiveCommunicationService` class exists anywhere in the codebase. There is real sample DAM content (fragments, IC template folders) but no service to render anything from it — see [Interactive Communications (IC)](#interactive-communications-ic). |

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
     field component's own code manages. To actually make one repeatable:
     register the generated component in `App.jsx`'s `customMappings`
     under its own field type, then configure the containing panel as
     repeatable (`minItems`/`maxItems`) in AEM Forms Editor.

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

**Not implemented.** There is no `InteractiveCommunicationService` class,
or any other service, anywhere in this codebase. What does exist is sample
DAM content — reusable document fragments and two example IC content nodes
— which is real, inspectable content modeling, but nothing renders it into
an actual document.

### What actually exists

| Asset Type | Location | Description |
|------------|----------|-------------|
| **Document Fragments** | `/content/dam/formsanddocuments/fragments/${appName}/` | Reusable content blocks (`header`, `footer`, `terms-and-conditions`, `customer-details`) — real JCR content, no rendering logic. |
| **Sample IC content nodes** | `/content/dam/formsanddocuments/ic/${appName}/` | `account-statement`, `welcome-kit` — placeholder content nodes, not functioning Interactive Communications. |
| **OSGi Configs** | `ui.config/` | Output Service / Document Merge configuration exists, but nothing in `core/` calls it for IC generation. |

### If you need real IC

Building `InteractiveCommunicationService` is a real, scoped project of its
own — the same kind of ground-truth API verification this session did for
`DoRService` (real AEM Forms Output Service APIs, a real Form Data Model
data source, real Print/Web channel rendering) would need to happen before
writing the calling code. The content structure above is a reasonable
starting point for the DAM layout; the service layer needs to be built from
scratch.

`bmad/06-Integrations/interactive-communications-guide.md` describes the
intended architecture (Print/Web channel split, FDM data sourcing) — read
it as a design sketch to build against, not documentation of something
already working.

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

Add BEAD (Business Entity AI Definition) files:

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
3. **Scope and build `InteractiveCommunicationService` from scratch** if IC
   is actually part of your project — do the same ground-truth API research
   this session did for `DoRService` (real AEM Forms Output Service /
   Print-Web channel APIs) rather than assuming the interface shape. Budget
   this as a real project phase, not an extension.
4. **Decide `FormSubmissionService`'s fate.** It's an orphaned TODO stub no
   longer referenced by `SignToDoRProcess`. Either implement its real HTTP
   call and re-wire something to use it, or delete it — a stub that looks
   like working code is a liability, as this session's audit of
   `InteractiveCommunicationServiceTest` demonstrated.
5. **Treat `SpecToCodeGenerator` as a component generator, not a form
   generator.** Use it to cut boilerplate for custom field components; plan
   on assembling actual form panels/layout/submission actions by hand in
   AEM Forms Editor.
6. **Reconcile the `bmad/` guides with reality.** Several BEAD/guide docs
   (e.g. `interactive-communications-guide.md`) describe features as if
   they're implemented. Since these are meant to brief an AI assistant
   before it writes code, an aspirational doc read as fact will make the
   next person's (or the next AI's) starting assumptions wrong in exactly
   the way this session's audit found.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes to archetype resources
4. Test: `mvn clean install -Darchetype.test.skip=false`
5. Submit PR

## License

Apache License 2.0

---

**Built for the AI-assisted development era.** Solid scaffolding for custom
components and workflow patterns. Adobe Sign is a real integration verified
against Adobe's docs but not yet a live account; IC and other external
systems are still yours to build. See [Implementation Status](#implementation-status).
