package ${package}.workflows;

import ${package}.services.AdobeSignOrchestrator;
import ${package}.services.FormSubmissionService;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignToDoRProcessTest {

    private SignToDoRProcess process;

    @Mock private AdobeSignOrchestrator signOrchestrator;
    @Mock private FormSubmissionService formSubmissionService;
    @Mock private WorkItem workItem;
    @Mock private WorkflowSession workflowSession;
    @Mock private MetaDataMap processArgs;
    @Mock private WorkflowData workflowData;
    @Mock private Workflow workflow;
    @Mock private MetaDataMap wfMetadata;

    @BeforeEach
    void setUp() throws Exception {
        process = new SignToDoRProcess();
        Field field = SignToDoRProcess.class.getDeclaredField("signOrchestrator");
        field.setAccessible(true);
        field.set(process, signOrchestrator);

        Field fssField = SignToDoRProcess.class.getDeclaredField("formSubmissionService");
        fssField.setAccessible(true);
        fssField.set(process, formSubmissionService);

        doNothing().when(formSubmissionService).processSubmission(any(), any());

        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn("/content/forms/data/payload");
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflow.getMetaDataMap()).thenReturn(wfMetadata);
    }

    @Test
    void testExecuteCreatesAgreementWhenNoneExists() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(signOrchestrator.createAgreement(any())).thenReturn("SIGN-new-123");

        process.execute(workItem, workflowSession, processArgs);

        verify(signOrchestrator).createAgreement("/content/forms/data/payload");
    }

    @Test
    void testExecuteStoresAgreementIdInMetadata() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(signOrchestrator.createAgreement(any())).thenReturn("SIGN-abc-456");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata).put("adobeSignAgreementId", "SIGN-abc-456");
    }

    @Test
    void testExecuteSetsSigningStatusOutForSignatureOnFirstCall() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(signOrchestrator.createAgreement(any())).thenReturn("SIGN-abc-456");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata).put("signingStatus", "OUT_FOR_SIGNATURE");
    }

    @Test
    void testExecuteChecksStatusWhenAgreementAlreadyExists() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("OUT_FOR_SIGNATURE");

        process.execute(workItem, workflowSession, processArgs);

        verify(signOrchestrator).getStatus("SIGN-existing");
        verify(signOrchestrator, never()).createAgreement(any());
    }

    @Test
    void testExecuteUpdatesSigningStatusOnSubsequentCall() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("OUT_FOR_SIGNATURE");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata).put("signingStatus", "OUT_FOR_SIGNATURE");
    }

    @Test
    void testExecuteGeneratesDoRWhenSigned() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("SIGNED");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata).put("dorStatus", "GENERATED");
    }

    @Test
    void testExecuteDoesNotSetDorStatusWhenNotSigned() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("OUT_FOR_SIGNATURE");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata, never()).put(eq("dorStatus"), any());
    }

    @Test
    void testExecuteDoesNotThrowOnValidInputs() {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(signOrchestrator.createAgreement(any())).thenReturn("SIGN-no-throw");

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));
    }

    @Test
    void testExecuteSetsSigningStatusFromOrchestratorOnSecondCall() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("SIGNED");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata).put("signingStatus", "SIGNED");
    }
}
