# AI-Powered Translation Service

This guide covers implementing AI-powered automatic translation for AEM content using Large Language Models. The approach provides high-quality, context-aware translations that outperform traditional machine translation for marketing content.

## Overview

Traditional translation workflows involve:
1. Export content to XLIFF/TMX
2. Send to translation vendor
3. Wait for human translation
4. Import back to AEM
5. Review and publish

AI-powered translation enables:
- **Instant translation** of pages and components
- **Context preservation** by translating full pages in single requests
- **Change detection** to re-translate only modified content
- **Live Copy integration** for MSM-based multilingual sites

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Translation Workflow                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                │
│  │   Source    │───▶│  Property   │───▶│   Content   │                │
│  │    Page     │    │  Extractor  │    │  Aggregator │                │
│  └─────────────┘    └─────────────┘    └──────┬──────┘                │
│                                               │                        │
│                                               ▼                        │
│                                    ┌─────────────────┐                 │
│                                    │   LLM Service   │                 │
│                                    │  (Translation)  │                 │
│                                    └────────┬────────┘                 │
│                                             │                          │
│                                             ▼                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                │
│  │   Target    │◀───│  Property   │◀───│  Response   │                │
│  │    Page     │    │   Writer    │    │   Parser    │                │
│  └─────────────┘    └─────────────┘    └─────────────┘                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## Change Tracking Strategy

To avoid re-translating unchanged content and detect manual edits, we store metadata:

| Property | Description |
|----------|-------------|
| `ai_original_{prop}` | Hash of source content at translation time |
| `ai_translated_{prop}` | Hash of AI-generated translation |
| `ai_translation_date` | Timestamp of last translation |
| `ai_translation_provider` | Which LLM provider was used |

### Change Detection Logic

```java
public enum TranslationStatus {
    UP_TO_DATE,           // Source unchanged, translation matches
    SOURCE_CHANGED,       // Source changed, needs re-translation
    MANUALLY_EDITED,      // Translation was manually modified
    NEVER_TRANSLATED      // No translation metadata exists
}

public TranslationStatus getStatus(Resource resource, String property) {
    ValueMap props = resource.getValueMap();

    String currentValue = props.get(property, String.class);
    String originalHash = props.get("ai_original_" + property, String.class);
    String translatedHash = props.get("ai_translated_" + property, String.class);

    if (originalHash == null) {
        return TranslationStatus.NEVER_TRANSLATED;
    }

    String currentHash = hash(currentValue);

    if (!currentHash.equals(translatedHash)) {
        return TranslationStatus.MANUALLY_EDITED;
    }

    // Check if source has changed (requires access to source page)
    String sourceValue = getSourcePropertyValue(resource, property);
    if (!hash(sourceValue).equals(originalHash)) {
        return TranslationStatus.SOURCE_CHANGED;
    }

    return TranslationStatus.UP_TO_DATE;
}
```

## Translation Service Interface

```java
package com.example.aem.bmad.core.services;

import org.apache.sling.api.resource.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI-powered translation service for AEM content.
 */
public interface AITranslationService {

    /**
     * Translate a single page to the target language
     *
     * @param sourcePage path to source page
     * @param targetPage path to target page (live copy or language copy)
     * @param targetLocale target language
     * @return translation result with statistics
     */
    TranslationResult translatePage(String sourcePage, String targetPage, Locale targetLocale);

    /**
     * Translate a component resource
     *
     * @param sourceComponent source component resource
     * @param targetComponent target component resource
     * @param targetLocale target language
     * @return translation result
     */
    TranslationResult translateComponent(Resource sourceComponent, Resource targetComponent, Locale targetLocale);

    /**
     * Translate a tree of pages
     *
     * @param sourceRoot root of source tree
     * @param targetRoot root of target tree
     * @param targetLocale target language
     * @param recursive include child pages
     * @return aggregated translation results
     */
    TranslationResult translateTree(String sourceRoot, String targetRoot, Locale targetLocale, boolean recursive);

    /**
     * Get translation status for a page
     *
     * @param pagePath page to check
     * @return map of property names to their translation status
     */
    Map<String, TranslationStatus> getTranslationStatus(String pagePath);

    /**
     * Translate specific text (for preview/testing)
     *
     * @param text source text
     * @param sourceLocale source language
     * @param targetLocale target language
     * @return translated text
     */
    String translateText(String text, Locale sourceLocale, Locale targetLocale);
}
```

## Translation Result Model

