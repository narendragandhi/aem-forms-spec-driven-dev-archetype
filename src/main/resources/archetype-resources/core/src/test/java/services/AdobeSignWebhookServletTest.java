package ${package}.services;

import ${package}.services.impl.AdobeSignOrchestratorImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class AdobeSignWebhookServletTest {

    private static final String CLIENT_ID_HEADER = "X-ADOBESIGN-CLIENTID";

    private AdobeSignWebhookServlet buildServlet(AemContext context, AdobeSignOrchestratorImpl orchestrator) {
        context.registerService(AdobeSignOrchestratorImpl.class, orchestrator);
        return context.registerInjectActivateService(new AdobeSignWebhookServlet());
    }

    // --- GET (verification handshake) ---

    @Test
    void testGetEchoesClientIdHeaderBack(AemContext context) throws ServletException, IOException {
        AdobeSignOrchestratorImpl orchestrator = mock(AdobeSignOrchestratorImpl.class);
        AdobeSignWebhookServlet servlet = buildServlet(context, orchestrator);

        context.request().addHeader(CLIENT_ID_HEADER, "CID-123");
        servlet.doGet(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("CID-123", context.response().getHeader(CLIENT_ID_HEADER));
    }

    @Test
    void testGetWithNoHeaderRespondsWithEmptyHeaderNotError(AemContext context) throws ServletException, IOException {
        AdobeSignOrchestratorImpl orchestrator = mock(AdobeSignOrchestratorImpl.class);
        AdobeSignWebhookServlet servlet = buildServlet(context, orchestrator);

        servlet.doGet(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("", context.response().getHeader(CLIENT_ID_HEADER));
    }

    // --- POST (event notification) ---

    @Test
    void testPostWithMatchingClientIdRecordsAgreementStatus(AemContext context) throws ServletException, IOException {
        AdobeSignOrchestratorImpl orchestrator = mock(AdobeSignOrchestratorImpl.class);
        when(orchestrator.getClientId()).thenReturn("CID-expected");
        AdobeSignWebhookServlet servlet = buildServlet(context, orchestrator);

        context.request().addHeader(CLIENT_ID_HEADER, "CID-expected");
        context.request().setContent("{\"agreement\":{\"id\":\"AGR-1\",\"status\":\"SIGNED\"}}".getBytes(StandardCharsets.UTF_8));

        servlet.doPost(context.request(), context.response());

        verify(orchestrator).recordWebhookStatus("AGR-1", "SIGNED");
        assertEquals(200, context.response().getStatus());
    }

    @Test
    void testPostWithMismatchedClientIdReturns403(AemContext context) throws ServletException, IOException {
        AdobeSignOrchestratorImpl orchestrator = mock(AdobeSignOrchestratorImpl.class);
        when(orchestrator.getClientId()).thenReturn("CID-expected");
        AdobeSignWebhookServlet servlet = buildServlet(context, orchestrator);

        context.request().addHeader(CLIENT_ID_HEADER, "CID-wrong");
        context.request().setContent("{\"agreement\":{\"id\":\"AGR-1\",\"status\":\"SIGNED\"}}".getBytes(StandardCharsets.UTF_8));

        servlet.doPost(context.request(), context.response());

        verify(orchestrator, never()).recordWebhookStatus(anyString(), anyString());
        assertEquals(403, context.response().getStatus());
    }

    @Test
    void testPostWhenClientIdNotConfiguredSkipsValidation(AemContext context) throws ServletException, IOException {
        AdobeSignOrchestratorImpl orchestrator = mock(AdobeSignOrchestratorImpl.class);
        when(orchestrator.getClientId()).thenReturn("");
        AdobeSignWebhookServlet servlet = buildServlet(context, orchestrator);

        context.request().setContent("{\"agreement\":{\"id\":\"AGR-2\",\"status\":\"OUT_FOR_SIGNATURE\"}}".getBytes(StandardCharsets.UTF_8));

        servlet.doPost(context.request(), context.response());

        verify(orchestrator).recordWebhookStatus("AGR-2", "OUT_FOR_SIGNATURE");
        assertEquals(200, context.response().getStatus());
    }

    @Test
    void testPostWithMissingAgreementFieldsIgnoresPayload(AemContext context) throws ServletException, IOException {
        AdobeSignOrchestratorImpl orchestrator = mock(AdobeSignOrchestratorImpl.class);
        when(orchestrator.getClientId()).thenReturn("");
        AdobeSignWebhookServlet servlet = buildServlet(context, orchestrator);

        context.request().setContent("{}".getBytes(StandardCharsets.UTF_8));

        servlet.doPost(context.request(), context.response());

        verify(orchestrator, never()).recordWebhookStatus(anyString(), anyString());
        assertEquals(200, context.response().getStatus());
    }

    @Test
    void testPostWithMalformedJsonDoesNotThrow(AemContext context) throws ServletException, IOException {
        AdobeSignOrchestratorImpl orchestrator = mock(AdobeSignOrchestratorImpl.class);
        when(orchestrator.getClientId()).thenReturn("");
        AdobeSignWebhookServlet servlet = buildServlet(context, orchestrator);

        context.request().setContent("not-json".getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> servlet.doPost(context.request(), context.response()));
        verify(orchestrator, never()).recordWebhookStatus(anyString(), anyString());
        assertEquals(200, context.response().getStatus());
    }
}
