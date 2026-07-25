package ${package}.services.impl;

import ${package}.services.InteractiveCommunicationException;
import com.adobe.aem.forms.ic.exception.ICException;
import com.adobe.aem.forms.ic.print.api.PrintChannelRenderService;
import com.adobe.aem.forms.ic.print.model.IcPdfRenderOptions;
import com.adobe.aemfd.docmanager.Document;
import com.adobe.aemfd.docmanager.DocumentFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Mocks PrintChannelRenderService directly (a real interface) and
 * java.net.http.HttpClient for the customer-data fetch. Does not exercise
 * a live AEM instance - on the instance this was built against,
 * PrintChannelRenderServiceImpl itself doesn't activate (see README), so
 * this is the same "verified against real API shape, not live-tested"
 * bar as AdobeSignOrchestratorImplTest.
 *
 * com.adobe.aemfd.docmanager.Document's byte[]/String/etc. constructors
 * delegate to the static DocumentFactory.getInstance() singleton, which is
 * null outside a real AEM runtime (verified via javap - not an assumption).
 * A minimal in-memory DocumentFactory is installed here so both this
 * test's own Document use and the real production code's
 * `new Document(customerData)` call work without a live instance.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InteractiveCommunicationServiceImplTest {

    private static final String ENDPOINT = "http://localhost:4502/bin/bmad/mock-finance-data";

    private InteractiveCommunicationServiceImpl service;

    @Mock
    private PrintChannelRenderService printChannelRenderService;

    @Mock
    private HttpClient httpClient;

    @BeforeAll
    static void installInMemoryDocumentFactory() {
        DocumentFactory.setInstance(new DocumentFactory() {
            @Override
            public Document newDocument(byte[] bytes) {
                return new InMemoryDocument(bytes);
            }

            @Override
            public Document newDocument(File file) {
                throw new UnsupportedOperationException("not used by this test");
            }

            @Override
            public Document newDocument(File file, boolean copy) {
                throw new UnsupportedOperationException("not used by this test");
            }

            @Override
            public Document newDocument(URL url) {
                throw new UnsupportedOperationException("not used by this test");
            }

            @Override
            public Document newDocument(InputStream inputStream) {
                throw new UnsupportedOperationException("not used by this test");
            }

            @Override
            public Document newDocument(String path) {
                throw new UnsupportedOperationException("not used by this test");
            }

            @Override
            public Document newDocument(String path, ResourceResolver resolver) {
                throw new UnsupportedOperationException("not used by this test");
            }

            @Override
            public Document newDocument(String path, ResourceResolver resolver, boolean b) {
                throw new UnsupportedOperationException("not used by this test");
            }
        });
    }

    /** Bypasses Document's delegate-to-DocumentFactory machinery via its no-arg constructor, which doesn't touch it. */
    private static class InMemoryDocument extends Document {
        private final byte[] bytes;

        InMemoryDocument(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public long length() {
            return bytes.length;
        }

        @Override
        public void dispose() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        service = new InteractiveCommunicationServiceImpl();
        setField("printChannelRenderService", printChannelRenderService);
        setField("customerDataEndpoint", ENDPOINT);
        setField("locale", "en");
        service.httpClient = httpClient;
    }

    private void setField(String name, Object value) throws Exception {
        Field field = InteractiveCommunicationServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> mockHttpResponse(int status, byte[] body) {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    @Test
    void testGeneratePrintPdfReturnsRenderedBytes() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockHttpResponse(200, "{\"customer\":{}}".getBytes(StandardCharsets.UTF_8)));
        byte[] renderedBytes = "%PDF-rendered".getBytes(StandardCharsets.UTF_8);
        when(printChannelRenderService.renderPdf(anyString(), any(Document.class), isNull(), any(IcPdfRenderOptions.class)))
            .thenReturn(new InMemoryDocument(renderedBytes));

        byte[] result = service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-1");

        assertArrayEquals(renderedBytes, result);
    }

    @Test
    void testGeneratePrintPdfPassesIcContentPathThrough() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(printChannelRenderService.renderPdf(anyString(), any(Document.class), isNull(), any(IcPdfRenderOptions.class)))
            .thenReturn(new InMemoryDocument("bytes".getBytes(StandardCharsets.UTF_8)));

        service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/welcome-kit", "CUST-2");

        verify(printChannelRenderService).renderPdf(eq("/content/dam/formsanddocuments/ic/AcmeApp/welcome-kit"), any(Document.class), isNull(), any(IcPdfRenderOptions.class));
    }

    @Test
    void testGeneratePrintPdfPassesConfiguredLocale() throws Exception {
        setField("locale", "fr");
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(printChannelRenderService.renderPdf(anyString(), any(Document.class), isNull(), any(IcPdfRenderOptions.class)))
            .thenReturn(new InMemoryDocument("bytes".getBytes(StandardCharsets.UTF_8)));

        service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-3");

        org.mockito.ArgumentCaptor<IcPdfRenderOptions> captor = org.mockito.ArgumentCaptor.forClass(IcPdfRenderOptions.class);
        verify(printChannelRenderService).renderPdf(anyString(), any(Document.class), isNull(), captor.capture());
        assertEquals("fr", captor.getValue().getLocale());
    }

    @Test
    void testGeneratePrintPdfFetchesFromConfiguredEndpointWithCustomerId() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(printChannelRenderService.renderPdf(anyString(), any(Document.class), isNull(), any(IcPdfRenderOptions.class)))
            .thenReturn(new InMemoryDocument("bytes".getBytes(StandardCharsets.UTF_8)));

        service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-42");

        org.mockito.ArgumentCaptor<HttpRequest> captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertTrue(captor.getValue().uri().toString().startsWith(ENDPOINT));
        assertTrue(captor.getValue().uri().toString().contains("CUST-42"));
    }

    @Test
    void testGeneratePrintPdfThrowsWhenCustomerDataFetchFails() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockHttpResponse(500, "{}".getBytes(StandardCharsets.UTF_8)));

        assertThrows(InteractiveCommunicationException.class,
            () -> service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-4"));
        verifyNoInteractions(printChannelRenderService);
    }

    @Test
    void testGeneratePrintPdfThrowsOnHttpIOException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new java.io.IOException("connection refused"));

        assertThrows(InteractiveCommunicationException.class,
            () -> service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-5"));
        verifyNoInteractions(printChannelRenderService);
    }

    @Test
    void testGeneratePrintPdfThrowsWhenRenderThrowsICException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(printChannelRenderService.renderPdf(anyString(), any(Document.class), isNull(), any(IcPdfRenderOptions.class)))
            .thenThrow(new ICException("render failed"));

        InteractiveCommunicationException e = assertThrows(InteractiveCommunicationException.class,
            () -> service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-6"));
        assertNotNull(e.getCause());
        assertInstanceOf(ICException.class, e.getCause());
    }
}
