# Architect Guide: Designing AEM Solutions with BMAD

This guide helps Solution Architects effectively use the BMAD (Breakthrough Method for Agile Development) framework to design robust, scalable AEM as a Cloud Service architectures.

## Your Role in BMAD

As a Solution Architect, you act as the **Architect Agent** in the BMAD framework. Your responsibilities include:

- Designing system architecture for AEM as a Cloud Service
- Defining component specifications and technical standards
- Creating content models and information architecture
- Configuring Dispatcher rules and caching strategies
- Defining integration patterns with external systems
- Ensuring non-functional requirements (performance, security, scalability)

## Getting Started

### Step 1: Understand the Project Structure

```
bmad/
├── 02-Model-Definition/           # Content architecture
│   ├── content-models.md          # Templates, components, content fragments
│   ├── information-architecture.md # Site structure, navigation
│   └── design-system.md           # Design tokens, patterns
├── 03-Architecture-Design/        # Your primary workspace
│   ├── system-architecture.md     # High-level architecture
│   ├── component-design.md        # Component specifications
│   └── dispatcher-rules.md        # CDN/Dispatcher config
├── 06-Integrations/               # Integration patterns
│   ├── osgi-services.md
│   ├── rest-api-patterns.md
│   └── external-services-integration.md
└── traceability-matrix.md         # Track requirement coverage
```

### Step 2: Review Business Requirements

Before designing, thoroughly review:

1. **Business Requirements**: [requirements.md](../01-Business-Discovery/requirements.md)
2. **User Stories**: [user-stories.md](../01-Business-Discovery/user-stories.md)

Create a technical requirements summary:

| Business Requirement | Technical Implication |
|---------------------|----------------------|
| Multi-language support | MSM/Live Copy architecture, i18n framework |
| Personalization | Adobe Target integration, ContextHub |
| High availability | AEM as a Cloud Service auto-scaling |
| Fast page loads | Dispatcher caching, CDN optimization |

## Phase 02: Model Definition

### Content Models

Define the content architecture in `content-models.md`:

#### Template Definition

```markdown
## Page Templates

### Marketing Landing Page Template

**Purpose**: Flexible landing page for campaigns

**Structure**:
- Header (fixed)
- Hero Container (parsys - Hero components only)
- Main Content (parsys - all components)
- Related Content (fixed - Content Fragment List)
- Footer (fixed)

**Allowed Components**:
- Hero Banner
- Text with Image
- Card Container
- CTA Block
- Video Player

**Policies**:
- Max 1 Hero component
- Responsive grid: 12 columns
```

#### Component Specification

```markdown
## Hero Component

**Type**: Content Component
**Category**: Lead Generation

### Content Model

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| headline | String | Yes | Main headline (max 80 chars) |
| subheadline | String | No | Supporting text (max 200 chars) |
| backgroundImage | Asset Reference | Yes | Hero background image |
| ctaText | String | No | Button label |
| ctaLink | Path/URL | No | Button destination |
| ctaTarget | Dropdown | No | _self, _blank |

### Renditions Required

| Name | Dimensions | Use Case |
|------|------------|----------|
| desktop | 1920x800 | Large screens |
| tablet | 1024x600 | Tablet portrait |
| mobile | 768x400 | Mobile devices |
```

Reference: [content-models.md](../02-Model-Definition/content-models.md)

### Information Architecture

Define site structure:

```
/content/aem-bmad-showcase/
├── us/
│   └── en/
│       ├── home
│       ├── products/
│       │   ├── product-a
│       │   └── product-b
│       ├── about/
│       └── contact
├── fr/
│   └── fr/
│       └── [mirrored structure via MSM]
└── de/
    └── de/
        └── [mirrored structure via MSM]
```

Reference: [information-architecture.md](../02-Model-Definition/information-architecture.md)

### Design System Integration

Document design tokens for developers:

```markdown
## Design Tokens

### Colors
| Token | Value | Usage |
|-------|-------|-------|
| --color-primary | #0066CC | Primary actions, links |
| --color-secondary | #333333 | Body text |
| --color-accent | #FF6600 | Highlights, CTAs |

### Typography
| Token | Value | Usage |
|-------|-------|-------|
| --font-heading | 'Roboto', sans-serif | Headings |
| --font-body | 'Open Sans', sans-serif | Body text |

### Spacing
| Token | Value |
|-------|-------|
| --spacing-xs | 4px |
| --spacing-sm | 8px |
| --spacing-md | 16px |
| --spacing-lg | 24px |
| --spacing-xl | 48px |
```

Reference: [design-system.md](../02-Model-Definition/design-system.md)

## Phase 03: Architecture Design

### System Architecture

Document the high-level architecture in `system-architecture.md`:

```markdown
## AEM as a Cloud Service Architecture

### Service Tiers

┌─────────────────────────────────────────────────────────────┐
│                     CDN (Fastly)                            │
│  - SSL termination                                          │
│  - Edge caching                                             │
│  - WAF protection                                           │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Dispatcher                               │
│  - Request filtering                                        │
│  - Caching layer                                            │
│  - Load balancing                                           │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│              AEM Publish Service (Auto-scaled)              │
│  - Content delivery                                         │
│  - Personalization                                          │
│  - Edge delivery                                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              AEM Author Service                             │
│  - Content authoring                                        │
│  - Workflow management                                      │
│  - Asset management                                         │
└─────────────────────────────────────────────────────────────┘
```

### Integration Architecture

