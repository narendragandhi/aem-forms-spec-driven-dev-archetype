# Security Hardening Checklist

## Overview

This document provides a comprehensive security hardening checklist for the AEM BMAD Showcase application. It covers infrastructure security, application security, access controls, and compliance requirements for production deployment.

---

## Table of Contents

1. [Security Assessment Matrix](#1-security-assessment-matrix)
2. [Infrastructure Security](#2-infrastructure-security)
3. [AEM Platform Security](#3-aem-platform-security)
4. [Application Security](#4-application-security)
5. [Authentication & Authorization](#5-authentication--authorization)
6. [Data Protection](#6-data-protection)
7. [Network Security](#7-network-security)
8. [Monitoring & Incident Response](#8-monitoring--incident-response)
9. [Compliance Checklist](#9-compliance-checklist)

---

## 1. Security Assessment Matrix

### 1.1 Overall Security Status

| Category | Status | Risk Level | Last Reviewed |
|----------|--------|------------|---------------|
| Infrastructure | [ ] Complete | Medium | [Date] |
| AEM Platform | [ ] Complete | High | [Date] |
| Application Code | [ ] Complete | High | [Date] |
| Access Control | [ ] Complete | Critical | [Date] |
| Data Protection | [ ] Complete | High | [Date] |
| Network Security | [ ] Complete | Medium | [Date] |
| Monitoring | [ ] Complete | Medium | [Date] |

### 1.2 Risk Priority Legend

| Priority | Description | Remediation Timeline |
|----------|-------------|---------------------|
| **P1 - Critical** | Immediate security risk | Within 24 hours |
| **P2 - High** | Significant vulnerability | Within 1 week |
| **P3 - Medium** | Moderate risk | Within 1 month |
| **P4 - Low** | Minor improvement | Within quarter |

---

## 2. Infrastructure Security

### 2.1 Cloud Infrastructure (AEM as a Cloud Service)

#### Adobe Managed Security
- [x] Adobe manages underlying infrastructure security
- [x] Automatic security patching
- [x] DDoS protection at edge
- [x] SOC 2 Type II compliance

#### Customer Responsibilities

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Enable Cloud Manager IP allowlisting | P2 | [ ] | Restrict admin access |
| Configure CDN WAF rules | P2 | [ ] | Block malicious traffic |
| Review Adobe security bulletins monthly | P3 | [ ] | Stay informed of patches |
| Enable advanced logging | P3 | [ ] | Security event capture |

### 2.2 CDN/Edge Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Enable HTTPS everywhere | P1 | [ ] | Force TLS 1.2+ |
| Configure HSTS headers | P1 | [ ] | `max-age=31536000; includeSubDomains` |
| Enable rate limiting | P2 | [ ] | Prevent brute force |
| Configure WAF rules | P2 | [ ] | OWASP Core Rule Set |
| Block known malicious IPs | P3 | [ ] | Threat intelligence feeds |
| Enable bot management | P3 | [ ] | Mitigate automated attacks |

### 2.3 DNS Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Enable DNSSEC | P2 | [ ] | DNS spoofing protection |
| Configure CAA records | P3 | [ ] | Certificate issuance control |
| Monitor DNS for hijacking | P3 | [ ] | Alert on unauthorized changes |

---

## 3. AEM Platform Security

### 3.1 Author Instance Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Restrict Author access to VPN/IP allowlist | P1 | [ ] | No public access |
| Disable default admin account | P1 | [ ] | Create named admin accounts |
| Change default admin password | P1 | [ ] | Minimum 16 characters |
| Enable login attempt throttling | P1 | [ ] | Lock after 5 failed attempts |
| Configure session timeout | P2 | [ ] | 30 minutes for authors |
| Disable CRXDE Lite in production | P1 | [ ] | Development only |
| Disable WebDAV access | P2 | [ ] | Unless explicitly required |
| Disable Query Builder debug | P1 | [ ] | Information disclosure |

**CRXDE Lite Disabling:**
```json
// com.day.cq.commons.impl.ExternalizerImpl.cfg.json
// Note: CRXDE automatically disabled on AEMaaCS production
```

### 3.2 Publish Instance Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Enable anonymous user restrictions | P1 | [ ] | Limit permissions |
| Configure closed user groups (CUG) | P2 | [ ] | Protected content |
| Disable login page for anonymous | P2 | [ ] | Author-only authentication |
| Configure request filtering | P1 | [ ] | Block sensitive paths |
| Enable Sling referrer filter | P1 | [ ] | CSRF protection |

### 3.3 OSGi Security Configuration

**Adobe Granite CSRF Filter:**
```json
// com.adobe.granite.csrf.impl.CSRFFilter.cfg.json
{
    "filter.methods": ["POST", "PUT", "DELETE", "PATCH"],
    "filter.paths": ["/content", "/apps", "/libs"],
    "allow.empty.path": false,
    "allow.hosts": ["author.example.com"],
    "filter.enabled": true
}
```

**Apache Sling Referrer Filter:**
```json
// org.apache.sling.security.impl.ReferrerFilter.cfg.json
{
    "allow.empty": false,
    "allow.hosts": ["www.example.com", "stage.example.com"],
    "allow.hosts.regexp": [".*\\.example\\.com:443"],
    "filter.methods": ["POST", "PUT", "DELETE"]
}
```

**Security Headers Configuration:**
```json
// com.adobe.granite.security.impl.SecurityHeadersConfigServiceImpl.cfg.json
{
    "hsts.enabled": true,
    "hsts.maxage": 31536000,
    "hsts.includeSubDomains": true,
    "xss.protection.enabled": true,
    "content.type.nosniff": true,
    "referrer.policy": "strict-origin-when-cross-origin"
}
```

### 3.4 Bundle & Service Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Review installed bundles | P2 | [ ] | Remove unnecessary |
| Disable unused services | P2 | [ ] | Reduce attack surface |
| Configure service user mappings | P1 | [ ] | Least privilege |
| Review bundle permissions | P2 | [ ] | Proper scoping |

---

## 4. Application Security

### 4.1 Secure Coding Checklist

#### Input Validation

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Validate all user inputs server-side | P1 | [ ] | Never trust client |
| Encode outputs (XSS prevention) | P1 | [ ] | Context-aware encoding |
| Use parameterized queries | P1 | [ ] | SQL/JCR injection |
| Validate file uploads | P1 | [ ] | Type, size, content |
| Sanitize URL parameters | P1 | [ ] | Path traversal prevention |

#### HTL (Sightly) Security

**Proper context-aware encoding:**
```html
<!-- Correct - auto-escaping for HTML context -->
<p>${properties.description}</p>

<!-- Correct - explicit URI context -->
<a href="${properties.link @ context='uri'}">Link</a>

<!-- DANGEROUS - disable escaping -->
<div>${properties.html @ context='unsafe'}</div>

<!-- Correct - JavaScript context -->
<script>var data = "${properties.data @ context='scriptString'}";</script>
```

| Context | Use Case | Example |
|---------|----------|---------|
| `text` | Default, HTML text | `${text}` |
| `html` | Trusted HTML | `${html @ context='html'}` |
| `uri` | URLs/links | `${url @ context='uri'}` |
| `attribute` | HTML attributes | `${attr @ context='attribute'}` |
| `scriptString` | JS strings | `${str @ context='scriptString'}` |

### 4.2 Sling Model Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Use service users, not admin | P1 | [ ] | Principle of least privilege |
| Validate resource access | P2 | [ ] | Authorization checks |
| Handle null safely | P2 | [ ] | Prevent NPE disclosure |
| Log security events | P2 | [ ] | Audit trail |

**Secure Sling Model Example:**
```java
@Model(adaptables = Resource.class)
public class SecureModel {

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    private static final String SERVICE_USER = "bmad-content-reader";

    public List<Resource> getProtectedContent() {
        Map<String, Object> params = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(params)) {
            // Use service user resolver with limited permissions
            return findContent(resolver);
        } catch (LoginException e) {
            LOG.error("Failed to obtain service resolver", e);
            return Collections.emptyList();
        }
    }
}
```

### 4.3 LLM Integration Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Store API keys in Cloud Manager secrets | P1 | [ ] | Never in code |
| Implement rate limiting | P1 | [ ] | Prevent abuse |
| Validate/sanitize LLM outputs | P1 | [ ] | XSS in responses |
| Log LLM interactions (sanitized) | P2 | [ ] | Audit trail |
| Implement prompt injection defenses | P1 | [ ] | Malicious prompts |
| Set maximum token limits | P2 | [ ] | Cost control |

**Prompt Injection Prevention:**
```java
public class LLMSecurityService {

    private static final Pattern INJECTION_PATTERNS = Pattern.compile(
        "(?i)(ignore previous|forget instructions|system prompt|you are now)",
        Pattern.CASE_INSENSITIVE
    );

    public String sanitizeUserInput(String input) {
        // Check for known injection patterns
        if (INJECTION_PATTERNS.matcher(input).find()) {
            LOG.warn("Potential prompt injection detected");
            throw new SecurityException("Invalid input detected");
        }

        // Limit input length
        if (input.length() > MAX_INPUT_LENGTH) {
            input = input.substring(0, MAX_INPUT_LENGTH);
        }

        // Escape special characters
        return StringEscapeUtils.escapeHtml4(input);
    }
}
```

### 4.4 Dependency Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Run OWASP dependency check | P1 | [ ] | CI pipeline |
| Update vulnerable dependencies | P1 | [ ] | Within SLA |
| Pin dependency versions | P2 | [ ] | Reproducible builds |
| Review transitive dependencies | P2 | [ ] | Supply chain |
| Sign artifacts | P3 | [ ] | Integrity verification |

**Maven OWASP Plugin Configuration:**
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.0</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <suppressionFiles>
            <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

## 5. Authentication & Authorization

### 5.1 Authentication Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Integrate SSO/SAML | P1 | [ ] | Corporate identity |
| Enable MFA for authors | P1 | [ ] | Required for admins |
| Configure password policy | P1 | [ ] | Min 12 chars, complexity |
| Implement account lockout | P1 | [ ] | After 5 failed attempts |
| Secure session management | P1 | [ ] | HttpOnly, Secure cookies |
| Configure session timeout | P2 | [ ] | 30 mins idle, 8 hrs max |

**SAML Configuration Checklist:**
- [ ] SAML certificate imported to trust store
- [ ] IDP metadata configured
- [ ] Assertion encryption enabled
- [ ] Signature verification enabled
- [ ] User attribute mapping configured
- [ ] Group synchronization configured

### 5.2 Authorization Matrix

| Role | Content Access | Admin Access | Deploy Access |
|------|---------------|--------------|---------------|
| Content Author | Read/Write assigned | None | None |
| Content Reviewer | Read all, Approve | None | None |
| Content Admin | Full content | Limited | None |
| Developer | Full (non-prod) | Config only | Dev/Stage |
| Operations | Read only | Full | All |
| Security Admin | Audit only | Full | None |

### 5.3 Service User Configuration

| Service User | Purpose | Permissions |
|--------------|---------|-------------|
| `bmad-content-reader` | Read published content | `/content/bmad-showcase:read` |
| `bmad-workflow-service` | Execute workflows | `/content:read,write`, `/var/workflow:write` |
| `bmad-replication-service` | Content replication | `/content:read,replicate` |
| `bmad-llm-service` | LLM integration | `/conf:read`, `/content:read` |

**Service User Mapping:**
```json
// org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-bmad.cfg.json
{
    "user.mapping": [
        "com.example.aem.bmad.core:content-reader=bmad-content-reader",
        "com.example.aem.bmad.core:workflow-service=bmad-workflow-service",
        "com.example.aem.bmad.core:llm-service=bmad-llm-service"
    ]
}
```

---

## 6. Data Protection

### 6.1 Data Classification

| Classification | Description | Examples | Controls |
|---------------|-------------|----------|----------|
| **Public** | Freely available | Marketing content | Standard |
| **Internal** | Business use only | Internal docs | Access control |
| **Confidential** | Limited access | Customer data | Encryption + ACL |
| **Restricted** | Highly sensitive | PII, credentials | Full encryption |

### 6.2 Encryption Checklist

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| TLS 1.2+ for all connections | P1 | [ ] | Disable TLS 1.0/1.1 |
| Encrypt sensitive data at rest | P1 | [ ] | AEM Crypto API |
| Secure credential storage | P1 | [ ] | Cloud Manager secrets |
| Certificate management | P2 | [ ] | 90-day rotation |
| Key rotation procedures | P2 | [ ] | Annual minimum |

### 6.3 PII Handling

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Identify all PII fields | P1 | [ ] | Data mapping |
| Implement data minimization | P1 | [ ] | Collect only needed |
| Configure data retention | P1 | [ ] | Delete when expired |
| Enable audit logging for PII | P1 | [ ] | Access tracking |
| Implement export capability | P2 | [ ] | GDPR compliance |
| Implement deletion capability | P2 | [ ] | Right to erasure |

---

## 7. Network Security

### 7.1 Dispatcher Security

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Block sensitive paths | P1 | [ ] | /crx, /system, /libs |
| Enable request filtering | P1 | [ ] | Filter selectors, extensions |
| Configure method restrictions | P1 | [ ] | Allow only needed |
| Block query parameters attacks | P1 | [ ] | SQL/JCR injection |
| Enable ModSecurity | P2 | [ ] | WAF rules |

**Dispatcher Filter Configuration:**
```apache
/filter {
    # Deny all by default
    /0001 { /type "deny" /url "*" }

    # Allow content paths
    /0100 { /type "allow" /url "/content/bmad-showcase/*" }
    /0101 { /type "allow" /url "/content/dam/bmad-showcase/*" }

    # Allow clientlibs
    /0200 { /type "allow" /url "/etc.clientlibs/*" }

    # Block sensitive paths
    /0900 { /type "deny" /url "/crx/*" }
    /0901 { /type "deny" /url "/system/*" }
    /0902 { /type "deny" /url "/apps/*" }
    /0903 { /type "deny" /url "/libs/*" }
    /0904 { /type "deny" /url "/admin/*" }
    /0905 { /type "deny" /url "*.infinity.json" }
    /0906 { /type "deny" /url "*.tidy.json" }
    /0907 { /type "deny" /url "*.-1.json" }
    /0908 { /type "deny" /selectors '(feed|rss|pages|languages|blueprint|hierarchiey|infinity|tidy|sysview|docview|query|jcr:content|_jcr_content|[0-9-]+)' }
}
```

### 7.2 Security Headers

```apache
# dispatcher/src/conf.d/available_vhosts/security-headers.conf

# Prevent clickjacking
Header always set X-Frame-Options "SAMEORIGIN"

# XSS Protection
Header always set X-XSS-Protection "1; mode=block"

# Prevent MIME sniffing
Header always set X-Content-Type-Options "nosniff"

# Strict Transport Security
Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"

# Content Security Policy
Header always set Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://api.openai.com;"

# Referrer Policy
Header always set Referrer-Policy "strict-origin-when-cross-origin"

# Permissions Policy
Header always set Permissions-Policy "geolocation=(), microphone=(), camera=()"
```

---

## 8. Monitoring & Incident Response

### 8.1 Security Monitoring

| Item | Priority | Status | Notes |
|------|----------|--------|-------|
| Enable security event logging | P1 | [ ] | All auth events |
| Configure SIEM integration | P2 | [ ] | Central monitoring |
| Set up security alerts | P1 | [ ] | Failed logins, privilege changes |
| Monitor for anomalies | P2 | [ ] | Unusual access patterns |
| Enable audit logs | P1 | [ ] | Retain 1 year |

### 8.2 Security Alerts

| Alert | Condition | Priority | Response |
|-------|-----------|----------|----------|
| Brute Force Attempt | >10 failed logins/min | P1 | Block IP, investigate |
| Privilege Escalation | Unauthorized admin access | P1 | Immediate investigation |
| Unusual Data Access | High volume data export | P2 | Investigate user |
| SQL/JCR Injection Attempt | Attack pattern detected | P1 | Block request, review |
| Certificate Expiry | <30 days to expiry | P2 | Renew certificate |

### 8.3 Incident Response Plan

**Severity Classification:**

| Severity | Definition | Response Time |
|----------|------------|---------------|
| SEV-1 | Active breach, data loss | 15 minutes |
| SEV-2 | Attempted breach, vulnerability | 1 hour |
| SEV-3 | Policy violation, minor issue | 4 hours |
| SEV-4 | Security improvement | 24 hours |

**Response Procedures:**

1. **Detection** - Alert triggered or reported
2. **Triage** - Assess severity and scope
3. **Containment** - Isolate affected systems
4. **Eradication** - Remove threat
5. **Recovery** - Restore normal operations
6. **Lessons Learned** - Post-incident review

---

## 9. Compliance Checklist

### 9.1 OWASP Top 10 Coverage

| Vulnerability | Status | Controls |
|--------------|--------|----------|
| A01:2021 Broken Access Control | [ ] | ACLs, Service users |
| A02:2021 Cryptographic Failures | [ ] | TLS, AEM Crypto |
| A03:2021 Injection | [ ] | Input validation, HTL encoding |
| A04:2021 Insecure Design | [ ] | Security architecture review |
| A05:2021 Security Misconfiguration | [ ] | This checklist |
| A06:2021 Vulnerable Components | [ ] | OWASP Dependency Check |
| A07:2021 Auth Failures | [ ] | SSO, MFA, lockout |
| A08:2021 Integrity Failures | [ ] | Signed artifacts, CI/CD controls |
| A09:2021 Logging Failures | [ ] | Audit logging, SIEM |
| A10:2021 SSRF | [ ] | URL validation, allowlists |

### 9.2 Security Testing Requirements

| Test Type | Frequency | Responsible | Last Run |
|-----------|-----------|-------------|----------|
| SAST (Static Analysis) | Every build | CI Pipeline | [Date] |
| DAST (Dynamic Analysis) | Weekly | Security Team | [Date] |
| Dependency Scan | Daily | CI Pipeline | [Date] |
| Penetration Test | Annually | External | [Date] |
| Security Review | Quarterly | Security Team | [Date] |

### 9.3 Pre-Production Security Signoff

- [ ] Security architecture review completed
- [ ] Threat modeling documented
- [ ] Code security review completed
- [ ] Penetration testing completed
- [ ] Vulnerability remediation verified
- [ ] Security monitoring configured
- [ ] Incident response plan in place
- [ ] Security documentation current
- [ ] Security training completed for team

---

## Appendix A: Quick Security Commands

```bash
# Check installed bundles
curl -u admin:admin http://localhost:4502/system/console/bundles.json

# List users
curl -u admin:admin http://localhost:4502/bin/querybuilder.json?path=/home/users&type=rep:User

# Check OSGi config
curl -u admin:admin http://localhost:4502/system/console/configMgr/*.json

# Check service users
curl -u admin:admin http://localhost:4502/system/console/services?filter=org.apache.sling.serviceusermapping

# Test CSRF token
curl -u admin:admin http://localhost:4502/libs/granite/csrf/token.json
```

---

## Appendix B: Security Resources

- [Adobe Security Bulletins](https://helpx.adobe.com/security.html)
- [AEM Security Checklist](https://experienceleague.adobe.com/docs/experience-manager-65/administering/security/security-checklist.html)
- [OWASP Top 10](https://owasp.org/Top10/)
- [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks/)

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Security Team | Initial version |

**Review Cycle:** Quarterly
**Next Review:** [Current Date + 3 months]
**Owner:** Security Team
