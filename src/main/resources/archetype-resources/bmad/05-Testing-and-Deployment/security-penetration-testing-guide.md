# Security and Penetration Testing Guide

## Overview

This guide provides comprehensive security and penetration testing procedures for the AEM BMAD Showcase application. It covers OWASP testing methodology, automated security scanning, manual penetration testing procedures, vulnerability management, and pre-production security gates.

---

## Table of Contents

1. [Security Testing Strategy](#1-security-testing-strategy)
2. [OWASP Testing Methodology](#2-owasp-testing-methodology)
3. [Automated Security Scanning](#3-automated-security-scanning)
4. [Manual Penetration Testing](#4-manual-penetration-testing)
5. [AEM-Specific Security Tests](#5-aem-specific-security-tests)
6. [API Security Testing](#6-api-security-testing)
7. [Vulnerability Management](#7-vulnerability-management)
8. [Security Gates](#8-security-gates)

---

## 1. Security Testing Strategy

### 1.1 Security Testing Pyramid

```
                    ┌─────────────────┐
                    │  Penetration    │  Annual/Major Release
                    │    Testing      │  (External)
                    ├─────────────────┤
                 ┌──┴─────────────────┴──┐
                 │  Security Code Review │  Per Feature
                 │  (Manual + SAST)      │
                 ├───────────────────────┤
              ┌──┴───────────────────────┴──┐
              │    DAST Scanning            │  Weekly/Per Deploy
              ├─────────────────────────────┤
           ┌──┴─────────────────────────────┴──┐
           │  Dependency Scanning (SCA)        │  Daily/Per Build
           ├───────────────────────────────────┤
        ┌──┴───────────────────────────────────┴──┐
        │  SAST + Security Unit Tests             │  Every Commit
        └─────────────────────────────────────────┘
```

### 1.2 Testing Schedule

| Test Type | Frequency | Environment | Owner |
|-----------|-----------|-------------|-------|
| SAST | Every commit | CI Pipeline | Dev Team |
| SCA/Dependency | Daily | CI Pipeline | Dev Team |
| DAST | Weekly | Stage | Security Team |
| Manual Code Review | Per feature | Pre-merge | Security Champion |
| Internal Pen Test | Quarterly | Stage | Security Team |
| External Pen Test | Annually | Production-like | Third Party |
| Red Team Exercise | Annually | Production | External |

### 1.3 Testing Scope

| Component | In Scope | Out of Scope |
|-----------|----------|--------------|
| AEM Application Code | Yes | Adobe AEM Platform |
| Custom Components | Yes | Core Components |
| OSGi Services | Yes | AEM Standard Services |
| Dispatcher Config | Yes | CDN Infrastructure |
| API Endpoints | Yes | Third-party APIs (test integration) |
| LLM Integration | Yes (prompts, outputs) | LLM Provider Security |

---

## 2. OWASP Testing Methodology

### 2.1 OWASP Testing Categories

| Category | Description | AEM Relevance |
|----------|-------------|---------------|
| **WSTG-INFO** | Information Gathering | Server fingerprinting, path disclosure |
| **WSTG-CONF** | Configuration Testing | OSGi configs, dispatcher rules |
| **WSTG-IDNT** | Identity Management | User provisioning, roles |
| **WSTG-ATHN** | Authentication | Login, SSO, session management |
| **WSTG-ATHZ** | Authorization | ACLs, CUGs, permissions |
| **WSTG-SESS** | Session Management | Session tokens, timeout |
| **WSTG-INPV** | Input Validation | XSS, injection, file upload |
| **WSTG-ERRH** | Error Handling | Stack traces, error messages |
| **WSTG-CRYP** | Cryptography | TLS, password storage |
| **WSTG-BUSN** | Business Logic | Workflow bypass, rate limiting |

### 2.2 OWASP Top 10 Test Cases

#### A01:2021 - Broken Access Control

```yaml
Test Cases:
  - ID: AC-001
    Name: Vertical Privilege Escalation
    Steps:
      1. Login as regular author
      2. Attempt to access admin-only URLs (/crx/de, /system/console)
      3. Attempt to modify content outside assigned paths
    Expected: Access denied with 403 response

  - ID: AC-002
    Name: Horizontal Privilege Escalation
    Steps:
      1. Login as user A
      2. Attempt to access user B's profile/content
      3. Attempt to modify user B's preferences
    Expected: Access denied or only own content visible

  - ID: AC-003
    Name: Direct Object Reference
    Steps:
      1. Identify resource IDs in URLs
      2. Modify IDs to reference other users' resources
      3. Check for authorization enforcement
    Expected: Proper authorization checks on all resources
```

#### A03:2021 - Injection

```yaml
Test Cases:
  - ID: INJ-001
    Name: JCR Query Injection
    Steps:
      1. Identify query parameters in search/filter features
      2. Inject JCR-SQL2 syntax: ' OR 1=1 --
      3. Inject XPath syntax: '] or true() or ['
      4. Monitor for unexpected results or errors
    Expected: Parameterized queries, no injection possible

  - ID: INJ-002
    Name: XSS in User Content
    Steps:
      1. Input <script>alert('XSS')</script> in all text fields
      2. Input event handlers: <img onerror="alert(1)" src="x">
      3. Test SVG payloads: <svg onload="alert(1)">
      4. Check stored content rendering
    Expected: All input sanitized, no script execution

  - ID: INJ-003
    Name: Server-Side Template Injection
    Steps:
      1. Test HTL expression injection: ${7*7}
      2. Test in component properties
      3. Check for template engine errors
    Expected: No expression evaluation from user input
```

#### A07:2021 - Identification and Authentication Failures

```yaml
Test Cases:
  - ID: AUTH-001
    Name: Brute Force Protection
    Steps:
      1. Attempt 10+ failed logins
      2. Check for account lockout
      3. Check for rate limiting
      4. Monitor response times for timing attacks
    Expected: Account locked after 5 attempts, consistent response times

  - ID: AUTH-002
    Name: Session Fixation
    Steps:
      1. Note session ID before login
      2. Login and check session ID
      3. Verify session ID changed
    Expected: New session ID generated on authentication

  - ID: AUTH-003
    Name: Session Timeout
    Steps:
      1. Login and note session
      2. Wait for configured timeout period
      3. Attempt to use session
    Expected: Session invalidated after timeout
```

### 2.3 OWASP Testing Checklist

```markdown
## Pre-Testing Checklist

- [ ] Scope defined and approved
- [ ] Test accounts provisioned
- [ ] Test environment isolated
- [ ] Baseline captured
- [ ] Tools configured
- [ ] Emergency contacts available

## Information Gathering
- [ ] WSTG-INFO-01: Search engine reconnaissance
- [ ] WSTG-INFO-02: Web server fingerprinting
- [ ] WSTG-INFO-03: Application framework fingerprinting
- [ ] WSTG-INFO-04: Application entry points
- [ ] WSTG-INFO-05: Web application structure mapping

## Configuration Testing
- [ ] WSTG-CONF-01: Network infrastructure
- [ ] WSTG-CONF-02: Application platform configuration
- [ ] WSTG-CONF-03: File extension handling
- [ ] WSTG-CONF-04: Backup and unreferenced files
- [ ] WSTG-CONF-05: Admin interfaces

## Authentication Testing
- [ ] WSTG-ATHN-01: Credentials over encrypted channel
- [ ] WSTG-ATHN-02: Default credentials
- [ ] WSTG-ATHN-03: Weak lock out mechanism
- [ ] WSTG-ATHN-04: Bypassing authentication
- [ ] WSTG-ATHN-05: Password change functionality

## Authorization Testing
- [ ] WSTG-ATHZ-01: Directory traversal
- [ ] WSTG-ATHZ-02: Authorization bypass
- [ ] WSTG-ATHZ-03: Privilege escalation
- [ ] WSTG-ATHZ-04: IDOR (Insecure Direct Object Reference)

## Input Validation
- [ ] WSTG-INPV-01: Reflected XSS
- [ ] WSTG-INPV-02: Stored XSS
- [ ] WSTG-INPV-03: HTTP verb tampering
- [ ] WSTG-INPV-05: SQL/NoSQL injection
- [ ] WSTG-INPV-11: Code injection
- [ ] WSTG-INPV-12: Command injection
```

---

## 3. Automated Security Scanning

### 3.1 SAST (Static Application Security Testing)

**SonarQube Configuration:**
```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.9.1.2184</version>
</plugin>

<!-- sonar-project.properties -->
sonar.projectKey=aem-bmad-showcase
sonar.sources=core/src/main/java,ui.apps/src/main/content
sonar.tests=core/src/test/java
sonar.java.binaries=core/target/classes
sonar.coverage.jacoco.xmlReportPaths=core/target/site/jacoco/jacoco.xml

# Security Rules
sonar.issue.ignore.multicriteria=e1
sonar.issue.ignore.multicriteria.e1.ruleKey=java:S2068
sonar.issue.ignore.multicriteria.e1.resourceKey=**/test/**
```

**FindSecBugs Integration:**
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.7.3.0</version>
    <configuration>
        <plugins>
            <plugin>
                <groupId>com.h3xstream.findsecbugs</groupId>
                <artifactId>findsecbugs-plugin</artifactId>
                <version>1.12.0</version>
            </plugin>
        </plugins>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <failOnError>true</failOnError>
    </configuration>
</plugin>
```

### 3.2 SCA (Software Composition Analysis)

**OWASP Dependency Check:**
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
        <formats>
            <format>HTML</format>
            <format>JSON</format>
        </formats>
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

**Suppression File:**
```xml
<!-- dependency-check-suppressions.xml -->
<suppressions>
    <!-- False positive: Not using vulnerable function -->
    <suppress>
        <notes>We don't use the affected XML parsing functionality</notes>
        <packageUrl regex="true">^pkg:maven/org\.apache\.xerces/.*$</packageUrl>
        <cve>CVE-2022-XXXXX</cve>
    </suppress>

    <!-- Accepted risk with compensating controls -->
    <suppress until="2024-06-01">
        <notes>Accepted risk - scheduled for upgrade in Q2</notes>
        <gav regex="true">.*:vulnerable-library:.*</gav>
        <cvssBelow>7.0</cvssBelow>
    </suppress>
</suppressions>
```

### 3.3 DAST (Dynamic Application Security Testing)

**OWASP ZAP Configuration:**
```yaml
# zap-config.yaml
env:
  contexts:
    - name: "AEM BMAD Showcase"
      urls:
        - "https://stage.example.com"
      includePaths:
        - "https://stage.example.com/content/bmad-showcase/.*"
      excludePaths:
        - "https://stage.example.com/crx/.*"
        - "https://stage.example.com/system/.*"
      authentication:
        method: "form"
        parameters:
          loginUrl: "https://stage.example.com/libs/granite/core/content/login.html"
          loginRequestData: "j_username={%username%}&j_password={%password%}"
        verification:
          method: "response"
          loggedInRegex: "\\QWelcome\\E"

jobs:
  - type: spider
    parameters:
      maxDuration: 60
      maxDepth: 10

  - type: spiderAjax
    parameters:
      maxDuration: 30
      maxCrawlDepth: 5

  - type: passiveScan-wait
    parameters:
      maxDuration: 60

  - type: activeScan
    parameters:
      policy: "Default Policy"
      maxDuration: 120
      maxRuleDurationInMins: 5

  - type: report
    parameters:
      template: "traditional-html"
      reportDir: "/zap/reports"
      reportFile: "zap-report"
```

**ZAP in CI/CD:**
```bash
#!/bin/bash
# run-zap-scan.sh

docker run --rm \
  -v $(pwd)/zap-config.yaml:/zap/config.yaml:ro \
  -v $(pwd)/reports:/zap/reports \
  owasp/zap2docker-stable zap.sh -cmd \
    -autorun /zap/config.yaml

# Check for high/critical findings
HIGH_COUNT=$(jq '.site[].alerts[] | select(.riskcode >= 3) | .name' reports/zap-report.json | wc -l)

if [ $HIGH_COUNT -gt 0 ]; then
  echo "FAIL: $HIGH_COUNT high/critical vulnerabilities found"
  exit 1
fi
```

### 3.4 Cloud Manager Security Scanning

**Built-in Scanning:**
```yaml
# Cloud Manager automatically performs:
- Code Quality Scanning (SonarQube)
- Security Scanning (custom rules + OWASP)
- Dependency Vulnerability Scanning
- Container Image Scanning

# Quality Gates
security.hotspots.reviewed: 100%
security.rating: A
vulnerabilities: 0 (blocker/critical)
```

---

## 4. Manual Penetration Testing

### 4.1 Penetration Test Methodology

```
┌─────────────────┐
│ 1. Reconnaissance│
│   - OSINT        │
│   - Fingerprint  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. Scanning     │
│   - Port scan   │
│   - Vuln scan   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 3. Enumeration  │
│   - Users       │
│   - Endpoints   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 4. Exploitation │
│   - Test vulns  │
│   - Validate    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 5. Post-Exploit │
│   - Persistence │
│   - Pivot       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 6. Reporting    │
│   - Document    │
│   - Remediate   │
└─────────────────┘
```

### 4.2 Reconnaissance Checklist

```markdown
## Passive Reconnaissance
- [ ] Google dork: site:example.com filetype:xml
- [ ] Google dork: site:example.com inurl:crx
- [ ] Shodan search for IP ranges
- [ ] Certificate transparency logs
- [ ] DNS enumeration (subdomains)
- [ ] WHOIS information
- [ ] Social engineering research

## Active Reconnaissance
- [ ] Nmap port scan: nmap -sS -sV -O target
- [ ] Web server fingerprint: whatweb target
- [ ] Technology stack: wappalyzer
- [ ] Directory enumeration: dirb/gobuster
- [ ] Endpoint discovery: burp spider
```

### 4.3 Attack Scenarios

**Scenario 1: Unauthenticated Access**
```markdown
Objective: Access sensitive data without authentication

Steps:
1. Enumerate publicly accessible endpoints
2. Test default/weak credentials on admin interfaces
3. Test for path traversal to access protected content
4. Check for information disclosure in error messages
5. Test for IDOR in API endpoints

Tools: Burp Suite, curl, dirb

Success Criteria:
- Access to content without authentication = Critical
- Information disclosure = High
- Default credentials work = Critical
```

**Scenario 2: Privilege Escalation**
```markdown
Objective: Escalate from regular user to admin

Steps:
1. Login as regular content author
2. Enumerate accessible endpoints
3. Test horizontal privilege escalation (access other users' content)
4. Test vertical privilege escalation (access admin functions)
5. Attempt to modify ACLs/permissions
6. Test for insecure direct object references

Tools: Burp Suite, custom scripts

Success Criteria:
- Admin access gained = Critical
- Cross-user data access = High
- Permission modification = Critical
```

**Scenario 3: Injection Attacks**
```markdown
Objective: Execute code or queries via injection

Steps:
1. Identify all input points (forms, URLs, headers)
2. Test XSS payloads in all text inputs
3. Test JCR-SQL2 injection in search/query features
4. Test command injection if shell operations exist
5. Test SSTI in templating contexts
6. Test file upload vulnerabilities

Payloads:
- XSS: <script>alert(document.domain)</script>
- JCR: ' OR true() --
- Command: ; cat /etc/passwd
- SSTI: ${7*7}

Success Criteria:
- Code execution = Critical
- Data exfiltration = Critical
- XSS (stored) = High
- XSS (reflected) = Medium
```

### 4.4 Penetration Testing Tools

| Category | Tools | Purpose |
|----------|-------|---------|
| **Proxy** | Burp Suite Pro, OWASP ZAP | Intercept and modify traffic |
| **Scanning** | Nmap, Nikto, Nuclei | Vulnerability discovery |
| **Fuzzing** | ffuf, wfuzz | Input fuzzing |
| **Exploitation** | Metasploit, custom scripts | Validate vulnerabilities |
| **Recon** | Amass, subfinder | Subdomain enumeration |
| **Credentials** | Hydra, Hashcat | Password attacks |

### 4.5 Pen Test Report Template

```markdown
# Penetration Test Report

## Executive Summary
- Test Period: [Dates]
- Scope: [Systems tested]
- Critical Findings: [Count]
- High Findings: [Count]
- Overall Risk Rating: [Critical/High/Medium/Low]

## Methodology
- Approach: [Black/Gray/White box]
- Standards: OWASP WSTG, PTES

## Findings Summary

| ID | Title | Risk | CVSS | Status |
|----|-------|------|------|--------|
| F-001 | [Title] | Critical | 9.8 | Open |
| F-002 | [Title] | High | 7.5 | Open |

## Detailed Findings

### F-001: [Vulnerability Title]

**Risk Rating:** Critical (CVSS 9.8)

**Description:**
[Detailed description of the vulnerability]

**Affected Component:**
[URL/endpoint/function affected]

**Evidence:**
[Screenshots, request/response, PoC]

**Impact:**
[Business and technical impact]

**Remediation:**
[Specific steps to fix]

**References:**
- CWE-XXX
- OWASP-XXX

## Recommendations
1. [Priority remediation steps]

## Appendix
- Tool outputs
- Full request/response logs
- Additional evidence
```

---

## 5. AEM-Specific Security Tests

### 5.1 AEM Attack Vectors

```yaml
AEM-Specific Tests:
  - name: CRXDE Access
    test: Attempt access to /crx/de, /crx/explorer
    expected: 403 Forbidden (production)
    risk: Critical

  - name: System Console Access
    test: Attempt access to /system/console
    expected: 403 Forbidden (production)
    risk: Critical

  - name: Query Builder Exposure
    test: Access /bin/querybuilder.json without auth
    expected: 401 Unauthorized
    risk: High

  - name: Servlet Enumeration
    test: Access *.infinity.json, *.tidy.json
    expected: Blocked by dispatcher
    risk: Medium

  - name: User Enumeration
    test: /bin/security/authorizables.json
    expected: 403 Forbidden
    risk: High

  - name: Default Credentials
    test: admin/admin, author/author
    expected: Should fail
    risk: Critical

  - name: Replication Exposure
    test: /etc/replication/agents.author.html
    expected: Authenticated only
    risk: High
```

### 5.2 Dispatcher Security Tests

```bash
#!/bin/bash
# dispatcher-security-test.sh

BASE_URL="https://www.example.com"

echo "Testing Dispatcher Security Rules..."

# Test sensitive path blocking
SENSITIVE_PATHS=(
    "/crx/de/index.jsp"
    "/crx/explorer/browser/index.jsp"
    "/system/console"
    "/apps/"
    "/libs/"
    "/admin/"
    "/etc/replication"
    "/bin/querybuilder.json"
)

for path in "${SENSITIVE_PATHS[@]}"; do
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$path")
    if [ "$status" != "403" ] && [ "$status" != "404" ]; then
        echo "FAIL: $path returned $status (expected 403/404)"
    else
        echo "PASS: $path blocked ($status)"
    fi
done

# Test selector/extension blocking
BLOCKED_SELECTORS=(
    "/content/site.infinity.json"
    "/content/site.tidy.json"
    "/content/site.sysview.xml"
    "/content/site.docview.json"
    "/content/site.-1.json"
    "/content/site.query.json"
)

for path in "${BLOCKED_SELECTORS[@]}"; do
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$path")
    if [ "$status" != "403" ] && [ "$status" != "404" ]; then
        echo "FAIL: $path returned $status (expected 403/404)"
    else
        echo "PASS: $path blocked ($status)"
    fi
done

echo "Testing Security Headers..."
headers=$(curl -sI "$BASE_URL/content/site/en.html")

check_header() {
    if echo "$headers" | grep -qi "$1"; then
        echo "PASS: $1 header present"
    else
        echo "FAIL: $1 header missing"
    fi
}

check_header "X-Frame-Options"
check_header "X-Content-Type-Options"
check_header "X-XSS-Protection"
check_header "Strict-Transport-Security"
check_header "Content-Security-Policy"
```

### 5.3 Component Security Tests

```java
class ComponentSecurityTest {

    @Test
    void testHeroComponentXSSPrevention() {
        // Given - malicious input
        String maliciousTitle = "<script>alert('XSS')</script>";
        String maliciousDesc = "<img onerror='alert(1)' src='x'>";

        // When - rendered through HTL
        String rendered = renderComponent("hero", Map.of(
            "title", maliciousTitle,
            "description", maliciousDesc
        ));

        // Then - should be escaped
        assertThat(rendered).doesNotContain("<script>");
        assertThat(rendered).doesNotContain("onerror=");
        assertThat(rendered).contains("&lt;script&gt;");
    }

    @Test
    void testComponentDoesNotExposeInternalPaths() {
        // Given
        String rendered = renderComponent("card", Map.of(
            "path", "/content/dam/internal/secret.pdf"
        ));

        // Then - internal paths should be hidden or mapped
        assertThat(rendered).doesNotContain("/content/dam/internal");
    }

    @Test
    void testComponentAccessControl() {
        // Given - restricted content
        Resource restrictedResource = getResource("/content/protected/page");

        // When - accessed by anonymous user
        try (ResourceResolver anonResolver = getAnonymousResolver()) {
            Resource accessed = anonResolver.getResource(restrictedResource.getPath());

            // Then
            assertThat(accessed).isNull();
        }
    }
}
```

---

## 6. API Security Testing

### 6.1 API Security Checklist

```markdown
## Authentication
- [ ] All endpoints require authentication (except public)
- [ ] JWT tokens properly validated
- [ ] Token expiration enforced
- [ ] Refresh token rotation implemented
- [ ] Session invalidation on logout

## Authorization
- [ ] Role-based access control enforced
- [ ] Resource-level authorization checked
- [ ] No horizontal privilege escalation
- [ ] No vertical privilege escalation
- [ ] Admin functions properly protected

## Input Validation
- [ ] All parameters validated
- [ ] Type checking enforced
- [ ] Length limits applied
- [ ] Special characters handled
- [ ] File upload restrictions

## Rate Limiting
- [ ] Rate limiting implemented
- [ ] Per-user/IP limits
- [ ] Brute force protection
- [ ] DoS protection

## Response Security
- [ ] Sensitive data not exposed
- [ ] Error messages sanitized
- [ ] CORS properly configured
- [ ] Security headers present
```

### 6.2 API Security Tests

```java
class APISecurityTest {

    @Test
    void testEndpointRequiresAuthentication() {
        Response response = given()
            .when()
            .get("/api/content/protected")
            .then()
            .extract().response();

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void testInvalidTokenRejected() {
        Response response = given()
            .header("Authorization", "Bearer invalid-token")
            .when()
            .get("/api/content/protected")
            .then()
            .extract().response();

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void testRateLimitingEnforced() {
        // Make requests until rate limited
        int successCount = 0;
        for (int i = 0; i < 100; i++) {
            Response response = given()
                .header("Authorization", "Bearer " + getValidToken())
                .when()
                .get("/api/search?q=test")
                .then()
                .extract().response();

            if (response.statusCode() == 429) {
                break;
            }
            successCount++;
        }

        assertThat(successCount).isLessThan(100);
    }

    @Test
    void testSQLInjectionPrevented() {
        Response response = given()
            .header("Authorization", "Bearer " + getValidToken())
            .param("q", "' OR '1'='1")
            .when()
            .get("/api/search")
            .then()
            .extract().response();

        // Should either sanitize or reject
        assertThat(response.statusCode()).isIn(200, 400);
        assertThat(response.body().asString()).doesNotContain("SQL");
    }
}
```

---

## 7. Vulnerability Management

### 7.1 Vulnerability Classification

| Severity | CVSS Range | SLA | Examples |
|----------|------------|-----|----------|
| **Critical** | 9.0-10.0 | 24 hours | RCE, Auth bypass |
| **High** | 7.0-8.9 | 7 days | Stored XSS, SQLi |
| **Medium** | 4.0-6.9 | 30 days | CSRF, Info disclosure |
| **Low** | 0.1-3.9 | 90 days | Minor info leak |
| **Info** | 0.0 | Best effort | Hardening suggestions |

### 7.2 Vulnerability Lifecycle

```
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ Discover │──▶│ Triage   │──▶│ Assign   │──▶│ Fix      │
└──────────┘   └──────────┘   └──────────┘   └──────────┘
                                                  │
                                                  ▼
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ Close    │◀──│ Deploy   │◀──│ Verify   │◀──│ Test     │
└──────────┘   └──────────┘   └──────────┘   └──────────┘
```

### 7.3 Vulnerability Tracking

```yaml
# vulnerability-template.yaml
id: VULN-2024-001
title: Stored XSS in Hero Component
severity: High
cvss: 7.5
status: Open
discovered: 2024-01-15
discovered_by: Security Team
affected_component: core/components/hero
affected_versions: 1.0.0 - 1.2.3
description: |
  The hero component title field does not properly
  sanitize user input, allowing stored XSS attacks.
proof_of_concept: |
  1. Navigate to hero component dialog
  2. Enter <script>alert(1)</script> in title
  3. Save and publish
  4. View published page - XSS executes
remediation: |
  Apply HTL context escaping to title output:
  ${properties.title @ context='text'}
references:
  - CWE-79
  - OWASP-A03:2021
fix_version: 1.2.4
fix_pr: https://github.com/org/repo/pull/123
verified: false
```

---

## 8. Security Gates

### 8.1 Pre-Merge Security Gate

```yaml
# Required before merge to main
pre_merge_checks:
  sast:
    tool: SonarQube
    threshold:
      security_rating: A
      vulnerabilities: 0  # Critical/High
      security_hotspots_reviewed: 100%

  dependency_scan:
    tool: OWASP Dependency Check
    threshold:
      cvss_fail: 7.0
      suppressions_reviewed: true

  secrets_scan:
    tool: GitLeaks/TruffleHog
    threshold:
      secrets_found: 0

  code_review:
    security_champion_approved: required
```

### 8.2 Pre-Deploy Security Gate

```yaml
# Required before production deployment
pre_deploy_checks:
  dast:
    tool: OWASP ZAP
    threshold:
      critical: 0
      high: 0

  container_scan:
    tool: Trivy
    threshold:
      critical: 0
      high: 0

  compliance_check:
    cloud_manager: pass
    security_review: approved

  penetration_test:
    last_test: < 90 days
    open_critical: 0
    open_high: 0
```

### 8.3 Security Gate Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                    PR Created                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Pre-Merge Gate                                                  │
│ ├── SAST Scan ──────────── Pass/Fail                           │
│ ├── Dependency Scan ────── Pass/Fail                           │
│ ├── Secrets Scan ───────── Pass/Fail                           │
│ └── Security Review ────── Approved/Rejected                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                    All Pass? ├── No ──▶ Block Merge
                              │
                              ▼ Yes
┌─────────────────────────────────────────────────────────────────┐
│                    Merge to Main                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Pre-Deploy Gate                                                 │
│ ├── DAST Scan ──────────── Pass/Fail                           │
│ ├── Container Scan ─────── Pass/Fail                           │
│ ├── Compliance Check ───── Pass/Fail                           │
│ └── Pen Test Status ────── Current/Expired                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                    All Pass? ├── No ──▶ Block Deploy
                              │
                              ▼ Yes
┌─────────────────────────────────────────────────────────────────┐
│                    Deploy to Production                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Appendix A: Security Testing Tools

| Tool | Purpose | License |
|------|---------|---------|
| Burp Suite Pro | Manual testing, proxy | Commercial |
| OWASP ZAP | DAST, automation | Open Source |
| SonarQube | SAST | Open Source/Commercial |
| FindSecBugs | Java SAST | Open Source |
| OWASP Dependency Check | SCA | Open Source |
| Snyk | SCA, Container | Freemium |
| Nuclei | Vulnerability scanning | Open Source |
| GitLeaks | Secrets scanning | Open Source |

## Appendix B: Resources

- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [OWASP Top 10](https://owasp.org/Top10/)
- [AEM Security Checklist](https://experienceleague.adobe.com/docs/experience-manager-65/administering/security/security-checklist.html)
- [CWE Database](https://cwe.mitre.org/)
- [CVE Database](https://cve.mitre.org/)

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Security Team | Initial version |

**Review Cycle:** Quarterly
**Next Review:** [Current Date + 3 months]
