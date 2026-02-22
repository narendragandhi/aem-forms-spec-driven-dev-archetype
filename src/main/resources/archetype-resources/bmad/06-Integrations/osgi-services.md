# OSGi Services for AEM Integrations

This document covers OSGi service patterns and best practices for creating reusable, configurable integration services in AEM as a Cloud Service.

## Table of Contents

1. [OSGi Fundamentals](#osgi-fundamentals)
2. [Service Patterns](#service-patterns)
3. [Configuration Management](#configuration-management)
4. [Service Registration](#service-registration)
5. [Dependency Injection](#dependency-injection)
6. [HTTP Client Services](#http-client-services)
7. [Schedulers and Jobs](#schedulers-and-jobs)
8. [Health Checks](#health-checks)

---

## OSGi Fundamentals

### OSGi Architecture in AEM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        AEM as a Cloud Service                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                         OSGi Framework                               ││
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐            ││
│  │  │    Bundle 1   │  │    Bundle 2   │  │    Bundle 3   │            ││
│  │  │  (Services)   │  │  (Services)   │  │  (Services)   │            ││
│  │  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘            ││
│  │          │                  │                  │                     ││
│  │          └──────────────────┼──────────────────┘                     ││
│  │                             ▼                                        ││
│  │  ┌─────────────────────────────────────────────────────────────────┐││
│  │  │                    Service Registry                              │││
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │││
│  │  │  │ Service A   │  │ Service B   │  │ Service C   │               │││
│  │  │  │ (Interface) │  │ (Interface) │  │ (Interface) │               │││
│  │  │  └─────────────┘  └─────────────┘  └─────────────┘               │││
│  │  └─────────────────────────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────────────────────────┘│
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                    Configuration Admin                               ││
│  │  ┌─────────────────────────────────────────────────────────────────┐││
│  │  │  /apps/bmad/osgiconfig/config.author/                           │││
│  │  │  /apps/bmad/osgiconfig/config.publish/                          │││
│  │  │  /apps/bmad/osgiconfig/config/                                  │││
│  │  └─────────────────────────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────────────────────────┘│
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Key OSGi Annotations

| Annotation | Purpose |
|------------|---------|
| `@Component` | Declares a class as an OSGi component/service |
| `@Activate` | Method called when component is activated |
| `@Deactivate` | Method called when component is deactivated |
| `@Modified` | Method called when configuration changes |
| `@Reference` | Inject a dependency on another service |
| `@Designate` | Links component to a configuration class |
| `@ObjectClassDefinition` | Defines configuration properties |
| `@AttributeDefinition` | Defines a single configuration property |

---

## Service Patterns

### Interface + Implementation Pattern

Always define a service interface separate from the implementation:

```java
// Interface
package com.example.aem.bmad.services;

public interface NotificationService {

    /**
     * Send a notification to a user
     */
    void sendNotification(String userId, String message, NotificationType type);

    /**
     * Send a notification to multiple users
     */
    void sendBulkNotification(List<String> userIds, String message, NotificationType type);

    /**
     * Check if notifications are enabled
     */
    boolean isEnabled();

    enum NotificationType {
        INFO, WARNING, ERROR, SUCCESS
    }
}
```

```java
// Implementation
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.NotificationService;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component(
    service = NotificationService.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = NotificationServiceImpl.Config.class)
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @ObjectClassDefinition(
        name = "BMAD Notification Service Configuration",
        description = "Configuration for the notification service"
    )
    public @interface Config {

        @AttributeDefinition(
            name = "Enabled",
            description = "Enable or disable notifications"
        )
        boolean enabled() default true;

        @AttributeDefinition(
            name = "Notification Endpoint",
            description = "External notification service URL"
        )
        String endpoint() default "https://api.notifications.example.com";

        @AttributeDefinition(
            name = "API Key",
            description = "API key for notification service"
        )
        String apiKey();

        @AttributeDefinition(
            name = "Timeout (ms)",
            description = "Request timeout in milliseconds"
        )
        int timeout() default 5000;
    }

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("Notification service activated: enabled={}, endpoint={}",
            config.enabled(), config.endpoint());
    }

    @Deactivate
    protected void deactivate() {
        LOG.info("Notification service deactivated");
    }

    @Override
    public void sendNotification(String userId, String message, NotificationType type) {
        if (!config.enabled()) {
            LOG.debug("Notifications disabled, skipping for user: {}", userId);
            return;
        }

        LOG.info("Sending {} notification to user {}: {}", type, userId, message);
        // Implementation details...
    }

    @Override
    public void sendBulkNotification(List<String> userIds, String message, NotificationType type) {
        userIds.forEach(userId -> sendNotification(userId, message, type));
    }

    @Override
    public boolean isEnabled() {
        return config.enabled();
    }
}
```

### Factory Pattern for Multiple Configurations

Use factory components when you need multiple instances of the same service:

```java
package com.example.aem.bmad.services;

public interface ExternalApiClient {
    String getName();
    String call(String endpoint, String method, String body);
    boolean isHealthy();
}
```

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.ExternalApiClient;
import com.example.aem.bmad.services.HttpClientService;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
    service = ExternalApiClient.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ExternalApiClientFactory.Config.class, factory = true)
public class ExternalApiClientFactory implements ExternalApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalApiClientFactory.class);

    @ObjectClassDefinition(
        name = "BMAD External API Client Factory",
        description = "Factory configuration for external API clients"
    )
    public @interface Config {

        @AttributeDefinition(
            name = "Client Name",
            description = "Unique identifier for this API client"
        )
        String clientName();

        @AttributeDefinition(
            name = "Base URL",
            description = "Base URL for the API"
        )
        String baseUrl();

        @AttributeDefinition(
            name = "API Key",
            description = "API key for authentication"
        )
        String apiKey();

        @AttributeDefinition(
            name = "Timeout (ms)",
            description = "Request timeout"
        )
        int timeout() default 10000;

        @AttributeDefinition(
            name = "Max Retries",
            description = "Maximum retry attempts"
        )
        int maxRetries() default 3;
    }

    @Reference
    private HttpClientService httpClient;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("External API client configured: name={}, baseUrl={}",
            config.clientName(), config.baseUrl());
    }

    @Override
    public String getName() {
        return config.clientName();
    }

    @Override
    public String call(String endpoint, String method, String body) {
        String url = config.baseUrl() + endpoint;

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + config.apiKey());
        headers.put("Content-Type", "application/json");

        // Implementation with retry logic...
        var response = httpClient.request(method, url, body, headers);
        return response.getBody();
    }

    @Override
    public boolean isHealthy() {
        try {
            call("/health", "GET", null);
            return true;
        } catch (Exception e) {
            LOG.warn("Health check failed for {}: {}", config.clientName(), e.getMessage());
            return false;
        }
    }
}
```

### Service Ranking and Selection

Control which implementation is used when multiple services implement the same interface:

```java
@Component(
    service = ContentEnricher.class,
    property = {
        "service.ranking:Integer=100"  // Higher ranking = higher priority
    }
)
public class PremiumContentEnricher implements ContentEnricher {
    // Premium implementation
}

@Component(
    service = ContentEnricher.class,
    property = {
        "service.ranking:Integer=10"  // Lower ranking = fallback
    }
)
public class BasicContentEnricher implements ContentEnricher {
    // Basic implementation
}
```

---

## Configuration Management

### Run Mode Specific Configurations

AEM supports run mode-specific configurations:

```
/apps/bmad/osgiconfig/
├── config/                                    # All environments
│   └── com.example.aem.bmad.services.impl.CacheServiceImpl.cfg.json
├── config.author/                             # Author only
│   └── com.example.aem.bmad.services.impl.PreviewService.cfg.json
├── config.publish/                            # Publish only
│   └── com.example.aem.bmad.services.impl.CDNService.cfg.json
├── config.dev/                                # Dev environment
│   └── com.example.aem.bmad.services.impl.LoggingConfig.cfg.json
├── config.stage/                              # Stage environment
│   └── com.example.aem.bmad.services.impl.SalesforceService.cfg.json
└── config.prod/                               # Production
    └── com.example.aem.bmad.services.impl.SalesforceService.cfg.json
```

### Configuration File Format (.cfg.json)

```json
{
    "enabled": true,
    "endpoint": "https://api.example.com",
    "apiKey": "$[secret:BMAD_API_KEY]",
    "timeout": 5000,
    "allowedPaths": [
        "/content/bmad",
        "/content/dam/bmad"
    ],
    "features": {
        "caching": true,
        "logging": true
    }
}
```

### Using Cloud Manager Secrets

```json
{
    "apiKey": "$[secret:SALESFORCE_API_KEY]",
    "clientSecret": "$[secret:SALESFORCE_CLIENT_SECRET]",
    "password": "$[secret:SALESFORCE_PASSWORD]"
}
```

### Environment Variables

```json
{
    "endpoint": "$[env:API_ENDPOINT;default=https://api.example.com]",
    "timeout": "$[env:API_TIMEOUT;default=5000]"
}
```

---

## Service Registration

### Service Properties

Add custom properties to services for filtering:

```java
@Component(
    service = IntegrationHandler.class,
    property = {
        "integration.type=crm",
        "integration.vendor=salesforce",
        "integration.version=v1"
    }
)
public class SalesforceIntegrationHandler implements IntegrationHandler {
    // Implementation
}
```

### Filtering Service References

```java
@Component(service = IntegrationOrchestrator.class)
public class IntegrationOrchestrator {

    // Inject only CRM integrations
    @Reference(target = "(integration.type=crm)")
    private List<IntegrationHandler> crmHandlers;

    // Inject specific vendor
    @Reference(target = "(&(integration.type=crm)(integration.vendor=salesforce))")
    private IntegrationHandler salesforceHandler;

    // Inject all handlers with optional cardinality
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile List<IntegrationHandler> allHandlers;

    protected void bindIntegrationHandler(IntegrationHandler handler) {
        // Called when handler becomes available
    }

    protected void unbindIntegrationHandler(IntegrationHandler handler) {
        // Called when handler goes away
    }
}
```

---

## Dependency Injection

### Reference Cardinality

| Cardinality | Description |
|-------------|-------------|
| `MANDATORY` | Exactly one service required (default) |
| `OPTIONAL` | Zero or one service |
| `MULTIPLE` | Zero or more services |
| `AT_LEAST_ONE` | One or more services required |

### Reference Policy

```java
@Component(service = AggregatorService.class)
public class AggregatorService {

    // Static policy - component restarts when reference changes
    @Reference(policy = ReferencePolicy.STATIC)
    private ConfigService configService;

    // Dynamic policy - component stays running, reference updated
    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.MULTIPLE,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile List<DataProvider> dataProviders;

    // Bind/unbind methods for dynamic references
    protected void bindDataProvider(DataProvider provider, Map<String, Object> properties) {
        LOG.info("DataProvider bound: {}", properties.get("provider.name"));
    }

    protected void unbindDataProvider(DataProvider provider, Map<String, Object> properties) {
        LOG.info("DataProvider unbound: {}", properties.get("provider.name"));
    }
}
```

### Lazy vs Immediate Activation

```java
// Lazy activation (default) - component activated on first use
@Component(service = LazyService.class)
public class LazyServiceImpl implements LazyService {
    // Activated when first @Reference to this service is resolved
}

// Immediate activation - component activated at bundle start
@Component(service = EagerService.class, immediate = true)
public class EagerServiceImpl implements EagerService {
    // Activated immediately when bundle starts
}
```

---

## HTTP Client Services

### HTTP Client Service Interface

```java
package com.example.aem.bmad.services;

import java.util.Map;

public interface HttpClientService {

    /**
     * Perform GET request
     */
    HttpResponse get(String url, Map<String, String> headers);

    /**
     * Perform POST request
     */
    HttpResponse post(String url, String body, Map<String, String> headers);

    /**
     * Perform PUT request
     */
    HttpResponse put(String url, String body, Map<String, String> headers);

    /**
     * Perform DELETE request
     */
    HttpResponse delete(String url, Map<String, String> headers);

    /**
     * Perform PATCH request
     */
    HttpResponse patch(String url, String body, Map<String, String> headers);

    /**
     * Generic request method
     */
    HttpResponse request(String method, String url, String body, Map<String, String> headers);
}
```

### HTTP Response Model

```java
package com.example.aem.bmad.models;

public class HttpResponse {

    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;
    private final long durationMs;

    public HttpResponse(int statusCode, String body, Map<String, String> headers, long durationMs) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
        this.durationMs = durationMs;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }
    public long getDurationMs() { return durationMs; }

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    public boolean isServerError() {
        return statusCode >= 500;
    }
}
```

### HTTP Client Implementation

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.models.HttpResponse;
import com.example.aem.bmad.services.HttpClientService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component(service = HttpClientService.class, immediate = true)
@Designate(ocd = HttpClientServiceImpl.Config.class)
public class HttpClientServiceImpl implements HttpClientService {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientServiceImpl.class);

    @ObjectClassDefinition(name = "BMAD HTTP Client Configuration")
    public @interface Config {

        @AttributeDefinition(name = "Connection Timeout (ms)")
        int connectTimeout() default 5000;

        @AttributeDefinition(name = "Socket Timeout (ms)")
        int socketTimeout() default 30000;

        @AttributeDefinition(name = "Connection Request Timeout (ms)")
        int connectionRequestTimeout() default 5000;

        @AttributeDefinition(name = "Max Total Connections")
        int maxTotalConnections() default 100;

        @AttributeDefinition(name = "Max Connections Per Route")
        int maxConnectionsPerRoute() default 20;
    }

    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    @Activate
    @Modified
    protected void activate(Config config) {
        // Connection pool manager
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(config.maxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.maxConnectionsPerRoute());

        // Request configuration
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(config.connectTimeout())
            .setSocketTimeout(config.socketTimeout())
            .setConnectionRequestTimeout(config.connectionRequestTimeout())
            .build();

        // Build HTTP client
        httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();

        LOG.info("HTTP client configured: connectTimeout={}, socketTimeout={}, maxConnections={}",
            config.connectTimeout(), config.socketTimeout(), config.maxTotalConnections());
    }

    @Deactivate
    protected void deactivate() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOG.warn("Error closing HTTP client", e);
            }
        }
        if (connectionManager != null) {
            connectionManager.close();
        }
    }

    @Override
    public HttpResponse get(String url, Map<String, String> headers) {
        return request("GET", url, null, headers);
    }

    @Override
    public HttpResponse post(String url, String body, Map<String, String> headers) {
        return request("POST", url, body, headers);
    }

    @Override
    public HttpResponse put(String url, String body, Map<String, String> headers) {
        return request("PUT", url, body, headers);
    }

    @Override
    public HttpResponse delete(String url, Map<String, String> headers) {
        return request("DELETE", url, null, headers);
    }

    @Override
    public HttpResponse patch(String url, String body, Map<String, String> headers) {
        return request("PATCH", url, body, headers);
    }

    @Override
    public HttpResponse request(String method, String url, String body, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();

        HttpRequestBase request = createRequest(method, url, body);

        // Apply headers
        if (headers != null) {
            headers.forEach(request::setHeader);
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null
                ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                : null;

            Map<String, String> responseHeaders = new HashMap<>();
            for (var header : response.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }

            long duration = System.currentTimeMillis() - startTime;
            LOG.debug("{} {} completed in {}ms with status {}",
                method, url, duration, statusCode);

            return new HttpResponse(statusCode, responseBody, responseHeaders, duration);

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            LOG.error("{} {} failed after {}ms: {}", method, url, duration, e.getMessage());
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private HttpRequestBase createRequest(String method, String url, String body) {
        HttpRequestBase request;

        switch (method.toUpperCase()) {
            case "GET":
                request = new HttpGet(url);
                break;
            case "POST":
                HttpPost post = new HttpPost(url);
                if (body != null) {
                    post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
                }
                request = post;
                break;
            case "PUT":
                HttpPut put = new HttpPut(url);
                if (body != null) {
                    put.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
                }
                request = put;
                break;
            case "DELETE":
                request = new HttpDelete(url);
                break;
            case "PATCH":
                HttpPatch patch = new HttpPatch(url);
                if (body != null) {
                    patch.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
                }
                request = patch;
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        return request;
    }
}
```

---

## Schedulers and Jobs

### Scheduled Service Pattern

```java
package com.example.aem.bmad.schedulers;

import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = Runnable.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ContentSyncScheduler.Config.class)
public class ContentSyncScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ContentSyncScheduler.class);

    @ObjectClassDefinition(
        name = "BMAD Content Sync Scheduler",
        description = "Scheduled job for syncing content with external systems"
    )
    public @interface Config {

        @AttributeDefinition(
            name = "Enabled",
            description = "Enable/disable the scheduler"
        )
        boolean enabled() default true;

        @AttributeDefinition(
            name = "Cron Expression",
            description = "Cron expression for scheduling (e.g., '0 0 * * * ?' for hourly)"
        )
        String scheduler_expression() default "0 0 * * * ?";

        @AttributeDefinition(
            name = "Concurrent Execution",
            description = "Allow concurrent execution"
        )
        boolean scheduler_concurrent() default false;

        @AttributeDefinition(
            name = "Run On Leader",
            description = "Run only on cluster leader"
        )
        boolean scheduler_runOn() default true; // Requires AEM Cloud

        @AttributeDefinition(
            name = "Content Paths",
            description = "Paths to sync"
        )
        String[] contentPaths() default {"/content/bmad"};
    }

    @Reference
    private ContentSyncService contentSyncService;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("Content sync scheduler configured: enabled={}, expression={}",
            config.enabled(), config.scheduler_expression());
    }

    @Override
    public void run() {
        if (!config.enabled()) {
            LOG.debug("Content sync scheduler disabled, skipping execution");
            return;
        }

        LOG.info("Starting scheduled content sync");
        long startTime = System.currentTimeMillis();

        try {
            for (String path : config.contentPaths()) {
                contentSyncService.syncContent(path);
            }

            long duration = System.currentTimeMillis() - startTime;
            LOG.info("Content sync completed in {}ms", duration);

        } catch (Exception e) {
            LOG.error("Content sync failed", e);
        }
    }
}
```

### Sling Jobs for Reliable Background Processing

```java
package com.example.aem.bmad.jobs;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = JobConsumer.class,
    immediate = true,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + IntegrationSyncJobConsumer.JOB_TOPIC
    }
)
public class IntegrationSyncJobConsumer implements JobConsumer {

    public static final String JOB_TOPIC = "bmad/integration/sync";
    public static final String PROP_CONTENT_PATH = "contentPath";
    public static final String PROP_INTEGRATION_TYPE = "integrationType";

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationSyncJobConsumer.class);

    @Reference
    private SalesforceService salesforceService;

    @Reference
    private AnalyticsService analyticsService;

    @Override
    public JobResult process(Job job) {
        String contentPath = job.getProperty(PROP_CONTENT_PATH, String.class);
        String integrationType = job.getProperty(PROP_INTEGRATION_TYPE, String.class);

        LOG.info("Processing sync job: path={}, type={}", contentPath, integrationType);

        try {
            switch (integrationType) {
                case "salesforce":
                    salesforceService.syncContent(contentPath);
                    break;
                case "analytics":
                    analyticsService.syncContent(contentPath);
                    break;
                default:
                    LOG.warn("Unknown integration type: {}", integrationType);
                    return JobResult.CANCEL;
            }

            LOG.info("Sync job completed successfully");
            return JobResult.OK;

        } catch (Exception e) {
            LOG.error("Sync job failed", e);
            return JobResult.FAILED;
        }
    }
}
```

### Job Manager for Creating Jobs

```java
package com.example.aem.bmad.services.impl;

import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobBuilder;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service = IntegrationJobService.class)
public class IntegrationJobService {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationJobService.class);

    @Reference
    private JobManager jobManager;

    public void scheduleSync(String contentPath, String integrationType) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(IntegrationSyncJobConsumer.PROP_CONTENT_PATH, contentPath);
        properties.put(IntegrationSyncJobConsumer.PROP_INTEGRATION_TYPE, integrationType);

        jobManager.addJob(IntegrationSyncJobConsumer.JOB_TOPIC, properties);
        LOG.info("Scheduled sync job: path={}, type={}", contentPath, integrationType);
    }

    public void scheduleDelayedSync(String contentPath, String integrationType, int delayMinutes) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(IntegrationSyncJobConsumer.PROP_CONTENT_PATH, contentPath);
        properties.put(IntegrationSyncJobConsumer.PROP_INTEGRATION_TYPE, integrationType);

        JobBuilder.ScheduleBuilder scheduleBuilder = jobManager.createJob(IntegrationSyncJobConsumer.JOB_TOPIC)
            .properties(properties)
            .schedule();

        scheduleBuilder.at(new Date(System.currentTimeMillis() + (delayMinutes * 60 * 1000)));
        scheduleBuilder.add();

        LOG.info("Scheduled delayed sync job in {} minutes", delayMinutes);
    }
}
```

---

## Health Checks

### Health Check Interface

```java
package com.example.aem.bmad.services;

public interface HealthCheck {

    String getName();
    HealthStatus check();
    String getDescription();

    enum HealthStatus {
        HEALTHY, DEGRADED, UNHEALTHY
    }

    class HealthResult {
        private final HealthStatus status;
        private final String message;
        private final long responseTimeMs;

        public HealthResult(HealthStatus status, String message, long responseTimeMs) {
            this.status = status;
            this.message = message;
            this.responseTimeMs = responseTimeMs;
        }

        public HealthStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public long getResponseTimeMs() { return responseTimeMs; }
    }
}
```

### Sling Health Check Implementation

```java
package com.example.aem.bmad.healthchecks;

import com.example.aem.bmad.services.SalesforceService;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = HealthCheck.class,
    immediate = true,
    property = {
        HealthCheck.NAME + "=BMAD Salesforce Integration",
        HealthCheck.TAGS + "=bmad,integration,salesforce",
        HealthCheck.MBEAN_NAME + "=bmadSalesforceHealthCheck"
    }
)
public class SalesforceHealthCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceHealthCheck.class);

    @Reference
    private SalesforceService salesforceService;

    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();

        long startTime = System.currentTimeMillis();

        try {
            boolean healthy = salesforceService.isHealthy();
            long duration = System.currentTimeMillis() - startTime;

            if (healthy) {
                log.info("Salesforce connection healthy ({}ms)", duration);
            } else {
                log.warn("Salesforce connection degraded ({}ms)", duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.critical("Salesforce connection failed ({}ms): {}", duration, e.getMessage());
            LOG.error("Salesforce health check failed", e);
        }

        return new Result(log);
    }
}
```

### Composite Health Check

```java
package com.example.aem.bmad.healthchecks;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.*;

import java.util.List;

@Component(
    service = HealthCheck.class,
    immediate = true,
    property = {
        HealthCheck.NAME + "=BMAD All Integrations",
        HealthCheck.TAGS + "=bmad,integration,composite",
        HealthCheck.MBEAN_NAME + "=bmadIntegrationsHealthCheck"
    }
)
public class AllIntegrationsHealthCheck implements HealthCheck {

    @Reference(
        target = "(component.name=*HealthCheck)",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    private volatile List<HealthCheck> healthChecks;

    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();

        int healthy = 0;
        int unhealthy = 0;

        for (HealthCheck check : healthChecks) {
            // Skip self to avoid recursion
            if (check == this) continue;

            Result result = check.execute();
            if (result.isOk()) {
                healthy++;
                log.info("{}: OK", check.getClass().getSimpleName());
            } else {
                unhealthy++;
                log.warn("{}: {}", check.getClass().getSimpleName(), result.getStatus());
            }
        }

        log.info("Summary: {} healthy, {} unhealthy", healthy, unhealthy);

        return new Result(log);
    }
}
```

### Health Check Servlet

```java
package com.example.aem.bmad.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.api.execution.HealthCheckSelector;
import org.osgi.service.component.annotations.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.*;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/bmad/health",
        "sling.servlet.methods=GET"
    }
)
public class HealthCheckServlet extends SlingSafeMethodsServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private HealthCheckExecutor healthCheckExecutor;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        String tags = request.getParameter("tags");
        HealthCheckSelector selector = tags != null
            ? HealthCheckSelector.tags(tags.split(","))
            : HealthCheckSelector.tags("bmad");

        List<HealthCheckExecutionResult> results = healthCheckExecutor.execute(selector);

        Map<String, Object> healthReport = new HashMap<>();
        List<Map<String, Object>> checks = new ArrayList<>();

        boolean allHealthy = true;

        for (HealthCheckExecutionResult result : results) {
            Map<String, Object> checkResult = new HashMap<>();
            checkResult.put("name", result.getHealthCheckMetadata().getName());
            checkResult.put("status", result.getHealthCheckResult().isOk() ? "OK" : "FAILED");
            checkResult.put("elapsed", result.getElapsedTimeInMs());

            if (!result.getHealthCheckResult().isOk()) {
                allHealthy = false;
            }

            checks.add(checkResult);
        }

        healthReport.put("status", allHealthy ? "healthy" : "unhealthy");
        healthReport.put("timestamp", System.currentTimeMillis());
        healthReport.put("checks", checks);

        response.setContentType("application/json");
        response.setStatus(allHealthy ? 200 : 503);

        MAPPER.writeValue(response.getWriter(), healthReport);
    }
}
```

---

## Traceability

| Spec ID | Pattern | Description | Status |
|---------|---------|-------------|--------|
| INT-OS-001 | Service Interface | Interface + Implementation pattern | Documented |
| INT-OS-002 | Factory Pattern | Multiple service instances | Documented |
| INT-OS-003 | Configuration | Run-mode specific configs | Documented |
| INT-OS-004 | Dependency Injection | Reference cardinality and policy | Documented |
| INT-OS-005 | HTTP Client | Pooled HTTP client service | Documented |
| INT-OS-006 | Schedulers | Cron-based scheduled tasks | Documented |
| INT-OS-007 | Sling Jobs | Reliable background processing | Documented |
| INT-OS-008 | Health Checks | Service health monitoring | Documented |
