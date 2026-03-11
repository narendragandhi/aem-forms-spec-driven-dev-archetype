package ${package}.services.impl;

import ${package}.services.AdobeSignOrchestrator;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component(service = AdobeSignOrchestrator.class)
public class AdobeSignOrchestratorImpl implements AdobeSignOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AdobeSignOrchestratorImpl.class);
    
    private final Map<String, String> agreementStore = new ConcurrentHashMap<>();

    @Override
    public String createAgreement(String data) {
        String agreementId = "SIGN-" + UUID.randomUUID().toString();
        LOG.info("Creating mock Adobe Sign agreement: {} for data", agreementId);
        agreementStore.put(agreementId, "OUT_FOR_SIGNATURE");
        return agreementId;
    }

    @Override
    public String getStatus(String agreementId) {
        String currentStatus = agreementStore.getOrDefault(agreementId, "NOT_FOUND");
        if ("OUT_FOR_SIGNATURE".equals(currentStatus) && Math.random() > 0.7) {
            agreementStore.put(agreementId, "SIGNED");
        }
        return agreementStore.getOrDefault(agreementId, "NOT_FOUND");
    }
}
