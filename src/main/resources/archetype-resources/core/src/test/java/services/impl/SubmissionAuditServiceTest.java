package ${package}.services.impl;

import ${package}.services.FormSubmissionException;
import ${package}.services.FormSubmissionService;
import com.adobe.aemds.guide.model.FormSubmitInfo;
import com.adobe.aemds.guide.utils.GuideConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies SubmissionAuditService's real contract: it implements
 * com.adobe.aemds.guide.service.FormSubmitActionService (matching Adobe's
 * own aem-core-forms-components CustomAFSubmitService sample), builds an
 * audit record from FormSubmitInfo's real fields, and forwards it through
 * the archetype's own already-verified FormSubmissionService rather than
 * a mocked HTTP client directly - the collaboration this class actually
 * owns is that hand-off, not the HTTP call itself (already covered by
 * FormSubmissionServiceTest).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmissionAuditServiceTest {

    private SubmissionAuditService submissionAuditService;

    @Mock
    private FormSubmissionService formSubmissionService;

    @BeforeEach
    void setUp() {
        submissionAuditService = new SubmissionAuditService();
        submissionAuditService.formSubmissionService = formSubmissionService;
    }

    private FormSubmitInfo formSubmitInfo() {
        FormSubmitInfo info = new FormSubmitInfo();
        info.setFormContainerPath("/content/forms/af/AcmeApp/employee-onboarding");
        info.setFormSubmitter("jane.doe@acme.com");
        info.setClientIP("203.0.113.7");
        info.setUserAgent("Mozilla/5.0");
        info.setReferer("https://acme.example/onboarding");
        info.setData("{\"fullName\":\"Jane Doe\"}");
        return info;
    }

    @Test
    void testGetServiceNameReturnsStableIdentifier() {
        assertEquals("bmadSubmissionAuditService", submissionAuditService.getServiceName());
    }

    @Test
    void testSubmitForwardsAuditRecordToFormSubmissionService() throws Exception {
        Map<String, Object> result = submissionAuditService.submit(formSubmitInfo());

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(formSubmissionService).processSubmission(jsonCaptor.capture(), pathCaptor.capture());
        assertEquals("/content/forms/af/AcmeApp/employee-onboarding", pathCaptor.getValue());

        String json = jsonCaptor.getValue();
        assertTrue(json.contains("\"formContainerPath\":\"/content/forms/af/AcmeApp/employee-onboarding\""));
        assertTrue(json.contains("\"formSubmitter\":\"jane.doe@acme.com\""));
        assertTrue(json.contains("\"clientIP\":\"203.0.113.7\""));
        assertTrue(json.contains("\"userAgent\":\"Mozilla/5.0\""));
        assertTrue(json.contains("\"referer\":\"https://acme.example/onboarding\""));
        assertTrue(json.contains("\"submittedAt\""));
        // Real submitted data is embedded as real JSON, not a
        // doubly-escaped string.
        assertTrue(json.contains("\"data\":{\"fullName\":\"Jane Doe\"}"));

        assertEquals(Boolean.TRUE, result.get(GuideConstants.FORM_SUBMISSION_COMPLETE));
    }

    @Test
    void testSubmitFallsBackToTextNodeWhenDataIsNotValidJson() throws Exception {
        FormSubmitInfo info = formSubmitInfo();
        info.setData("not-json");

        submissionAuditService.submit(info);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(formSubmissionService).processSubmission(jsonCaptor.capture(), any());
        assertTrue(jsonCaptor.getValue().contains("\"data\":\"not-json\""));
    }

    @Test
    void testSubmitReportsErrorWhenFormSubmissionServiceThrows() throws Exception {
        doThrow(new FormSubmissionException("connection refused"))
            .when(formSubmissionService).processSubmission(any(), any());

        Map<String, Object> result = submissionAuditService.submit(formSubmitInfo());

        assertEquals(Boolean.FALSE, result.get(GuideConstants.FORM_SUBMISSION_COMPLETE));
        assertNotNull(result.get(GuideConstants.FORM_SUBMISSION_ERROR));
    }
}
