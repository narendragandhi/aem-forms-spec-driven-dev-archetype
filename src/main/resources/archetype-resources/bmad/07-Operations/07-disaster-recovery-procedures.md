# Disaster Recovery Procedures

## Overview

This document defines the disaster recovery (DR) strategy and procedures for the AEM BMAD Showcase application. It covers recovery objectives, backup procedures, failover mechanisms, and restoration processes to ensure business continuity in the event of a disaster.

---

## Table of Contents

1. [Recovery Objectives](#1-recovery-objectives)
2. [Disaster Scenarios](#2-disaster-scenarios)
3. [Backup Strategy](#3-backup-strategy)
4. [Recovery Procedures](#4-recovery-procedures)
5. [Failover Architecture](#5-failover-architecture)
6. [Communication Plan](#6-communication-plan)
7. [Testing and Validation](#7-testing-and-validation)
8. [Roles and Responsibilities](#8-roles-and-responsibilities)

---

## 1. Recovery Objectives

### 1.1 Recovery Time Objective (RTO)

| System | RTO | Priority | Justification |
|--------|-----|----------|---------------|
| **Public Website (CDN)** | 15 minutes | P1 | Revenue impact |
| **AEM Publish** | 1 hour | P1 | Core functionality |
| **AEM Author** | 4 hours | P2 | Content operations |
| **Analytics** | 24 hours | P3 | Non-critical |
| **Email Services** | 4 hours | P2 | Communications |
| **LLM Integration** | 4 hours | P3 | Enhanced features |

### 1.2 Recovery Point Objective (RPO)

| Data Type | RPO | Backup Frequency | Retention |
|-----------|-----|------------------|-----------|
| **Content Repository** | 1 hour | Continuous | 30 days |
| **User Data** | 1 hour | Continuous | 30 days |
| **Configuration** | 24 hours | Daily | 90 days |
| **Logs** | 24 hours | Daily | 90 days |
| **Analytics Data** | 24 hours | Daily | 1 year |

### 1.3 Service Level Targets

| Metric | Normal | Degraded | Emergency |
|--------|--------|----------|-----------|
| Availability | 99.9% | 99.0% | 95.0% |
| Response Time | < 3s | < 5s | < 10s |
| Functionality | Full | Core only | Read-only |

---

## 2. Disaster Scenarios

### 2.1 Scenario Classification

| Category | Severity | Example Scenarios | Expected RTO |
|----------|----------|-------------------|--------------|
| **Level 1** | Critical | Complete site outage, data center failure | < 1 hour |
| **Level 2** | High | Partial outage, service degradation | 1-4 hours |
| **Level 3** | Medium | Single component failure | 4-8 hours |
| **Level 4** | Low | Performance degradation | 8-24 hours |

### 2.2 Detailed Scenarios

#### Scenario 1: Complete AEM Cloud Outage
```
Trigger: Adobe AEM Cloud regional failure
Impact: Complete website unavailability
Detection: Synthetic monitoring, Adobe status page
Response Time: 15 minutes
Recovery:
1. Confirm Adobe status and ETA
2. Activate CDN cached content (if available)
3. Enable maintenance page
4. Communicate to stakeholders
5. Monitor Adobe recovery
6. Verify full functionality post-recovery
```

#### Scenario 2: Content Repository Corruption
```
Trigger: Data corruption, failed deployment
Impact: Content errors, broken pages
Detection: Error monitoring, user reports
Response Time: 30 minutes
Recovery:
1. Identify corruption scope
2. Stop replication to prevent spread
3. Restore from latest backup
4. Verify content integrity
5. Resume normal operations
6. Post-incident analysis
```

#### Scenario 3: CDN/Edge Failure
```
Trigger: CDN provider outage
Impact: Global or regional unavailability
Detection: Multi-region monitoring
Response Time: 5 minutes
Recovery:
1. Confirm CDN status
2. Failover to backup CDN (if available)
3. Update DNS to bypass CDN
4. Monitor direct origin traffic
5. Restore CDN when available
```

#### Scenario 4: Database/Repository Full
```
Trigger: Storage capacity exceeded
Impact: Write operations fail
Detection: Capacity monitoring alerts
Response Time: 1 hour
Recovery:
1. Stop non-critical write operations
2. Clear temporary files and caches
3. Archive old content versions
4. Run garbage collection
5. Scale storage if needed
6. Implement retention policies
```

#### Scenario 5: Security Breach
```
Trigger: Unauthorized access detected
Impact: Potential data exposure
Detection: Security monitoring, anomaly detection
Response Time: Immediate
Recovery:
1. Isolate affected systems
2. Revoke compromised credentials
3. Assess breach scope
4. Notify security team and management
5. Implement containment measures
6. Begin forensic investigation
7. Restore from known-good backup
8. Regulatory notifications (if required)
```

#### Scenario 6: Third-Party Service Failure
```
Trigger: LLM API, Email service outage
Impact: Feature degradation
Detection: Integration monitoring
Response Time: 30 minutes
Recovery:
1. Identify failed service
2. Enable graceful degradation
3. Show user-friendly error messages
4. Switch to backup provider (if available)
5. Queue requests for retry
6. Monitor service recovery
```

---

## 3. Backup Strategy

### 3.1 Backup Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Primary Region (US-East)                     │
├─────────────────────────────────────────────────────────────────┤
│  AEM Author ──┬── Continuous Replication ──▶ AEM Publish       │
│               │                                                 │
│               └── Hourly Snapshots ─────────▶ Backup Storage   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Secondary Region (EU-West)                     │
├─────────────────────────────────────────────────────────────────┤
│  Cold Standby Author │ Daily Sync │ Backup Repository          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Archive Storage (S3/Azure)                   │
├─────────────────────────────────────────────────────────────────┤
│  Weekly Archives │ Monthly Archives │ Annual Archives          │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Backup Schedule

| Backup Type | Frequency | Retention | Storage |
|-------------|-----------|-----------|---------|
| **Continuous** | Real-time | 24 hours | Primary region |
| **Hourly Snapshots** | Every hour | 7 days | Primary region |
| **Daily Full** | 02:00 UTC | 30 days | Secondary region |
| **Weekly Archive** | Sunday 04:00 | 90 days | Archive storage |
| **Monthly Archive** | 1st of month | 1 year | Archive storage |
| **Annual Archive** | January 1st | 7 years | Cold storage |

### 3.3 AEM Cloud Backup (Adobe Managed)

**Included by Adobe:**
- Repository snapshots (configurable frequency)
- Point-in-time recovery capability
- Cross-region replication (optional)
- Automated backup verification

**Customer Responsibilities:**
- Define backup schedule via Cloud Manager
- Configure retention policies
- Test recovery procedures
- Maintain configuration backups

### 3.4 Configuration Backup

**Items to Backup:**
```bash
# Configuration backup script
#!/bin/bash

BACKUP_DIR="/backup/config/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# Export OSGi configurations
aio cloudmanager:environment:get-configurations \
  --environment-id $ENV_ID \
  --program-id $PROGRAM_ID \
  > $BACKUP_DIR/osgi-configs.json

# Export environment variables
aio cloudmanager:environment:get-variables \
  --environment-id $ENV_ID \
  --program-id $PROGRAM_ID \
  > $BACKUP_DIR/env-vars.json

# Export dispatcher configuration (from repo)
git archive HEAD dispatcher/ > $BACKUP_DIR/dispatcher-config.tar

# Export Cloud Manager pipeline configs
aio cloudmanager:pipeline:list --program-id $PROGRAM_ID --json \
  > $BACKUP_DIR/pipelines.json

# Compress and encrypt
tar -czf $BACKUP_DIR.tar.gz $BACKUP_DIR
gpg --encrypt --recipient backup@example.com $BACKUP_DIR.tar.gz
```

### 3.5 Backup Verification

| Check | Frequency | Method | Owner |
|-------|-----------|--------|-------|
| Backup completion | Daily | Automated | System |
| Backup integrity | Weekly | Checksum | System |
| Restore test (sample) | Monthly | Manual | Ops |
| Full DR test | Quarterly | Manual | DR Team |

---

## 4. Recovery Procedures

### 4.1 Emergency Response Flowchart

```
┌─────────────────┐
│ Incident        │
│ Detected        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│ Assess Impact   │────▶│ Level 1-2?      │
└────────┬────────┘     └────────┬────────┘
         │                       │
         │ No                    │ Yes
         ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│ Standard        │     │ Activate DR     │
│ Incident        │     │ Team            │
│ Response        │     └────────┬────────┘
└─────────────────┘              │
                                 ▼
                        ┌─────────────────┐
                        │ Execute DR      │
                        │ Runbook         │
                        └────────┬────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │ Verify          │
                        │ Recovery        │
                        └────────┬────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │ Resume Normal   │
                        │ Operations      │
                        └─────────────────┘
```

### 4.2 AEM Repository Restoration

**Step 1: Assess and Prepare**
```bash
# Check available restore points
aio cloudmanager:environment:list-restore-points \
  --environment-id $ENV_ID \
  --program-id $PROGRAM_ID

# Verify backup integrity
aio cloudmanager:environment:check-backup \
  --backup-id $BACKUP_ID \
  --program-id $PROGRAM_ID
```

**Step 2: Initiate Restoration**
```bash
# Create restoration request
aio cloudmanager:environment:restore \
  --environment-id $ENV_ID \
  --program-id $PROGRAM_ID \
  --restore-point "2024-01-15T10:00:00Z" \
  --components "repository"

# Monitor restoration progress
aio cloudmanager:environment:get-restoration-status \
  --environment-id $ENV_ID \
  --program-id $PROGRAM_ID
```

**Step 3: Verification**
```bash
# Verify AEM health
curl -s https://author-p$PROGRAM_ID-e$ENV_ID.adobeaemcloud.com/libs/granite/core/content/login.html

# Verify content integrity
curl -s https://author-p$PROGRAM_ID-e$ENV_ID.adobeaemcloud.com/content/bmad-showcase.json

# Run smoke tests
npm run test:smoke -- --env=restored
```

### 4.3 Content Restoration Procedures

**Restore Specific Content Path:**
```java
// Content restoration utility
public class ContentRestoreService {

    public void restoreContent(String path, String backupTimestamp) {
        LOG.info("Restoring content at {} from {}", path, backupTimestamp);

        // 1. Create backup of current state
        createBackup(path, "pre-restore");

        // 2. Fetch content from backup
        Resource backupContent = getBackupContent(path, backupTimestamp);

        // 3. Restore content
        try (ResourceResolver resolver = getServiceResolver()) {
            Resource target = resolver.getResource(path);

            // Remove existing content
            if (target != null) {
                resolver.delete(target);
            }

            // Restore from backup
            copyContent(backupContent, path, resolver);

            resolver.commit();
            LOG.info("Content restored successfully at {}", path);

        } catch (Exception e) {
            LOG.error("Content restoration failed", e);
            // Rollback to pre-restore backup
            restoreContent(path, "pre-restore");
            throw new RestoreException("Failed to restore content", e);
        }
    }
}
```

### 4.4 Configuration Restoration

**Step 1: Restore OSGi Configurations**
```bash
# Apply backed-up configurations
aio cloudmanager:environment:set-configurations \
  --environment-id $ENV_ID \
  --program-id $PROGRAM_ID \
  --file backup/osgi-configs.json

# Trigger deployment to apply
aio cloudmanager:pipeline:start $PIPELINE_ID \
  --program-id $PROGRAM_ID
```

**Step 2: Restore Environment Variables**
```bash
# Apply backed-up variables
cat backup/env-vars.json | jq -r '.[] | "--variable \(.name) \(.value)"' | \
  xargs aio cloudmanager:environment:set-variables \
    --environment-id $ENV_ID \
    --program-id $PROGRAM_ID
```

### 4.5 Third-Party Service Failover

**LLM Service Failover:**
```java
@Component(service = LLMService.class)
public class ResilientLLMService implements LLMService {

    private final List<LLMProvider> providers = Arrays.asList(
        new OpenAIProvider(),
        new ClaudeProvider(),
        new GeminiProvider()
    );

    @Override
    public String generateContent(String prompt) {
        for (LLMProvider provider : providers) {
            try {
                if (provider.isHealthy()) {
                    return provider.generate(prompt);
                }
            } catch (ServiceException e) {
                LOG.warn("Provider {} failed, trying next", provider.getName());
                markProviderUnhealthy(provider);
            }
        }

        // All providers failed - graceful degradation
        LOG.error("All LLM providers unavailable");
        throw new ServiceUnavailableException(
            "AI content generation temporarily unavailable");
    }
}
```

---

## 5. Failover Architecture

### 5.1 Multi-Region Architecture

```
                    ┌─────────────────┐
                    │   Global DNS    │
                    │  (Route 53)     │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
     ┌────────────┐  ┌────────────┐  ┌────────────┐
     │  CDN Edge  │  │  CDN Edge  │  │  CDN Edge  │
     │  (US)      │  │  (EU)      │  │  (APAC)    │
     └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
           │               │               │
           └───────────────┼───────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
     ┌─────────────────┐      ┌─────────────────┐
     │ Primary Region  │      │ DR Region       │
     │ (AEM Cloud)     │◀────▶│ (Standby)       │
     │ Author/Publish  │      │ Read-only       │
     └─────────────────┘      └─────────────────┘
```

### 5.2 Failover Triggers

| Trigger | Threshold | Action | Automatic |
|---------|-----------|--------|-----------|
| Health check failures | 3 consecutive | Alert DR team | No |
| Error rate | > 50% for 5 min | Automatic failover | Yes |
| Response time | > 10s for 10 min | Traffic reduction | Yes |
| Region unavailable | Confirmed outage | Manual failover | No |

### 5.3 DNS Failover Configuration

```yaml
# Route 53 Health Check Configuration
HealthCheck:
  Type: HTTPS
  ResourcePath: /health
  FullyQualifiedDomainName: www.example.com
  Port: 443
  RequestInterval: 10
  FailureThreshold: 3
  MeasureLatency: true
  Regions:
    - us-east-1
    - eu-west-1
    - ap-southeast-1

# Failover Record Set
RecordSet:
  Name: www.example.com
  Type: A
  SetIdentifier: Primary
  Failover: PRIMARY
  HealthCheckId: !Ref PrimaryHealthCheck
  AliasTarget:
    DNSName: primary.cdn.example.com
    EvaluateTargetHealth: true

RecordSet:
  Name: www.example.com
  Type: A
  SetIdentifier: Secondary
  Failover: SECONDARY
  AliasTarget:
    DNSName: secondary.cdn.example.com
    EvaluateTargetHealth: false
```

### 5.4 Manual Failover Procedure

**Step 1: Confirm Outage**
```bash
# Check primary region health
curl -I https://primary.example.com/health
curl -I https://primary.example.com/content/bmad-showcase/en.html

# Confirm with monitoring
check_monitoring_dashboard
check_adobe_status_page
```

**Step 2: Activate DR Region**
```bash
# Sync latest data to DR region (if possible)
aio cloudmanager:environment:sync \
  --source-env $PRIMARY_ENV \
  --target-env $DR_ENV \
  --program-id $PROGRAM_ID

# Start DR instances
aio cloudmanager:environment:start \
  --environment-id $DR_ENV \
  --program-id $PROGRAM_ID
```

**Step 3: Update DNS**
```bash
# Update Route 53 to point to DR
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE \
  --change-batch file://dns-failover.json

# Verify DNS propagation
dig www.example.com
```

**Step 4: Verify Failover**
```bash
# Run smoke tests against DR
npm run test:smoke -- --env=dr

# Monitor DR metrics
watch -n 30 'curl -s https://www.example.com/health | jq'
```

---

## 6. Communication Plan

### 6.1 Notification Matrix

| Audience | Channel | Trigger | Responsible |
|----------|---------|---------|-------------|
| DR Team | PagerDuty | Any Level 1-2 | Monitoring |
| IT Management | Phone + Slack | Level 1 confirmed | Incident Commander |
| Executive Team | Email + Call | Level 1, > 30 min | IT Management |
| Customers | Status page | Any customer impact | Communications |
| Partners | Email | Extended outage | Account Team |

### 6.2 Communication Templates

**Internal Notification:**
```
🚨 DR ACTIVATED - [Severity Level]

Incident: [Brief description]
Time Detected: [Timestamp]
Impact: [Scope and user impact]
Current Status: [Investigating/Mitigating/Resolved]

DR Team Bridge: [Conference details]
Incident Commander: [Name]

Actions Taken:
- [Action 1]
- [Action 2]

Next Update: [Timestamp + 30 min]
```

**Status Page Update:**
```
[Investigating] Website Performance Issues

We are currently investigating reports of degraded website
performance affecting [region/feature].

Impact: Some users may experience slower page load times or
        intermittent errors.

Current Status: Our team is actively working to resolve this
                issue. We have activated our disaster recovery
                procedures.

Next Update: We will provide an update within 30 minutes.

Last Updated: [Timestamp]
```

### 6.3 Escalation Path

```
Level 1 (0-15 min):
On-Call Engineer → Platform Team Lead

Level 2 (15-30 min):
Platform Team Lead → IT Director

Level 3 (30-60 min):
IT Director → CTO

Level 4 (60+ min):
CTO → CEO (for customer/media communication)
```

---

## 7. Testing and Validation

### 7.1 DR Test Schedule

| Test Type | Frequency | Duration | Scope |
|-----------|-----------|----------|-------|
| **Tabletop Exercise** | Monthly | 2 hours | Process review |
| **Component Test** | Monthly | 4 hours | Single component |
| **Partial Failover** | Quarterly | 8 hours | Non-prod environment |
| **Full DR Test** | Annually | 1-2 days | Production failover |

### 7.2 DR Test Scenarios

**Scenario 1: Repository Restore Test**
```
Objective: Validate content restoration from backup
Duration: 4 hours
Environment: Stage

Steps:
1. Create test content in Stage
2. Take baseline backup
3. Modify/delete test content
4. Initiate restoration
5. Verify content integrity
6. Document results

Success Criteria:
- Restoration completes within RTO
- Content matches baseline
- No data loss beyond RPO
```

**Scenario 2: Regional Failover Test**
```
Objective: Validate DR region activation
Duration: 8 hours
Environment: Production (during maintenance window)

Steps:
1. Announce maintenance window
2. Simulate primary region failure
3. Activate DR procedures
4. Verify DR region serves traffic
5. Run acceptance tests
6. Fail back to primary
7. Verify normal operations

Success Criteria:
- Failover completes within RTO
- No data loss
- All critical functions operational
```

### 7.3 Test Checklist

**Pre-Test:**
- [ ] Notify stakeholders
- [ ] Document current system state
- [ ] Verify backup availability
- [ ] Prepare rollback plan
- [ ] Assign roles and responsibilities

**During Test:**
- [ ] Follow runbook procedures
- [ ] Document deviations
- [ ] Capture timing metrics
- [ ] Note issues and blockers
- [ ] Verify success criteria

**Post-Test:**
- [ ] Restore normal operations
- [ ] Verify system stability
- [ ] Document lessons learned
- [ ] Update runbooks as needed
- [ ] Schedule follow-up actions

### 7.4 DR Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Actual RTO | ≤ Defined RTO | Time to recovery |
| Actual RPO | ≤ Defined RPO | Data loss measurement |
| Runbook Accuracy | 100% | Steps matching actual |
| Team Response | < 15 min | Time to assemble |
| Communication | All notified | Notification audit |

---

## 8. Roles and Responsibilities

### 8.1 DR Team Structure

```
                    ┌─────────────────┐
                    │    Incident     │
                    │   Commander     │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  Technical    │   │Communications │   │   Business    │
│     Lead      │   │     Lead      │   │    Lead       │
└───────┬───────┘   └───────────────┘   └───────────────┘
        │
   ┌────┴────┐
   │         │
   ▼         ▼
┌──────┐  ┌──────┐
│ Ops  │  │ Dev  │
│ Team │  │ Team │
└──────┘  └──────┘
```

### 8.2 Role Definitions

| Role | Responsibilities | Primary Contact |
|------|-----------------|-----------------|
| **Incident Commander** | Overall coordination, decisions | [Name] |
| **Technical Lead** | Technical recovery execution | [Name] |
| **Communications Lead** | Stakeholder updates | [Name] |
| **Business Lead** | Business impact assessment | [Name] |
| **Operations Team** | System recovery tasks | [Team] |
| **Development Team** | Code/config fixes | [Team] |

### 8.3 Contact List

| Role | Name | Phone | Email | Backup |
|------|------|-------|-------|--------|
| Incident Commander | [Primary] | [Phone] | [Email] | [Backup] |
| Technical Lead | [Primary] | [Phone] | [Email] | [Backup] |
| Operations Lead | [Primary] | [Phone] | [Email] | [Backup] |
| Development Lead | [Primary] | [Phone] | [Email] | [Backup] |
| Communications | [Primary] | [Phone] | [Email] | [Backup] |

### 8.4 Vendor Contacts

| Vendor | Support Level | Contact | Escalation |
|--------|--------------|---------|------------|
| Adobe | Enterprise | [Portal/Phone] | [TAM Name] |
| CDN Provider | Premium | [Phone] | [Account Manager] |
| LLM Provider | Business | [Email] | [Support Portal] |
| DNS Provider | Premium | [Phone] | [Account Manager] |

---

## Appendix A: Quick Reference

**Emergency Numbers:**
- DR Hotline: [Number]
- Adobe Emergency Support: [Number]
- IT On-Call: [Number]

**Key URLs:**
- Status Page: status.example.com
- DR Dashboard: [Internal URL]
- Adobe Status: status.adobe.com
- Runbooks: [Wiki URL]

**Critical Commands:**
```bash
# Check AEM health
curl https://author.example.com/libs/granite/core/content/login.html

# Initiate Cloud Manager restore
aio cloudmanager:environment:restore --environment-id $ENV_ID

# Update DNS failover
aws route53 change-resource-record-sets --hosted-zone-id $ZONE

# Page DR team
pagerduty trigger --service dr-team --severity critical
```

---

## Appendix B: Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | DR Team | Initial version |

**Review Cycle:** Semi-annually
**Next Review:** [Current Date + 6 months]
**Owner:** IT Operations

---

## Appendix C: Related Documents

- [Operational Runbooks](./01-operational-runbooks.md)
- [Monitoring and Logging Strategy](./02-monitoring-logging-strategy.md)
- [Environment Configuration Guide](./03-environment-configuration-guide.md)
- [Security Hardening Checklist](./04-security-hardening-checklist.md)
- [Incident Response Plan](./01-operational-runbooks.md#3-incident-response)
