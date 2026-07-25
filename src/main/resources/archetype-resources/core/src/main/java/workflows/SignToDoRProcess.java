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
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * On first execution, renders a pre-signature draft of the submitted
 * Adaptive Form via the real DoRService and sends it to Adobe Sign for
 * signature. On later executions, polls (or picks up a webhook-recorded)
 * status; once SIGNED, downloads the actually-signed document (with audit
 * trail) from Adobe Sign and stores that as the Document of Record - the
 * final signed artifact, not a re-render of the pre-signature draft.
 */
@Component(
    service = WorkflowProcess.class,
    property = {
        "process.label=BMAD: Sign to DoR Orchestrator"
    }
)
@Designate(ocd = SignToDoRProcess.Config.class)
public class SignToDoRProcess implements WorkflowProcess {

    private static final Logger LOG = LoggerFactory.getLogger(SignToDoRProcess.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ObjectClassDefinition(
        name = "AEM Forms Sign-to-DoR Process Configuration",
        description = "Configuration for the Document of Record generated once an agreement is signed."
    )
    public @interface Config {
        @AttributeDefinition(
            name = "Adaptive Form Path",
            description = "The Adaptive Form resource rendered as the pre-signature draft.",
            type = AttributeType.STRING
        )
        String adaptive_form_path() default "/content/forms/af/${appName}/financial-application";

        @AttributeDefinition(
            name = "Generated DoR Storage Path",
            description = "DAM folder new Document of Record PDFs are saved under (one file per agreement id).",
            type = AttributeType.STRING
        )
        String dor_storage_path() default "/content/dam/formsanddocuments/generated-dor/${appName}";

        @AttributeDefinition(
            name = "Document of Record Locale",
            description = "BCP 47 language tag (e.g. en, en-US) used to render the pre-signature draft. "
                    + "DoRService.render() NPEs internally if DoROptions.locale is left unset.",
            type = AttributeType.STRING
        )
        String dor_locale() default "en";

        @AttributeDefinition(
            name = "Signer Email Field",
            description = "The field name in the submitted form data JSON that holds the signer's email address.",
            type = AttributeType.STRING
        )
        String signer_email_field() default "email";
    }

    @Reference
    private AdobeSignOrchestrator signOrchestrator;

    @Reference
    private DoRService doRService;

    private String adaptiveFormPath;
    private String dorStoragePath;
    private Locale dorLocale;
    private String signerEmailField;

    @Activate
    protected void activate(final Config config) {
        this.adaptiveFormPath = config.adaptive_form_path();
        this.dorStoragePath = config.dor_storage_path();
        this.dorLocale = Locale.forLanguageTag(config.dor_locale());
        this.signerEmailField = config.signer_email_field();
    }

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        String payload = workItem.getWorkflowData().getPayload().toString();
        MetaDataMap wfMetadata = workItem.getWorkflow().getMetaDataMap();
        String agreementId = wfMetadata.get("adobeSignAgreementId", String.class);

        if (agreementId == null) {
            sendForSignature(payload, workflowSession, wfMetadata);
        } else {
            checkStatusAndMaybeGenerateDoR(agreementId, workflowSession, wfMetadata);
        }
    }

    // Renders a pre-signature draft (real DoRService call, same prerequisites
    // documented in the README) and sends it to Adobe Sign. A failure here
    // is tracked (signingStatus=FAILED) rather than thrown as a
    // WorkflowException so a transient failure can be retried without
    // aborting the whole workflow.
    private void sendForSignature(String payload, WorkflowSession workflowSession, MetaDataMap wfMetadata) {
        ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
        if (resolver == null) {
            LOG.error("Could not adapt WorkflowSession to a ResourceResolver; cannot send document for signature");
            wfMetadata.put("signingStatus", "FAILED");
            return;
        }

        Resource formResource = resolver.getResource(adaptiveFormPath);
        if (formResource == null) {
            LOG.error("Adaptive Form not found at {}; cannot send document for signature", adaptiveFormPath);
            wfMetadata.put("signingStatus", "FAILED");
            return;
        }

        try {
            byte[] draftPdf = renderDraft(formResource, payload);
            String signerEmail = extractSignerEmail(payload);
            String documentName = "DoR-Draft-" + System.currentTimeMillis() + ".pdf";

            String agreementId = signOrchestrator.createAgreement(draftPdf, documentName, signerEmail);
            wfMetadata.put("adobeSignAgreementId", agreementId);
            wfMetadata.put("signingStatus", "OUT_FOR_SIGNATURE");
            LOG.info("Sent document for signature, agreement: {}", agreementId);
        } catch (DoRGenerationException | AdobeSignException e) {
            LOG.error("Failed to send document for signature", e);
            wfMetadata.put("signingStatus", "FAILED");
        }
    }

