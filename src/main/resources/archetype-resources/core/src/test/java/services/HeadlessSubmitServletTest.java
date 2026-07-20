package ${package}.services;

import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertTrue(context.response().getContentType().startsWith("application/json"));
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

    @Test
    void testGetWithWorkflowSessionReturnsWorkflowStatus(AemContext context) throws Exception {
        Workflow workflow = mock(Workflow.class);
        MetaDataMap metaData = mock(MetaDataMap.class);
        when(workflow.getState()).thenReturn("RUNNING");
        when(workflow.getMetaDataMap()).thenReturn(metaData);
        when(metaData.get("signingStatus", String.class)).thenReturn("SIGNED");
        when(metaData.get("dorStatus", String.class)).thenReturn("COMPLETED");

        WorkflowSession session = mock(WorkflowSession.class);
        when(session.getWorkflow("WF-real")).thenReturn(workflow);
        context.registerAdapter(ResourceResolver.class, WorkflowSession.class, session);

        context.request().addRequestParameter("workflowId", "WF-real");
        servlet.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertTrue(output.contains("\"state\":\"RUNNING\""));
        assertTrue(output.contains("\"signingStatus\":\"SIGNED\""));
        assertTrue(output.contains("\"dorStatus\":\"COMPLETED\""));
    }

    @Test
    void testGetWithWorkflowMissingMetadataUsesDefaults(AemContext context) throws Exception {
        Workflow workflow = mock(Workflow.class);
        MetaDataMap metaData = mock(MetaDataMap.class);
        when(workflow.getState()).thenReturn(null);
        when(workflow.getMetaDataMap()).thenReturn(metaData);

        WorkflowSession session = mock(WorkflowSession.class);
        when(session.getWorkflow("WF-empty")).thenReturn(workflow);
        context.registerAdapter(ResourceResolver.class, WorkflowSession.class, session);

        context.request().addRequestParameter("workflowId", "WF-empty");
        servlet.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertTrue(output.contains("\"state\":\"UNKNOWN\""));
        assertTrue(output.contains("\"signingStatus\":\"PENDING\""));
        assertTrue(output.contains("\"dorStatus\":\"NOT_STARTED\""));
    }

    @Test
    void testGetWithUnknownWorkflowIdFallsBack(AemContext context) throws Exception {
        WorkflowSession session = mock(WorkflowSession.class);
        when(session.getWorkflow("WF-unknown")).thenReturn(null);
        context.registerAdapter(ResourceResolver.class, WorkflowSession.class, session);

        context.request().addRequestParameter("workflowId", "WF-unknown");
        servlet.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertTrue(output.contains("WF-unknown"));
        assertTrue(output.contains("\"state\":\"RUNNING\""));
    }

    @Test
    void testGetWorkflowLookupFailureReturns500(AemContext context) throws Exception {
        WorkflowSession session = mock(WorkflowSession.class);
        when(session.getWorkflow("WF-boom")).thenThrow(new RuntimeException("lookup failed"));
        context.registerAdapter(ResourceResolver.class, WorkflowSession.class, session);

        context.request().addRequestParameter("workflowId", "WF-boom");
        servlet.doGet(context.request(), context.response());

        assertEquals(500, context.response().getStatus());
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
        assertTrue(context.response().getOutputAsString().contains("\"status\":\"error\""));
    }

    @Test
    void testPostResponseContentType(AemContext context) throws ServletException, IOException {
        context.request().setContent("{}".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertTrue(context.response().getContentType().startsWith("application/json"));
    }

    @Test
    void testPostSuccessResponseContainsSuccessStatus(AemContext context) throws ServletException, IOException {
        context.request().setContent("{\"name\":\"Test\"}".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("\"status\":\"success\""));
    }

    @Test
    void testPostWithEmptyBodyReturns200(AemContext context) throws ServletException, IOException {
        context.request().setContent("".getBytes(StandardCharsets.UTF_8));
        context.request().setContentType("application/json");
        servlet.doPost(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
    }
}
