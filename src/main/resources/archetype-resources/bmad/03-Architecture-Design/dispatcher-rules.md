# Dispatcher Rules

This document outlines the configuration for the AEM Dispatcher, which is responsible for caching, load balancing, and security.

## Caching Strategy

- **Cache Everything**: The default strategy is to cache all content and then invalidate it when it changes.
- **Cache Headers**: Use standard `Cache-Control` headers to control the caching behavior in the browser and CDN.
- **Time-to-Live (TTL)**: Set a reasonable TTL for cached content to ensure that it is eventually re-validated.

## Filter Rules

The following filter rules are applied to restrict access to sensitive paths:

```
/filter {
    /0001 { /type "deny"  /glob "*" }
    /0002 { /type "allow" /url "/content/*" }
    /0003 { /type "allow" /url "/etc/clientlibs/*" }
}
```

## Rewrite Rules

The following rewrite rules are used to implement user-friendly URLs:

```
/rewrite {
    /0001 {
        /type "rewrite"
        /url "^/products/(.*)$"
        /into "/content/aem-bmad-showcase/us/en/products/$1"
    }
}
```

## Invalidation

- **Auto-invalidation**: The dispatcher is configured to automatically invalidate cached content when a page is published or unpublished.
- **Flush Agents**: Flush agents are configured on the AEM author and publish instances to send invalidation requests to the dispatcher.
