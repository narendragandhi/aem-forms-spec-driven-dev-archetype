package ${package}.services;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Real, reachable entry point for Interactive Communication generation -
 * GET /bin/bmad/interactive-communication?icPath=...&customerId=...
 * streams back the rendered PDF. Exists so InteractiveCommunicationService
 * isn't another orphaned, unreachable service.
 */
@Component(service = { Servlet.class })
@SlingServletPaths({"/bin/bmad/interactive-communication"})
@ServiceDescription("BMAD Interactive Communication Generation Service")
public class InteractiveCommunicationServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(InteractiveCommunicationServlet.class);

    @Reference
    private transient InteractiveCommunicationService interactiveCommunicationService;

    @Override
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws ServletException, IOException {
        String icPath = req.getParameter("icPath");
        String customerId = req.getParameter("customerId");

        if (icPath == null || icPath.isBlank() || customerId == null || customerId.isBlank()) {
            resp.sendError(SlingHttpServletResponse.SC_BAD_REQUEST, "Missing icPath or customerId");
            return;
        }

        try {
            byte[] pdf = interactiveCommunicationService.generatePrintPdf(icPath, customerId);
            resp.setContentType("application/pdf");
            resp.setContentLength(pdf.length);
            resp.getOutputStream().write(pdf);
        } catch (InteractiveCommunicationException e) {
            LOG.error("Failed to generate Interactive Communication for icPath: {}, customerId: {}", icPath, customerId, e);
            resp.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