```java
package com.example.aem.bmad.core.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a translation operation.
 */
public class TranslationResult {

    private final boolean success;
    private final int propertiesTranslated;
    private final int propertiesSkipped;
    private final int tokensUsed;
    private final long durationMs;
    private final List<String> errors;
    private final List<String> warnings;

    private TranslationResult(Builder builder) {
        this.success = builder.success;
        this.propertiesTranslated = builder.propertiesTranslated;
        this.propertiesSkipped = builder.propertiesSkipped;
        this.tokensUsed = builder.tokensUsed;
        this.durationMs = builder.durationMs;
        this.errors = builder.errors;
        this.warnings = builder.warnings;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TranslationResult error(String error) {
        return builder().success(false).addError(error).build();
    }

    // Getters
    public boolean isSuccess() { return success; }
    public int getPropertiesTranslated() { return propertiesTranslated; }
    public int getPropertiesSkipped() { return propertiesSkipped; }
    public int getTokensUsed() { return tokensUsed; }
    public long getDurationMs() { return durationMs; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }

    public static class Builder {
        private boolean success = true;
        private int propertiesTranslated = 0;
        private int propertiesSkipped = 0;
        private int tokensUsed = 0;
        private long durationMs = 0;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder propertiesTranslated(int count) { this.propertiesTranslated = count; return this; }
        public Builder propertiesSkipped(int count) { this.propertiesSkipped = count; return this; }
        public Builder tokensUsed(int tokens) { this.tokensUsed = tokens; return this; }
        public Builder durationMs(long duration) { this.durationMs = duration; return this; }
        public Builder addError(String error) { this.errors.add(error); return this; }
        public Builder addWarning(String warning) { this.warnings.add(warning); return this; }

        public TranslationResult build() {
            return new TranslationResult(this);
        }
    }
}
```

## Implementation

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.models.LLMRequest;
import com.example.aem.bmad.core.models.LLMResponse;
import com.example.aem.bmad.core.models.TranslationResult;
import com.example.aem.bmad.core.services.AITranslationService;
import com.example.aem.bmad.core.services.LLMService;
import com.example.aem.bmad.core.services.TranslationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.*;

@Component(service = AITranslationService.class)
@Designate(ocd = AITranslationServiceImpl.Config.class)
public class AITranslationServiceImpl implements AITranslationService {

    private static final Logger LOG = LoggerFactory.getLogger(AITranslationServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Properties that typically contain translatable text
    private static final Set<String> TEXT_PROPERTIES = Set.of(
        "jcr:title", "jcr:description", "text", "title", "description",
        "alt", "linkText", "buttonText", "heading", "subheading",
        "caption", "label", "placeholder", "cta", "headline"
    );

    @ObjectClassDefinition(name = "BMAD AI Translation Configuration")
    public @interface Config {

        @AttributeDefinition(name = "LLM Provider", description = "Which LLM to use for translation")
        String llmProvider() default "openai";

        @AttributeDefinition(name = "Model", description = "Model to use for translation")
        String model() default "gpt-4o";

        @AttributeDefinition(name = "Additional Text Properties", description = "Extra property names to translate")
        String[] additionalTextProperties() default {};

        @AttributeDefinition(name = "Skip Properties", description = "Properties to never translate")
        String[] skipProperties() default {"sling:resourceType", "jcr:primaryType"};

        @AttributeDefinition(name = "Batch Size", description = "Properties per LLM request")
        int batchSize() default 20;

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;
    }

    @Reference(target = "(provider=openai)")
    private LLMService llmService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private Config config;
    private Set<String> allTextProperties;
    private Set<String> skipProperties;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;

        // Combine default and configured text properties
        allTextProperties = new HashSet<>(TEXT_PROPERTIES);
        if (config.additionalTextProperties() != null) {
            allTextProperties.addAll(Arrays.asList(config.additionalTextProperties()));
        }

        skipProperties = config.skipProperties() != null
            ? new HashSet<>(Arrays.asList(config.skipProperties()))
            : Collections.emptySet();

        LOG.info("AI Translation service configured: provider={}, model={}",
            config.llmProvider(), config.model());
    }

    @Override
    public TranslationResult translatePage(String sourcePage, String targetPage, Locale targetLocale) {
        if (!config.enabled()) {
            return TranslationResult.error("Translation service is disabled");
        }

        long startTime = System.currentTimeMillis();
        TranslationResult.Builder result = TranslationResult.builder();

        Map<String, Object> authInfo = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, "bmad-translation-service"
        );

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Resource sourceResource = resolver.getResource(sourcePage + "/jcr:content");
            Resource targetResource = resolver.getResource(targetPage + "/jcr:content");

