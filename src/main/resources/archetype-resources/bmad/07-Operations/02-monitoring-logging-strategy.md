# Monitoring and Logging Strategy

## Overview

This document defines the comprehensive monitoring and logging strategy for the AEM BMAD Showcase application. It covers observability pillars (metrics, logs, traces), alerting thresholds, dashboard designs, and tooling recommendations.

---

## Table of Contents

1. [Observability Architecture](#1-observability-architecture)
2. [Metrics Strategy](#2-metrics-strategy)
3. [Logging Strategy](#3-logging-strategy)
4. [Distributed Tracing](#4-distributed-tracing)
5. [Alerting Strategy](#5-alerting-strategy)
6. [Dashboards](#6-dashboards)
7. [Tooling Recommendations](#7-tooling-recommendations)

---

## 1. Observability Architecture

### 1.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Data Sources                                  │
├─────────────┬─────────────┬─────────────┬─────────────┬────────────────┤
│ AEM Author  │ AEM Publish │ Dispatcher  │ CDN         │ LLM Services   │
└──────┬──────┴──────┬──────┴──────┬──────┴──────┬──────┴───────┬────────┘
       │             │             │             │              │
       ▼             ▼             ▼             ▼              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        Collection Layer                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  Prometheus Exporters  │  Fluentd/Fluent Bit  │  OpenTelemetry Agent   │
└─────────────────────────────────────────────────────────────────────────┘
       │                          │                        │
       ▼                          ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Storage Layer                                   │
├──────────────────┬──────────────────────┬───────────────────────────────┤
│  Prometheus/     │  Elasticsearch/      │  Jaeger/                      │
│  Victoria        │  Loki                │  Tempo                        │
│  Metrics         │  Logs                │  Traces                       │
└──────────────────┴──────────────────────┴───────────────────────────────┘
       │                          │                        │
       └──────────────────────────┼────────────────────────┘
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Visualization & Alerting                           │
├─────────────────────────────┬───────────────────────────────────────────┤
│         Grafana             │           PagerDuty / OpsGenie            │
└─────────────────────────────┴───────────────────────────────────────────┘
```

### 1.2 Design Principles

| Principle | Description |
|-----------|-------------|
| **Centralized** | All observability data flows to central platform |
| **Correlated** | Metrics, logs, and traces linkable via trace ID |
| **Real-time** | Sub-minute latency for critical metrics |
| **Retained** | Appropriate retention per data type |
| **Actionable** | Every alert has a runbook |

---

## 2. Metrics Strategy

### 2.1 Key Metrics Categories

#### Infrastructure Metrics

| Metric | Description | Collection Interval |
|--------|-------------|---------------------|
| `cpu_usage_percent` | CPU utilization | 15s |
| `memory_usage_bytes` | Memory consumption | 15s |
| `disk_usage_percent` | Disk space utilization | 60s |
| `network_io_bytes` | Network throughput | 15s |

#### JVM Metrics (AEM)

| Metric | Description | Collection Interval |
|--------|-------------|---------------------|
| `jvm_heap_used_bytes` | Heap memory usage | 15s |
| `jvm_gc_pause_seconds` | Garbage collection pause | 15s |
| `jvm_threads_current` | Active thread count | 30s |
| `jvm_classes_loaded` | Loaded class count | 60s |

#### Application Metrics

| Metric | Description | Collection Interval |
|--------|-------------|---------------------|
| `aem_request_duration_seconds` | Request latency histogram | 15s |
| `aem_request_total` | Total request count | 15s |
| `aem_error_total` | Error count by type | 15s |
| `aem_active_sessions` | Active user sessions | 30s |

#### Business Metrics

| Metric | Description | Collection Interval |
|--------|-------------|---------------------|
| `content_published_total` | Content publish count | 60s |
| `component_render_total` | Component render count | 15s |
| `llm_request_total` | LLM API calls | 15s |
| `llm_tokens_used` | Token consumption | 60s |

### 2.2 Custom AEM Metrics

**Sling Model Performance:**
```java
@Reference
private MetricService metricService;

@PostConstruct
protected void init() {
    Timer.Sample sample = Timer.start(metricService.getRegistry());
    try {
        // Model initialization logic
    } finally {
        sample.stop(Timer.builder("aem.model.init")
            .tag("model", this.getClass().getSimpleName())
            .register(metricService.getRegistry()));
    }
}
```

**Repository Query Performance:**
```java
private void executeQuery(String query) {
    long start = System.nanoTime();
    try {
        // Execute query
    } finally {
        metricService.counter("aem.query.total")
            .tag("query_type", getQueryType(query))
            .increment();
        metricService.timer("aem.query.duration")
            .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }
}
```

### 2.3 Dispatcher Metrics

| Metric | Source | Description |
|--------|--------|-------------|
| `dispatcher_cache_hit_ratio` | mod_dispatcher | Cache hit percentage |
| `dispatcher_requests_total` | Apache access log | Total requests |
| `dispatcher_backend_connections` | mod_status | Backend connections |
| `dispatcher_queue_length` | mod_dispatcher | Request queue depth |

### 2.4 Metric Retention Policy

| Resolution | Retention | Use Case |
|------------|-----------|----------|
| Raw (15s) | 7 days | Real-time monitoring |
| 1-minute | 30 days | Recent analysis |
| 5-minute | 90 days | Trend analysis |
| 1-hour | 1 year | Capacity planning |
| 1-day | 3 years | Historical reporting |

---

## 3. Logging Strategy

### 3.1 Log Levels

| Level | Usage | Examples |
|-------|-------|----------|
| ERROR | Application errors requiring attention | Exceptions, service failures |
| WARN | Potential issues, degraded behavior | Retry attempts, slow queries |
| INFO | Significant business events | Content published, user actions |
| DEBUG | Development troubleshooting | Method entry/exit, variable values |
| TRACE | Detailed debugging (rarely enabled) | Full request/response bodies |

### 3.2 Structured Logging Format

**Standard Log Schema:**
```json
{
  "timestamp": "2024-01-15T14:32:45.123Z",
  "level": "INFO",
  "logger": "com.example.aem.bmad.core.services.LLMServiceImpl",
  "message": "LLM request completed",
  "traceId": "abc123def456",
  "spanId": "789ghi012",
  "thread": "http-nio-4502-exec-10",
  "context": {
    "userId": "admin",
    "requestPath": "/content/bmad-showcase/en",
    "component": "hero",
    "action": "generateContent"
  },
  "metrics": {
    "duration_ms": 1234,
    "tokens_used": 150
  }
}
```

### 3.3 AEM Log Configuration

**logback.xml customization:**
```xml
<configuration>
    <!-- Structured JSON Appender -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${sling.home}/logs/structured.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${sling.home}/logs/structured.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>

    <!-- Application Logging -->
    <logger name="com.example.aem.bmad" level="INFO" additivity="false">
        <appender-ref ref="JSON_FILE" />
    </logger>

    <!-- Performance Logging -->
    <logger name="com.example.aem.bmad.performance" level="DEBUG" additivity="false">
        <appender-ref ref="JSON_FILE" />
    </logger>
</configuration>
```

### 3.4 Log Files Reference

| Log File | Purpose | Key Events |
|----------|---------|------------|
| `error.log` | Application errors | Exceptions, stack traces |
| `request.log` | HTTP requests | Request/response timing |
| `access.log` | Access audit | User access patterns |
| `replication.log` | Content replication | Publish/activation events |
| `history.log` | Workflow history | Workflow state changes |
| `audit.log` | Security audit | Permission changes, admin actions |
| `structured.log` | Custom structured logs | Application events in JSON |

### 3.5 Log Collection Pipeline

```yaml
# Fluent Bit Configuration
[INPUT]
    Name              tail
    Path              /opt/aem/crx-quickstart/logs/*.log
    Tag               aem.*
    Multiline         On
    Parser            java_multiline

[FILTER]
    Name              parser
    Match             aem.structured
    Key_Name          log
    Parser            json

[FILTER]
    Name              modify
    Match             aem.*
    Add               environment ${ENVIRONMENT}
    Add               cluster ${CLUSTER_NAME}
    Add               instance_id ${INSTANCE_ID}

[OUTPUT]
    Name              es
    Match             aem.*
    Host              elasticsearch.example.com
    Port              9200
    Index             aem-logs-%Y.%m.%d
    Type              _doc
```

### 3.6 Log Retention Policy

| Log Type | Hot Storage | Warm Storage | Cold/Archive |
|----------|-------------|--------------|--------------|
| Error logs | 7 days | 30 days | 90 days |
| Request logs | 3 days | 14 days | 30 days |
| Access logs | 7 days | 30 days | 90 days |
| Audit logs | 30 days | 90 days | 1 year |
| Debug logs | 1 day | 7 days | Not retained |

---

## 4. Distributed Tracing

### 4.1 Trace Context Propagation

**OpenTelemetry Integration:**
```java
@Reference
private Tracer tracer;

public void processRequest(SlingHttpServletRequest request) {
    Span span = tracer.spanBuilder("component.render")
        .setParent(extractContext(request))
        .setAttribute("component.type", "hero")
        .setAttribute("component.path", resource.getPath())
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
        // Process request
    } catch (Exception e) {
        span.setStatus(StatusCode.ERROR);
        span.recordException(e);
        throw e;
    } finally {
        span.end();
    }
}
```

### 4.2 Key Spans

| Span Name | Attributes | Purpose |
|-----------|------------|---------|
| `http.request` | method, path, status | Incoming request |
| `component.render` | type, path | Component rendering |
| `repository.query` | query, duration | JCR queries |
| `llm.request` | provider, model, tokens | LLM API calls |
| `replication.publish` | path, target | Content replication |
| `cache.lookup` | key, hit/miss | Dispatcher cache |

### 4.3 Sampling Strategy

| Environment | Strategy | Rate |
|-------------|----------|------|
| Development | Always On | 100% |
| Stage | Head-based | 10% |
| Production | Adaptive | 1-5% |
| Production (errors) | Always On | 100% |

---

## 5. Alerting Strategy

### 5.1 Alert Severity Levels

| Severity | Response | Examples |
|----------|----------|----------|
| **Critical** | Immediate page | Site down, data loss risk |
| **High** | < 15 min response | Feature broken, high error rate |
| **Medium** | < 1 hour response | Degraded performance |
| **Low** | Next business day | Warning thresholds |

### 5.2 Alert Definitions

#### Critical Alerts

```yaml
# Site Availability
- alert: AEMPublishDown
  expr: probe_success{job="aem-publish"} == 0
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "AEM Publish instance is down"
    runbook: "https://wiki/runbooks/aem-publish-down"

# Error Rate Spike
- alert: HighErrorRate
  expr: rate(aem_error_total[5m]) / rate(aem_request_total[5m]) > 0.05
  for: 3m
  labels:
    severity: critical
  annotations:
    summary: "Error rate exceeds 5%"
    runbook: "https://wiki/runbooks/high-error-rate"
```

#### High Alerts

```yaml
# Response Time
- alert: HighResponseTime
  expr: histogram_quantile(0.95, rate(aem_request_duration_seconds_bucket[5m])) > 3
  for: 5m
  labels:
    severity: high
  annotations:
    summary: "95th percentile response time > 3s"
    runbook: "https://wiki/runbooks/slow-response"

# Replication Queue
- alert: ReplicationQueueBlocked
  expr: aem_replication_queue_depth > 100
  for: 10m
  labels:
    severity: high
  annotations:
    summary: "Replication queue backed up"
    runbook: "https://wiki/runbooks/replication-queue"
```

#### Medium Alerts

```yaml
# JVM Memory
- alert: HighHeapUsage
  expr: jvm_heap_used_bytes / jvm_heap_max_bytes > 0.85
  for: 10m
  labels:
    severity: medium
  annotations:
    summary: "JVM heap usage above 85%"
    runbook: "https://wiki/runbooks/high-heap"

# Cache Hit Rate
- alert: LowCacheHitRate
  expr: dispatcher_cache_hit_ratio < 0.7
  for: 15m
  labels:
    severity: medium
  annotations:
    summary: "Dispatcher cache hit rate below 70%"
    runbook: "https://wiki/runbooks/low-cache-hit"
```

### 5.3 Alert Routing

```yaml
# Alertmanager Configuration
route:
  receiver: 'default'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty-critical'
      continue: true
    - match:
        severity: high
      receiver: 'slack-ops'
      group_wait: 1m
    - match:
        severity: medium
      receiver: 'slack-ops'
    - match:
        severity: low
      receiver: 'email-ops'

receivers:
  - name: 'pagerduty-critical'
    pagerduty_configs:
      - service_key: '<service-key>'
        severity: critical
  - name: 'slack-ops'
    slack_configs:
      - channel: '#aem-alerts'
        send_resolved: true
  - name: 'email-ops'
    email_configs:
      - to: 'ops-team@example.com'
```

### 5.4 Alert Suppression Rules

```yaml
# Suppress alerts during maintenance
inhibit_rules:
  - source_match:
      alertname: 'MaintenanceWindow'
    target_match_re:
      severity: 'low|medium'
    equal: ['environment']

  # Suppress downstream alerts when upstream is down
  - source_match:
      alertname: 'AEMPublishDown'
    target_match:
      alertname: 'HighResponseTime'
```

---

## 6. Dashboards

### 6.1 Dashboard Hierarchy

```
📊 Executive Overview
├── 📈 Business KPIs
├── 📈 Availability SLA
└── 📈 Cost Overview

📊 Operations Dashboard
├── 📈 System Health
├── 📈 Infrastructure Metrics
├── 📈 Incident Timeline
└── 📈 On-Call Summary

📊 Application Dashboard
├── 📈 Request Performance
├── 📈 Error Analysis
├── 📈 Component Performance
└── 📈 User Sessions

📊 Integration Dashboard
├── 📈 LLM Service Health
├── 📈 Replication Status
├── 📈 CDN Performance
└── 📈 External APIs
```

### 6.2 Key Dashboard Panels

#### System Health Dashboard

| Panel | Visualization | Metrics |
|-------|--------------|---------|
| Uptime Status | Stat | `probe_success` |
| Response Time | Time series | `aem_request_duration_seconds` |
| Error Rate | Gauge | `rate(aem_error_total[5m])` |
| Request Volume | Time series | `rate(aem_request_total[5m])` |
| JVM Heap | Time series | `jvm_heap_used_bytes` |
| GC Pauses | Time series | `jvm_gc_pause_seconds` |

#### Business Metrics Dashboard

| Panel | Visualization | Metrics |
|-------|--------------|---------|
| Content Published | Counter | `content_published_total` |
| Active Sessions | Stat | `aem_active_sessions` |
| Component Usage | Bar chart | `component_render_total` by type |
| LLM Token Usage | Time series | `llm_tokens_used` |
| Popular Pages | Table | Top paths by request count |

### 6.3 Sample Grafana Dashboard JSON

```json
{
  "title": "AEM BMAD Overview",
  "panels": [
    {
      "title": "System Availability",
      "type": "stat",
      "targets": [{
        "expr": "avg(probe_success{job='aem'})*100"
      }],
      "fieldConfig": {
        "defaults": {
          "unit": "percent",
          "thresholds": {
            "steps": [
              {"color": "red", "value": 0},
              {"color": "yellow", "value": 99},
              {"color": "green", "value": 99.9}
            ]
          }
        }
      }
    },
    {
      "title": "Request Rate",
      "type": "timeseries",
      "targets": [{
        "expr": "sum(rate(aem_request_total[5m]))",
        "legendFormat": "Requests/sec"
      }]
    },
    {
      "title": "Response Time (p95)",
      "type": "timeseries",
      "targets": [{
        "expr": "histogram_quantile(0.95, sum(rate(aem_request_duration_seconds_bucket[5m])) by (le))",
        "legendFormat": "p95"
      }]
    }
  ]
}
```

---

## 7. Tooling Recommendations

### 7.1 Recommended Stack

| Category | Tool | Alternative | Notes |
|----------|------|-------------|-------|
| **Metrics** | Prometheus | Victoria Metrics | Use Victoria for scale |
| **Logs** | Loki | Elasticsearch | Loki for cost efficiency |
| **Traces** | Tempo | Jaeger | Tempo for Grafana integration |
| **Visualization** | Grafana | DataDog | Grafana for flexibility |
| **Alerting** | Alertmanager | PagerDuty | PagerDuty for on-call |

### 7.2 AEM-Specific Tools

| Tool | Purpose | Integration |
|------|---------|-------------|
| **AEM Cloud Service Monitoring** | Built-in AEM metrics | Native |
| **Splunk App for Adobe** | AEM log analysis | Splunk |
| **New Relic APM** | Full-stack observability | Java agent |
| **Dynatrace** | AI-powered monitoring | OneAgent |

### 7.3 Implementation Checklist

**Phase 1: Foundation (Week 1-2)**
- [ ] Deploy Prometheus/Victoria Metrics
- [ ] Configure JMX exporter for AEM
- [ ] Set up basic dashboards
- [ ] Configure critical alerts

**Phase 2: Logs (Week 3-4)**
- [ ] Deploy Loki/Elasticsearch
- [ ] Configure log shipping
- [ ] Create log parsing rules
- [ ] Set up log-based alerts

**Phase 3: Traces (Week 5-6)**
- [ ] Deploy Tempo/Jaeger
- [ ] Instrument application code
- [ ] Configure sampling
- [ ] Link traces to logs/metrics

**Phase 4: Optimization (Week 7-8)**
- [ ] Tune alert thresholds
- [ ] Create SLO dashboards
- [ ] Document runbooks
- [ ] Train operations team

---

## Appendix A: Metric Naming Conventions

```
# Format: <namespace>_<metric>_<unit>
aem_request_duration_seconds
aem_request_total
aem_error_total
jvm_heap_used_bytes
dispatcher_cache_hit_ratio
llm_tokens_used_total
```

## Appendix B: Log Query Examples

**Find errors by component:**
```
{job="aem"} |~ "ERROR" | json | component="hero"
```

**Slow requests:**
```
{job="aem"} | json | duration_ms > 3000
```

**User activity:**
```
{job="aem"} | json | userId="admin" | line_format "{{.timestamp}} - {{.action}}"
```

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Platform Team | Initial version |

**Review Cycle:** Quarterly
**Next Review:** [Current Date + 3 months]
