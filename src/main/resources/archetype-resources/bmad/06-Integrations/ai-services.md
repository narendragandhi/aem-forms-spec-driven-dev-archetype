# AI Services Integration

This guide covers integrating Large Language Model (LLM) services into AEM for content creation, translation, and intelligent content management. The architecture supports multiple LLM providers through a unified abstraction layer.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        AEM as a Cloud Service                          │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │  Content    │  │ Translation │  │  Sidebar    │  │   Workflow  │   │
│  │  Creation   │  │   Service   │  │    AI       │  │   Process   │   │
│  │  Dialog     │  │             │  │  Assistant  │  │    Steps    │   │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘   │
│         │                │                │                │          │
│         └────────────────┴────────────────┴────────────────┘          │
│                                   │                                    │
│                          ┌────────▼────────┐                          │
│                          │   LLM Service   │                          │
│                          │   Abstraction   │                          │
│                          └────────┬────────┘                          │
│                                   │                                    │
│         ┌─────────────────────────┼─────────────────────────┐         │
│         │                         │                         │         │
│  ┌──────▼──────┐  ┌───────────────▼───────────────┐  ┌──────▼──────┐ │
│  │   OpenAI    │  │         Anthropic             │  │   Google    │ │
│  │   Backend   │  │     Claude Backend            │  │   Gemini    │ │
│  └─────────────┘  └───────────────────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

## Supported LLM Providers

| Provider | Models | Use Cases |
|----------|--------|-----------|
| **OpenAI** | GPT-4o, GPT-4-turbo, GPT-3.5-turbo | General content, summarization |
| **Anthropic Claude** | Claude 3.5 Sonnet, Claude 3 Opus | Long-form content, analysis |
| **Google Gemini** | Gemini Pro, Gemini Pro Vision | Multimodal content, images |
| **Azure OpenAI** | GPT-4, GPT-3.5 (Azure-hosted) | Enterprise compliance |

## Core Service Interfaces

### LLMService Interface

```java
package com.example.aem.bmad.core.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction layer for Large Language Model services.
 * Implementations can target OpenAI, Claude, Gemini, or other providers.
 */
public interface LLMService {

    /**
     * Get the provider identifier
     */
    String getProviderId();

    /**
     * Check if the service is available and configured
     */
    boolean isAvailable();

    /**
     * Generate a completion for the given prompt
     *
     * @param request the completion request with prompt and options
     * @return the completion response
     */
    LLMResponse complete(LLMRequest request);

    /**
     * Generate a completion asynchronously
     *
     * @param request the completion request
     * @return future containing the completion response
     */
    CompletableFuture<LLMResponse> completeAsync(LLMRequest request);

    /**
     * Stream a completion response (for real-time UI updates)
     *
     * @param request the completion request
     * @param callback callback invoked for each chunk
     */
    void streamCompletion(LLMRequest request, StreamCallback callback);

    /**
     * Generate embeddings for text (for RAG/semantic search)
     *
     * @param texts list of texts to embed
     * @return list of embedding vectors
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Callback interface for streaming responses
     */
    interface StreamCallback {
        void onChunk(String chunk);
        void onComplete(LLMResponse fullResponse);
        void onError(Exception e);
    }
}
```

### LLMRequest Model

```java
package com.example.aem.bmad.core.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request object for LLM completions.
 * Supports chat-style messages and configuration options.
 */
public class LLMRequest {

    private final List<Message> messages;
    private String model;
    private double temperature = 0.7;
    private int maxTokens = 2048;
    private String systemPrompt;
    private Map<String, Object> metadata;

    private LLMRequest(Builder builder) {
        this.messages = builder.messages;
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.systemPrompt = builder.systemPrompt;
        this.metadata = builder.metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience method for simple single-prompt requests
     */
    public static LLMRequest simple(String prompt) {
        return builder()
            .addUserMessage(prompt)
            .build();
    }

    /**
     * Convenience method with system prompt
     */
    public static LLMRequest withSystem(String systemPrompt, String userPrompt) {
        return builder()
            .systemPrompt(systemPrompt)
            .addUserMessage(userPrompt)
            .build();
    }

    // Getters
    public List<Message> getMessages() { return messages; }
    public String getModel() { return model; }
    public double getTemperature() { return temperature; }
    public int getMaxTokens() { return maxTokens; }
    public String getSystemPrompt() { return systemPrompt; }
    public Map<String, Object> getMetadata() { return metadata; }

    public static class Message {
        private final String role; // "user", "assistant", "system"
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    public static class Builder {
        private List<Message> messages = new ArrayList<>();
        private String model;
        private double temperature = 0.7;
        private int maxTokens = 2048;
        private String systemPrompt;
        private Map<String, Object> metadata = new HashMap<>();

        public Builder addMessage(String role, String content) {
            messages.add(new Message(role, content));
            return this;
        }

        public Builder addUserMessage(String content) {
            return addMessage("user", content);
        }

        public Builder addAssistantMessage(String content) {
            return addMessage("assistant", content);
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public LLMRequest build() {
            return new LLMRequest(this);
        }
    }
}
```

