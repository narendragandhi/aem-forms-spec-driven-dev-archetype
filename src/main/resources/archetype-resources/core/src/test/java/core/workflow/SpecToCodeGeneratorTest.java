package ${package}.core.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpecToCodeGeneratorTest {

    private SpecToCodeGenerator specToCodeGenerator;

    private TestLogger logger = TestLoggerFactory.getTestLogger(SpecToCodeGenerator.class);

    @BeforeEach
    void setUp() {
        TestLoggerFactory.clear();
        specToCodeGenerator = new SpecToCodeGenerator();
    }

    @Test
    void testGenerateLogsSpecPath() {
        String specPath = "/content/specs/form-spec.json";
        String outputPath = "/apps/generated/components";

        specToCodeGenerator.generate(specPath, outputPath);

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.size() >= 1, "Should have log events");

        boolean hasSpecPathLog = events.stream()
            .anyMatch(e -> e.getMessage().contains("Invoking Spec-to-Code Generator") &&
                          e.getArguments().toString().contains(specPath));
        assertTrue(hasSpecPathLog, "Should log the spec path");
    }

    @Test
    void testGenerateLogsOutputPath() {
        String specPath = "/content/specs/component-spec.json";
        String outputPath = "/apps/myproject/components/newcomponent";

        specToCodeGenerator.generate(specPath, outputPath);

        List<LoggingEvent> events = logger.getLoggingEvents();

        boolean hasOutputPathLog = events.stream()
            .anyMatch(e -> e.getArguments().toString().contains(outputPath));
        assertTrue(hasOutputPathLog, "Should log the output path");
    }

    @Test
    void testGenerateLogsCompletionMessage() {
        String specPath = "/content/specs/test-spec.json";
        String outputPath = "/apps/test/output";

        specToCodeGenerator.generate(specPath, outputPath);

        List<LoggingEvent> events = logger.getLoggingEvents();

        boolean hasCompletionLog = events.stream()
            .anyMatch(e -> e.getMessage().contains("Spec-to-Code Generation (conceptual) completed"));
        assertTrue(hasCompletionLog, "Should log completion message");
    }

    @Test
    void testGenerateWithEmptyPaths() {
        specToCodeGenerator.generate("", "");

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(2, events.size(), "Should have two log events (start and completion)");
    }

    @Test
    void testGenerateWithNullPaths() {
        // Should handle null paths gracefully (logging them as null)
        assertDoesNotThrow(() -> specToCodeGenerator.generate(null, null));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.size() >= 1, "Should still log events even with null paths");
    }

    @Test
    void testGenerateLogsAtInfoLevel() {
        specToCodeGenerator.generate("/spec/path", "/output/path");

        List<LoggingEvent> events = logger.getLoggingEvents();

        boolean allInfoLevel = events.stream()
            .allMatch(e -> e.getLevel() == Level.INFO);
        assertTrue(allInfoLevel, "All log messages should be at INFO level");
    }

    @Test
    void testGenerateWithSpecialCharactersInPath() {
        String specPath = "/content/specs/form-spec (1).json";
        String outputPath = "/apps/my-project/components/new_component";

        assertDoesNotThrow(() -> specToCodeGenerator.generate(specPath, outputPath));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.size() >= 1);
    }

    @Test
    void testGenerateWithLongPaths() {
        String specPath = "/content/" + "nested/".repeat(20) + "spec.json";
        String outputPath = "/apps/" + "deep/".repeat(20) + "component";

        assertDoesNotThrow(() -> specToCodeGenerator.generate(specPath, outputPath));
    }

    @Test
    void testGenerateMultipleTimes() {
        specToCodeGenerator.generate("/spec1", "/output1");
        specToCodeGenerator.generate("/spec2", "/output2");
        specToCodeGenerator.generate("/spec3", "/output3");

        List<LoggingEvent> events = logger.getLoggingEvents();
        // Each generate call should produce 2 log events (start + completion)
        assertEquals(6, events.size(), "Should have 6 log events for 3 generate calls");
    }
}
