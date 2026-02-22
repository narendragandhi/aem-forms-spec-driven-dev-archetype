# REST API Integration Patterns for AEM

This document provides comprehensive patterns for implementing REST APIs in AEM as a Cloud Service, including inbound APIs (exposing AEM data) and outbound integrations (consuming external services).

## Table of Contents

1. [Sling Servlets](#sling-servlets)
2. [JSON Exporter](#json-exporter)
3. [HTTP Client Services](#http-client-services)
4. [API Security](#api-security)
5. [Error Handling](#error-handling)
6. [Caching Strategies](#caching-strategies)

---

## Sling Servlets

Sling Servlets are the primary mechanism for exposing REST endpoints in AEM.

### Servlet Registration Patterns

#### Pattern 1: Resource Type Binding
Best for component-specific APIs that operate on a specific resource type.

```java
package com.example.aem.bmad.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "bmad/components/hero",
    methods = "GET",
    selectors = "data",
    extensions = "json"
)
public class HeroDataServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Access the resource
        var resource = request.getResource();
        var valueMap = resource.getValueMap();

        // Build JSON response
        var json = String.format(
            "{\"title\": \"%s\", \"description\": \"%s\"}",
            valueMap.get("title", ""),
            valueMap.get("description", "")
        );

        response.getWriter().write(json);
    }
}
```

**Usage**: `GET /content/bmad/page/jcr:content/hero.data.json`

#### Pattern 2: Path-Based Binding
Best for global APIs that don't depend on a specific resource.

```java
package com.example.aem.bmad.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(service = Servlet.class)
@SlingServletPaths("/api/bmad/v1/leads")
public class LeadCaptureServlet extends SlingAllMethodsServlet {

    @Reference
    private CrmIntegrationService crmService;

    @Override
    protected void doPost(SlingHttpServletRequest request,
                          SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        try {
            // Parse request body
            var reader = request.getReader();
            var body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            // Process lead submission
            var result = crmService.submitLead(body.toString());

            response.setStatus(201);
            response.getWriter().write(
                String.format("{\"success\": true, \"leadId\": \"%s\"}", result.getLeadId())
            );

        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write(
                String.format("{\"success\": false, \"error\": \"%s\"}", e.getMessage())
            );
        }
    }
}
```

**Usage**: `POST /api/bmad/v1/leads`

#### Pattern 3: Servlet Filter for Cross-Cutting Concerns

```java
package com.example.aem.bmad.filters;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

@Component
@SlingServletFilter(
    scope = SlingServletFilterScope.REQUEST,
    pattern = "/api/bmad/.*"
)
public class ApiLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ApiLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        var slingRequest = (SlingHttpServletRequest) req;
        var startTime = System.currentTimeMillis();

        LOG.info("API Request: {} {}",
            slingRequest.getMethod(),
            slingRequest.getRequestURI()
        );

        chain.doFilter(req, res);

        var duration = System.currentTimeMillis() - startTime;
        var slingResponse = (SlingHttpServletResponse) res;

        LOG.info("API Response: {} {} - {} ({}ms)",
            slingRequest.getMethod(),
            slingRequest.getRequestURI(),
            slingResponse.getStatus(),
            duration
        );
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
```

---

## JSON Exporter

The Sling Model Exporter provides a declarative way to expose content as JSON.

### Basic JSON Export

```java
package com.example.aem.bmad.models;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = {HeroModel.class, ComponentExporter.class},
    resourceType = HeroModel.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION
)
public class HeroModel implements ComponentExporter {

    public static final String RESOURCE_TYPE = "bmad/components/hero";

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String backgroundImage;

    @ValueMapValue
    private String ctaText;

    @ValueMapValue
    private String ctaLink;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public String getCtaText() {
        return ctaText;
    }

    public String getCtaLink() {
        return ctaLink;
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
```

**Usage**: `GET /content/bmad/page/jcr:content/hero.model.json`

### Custom JSON Structure with Jackson

```java
package com.example.aem.bmad.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.apache.sling.models.annotations.*;

@Model(...)
@Exporter(name = "jackson", extensions = "json")
@JsonRootName("heroComponent")
public class HeroModelAdvanced implements ComponentExporter {

    @ValueMapValue
    @JsonProperty("heroTitle")  // Custom JSON property name
    private String title;

    @ValueMapValue
    @JsonIgnore  // Exclude from JSON
    private String internalNote;

    @JsonProperty("metadata")
    public HeroMetadata getMetadata() {
        return new HeroMetadata(
            System.currentTimeMillis(),
            "1.0"
        );
    }

    // Inner class for nested JSON
    public static class HeroMetadata {
        private final long timestamp;
        private final String version;

        public HeroMetadata(long timestamp, String version) {
            this.timestamp = timestamp;
            this.version = version;
        }

        public long getTimestamp() { return timestamp; }
        public String getVersion() { return version; }
    }
}
```

---

## HTTP Client Services

For outbound API calls, create reusable OSGi services.

### Generic HTTP Client Service

```java
package com.example.aem.bmad.services;

import java.util.Map;

public interface HttpClientService {

    /**
     * Perform a GET request
     */
    HttpResponse get(String url, Map<String, String> headers);

    /**
     * Perform a POST request with JSON body
     */
    HttpResponse post(String url, String jsonBody, Map<String, String> headers);

    /**
     * Perform a PUT request with JSON body
     */
    HttpResponse put(String url, String jsonBody, Map<String, String> headers);

    /**
     * Perform a DELETE request
     */
    HttpResponse delete(String url, Map<String, String> headers);
}
```

### HTTP Client Implementation

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.HttpClientService;
import com.example.aem.bmad.services.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Component(service = HttpClientService.class, immediate = true)
@Designate(ocd = HttpClientServiceImpl.Config.class)
public class HttpClientServiceImpl implements HttpClientService {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientServiceImpl.class);

    @ObjectClassDefinition(name = "BMAD HTTP Client Configuration")
    public @interface Config {

        @AttributeDefinition(
            name = "Connection Timeout",
            description = "Connection timeout in milliseconds"
        )
        int connectionTimeout() default 5000;

        @AttributeDefinition(
            name = "Socket Timeout",
            description = "Socket timeout in milliseconds"
        )
        int socketTimeout() default 30000;

        @AttributeDefinition(
            name = "Max Connections",
            description = "Maximum number of connections"
        )
        int maxConnections() default 100;
    }

    private CloseableHttpClient httpClient;
    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;

        var requestConfig = RequestConfig.custom()
            .setConnectTimeout(config.connectionTimeout())
            .setSocketTimeout(config.socketTimeout())
            .build();

        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setMaxConnTotal(config.maxConnections())
            .build();

        LOG.info("HTTP Client initialized with timeout: {}ms", config.connectionTimeout());
    }

    @Deactivate
    protected void deactivate() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            LOG.error("Error closing HTTP client", e);
        }
    }

    @Override
    public HttpResponse get(String url, Map<String, String> headers) {
        var request = new HttpGet(url);
        headers.forEach(request::setHeader);
        return execute(request);
    }

    @Override
    public HttpResponse post(String url, String jsonBody, Map<String, String> headers) {
        var request = new HttpPost(url);
        headers.forEach(request::setHeader);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonBody, "UTF-8"));
        return execute(request);
    }

    @Override
    public HttpResponse put(String url, String jsonBody, Map<String, String> headers) {
        var request = new HttpPut(url);
        headers.forEach(request::setHeader);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(jsonBody, "UTF-8"));
        return execute(request);
    }

    @Override
    public HttpResponse delete(String url, Map<String, String> headers) {
        var request = new HttpDelete(url);
        headers.forEach(request::setHeader);
        return execute(request);
    }

    private HttpResponse execute(HttpUriRequest request) {
        try {
            LOG.debug("Executing {} {}", request.getMethod(), request.getURI());

            try (var response = httpClient.execute(request)) {
                var statusCode = response.getStatusLine().getStatusCode();
                var body = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity())
                    : "";

                LOG.debug("Response: {} - {} bytes", statusCode, body.length());

                return new HttpResponse(statusCode, body);
            }
        } catch (IOException e) {
            LOG.error("HTTP request failed: {}", e.getMessage(), e);
            return new HttpResponse(0, e.getMessage(), true);
        }
    }
}
```

### Response Object

```java
package com.example.aem.bmad.services;

