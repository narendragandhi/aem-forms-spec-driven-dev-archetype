# Performance Baseline and SLAs

## Overview

This document establishes performance baselines, Service Level Agreements (SLAs), and Service Level Objectives (SLOs) for the AEM BMAD Showcase application. It defines measurable targets, monitoring requirements, and remediation procedures for maintaining optimal performance.

---

## Table of Contents

1. [Performance Objectives](#1-performance-objectives)
2. [Service Level Agreements](#2-service-level-agreements)
3. [Service Level Objectives](#3-service-level-objectives)
4. [Performance Baselines](#4-performance-baselines)
5. [Component Performance Targets](#5-component-performance-targets)
6. [Load Testing Requirements](#6-load-testing-requirements)
7. [Performance Monitoring](#7-performance-monitoring)
8. [Capacity Planning](#8-capacity-planning)

---

## 1. Performance Objectives

### 1.1 Core Performance Goals

| Objective | Target | Measurement |
|-----------|--------|-------------|
| **Page Load Time** | < 3 seconds | Time to Interactive (TTI) |
| **First Contentful Paint** | < 1.5 seconds | FCP metric |
| **Largest Contentful Paint** | < 2.5 seconds | LCP metric |
| **Cumulative Layout Shift** | < 0.1 | CLS score |
| **Time to First Byte** | < 200ms | Server response time |
| **API Response Time** | < 500ms | 95th percentile |

### 1.2 Business Impact Mapping

| Performance Level | User Impact | Business Impact |
|------------------|-------------|-----------------|
| Excellent (< 1s) | Seamless experience | High conversion |
| Good (1-3s) | Acceptable | Normal conversion |
| Fair (3-5s) | Noticeable delay | 10-20% drop |
| Poor (5-10s) | Frustration | 30-50% drop |
| Critical (> 10s) | Abandonment | 70%+ drop |

---

## 2. Service Level Agreements

### 2.1 Availability SLA

| Service | Target | Measurement Window | Exclusions |
|---------|--------|-------------------|------------|
| **Public Website** | 99.9% | Monthly | Scheduled maintenance |
| **Author Environment** | 99.5% | Monthly | Maintenance windows |
| **CDN/Edge** | 99.99% | Monthly | Provider SLA |
| **API Services** | 99.5% | Monthly | Scheduled maintenance |

**Availability Calculation:**
```
Availability % = (Total Minutes - Downtime Minutes) / Total Minutes × 100

Monthly Targets:
- 99.9% = max 43.2 minutes downtime/month
- 99.5% = max 3.6 hours downtime/month
- 99.99% = max 4.32 minutes downtime/month
```

### 2.2 Performance SLA

| Metric | Target | Threshold | Measurement |
|--------|--------|-----------|-------------|
| Page Load (p50) | < 2s | < 3s | Synthetic monitoring |
| Page Load (p95) | < 4s | < 6s | RUM data |
| API Response (p50) | < 200ms | < 500ms | Server logs |
| API Response (p95) | < 500ms | < 1000ms | Server logs |
| Error Rate | < 0.1% | < 1% | Request logs |

### 2.3 SLA Credit Table

| Availability | Credit |
|-------------|--------|
| 99.0% - 99.9% | 10% |
| 95.0% - 99.0% | 25% |
| 90.0% - 95.0% | 50% |
| < 90.0% | 100% |

---

## 3. Service Level Objectives

### 3.1 SLO Definitions

| SLO | Target | Error Budget | Window |
|-----|--------|--------------|--------|
| **Website Availability** | 99.9% | 0.1% (43 min/month) | Rolling 30 days |
| **Page Performance** | 95% requests < 3s | 5% slow requests | Rolling 7 days |
| **API Performance** | 99% requests < 500ms | 1% slow requests | Rolling 7 days |
| **Error Rate** | < 0.5% of requests | 0.5% errors | Rolling 24 hours |
| **Successful Deploys** | 95% first-attempt | 5% rollbacks | Per quarter |

### 3.2 SLO Burn Rate Alerts

| Alert | Burn Rate | Time Window | Action |
|-------|-----------|-------------|--------|
| Critical | 14.4x | 1 hour | Page on-call |
| High | 6x | 6 hours | Slack alert |
| Medium | 3x | 24 hours | Email team |
| Low | 1x | 3 days | Review weekly |

**Burn Rate Formula:**
```
Burn Rate = (Error Rate × Time Window) / (Error Budget × SLO Window)

Example:
- SLO: 99.9% availability over 30 days
- Error Budget: 0.1% = 43.2 minutes
- If 6 minutes downtime in 1 hour:
  Burn Rate = (6/60) / (43.2/43200) = 0.1 / 0.001 = 100x
```

### 3.3 Error Budget Policy

**Error Budget Remaining > 50%:**
- Normal development velocity
- Deploy to production as planned
- Focus on feature development

**Error Budget Remaining 25-50%:**
- Reduce deployment frequency
- Prioritize reliability work
- Root cause analysis required

**Error Budget Remaining < 25%:**
- Feature freeze
- Focus entirely on reliability
- Incident review mandatory

**Error Budget Exhausted:**
- Emergency reliability work only
- No feature deployments
- Executive escalation

---

## 4. Performance Baselines

### 4.1 Page Performance Baselines

| Page Type | TTFB | FCP | LCP | TTI | CLS |
|-----------|------|-----|-----|-----|-----|
| Homepage | 150ms | 800ms | 1.8s | 2.5s | 0.05 |
| Content Page | 120ms | 700ms | 1.5s | 2.2s | 0.03 |
| Product Detail | 180ms | 900ms | 2.0s | 2.8s | 0.08 |
| Search Results | 200ms | 1.0s | 2.2s | 3.0s | 0.10 |
| Landing Page | 160ms | 850ms | 1.9s | 2.6s | 0.04 |

### 4.2 API Performance Baselines

| Endpoint | p50 | p90 | p99 | Max |
|----------|-----|-----|-----|-----|
| `/content/*.model.json` | 50ms | 120ms | 300ms | 1s |
| `/api/search` | 100ms | 250ms | 500ms | 2s |
| `/api/llm/generate` | 1s | 3s | 8s | 30s |
| `/api/translate` | 500ms | 1.5s | 4s | 15s |
| `/bin/replication` | 200ms | 500ms | 1s | 5s |

### 4.3 Infrastructure Baselines

| Resource | Normal | Warning | Critical |
|----------|--------|---------|----------|
| CPU Usage | < 60% | 60-80% | > 80% |
| Memory Usage | < 70% | 70-85% | > 85% |
| JVM Heap | < 75% | 75-90% | > 90% |
| Disk Usage | < 70% | 70-85% | > 85% |
| GC Pause | < 200ms | 200-500ms | > 500ms |
| Thread Count | < 500 | 500-800 | > 800 |

---

## 5. Component Performance Targets

### 5.1 AEM Component Render Times

| Component | Target | Threshold | Measurement |
|-----------|--------|-----------|-------------|
| Hero | < 50ms | < 100ms | Server render |
| Navigation | < 30ms | < 75ms | Server render |
| Card | < 20ms | < 50ms | Server render |
| Carousel | < 80ms | < 150ms | Server + client |
| Card Grid | < 100ms | < 200ms | Server render |
| Search | < 300ms | < 500ms | Full query |

### 5.2 Component Optimization Guidelines

**Hero Component:**
```
Target: < 50ms render time
Optimizations:
- Lazy load background images
- Use picture element with srcset
- Preload critical assets
- Minimize DOM elements
```

**Carousel Component:**
```
Target: < 80ms render time
Optimizations:
- Load only visible slides initially
- Use CSS transforms for animations
- Implement virtual scrolling for many items
- Defer non-critical JavaScript
```

### 5.3 Client-side Performance Budgets

| Resource Type | Budget | Current | Status |
|--------------|--------|---------|--------|
| Total JS | 200 KB | [Measure] | [ ] |
| Total CSS | 50 KB | [Measure] | [ ] |
| Total Images | 500 KB | [Measure] | [ ] |
| Total Fonts | 100 KB | [Measure] | [ ] |
| DOM Elements | 1500 | [Measure] | [ ] |
| Third-party | 100 KB | [Measure] | [ ] |

---

## 6. Load Testing Requirements

### 6.1 Load Testing Profiles

**Baseline Test:**
```yaml
Profile: Baseline
Users: 100 concurrent
Duration: 30 minutes
Ramp-up: 5 minutes
Think Time: 3-5 seconds
Purpose: Establish performance baseline
```

**Stress Test:**
```yaml
Profile: Stress
Users: 500 concurrent
Duration: 60 minutes
Ramp-up: 15 minutes
Think Time: 2-3 seconds
Purpose: Identify breaking points
```

**Spike Test:**
```yaml
Profile: Spike
Users: 1000 concurrent (spike)
Duration: 10 minutes spike
Ramp-up: Immediate
Purpose: Test auto-scaling response
```

**Endurance Test:**
```yaml
Profile: Endurance
Users: 200 concurrent
Duration: 8 hours
Ramp-up: 30 minutes
Purpose: Identify memory leaks, degradation
```

### 6.2 Load Test Scenarios

| Scenario | Weight | Actions |
|----------|--------|---------|
| Browse Homepage | 30% | Load home, view hero, scroll |
| Browse Content | 40% | Navigate pages, read content |
| Search | 15% | Search query, view results |
| AI Content | 10% | Generate AI content |
| Author Edit | 5% | Edit page (Author only) |

### 6.3 Load Testing Schedule

| Test Type | Frequency | Environment | Automation |
|-----------|-----------|-------------|------------|
| Baseline | Weekly | Stage | CI/CD |
| Stress | Monthly | Stage | Scheduled |
| Spike | Quarterly | Stage | Manual |
| Endurance | Quarterly | Stage | Scheduled |
| Production Validation | Per release | Production | Manual |

### 6.4 Pass/Fail Criteria

| Metric | Pass | Marginal | Fail |
|--------|------|----------|------|
| p95 Response Time | < 3s | 3-5s | > 5s |
| Error Rate | < 1% | 1-3% | > 3% |
| Throughput Variance | < 10% | 10-20% | > 20% |
| CPU Peak | < 80% | 80-90% | > 90% |
| Memory Peak | < 85% | 85-95% | > 95% |

---

## 7. Performance Monitoring

### 7.1 Key Performance Indicators

**Real User Monitoring (RUM):**
```javascript
// Performance monitoring script
const observer = new PerformanceObserver((list) => {
    for (const entry of list.getEntries()) {
        sendToAnalytics({
            metric: entry.name,
            value: entry.value,
            page: window.location.pathname
        });
    }
});

observer.observe({ entryTypes: ['largest-contentful-paint', 'first-input', 'layout-shift'] });
```

### 7.2 Synthetic Monitoring Configuration

| Check | Frequency | Locations | Alert |
|-------|-----------|-----------|-------|
| Homepage Load | 1 minute | 5 regions | p95 > 3s |
| API Health | 30 seconds | 3 regions | 2 failures |
| SSL Certificate | Daily | 1 region | < 30 days |
| DNS Resolution | 5 minutes | 5 regions | > 100ms |

### 7.3 Performance Dashboard Panels

| Panel | Metrics | Visualization |
|-------|---------|---------------|
| Availability | Uptime %, Errors | Stat, Time series |
| Response Time | p50, p95, p99 | Time series |
| Throughput | Requests/sec | Time series |
| Error Rate | % errors by type | Time series |
| Core Web Vitals | LCP, FID, CLS | Gauges |
| Resource Usage | CPU, Memory, Disk | Time series |

### 7.4 Performance Alerts

```yaml
# Prometheus Alert Rules
groups:
  - name: performance
    rules:
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High p95 response time"
          runbook: "https://wiki/runbooks/high-latency"

      - alert: LowCacheHitRate
        expr: dispatcher_cache_hit_ratio < 0.7
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "Dispatcher cache hit rate below 70%"

      - alert: HighGCPause
        expr: jvm_gc_pause_seconds_max > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM GC pause exceeds 500ms"
```

---

## 8. Capacity Planning

### 8.1 Capacity Model

| Traffic Level | Concurrent Users | Requests/sec | Infrastructure |
|--------------|------------------|--------------|----------------|
| Low | 100 | 50 | 2 Publish |
| Normal | 500 | 250 | 4 Publish |
| High | 1000 | 500 | 6 Publish |
| Peak | 2000 | 1000 | 8 Publish |
| Burst | 5000 | 2500 | 12 Publish |

### 8.2 Scaling Triggers

| Trigger | Threshold | Action | Cooldown |
|---------|-----------|--------|----------|
| CPU > 70% | 5 minutes | Scale up | 10 minutes |
| CPU < 30% | 15 minutes | Scale down | 30 minutes |
| Response > 2s | 5 minutes | Scale up | 10 minutes |
| Queue > 100 | 1 minute | Scale up | 5 minutes |

### 8.3 Resource Projections

| Quarter | Traffic Growth | Resources Needed | Budget Impact |
|---------|---------------|------------------|---------------|
| Q1 | Baseline | 4 Publish | Baseline |
| Q2 | +20% | 5 Publish | +25% |
| Q3 | +40% | 6 Publish | +50% |
| Q4 | +60% | 7 Publish | +75% |

### 8.4 Capacity Review Checklist

**Monthly Review:**
- [ ] Analyze traffic trends
- [ ] Review resource utilization
- [ ] Check cache efficiency
- [ ] Validate SLO compliance
- [ ] Update capacity projections

**Quarterly Review:**
- [ ] Load test updated scenarios
- [ ] Review scaling policies
- [ ] Update cost projections
- [ ] Plan infrastructure changes
- [ ] Benchmark against baseline

---

## Appendix A: Performance Testing Tools

| Tool | Purpose | License |
|------|---------|---------|
| Apache JMeter | Load testing | Open Source |
| Gatling | Performance testing | Open Source |
| k6 | Modern load testing | Open Source |
| Lighthouse | Web performance audit | Open Source |
| WebPageTest | Real browser testing | Free + Paid |
| New Relic | APM | Commercial |

## Appendix B: Core Web Vitals Thresholds

| Metric | Good | Needs Improvement | Poor |
|--------|------|-------------------|------|
| LCP | < 2.5s | 2.5s - 4s | > 4s |
| FID | < 100ms | 100ms - 300ms | > 300ms |
| CLS | < 0.1 | 0.1 - 0.25 | > 0.25 |
| INP | < 200ms | 200ms - 500ms | > 500ms |

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Platform Team | Initial version |

**Review Cycle:** Quarterly
**Next Review:** [Current Date + 3 months]
