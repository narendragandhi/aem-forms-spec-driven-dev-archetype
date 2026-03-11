package ${package}.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import ${package}.services.AdobeSignOrchestrator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow process that orchestrates Adobe Sign and subsequent DoR generation.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=BMAD: Sign to DoR Orchestrator"
    }
)
public class SignToDoRProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(SignToDoRProcess.class);

    @Reference
    private AdobeSignOrchestrator signOrchestrator;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) 
            throws WorkflowException {
        
        String payload = workItem.getWorkflowData().getPayload().toString();
        MetaDataMap wfMetadata = workItem.getWorkflow().getMetaDataMap();
        String agreementId = wfMetadata.get("adobeSignAgreementId", String.class);

        if (agreementId == null) {
            agreementId = signOrchestrator.createAgreement(payload);
            wfMetadata.put("adobeSignAgreementId", agreementId);
            wfMetadata.put("signingStatus", "OUT_FOR_SIGNATURE");
        } else {
            String status = signOrchestrator.getStatus(agreementId);
            wfMetadata.put("signingStatus", status);
            
            if ("SIGNED".equals(status)) {
                generateDoR(payload);
                wfMetadata.put("dorStatus", "GENERATED");
            }
        }
    }

    private void generateDoR(String data) {
        LOG.info("MOCK DoR GENERATION: Creating PDF Document of Record for data...");
    }
}
