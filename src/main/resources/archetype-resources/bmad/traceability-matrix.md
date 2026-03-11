# Requirements Traceability Matrix

This document provides end-to-end traceability from business requirements through implementation and testing. It ensures that every requirement can be traced to its specification, implementation, and validation.

## Overview

```
Business Requirement → User Story → Technical Spec → Implementation → Test
       (REQ)              (US)         (SPEC)           (CODE)        (TEST)
```

---

## Hero Component Traceability

| Artifact Type | ID | Description | Traces To |
|--------------|-----|-------------|-----------|
| **Requirement** | REQ-COMP-001 | Library of reusable components (hero, carousel, text with image) | US-CA-003 |
| **User Story** | US-CA-003 | Add Hero Component to showcase key messaging | SPEC-HERO-001 |
| **Specification** | SPEC-HERO-001 | Hero Component Design | CODE-HERO-* |
| **Code - Model** | CODE-HERO-001 | `HeroModel.java` - Sling Model | TEST-HERO-001 |
| **Code - HTL** | CODE-HERO-002 | `hero.html` - HTL Template | TEST-HERO-002 |
| **Code - Dialog** | CODE-HERO-003 | `_cq_dialog/.content.xml` | TEST-HERO-003 |
| **Test - Unit** | TEST-HERO-001 | `HeroModelTest.java` | US-CA-003 AC |
| **Test - UI** | TEST-HERO-002 | Hero rendering UI test | US-CA-003 AC |
| **Test - Author** | TEST-HERO-003 | Dialog authoring test | US-CA-003 AC |

### Detailed Mappings

#### User Story → Acceptance Criteria → Implementation

| AC ID | Acceptance Criteria | Implementation | Test |
|-------|--------------------|--------------------|------|
| US-CA-003.AC1 | Dialog shows heading, subheading, background image, CTA fields | `_cq_dialog/.content.xml` | Manual dialog test |
| US-CA-003.AC2 | All text fields support i18n | `HeroModel.java` (String types) | `HeroModelTest.shouldSupportUnicodeInHeading()` |
| US-CA-003.AC3 | Background image selectable from DAM | Dialog pathfield config | Manual DAM picker test |
| US-CA-003.AC4 | CTA link supports internal/external URLs | `HeroModel.getCtaButtonLink()` | `HeroModelTest.shouldSupportExternalUrls()` |
| US-CA-003.AC5 | WCAG 2.1 AA compliance | `hero.html` ARIA attributes | Axe accessibility scan |
| US-CA-003.AC6 | Responsive across breakpoints | CSS media queries | Visual regression test |

---

## Text with Image Component Traceability

| Artifact Type | ID | Description | Traces To |
|--------------|-----|-------------|-----------|
| **Requirement** | REQ-COMP-001 | Library of reusable components | US-CA-004 |
| **User Story** | US-CA-004 | Add Text with Image Component | SPEC-TWI-001 |
| **Specification** | SPEC-TWI-001 | Text with Image Design | CODE-TWI-* |
| **Code - Model** | CODE-TWI-001 | `TextWithImageModel.java` | TEST-TWI-001 |
| **Code - HTL** | CODE-TWI-002 | `textwithimage.html` | TEST-TWI-002 |
| **Test - Unit** | TEST-TWI-001 | `TextWithImageModelTest.java` | US-CA-004 AC |

### Detailed Mappings

| AC ID | Acceptance Criteria | Implementation | Test |
|-------|--------------------|--------------------|------|
| US-CA-004.AC1 | Dialog for heading, body, image, position | `_cq_dialog/.content.xml` | Manual dialog test |
| US-CA-004.AC2 | Image position Left/Right options | `imagePosition` field | `TextWithImageModelTest` |
| US-CA-004.AC3 | RTE supports formatting | Dialog RTE config | Manual RTE test |
| US-CA-004.AC4 | Layout positions image correctly | `textwithimage.html` + CSS | Visual test |
| US-CA-004.AC5 | Image alt text for a11y | HTL img alt attribute | Axe scan |
| US-CA-004.AC6 | Responsive with mobile stacking | CSS media queries | Visual regression |

---

## Elite AEM Forms Traceability (Omnichannel & Headless)

| Artifact Type | ID | Description | Traces To |
|--------------|-----|-------------|-----------|
| **Requirement** | REQ-FORMS-001 | Omnichannel Headless Form Delivery & Status | US-FORMS-001 |
| **Requirement** | REQ-FORMS-002 | Asynchronous E-Sign & DoR Workflow | US-FORMS-002 |
| **Requirement** | REQ-FORMS-003 | Enterprise Hardening (DRM, FDM, AFCS) | US-FORMS-003 |
| **User Story** | US-FORMS-001 | As a developer, I want to deliver forms headlessly via React | SPEC-FORMS-001 |
| **User Story** | US-FORMS-002 | As a user, I want to sign my application and receive a DoR | SPEC-FORMS-002 |
| **User Story** | US-FORMS-003 | As an architect, I want a hardened enterprise form foundation | SPEC-FORMS-003 |
| **Specification** | SPEC-FORMS-001 | `headless-forms.md` | CODE-FORMS-BFF-* |
| **Specification** | SPEC-FORMS-002 | `omnichannel-architecture.md` | CODE-FORMS-WF-* |
| **Specification** | SPEC-FORMS-003 | `enterprise-hardening-guide.md` | CODE-FORMS-SEC-* |
| **Code - BFF** | CODE-FORMS-001 | `HeadlessFormService.java` | TEST-FORMS-001 |
| **Code - React** | CODE-FORMS-002 | `App.js` (Headless React Consumer) | TEST-FORMS-002 |
| **Code - Workflow**| CODE-FORMS-003 | `SignToDoRProcess.java` | TEST-FORMS-003 |
| **Test - E2E** | TEST-FORMS-002 | `omnichannel-flow.cy.js` (Cypress) | US-FORMS-001 AC |
| **Test - Unit** | TEST-FORMS-003 | `FinancialApplicationModelTest.java` | US-FORMS-002 AC |