public class HttpResponse {

    private final int statusCode;
    private final String body;
    private final boolean error;

    public HttpResponse(int statusCode, String body) {
        this(statusCode, body, false);
    }

    public HttpResponse(int statusCode, String body, boolean error) {
        this.statusCode = statusCode;
        this.body = body;
        this.error = error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public boolean isError() {
        return error;
    }

    public boolean isSuccess() {
        return !error && statusCode >= 200 && statusCode < 300;
    }
}
```

---

## API Security

### API Key Authentication Filter

```java
package com.example.aem.bmad.filters;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;

import javax.servlet.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@SlingServletFilter(
    scope = SlingServletFilterScope.REQUEST,
    pattern = "/api/bmad/v1/.*"
)
@Designate(ocd = ApiKeyAuthFilter.Config.class)
public class ApiKeyAuthFilter implements Filter {

    @ObjectClassDefinition(name = "BMAD API Key Authentication")
    public @interface Config {

        @AttributeDefinition(
            name = "Valid API Keys",
            description = "List of valid API keys (should use Cloud Manager secrets in production)"
        )
        String[] validApiKeys() default {};

        @AttributeDefinition(
            name = "Header Name",
            description = "HTTP header name for API key"
        )
        String headerName() default "X-API-Key";

        @AttributeDefinition(
            name = "Enabled",
            description = "Enable API key authentication"
        )
        boolean enabled() default true;
    }

    private Set<String> validKeys;
    private String headerName;
    private boolean enabled;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.validKeys = new HashSet<>(Arrays.asList(config.validApiKeys()));
        this.headerName = config.headerName();
        this.enabled = config.enabled();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(req, res);
            return;
        }

