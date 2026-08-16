package ${package}.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

/** Read-only health and metrics endpoints for local demos and platform probes. */
@Component(service = Servlet.class)
@SlingServletPaths({"/bin/bmad/observability/health", "/bin/bmad/observability/metrics"})
@ServiceDescription("BMAD Forms observability endpoints")
public class ObservabilityServlet extends SlingSafeMethodsServlet {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private ObservabilityService observability;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ObjectNode node = MAPPER.createObjectNode();
        boolean health = request.getPathInfo() != null && request.getPathInfo().endsWith("/health");
        node.put("status", "UP");
        node.put("foundationVersion", "${foundationVersion}");
        if (!health) {
            node.put("requests", observability.getRequests());
            node.put("submissions", observability.getSubmissions());
            node.put("failures", observability.getFailures());
            node.put("statusPolls", observability.getStatusPolls());
        }
        response.getWriter().write(MAPPER.writeValueAsString(node));
    }
}
