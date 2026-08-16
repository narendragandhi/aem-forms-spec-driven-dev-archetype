package ${package}.services;

import ${package}.services.impl.AdobeSignOrchestratorImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Receives real Adobe Sign webhook notifications. Must be reachable over a
 * public HTTPS URL for Adobe to deliver events to it - see README for the
 * tunnel/deployment note. AdobeSignOrchestratorImpl.getStatus() still works
 * without this (it falls back to a live API call), so this servlet is an
 * optimization/faster-status-update path, not a hard dependency.
 */
@Component(service = { Servlet.class })
@SlingServletPaths({"/bin/bmad/adobe-sign-webhook"})
@ServiceDescription("BMAD Adobe Sign Webhook Receiver")
public class AdobeSignWebhookServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AdobeSignWebhookServlet.class);
    private static final String CLIENT_ID_HEADER = "X-ADOBESIGN-CLIENTID";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private transient AdobeSignOrchestratorImpl adobeSignOrchestrator;

    /**
     * Adobe Sign sends a GET verification request when a webhook is
     * registered and expects the client ID header echoed back.
     */
    @Override
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws IOException {
        String clientId = req.getHeader(CLIENT_ID_HEADER);
        resp.setHeader(CLIENT_ID_HEADER, clientId != null ? clientId : "");
        resp.setStatus(SlingHttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(SlingHttpServletRequest req, SlingHttpServletResponse resp)
            throws ServletException, IOException {

        String receivedClientId = req.getHeader(CLIENT_ID_HEADER);
        String expectedClientId = adobeSignOrchestrator.getClientId();
        if (expectedClientId != null && !expectedClientId.isBlank() && !expectedClientId.equals(receivedClientId)) {
            LOG.warn("Rejected Adobe Sign webhook with unexpected {} header", CLIENT_ID_HEADER);
            resp.sendError(SlingHttpServletResponse.SC_FORBIDDEN, "Unrecognized client id");
            return;
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        try {
            JsonNode payload = MAPPER.readTree(body.toString());
            JsonNode agreement = payload.get("agreement");
            if (agreement != null && agreement.get("id") != null && agreement.get("status") != null) {
                adobeSignOrchestrator.recordWebhookStatus(agreement.get("id").asText(), agreement.get("status").asText());
            } else {
                // Never log webhook payloads: they may contain signer and document data.
                LOG.debug("Adobe Sign webhook payload had no agreement id/status, ignoring");
            }
        } catch (IOException e) {
            LOG.warn("Could not parse Adobe Sign webhook payload", e);
        }

        // Respond promptly - Adobe Sign expects a fast 200 and retries on timeout/failure.
        resp.setStatus(SlingHttpServletResponse.SC_OK);
    }
}