    private byte[] renderDraft(Resource formResource, String payload) throws DoRGenerationException {
        DoROptions options = new DoROptions();
        options.setFormResource(formResource);
        options.setData(payload);
        options.setLocale(dorLocale);

        DoRResult result = doRService.render(options);
        return result.getContent();
    }

    private String extractSignerEmail(String payloadJson) throws AdobeSignException {
        JsonNode node;
        try {
            node = MAPPER.readTree(payloadJson);
        } catch (IOException e) {
            throw new AdobeSignException("Could not parse form submission payload as JSON to extract the signer email", e);
        }
        JsonNode value = node.get(signerEmailField);
        if (value == null || value.asText().isBlank()) {
            throw new AdobeSignException("Form submission payload has no non-blank \"" + signerEmailField + "\" field to use as the Adobe Sign signer email");
        }
        return value.asText();
    }

    private void checkStatusAndMaybeGenerateDoR(String agreementId, WorkflowSession workflowSession, MetaDataMap wfMetadata) {
        String status;
        try {
            status = signOrchestrator.getStatus(agreementId);
        } catch (AdobeSignException e) {
            LOG.error("Failed to check signing status for agreement: {}", agreementId, e);
            return;
        }

        wfMetadata.put("signingStatus", status);

        if ("SIGNED".equals(status)) {
            generateDoR(agreementId, workflowSession, wfMetadata);
        }
    }

    // Signing already succeeded by the time this runs, so a DoR failure is
    // tracked (dorStatus=FAILED, real exception logged) rather than thrown
    // as a WorkflowException — that would abort the whole workflow over a
    // step that can reasonably be retried, for a document whose source
    // agreement is already signed.
    private void generateDoR(String agreementId, WorkflowSession workflowSession, MetaDataMap wfMetadata) {
        LOG.info("Fetching signed Document of Record for agreement: {}", agreementId);

        ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
        if (resolver == null) {
            LOG.error("Could not adapt WorkflowSession to a ResourceResolver; cannot store Document of Record for agreement: {}", agreementId);
            wfMetadata.put("dorStatus", "FAILED");
            return;
        }

        try {
            byte[] signedPdf = signOrchestrator.getSignedDocument(agreementId);
            String assetPath = saveDoR(resolver, agreementId, signedPdf);

            wfMetadata.put("dorStatus", "GENERATED");
            wfMetadata.put("dorAssetPath", assetPath);
            LOG.info("Document of Record stored for agreement {}: {}", agreementId, assetPath);
        } catch (AdobeSignException | DoRGenerationException e) {
            LOG.error("Document of Record generation failed for agreement: {}", agreementId, e);
            wfMetadata.put("dorStatus", "FAILED");
        }
    }

    private String saveDoR(ResourceResolver resolver, String agreementId, byte[] pdfContent) throws DoRGenerationException {
        AssetManager assetManager = resolver.adaptTo(AssetManager.class);
        if (assetManager == null) {
            throw new DoRGenerationException("Could not adapt ResourceResolver to AssetManager to save the generated Document of Record");
        }
        String assetPath = dorStoragePath + "/" + agreementId + ".pdf";
        // AssetManager#createAsset(String, InputStream, String, boolean) is
        // deprecated in favor of async Asset Compute Service ingestion, but
        // still the standard synchronous way to create an asset from bytes
        // already in hand — appropriate here since the PDF is generated,
        // not uploaded, and the caller needs assetPath back immediately.
        Asset asset = assetManager.createAsset(assetPath, new ByteArrayInputStream(pdfContent), "application/pdf", true);
        return asset.getPath();
    }
}
