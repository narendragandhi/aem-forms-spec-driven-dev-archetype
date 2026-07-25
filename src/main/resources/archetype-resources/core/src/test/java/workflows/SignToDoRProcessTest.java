package ${package}.workflows;

import ${package}.services.AdobeSignException;
import ${package}.services.AdobeSignOrchestrator;
import com.adobe.aemds.guide.addon.dor.DoRGenerationException;
import com.adobe.aemds.guide.addon.dor.DoROptions;
import com.adobe.aemds.guide.addon.dor.DoRResult;
import com.adobe.aemds.guide.addon.dor.DoRService;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignToDoRProcessTest {

    private static final String ADAPTIVE_FORM_PATH = "/content/forms/af/AcmeApp/financial-application";
    private static final String DOR_STORAGE_PATH = "/content/dam/formsanddocuments/generated-dor/AcmeApp";
    private static final String PAYLOAD_JSON = "{\"firstName\":\"Jane\",\"email\":\"signer@example.com\"}";

    private SignToDoRProcess process;

    @Mock private AdobeSignOrchestrator signOrchestrator;
    @Mock private DoRService doRService;
    @Mock private WorkItem workItem;
    @Mock private WorkflowSession workflowSession;
    @Mock private MetaDataMap processArgs;
    @Mock private WorkflowData workflowData;
    @Mock private Workflow workflow;
    @Mock private MetaDataMap wfMetadata;
    @Mock private ResourceResolver resourceResolver;
    @Mock private Resource formResource;
    @Mock private AssetManager assetManager;
    @Mock private Asset asset;
    @Mock private DoRResult doRResult;

    @BeforeEach
    void setUp() throws Exception {
        process = new SignToDoRProcess();
        setField("signOrchestrator", signOrchestrator);
        setField("doRService", doRService);
        setField("adaptiveFormPath", ADAPTIVE_FORM_PATH);
        setField("dorStoragePath", DOR_STORAGE_PATH);
        setField("dorLocale", Locale.forLanguageTag("en"));
        setField("signerEmailField", "email");

        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(PAYLOAD_JSON);
        when(workItem.getWorkflow()).thenReturn(workflow);
        when(workflow.getMetaDataMap()).thenReturn(wfMetadata);

        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(resourceResolver);
        when(resourceResolver.getResource(ADAPTIVE_FORM_PATH)).thenReturn(formResource);
        when(resourceResolver.adaptTo(AssetManager.class)).thenReturn(assetManager);
        when(assetManager.createAsset(any(), any(ByteArrayInputStream.class), eq("application/pdf"), eq(true)))
                .thenReturn(asset);
        when(asset.getPath()).thenReturn(DOR_STORAGE_PATH + "/SIGN-existing.pdf");
    }

    private void setField(String name, Object value) throws Exception {
        Field field = SignToDoRProcess.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(process, value);
    }

    // --- No agreement yet: render draft, send for signature ---

    @Test
    void testExecuteCreatesAgreementWhenNoneExists() throws WorkflowException, DoRGenerationException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenReturn(doRResult);
        when(doRResult.getContent()).thenReturn(new byte[]{1, 2, 3});
        when(signOrchestrator.createAgreement(any(), any(), eq("signer@example.com"))).thenReturn("SIGN-new-123");

        process.execute(workItem, workflowSession, processArgs);

        verify(signOrchestrator).createAgreement(any(byte[].class), any(String.class), eq("signer@example.com"));
    }

    @Test
    void testExecuteStoresAgreementIdInMetadata() throws WorkflowException, DoRGenerationException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenReturn(doRResult);
        when(doRResult.getContent()).thenReturn(new byte[]{1, 2, 3});
        when(signOrchestrator.createAgreement(any(), any(), any())).thenReturn("SIGN-abc-456");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata).put("adobeSignAgreementId", "SIGN-abc-456");
    }

    @Test
    void testExecuteSetsSigningStatusOutForSignatureOnFirstCall() throws WorkflowException, DoRGenerationException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenReturn(doRResult);
        when(doRResult.getContent()).thenReturn(new byte[]{1, 2, 3});
        when(signOrchestrator.createAgreement(any(), any(), any())).thenReturn("SIGN-abc-456");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata).put("signingStatus", "OUT_FOR_SIGNATURE");
    }

    @Test
    void testExecuteRendersDraftAgainstTheConfiguredFormWithThePayloadAndLocale() throws WorkflowException, DoRGenerationException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenReturn(doRResult);
        when(doRResult.getContent()).thenReturn(new byte[]{1, 2, 3});
        when(signOrchestrator.createAgreement(any(), any(), any())).thenReturn("SIGN-abc-456");

        process.execute(workItem, workflowSession, processArgs);

        ArgumentCaptor<DoROptions> captor = ArgumentCaptor.forClass(DoROptions.class);
        verify(doRService).render(captor.capture());
        assertEquals(formResource, captor.getValue().getFormResource());
        assertEquals(PAYLOAD_JSON, captor.getValue().getData());
        assertEquals(Locale.forLanguageTag("en"), captor.getValue().getLocale());
    }

    @Test
    void testExecuteExtractsSignerEmailFromConfiguredPayloadField() throws WorkflowException, DoRGenerationException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenReturn(doRResult);
        when(doRResult.getContent()).thenReturn(new byte[]{1, 2, 3});
        when(signOrchestrator.createAgreement(any(), any(), any())).thenReturn("SIGN-abc-456");

        process.execute(workItem, workflowSession, processArgs);

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(signOrchestrator).createAgreement(any(byte[].class), any(String.class), emailCaptor.capture());
        assertEquals("signer@example.com", emailCaptor.getValue());
    }

    @Test
    void testExecuteSetsSigningStatusFailedWhenDraftRenderThrows() throws WorkflowException, DoRGenerationException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenThrow(new DoRGenerationException("rendering failed"));

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));

        verify(wfMetadata).put("signingStatus", "FAILED");
        verify(signOrchestrator, never()).createAgreement(any(), any(), any());
    }

    @Test
    void testExecuteSetsSigningStatusFailedWhenCreateAgreementThrows() throws WorkflowException, DoRGenerationException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenReturn(doRResult);
        when(doRResult.getContent()).thenReturn(new byte[]{1, 2, 3});
        when(signOrchestrator.createAgreement(any(), any(), any())).thenThrow(new AdobeSignException("Adobe Sign is down"));

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));

        verify(wfMetadata).put("signingStatus", "FAILED");
    }

    @Test
    void testExecuteSetsSigningStatusFailedWhenPayloadMissingEmailField() throws WorkflowException, DoRGenerationException, AdobeSignException {
        when(workflowData.getPayload()).thenReturn("{\"firstName\":\"Jane\"}");
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenReturn(doRResult);
        when(doRResult.getContent()).thenReturn(new byte[]{1, 2, 3});

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));

        verify(wfMetadata).put("signingStatus", "FAILED");
        verify(signOrchestrator, never()).createAgreement(any(), any(), any());
    }

    @Test
    void testExecuteSetsSigningStatusFailedWhenResourceResolverUnavailable() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(workflowSession.adaptTo(ResourceResolver.class)).thenReturn(null);

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));

        verify(wfMetadata).put("signingStatus", "FAILED");
        verifyNoInteractions(doRService, signOrchestrator);
    }

    @Test
    void testExecuteSetsSigningStatusFailedWhenFormResourceNotFound() throws WorkflowException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(resourceResolver.getResource(ADAPTIVE_FORM_PATH)).thenReturn(null);

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));

        verify(wfMetadata).put("signingStatus", "FAILED");
        verifyNoInteractions(doRService);
    }

    // --- Agreement exists: check status, fetch signed doc when SIGNED ---

    @Test
    void testExecuteChecksStatusWhenAgreementAlreadyExists() throws WorkflowException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("OUT_FOR_SIGNATURE");

        process.execute(workItem, workflowSession, processArgs);

        verify(signOrchestrator).getStatus("SIGN-existing");
        verify(signOrchestrator, never()).createAgreement(any(), any(), any());
    }

    @Test
    void testExecuteUpdatesSigningStatusOnSubsequentCall() throws WorkflowException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("OUT_FOR_SIGNATURE");

        process.execute(workItem, workflowSession, processArgs);

        verify(wfMetadata).put("signingStatus", "OUT_FOR_SIGNATURE");
    }

    @Test
    void testExecuteDoesNotGenerateDoRWhenNotSigned() throws WorkflowException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("OUT_FOR_SIGNATURE");

        process.execute(workItem, workflowSession, processArgs);

        verify(signOrchestrator, never()).getSignedDocument(any());
        verify(wfMetadata, never()).put(eq("dorStatus"), any());
    }

    @Test
    void testExecuteFetchesSignedDocumentAndSetsDorStatusGeneratedWhenSigned() throws WorkflowException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("SIGNED");
        when(signOrchestrator.getSignedDocument("SIGN-existing")).thenReturn(new byte[]{1, 2, 3});

        process.execute(workItem, workflowSession, processArgs);

        verify(signOrchestrator).getSignedDocument("SIGN-existing");
        verify(wfMetadata).put("dorStatus", "GENERATED");
    }

    @Test
    void testExecuteDoesNotReRenderDraftWhenFetchingSignedDocument() throws WorkflowException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("SIGNED");
        when(signOrchestrator.getSignedDocument("SIGN-existing")).thenReturn(new byte[]{1, 2, 3});

        process.execute(workItem, workflowSession, processArgs);

        // The final Document of Record is the actually-signed document
        // downloaded from Adobe Sign, not a second DoRService render.
        verifyNoInteractions(doRService);
    }

    @Test
    void testExecuteSavesSignedDocumentAsADamAssetAndRecordsItsPath() throws WorkflowException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("SIGNED");
        when(signOrchestrator.getSignedDocument("SIGN-existing")).thenReturn(new byte[]{1, 2, 3});

        process.execute(workItem, workflowSession, processArgs);

        verify(assetManager).createAsset(eq(DOR_STORAGE_PATH + "/SIGN-existing.pdf"), any(ByteArrayInputStream.class), eq("application/pdf"), eq(true));
        verify(wfMetadata).put("dorAssetPath", DOR_STORAGE_PATH + "/SIGN-existing.pdf");
    }

    @Test
    void testExecuteSetsDorStatusFailedWhenGetSignedDocumentThrows() throws WorkflowException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenReturn("SIGNED");
        when(signOrchestrator.getSignedDocument("SIGN-existing")).thenThrow(new AdobeSignException("download failed"));

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));

        verify(wfMetadata).put("dorStatus", "FAILED");
        verify(wfMetadata, never()).put(eq("dorStatus"), eq("GENERATED"));
    }

    @Test
    void testExecuteDoesNotUpdateSigningStatusWhenGetStatusThrows() throws WorkflowException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn("SIGN-existing");
        when(signOrchestrator.getStatus("SIGN-existing")).thenThrow(new AdobeSignException("Adobe Sign unreachable"));

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));

        verify(wfMetadata, never()).put(eq("signingStatus"), any());
        verify(signOrchestrator, never()).getSignedDocument(any());
    }

    @Test
    void testExecuteDoesNotThrowOnValidInputs() throws DoRGenerationException, AdobeSignException {
        when(wfMetadata.get("adobeSignAgreementId", String.class)).thenReturn(null);
        when(doRService.render(any())).thenReturn(doRResult);
        when(doRResult.getContent()).thenReturn(new byte[]{1, 2, 3});
        when(signOrchestrator.createAgreement(any(), any(), any())).thenReturn("SIGN-no-throw");

        assertDoesNotThrow(() -> process.execute(workItem, workflowSession, processArgs));
    }
}
