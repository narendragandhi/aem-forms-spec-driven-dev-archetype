# QA Engineer Guide

## Overview

This guide provides comprehensive onboarding and workflow documentation for QA Engineers working on the AEM BMAD Showcase project. It covers AEM-specific testing workflows, test case management, bug reporting, regression testing, UAT coordination, and QA tools.

---

## Table of Contents

1. [QA Role Overview](#1-qa-role-overview)
2. [AEM Environment Setup](#2-aem-environment-setup)
3. [Test Case Management](#3-test-case-management)
4. [Testing Workflows](#4-testing-workflows)
5. [Bug Reporting](#5-bug-reporting)
6. [Regression Testing](#6-regression-testing)
7. [UAT Coordination](#7-uat-coordination)
8. [QA Tools and Resources](#8-qa-tools-and-resources)

---

## 1. QA Role Overview

### 1.1 QA Responsibilities

| Responsibility | Description | Frequency |
|---------------|-------------|-----------|
| **Test Planning** | Create test plans for features/sprints | Per sprint |
| **Test Execution** | Execute manual and automated tests | Daily |
| **Bug Reporting** | Document and track defects | As found |
| **Regression Testing** | Verify existing functionality | Pre-release |
| **UAT Support** | Coordinate business testing | Per release |
| **Quality Metrics** | Track and report quality KPIs | Weekly |
| **Process Improvement** | Identify testing improvements | Ongoing |

### 1.2 QA in BMAD Methodology

```
BMAD Phase Alignment:

Phase 04: Development Sprint
├── Sprint QA Planning
├── Story Testing
├── Daily Smoke Tests
└── Sprint Demo Support

Phase 05: Testing & Deployment
├── Integration Testing
├── Regression Testing
├── Performance Testing
├── Security Testing
├── UAT Coordination
└── Go-Live Support
```

### 1.3 QA Skills Matrix

| Skill | Required Level | Learning Path |
|-------|---------------|---------------|
| AEM Authoring | Expert | Internal training |
| AEM Touch UI | Expert | Adobe tutorials |
| Manual Testing | Expert | Experience |
| Automation (Selenium/Playwright) | Intermediate | Team mentorship |
| API Testing | Intermediate | Postman training |
| Performance Testing | Basic | JMeter basics |
| Accessibility Testing | Intermediate | WCAG training |

---

## 2. AEM Environment Setup

### 2.1 Environment Access

| Environment | Purpose | URL | Access |
|-------------|---------|-----|--------|
| **Local** | Dev testing | localhost:4502 | Request from DevOps |
| **Development** | Integration | dev.example.com | VPN + SSO |
| **Stage** | UAT/Performance | stage.example.com | VPN + SSO |
| **Production** | Verification only | www.example.com | Read-only |

### 2.2 Local AEM Setup

```bash
# 1. Prerequisites
# - Java 11+ installed
# - 8GB RAM minimum
# - Download AEM SDK from Adobe

# 2. Start AEM Author
java -jar aem-author-p4502.jar

# 3. Install Content Package
curl -u admin:admin -F file=@"content-package.zip" \
  -F install=true \
  http://localhost:4502/crx/packmgr/service.jsp

# 4. Verify Installation
curl -u admin:admin http://localhost:4502/content/bmad-showcase/en.html
```

### 2.3 Author Interface Orientation

```
AEM Author Navigation:
├── Sites Console (/sites.html)
│   ├── Content tree
│   ├── Page operations (Create, Edit, Delete)
│   └── Publishing workflows
├── Assets Console (/assets.html)
│   ├── DAM folder structure
│   ├── Asset upload/management
│   └── Image profiles
├── Tools Console (/tools.html)
│   ├── Workflows
│   ├── Replication
│   └── Cloud Services
└── CRXDE Lite (/crx/de) [Dev only]
    ├── Repository browser
    └── Node inspection
```

### 2.4 Key AEM Concepts for QA

| Concept | Description | Testing Relevance |
|---------|-------------|-------------------|
| **Components** | Reusable content blocks | Functional testing |
| **Templates** | Page structure definitions | Layout testing |
| **Workflows** | Content approval processes | Process testing |
| **Replication** | Author→Publish sync | Publishing testing |
| **Dispatcher** | Caching layer | Cache invalidation |
| **Experience Fragments** | Reusable content sections | Cross-page testing |

---

## 3. Test Case Management

### 3.1 Test Case Structure

```markdown
## Test Case Template

**Test ID:** TC-[Component]-[Number]
**Title:** [Clear description of what is being tested]
**Priority:** P1/P2/P3/P4
**Type:** Functional/Regression/Integration/E2E

### Preconditions
- [Required state before test]

### Test Steps
1. [Step 1]
2. [Step 2]
3. [Step 3]

### Expected Results
- [What should happen]

### Actual Results
- [What actually happened - fill during execution]

### Test Data
- [Required test data]

### Environment
- [Tested on which environment]

### Attachments
- [Screenshots, logs]
```

### 3.2 Test Case Categories

| Category | Description | Example |
|----------|-------------|---------|
| **Component Tests** | Individual component functionality | Hero CTA click |
| **Page Tests** | Full page behavior | Homepage load |
| **Workflow Tests** | Content processes | Publish workflow |
| **Integration Tests** | System interactions | LLM content generation |
| **Cross-Browser** | Browser compatibility | Chrome, Safari, Edge |
| **Responsive** | Device compatibility | Mobile, tablet, desktop |
| **Accessibility** | WCAG compliance | Screen reader navigation |

### 3.3 Test Case Examples

#### Hero Component Test Cases

```markdown
## TC-HERO-001: Hero Title Display
**Priority:** P1 | **Type:** Functional

### Preconditions
- Hero component authored with title

### Test Steps
1. Navigate to page with hero component
2. Observe hero section

### Expected Results
- Title displays correctly
- Title uses proper heading level (H1)
- Title is readable against background

---

## TC-HERO-002: Hero CTA Button Navigation
**Priority:** P1 | **Type:** Functional

### Preconditions
- Hero component with CTA configured

### Test Steps
1. Navigate to page with hero
2. Click CTA button
3. Observe navigation

### Expected Results
- Button is clickable
- Navigation occurs to configured URL
- Page loads correctly

---

## TC-HERO-003: Hero Responsive Behavior
**Priority:** P2 | **Type:** Responsive

### Test Steps
1. View hero on desktop (1920px)
2. View hero on tablet (768px)
3. View hero on mobile (375px)

### Expected Results
- Desktop: Full-width hero, large text
- Tablet: Adjusted proportions
- Mobile: Stacked layout, readable text

---

## TC-HERO-004: Hero Accessibility
**Priority:** P2 | **Type:** Accessibility

### Test Steps
1. Navigate to hero using screen reader
2. Check image alt text
3. Verify heading structure
4. Test keyboard navigation

### Expected Results
- All content announced by screen reader
- Image has descriptive alt text
- Proper heading hierarchy
- CTA focusable and activatable via keyboard
```

### 3.4 Test Suite Organization

```
Test Suites/
├── Smoke Tests/
│   ├── TC-SMOKE-001: Homepage loads
│   ├── TC-SMOKE-002: Navigation works
│   └── TC-SMOKE-003: Key pages accessible
├── Component Tests/
│   ├── Hero/
│   ├── Card/
│   ├── Carousel/
│   └── Navigation/
├── Integration Tests/
│   ├── LLM Integration/
│   ├── Email Integration/
│   └── Analytics Integration/
├── Regression Tests/
│   └── [All P1 functional tests]
└── Accessibility Tests/
    └── [WCAG 2.1 AA tests]
```

---

## 4. Testing Workflows

### 4.1 Sprint Testing Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                    Sprint Start                                 │
│                         │                                       │
│     ┌───────────────────┼───────────────────┐                  │
│     ▼                   ▼                   ▼                  │
│ ┌─────────┐      ┌─────────────┐     ┌──────────────┐          │
│ │ Review  │      │ Create Test │     │ Setup Test   │          │
│ │ Stories │      │ Cases       │     │ Data         │          │
│ └────┬────┘      └──────┬──────┘     └──────┬───────┘          │
│      └──────────────────┼───────────────────┘                  │
│                         │                                       │
│                         ▼                                       │
│              ┌─────────────────────┐                           │
│              │ Daily Testing       │                           │
│              │ (as stories done)   │                           │
│              └──────────┬──────────┘                           │
│                         │                                       │
│            ┌────────────┴────────────┐                         │
│            │                         │                         │
│     Found Bug?                No Bugs?                         │
│            │                         │                         │
│            ▼                         ▼                         │
│     ┌─────────────┐          ┌─────────────┐                   │
│     │ Report Bug  │          │ Mark Story  │                   │
│     │ Block Story │          │ QA Approved │                   │
│     └─────────────┘          └─────────────┘                   │
│                                                                 │
│                    Sprint End                                   │
│                         │                                       │
│              ┌──────────┴──────────┐                           │
│              │ Regression Testing  │                           │
│              │ Sprint Demo Support │                           │
│              └─────────────────────┘                           │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 Story Testing Workflow

```markdown
## Story Testing Process

### 1. Story Review (Before Dev)
- [ ] Understand acceptance criteria
- [ ] Identify test scenarios
- [ ] Create test cases
- [ ] Prepare test data

### 2. Dev Complete (Story in QA)
- [ ] Verify in dev environment
- [ ] Execute test cases
- [ ] Test edge cases
- [ ] Cross-browser testing
- [ ] Responsive testing
- [ ] Accessibility check

### 3. Bug Found
- [ ] Document reproduction steps
- [ ] Attach screenshots/videos
- [ ] Log bug in tracking system
- [ ] Move story to "In Progress"
- [ ] Notify developer

### 4. Bug Fixed
- [ ] Verify fix
- [ ] Regression test related areas
- [ ] Update test case results

### 5. QA Approved
- [ ] All test cases pass
- [ ] No open bugs
- [ ] Move story to "Done"
- [ ] Update test metrics
```

### 4.3 Release Testing Workflow

```markdown
## Release Testing Checklist

### Pre-Release (Stage Environment)
- [ ] Deploy to Stage complete
- [ ] Smoke test pass
- [ ] Full regression suite executed
- [ ] Performance test completed
- [ ] Security scan clean
- [ ] Accessibility audit pass
- [ ] UAT sign-off obtained

### Go-Live (Production)
- [ ] Deployment window scheduled
- [ ] Rollback plan ready
- [ ] Monitoring dashboards ready
- [ ] On-call contacts confirmed
- [ ] Deployment executed
- [ ] Smoke test pass
- [ ] Key journeys verified
- [ ] Stakeholders notified

### Post-Release
- [ ] Monitor error rates
- [ ] Check analytics
- [ ] Verify CDN caching
- [ ] Document any issues
- [ ] Retrospective notes
```

### 4.4 Daily QA Activities

| Time | Activity |
|------|----------|
| 9:00 AM | Check overnight automation results |
| 9:15 AM | Daily standup participation |
| 9:30 AM | Triage new bugs |
| 10:00 AM | Story testing |
| 12:00 PM | Update test metrics |
| 1:00 PM | Story testing continued |
| 3:00 PM | Cross-browser/responsive testing |
| 4:00 PM | Bug verification |
| 4:30 PM | End-of-day summary |

---

## 5. Bug Reporting

### 5.1 Bug Report Template

```markdown
## Bug Report

**Bug ID:** BUG-[Number]
**Title:** [Short, descriptive title]
**Severity:** Critical/High/Medium/Low
**Priority:** P1/P2/P3/P4
**Environment:** Dev/Stage/Prod
**Browser/Device:** [e.g., Chrome 120, iPhone 14]

### Description
[Clear description of the bug]

### Steps to Reproduce
1. Go to [URL]
2. Click on [element]
3. Observe [behavior]

### Expected Result
[What should happen]

### Actual Result
[What actually happens]

### Attachments
- Screenshot: [link]
- Video: [link]
- Console logs: [attached]

### Additional Info
- User account: [test user used]
- Test data: [specific data that triggers bug]
- Related story: [STORY-XXX]

### Developer Notes
[Space for dev to add context]
```

### 5.2 Severity Guidelines

| Severity | Definition | Examples | SLA |
|----------|------------|----------|-----|
| **Critical** | Site down, data loss, security breach | Homepage 500, payment fails | 4 hours |
| **High** | Major feature broken, no workaround | Search broken, nav broken | 1 day |
| **Medium** | Feature broken, workaround exists | Filter not working, minor UI issue | 3 days |
| **Low** | Minor issue, cosmetic | Typo, alignment off | Next sprint |

### 5.3 Bug Triage Process

```
┌─────────────────┐
│   Bug Reported  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  QA Validates   │
│  - Reproducible?│
│  - Duplicate?   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
 Invalid   Valid
    │         │
    ▼         ▼
┌────────┐  ┌─────────────┐
│ Close  │  │ Assign      │
│ Won't  │  │ Severity    │
│ Fix    │  │ Priority    │
└────────┘  └──────┬──────┘
                   │
                   ▼
           ┌─────────────┐
           │ Dev Assigns │
           │ Sprint      │
           └─────────────┘
```

### 5.4 Bug Communication

**Slack Template for Critical Bugs:**
```
🔴 CRITICAL BUG FOUND

Bug ID: BUG-123
Title: Homepage returning 500 error
Environment: Stage
Blocking: Yes - Release testing

Steps: Navigate to www.example.com

Dev assigned: @developer
ETA: [requested]

Thread updates below 👇
```

---

## 6. Regression Testing

### 6.1 Regression Test Suite

```markdown
## Regression Test Suite Structure

### Core Functionality (Must Pass)
- [ ] Homepage loads < 3s
- [ ] Global navigation functional
- [ ] Search returns results
- [ ] All page templates render
- [ ] Authentication works
- [ ] Publishing workflow completes

### Component Regression
- [ ] Hero component variations
- [ ] Card grid layouts
- [ ] Carousel navigation
- [ ] Navigation mega-menu
- [ ] Footer links

### Integration Regression
- [ ] LLM content generation
- [ ] Email notifications
- [ ] Analytics tracking
- [ ] Form submissions

### Cross-Browser Matrix
| Browser | Windows | Mac | Mobile |
|---------|---------|-----|--------|
| Chrome | ✓ | ✓ | ✓ |
| Safari | - | ✓ | ✓ |
| Edge | ✓ | ✓ | - |
| Firefox | ✓ | ✓ | - |

### Accessibility Regression
- [ ] Keyboard navigation
- [ ] Screen reader compatibility
- [ ] Color contrast
- [ ] Focus indicators
```

### 6.2 Regression Automation

```javascript
// playwright-regression.spec.js
import { test, expect } from '@playwright/test';

test.describe('Core Regression Suite', () => {

    test('Homepage loads successfully', async ({ page }) => {
        await page.goto('/content/bmad-showcase/en/home.html');

        await expect(page).toHaveTitle(/Home/);
        await expect(page.locator('.hero')).toBeVisible();
        await expect(page.locator('nav')).toBeVisible();
    });

    test('Navigation works', async ({ page }) => {
        await page.goto('/content/bmad-showcase/en/home.html');

        // Test main nav
        await page.click('nav a:has-text("About")');
        await expect(page).toHaveURL(/about/);

        // Test breadcrumb
        await expect(page.locator('.breadcrumb')).toBeVisible();
    });

    test('Search returns results', async ({ page }) => {
        await page.goto('/content/bmad-showcase/en/home.html');

        await page.click('[data-testid="search-toggle"]');
        await page.fill('[data-testid="search-input"]', 'bmad');
        await page.press('[data-testid="search-input"]', 'Enter');

        await expect(page.locator('.search-results')).toBeVisible();
        await expect(page.locator('.search-result-item').first()).toBeVisible();
    });

    test('Hero CTA navigates correctly', async ({ page }) => {
        await page.goto('/content/bmad-showcase/en/home.html');

        const ctaHref = await page.locator('.hero__cta').getAttribute('href');
        await page.click('.hero__cta');

        await expect(page).toHaveURL(new RegExp(ctaHref));
    });
});

test.describe('Accessibility Regression', () => {

    test('Page passes axe accessibility scan', async ({ page }) => {
        await page.goto('/content/bmad-showcase/en/home.html');

        const accessibilityScanResults = await new AxeBuilder({ page }).analyze();

        expect(accessibilityScanResults.violations).toEqual([]);
    });

    test('Keyboard navigation works', async ({ page }) => {
        await page.goto('/content/bmad-showcase/en/home.html');

        await page.keyboard.press('Tab');
        await expect(page.locator(':focus')).toBeVisible();

        await page.keyboard.press('Enter');
        // Should navigate
    });
});
```

### 6.3 Regression Schedule

| Release Type | Regression Scope | Timeline |
|--------------|-----------------|----------|
| Hotfix | Smoke + Affected Area | Same day |
| Sprint Release | Full Regression | 2 days |
| Major Release | Full + Extended | 1 week |
| Annual Release | Full + Exploratory | 2 weeks |

---

## 7. UAT Coordination

### 7.1 UAT Planning

```markdown
## UAT Planning Checklist

### 2 Weeks Before UAT
- [ ] Define UAT scope and objectives
- [ ] Identify UAT participants
- [ ] Create UAT test scenarios
- [ ] Prepare UAT environment
- [ ] Schedule UAT sessions
- [ ] Create training materials

### 1 Week Before UAT
- [ ] Send UAT invitations
- [ ] Deploy to UAT environment
- [ ] Verify test data available
- [ ] Conduct dry run
- [ ] Prepare feedback forms

### UAT Week
- [ ] Conduct kickoff meeting
- [ ] Provide user support
- [ ] Track issues real-time
- [ ] Daily status updates
- [ ] Document feedback

### Post-UAT
- [ ] Compile UAT results
- [ ] Prioritize issues found
- [ ] Obtain sign-off
- [ ] Document lessons learned
```

### 7.2 UAT Test Scenarios

```markdown
## UAT Scenario Template

**Scenario:** [Business scenario name]
**Persona:** [User type - Content Author, Marketer, etc.]
**Business Objective:** [What business goal is being tested]

### Steps
1. Login as [persona]
2. Navigate to [area]
3. Perform [action]
4. Verify [outcome]

### Success Criteria
- [ ] [Criterion 1]
- [ ] [Criterion 2]

### UAT Participant Feedback
- Works as expected: Yes/No
- Comments: [Free text]
- Suggestions: [Free text]
```

### 7.3 UAT Communication

**UAT Kickoff Email Template:**
```
Subject: UAT Kickoff - AEM BMAD Showcase v1.2

Dear UAT Participants,

We're ready to begin User Acceptance Testing for version 1.2.

**UAT Details:**
- Dates: [Start] to [End]
- Environment: stage.example.com
- Login: Use your SSO credentials

**What's New:**
- Hero component enhancements
- New carousel features
- LLM content generation

**How to Participate:**
1. Access the UAT environment
2. Follow test scenarios (attached)
3. Log any issues in [tool]
4. Complete feedback form

**Support:**
- Slack: #uat-support
- Email: qa-team@example.com
- Daily standup: 10 AM

Thank you for your participation!
QA Team
```

### 7.4 UAT Sign-Off

```markdown
## UAT Sign-Off Form

**Release:** v1.2.0
**UAT Period:** [Dates]
**Environment:** Stage

### UAT Summary
- Total Scenarios: 25
- Passed: 23
- Failed: 0
- Deferred: 2

### Deferred Items
| Item | Reason | Target Release |
|------|--------|----------------|
| Feature X | Low priority | v1.3.0 |
| Feature Y | Needs design | v1.3.0 |

### Sign-Off
By signing below, I confirm that UAT has been completed
satisfactorily and the release may proceed to production.

**Business Owner:** _________________ Date: _______
**Product Manager:** ________________ Date: _______
**QA Lead:** _______________________ Date: _______
```

---

## 8. QA Tools and Resources

### 8.1 Testing Tools

| Tool | Purpose | Access |
|------|---------|--------|
| **Jira** | Test case & bug management | SSO |
| **Confluence** | Test documentation | SSO |
| **Playwright** | E2E automation | Local + CI |
| **Postman** | API testing | Free download |
| **BrowserStack** | Cross-browser testing | Team account |
| **Axe DevTools** | Accessibility testing | Browser extension |
| **Lighthouse** | Performance/a11y audits | Chrome DevTools |
| **Charles Proxy** | Network debugging | Licensed |

### 8.2 Browser DevTools for QA

```markdown
## Essential DevTools Skills

### Elements Tab
- Inspect component structure
- Check CSS classes applied
- Verify accessibility attributes (role, aria-*)
- Test responsive breakpoints

### Console Tab
- Check for JavaScript errors
- Verify analytics events fire
- Debug component behavior

### Network Tab
- Check API response times
- Verify correct endpoints called
- Inspect request/response payloads
- Check for failed requests (red)

### Application Tab
- Inspect cookies
- Check localStorage/sessionStorage
- Verify service worker status

### Lighthouse Tab
- Run performance audit
- Run accessibility audit
- Run SEO audit
- Run PWA audit
```

### 8.3 AEM-Specific Testing Tips

```markdown
## AEM Testing Tips

### Dispatcher Testing
1. Check X-Dispatcher header in response
2. Verify caching: second request should be faster
3. Test cache invalidation after publish

### Component Dialog Testing
1. Open component dialog
2. Test all field validations
3. Test field dependencies (show/hide)
4. Verify default values
5. Test required fields

### Publishing Testing
1. Author content
2. Preview content
3. Publish content
4. Verify on publish tier
5. Verify dispatcher cache cleared

### Multi-Site Testing
1. Test language copy behavior
2. Test live copy inheritance
3. Test blueprint changes propagation

### Experience Fragment Testing
1. Test fragment renders on page
2. Test fragment variations
3. Test fragment in multiple locations
```

### 8.4 QA Metrics Dashboard

| Metric | Target | Current |
|--------|--------|---------|
| **Test Coverage** | 80% | [Track] |
| **Defect Density** | < 5/KLOC | [Track] |
| **Test Pass Rate** | > 95% | [Track] |
| **Bug Escape Rate** | < 5% | [Track] |
| **Regression Pass Rate** | 100% | [Track] |
| **Mean Time to Detect** | < 2 days | [Track] |
| **UAT Pass Rate** | > 90% | [Track] |

---

## Appendix A: Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| DevTools | F12 / Cmd+Opt+I |
| Responsive Mode | Cmd+Shift+M |
| Element Inspector | Cmd+Shift+C |
| Console | Cmd+Opt+J |
| Hard Refresh | Cmd+Shift+R |
| Clear Cache | Settings > Clear |

## Appendix B: Common AEM Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Stale content | Dispatcher cache | Clear cache, republish |
| 404 errors | Missing content/mapping | Check sling:alias, redirects |
| Component not rendering | Missing model/dialog | Check node structure |
| Styling issues | Clientlib not loading | Check categories, dependencies |

## Appendix C: BEAD Task Integration

As a QA Engineer, you'll work with BEAD tasks:

```markdown
## QA BEAD Task Template

**Task ID:** BEAD-QA-001
**Type:** test-task
**Story:** STORY-123

### Testing Scope
- Component: Hero
- Test Types: Functional, Accessibility, Responsive

### Acceptance Criteria
- [ ] All test cases created
- [ ] All test cases executed
- [ ] No P1/P2 bugs open
- [ ] Cross-browser verified

### Definition of Done
- Test cases documented
- Test execution complete
- Bug reports filed
- Story marked QA Approved
```

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | QA Team | Initial version |

**Review Cycle:** Quarterly
**Next Review:** [Current Date + 3 months]
