package ${package}.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(service = { Servlet.class })
@SlingServletPaths("/bin/bmad/headless-form-service")
@ServiceDescription("BMAD Headless Form Orchestration Service")
public class HeadlessFormService extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(HeadlessFormService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doGet(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {

        String formPath = req.getParameter("formPath");
        if (formPath == null || formPath.isEmpty()) {
            resp.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing formPath");
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ObjectNode metadata = MAPPER.createObjectNode();
        metadata.put("agentAsCode", true);
        metadata.put("mode", "headless-react");

        ObjectNode wrapper = MAPPER.createObjectNode();
        wrapper.put("bmadVersion", "6.0");
        wrapper.put("formId", String.valueOf(formPath.hashCode()));
        wrapper.put("endpoint", formPath + ".model.json");
        wrapper.put("prefillUrl", "/bin/bmad/mock-finance-data");
        wrapper.put("submitUrl", "/bin/bmad/headless-submit");
        wrapper.set("metadata", metadata);

        resp.getWriter().write(MAPPER.writeValueAsString(wrapper));
    }
}
