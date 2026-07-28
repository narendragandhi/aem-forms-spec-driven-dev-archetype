package ${package}.services.impl;

import com.adobe.forms.common.service.ContentType;
import com.adobe.forms.common.service.DataOptions;
import com.adobe.forms.common.service.FormsException;
import com.adobe.forms.common.service.PrefillData;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Mocks java.net.http.HttpClient, same pattern as
 * FormSubmissionServiceTest/AdobeSignOrchestratorImplTest - verifies the
 * real request shape sent to the configured prefill endpoint and the real
 * response shape returned to the DataProvider interface's caller, not
 * just that a call happens.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrefillDataServiceTest {

    private PrefillDataService prefillDataService;

    @Mock
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        prefillDataService = new PrefillDataService();
        prefillDataService.httpClient = httpClient;
        activate("http://localhost:4502/bin/bmad/mock-finance-data", "", "");
    }

    private void activate(String endpoint, String username, String password) throws Exception {
        PrefillDataService.Config config = new PrefillDataService.Config() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return PrefillDataService.Config.class;
            }

            @Override
            public String prefill_data_endpoint() {
                return endpoint;
            }

            @Override
            public String prefill_data_endpoint_username() {
                return username;
            }

            @Override
            public String prefill_data_endpoint_password() {
                return password;
            }
        };
        Method activateMethod = PrefillDataService.class.getDeclaredMethod("activate", PrefillDataService.Config.class);
        activateMethod.setAccessible(true);
        activateMethod.invoke(prefillDataService, config);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> mockResponse(int status, String body) {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        return response;
    }

    private String readAll(PrefillData data) throws Exception {
        return new String(data.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void testGetPrefillDataSendsNoAuthorizationHeaderByDefault() throws Exception {
        DataOptions options = new DataOptions();
        options.setDataRef("CUST-10293");
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockResponse(200, "{}"));

        prefillDataService.getPrefillData(options);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertTrue(captor.getValue().headers().firstValue("Authorization").isEmpty(),
            "No Authorization header should be sent when no credentials are configured");
    }

    @Test
    void testGetPrefillDataSendsBasicAuthHeaderWhenCredentialsConfigured() throws Exception {
        // Live-confirmed this session: the shipped default endpoint
        // (MockFinanceDataServlet, a real Sling servlet on the same AEM
        // instance) returns a real HTTP 401 with no credentials - curl
        // without -u vs with -u admin:admin directly confirmed this.
        activate("http://localhost:4502/bin/bmad/mock-finance-data", "admin", "admin");
        DataOptions options = new DataOptions();
        options.setDataRef("CUST-10293");
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockResponse(200, "{}"));

        prefillDataService.getPrefillData(options);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        String expected = "Basic " + java.util.Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, captor.getValue().headers().firstValue("Authorization").orElse(null));
    }

    @Test
    void testGetPrefillDataUsesDataRefAsIdentifier() throws Exception {
        DataOptions options = new DataOptions();
        options.setDataRef("CUST-10293");
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockResponse(200, "{\"fullName\":\"Jane Doe\"}"));

        PrefillData result = prefillDataService.getPrefillData(options);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertTrue(captor.getValue().uri().toString().contains("customerId=CUST-10293"));
        assertEquals(ContentType.JSON, result.getContentType());
        assertEquals("{\"data\":{\"fullName\":\"Jane Doe\"}}", readAll(result));
    }

    @Test
    void testGetPrefillDataFallsBackToCustomerIdExtra() throws Exception {
        DataOptions options = new DataOptions();
        Map<String, Object> extras = new HashMap<>();
        extras.put("customerId", "CUST-99");
        options.setExtras(extras);
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockResponse(200, "{}"));

        prefillDataService.getPrefillData(options);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertTrue(captor.getValue().uri().toString().contains("customerId=CUST-99"));
    }

    @Test
    void testGetPrefillDataReturnsEmptyWhenNoIdentifierPresent() throws Exception {
        DataOptions options = new DataOptions();

        PrefillData result = prefillDataService.getPrefillData(options);

        assertEquals("{\"data\":{}}", readAll(result));
        verifyNoInteractions(httpClient);
    }

    @Test
    void testGetPrefillDataReturnsEmptyOnNon2xxResponse() throws Exception {
        DataOptions options = new DataOptions();
        options.setDataRef("CUST-1");
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockResponse(404, "not found"));

        PrefillData result = prefillDataService.getPrefillData(options);

        assertEquals("{\"data\":{}}", readAll(result));
    }

    @Test
    void testGetPrefillDataThrowsFormsExceptionOnIOException() throws Exception {
        DataOptions options = new DataOptions();
        options.setDataRef("CUST-1");
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new java.io.IOException("connection refused"));

        assertThrows(FormsException.class, () -> prefillDataService.getPrefillData(options));
    }

    @Test
    void testGetPrefillDataPrefersDataRefOverCustomerIdExtra() throws Exception {
        DataOptions options = new DataOptions();
        options.setDataRef("CUST-DATAREF");
        Map<String, Object> extras = new HashMap<>();
        extras.put("customerId", "CUST-EXTRA");
        options.setExtras(extras);
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockResponse(200, "{}"));

        prefillDataService.getPrefillData(options);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertTrue(captor.getValue().uri().toString().contains("customerId=CUST-DATAREF"));
    }
}
