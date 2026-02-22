# Adobe Target Integration

This document provides comprehensive patterns and implementation guidance for integrating Adobe Target with AEM as a Cloud Service, covering personalization, A/B testing, automated optimization, and Experience Cloud integration.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Target Setup and Configuration](#target-setup-and-configuration)
3. [Client-Side Integration (at.js)](#client-side-integration-atjs)
4. [Server-Side Decisioning](#server-side-decisioning)
5. [Experience Targeting (XT)](#experience-targeting-xt)
6. [A/B and Multivariate Testing](#ab-and-multivariate-testing)
7. [Automated Personalization](#automated-personalization)
8. [Recommendations](#recommendations)
9. [Audiences and Segments](#audiences-and-segments)
10. [AEM Experience Fragments as Offers](#aem-experience-fragments-as-offers)
11. [Analytics for Target (A4T)](#analytics-for-target-a4t)
12. [Visual Experience Composer Integration](#visual-experience-composer-integration)
13. [Form-Based Experience Composer](#form-based-experience-composer)
14. [OSGi Services](#osgi-services)
15. [Testing and QA](#testing-and-qa)
16. [Best Practices](#best-practices)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AEM as a Cloud Service                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────────────────┐ │
│  │ Experience      │    │ Target          │    │ Personalization          │ │
│  │ Fragments       │───▶│ Integration     │◀──▶│ Service                  │ │
│  │ (Offers)        │    │ Service         │    │ (Server-Side)            │ │
│  └─────────────────┘    └────────┬────────┘    └──────────────────────────┘ │
│                                  │                                           │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                         Page / Component Layer                           │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │ │
│  │  │ Target          │  │ Personalization │  │ at.js Integration       │  │ │
│  │  │ Components      │  │ Containers      │  │ (Client-Side)           │  │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────┬─────────────────────────────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
                    ▼                   ▼                   ▼
┌─────────────────────────┐ ┌─────────────────────┐ ┌─────────────────────────┐
│     Adobe Target        │ │  Adobe Analytics    │ │  Experience Cloud ID    │
│     Edge Network        │ │  (A4T Reporting)    │ │  Service                │
│  ┌─────────────────┐    │ │                     │ │                         │
│  │ Delivery API    │    │ │                     │ │                         │
│  │ Decisioning     │    │ │                     │ │                         │
│  │ Engine          │    │ │                     │ │                         │
│  └─────────────────┘    │ │                     │ │                         │
└─────────────────────────┘ └─────────────────────┘ └─────────────────────────┘
```

### Integration Patterns

| Pattern | Use Case | Latency | Complexity |
|---------|----------|---------|------------|
| Client-Side (at.js) | Visual testing, simple personalization | Medium | Low |
| Server-Side Decisioning | Performance-critical, edge rendering | Low | Medium |
| Hybrid | Complex scenarios, partial server-side | Variable | High |
| On-Device Decisioning | Offline-first, zero latency | Lowest | Medium |

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| TargetService | Server-side Target API calls | `core/` bundle |
| PersonalizationService | Content personalization logic | `core/` bundle |
| Target Components | AEM components with Target integration | `ui.apps/` |
| Experience Fragments | Content offers for Target activities | `ui.content/` |
| at.js | Client-side Target library | External (Adobe CDN) |

---

## Target Setup and Configuration

### Target Property Configuration

```java
package com.example.aem.bmad.core.config;

import org.osgi.service.metatype.annotations.*;

@ObjectClassDefinition(
    name = "BMAD Adobe Target Configuration",
    description = "Configuration for Adobe Target integration"
)
public @interface AdobeTargetConfig {

    @AttributeDefinition(
        name = "Client Code",
        description = "Adobe Target client code"
    )
    String clientCode();

    @AttributeDefinition(
        name = "IMS Organization ID",
        description = "Experience Cloud Organization ID"
    )
    String imsOrgId();

    @AttributeDefinition(
        name = "Target Property Token",
        description = "Target property token for workspace isolation"
    )
    String propertyToken();

    @AttributeDefinition(
        name = "Environment ID",
        description = "Target environment ID (production, staging, development)"
    )
    String environmentId() default "production";

    @AttributeDefinition(
        name = "Server Domain",
        description = "Target server domain"
    )
    String serverDomain() default "tt.omtrdc.net";

    @AttributeDefinition(
        name = "Decisioning Method",
        description = "Decisioning method (server-side, on-device, hybrid)"
    )
    String decisioningMethod() default "server-side";

    @AttributeDefinition(
        name = "Enable A4T",
        description = "Enable Analytics for Target reporting"
    )
    boolean enableA4T() default true;

    @AttributeDefinition(
        name = "Global Mbox Name",
        description = "Global mbox name for page-level targeting"
    )
    String globalMboxName() default "target-global-mbox";

    @AttributeDefinition(
        name = "Timeout (ms)",
        description = "Target API timeout in milliseconds"
    )
    int timeout() default 3000;

    @AttributeDefinition(
        name = "Default Content Visible",
        description = "Show default content while Target loads"
    )
    boolean defaultContentVisible() default true;
}
```

### Target Cloud Configuration (AEM)

```xml
<!-- /apps/aem-bmad-showcase/config/com.day.cq.personalization.impl.TargetConfigImpl.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="sling:OsgiConfig"
          clientcode="${ADOBE_TARGET_CLIENT_CODE}"
          imsorg="${ADOBE_IMS_ORG_ID}"
          server="tt.omtrdc.net"
          timeout="5000"/>
```

---

## Client-Side Integration (at.js)

### at.js Setup in AEM

```html
<!-- headlibs.html - Load at.js -->
<sly data-sly-use.target="com.example.aem.bmad.core.models.TargetConfigModel">
    <!-- Prehiding snippet to prevent flicker -->
    <sly data-sly-test="${target.enabled}">
        <style>
            .at-hide {
                opacity: 0 !important;
            }
        </style>
        <script>
            (function(win, doc, style, timeout) {
                var STYLE_ID = 'at-body-style';

                function getParent() {
                    return doc.getElementsByTagName('head')[0];
                }

                function addStyle(parent, id, def) {
                    if (!parent) return;
                    var style = doc.createElement('style');
                    style.id = id;
                    style.innerHTML = def;
                    parent.appendChild(style);
                }

                function removeStyle(parent, id) {
                    if (!parent) return;
                    var style = doc.getElementById(id);
                    if (!style) return;
                    parent.removeChild(style);
                }

                addStyle(getParent(), STYLE_ID, style);

                setTimeout(function() {
                    removeStyle(getParent(), STYLE_ID);
                }, timeout);
            }(window, document, "body {opacity: 0 !important}", 3000));
        </script>

        <!-- at.js 2.x -->
        <script src="https://assets.adobedtm.com/your-property/at.js" async></script>
    </sly>
</sly>
```

### at.js Configuration Model

```java
package com.example.aem.bmad.core.models;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.example.aem.bmad.core.config.AdobeTargetConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.*;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = {TargetConfigModel.class, ComponentExporter.class},
    resourceType = "bmad/components/page"
)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION
)
public class TargetConfigModel implements ComponentExporter {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private AdobeTargetConfig targetConfig;

    private Map<String, Object> atjsConfig;
    private boolean enabled;

    @PostConstruct
    protected void init() {
        enabled = targetConfig != null && targetConfig.clientCode() != null;

        if (enabled) {
            buildAtjsConfig();
        }
    }

    private void buildAtjsConfig() {
        atjsConfig = new LinkedHashMap<>();

        // Core settings
        atjsConfig.put("clientCode", targetConfig.clientCode());
        atjsConfig.put("imsOrgId", targetConfig.imsOrgId());
        atjsConfig.put("serverDomain", targetConfig.serverDomain());
        atjsConfig.put("timeout", targetConfig.timeout());
        atjsConfig.put("globalMboxName", targetConfig.globalMboxName());

        // Feature flags
        atjsConfig.put("bodyHiddenStyle", "body {opacity: 0 !important}");
        atjsConfig.put("bodyHidingEnabled", !targetConfig.defaultContentVisible());
        atjsConfig.put("deviceIdLifetime", 63244800000L); // 2 years

        // Decisioning
        atjsConfig.put("decisioningMethod", targetConfig.decisioningMethod());

        // Visitor API
        atjsConfig.put("visitorApiTimeout", 2000);

        // A4T
        atjsConfig.put("analyticsLogging", targetConfig.enableA4T() ? "client_side" : "disabled");
        atjsConfig.put("supplementalDataIdParamTimeout", 30);

        // Cross-domain
        atjsConfig.put("crossDomain", "enabled");
        atjsConfig.put("secureOnly", true);

        // Property token
        if (targetConfig.propertyToken() != null && !targetConfig.propertyToken().isEmpty()) {
            atjsConfig.put("propertyToken", targetConfig.propertyToken());
        }
    }

    @JsonProperty("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JsonProperty("atjsConfig")
    public Map<String, Object> getAtjsConfig() {
        return atjsConfig;
    }

    @JsonProperty("atjsConfigJson")
    public String getAtjsConfigJson() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(atjsConfig);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public String getExportedType() {
        return "bmad/components/page";
    }
}
```

### at.js Initialization Script

```javascript
// target-init.js
(function() {
    'use strict';

    // Wait for at.js to load
    window.targetGlobalSettings = {
        clientCode: window.bmadTargetConfig.clientCode,
        imsOrgId: window.bmadTargetConfig.imsOrgId,
        serverDomain: window.bmadTargetConfig.serverDomain,
        timeout: window.bmadTargetConfig.timeout,
        globalMboxName: window.bmadTargetConfig.globalMboxName,
        bodyHiddenStyle: 'body {opacity: 0 !important}',
        bodyHidingEnabled: true,
        deviceIdLifetime: 63244800000,
        sessionIdLifetime: 1860000,
        selectorsPollingTimeout: 5000,
        visitorApiTimeout: 2000,
        overrideMboxEdgeServer: false,
        overrideMboxEdgeServerTimeout: 1860000,
        optoutEnabled: false,
        optinEnabled: true,
        secureOnly: true,
        supplementalDataIdParamTimeout: 30,
        authoringScriptUrl: '//cdn.tt.omtrdc.net/cdn/target-vec.js',
        urlSizeLimit: 2048,
        endpoint: '/rest/v1/delivery',
        pageLoadEnabled: true,
        viewsEnabled: true,
        analyticsLogging: 'client_side',
        serverState: window.bmadTargetServerState || null,
        decisioningMethod: window.bmadTargetConfig.decisioningMethod || 'server-side',
        pollingInterval: 300000,
        artifactLocation: window.bmadTargetConfig.artifactLocation || '',
        artifactFormat: 'json',
        artifactPayload: null,
        environmentId: window.bmadTargetConfig.environmentId || null,
        cdnEnvironment: 'production',
        telemetryEnabled: true,
        aepSandboxId: null,
        aepSandboxName: null
    };

    // Page load callback
    document.addEventListener('at-library-loaded', function() {
        adobe.target.getOffers({
            request: {
                execute: {
                    pageLoad: {}
                }
            }
        })
        .then(function(response) {
            adobe.target.applyOffers({response: response});
            document.dispatchEvent(new CustomEvent('target:offers-applied'));
        })
        .catch(function(error) {
            console.error('Target getOffers error:', error);
            document.body.classList.remove('at-hide');
        });
    });

    // Track response tokens
    document.addEventListener('at-request-succeeded', function(e) {
        var responseTokens = e.detail.responseTokens;

        if (responseTokens && responseTokens.length > 0) {
            responseTokens.forEach(function(token) {
                console.log('Response Token:', token);
                trackToAnalytics(token);
            });
        }
    });

    function trackToAnalytics(token) {
        if (!window.adobeDataLayer) return;

        window.adobeDataLayer.push({
            event: 'target:activity-rendered',
            target: {
                activityName: token['activity.name'],
                activityId: token['activity.id'],
                experienceName: token['experience.name'],
                experienceId: token['experience.id'],
                offerId: token['offer.id'],
                offerName: token['offer.name']
            }
        });
    }

})();
```

---

## Server-Side Decisioning

### Target Delivery API Service

```java
package com.example.aem.bmad.core.services;

import java.util.List;
import java.util.Map;

/**
 * Service for Adobe Target server-side decisioning
 */
public interface TargetDeliveryService {

    /**
     * Execute Target delivery request
     */
    TargetResponse executeDelivery(TargetRequest request);

    /**
     * Get offers for specific mbox
     */
    List<TargetOffer> getOffers(String mboxName, Map<String, Object> parameters);

    /**
     * Get offers for page load (global mbox)
     */
    List<TargetOffer> getPageLoadOffers(Map<String, Object> parameters);

    /**
     * Prefetch offers for mboxes
     */
    Map<String, List<TargetOffer>> prefetchOffers(List<String> mboxNames, Map<String, Object> parameters);

    /**
     * Send notification for display/click
     */
    void sendNotification(TargetNotification notification);

    /**
     * Check if Target is available
     */
    boolean isAvailable();

    /**
     * Target request object
     */
    class TargetRequest {
        private String visitorId;
        private String sessionId;
        private String tntId;
        private Map<String, Object> context;
        private Map<String, Object> profile;
        private Map<String, Object> execute;
        private Map<String, Object> prefetch;
        private List<TargetNotification> notifications;

        // Getters and setters
        public String getVisitorId() { return visitorId; }
        public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getTntId() { return tntId; }
        public void setTntId(String tntId) { this.tntId = tntId; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
        public Map<String, Object> getProfile() { return profile; }
        public void setProfile(Map<String, Object> profile) { this.profile = profile; }
        public Map<String, Object> getExecute() { return execute; }
        public void setExecute(Map<String, Object> execute) { this.execute = execute; }
        public Map<String, Object> getPrefetch() { return prefetch; }
        public void setPrefetch(Map<String, Object> prefetch) { this.prefetch = prefetch; }
        public List<TargetNotification> getNotifications() { return notifications; }
        public void setNotifications(List<TargetNotification> notifications) { this.notifications = notifications; }
    }

    /**
     * Target response object
     */
    class TargetResponse {
        private String requestId;
        private String visitorState;
        private Map<String, Object> execute;
        private Map<String, Object> prefetch;
        private List<TargetOffer> pageLoadOffers;
        private Map<String, List<TargetOffer>> mboxOffers;

        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getVisitorState() { return visitorState; }
        public void setVisitorState(String visitorState) { this.visitorState = visitorState; }
        public Map<String, Object> getExecute() { return execute; }
        public void setExecute(Map<String, Object> execute) { this.execute = execute; }
        public Map<String, Object> getPrefetch() { return prefetch; }
        public void setPrefetch(Map<String, Object> prefetch) { this.prefetch = prefetch; }
        public List<TargetOffer> getPageLoadOffers() { return pageLoadOffers; }
        public void setPageLoadOffers(List<TargetOffer> pageLoadOffers) { this.pageLoadOffers = pageLoadOffers; }
        public Map<String, List<TargetOffer>> getMboxOffers() { return mboxOffers; }
        public void setMboxOffers(Map<String, List<TargetOffer>> mboxOffers) { this.mboxOffers = mboxOffers; }
    }

    /**
     * Target offer
     */
    class TargetOffer {
        private String id;
        private String type; // html, json, redirect, dynamic
        private String content;
        private Map<String, Object> data;
        private String activityId;
        private String activityName;
        private String experienceId;
        private String experienceName;
        private Map<String, String> responseTokens;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        public String getActivityId() { return activityId; }
        public void setActivityId(String activityId) { this.activityId = activityId; }
        public String getActivityName() { return activityName; }
        public void setActivityName(String activityName) { this.activityName = activityName; }
        public String getExperienceId() { return experienceId; }
        public void setExperienceId(String experienceId) { this.experienceId = experienceId; }
        public String getExperienceName() { return experienceName; }
        public void setExperienceName(String experienceName) { this.experienceName = experienceName; }
        public Map<String, String> getResponseTokens() { return responseTokens; }
        public void setResponseTokens(Map<String, String> responseTokens) { this.responseTokens = responseTokens; }
    }

    /**
     * Target notification for impressions/clicks
     */
    class TargetNotification {
        private String id;
        private String type; // display, click
        private long timestamp;
        private String mbox;
        private List<String> tokens;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getMbox() { return mbox; }
        public void setMbox(String mbox) { this.mbox = mbox; }
        public List<String> getTokens() { return tokens; }
        public void setTokens(List<String> tokens) { this.tokens = tokens; }
    }
}
```

### Target Delivery Service Implementation

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.config.AdobeTargetConfig;
import com.example.aem.bmad.core.services.HttpClientService;
import com.example.aem.bmad.core.services.TargetDeliveryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = TargetDeliveryService.class, immediate = true)
@Designate(ocd = AdobeTargetConfig.class)
public class TargetDeliveryServiceImpl implements TargetDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(TargetDeliveryServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private HttpClientService httpClient;

    private AdobeTargetConfig config;
    private String deliveryApiUrl;

    @Activate
    @Modified
    protected void activate(AdobeTargetConfig config) {
        this.config = config;
        this.deliveryApiUrl = String.format(
            "https://%s.%s/rest/v1/delivery",
            config.clientCode(),
            config.serverDomain()
        );
        LOG.info("Target Delivery Service configured for client: {}", config.clientCode());
    }

    @Override
    public TargetResponse executeDelivery(TargetRequest request) {
        try {
            Map<String, Object> payload = buildDeliveryPayload(request);
            String jsonPayload = MAPPER.writeValueAsString(payload);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("cache-control", "no-cache");

            String url = deliveryApiUrl + "?client=" + config.clientCode() +
                "&sessionId=" + (request.getSessionId() != null ? request.getSessionId() : generateSessionId());

            var response = httpClient.post(url, jsonPayload, headers);

            if (response.isSuccess()) {
                return parseDeliveryResponse(response.getBody());
            } else {
                LOG.error("Target delivery failed: {}", response.getBody());
                return new TargetResponse();
            }
        } catch (Exception e) {
            LOG.error("Error executing Target delivery", e);
            return new TargetResponse();
        }
    }

    @Override
    public List<TargetOffer> getOffers(String mboxName, Map<String, Object> parameters) {
        TargetRequest request = new TargetRequest();
        request.setSessionId(generateSessionId());

        Map<String, Object> execute = new HashMap<>();
        List<Map<String, Object>> mboxes = new ArrayList<>();

        Map<String, Object> mbox = new HashMap<>();
        mbox.put("name", mboxName);
        mbox.put("index", 0);
        if (parameters != null) {
            mbox.put("parameters", parameters);
        }
        mboxes.add(mbox);

        execute.put("mboxes", mboxes);
        request.setExecute(execute);

        TargetResponse response = executeDelivery(request);

        if (response.getMboxOffers() != null && response.getMboxOffers().containsKey(mboxName)) {
            return response.getMboxOffers().get(mboxName);
        }

        return Collections.emptyList();
    }

    @Override
    public List<TargetOffer> getPageLoadOffers(Map<String, Object> parameters) {
        TargetRequest request = new TargetRequest();
        request.setSessionId(generateSessionId());

        Map<String, Object> execute = new HashMap<>();
        Map<String, Object> pageLoad = new HashMap<>();

        if (parameters != null) {
            pageLoad.put("parameters", parameters);
        }

        execute.put("pageLoad", pageLoad);
        request.setExecute(execute);

        TargetResponse response = executeDelivery(request);
        return response.getPageLoadOffers() != null ? response.getPageLoadOffers() : Collections.emptyList();
    }

    @Override
    public Map<String, List<TargetOffer>> prefetchOffers(List<String> mboxNames, Map<String, Object> parameters) {
        TargetRequest request = new TargetRequest();
        request.setSessionId(generateSessionId());

        Map<String, Object> prefetch = new HashMap<>();
        List<Map<String, Object>> mboxes = new ArrayList<>();

        for (int i = 0; i < mboxNames.size(); i++) {
            Map<String, Object> mbox = new HashMap<>();
            mbox.put("name", mboxNames.get(i));
            mbox.put("index", i);
            if (parameters != null) {
                mbox.put("parameters", parameters);
            }
            mboxes.add(mbox);
        }

        prefetch.put("mboxes", mboxes);
        request.setPrefetch(prefetch);

        TargetResponse response = executeDelivery(request);
        return response.getMboxOffers() != null ? response.getMboxOffers() : Collections.emptyMap();
    }

    @Override
    public void sendNotification(TargetNotification notification) {
        try {
            TargetRequest request = new TargetRequest();
            request.setSessionId(generateSessionId());
            request.setNotifications(Collections.singletonList(notification));

            executeDelivery(request);
            LOG.debug("Sent Target notification: {}", notification.getType());
        } catch (Exception e) {
            LOG.error("Error sending Target notification", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config != null && config.clientCode() != null;
    }

    private Map<String, Object> buildDeliveryPayload(TargetRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Request ID
        payload.put("requestId", UUID.randomUUID().toString());

        // IMS Org ID
        if (config.imsOrgId() != null) {
            Map<String, Object> id = new HashMap<>();
            id.put("marketingCloudVisitorId", request.getVisitorId());
            id.put("tntId", request.getTntId());
            payload.put("id", id);
        }

        // Context
        Map<String, Object> context = request.getContext();
        if (context == null) {
            context = new HashMap<>();
        }
        context.put("channel", "web");
        context.put("timeOffsetInMinutes", 0);

        Map<String, Object> screen = new HashMap<>();
        screen.put("width", 1920);
        screen.put("height", 1080);
        screen.put("colorDepth", 24);
        screen.put("orientation", "landscape");
        context.put("screen", screen);

        payload.put("context", context);

        // Property token
        if (config.propertyToken() != null && !config.propertyToken().isEmpty()) {
            Map<String, Object> property = new HashMap<>();
            property.put("token", config.propertyToken());
            payload.put("property", property);
        }

        // Experience Cloud
        Map<String, Object> experienceCloud = new HashMap<>();
        Map<String, Object> analytics = new HashMap<>();

        if (config.enableA4T()) {
            analytics.put("logging", "server_side");
            analytics.put("supplementalDataId", UUID.randomUUID().toString());
        }

        experienceCloud.put("analytics", analytics);
        payload.put("experienceCloud", experienceCloud);

        // Execute
        if (request.getExecute() != null) {
            payload.put("execute", request.getExecute());
        }

        // Prefetch
        if (request.getPrefetch() != null) {
            payload.put("prefetch", request.getPrefetch());
        }

        // Notifications
        if (request.getNotifications() != null && !request.getNotifications().isEmpty()) {
            List<Map<String, Object>> notificationsList = new ArrayList<>();
            for (TargetNotification n : request.getNotifications()) {
                Map<String, Object> notif = new HashMap<>();
                notif.put("id", n.getId());
                notif.put("type", n.getType());
                notif.put("timestamp", n.getTimestamp());
                notif.put("mbox", Map.of("name", n.getMbox()));
                notif.put("tokens", n.getTokens());
                notificationsList.add(notif);
            }
            payload.put("notifications", notificationsList);
        }

        return payload;
    }

    private TargetResponse parseDeliveryResponse(String responseBody) {
        TargetResponse response = new TargetResponse();

        try {
            JsonNode root = MAPPER.readTree(responseBody);

            response.setRequestId(root.path("requestId").asText());

            // Parse execute response
            JsonNode execute = root.path("execute");
            if (!execute.isMissingNode()) {
                // Page load offers
                JsonNode pageLoad = execute.path("pageLoad");
                if (!pageLoad.isMissingNode()) {
                    response.setPageLoadOffers(parseOptions(pageLoad.path("options")));
                }

                // Mbox offers
                JsonNode mboxes = execute.path("mboxes");
                if (mboxes.isArray()) {
                    Map<String, List<TargetOffer>> mboxOffers = new HashMap<>();
                    for (JsonNode mbox : mboxes) {
                        String mboxName = mbox.path("name").asText();
                        List<TargetOffer> offers = parseOptions(mbox.path("options"));
                        mboxOffers.put(mboxName, offers);
                    }
                    response.setMboxOffers(mboxOffers);
                }
            }

            // Parse prefetch response
            JsonNode prefetch = root.path("prefetch");
            if (!prefetch.isMissingNode()) {
                JsonNode mboxes = prefetch.path("mboxes");
                if (mboxes.isArray()) {
                    Map<String, List<TargetOffer>> mboxOffers = new HashMap<>();
                    for (JsonNode mbox : mboxes) {
                        String mboxName = mbox.path("name").asText();
                        List<TargetOffer> offers = parseOptions(mbox.path("options"));
                        mboxOffers.put(mboxName, offers);
                    }
                    if (response.getMboxOffers() == null) {
                        response.setMboxOffers(mboxOffers);
                    } else {
                        response.getMboxOffers().putAll(mboxOffers);
                    }
                }
            }

        } catch (Exception e) {
            LOG.error("Error parsing Target response", e);
        }

        return response;
    }

    private List<TargetOffer> parseOptions(JsonNode options) {
        List<TargetOffer> offers = new ArrayList<>();

        if (options.isArray()) {
            for (JsonNode option : options) {
                TargetOffer offer = new TargetOffer();
                offer.setId(option.path("eventToken").asText());
                offer.setType(option.path("type").asText());
                offer.setContent(option.path("content").asText());

                // Response tokens
                JsonNode responseTokens = option.path("responseTokens");
                if (!responseTokens.isMissingNode()) {
                    Map<String, String> tokens = new HashMap<>();
                    responseTokens.fields().forEachRemaining(entry ->
                        tokens.put(entry.getKey(), entry.getValue().asText())
                    );
                    offer.setResponseTokens(tokens);

                    // Extract activity/experience info
                    offer.setActivityId(tokens.get("activity.id"));
                    offer.setActivityName(tokens.get("activity.name"));
                    offer.setExperienceId(tokens.get("experience.id"));
                    offer.setExperienceName(tokens.get("experience.name"));
                }

                offers.add(offer);
            }
        }

        return offers;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

### Server-Side Rendered Component

```java
package com.example.aem.bmad.core.models;

import com.adobe.cq.export.json.ComponentExporter;
import com.example.aem.bmad.core.services.TargetDeliveryService;
import com.example.aem.bmad.core.services.TargetDeliveryService.TargetOffer;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.*;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = {PersonalizedHeroModel.class, ComponentExporter.class},
    resourceType = PersonalizedHeroModel.RESOURCE_TYPE
)
public class PersonalizedHeroModel implements ComponentExporter {

    static final String RESOURCE_TYPE = "aem-bmad-showcase/components/content/personalized-hero";
    private static final String MBOX_NAME = "hero-personalization";

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private TargetDeliveryService targetService;

    @ValueMapValue @Optional
    private String defaultHeading;

    @ValueMapValue @Optional
    private String defaultSubheading;

    @ValueMapValue @Optional
    private String defaultBackgroundImage;

    private String heading;
    private String subheading;
    private String backgroundImage;
    private String activityId;
    private String experienceName;
    private boolean personalized;

    @PostConstruct
    protected void init() {
        // Set defaults
        heading = defaultHeading;
        subheading = defaultSubheading;
        backgroundImage = defaultBackgroundImage;
        personalized = false;

        // Attempt personalization
        if (targetService != null && targetService.isAvailable()) {
            try {
                Map<String, Object> params = buildMboxParameters();
                List<TargetOffer> offers = targetService.getOffers(MBOX_NAME, params);

                if (!offers.isEmpty()) {
                    applyOffers(offers);
                }
            } catch (Exception e) {
                // Fall back to default content
            }
        }
    }

    private Map<String, Object> buildMboxParameters() {
        Map<String, Object> params = new HashMap<>();

        // Page context
        params.put("pagePath", request.getPathInfo());
        params.put("pageLocale", request.getLocale().toString());

        // User context (from cookies/session)
        String userSegment = request.getCookies() != null ?
            Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals("user_segment"))
                .findFirst()
                .map(c -> c.getValue())
                .orElse("new-visitor") : "new-visitor";
        params.put("userSegment", userSegment);

        // Geo context (if available)
        String geoRegion = request.getHeader("X-Geo-Region");
        if (geoRegion != null) {
            params.put("geo.region", geoRegion);
        }

        return params;
    }

    private void applyOffers(List<TargetOffer> offers) {
        for (TargetOffer offer : offers) {
            if ("json".equals(offer.getType()) && offer.getData() != null) {
                Map<String, Object> data = offer.getData();

                if (data.containsKey("heading")) {
                    heading = (String) data.get("heading");
                }
                if (data.containsKey("subheading")) {
                    subheading = (String) data.get("subheading");
                }
                if (data.containsKey("backgroundImage")) {
                    backgroundImage = (String) data.get("backgroundImage");
                }

                activityId = offer.getActivityId();
                experienceName = offer.getExperienceName();
                personalized = true;

            } else if ("html".equals(offer.getType()) && offer.getContent() != null) {
                // For HTML offers, content replaces the entire component
                // Handle separately in HTL
            }
        }
    }

    // Getters
    public String getHeading() { return heading; }
    public String getSubheading() { return subheading; }
    public String getBackgroundImage() { return backgroundImage; }
    public String getActivityId() { return activityId; }
    public String getExperienceName() { return experienceName; }
    public boolean isPersonalized() { return personalized; }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
```

---

## Experience Targeting (XT)

### Experience Targeting Component

```html
<!-- experience-targeting.html -->
<sly data-sly-use.target="com.example.aem.bmad.core.models.ExperienceTargetingModel">
    <div class="cmp-experience-targeting"
         id="${target.componentId}"
         data-mbox="${target.mboxName}"
         data-target-enabled="${target.enabled}">

        <!-- Default content (shown if Target unavailable) -->
        <sly data-sly-test="${!target.hasTargetedContent}">
            <sly data-sly-resource="${'default' @ resourceType='aem-bmad-showcase/components/content/container'}"/>
        </sly>

        <!-- Targeted content -->
        <sly data-sly-test="${target.hasTargetedContent}">
            <div class="cmp-experience-targeting__content"
                 data-activity-id="${target.activityId}"
                 data-experience-id="${target.experienceId}">

                <!-- Server-rendered personalized content -->
                <sly data-sly-test="${target.serverSideContent}">
                    ${target.serverSideContent @ context='html'}
                </sly>

                <!-- Experience fragment offer -->
                <sly data-sly-test="${target.experienceFragmentPath}">
                    <sly data-sly-resource="${target.experienceFragmentPath @ resourceType='cq/experience-fragments/editor/components/experiencefragment'}"/>
                </sly>

            </div>
        </sly>

        <!-- Tracking pixel for impressions -->
        <sly data-sly-test="${target.hasTargetedContent && target.trackingUrl}">
            <img src="${target.trackingUrl @ context='uri'}"
                 style="display:none"
                 alt=""
                 aria-hidden="true"/>
        </sly>
    </div>
</sly>
```

### Experience Targeting Model

```java
package com.example.aem.bmad.core.models;

import com.example.aem.bmad.core.services.TargetDeliveryService;
import com.example.aem.bmad.core.services.TargetDeliveryService.TargetOffer;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.*;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ExperienceTargetingModel.class,
    resourceType = ExperienceTargetingModel.RESOURCE_TYPE
)
public class ExperienceTargetingModel {

    static final String RESOURCE_TYPE = "aem-bmad-showcase/components/content/experience-targeting";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @OSGiService
    private TargetDeliveryService targetService;

    @ValueMapValue @Optional
    private String mboxName;

    @ValueMapValue @Optional
    private String defaultExperienceFragment;

    @ValueMapValue @Optional
    private boolean enableServerSide;

    private String componentId;
    private boolean enabled;
    private boolean hasTargetedContent;
    private String serverSideContent;
    private String experienceFragmentPath;
    private String activityId;
    private String experienceId;
    private String trackingUrl;

    @PostConstruct
    protected void init() {
        componentId = resource.getPath().hashCode() + "-xt";
        enabled = targetService != null && targetService.isAvailable();
        hasTargetedContent = false;

        if (mboxName == null || mboxName.isEmpty()) {
            mboxName = "xt-" + resource.getName();
        }

        if (enabled && enableServerSide) {
            fetchTargetContent();
        } else {
            // Use default experience fragment
            experienceFragmentPath = defaultExperienceFragment;
        }
    }

    private void fetchTargetContent() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("componentPath", resource.getPath());

            List<TargetOffer> offers = targetService.getOffers(mboxName, params);

            if (!offers.isEmpty()) {
                TargetOffer offer = offers.get(0);
                hasTargetedContent = true;
                activityId = offer.getActivityId();
                experienceId = offer.getExperienceId();

                if ("html".equals(offer.getType())) {
                    serverSideContent = offer.getContent();
                } else if ("json".equals(offer.getType()) && offer.getData() != null) {
                    // JSON offer might contain experience fragment path
                    Object xfPath = offer.getData().get("experienceFragmentPath");
                    if (xfPath != null) {
                        experienceFragmentPath = xfPath.toString();
                    }
                }

                // Build tracking URL if needed
                if (offer.getResponseTokens() != null) {
                    // Tracking handled via response tokens
                }
            }
        } catch (Exception e) {
            // Fall back to default
            experienceFragmentPath = defaultExperienceFragment;
        }
    }

    // Getters
    public String getComponentId() { return componentId; }
    public String getMboxName() { return mboxName; }
    public boolean isEnabled() { return enabled; }
    public boolean isHasTargetedContent() { return hasTargetedContent; }
    public String getServerSideContent() { return serverSideContent; }
    public String getExperienceFragmentPath() { return experienceFragmentPath; }
    public String getActivityId() { return activityId; }
    public String getExperienceId() { return experienceId; }
    public String getTrackingUrl() { return trackingUrl; }
}
```

---

## A/B and Multivariate Testing

### A/B Test Component

```java
package com.example.aem.bmad.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.*;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ABTestModel.class,
    resourceType = ABTestModel.RESOURCE_TYPE
)
public class ABTestModel {

    static final String RESOURCE_TYPE = "aem-bmad-showcase/components/content/ab-test";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue @Optional
    private String mboxName;

    @ValueMapValue @Optional
    private String testName;

    @ValueMapValue @Optional
    private int trafficAllocation; // Percentage

    @ChildResource(name = "variations")
    @Optional
    private List<Resource> variations;

    private String selectedVariation;
    private String controlVariationPath;
    private List<Variation> variationList;
    private boolean isInTest;

    @PostConstruct
    protected void init() {
        variationList = new ArrayList<>();
        isInTest = false;

        if (variations != null) {
            for (Resource variationRes : variations) {
                Variation v = new Variation();
                v.setName(variationRes.getValueMap().get("name", String.class));
                v.setPath(variationRes.getValueMap().get("contentPath", String.class));
                v.setWeight(variationRes.getValueMap().get("weight", 50));
                v.setIsControl(variationRes.getValueMap().get("isControl", false));

                if (v.isControl()) {
                    controlVariationPath = v.getPath();
                }

                variationList.add(v);
            }
        }

        // Determine if user is in test based on traffic allocation
        if (trafficAllocation > 0) {
            int random = new Random().nextInt(100);
            isInTest = random < trafficAllocation;
        }

        // Select variation (would normally come from Target)
        if (isInTest && !variationList.isEmpty()) {
            selectedVariation = selectVariation();
        } else {
            selectedVariation = controlVariationPath;
        }
    }

    private String selectVariation() {
        // Simple weighted random selection
        int totalWeight = variationList.stream().mapToInt(Variation::getWeight).sum();
        int random = new Random().nextInt(totalWeight);
        int cumulative = 0;

        for (Variation v : variationList) {
            cumulative += v.getWeight();
            if (random < cumulative) {
                return v.getPath();
            }
        }

        return controlVariationPath;
    }

    // Getters
    public String getMboxName() { return mboxName; }
    public String getTestName() { return testName; }
    public String getSelectedVariation() { return selectedVariation; }
    public List<Variation> getVariationList() { return variationList; }
    public boolean isInTest() { return isInTest; }

    public static class Variation {
        private String name;
        private String path;
        private int weight;
        private boolean isControl;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public boolean isControl() { return isControl; }
        public void setIsControl(boolean isControl) { this.isControl = isControl; }
    }
}
```

### Client-Side A/B Test Tracking

```javascript
// ab-test-tracking.js
(function() {
    'use strict';

    var dataLayer = window.adobeDataLayer = window.adobeDataLayer || [];

    /**
     * Track A/B test exposure
     */
    function trackTestExposure(testData) {
        dataLayer.push({
            event: 'ab-test:exposure',
            test: {
                name: testData.testName,
                activityId: testData.activityId,
                experienceId: testData.experienceId,
                experienceName: testData.experienceName,
                variationId: testData.variationId,
                isControl: testData.isControl
            }
        });

        // Also push to Target for reporting
        if (window.adobe && window.adobe.target) {
            adobe.target.trackEvent({
                mbox: testData.mboxName,
                params: {
                    'testExposure': testData.testName,
                    'variationId': testData.variationId
                }
            });
        }
    }

    /**
     * Track A/B test conversion
     */
    function trackConversion(testName, conversionType, conversionValue) {
        dataLayer.push({
            event: 'ab-test:conversion',
            test: {
                name: testName,
                conversionType: conversionType,
                conversionValue: conversionValue
            }
        });

        // Send conversion to Target
        if (window.adobe && window.adobe.target) {
            adobe.target.trackEvent({
                mbox: 'orderConfirmPage', // or specific conversion mbox
                params: {
                    'orderId': conversionValue.orderId,
                    'orderTotal': conversionValue.total,
                    'productPurchasedId': conversionValue.products
                }
            });
        }
    }

    // Initialize test tracking
    document.addEventListener('DOMContentLoaded', function() {
        // Find all test containers
        document.querySelectorAll('[data-ab-test]').forEach(function(container) {
            var testData = JSON.parse(container.getAttribute('data-ab-test'));
            trackTestExposure(testData);
        });
    });

    // Expose globally
    window.bmadABTest = {
        trackExposure: trackTestExposure,
        trackConversion: trackConversion
    };

})();
```

---

## Automated Personalization

### Automated Personalization Service

```java
package com.example.aem.bmad.core.services;

import java.util.List;
import java.util.Map;

/**
 * Service for Adobe Target Automated Personalization (AP)
 */
public interface AutoPersonalizationService {

    /**
     * Get personalized content based on visitor profile and machine learning
     */
    PersonalizedContent getPersonalizedContent(String mboxName, VisitorContext context);

    /**
     * Update visitor profile with new attributes
     */
    void updateVisitorProfile(String visitorId, Map<String, Object> profileAttributes);

    /**
     * Get recommended experiences based on visitor behavior
     */
    List<Experience> getRecommendedExperiences(String activityId, VisitorContext context);

    /**
     * Visitor context for personalization
     */
    class VisitorContext {
        private String visitorId;
        private String sessionId;
        private Map<String, Object> profileAttributes;
        private Map<String, Object> mboxParameters;
        private Map<String, Object> geoContext;
        private String deviceType;
        private String browser;

        // Getters and setters
        public String getVisitorId() { return visitorId; }
        public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public Map<String, Object> getProfileAttributes() { return profileAttributes; }
        public void setProfileAttributes(Map<String, Object> profileAttributes) { this.profileAttributes = profileAttributes; }
        public Map<String, Object> getMboxParameters() { return mboxParameters; }
        public void setMboxParameters(Map<String, Object> mboxParameters) { this.mboxParameters = mboxParameters; }
        public Map<String, Object> getGeoContext() { return geoContext; }
        public void setGeoContext(Map<String, Object> geoContext) { this.geoContext = geoContext; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getBrowser() { return browser; }
        public void setBrowser(String browser) { this.browser = browser; }
    }

    /**
     * Personalized content response
     */
    class PersonalizedContent {
        private String content;
        private String contentType;
        private double confidence;
        private String algorithm;
        private Map<String, String> responseTokens;

        // Getters and setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Map<String, String> getResponseTokens() { return responseTokens; }
        public void setResponseTokens(Map<String, String> responseTokens) { this.responseTokens = responseTokens; }
    }

    /**
     * Experience definition
     */
    class Experience {
        private String id;
        private String name;
        private String content;
        private double score;
        private List<String> audiences;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public List<String> getAudiences() { return audiences; }
        public void setAudiences(List<String> audiences) { this.audiences = audiences; }
    }
}
```

---

## Recommendations

### Recommendations Service

```java
package com.example.aem.bmad.core.services;

import java.util.List;
import java.util.Map;

/**
 * Service for Adobe Target Recommendations
 */
public interface RecommendationsService {

    /**
     * Get product recommendations
     */
    List<Recommendation> getRecommendations(RecommendationRequest request);

    /**
     * Get recommendations for specific entity
     */
    List<Recommendation> getRecommendationsForEntity(String entityId, String algorithm, int limit);

    /**
     * Update entity catalog
     */
    void updateEntity(RecommendationEntity entity);

    /**
     * Track user behavior for recommendations
     */
    void trackBehavior(BehaviorEvent event);

    /**
     * Recommendation request
     */
    class RecommendationRequest {
        private String mboxName;
        private String entityId;
        private String categoryId;
        private String algorithm;
        private int limit;
        private Map<String, Object> filters;
        private Map<String, Object> context;

        // Getters and setters
        public String getMboxName() { return mboxName; }
        public void setMboxName(String mboxName) { this.mboxName = mboxName; }
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    /**
     * Recommendation result
     */
    class Recommendation {
        private String entityId;
        private String name;
        private String description;
        private String imageUrl;
        private String pageUrl;
        private double price;
        private double salePrice;
        private String category;
        private double score;
        private String algorithm;
        private Map<String, Object> customAttributes;

        // Getters and setters
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getPageUrl() { return pageUrl; }
        public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public double getSalePrice() { return salePrice; }
        public void setSalePrice(double salePrice) { this.salePrice = salePrice; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Map<String, Object> getCustomAttributes() { return customAttributes; }
        public void setCustomAttributes(Map<String, Object> customAttributes) { this.customAttributes = customAttributes; }
    }

    /**
     * Entity for catalog
     */
    class RecommendationEntity {
        private String id;
        private String name;
        private String categoryId;
        private String message;
        private String thumbnailUrl;
        private String pageUrl;
        private double value;
        private String inventory;
        private Map<String, Object> customAttributes;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public String getPageUrl() { return pageUrl; }
        public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
        public String getInventory() { return inventory; }
        public void setInventory(String inventory) { this.inventory = inventory; }
        public Map<String, Object> getCustomAttributes() { return customAttributes; }
        public void setCustomAttributes(Map<String, Object> customAttributes) { this.customAttributes = customAttributes; }
    }

    /**
     * Behavior tracking event
     */
    class BehaviorEvent {
        private String visitorId;
        private String entityId;
        private String eventType; // view, add-to-cart, purchase
        private long timestamp;
        private Map<String, Object> eventData;

        // Getters and setters
        public String getVisitorId() { return visitorId; }
        public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getEventData() { return eventData; }
        public void setEventData(Map<String, Object> eventData) { this.eventData = eventData; }
    }
}
```

### Recommendations Component

```html
<!-- recommendations.html -->
<sly data-sly-use.recs="com.example.aem.bmad.core.models.RecommendationsModel">
    <div class="cmp-recommendations"
         id="${recs.componentId}"
         data-mbox="${recs.mboxName}"
         data-entity-id="${recs.currentEntityId}">

        <h2 class="cmp-recommendations__title">${recs.title || 'Recommended for You'}</h2>

        <div class="cmp-recommendations__grid">
            <sly data-sly-list.item="${recs.recommendations}">
                <article class="cmp-recommendations__item"
                         data-entity-id="${item.entityId}"
                         data-rec-algorithm="${item.algorithm}">
                    <a href="${item.pageUrl @ context='uri'}" class="cmp-recommendations__link">
                        <img src="${item.imageUrl @ context='uri'}"
                             alt="${item.name}"
                             class="cmp-recommendations__image"
                             loading="lazy"/>
                        <div class="cmp-recommendations__content">
                            <h3 class="cmp-recommendations__name">${item.name}</h3>
                            <p class="cmp-recommendations__description">${item.description}</p>
                            <div class="cmp-recommendations__price">
                                <sly data-sly-test="${item.salePrice > 0 && item.salePrice < item.price}">
                                    <span class="cmp-recommendations__price--original">$${item.price}</span>
                                    <span class="cmp-recommendations__price--sale">$${item.salePrice}</span>
                                </sly>
                                <sly data-sly-test="${!item.salePrice || item.salePrice >= item.price}">
                                    <span class="cmp-recommendations__price--current">$${item.price}</span>
                                </sly>
                            </div>
                        </div>
                    </a>
                </article>
            </sly>
        </div>

        <!-- Fallback for no recommendations -->
        <sly data-sly-test="${!recs.recommendations || recs.recommendations.size == 0}">
            <p class="cmp-recommendations__empty">No recommendations available.</p>
        </sly>
    </div>
</sly>
```

---

## Audiences and Segments

### Audience Targeting Model

```java
package com.example.aem.bmad.core.models;

import java.util.*;

/**
 * Audience definition for Target activities
 */
public class AudienceDefinition {

    private String id;
    private String name;
    private String description;
    private List<AudienceRule> rules;
    private String combineOperator; // AND, OR

    public enum RuleType {
        BROWSER,
        OPERATING_SYSTEM,
        VISITOR_PROFILE,
        TRAFFIC_SOURCES,
        GEO,
        TIME_FRAME,
        MOBILE,
        CUSTOM_PARAMETERS,
        PAGE,
        BEHAVIOR
    }

    public static class AudienceRule {
        private RuleType type;
        private String attribute;
        private String operator; // equals, contains, starts_with, etc.
        private String value;
        private List<String> values;

        // Builder pattern for rules
        public static AudienceRule browser(String operator, String... browsers) {
            AudienceRule rule = new AudienceRule();
            rule.type = RuleType.BROWSER;
            rule.attribute = "browser";
            rule.operator = operator;
            rule.values = Arrays.asList(browsers);
            return rule;
        }

        public static AudienceRule geo(String attribute, String operator, String value) {
            AudienceRule rule = new AudienceRule();
            rule.type = RuleType.GEO;
            rule.attribute = attribute; // country, region, city, dma
            rule.operator = operator;
            rule.value = value;
            return rule;
        }

        public static AudienceRule visitorProfile(String attribute, String operator, String value) {
            AudienceRule rule = new AudienceRule();
            rule.type = RuleType.VISITOR_PROFILE;
            rule.attribute = attribute;
            rule.operator = operator;
            rule.value = value;
            return rule;
        }

        public static AudienceRule behavior(String attribute, String operator, String value) {
            AudienceRule rule = new AudienceRule();
            rule.type = RuleType.BEHAVIOR;
            rule.attribute = attribute; // page_count, time_on_site, recency
            rule.operator = operator;
            rule.value = value;
            return rule;
        }

        public static AudienceRule customParameter(String paramName, String operator, String value) {
            AudienceRule rule = new AudienceRule();
            rule.type = RuleType.CUSTOM_PARAMETERS;
            rule.attribute = paramName;
            rule.operator = operator;
            rule.value = value;
            return rule;
        }

        // Getters and setters
        public RuleType getType() { return type; }
        public void setType(RuleType type) { this.type = type; }
        public String getAttribute() { return attribute; }
        public void setAttribute(String attribute) { this.attribute = attribute; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public List<String> getValues() { return values; }
        public void setValues(List<String> values) { this.values = values; }
    }

    // Builder for audience definitions
    public static AudienceDefinition create(String name) {
        AudienceDefinition audience = new AudienceDefinition();
        audience.id = UUID.randomUUID().toString();
        audience.name = name;
        audience.rules = new ArrayList<>();
        audience.combineOperator = "AND";
        return audience;
    }

    public AudienceDefinition withRule(AudienceRule rule) {
        this.rules.add(rule);
        return this;
    }

    public AudienceDefinition combineWithOr() {
        this.combineOperator = "OR";
        return this;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<AudienceRule> getRules() { return rules; }
    public String getCombineOperator() { return combineOperator; }
}
```

### Common Audience Definitions

```java
package com.example.aem.bmad.core.audiences;

import com.example.aem.bmad.core.models.AudienceDefinition;
import com.example.aem.bmad.core.models.AudienceDefinition.AudienceRule;

/**
 * Predefined audiences for common targeting scenarios
 */
public final class CommonAudiences {

    private CommonAudiences() {}

    /**
     * New visitors (first visit)
     */
    public static AudienceDefinition newVisitors() {
        return AudienceDefinition.create("New Visitors")
            .withRule(AudienceRule.visitorProfile("isNewVisitor", "equals", "true"));
    }

    /**
     * Returning visitors
     */
    public static AudienceDefinition returningVisitors() {
        return AudienceDefinition.create("Returning Visitors")
            .withRule(AudienceRule.visitorProfile("isNewVisitor", "equals", "false"));
    }

    /**
     * Mobile users
     */
    public static AudienceDefinition mobileUsers() {
        return AudienceDefinition.create("Mobile Users")
            .withRule(AudienceRule.browser("equals", "Mobile Safari", "Chrome Mobile", "Firefox Mobile"));
    }

    /**
     * High-value customers (based on profile)
     */
    public static AudienceDefinition highValueCustomers() {
        return AudienceDefinition.create("High-Value Customers")
            .withRule(AudienceRule.visitorProfile("lifetimeValue", "greaterThan", "1000"));
    }

    /**
     * Geographic targeting - US only
     */
    public static AudienceDefinition usVisitors() {
        return AudienceDefinition.create("US Visitors")
            .withRule(AudienceRule.geo("country", "equals", "US"));
    }

    /**
     * Engaged visitors (multiple page views)
     */
    public static AudienceDefinition engagedVisitors() {
        return AudienceDefinition.create("Engaged Visitors")
            .withRule(AudienceRule.behavior("page_count", "greaterThan", "3"));
    }

    /**
     * Cart abandoners
     */
    public static AudienceDefinition cartAbandoners() {
        return AudienceDefinition.create("Cart Abandoners")
            .withRule(AudienceRule.visitorProfile("hasCartItems", "equals", "true"))
            .withRule(AudienceRule.visitorProfile("hasCompletedPurchase", "equals", "false"));
    }

    /**
     * Segment from Adobe Audience Manager
     */
    public static AudienceDefinition audienceManagerSegment(String segmentId, String segmentName) {
        return AudienceDefinition.create(segmentName)
            .withRule(AudienceRule.customParameter("aam_segment", "contains", segmentId));
    }
}
```

---

## AEM Experience Fragments as Offers

### Experience Fragment Export Configuration

```xml
<!-- /conf/aem-bmad-showcase/settings/cloudconfigs/target/experiencefragments/.content.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
          jcr:primaryType="cq:Page">
    <jcr:content
        jcr:primaryType="cq:PageContent"
        jcr:title="Experience Fragments for Target"
        sling:resourceType="cq/experience-fragments/components/cloudserviceconfig"
        enabled="{Boolean}true"
        targetClientCode="${ADOBE_TARGET_CLIENT_CODE}"
        targetWorkspace="default"/>
</jcr:root>
```

### Experience Fragment to Target Sync Service

```java
package com.example.aem.bmad.core.services;

import java.util.List;

/**
 * Service for syncing Experience Fragments to Adobe Target as offers
 */
public interface ExperienceFragmentTargetService {

    /**
     * Export Experience Fragment to Target as offer
     */
    ExportResult exportToTarget(String experienceFragmentPath);

    /**
     * Export multiple Experience Fragments
     */
    List<ExportResult> exportMultipleToTarget(List<String> experienceFragmentPaths);

    /**
     * Get Target offer ID for Experience Fragment
     */
    String getTargetOfferId(String experienceFragmentPath);

    /**
     * Check if Experience Fragment is synced with Target
     */
    boolean isSyncedWithTarget(String experienceFragmentPath);

    /**
     * Delete Experience Fragment offer from Target
     */
    boolean deleteFromTarget(String experienceFragmentPath);

    /**
     * Export result
     */
    class ExportResult {
        private String experienceFragmentPath;
        private String targetOfferId;
        private boolean success;
        private String errorMessage;
        private long exportTimestamp;

        // Getters and setters
        public String getExperienceFragmentPath() { return experienceFragmentPath; }
        public void setExperienceFragmentPath(String experienceFragmentPath) { this.experienceFragmentPath = experienceFragmentPath; }
        public String getTargetOfferId() { return targetOfferId; }
        public void setTargetOfferId(String targetOfferId) { this.targetOfferId = targetOfferId; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getExportTimestamp() { return exportTimestamp; }
        public void setExportTimestamp(long exportTimestamp) { this.exportTimestamp = exportTimestamp; }

        public static ExportResult success(String path, String offerId) {
            ExportResult result = new ExportResult();
            result.experienceFragmentPath = path;
            result.targetOfferId = offerId;
            result.success = true;
            result.exportTimestamp = System.currentTimeMillis();
            return result;
        }

        public static ExportResult failure(String path, String error) {
            ExportResult result = new ExportResult();
            result.experienceFragmentPath = path;
            result.success = false;
            result.errorMessage = error;
            result.exportTimestamp = System.currentTimeMillis();
            return result;
        }
    }
}
```

---

## Analytics for Target (A4T)

See [adobe-analytics-integration.md](./adobe-analytics-integration.md#analytics-for-target-a4t) for detailed A4T integration.

### A4T Data Layer Integration

```javascript
// a4t-integration.js
(function() {
    'use strict';

    var dataLayer = window.adobeDataLayer = window.adobeDataLayer || [];

    /**
     * Push Target activity data to Analytics data layer
     */
    function pushTargetDataToAnalytics(targetData) {
        dataLayer.push({
            event: 'target:activity-loaded',
            target: {
                activityId: targetData.activityId,
                activityName: targetData.activityName,
                experienceId: targetData.experienceId,
                experienceName: targetData.experienceName,
                offerId: targetData.offerId,
                offerName: targetData.offerName
            }
        });
    }

    /**
     * Handle Target response tokens for A4T
     */
    document.addEventListener('at-request-succeeded', function(e) {
        var responseTokens = e.detail.responseTokens;

        if (responseTokens && responseTokens.length > 0) {
            responseTokens.forEach(function(token) {
                // Extract A4T relevant data
                var a4tData = {
                    activityId: token['activity.id'],
                    activityName: token['activity.name'],
                    experienceId: token['experience.id'],
                    experienceName: token['experience.name'],
                    offerId: token['offer.id'],
                    offerName: token['offer.name'],
                    activityType: token['activity.type']
                };

                pushTargetDataToAnalytics(a4tData);

                // Set Analytics variables for A4T
                if (typeof s !== 'undefined') {
                    s.eVar60 = a4tData.activityName + ':' + a4tData.experienceName;
                    s.prop60 = a4tData.activityId + ':' + a4tData.experienceId;
                    s.events = s.events ? s.events + ',event200' : 'event200';
                }
            });
        }
    });

    // Track Target conversions to Analytics
    window.trackTargetConversion = function(conversionData) {
        dataLayer.push({
            event: 'target:conversion',
            target: conversionData,
            analytics: {
                events: 'event201',
                eVar61: conversionData.goalId,
                prop61: conversionData.conversionValue
            }
        });
    };

})();
```

---

## Visual Experience Composer Integration

### VEC Support HTL

```html
<!-- page.html - VEC support attributes -->
<sly data-sly-use.target="com.example.aem.bmad.core.models.TargetConfigModel">
    <body class="page ${target.vecEnabled ? 'at-body-click-tracking' : ''}"
          data-at-mbox-name="${target.globalMboxName}">

        <!-- VEC selector attributes on components -->
        <div class="cmp-container"
             data-aue-behavior="component"
             data-aue-resource="${resource.path}"
             data-at-src="${resource.path}.html">

            <!-- Component content -->
            <sly data-sly-resource="${'content' @ resourceType='wcm/foundation/components/parsys'}"/>

        </div>

    </body>
</sly>
```

### VEC Helper Script

```javascript
// vec-helper.js
(function() {
    'use strict';

    /**
     * Initialize VEC helper functionality
     */
    function initVECHelper() {
        // Add VEC-compatible selectors to components
        document.querySelectorAll('[data-cmp-data-layer]').forEach(function(component) {
            var componentData = JSON.parse(component.getAttribute('data-cmp-data-layer'));

            // Add unique selector for VEC
            if (!component.id) {
                component.id = 'cmp-' + componentData.id;
            }

            // Add data attribute for VEC targeting
            component.setAttribute('data-at-component-type', componentData['@type']);
        });

        // Handle dynamic content for VEC
        var observer = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach(function(node) {
                        if (node.nodeType === 1 && node.hasAttribute('data-cmp-data-layer')) {
                            initVECComponent(node);
                        }
                    });
                }
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    function initVECComponent(component) {
        var componentData = JSON.parse(component.getAttribute('data-cmp-data-layer'));

        if (!component.id) {
            component.id = 'cmp-' + componentData.id;
        }

        component.setAttribute('data-at-component-type', componentData['@type']);
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initVECHelper);
    } else {
        initVECHelper();
    }

})();
```

---

## Form-Based Experience Composer

### Form-Based Targeting Component

```java
package com.example.aem.bmad.core.models;

import com.example.aem.bmad.core.services.TargetDeliveryService;
import com.example.aem.bmad.core.services.TargetDeliveryService.TargetOffer;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.*;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = FormBasedTargetModel.class,
    resourceType = FormBasedTargetModel.RESOURCE_TYPE
)
public class FormBasedTargetModel {

    static final String RESOURCE_TYPE = "aem-bmad-showcase/components/content/form-based-target";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @OSGiService
    private TargetDeliveryService targetService;

    @ValueMapValue @Optional
    private String mboxName;

    @ValueMapValue @Optional
    private String[] mboxParameters;

    private String componentId;
    private List<TargetOffer> offers;
    private String jsonOfferContent;
    private String htmlOfferContent;

    @PostConstruct
    protected void init() {
        componentId = "form-target-" + resource.getPath().hashCode();
        offers = new ArrayList<>();

        if (mboxName != null && targetService != null && targetService.isAvailable()) {
            fetchOffers();
        }
    }

    private void fetchOffers() {
        Map<String, Object> params = new HashMap<>();

        // Parse mbox parameters from component config
        if (mboxParameters != null) {
            for (String param : mboxParameters) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2) {
                    params.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        // Add request parameters
        params.put("pagePath", request.getPathInfo());
        params.put("locale", request.getLocale().toString());

        offers = targetService.getOffers(mboxName, params);

        // Process offers
        for (TargetOffer offer : offers) {
            if ("json".equals(offer.getType())) {
                try {
                    jsonOfferContent = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(offer.getData());
                } catch (Exception e) {
                    // ignore
                }
            } else if ("html".equals(offer.getType())) {
                htmlOfferContent = offer.getContent();
            }
        }
    }

    // Getters
    public String getComponentId() { return componentId; }
    public String getMboxName() { return mboxName; }
    public List<TargetOffer> getOffers() { return offers; }
    public String getJsonOfferContent() { return jsonOfferContent; }
    public String getHtmlOfferContent() { return htmlOfferContent; }
    public boolean hasOffers() { return !offers.isEmpty(); }
}
```

### Form-Based Target HTL

```html
<!-- form-based-target.html -->
<sly data-sly-use.target="com.example.aem.bmad.core.models.FormBasedTargetModel">
    <div class="cmp-form-target"
         id="${target.componentId}"
         data-mbox="${target.mboxName}">

        <!-- JSON offer rendering -->
        <sly data-sly-test="${target.jsonOfferContent}">
            <div class="cmp-form-target__json"
                 data-json-offer="${target.jsonOfferContent @ context='attribute'}">
                <!-- JavaScript will render based on JSON data -->
            </div>
        </sly>

        <!-- HTML offer rendering -->
        <sly data-sly-test="${target.htmlOfferContent}">
            <div class="cmp-form-target__html">
                ${target.htmlOfferContent @ context='html'}
            </div>
        </sly>

        <!-- No offers fallback -->
        <sly data-sly-test="${!target.hasOffers}">
            <sly data-sly-resource="${'default' @ resourceType='aem-bmad-showcase/components/content/container'}"/>
        </sly>

    </div>
</sly>

<!-- Client-side JSON offer handler -->
<script>
    (function() {
        var container = document.getElementById('${target.componentId}');
        var jsonContainer = container.querySelector('.cmp-form-target__json');

        if (jsonContainer) {
            var offerData = JSON.parse(jsonContainer.getAttribute('data-json-offer'));

            // Render based on offer data structure
            renderJsonOffer(jsonContainer, offerData);
        }

        function renderJsonOffer(container, data) {
            // Custom rendering logic based on JSON structure
            if (data.template === 'hero') {
                container.innerHTML = createHeroMarkup(data);
            } else if (data.template === 'banner') {
                container.innerHTML = createBannerMarkup(data);
            }
        }

        function createHeroMarkup(data) {
            return '<div class="dynamic-hero">' +
                '<h1>' + data.heading + '</h1>' +
                '<p>' + data.subheading + '</p>' +
                '<a href="' + data.ctaLink + '">' + data.ctaText + '</a>' +
                '</div>';
        }

        function createBannerMarkup(data) {
            return '<div class="dynamic-banner">' +
                '<img src="' + data.imageUrl + '" alt="' + data.altText + '">' +
                '<div class="banner-content">' + data.message + '</div>' +
                '</div>';
        }
    })();
</script>
```

---

## OSGi Services

### OSGi Configuration Files

**config.dev/com.example.aem.bmad.core.config.AdobeTargetConfig~dev.cfg.json:**
```json
{
    "clientCode": "bmad-dev",
    "imsOrgId": "XXXXXXXXXXXXXX@AdobeOrg",
    "propertyToken": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "environmentId": "development",
    "serverDomain": "tt.omtrdc.net",
    "decisioningMethod": "server-side",
    "enableA4T": true,
    "globalMboxName": "target-global-mbox",
    "timeout": 5000,
    "defaultContentVisible": true
}
```

**config.prod/com.example.aem.bmad.core.config.AdobeTargetConfig~prod.cfg.json:**
```json
{
    "clientCode": "bmad-prod",
    "imsOrgId": "XXXXXXXXXXXXXX@AdobeOrg",
    "propertyToken": "yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy",
    "environmentId": "production",
    "serverDomain": "tt.omtrdc.net",
    "decisioningMethod": "hybrid",
    "enableA4T": true,
    "globalMboxName": "target-global-mbox",
    "timeout": 3000,
    "defaultContentVisible": false
}
```

---

## Testing and QA

### Target QA Mode

```javascript
// target-qa.js
(function() {
    'use strict';

    /**
     * Enable Target QA mode via URL parameter
     * Usage: ?at_preview_token=TOKEN&at_preview_index=1_1
     */
    function initTargetQA() {
        var urlParams = new URLSearchParams(window.location.search);

        var previewToken = urlParams.get('at_preview_token');
        var previewIndex = urlParams.get('at_preview_index');

        if (previewToken) {
            // Store in session for subsequent page loads
            sessionStorage.setItem('at_preview_token', previewToken);

            if (previewIndex) {
                sessionStorage.setItem('at_preview_index', previewIndex);
            }

            console.log('Target QA Mode enabled');
            showQABadge();
        }

        // Check session storage
        if (sessionStorage.getItem('at_preview_token')) {
            window.targetGlobalSettings = window.targetGlobalSettings || {};
            window.targetGlobalSettings.qaMode = true;
            showQABadge();
        }
    }

    function showQABadge() {
        var badge = document.createElement('div');
        badge.id = 'target-qa-badge';
        badge.innerHTML = 'Target QA Mode';
        badge.style.cssText = 'position:fixed;top:0;left:50%;transform:translateX(-50%);' +
            'background:#ff5722;color:white;padding:5px 15px;font-size:12px;' +
            'z-index:9999;border-radius:0 0 4px 4px;';

        document.body.appendChild(badge);

        // Add exit button
        var exitBtn = document.createElement('button');
        exitBtn.innerHTML = 'Exit QA';
        exitBtn.style.cssText = 'margin-left:10px;background:white;color:#ff5722;' +
            'border:none;padding:2px 8px;cursor:pointer;border-radius:2px;';
        exitBtn.onclick = exitQAMode;

        badge.appendChild(exitBtn);
    }

    function exitQAMode() {
        sessionStorage.removeItem('at_preview_token');
        sessionStorage.removeItem('at_preview_index');

        // Remove QA params from URL
        var url = new URL(window.location);
        url.searchParams.delete('at_preview_token');
        url.searchParams.delete('at_preview_index');

        window.location.href = url.toString();
    }

    initTargetQA();

})();
```

### Unit Test for Target Service

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.config.AdobeTargetConfig;
import com.example.aem.bmad.core.services.HttpClientService;
import com.example.aem.bmad.core.services.TargetDeliveryService;
import com.example.aem.bmad.core.services.TargetDeliveryService.TargetRequest;
import com.example.aem.bmad.core.services.TargetDeliveryService.TargetResponse;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class TargetDeliveryServiceImplTest {

    private final AemContext context = new AemContext();

    @Mock
    private HttpClientService httpClient;

    @Mock
    private AdobeTargetConfig config;

    private TargetDeliveryServiceImpl targetService;

    @BeforeEach
    void setUp() {
        when(config.clientCode()).thenReturn("test-client");
        when(config.serverDomain()).thenReturn("tt.omtrdc.net");
        when(config.imsOrgId()).thenReturn("TEST@AdobeOrg");
        when(config.timeout()).thenReturn(3000);
        when(config.enableA4T()).thenReturn(true);

        targetService = new TargetDeliveryServiceImpl();
        // Inject dependencies using reflection or context
    }

    @Test
    void testGetOffersSuccess() {
        // Setup mock response
        String mockResponse = """
            {
                "requestId": "test-123",
                "execute": {
                    "mboxes": [{
                        "name": "test-mbox",
                        "options": [{
                            "type": "html",
                            "content": "<div>Personalized Content</div>",
                            "responseTokens": {
                                "activity.id": "12345",
                                "activity.name": "Test Activity",
                                "experience.id": "0",
                                "experience.name": "Experience A"
                            }
                        }]
                    }]
                }
            }
            """;

        when(httpClient.post(anyString(), anyString(), anyMap()))
            .thenReturn(new HttpResponse(200, mockResponse));

        // Execute
        var offers = targetService.getOffers("test-mbox", Map.of("param1", "value1"));

        // Verify
        assertNotNull(offers);
        assertEquals(1, offers.size());
        assertEquals("html", offers.get(0).getType());
        assertEquals("<div>Personalized Content</div>", offers.get(0).getContent());
        assertEquals("12345", offers.get(0).getActivityId());
    }

    @Test
    void testGetOffersHandlesError() {
        when(httpClient.post(anyString(), anyString(), anyMap()))
            .thenReturn(new HttpResponse(500, "Internal Server Error"));

        var offers = targetService.getOffers("test-mbox", null);

        assertNotNull(offers);
        assertTrue(offers.isEmpty());
    }

    @Test
    void testIsAvailable() {
        assertTrue(targetService.isAvailable());
    }

    // Helper class for mock
    static class HttpResponse {
        private final int statusCode;
        private final String body;

        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }
    }
}
```

---

## Best Practices

### Implementation Checklist

```
□ Setup
  □ Target property created with correct workspace
  □ IMS integration configured
  □ at.js 2.x deployed
  □ ECID extension enabled

□ Client-Side
  □ Pre-hiding snippet implemented (flicker prevention)
  □ Data layer integration for Target events
  □ Response token handling
  □ Error fallbacks configured

□ Server-Side
  □ Delivery API service implemented
  □ Session ID management
  □ Visitor ID correlation
  □ Timeout handling
  □ Circuit breaker pattern

□ A4T Integration
  □ Analytics extension configured
  □ Supplemental Data ID generation
  □ Response token to Analytics mapping
  □ Conversion tracking

□ Content Authoring
  □ Experience Fragments for offers
  □ Target cloud configuration
  □ Offer export automation
  □ VEC compatibility

□ Testing
  □ QA mode enabled
  □ Preview links working
  □ Activity validation
  □ Audience testing

□ Performance
  □ Server-side decisioning where appropriate
  □ Prefetch for known mboxes
  □ Caching for static offers
  □ Timeout tuning
```

### Performance Optimization

```java
/**
 * Performance best practices for Target integration
 */
public class TargetPerformanceGuide {

    /**
     * Use prefetch for predictable content
     */
    public void prefetchRecommendations() {
        // Prefetch mboxes that will be needed on page
        List<String> mboxesToPrefetch = Arrays.asList(
            "hero-personalization",
            "sidebar-recommendations",
            "footer-promo"
        );

        // Single request for multiple mboxes
        targetService.prefetchOffers(mboxesToPrefetch, getCommonParams());
    }

    /**
     * Cache static Target responses
     */
    public void cacheStaticOffers() {
        // Cache offers that don't change based on visitor
        // Example: promotional banners, static content variations
        // Use short TTL (5-10 minutes) to allow updates
    }

    /**
     * Use server-side for critical content
     */
    public void serverSideForAboveFold() {
        // Hero, navigation, and above-fold content
        // Should use server-side to prevent flicker
        // Client-side for below-fold and progressive enhancement
    }

    /**
     * Implement graceful degradation
     */
    public void gracefulDegradation() {
        // Always have default content
        // Timeout after 3 seconds maximum
        // Log failures for monitoring
        // Don't block page render
    }
}
```

---

## Traceability

| Spec ID | Component | Description | Status |
|---------|-----------|-------------|--------|
| TGT-001 | Target Config | OSGi configuration service | Documented |
| TGT-002 | at.js Integration | Client-side Target library setup | Documented |
| TGT-003 | Delivery API | Server-side Target service | Documented |
| TGT-004 | Experience Targeting | XT component model | Documented |
| TGT-005 | A/B Testing | Test component and tracking | Documented |
| TGT-006 | Auto Personalization | AP service interface | Documented |
| TGT-007 | Recommendations | Recommendations service | Documented |
| TGT-008 | Audiences | Audience definition models | Documented |
| TGT-009 | Experience Fragments | XF to Target export | Documented |
| TGT-010 | A4T | Analytics for Target integration | Documented |
| TGT-011 | VEC | Visual Experience Composer support | Documented |
| TGT-012 | Form-Based | Form-based composer component | Documented |
| TGT-013 | QA Mode | Testing and preview functionality | Documented |
| TGT-014 | CDN/Dispatcher | Edge personalization patterns | Documented |
| TGT-015 | Stateless Architecture | Visitor tracking without sessions | Documented |
| TGT-016 | Cloud Manager Secrets | Secret variable injection | Documented |
| TGT-017 | Edge Decisioning | On-device and edge decisioning | Documented |
| TGT-018 | RDE/Local Testing | Mock services for development | Documented |

---

## AEMaaCS Cloud Architecture Patterns

This section covers cloud-native architectural considerations specific to AEM as a Cloud Service for Target personalization.

### CDN and Dispatcher Strategy for Personalization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                  Personalization Caching Architecture                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  [Visitor] ──▶ [Fastly CDN] ──▶ [Dispatcher] ──▶ [AEM Publish]             │
│       │            │                │                │                       │
│       │            │                │                │                       │
│       │       ┌────┴────┐      ┌────┴────┐     ┌────┴────┐                  │
│       │       │ Static  │      │ ESI     │     │ Server  │                  │
│       │       │ Content │      │ Include │     │ Side    │                  │
│       │       │ (Cache) │      │ (Edge)  │     │ Target  │                  │
│       │       └─────────┘      └─────────┘     └─────────┘                  │
│       │                                                                      │
│       └──────▶ [Target Edge Network] ◀── at.js calls                        │
│                     │                                                        │
│                     ▼                                                        │
│              [Personalized Content]                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Dispatcher Configuration for Personalization

```apache
# dispatcher/src/conf.dispatcher.d/filters/target-filters.any

# Allow Target delivery API proxy requests
/0300 { /type "allow" /method "POST" /url "/api/target/delivery" }

# Allow Target mbox requests
/0301 { /type "allow" /method "GET" /url "/content/*.target.json" }

# Allow QA mode preview parameters
/0302 { /type "allow" /method "GET" /url "*" /query "at_preview_*" }

# Block caching of personalized content markers
/0303 { /type "deny" /url "*.personalized.html" }
```

```apache
# dispatcher/src/conf.dispatcher.d/cache/target-cache.any

# Personalization cache rules
/target-cache {
    # Never cache Target API responses
    /0001 {
        /glob "/api/target/*"
        /type "deny"
    }

    # Never cache content with personalization markers
    /0002 {
        /glob "*.target.json"
        /type "deny"
    }

    # Cache static Target offers (Experience Fragments)
    /0003 {
        /glob "/content/experience-fragments/*/master.content.html"
        /type "allow"
    }

    # Cache default content, personalize client-side
    /0004 {
        /glob "*.html"
        /type "allow"
    }
}
```

### Stateless Personalization Patterns

#### Stateless Target Request Handler

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.TargetDeliveryService;
import com.example.aem.bmad.core.services.VisitorIdentificationService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.*;

import javax.servlet.http.Cookie;
import java.util.*;

/**
 * Stateless Target request handler.
 * Extracts visitor context from cookies/headers - no server session.
 */
@Component(service = StatelessTargetRequestHandler.class)
public class StatelessTargetRequestHandler {

    private static final String TNT_ID_COOKIE = "mbox";

    @Reference
    private VisitorIdentificationService visitorService;

    @Reference
    private TargetDeliveryService targetService;

    /**
     * Build Target request from stateless HTTP request context
     */
    public TargetDeliveryService.TargetRequest buildRequest(SlingHttpServletRequest request) {
        TargetDeliveryService.TargetRequest targetRequest = new TargetDeliveryService.TargetRequest();

        // Extract visitor IDs from cookies (stateless)
        targetRequest.setTntId(extractTntId(request));
        targetRequest.setSessionId(extractSessionId(request));
        targetRequest.setVisitorId(visitorService.getVisitorId(request));

        // Build context from headers (no session state)
        targetRequest.setContext(buildContextFromHeaders(request));

        return targetRequest;
    }

    private String extractTntId(SlingHttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (TNT_ID_COOKIE.equals(cookie.getName())) {
                String[] parts = cookie.getValue().split("#");
                return parts.length >= 2 ? parts[1] : null;
            }
        }
        return null;
    }

    private String extractSessionId(SlingHttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return generateSessionId();

        for (Cookie cookie : cookies) {
            if (TNT_ID_COOKIE.equals(cookie.getName())) {
                String[] parts = cookie.getValue().split("#");
                return parts.length >= 1 ? parts[0] : generateSessionId();
            }
        }
        return generateSessionId();
    }

    private Map<String, Object> buildContextFromHeaders(SlingHttpServletRequest request) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("channel", "web");

        // Geo from CDN headers (Fastly)
        Map<String, Object> geo = new LinkedHashMap<>();
        geo.put("countryCode", request.getHeader("X-Geo-Country"));
        geo.put("stateCode", request.getHeader("X-Geo-Region"));
        geo.put("city", request.getHeader("X-Geo-City"));
        context.put("geo", geo);

        return context;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
```

### Cloud Manager Secret Management

```java
package com.example.aem.bmad.core.config;

import org.osgi.service.metatype.annotations.*;

@ObjectClassDefinition(
    name = "BMAD Target Cloud Configuration",
    description = "Target configuration with Cloud Manager secret injection"
)
public @interface TargetCloudConfig {

    @AttributeDefinition(
        name = "Client Code",
        description = "Use $[env:ADOBE_TARGET_CLIENT_CODE]"
    )
    String clientCode() default "$[env:ADOBE_TARGET_CLIENT_CODE]";

    @AttributeDefinition(
        name = "Property Token",
        description = "Use $[secret:ADOBE_TARGET_PROPERTY_TOKEN]"
    )
    String propertyToken() default "$[secret:ADOBE_TARGET_PROPERTY_TOKEN]";

    @AttributeDefinition(
        name = "IMS Org ID",
        description = "Use $[env:ADOBE_IMS_ORG_ID]"
    )
    String imsOrgId() default "$[env:ADOBE_IMS_ORG_ID]";
}
```

#### Environment-Specific Configs

```json
// config.prod/com.example.aem.bmad.core.config.TargetCloudConfig~prod.cfg.json
{
    "clientCode": "$[env:ADOBE_TARGET_CLIENT_CODE]",
    "propertyToken": "$[secret:ADOBE_TARGET_PROPERTY_TOKEN_PROD]",
    "imsOrgId": "$[env:ADOBE_IMS_ORG_ID]",
    "decisioningMethod": "hybrid",
    "enableA4T": true
}
```

### On-Device Decisioning

```java
package com.example.aem.bmad.core.services;

/**
 * On-Device Decisioning for zero-latency personalization.
 * Rules downloaded and cached, decisions made locally.
 */
public interface OnDeviceDecisioningService {

    boolean isAvailable();

    DecisionResult getDecision(String mboxName, java.util.Map<String, Object> context);

    void refreshArtifact();

    class DecisionResult {
        private String content;
        private boolean isDefault;
        private long latencyMs;

        public static DecisionResult defaultContent(String content) {
            DecisionResult r = new DecisionResult();
            r.content = content;
            r.isDefault = true;
            r.latencyMs = 0;
            return r;
        }

        // Getters/setters
        public String getContent() { return content; }
        public boolean isDefault() { return isDefault; }
        public long getLatencyMs() { return latencyMs; }
    }
}
```

### Mock Target Service for RDE/Local

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.TargetDeliveryService;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Mock Target service for local SDK and RDE testing.
 */
@Component(
    service = TargetDeliveryService.class,
    property = {"service.ranking:Integer=100"},
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = MockTargetServiceImpl.Config.class)
public class MockTargetServiceImpl implements TargetDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(MockTargetServiceImpl.class);

    @ObjectClassDefinition(name = "BMAD Mock Target Service")
    public @interface Config {
        @AttributeDefinition(name = "Enable") boolean enabled() default true;
        @AttributeDefinition(name = "Default Content") String defaultContent() default "<div>Mock Content</div>";
        @AttributeDefinition(name = "Latency (ms)") int latencyMs() default 50;
    }

    private Config config;

    @Activate
    protected void activate(Config config) {
        this.config = config;
        LOG.info("Mock Target Service activated");
    }

    @Override
    public List<TargetOffer> getOffers(String mboxName, Map<String, Object> params) {
        simulateLatency();

        TargetOffer offer = new TargetOffer();
        offer.setId("mock-" + mboxName);
        offer.setType("html");
        offer.setContent(config.defaultContent());
        offer.setActivityId("mock-activity");
        offer.setExperienceName("Mock Experience");

        LOG.info("[MOCK TARGET] Returning mock offer for: {}", mboxName);
        return Collections.singletonList(offer);
    }

    @Override
    public boolean isAvailable() { return config.enabled(); }

    @Override
    public TargetResponse executeDelivery(TargetRequest request) {
        return new TargetResponse();
    }

    @Override
    public List<TargetOffer> getPageLoadOffers(Map<String, Object> params) {
        return getOffers("page-load", params);
    }

    @Override
    public Map<String, List<TargetOffer>> prefetchOffers(List<String> mboxNames, Map<String, Object> params) {
        Map<String, List<TargetOffer>> result = new LinkedHashMap<>();
        mboxNames.forEach(m -> result.put(m, getOffers(m, params)));
        return result;
    }

    @Override
    public void sendNotification(TargetNotification notification) {
        LOG.info("[MOCK TARGET] Notification: {}", notification.getType());
    }

    private void simulateLatency() {
        try { Thread.sleep(config.latencyMs()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

#### Local Config

```json
// config.local/com.example.aem.bmad.core.services.impl.MockTargetServiceImpl.cfg.json
{
    "enabled": true,
    "defaultContent": "<div class='mock-personalization'>Local Mock Content</div>",
    "latencyMs": 25
}
```
