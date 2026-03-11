# AEM Forms BMAD Archetype

A Maven archetype for creating AI-ready AEM Forms projects using the **BMAD** (Business-Model-Architecture-Development) methodology. Generate production-ready AEM Forms projects in minutes, not days.

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
    -DaemVersion=cloud

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

1. **Define a spec** in `specs/my-component.json`:
```json
{
  "name": "CustomerFeedback",
  "fields": [
    {"name": "rating", "type": "number", "min": 1, "max": 5},
    {"name": "comment", "type": "string", "maxLength": 500}
  ]
}
```

2. **Ask AI to generate**:
   - Sling Model in `core/`
   - AEM component in `ui.apps/`
   - React component in `ui.frontend.react.forms.af/`

3. **Build and deploy**:
```bash
mvn clean install -PautoInstallSinglePackage
```

## Interactive Communications (IC)

The archetype includes full support for **AEM Forms Interactive Communications** - personalized, multi-channel document generation.

### Included IC Assets

| Asset Type | Location | Description |
|------------|----------|-------------|
| **Document Fragments** | `/content/dam/formsanddocuments/fragments/${appName}/` | Reusable content blocks |
| **Sample ICs** | `/content/dam/formsanddocuments/ic/${appName}/` | Account Statement, Welcome Kit |
| **Form Data Models** | `/content/dam/formsanddocuments-fdm/${appName}/` | REST Customer API |
| **OSGi Configs** | `ui.config/` | Output Service, Document Merge |

### Document Fragments

Pre-built fragments for common use cases:
- `header` - Branded document header
- `footer` - Contact info and disclaimers
- `terms-and-conditions` - Legal text
- `customer-details` - Name/address block

### Creating an Interactive Communication

```bash
# 1. Configure your data source (Form Data Model)
# 2. Create fragments for reusable content
# 3. Design Print Channel (PDF) and Web Channel (HTML)
# 4. Generate via API or workflow
```

### IC Generation API

```java
// Generate account statement
InteractiveCommunicationService icService;

Document pdf = icService.generate(
    "/content/dam/formsanddocuments/ic/${appName}/account-statement",
    customerId,
    new ICOptions().setChannel(Channel.PRINT)
);
```

### Use Cases

- **Account Statements** - Monthly financial summaries
- **Welcome Kits** - New customer onboarding
- **Policy Documents** - Insurance/legal documents
- **Correspondence** - Personalized letters

> See `bmad/06-Integrations/interactive-communications-guide.md` for full documentation.

## Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `groupId` | - | Maven group ID |
| `artifactId` | - | Maven artifact ID |
| `appName` | - | Application name (used in paths) |
| `aemVersion` | `cloud` | AEM version (`cloud`, `6.5`) |
| `includeSpecDrivenExamples` | `true` | Include spec-driven form examples |
| `includeShowcaseExamples` | `true` | Include showcase components |

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

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes to archetype resources
4. Test: `mvn clean install -Darchetype.test.skip=false`
5. Submit PR

## License

Apache License 2.0

---

**Built for the AI-assisted development era.** Generate, iterate, deploy faster.