### Detailed Mappings (Elite Forms)

| AC ID | Acceptance Criteria | Implementation | Test |
|-------|--------------------|--------------------|------|
| US-FORMS-001.AC1 | Form model fetched via BFF endpoint | `HeadlessFormService.java` | Cypress Intercept Test |
| US-FORMS-001.AC2 | React app renders AF components | `@aemforms/af-react-renderer` | `App.js` snapshot |
| US-FORMS-002.AC1 | Submission triggers Sign workflow | `HeadlessSubmitServlet.java` | Workflow instance check |
| US-FORMS-002.AC2 | Live status polling in UI | `useEffect` + `/bin/bmad/headless-status` | Cypress status loop |
| US-FORMS-003.AC1 | Unified Design Tokens applied | `variables.css` + `App.css` | Visual regression |
| US-FORMS-003.AC2 | DRM Security Policy applied to PDF | `enterprise-hardening-guide.md` | Manual PDF audit |

---

## File Location Reference

### Specifications (SPEC)
| ID | File Path |
|----|-----------|
| SPEC-HERO-001 | `bmad/03-Architecture-Design/component-design.md#hero-component` |
| SPEC-TWI-001 | `bmad/03-Architecture-Design/component-design.md#text-with-image-component` |
| SPEC-CONTENT-001 | `bmad/02-Model-Definition/content-models.md` |
| SPEC-DESIGN-001 | `bmad/02-Model-Definition/design-system.md` |

### Implementation (CODE)
| ID | File Path |
|----|-----------|
| CODE-HERO-001 | `core/src/main/java/com/example/aem/bmad/core/models/HeroModel.java` |
| CODE-HERO-002 | `ui.apps/src/main/content/jcr_root/apps/aem-bmad-showcase/components/content/hero/hero.html` |
| CODE-TWI-001 | `core/src/main/java/com/example/aem/bmad/core/models/TextWithImageModel.java` |
| CODE-TWI-002 | `ui.apps/src/main/content/jcr_root/apps/aem-bmad-showcase/components/content/textwithimage/textwithimage.html` |

### Tests (TEST)
| ID | File Path |
|----|-----------|
| TEST-HERO-001 | `core/src/test/java/com/example/aem/bmad/core/models/HeroModelTest.java` |
| TEST-TWI-001 | `core/src/test/java/com/example/aem/bmad/core/models/TextWithImageModelTest.java` |

---

## Cross-Functional Requirements Traceability

### Accessibility (WCAG 2.1 AA)

| Requirement | User Stories | Implementation | Validation |
|-------------|--------------|----------------|------------|
| REQ-A11Y-001: Keyboard navigation | US-CA-003.AC5, US-CA-004.AC5 | ARIA attributes, tabindex | Axe-core CI scan |
| REQ-A11Y-002: Screen reader support | US-CA-003.AC5 | Semantic HTML, aria-labels | Manual NVDA/VoiceOver test |
| REQ-A11Y-003: Color contrast | All UI components | Design tokens (4.5:1 ratio) | Lighthouse audit |

### Internationalization (i18n)

| Requirement | User Stories | Implementation | Validation |
|-------------|--------------|----------------|------------|
| REQ-I18N-001: Externalized strings | US-CA-003.AC2, US-CA-004.AC2 | AEM i18n framework | `HeroModelTest` unicode tests |
| REQ-I18N-002: Multi-language support | All content components | Language copies, MSM | Manual translation workflow |

### Performance

| Requirement | User Stories | Implementation | Validation |
|-------------|--------------|----------------|------------|
| REQ-PERF-001: LCP < 2.5s | US-WV-002.AC1 | Image optimization, lazy load | Lighthouse CI |
| REQ-PERF-002: FID < 100ms | US-WV-002.AC2 | Code splitting, async JS | Web Vitals monitoring |

---

## Sprint Traceability

### Sprint 1

| User Story | Tasks | Status | Code Artifacts | Test Artifacts |
|------------|-------|--------|----------------|----------------|
| US-CA-003 | Hero Component | Complete | `HeroModel.java`, `hero.html` | `HeroModelTest.java` |
| US-CA-004 | Text with Image | Pending | - | - |

---

## How to Use This Matrix

1. **Forward Tracing** (Requirements → Implementation):
   - Start with a business requirement (REQ-*)
   - Find related user stories (US-*)
   - Locate technical specifications (SPEC-*)
   - Find implementation files (CODE-*)
   - Identify test coverage (TEST-*)

2. **Backward Tracing** (Implementation → Requirements):
   - Start with a code file
   - Find its ID in the CODE table
   - Trace back through SPEC to US to REQ

3. **Impact Analysis**:
   - When a requirement changes, use forward tracing to identify all affected code and tests
   - When code changes, use backward tracing to validate requirement coverage

4. **Coverage Gaps**:
   - Any USER STORY without corresponding CODE or TEST indicates incomplete implementation
   - Review during sprint planning and retrospectives
