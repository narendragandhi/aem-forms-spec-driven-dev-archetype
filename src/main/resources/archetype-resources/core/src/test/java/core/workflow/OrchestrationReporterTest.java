package ${package}.core.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestrationReporterTest {

    private OrchestrationReporter orchestrationReporter;

    @Mock
    private OrchestrationConfig orchestrationConfig;

    private TestLogger logger = TestLoggerFactory.getTestLogger(OrchestrationReporter.class);

    @BeforeEach
    void setUp() throws Exception {
        TestLoggerFactory.clear();
        orchestrationReporter = new OrchestrationReporter();

        // Inject the mock OrchestrationConfig using reflection
        Field configField = OrchestrationReporter.class.getDeclaredField("orchestrationConfig");
        configField.setAccessible(true);
        configField.set(orchestrationReporter, orchestrationConfig);
    }

    @Test
    void testReportStatusLogsEvent() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://localhost:8080/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        orchestrationReporter.reportStatus("TestEvent", "Test details here");

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());

        LoggingEvent event = events.get(0);
        assertEquals(Level.INFO, event.getLevel());
        assertTrue(event.getMessage().contains("Reporting event"));
    }

    @Test
    void testReportStatusIncludesEventName() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("test");

        String eventName = "DeploymentComplete";
        orchestrationReporter.reportStatus(eventName, "Deployment finished");

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.get(0).getArguments().toString().contains(eventName));
    }

    @Test
    void testReportStatusIncludesDetails() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("test");

        String details = "Build version 1.2.3 deployed successfully";
        orchestrationReporter.reportStatus("BuildComplete", details);

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.get(0).getArguments().toString().contains(details));
    }

    @Test
    void testReportStatusIncludesEnvironment() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("production");

        orchestrationReporter.reportStatus("TestEvent", "details");

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.get(0).getArguments().toString().contains("production"));
    }

    @Test
    void testReportStatusIncludesWebhookUrl() {
        String webhookUrl = "https://orchestrator.example.com/notify";
        when(orchestrationConfig.getWebhookUrl()).thenReturn(webhookUrl);
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        orchestrationReporter.reportStatus("TestEvent", "details");

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.get(0).getArguments().toString().contains(webhookUrl));
    }

    @Test
    void testReportSpecGenerationOutcomeSuccess() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        orchestrationReporter.reportSpecGenerationOutcome("spec-123", true, "Generation completed");

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());

        String logContent = events.get(0).getArguments().toString();
        assertTrue(logContent.contains("spec-123"));
        assertTrue(logContent.contains("SUCCESS"));
        assertTrue(logContent.contains("Generation completed"));
    }

    @Test
    void testReportSpecGenerationOutcomeFailure() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        orchestrationReporter.reportSpecGenerationOutcome("spec-456", false, "Parsing error occurred");

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());

        String logContent = events.get(0).getArguments().toString();
        assertTrue(logContent.contains("spec-456"));
        assertTrue(logContent.contains("FAILURE"));
        assertTrue(logContent.contains("Parsing error occurred"));
    }

    @Test
    void testReportSpecGenerationOutcomeCallsReportStatus() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        orchestrationReporter.reportSpecGenerationOutcome("spec-789", true, "OK");

        List<LoggingEvent> events = logger.getLoggingEvents();
        // reportSpecGenerationOutcome internally calls reportStatus
        assertTrue(events.get(0).getArguments().toString().contains("SpecGeneration"));
    }

    @Test
    void testReportStatusWithEmptyEvent() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        assertDoesNotThrow(() -> orchestrationReporter.reportStatus("", ""));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
    }

    @Test
    void testReportStatusWithNullValues() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn(null);
        when(orchestrationConfig.getEnvironment()).thenReturn(null);

        assertDoesNotThrow(() -> orchestrationReporter.reportStatus(null, null));
    }

    @Test
    void testReportSpecGenerationOutcomeWithEmptySpecId() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        assertDoesNotThrow(() ->
            orchestrationReporter.reportSpecGenerationOutcome("", true, "Empty spec ID test")
        );
    }

    @Test
    void testReportStatusMultipleTimes() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        orchestrationReporter.reportStatus("Event1", "Details1");
        orchestrationReporter.reportStatus("Event2", "Details2");
        orchestrationReporter.reportStatus("Event3", "Details3");

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(3, events.size());
    }

    @Test
    void testOrchestrationConfigInteraction() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        orchestrationReporter.reportStatus("TestEvent", "TestDetails");

        verify(orchestrationConfig).getWebhookUrl();
        verify(orchestrationConfig).getEnvironment();
    }

    @Test
    void testReportSpecGenerationWithLongMessage() {
        when(orchestrationConfig.getWebhookUrl()).thenReturn("http://test/webhook");
        when(orchestrationConfig.getEnvironment()).thenReturn("dev");

        String longMessage = "A".repeat(1000);
        assertDoesNotThrow(() ->
            orchestrationReporter.reportSpecGenerationOutcome("spec-long", true, longMessage)
        );
    }
}
