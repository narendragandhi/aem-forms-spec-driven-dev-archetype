# Integration Testing Guide

## Overview

This guide provides comprehensive procedures for integration testing in the AEM BMAD Showcase project. It covers testing patterns, environment setup, third-party integrations, CI/CD pipeline configuration, and test data management strategies.

---

## Table of Contents

1. [Integration Testing Strategy](#1-integration-testing-strategy)
2. [AEM Integration Test Patterns](#2-aem-integration-test-patterns)
3. [Third-Party Integration Testing](#3-third-party-integration-testing)
4. [Test Environment Setup](#4-test-environment-setup)
5. [CI/CD Pipeline Integration](#5-cicd-pipeline-integration)
6. [Test Data Management](#6-test-data-management)
7. [API Contract Testing](#7-api-contract-testing)
8. [End-to-End Testing](#8-end-to-end-testing)

---

## 1. Integration Testing Strategy

### 1.1 Testing Pyramid for AEM

```
                    ┌─────────────┐
                    │   E2E/UI    │  5% - Slow, Expensive
                    │   Tests     │
                    ├─────────────┤
                 ┌──┴─────────────┴──┐
                 │   Integration     │  20% - Medium Speed
                 │      Tests        │
                 ├───────────────────┤
              ┌──┴───────────────────┴──┐
              │      Unit Tests         │  75% - Fast, Cheap
              └─────────────────────────┘
```

### 1.2 Integration Test Categories

| Category | Scope | Tools | Execution |
|----------|-------|-------|-----------|
| **Component Integration** | Sling Model + JCR | AEM Mocks, wcm.io | Build time |
| **Service Integration** | OSGi Services | AEM Testing Clients | Build + Stage |
| **API Integration** | REST/GraphQL endpoints | REST Assured | Stage |
| **External Integration** | LLM, Email, Analytics | WireMock, Testcontainers | Build + Stage |
| **Dispatcher Integration** | Cache, routing | Docker Dispatcher | Pre-deploy |

### 1.3 Test Coverage Requirements

| Layer | Minimum Coverage | Target | Critical Paths |
|-------|-----------------|--------|----------------|
| Sling Models | 70% | 85% | All public methods |
| OSGi Services | 80% | 90% | All business logic |
| Servlets | 70% | 85% | All endpoints |
| API Endpoints | 90% | 100% | All external APIs |
| Integrations | 80% | 95% | All third-party calls |

---

## 2. AEM Integration Test Patterns

### 2.1 Sling Model Integration Tests

**Using AEM Mocks (io.wcm.testing.mock.aem):**

```java
@ExtendWith(AemContextExtension.class)
class HeroModelIntegrationTest {

    private final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @BeforeEach
    void setUp() {
        // Load content structure
        context.load().json("/content/hero-test-data.json", "/content/test");

        // Register OSGi services
        context.registerService(LLMService.class, new MockLLMService());

        // Register Sling Models
        context.addModelsForClasses(HeroModel.class);
    }

    @Test
    void testHeroModelWithAllProperties() {
        // Given
        Resource resource = context.resourceResolver()
            .getResource("/content/test/hero-complete");

        // When
        HeroModel hero = resource.adaptTo(HeroModel.class);

        // Then
        assertThat(hero).isNotNull();
        assertThat(hero.getTitle()).isEqualTo("Welcome to BMAD");
        assertThat(hero.getDescription()).isNotBlank();
        assertThat(hero.getBackgroundImage()).startsWith("/content/dam/");
        assertThat(hero.getCta()).isNotNull();
        assertThat(hero.getCta().getUrl()).isNotBlank();
    }

    @Test
    void testHeroModelExportsValidJson() {
        // Given
        Resource resource = context.resourceResolver()
            .getResource("/content/test/hero-complete");
        HeroModel hero = resource.adaptTo(HeroModel.class);

        // When
        String json = hero.getExportedType();

        // Then
        assertThat(json).isEqualTo("bmad-showcase/components/hero");
    }

    @Test
    void testHeroModelHandlesMissingImage() {
        // Given - hero without background image
        Resource resource = context.resourceResolver()
            .getResource("/content/test/hero-no-image");

        // When
        HeroModel hero = resource.adaptTo(HeroModel.class);

        // Then
        assertThat(hero.getBackgroundImage()).isNull();
        assertThat(hero.getPlaceholderClass()).isEqualTo("hero--no-image");
    }
}
```

**Test Data JSON:**
```json
// /src/test/resources/content/hero-test-data.json
{
    "hero-complete": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "bmad-showcase/components/hero",
        "title": "Welcome to BMAD",
        "description": "Experience the power of BMAD methodology",
        "backgroundImage": "/content/dam/bmad-showcase/hero-bg.jpg",
        "cta": {
            "text": "Learn More",
            "url": "/content/bmad-showcase/en/about"
        }
    },
    "hero-no-image": {
        "jcr:primaryType": "nt:unstructured",
        "sling:resourceType": "bmad-showcase/components/hero",
        "title": "Simple Hero"
    }
}
```

### 2.2 OSGi Service Integration Tests

```java
@ExtendWith(OsgiContextExtension.class)
class ContentCreationServiceIntegrationTest {

    private final OsgiContext osgiContext = new OsgiContext();

    private ContentCreationService contentService;
    private MockLLMService mockLLMService;

    @BeforeEach
    void setUp() {
        // Setup mock LLM service
        mockLLMService = new MockLLMService();
        osgiContext.registerService(LLMService.class, mockLLMService);

        // Setup mock resource resolver factory
        osgiContext.registerService(ResourceResolverFactory.class,
            new MockResourceResolverFactory());

        // Register the service under test
        contentService = osgiContext.registerInjectActivateService(
            new ContentCreationServiceImpl());
    }

    @Test
    void testGenerateHeroContent() {
        // Given
        mockLLMService.setResponse("Generated hero content for testing");
        ContentRequest request = ContentRequest.builder()
            .type(ContentType.HERO)
            .context("Product launch page")
            .tone("Professional")
            .build();

        // When
        ContentResponse response = contentService.generateContent(request);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getContent()).contains("Generated hero content");
        assertThat(mockLLMService.getLastPrompt()).contains("HERO");
    }

    @Test
    void testServiceHandlesLLMFailure() {
        // Given
        mockLLMService.setThrowException(new LLMServiceException("API Error"));
        ContentRequest request = ContentRequest.builder()
            .type(ContentType.HERO)
            .context("Test")
            .build();

        // When
        ContentResponse response = contentService.generateContent(request);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("content generation failed");
    }
}
```

### 2.3 Servlet Integration Tests

```java
@ExtendWith(SlingContextExtension.class)
class SearchServletIntegrationTest {

    private final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @BeforeEach
    void setUp() {
        // Load test content
        context.load().json("/content/search-test-content.json", "/content/bmad-showcase");

        // Create search index (for Oak)
        createSearchIndex(context);
    }

    @Test
    void testSearchReturnsResults() throws Exception {
        // Given
        SearchServlet servlet = context.registerInjectActivateService(new SearchServlet());
        context.request().setQueryString("q=bmad&limit=10");

        // When
        servlet.doGet(context.request(), context.response());

        // Then
        assertThat(context.response().getStatus()).isEqualTo(200);

        JsonObject result = parseJson(context.response().getOutputAsString());
        assertThat(result.getJsonArray("results")).isNotEmpty();
        assertThat(result.getInt("total")).isGreaterThan(0);
    }

    @Test
    void testSearchWithNoResults() throws Exception {
        // Given
        SearchServlet servlet = context.registerInjectActivateService(new SearchServlet());
        context.request().setQueryString("q=nonexistent12345");

        // When
        servlet.doGet(context.request(), context.response());

        // Then
        assertThat(context.response().getStatus()).isEqualTo(200);

        JsonObject result = parseJson(context.response().getOutputAsString());
        assertThat(result.getJsonArray("results")).isEmpty();
        assertThat(result.getInt("total")).isEqualTo(0);
    }

    @Test
    void testSearchValidatesInput() throws Exception {
        // Given
        SearchServlet servlet = context.registerInjectActivateService(new SearchServlet());
        context.request().setQueryString("q=<script>alert('xss')</script>");

        // When
        servlet.doGet(context.request(), context.response());

        // Then
        assertThat(context.response().getStatus()).isEqualTo(400);
    }
}
```

---

## 3. Third-Party Integration Testing

### 3.1 LLM Service Integration Testing

**Using WireMock:**

```java
@ExtendWith(WireMockExtension.class)
class LLMServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private LLMServiceImpl llmService;

    @BeforeEach
    void setUp() {
        LLMServiceConfig config = new LLMServiceConfig();
        config.setEndpoint(wireMock.baseUrl());
        config.setApiKey("test-api-key");
        config.setTimeoutSeconds(5);

        llmService = new LLMServiceImpl();
        llmService.activate(config);
    }

    @Test
    void testSuccessfulGeneration() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "choices": [{
                            "message": {
                                "content": "Generated content here"
                            }
                        }],
                        "usage": {
                            "total_tokens": 150
                        }
                    }
                    """)));

        // When
        LLMResponse response = llmService.generate("Test prompt");

        // Then
        assertThat(response.getContent()).isEqualTo("Generated content here");
        assertThat(response.getTokensUsed()).isEqualTo(150);

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
            .withHeader("Authorization", containing("Bearer test-api-key")));
    }

    @Test
    void testRateLimitHandling() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Retry-After", "60")));

        // When/Then
        assertThatThrownBy(() -> llmService.generate("Test prompt"))
            .isInstanceOf(RateLimitException.class)
            .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    void testTimeoutHandling() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
            .willReturn(aResponse()
                .withFixedDelay(10000))); // 10 second delay

        // When/Then
        assertThatThrownBy(() -> llmService.generate("Test prompt"))
            .isInstanceOf(LLMServiceException.class)
            .hasMessageContaining("timeout");
    }
}
```

### 3.2 Email Service Integration Testing

```java
@Testcontainers
class EmailServiceIntegrationTest {

    @Container
    static GenericContainer<?> mailhog = new GenericContainer<>("mailhog/mailhog:latest")
        .withExposedPorts(1025, 8025);

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        EmailServiceConfig config = new EmailServiceConfig();
        config.setSmtpHost(mailhog.getHost());
        config.setSmtpPort(mailhog.getMappedPort(1025));
        config.setFromAddress("noreply@example.com");

        emailService = new EmailServiceImpl();
        emailService.activate(config);
    }

    @Test
    void testSendEmail() throws Exception {
        // Given
        EmailRequest request = EmailRequest.builder()
            .to("user@example.com")
            .subject("Test Email")
            .body("This is a test email")
            .build();

        // When
        emailService.send(request);

        // Then - verify via MailHog API
        String mailhogUrl = "http://" + mailhog.getHost() + ":"
            + mailhog.getMappedPort(8025) + "/api/v2/messages";

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            JsonArray messages = fetchMailhogMessages(mailhogUrl);
            assertThat(messages.size()).isGreaterThan(0);
            assertThat(messages.getJsonObject(0).getString("Subject"))
                .isEqualTo("Test Email");
        });
    }
}
```

### 3.3 Analytics Integration Testing

```java
class AnalyticsServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension analyticsMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Test
    void testPageViewTracking() {
        // Given
        analyticsMock.stubFor(post(urlEqualTo("/collect"))
            .willReturn(aResponse().withStatus(204)));

        AnalyticsService service = createService(analyticsMock.baseUrl());

        // When
        service.trackPageView("/content/bmad-showcase/en/home", "user123");

        // Then
        analyticsMock.verify(postRequestedFor(urlEqualTo("/collect"))
            .withRequestBody(containing("t=pageview"))
            .withRequestBody(containing("dp=/content/bmad-showcase/en/home")));
    }

    @Test
    void testEventTracking() {
        // Given
        analyticsMock.stubFor(post(urlEqualTo("/collect"))
            .willReturn(aResponse().withStatus(204)));

        AnalyticsService service = createService(analyticsMock.baseUrl());

        // When
        service.trackEvent("button_click", "cta", "hero_learn_more");

        // Then
        analyticsMock.verify(postRequestedFor(urlEqualTo("/collect"))
            .withRequestBody(containing("t=event"))
            .withRequestBody(containing("ec=button_click"))
            .withRequestBody(containing("ea=cta")));
    }
}
```

---

## 4. Test Environment Setup

### 4.1 Local Integration Test Environment

**Docker Compose for Local Testing:**
```yaml
# docker-compose.test.yml
version: '3.8'

services:
  aem-author:
    image: adobe/aem-cs-sdk:latest
    ports:
      - "4502:4502"
    environment:
      - AEM_RUNMODE=author,local
    volumes:
      - ./target/aem-bmad-showcase.all-1.0.0-SNAPSHOT.zip:/opt/aem/install/package.zip

  aem-publish:
    image: adobe/aem-cs-sdk:latest
    ports:
      - "4503:4503"
    environment:
      - AEM_RUNMODE=publish,local
    depends_on:
      - aem-author

  dispatcher:
    image: adobe/aem-cs-dispatcher:latest
    ports:
      - "8080:80"
    volumes:
      - ./dispatcher/src:/mnt/dispatcher
    depends_on:
      - aem-publish

  wiremock:
    image: wiremock/wiremock:latest
    ports:
      - "8089:8080"
    volumes:
      - ./test/wiremock:/home/wiremock

  mailhog:
    image: mailhog/mailhog:latest
    ports:
      - "1025:1025"
      - "8025:8025"
```

### 4.2 Test Data Seeding

```java
public class TestDataSeeder {

    private final ResourceResolverFactory resolverFactory;

    public void seedTestContent() throws Exception {
        try (ResourceResolver resolver = getServiceResolver()) {
            // Create test content structure
            createPage(resolver, "/content/bmad-showcase/test", "Test Root");
            createPage(resolver, "/content/bmad-showcase/test/page1", "Test Page 1");
            createPage(resolver, "/content/bmad-showcase/test/page2", "Test Page 2");

            // Create test components
            createHeroComponent(resolver, "/content/bmad-showcase/test/page1/jcr:content/hero");
            createCarouselComponent(resolver, "/content/bmad-showcase/test/page1/jcr:content/carousel");

            // Create test assets
            uploadTestAsset(resolver, "/content/dam/bmad-showcase/test/image1.jpg");

            resolver.commit();
        }
    }

    public void cleanupTestContent() throws Exception {
        try (ResourceResolver resolver = getServiceResolver()) {
            Resource testRoot = resolver.getResource("/content/bmad-showcase/test");
            if (testRoot != null) {
                resolver.delete(testRoot);
                resolver.commit();
            }
        }
    }
}
```

### 4.3 Mock Service Configuration

```json
// wiremock/mappings/llm-success.json
{
    "request": {
        "method": "POST",
        "urlPattern": "/v1/chat/completions"
    },
    "response": {
        "status": 200,
        "headers": {
            "Content-Type": "application/json"
        },
        "jsonBody": {
            "id": "test-completion-123",
            "choices": [{
                "message": {
                    "role": "assistant",
                    "content": "{{randomValue type='SENTENCE'}}"
                }
            }],
            "usage": {
                "prompt_tokens": 50,
                "completion_tokens": 100,
                "total_tokens": 150
            }
        },
        "transformers": ["response-template"]
    }
}
```

---

## 5. CI/CD Pipeline Integration

### 5.1 Cloud Manager Pipeline Configuration

```yaml
# Pipeline Stages
stages:
  - build:
      - compile
      - unit-tests
      - integration-tests-mock  # Using mocked services
      - static-analysis

  - stage-deploy:
      - deploy-to-stage
      - integration-tests-stage  # Against real services
      - performance-tests
      - security-scan

  - production-deploy:
      - approval-gate
      - deploy-to-production
      - smoke-tests
```

### 5.2 Maven Integration Test Configuration

```xml
<!-- pom.xml -->
<profiles>
    <profile>
        <id>integration-tests</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <version>3.0.0</version>
                    <configuration>
                        <includes>
                            <include>**/*IT.java</include>
                            <include>**/*IntegrationTest.java</include>
                        </includes>
                        <systemPropertyVariables>
                            <aem.author.url>${aem.author.url}</aem.author.url>
                            <aem.publish.url>${aem.publish.url}</aem.publish.url>
                            <wiremock.url>${wiremock.url}</wiremock.url>
                        </systemPropertyVariables>
                    </configuration>
                    <executions>
                        <execution>
                            <goals>
                                <goal>integration-test</goal>
                                <goal>verify</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### 5.3 Integration Test Quality Gates

| Gate | Threshold | Action |
|------|-----------|--------|
| Test Pass Rate | 100% | Block deployment |
| Code Coverage | ≥ 70% | Warning |
| Integration Coverage | ≥ 80% | Block deployment |
| Test Duration | < 30 min | Warning |
| Flaky Test Rate | < 5% | Warning |

---

## 6. Test Data Management

### 6.1 Test Data Strategy

| Data Type | Approach | Refresh Frequency |
|-----------|----------|-------------------|
| **Static Content** | Version controlled JSON | Per release |
| **Dynamic Content** | Generated at runtime | Per test run |
| **User Data** | Anonymized production | Weekly |
| **Configuration** | Environment-specific | Per deployment |

### 6.2 Data Fixtures

```java
public class TestFixtures {

    public static HeroModel createHero() {
        return HeroModel.builder()
            .title("Test Hero Title")
            .description("Test description for hero component")
            .backgroundImage("/content/dam/test/hero-bg.jpg")
            .cta(createCTA())
            .build();
    }

    public static CardModel createCard() {
        return CardModel.builder()
            .title("Test Card")
            .description("Card description")
            .image("/content/dam/test/card.jpg")
            .link("/content/test/page")
            .build();
    }

    public static List<CardModel> createCardGrid(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createCard().toBuilder()
                .title("Card " + (i + 1))
                .build())
            .collect(Collectors.toList());
    }
}
```

### 6.3 Database State Management

```java
@ExtendWith(AemContextExtension.class)
class ContentRepositoryStateTest {

    @BeforeEach
    void setUp(AemContext context) {
        // Snapshot current state
        RepositorySnapshot.capture(context, "baseline");
    }

    @AfterEach
    void tearDown(AemContext context) {
        // Restore to baseline
        RepositorySnapshot.restore(context, "baseline");
    }

    @Test
    void testContentModification(AemContext context) {
        // Test can modify content freely
        // State will be restored after test
    }
}
```

---

## 7. API Contract Testing

### 7.1 Consumer-Driven Contract Testing

**Using Pact:**

```java
@ExtendWith(PactConsumerTestExt.class)
class ContentAPIContractTest {

    @Pact(consumer = "frontend", provider = "aem-content-api")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
            .given("page exists")
            .uponReceiving("a request for page content")
            .path("/content/bmad-showcase/en/home.model.json")
            .method("GET")
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(new PactDslJsonBody()
                .stringType("title", "Home")
                .stringType("description")
                .object("hero")
                    .stringType("title")
                    .stringType("backgroundImage")
                .closeObject()
                .array("components")
                    .object()
                        .stringType(":type")
                    .closeObject()
                .closeArray())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPact")
    void testContentAPI(MockServer mockServer) {
        ContentAPIClient client = new ContentAPIClient(mockServer.getUrl());

        PageContent content = client.getPageContent("/en/home");

        assertThat(content.getTitle()).isEqualTo("Home");
        assertThat(content.getHero()).isNotNull();
    }
}
```

### 7.2 Schema Validation

```java
class JSONSchemaValidationTest {

    @Test
    void testPageModelMatchesSchema() throws Exception {
        // Given
        String pageJson = fetchPageModel("/content/bmad-showcase/en/home.model.json");
        JsonSchema schema = loadSchema("/schemas/page-model-schema.json");

        // When/Then
        assertThatCode(() -> schema.validate(parseJson(pageJson)))
            .doesNotThrowAnyException();
    }

    @Test
    void testComponentExportMatchesSchema() throws Exception {
        // Given
        String heroJson = fetchComponentModel("/content/bmad-showcase/en/home/jcr:content/hero.model.json");
        JsonSchema schema = loadSchema("/schemas/hero-component-schema.json");

        // When/Then
        assertThatCode(() -> schema.validate(parseJson(heroJson)))
            .doesNotThrowAnyException();
    }
}
```

---

## 8. End-to-End Testing

### 8.1 E2E Test Framework

**Using Playwright:**

```java
class HomePageE2ETest {

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @BeforeEach
    void createPage() {
        page = browser.newPage();
    }

    @Test
    void testHomePageLoads() {
        // Navigate to home page
        page.navigate("https://stage.example.com/content/bmad-showcase/en/home.html");

        // Verify hero is visible
        assertThat(page.locator(".hero")).isVisible();
        assertThat(page.locator(".hero__title")).hasText("Welcome to BMAD");

        // Verify navigation
        assertThat(page.locator("nav")).isVisible();
        assertThat(page.locator("nav a")).hasCount(greaterThan(3));
    }

    @Test
    void testHeroCTANavigation() {
        page.navigate("https://stage.example.com/content/bmad-showcase/en/home.html");

        // Click CTA button
        page.locator(".hero__cta").click();

        // Verify navigation
        assertThat(page.url()).contains("/about");
    }

    @Test
    void testSearchFunctionality() {
        page.navigate("https://stage.example.com/content/bmad-showcase/en/home.html");

        // Open search
        page.locator("[data-testid='search-toggle']").click();

        // Type search query
        page.locator("[data-testid='search-input']").fill("BMAD methodology");

        // Submit search
        page.keyboard().press("Enter");

        // Verify results
        assertThat(page.locator(".search-results")).isVisible();
        assertThat(page.locator(".search-result-item")).hasCount(greaterThan(0));
    }

    @AfterEach
    void closePage() {
        page.close();
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }
}
```

### 8.2 Visual Regression Testing

```java
class VisualRegressionTest {

    @Test
    void testHomePageVisualRegression() {
        page.navigate("https://stage.example.com/content/bmad-showcase/en/home.html");

        // Wait for page to stabilize
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Take screenshot and compare
        assertThat(page).hasScreenshot("home-page.png", new ScreenshotOptions()
            .setMaxDiffPixelRatio(0.01));
    }

    @Test
    void testHeroComponentVariants() {
        String[] variants = {"default", "dark", "centered", "minimal"};

        for (String variant : variants) {
            page.navigate("https://stage.example.com/content/bmad-showcase/en/components/hero-" + variant + ".html");

            assertThat(page.locator(".hero")).hasScreenshot("hero-" + variant + ".png");
        }
    }
}
```

---

## Appendix A: Integration Test Checklist

**Before PR Merge:**
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] No new flaky tests introduced
- [ ] Test coverage meets threshold
- [ ] API contracts verified

**Before Stage Deployment:**
- [ ] Mock integration tests passing
- [ ] Contract tests verified
- [ ] Performance baseline met

**Before Production Deployment:**
- [ ] Stage E2E tests passing
- [ ] Visual regression approved
- [ ] Security scans clean
- [ ] Load test results acceptable

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-15 | QA Team | Initial version |

**Review Cycle:** Quarterly
**Next Review:** [Current Date + 3 months]
