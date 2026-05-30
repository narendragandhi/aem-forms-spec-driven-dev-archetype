package ${package}.core.servlets;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class MockFinanceDataServletTest {

    private final MockFinanceDataServlet servlet = new MockFinanceDataServlet();

    @Test
    void testGetReturns200(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
    }

    @Test
    void testGetResponseContentType(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
    }

    @Test
    void testGetResponseCharacterEncoding(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertEquals("UTF-8", context.response().getCharacterEncoding());
    }

    @Test
    void testGetResponseIsNotEmpty(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        String output = context.response().getOutputAsString();
        assertNotNull(output);
        assertFalse(output.isEmpty());
    }

    @Test
    void testGetResponseContainsCustomer(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("customer"));
    }

    @Test
    void testGetResponseContainsEmploymentHistory(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("employmentHistory"));
    }

    @Test
    void testGetResponseContainsTransactions(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("transactions"));
    }

    @Test
    void testGetResponseContainsCustomerId(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("CUST-10293"));
    }

    @Test
    void testGetResponseContainsCustomerStatus(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("Premier"));
    }

    @Test
    void testGetResponseIsValidJsonStructure(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        String output = context.response().getOutputAsString().trim();
        assertTrue(output.startsWith("{"), "Response should start with {");
        assertTrue(output.endsWith("}"), "Response should end with }");
    }

    @Test
    void testGetIsIdempotent(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        String output1 = context.response().getOutputAsString();

        AemContext context2 = new AemContext();
        servlet.doGet(context2.request(), context2.response());
        String output2 = context2.response().getOutputAsString();

        assertEquals(output1, output2, "GET should return same data on every call");
    }
}
