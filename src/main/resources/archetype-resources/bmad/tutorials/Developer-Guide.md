# Developer Guide: Building AEM Components with BMAD

This guide helps Developers effectively use the BMAD (Breakthrough Method for Agile Development) framework to implement AEM as a Cloud Service components and features.

## Your Role in BMAD

As a Developer, you act as the **Developer Agent** in the BMAD framework. Your responsibilities include:

- Implementing AEM components following architectural specifications
- Writing Sling Models, HTL templates, and CSS
- Creating unit and integration tests
- Following coding standards and best practices
- Ensuring accessibility and internationalization
- Participating in code reviews

## Getting Started

### Step 1: Understand the Project Structure

```
aem-bmad-showcase/
├── core/                          # Java backend (Sling Models, Services)
│   └── src/main/java/
├── ui.apps/                       # AEM components, templates, configs
│   └── src/main/content/jcr_root/
│       └── apps/aem-bmad-showcase/
│           └── components/
├── ui.frontend/                   # Frontend assets (CSS, JS)
│   └── src/main/webpack/
├── dispatcher/                    # Dispatcher configuration
└── bmad/                          # BMAD documentation
    ├── 03-Architecture-Design/    # Your specifications
    │   └── component-design.md
    ├── 04-Development-Sprint/     # Sprint planning & guidelines
    │   ├── sprint-1-plan.md
    │   └── development-guidelines.md
    └── 02-Model-Definition/
        └── design-system.md       # Design tokens
```

### Step 2: Review Before Coding

Before implementing any component, review:

1. **Component Specification**: [component-design.md](../03-Architecture-Design/component-design.md)
2. **Design System**: [design-system.md](../02-Model-Definition/design-system.md)
3. **Development Guidelines**: [development-guidelines.md](../04-Development-Sprint/development-guidelines.md)
4. **User Story & Acceptance Criteria**: [user-stories.md](../01-Business-Discovery/user-stories.md)

## Development Workflow

### Step 1: Local Environment Setup

```bash
# 1. Start AEM SDK Author instance
java -jar aem-sdk-quickstart-*.jar

# 2. Build and deploy to local AEM
mvn clean install -PautoInstallPackage

# 3. For frontend-only changes
cd ui.frontend
npm run dev
```

### Step 2: Component Implementation Checklist

For each component, implement in this order:

- [ ] **Sling Model** - Backend logic and data binding
- [ ] **HTL Template** - Markup structure
- [ ] **Dialog** - Author configuration UI
- [ ] **CSS/SCSS** - Styling with design tokens
- [ ] **JavaScript** - Client-side behavior (if needed)
- [ ] **Unit Tests** - JUnit for Sling Model
- [ ] **i18n** - Externalize all text

## Implementing Components

### Sling Model (Java)

Location: `core/src/main/java/com/example/aem/bmad/core/models/`

```java
package com.example.aem.bmad.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.Required;

import javax.annotation.PostConstruct;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Hero.class,
    resourceType = HeroImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HeroImpl implements Hero {

    protected static final String RESOURCE_TYPE = "aem-bmad-showcase/components/hero";

    @ValueMapValue
    @Required
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

    @ValueMapValue
    private boolean openInNewTab;

    private String ctaTarget;

    @PostConstruct
    protected void init() {
        ctaTarget = openInNewTab ? "_blank" : "_self";
    }

    @Override
    public String getHeadline() {
        return headline;
    }

    @Override
    public String getSubheadline() {
        return subheadline;
    }

    @Override
    public String getBackgroundImage() {
        return backgroundImage;
    }

    @Override
    public String getCtaText() {
        return ctaText;
    }

    @Override
    public String getCtaLink() {
        return ctaLink;
    }

    @Override
    public String getCtaTarget() {
        return ctaTarget;
    }

    @Override
    public boolean isEmpty() {
        return headline == null || headline.trim().isEmpty();
    }
}
```

### Interface

```java
package com.example.aem.bmad.core.models;

import com.adobe.cq.wcm.core.components.models.Component;

public interface Hero extends Component {
    String getHeadline();
    String getSubheadline();
    String getBackgroundImage();
    String getCtaText();
    String getCtaLink();
    String getCtaTarget();
}
```

### HTL Template

Location: `ui.apps/src/main/content/jcr_root/apps/aem-bmad-showcase/components/hero/hero.html`

