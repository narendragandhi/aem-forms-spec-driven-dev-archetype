package ${package}.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FormSubmissionServiceTest {

    private FormSubmissionService formSubmissionService;

    private TestLogger logger = TestLoggerFactory.getTestLogger(FormSubmissionService.class);

    @BeforeEach
    void setUp() {
        TestLoggerFactory.clear();
        formSubmissionService = new FormSubmissionService();
    }

    @Test
    void testActivateWithDefaultConfig() throws Exception {
        // Create a mock config with default values
        FormSubmissionService.Config config = createMockConfig(
            "http://localhost:8080/api/submit-form",
            ""
        );

        // Call activate method using reflection
        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);

        // Verify logging occurred
        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.size() >= 1, "Should have at least one log event");

        LoggingEvent activationEvent = events.stream()
            .filter(e -> e.getMessage().contains("FormSubmissionService activated"))
            .findFirst()
            .orElse(null);

        assertNotNull(activationEvent, "Activation log message should be present");
        assertEquals(Level.INFO, activationEvent.getLevel());
    }

    @Test
    void testActivateWithApiKey() throws Exception {
        FormSubmissionService.Config config = createMockConfig(
            "https://api.example.com/forms",
            "test-api-key-123"
        );

        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.size() >= 1);

        // Verify that API key presence is logged correctly (should show true)
        LoggingEvent activationEvent = events.stream()
            .filter(e -> e.getMessage().contains("FormSubmissionService activated"))
            .findFirst()
            .orElse(null);

        assertNotNull(activationEvent);
        assertTrue(activationEvent.getArguments().toString().contains("true"));
    }

    @Test
    void testProcessSubmissionLogsFormIdentifier() throws Exception {
        // Activate service first
        FormSubmissionService.Config config = createMockConfig(
            "http://localhost:8080/api/submit-form",
            ""
        );

        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);

        TestLoggerFactory.clear();

        String formDataJson = "{\"name\": \"John Doe\", \"email\": \"john@example.com\"}";
        String formIdentifier = "registration-form-001";

        // Process submission - this will simulate a 1500ms delay
        formSubmissionService.processSubmission(formDataJson, formIdentifier);

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.size() >= 1, "Should have log events for processing");

        // Verify form identifier is logged
        boolean hasFormIdentifierLog = events.stream()
            .anyMatch(e -> e.getMessage().contains("Processing submission") &&
                          e.getArguments().toString().contains(formIdentifier));
        assertTrue(hasFormIdentifierLog, "Should log form identifier");
    }

    @Test
    void testProcessSubmissionLogsFormData() throws Exception {
        FormSubmissionService.Config config = createMockConfig(
            "http://localhost:8080/api/submit-form",
            ""
        );

        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);

        TestLoggerFactory.clear();

        String formDataJson = "{\"firstName\": \"Jane\", \"lastName\": \"Smith\"}";
        String formIdentifier = "contact-form-002";

        formSubmissionService.processSubmission(formDataJson, formIdentifier);

        List<LoggingEvent> events = logger.getLoggingEvents();

        // Verify form data is logged at DEBUG level
        boolean hasFormDataLog = events.stream()
            .anyMatch(e -> e.getLevel() == Level.DEBUG &&
                          e.getMessage().contains("Received form data"));
        assertTrue(hasFormDataLog, "Should log form data at DEBUG level");
    }

    @Test
    void testProcessSubmissionLogsEndpointAttempt() throws Exception {
        String customEndpoint = "https://custom-api.example.com/submit";
        FormSubmissionService.Config config = createMockConfig(customEndpoint, "");

        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);

        TestLoggerFactory.clear();

        formSubmissionService.processSubmission("{}", "test-form");

        List<LoggingEvent> events = logger.getLoggingEvents();

        // Verify endpoint is logged
        boolean hasEndpointLog = events.stream()
            .anyMatch(e -> e.getMessage().contains("Attempting to send data") &&
                          e.getArguments().toString().contains(customEndpoint));
        assertTrue(hasEndpointLog, "Should log the API endpoint");
    }

    @Test
    void testProcessSubmissionSuccess() throws Exception {
        FormSubmissionService.Config config = createMockConfig(
            "http://localhost:8080/api/submit-form",
            ""
        );

        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);

        TestLoggerFactory.clear();

        String formDataJson = "{\"test\": \"data\"}";
        String formIdentifier = "success-test-form";

        // This should complete successfully (with simulated delay)
        assertDoesNotThrow(() ->
            formSubmissionService.processSubmission(formDataJson, formIdentifier)
        );

        List<LoggingEvent> events = logger.getLoggingEvents();

        // Verify success message is logged
        boolean hasSuccessLog = events.stream()
            .anyMatch(e -> e.getMessage().contains("Simulated successful data submission"));
        assertTrue(hasSuccessLog, "Should log successful submission");
    }

    @Test
    void testProcessSubmissionWithEmptyFormData() throws Exception {
        FormSubmissionService.Config config = createMockConfig(
            "http://localhost:8080/api/submit-form",
            ""
        );

        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);

        // Should handle empty JSON gracefully
        assertDoesNotThrow(() ->
            formSubmissionService.processSubmission("{}", "empty-form")
        );
    }

    @Test
    void testProcessSubmissionWithNullFormData() throws Exception {
        FormSubmissionService.Config config = createMockConfig(
            "http://localhost:8080/api/submit-form",
            ""
        );

        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);

        // Should handle null form data (may log null in debug)
        assertDoesNotThrow(() ->
            formSubmissionService.processSubmission(null, "null-data-form")
        );
    }

    /**
     * Helper method to create a mock Config annotation proxy
     */
    private FormSubmissionService.Config createMockConfig(String endpoint, String apiKey) {
        return new FormSubmissionService.Config() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return FormSubmissionService.Config.class;
            }

            @Override
            public String submission_api_endpoint() {
                return endpoint;
            }

            @Override
            public String api_key() {
                return apiKey;
            }
        };
    }
}
