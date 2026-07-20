package ${package}.services.impl;

import ${package}.services.AdobeSignOrchestrator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = AdobeSignOrchestrator.class)
public class AdobeSignOrchestratorImpl implements AdobeSignOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AdobeSignOrchestratorImpl.class);
    long signingTimeoutMs = 30_000L; // package-private so unit tests can override

    private final Map<String, String> agreementStore = new ConcurrentHashMap<>();
    private final Map<String, Long> creationTimes = new ConcurrentHashMap<>();

    @Override
    public String createAgreement(String data) {
        String agreementId = "SIGN-" + UUID.randomUUID().toString();
        LOG.info("Creating Adobe Sign agreement: {}", agreementId);
        agreementStore.put(agreementId, "OUT_FOR_SIGNATURE");
        creationTimes.put(agreementId, System.currentTimeMillis());
        return agreementId;
    }

    @Deactivate
    protected void deactivate() {
        agreementStore.clear();
        creationTimes.clear();
    }

    @Override
    public String getStatus(String agreementId) {
        if (!agreementStore.containsKey(agreementId)) {
            return "NOT_FOUND";
        }
        if ("OUT_FOR_SIGNATURE".equals(agreementStore.get(agreementId))) {
            long elapsed = System.currentTimeMillis() - creationTimes.getOrDefault(agreementId, 0L);
            if (elapsed >= signingTimeoutMs) {
                agreementStore.put(agreementId, "SIGNED");
            }
        }
        return agreementStore.get(agreementId);
    }
}
