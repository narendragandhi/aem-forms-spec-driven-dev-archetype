# Integration Best Practices

This document outlines AEM-specific best practices for building robust, maintainable integrations with external services. Following these patterns ensures reliability, performance, and observability in production environments.

## Table of Contents

1. [Error Handling](#error-handling)
2. [Caching Strategies](#caching-strategies)
3. [Circuit Breaker Pattern](#circuit-breaker-pattern)
4. [Retry Logic](#retry-logic)
5. [Timeout Configuration](#timeout-configuration)
6. [Monitoring and Observability](#monitoring-and-observability)
7. [Secret Management](#secret-management)
8. [Rate Limiting](#rate-limiting)

---

## Error Handling

### Standardized Error Response Model

```java
package com.example.aem.bmad.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntegrationError {

    private final String errorCode;
    private final String message;
    private final String service;
    private final Instant timestamp;
    private final String correlationId;
    private final Map<String, Object> details;
    private final boolean retryable;

    private IntegrationError(Builder builder) {
        this.errorCode = builder.errorCode;
        this.message = builder.message;
        this.service = builder.service;
        this.timestamp = Instant.now();
        this.correlationId = builder.correlationId;
        this.details = builder.details;
        this.retryable = builder.retryable;
    }

    // Getters
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public String getService() { return service; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public Map<String, Object> getDetails() { return details; }
    public boolean isRetryable() { return retryable; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String errorCode;
        private String message;
        private String service;
        private String correlationId;
        private Map<String, Object> details;
        private boolean retryable = false;

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder retryable(boolean retryable) {
            this.retryable = retryable;
            return this;
        }

        public IntegrationError build() {
            return new IntegrationError(this);
        }
    }
}
```

### Custom Integration Exceptions

```java
package com.example.aem.bmad.exceptions;

public class IntegrationException extends RuntimeException {

    private final String errorCode;
    private final String service;
    private final boolean retryable;

    public IntegrationException(String message, String errorCode, String service, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.service = service;
        this.retryable = retryable;
    }

    public IntegrationException(String message, Throwable cause, String errorCode, String service, boolean retryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.service = service;
        this.retryable = retryable;
    }

    public String getErrorCode() { return errorCode; }
    public String getService() { return service; }
    public boolean isRetryable() { return retryable; }

    // Factory methods for common error types
    public static IntegrationException timeout(String service) {
        return new IntegrationException(
            "Request to " + service + " timed out",
            "INTEGRATION_TIMEOUT",
            service,
            true
        );
    }

    public static IntegrationException connectionFailed(String service, Throwable cause) {
        return new IntegrationException(
            "Failed to connect to " + service,
            "CONNECTION_FAILED",
            service,
            true,
            cause
        );
    }

    public static IntegrationException authenticationFailed(String service) {
        return new IntegrationException(
            "Authentication failed for " + service,
            "AUTH_FAILED",
            service,
            false
        );
    }

    public static IntegrationException rateLimited(String service) {
        return new IntegrationException(
            "Rate limit exceeded for " + service,
            "RATE_LIMITED",
            service,
            true
        );
    }

    public static IntegrationException serverError(String service, int statusCode) {
        return new IntegrationException(
            service + " returned server error: " + statusCode,
            "SERVER_ERROR",
            service,
            true
        );
    }

    private IntegrationException(String message, String errorCode, String service, boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.service = service;
        this.retryable = retryable;
    }
}
```

### Error Response Mapping

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.exceptions.IntegrationException;
import com.example.aem.bmad.models.HttpResponse;

public class ErrorResponseMapper {

    private final String serviceName;

    public ErrorResponseMapper(String serviceName) {
        this.serviceName = serviceName;
    }

    public void validateResponse(HttpResponse response) throws IntegrationException {
        int statusCode = response.getStatusCode();

        if (statusCode >= 200 && statusCode < 300) {
            return; // Success
        }

        switch (statusCode) {
            case 401:
            case 403:
                throw IntegrationException.authenticationFailed(serviceName);
            case 429:
                throw IntegrationException.rateLimited(serviceName);
            case 408:
            case 504:
                throw IntegrationException.timeout(serviceName);
            case 500:
            case 502:
            case 503:
                throw IntegrationException.serverError(serviceName, statusCode);
            default:
                throw new IntegrationException(
                    "Unexpected response from " + serviceName + ": " + statusCode,
                    "UNEXPECTED_RESPONSE",
                    serviceName,
                    statusCode >= 500
                );
        }
    }
}
```

---

## Caching Strategies

### Cache Service Interface

```java
package com.example.aem.bmad.services;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface CacheService {

    /**
     * Get cached value
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Put value in cache with TTL
     */
    <T> void put(String key, T value, long ttl, TimeUnit unit);

    /**
     * Remove from cache
     */
    void remove(String key);

    /**
     * Clear all cached entries for a prefix
     */
    void clearByPrefix(String prefix);

    /**
     * Check if key exists in cache
     */
    boolean contains(String key);
}
```

### In-Memory Cache Implementation

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.CacheService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component(service = CacheService.class, immediate = true)
public class InMemoryCacheServiceImpl implements CacheService {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryCacheServiceImpl.class);

    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupExecutor;

    @Activate
    protected void activate() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
        LOG.info("In-memory cache service activated");
    }

    @Deactivate
    protected void deactivate() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
        cache.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                cache.remove(key);
            }
            return Optional.empty();
        }
        return Optional.of((T) entry.getValue());
    }

    @Override
    public <T> void put(String key, T value, long ttl, TimeUnit unit) {
        long expiresAt = System.currentTimeMillis() + unit.toMillis(ttl);
        cache.put(key, new CacheEntry<>(value, expiresAt));
        LOG.debug("Cached key: {} with TTL: {} {}", key, ttl, unit);
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public void clearByPrefix(String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
        LOG.info("Cleared cache entries with prefix: {}", prefix);
    }

    @Override
    public boolean contains(String key) {
        CacheEntry<?> entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    private void cleanupExpiredEntries() {
        int removed = 0;
        for (var iterator = cache.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            LOG.debug("Cleaned up {} expired cache entries", removed);
        }
    }

    private static class CacheEntry<T> {
        private final T value;
        private final long expiresAt;

        CacheEntry(T value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        T getValue() { return value; }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
```

### Caching Decorator for Services

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CachingDecorator {

    private static final Logger LOG = LoggerFactory.getLogger(CachingDecorator.class);

    private final CacheService cacheService;
    private final String cachePrefix;
    private final long defaultTtl;
    private final TimeUnit defaultUnit;

    public CachingDecorator(CacheService cacheService, String cachePrefix, long defaultTtl, TimeUnit defaultUnit) {
        this.cacheService = cacheService;
        this.cachePrefix = cachePrefix;
        this.defaultTtl = defaultTtl;
        this.defaultUnit = defaultUnit;
    }

    public <T> T getOrFetch(String key, Class<T> type, Supplier<T> fetcher) {
        return getOrFetch(key, type, fetcher, defaultTtl, defaultUnit);
    }

    public <T> T getOrFetch(String key, Class<T> type, Supplier<T> fetcher, long ttl, TimeUnit unit) {
        String cacheKey = cachePrefix + ":" + key;

        Optional<T> cached = cacheService.get(cacheKey, type);
        if (cached.isPresent()) {
            LOG.debug("Cache hit for key: {}", cacheKey);
            return cached.get();
        }

        LOG.debug("Cache miss for key: {}, fetching...", cacheKey);
        T value = fetcher.get();

        if (value != null) {
            cacheService.put(cacheKey, value, ttl, unit);
        }

        return value;
    }

    public void invalidate(String key) {
        cacheService.remove(cachePrefix + ":" + key);
    }

    public void invalidateAll() {
        cacheService.clearByPrefix(cachePrefix + ":");
    }
}
```

---

## Circuit Breaker Pattern

### Circuit Breaker Interface

```java
package com.example.aem.bmad.services;

import java.util.function.Supplier;

public interface CircuitBreaker {

    /**
     * Execute a function with circuit breaker protection
     */
    <T> T execute(Supplier<T> action) throws CircuitBreakerOpenException;

    /**
     * Execute a function with fallback
     */
    <T> T executeWithFallback(Supplier<T> action, Supplier<T> fallback);

    /**
     * Get current circuit state
     */
    CircuitState getState();

    /**
     * Reset circuit to closed state
     */
    void reset();

    /**
     * Get circuit breaker statistics
     */
    CircuitStats getStats();

    enum CircuitState {
        CLOSED,     // Normal operation
        OPEN,       // Failing, rejecting requests
        HALF_OPEN   // Testing if service recovered
    }

    class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    interface CircuitStats {
        long getSuccessCount();
        long getFailureCount();
        long getRejectedCount();
        double getFailureRate();
        long getLastFailureTimestamp();
    }
}
```

### Circuit Breaker Implementation

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CircuitBreakerImpl implements CircuitBreaker {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerImpl.class);

    private final String name;
    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final int halfOpenMaxCalls;

    private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong rejectedCount = new AtomicLong(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong halfOpenCalls = new AtomicLong(0);

    public CircuitBreakerImpl(String name, int failureThreshold, long resetTimeoutMs, int halfOpenMaxCalls) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
    }

    @Override
    public <T> T execute(Supplier<T> action) throws CircuitBreakerOpenException {
        if (!allowRequest()) {
            rejectedCount.incrementAndGet();
            throw new CircuitBreakerOpenException("Circuit breaker '" + name + "' is open");
        }

        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    @Override
    public <T> T executeWithFallback(Supplier<T> action, Supplier<T> fallback) {
        try {
            return execute(action);
        } catch (CircuitBreakerOpenException e) {
            LOG.warn("Circuit breaker open, using fallback for: {}", name);
            return fallback.get();
        } catch (Exception e) {
            LOG.warn("Action failed, using fallback for: {}", name, e);
            return fallback.get();
        }
    }

    private boolean allowRequest() {
        CircuitState currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                if (System.currentTimeMillis() - lastFailureTime.get() >= resetTimeoutMs) {
                    if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                        halfOpenCalls.set(0);
                        LOG.info("Circuit breaker '{}' transitioning to HALF_OPEN", name);
                    }
                    return true;
                }
                return false;

            case HALF_OPEN:
                return halfOpenCalls.incrementAndGet() <= halfOpenMaxCalls;

            default:
                return false;
        }
    }

    private void onSuccess() {
        successCount.incrementAndGet();

        if (state.get() == CircuitState.HALF_OPEN) {
            if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.CLOSED)) {
                failureCount.set(0);
                LOG.info("Circuit breaker '{}' closed after successful half-open test", name);
            }
        }
    }

    private void onFailure() {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        CircuitState currentState = state.get();

        if (currentState == CircuitState.HALF_OPEN) {
            state.set(CircuitState.OPEN);
            LOG.warn("Circuit breaker '{}' reopened after half-open failure", name);
        } else if (currentState == CircuitState.CLOSED && failureCount.get() >= failureThreshold) {
            state.set(CircuitState.OPEN);
            LOG.warn("Circuit breaker '{}' opened after {} failures", name, failureThreshold);
        }
    }

    @Override
    public CircuitState getState() {
        return state.get();
    }

    @Override
    public void reset() {
        state.set(CircuitState.CLOSED);
        failureCount.set(0);
        LOG.info("Circuit breaker '{}' manually reset", name);
    }

    @Override
    public CircuitStats getStats() {
        return new CircuitStatsImpl(
            successCount.get(),
            failureCount.get(),
            rejectedCount.get(),
            lastFailureTime.get()
        );
    }

    private static class CircuitStatsImpl implements CircuitStats {
        private final long successCount;
        private final long failureCount;
        private final long rejectedCount;
        private final long lastFailureTimestamp;

        CircuitStatsImpl(long successCount, long failureCount, long rejectedCount, long lastFailureTimestamp) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.rejectedCount = rejectedCount;
            this.lastFailureTimestamp = lastFailureTimestamp;
        }

        @Override public long getSuccessCount() { return successCount; }
        @Override public long getFailureCount() { return failureCount; }
        @Override public long getRejectedCount() { return rejectedCount; }
        @Override public long getLastFailureTimestamp() { return lastFailureTimestamp; }

        @Override
        public double getFailureRate() {
            long total = successCount + failureCount;
            return total > 0 ? (double) failureCount / total : 0.0;
        }
    }
}
```

### Circuit Breaker Factory

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.CircuitBreaker;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = CircuitBreakerFactory.class, immediate = true)
@Designate(ocd = CircuitBreakerFactory.Config.class)
public class CircuitBreakerFactory {

    @ObjectClassDefinition(name = "BMAD Circuit Breaker Configuration")
    public @interface Config {

        @AttributeDefinition(name = "Default Failure Threshold")
        int defaultFailureThreshold() default 5;

        @AttributeDefinition(name = "Default Reset Timeout (ms)")
        long defaultResetTimeoutMs() default 30000;

        @AttributeDefinition(name = "Half-Open Max Calls")
        int halfOpenMaxCalls() default 3;
    }

    private Config config;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    public CircuitBreaker getOrCreate(String name) {
        return circuitBreakers.computeIfAbsent(name, n ->
            new CircuitBreakerImpl(
                n,
                config.defaultFailureThreshold(),
                config.defaultResetTimeoutMs(),
                config.halfOpenMaxCalls()
            )
        );
    }

    public CircuitBreaker getOrCreate(String name, int failureThreshold, long resetTimeoutMs) {
        return circuitBreakers.computeIfAbsent(name, n ->
            new CircuitBreakerImpl(n, failureThreshold, resetTimeoutMs, config.halfOpenMaxCalls())
        );
    }
}
```

---

## Retry Logic

### Retry Policy Interface

```java
package com.example.aem.bmad.services;

import java.util.function.Predicate;
import java.util.function.Supplier;

public interface RetryPolicy {

    /**
     * Execute with retry logic
     */
    <T> T execute(Supplier<T> action) throws Exception;

    /**
     * Execute with retry logic and custom exception handler
     */
    <T> T execute(Supplier<T> action, Predicate<Exception> shouldRetry) throws Exception;
}
```

### Exponential Backoff Retry Implementation

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class ExponentialBackoffRetry implements RetryPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(ExponentialBackoffRetry.class);

    private final int maxRetries;
    private final long initialDelayMs;
    private final double multiplier;
    private final long maxDelayMs;
    private final Predicate<Exception> defaultShouldRetry;

    private ExponentialBackoffRetry(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.initialDelayMs = builder.initialDelayMs;
        this.multiplier = builder.multiplier;
        this.maxDelayMs = builder.maxDelayMs;
        this.defaultShouldRetry = builder.shouldRetry;
    }

    @Override
    public <T> T execute(Supplier<T> action) throws Exception {
        return execute(action, defaultShouldRetry);
    }

    @Override
    public <T> T execute(Supplier<T> action, Predicate<Exception> shouldRetry) throws Exception {
        Exception lastException = null;
        long delay = initialDelayMs;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;

                if (attempt > maxRetries || !shouldRetry.test(e)) {
                    LOG.error("Retry exhausted or non-retryable exception after {} attempts", attempt, e);
                    throw e;
                }

                LOG.warn("Attempt {} failed, retrying in {} ms: {}", attempt, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                delay = Math.min((long) (delay * multiplier), maxDelayMs);
            }
        }

        throw lastException;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxRetries = 3;
        private long initialDelayMs = 1000;
        private double multiplier = 2.0;
        private long maxDelayMs = 30000;
        private Predicate<Exception> shouldRetry = e -> true;

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialDelay(long delayMs) {
            this.initialDelayMs = delayMs;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder maxDelay(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public Builder shouldRetry(Predicate<Exception> shouldRetry) {
            this.shouldRetry = shouldRetry;
            return this;
        }

        public ExponentialBackoffRetry build() {
            return new ExponentialBackoffRetry(this);
        }
    }
}
```

---

## Timeout Configuration

### Timeout-Aware HTTP Client Wrapper

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.exceptions.IntegrationException;
import com.example.aem.bmad.models.HttpResponse;
import com.example.aem.bmad.services.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

public class TimeoutHttpClientWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(TimeoutHttpClientWrapper.class);

    private final HttpClientService httpClient;
    private final ExecutorService executor;
    private final long defaultTimeoutMs;

    public TimeoutHttpClientWrapper(HttpClientService httpClient, long defaultTimeoutMs) {
        this.httpClient = httpClient;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.executor = Executors.newCachedThreadPool();
    }

    public HttpResponse getWithTimeout(String url, Map<String, String> headers) {
        return getWithTimeout(url, headers, defaultTimeoutMs);
    }

    public HttpResponse getWithTimeout(String url, Map<String, String> headers, long timeoutMs) {
        return executeWithTimeout(() -> httpClient.get(url, headers), url, timeoutMs);
    }

    public HttpResponse postWithTimeout(String url, String body, Map<String, String> headers) {
        return postWithTimeout(url, body, headers, defaultTimeoutMs);
    }

    public HttpResponse postWithTimeout(String url, String body, Map<String, String> headers, long timeoutMs) {
        return executeWithTimeout(() -> httpClient.post(url, body, headers), url, timeoutMs);
    }

    private HttpResponse executeWithTimeout(Callable<HttpResponse> task, String url, long timeoutMs) {
        Future<HttpResponse> future = executor.submit(task);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            LOG.error("Request timed out after {} ms: {}", timeoutMs, url);
            throw IntegrationException.timeout(extractServiceName(url));
        } catch (ExecutionException e) {
            LOG.error("Request execution failed: {}", url, e.getCause());
            throw new RuntimeException("Request failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
    }

    private String extractServiceName(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
```

---

## Monitoring and Observability

### Integration Metrics Service

```java
package com.example.aem.bmad.services;

import java.util.Map;

public interface IntegrationMetricsService {

    /**
     * Record a successful integration call
     */
    void recordSuccess(String serviceName, String operation, long durationMs);

    /**
     * Record a failed integration call
     */
    void recordFailure(String serviceName, String operation, String errorCode, long durationMs);

    /**
     * Record a cache hit
     */
    void recordCacheHit(String serviceName, String cacheKey);

    /**
     * Record a cache miss
     */
    void recordCacheMiss(String serviceName, String cacheKey);

    /**
     * Record circuit breaker state change
     */
    void recordCircuitBreakerStateChange(String serviceName, String newState);

    /**
     * Get metrics summary for a service
     */
    Map<String, Object> getMetrics(String serviceName);

    /**
     * Get all metrics
     */
    Map<String, Map<String, Object>> getAllMetrics();
}
```

### Metrics Implementation

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.IntegrationMetricsService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Component(service = IntegrationMetricsService.class, immediate = true)
public class IntegrationMetricsServiceImpl implements IntegrationMetricsService {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationMetricsServiceImpl.class);

    private final Map<String, ServiceMetrics> metricsMap = new ConcurrentHashMap<>();

    @Override
    public void recordSuccess(String serviceName, String operation, long durationMs) {
        getOrCreateMetrics(serviceName).recordSuccess(operation, durationMs);
    }

    @Override
    public void recordFailure(String serviceName, String operation, String errorCode, long durationMs) {
        getOrCreateMetrics(serviceName).recordFailure(operation, errorCode, durationMs);
        LOG.warn("Integration failure recorded: service={}, operation={}, errorCode={}, duration={}ms",
            serviceName, operation, errorCode, durationMs);
    }

    @Override
    public void recordCacheHit(String serviceName, String cacheKey) {
        getOrCreateMetrics(serviceName).recordCacheHit();
    }

    @Override
    public void recordCacheMiss(String serviceName, String cacheKey) {
        getOrCreateMetrics(serviceName).recordCacheMiss();
    }

    @Override
    public void recordCircuitBreakerStateChange(String serviceName, String newState) {
        getOrCreateMetrics(serviceName).recordCircuitBreakerState(newState);
        LOG.info("Circuit breaker state change: service={}, newState={}", serviceName, newState);
    }

    @Override
    public Map<String, Object> getMetrics(String serviceName) {
        ServiceMetrics metrics = metricsMap.get(serviceName);
        return metrics != null ? metrics.toMap() : Collections.emptyMap();
    }

    @Override
    public Map<String, Map<String, Object>> getAllMetrics() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        metricsMap.forEach((name, metrics) -> result.put(name, metrics.toMap()));
        return result;
    }

    private ServiceMetrics getOrCreateMetrics(String serviceName) {
        return metricsMap.computeIfAbsent(serviceName, ServiceMetrics::new);
    }

    private static class ServiceMetrics {
        private final String serviceName;
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final LongAdder cacheHits = new LongAdder();
        private final LongAdder cacheMisses = new LongAdder();
        private final LongAdder totalDurationMs = new LongAdder();
        private final AtomicLong minDurationMs = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxDurationMs = new AtomicLong(0);
        private final Map<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
        private volatile String circuitBreakerState = "CLOSED";

        ServiceMetrics(String serviceName) {
            this.serviceName = serviceName;
        }

        void recordSuccess(String operation, long durationMs) {
            successCount.increment();
            recordDuration(durationMs);
        }

        void recordFailure(String operation, String errorCode, long durationMs) {
            failureCount.increment();
            errorCounts.computeIfAbsent(errorCode, k -> new LongAdder()).increment();
            recordDuration(durationMs);
        }

        void recordCacheHit() {
            cacheHits.increment();
        }

        void recordCacheMiss() {
            cacheMisses.increment();
        }

        void recordCircuitBreakerState(String state) {
            this.circuitBreakerState = state;
        }

        private void recordDuration(long durationMs) {
            totalDurationMs.add(durationMs);
            minDurationMs.updateAndGet(current -> Math.min(current, durationMs));
            maxDurationMs.updateAndGet(current -> Math.max(current, durationMs));
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            long total = successCount.sum() + failureCount.sum();

            map.put("serviceName", serviceName);
            map.put("successCount", successCount.sum());
            map.put("failureCount", failureCount.sum());
            map.put("totalRequests", total);
            map.put("successRate", total > 0 ? (double) successCount.sum() / total : 0.0);
            map.put("cacheHits", cacheHits.sum());
            map.put("cacheMisses", cacheMisses.sum());
            map.put("cacheHitRate", getCacheHitRate());
            map.put("avgDurationMs", total > 0 ? totalDurationMs.sum() / total : 0);
            map.put("minDurationMs", minDurationMs.get() == Long.MAX_VALUE ? 0 : minDurationMs.get());
            map.put("maxDurationMs", maxDurationMs.get());
            map.put("circuitBreakerState", circuitBreakerState);

            Map<String, Long> errors = new HashMap<>();
            errorCounts.forEach((code, count) -> errors.put(code, count.sum()));
            map.put("errorsByCode", errors);

            return map;
        }

        private double getCacheHitRate() {
            long total = cacheHits.sum() + cacheMisses.sum();
            return total > 0 ? (double) cacheHits.sum() / total : 0.0;
        }
    }
}
```

### Metrics Servlet

```java
package com.example.aem.bmad.servlets;

import com.example.aem.bmad.services.IntegrationMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.*;

import javax.servlet.Servlet;
import java.io.IOException;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/bmad/metrics",
        "sling.servlet.methods=GET"
    }
)
public class IntegrationMetricsServlet extends SlingSafeMethodsServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private IntegrationMetricsService metricsService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        String serviceName = request.getParameter("service");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (serviceName != null && !serviceName.isEmpty()) {
            MAPPER.writeValue(response.getWriter(), metricsService.getMetrics(serviceName));
        } else {
            MAPPER.writeValue(response.getWriter(), metricsService.getAllMetrics());
        }
    }
}
```

---

## Secret Management

### Secret Provider Interface

```java
package com.example.aem.bmad.services;

import java.util.Optional;

public interface SecretProvider {

    /**
     * Get a secret value by name
     */
    Optional<String> getSecret(String secretName);

    /**
     * Get a secret with default fallback
     */
    String getSecretOrDefault(String secretName, String defaultValue);

    /**
     * Check if a secret exists
     */
    boolean hasSecret(String secretName);
}
```

### Cloud Manager Secret Provider

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.SecretProvider;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Retrieves secrets from environment variables set by Cloud Manager.
 * Secrets should be configured in Cloud Manager and exposed as env vars.
 */
@Component(service = SecretProvider.class, immediate = true)
public class CloudManagerSecretProvider implements SecretProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CloudManagerSecretProvider.class);
    private static final String SECRET_PREFIX = "BMAD_SECRET_";

    @Override
    public Optional<String> getSecret(String secretName) {
        String envVarName = SECRET_PREFIX + secretName.toUpperCase().replace(".", "_");
        String value = System.getenv(envVarName);

        if (value == null || value.isEmpty()) {
            LOG.debug("Secret not found: {}", secretName);
            return Optional.empty();
        }

        LOG.debug("Secret retrieved: {}", secretName);
        return Optional.of(value);
    }

    @Override
    public String getSecretOrDefault(String secretName, String defaultValue) {
        return getSecret(secretName).orElse(defaultValue);
    }

    @Override
    public boolean hasSecret(String secretName) {
        return getSecret(secretName).isPresent();
    }
}
```

---

## Rate Limiting

### Rate Limiter Interface

```java
package com.example.aem.bmad.services;

public interface RateLimiter {

    /**
     * Try to acquire a permit
     * @return true if permit acquired, false if rate limited
     */
    boolean tryAcquire();

    /**
     * Acquire a permit, blocking if necessary
     */
    void acquire() throws InterruptedException;

    /**
     * Get current available permits
     */
    int getAvailablePermits();
}
```

### Token Bucket Rate Limiter

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter implements RateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    private final int maxTokens;
    private final long refillIntervalMs;
    private final int tokensPerRefill;

    private final AtomicLong availableTokens;
    private final AtomicLong lastRefillTime;
    private final ReentrantLock refillLock = new ReentrantLock();

    public TokenBucketRateLimiter(int maxTokens, long refillIntervalMs, int tokensPerRefill) {
        this.maxTokens = maxTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.tokensPerRefill = tokensPerRefill;
        this.availableTokens = new AtomicLong(maxTokens);
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public boolean tryAcquire() {
        refill();

        while (true) {
            long current = availableTokens.get();
            if (current <= 0) {
                LOG.debug("Rate limit exceeded, no tokens available");
                return false;
            }
            if (availableTokens.compareAndSet(current, current - 1)) {
                return true;
            }
        }
    }

    @Override
    public void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            Thread.sleep(refillIntervalMs / tokensPerRefill);
        }
    }

    @Override
    public int getAvailablePermits() {
        refill();
        return (int) availableTokens.get();
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long lastRefill = lastRefillTime.get();
        long elapsed = now - lastRefill;

        if (elapsed >= refillIntervalMs) {
            if (refillLock.tryLock()) {
                try {
                    // Recalculate in case another thread already refilled
                    elapsed = now - lastRefillTime.get();
                    if (elapsed >= refillIntervalMs) {
                        long tokensToAdd = (elapsed / refillIntervalMs) * tokensPerRefill;
                        long newTokens = Math.min(maxTokens, availableTokens.get() + tokensToAdd);
                        availableTokens.set(newTokens);
                        lastRefillTime.set(now);
                    }
                } finally {
                    refillLock.unlock();
                }
            }
        }
    }
}
```

---

## Traceability

| Spec ID | Pattern | Description | Status |
|---------|---------|-------------|--------|
| INT-BP-001 | Error Handling | Standardized error model and exceptions | Documented |
| INT-BP-002 | Caching | In-memory cache with TTL | Documented |
| INT-BP-003 | Circuit Breaker | Fail-fast protection pattern | Documented |
| INT-BP-004 | Retry Logic | Exponential backoff retry | Documented |
| INT-BP-005 | Timeout | Configurable request timeouts | Documented |
| INT-BP-006 | Monitoring | Metrics collection and reporting | Documented |
| INT-BP-007 | Secrets | Cloud Manager secret management | Documented |
| INT-BP-008 | Rate Limiting | Token bucket rate limiter | Documented |
