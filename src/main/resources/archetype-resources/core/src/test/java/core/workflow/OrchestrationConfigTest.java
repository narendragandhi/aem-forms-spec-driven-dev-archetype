package ${package}.core.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrchestrationConfigTest {

    private OrchestrationConfig orchestrationConfig;

    @BeforeEach
    void setUp() {
        orchestrationConfig = new OrchestrationConfig();
    }

    @Test
    void testActivateWithDefaultConfig() throws Exception {
        OrchestrationConfig.Config config = createMockConfig("dev", "http://localhost:8080/orchestration/webhook", false);

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertEquals("dev", orchestrationConfig.getEnvironment());
        assertEquals("http://localhost:8080/orchestration/webhook", orchestrationConfig.getWebhookUrl());
        assertFalse(orchestrationConfig.isFeatureFlagAEnabled());
    }

    @Test
    void testActivateWithProductionConfig() throws Exception {
        OrchestrationConfig.Config config = createMockConfig(
            "prod",
            "https://production-orchestrator.example.com/webhook",
            true
        );

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertEquals("prod", orchestrationConfig.getEnvironment());
        assertEquals("https://production-orchestrator.example.com/webhook", orchestrationConfig.getWebhookUrl());
        assertTrue(orchestrationConfig.isFeatureFlagAEnabled());
    }

    @Test
    void testActivateWithStagingConfig() throws Exception {
        OrchestrationConfig.Config config = createMockConfig(
            "stage",
            "https://staging-orchestrator.example.com/webhook",
            true
        );

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertEquals("stage", orchestrationConfig.getEnvironment());
        assertTrue(orchestrationConfig.isFeatureFlagAEnabled());
    }

    @Test
    void testGetEnvironmentReturnsConfiguredValue() throws Exception {
        OrchestrationConfig.Config config = createMockConfig("test-env", "http://test", false);

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertEquals("test-env", orchestrationConfig.getEnvironment());
    }

    @Test
    void testGetWebhookUrlReturnsConfiguredValue() throws Exception {
        String customUrl = "https://custom-webhook.example.org/notify";
        OrchestrationConfig.Config config = createMockConfig("dev", customUrl, false);

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertEquals(customUrl, orchestrationConfig.getWebhookUrl());
    }

    @Test
    void testFeatureFlagEnabled() throws Exception {
        OrchestrationConfig.Config config = createMockConfig("dev", "http://test", true);

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertTrue(orchestrationConfig.isFeatureFlagAEnabled());
    }

    @Test
    void testFeatureFlagDisabled() throws Exception {
        OrchestrationConfig.Config config = createMockConfig("dev", "http://test", false);

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertFalse(orchestrationConfig.isFeatureFlagAEnabled());
    }

    @Test
    void testModifyConfiguration() throws Exception {
        // First activation
        OrchestrationConfig.Config initialConfig = createMockConfig("dev", "http://initial", false);
        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, initialConfig);

        assertEquals("dev", orchestrationConfig.getEnvironment());

        // Modify configuration (simulating @Modified)
        OrchestrationConfig.Config modifiedConfig = createMockConfig("prod", "http://modified", true);
        activateMethod.invoke(orchestrationConfig, modifiedConfig);

        assertEquals("prod", orchestrationConfig.getEnvironment());
        assertEquals("http://modified", orchestrationConfig.getWebhookUrl());
        assertTrue(orchestrationConfig.isFeatureFlagAEnabled());
    }

    @Test
    void testConfigWithEmptyValues() throws Exception {
        OrchestrationConfig.Config config = createMockConfig("", "", false);

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertEquals("", orchestrationConfig.getEnvironment());
        assertEquals("", orchestrationConfig.getWebhookUrl());
    }

    @Test
    void testConfigWithSpecialCharactersInWebhookUrl() throws Exception {
        String specialUrl = "https://example.com/webhook?env=dev&token=abc123";
        OrchestrationConfig.Config config = createMockConfig("dev", specialUrl, false);

        Method activateMethod = OrchestrationConfig.class.getDeclaredMethod("activate", OrchestrationConfig.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(orchestrationConfig, config);

        assertEquals(specialUrl, orchestrationConfig.getWebhookUrl());
    }

    /**
     * Helper method to create a mock Config annotation proxy
     */
    private OrchestrationConfig.Config createMockConfig(String environment, String webhookUrl, boolean featureFlagAEnabled) {
        return new OrchestrationConfig.Config() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return OrchestrationConfig.Config.class;
            }

            @Override
            public String environment() {
                return environment;
            }

            @Override
            public String webhookUrl() {
                return webhookUrl;
            }

            @Override
            public boolean featureFlagAEnabled() {
                return featureFlagAEnabled;
            }
        };
    }
}