            if (sourceResource == null || targetResource == null) {
                return TranslationResult.error("Source or target page not found");
            }

            // Extract all translatable content from source
            Map<String, String> contentToTranslate = extractTranslatableContent(sourceResource);

            if (contentToTranslate.isEmpty()) {
                return result.propertiesSkipped(0).durationMs(System.currentTimeMillis() - startTime).build();
            }

            // Translate in batches
            Map<String, String> translations = translateBatch(contentToTranslate, targetLocale, result);

            // Apply translations to target
            applyTranslations(targetResource, translations, contentToTranslate, resolver);

            resolver.commit();

            result.propertiesTranslated(translations.size());
            result.durationMs(System.currentTimeMillis() - startTime);

            LOG.info("Translated page {} to {}: {} properties in {}ms",
                sourcePage, targetLocale, translations.size(), result.build().getDurationMs());

            return result.build();

        } catch (Exception e) {
            LOG.error("Translation failed for page: {}", sourcePage, e);
            return TranslationResult.error(e.getMessage());
        }
    }

    @Override
    public TranslationResult translateComponent(Resource sourceComponent, Resource targetComponent, Locale targetLocale) {
        // Similar to translatePage but for single component
        Map<String, String> content = extractComponentContent(sourceComponent);
        TranslationResult.Builder result = TranslationResult.builder();

        Map<String, String> translations = translateBatch(content, targetLocale, result);

        try {
            applyTranslations(targetComponent, translations, content, targetComponent.getResourceResolver());
            targetComponent.getResourceResolver().commit();
            return result.propertiesTranslated(translations.size()).build();
        } catch (PersistenceException e) {
            return TranslationResult.error(e.getMessage());
        }
    }

    @Override
    public TranslationResult translateTree(String sourceRoot, String targetRoot, Locale targetLocale, boolean recursive) {
        TranslationResult.Builder aggregateResult = TranslationResult.builder();
        int totalTranslated = 0;
        int totalSkipped = 0;
        int totalTokens = 0;

        // Get all pages under source root
        List<String> sourcePages = getChildPages(sourceRoot, recursive);

        for (String sourcePage : sourcePages) {
            String relativePath = sourcePage.substring(sourceRoot.length());
            String targetPage = targetRoot + relativePath;

            TranslationResult pageResult = translatePage(sourcePage, targetPage, targetLocale);

            if (pageResult.isSuccess()) {
                totalTranslated += pageResult.getPropertiesTranslated();
                totalSkipped += pageResult.getPropertiesSkipped();
                totalTokens += pageResult.getTokensUsed();
            } else {
                pageResult.getErrors().forEach(aggregateResult::addError);
            }
        }

        return aggregateResult
            .propertiesTranslated(totalTranslated)
            .propertiesSkipped(totalSkipped)
            .tokensUsed(totalTokens)
            .build();
    }

    @Override
    public Map<String, TranslationStatus> getTranslationStatus(String pagePath) {
        Map<String, TranslationStatus> statusMap = new HashMap<>();

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "bmad-translation-service"))) {

            Resource pageContent = resolver.getResource(pagePath + "/jcr:content");
            if (pageContent != null) {
                checkResourceStatus(pageContent, statusMap, "");
            }
        } catch (LoginException e) {
            LOG.error("Failed to get translation status", e);
        }

        return statusMap;
    }

    @Override
    public String translateText(String text, Locale sourceLocale, Locale targetLocale) {
        if (!config.enabled() || text == null || text.trim().isEmpty()) {
            return text;
        }

        String systemPrompt = buildTranslationSystemPrompt(sourceLocale, targetLocale);

        LLMRequest request = LLMRequest.builder()
            .systemPrompt(systemPrompt)
            .addUserMessage(text)
            .model(config.model())
            .temperature(0.3) // Lower temperature for more consistent translations
            .build();

        LLMResponse response = llmService.complete(request);
        return response.isSuccess() ? response.getContent().trim() : text;
    }

    // ===== Private Helper Methods =====

    private Map<String, String> extractTranslatableContent(Resource resource) {
        Map<String, String> content = new LinkedHashMap<>();
        extractFromResource(resource, content, "");
        return content;
    }

    private void extractFromResource(Resource resource, Map<String, String> content, String prefix) {
        ValueMap props = resource.getValueMap();

        for (String prop : props.keySet()) {
            if (skipProperties.contains(prop)) continue;

            // Check if property is translatable
            if (isTranslatableProperty(prop)) {
                String value = props.get(prop, String.class);
                if (value != null && !value.trim().isEmpty() && looksLikeText(value)) {
                    String key = prefix.isEmpty() ? prop : prefix + "/" + prop;
                    content.put(key, value);
                }
            }
        }

        // Recurse into child resources (components)
        for (Resource child : resource.getChildren()) {
            String childPrefix = prefix.isEmpty() ? child.getName() : prefix + "/" + child.getName();
            extractFromResource(child, content, childPrefix);
        }
    }

    private Map<String, String> extractComponentContent(Resource component) {
        Map<String, String> content = new LinkedHashMap<>();
        ValueMap props = component.getValueMap();

        for (String prop : props.keySet()) {
            if (skipProperties.contains(prop)) continue;

            if (isTranslatableProperty(prop)) {
                String value = props.get(prop, String.class);
                if (value != null && !value.trim().isEmpty() && looksLikeText(value)) {
                    content.put(prop, value);
                }
            }
        }

        return content;
    }

    private boolean isTranslatableProperty(String propertyName) {
        // Check explicit text properties
        if (allTextProperties.contains(propertyName)) {
            return true;
        }

        // Heuristic: properties ending with common suffixes
        String lower = propertyName.toLowerCase();
        return lower.endsWith("text") ||
               lower.endsWith("title") ||
               lower.endsWith("description") ||
               lower.endsWith("label") ||
               lower.endsWith("heading") ||
               lower.endsWith("caption");
    }

    private boolean looksLikeText(String value) {
        // Heuristic: text typically has spaces and is not a path/URL/ID
        if (value.startsWith("/") || value.startsWith("http") || value.startsWith("{")) {
            return false;
        }
        // Contains at least one space (multi-word text)
        return value.contains(" ") || value.length() > 20;
    }

    private Map<String, String> translateBatch(Map<String, String> content, Locale targetLocale,
                                                TranslationResult.Builder result) {
        Map<String, String> translations = new LinkedHashMap<>();

        // Process in batches
        List<Map.Entry<String, String>> entries = new ArrayList<>(content.entrySet());

        for (int i = 0; i < entries.size(); i += config.batchSize()) {
            int end = Math.min(i + config.batchSize(), entries.size());
            List<Map.Entry<String, String>> batch = entries.subList(i, end);

            Map<String, String> batchTranslations = translateSingleBatch(batch, targetLocale, result);
            translations.putAll(batchTranslations);
        }

        return translations;
    }

    private Map<String, String> translateSingleBatch(List<Map.Entry<String, String>> batch,
                                                      Locale targetLocale,
                                                      TranslationResult.Builder result) {
        Map<String, String> translations = new LinkedHashMap<>();

        try {
            // Build structured prompt for batch translation
            StringBuilder prompt = new StringBuilder();
            prompt.append("Translate the following content to ").append(targetLocale.getDisplayLanguage());
            prompt.append(". Return a JSON object where keys match the input keys.\n\n");
            prompt.append("```json\n{\n");

            for (int i = 0; i < batch.size(); i++) {
                Map.Entry<String, String> entry = batch.get(i);
                prompt.append("  \"").append(escapeJson(entry.getKey())).append("\": \"")
                      .append(escapeJson(entry.getValue())).append("\"");
                if (i < batch.size() - 1) prompt.append(",");
                prompt.append("\n");
            }
            prompt.append("}\n```");

            String systemPrompt = "You are a professional translator. Translate content accurately while " +
                "preserving HTML markup, placeholders like ${variable}, and formatting. " +
                "Return ONLY valid JSON with translated values.";

            LLMRequest request = LLMRequest.builder()
                .systemPrompt(systemPrompt)
                .addUserMessage(prompt.toString())
                .model(config.model())
                .temperature(0.3)
                .maxTokens(4096)
                .build();

            LLMResponse response = llmService.complete(request);

            if (response.isSuccess()) {
                // Parse JSON response
                String jsonContent = extractJsonFromResponse(response.getContent());
                JsonNode root = MAPPER.readTree(jsonContent);

                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    translations.put(field.getKey(), field.getValue().asText());
                }

                result.tokensUsed(result.build().getTokensUsed() + response.getUsage().getTotalTokens());
            } else {
                result.addError("Translation batch failed: " + response.getError());
            }

        } catch (Exception e) {
            LOG.error("Batch translation error", e);
            result.addError("Batch translation error: " + e.getMessage());
        }

        return translations;
    }

    private void applyTranslations(Resource targetResource, Map<String, String> translations,
                                   Map<String, String> originalContent, ResourceResolver resolver)
            throws PersistenceException {

        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String path = entry.getKey();
            String translatedValue = entry.getValue();
            String originalValue = originalContent.get(path);

            // Parse path to get resource and property
            String resourcePath;
            String propertyName;

            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                resourcePath = path.substring(0, lastSlash);
                propertyName = path.substring(lastSlash + 1);
            } else {
                resourcePath = "";
                propertyName = path;
            }

            Resource targetProp = resourcePath.isEmpty()
                ? targetResource
                : targetResource.getChild(resourcePath);

            if (targetProp != null) {
                ModifiableValueMap props = targetProp.adaptTo(ModifiableValueMap.class);
                if (props != null) {
                    // Set translated value
                    props.put(propertyName, translatedValue);

                    // Store tracking metadata
                    props.put("ai_original_" + propertyName, hash(originalValue));
                    props.put("ai_translated_" + propertyName, hash(translatedValue));
                    props.put("ai_translation_date", Calendar.getInstance());
                    props.put("ai_translation_provider", config.llmProvider());
                }
            }
        }
    }

    private void checkResourceStatus(Resource resource, Map<String, TranslationStatus> statusMap, String prefix) {
        ValueMap props = resource.getValueMap();

        for (String prop : allTextProperties) {
            String originalHashProp = "ai_original_" + prop;
            String translatedHashProp = "ai_translated_" + prop;

            if (props.containsKey(prop)) {
                String key = prefix.isEmpty() ? prop : prefix + "/" + prop;

                if (!props.containsKey(originalHashProp)) {
                    statusMap.put(key, TranslationStatus.NEVER_TRANSLATED);
                } else {
                    String currentValue = props.get(prop, String.class);
                    String translatedHash = props.get(translatedHashProp, String.class);

                    if (!hash(currentValue).equals(translatedHash)) {
                        statusMap.put(key, TranslationStatus.MANUALLY_EDITED);
                    } else {
                        statusMap.put(key, TranslationStatus.UP_TO_DATE);
                    }
                }
            }
        }

        for (Resource child : resource.getChildren()) {
            String childPrefix = prefix.isEmpty() ? child.getName() : prefix + "/" + child.getName();
            checkResourceStatus(child, statusMap, childPrefix);
        }
    }

    private List<String> getChildPages(String rootPath, boolean recursive) {
        List<String> pages = new ArrayList<>();
        // Implementation would query for cq:Page nodes under rootPath
        // Using JCR query or Sling resource iteration
        return pages;
    }

    private String buildTranslationSystemPrompt(Locale source, Locale target) {
        return String.format(
            "You are a professional translator. Translate from %s to %s. " +
            "Maintain the original tone and style. Preserve any HTML markup, " +
            "placeholders (like ${variable}), and special formatting. " +
            "Return only the translated text without explanations.",
            source.getDisplayLanguage(),
            target.getDisplayLanguage()
        );
    }

    private String hash(String value) {
        if (value == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(value.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(value.hashCode());
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String extractJsonFromResponse(String response) {
        // Extract JSON from potential markdown code blocks
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
```

## Live Copy Rollout Integration

Integrate AI translation as a rollout action in AEM's Multi-Site Manager:

```java
package com.example.aem.bmad.core.msm;

import com.day.cq.wcm.msm.api.*;
import com.example.aem.bmad.core.services.AITranslationService;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.*;

import java.util.Locale;

@Component(
    service = LiveAction.class,
    property = {
        "liveActionName=aiTranslate"
    }
)
public class AITranslationRolloutAction implements LiveAction {

    @Reference
    private AITranslationService translationService;

    @Override
    public String getName() {
        return "aiTranslate";
    }

    @Override
    public void execute(Resource source, Resource target, LiveRelationship relation, boolean autoSave, boolean isResetRollout)
            throws WCMException {

        // Determine target locale from path or configuration
        Locale targetLocale = determineLocale(target.getPath());

        if (targetLocale != null) {
            translationService.translateComponent(source, target, targetLocale);
        }
    }

    @Override
    public String getParameterName() {
        return null;
    }

    @Override
    public String[] getPropertiesNames() {
        return new String[0];
    }

    @Override
    public int getRank() {
        return 100; // Execute after standard rollout actions
    }

    @Override
    public String getTitle() {
        return "AI Translate";
    }

    private Locale determineLocale(String path) {
        // Extract locale from path structure like /content/site/en, /content/site/de
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (segment.length() == 2) {
                return new Locale(segment);
            }
            if (segment.contains("_") && segment.length() == 5) {
                String[] parts = segment.split("_");
                return new Locale(parts[0], parts[1]);
            }
        }
        return null;
    }
}
```

## Workflow Process Step

Add translation as a workflow step for approval workflows:

```java
package com.example.aem.bmad.core.workflow;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.example.aem.bmad.core.models.TranslationResult;
import com.example.aem.bmad.core.services.AITranslationService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=AI Translation"
    }
)
public class AITranslationWorkflowProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(AITranslationWorkflowProcess.class);

    @Reference
    private AITranslationService translationService;

    @Override
    public void execute(WorkItem workItem, WorkflowSession session, MetaDataMap args) throws WorkflowException {
        String payload = workItem.getWorkflowData().getPayload().toString();

        // Get arguments
        String targetPath = args.get("targetPath", String.class);
        String targetLang = args.get("targetLanguage", "en");

        if (targetPath == null) {
            throw new WorkflowException("Target path is required");
        }

        Locale targetLocale = Locale.forLanguageTag(targetLang);

        LOG.info("Starting AI translation workflow: {} -> {} ({})", payload, targetPath, targetLang);

        TranslationResult result = translationService.translatePage(payload, targetPath, targetLocale);

        if (!result.isSuccess()) {
            throw new WorkflowException("Translation failed: " + String.join(", ", result.getErrors()));
        }

        LOG.info("Translation complete: {} properties translated", result.getPropertiesTranslated());

        // Store result in workflow metadata for downstream steps
        workItem.getWorkflow().getMetaDataMap().put("translationResult", result);
    }
}
```

## Translation Servlet (for UI)

```java
package com.example.aem.bmad.core.servlets;

