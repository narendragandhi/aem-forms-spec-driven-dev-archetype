package ${package}.workflows;

import ${package}.services.AdobeSignOrchestrator;
import ${package}.services.FormSubmissionService;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Reference
    private FormSubmissionService formSubmissionService;

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

    private void generateDoR(String payload) {
        LOG.info("Generating Document of Record for payload: {}", payload);
        formSubmissionService.processSubmission(payload, "sign-to-dor-workflow");
    }
}