```html
<sly data-sly-use.hero="com.example.aem.bmad.core.models.Hero"
     data-sly-use.templates="core/wcm/components/commons/v1/templates.html">

    <sly data-sly-test="${hero.empty}">
        <sly data-sly-call="${templates.placeholder @ isEmpty=true}"/>
    </sly>

    <sly data-sly-test="${!hero.empty}">
        <section class="cmp-hero"
                 data-cmp-is="hero"
                 aria-label="${hero.headline @ context='attribute'}">

            <div class="cmp-hero__background"
                 style="background-image: url('${hero.backgroundImage @ context='uri'}');"
                 role="img"
                 aria-hidden="true">
            </div>

            <div class="cmp-hero__content">
                <h1 class="cmp-hero__headline">${hero.headline @ context='html'}</h1>

                <sly data-sly-test="${hero.subheadline}">
                    <p class="cmp-hero__subheadline">${hero.subheadline @ context='html'}</p>
                </sly>

                <sly data-sly-test="${hero.ctaLink && hero.ctaText}">
                    <a href="${hero.ctaLink @ context='uri'}"
                       class="cmp-hero__cta"
                       target="${hero.ctaTarget}"
                       data-sly-attribute.rel="${hero.ctaTarget == '_blank' ? 'noopener noreferrer' : ''}">
                        ${hero.ctaText @ context='html'}
                    </a>
                </sly>
            </div>
        </section>
    </sly>
</sly>
```

### Component Dialog

Location: `ui.apps/src/main/content/jcr_root/apps/aem-bmad-showcase/components/hero/_cq_dialog/.content.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:granite="http://www.adobe.com/jcr/granite/1.0"
          xmlns:cq="http://www.day.com/jcr/cq/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="nt:unstructured"
    jcr:title="Hero"
    sling:resourceType="cq/gui/components/authoring/dialog">
    <content
        jcr:primaryType="nt:unstructured"
        sling:resourceType="granite/ui/components/coral/foundation/container">
        <items jcr:primaryType="nt:unstructured">
            <tabs
                jcr:primaryType="nt:unstructured"
                sling:resourceType="granite/ui/components/coral/foundation/tabs"
                maximized="{Boolean}true">
                <items jcr:primaryType="nt:unstructured">
                    <!-- Properties Tab -->
                    <properties
                        jcr:primaryType="nt:unstructured"
                        jcr:title="Properties"
                        sling:resourceType="granite/ui/components/coral/foundation/container"
                        margin="{Boolean}true">
                        <items jcr:primaryType="nt:unstructured">
                            <columns
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="granite/ui/components/coral/foundation/fixedcolumns"
                                margin="{Boolean}true">
                                <items jcr:primaryType="nt:unstructured">
                                    <column
                                        jcr:primaryType="nt:unstructured"
                                        sling:resourceType="granite/ui/components/coral/foundation/container">
                                        <items jcr:primaryType="nt:unstructured">
                                            <headline
                                                jcr:primaryType="nt:unstructured"
                                                sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                                                fieldLabel="Headline"
                                                fieldDescription="Main headline text (max 80 characters)"
                                                name="./headline"
                                                maxlength="80"
                                                required="{Boolean}true"/>
                                            <subheadline
                                                jcr:primaryType="nt:unstructured"
                                                sling:resourceType="granite/ui/components/coral/foundation/form/textarea"
                                                fieldLabel="Subheadline"
                                                fieldDescription="Supporting text (max 200 characters)"
                                                name="./subheadline"
                                                maxlength="200"/>
                                            <backgroundImage
                                                jcr:primaryType="nt:unstructured"
                                                sling:resourceType="granite/ui/components/coral/foundation/form/pathfield"
                                                fieldLabel="Background Image"
                                                fieldDescription="Select hero background image"
                                                name="./backgroundImage"
                                                rootPath="/content/dam"
                                                required="{Boolean}true"/>
                                        </items>
                                    </column>
                                </items>
                            </columns>
                        </items>
                    </properties>
                    <!-- CTA Tab -->
                    <cta
                        jcr:primaryType="nt:unstructured"
                        jcr:title="Call to Action"
                        sling:resourceType="granite/ui/components/coral/foundation/container"
                        margin="{Boolean}true">
                        <items jcr:primaryType="nt:unstructured">
                            <columns
                                jcr:primaryType="nt:unstructured"
                                sling:resourceType="granite/ui/components/coral/foundation/fixedcolumns"
                                margin="{Boolean}true">
                                <items jcr:primaryType="nt:unstructured">
                                    <column
                                        jcr:primaryType="nt:unstructured"
                                        sling:resourceType="granite/ui/components/coral/foundation/container">
                                        <items jcr:primaryType="nt:unstructured">
                                            <ctaText
                                                jcr:primaryType="nt:unstructured"
                                                sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
                                                fieldLabel="CTA Text"
                                                fieldDescription="Button label"
                                                name="./ctaText"/>
                                            <ctaLink
                                                jcr:primaryType="nt:unstructured"
                                                sling:resourceType="granite/ui/components/coral/foundation/form/pathfield"
                                                fieldLabel="CTA Link"
                                                fieldDescription="Button destination URL"
                                                name="./ctaLink"/>
                                            <openInNewTab
                                                jcr:primaryType="nt:unstructured"
                                                sling:resourceType="granite/ui/components/coral/foundation/form/checkbox"
                                                fieldDescription="Open link in new tab"
                                                name="./openInNewTab"
                                                text="Open in new tab"
                                                uncheckedValue="false"
                                                value="{Boolean}true"/>
                                        </items>
                                    </column>
                                </items>
                            </columns>
                        </items>
                    </cta>
                </items>
            </tabs>
        </items>
    </content>
</jcr:root>
```

