package ${package}.core.workflow;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = OrchestrationReporter.class, immediate = true)
public class OrchestrationReporter {

    private static final Logger LOG = LoggerFactory.getLogger(OrchestrationReporter.class);

    @Reference
    private OrchestrationConfig orchestrationConfig;

    public void reportStatus(String event, String details) {
        String webhookUrl = orchestrationConfig.getWebhookUrl();
        String environment = orchestrationConfig.getEnvironment();

        LOG.info("Reporting event '{}' with details '{}' to orchestrator (Environment: {}, Webhook: {})",
                event, details, environment, webhookUrl);

        // TODO: POST {"event": event, "details": details, "env": environment} to webhookUrl
        // using Java 11 HttpClient, Apache HttpClient, or OkHttp.
    }

    public void reportSpecGenerationOutcome(String specId, boolean success, String message) {
        String outcome = success ? "SUCCESS" : "FAILURE";
        reportStatus("SpecGeneration",
                String.format("Spec ID: %s, Outcome: %s, Message: %s", specId, outcome, message));
    }
}
