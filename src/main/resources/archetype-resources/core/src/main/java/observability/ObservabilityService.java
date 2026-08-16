package ${package}.observability;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Small, dependency-free telemetry contract shared by generated form services. */
@Component(service = ObservabilityService.class)
public class ObservabilityService {
    private static final Logger LOG = LoggerFactory.getLogger(ObservabilityService.class);
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong submissions = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong statusPolls = new AtomicLong();

    public String beginRequest(String requested) {
        requests.incrementAndGet();
        String value = requested == null ? "" : requested.trim();
        return value.matches("[A-Za-z0-9._-]{1,64}") ? value : UUID.randomUUID().toString();
    }

    public void recordSubmission(String correlationId) {
        submissions.incrementAndGet();
        LOG.info("event=form.submission correlationId={}", correlationId);
    }

    public void recordStatusPoll(String correlationId) {
        statusPolls.incrementAndGet();
        LOG.debug("event=form.status_poll correlationId={}", correlationId);
    }

    public void recordFailure(String correlationId, String operation, Throwable error) {
        failures.incrementAndGet();
        LOG.warn("event=form.failure correlationId={} operation={} errorType={}", correlationId, operation,
                error == null ? "unknown" : error.getClass().getSimpleName());
    }

    public long getRequests() { return requests.get(); }
    public long getSubmissions() { return submissions.get(); }
    public long getFailures() { return failures.get(); }
    public long getStatusPolls() { return statusPolls.get(); }
}