### CSS/SCSS

Location: `ui.frontend/src/main/webpack/components/hero/hero.scss`

```scss
// Use design system tokens
@import '../../site/variables';

.cmp-hero {
  position: relative;
  min-height: 500px;
  display: flex;
  align-items: center;
  overflow: hidden;

  &__background {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-size: cover;
    background-position: center;
    z-index: 1;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: linear-gradient(
        to right,
        rgba(0, 0, 0, 0.7) 0%,
        rgba(0, 0, 0, 0.3) 100%
      );
    }
  }

  &__content {
    position: relative;
    z-index: 2;
    max-width: 600px;
    padding: var(--spacing-xl);
    color: #ffffff;
  }

  &__headline {
    font-family: var(--font-heading);
    font-size: 3rem;
    font-weight: 700;
    line-height: 1.2;
    margin: 0 0 var(--spacing-md);

    @media (max-width: 768px) {
      font-size: 2rem;
    }
  }

  &__subheadline {
    font-family: var(--font-body);
    font-size: 1.25rem;
    line-height: 1.6;
    margin: 0 0 var(--spacing-lg);
    opacity: 0.9;

    @media (max-width: 768px) {
      font-size: 1rem;
    }
  }

  &__cta {
    display: inline-block;
    padding: var(--spacing-sm) var(--spacing-lg);
    background-color: var(--color-accent);
    color: #ffffff;
    font-family: var(--font-body);
    font-weight: 600;
    text-decoration: none;
    border-radius: 4px;
    transition: background-color 0.3s ease, transform 0.2s ease;

    &:hover,
    &:focus {
      background-color: darken(#ff6600, 10%);
      transform: translateY(-2px);
    }

    &:focus {
      outline: 2px solid #ffffff;
      outline-offset: 2px;
    }
  }
}
```

### Unit Test (JUnit 5)

Location: `core/src/test/java/com/example/aem/bmad/core/models/HeroImplTest.java`

```java
package com.example.aem.bmad.core.models;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class HeroImplTest {

    private final AemContext context = new AemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(HeroImpl.class);
    }

    @Test
    void testHeroWithAllProperties() {
        context.build().resource("/content/hero",
            "sling:resourceType", "aem-bmad-showcase/components/hero",
            "headline", "Welcome to BMAD",
            "subheadline", "AI-driven development",
            "backgroundImage", "/content/dam/hero.jpg",
            "ctaText", "Learn More",
            "ctaLink", "/content/about",
            "openInNewTab", false
        ).commit();

        context.currentResource("/content/hero");
        Hero hero = context.request().adaptTo(Hero.class);

        assertNotNull(hero);
        assertEquals("Welcome to BMAD", hero.getHeadline());
        assertEquals("AI-driven development", hero.getSubheadline());
        assertEquals("/content/dam/hero.jpg", hero.getBackgroundImage());
        assertEquals("Learn More", hero.getCtaText());
        assertEquals("/content/about", hero.getCtaLink());
        assertEquals("_self", hero.getCtaTarget());
        assertFalse(hero.isEmpty());
    }

    @Test
    void testHeroWithNewTabEnabled() {
        context.build().resource("/content/hero",
            "sling:resourceType", "aem-bmad-showcase/components/hero",
            "headline", "Test",
            "backgroundImage", "/content/dam/hero.jpg",
            "openInNewTab", true
        ).commit();

        context.currentResource("/content/hero");
        Hero hero = context.request().adaptTo(Hero.class);

        assertEquals("_blank", hero.getCtaTarget());
    }

    @Test
    void testEmptyHero() {
        context.build().resource("/content/hero",
            "sling:resourceType", "aem-bmad-showcase/components/hero"
        ).commit();

        context.currentResource("/content/hero");
        Hero hero = context.request().adaptTo(Hero.class);

        assertTrue(hero.isEmpty());
    }
}
```

