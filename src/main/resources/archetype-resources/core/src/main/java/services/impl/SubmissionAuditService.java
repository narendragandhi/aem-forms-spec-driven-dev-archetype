package ${package}.services.impl;

import ${package}.services.FormSubmissionException;
import ${package}.services.FormSubmissionService;
import com.adobe.aemds.guide.common.GuideValidationResult;
import com.adobe.aemds.guide.model.FormSubmitInfo;
import com.adobe.aemds.guide.service.FormSubmitActionService;
import com.adobe.aemds.guide.utils.GuideConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Real AEM Forms submission-audit integration: implements the actual
 * com.adobe.aemds.guide.service.FormSubmitActionService interface
 * (verified via javap against the pinned aem-forms-sdk-api jar), the same
 * class of extension point Adobe's own aem-core-forms-components
 * integration-test fixture uses for a custom submit handler
 * (it/core/.../service/CustomAFSubmitService.java, registered with
 * {@code @Component(service = FormSubmitActionService.class)} and a
 * {@code getServiceName()}/{@code submit(FormSubmitInfo)} shape this
 * class matches exactly).
 *
 * <p>Selecting this as a form's submit service makes it the form's actual
 * submission handler (per the real sample above, {@code submit()} is
 * expected to fully own persisting the data and reporting success/failure
 * via {@link GuideConstants#FORM_SUBMISSION_COMPLETE} /
 * {@link GuideConstants#FORM_SUBMISSION_ERROR} - there is no confirmed
 * "runs alongside restendpoint" mode). Rather than inventing new JCR
 * storage and service-user/ACL plumbing this session couldn't live-test,
 * this builds a structured audit record from {@link FormSubmitInfo}'s
 * real fields (submission id, form path, submitter, client IP, user
 * agent, referer, timestamp, and the submitted data itself) and forwards
 * it through the archetype's own already real, live-verified
 * {@link FormSubmissionService} - the same HTTP-forward mechanism proven
 * this session against both a real listener and a real connection-refused
 * failure. Point its configured endpoint at whatever system of record
 * (SIEM, data warehouse, ticketing system) should retain the trail.
 *
 * <p>A real, discoverable datasource entry for this service also ships at
 * {@code ui.apps/.../customsubmission/submissionAudit/.content.xml}
 * ({@code guideComponentType="fd/af/components/guidesubmittype"},
 * {@code submitService="&lt;getServiceName()&gt;"}), matching the shape of
 * Adobe's own real {@code customsubmission/logsubmit} sample - this makes
 * the service selectable from the Adaptive Forms Editor's Submit Action
 * Type dropdown. <b>Honesty note</b>: that confirms how a submit service
 * is registered for editor discovery, but the exact {@code guideContainer}
 * XML property (actionType value / property name) that references a
 * chosen {@code submitService} by name isn't shown by any real sample this
 * session could find - Adobe's own IT content only demonstrates
 * {@code restendpoint}/{@code email} action types wired through content,
 * not a custom {@code FormSubmitActionService}. Confirm the exact
 * authoring step against your own instance's Rule Editor / Submit Action
 * Type UI before relying on this being reachable purely through generated
 * content.
 */
@Component(service = FormSubmitActionService.class, immediate = true)
public class SubmissionAuditService implements FormSubmitActionService {

    private static final Logger LOG = LoggerFactory.getLogger(SubmissionAuditService.class);
    private static final String SERVICE_NAME = "bmadSubmissionAuditService";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    FormSubmissionService formSubmissionService; // package-private so unit tests can substitute a mock

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Map<String, Object> submit(FormSubmitInfo formSubmitInfo) {
        Map<String, Object> result = new HashMap<>();
        String formContainerPath = formSubmitInfo.getFormContainerPath();
        try {
            String auditRecord = buildAuditRecord(formSubmitInfo);
            formSubmissionService.processSubmission(auditRecord, formContainerPath);
            LOG.info("Submission audit record dispatched for form: {} (submissionId={})",
                formContainerPath, formSubmitInfo.getSubmissionId());
            result.put(GuideConstants.FORM_SUBMISSION_COMPLETE, Boolean.TRUE);
        } catch (FormSubmissionException e) {
            LOG.error("Failed to dispatch submission audit record for form: {}", formContainerPath, e);
            GuideValidationResult validationResult = new GuideValidationResult();
            validationResult.setOriginCode("500");
            validationResult.setErrorMessage("Failed to record form submission");
            result.put(GuideConstants.FORM_SUBMISSION_COMPLETE, Boolean.FALSE);
            result.put(GuideConstants.FORM_SUBMISSION_ERROR, validationResult);
        }
        return result;
    }

    // Package-private so unit tests can assert on the exact payload shape
    // without going through the OSGi-mocked FormSubmissionService.
    String buildAuditRecord(FormSubmitInfo formSubmitInfo) {
        ObjectNode audit = MAPPER.createObjectNode();
        audit.put("submissionId", formSubmitInfo.getSubmissionId());
        audit.put("formContainerPath", formSubmitInfo.getFormContainerPath());
        audit.put("formSubmitter", formSubmitInfo.getFormSubmitter());
        audit.put("clientIP", formSubmitInfo.getClientIP());
        audit.put("userAgent", formSubmitInfo.getUserAgent());
        audit.put("referer", formSubmitInfo.getReferer());
        audit.put("submittedAt", Instant.now().toString());
        audit.set("data", parseDataOrNull(formSubmitInfo.getData()));
        return audit.toString();
    }

    // The real submitted data is a JSON string on Core Components forms
    // (the same shape confirmed live this session for prefill/submit) -
    // embedded as real JSON rather than a doubly-escaped string when it
    // parses; falls back to a plain text node so a submission is never
    // dropped from the trail just because its data wasn't valid JSON.
    private JsonNode parseDataOrNull(String data) {
        if (data == null) {
            return MAPPER.nullNode();
        }
        try {
            return MAPPER.readTree(data);
        } catch (Exception e) {
            return MAPPER.getNodeFactory().textNode(data);
        }
    }
}
