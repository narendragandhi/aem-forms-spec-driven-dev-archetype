# Adobe Analytics Integration

This document provides comprehensive patterns and implementation guidance for integrating Adobe Analytics with AEM as a Cloud Service, covering data layer architecture, event tracking, custom dimensions, and Experience Cloud integration.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Adobe Launch (Tags) Setup](#adobe-launch-tags-setup)
3. [Data Layer Implementation](#data-layer-implementation)
4. [Page View Tracking](#page-view-tracking)
5. [Component Interaction Tracking](#component-interaction-tracking)
6. [Event Tracking](#event-tracking)
7. [Custom Variables (eVars, Props, Events)](#custom-variables-evars-props-events)
8. [Experience Cloud ID Service](#experience-cloud-id-service)
9. [Server-Side Analytics Service](#server-side-analytics-service)
10. [Analytics for Target (A4T)](#analytics-for-target-a4t)
11. [Testing and Debugging](#testing-and-debugging)
12. [OSGi Configuration](#osgi-configuration)
13. [Best Practices](#best-practices)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AEM as a Cloud Service                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────────┐    ┌──────────────────────────────┐ │
│  │  AEM Page   │───▶│  Data Layer     │───▶│  Adobe Client Data Layer     │ │
│  │  Component  │    │  Sling Models   │    │  (adobeDataLayer[])          │ │
│  └─────────────┘    └─────────────────┘    └──────────────┬───────────────┘ │
│                                                            │                 │
│  ┌─────────────────────────────────────────────────────────▼───────────────┐ │
│  │                    Adobe Launch (Tags) Container                         │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │ │
│  │  │   Rules     │  │ Data        │  │ Extensions  │  │ Environment     │ │ │
│  │  │   Engine    │  │ Elements    │  │ (AA, ECID)  │  │ Config          │ │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Adobe Experience Cloud                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐  │
│  │ Adobe Analytics │  │ Experience Cloud │  │ Adobe Target (A4T)         │  │
│  │ Reporting       │  │ ID Service       │  │ Reporting Integration      │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| Adobe Client Data Layer | Standardized data layer for AEM | Core Components dependency |
| Sling Models | Server-side data layer population | `core/` bundle |
| Adobe Launch | Tag management and rule execution | External (Adobe Experience Platform) |
| Analytics Extension | Adobe Analytics tracking | Launch extension |
| ECID Extension | Cross-solution visitor identification | Launch extension |

---

## Adobe Launch (Tags) Setup

### Launch Property Configuration

```
Property Settings:
├── Name: AEM BMAD Showcase
├── Platform: Web
├── Return empty string for undefined data elements: ✓
└── Enable host-based scaling: ✓

Extensions Required:
├── Adobe Analytics (v2.x)
├── Experience Cloud ID Service (v5.x)
├── Adobe Client Data Layer (v2.x)
├── Core (v3.x)
└── Common Analytics Plugins (v2.x)
```

### Analytics Extension Configuration

```javascript
// Launch Analytics Extension Settings
{
  "libraryManagement": {
    "type": "managed",
    "reportSuites": {
      "production": ["bmad-prod-rs"],
      "staging": ["bmad-stage-rs"],
      "development": ["bmad-dev-rs"]
    }
  },
  "trackerProperties": {
    "trackingServer": "bmad.sc.omtrdc.net",
    "trackingServerSecure": "bmad.sc.omtrdc.net",
    "charSet": "UTF-8",
    "currencyCode": "USD"
  },
  "linkTracking": {
    "trackDownloadLinks": true,
    "downloadExtensions": ["pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip"],
    "trackOutboundLinks": true,
    "keepUrlParameters": false
  }
}
```

### ECID Extension Configuration

```javascript
// Experience Cloud ID Service Settings
{
  "orgId": "XXXXXXXXXXXXXX@AdobeOrg",
  "idSyncContainerID": 0,
  "useCookieDomain": true,
  "cookieDomain": ".example.com",
  "crossDomain": {
    "enabled": true,
    "domains": ["example.com", "blog.example.com", "shop.example.com"]
  },
  "optIn": {
    "enabled": true,
    "gdprApplies": true
  }
}
```

---

## Data Layer Implementation

### Core Data Layer Service

```java
package com.example.aem.bmad.core.services;

import java.util.Map;

/**
 * Service for managing Adobe Client Data Layer entries
 */
public interface DataLayerService {

    /**
     * Get page-level data layer object
     */
    Map<String, Object> getPageData(String pagePath);

    /**
     * Get component-level data layer object
     */
    Map<String, Object> getComponentData(String componentPath, String componentType);

    /**
     * Generate unique component ID for data layer
     */
    String generateComponentId(String resourcePath);

    /**
     * Check if data layer is enabled
     */
    boolean isDataLayerEnabled();
}
```

### Data Layer Service Implementation

```java
package com.example.aem.bmad.core.services.impl;

import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.example.aem.bmad.core.services.DataLayerService;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

@Component(service = DataLayerService.class, immediate = true)
@Designate(ocd = DataLayerServiceImpl.Config.class)
public class DataLayerServiceImpl implements DataLayerService {

    private static final Logger LOG = LoggerFactory.getLogger(DataLayerServiceImpl.class);
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @ObjectClassDefinition(name = "BMAD Data Layer Configuration")
    public @interface Config {

        @AttributeDefinition(name = "Enable Data Layer", description = "Enable Adobe Client Data Layer")
        boolean enabled() default true;

        @AttributeDefinition(name = "Include Component Path", description = "Include full component path in data layer")
        boolean includeComponentPath() default false;

        @AttributeDefinition(name = "Date Format", description = "Date format for timestamps")
        String dateFormat() default "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    }

    @Reference
    private ResourceResolverFactory resolverFactory;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("Data Layer service configured, enabled: {}", config.enabled());
    }

    @Override
    public Map<String, Object> getPageData(String pagePath) {
        Map<String, Object> pageData = new LinkedHashMap<>();

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "dataLayerService"))) {

            PageManager pageManager = resolver.adaptTo(PageManager.class);
            if (pageManager == null) {
                return pageData;
            }

            Page page = pageManager.getPage(pagePath);
            if (page == null) {
                return pageData;
            }

            // Core page properties
            pageData.put("@type", "bmad/components/page");
            pageData.put("repo:path", page.getPath());
            pageData.put("dc:title", page.getTitle());
            pageData.put("dc:description", page.getDescription());
            pageData.put("xdm:language", page.getLanguage(false).toString());
            pageData.put("xdm:template", page.getTemplate() != null ? page.getTemplate().getPath() : null);

            // Timestamps
            Calendar created = page.getProperties().get("jcr:created", Calendar.class);
            Calendar modified = page.getLastModified();
            if (created != null) {
                pageData.put("repo:createdAt", ISO_FORMAT.format(created.getTime()));
            }
            if (modified != null) {
                pageData.put("repo:modifiedAt", ISO_FORMAT.format(modified.getTime()));
            }

            // Tags
            String[] tags = page.getProperties().get("cq:tags", String[].class);
            if (tags != null && tags.length > 0) {
                pageData.put("xdm:tags", Arrays.asList(tags));
            }

            // Custom BMAD properties
            pageData.put("bmad:pageType", getPageType(page));
            pageData.put("bmad:siteSection", getSiteSection(page));
            pageData.put("bmad:pageHierarchy", getPageHierarchy(page));

        } catch (Exception e) {
            LOG.error("Error building page data layer for: {}", pagePath, e);
        }

        return pageData;
    }

    @Override
    public Map<String, Object> getComponentData(String componentPath, String componentType) {
        Map<String, Object> componentData = new LinkedHashMap<>();

        componentData.put("@type", componentType);
        componentData.put("repo:modifyDate", ISO_FORMAT.format(new Date()));
        componentData.put("dc:title", ""); // To be populated by component model

        if (config.includeComponentPath()) {
            componentData.put("repo:path", componentPath);
        }

        // Generate deterministic component ID
        componentData.put("id", generateComponentId(componentPath));

        return componentData;
    }

    @Override
    public String generateComponentId(String resourcePath) {
        return ComponentUtils.getId(resourcePath, null, null);
    }

    @Override
    public boolean isDataLayerEnabled() {
        return config.enabled();
    }

    private String getPageType(Page page) {
        String template = page.getTemplate() != null ? page.getTemplate().getName() : "unknown";
        return switch (template) {
            case "homepage" -> "home";
            case "content-page" -> "content";
            case "product-page" -> "product";
            case "category-page" -> "category";
            case "landing-page" -> "landing";
            case "article-page" -> "article";
            default -> "other";
        };
    }

    private String getSiteSection(Page page) {
        // Get second-level page as site section
        Page root = page.getAbsoluteParent(2);
        return root != null ? root.getName() : "root";
    }

    private String getPageHierarchy(Page page) {
        StringBuilder hierarchy = new StringBuilder();
        int depth = page.getDepth();

        for (int i = 2; i <= depth; i++) {
            Page ancestor = page.getAbsoluteParent(i);
            if (ancestor != null) {
                if (hierarchy.length() > 0) {
                    hierarchy.append("|");
                }
                hierarchy.append(ancestor.getName());
            }
        }

        return hierarchy.toString();
    }
}
```

### Page Data Layer Sling Model

```java
package com.example.aem.bmad.core.models;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.PageData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.example.aem.bmad.core.services.DataLayerService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.*;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = {AnalyticsPageModel.class, ComponentExporter.class},
    resourceType = "bmad/components/page"
)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION
)
public class AnalyticsPageModel implements ComponentExporter {

    static final String RESOURCE_TYPE = "bmad/components/page";

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private Style currentStyle;

    @OSGiService
    private DataLayerService dataLayerService;

    private Map<String, Object> analyticsData;

    @PostConstruct
    protected void init() {
        analyticsData = new LinkedHashMap<>();

        if (dataLayerService.isDataLayerEnabled()) {
            buildAnalyticsData();
        }
    }

    private void buildAnalyticsData() {
        // Page Info
        Map<String, Object> pageInfo = new LinkedHashMap<>();
        pageInfo.put("pageName", getPageName());
        pageInfo.put("pageTitle", currentPage.getTitle());
        pageInfo.put("pageType", getPageType());
        pageInfo.put("pageUrl", request.getRequestURL().toString());
        pageInfo.put("pagePath", currentPage.getPath());
        pageInfo.put("language", currentPage.getLanguage(false).toString());
        pageInfo.put("siteSection", getSiteSection());
        pageInfo.put("server", request.getServerName());

        // Content Info
        Map<String, Object> contentInfo = new LinkedHashMap<>();
        contentInfo.put("template", currentPage.getTemplate() != null ?
            currentPage.getTemplate().getTitle() : "");
        contentInfo.put("author", currentPage.getProperties().get("jcr:createdBy", ""));
        contentInfo.put("publishDate", getPublishDate());
        contentInfo.put("tags", getTags());
        contentInfo.put("category", getCategory());

        // User Info (populated client-side or via authenticated state)
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("authState", "anonymous"); // Will be updated client-side
        userInfo.put("userType", "visitor");

        // Build analytics object
        analyticsData.put("page", pageInfo);
        analyticsData.put("content", contentInfo);
        analyticsData.put("user", userInfo);
        analyticsData.put("event", "pageLoad");
    }

    @JsonProperty("analyticsData")
    public Map<String, Object> getAnalyticsData() {
        return analyticsData;
    }

    @JsonProperty("dataLayerJson")
    public String getDataLayerJson() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(analyticsData);
        } catch (Exception e) {
            return "{}";
        }
    }

    @JsonIgnore
    public String getPageName() {
        // Format: Site:Section:PageName
        StringBuilder pageName = new StringBuilder();
        pageName.append("bmad:");
        pageName.append(getSiteSection());
        pageName.append(":");
        pageName.append(currentPage.getName());
        return pageName.toString();
    }

    @JsonIgnore
    public String getPageType() {
        String template = currentPage.getTemplate() != null ?
            currentPage.getTemplate().getName() : "";

        return switch (template) {
            case "homepage", "home" -> "Home";
            case "content-page", "article" -> "Content";
            case "product-page", "product" -> "Product";
            case "category-page", "category" -> "Category";
            case "landing-page", "campaign" -> "Landing";
            case "search-results" -> "Search";
            case "form-page" -> "Form";
            default -> "Other";
        };
    }

    @JsonIgnore
    public String getSiteSection() {
        Page sectionPage = currentPage.getAbsoluteParent(2);
        if (sectionPage != null) {
            return sectionPage.getTitle() != null ? sectionPage.getTitle() : sectionPage.getName();
        }
        return "Home";
    }

    @JsonIgnore
    public String getPublishDate() {
        Calendar onTime = currentPage.getProperties().get("cq:lastReplicated", Calendar.class);
        if (onTime != null) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").format(onTime.getTime());
        }
        return "";
    }

    @JsonIgnore
    public List<String> getTags() {
        String[] tags = currentPage.getProperties().get("cq:tags", String[].class);
        if (tags != null) {
            List<String> tagList = new ArrayList<>();
            for (String tag : tags) {
                // Extract tag title from path
                String[] parts = tag.split("/");
                tagList.add(parts[parts.length - 1]);
            }
            return tagList;
        }
        return Collections.emptyList();
    }

    @JsonIgnore
    public String getCategory() {
        List<String> tags = getTags();
        if (!tags.isEmpty()) {
            return tags.get(0);
        }
        return "";
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
```

### Component Data Layer Mixin

```java
package com.example.aem.bmad.core.models;

import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.util.Date;

/**
 * Mixin interface for components that support data layer
 */
public interface DataLayerAware {

    /**
     * Get component data layer object
     */
    @JsonProperty("dataLayer")
    default ComponentData getDataLayer() {
        return DataLayerBuilder.forComponent()
            .withId(getComponentId())
            .withType(getResourceType())
            .withTitle(getTitle())
            .withLastModifiedDate(getLastModifiedDate())
            .build();
    }

    /**
     * Get unique component ID for tracking
     */
    String getComponentId();

    /**
     * Get component resource type
     */
    String getResourceType();

    /**
     * Get component title for tracking
     */
    String getTitle();

    /**
     * Get last modified date
     */
    Date getLastModifiedDate();

    /**
     * Check if data layer tracking is enabled
     */
    default boolean isDataLayerEnabled() {
        return true;
    }
}
```

### Enhanced Hero Model with Data Layer

```java
package com.example.aem.bmad.core.models;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = {HeroModelWithAnalytics.class, ComponentExporter.class},
    resourceType = HeroModelWithAnalytics.RESOURCE_TYPE
)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION
)
public class HeroModelWithAnalytics implements ComponentExporter, DataLayerAware {

    static final String RESOURCE_TYPE = "aem-bmad-showcase/components/content/hero";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue
    @Optional
    private String heading;

    @ValueMapValue
    @Optional
    private String subheading;

    @ValueMapValue
    @Optional
    private String backgroundImage;

    @ValueMapValue
    @Optional
    private String ctaButtonText;

    @ValueMapValue
    @Optional
    private String ctaButtonLink;

    @ValueMapValue(name = "jcr:lastModified")
    @Optional
    private Calendar lastModified;

    private String componentId;

    @PostConstruct
    protected void init() {
        componentId = ComponentUtils.getId(resource, null, null);
    }

    // Standard getters
    public String getHeading() { return heading; }
    public String getSubheading() { return subheading; }
    public String getBackgroundImage() { return backgroundImage; }
    public String getCtaButtonText() { return ctaButtonText; }
    public String getCtaButtonLink() { return ctaButtonLink; }

    // Data Layer implementation
    @Override
    public String getComponentId() {
        return componentId;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getTitle() {
        return heading;
    }

    @Override
    public Date getLastModifiedDate() {
        return lastModified != null ? lastModified.getTime() : null;
    }

    @Override
    public ComponentData getDataLayer() {
        return DataLayerBuilder.extending(
                DataLayerBuilder.forComponent()
                    .withId(componentId)
                    .withType(RESOURCE_TYPE)
                    .withTitle(heading)
                    .withLastModifiedDate(getLastModifiedDate())
                    .build()
            )
            .withCustomProperty("bmad:ctaText", ctaButtonText)
            .withCustomProperty("bmad:ctaLink", ctaButtonLink)
            .withCustomProperty("bmad:hasImage", backgroundImage != null && !backgroundImage.isEmpty())
            .build();
    }

    /**
     * Get data layer JSON attribute for HTL
     */
    public String getDataLayerJson() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(getDataLayer().getJson());
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }
}
```

---

## Page View Tracking

### Page Template HTL Integration

```html
<!-- page.html -->
<sly data-sly-use.page="com.example.aem.bmad.core.models.AnalyticsPageModel">
    <!DOCTYPE html>
    <html lang="${currentPage.language}"
          data-cmp-data-layer-enabled>
        <head>
            <sly data-sly-call="${head.head @ page=page}"/>

            <!-- Adobe Launch Async Script (Header) -->
            <script src="//assets.adobedtm.com/launch-XXXXX-development.min.js" async></script>

            <!-- Data Layer Initialization (Before Launch) -->
            <script>
                window.adobeDataLayer = window.adobeDataLayer || [];
                window.digitalData = window.digitalData || {};

                // Initialize page data immediately
                window.adobeDataLayer.push({
                    "event": "cmp:show",
                    "eventInfo": {
                        "path": "page.${currentPage.name @ context='scriptString'}"
                    },
                    "page": ${page.dataLayerJson @ context='unsafe'}
                });
            </script>
        </head>
        <body class="page"
              id="${page.componentId}"
              data-cmp-data-layer='${page.dataLayerJson}'>

            <sly data-sly-call="${body.body @ page=page}"/>

            <!-- Page Load Complete Event -->
            <script>
                window.adobeDataLayer.push({
                    "event": "pageLoadComplete",
                    "page": ${page.dataLayerJson @ context='unsafe'}
                });
            </script>
        </body>
    </html>
</sly>
```

### Launch Rule: Page View Tracking

```javascript
// Launch Rule Configuration
{
  "name": "Page View - All Pages",
  "events": [
    {
      "modulePath": "core/src/lib/events/libraryLoaded.js",
      "settings": {}
    }
  ],
  "conditions": [],
  "actions": [
    {
      "modulePath": "adobe-analytics/src/lib/actions/setVariables.js",
      "settings": {
        "trackerProperties": {
          "pageName": "%page.pageName%",
          "channel": "%page.siteSection%",
          "prop1": "%page.pageType%",
          "prop2": "%page.language%",
          "prop3": "%page.template%",
          "eVar1": "%page.pageName%",
          "eVar2": "%page.pageType%",
          "eVar3": "%page.siteSection%",
          "eVar5": "%content.category%",
          "eVar10": "%content.author%",
          "eVar15": "%user.authState%",
          "events": "event1"
        }
      }
    },
    {
      "modulePath": "adobe-analytics/src/lib/actions/sendBeacon.js",
      "settings": {
        "type": "page"
      }
    }
  ]
}
```

---

## Component Interaction Tracking

### Component Click Tracking HTL

```html
<!-- hero.html with click tracking -->
<sly data-sly-use.hero="com.example.aem.bmad.core.models.HeroModelWithAnalytics">
    <section class="cmp-hero"
             id="${hero.componentId}"
             data-cmp-data-layer='${hero.dataLayerJson}'
             data-cmp-clickable
             role="banner"
             aria-labelledby="${hero.componentId}-title">

        <div class="cmp-hero__content">
            <h1 id="${hero.componentId}-title" class="cmp-hero__heading">
                ${hero.heading}
            </h1>

            <p class="cmp-hero__subheading">${hero.subheading}</p>

            <sly data-sly-test="${hero.ctaButtonText && hero.ctaButtonLink}">
                <a href="${hero.ctaButtonLink @ context='uri'}"
                   class="cmp-hero__cta"
                   data-cmp-clickable
                   data-analytics-click='{"event":"ctaClick","ctaText":"${hero.ctaButtonText @ context='scriptString'}","ctaLink":"${hero.ctaButtonLink @ context='scriptString'}","component":"hero"}'>
                    ${hero.ctaButtonText}
                </a>
            </sly>
        </div>

        <sly data-sly-test="${hero.backgroundImage}">
            <div class="cmp-hero__image"
                 style="background-image: url('${hero.backgroundImage @ context='uri'}');"
                 role="img"
                 aria-hidden="true">
            </div>
        </sly>
    </section>
</sly>
```

### Client-Side Click Tracking

```javascript
// analytics-tracking.js
(function() {
    'use strict';

    var dataLayer = window.adobeDataLayer = window.adobeDataLayer || [];

    /**
     * Track component clicks
     */
    function initClickTracking() {
        document.addEventListener('click', function(event) {
            var clickableElement = event.target.closest('[data-cmp-clickable]');

            if (clickableElement) {
                var componentElement = clickableElement.closest('[data-cmp-data-layer]');
                var componentData = componentElement ?
                    JSON.parse(componentElement.getAttribute('data-cmp-data-layer')) : {};

                // Check for specific analytics data
                var analyticsData = clickableElement.getAttribute('data-analytics-click');
                if (analyticsData) {
                    try {
                        analyticsData = JSON.parse(analyticsData);
                    } catch (e) {
                        analyticsData = {};
                    }
                }

                dataLayer.push({
                    event: 'cmp:click',
                    eventInfo: {
                        path: componentData.id || 'unknown'
                    },
                    component: componentData,
                    interaction: {
                        type: 'click',
                        element: clickableElement.tagName.toLowerCase(),
                        text: clickableElement.textContent.trim().substring(0, 100),
                        href: clickableElement.href || null,
                        ...analyticsData
                    }
                });
            }
        });
    }

    /**
     * Track component visibility (impressions)
     */
    function initImpressionTracking() {
        if (!('IntersectionObserver' in window)) {
            return;
        }

        var observer = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    var element = entry.target;
                    var componentData = element.getAttribute('data-cmp-data-layer');

                    if (componentData && !element.hasAttribute('data-tracked-impression')) {
                        element.setAttribute('data-tracked-impression', 'true');

                        dataLayer.push({
                            event: 'cmp:show',
                            eventInfo: {
                                path: JSON.parse(componentData).id
                            },
                            component: JSON.parse(componentData)
                        });
                    }
                }
            });
        }, {
            threshold: 0.5 // 50% visibility
        });

        document.querySelectorAll('[data-cmp-data-layer]').forEach(function(element) {
            observer.observe(element);
        });
    }

    /**
     * Track form interactions
     */
    function initFormTracking() {
        document.addEventListener('submit', function(event) {
            var form = event.target;
            if (form.tagName !== 'FORM') return;

            var formName = form.getAttribute('name') || form.id || 'unnamed-form';
            var formAction = form.action || window.location.href;

            dataLayer.push({
                event: 'formSubmit',
                form: {
                    name: formName,
                    action: formAction,
                    method: form.method || 'GET',
                    fieldCount: form.elements.length
                }
            });
        });

        // Track form field interactions
        document.addEventListener('focus', function(event) {
            var field = event.target;
            if (!field.form) return;

            var formName = field.form.getAttribute('name') || field.form.id || 'unnamed-form';
            var fieldName = field.name || field.id || 'unnamed-field';

            dataLayer.push({
                event: 'formFieldFocus',
                form: {
                    name: formName,
                    field: fieldName,
                    fieldType: field.type || field.tagName.toLowerCase()
                }
            });
        }, true);
    }

    /**
     * Track scroll depth
     */
    function initScrollTracking() {
        var milestones = [25, 50, 75, 90, 100];
        var trackedMilestones = {};

        function getScrollPercent() {
            var docHeight = Math.max(
                document.body.scrollHeight,
                document.documentElement.scrollHeight
            );
            var winHeight = window.innerHeight;
            var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            return Math.round((scrollTop / (docHeight - winHeight)) * 100);
        }

        window.addEventListener('scroll', debounce(function() {
            var percent = getScrollPercent();

            milestones.forEach(function(milestone) {
                if (percent >= milestone && !trackedMilestones[milestone]) {
                    trackedMilestones[milestone] = true;

                    dataLayer.push({
                        event: 'scrollDepth',
                        scroll: {
                            depth: milestone,
                            direction: 'down'
                        }
                    });
                }
            });
        }, 100));
    }

    /**
     * Track video interactions
     */
    function initVideoTracking() {
        document.querySelectorAll('video').forEach(function(video) {
            var videoId = video.id || video.src.split('/').pop();
            var trackedQuartiles = {};

            video.addEventListener('play', function() {
                dataLayer.push({
                    event: 'videoPlay',
                    video: { id: videoId, currentTime: video.currentTime }
                });
            });

            video.addEventListener('pause', function() {
                dataLayer.push({
                    event: 'videoPause',
                    video: { id: videoId, currentTime: video.currentTime }
                });
            });

            video.addEventListener('ended', function() {
                dataLayer.push({
                    event: 'videoComplete',
                    video: { id: videoId, duration: video.duration }
                });
            });

            video.addEventListener('timeupdate', function() {
                var percent = Math.round((video.currentTime / video.duration) * 100);
                var quartiles = [25, 50, 75];

                quartiles.forEach(function(q) {
                    if (percent >= q && !trackedQuartiles[q]) {
                        trackedQuartiles[q] = true;
                        dataLayer.push({
                            event: 'videoProgress',
                            video: { id: videoId, progress: q }
                        });
                    }
                });
            });
        });
    }

    // Utility: Debounce function
    function debounce(func, wait) {
        var timeout;
        return function() {
            var context = this, args = arguments;
            clearTimeout(timeout);
            timeout = setTimeout(function() {
                func.apply(context, args);
            }, wait);
        };
    }

    // Initialize all tracking
    document.addEventListener('DOMContentLoaded', function() {
        initClickTracking();
        initImpressionTracking();
        initFormTracking();
        initScrollTracking();
        initVideoTracking();
    });

})();
```

---

## Event Tracking

### Custom Event Service

```java
package com.example.aem.bmad.core.services;

import java.util.Map;

/**
 * Service for tracking custom analytics events server-side
 */
public interface AnalyticsEventService {

    /**
     * Track a custom event
     */
    void trackEvent(String eventName, Map<String, Object> properties);

    /**
     * Track a transaction/conversion
     */
    void trackConversion(String conversionType, double value, String orderId, Map<String, Object> properties);

    /**
     * Track an error
     */
    void trackError(String errorType, String errorMessage, String errorLocation);

    /**
     * Track search
     */
    void trackSearch(String searchTerm, int resultCount, String searchType);

    /**
     * Track content interaction
     */
    void trackContentInteraction(String contentId, String interactionType, Map<String, Object> metadata);
}
```

### Event Service Implementation

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.AnalyticsEventService;
import com.example.aem.bmad.core.services.HttpClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = AnalyticsEventService.class, immediate = true)
@Designate(ocd = AnalyticsEventServiceImpl.Config.class)
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsEventServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ObjectClassDefinition(name = "BMAD Analytics Event Service Configuration")
    public @interface Config {

        @AttributeDefinition(name = "Data Insertion API Endpoint")
        String apiEndpoint() default "https://api.omniture.com/admin/1.4/rest/";

        @AttributeDefinition(name = "Report Suite ID")
        String reportSuiteId();

        @AttributeDefinition(name = "API Username")
        String apiUsername();

        @AttributeDefinition(name = "API Secret")
        String apiSecret();

        @AttributeDefinition(name = "Enable Server-Side Tracking")
        boolean enableServerSide() default false;
    }

    @Reference
    private HttpClientService httpClient;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("Analytics Event Service configured for report suite: {}", config.reportSuiteId());
    }

    @Override
    public void trackEvent(String eventName, Map<String, Object> properties) {
        if (!config.enableServerSide()) {
            LOG.debug("Server-side tracking disabled, skipping event: {}", eventName);
            return;
        }

        try {
            Map<String, Object> eventData = buildEventPayload(eventName, properties);
            sendToAnalytics(eventData);
            LOG.info("Tracked event: {}", eventName);
        } catch (Exception e) {
            LOG.error("Failed to track event: {}", eventName, e);
        }
    }

    @Override
    public void trackConversion(String conversionType, double value, String orderId,
                                 Map<String, Object> properties) {
        Map<String, Object> conversionData = new HashMap<>(properties != null ? properties : new HashMap<>());
        conversionData.put("conversionType", conversionType);
        conversionData.put("revenue", value);
        conversionData.put("orderId", orderId);
        conversionData.put("events", "purchase");

        trackEvent("conversion", conversionData);
    }

    @Override
    public void trackError(String errorType, String errorMessage, String errorLocation) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorType", errorType);
        errorData.put("errorMessage", errorMessage);
        errorData.put("errorLocation", errorLocation);
        errorData.put("events", "event50"); // Custom error event

        trackEvent("error", errorData);
    }

    @Override
    public void trackSearch(String searchTerm, int resultCount, String searchType) {
        Map<String, Object> searchData = new HashMap<>();
        searchData.put("searchTerm", searchTerm);
        searchData.put("searchResults", resultCount);
        searchData.put("searchType", searchType);
        searchData.put("events", resultCount > 0 ? "event20" : "event21"); // Search success/null

        trackEvent("internalSearch", searchData);
    }

    @Override
    public void trackContentInteraction(String contentId, String interactionType,
                                          Map<String, Object> metadata) {
        Map<String, Object> interactionData = new HashMap<>(metadata != null ? metadata : new HashMap<>());
        interactionData.put("contentId", contentId);
        interactionData.put("interactionType", interactionType);

        trackEvent("contentInteraction", interactionData);
    }

    private Map<String, Object> buildEventPayload(String eventName, Map<String, Object> properties) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reportSuiteID", config.reportSuiteId());
        payload.put("timestamp", System.currentTimeMillis() / 1000);
        payload.put("eventName", eventName);

        if (properties != null) {
            payload.putAll(properties);
        }

        return payload;
    }

    private void sendToAnalytics(Map<String, Object> eventData) throws Exception {
        String payload = MAPPER.writeValueAsString(eventData);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-WSSE", generateWSSEHeader());

        var response = httpClient.post(config.apiEndpoint(), payload, headers);

        if (!response.isSuccess()) {
            throw new RuntimeException("Analytics API error: " + response.getBody());
        }
    }

    private String generateWSSEHeader() {
        // Generate WSSE authentication header for Adobe Analytics API
        String nonce = UUID.randomUUID().toString();
        String created = java.time.Instant.now().toString();
        String digest = generateDigest(nonce, created, config.apiSecret());

        return String.format(
            "UsernameToken Username=\"%s\", PasswordDigest=\"%s\", Nonce=\"%s\", Created=\"%s\"",
            config.apiUsername(), digest,
            Base64.getEncoder().encodeToString(nonce.getBytes()),
            created
        );
    }

    private String generateDigest(String nonce, String created, String secret) {
        try {
            String combined = nonce + created + secret;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate digest", e);
        }
    }
}
```

### Launch Rules for Event Tracking

```javascript
// Launch Rule: CTA Click Tracking
{
  "name": "CTA Click Tracking",
  "events": [
    {
      "modulePath": "adobe-client-data-layer/src/lib/events/dataLayerEvent.js",
      "settings": {
        "eventNames": ["cmp:click"]
      }
    }
  ],
  "conditions": [
    {
      "modulePath": "core/src/lib/conditions/customCode.js",
      "settings": {
        "source": "return event.interaction && event.interaction.type === 'click' && event.interaction.event === 'ctaClick';"
      }
    }
  ],
  "actions": [
    {
      "modulePath": "adobe-analytics/src/lib/actions/setVariables.js",
      "settings": {
        "trackerProperties": {
          "linkTrackVars": "prop10,prop11,eVar20,eVar21,events",
          "linkTrackEvents": "event10",
          "prop10": "%interaction.ctaText%",
          "prop11": "%interaction.component%",
          "eVar20": "%interaction.ctaText%",
          "eVar21": "%interaction.ctaLink%",
          "events": "event10"
        }
      }
    },
    {
      "modulePath": "adobe-analytics/src/lib/actions/sendBeacon.js",
      "settings": {
        "type": "link",
        "linkName": "CTA Click - %interaction.ctaText%",
        "linkType": "o"
      }
    }
  ]
}

// Launch Rule: Form Submission
{
  "name": "Form Submission Tracking",
  "events": [
    {
      "modulePath": "adobe-client-data-layer/src/lib/events/dataLayerEvent.js",
      "settings": {
        "eventNames": ["formSubmit"]
      }
    }
  ],
  "conditions": [],
  "actions": [
    {
      "modulePath": "adobe-analytics/src/lib/actions/setVariables.js",
      "settings": {
        "trackerProperties": {
          "linkTrackVars": "prop15,eVar30,events",
          "linkTrackEvents": "event15",
          "prop15": "%form.name%",
          "eVar30": "%form.name%",
          "events": "event15"
        }
      }
    },
    {
      "modulePath": "adobe-analytics/src/lib/actions/sendBeacon.js",
      "settings": {
        "type": "link",
        "linkName": "Form Submit - %form.name%",
        "linkType": "o"
      }
    }
  ]
}

// Launch Rule: Scroll Depth
{
  "name": "Scroll Depth Tracking",
  "events": [
    {
      "modulePath": "adobe-client-data-layer/src/lib/events/dataLayerEvent.js",
      "settings": {
        "eventNames": ["scrollDepth"]
      }
    }
  ],
  "conditions": [],
  "actions": [
    {
      "modulePath": "adobe-analytics/src/lib/actions/setVariables.js",
      "settings": {
        "trackerProperties": {
          "linkTrackVars": "prop20,events",
          "linkTrackEvents": "event25",
          "prop20": "%scroll.depth%",
          "events": "event25"
        }
      }
    },
    {
      "modulePath": "adobe-analytics/src/lib/actions/sendBeacon.js",
      "settings": {
        "type": "link",
        "linkName": "Scroll Depth - %scroll.depth%%",
        "linkType": "o"
      }
    }
  ]
}
```

---

## Custom Variables (eVars, Props, Events)

### Variable Mapping Specification

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BMAD Analytics Variable Map                          │
├─────────────────────────────────────────────────────────────────────────────┤
│ TRAFFIC VARIABLES (Props)                                                    │
├──────────┬───────────────────────────────┬─────────────────────────────────┤
│ Variable │ Name                          │ Description                      │
├──────────┼───────────────────────────────┼─────────────────────────────────┤
│ prop1    │ Page Type                     │ Homepage, Content, Product, etc. │
│ prop2    │ Language                      │ Page language code               │
│ prop3    │ Template                      │ AEM template name                │
│ prop4    │ Site Section                  │ Top-level navigation section     │
│ prop5    │ Sub-Section                   │ Second-level navigation          │
│ prop10   │ CTA Text                      │ Clicked CTA button text          │
│ prop11   │ Component Type                │ Interacted component type        │
│ prop15   │ Form Name                     │ Submitted form name              │
│ prop20   │ Scroll Depth                  │ Page scroll percentage           │
│ prop25   │ Search Term                   │ Internal search query            │
│ prop30   │ Error Type                    │ Error category                   │
│ prop50   │ Campaign ID                   │ Marketing campaign identifier    │
├──────────┴───────────────────────────────┴─────────────────────────────────┤
│ CONVERSION VARIABLES (eVars)                                                 │
├──────────┬───────────────────────────────┬────────────────┬────────────────┤
│ Variable │ Name                          │ Expiration     │ Allocation     │
├──────────┼───────────────────────────────┼────────────────┼────────────────┤
│ eVar1    │ Page Name                     │ Page View      │ Most Recent    │
│ eVar2    │ Page Type                     │ Visit          │ Most Recent    │
│ eVar3    │ Site Section                  │ Visit          │ Most Recent    │
│ eVar5    │ Content Category              │ Visit          │ Most Recent    │
│ eVar10   │ Author                        │ Never          │ Original       │
│ eVar15   │ User Auth State               │ Visit          │ Most Recent    │
│ eVar20   │ CTA Text                      │ Visit          │ Most Recent    │
│ eVar21   │ CTA Destination               │ Visit          │ Most Recent    │
│ eVar25   │ Search Term                   │ Visit          │ Most Recent    │
│ eVar26   │ Search Results Count          │ Hit            │ Most Recent    │
│ eVar30   │ Form Name                     │ Visit          │ Most Recent    │
│ eVar40   │ Campaign (Tracking Code)      │ 30 Days        │ Original       │
│ eVar50   │ Product ID                    │ Purchase       │ Most Recent    │
│ eVar51   │ Product Category              │ Purchase       │ Most Recent    │
│ eVar60   │ Target Experience             │ Visit          │ Most Recent    │
│ eVar70   │ Error Message                 │ Hit            │ Most Recent    │
├──────────┴───────────────────────────────┴────────────────┴────────────────┤
│ SUCCESS EVENTS                                                               │
├──────────┬───────────────────────────────┬─────────────────────────────────┤
│ Event    │ Name                          │ Type                             │
├──────────┼───────────────────────────────┼─────────────────────────────────┤
│ event1   │ Page View                     │ Counter                          │
│ event5   │ Unique Visitor                │ Counter                          │
│ event10  │ CTA Click                     │ Counter                          │
│ event15  │ Form Submit                   │ Counter                          │
│ event16  │ Form Start                    │ Counter                          │
│ event17  │ Form Abandon                  │ Counter                          │
│ event20  │ Internal Search               │ Counter                          │
│ event21  │ Search - No Results           │ Counter                          │
│ event25  │ Scroll Milestone              │ Counter                          │
│ event30  │ Video Start                   │ Counter                          │
│ event31  │ Video 25%                     │ Counter                          │
│ event32  │ Video 50%                     │ Counter                          │
│ event33  │ Video 75%                     │ Counter                          │
│ event34  │ Video Complete                │ Counter                          │
│ event40  │ Download                      │ Counter                          │
│ event50  │ Error                         │ Counter                          │
│ event60  │ Newsletter Signup             │ Counter                          │
│ event70  │ Social Share                  │ Counter                          │
│ event80  │ Print Page                    │ Counter                          │
│ event100 │ Purchase                      │ Counter                          │
│ event101 │ Revenue                       │ Currency                         │
│ event102 │ Units                         │ Numeric                          │
└──────────┴───────────────────────────────┴─────────────────────────────────┘
```

### Variable Helper Class

```java
package com.example.aem.bmad.core.analytics;

import java.util.*;

/**
 * Helper class for Adobe Analytics variable mapping
 */
public final class AnalyticsVariables {

    private AnalyticsVariables() {}

    // Props
    public static final String PROP_PAGE_TYPE = "prop1";
    public static final String PROP_LANGUAGE = "prop2";
    public static final String PROP_TEMPLATE = "prop3";
    public static final String PROP_SITE_SECTION = "prop4";
    public static final String PROP_SUB_SECTION = "prop5";
    public static final String PROP_CTA_TEXT = "prop10";
    public static final String PROP_COMPONENT_TYPE = "prop11";
    public static final String PROP_FORM_NAME = "prop15";
    public static final String PROP_SCROLL_DEPTH = "prop20";
    public static final String PROP_SEARCH_TERM = "prop25";
    public static final String PROP_ERROR_TYPE = "prop30";
    public static final String PROP_CAMPAIGN = "prop50";

    // eVars
    public static final String EVAR_PAGE_NAME = "eVar1";
    public static final String EVAR_PAGE_TYPE = "eVar2";
    public static final String EVAR_SITE_SECTION = "eVar3";
    public static final String EVAR_CONTENT_CATEGORY = "eVar5";
    public static final String EVAR_AUTHOR = "eVar10";
    public static final String EVAR_AUTH_STATE = "eVar15";
    public static final String EVAR_CTA_TEXT = "eVar20";
    public static final String EVAR_CTA_DESTINATION = "eVar21";
    public static final String EVAR_SEARCH_TERM = "eVar25";
    public static final String EVAR_SEARCH_RESULTS = "eVar26";
    public static final String EVAR_FORM_NAME = "eVar30";
    public static final String EVAR_CAMPAIGN = "eVar40";
    public static final String EVAR_PRODUCT_ID = "eVar50";
    public static final String EVAR_PRODUCT_CATEGORY = "eVar51";
    public static final String EVAR_TARGET_EXPERIENCE = "eVar60";
    public static final String EVAR_ERROR_MESSAGE = "eVar70";

    // Events
    public static final String EVENT_PAGE_VIEW = "event1";
    public static final String EVENT_UNIQUE_VISITOR = "event5";
    public static final String EVENT_CTA_CLICK = "event10";
    public static final String EVENT_FORM_SUBMIT = "event15";
    public static final String EVENT_FORM_START = "event16";
    public static final String EVENT_FORM_ABANDON = "event17";
    public static final String EVENT_INTERNAL_SEARCH = "event20";
    public static final String EVENT_SEARCH_NO_RESULTS = "event21";
    public static final String EVENT_SCROLL_MILESTONE = "event25";
    public static final String EVENT_VIDEO_START = "event30";
    public static final String EVENT_VIDEO_25 = "event31";
    public static final String EVENT_VIDEO_50 = "event32";
    public static final String EVENT_VIDEO_75 = "event33";
    public static final String EVENT_VIDEO_COMPLETE = "event34";
    public static final String EVENT_DOWNLOAD = "event40";
    public static final String EVENT_ERROR = "event50";
    public static final String EVENT_NEWSLETTER_SIGNUP = "event60";
    public static final String EVENT_SOCIAL_SHARE = "event70";
    public static final String EVENT_PRINT_PAGE = "event80";
    public static final String EVENT_PURCHASE = "event100";
    public static final String EVENT_REVENUE = "event101";
    public static final String EVENT_UNITS = "event102";

    /**
     * Build an events string from multiple events
     */
    public static String buildEvents(String... events) {
        return String.join(",", events);
    }

    /**
     * Build a linkTrackVars string
     */
    public static String buildLinkTrackVars(String... vars) {
        return String.join(",", vars);
    }
}
```

---

## Experience Cloud ID Service

### ECID Integration Service

```java
package com.example.aem.bmad.core.services;

/**
 * Service for Experience Cloud ID operations
 */
public interface ExperienceCloudIdService {

    /**
     * Get the Experience Cloud Org ID
     */
    String getOrgId();

    /**
     * Check if ECID is properly configured
     */
    boolean isConfigured();

    /**
     * Get visitor ID from cookie (server-side)
     */
    String getVisitorId(javax.servlet.http.HttpServletRequest request);

    /**
     * Generate supplemental data ID for Target integration
     */
    String generateSupplementalDataId();
}
```

### ECID Service Implementation

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.ExperienceCloudIdService;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component(service = ExperienceCloudIdService.class, immediate = true)
@Designate(ocd = ExperienceCloudIdServiceImpl.Config.class)
public class ExperienceCloudIdServiceImpl implements ExperienceCloudIdService {

    private static final Logger LOG = LoggerFactory.getLogger(ExperienceCloudIdServiceImpl.class);
    private static final String AMCV_COOKIE_PREFIX = "AMCV_";

    @ObjectClassDefinition(name = "BMAD Experience Cloud ID Configuration")
    public @interface Config {

        @AttributeDefinition(name = "Organization ID", description = "Experience Cloud Organization ID")
        String orgId();

        @AttributeDefinition(name = "Cookie Domain", description = "Cookie domain for ECID")
        String cookieDomain() default ".example.com";
    }

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("ECID Service configured for org: {}", config.orgId());
    }

    @Override
    public String getOrgId() {
        return config.orgId();
    }

    @Override
    public boolean isConfigured() {
        return config.orgId() != null && !config.orgId().isEmpty();
    }

    @Override
    public String getVisitorId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String cookieName = AMCV_COOKIE_PREFIX + config.orgId().replace("@", "_");

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                return parseVisitorIdFromCookie(cookie.getValue());
            }
        }

        return null;
    }

    @Override
    public String generateSupplementalDataId() {
        // Generate SDID for A4T integration
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String parseVisitorIdFromCookie(String cookieValue) {
        // AMCV cookie format: MCMID|<visitor_id>|...|
        if (cookieValue == null || !cookieValue.contains("MCMID|")) {
            return null;
        }

        try {
            String[] parts = cookieValue.split("\\|");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("MCMID".equals(parts[i])) {
                    return parts[i + 1];
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse ECID from cookie", e);
        }

        return null;
    }
}
```

---

## Server-Side Analytics Service

### Analytics HTTP Client

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.HttpClientService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = AnalyticsHttpClient.class, immediate = true)
@Designate(ocd = AnalyticsHttpClient.Config.class)
public class AnalyticsHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsHttpClient.class);

    @ObjectClassDefinition(name = "BMAD Analytics HTTP Client Configuration")
    public @interface Config {

        @AttributeDefinition(name = "Connection Timeout", description = "Connection timeout in ms")
        int connectionTimeout() default 5000;

        @AttributeDefinition(name = "Socket Timeout", description = "Socket timeout in ms")
        int socketTimeout() default 10000;

        @AttributeDefinition(name = "Max Connections", description = "Maximum total connections")
        int maxConnections() default 100;

        @AttributeDefinition(name = "Max Per Route", description = "Maximum connections per route")
        int maxPerRoute() default 20;
    }

    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    @Activate
    protected void activate(Config config) {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(config.maxConnections());
        connectionManager.setDefaultMaxPerRoute(config.maxPerRoute());

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(config.connectionTimeout())
            .setSocketTimeout(config.socketTimeout())
            .build();

        httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();

        LOG.info("Analytics HTTP client initialized with max connections: {}", config.maxConnections());
    }

    @Deactivate
    protected void deactivate() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
            if (connectionManager != null) {
                connectionManager.close();
            }
        } catch (Exception e) {
            LOG.error("Error closing HTTP client", e);
        }
    }

    public CloseableHttpClient getClient() {
        return httpClient;
    }
}
```

---

## Analytics for Target (A4T)

### A4T Integration Service

```java
package com.example.aem.bmad.core.services;

import java.util.Map;

/**
 * Service for Analytics for Target (A4T) integration
 */
public interface A4TService {

    /**
     * Get A4T payload for Target request
     */
    Map<String, Object> getA4TPayload(String visitorId, String supplementalDataId);

    /**
     * Log Target activity impression to Analytics
     */
    void logActivityImpression(String activityId, String experienceId, String visitorId);

    /**
     * Log Target conversion to Analytics
     */
    void logConversion(String activityId, String experienceId, String visitorId,
                       String goalId, Map<String, Object> conversionData);

    /**
     * Generate supplemental data ID for request correlation
     */
    String generateSdid();
}
```

### A4T Service Implementation

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.A4TService;
import com.example.aem.bmad.core.services.AnalyticsEventService;
import com.example.aem.bmad.core.services.ExperienceCloudIdService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = A4TService.class, immediate = true)
public class A4TServiceImpl implements A4TService {

    private static final Logger LOG = LoggerFactory.getLogger(A4TServiceImpl.class);

    @Reference
    private ExperienceCloudIdService ecidService;

    @Reference
    private AnalyticsEventService analyticsService;

    @Override
    public Map<String, Object> getA4TPayload(String visitorId, String supplementalDataId) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Analytics payload for Target
        Map<String, Object> analytics = new LinkedHashMap<>();
        Map<String, Object> logging = new LinkedHashMap<>();

        logging.put("logToAnalytics", true);
        logging.put("supplementalDataId", supplementalDataId != null ? supplementalDataId : generateSdid());

        analytics.put("logging", logging);
        payload.put("analytics", analytics);

        // Experience Cloud context
        Map<String, Object> experienceCloud = new LinkedHashMap<>();
        Map<String, Object> audienceManager = new LinkedHashMap<>();

        if (visitorId != null) {
            audienceManager.put("blob", visitorId);
        }

        experienceCloud.put("audienceManager", audienceManager);
        payload.put("experienceCloud", experienceCloud);

        return payload;
    }

    @Override
    public void logActivityImpression(String activityId, String experienceId, String visitorId) {
        Map<String, Object> impressionData = new HashMap<>();
        impressionData.put("activityId", activityId);
        impressionData.put("experienceId", experienceId);
        impressionData.put("visitorId", visitorId);
        impressionData.put("events", "event200"); // A4T impression event

        analyticsService.trackEvent("a4t:impression", impressionData);
        LOG.debug("Logged A4T impression for activity: {}", activityId);
    }

    @Override
    public void logConversion(String activityId, String experienceId, String visitorId,
                               String goalId, Map<String, Object> conversionData) {
        Map<String, Object> data = new HashMap<>(conversionData != null ? conversionData : new HashMap<>());
        data.put("activityId", activityId);
        data.put("experienceId", experienceId);
        data.put("visitorId", visitorId);
        data.put("goalId", goalId);
        data.put("events", "event201"); // A4T conversion event

        analyticsService.trackEvent("a4t:conversion", data);
        LOG.debug("Logged A4T conversion for activity: {}, goal: {}", activityId, goalId);
    }

    @Override
    public String generateSdid() {
        return ecidService.generateSupplementalDataId();
    }
}
```

---

## Testing and Debugging

### Analytics Debug Mode

```javascript
// Enable debug mode in browser console
localStorage.setItem('sdsat_debug', 'true');

// Or via URL parameter
// https://example.com?_sdsat_debug=true

// Adobe Analytics debug
_satellite.setDebug(true);

// Check data layer
console.log(window.adobeDataLayer);

// Check Analytics object
console.log(s);
console.log(s.version);

// Check tracking server
console.log(s.trackingServer);
console.log(s.trackingServerSecure);
```

### Launch Debugging Script

```javascript
// analytics-debug.js - Include in development only
(function() {
    'use strict';

    // Only run in non-production
    if (window.location.hostname.includes('prod')) {
        return;
    }

    // Log all data layer pushes
    var originalPush = window.adobeDataLayer.push;
    window.adobeDataLayer.push = function() {
        console.group('📊 Data Layer Push');
        console.log(arguments);
        console.groupEnd();
        return originalPush.apply(this, arguments);
    };

    // Log all Analytics calls
    if (typeof s !== 'undefined') {
        var originalTrack = s.t;
        var originalTrackLink = s.tl;

        s.t = function() {
            console.group('📈 Analytics Page View');
            console.log('pageName:', s.pageName);
            console.log('events:', s.events);
            console.log('eVars:', getEVars());
            console.log('props:', getProps());
            console.groupEnd();
            return originalTrack.apply(this, arguments);
        };

        s.tl = function() {
            console.group('🔗 Analytics Link Track');
            console.log('linkName:', arguments[2]);
            console.log('linkType:', arguments[1]);
            console.log('events:', s.linkTrackEvents);
            console.groupEnd();
            return originalTrackLink.apply(this, arguments);
        };
    }

    function getEVars() {
        var evars = {};
        for (var i = 1; i <= 75; i++) {
            if (s['eVar' + i]) {
                evars['eVar' + i] = s['eVar' + i];
            }
        }
        return evars;
    }

    function getProps() {
        var props = {};
        for (var i = 1; i <= 75; i++) {
            if (s['prop' + i]) {
                props['prop' + i] = s['prop' + i];
            }
        }
        return props;
    }

    console.log('🐛 Analytics Debug Mode Enabled');
})();
```

### Unit Test for Data Layer Model

```java
package com.example.aem.bmad.core.models;

import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class AnalyticsPageModelTest {

    private final AemContext context = new AemContext();

    @BeforeEach
    void setUp() {
        context.load().json("/analytics/page-content.json", "/content/bmad/en/home");
    }

    @Test
    void testAnalyticsDataPopulation() {
        Page page = context.pageManager().getPage("/content/bmad/en/home");
        context.currentPage(page);
        context.currentResource(page.getContentResource());

        AnalyticsPageModel model = context.request().adaptTo(AnalyticsPageModel.class);

        assertNotNull(model);
        Map<String, Object> analyticsData = model.getAnalyticsData();

        assertNotNull(analyticsData);
        assertTrue(analyticsData.containsKey("page"));
        assertTrue(analyticsData.containsKey("content"));
        assertTrue(analyticsData.containsKey("event"));

        @SuppressWarnings("unchecked")
        Map<String, Object> pageData = (Map<String, Object>) analyticsData.get("page");
        assertEquals("bmad:Home:home", pageData.get("pageName"));
        assertEquals("Home", pageData.get("pageType"));
    }

    @Test
    void testDataLayerJsonGeneration() {
        Page page = context.pageManager().getPage("/content/bmad/en/home");
        context.currentPage(page);
        context.currentResource(page.getContentResource());

        AnalyticsPageModel model = context.request().adaptTo(AnalyticsPageModel.class);

        String json = model.getDataLayerJson();

        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("pageName"));
    }
}
```

---

## OSGi Configuration

### Analytics Configuration Factory

```java
package com.example.aem.bmad.core.config;

import org.osgi.service.metatype.annotations.*;

@ObjectClassDefinition(
    name = "BMAD Adobe Analytics Configuration",
    description = "Configuration for Adobe Analytics integration per environment"
)
public @interface AdobeAnalyticsConfig {

    @AttributeDefinition(
        name = "Environment",
        description = "Environment name (dev, stage, prod)"
    )
    String environment() default "dev";

    @AttributeDefinition(
        name = "Report Suite ID",
        description = "Adobe Analytics Report Suite ID"
    )
    String reportSuiteId();

    @AttributeDefinition(
        name = "Tracking Server",
        description = "Analytics tracking server hostname"
    )
    String trackingServer();

    @AttributeDefinition(
        name = "Tracking Server Secure",
        description = "Analytics secure tracking server hostname"
    )
    String trackingServerSecure();

    @AttributeDefinition(
        name = "Experience Cloud Org ID",
        description = "Adobe Experience Cloud Organization ID"
    )
    String experienceCloudOrgId();

    @AttributeDefinition(
        name = "Launch Property ID",
        description = "Adobe Launch Property ID"
    )
    String launchPropertyId();

    @AttributeDefinition(
        name = "Launch Environment",
        description = "Adobe Launch environment (development, staging, production)"
    )
    String launchEnvironment() default "development";

    @AttributeDefinition(
        name = "Enable Data Layer",
        description = "Enable Adobe Client Data Layer"
    )
    boolean enableDataLayer() default true;

    @AttributeDefinition(
        name = "Enable Server-Side Tracking",
        description = "Enable server-side Analytics calls"
    )
    boolean enableServerSideTracking() default false;

    @AttributeDefinition(
        name = "Debug Mode",
        description = "Enable Analytics debug mode"
    )
    boolean debugMode() default false;
}
```

### Environment-Specific OSGi Configs

```
ui.config/src/main/content/jcr_root/apps/aem-bmad-showcase/osgiconfig/

config.dev/
└── com.example.aem.bmad.core.config.AdobeAnalyticsConfig~dev.cfg.json

config.stage/
└── com.example.aem.bmad.core.config.AdobeAnalyticsConfig~stage.cfg.json

config.prod/
└── com.example.aem.bmad.core.config.AdobeAnalyticsConfig~prod.cfg.json
```

**config.dev/com.example.aem.bmad.core.config.AdobeAnalyticsConfig~dev.cfg.json:**
```json
{
    "environment": "dev",
    "reportSuiteId": "bmad-dev-rs",
    "trackingServer": "bmad.sc.omtrdc.net",
    "trackingServerSecure": "bmad.sc.omtrdc.net",
    "experienceCloudOrgId": "XXXXXXXXXXXXXX@AdobeOrg",
    "launchPropertyId": "XXXXXXXXXXXXXXXXX",
    "launchEnvironment": "development",
    "enableDataLayer": true,
    "enableServerSideTracking": false,
    "debugMode": true
}
```

**config.prod/com.example.aem.bmad.core.config.AdobeAnalyticsConfig~prod.cfg.json:**
```json
{
    "environment": "prod",
    "reportSuiteId": "bmad-prod-rs",
    "trackingServer": "bmad.sc.omtrdc.net",
    "trackingServerSecure": "bmad.sc.omtrdc.net",
    "experienceCloudOrgId": "XXXXXXXXXXXXXX@AdobeOrg",
    "launchPropertyId": "XXXXXXXXXXXXXXXXX",
    "launchEnvironment": "production",
    "enableDataLayer": true,
    "enableServerSideTracking": true,
    "debugMode": false
}
```

---

## Best Practices

### Implementation Checklist

```
□ Data Layer
  □ Adobe Client Data Layer initialized before Launch
  □ Page-level data populated on DOM ready
  □ Component data attributes properly set
  □ Custom properties follow naming convention

□ Launch Configuration
  □ Analytics extension configured with correct report suite
  □ ECID extension enabled with correct Org ID
  □ Data Elements mapped to data layer paths
  □ Rules follow naming convention

□ Variable Governance
  □ Variable map documented and maintained
  □ Props used for traffic analysis
  □ eVars configured with appropriate expiration
  □ Events follow logical numbering scheme

□ Performance
  □ Launch script loaded async
  □ Minimal blocking in data layer population
  □ Server-side tracking for critical events
  □ Connection pooling for API calls

□ Privacy & Compliance
  □ Opt-in/opt-out mechanism implemented
  □ GDPR data deletion support
  □ Cookie consent integration
  □ Data retention policies configured

□ Testing
  □ Debug mode available in non-prod
  □ Data layer unit tests
  □ Launch rule validation
  □ Cross-browser testing
  □ Mobile device testing
```

### Performance Optimization

```javascript
// Defer non-critical tracking
requestIdleCallback(function() {
    // Track scroll depth
    initScrollTracking();

    // Track component impressions
    initImpressionTracking();
});

// Use beacon for exit events
window.addEventListener('beforeunload', function() {
    navigator.sendBeacon('/analytics/exit', JSON.stringify({
        page: window.location.pathname,
        timeOnPage: performance.now()
    }));
});
```

---

## Traceability

| Spec ID | Component | Description | Status |
|---------|-----------|-------------|--------|
| AA-001 | Data Layer Service | Core data layer service | Documented |
| AA-002 | Page Model | Analytics page model with data layer | Documented |
| AA-003 | Component Model | Data layer aware components | Documented |
| AA-004 | Client Tracking | Click, scroll, form, video tracking | Documented |
| AA-005 | Event Service | Server-side event tracking | Documented |
| AA-006 | ECID Service | Experience Cloud ID integration | Documented |
| AA-007 | A4T Service | Analytics for Target integration | Documented |
| AA-008 | Launch Rules | Page view, CTA, form, scroll rules | Documented |
| AA-009 | Variable Map | Props, eVars, events specification | Documented |
| AA-010 | OSGi Config | Environment-specific configuration | Documented |
| AA-011 | CDN/Dispatcher | Cloud-native caching patterns | Documented |
| AA-012 | Stateless Architecture | Visitor tracking without sessions | Documented |
| AA-013 | Cloud Manager Secrets | Secret variable injection | Documented |
| AA-014 | RDE/Local Testing | Mock services for development | Documented |

---

## AEMaaCS Cloud Architecture Patterns

This section covers cloud-native architectural considerations specific to AEM as a Cloud Service for Analytics integration.

### CDN and Dispatcher Caching Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Analytics Caching Architecture                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  [Visitor] ──▶ [Fastly CDN] ──▶ [Dispatcher] ──▶ [AEM Publish]             │
│                    │                  │                │                     │
│                    │                  │                │                     │
│              Cache Static        Cache HTML      Generate                    │
│              Assets (JS/CSS)     (with ESI)     Data Layer                  │
│                    │                  │                │                     │
│                    ▼                  ▼                ▼                     │
│              Long TTL           Short TTL        No Cache                    │
│              (1 year)           (5-10 min)      (dynamic)                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Dispatcher Configuration for Analytics

```apache
# dispatcher/src/conf.d/rewrites/analytics-rewrites.rules

# Analytics beacon endpoints - bypass cache
RewriteRule ^/analytics/(.*)$ - [PT,E=ANALYTICS_REQUEST:true]

# Data layer JSON endpoints - short cache
RewriteRule ^/content/(.*)\.datalayer\.json$ - [PT,E=DATALAYER:true]
```

```apache
# dispatcher/src/conf.dispatcher.d/filters/analytics-filters.any

# Allow analytics beacon requests
/0200 { /type "allow" /method "POST" /url "/analytics/*" }

# Allow data layer JSON requests
/0201 { /type "allow" /method "GET" /url "/content/*.datalayer.json" }

# Block direct access to tracking pixels from cache
/0202 { /type "deny" /url "*.gif" /query "s_*" }
```

```apache
# dispatcher/src/conf.dispatcher.d/cache/analytics-cache.any

# Cache rules for analytics-related content
/analytics-cache {
    # Static Launch library - cache aggressively
    /0001 {
        /glob "*.adobedtm.com/*"
        /type "allow"
    }

    # Data layer JSON - short TTL
    /0002 {
        /glob "*.datalayer.json"
        /type "allow"
    }

    # Never cache beacon responses
    /0003 {
        /glob "/analytics/*"
        /type "deny"
    }
}
```

#### CDN Cache Headers for Analytics Assets

```java
package com.example.aem.bmad.core.filters;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Component;

import javax.servlet.*;
import java.io.IOException;

@Component(
    service = Filter.class,
    property = {
        "sling.filter.scope=REQUEST",
        "sling.filter.pattern=/content/.*\\.datalayer\\.json",
        "service.ranking:Integer=700"
    }
)
public class DataLayerCacheHeaderFilter implements Filter {

    private static final int SHORT_CACHE_TTL = 300; // 5 minutes

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof SlingHttpServletResponse) {
            SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

            // Set short cache TTL for data layer
            slingResponse.setHeader("Cache-Control", "public, max-age=" + SHORT_CACHE_TTL);
            slingResponse.setHeader("Surrogate-Control", "max-age=" + SHORT_CACHE_TTL);

            // Add Vary header for personalization
            slingResponse.setHeader("Vary", "Cookie, Accept-Encoding");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
```

### Stateless Architecture Patterns

AEM as a Cloud Service is stateless by design. Analytics tracking must not rely on server-side sessions.

#### Stateless Visitor Identification

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.VisitorIdentificationService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import java.util.UUID;

/**
 * Stateless visitor identification service.
 * Uses cookies and headers - no server-side session state.
 */
@Component(service = VisitorIdentificationService.class)
public class StatelessVisitorIdentificationService implements VisitorIdentificationService {

    private static final Logger LOG = LoggerFactory.getLogger(StatelessVisitorIdentificationService.class);

    // Cookie names
    private static final String ECID_COOKIE = "AMCV_";
    private static final String VISITOR_COOKIE = "bmad_visitor_id";
    private static final String SESSION_COOKIE = "bmad_session_id";

    @Override
    public String getVisitorId(SlingHttpServletRequest request) {
        // Priority 1: Experience Cloud ID from AMCV cookie
        String ecid = getEcidFromCookie(request);
        if (ecid != null) {
            return ecid;
        }

        // Priority 2: First-party visitor cookie
        String visitorId = getCookieValue(request, VISITOR_COOKIE);
        if (visitorId != null) {
            return visitorId;
        }

        // Priority 3: Generate new visitor ID (will be set client-side)
        return generateVisitorId();
    }

    @Override
    public String getSessionId(SlingHttpServletRequest request) {
        // Session ID from cookie - no server session
        String sessionId = getCookieValue(request, SESSION_COOKIE);
        if (sessionId != null) {
            return sessionId;
        }

        // Generate new session ID
        return generateSessionId();
    }

    @Override
    public boolean isNewVisitor(SlingHttpServletRequest request) {
        return getCookieValue(request, VISITOR_COOKIE) == null &&
               getEcidFromCookie(request) == null;
    }

    @Override
    public boolean isNewSession(SlingHttpServletRequest request) {
        return getCookieValue(request, SESSION_COOKIE) == null;
    }

    private String getEcidFromCookie(SlingHttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (cookie.getName().startsWith(ECID_COOKIE)) {
                return parseEcidFromAmcvCookie(cookie.getValue());
            }
        }
        return null;
    }

    private String parseEcidFromAmcvCookie(String cookieValue) {
        // AMCV cookie format: MCMID|<ecid>|...
        if (cookieValue != null && cookieValue.contains("MCMID|")) {
            String[] parts = cookieValue.split("\\|");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("MCMID".equals(parts[i])) {
                    return parts[i + 1];
                }
            }
        }
        return null;
    }

    private String getCookieValue(SlingHttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String generateVisitorId() {
        return "v_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateSessionId() {
        return "s_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
```

#### Visitor Context from Headers (No Session)

```java
package com.example.aem.bmad.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Extracts visitor context from request headers - stateless approach
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class StatelessVisitorContext {

    @Self
    private SlingHttpServletRequest request;

    private Map<String, Object> context;

    @PostConstruct
    protected void init() {
        context = new HashMap<>();

        // Geo context from CDN headers (Fastly)
        context.put("geo.country", getHeader("X-Geo-Country", "US"));
        context.put("geo.region", getHeader("X-Geo-Region", ""));
        context.put("geo.city", getHeader("X-Geo-City", ""));
        context.put("geo.latitude", getHeader("X-Geo-Latitude", ""));
        context.put("geo.longitude", getHeader("X-Geo-Longitude", ""));

        // Device context
        context.put("device.type", detectDeviceType());
        context.put("device.isMobile", isMobileDevice());

        // Request context
        context.put("request.referrer", request.getHeader("Referer"));
        context.put("request.userAgent", request.getHeader("User-Agent"));
        context.put("request.acceptLanguage", request.getHeader("Accept-Language"));

        // Akamai/Fastly edge context
        context.put("edge.cacheStatus", getHeader("X-Cache", ""));
        context.put("edge.pop", getHeader("X-Served-By", ""));
    }

    private String getHeader(String name, String defaultValue) {
        String value = request.getHeader(name);
        return value != null ? value : defaultValue;
    }

    private String detectDeviceType() {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "unknown";

        ua = ua.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android")) {
            return "mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "tablet";
        }
        return "desktop";
    }

    private boolean isMobileDevice() {
        return "mobile".equals(detectDeviceType());
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public String getCountry() {
        return (String) context.get("geo.country");
    }

    public String getDeviceType() {
        return (String) context.get("device.type");
    }
}
```

### Cloud Manager Secret Management

#### Secret Variable Configuration

```yaml
# Cloud Manager Environment Variables (via UI or API)
# These are injected at runtime and never stored in code

# Analytics Secrets
ADOBE_ANALYTICS_REPORT_SUITE_PROD: "bmad-prod-rs"
ADOBE_ANALYTICS_REPORT_SUITE_STAGE: "bmad-stage-rs"
ADOBE_ANALYTICS_API_KEY: "<secret>"
ADOBE_ANALYTICS_API_SECRET: "<secret>"

# Experience Cloud
ADOBE_IMS_ORG_ID: "XXXXXXXXXXXXXX@AdobeOrg"
ADOBE_LAUNCH_PROPERTY_ID: "<property-id>"

# Data Insertion API
ADOBE_ANALYTICS_INSERTION_ENDPOINT: "https://api.omniture.com/admin/1.4/rest/"
```

#### OSGi Configuration with Secret Injection

```java
package com.example.aem.bmad.core.config;

import org.osgi.service.metatype.annotations.*;

@ObjectClassDefinition(
    name = "BMAD Analytics Cloud Configuration",
    description = "Analytics configuration with Cloud Manager secret injection"
)
public @interface AnalyticsCloudConfig {

    @AttributeDefinition(
        name = "Report Suite ID",
        description = "Report suite ID - use $[env:ADOBE_ANALYTICS_REPORT_SUITE_PROD] for production"
    )
    String reportSuiteId() default "$[env:ADOBE_ANALYTICS_REPORT_SUITE_PROD]";

    @AttributeDefinition(
        name = "IMS Org ID",
        description = "Experience Cloud Org ID - use $[env:ADOBE_IMS_ORG_ID]"
    )
    String imsOrgId() default "$[env:ADOBE_IMS_ORG_ID]";

    @AttributeDefinition(
        name = "API Key",
        description = "Analytics API key - use $[secret:ADOBE_ANALYTICS_API_KEY]"
    )
    String apiKey() default "$[secret:ADOBE_ANALYTICS_API_KEY]";

    @AttributeDefinition(
        name = "API Secret",
        description = "Analytics API secret - use $[secret:ADOBE_ANALYTICS_API_SECRET]"
    )
    String apiSecret() default "$[secret:ADOBE_ANALYTICS_API_SECRET]";

    @AttributeDefinition(
        name = "Launch Property ID",
        description = "Adobe Launch property ID"
    )
    String launchPropertyId() default "$[env:ADOBE_LAUNCH_PROPERTY_ID]";
}
```

#### Environment-Specific Config Files

```
ui.config/src/main/content/jcr_root/apps/aem-bmad-showcase/osgiconfig/

config/
└── com.example.aem.bmad.core.config.AnalyticsCloudConfig.cfg.json

config.dev/
└── com.example.aem.bmad.core.config.AnalyticsCloudConfig~dev.cfg.json

config.stage/
└── com.example.aem.bmad.core.config.AnalyticsCloudConfig~stage.cfg.json

config.prod/
└── com.example.aem.bmad.core.config.AnalyticsCloudConfig~prod.cfg.json
```

**config.prod/com.example.aem.bmad.core.config.AnalyticsCloudConfig~prod.cfg.json:**
```json
{
    "reportSuiteId": "$[env:ADOBE_ANALYTICS_REPORT_SUITE_PROD]",
    "imsOrgId": "$[env:ADOBE_IMS_ORG_ID]",
    "apiKey": "$[secret:ADOBE_ANALYTICS_API_KEY]",
    "apiSecret": "$[secret:ADOBE_ANALYTICS_API_SECRET]",
    "launchPropertyId": "$[env:ADOBE_LAUNCH_PROPERTY_ID]"
}
```

### RDE and Local SDK Testing

#### Mock Analytics Service for Local Development

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.AnalyticsEventService;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mock Analytics service for local SDK and RDE testing.
 * Logs events instead of sending to Adobe Analytics.
 */
@Component(
    service = AnalyticsEventService.class,
    property = {
        "service.ranking:Integer=100"  // Higher ranking for local override
    },
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = MockAnalyticsServiceImpl.Config.class)
public class MockAnalyticsServiceImpl implements AnalyticsEventService {

    private static final Logger LOG = LoggerFactory.getLogger(MockAnalyticsServiceImpl.class);

    @ObjectClassDefinition(name = "BMAD Mock Analytics Service (Local/RDE)")
    public @interface Config {

        @AttributeDefinition(name = "Enable Mock Service")
        boolean enabled() default true;

        @AttributeDefinition(name = "Log Level", options = {
            @Option(label = "DEBUG", value = "DEBUG"),
            @Option(label = "INFO", value = "INFO"),
            @Option(label = "WARN", value = "WARN")
        })
        String logLevel() default "INFO";

        @AttributeDefinition(name = "Log to Console")
        boolean logToConsole() default true;

        @AttributeDefinition(name = "Store Events in Memory")
        boolean storeEvents() default true;
    }

    private Config config;
    private java.util.List<Map<String, Object>> storedEvents = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("Mock Analytics Service activated - events will be logged, not sent to Adobe");
    }

    @Override
    public void trackEvent(String eventName, Map<String, Object> properties) {
        if (!config.enabled()) {
            return;
        }

        String message = String.format("[MOCK ANALYTICS] Event: %s | Properties: %s", eventName, properties);

        switch (config.logLevel()) {
            case "DEBUG":
                LOG.debug(message);
                break;
            case "WARN":
                LOG.warn(message);
                break;
            default:
                LOG.info(message);
        }

        if (config.storeEvents()) {
            Map<String, Object> event = new java.util.HashMap<>(properties);
            event.put("_eventName", eventName);
            event.put("_timestamp", System.currentTimeMillis());
            storedEvents.add(event);
        }
    }

    @Override
    public void trackConversion(String conversionType, double value, String orderId, Map<String, Object> properties) {
        Map<String, Object> conversionData = new java.util.HashMap<>(properties != null ? properties : new java.util.HashMap<>());
        conversionData.put("conversionType", conversionType);
        conversionData.put("value", value);
        conversionData.put("orderId", orderId);
        trackEvent("conversion", conversionData);
    }

    @Override
    public void trackError(String errorType, String errorMessage, String errorLocation) {
        trackEvent("error", Map.of(
            "errorType", errorType,
            "errorMessage", errorMessage,
            "errorLocation", errorLocation
        ));
    }

    @Override
    public void trackSearch(String searchTerm, int resultCount, String searchType) {
        trackEvent("search", Map.of(
            "searchTerm", searchTerm,
            "resultCount", resultCount,
            "searchType", searchType
        ));
    }

    @Override
    public void trackContentInteraction(String contentId, String interactionType, Map<String, Object> metadata) {
        Map<String, Object> data = new java.util.HashMap<>(metadata != null ? metadata : new java.util.HashMap<>());
        data.put("contentId", contentId);
        data.put("interactionType", interactionType);
        trackEvent("contentInteraction", data);
    }

    /**
     * Get all stored events (for testing assertions)
     */
    public java.util.List<Map<String, Object>> getStoredEvents() {
        return new java.util.ArrayList<>(storedEvents);
    }

    /**
     * Clear stored events
     */
    public void clearStoredEvents() {
        storedEvents.clear();
    }
}
```

#### Local SDK OSGi Config

```json
// config.local/com.example.aem.bmad.core.services.impl.MockAnalyticsServiceImpl.cfg.json
{
    "enabled": true,
    "logLevel": "DEBUG",
    "logToConsole": true,
    "storeEvents": true
}
```

#### RDE Testing Servlet

```java
package com.example.aem.bmad.core.servlets;

import com.example.aem.bmad.core.services.impl.MockAnalyticsServiceImpl;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import java.io.IOException;

/**
 * Debug servlet for viewing captured analytics events in RDE/local
 * Available at: /bin/bmad/analytics/debug
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/bmad/analytics/debug",
        "sling.servlet.methods=GET"
    }
)
public class AnalyticsDebugServlet extends SlingSafeMethodsServlet {

    @Reference(target = "(component.name=*.MockAnalyticsServiceImpl)")
    private MockAnalyticsServiceImpl mockService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        if (mockService == null) {
            response.getWriter().write("{\"error\": \"Mock service not available - only works in local/RDE\"}");
            return;
        }

        var events = mockService.getStoredEvents();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(response.getWriter(), events);
    }
}
```

### Content Fragment Analytics Integration

```java
package com.example.aem.bmad.core.models;

import com.adobe.cq.dam.cfm.ContentFragment;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Model for tracking Content Fragment analytics
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ContentFragmentAnalyticsModel.class,
    resourceType = "dam/cfm/components/contentfragment"
)
public class ContentFragmentAnalyticsModel {

    @SlingObject
    private Resource resource;

    @Self
    private SlingHttpServletRequest request;

    private Map<String, Object> analyticsData;
    private ContentFragment fragment;

    @PostConstruct
    protected void init() {
        analyticsData = new LinkedHashMap<>();

        Resource fragmentResource = resource.getChild("jcr:content");
        if (fragmentResource != null) {
            fragment = fragmentResource.adaptTo(ContentFragment.class);
        }

        if (fragment != null) {
            buildAnalyticsData();
        }
    }

    private void buildAnalyticsData() {
        analyticsData.put("contentType", "content-fragment");
        analyticsData.put("fragmentId", fragment.getName());
        analyticsData.put("fragmentTitle", fragment.getTitle());
        analyticsData.put("fragmentModel", getModelPath());
        analyticsData.put("fragmentPath", resource.getPath());

        // Fragment metadata
        analyticsData.put("fragmentDescription", fragment.getDescription());
        analyticsData.put("fragmentTags", getFragmentTags());

        // Variation info
        String variation = request.getParameter("variation");
        if (variation != null) {
            analyticsData.put("variation", variation);
        }

        // Element tracking
        List<String> elements = new ArrayList<>();
        fragment.getElements().forEachRemaining(el -> elements.add(el.getName()));
        analyticsData.put("elements", elements);
    }

    private String getModelPath() {
        try {
            return fragment.getTemplate().getTitle();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private List<String> getFragmentTags() {
        // Extract tags from fragment metadata
        return Collections.emptyList();
    }

    public Map<String, Object> getAnalyticsData() {
        return analyticsData;
    }

    public String getAnalyticsJson() {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(analyticsData);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```
