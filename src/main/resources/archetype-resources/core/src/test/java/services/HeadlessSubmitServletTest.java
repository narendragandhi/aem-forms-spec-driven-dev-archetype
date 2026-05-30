package ${package}.services;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class HeadlessSubmitServletTest {

    private final HeadlessSubmitServlet servlet = new HeadlessSubmitServlet();

    // --- GET (status polling) ---

    @Test
    void testGetWithMissingWorkflowIdReturns400(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertEquals(400, context.response().getStatus());
    }

    @Test
    void testGetWithWorkflowIdReturnsFallbackJson(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("workflowId", "WF-12345");
        servlet.doGet(context.request(), context.response());
        String output = context.response().getOutputAsString();
        assertTrue(output.contains("WF-12345"));
        assertTrue(output.contains("state"));
    }

    @Test
    void testGetResponseContentType(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("workflowId", "WF-12345");
        servlet.doGet(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
    }

    @Test
    void testGetFallbackJsonContainsSigningStatus(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("workflowId", "WF-test");
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("signingStatus"));
    }

    @Test
    void testGetFallbackJsonContainsDorStatus(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("workflowId", "WF-test");
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("dorStatus"));
    }

    // --- POST (form submission) ---

    @Test
    void testPostSuccessReturns200(AemContext context) throws ServletException, IOException {
        context.request().setContent("{\"name\":\"John Doe\"}".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
    }

    @Test
    void testPostSuccessResponseContainsWorkflowId(AemContext context) throws ServletException, IOException {
        context.request().setContent("{\"name\":\"Jane\"}".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        String output = context.response().getOutputAsString();
        assertTrue(output.contains("workflowId"));
        assertTrue(output.contains("WF-"));
    }

    @Test
    void testPostWithErrorBodyReturns500(AemContext context) throws ServletException, IOException {
        context.request().setContent("{\"error\":true}".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertEquals(500, context.response().getStatus());
    }

    @Test
    void testPostWithErrorBodyReturnsErrorStatus(AemContext context) throws ServletException, IOException {
        context.request().setContent("{\"error\":true}".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("\"status\": \"error\""));
    }

    @Test
    void testPostResponseContentType(AemContext context) throws ServletException, IOException {
        context.request().setContent("{}".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
    }

    @Test
    void testPostSuccessResponseContainsSuccessStatus(AemContext context) throws ServletException, IOException {
        context.request().setContent("{\"name\":\"Test\"}".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("\"status\": \"success\""));
    }

    @Test
    void testPostWithEmptyBodyReturns200(AemContext context) throws ServletException, IOException {
        context.request().setContent("".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
    }
}
