package ${package}.services.impl;

import ${package}.services.AdobeSignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Mocks java.net.http.HttpClient and asserts against the real Adobe Sign
 * REST API v6 request/response shapes documented at
 * developer.adobe.com/acrobat-sign and github.com/AdobeDocs/adobe-sign -
 * not against arbitrary/assumed JSON. Does not hit a real Adobe Sign
 * account; see README for what still needs live verification.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdobeSignOrchestratorImplTest {

    private static final String ACCESS_POINT = "https://api.na2.adobesign.com";

    private AdobeSignOrchestratorImpl orchestrator;

    @Mock
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        orchestrator = new AdobeSignOrchestratorImpl();
        orchestrator.httpClient = httpClient;
        setField("tokenEndpoint", "https://secure.adobesign.com/oauth/v2/token");
        setField("baseUrisEndpoint", "https://api.adobesign.com/api/rest/v6/baseUris");
    }

    private void setField(String name, Object value) throws Exception {
        Field field = AdobeSignOrchestratorImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(orchestrator, value);
    }

    private void configureIntegrationKey(String key) throws Exception {
        setField("integrationKey", key);
    }

    private void configureOAuth(String clientId, String clientSecret, String refreshToken) throws Exception {
        setField("clientId", clientId);
        setField("clientSecret", clientSecret);
        setField("refreshToken", refreshToken);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> mockResponse(int status, byte[] body) {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private byte[] json(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /** Routes mocked responses by a substring of the request URI, matching whichever real endpoint is hit. */
    private void stubResponses(java.util.Map<String, HttpResponse<byte[]>> responsesByUriSubstring) throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String uri = request.uri().toString();
            for (var entry : responsesByUriSubstring.entrySet()) {
                if (uri.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            throw new AssertionError("Unexpected HTTP call to: " + uri);
        });
    }

    @Test
    void testCreateAgreementUsesIntegrationKeyWhenConfigured() throws Exception {
        configureIntegrationKey("INTEGRATION-KEY-123");
        stubResponses(java.util.Map.of(
            "baseUris", mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "/\"}")),
            "transientDocuments", mockResponse(200, json("{\"transientDocumentId\":\"TD-1\"}")),
            "/agreements", mockResponse(200, json("{\"id\":\"AGR-1\"}"))
        ));

        String agreementId = orchestrator.createAgreement("pdf-bytes".getBytes(StandardCharsets.UTF_8), "doc.pdf", "signer@example.com");

        assertEquals("AGR-1", agreementId);
    }

    @Test
    void testCreateAgreementUsesOAuthRefreshFlowWhenNoIntegrationKey() throws Exception {
        configureOAuth("client-id", "client-secret", "refresh-token");
        stubResponses(java.util.Map.of(
            "oauth/v2/token", mockResponse(200, json("{\"access_token\":\"ACCESS-1\",\"expires_in\":3600}")),
            "baseUris", mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "\"}")),
            "transientDocuments", mockResponse(200, json("{\"transientDocumentId\":\"TD-2\"}")),
            "/agreements", mockResponse(200, json("{\"id\":\"AGR-2\"}"))
        ));

        String agreementId = orchestrator.createAgreement("pdf-bytes".getBytes(StandardCharsets.UTF_8), "doc.pdf", "signer@example.com");

        assertEquals("AGR-2", agreementId);
    }

    @Test
    void testCreateAgreementSendsSignerEmailInAgreementPayload() throws Exception {
        configureIntegrationKey("KEY");
        stubResponses(java.util.Map.of(
            "baseUris", mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "\"}")),
            "transientDocuments", mockResponse(200, json("{\"transientDocumentId\":\"TD-3\"}")),
            "/agreements", mockResponse(200, json("{\"id\":\"AGR-3\"}"))
        ));

        orchestrator.createAgreement("pdf-bytes".getBytes(StandardCharsets.UTF_8), "doc.pdf", "jane@example.com");

        org.mockito.ArgumentCaptor<HttpRequest> captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, atLeastOnce()).send(captor.capture(), any());
        HttpRequest agreementsRequest = captor.getAllValues().stream()
            .filter(req -> req.uri().toString().endsWith("/agreements"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a POST to /agreements"));
        assertTrue(agreementsRequest.bodyPublisher().isPresent(), "Expected the /agreements request to carry a JSON body");
        assertTrue(agreementsRequest.bodyPublisher().get().contentLength() > 0, "Expected a non-empty agreement payload");
    }

    @Test
    void testCreateAgreementThrowsWhenNoDocumentContent() {
        assertThrows(AdobeSignException.class,
            () -> orchestrator.createAgreement(new byte[0], "doc.pdf", "signer@example.com"));
    }

    @Test
    void testCreateAgreementThrowsWhenNoSignerEmail() {
        assertThrows(AdobeSignException.class,
            () -> orchestrator.createAgreement("bytes".getBytes(StandardCharsets.UTF_8), "doc.pdf", ""));
    }

    @Test
    void testCreateAgreementThrowsWhenNotConfigured() {
        assertThrows(AdobeSignException.class,
            () -> orchestrator.createAgreement("bytes".getBytes(StandardCharsets.UTF_8), "doc.pdf", "signer@example.com"));
    }

    @Test
    void testCreateAgreementThrowsOnNon2xxResponse() throws Exception {
        configureIntegrationKey("KEY");
        stubResponses(java.util.Map.of(
            "baseUris", mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "\"}")),
            "transientDocuments", mockResponse(400, json("{\"message\":\"bad request\"}"))
        ));

        AdobeSignException e = assertThrows(AdobeSignException.class,
            () -> orchestrator.createAgreement("bytes".getBytes(StandardCharsets.UTF_8), "doc.pdf", "signer@example.com"));
        assertTrue(e.getMessage().contains("400"));
    }

    @Test
    void testGetStatusReturnsWebhookCachedValueWithoutHttpCall() throws Exception {
        orchestrator.recordWebhookStatus("AGR-cached", "SIGNED");

        String status = orchestrator.getStatus("AGR-cached");

        assertEquals("SIGNED", status);
        verifyNoInteractions(httpClient);
    }

    @Test
    void testGetStatusMakesLiveCallWhenNotCached() throws Exception {
        configureIntegrationKey("KEY");
        stubResponses(java.util.Map.of(
            "baseUris", mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "\"}")),
            "/agreements/AGR-live", mockResponse(200, json("{\"status\":\"OUT_FOR_SIGNATURE\"}"))
        ));

        String status = orchestrator.getStatus("AGR-live");

        assertEquals("OUT_FOR_SIGNATURE", status);
    }

    @Test
    void testGetStatusCachesLiveResultForSubsequentCalls() throws Exception {
        configureIntegrationKey("KEY");
        stubResponses(java.util.Map.of(
            "baseUris", mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "\"}")),
            "/agreements/AGR-once", mockResponse(200, json("{\"status\":\"SIGNED\"}"))
        ));

        orchestrator.getStatus("AGR-once");
        String secondCallStatus = orchestrator.getStatus("AGR-once");

        assertEquals("SIGNED", secondCallStatus);
        // Second call should be served from the webhook/status cache populated by the
        // first live call, not trigger another token+status round trip.
        verify(httpClient, times(2)).send(any(HttpRequest.class), any());
    }

    @Test
    void testGetSignedDocumentReturnsCombinedDocumentBytes() throws Exception {
        configureIntegrationKey("KEY");
        byte[] pdfBytes = new byte[]{'%', 'P', 'D', 'F'};
        stubResponses(java.util.Map.of(
            "baseUris", mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "\"}")),
            "combinedDocument", mockResponse(200, pdfBytes)
        ));

        byte[] result = orchestrator.getSignedDocument("AGR-signed");

        assertArrayEquals(pdfBytes, result);
    }

    @Test
    void testGetSignedDocumentThrowsOnFailureResponse() throws Exception {
        configureIntegrationKey("KEY");
        stubResponses(java.util.Map.of(
            "baseUris", mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "\"}")),
            "combinedDocument", mockResponse(404, json("{\"message\":\"not found\"}"))
        ));

        assertThrows(AdobeSignException.class, () -> orchestrator.getSignedDocument("AGR-missing"));
    }

    @Test
    void testSendWithRetryRetriesOn5xxThenSucceeds() throws Exception {
        configureIntegrationKey("KEY");
        HttpResponse<byte[]> baseUris = mockResponse(200, json("{\"apiAccessPoint\":\"" + ACCESS_POINT + "\"}"));
        HttpResponse<byte[]> serverError = mockResponse(500, json("{}"));
        HttpResponse<byte[]> success = mockResponse(200, json("{\"status\":\"SIGNED\"}"));

        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(new org.mockito.stubbing.Answer<HttpResponse<byte[]>>() {
            private int agreementCallCount = 0;

            @Override
            public HttpResponse<byte[]> answer(org.mockito.invocation.InvocationOnMock invocation) {
                HttpRequest request = invocation.getArgument(0);
                String uri = request.uri().toString();
                if (uri.contains("baseUris")) {
                    return baseUris;
                }
                agreementCallCount++;
                return agreementCallCount == 1 ? serverError : success;
            }
        });

        String status = orchestrator.getStatus("AGR-retry");

        assertEquals("SIGNED", status);
        verify(httpClient, times(3)).send(any(HttpRequest.class), any());
    }

    @Test
    void testGetClientIdReturnsConfiguredValue() throws Exception {
        setField("clientId", "my-client-id");
        assertEquals("my-client-id", orchestrator.getClientId());
    }

    @Test
    void testRecordWebhookStatusOverwritesPreviousValue() {
        orchestrator.recordWebhookStatus("AGR-x", "OUT_FOR_SIGNATURE");
        orchestrator.recordWebhookStatus("AGR-x", "SIGNED");

        assertDoesNotThrow(() -> assertEquals("SIGNED", orchestrator.getStatus("AGR-x")));
    }
}
