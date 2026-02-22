# AEM Headless & GraphQL Integration

This document provides comprehensive guidance on implementing headless content delivery using AEM as a Cloud Service. It covers Content Fragments, GraphQL APIs, and SPA integration patterns.

## Table of Contents

1. [Headless Architecture Overview](#headless-architecture-overview)
2. [Content Fragment Models](#content-fragment-models)
3. [GraphQL API](#graphql-api)
4. [Persisted Queries](#persisted-queries)
5. [SPA Integration Patterns](#spa-integration-patterns)
6. [Remote SPA Editor](#remote-spa-editor)
7. [Hybrid Architecture](#hybrid-architecture)
8. [Caching Strategies](#caching-strategies)

---

## Headless Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           AEM as a Cloud Service                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────┐ │
│  │   Content Fragment  │    │    Content Fragment │    │     Assets      │ │
│  │       Models        │───▶│       Editor        │───▶│   (DAM)         │ │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────┘ │
│            │                           │                        │           │
│            ▼                           ▼                        ▼           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        GraphQL Endpoint                              │   │
│  │                   /content/cq:graphql/global/endpoint               │   │
│  └───────────────────────────────────┬─────────────────────────────────┘   │
│                                      │                                      │
└──────────────────────────────────────┼──────────────────────────────────────┘
                                       │
        ┌──────────────────────────────┼──────────────────────────────────┐
        │                              │                                   │
        ▼                              ▼                                   ▼
┌───────────────────┐    ┌───────────────────────┐    ┌───────────────────────┐
│    React SPA      │    │      Next.js SSR      │    │    Mobile App         │
│    (Client)       │    │       (Server)        │    │    (Native)           │
└───────────────────┘    └───────────────────────┘    └───────────────────────┘
```

### Key Concepts

1. **Content Fragments**: Structured, channel-agnostic content that can be delivered via APIs
2. **Content Fragment Models**: Schema definitions that define the structure of Content Fragments
3. **GraphQL API**: Query language for retrieving Content Fragment data
4. **Persisted Queries**: Pre-defined GraphQL queries stored in AEM for better caching and performance

---

## Content Fragment Models

### Model Definition Best Practices

Content Fragment Models define the schema for your content. They should be:
- **Reusable**: Design models that can serve multiple use cases
- **Nested**: Use fragment references for complex relationships
- **Versioned**: Plan for model evolution

### Example: Article Content Fragment Model

```
Article Model
├── Title (Single-line text) - Required
├── Slug (Single-line text) - Required, Unique
├── Summary (Multi-line text)
├── Body (Multi-line text, Rich text)
├── Featured Image (Content Reference - Assets)
├── Author (Fragment Reference - Author Model)
├── Category (Enumeration)
├── Tags (Tags)
├── Publish Date (Date and Time)
├── Related Articles (Fragment Reference - Article Model, Multi-value)
└── SEO
    ├── Meta Title (Single-line text)
    ├── Meta Description (Multi-line text)
    └── OG Image (Content Reference - Assets)
```

### Creating Models Programmatically

```java
package com.example.aem.bmad.models;

import com.adobe.cq.contentfragment.model.ContentFragmentModel;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;

import javax.annotation.PostConstruct;

/**
 * Sling Model for validating Content Fragment structure
 */
@Model(
    adaptables = Resource.class,
    adapters = ArticleFragment.class,
    resourceType = "dam/cfm/models/article"
)
public class ArticleFragment {

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String slug;

    @ValueMapValue
    private String summary;

    @ValueMapValue
    private String body;

    @ValueMapValue(name = "featuredImage")
    private String featuredImagePath;

    @ValueMapValue
    private String[] tags;

    @ValueMapValue
    private String publishDate;

    @Self
    private Resource resource;

    @PostConstruct
    protected void init() {
        // Perform any initialization or validation
        if (slug == null || slug.isEmpty()) {
            slug = title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        }
    }

    // Getters
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getSummary() { return summary; }
    public String getBody() { return body; }
    public String getFeaturedImagePath() { return featuredImagePath; }
    public String[] getTags() { return tags; }
    public String getPublishDate() { return publishDate; }
}
```

---

## GraphQL API

### GraphQL Endpoint Configuration

AEM provides GraphQL endpoints at:
- **Global**: `/content/cq:graphql/global/endpoint.json`
- **Site-specific**: `/content/cq:graphql/{site}/endpoint.json`

### Schema Exploration

Use the GraphiQL IDE at:
```
https://<author-host>/content/cq:graphql/global/endpoint.json?explorer
```

### Query Examples

#### Basic Query - List Articles

```graphql
query ArticleList($first: Int = 10, $offset: Int = 0) {
  articleList(
    first: $first
    offset: $offset
    sort: "publishDate DESC"
  ) {
    items {
      _path
      title
      slug
      summary
      publishDate
      featuredImage {
        ... on ImageRef {
          _path
          _dynamicUrl
          width
          height
        }
      }
      author {
        ... on AuthorModel {
          name
          avatar {
            ... on ImageRef {
              _dynamicUrl
            }
          }
        }
      }
    }
    _references {
      ... on ImageRef {
        _path
        _dynamicUrl
      }
    }
  }
}
```

#### Single Item Query - Article by Slug

```graphql
query ArticleBySlug($slug: String!) {
  articleList(
    filter: { slug: { _expressions: [{ value: $slug }] } }
  ) {
    items {
      _path
      _metadata {
        stringMetadata {
          name
          value
        }
      }
      title
      slug
      summary
      body {
        html
        plaintext
        markdown
        json
      }
      featuredImage {
        ... on ImageRef {
          _path
          _dynamicUrl
          width
          height
          mimeType
        }
      }
      author {
        ... on AuthorModel {
          name
          bio {
            html
          }
          avatar {
            ... on ImageRef {
              _dynamicUrl
            }
          }
          socialLinks {
            platform
            url
          }
        }
      }
      relatedArticles {
        ... on ArticleModel {
          _path
          title
          slug
          summary
        }
      }
      seo {
        metaTitle
        metaDescription
        ogImage {
          ... on ImageRef {
            _dynamicUrl
          }
        }
      }
    }
  }
}
```

#### Filtered Query with Pagination

```graphql
query ArticlesByCategory(
  $category: String!
  $first: Int = 10
  $offset: Int = 0
) {
  articleList(
    filter: {
      category: { _expressions: [{ value: $category }] }
      publishDate: { _expressions: [{ value: "2024-01-01", _operator: GREATER_EQUAL }] }
    }
    sort: "publishDate DESC"
    first: $first
    offset: $offset
  ) {
    items {
      _path
      title
      slug
      summary
      category
      publishDate
    }
  }
}
```

#### Full-Text Search Query

```graphql
query SearchArticles($searchTerm: String!, $first: Int = 20) {
  articleList(
    filter: {
      _logOp: OR
      title: { _expressions: [{ value: $searchTerm, _operator: CONTAINS }] }
      body: { _expressions: [{ value: $searchTerm, _operator: CONTAINS }] }
      summary: { _expressions: [{ value: $searchTerm, _operator: CONTAINS }] }
    }
    first: $first
  ) {
    items {
      _path
      title
      slug
      summary
    }
  }
}
```

---

## Persisted Queries

### Why Use Persisted Queries?

1. **Performance**: Cached at CDN layer
2. **Security**: No arbitrary queries from clients
3. **Optimization**: Pre-validated and optimized
4. **Consistency**: Same query used across clients

### Creating Persisted Queries

Persisted queries are stored under `/conf/<project>/settings/graphql/persistentQueries/`

#### Structure
```
/conf/bmad/settings/graphql/persistentQueries/
├── article-list
├── article-by-slug
├── articles-by-category
└── search-articles
```

### Persisted Query Definition

```json
{
  "query": "query ArticleList($first: Int = 10, $offset: Int = 0) { articleList(first: $first, offset: $offset, sort: \"publishDate DESC\") { items { _path title slug summary publishDate featuredImage { ... on ImageRef { _dynamicUrl } } } } }",
  "variables": {
    "first": 10,
    "offset": 0
  }
}
```

### Accessing Persisted Queries

```
GET /graphql/execute.json/bmad/article-list
GET /graphql/execute.json/bmad/article-list;first=5;offset=10
GET /graphql/execute.json/bmad/article-by-slug;slug=my-article
```

### Java Client for Persisted Queries

```java
package com.example.aem.bmad.services;

import java.util.Map;

public interface GraphQLClient {

    /**
     * Execute a persisted query
     */
    <T> T executePersistedQuery(String queryPath, Map<String, Object> variables, Class<T> responseType);

    /**
     * Execute a persisted query with cache control
     */
    <T> T executePersistedQuery(String queryPath, Map<String, Object> variables,
                                 Class<T> responseType, CacheControl cacheControl);

    /**
     * Execute raw GraphQL query (author only)
     */
    <T> T executeQuery(String query, Map<String, Object> variables, Class<T> responseType);
}
```

### GraphQL Client Implementation

```java
package com.example.aem.bmad.services.impl;

import com.example.aem.bmad.services.GraphQLClient;
import com.example.aem.bmad.services.HttpClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component(service = GraphQLClient.class, immediate = true)
@Designate(ocd = GraphQLClientImpl.Config.class)
public class GraphQLClientImpl implements GraphQLClient {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLClientImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ObjectClassDefinition(name = "BMAD GraphQL Client Configuration")
    public @interface Config {

        @AttributeDefinition(name = "AEM Host", description = "AEM publish host URL")
        String aemHost() default "https://publish-p12345-e12345.adobeaemcloud.com";

        @AttributeDefinition(name = "Endpoint Path", description = "GraphQL endpoint path")
        String endpointPath() default "/graphql/execute.json";

        @AttributeDefinition(name = "Default Cache TTL", description = "Default cache TTL in seconds")
        int defaultCacheTtl() default 300;
    }

    @Reference
    private HttpClientService httpClient;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("GraphQL client configured for: {}", config.aemHost());
    }

    @Override
    public <T> T executePersistedQuery(String queryPath, Map<String, Object> variables, Class<T> responseType) {
        return executePersistedQuery(queryPath, variables, responseType, null);
    }

    @Override
    public <T> T executePersistedQuery(String queryPath, Map<String, Object> variables,
                                        Class<T> responseType, CacheControl cacheControl) {
        try {
            // Build URL with variables as path parameters
            StringBuilder urlBuilder = new StringBuilder(config.aemHost())
                .append(config.endpointPath())
                .append("/")
                .append(queryPath);

            if (variables != null && !variables.isEmpty()) {
                String params = variables.entrySet().stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(
                        String.valueOf(e.getValue()), StandardCharsets.UTF_8))
                    .collect(Collectors.joining(";", ";", ""));
                urlBuilder.append(params);
            }

            String url = urlBuilder.toString();

            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");

            if (cacheControl != null) {
                headers.put("Cache-Control", cacheControl.toString());
            }

            var response = httpClient.get(url, headers);

            if (response.isSuccess()) {
                JsonNode root = MAPPER.readTree(response.getBody());
                JsonNode data = root.path("data");
                return MAPPER.treeToValue(data, responseType);
            } else {
                LOG.error("GraphQL query failed: {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("GraphQL query failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            LOG.error("Error executing persisted query: {}", queryPath, e);
            throw new RuntimeException("GraphQL execution failed", e);
        }
    }

    @Override
    public <T> T executeQuery(String query, Map<String, Object> variables, Class<T> responseType) {
        try {
            String url = config.aemHost() + "/content/cq:graphql/global/endpoint.json";

            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            if (variables != null) {
                body.put("variables", variables);
            }

            String jsonBody = MAPPER.writeValueAsString(body);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");

            var response = httpClient.post(url, jsonBody, headers);

            if (response.isSuccess()) {
                JsonNode root = MAPPER.readTree(response.getBody());

                // Check for GraphQL errors
                if (root.has("errors")) {
                    LOG.error("GraphQL errors: {}", root.get("errors"));
                    throw new RuntimeException("GraphQL query returned errors");
                }

                JsonNode data = root.path("data");
                return MAPPER.treeToValue(data, responseType);
            } else {
                throw new RuntimeException("GraphQL query failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            LOG.error("Error executing GraphQL query", e);
            throw new RuntimeException("GraphQL execution failed", e);
        }
    }

    public static class CacheControl {
        private final int maxAge;
        private final boolean noCache;
        private final boolean noStore;

        private CacheControl(int maxAge, boolean noCache, boolean noStore) {
            this.maxAge = maxAge;
            this.noCache = noCache;
            this.noStore = noStore;
        }

        public static CacheControl maxAge(int seconds) {
            return new CacheControl(seconds, false, false);
        }

        public static CacheControl noCache() {
            return new CacheControl(0, true, false);
        }

        public static CacheControl noStore() {
            return new CacheControl(0, false, true);
        }

        @Override
        public String toString() {
            if (noStore) return "no-store";
            if (noCache) return "no-cache";
            return "max-age=" + maxAge;
        }
    }
}
```

---

## SPA Integration Patterns

### React Integration with AEM Headless

```typescript
// lib/aem-client.ts
import { AEMHeadless } from '@adobe/aem-headless-client-js';

const aemClient = new AEMHeadless({
  serviceURL: process.env.NEXT_PUBLIC_AEM_HOST,
  endpoint: '/content/cq:graphql/bmad/endpoint.json',
  auth: process.env.AEM_AUTH_TOKEN ? `Bearer ${process.env.AEM_AUTH_TOKEN}` : undefined,
});

export async function getArticles(first: number = 10, offset: number = 0) {
  const response = await aemClient.runPersistedQuery(
    'bmad/article-list',
    { first, offset }
  );
  return response.data?.articleList?.items || [];
}

export async function getArticleBySlug(slug: string) {
  const response = await aemClient.runPersistedQuery(
    'bmad/article-by-slug',
    { slug }
  );
  const items = response.data?.articleList?.items;
  return items && items.length > 0 ? items[0] : null;
}

export async function searchArticles(searchTerm: string) {
  const response = await aemClient.runPersistedQuery(
    'bmad/search-articles',
    { searchTerm }
  );
  return response.data?.articleList?.items || [];
}
```

### Next.js Integration

```typescript
// app/articles/page.tsx
import { getArticles } from '@/lib/aem-client';
import ArticleCard from '@/components/ArticleCard';

export const revalidate = 300; // ISR: Revalidate every 5 minutes

export default async function ArticlesPage() {
  const articles = await getArticles(20);

  return (
    <main>
      <h1>Articles</h1>
      <div className="article-grid">
        {articles.map((article) => (
          <ArticleCard key={article._path} article={article} />
        ))}
      </div>
    </main>
  );
}
```

```typescript
// app/articles/[slug]/page.tsx
import { getArticleBySlug, getArticles } from '@/lib/aem-client';
import { notFound } from 'next/navigation';

export async function generateStaticParams() {
  const articles = await getArticles(100);
  return articles.map((article) => ({
    slug: article.slug,
  }));
}

export default async function ArticlePage({
  params,
}: {
  params: { slug: string };
}) {
  const article = await getArticleBySlug(params.slug);

  if (!article) {
    notFound();
  }

  return (
    <article>
      <h1>{article.title}</h1>
      {article.featuredImage && (
        <img
          src={article.featuredImage._dynamicUrl}
          alt={article.title}
        />
      )}
      <div dangerouslySetInnerHTML={{ __html: article.body.html }} />
    </article>
  );
}
```

### React SPA with AEM SPA Editor Support

```typescript
// components/ArticleComponent.tsx
import { MapTo, withMappable } from '@adobe/aem-react-editable-components';

interface ArticleProps {
  title: string;
  summary: string;
  body: {
    html: string;
  };
  featuredImage?: {
    _dynamicUrl: string;
  };
  cqPath?: string;
  isInEditor?: boolean;
}

const ArticleComponent: React.FC<ArticleProps> = ({
  title,
  summary,
  body,
  featuredImage,
  isInEditor,
}) => {
  return (
    <article className="article-component">
      <h2>{title}</h2>
      {featuredImage && (
        <img src={featuredImage._dynamicUrl} alt={title} />
      )}
      <p className="summary">{summary}</p>
      <div
        className="body"
        dangerouslySetInnerHTML={{ __html: body.html }}
      />
    </article>
  );
};

export default MapTo('bmad/components/article')(
  withMappable(ArticleComponent, {
    emptyLabel: 'Article',
    isEmpty: (props) => !props.title,
  })
);
```

---

## Remote SPA Editor

### Configuration for Remote SPAs

```json
// AEM configuration at /conf/bmad/settings/spa-hosting
{
  "jcr:primaryType": "cq:Page",
  "jcr:content": {
    "jcr:primaryType": "nt:unstructured",
    "sling:resourceType": "spa/configuration",
    "remoteSPAUrl": "https://my-spa.vercel.app",
    "allowedOrigins": [
      "https://my-spa.vercel.app",
      "http://localhost:3000"
    ]
  }
}
```

### CORS Configuration for Remote SPA

```java
package com.example.aem.bmad.config;

import org.osgi.service.metatype.annotations.*;

@ObjectClassDefinition(name = "BMAD CORS Configuration for Remote SPA")
public @interface RemoteSpaCorsConfig {

    @AttributeDefinition(
        name = "Allowed Origins",
        description = "Origins allowed to access AEM APIs"
    )
    String[] allowedOrigins() default {
        "https://my-spa.vercel.app",
        "http://localhost:3000"
    };

    @AttributeDefinition(
        name = "Allowed Methods",
        description = "HTTP methods allowed"
    )
    String[] allowedMethods() default {"GET", "POST", "OPTIONS"};

    @AttributeDefinition(
        name = "Allowed Headers",
        description = "Headers allowed in requests"
    )
    String[] allowedHeaders() default {"Authorization", "Content-Type", "Accept"};

    @AttributeDefinition(
        name = "Exposed Headers",
        description = "Headers exposed to client"
    )
    String[] exposedHeaders() default {"Content-Length"};

    @AttributeDefinition(
        name = "Max Age",
        description = "Preflight cache duration in seconds"
    )
    long maxAge() default 86400;
}
```

---

## Hybrid Architecture

### Combining Headless + Traditional AEM

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          AEM as a Cloud Service                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────┐       ┌─────────────────────────────────┐ │
│  │   Traditional Pages    │       │      Content Fragments          │ │
│  │   (Experience Fragments,│       │   (Headless Content)            │ │
│  │    Editable Templates) │       │                                 │ │
│  └───────────┬─────────────┘       └───────────────┬─────────────────┘ │
│              │                                     │                   │
│              ▼                                     ▼                   │
│  ┌─────────────────────────┐       ┌─────────────────────────────────┐ │
│  │    HTML Rendering      │       │      GraphQL API               │ │
│  │    (HTL + Sling)       │       │                                 │ │
│  └───────────┬─────────────┘       └───────────────┬─────────────────┘ │
│              │                                     │                   │
└──────────────┼─────────────────────────────────────┼───────────────────┘
               │                                     │
               ▼                                     ▼
        ┌────────────┐                      ┌────────────────┐
        │   Web      │                      │   Mobile App   │
        │  Browser   │                      │   React SPA    │
        └────────────┘                      └────────────────┘
```

### Model Exporter for Hybrid Delivery

```java
package com.example.aem.bmad.models;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.*;
import org.apache.sling.models.annotations.injectorspecific.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { HeroModel.class, ComponentExporter.class },
    resourceType = "bmad/components/hero",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(
    name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
    extensions = ExporterConstants.SLING_MODEL_EXTENSION
)
public class HeroModel implements ComponentExporter {

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String subheading;

    @ValueMapValue
    private String backgroundImage;

    @ValueMapValue
    private String ctaText;

    @ValueMapValue
    private String ctaLink;

    @JsonProperty("heading")
    public String getHeading() { return heading; }

    @JsonProperty("subheading")
    public String getSubheading() { return subheading; }

    @JsonProperty("backgroundImage")
    public String getBackgroundImage() { return backgroundImage; }

    @JsonProperty("ctaText")
    public String getCtaText() { return ctaText; }

    @JsonProperty("ctaLink")
    public String getCtaLink() { return ctaLink; }

    @Override
    public String getExportedType() {
        return "bmad/components/hero";
    }
}
```

---

## Caching Strategies

### CDN Cache Headers for GraphQL

```java
package com.example.aem.bmad.filters;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;

import javax.servlet.*;
import java.io.IOException;

@Component(
    service = Filter.class,
    property = {
        "sling.filter.scope=REQUEST",
        "sling.filter.pattern=/graphql/execute.json/.*",
        "service.ranking:Integer=100"
    }
)
@Designate(ocd = GraphQLCacheFilter.Config.class)
public class GraphQLCacheFilter implements Filter {

    @ObjectClassDefinition(name = "BMAD GraphQL Cache Filter Configuration")
    public @interface Config {

        @AttributeDefinition(name = "Cache TTL", description = "Cache TTL in seconds")
        int cacheTtl() default 300;

        @AttributeDefinition(name = "Stale While Revalidate", description = "SWR duration in seconds")
        int staleWhileRevalidate() default 60;
    }

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof SlingHttpServletResponse) {
            SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

            // Set cache control headers for CDN
            String cacheControl = String.format(
                "public, max-age=%d, stale-while-revalidate=%d",
                config.cacheTtl(),
                config.staleWhileRevalidate()
            );

            slingResponse.setHeader("Cache-Control", cacheControl);
            slingResponse.setHeader("Surrogate-Control", "max-age=" + config.cacheTtl());

            // Add Vary header for proper cache key
            slingResponse.setHeader("Vary", "Accept-Encoding");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
```

### Client-Side Caching with SWR

```typescript
// hooks/useArticles.ts
import useSWR from 'swr';
import { getArticles, getArticleBySlug } from '@/lib/aem-client';

export function useArticles(first: number = 10, offset: number = 0) {
  return useSWR(
    ['articles', first, offset],
    () => getArticles(first, offset),
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      dedupingInterval: 60000, // 1 minute
    }
  );
}

export function useArticle(slug: string) {
  return useSWR(
    slug ? ['article', slug] : null,
    () => getArticleBySlug(slug),
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      dedupingInterval: 300000, // 5 minutes
    }
  );
}
```

### Incremental Static Regeneration Pattern

```typescript
// Next.js ISR configuration
export const revalidate = 300; // Revalidate at most every 5 minutes

export async function generateStaticParams() {
  // Pre-render the most recent 50 articles at build time
  const articles = await getArticles(50);
  return articles.map((article) => ({
    slug: article.slug,
  }));
}

// For pages not pre-rendered, generate on-demand and cache
export const dynamicParams = true;
```

---

## Traceability

| Spec ID | Feature | Description | Status |
|---------|---------|-------------|--------|
| INT-HL-001 | Content Fragment Models | Schema definitions for headless content | Documented |
| INT-HL-002 | GraphQL API | Query language and endpoint configuration | Documented |
| INT-HL-003 | Persisted Queries | Pre-defined, cached GraphQL queries | Documented |
| INT-HL-004 | React/Next.js Integration | Frontend framework integration patterns | Documented |
| INT-HL-005 | Remote SPA Editor | Editing support for remote SPAs | Documented |
| INT-HL-006 | Hybrid Architecture | Combined headless + traditional delivery | Documented |
| INT-HL-007 | CDN Caching | Cache headers and strategies | Documented |
| INT-HL-008 | GraphQL Client | Java service for executing GraphQL | Documented |