        var request = (SlingHttpServletRequest) req;
        var response = (SlingHttpServletResponse) res;
        var apiKey = request.getHeader(headerName);

        if (apiKey == null || !validKeys.contains(apiKey)) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or missing API key\"}");
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
```

### CORS Configuration

```java
package com.example.aem.bmad.filters;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;

import javax.servlet.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@SlingServletFilter(
    scope = SlingServletFilterScope.REQUEST,
    pattern = "/api/bmad/.*"
)
@Designate(ocd = CorsFilter.Config.class)
public class CorsFilter implements Filter {

    @ObjectClassDefinition(name = "BMAD CORS Configuration")
    public @interface Config {

        @AttributeDefinition(
            name = "Allowed Origins",
            description = "List of allowed origins"
        )
        String[] allowedOrigins() default {"https://www.example.com"};

        @AttributeDefinition(
            name = "Allowed Methods",
            description = "Allowed HTTP methods"
        )
        String[] allowedMethods() default {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

        @AttributeDefinition(
            name = "Allowed Headers",
            description = "Allowed request headers"
        )
        String[] allowedHeaders() default {"Content-Type", "X-API-Key", "Authorization"};

        @AttributeDefinition(
            name = "Max Age",
            description = "Preflight cache duration in seconds"
        )
        int maxAge() default 3600;
    }

    private Set<String> allowedOrigins;
    private String allowedMethods;
    private String allowedHeaders;
    private int maxAge;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.allowedOrigins = Arrays.stream(config.allowedOrigins())
            .collect(Collectors.toSet());
        this.allowedMethods = String.join(", ", config.allowedMethods());
        this.allowedHeaders = String.join(", ", config.allowedHeaders());
        this.maxAge = config.maxAge();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        var request = (SlingHttpServletRequest) req;
        var response = (SlingHttpServletResponse) res;
        var origin = request.getHeader("Origin");

        if (origin != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", allowedMethods);
            response.setHeader("Access-Control-Allow-Headers", allowedHeaders);
            response.setHeader("Access-Control-Max-Age", String.valueOf(maxAge));
        }

        // Handle preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(200);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
```

---

## Error Handling

### Standard Error Response Structure

```java
package com.example.aem.bmad.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String timestamp;
    private final String traceId;

    private ApiError(Builder builder) {
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        this.path = builder.path;
        this.timestamp = Instant.now().toString();
        this.traceId = builder.traceId;
    }

    // Getters...
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public String getTimestamp() { return timestamp; }
    public String getTraceId() { return traceId; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String error;
        private String message;
        private String path;
        private String traceId;

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public ApiError build() {
            return new ApiError(this);
        }
    }
}
```

### Exception Handler

```java
package com.example.aem.bmad.servlets;

import com.example.aem.bmad.models.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public final class ApiResponseHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ApiResponseHelper.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiResponseHelper() {}

    public static void sendError(SlingHttpServletResponse response,
                                  int status,
                                  String error,
                                  String message,
                                  String path) throws IOException {

        var traceId = UUID.randomUUID().toString();

        LOG.error("API Error [{}]: {} - {} (path: {})", traceId, error, message, path);

        var apiError = ApiError.builder()
            .status(status)
            .error(error)
            .message(message)
            .path(path)
            .traceId(traceId)
            .build();

        response.setStatus(status);
        response.setContentType("application/json");
        MAPPER.writeValue(response.getWriter(), apiError);
    }

    public static void sendSuccess(SlingHttpServletResponse response,
                                    Object data) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json");
        MAPPER.writeValue(response.getWriter(), data);
    }

    public static void sendCreated(SlingHttpServletResponse response,
                                    Object data) throws IOException {
        response.setStatus(201);
        response.setContentType("application/json");
        MAPPER.writeValue(response.getWriter(), data);
    }
}
```

---

## Caching Strategies

### Response Caching Headers

```java
package com.example.aem.bmad.filters;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;

import javax.servlet.*;
import java.io.IOException;

@Component
@SlingServletFilter(
    scope = SlingServletFilterScope.REQUEST,
    pattern = "/api/bmad/v1/content/.*"
)
public class CacheControlFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        var response = (SlingHttpServletResponse) res;

        // Set caching headers for content APIs
        response.setHeader("Cache-Control", "public, max-age=300, stale-while-revalidate=60");
        response.setHeader("Vary", "Accept-Encoding");

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
```

---

## Traceability

| Spec ID | Pattern | Description | Status |
|---------|---------|-------------|--------|
| INT-REST-001 | Sling Servlet Resource Type | Component-specific REST endpoints | Documented |
| INT-REST-002 | Sling Servlet Path | Global REST endpoints | Documented |
| INT-REST-003 | JSON Exporter | Sling Model JSON export | Documented |
| INT-REST-004 | HTTP Client Service | Outbound API calls | Documented |
| INT-REST-005 | API Key Auth | API authentication | Documented |
| INT-REST-006 | CORS Filter | Cross-origin requests | Documented |
| INT-REST-007 | Error Handling | Standardized error responses | Documented |
| INT-REST-008 | Cache Control | Response caching | Documented |