### LLMResponse Model

```java
package com.example.aem.bmad.core.models;

/**
 * Response from an LLM completion request.
 */
public class LLMResponse {

    private final boolean success;
    private final String content;
    private final String error;
    private final Usage usage;
    private final String model;
    private final String finishReason;

    private LLMResponse(Builder builder) {
        this.success = builder.success;
        this.content = builder.content;
        this.error = builder.error;
        this.usage = builder.usage;
        this.model = builder.model;
        this.finishReason = builder.finishReason;
    }

    public static LLMResponse success(String content, Usage usage) {
        return new Builder()
            .success(true)
            .content(content)
            .usage(usage)
            .build();
    }

    public static LLMResponse error(String error) {
        return new Builder()
            .success(false)
            .error(error)
            .build();
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getContent() { return content; }
    public String getError() { return error; }
    public Usage getUsage() { return usage; }
    public String getModel() { return model; }
    public String getFinishReason() { return finishReason; }

    /**
     * Token usage statistics
     */
    public static class Usage {
        private final int promptTokens;
        private final int completionTokens;
        private final int totalTokens;

        public Usage(int promptTokens, int completionTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = promptTokens + completionTokens;
        }

        public int getPromptTokens() { return promptTokens; }
        public int getCompletionTokens() { return completionTokens; }
        public int getTotalTokens() { return totalTokens; }
    }

    public static class Builder {
        private boolean success;
        private String content;
        private String error;
        private Usage usage;
        private String model;
        private String finishReason;

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder usage(Usage usage) { this.usage = usage; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder finishReason(String finishReason) { this.finishReason = finishReason; return this; }

        public LLMResponse build() {
            return new LLMResponse(this);
        }
    }
}
```

## OSGi Configuration

### Provider-Agnostic Configuration

```java
@ObjectClassDefinition(
    name = "BMAD LLM Service Configuration",
    description = "Configuration for AI/LLM service integration"
)
public @interface LLMServiceConfig {

    @AttributeDefinition(
        name = "Provider",
        description = "LLM provider to use",
        options = {
            @Option(label = "OpenAI", value = "openai"),
            @Option(label = "Anthropic Claude", value = "claude"),
            @Option(label = "Google Gemini", value = "gemini"),
            @Option(label = "Azure OpenAI", value = "azure-openai")
        }
    )
    String provider() default "openai";

    @AttributeDefinition(
        name = "API Key",
        description = "API key for the selected provider (use secrets management in production)"
    )
    String apiKey();

    @AttributeDefinition(
        name = "Default Model",
        description = "Default model to use for completions"
    )
    String defaultModel() default "gpt-4o";

    @AttributeDefinition(
        name = "Default Temperature",
        description = "Default creativity setting (0.0 - 1.0)"
    )
    double defaultTemperature() default 0.7;

    @AttributeDefinition(
        name = "Max Tokens",
        description = "Maximum tokens in response"
    )
    int maxTokens() default 2048;

    @AttributeDefinition(
        name = "Timeout (seconds)",
        description = "Request timeout"
    )
    int timeout() default 60;

    @AttributeDefinition(
        name = "Enabled",
        description = "Enable/disable LLM features"
    )
    boolean enabled() default true;
}
```

### Sling Context Aware Configuration

For site-specific LLM settings, use Sling Context Aware Configuration:

```java
package com.example.aem.bmad.core.caconfig;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(
    label = "AI Services Configuration",
    description = "Site-specific AI/LLM configuration"
)
public @interface AIServicesConfig {

    @Property(label = "Enabled", description = "Enable AI features for this site")
    boolean enabled() default true;

    @Property(label = "Provider", description = "LLM provider override")
    String provider() default "";

    @Property(label = "Content Creation Model", description = "Model for content creation tasks")
    String contentCreationModel() default "gpt-4o";

    @Property(label = "Translation Model", description = "Model for translation tasks")
    String translationModel() default "gpt-4o";

    @Property(label = "Allowed Services", description = "Comma-separated list of allowed AI services")
    String[] allowedServices() default {"content-creation", "translation", "summarization"};

    @Property(label = "Max Requests Per Hour", description = "Rate limit per user per hour")
    int maxRequestsPerHour() default 100;
}
```

