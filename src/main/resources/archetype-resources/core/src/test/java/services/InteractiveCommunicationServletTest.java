package ${package}.services;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class InteractiveCommunicationServletTest {

    private InteractiveCommunicationServlet buildServlet(AemContext context, InteractiveCommunicationService service) {
        context.registerService(InteractiveCommunicationService.class, service);
        return context.registerInjectActivateService(new InteractiveCommunicationServlet());
    }

    @Test
    void testMissingIcPathReturns400(AemContext context) throws ServletException, IOException {
        InteractiveCommunicationService service = mock(InteractiveCommunicationService.class);
        InteractiveCommunicationServlet servlet = buildServlet(context, service);

        context.request().addRequestParameter("customerId", "CUST-1");
        servlet.doGet(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        verifyNoInteractions(service);
    }

    @Test
    void testMissingCustomerIdReturns400(AemContext context) throws ServletException, IOException {
        InteractiveCommunicationService service = mock(InteractiveCommunicationService.class);
        InteractiveCommunicationServlet servlet = buildServlet(context, service);

        context.request().addRequestParameter("icPath", "/content/dam/formsanddocuments/ic/AcmeApp/account-statement");
        servlet.doGet(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        verifyNoInteractions(service);
    }

    @Test
    void testSuccessReturnsPdfContentTypeAndBytes(AemContext context) throws Exception {
        InteractiveCommunicationService service = mock(InteractiveCommunicationService.class);
        byte[] pdfBytes = new byte[]{'%', 'P', 'D', 'F'};
        when(service.generatePrintPdf("/content/dam/formsanddocuments/ic/AcmeApp/account-statement", "CUST-1")).thenReturn(pdfBytes);
        InteractiveCommunicationServlet servlet = buildServlet(context, service);

        context.request().addRequestParameter("icPath", "/content/dam/formsanddocuments/ic/AcmeApp/account-statement");
        context.request().addRequestParameter("customerId", "CUST-1");
        servlet.doGet(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("application/pdf", context.response().getContentType());
        assertArrayEquals(pdfBytes, context.response().getOutput());
    }

    @Test
    void testServiceExceptionReturns500(AemContext context) throws Exception {
        InteractiveCommunicationService service = mock(InteractiveCommunicationService.class);
        when(service.generatePrintPdf(anyString(), anyString())).thenThrow(new InteractiveCommunicationException("render failed"));
        InteractiveCommunicationServlet servlet = buildServlet(context, service);

        context.request().addRequestParameter("icPath", "/content/dam/formsanddocuments/ic/AcmeApp/account-statement");
        context.request().addRequestParameter("customerId", "CUST-1");
        servlet.doGet(context.request(), context.response());

        assertEquals(500, context.response().getStatus());
    }
}
