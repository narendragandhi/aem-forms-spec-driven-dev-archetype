# Environment Configuration Guide

## Overview

This document provides comprehensive guidance for configuring the AEM BMAD Showcase application across different environments (Development, Stage, Production). It covers environment-specific settings, secrets management, configuration promotion, and environment parity practices.

---

## Table of Contents

1. [Environment Overview](#1-environment-overview)
2. [Configuration Hierarchy](#2-configuration-hierarchy)
3. [OSGi Configurations](#3-osgi-configurations)
4. [Environment Variables](#4-environment-variables)
5. [Secrets Management](#5-secrets-management)
6. [Dispatcher Configuration](#6-dispatcher-configuration)
7. [Cloud Manager Configuration](#7-cloud-manager-configuration)
8. [Configuration Promotion](#8-configuration-promotion)

---

## 1. Environment Overview

### 1.1 Environment Matrix

| Environment | Purpose | Access | Data |
|-------------|---------|--------|------|
| **Local** | Developer workstation | Individual | Sample/Mock |
| **Development** | Integration testing | Team | Synthetic |
| **Stage** | UAT & Performance | Stakeholders | Production-like |
| **Production** | Live site | Public | Real |

### 1.2 Environment URLs

| Environment | Author | Publish | Dispatcher |
|-------------|--------|---------|------------|
| Local | `localhost:4502` | `localhost:4503` | `localhost:8080` |
| Development | `author-dev-p{id}.adobeaemcloud.com` | `publish-dev-p{id}.adobeaemcloud.com` | `dev.example.com` |
| Stage | `author-stage-p{id}.adobeaemcloud.com` | `publish-stage-p{id}.adobeaemcloud.com` | `stage.example.com` |
| Production | `author-p{id}.adobeaemcloud.com` | `publish-p{id}.adobeaemcloud.com` | `www.example.com` |

### 1.3 Environment Specifications

| Resource | Local | Development | Stage | Production |
|----------|-------|-------------|-------|------------|
| Author Memory | 4 GB | 8 GB | 16 GB | 32 GB |
| Publish Memory | 4 GB | 8 GB | 16 GB | 32 GB |
| Publish Instances | 1 | 1 | 2 | 4+ |
| Dispatcher Nodes | 1 | 1 | 2 | 4+ |
| CDN | None | None | Enabled | Enabled |

---

## 2. Configuration Hierarchy

### 2.1 Configuration Layers

```
┌─────────────────────────────────────────────────────────┐
│ Layer 5: Runtime Overrides (JVM Properties)            │
├─────────────────────────────────────────────────────────┤
│ Layer 4: Environment Variables                         │
├─────────────────────────────────────────────────────────┤
│ Layer 3: Cloud Manager Secrets                         │
├─────────────────────────────────────────────────────────┤
│ Layer 2: OSGi Config (runmode-specific)                │
├─────────────────────────────────────────────────────────┤
│ Layer 1: OSGi Config (default)                         │
└─────────────────────────────────────────────────────────┘
         ▲ Higher layers override lower layers
```

### 2.2 Configuration File Structure

```
ui.config/
└── src/main/content/jcr_root/apps/bmad-showcase/osgiconfig/
    ├── config/                          # All environments
    │   └── com.example.ServiceA.cfg.json
    ├── config.author/                   # Author only
    │   └── com.example.AuthorService.cfg.json
    ├── config.publish/                  # Publish only
    │   └── com.example.PublishService.cfg.json
    ├── config.dev/                      # Development
    │   └── com.example.ServiceA.cfg.json
    ├── config.stage/                    # Stage
    │   └── com.example.ServiceA.cfg.json
    └── config.prod/                     # Production
        └── com.example.ServiceA.cfg.json
```

### 2.3 Runmode Hierarchy

| Runmode | Applies To | Priority |
|---------|------------|----------|
| `config` | All environments | 1 (lowest) |
| `config.author` | Author instances | 2 |
| `config.publish` | Publish instances | 2 |
| `config.dev` | Development | 3 |
| `config.stage` | Stage | 3 |
| `config.prod` | Production | 3 |
| `config.author.dev` | Author + Development | 4 (highest) |

---

## 3. OSGi Configurations

### 3.1 LLM Service Configuration

**Base Configuration (config/):**
```json
// com.example.aem.bmad.core.services.impl.LLMServiceImpl.cfg.json
{
    "enabled": true,
    "defaultProvider": "openai",
    "maxTokens": 2000,
    "timeoutSeconds": 30,
    "retryAttempts": 3,
    "retryDelayMs": 1000
}
```

**Development Override (config.dev/):**
```json
{
    "enabled": true,
    "defaultProvider": "openai",
    "maxTokens": 500,
    "timeoutSeconds": 60,
    "rateLimitPerMinute": 10,
    "debugMode": true
}
```

**Production Override (config.prod/):**
```json
{
    "enabled": true,
    "defaultProvider": "openai",
    "maxTokens": 4000,
    "timeoutSeconds": 30,
    "rateLimitPerMinute": 100,
    "debugMode": false,
    "apiKey": "$[secret:LLM_API_KEY]"
}
```

### 3.2 Email Service Configuration

**Base Configuration:**
```json
// com.example.aem.bmad.core.services.impl.EmailServiceImpl.cfg.json
{
    "enabled": false,
    "smtpHost": "localhost",
    "smtpPort": 25,
    "fromAddress": "noreply@example.com"
}
```

**Production Override:**
```json
{
    "enabled": true,
    "smtpHost": "$[env:SENDGRID_HOST;default=smtp.sendgrid.net]",
    "smtpPort": 587,
    "smtpUsername": "$[secret:SENDGRID_USERNAME]",
    "smtpPassword": "$[secret:SENDGRID_PASSWORD]",
    "fromAddress": "noreply@example.com",
    "tlsEnabled": true
}
```

### 3.3 Logging Configuration

**Development (verbose logging):**
```json
// org.apache.sling.commons.log.LogManager.factory.config~bmad-app.cfg.json
{
    "org.apache.sling.commons.log.file": "logs/bmad-application.log",
    "org.apache.sling.commons.log.level": "debug",
    "org.apache.sling.commons.log.names": [
        "com.example.aem.bmad"
    ],
    "org.apache.sling.commons.log.pattern": "{0,date,yyyy-MM-dd HH:mm:ss.SSS} *{4}* [{2}] {3} {5}"
}
```

**Production (info level):**
```json
{
    "org.apache.sling.commons.log.file": "logs/bmad-application.log",
    "org.apache.sling.commons.log.level": "info",
    "org.apache.sling.commons.log.names": [
        "com.example.aem.bmad"
    ]
}
```

### 3.4 Replication Agents

**Author to Publish (Production):**
```json
// com.day.cq.replication.impl.AgentManagerImpl~publish.cfg.json
{
    "agentId": "publish",
    "jcr:title": "Publish Agent",
    "enabled": true,
    "transportUri": "$[env:PUBLISH_URL]/bin/receive?sling:authRequestLogin=1",
    "transportUser": "$[secret:REPLICATION_USER]",
    "transportPassword": "$[secret:REPLICATION_PASSWORD]",
    "retryDelay": "60000",
    "queueBatchMode": true
}
```

---

## 4. Environment Variables

### 4.1 Variable Naming Convention

```
# Format: <SERVICE>_<COMPONENT>_<PROPERTY>
LLM_OPENAI_ENDPOINT=https://api.openai.com/v1
LLM_OPENAI_MODEL=gpt-4
SENDGRID_HOST=smtp.sendgrid.net
CDN_PURGE_ENDPOINT=https://api.fastly.com/purge
```

### 4.2 Environment Variable Reference

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `AEM_RUNMODE` | Active runmodes | `author,dev` | Yes |
| `LLM_PROVIDER` | LLM service provider | `openai` | No |
| `LLM_OPENAI_ENDPOINT` | OpenAI API endpoint | `https://api.openai.com/v1` | No |
| `LLM_OPENAI_MODEL` | OpenAI model name | `gpt-4` | No |
| `SENDGRID_HOST` | SendGrid SMTP host | `smtp.sendgrid.net` | No |
| `CDN_ENABLED` | Enable CDN integration | `false` | No |
| `CDN_PURGE_ENDPOINT` | CDN cache purge URL | - | If CDN enabled |
| `ANALYTICS_ID` | Analytics tracking ID | - | No |

### 4.3 Cloud Manager Environment Variables

**Setting via Cloud Manager UI:**
1. Navigate to Program > Environments > [Environment]
2. Select "Environment Variables" tab
3. Add/Edit variables
4. Variables are injected at container startup

**Setting via Cloud Manager API:**
```bash
# List current variables
aio cloudmanager:environment:get-variables <environment-id> --programId <program-id>

# Set variable
aio cloudmanager:environment:set-variables <environment-id> \
  --programId <program-id> \
  --variable LLM_PROVIDER openai \
  --variable CDN_ENABLED true
```

---

## 5. Secrets Management

### 5.1 Secret Types

| Type | Storage | Access | Examples |
|------|---------|--------|----------|
| **API Keys** | Cloud Manager | Runtime only | LLM API key, SendGrid key |
| **Credentials** | Cloud Manager | Runtime only | Service account passwords |
| **Certificates** | AEM Trust Store | Runtime | SSL certs, signing certs |
| **Tokens** | Cloud Manager | Runtime only | OAuth tokens |

### 5.2 Cloud Manager Secrets

**Creating Secrets:**
```bash
# Set secret (masked in logs/UI)
aio cloudmanager:environment:set-variables <environment-id> \
  --programId <program-id> \
  --secretString LLM_API_KEY sk-xxxxxxxxxxxxx \
  --secretString SENDGRID_API_KEY SG.xxxxxxxxxxxxx
```

**Referencing in OSGi Config:**
```json
{
    "apiKey": "$[secret:LLM_API_KEY]",
    "password": "$[secret:SERVICE_PASSWORD]"
}
```

### 5.3 Secret Rotation Procedure

**Step 1: Generate New Secret**
- Create new API key/credentials in provider console
- Do not revoke old credentials yet

**Step 2: Update Cloud Manager**
```bash
aio cloudmanager:environment:set-variables <environment-id> \
  --programId <program-id> \
  --secretString LLM_API_KEY <new-key>
```

**Step 3: Deploy to Apply**
- Trigger deployment or restart pods
- Verify new credentials work

**Step 4: Revoke Old Secret**
- After verification, revoke old credentials
- Document rotation in change log

### 5.4 AEM Trust Store Management

**Importing Certificates:**
```bash
# Via cURL
curl -u admin:admin -F "file=@certificate.crt" \
  http://localhost:4502/libs/granite/security/post/truststore.html
```

**Trust Store Location:**
- Author: `/etc/truststore`
- Accessible via: Tools > Security > Trust Store

---

## 6. Dispatcher Configuration

### 6.1 Environment-Specific Dispatcher Config

**Directory Structure:**
```
dispatcher/
└── src/
    ├── conf.d/
    │   ├── available_vhosts/
    │   │   ├── default.vhost           # Template
    │   │   ├── dev.vhost               # Development
    │   │   ├── stage.vhost             # Stage
    │   │   └── prod.vhost              # Production
    │   ├── enabled_vhosts/             # Symlinks
    │   ├── rewrites/
    │   └── variables/
    │       ├── custom.vars             # Default variables
    │       └── global.vars             # Global variables
    └── conf.dispatcher.d/
        ├── available_farms/
        │   └── default.farm
        ├── cache/
        ├── clientheaders/
        ├── filters/
        └── renders/
```

### 6.2 Environment Variables in Dispatcher

**conf.d/variables/custom.vars:**
```apache
# Environment-specific variables (set via Cloud Manager)
Define PUBLISH_HOST "${PUBLISH_HOST}"
Define CDN_HOST "${CDN_HOST}"
Define ALLOWED_ORIGINS "${ALLOWED_ORIGINS}"
```

**Usage in vhost:**
```apache
<VirtualHost *:80>
    ServerName ${CDN_HOST}

    # CORS headers
    Header set Access-Control-Allow-Origin "${ALLOWED_ORIGINS}"

    # Backend proxy
    ProxyPass /content http://${PUBLISH_HOST}:4503/content
</VirtualHost>
```

### 6.3 Cache Configuration by Environment

**Development (minimal caching):**
```apache
/cache {
    /docroot "/mnt/var/www/html"
    /statfileslevel "2"
    /allowAuthorized "1"
    /rules {
        /0000 { /type "deny" /glob "*" }
        /0001 { /type "allow" /glob "*.css" }
        /0002 { /type "allow" /glob "*.js" }
    }
    /invalidate {
        /0000 { /type "allow" /glob "*" }
    }
    /enableTTL "1"
}
```

**Production (aggressive caching):**
```apache
/cache {
    /docroot "/mnt/var/www/html"
    /statfileslevel "4"
    /allowAuthorized "0"
    /rules {
        /0000 { /type "deny" /glob "*" }
        /0001 { /type "allow" /glob "/content/*" }
        /0002 { /type "allow" /glob "/etc.clientlibs/*" }
        /0003 { /type "allow" /glob "*.html" }
        /0004 { /type "allow" /glob "*.css" }
        /0005 { /type "allow" /glob "*.js" }
        /0006 { /type "allow" /glob "*.json" }
        /0007 { /type "allow" /glob "*.woff2" }
    }
    /invalidate {
        /0000 { /type "allow" /glob "*.html" }
    }
    /enableTTL "1"
    /gracePeriod "300"
}
```

---

## 7. Cloud Manager Configuration

### 7.1 Pipeline Configuration

**Build Pipeline (.cloudmanager/maven/settings.xml):**
```xml
<settings>
    <profiles>
        <profile>
            <id>cloudmanager</id>
            <repositories>
                <repository>
                    <id>adobe-public</id>
                    <url>https://repo.adobe.com/nexus/content/groups/public</url>
                </repository>
            </repositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>cloudmanager</activeProfile>
    </activeProfiles>
</settings>
```

### 7.2 Pipeline Environment Variables

| Variable | Stage | Production | Description |
|----------|-------|------------|-------------|
| `CM_BUILD_PHASE` | true | true | Current build phase |
| `CM_PROGRAM_ID` | auto | auto | Program identifier |
| `CM_ENVIRONMENT_ID` | auto | auto | Environment identifier |
| `MAVEN_OPTS` | `-Xmx2048m` | `-Xmx2048m` | Maven JVM options |

### 7.3 Quality Gate Configuration

**Code Quality Rules (.cloudmanager/sonar/rules.xml):**
```xml
<rules>
    <rule>
        <key>squid:S1192</key>
        <priority>MINOR</priority>
    </rule>
    <rule>
        <key>CQRules:CQBP-71</key>
        <priority>CRITICAL</priority>
    </rule>
</rules>
```

---

## 8. Configuration Promotion

### 8.1 Promotion Workflow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    Local    │────▶│ Development │────▶│    Stage    │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ Production  │
                                        └─────────────┘
```

### 8.2 Configuration Diff Checklist

**Before Promoting to Stage:**
- [ ] All OSGi configs validated
- [ ] No development-only settings (debugMode, etc.)
- [ ] Secrets configured in Stage Cloud Manager
- [ ] Dispatcher rules appropriate for Stage

**Before Promoting to Production:**
- [ ] Stage testing complete
- [ ] Performance validated
- [ ] Secrets configured in Production Cloud Manager
- [ ] Monitoring dashboards ready
- [ ] Rollback plan documented

### 8.3 Configuration Validation

**Local Validation Script:**
```bash
#!/bin/bash
# validate-config.sh

ENV=$1
CONFIG_DIR="ui.config/src/main/content/jcr_root/apps/bmad-showcase/osgiconfig"

echo "Validating configuration for environment: $ENV"

# Check for development-only settings
if [ "$ENV" == "prod" ]; then
    if grep -r "debugMode.*true" "$CONFIG_DIR/config.prod/"; then
        echo "ERROR: debugMode enabled in production config"
        exit 1
    fi
fi

# Validate JSON syntax
find "$CONFIG_DIR" -name "*.cfg.json" -exec python -m json.tool {} \; > /dev/null

# Check for unresolved placeholders
if grep -r '\$\[env:' "$CONFIG_DIR/config.$ENV/" | grep -v "default="; then
    echo "WARNING: Environment variables without defaults found"
fi

echo "Configuration validation passed"
```

### 8.4 Environment Parity Best Practices

| Practice | Description |
|----------|-------------|
| **Infrastructure as Code** | All config in version control |
| **Immutable Deployments** | Same artifact across environments |
| **Feature Flags** | Control features without config changes |
| **Configuration Templating** | Use placeholders over hardcoded values |
| **Automated Validation** | CI checks for config consistency |

---

## Appendix A: Configuration Quick Reference

**OSGi Placeholder Syntax:**
```
$[env:VARIABLE_NAME]              # Environment variable (required)
$[env:VARIABLE_NAME;default=val]  # With default value
$[secret:SECRET_NAME]             # Cloud Manager secret
$[prop:property.name]             # System property
```

**Common Runmodes:**
- `author`, `publish` - Instance type
- `dev`, `stage`, `prod` - Environment
- `nosamplecontent` - Skip sample content

---

## Appendix B: Troubleshooting

**Config Not Applied:**
1. Check runmode matches: `/system/console/status-slingsettings`
2. Verify file location and naming
3. Check OSGi console: `/system/console/configMgr`

**Secret Not Resolved:**
1. Verify secret exists in Cloud Manager
2. Check secret name spelling
3. Redeploy after adding secrets

**Environment Variable Not Available:**
1. Check Cloud Manager variable configuration
2. Verify variable name case sensitivity
3. Restart environment after changes

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | Platform Team | Initial version |

**Review Cycle:** Quarterly
**Next Review:** [Current Date + 3 months]
