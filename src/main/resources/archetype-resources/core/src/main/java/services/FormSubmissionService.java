package ${package}.services;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Dispatches a form submission to a configured external REST API - real
 * HTTP call, same java.net.http.HttpClient pattern used by
 * AdobeSignOrchestratorImpl/InteractiveCommunicationServiceImpl.
 *
 * This service can be invoked from a workflow process step (or, as wired
 * in this archetype, from HeadlessSubmitServlet) after a user submits an
 * Adaptive Form.
 */
@Component(service = FormSubmissionService.class)
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
            description = "Bearer token for authentication with the external service.",
            type = AttributeType.PASSWORD
        )
        String api_key() default "";
    }

    private String submissionApiEndpoint;
    private String apiKey;

    HttpClient httpClient = HttpClient.newHttpClient(); // package-private so unit tests can substitute a mock

    @Activate
    protected void activate(final Config config) {
        this.submissionApiEndpoint = config.submission_api_endpoint();
        this.apiKey = config.api_key();
        LOG.info("FormSubmissionService activated with endpoint: {} and API Key present: {}",
            submissionApiEndpoint, !apiKey.isEmpty());
    }

    /**
     * Processes the submitted form data by sending it to the configured
     * external REST API.
     *
     * @param formDataJson The JSON data submitted from the Adaptive Form.
     * @param formIdentifier A unique identifier for the form that was submitted.
     */
    public void processSubmission(String formDataJson, String formIdentifier) throws FormSubmissionException {
        LOG.info("Processing submission for form: {}", formIdentifier);
        LOG.debug("Received form data: {}", formDataJson);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(submissionApiEndpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(formDataJson != null ? formDataJson : "{}", StandardCharsets.UTF_8));
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FormSubmissionException("Submission API returned HTTP " + response.statusCode()
                        + " for form " + formIdentifier + ": " + response.body());
            }
        } catch (IOException e) {
            throw new FormSubmissionException("Failed to reach submission API for form " + formIdentifier
                    + " at " + submissionApiEndpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FormSubmissionException("Interrupted while submitting form " + formIdentifier, e);
        }

        LOG.info("Form data dispatched to external API for form: {}", formIdentifier);
    }
}
