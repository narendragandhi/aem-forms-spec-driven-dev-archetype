# GDPR and Privacy Compliance Guide

## Overview

This document provides guidance for ensuring the AEM BMAD Showcase application complies with GDPR (General Data Protection Regulation) and other privacy regulations. It covers data handling practices, user rights implementation, consent management, and compliance procedures.

---

## Table of Contents

1. [Regulatory Overview](#1-regulatory-overview)
2. [Data Inventory](#2-data-inventory)
3. [Lawful Basis for Processing](#3-lawful-basis-for-processing)
4. [User Rights Implementation](#4-user-rights-implementation)
5. [Consent Management](#5-consent-management)
6. [Data Protection Measures](#6-data-protection-measures)
7. [Third-Party Data Processors](#7-third-party-data-processors)
8. [Incident Response](#8-incident-response)
9. [Compliance Checklist](#9-compliance-checklist)

---

## 1. Regulatory Overview

### 1.1 Applicable Regulations

| Regulation | Region | Key Requirements | Applicability |
|------------|--------|-----------------|---------------|
| **GDPR** | EU/EEA | Consent, data rights, breach notification | EU users |
| **CCPA/CPRA** | California | Right to know, delete, opt-out | CA residents |
| **LGPD** | Brazil | Similar to GDPR | Brazilian users |
| **PIPEDA** | Canada | Consent, access rights | Canadian users |
| **UK GDPR** | UK | Post-Brexit GDPR adaptation | UK users |

### 1.2 Key GDPR Principles

| Principle | Description | Implementation |
|-----------|-------------|----------------|
| **Lawfulness** | Valid legal basis for processing | Document lawful basis |
| **Fairness** | Transparent about data use | Privacy notices |
| **Purpose Limitation** | Specific, explicit purposes | Purpose documentation |
| **Data Minimization** | Only necessary data | Review data collected |
| **Accuracy** | Keep data accurate | Update mechanisms |
| **Storage Limitation** | Retain only as needed | Retention policies |
| **Integrity** | Secure processing | Security measures |
| **Accountability** | Demonstrate compliance | Documentation |

### 1.3 Roles and Responsibilities

| Role | Responsibility | Contact |
|------|----------------|---------|
| **Data Controller** | Determines processing purposes | [Organization Name] |
| **Data Processor** | Processes on behalf of controller | Adobe (AEM Cloud) |
| **Data Protection Officer** | Oversees compliance | dpo@example.com |
| **Privacy Team** | Implements privacy controls | privacy@example.com |

---

## 2. Data Inventory

### 2.1 Personal Data Categories

| Category | Data Elements | Location | Retention |
|----------|--------------|----------|-----------|
| **Identity** | Name, username | AEM Repository | Account lifetime |
| **Contact** | Email, phone | AEM Repository | Account lifetime + 30 days |
| **Technical** | IP address, device info | Access logs | 90 days |
| **Behavioral** | Page views, clicks | Analytics | 26 months |
| **Preferences** | Language, consent | AEM/Cookies | Session/Persistent |
| **Content** | User submissions | AEM Repository | As defined |

### 2.2 Data Flow Mapping

```
┌─────────────────────────────────────────────────────────────────┐
│                        Data Collection                          │
├─────────────┬─────────────┬─────────────┬──────────────────────┤
│ Web Forms   │ Cookies     │ Analytics   │ LLM Interactions     │
└──────┬──────┴──────┬──────┴──────┬──────┴──────────┬───────────┘
       │             │             │                 │
       ▼             ▼             ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Processing                              │
├─────────────────────────────────────────────────────────────────┤
│ AEM Repository │ Analytics Platform │ LLM Provider             │
└────────────────┴────────────────────┴───────────────────────────┘
       │                    │                    │
       ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Storage                                │
├─────────────────────────────────────────────────────────────────┤
│ Adobe Cloud (EU/US) │ Analytics Cloud │ LLM Provider Cloud     │
└─────────────────────┴─────────────────┴─────────────────────────┘
```

### 2.3 Data Processing Register

| Process | Purpose | Lawful Basis | Data Categories | Recipients | Transfers |
|---------|---------|--------------|-----------------|------------|-----------|
| Content Personalization | Enhance UX | Consent | Preferences, Behavior | AEM | None |
| Analytics | Site improvement | Legitimate Interest | Technical, Behavioral | Analytics Provider | US (SCCs) |
| AI Translation | Content localization | Contract | Content | LLM Provider | US (SCCs) |
| Email Marketing | Communications | Consent | Identity, Contact | Email Provider | US (SCCs) |
| Contact Forms | Customer inquiry | Contract | Identity, Contact | CRM | None |

---

## 3. Lawful Basis for Processing

### 3.1 Lawful Basis Reference

| Basis | When to Use | Documentation Required |
|-------|-------------|----------------------|
| **Consent** | Marketing, cookies, profiling | Consent records, withdrawal mechanism |
| **Contract** | Fulfilling service requests | Service terms, user agreements |
| **Legal Obligation** | Tax records, fraud prevention | Relevant legal requirements |
| **Vital Interests** | Emergency situations | Rare, document necessity |
| **Public Task** | Government/public functions | Authorization documentation |
| **Legitimate Interest** | Analytics, security | LIA (Legitimate Interest Assessment) |

### 3.2 Legitimate Interest Assessment Template

```markdown
## Legitimate Interest Assessment

### Processing Activity: [Name]

### 1. Purpose Test
- What is the purpose of processing?
- Is it a legitimate interest?
- Is the interest specific enough?

### 2. Necessity Test
- Is the processing necessary for this purpose?
- Could the same result be achieved differently?
- Is this the least intrusive way?

### 3. Balancing Test
- What is the impact on individuals?
- What is the likelihood of harm?
- Are there vulnerable groups affected?
- What safeguards can be put in place?

### Conclusion
[Document decision and reasoning]

### Review Date: [Annual review required]
```

### 3.3 Processing Activities by Basis

**Consent Required:**
- Email marketing subscription
- Non-essential cookies (analytics, advertising)
- Content personalization
- Sharing data with third-party marketers

**Contract Basis:**
- Account creation and management
- Service delivery
- Customer support
- Transaction processing

**Legitimate Interest:**
- Website analytics (anonymized)
- Fraud prevention
- Security logging
- Service improvement

---

## 4. User Rights Implementation

### 4.1 GDPR Rights Matrix

| Right | Description | Timeline | Implementation |
|-------|-------------|----------|----------------|
| **Access** | Request copy of data | 30 days | Export function |
| **Rectification** | Correct inaccurate data | 30 days | Edit profile |
| **Erasure** | Delete personal data | 30 days | Delete account |
| **Restriction** | Limit processing | 30 days | Manual process |
| **Portability** | Receive data in machine format | 30 days | JSON export |
| **Object** | Stop certain processing | Immediately | Preference center |
| **Automated Decision** | Human review | 30 days | Manual review |

### 4.2 Data Subject Request (DSR) Process

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Receive   │───▶│   Verify    │───▶│   Process   │───▶│   Respond   │
│   Request   │    │   Identity  │    │   Request   │    │   & Log     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
      │                  │                  │                  │
      ▼                  ▼                  ▼                  ▼
   Log request      2FA or ID         Execute right      Notify user
   in system        verification       within AEM         Document
```

### 4.3 AEM Data Export Implementation

**User Data Export Servlet:**
```java
@Component(service = Servlet.class)
@SlingServletPaths("/bin/privacy/export")
public class DataExportServlet extends SlingAllMethodsServlet {

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request,
                        SlingHttpServletResponse response)
            throws IOException {

        String userId = request.getParameter("userId");

        // Verify authorization
        if (!isAuthorized(request, userId)) {
            response.sendError(403, "Unauthorized");
            return;
        }

        // Collect user data
        UserDataExport export = new UserDataExport();
        export.setProfile(getUserProfile(userId));
        export.setPreferences(getUserPreferences(userId));
        export.setActivityHistory(getActivityHistory(userId));
        export.setContentSubmissions(getContentSubmissions(userId));

        // Export as JSON
        response.setContentType("application/json");
        response.setHeader("Content-Disposition",
            "attachment; filename=\"user-data-export.json\"");

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), export);

        // Log the export
        auditLog("DATA_EXPORT", userId, request);
    }
}
```

### 4.4 Data Deletion Implementation

**User Data Deletion Service:**
```java
@Component(service = DataDeletionService.class)
public class DataDeletionServiceImpl implements DataDeletionService {

    private static final Logger LOG = LoggerFactory.getLogger(DataDeletionServiceImpl.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private AuditLogService auditLog;

    @Override
    public DeletionResult deleteUserData(String userId, DeletionScope scope) {
        DeletionResult result = new DeletionResult();

        try (ResourceResolver resolver = getServiceResolver()) {
            // Delete profile
            if (scope.includesProfile()) {
                deleteUserProfile(resolver, userId);
                result.addDeleted("profile");
            }

            // Delete preferences
            if (scope.includesPreferences()) {
                deleteUserPreferences(resolver, userId);
                result.addDeleted("preferences");
            }

            // Delete content submissions
            if (scope.includesContent()) {
                deleteUserContent(resolver, userId);
                result.addDeleted("content");
            }

            // Delete from analytics (async)
            if (scope.includesAnalytics()) {
                scheduleAnalyticsDeletion(userId);
                result.addScheduled("analytics");
            }

            resolver.commit();
            auditLog.log("DATA_DELETION", userId, result);

        } catch (Exception e) {
            LOG.error("Failed to delete user data: {}", userId, e);
            result.setError(e.getMessage());
        }

        return result;
    }
}
```

---

## 5. Consent Management

### 5.1 Consent Requirements

| Processing Activity | Consent Required | Granularity | Withdrawal |
|--------------------|------------------|-------------|------------|
| Essential Cookies | No (Legitimate Interest) | N/A | N/A |
| Analytics Cookies | Yes | Separate | Easy |
| Marketing Cookies | Yes | Separate | Easy |
| Email Marketing | Yes | Per type | One-click |
| Profiling | Yes | Explicit | Easy |
| Data Sharing | Yes | Per recipient | Immediate |

### 5.2 Cookie Consent Banner

**Implementation Requirements:**
- [ ] Banner appears before non-essential cookies set
- [ ] Clear accept/reject options
- [ ] Granular category selection
- [ ] Link to full cookie policy
- [ ] Preference center accessible
- [ ] Consent persists appropriately
- [ ] Re-consent for material changes

**Cookie Categories:**
```javascript
const cookieCategories = {
    necessary: {
        name: "Strictly Necessary",
        description: "Required for the website to function",
        required: true,
        cookies: ["session_id", "csrf_token"]
    },
    analytics: {
        name: "Analytics",
        description: "Help us understand how visitors use our site",
        required: false,
        cookies: ["_ga", "_gid", "_gat"]
    },
    marketing: {
        name: "Marketing",
        description: "Used for targeted advertising",
        required: false,
        cookies: ["_fbp", "ads_id"]
    },
    preferences: {
        name: "Preferences",
        description: "Remember your settings and preferences",
        required: false,
        cookies: ["language", "theme"]
    }
};
```

### 5.3 Consent Record Schema

```json
{
    "consentId": "uuid-here",
    "userId": "user123",
    "timestamp": "2024-01-15T14:32:00Z",
    "version": "1.2",
    "source": "cookie_banner",
    "ipAddress": "192.168.1.1 (hashed)",
    "userAgent": "Mozilla/5.0...",
    "consents": {
        "necessary": true,
        "analytics": true,
        "marketing": false,
        "preferences": true
    },
    "privacyPolicyVersion": "2.0",
    "expiresAt": "2025-01-15T14:32:00Z"
}
```

### 5.4 Consent Storage

**AEM Consent Repository Structure:**
```
/var/bmad-showcase/consent/
├── users/
│   └── {userId}/
│       ├── current.json         # Current consent state
│       └── history/
│           ├── 2024-01-01.json  # Historical records
│           └── 2024-06-15.json
└── anonymous/
    └── {consentId}.json         # Anonymous consent records
```

---

## 6. Data Protection Measures

### 6.1 Technical Measures

| Measure | Implementation | Status |
|---------|---------------|--------|
| Encryption at Rest | AEM Cloud encryption | [ ] Verified |
| Encryption in Transit | TLS 1.2+ | [ ] Verified |
| Pseudonymization | User ID mapping | [ ] Implemented |
| Access Controls | Role-based ACLs | [ ] Configured |
| Audit Logging | All PII access | [ ] Enabled |
| Data Minimization | Collection review | [ ] Documented |

### 6.2 Data Retention Schedule

| Data Type | Retention Period | Deletion Method | Legal Basis |
|-----------|-----------------|-----------------|-------------|
| User Accounts | Account lifetime + 30 days | Automated | Contract |
| Transaction Records | 7 years | Manual review | Legal obligation |
| Analytics Data | 26 months | Automated | Legitimate interest |
| Access Logs | 90 days | Automated | Security |
| Consent Records | Duration + 3 years | Automated | Accountability |
| Marketing Lists | Until opt-out | On request | Consent |

### 6.3 Data Retention Implementation

**Scheduled Cleanup Job:**
```java
@Component(
    service = Runnable.class,
    property = {
        "scheduler.expression=0 0 2 * * ?", // Daily at 2 AM
        "scheduler.concurrent=false"
    }
)
public class DataRetentionJob implements Runnable {

    @Reference
    private DataRetentionService retentionService;

    @Override
    public void run() {
        LOG.info("Starting data retention cleanup");

        // Clean expired user data
        int usersDeleted = retentionService.cleanupExpiredUsers();

        // Clean old consent records
        int consentsArchived = retentionService.archiveOldConsents();

        // Clean analytics data
        int analyticsDeleted = retentionService.cleanupAnalytics();

        // Clean access logs
        int logsDeleted = retentionService.cleanupLogs();

        LOG.info("Retention cleanup complete: users={}, consents={}, analytics={}, logs={}",
            usersDeleted, consentsArchived, analyticsDeleted, logsDeleted);
    }
}
```

### 6.4 Privacy by Design Checklist

- [ ] Data minimization in forms (only collect necessary data)
- [ ] Privacy-first default settings
- [ ] Clear purpose specification at collection
- [ ] Secure storage configuration
- [ ] Access logging enabled
- [ ] Retention periods defined
- [ ] Deletion procedures implemented
- [ ] Export functionality available
- [ ] Consent mechanisms in place
- [ ] Third-party integrations reviewed

---

## 7. Third-Party Data Processors

### 7.1 Processor Inventory

| Processor | Service | Data Processed | Location | DPA Status |
|-----------|---------|---------------|----------|------------|
| Adobe | AEM Cloud | All site data | EU/US | [ ] Signed |
| OpenAI | LLM API | Content, prompts | US | [ ] Signed |
| SendGrid | Email | Email addresses | US | [ ] Signed |
| Google | Analytics | Behavioral data | US | [ ] Signed |
| Fastly | CDN | IP, requests | Global | [ ] Signed |

### 7.2 Data Processing Agreement Requirements

**DPA Checklist:**
- [ ] Clearly defined processing instructions
- [ ] Confidentiality obligations
- [ ] Security measures specified
- [ ] Sub-processor notification requirements
- [ ] Assistance with data subject rights
- [ ] Deletion/return of data on termination
- [ ] Audit rights
- [ ] Breach notification procedures
- [ ] Cross-border transfer mechanisms (SCCs)

### 7.3 International Transfer Safeguards

| Transfer | Mechanism | Documentation |
|----------|-----------|---------------|
| EU → US (Adobe) | SCCs + DPA | Adobe DPA |
| EU → US (OpenAI) | SCCs + DPA | OpenAI DPA |
| EU → US (Analytics) | SCCs + Supplementary Measures | Google DPA |

### 7.4 Sub-Processor Management

**Sub-Processor Change Notification Process:**
1. Receive notification from processor
2. Review new sub-processor's security measures
3. Assess impact on data processing
4. Document approval or object within 30 days
5. Update processor inventory

---

## 8. Incident Response

### 8.1 Data Breach Classification

| Category | Examples | Notification Required |
|----------|----------|----------------------|
| **High Risk** | PII exposed, identity theft risk | DPA within 72 hours + individuals |
| **Medium Risk** | Limited data exposure | DPA within 72 hours |
| **Low Risk** | Encrypted data, no access | Internal documentation only |
| **Near Miss** | Vulnerability found, no breach | Internal documentation |

### 8.2 Breach Response Timeline

```
Hour 0-1: Detection & Initial Assessment
├── Contain the breach
├── Preserve evidence
└── Notify incident response team

Hour 1-24: Investigation
├── Determine scope and impact
├── Identify affected individuals
└── Assess risk to individuals

Hour 24-48: Notification Preparation
├── Prepare DPA notification
├── Draft individual notifications
└── Coordinate with legal

Hour 48-72: Notification
├── Submit DPA notification
├── Begin individual notifications
└── Document all communications

Ongoing: Remediation
├── Implement fixes
├── Update security measures
└── Post-incident review
```

### 8.3 DPA Notification Template

```
DATA BREACH NOTIFICATION

1. Organization Details
   - Name: [Organization]
   - DPO Contact: [Email/Phone]

2. Nature of Breach
   - Date Detected: [Date]
   - Date Occurred: [Date/Range]
   - Type: [Confidentiality/Integrity/Availability]
   - Description: [What happened]

3. Data Affected
   - Categories: [Personal data types]
   - Approximate Records: [Number]
   - Affected Individuals: [Number/Categories]

4. Likely Consequences
   - [Risk assessment for individuals]

5. Measures Taken
   - Containment: [Actions taken]
   - Remediation: [Planned actions]
   - Prevention: [Future measures]

6. Communication to Individuals
   - [Yes/No and rationale]
   - [If yes, method and timeline]
```

### 8.4 Breach Documentation Requirements

| Document | Contents | Retention |
|----------|----------|-----------|
| Incident Report | Full breach details | 5 years |
| Evidence Log | Technical evidence | 5 years |
| Notification Records | DPA and individual notices | 5 years |
| Remediation Plan | Actions taken | 5 years |
| Post-Incident Review | Lessons learned | 5 years |

---

## 9. Compliance Checklist

### 9.1 Pre-Launch Privacy Checklist

**Documentation:**
- [ ] Privacy policy published and accessible
- [ ] Cookie policy published
- [ ] Data processing records complete
- [ ] Legitimate interest assessments documented
- [ ] DPAs signed with all processors

**Technical:**
- [ ] Cookie consent banner implemented
- [ ] Consent storage functional
- [ ] Data export functionality tested
- [ ] Data deletion functionality tested
- [ ] Access logging enabled
- [ ] Encryption verified

**Organizational:**
- [ ] DPO appointed (if required)
- [ ] Staff privacy training completed
- [ ] Incident response procedures documented
- [ ] DSR handling procedures documented

### 9.2 Ongoing Compliance Activities

| Activity | Frequency | Owner | Status |
|----------|-----------|-------|--------|
| Privacy policy review | Annual | Legal | [ ] |
| Data inventory update | Quarterly | Privacy | [ ] |
| Processor audit | Annual | Privacy | [ ] |
| Staff training | Annual | HR | [ ] |
| Consent audit | Quarterly | Dev | [ ] |
| Retention cleanup | Daily | System | [ ] |
| Access log review | Monthly | Security | [ ] |
| DSR process test | Quarterly | Privacy | [ ] |

### 9.3 Compliance Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| DSR Response Time | < 30 days | Average response time |
| DSR Completion Rate | 100% | Completed / Received |
| Consent Rate | > 40% | Accepted / Presented |
| Breach Response Time | < 72 hours | Time to notification |
| Training Completion | 100% | Staff trained / Total |
| Policy Acknowledgment | 100% | Acknowledged / Total |

---

## Appendix A: Privacy Policy Sections

Required sections for GDPR-compliant privacy policy:
1. Controller identity and contact details
2. DPO contact details (if applicable)
3. Purposes and legal basis for processing
4. Categories of personal data
5. Recipients of personal data
6. International transfers
7. Retention periods
8. Data subject rights
9. Right to lodge complaint
10. Source of data (if not from subject)
11. Automated decision-making details
12. Updates to the policy

## Appendix B: Key Contacts

| Role | Contact | When to Contact |
|------|---------|-----------------|
| Data Protection Officer | dpo@example.com | Privacy questions, DSRs |
| Privacy Team | privacy@example.com | Implementation questions |
| Legal Team | legal@example.com | Policy review, breaches |
| Security Team | security@example.com | Technical security |
| Customer Support | support@example.com | User inquiries |

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Privacy Team | Initial version |

**Review Cycle:** Annual (or on regulatory change)
**Next Review:** [Current Date + 12 months]
**Owner:** Data Protection Officer