## Internationalization (i18n)

### Externalizing Text

Use AEM's i18n framework for all user-facing text:

```html
<!-- In HTL -->
<sly data-sly-use.i18n="com.day.cq.i18n.I18n">
    <button>${'Read More' @ i18n}</button>
</sly>
```

### Dictionary Location

`ui.apps/src/main/content/jcr_root/apps/aem-bmad-showcase/i18n/`

```
en.json:
{
  "Read More": "Read More",
  "Learn More": "Learn More"
}

fr.json:
{
  "Read More": "En savoir plus",
  "Learn More": "En savoir plus"
}
```

## Accessibility Checklist

For every component, verify:

- [ ] **Semantic HTML**: Using correct elements (`<section>`, `<nav>`, `<button>`)
- [ ] **Keyboard Navigation**: All interactive elements focusable
- [ ] **Focus Indicators**: Visible focus states
- [ ] **ARIA Labels**: Descriptive labels for screen readers
- [ ] **Color Contrast**: Meets WCAG 2.1 AA (4.5:1 for text)
- [ ] **Alt Text**: Images have descriptive alternatives
- [ ] **Skip Links**: Allow bypassing repetitive content

## Working with AI Agents (BEAD)

When AI agents assist with development:

### Your BEAD Tasks Might Look Like

```
BEAD Issue: HERO-001
Title: Implement Hero Sling Model
Status: In Progress
Dependencies: None
Context:
  - Component spec: bmad/03-Architecture-Design/component-design.md#hero
  - Design tokens: bmad/02-Model-Definition/design-system.md
Acceptance:
  - [ ] All fields from spec implemented
  - [ ] @PostConstruct for derived fields
  - [ ] isEmpty() returns true when headline is empty
```

### AI Agent Workflow

```
Architect's Spec
    ↓
BEAD breaks into tasks:
├── HERO-001: Sling Model
├── HERO-002: HTL Template
├── HERO-003: Dialog XML
├── HERO-004: SCSS Styling
└── HERO-005: JUnit Tests
    ↓
You implement each task
    ↓
AI Code Reviewer validates
```

## Build and Deploy

### Local Development

```bash
# Full build and deploy
mvn clean install -PautoInstallPackage

# Deploy only core bundle
mvn clean install -PautoInstallBundle -pl core

# Frontend only (with watch)
cd ui.frontend && npm run dev
```

### Cloud Manager Deployment

Follows CI/CD pipeline defined in [deployment-plan.md](../05-Testing-and-Deployment/deployment-plan.md)

## Code Review Checklist

Before submitting for review:

- [ ] Code follows [development-guidelines.md](../04-Development-Sprint/development-guidelines.md)
- [ ] Unit tests pass with >80% coverage
- [ ] No hardcoded strings (i18n applied)
- [ ] Accessibility requirements met
- [ ] Design system tokens used (no hardcoded colors/fonts)
- [ ] Component works in AEM editor
- [ ] Component renders correctly on all viewports

## Key Resources

| Resource | Location |
|----------|----------|
| Component Specs | `bmad/03-Architecture-Design/component-design.md` |
| Design System | `bmad/02-Model-Definition/design-system.md` |
| Dev Guidelines | `bmad/04-Development-Sprint/development-guidelines.md` |
| Sprint Plan | `bmad/04-Development-Sprint/sprint-1-plan.md` |
| Testing Strategy | `bmad/05-Testing-and-Deployment/testing-strategy.md` |

## Best Practices

1. **Extend Core Components**: Use AEM Core Components as base where possible
2. **Test First**: Write tests before or alongside implementation
3. **Accessibility Always**: Build accessibility in, don't bolt it on
4. **Design Tokens**: Never hardcode colors, fonts, or spacing
5. **Clean Code**: Follow SOLID principles, keep methods small
6. **Document**: Add JSDoc/JavaDoc for public APIs

## Next Steps

1. Set up your [local development environment](../04-Development-Sprint/development-guidelines.md#local-development-environment)
2. Review the [Sprint 1 Plan](../04-Development-Sprint/sprint-1-plan.md)
3. Study the [Component Design](../03-Architecture-Design/component-design.md) specs
4. Familiarize yourself with [AEM Core Components](https://experienceleague.adobe.com/docs/experience-manager-core-components/using/introduction.html)
