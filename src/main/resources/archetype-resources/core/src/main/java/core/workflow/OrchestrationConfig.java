package ${package}.core.workflow;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import org.osgi.service.component.annotations.Component;

@Component(service = OrchestrationConfig.class, immediate = true)
@Designate(ocd = OrchestrationConfig.Config.class)
public class OrchestrationConfig {

    @ObjectClassDefinition(name = "Orchestration Configuration",
                           description = "Configuration managed by an external orchestration system (e.g., GasTown).")
    public @interface Config {
        @AttributeDefinition(name = "Deployment Environment",
                             description = "The target deployment environment (e.g., dev, stage, prod).",
                             type = AttributeType.STRING)
        String environment() default "dev";

        @AttributeDefinition(name = "Webhook URL for Notifications",
                             description = "URL to send status updates or notifications to the orchestration system.",
                             type = AttributeType.STRING)
        String webhookUrl() default "http://localhost:8080/orchestration/webhook";

        @AttributeDefinition(name = "Feature Flag A (Example)",
                             description = "Example feature flag controlled by orchestration.",
                             type = AttributeType.BOOLEAN)
        boolean featureFlagAEnabled() default false;
    }

    private String environment;
    private String webhookUrl;
    private boolean featureFlagAEnabled;

    @org.osgi.service.component.annotations.Activate
    @org.osgi.service.component.annotations.Modified
    protected void activate(Config config) {
        this.environment = config.environment();
        this.webhookUrl = config.webhookUrl();
        this.featureFlagAEnabled = config.featureFlagAEnabled();
    }

    public String getEnvironment() {
        return environment;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public boolean isFeatureFlagAEnabled() {
        return featureFlagAEnabled;
    }
}