import com.example.aem.bmad.core.models.TranslationResult;
import com.example.aem.bmad.core.services.AITranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.*;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/bmad/translate",
        "sling.servlet.methods=POST"
    }
)
public class AITranslationServlet extends SlingAllMethodsServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private AITranslationService translationService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String sourcePath = request.getParameter("sourcePath");
        String targetPath = request.getParameter("targetPath");
        String targetLang = request.getParameter("targetLanguage");
        boolean recursive = "true".equals(request.getParameter("recursive"));

        response.setContentType("application/json");

        if (sourcePath == null || targetPath == null || targetLang == null) {
            sendError(response, 400, "Missing required parameters");
            return;
        }

        Locale targetLocale = Locale.forLanguageTag(targetLang);
        TranslationResult result;

        if (recursive) {
            result = translationService.translateTree(sourcePath, targetPath, targetLocale, true);
        } else {
            result = translationService.translatePage(sourcePath, targetPath, targetLocale);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", result.isSuccess());
        responseData.put("propertiesTranslated", result.getPropertiesTranslated());
        responseData.put("tokensUsed", result.getTokensUsed());
        responseData.put("durationMs", result.getDurationMs());
        responseData.put("errors", result.getErrors());
        responseData.put("warnings", result.getWarnings());

        response.getWriter().write(MAPPER.writeValueAsString(responseData));
    }

    private void sendError(SlingHttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
```

## Best Practices

### Translation Quality

1. **Use full page context**: Translate entire pages in single requests for better context
2. **Lower temperature**: Use 0.2-0.4 for consistent, accurate translations
3. **Preserve markup**: System prompt should explicitly mention preserving HTML/placeholders
4. **Review critical content**: AI translation for legal/medical content should have human review

### Performance

1. **Batch translations**: Group properties to reduce API calls
2. **Cache translations**: Consider caching common phrases
3. **Async processing**: Use workflow for large translation jobs
4. **Change detection**: Only re-translate modified content

### Cost Management

1. **Track token usage**: Monitor and report on translation costs
2. **Set rate limits**: Prevent runaway translation jobs
3. **Use appropriate models**: GPT-3.5 may suffice for simple content

## Next Steps

- [AI Services Integration](ai-services.md) - Core LLM service layer
- [Content Creation Dialog](content-creation-dialog.md) - Author-facing AI tools
