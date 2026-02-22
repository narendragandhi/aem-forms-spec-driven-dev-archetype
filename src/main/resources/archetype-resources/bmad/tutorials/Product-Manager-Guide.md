# Product Manager Guide: Using BMAD for AEM Projects

This guide helps Product Managers effectively use the BMAD (Breakthrough Method for Agile Development) framework to drive AEM as a Cloud Service projects from business discovery through delivery.

## Your Role in BMAD

As a Product Manager, you act as the **PM Agent** in the BMAD framework. Your responsibilities include:

- Driving business discovery and stakeholder alignment
- Gathering and documenting requirements
- Creating user stories with clear acceptance criteria
- Prioritizing the backlog based on business value
- Collaborating with the Architect Agent on content models
- Ensuring traceability from requirements to implementation

## Getting Started

### Step 1: Understand the Project Structure

```
bmad/
├── 01-Business-Discovery/     # Your primary workspace
│   ├── requirements.md        # Business & functional requirements
│   └── user-stories.md        # User stories with acceptance criteria
├── 02-Model-Definition/       # Collaborate with Architect
│   ├── content-models.md      # Content type definitions
│   ├── information-architecture.md
│   └── design-system.md
└── traceability-matrix.md     # Track requirement coverage
```

### Step 2: Business Discovery Phase

#### 2.1 Stakeholder Interviews

Conduct interviews with key stakeholders to understand:

| Question Area | Example Questions |
|---------------|-------------------|
| Business Goals | What are the primary business objectives for this website? |
| Target Audience | Who are the main user personas? |
| Content Strategy | What types of content will be published? How often? |
| Integrations | What existing systems need to integrate (CRM, Analytics, Translation)? |
| Success Metrics | How will we measure success? |

#### 2.2 Document Requirements

Create your requirements document following this structure:

```markdown
# Business Requirements

## Business Goals
- [Goal 1]: [Description and success metric]
- [Goal 2]: [Description and success metric]

## Functional Requirements

### Content Authoring
- [Requirement]: [Details]

### Digital Asset Management
- [Requirement]: [Details]

### Personalization
- [Requirement]: [Details]
```

Reference: [requirements.md](../01-Business-Discovery/requirements.md)

### Step 3: Write User Stories

#### User Story Template

```markdown
## US-[ID]: [Story Title]

**As a** [user role],
**I want** [capability/feature],
**So that** [business value/benefit].

### Acceptance Criteria

- [ ] Given [context], when [action], then [expected result]
- [ ] Given [context], when [action], then [expected result]

### Technical Notes
- [Any technical considerations for the development team]

### Dependencies
- [List any dependent stories or external dependencies]
```

#### Example User Story

```markdown
## US-001: Hero Component with CTA

**As a** content author,
**I want** to create hero banners with customizable headlines, images, and call-to-action buttons,
**So that** I can create engaging landing pages that drive conversions.

### Acceptance Criteria

- [ ] Given I am on the page editor, when I drag the Hero component, then it should be added to the page
- [ ] Given a Hero component, when I open the dialog, then I can configure: headline, subheadline, background image, CTA text, CTA link
- [ ] Given a configured Hero, when I view the page, then it displays responsively on desktop, tablet, and mobile
- [ ] Given a Hero with CTA, when a visitor clicks the button, then they are navigated to the configured link

### Technical Notes
- Must support both internal and external links
- Image should support smart cropping for different viewports
- Must meet WCAG 2.1 AA accessibility standards

### Dependencies
- Design system tokens must be defined
- Image rendition profiles configured in DAM
```

Reference: [user-stories.md](../01-Business-Discovery/user-stories.md)

### Step 4: Collaborate on Content Models

Work with the Architect to define content models in `02-Model-Definition/`:

#### Your Input for Content Models

| Your Contribution | Architect's Contribution |
|-------------------|-------------------------|
| Content types needed (articles, products, events) | Template technical structure |
| Required fields per content type | Sling Model definitions |
| Editorial workflow requirements | Component dialog specifications |
| Multi-language requirements | i18n implementation approach |

#### Content Model Review Checklist

- [ ] All required content types are defined
- [ ] Field requirements match editorial needs
- [ ] Workflow states support business processes
- [ ] Multi-language strategy is practical for authors
- [ ] DAM asset types cover all media needs

### Step 5: Maintain Traceability

Use the traceability matrix to track requirement coverage:

```markdown
| Requirement ID | User Story | Component | Test Case | Status |
|----------------|------------|-----------|-----------|--------|
| REQ-001 | US-001 | Hero | TC-001 | Implemented |
| REQ-002 | US-002 | Navigation | TC-002 | In Progress |
```

Reference: [traceability-matrix.md](../traceability-matrix.md)

## Working with AI Agents

### Using BMAD with AI Assistance

When working with AI agents in the BMAD framework:

1. **Clear Requirements**: Provide detailed, unambiguous requirements
2. **Acceptance Criteria**: Write testable acceptance criteria
3. **Context**: Reference existing documentation and design system
4. **Feedback Loop**: Review AI-generated artifacts and provide corrections

### BEAD Integration

For complex features, AI agents use BEAD for task management:

```
Your User Story (BMAD)
    ↓
Mayor AI breaks down into tasks (GasTown)
    ↓
Individual AI agents execute tasks (BEAD)
    ↓
You review and approve deliverables
```

## Sprint Planning

### Preparing for Sprint Planning

1. **Prioritize Backlog**: Rank user stories by business value
2. **Story Points**: Work with the team to estimate complexity
3. **Dependencies**: Identify and communicate cross-team dependencies
4. **Definition of Done**: Ensure clear completion criteria

### Sprint Planning Checklist

- [ ] User stories are refined and ready
- [ ] Acceptance criteria are clear and testable
- [ ] Dependencies are identified
- [ ] Design assets are available
- [ ] Technical feasibility confirmed with Architect

Reference: [sprint-1-plan.md](../04-Development-Sprint/sprint-1-plan.md)

## Key Artifacts You Own

| Artifact | Location | Purpose |
|----------|----------|---------|
| Business Requirements | `01-Business-Discovery/requirements.md` | High-level business goals |
| User Stories | `01-Business-Discovery/user-stories.md` | Detailed feature specifications |
| Information Architecture | `02-Model-Definition/information-architecture.md` | Site structure and navigation |
| Traceability Matrix | `traceability-matrix.md` | Requirement coverage tracking |

## Best Practices

1. **Be Specific**: Vague requirements lead to misaligned implementations
2. **Think Multi-Channel**: Consider how content will be used across touchpoints
3. **Accessibility First**: Include accessibility in acceptance criteria
4. **Iterate**: Refine requirements as you learn more
5. **Communicate**: Regular sync with Architect and Development team

## Next Steps

1. Review the [Business Requirements](../01-Business-Discovery/requirements.md) template
2. Study the [User Stories](../01-Business-Discovery/user-stories.md) examples
3. Understand the [Content Models](../02-Model-Definition/content-models.md)
4. Familiarize yourself with AEM authoring capabilities
