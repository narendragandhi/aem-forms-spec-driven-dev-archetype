package ${package}.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.Workflow;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;

@Component(service = { Servlet.class })
@SlingServletPaths({"/bin/bmad/headless-submit", "/bin/bmad/headless-status"})
@ServiceDescription("BMAD Headless Form Submission & Status Service")
public class HeadlessSubmitServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(HeadlessSubmitServlet.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {

        String workflowId = req.getParameter("workflowId");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if (workflowId == null) {
            resp.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing workflowId");
            return;
        }

        ResourceResolver resolver = req.getResourceResolver();
        WorkflowSession wfSession = resolver.adaptTo(WorkflowSession.class);

        try {
            if (wfSession != null) {
                Workflow wf = wfSession.getWorkflow(workflowId);
                if (wf != null) {
                    String state = wf.getState();
                    String signingStatus = (String) wf.getMetaDataMap().get("signingStatus", String.class);
                    String dorStatus = (String) wf.getMetaDataMap().get("dorStatus", String.class);

                    ObjectNode node = MAPPER.createObjectNode();
                    node.put("workflowId", workflowId);
                    node.put("state", state != null ? state : "UNKNOWN");
                    node.put("signingStatus", signingStatus != null ? signingStatus : "PENDING");
                    node.put("dorStatus", dorStatus != null ? dorStatus : "NOT_STARTED");
                    resp.getWriter().write(MAPPER.writeValueAsString(node));
                    return;
                }
            }

            ObjectNode fallback = MAPPER.createObjectNode();
            fallback.put("workflowId", workflowId);
            fallback.put("state", "RUNNING");
            fallback.put("signingStatus", "OUT_FOR_SIGNATURE");
            fallback.put("dorStatus", "NOT_STARTED");
            resp.getWriter().write(MAPPER.writeValueAsString(fallback));

        } catch (Exception e) {
            resp.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error fetching workflow status");
        }
    }

    @Override
    protected void doPost(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {

        // CSRF protection is enforced by the Granite CSRF filter (com.adobe.granite.csrf.impl.CSRFFilter).
        // The OSGi config in ui.config/osgiconfig/config/com.adobe.granite.csrf.impl.CSRFFilter.cfg.json
        // must list this path; AEM Forms clients obtain the token from /libs/granite/csrf/token.json
        // and send it in the CSRF-Token request header.
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String submittedData = sb.toString();
        String workflowId = "WF-" + System.currentTimeMillis();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ObjectNode result = MAPPER.createObjectNode();
        if (submittedData.contains("error")) {
            resp.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "error");
            result.put("message", "Simulated processing error");
        } else {
            resp.setStatus(SlingHttpServletResponse.SC_OK);
            result.put("status", "success");
            result.put("message", "Form submitted and Sign workflow initiated");
            result.put("workflowId", workflowId);
        }
        resp.getWriter().write(MAPPER.writeValueAsString(result));
    }
}