```markdown
## External Integrations

### Adobe Analytics
- **Pattern**: Client-side (Adobe Launch)
- **Data Layer**: ACDL (Adobe Client Data Layer)
- **Events**: Page views, component interactions, conversions

### Adobe Target
- **Pattern**: Server-side + Client-side hybrid
- **Delivery**: at.js 2.x
- **Personalization**: Experience Fragments as offers

### Translation Service
- **Pattern**: AEM Translation Framework
- **Connector**: Custom OSGi service
- **Workflow**: Human translation with AI pre-translation
```

Reference: [system-architecture.md](../03-Architecture-Design/system-architecture.md)

### Component Design

Create detailed technical specifications:

```markdown
## Hero Component - Technical Design

### Sling Model

```java
@Model(adaptables = SlingHttpServletRequest.class,
       adapters = Hero.class,
       defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class HeroImpl implements Hero {

    @ValueMapValue
    private String headline;

    @ValueMapValue
    private String subheadline;

    @ValueMapValue
    @Required
    private String backgroundImage;

    @ValueMapValue
    private String ctaText;

    @ValueMapValue
    private String ctaLink;

    // Getters...
}
```

### HTL Template Structure

```html
<section class="cmp-hero"
         data-cmp-is="hero"
         aria-label="${hero.headline}">
    <div class="cmp-hero__background"
         style="background-image: url('${hero.backgroundImage}')">
    </div>
    <div class="cmp-hero__content">
        <h1 class="cmp-hero__headline">${hero.headline}</h1>
        <p class="cmp-hero__subheadline">${hero.subheadline}</p>
        <sly data-sly-test="${hero.ctaLink}">
            <a href="${hero.ctaLink}" class="cmp-hero__cta">
                ${hero.ctaText}
            </a>
        </sly>
    </div>
</section>
```

### Dialog Definition

| Tab | Field | Granite UI Component |
|-----|-------|---------------------|
| Properties | Headline | textfield |
| Properties | Subheadline | textarea |
| Properties | Background Image | pathfield (dam) |
| CTA | CTA Text | textfield |
| CTA | CTA Link | pathfield |
| CTA | Open in New Tab | checkbox |
```

Reference: [component-design.md](../03-Architecture-Design/component-design.md)

### Dispatcher Configuration

Define caching and security rules:

```markdown
## Dispatcher Rules

### Caching Strategy

| Content Type | Cache Duration | Invalidation |
|--------------|----------------|--------------|
| HTML pages | 5 minutes | On publish |
| Client libs | 1 year | Versioned URLs |
| Images | 1 week | Content hash |
| JSON APIs | No cache | - |

### Security Filters

```apache
# Block sensitive paths
/0001 { /type "deny" /path "/etc/*" }
/0002 { /type "deny" /path "/libs/*" }
/0003 { /type "deny" /selectors '(feed|hierarchical|hierarchymerge)' }

# Allow content paths
/0100 { /type "allow" /path "/content/aem-bmad-showcase/*" }
```
```

Reference: [dispatcher-rules.md](../03-Architecture-Design/dispatcher-rules.md)

## Working with AI Agents

### GasTown Integration

As an Architect, you provide specifications that AI agents follow:

```
Your Component Design (BMAD)
    ↓
Mayor AI assigns to specialized agents (GasTown)
    ↓
AEM Component Coder implements Sling Model, HTL, CSS
AEM Test Writer creates JUnit tests
AEM Code Reviewer validates against your specs
    ↓
You review and approve technical implementation
```

### Specification Quality Checklist

For AI agents to be effective, your specifications must be:

- [ ] **Complete**: All fields, types, and constraints defined
- [ ] **Unambiguous**: Clear naming conventions and patterns
- [ ] **Testable**: Acceptance criteria that can be verified
- [ ] ** Referenced**: Links to design system, requirements
- [ ] **Consistent**: Follows established project patterns

## Architecture Review Checklist

### Before Development

- [ ] Content models support all user stories
- [ ] Component specifications are complete
- [ ] Integration patterns documented
- [ ] Dispatcher rules defined
- [ ] Performance requirements addressed
- [ ] Security considerations documented
- [ ] Accessibility requirements in specs

### During Development

- [ ] Code follows architecture patterns
- [ ] No anti-patterns introduced
- [ ] Performance benchmarks met
- [ ] Security scan passed

## Key Artifacts You Own

| Artifact | Location | Purpose |
|----------|----------|---------|
| Content Models | `02-Model-Definition/content-models.md` | Template/component definitions |
| Information Architecture | `02-Model-Definition/information-architecture.md` | Site structure |
| System Architecture | `03-Architecture-Design/system-architecture.md` | High-level design |
| Component Design | `03-Architecture-Design/component-design.md` | Technical specs |
| Dispatcher Rules | `03-Architecture-Design/dispatcher-rules.md` | CDN/caching config |
| Integration Patterns | `06-Integrations/` | External system integration |

## Best Practices

1. **Design for Authoring**: Make components intuitive for content authors
2. **Performance First**: Consider caching and optimization from the start
3. **Extend Core Components**: Build on AEM Core Components where possible
4. **Document Decisions**: Record architectural decisions and rationale
5. **Review Continuously**: Architecture evolves; update docs as needed

## Next Steps

1. Review the [System Architecture](../03-Architecture-Design/system-architecture.md) template
2. Study the [Component Design](../03-Architecture-Design/component-design.md) patterns
3. Understand [Dispatcher Rules](../03-Architecture-Design/dispatcher-rules.md)
4. Review [Integration Patterns](../06-Integrations/README.md)
5. Familiarize yourself with AEM as a Cloud Service best practices
