package ${package}.services;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to handle post-processing of AEM Forms submissions.
 *
 * This service can be invoked from a workflow process step after a user submits an Adaptive Form.
 * It demonstrates how to integrate with external systems via a REST API, with configurable endpoints.
 */
@Component(service = FormSubmissionService.class, immediate = true)
@Designate(ocd = FormSubmissionService.Config.class)
public class FormSubmissionService {

    private static final Logger LOG = LoggerFactory.getLogger(FormSubmissionService.class);

    @ObjectClassDefinition(
        name = "AEM Forms Submission Service Configuration",
        description = "Configuration for external API integration in AEM Forms submission post-processing."
    )
    public @interface Config {
        @AttributeDefinition(
            name = "Submission API Endpoint",
            description = "The URL of the external REST API to send form data to.",
            type = AttributeType.STRING
        )
        String submission_api_endpoint() default "http://localhost:8080/api/submit-form";

        @AttributeDefinition(
            name = "API Key/Token (Optional)",
            description = "API key or token for authentication with the external service.",
            type = AttributeType.STRING
        )
        String api_key() default "";
    }

    private String submissionApiEndpoint;
    private String apiKey;

    @Activate
    protected void activate(final Config config) {
        this.submissionApiEndpoint = config.submission_api_endpoint();
        this.apiKey = config.api_key();
        LOG.info("FormSubmissionService activated with endpoint: {} and API Key present: {}",
            submissionApiEndpoint, !apiKey.isEmpty());
    }

    /**
     * Processes the submitted form data by sending it to a configured external REST API.
     *
     * @param formDataJson The JSON data submitted from the Adaptive Form.
     * @param formIdentifier A unique identifier for the form that was submitted.
     */
    public void processSubmission(String formDataJson, String formIdentifier) {
        LOG.info("Processing submission for form: {}", formIdentifier);
        LOG.debug("Received form data: {}", formDataJson);

        try {
            LOG.info("Attempting to send data to external API: {}", submissionApiEndpoint);

            // --- REST API Integration Example (Placeholder) ---
            // In a real scenario, you would use an HTTP client library (e.g., Apache HttpClient, OkHttp, Java 11+ HttpClient)
            // to send the formDataJson to the submissionApiEndpoint.

            // Example using a conceptual HttpClient:
            // HttpClient httpClient = HttpClient.newBuilder().build();
            // HttpRequest request = HttpRequest.newBuilder()
            //     .uri(URI.create(submissionApiEndpoint))
            //     .header("Content-Type", "application/json")
            //     .header("Authorization", "Bearer " + apiKey) // If API key is used
            //     .POST(HttpRequest.BodyPublishers.ofString(formDataJson))
            //     .build();
            //
            // HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            //
            // if (response.statusCode() == 200) {
            //     LOG.info("Form data successfully sent to external API for {}. Response: {}", formIdentifier, response.body());
            // } else {
            //     LOG.warn("Failed to send form data to external API for {}. Status: {}, Response: {}", formIdentifier, response.statusCode(), response.body());
            //     // Depending on requirements, throw an exception to fail the workflow step
            // }

            // Simulate a successful API call
            Thread.sleep(1500);
            LOG.info("Simulated successful data submission to external API for form: {}", formIdentifier);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Form submission to external API interrupted for {}: {}", formIdentifier, e.getMessage());
            // Propagate as a runtime exception or custom WorkflowException if used in a workflow
            throw new RuntimeException("Form submission to external API interrupted.", e);
        } catch (Exception e) {
            LOG.error("Failed to send form data to external API for {}: {}", formIdentifier, e.getMessage());
            // Propagate as a runtime exception or custom WorkflowException if used in a workflow
            throw new RuntimeException("Failed to send form data to external API.", e);
        }
    }
}
