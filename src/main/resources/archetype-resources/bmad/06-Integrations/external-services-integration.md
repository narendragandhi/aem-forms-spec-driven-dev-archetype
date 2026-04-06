# External Services Integration

This document provides patterns and examples for integrating AEM with external enterprise services including CRM platforms, analytics tools, translation services, and other third-party APIs.

## Table of Contents

1. [Form Data Model (FDM)](#form-data-model-fdm)
2. [GraphQL FDM](#graphql-fdm)
3. [OData FDM](#odata-fdm)
4. [Edge Delivery Services](#edge-delivery-services)
5. [CRM Integration (Salesforce)](#crm-integration-salesforce)
6. [Adobe Analytics Integration](#adobe-analytics-integration)
7. [Translation Service Integration](#translation-service-integration)
8. [Commerce Integration](#commerce-integration)
9. [Email Service Integration](#email-service-integration)

---

## Form Data Model (FDM)

### Overview

Form Data Models (FDM) are the backbone of data integration in AEM Forms, providing:
- Data source abstraction
- Pre-fill capabilities
- Submission handling
- Service orchestration

### Creating an FDM

```bash
# FDM location
/conf/{appName}/settings/fdm/
```

### FDM Structure

```xml
<!-- /conf/app/settings/fdm/customer-fdm/.content.xml -->
<jcr:root xmlns:jcr="http://jcp.org/jcr/1.0"
    jcr:primaryType="fdm:formdatamodel"
    jcr:title="Customer Data FDM"
    fdm:description="FDM for customer data operations"
    fdm:schemaVersion="1.0">
    
    <dataSources jcr:primaryType="nt:unstructured">
        <rest-service
            fdm:type="rest"
            fdm:serviceReference="/conf/global/settings/cloudconfigs/fdm/rest-customer"/>
    </dataSources>
    
    <entities jcr:primaryType="nt:unstructured">
        <customer
            jcr:primaryType="fdm:entity"
            fdm:label="Customer"
            fdm:url="customer/{id}">
            <firstName fdm:dataType="string"/>
            <lastName fdm:dataType="string"/>
            <email fdm:dataType="string"/>
            <phone fdm:dataType="string"/>
        </customer>
    </entities>
</jcr:root>
```

---

## GraphQL FDM

### When to Use GraphQL

- Complex data queries with nested relationships
- Mobile applications requiring efficient data loading
- Headless Adaptive Forms with React/Vue/Angular
- AFaaCS (Adaptive Forms as a Cloud Service)

### GraphQL FDM Configuration

```xml
<!-- /conf/app/settings/fdm/graphql-customer-fdm/.content.xml -->
<jcr:root xmlns:jcr="http://jcp.org/jcr/1.0"
    jcr:primaryType="fdm:formdatamodel"
    jcr:title="GraphQL Customer FDM"
    fdm:description="FDM using GraphQL for customer data"
    fdm:schemaVersion="2.0">
    
    <dataSources jcr:primaryType="nt:unstructured">
        <graphql-ds
            fdm:type="graphql"
            fdm:serviceReference="/conf/global/settings/cloudconfigs/fdm/graphql-customer"
            fdm:graphiqlEnabled="true"/>
    </dataSources>
    
    <entities jcr:primaryType="nt:unstructured">
        <customerList
            fdm:label="Customers"
            fdm:query="query GetCustomers($limit: Int, $offset: Int) { customers(limit: $limit, offset: $offset) { id firstName lastName email } }">
            <id fdm:dataType="string"/>
            <firstName fdm:dataType="string"/>
            <lastName fdm:dataType="string"/>
            <email fdm:dataType="string"/>
        </customerList>
        
        <customerDetail
            fdm:label="Customer Detail"
            fdm:query="query GetCustomer($id: ID!) { customer(id: $id) { id firstName lastName email address { street city state zip } orders { id total status } } }">
            <id fdm:dataType="string"/>
            <firstName fdm:dataType="string"/>
            <lastName fdm:dataType="string"/>
            <email fdm:dataType="string"/>
            <address fdm:dataType="object">
                <street fdm:dataType="string"/>
                <city fdm:dataType="string"/>
                <state fdm:dataType="string"/>
                <zip fdm:dataType="string"/>
            </address>
            <orders fdm:dataType="array">
                <id fdm:dataType="string"/>
                <total fdm:dataType="number"/>
                <status fdm:dataType="string"/>
            </orders>
        </customerDetail>
    </entities>
</jcr:root>
```

### GraphQL Schema Example

```graphql
# schema.graphqls
type Customer {
    id: ID!
    firstName: String!
    lastName: String!
    email: String!
    phone: String
    address: Address
    orders: [Order]
}

type Address {
    street: String!
    city: String!
    state: String!
    zip: String!
}

type Order {
    id: ID!
    total: Float!
    status: OrderStatus!
    items: [OrderItem]
}

enum OrderStatus {
    PENDING
    PROCESSING
    SHIPPED
    DELIVERED
    CANCELLED
}

type Query {
    customers(limit: Int, offset: Int): [Customer]
    customer(id: ID!): Customer
}

type Mutation {
    createCustomer(input: CustomerInput!): Customer
    updateCustomer(id: ID!, input: CustomerInput!): Customer
    deleteCustomer(id: ID!): Boolean
}
```

### Headless Form Integration

```javascript
// React component using GraphQL FDM
import { useAdaptiveForm } from '@adobe/aem-forms-af-react';

const CustomerList = () => {
    const { data, submit } = useAdaptiveForm({
        fdm: '/conf/app/settings/fdm/graphql-customer-fdm',
        entity: 'customerList'
    });
    
    // GraphQL queries are handled automatically by the FDM
    return (
        <Form>
            <FieldArray name="customers">
                {data?.customers?.map(customer => (
                    <div key={customer.id}>
                        {customer.firstName} {customer.lastName}
                    </div>
                ))}
            </FieldArray>
        </Form>
    );
};
```

---

## OData FDM

### When to Use OData

- Microsoft ecosystem integration (Dynamics, SharePoint)
- Standardized REST API with query options
- Enterprise APIs following OData protocol
- $select, $filter, $expand queries

### OData FDM Configuration

```xml
<!-- /conf/app/settings/fdm/odata-customer-fdm/.content.xml -->
<jcr:root xmlns:jcr="http://jcp.org/jcr/1.0"
    jcr:primaryType="fdm:formdatamodel"
    jcr:title="OData Customer FDM"
    fdm:description="FDM using OData for Microsoft integration"
    fdm:schemaVersion="2.0">
    
    <dataSources jcr:primaryType="nt:unstructured">
        <odata-ds
            fdm:type="odata"
            fdm:serviceReference="/conf/global/settings/cloudconfigs/fdm/odata-microsoft"
            fdm:authenticationType="oauth2"/>
    </dataSources>
    
    <entities jcr:primaryType="nt:unstructured">
        <customerEntity
            fdm:label="Customer"
            fdm:url="Customers"
            fdm:queryTemplate="$select=FirstName,LastName,Email&$filter=Active eq true">
            <ID fdm:dataType="string" fdm:key="true"/>
            <FirstName fdm:dataType="string"/>
            <LastName fdm:dataType="string"/>
            <Email fdm:dataType="string"/>
            <Company fdm:dataType="string"/>
        </customerEntity>
        
        <orderEntity
            fdm:label="Orders"
            fdm:url="Orders"
            fdm:queryTemplate="$expand=Customer&$orderby=OrderDate desc">
            <ID fdm:dataType="string" fdm:key="true"/>
            <CustomerID fdm:dataType="string"/>
            <OrderDate fdm:dataType="date"/>
            <Total fdm:dataType="number"/>
            <Status fdm:dataType="string"/>
        </orderEntity>
    </entities>
</jcr:root>
```

### OData Query Examples

| OData Query | Description |
|------------|-------------|
| `$select=FirstName,LastName` | Select specific fields |
| `$filter=Active eq true` | Filter records |
| `$orderby=LastName` | Sort results |
| `$expand=Orders` | Include related data |
| `$top=10` | Limit results |
| `$skip=20` | Paginate results |

---

## Edge Delivery Services

### When to Use Edge Delivery

- High-performance form rendering
- Microsoft Word/Google Docs authoring
- Franklin (block-based editor)
- Universal Editor integration

### Edge Delivery FDM

```xml
<!-- /conf/app/settings/fdm/edge-delivery-fdm/.content.xml -->
<jcr:root xmlns:jcr="http://jcp.org/jcr/1.0"
    jcr:primaryType="fdm:formdatamodel"
    jcr:title="Edge Delivery FDM"
    fdm:description="FDM for Edge Delivery Services"
    fdm:schemaVersion="2.0">
    
    <dataSources jcr:primaryType="nt:unstructured">
        <edge-ds
            fdm:type="rest"
            fdm:serviceReference="/conf/global/settings/cloudconfigs/fdm/edge-delivery"
            fdm:useAemProxy="true"/>
    </dataSources>
    
    <entities jcr:primaryType="nt:unstructured">
        <formSubmission
            fdm:label="Form Submission"
            fdm:postUrl="/{formPath}/submit">
            <formPath fdm:dataType="string"/>
            <data fdm:dataType="object"/>
            <metadata fdm:dataType="object"/>
        </formSubmission>
    </entities>
</jcr:root>
```

### Microsoft Word Authoring

Forms can be authored in Microsoft Word and published to Edge Delivery Services:

```bash
# SharePoint document library
# /sites/forms/loan-application.docx
```

Word document structure:
- Form fields become form controls
- Sections become form panels
- Validation rules are embedded

---

## CRM Integration (Salesforce)

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                          AEM Author/Publish                      │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐ │
│  │Contact Form │───▶│  Workflow   │───▶│  CRM Integration    │ │
│  │  Component  │    │  Process    │    │  Service            │ │
│  └─────────────┘    └─────────────┘    └──────────┬──────────┘ │
└──────────────────────────────────────────────────┼──────────────┘
                                                    │
                                                    ▼
                                          ┌─────────────────┐
                                          │   Salesforce    │
                                          │   REST API      │
                                          └─────────────────┘
```

### Salesforce OAuth Configuration

```java
package com.example.aem.bmad.services;

import org.osgi.service.metatype.annotations.*;

@ObjectClassDefinition(
    name = "BMAD Salesforce Configuration",
    description = "Configuration for Salesforce CRM integration"
)
public @interface SalesforceConfig {

    @AttributeDefinition(
        name = "Login URL",
        description = "Salesforce OAuth login URL"
    )
    String loginUrl() default "https://login.salesforce.com";

    @AttributeDefinition(
        name = "Client ID",
        description = "Connected App Consumer Key"
    )
    String clientId();

    @AttributeDefinition(
        name = "Client Secret",
        description = "Connected App Consumer Secret (use Cloud Manager secret variables)"
    )
    String clientSecret();

    @AttributeDefinition(
        name = "Username",
        description = "Salesforce API username"
    )
    String username();

    @AttributeDefinition(
        name = "Password",
        description = "Salesforce password + security token"
    )
    String password();

    @AttributeDefinition(
        name = "API Version",
        description = "Salesforce API version"
    )
    String apiVersion() default "v58.0";

    @AttributeDefinition(
        name = "Token Cache TTL",
        description = "OAuth token cache time in seconds"
    )
    int tokenCacheTtl() default 3600;
}
```

### Salesforce Integration Service

```java
package com.example.aem.bmad.services;

import com.example.aem.bmad.models.Lead;
import com.example.aem.bmad.models.SalesforceResponse;

public interface SalesforceService {

    /**
     * Authenticate and obtain access token
     */
    String getAccessToken();

    /**
     * Create a new lead in Salesforce
     */
    SalesforceResponse createLead(Lead lead);

    /**
     * Update an existing lead
     */
    SalesforceResponse updateLead(String leadId, Lead lead);

    /**
     * Query leads using SOQL
     */
    SalesforceResponse queryLeads(String soqlQuery);

    /**
     * Check connection health
     */
    boolean isHealthy();
}
```

### Salesforce Service Implementation

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.models.Lead;
import com.example.aem.bmad.models.SalesforceResponse;
import com.example.aem.bmad.services.HttpClientService;
import com.example.aem.bmad.services.SalesforceConfig;
import com.example.aem.bmad.services.SalesforceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = SalesforceService.class, immediate = true)
@Designate(ocd = SalesforceConfig.class)
public class SalesforceServiceImpl implements SalesforceService {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private HttpClientService httpClient;

    private SalesforceConfig config;
    private final Map<String, TokenCache> tokenCache = new ConcurrentHashMap<>();

    @Activate
    @Modified
    protected void activate(SalesforceConfig config) {
        this.config = config;
        LOG.info("Salesforce service configured for: {}", config.loginUrl());
    }

    @Override
    public String getAccessToken() {
        var cached = tokenCache.get("access_token");
        if (cached != null && !cached.isExpired()) {
            return cached.getToken();
        }

        try {
            var tokenUrl = config.loginUrl() + "/services/oauth2/token";

            var body = String.format(
                "grant_type=password&client_id=%s&client_secret=%s&username=%s&password=%s",
                URLEncoder.encode(config.clientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.clientSecret(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.username(), StandardCharsets.UTF_8),
                URLEncoder.encode(config.password(), StandardCharsets.UTF_8)
            );

            var headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");

            var response = httpClient.post(tokenUrl, body, headers);

            if (response.isSuccess()) {
                var json = MAPPER.readTree(response.getBody());
                var accessToken = json.get("access_token").asText();
                var instanceUrl = json.get("instance_url").asText();

                // Cache both tokens
                tokenCache.put("access_token", new TokenCache(accessToken, config.tokenCacheTtl()));
                tokenCache.put("instance_url", new TokenCache(instanceUrl, config.tokenCacheTtl()));

                LOG.info("Successfully obtained Salesforce access token");
                return accessToken;
            } else {
                LOG.error("Failed to obtain Salesforce token: {}", response.getBody());
                throw new RuntimeException("OAuth authentication failed");
            }
        } catch (Exception e) {
            LOG.error("Error authenticating with Salesforce", e);
            throw new RuntimeException("Salesforce authentication error", e);
        }
    }

    @Override
    public SalesforceResponse createLead(Lead lead) {
        try {
            var accessToken = getAccessToken();
            var instanceUrl = tokenCache.get("instance_url").getToken();
            var url = instanceUrl + "/services/data/" + config.apiVersion() + "/sobjects/Lead";

            var headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + accessToken);
            headers.put("Content-Type", "application/json");

            var leadJson = MAPPER.writeValueAsString(lead.toSalesforceFormat());
            var response = httpClient.post(url, leadJson, headers);

            if (response.isSuccess()) {
                var json = MAPPER.readTree(response.getBody());
                LOG.info("Lead created in Salesforce: {}", json.get("id").asText());
                return SalesforceResponse.success(json.get("id").asText());
            } else {
                LOG.error("Failed to create lead: {}", response.getBody());
                return SalesforceResponse.error(response.getBody());
            }
        } catch (Exception e) {
            LOG.error("Error creating Salesforce lead", e);
            return SalesforceResponse.error(e.getMessage());
        }
    }

    @Override
    public SalesforceResponse updateLead(String leadId, Lead lead) {
        try {
            var accessToken = getAccessToken();
            var instanceUrl = tokenCache.get("instance_url").getToken();
            var url = instanceUrl + "/services/data/" + config.apiVersion() + "/sobjects/Lead/" + leadId;

            var headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + accessToken);
            headers.put("Content-Type", "application/json");

            var leadJson = MAPPER.writeValueAsString(lead.toSalesforceFormat());
            var response = httpClient.put(url, leadJson, headers);

            if (response.getStatusCode() == 204) {
                LOG.info("Lead updated in Salesforce: {}", leadId);
                return SalesforceResponse.success(leadId);
            } else {
                LOG.error("Failed to update lead: {}", response.getBody());
                return SalesforceResponse.error(response.getBody());
            }
        } catch (Exception e) {
            LOG.error("Error updating Salesforce lead", e);
            return SalesforceResponse.error(e.getMessage());
        }
    }

    @Override
    public SalesforceResponse queryLeads(String soqlQuery) {
        try {
            var accessToken = getAccessToken();
            var instanceUrl = tokenCache.get("instance_url").getToken();
            var url = instanceUrl + "/services/data/" + config.apiVersion() + "/query?q=" +
                URLEncoder.encode(soqlQuery, StandardCharsets.UTF_8);

            var headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + accessToken);

            var response = httpClient.get(url, headers);

            if (response.isSuccess()) {
                return SalesforceResponse.success(response.getBody());
            } else {
                return SalesforceResponse.error(response.getBody());
            }
        } catch (Exception e) {
            LOG.error("Error querying Salesforce", e);
            return SalesforceResponse.error(e.getMessage());
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            getAccessToken();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Token cache helper class
    private static class TokenCache {
        private final String token;
        private final long expiresAt;

        TokenCache(String token, int ttlSeconds) {
            this.token = token;
            this.expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000L);
        }

        String getToken() {
            return token;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
```

### Lead Model

```java
package com.example.aem.bmad.models;

import java.util.HashMap;
import java.util.Map;

public class Lead {

    private String firstName;
    private String lastName;
    private String email;
    private String company;
    private String phone;
    private String leadSource;
    private String description;

    // Getters and setters...

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLeadSource() { return leadSource; }
    public void setLeadSource(String leadSource) { this.leadSource = leadSource; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Convert to Salesforce API format
     */
    public Map<String, Object> toSalesforceFormat() {
        var map = new HashMap<String, Object>();
        map.put("FirstName", firstName);
        map.put("LastName", lastName);
        map.put("Email", email);
        map.put("Company", company);
        map.put("Phone", phone);
        map.put("LeadSource", leadSource != null ? leadSource : "Web");
        map.put("Description", description);
        return map;
    }

    /**
     * Create Lead from form submission
     */
    public static Lead fromFormData(Map<String, String[]> params) {
        var lead = new Lead();
        lead.setFirstName(getParam(params, "firstName"));
        lead.setLastName(getParam(params, "lastName"));
        lead.setEmail(getParam(params, "email"));
        lead.setCompany(getParam(params, "company"));
        lead.setPhone(getParam(params, "phone"));
        lead.setLeadSource("AEM Website");
        lead.setDescription(getParam(params, "message"));
        return lead;
    }

    private static String getParam(Map<String, String[]> params, String key) {
        var values = params.get(key);
        return values != null && values.length > 0 ? values[0] : null;
    }
}
```

### Workflow Process Step for CRM Integration

```java
package com.example.aem.bmad.workflow;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.example.aem.bmad.models.Lead;
import com.example.aem.bmad.services.SalesforceService;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = WorkflowProcess.class,
    property = {"process.label=BMAD: Submit Lead to Salesforce"}
)
public class SalesforceLeadWorkflowProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(SalesforceLeadWorkflowProcess.class);

    @Reference
    private SalesforceService salesforceService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        var payloadPath = workItem.getWorkflowData().getPayload().toString();
        LOG.info("Processing CRM submission for: {}", payloadPath);

        try (var resolver = resolverFactory.getServiceResourceResolver(null)) {
            var resource = resolver.getResource(payloadPath);
            if (resource == null) {
                throw new WorkflowException("Payload resource not found: " + payloadPath);
            }

            var valueMap = resource.getValueMap();

            var lead = new Lead();
            lead.setFirstName(valueMap.get("firstName", String.class));
            lead.setLastName(valueMap.get("lastName", String.class));
            lead.setEmail(valueMap.get("email", String.class));
            lead.setCompany(valueMap.get("company", String.class));
            lead.setPhone(valueMap.get("phone", String.class));
            lead.setLeadSource("AEM Website Form");
            lead.setDescription(valueMap.get("message", String.class));

            var result = salesforceService.createLead(lead);

            if (result.isSuccess()) {
                LOG.info("Lead submitted to Salesforce: {}", result.getId());

                // Store Salesforce ID back to AEM
                var modifiableMap = resource.adaptTo(org.apache.sling.api.resource.ModifiableValueMap.class);
                if (modifiableMap != null) {
                    modifiableMap.put("salesforceId", result.getId());
                    resolver.commit();
                }
            } else {
                LOG.error("Failed to submit lead: {}", result.getError());
                throw new WorkflowException("Salesforce submission failed: " + result.getError());
            }
        } catch (Exception e) {
            LOG.error("Error in Salesforce workflow process", e);
            throw new WorkflowException("CRM integration error", e);
        }
    }
}
```

---

## Adobe Analytics Integration

### Data Layer Implementation

```java
package com.example.aem.bmad.models;

import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import java.util.Calendar;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = PageDataLayer.class,
    resourceType = "bmad/components/page"
)
public class PageDataLayer {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue(name = "jcr:title")
    @Default(values = "")
    private String title;

    @ValueMapValue(name = "jcr:description")
    @Default(values = "")
    private String description;

    @ValueMapValue(name = "cq:tags")
    @Default(values = {})
    private String[] tags;

    @ValueMapValue(name = "jcr:created")
    private Calendar created;

    @ValueMapValue(name = "jcr:lastModified")
    private Calendar lastModified;

    /**
     * Get the data layer JSON for the page
     */
    public ComponentData getDataLayer() {
        return DataLayerBuilder.forComponent()
            .withId(resource.getPath())
            .withType("bmad/components/page")
            .withTitle(title)
            .withDescription(description)
            .withLastModifiedDate(lastModified != null ? lastModified.getTime() : null)
            .build();
    }

    /**
     * Get custom analytics properties
     */
    public AnalyticsProperties getAnalyticsProperties() {
        return new AnalyticsProperties(
            title,
            resource.getPath(),
            tags,
            request.getRequestURI()
        );
    }

    public static class AnalyticsProperties {
        private final String pageTitle;
        private final String pagePath;
        private final String[] pageTags;
        private final String pageUrl;

        public AnalyticsProperties(String pageTitle, String pagePath, String[] pageTags, String pageUrl) {
            this.pageTitle = pageTitle;
            this.pagePath = pagePath;
            this.pageTags = pageTags;
            this.pageUrl = pageUrl;
        }

        public String getPageTitle() { return pageTitle; }
        public String getPagePath() { return pagePath; }
        public String[] getPageTags() { return pageTags; }
        public String getPageUrl() { return pageUrl; }
    }
}
```

### Analytics Event Tracking Service

```java
package com.example.aem.bmad.services;

import java.util.Map;

public interface AnalyticsService {

    /**
     * Track a page view event
     */
    void trackPageView(String pagePath, Map<String, Object> properties);

    /**
     * Track a custom event
     */
    void trackEvent(String eventName, Map<String, Object> properties);

    /**
     * Track a form submission
     */
    void trackFormSubmission(String formName, Map<String, Object> properties);

    /**
     * Track a component interaction
     */
    void trackComponentInteraction(String componentType, String action, Map<String, Object> properties);
}
```

### Client-Side Analytics Integration (HTL + JS)

```html
<!-- analytics-datalayer.html -->
<sly data-sly-use.page="com.example.aem.bmad.models.PageDataLayer">
    <script type="application/json" id="aem-datalayer">
        {
            "page": {
                "@type": "bmad/components/page",
                "dc:title": "${page.analyticsProperties.pageTitle @ context='scriptString'}",
                "repo:path": "${page.analyticsProperties.pagePath @ context='scriptString'}",
                "xdm:URL": "${page.analyticsProperties.pageUrl @ context='scriptString'}",
                "xdm:tags": ${page.analyticsProperties.pageTags @ context='unsafe'}
            }
        }
    </script>
</sly>

<script>
    // Initialize Adobe Data Layer
    window.adobeDataLayer = window.adobeDataLayer || [];

    // Push page data
    window.adobeDataLayer.push(function(dl) {
        var dataLayerElement = document.getElementById('aem-datalayer');
        if (dataLayerElement) {
            var pageData = JSON.parse(dataLayerElement.textContent);
            dl.push({
                event: 'page-loaded',
                page: pageData.page
            });
        }
    });

    // Track component clicks
    document.addEventListener('click', function(e) {
        var component = e.target.closest('[data-cmp-data-layer]');
        if (component) {
            var componentData = JSON.parse(component.getAttribute('data-cmp-data-layer'));
            window.adobeDataLayer.push({
                event: 'component-click',
                component: componentData
            });
        }
    });
</script>
```

---

## Translation Service Integration

### Translation Service Interface

```java
package com.example.aem.bmad.services;

import java.util.List;
import java.util.Map;

public interface TranslationService {

    /**
     * Translate a single text string
     */
    TranslationResult translate(String text, String sourceLanguage, String targetLanguage);

    /**
     * Batch translate multiple strings
     */
    List<TranslationResult> translateBatch(List<String> texts, String sourceLanguage, String targetLanguage);

    /**
     * Get supported languages
     */
    Map<String, String> getSupportedLanguages();

    /**
     * Detect language of text
     */
    String detectLanguage(String text);
}
```

### Translation Service Implementation (Google Translate Example)

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.HttpClientService;
import com.example.aem.bmad.services.TranslationResult;
import com.example.aem.bmad.services.TranslationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = TranslationService.class, immediate = true)
@Designate(ocd = GoogleTranslationServiceImpl.Config.class)
public class GoogleTranslationServiceImpl implements TranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleTranslationServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ObjectClassDefinition(name = "BMAD Google Translation Configuration")
    public @interface Config {

        @AttributeDefinition(name = "API Key", description = "Google Cloud Translation API key")
        String apiKey();

        @AttributeDefinition(name = "API Endpoint", description = "Google Translation API endpoint")
        String apiEndpoint() default "https://translation.googleapis.com/language/translate/v2";

        @AttributeDefinition(name = "Default Source Language", description = "Default source language code")
        String defaultSourceLanguage() default "en";
    }

    @Reference
    private HttpClientService httpClient;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("Google Translation service configured");
    }

    @Override
    public TranslationResult translate(String text, String sourceLanguage, String targetLanguage) {
        try {
            var url = String.format(
                "%s?key=%s&q=%s&source=%s&target=%s",
                config.apiEndpoint(),
                config.apiKey(),
                URLEncoder.encode(text, StandardCharsets.UTF_8),
                sourceLanguage,
                targetLanguage
            );

            var response = httpClient.get(url, new HashMap<>());

            if (response.isSuccess()) {
                var json = MAPPER.readTree(response.getBody());
                var translations = json.path("data").path("translations");

                if (translations.isArray() && translations.size() > 0) {
                    var translatedText = translations.get(0).path("translatedText").asText();
                    return TranslationResult.success(translatedText, targetLanguage);
                }
            }

            LOG.error("Translation failed: {}", response.getBody());
            return TranslationResult.error("Translation failed");

        } catch (Exception e) {
            LOG.error("Error calling translation API", e);
            return TranslationResult.error(e.getMessage());
        }
    }

    @Override
    public List<TranslationResult> translateBatch(List<String> texts, String sourceLanguage, String targetLanguage) {
        // For batch translation, use the batch endpoint or make parallel calls
        return texts.stream()
            .map(text -> translate(text, sourceLanguage, targetLanguage))
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getSupportedLanguages() {
        try {
            var url = config.apiEndpoint().replace("/translate/v2", "/languages")
                + "?key=" + config.apiKey() + "&target=en";

            var response = httpClient.get(url, new HashMap<>());

            if (response.isSuccess()) {
                var json = MAPPER.readTree(response.getBody());
                var languages = json.path("data").path("languages");

                var result = new HashMap<String, String>();
                for (JsonNode lang : languages) {
                    result.put(
                        lang.path("language").asText(),
                        lang.path("name").asText()
                    );
                }
                return result;
            }
        } catch (Exception e) {
            LOG.error("Error fetching supported languages", e);
        }
        return Collections.emptyMap();
    }

    @Override
    public String detectLanguage(String text) {
        try {
            var url = config.apiEndpoint().replace("/translate/v2", "/detect")
                + "?key=" + config.apiKey()
                + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            var response = httpClient.get(url, new HashMap<>());

            if (response.isSuccess()) {
                var json = MAPPER.readTree(response.getBody());
                return json.path("data").path("detections")
                    .get(0).get(0).path("language").asText();
            }
        } catch (Exception e) {
            LOG.error("Error detecting language", e);
        }
        return config.defaultSourceLanguage();
    }
}
```

### AEM Translation Framework Connector

```java
package com.example.aem.bmad.translation;

import com.adobe.granite.translation.api.*;
import com.adobe.granite.translation.core.common.AbstractTranslationService;
import com.example.aem.bmad.services.TranslationService;
import org.osgi.service.component.annotations.*;

@Component(
    service = com.adobe.granite.translation.api.TranslationService.class,
    property = {
        "label=BMAD Translation Connector",
        "service.ranking:Integer=100"
    }
)
public class BmadTranslationConnector extends AbstractTranslationService {

    @Reference
    private TranslationService translationService;

    @Override
    public TranslationResult translateString(String sourceString,
                                              String sourceLanguage,
                                              String targetLanguage,
                                              TranslationConstants.ContentType contentType,
                                              String contentCategory) throws TranslationException {

        var result = translationService.translate(sourceString, sourceLanguage, targetLanguage);

        if (result.isSuccess()) {
            return new TranslationResult(
                result.getTranslatedText(),
                sourceLanguage,
                targetLanguage,
                TranslationResultQuality.MACHINE,
                null
            );
        } else {
            throw new TranslationException("Translation failed: " + result.getError());
        }
    }

    @Override
    public String detectLanguage(String source, TranslationConstants.ContentType contentType) {
        return translationService.detectLanguage(source);
    }

    @Override
    public boolean isDirectionSupported(String sourceLanguage, String targetLanguage) {
        var supported = translationService.getSupportedLanguages();
        return supported.containsKey(sourceLanguage) && supported.containsKey(targetLanguage);
    }

    // ... other required methods
}
```

---

## Commerce Integration

### Product Service Interface

```java
package com.example.aem.bmad.services;

import com.example.aem.bmad.models.Product;
import com.example.aem.bmad.models.Category;

import java.util.List;
import java.util.Optional;

public interface CommerceService {

    /**
     * Get product by SKU
     */
    Optional<Product> getProductBySku(String sku);

    /**
     * Search products
     */
    List<Product> searchProducts(String query, int page, int pageSize);

    /**
     * Get products by category
     */
    List<Product> getProductsByCategory(String categoryId, int page, int pageSize);

    /**
     * Get category tree
     */
    List<Category> getCategoryTree();

    /**
     * Get product inventory
     */
    int getInventory(String sku);

    /**
     * Get product price (may vary by customer segment)
     */
    ProductPrice getPrice(String sku, String customerGroup);
}
```

---

## Email Service Integration

### Email Service Interface

```java
package com.example.aem.bmad.services;

import java.util.Map;

public interface EmailService {

    /**
     * Send a simple email
     */
    EmailResult sendEmail(String to, String subject, String body);

    /**
     * Send a templated email using AEM template
     */
    EmailResult sendTemplatedEmail(String to, String templatePath, Map<String, Object> variables);

    /**
     * Send email with attachments
     */
    EmailResult sendEmailWithAttachments(String to, String subject, String body, Map<String, byte[]> attachments);
}
```

### Email Service Implementation (SendGrid)

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.EmailResult;
import com.example.aem.bmad.services.EmailService;
import com.example.aem.bmad.services.HttpClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = EmailService.class, immediate = true)
@Designate(ocd = SendGridEmailServiceImpl.Config.class)
public class SendGridEmailServiceImpl implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(SendGridEmailServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SENDGRID_API = "https://api.sendgrid.com/v3/mail/send";

    @ObjectClassDefinition(name = "BMAD SendGrid Email Configuration")
    public @interface Config {

        @AttributeDefinition(name = "API Key", description = "SendGrid API key")
        String apiKey();

        @AttributeDefinition(name = "From Email", description = "Default sender email")
        String fromEmail() default "noreply@example.com";

        @AttributeDefinition(name = "From Name", description = "Default sender name")
        String fromName() default "BMAD Website";
    }

    @Reference
    private HttpClientService httpClient;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("SendGrid email service configured");
    }

    @Override
    public EmailResult sendEmail(String to, String subject, String body) {
        try {
            var payload = buildEmailPayload(to, subject, body);
            var headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + config.apiKey());
            headers.put("Content-Type", "application/json");

            var response = httpClient.post(SENDGRID_API, payload, headers);

            if (response.getStatusCode() == 202) {
                LOG.info("Email sent successfully to: {}", to);
                return EmailResult.success();
            } else {
                LOG.error("Email send failed: {}", response.getBody());
                return EmailResult.error(response.getBody());
            }
        } catch (Exception e) {
            LOG.error("Error sending email", e);
            return EmailResult.error(e.getMessage());
        }
    }

    @Override
    public EmailResult sendTemplatedEmail(String to, String templatePath, Map<String, Object> variables) {
        // Template rendering would integrate with AEM's template engine
        // For now, this is a placeholder
        throw new UnsupportedOperationException("Template email not yet implemented");
    }

    @Override
    public EmailResult sendEmailWithAttachments(String to, String subject, String body, Map<String, byte[]> attachments) {
        // Implementation with attachment handling
        throw new UnsupportedOperationException("Email attachments not yet implemented");
    }

    private String buildEmailPayload(String to, String subject, String body) throws Exception {
        var email = new HashMap<String, Object>();

        // From
        var from = new HashMap<String, String>();
        from.put("email", config.fromEmail());
        from.put("name", config.fromName());
        email.put("from", from);

        // To
        var toList = new ArrayList<Map<String, Object>>();
        var toEntry = new HashMap<String, Object>();
        toEntry.put("to", List.of(Map.of("email", to)));
        toList.add(toEntry);
        email.put("personalizations", toList);

        // Subject and content
        email.put("subject", subject);
        email.put("content", List.of(
            Map.of("type", "text/html", "value", body)
        ));

        return MAPPER.writeValueAsString(email);
    }
}
```

---

## Traceability

| Spec ID | Integration | Description | Status |
|---------|-------------|-------------|--------|
| INT-EXT-001 | Salesforce CRM | Lead capture and sync | Documented |
| INT-EXT-002 | Adobe Analytics | Data layer and tracking | Documented |
| INT-EXT-003 | Translation API | Multi-language support | Documented |
| INT-EXT-004 | Commerce Backend | Product catalog sync | Documented |
| INT-EXT-005 | Email Service | Transactional emails | Documented |
| INT-EXT-006 | Workflow Process | CRM workflow step | Documented |
