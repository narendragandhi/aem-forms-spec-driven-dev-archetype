# Security Hardening Guide

This document covers security best practices for AEM Forms projects, including CSRF protection, CAPTCHA integration, and XSS prevention.

## Table of Contents

1. [CSRF Protection](#csrf-protection)
2. [CAPTCHA Integration](#captcha-integration)
3. [XSS Prevention](#xss-prevention)
4. [Form Submission Security](#form-submission-security)
5. [FDM Security](#fdm-security)

---

## CSRF Protection

### Overview

AEM provides CSRF token protection for forms. Tokens are generated on page load and validated on submission.

### Configuration

```xml
<!-- /apps/{appName}/config/org.apache.sling.security.impl.ReferrerFilter.any -->
{
    "allow.hosts": [
        "example.com",
        "*.adobe.com"
    ],
    "allow.hosts.regexp": [],
    "empty.referrer.ok": false,
    "non.ui.trails.allowed": [
        "/bin/cq/security/validate"
    ]
}
```

### Form Implementation

```html
<!-- Form include CSRF token -->
<form action="${formSubmitUrl}" method="POST">
    <input type="hidden" name="_csrf" value="${csrfToken}" />
    <!-- Form fields -->
</form>
```

### Custom CSRF Filter

```java
package com.example.aem.bmad.security;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.*;
import org.apache.sling.api.servlets.*;
import org.apache.sling.security.impl.*;

@Component
public class CustomCSRFFilter implements HttpFilter {
    
    @Override
    public void doFilter(SlingHttpServletRequest request, 
                         SlingHttpServletResponse response, 
                         Chain filterChain) {
        
        String token = request.getParameter("_csrf");
        String expectedToken = request.getResourceResolver()
            .getAttribute("com.adobegranite.csrf.impl.CSRFToken");
        
        if (token == null || !token.equals(expectedToken)) {
            response.setStatus(403);
            response.getWriter().write("CSRF validation failed");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

## CAPTCHA Integration

### Adobe Acrobat Sign CAPTCHA

For forms requiring Adobe Acrobat Sign:

```javascript
// React component with reCAPTCHA
import { useReCAPTCHA } from '@adobe/aem-forms-af-react-components';

const FormWithCaptcha = () => {
    const { execute, reset } = useReCAPTCHA({
        siteKey: '6LdXXX...'
    });
    
    const handleSubmit = async (data) => {
        const token = await execute();
        
        await fetch('/bin/bmad/headless-submit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ...data, recaptchaToken: token })
        });
    };
    
    return (
        <form onSubmit={handleSubmit}>
            {/* Form fields */}
        </form>
    );
};
```

### Custom CAPTCHA Implementation

```java
package com.example.aem.bmad.services;

import org.osgi.service.component.annotations.*;

@Component
public class CaptchaService {
    
    public boolean verify(String token) {
        // Verify with Google reCAPTCHA API
        String url = "https://www.google.com/recaptcha/api/siteverify";
        // ... verification logic
        return result.isSuccess();
    }
}
```

### Form Rule for CAPTCHA

```javascript
// Rule: Validate CAPTCHA before submission
when: submit
do: if (!verifyRecaptcha()) {
    showError("Please complete the CAPTCHA");
    cancel submit;
}
```

---

## XSS Prevention

### Rich Text Editor (RTE) XSS

AEM Forms RTE allows HTML input. Sanitize to prevent XSS:

```java
package com.example.aem.bmad.security;

import org.apache.sling.xss.XSSAPI;

@Service
public class XssPreventionService {
    
    @Reference
    private XSSAPI xssAPI;
    
    public String sanitizeHtml(String html) {
        // Allow specific tags
        return xssAPI.encodeForHTML(html);
    }
    
    public String sanitizeForAttribute(String input) {
        return xssAPI.encodeForHTMLAttr(input);
    }
}
```

### RTE Configuration

```xml
<!-- /apps/{appName}/config/editor/tinyMCE.xml -->
<rte jcr:primaryType="nt:unstructured">
    <tools jcr:primaryType="nt:unstructured" 
           name="format"
           value="bold italic underline strike-through"/>
    <plugins jcr:primaryType="nt:unstructured">
        <plugin name="lists"/>
        <plugin name="link"/>
    </plugins>
    <validElements>
        "p,br,strong/b,em/i,u,ul,ol,li,a[href|title]"
    </validElements>
</rte>
```

### React XSS Prevention

```javascript
import DOMPurify from 'dompurify';

const SafeHTML = ({ html }) => {
    const sanitized = DOMPurify.sanitize(html, {
        ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'ul', 'ol', 'li', 'a'],
        ALLOWED_ATTR: ['href', 'title']
    });
    
    return <div dangerouslySetInnerHTML={{ __html: sanitized }} />;
};
```

---

## Form Submission Security

### Validate Input

```java
package com.example.aem.bmad.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.xss.XSSAPI;

@Component
@Service(Servlet.class)
public class SecureSubmitServlet extends SlingAllMethodsServlet {
    
    @Reference
    private XSSAPI xssAPI;
    
    @Override
    protected void doPost(SlingHttpServletRequest request, 
                          SlingHttpServletResponse response) {
        
        // Get and sanitize input
        String firstName = request.getParameter("firstName");
        String email = request.getParameter("email");
        
        // Validate
        if (!isValidEmail(email)) {
            response.setStatus(400);
            return;
        }
        
        // Sanitize for storage
        String safeFirstName = xssAPI.encodeForHTML(firstName);
        String safeEmail = xssAPI.encodeForHTML(email);
        
        // Process...
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@.+$");
    }
}
```

### Rate Limiting

```java
package com.example.aem.bmad.security;

import java.util.concurrent.*;
import java.util.Map;

@Component
public class RateLimitService {
    
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    
    public boolean isAllowed(String clientId) {
        long now = System.currentTimeMillis();
        long lastTime = lastRequestTime.getOrDefault(clientId, 0L);
        
        if (now - lastTime > 60000) {
            requestCounts.put(clientId, 0);
            lastRequestTime.put(clientId, now);
        }
        
        int count = requestCounts.getOrDefault(clientId, 0);
        if (count >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        
        requestCounts.put(clientId, count + 1);
        return true;
    }
}
```

---

## FDM Security

### Secure FDM Queries

```java
package com.example.aem.bmad.services;

import org.apache.sling.xss.XSSAPI;

@Service
public class SecureFdmService {
    
    @Reference
    private XSSAPI xssAPI;
    
    public List<Customer> queryCustomers(String searchTerm) {
        // NEVER use string concatenation for queries
        // Use parameterized queries
        QueryBuilder queryBuilder = getQueryBuilder();
        
        // This is SAFE
        Query query = queryBuilder.createQuery(PredicateGroup.create(
            PredicateFactory.create("jcr:title", searchTerm)
        ));
        
        // NOT this (vulnerable):
        // String sql = "SELECT * FROM customers WHERE name = '" + searchTerm + "'";
        
        return query.getResult().getNodes().stream()
            .map(this::mapToCustomer)
            .collect(Collectors.toList());
    }
}
```

### FDM Input Validation

```xml
<!-- FDM entity validation -->
<entity name="customer" query="SELECT * FROM customer">
    <field name="email" 
           validate="^[A-Za-z0-9+_.-]+@.+$" 
           required="true"/>
    <field name="phone" 
           validate="^\+?[0-9]{10,15}$"
           required="false"/>
</entity>
```

---

## Security Headers

### Dispatcher Configuration

```
# /dispatcher/src/conf.d/headers/headers.any

# X-Frame-Options
Header always set X-Frame-Options "SAMEORIGIN"

# X-Content-Type-Options
Header always set X-Content-Type-Options "nosniff"

# X-XSS-Protection
Header always set X-XSS-Protection "1; mode=block"

# Content Security Policy
Header always set Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';"

# Referrer-Policy
Header always set Referrer-Policy "strict-origin-when-cross-origin"

# Permissions-Policy
Header always set Permissions-Policy "geolocation=(), microphone=(), camera=()"
```

---

## Security Checklist

- [ ] Enable CSRF protection on all forms
- [ ] Implement CAPTCHA for public-facing forms
- [ ] Sanitize RTE HTML input/output
- [ ] Validate all form inputs
- [ ] Use parameterized queries for FDM
- [ ] Configure security headers in Dispatcher
- [ ] Enable rate limiting on submission endpoints
- [ ] Log security events
- [ ] Regular security audits
- [ ] Keep AEM Forms patches up to date
