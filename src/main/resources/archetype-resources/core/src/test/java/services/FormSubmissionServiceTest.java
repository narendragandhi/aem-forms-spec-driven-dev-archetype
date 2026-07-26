package ${package}.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Mocks java.net.http.HttpClient, same pattern as
 * AdobeSignOrchestratorImplTest/InteractiveCommunicationServiceImplTest -
 * verifies the real request shape sent to the configured submission API,
 * not just that logging happens.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FormSubmissionServiceTest {

    private FormSubmissionService formSubmissionService;

    @Mock
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        formSubmissionService = new FormSubmissionService();
        formSubmissionService.httpClient = httpClient;
    }

    private void activate(String endpoint, String apiKey) throws Exception {
        FormSubmissionService.Config config = createMockConfig(endpoint, apiKey);
        Method activateMethod = FormSubmissionService.class.getDeclaredMethod("activate", FormSubmissionService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(formSubmissionService, config);
    }

    private FormSubmissionService.Config createMockConfig(String endpoint, String apiKey) {
        return new FormSubmissionService.Config() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return FormSubmissionService.Config.class;
            }

            @Override
            public String submission_api_endpoint() {
                return endpoint;
            }

            @Override
            public String api_key() {
                return apiKey;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    @Test
    void testProcessSubmissionPostsToConfiguredEndpoint() throws Exception {
        activate("https://api.example.com/forms", "");
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockResponse(200, "{}"));

        formSubmissionService.processSubmission("{\"name\":\"Jane\"}", "form-1");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertEquals("https://api.example.com/forms", captor.getValue().uri().toString());
        assertEquals("POST", captor.getValue().method());
    }

    @Test
    void testProcessSubmissionSendsBearerTokenWhenApiKeyConfigured() throws Exception {
        activate("https://api.example.com/forms", "secret-key");
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockResponse(200, "{}"));

        formSubmissionService.processSubmission("{}", "form-2");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertEquals("Bearer secret-key", captor.getValue().headers().firstValue("Authorization").orElse(null));
    }

    @Test
    void testProcessSubmissionOmitsAuthorizationHeaderWhenNoApiKey() throws Exception {
        activate("https://api.example.com/forms", "");
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockResponse(200, "{}"));

        formSubmissionService.processSubmission("{}", "form-3");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertTrue(captor.getValue().headers().firstValue("Authorization").isEmpty());
    }

    @Test
    void testProcessSubmissionThrowsOnNon2xxResponse() throws Exception {
        activate("https://api.example.com/forms", "");
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockResponse(500, "server error"));

        FormSubmissionException e = assertThrows(FormSubmissionException.class,
            () -> formSubmissionService.processSubmission("{}", "form-4"));
        assertTrue(e.getMessage().contains("500"));
    }

    @Test
    void testProcessSubmissionThrowsOnIOException() throws Exception {
        activate("https://api.example.com/forms", "");
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new java.io.IOException("connection refused"));

        assertThrows(FormSubmissionException.class,
            () -> formSubmissionService.processSubmission("{}", "form-5"));
    }

    @Test
    void testProcessSubmissionSucceedsOnNullFormData() throws Exception {
        activate("https://api.example.com/forms", "");
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockResponse(200, "{}"));

        assertDoesNotThrow(() -> formSubmissionService.processSubmission(null, "form-6"));
    }
}