## OpenAI Implementation

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.models.LLMRequest;
import com.example.aem.bmad.core.models.LLMResponse;
import com.example.aem.bmad.core.services.HttpClientService;
import com.example.aem.bmad.core.services.LLMService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component(
    service = LLMService.class,
    property = {
        "provider=openai"
    }
)
@Designate(ocd = OpenAIServiceImpl.Config.class)
public class OpenAIServiceImpl implements LLMService {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAIServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String OPENAI_API = "https://api.openai.com/v1/chat/completions";
    private static final String EMBEDDINGS_API = "https://api.openai.com/v1/embeddings";

    @ObjectClassDefinition(name = "BMAD OpenAI Configuration")
    public @interface Config {
        @AttributeDefinition(name = "API Key")
        String apiKey();

        @AttributeDefinition(name = "Default Model")
        String defaultModel() default "gpt-4o";

        @AttributeDefinition(name = "Organization ID")
        String organizationId() default "";

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;
    }

    @Reference
    private HttpClientService httpClient;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("OpenAI service configured: enabled={}, model={}",
            config.enabled(), config.defaultModel());
    }

    @Override
    public String getProviderId() {
        return "openai";
    }

    @Override
    public boolean isAvailable() {
        return config.enabled() && config.apiKey() != null && !config.apiKey().isEmpty();
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        if (!isAvailable()) {
            return LLMResponse.error("OpenAI service is not available");
        }

        try {
            String payload = buildRequestPayload(request);
            Map<String, String> headers = buildHeaders();

            var response = httpClient.post(OPENAI_API, payload, headers);

            if (response.isSuccess()) {
                return parseResponse(response.getBody());
            } else {
                LOG.error("OpenAI API error: {} - {}", response.getStatusCode(), response.getBody());
                return LLMResponse.error("API returned status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            LOG.error("Error calling OpenAI API", e);
            return LLMResponse.error(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<LLMResponse> completeAsync(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> complete(request));
    }

    @Override
    public void streamCompletion(LLMRequest request, StreamCallback callback) {
        // SSE streaming implementation would go here
        // For simplicity, falling back to regular completion
        try {
            LLMResponse response = complete(request);
            if (response.isSuccess()) {
                callback.onChunk(response.getContent());
                callback.onComplete(response);
            } else {
                callback.onError(new RuntimeException(response.getError()));
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (!isAvailable() || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("model", "text-embedding-3-small");
            ArrayNode inputArray = payload.putArray("input");
            texts.forEach(inputArray::add);

            var response = httpClient.post(EMBEDDINGS_API, payload.toString(), buildHeaders());

            if (response.isSuccess()) {
                JsonNode root = MAPPER.readTree(response.getBody());
                List<float[]> embeddings = new ArrayList<>();
                for (JsonNode data : root.get("data")) {
                    JsonNode embedding = data.get("embedding");
                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = (float) embedding.get(i).asDouble();
                    }
                    embeddings.add(vector);
                }
                return embeddings;
            }
        } catch (Exception e) {
            LOG.error("Error generating embeddings", e);
        }
        return Collections.emptyList();
    }

    private String buildRequestPayload(LLMRequest request) throws Exception {
        ObjectNode payload = MAPPER.createObjectNode();

        String model = request.getModel() != null ? request.getModel() : config.defaultModel();
        payload.put("model", model);
        payload.put("temperature", request.getTemperature());
        payload.put("max_tokens", request.getMaxTokens());

        ArrayNode messages = payload.putArray("messages");

        // Add system prompt if present
        if (request.getSystemPrompt() != null) {
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
        }

        // Add conversation messages
        for (LLMRequest.Message msg : request.getMessages()) {
            ObjectNode msgNode = messages.addObject();
            msgNode.put("role", msg.getRole());
            msgNode.put("content", msg.getContent());
        }

        return MAPPER.writeValueAsString(payload);
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + config.apiKey());
        headers.put("Content-Type", "application/json");
        if (config.organizationId() != null && !config.organizationId().isEmpty()) {
            headers.put("OpenAI-Organization", config.organizationId());
        }
        return headers;
    }

    private LLMResponse parseResponse(String responseBody) throws Exception {
        JsonNode root = MAPPER.readTree(responseBody);

        JsonNode choices = root.get("choices");
        if (choices != null && choices.size() > 0) {
            JsonNode choice = choices.get(0);
            String content = choice.get("message").get("content").asText();
            String finishReason = choice.get("finish_reason").asText();

            JsonNode usageNode = root.get("usage");
            LLMResponse.Usage usage = new LLMResponse.Usage(
                usageNode.get("prompt_tokens").asInt(),
                usageNode.get("completion_tokens").asInt()
            );

            return new LLMResponse.Builder()
                .success(true)
                .content(content)
                .usage(usage)
                .model(root.get("model").asText())
                .finishReason(finishReason)
                .build();
        }

        return LLMResponse.error("No choices in response");
    }
}
```

## Claude Implementation

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.models.LLMRequest;
import com.example.aem.bmad.core.models.LLMResponse;
import com.example.aem.bmad.core.services.HttpClientService;
import com.example.aem.bmad.core.services.LLMService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component(
    service = LLMService.class,
    property = {
        "provider=claude"
    }
)
@Designate(ocd = ClaudeServiceImpl.Config.class)
public class ClaudeServiceImpl implements LLMService {

    private static final Logger LOG = LoggerFactory.getLogger(ClaudeServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CLAUDE_API = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    @ObjectClassDefinition(name = "BMAD Anthropic Claude Configuration")
    public @interface Config {
        @AttributeDefinition(name = "API Key")
        String apiKey();

        @AttributeDefinition(name = "Default Model")
        String defaultModel() default "claude-sonnet-4-20250514";

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default true;
    }

    @Reference
    private HttpClientService httpClient;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        LOG.info("Claude service configured: enabled={}, model={}",
            config.enabled(), config.defaultModel());
    }

    @Override
    public String getProviderId() {
        return "claude";
    }

    @Override
    public boolean isAvailable() {
        return config.enabled() && config.apiKey() != null && !config.apiKey().isEmpty();
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        if (!isAvailable()) {
            return LLMResponse.error("Claude service is not available");
        }

        try {
            String payload = buildRequestPayload(request);
            Map<String, String> headers = buildHeaders();

            var response = httpClient.post(CLAUDE_API, payload, headers);

            if (response.isSuccess()) {
                return parseResponse(response.getBody());
            } else {
                LOG.error("Claude API error: {} - {}", response.getStatusCode(), response.getBody());
                return LLMResponse.error("API returned status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            LOG.error("Error calling Claude API", e);
            return LLMResponse.error(e.getMessage());
        }
    }

    @Override
    public CompletableFuture<LLMResponse> completeAsync(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> complete(request));
    }

    @Override
    public void streamCompletion(LLMRequest request, StreamCallback callback) {
        try {
            LLMResponse response = complete(request);
            if (response.isSuccess()) {
                callback.onChunk(response.getContent());
                callback.onComplete(response);
            } else {
                callback.onError(new RuntimeException(response.getError()));
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        // Claude doesn't have a native embeddings API
        // Would need to use a different provider or Voyager embeddings
        LOG.warn("Embeddings not supported by Claude provider");
        return Collections.emptyList();
    }

    private String buildRequestPayload(LLMRequest request) throws Exception {
        ObjectNode payload = MAPPER.createObjectNode();

        String model = request.getModel() != null ? request.getModel() : config.defaultModel();
        payload.put("model", model);
        payload.put("max_tokens", request.getMaxTokens());

        // Claude uses top-level system field
        if (request.getSystemPrompt() != null) {
            payload.put("system", request.getSystemPrompt());
        }

        ArrayNode messages = payload.putArray("messages");
        for (LLMRequest.Message msg : request.getMessages()) {
            ObjectNode msgNode = messages.addObject();
            msgNode.put("role", msg.getRole());
            msgNode.put("content", msg.getContent());
        }

        return MAPPER.writeValueAsString(payload);
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", config.apiKey());
        headers.put("anthropic-version", API_VERSION);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private LLMResponse parseResponse(String responseBody) throws Exception {
        JsonNode root = MAPPER.readTree(responseBody);

        JsonNode content = root.get("content");
        if (content != null && content.size() > 0) {
            StringBuilder textContent = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.get("type").asText())) {
                    textContent.append(block.get("text").asText());
                }
            }

            JsonNode usageNode = root.get("usage");
            LLMResponse.Usage usage = new LLMResponse.Usage(
                usageNode.get("input_tokens").asInt(),
                usageNode.get("output_tokens").asInt()
            );

            return new LLMResponse.Builder()
                .success(true)
                .content(textContent.toString())
                .usage(usage)
                .model(root.get("model").asText())
                .finishReason(root.get("stop_reason").asText())
                .build();
        }

        return LLMResponse.error("No content in response");
    }
}
```

## LLM Service Factory

To dynamically select the appropriate LLM service based on configuration:

```java
package com.example.aem.bmad.core.services.impl;

import com.example.aem.bmad.core.services.LLMService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service = LLMServiceFactory.class)
public class LLMServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(LLMServiceFactory.class);

    private final Map<String, LLMService> providers = new HashMap<>();

    @Reference(
        service = LLMService.class,
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindLLMService(LLMService service) {
        providers.put(service.getProviderId(), service);
        LOG.info("Registered LLM provider: {}", service.getProviderId());
    }

    protected void unbindLLMService(LLMService service) {
        providers.remove(service.getProviderId());
        LOG.info("Unregistered LLM provider: {}", service.getProviderId());
    }

    /**
     * Get an LLM service by provider ID
     */
    public LLMService getService(String providerId) {
        LLMService service = providers.get(providerId);
        if (service == null || !service.isAvailable()) {
            LOG.warn("LLM provider not available: {}", providerId);
            return null;
        }
        return service;
    }

    /**
     * Get the first available LLM service
     */
    public LLMService getAvailableService() {
        return providers.values().stream()
            .filter(LLMService::isAvailable)
            .findFirst()
            .orElse(null);
    }

    /**
     * Check if any LLM service is available
     */
    public boolean hasAvailableService() {
        return providers.values().stream().anyMatch(LLMService::isAvailable);
    }
}
```

## Usage Examples

### Basic Content Generation

```java
@Reference
private LLMServiceFactory llmFactory;

public String generateProductDescription(String productName, String features) {
    LLMService llm = llmFactory.getService("openai");
    if (llm == null) {
        return null;
    }

    LLMRequest request = LLMRequest.builder()
        .systemPrompt("You are a marketing copywriter. Write compelling product descriptions.")
        .addUserMessage("Write a product description for: " + productName + "\nFeatures: " + features)
        .temperature(0.8)
        .maxTokens(500)
        .build();

    LLMResponse response = llm.complete(request);
    return response.isSuccess() ? response.getContent() : null;
}
```

### Content Summarization

```java
public String summarizeContent(String content, int maxLength) {
    LLMService llm = llmFactory.getAvailableService();
    if (llm == null) {
        return content.substring(0, Math.min(content.length(), maxLength));
    }

    LLMRequest request = LLMRequest.builder()
        .systemPrompt("Summarize the following content concisely in about " + maxLength + " characters.")
        .addUserMessage(content)
        .temperature(0.3)
        .build();

    LLMResponse response = llm.complete(request);
    return response.isSuccess() ? response.getContent() : content;
}
```

## Security Considerations

1. **API Key Management**: Store API keys in AEM's Cloud Manager secrets, not in code or OSGi configs
2. **Rate Limiting**: Implement per-user rate limits to prevent abuse
3. **Content Filtering**: Validate AI-generated content before publishing
4. **Audit Logging**: Log all AI interactions for compliance
5. **Permission Checks**: Verify user has permission to use AI features

```java
// Example: Rate limiting decorator
public class RateLimitedLLMService implements LLMService {

    private final LLMService delegate;
    private final RateLimiter rateLimiter;

    public LLMResponse complete(LLMRequest request) {
        String userId = getCurrentUserId();
        if (!rateLimiter.tryAcquire(userId)) {
            return LLMResponse.error("Rate limit exceeded. Please try again later.");
        }
        return delegate.complete(request);
    }
}
```

## Monitoring and Observability

Track these metrics for AI service health:

- Request latency (p50, p95, p99)
- Token usage per request/user/site
- Error rates by provider
- Cost tracking (estimated from token usage)

```java
@Reference
private MetricsService metrics;

public LLMResponse complete(LLMRequest request) {
    long start = System.currentTimeMillis();
    try {
        LLMResponse response = delegate.complete(request);

        metrics.recordLatency("llm.request.latency", System.currentTimeMillis() - start);
        if (response.isSuccess()) {
            metrics.incrementCounter("llm.request.success");
            metrics.recordValue("llm.tokens.used", response.getUsage().getTotalTokens());
        } else {
            metrics.incrementCounter("llm.request.error");
        }

        return response;
    } catch (Exception e) {
        metrics.incrementCounter("llm.request.exception");
        throw e;
    }
}
```

## Next Steps

- [AI Translation Service](ai-translation.md) - Automatic content translation using LLMs
- [Content Creation Dialog](content-creation-dialog.md) - Author-facing AI content tools
- [RAG Implementation](rag-implementation.md) - Retrieval-augmented generation for contextual AI
