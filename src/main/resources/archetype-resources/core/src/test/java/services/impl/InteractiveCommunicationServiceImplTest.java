package ${package}.services.impl;

import ${package}.services.InteractiveCommunicationException;
import com.adobe.aem.forms.ic.exception.ICException;
import com.adobe.aem.forms.ic.print.api.PrintChannelRenderService;
import com.adobe.aem.forms.ic.print.model.IcPdfRenderOptions;
import com.adobe.aemfd.docmanager.Document;
import com.adobe.aemfd.docmanager.DocumentFactory;
import com.adobe.fd.output.api.OutputService;
import com.adobe.fd.output.api.OutputServiceException;
import com.adobe.fd.output.api.PDFOutputOptions;
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
    private OutputService outputService;

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
        setField("outputService", outputService);
        setField("customerDataEndpoint", ENDPOINT);
        setField("locale", "en");
        setField("customerDataUsername", "");
        setField("customerDataPassword", "");
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
    void testGeneratePrintPdfOmitsAuthorizationHeaderByDefault() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(printChannelRenderService.renderPdf(anyString(), any(Document.class), isNull(), any(IcPdfRenderOptions.class)))
            .thenReturn(new InMemoryDocument("bytes".getBytes(StandardCharsets.UTF_8)));

        service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-43");

        org.mockito.ArgumentCaptor<HttpRequest> captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertTrue(captor.getValue().headers().firstValue("Authorization").isEmpty(),
            "No username configured (the archetype default) -> no Authorization header sent");
    }

    @Test
    void testGeneratePrintPdfSendsBasicAuthWhenCredentialsConfigured() throws Exception {
        setField("customerDataUsername", "admin");
        setField("customerDataPassword", "admin");
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(printChannelRenderService.renderPdf(anyString(), any(Document.class), isNull(), any(IcPdfRenderOptions.class)))
            .thenReturn(new InMemoryDocument("bytes".getBytes(StandardCharsets.UTF_8)));

        service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-44");

        org.mockito.ArgumentCaptor<HttpRequest> captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        String expected = "Basic " + java.util.Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, captor.getValue().headers().firstValue("Authorization").orElse(null));
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

    // --- OutputService fallback (printChannelRenderService unbound) --------

    @Test
    void testGeneratePrintPdfFallsBackToOutputServiceWhenPrintChannelUnbound() throws Exception {
        setField("printChannelRenderService", null);
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockHttpResponse(200, "{\"customer\":{\"name\":\"Jane\"}}".getBytes(StandardCharsets.UTF_8)));
        byte[] renderedBytes = "%PDF-fallback".getBytes(StandardCharsets.UTF_8);
        when(outputService.generatePDFOutput(anyString(), any(Document.class), any(PDFOutputOptions.class)))
            .thenReturn(new InMemoryDocument(renderedBytes));

        byte[] result = service.generatePrintPdf("/content/dam/formsanddocuments/RealTestApp/dor-template.xdp", "CUST-7");

        assertArrayEquals(renderedBytes, result);
        verifyNoInteractions(printChannelRenderService);
    }

    @Test
    void testGeneratePrintPdfFallbackConvertsJsonCustomerDataToXml() throws Exception {
        setField("printChannelRenderService", null);
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockHttpResponse(200,
                "{\"customer\":{\"name\":\"Jane\",\"id\":\"CUST-8\"}}".getBytes(StandardCharsets.UTF_8)));
        when(outputService.generatePDFOutput(anyString(), any(Document.class), any(PDFOutputOptions.class)))
            .thenReturn(new InMemoryDocument("bytes".getBytes(StandardCharsets.UTF_8)));

        service.generatePrintPdf("/content/dam/formsanddocuments/RealTestApp/dor-template.xdp", "CUST-8");

        org.mockito.ArgumentCaptor<Document> captor = org.mockito.ArgumentCaptor.forClass(Document.class);
        verify(outputService).generatePDFOutput(anyString(), captor.capture(), any(PDFOutputOptions.class));
        String xml = new String(captor.getValue().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(xml.startsWith("<?xml"));
        assertTrue(xml.contains("<data>"));
        assertTrue(xml.contains("<customer>"));
        assertTrue(xml.contains("<name>Jane</name>"));
        assertTrue(xml.contains("<id>CUST-8</id>"));
    }

    @Test
    void testGeneratePrintPdfFallbackPassesTemplatePathAndConfiguredLocale() throws Exception {
        setField("printChannelRenderService", null);
        setField("locale", "fr");
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(outputService.generatePDFOutput(anyString(), any(Document.class), any(PDFOutputOptions.class)))
            .thenReturn(new InMemoryDocument("bytes".getBytes(StandardCharsets.UTF_8)));

        service.generatePrintPdf("/content/dam/formsanddocuments/RealTestApp/dor-template.xdp", "CUST-9");

        org.mockito.ArgumentCaptor<String> pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<PDFOutputOptions> optionsCaptor = org.mockito.ArgumentCaptor.forClass(PDFOutputOptions.class);
        verify(outputService).generatePDFOutput(pathCaptor.capture(), any(Document.class), optionsCaptor.capture());
        // crx:// prefix required - confirmed live: a bare repository path
        // fails with AEM_OUT_001_020 "Invalid template" / FileResource
        // "No File Found".
        assertEquals("crx:///content/dam/formsanddocuments/RealTestApp/dor-template.xdp", pathCaptor.getValue());
        assertEquals("fr", optionsCaptor.getValue().getLocale());
    }

    @Test
    void testGeneratePrintPdfFallbackThrowsWhenOutputServiceThrows() throws Exception {
        setField("printChannelRenderService", null);
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(outputService.generatePDFOutput(anyString(), any(Document.class), any(PDFOutputOptions.class)))
            .thenThrow(mock(OutputServiceException.class));

        InteractiveCommunicationException e = assertThrows(InteractiveCommunicationException.class,
            () -> service.generatePrintPdf("/content/dam/formsanddocuments/RealTestApp/dor-template.xdp", "CUST-10"));
        assertNotNull(e.getCause());
        assertInstanceOf(OutputServiceException.class, e.getCause());
    }

    @Test
    void testGeneratePrintPdfStillPrefersPrintChannelWhenBothAvailable() throws Exception {
        // printChannelRenderService is bound (default setUp) - the fallback
        // must not be used just because OutputService is also available.
        when(httpClient.send(any(HttpRequest.class), any()))
            .thenAnswer(invocation -> mockHttpResponse(200, "{}".getBytes(StandardCharsets.UTF_8)));
        when(printChannelRenderService.renderPdf(anyString(), any(Document.class), isNull(), any(IcPdfRenderOptions.class)))
            .thenReturn(new InMemoryDocument("bytes".getBytes(StandardCharsets.UTF_8)));

        service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-11");

        verifyNoInteractions(outputService);
    }
}
