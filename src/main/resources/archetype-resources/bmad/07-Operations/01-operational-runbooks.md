# Operational Runbooks

## Overview

This document provides standard operating procedures (SOPs) for managing the AEM BMAD Showcase application in production environments. These runbooks cover common operational tasks, troubleshooting procedures, and incident response guidelines.

---

## Table of Contents

1. [Daily Operations](#1-daily-operations)
2. [Deployment Procedures](#2-deployment-procedures)
3. [Incident Response](#3-incident-response)
4. [Troubleshooting Guide](#4-troubleshooting-guide)
5. [Maintenance Procedures](#5-maintenance-procedures)
6. [Escalation Matrix](#6-escalation-matrix)

---

## 1. Daily Operations

### 1.1 Health Check Procedure

**Frequency:** Every 4 hours or as automated

**Steps:**
1. Verify AEM Author instance availability
   ```bash
   curl -s -o /dev/null -w "%{http_code}" https://author.example.com/libs/granite/core/content/login.html
   # Expected: 200
   ```

2. Verify AEM Publish instance(s) availability
   ```bash
   curl -s -o /dev/null -w "%{http_code}" https://publish.example.com/content/bmad-showcase/us/en.html
   # Expected: 200
   ```

3. Check Dispatcher cache status
   ```bash
   curl -I https://www.example.com/content/bmad-showcase/us/en.html | grep "X-Dispatcher"
   # Expected: X-Dispatcher: dispatcher1
   ```

4. Verify replication queue status
   - Navigate to: Author > Tools > Deployment > Distribution
   - Confirm no blocked agents
   - Check queue depth < 100 items

### 1.2 Log Review Checklist

**Frequency:** Daily

| Log Location | What to Check | Alert Threshold |
|-------------|---------------|-----------------|
| `error.log` | Exception counts | > 50/hour |
| `request.log` | 5xx responses | > 1% of requests |
| `access.log` | Unusual traffic patterns | 3x normal volume |
| `replication.log` | Failed replications | Any failures |
| `dispatcher.log` | Cache invalidations | > 1000/hour |

### 1.3 Content Publishing Verification

**Steps:**
1. Create test content on Author
2. Publish test content
3. Verify on Publish within 60 seconds
4. Verify Dispatcher cache invalidation
5. Delete test content

---

## 2. Deployment Procedures

### 2.1 Standard Deployment (Cloud Manager)

**Pre-Deployment Checklist:**
- [ ] Code merged to deployment branch
- [ ] All tests passing in CI
- [ ] Change ticket approved
- [ ] Rollback plan documented
- [ ] Stakeholders notified

**Deployment Steps:**

1. **Initiate Pipeline**
   ```bash
   # Using Cloud Manager CLI
   aio cloudmanager:pipeline:start <pipeline-id> --program-id <program-id>
   ```

2. **Monitor Pipeline Stages**
   - Build & Unit Test (10-15 mins)
   - Code Quality Scan (5-10 mins)
   - Deploy to Stage (15-20 mins)
   - Stage Testing (manual/automated)
   - Deploy to Production (15-20 mins)

3. **Post-Deployment Verification**
   - Execute smoke tests
   - Verify component rendering
   - Check replication status
   - Monitor error rates for 30 minutes

### 2.2 Emergency Hotfix Procedure

**Criteria for Emergency Deployment:**
- Production P1 incident
- Security vulnerability
- Data corruption risk

**Expedited Steps:**
1. Create hotfix branch from production tag
2. Apply minimal fix
3. Peer review (minimum 1 reviewer)
4. Deploy to Stage for smoke test
5. Deploy to Production with approval
6. Monitor for 1 hour

### 2.3 Rollback Procedure

**When to Rollback:**
- Error rate > 5% post-deployment
- Critical functionality broken
- Performance degradation > 30%

**Steps:**
1. **Identify last known good version**
   ```bash
   aio cloudmanager:pipeline:list-executions <pipeline-id> --program-id <program-id>
   ```

2. **Initiate rollback**
   ```bash
   aio cloudmanager:pipeline:create-execution <pipeline-id> \
     --program-id <program-id> \
     --mode ROLL_BACK
   ```

3. **Verify rollback success**
4. **Document incident for post-mortem**

---

## 3. Incident Response

### 3.1 Incident Severity Levels

| Level | Description | Response Time | Examples |
|-------|-------------|---------------|----------|
| P1 | Critical - Site down | 15 minutes | Complete outage, data breach |
| P2 | High - Major feature broken | 1 hour | Checkout broken, login failing |
| P3 | Medium - Feature degraded | 4 hours | Slow performance, minor bugs |
| P4 | Low - Minor issue | 24 hours | UI glitches, typos |

### 3.2 P1 Incident Response Playbook

**Step 1: Acknowledge (0-5 mins)**
- Acknowledge alert
- Join incident channel/bridge
- Assign Incident Commander

**Step 2: Assess (5-15 mins)**
- Determine blast radius
- Identify affected systems
- Check recent changes

**Step 3: Mitigate (15-60 mins)**
- Apply temporary fix or rollback
- Communicate status to stakeholders
- Document actions taken

**Step 4: Resolve (1-4 hours)**
- Implement permanent fix
- Verify fix in production
- Close incident

**Step 5: Post-Mortem (24-48 hours)**
- Schedule blameless post-mortem
- Document timeline
- Identify action items

### 3.3 Communication Templates

**Initial Incident Notification:**
```
INCIDENT: [P1/P2/P3/P4] - [Brief Description]
TIME DETECTED: [Timestamp]
IMPACT: [User impact description]
STATUS: Investigating
NEXT UPDATE: [Timestamp + 30 mins]
```

**Status Update:**
```
INCIDENT UPDATE: [Brief Description]
CURRENT STATUS: [Investigating/Mitigating/Resolved]
ACTIONS TAKEN: [List of actions]
NEXT STEPS: [Planned actions]
NEXT UPDATE: [Timestamp]
```

---

## 4. Troubleshooting Guide

### 4.1 AEM Author Issues

#### Author Instance Unresponsive

**Symptoms:**
- Login page not loading
- 502/503 errors
- Slow response times

**Diagnosis:**
```bash
# Check JVM health
curl -u admin:admin http://localhost:4502/system/console/jmx/java.lang:type=Memory

# Check bundle status
curl -u admin:admin http://localhost:4502/system/console/bundles.json | jq '.s'
# Expected: [total, active, fragment, resolved, installed]

# Check repository health
curl -u admin:admin http://localhost:4502/system/console/jmx/org.apache.jackrabbit.oak:name=Repository,type=Repository
```

**Resolution Steps:**
1. Check available memory and disk space
2. Review error.log for exceptions
3. Verify all bundles are active
4. Restart instance if necessary

#### Replication Queue Blocked

**Symptoms:**
- Content not appearing on Publish
- Queue depth increasing
- Agent status shows "blocked"

**Diagnosis:**
```bash
# Check replication agent status
curl -u admin:admin http://localhost:4502/etc/replication/agents.author/publish.json
```

**Resolution Steps:**
1. Check Publish instance health
2. Clear blocked items if corrupted
3. Restart replication agent
4. Verify network connectivity

### 4.2 AEM Publish Issues

#### High Response Times

**Symptoms:**
- Page load time > 3 seconds
- Increased 504 timeouts

**Diagnosis:**
```bash
# Check Sling request processing
curl -u admin:admin http://localhost:4503/system/console/status-slingrequests.txt

# Check active queries
curl -u admin:admin http://localhost:4503/system/console/jmx/org.apache.jackrabbit.oak:name=Oak%20Query%20Statistics,type=QueryStats
```

**Resolution Steps:**
1. Check for slow queries in logs
2. Verify Dispatcher cache hit rate
3. Review recent content changes
4. Scale horizontally if load-related

### 4.3 Dispatcher Issues

#### Low Cache Hit Rate

**Symptoms:**
- Cache hit rate < 80%
- High origin load
- Slow response times

**Diagnosis:**
```bash
# Check cache statistics
grep -c "cache-action=HIT" /var/log/httpd/dispatcher.log
grep -c "cache-action=MISS" /var/log/httpd/dispatcher.log
```

**Resolution Steps:**
1. Review dispatcher.any rules
2. Check for excessive cache invalidations
3. Verify TTL configurations
4. Review query string handling

### 4.4 Integration Issues

#### LLM Service Failures

**Symptoms:**
- AI content generation failing
- Timeout errors in logs
- Empty responses

**Diagnosis:**
```bash
# Check LLM service status
curl -u admin:admin http://localhost:4502/system/console/configMgr/com.example.aem.bmad.core.services.impl.LLMServiceImpl

# Review service logs
grep "LLMService" /opt/aem/crx-quickstart/logs/error.log
```

**Resolution Steps:**
1. Verify API key validity
2. Check rate limits
3. Test connectivity to LLM provider
4. Verify OSGi configuration

---

## 5. Maintenance Procedures

### 5.1 Scheduled Maintenance Window

**Standard Maintenance Window:**
- Day: Saturday
- Time: 02:00-06:00 UTC
- Notification: 48 hours in advance

**Maintenance Checklist:**
- [ ] Notify stakeholders
- [ ] Create content freeze window
- [ ] Backup configurations
- [ ] Execute maintenance tasks
- [ ] Verify system health
- [ ] Resume normal operations

### 5.2 Repository Maintenance

**Frequency:** Weekly

**Online Compaction:**
```bash
# Trigger via JMX
curl -u admin:admin -X POST \
  http://localhost:4502/system/console/jmx/org.apache.jackrabbit.oak:name=Segment%20node%20store%20blob%20gc,type=BlobGarbageCollection/op/startBlobGC
```

**Datastore Garbage Collection:**
```bash
# Trigger via JMX
curl -u admin:admin -X POST \
  http://localhost:4502/system/console/jmx/org.apache.jackrabbit.oak:name=Segment%20node%20store%20revision%20gc,type=RevisionGarbageCollection/op/startRevisionGC
```

### 5.3 Log Rotation and Archival

**Retention Policy:**
| Log Type | Local Retention | Archive Retention |
|----------|-----------------|-------------------|
| error.log | 7 days | 90 days |
| request.log | 3 days | 30 days |
| access.log | 7 days | 90 days |
| audit.log | 30 days | 1 year |

### 5.4 Certificate Renewal

**Certificate Locations:**
- CDN certificates: Managed by Fastly/CloudFlare
- Origin certificates: Cloud Manager
- Internal certificates: AEM Trust Store

**Renewal Process:**
1. Generate new CSR 30 days before expiry
2. Submit to certificate authority
3. Test new certificate in Stage
4. Deploy to Production
5. Verify SSL handshake

---

## 6. Escalation Matrix

### 6.1 On-Call Rotation

| Week | Primary | Secondary | Manager |
|------|---------|-----------|---------|
| 1 | Team A | Team B | Manager 1 |
| 2 | Team B | Team C | Manager 2 |
| 3 | Team C | Team A | Manager 1 |
| 4 | Team A | Team B | Manager 2 |

### 6.2 Escalation Path

```
L1 Support (0-15 mins)
    ↓
L2 On-Call Engineer (15-30 mins)
    ↓
L3 Platform Team (30-60 mins)
    ↓
Engineering Manager (60+ mins)
    ↓
VP Engineering (Critical only)
```

### 6.3 External Escalation Contacts

| Vendor | Contact | When to Escalate |
|--------|---------|------------------|
| Adobe Support | Case Portal | AEM platform issues |
| Cloud Provider | Support Portal | Infrastructure issues |
| CDN Provider | Support Email | Edge delivery issues |
| LLM Provider | API Support | AI service issues |

---

## Appendix A: Quick Reference Commands

```bash
# AEM Health Check
curl -u admin:admin http://localhost:4502/system/console/productinfo

# Bundle Status
curl -u admin:admin http://localhost:4502/system/console/bundles.json

# Replication Status
curl -u admin:admin http://localhost:4502/etc/replication/agents.author.html

# Clear Dispatcher Cache
curl -X POST -H "CQ-Action: Delete" -H "CQ-Handle: /content" https://dispatcher.example.com/dispatcher/invalidate.cache

# Restart AEM
./crx-quickstart/bin/stop && ./crx-quickstart/bin/start

# Thread Dump
jstack <pid> > threaddump_$(date +%Y%m%d_%H%M%S).txt
```

---

## Appendix B: Key URLs

| Environment | Author | Publish | Dispatcher |
|-------------|--------|---------|------------|
| Development | localhost:4502 | localhost:4503 | localhost:8080 |
| Stage | author-stage.example.com | publish-stage.example.com | stage.example.com |
| Production | author.example.com | publish.example.com | www.example.com |

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Ops Team | Initial version |
| 1.1 | 2024-03-01 | Ops Team | Added LLM troubleshooting |

**Review Cycle:** Quarterly
**Next Review:** [Current Date + 3 months]
