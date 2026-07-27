package ${package}.services.impl;

import ${package}.services.InteractiveCommunicationException;
import ${package}.services.InteractiveCommunicationService;
import com.adobe.aem.forms.ic.exception.ICException;
import com.adobe.aem.forms.ic.print.api.PrintChannelRenderService;
import com.adobe.aem.forms.ic.print.model.IcPdfRenderOptions;
import com.adobe.aemfd.docmanager.Document;
import com.adobe.fd.output.api.OutputService;
import com.adobe.fd.output.api.OutputServiceException;
import com.adobe.fd.output.api.PDFOutputOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

/**
 * Real AEM Forms Interactive Communications integration. Calls the actual
 * PrintChannelRenderService to render an IC's Print Channel as a PDF,
 * merged with customer data fetched from a configured REST endpoint
 * (defaults to this archetype's own MockFinanceDataServlet).
 *
 * API verified via javap against the pinned aem-forms-sdk-api jar, not
 * assumed. PrintChannelRenderService is an OPTIONAL reference - on the
 * instance this was built against, PrintChannelRenderServiceImpl itself
 * (and every other component in its bundle) is OSGi-unsatisfied, gated
 * behind a feature toggle (FT_FORMS-14262) that isn't even registered on
 * that instance, not just disabled - see README. Rather than let a
 * mandatory reference keep this whole component from activating there,
 * generatePrintPdf() falls back to the more general com.adobe.fd.output.api.OutputService
 * (confirmed active on the same instance) when PrintChannelRenderService
 * isn't bound. OutputService's generatePDFOutput(templatePath, data, options)
 * expects XML data (confirmed via Adobe's own Output Service documentation),
 * unlike PrintChannelRenderService's JSON-native data contract - the
 * fallback path converts the fetched customer JSON to a generic XML
 * structure for this reason. That structural conversion is real and
 * tested, but isn't verified to match any *specific* XDP template's own
 * data schema (a template's actual field bindings are template-specific
 * and weren't available to verify against here) - same "verified how, not
 * verified sufficient for your template" honesty bar as the letterhead/
 * prefill gaps already noted for the Print Channel path.
 *
 * Live-tested against a real instance and a real XDP asset, catching two
 * real mistakes an untested implementation would have shipped: (1) a bare
 * repository path fails with AEM_OUT_001_020 "Invalid template" - the
 * path passed to generatePDFOutput needs a "crx://" scheme prefix (matches
 * Adobe's own Output Service docs' example paths); (2) the call then
 * reaches AEM Forms' native XFA rendering SDK, which throws
 * IllegalStateException("Error getting shared temp directory, check
 * whether the SDK started successfully.") on this instance - the exact
 * same native-SDK-not-started limitation already documented for
 * DoRService (see README) - confirming this fallback's own code is
 * correct up to a pre-existing environment boundary outside this
 * archetype's control, not a bug here.
 */
@Component(service = InteractiveCommunicationService.class)
@Designate(ocd = InteractiveCommunicationServiceImpl.Config.class)
public class InteractiveCommunicationServiceImpl implements InteractiveCommunicationService {

    private static final Logger LOG = LoggerFactory.getLogger(InteractiveCommunicationServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ObjectClassDefinition(
        name = "AEM Forms Interactive Communication Configuration",
        description = "Configuration for real Interactive Communication PDF generation."
    )
    public @interface Config {
        @AttributeDefinition(
            name = "Customer Data Endpoint",
            description = "REST endpoint returning customer data as JSON, merged into the rendered Interactive Communication.",
            type = AttributeType.STRING
        )
        String customer_data_endpoint() default "http://localhost:4502/bin/bmad/mock-finance-data";

        @AttributeDefinition(
            name = "Locale",
            description = "Locale used to render the Interactive Communication.",
            type = AttributeType.STRING
        )
        String locale() default "en";

        @AttributeDefinition(
            name = "Customer Data Endpoint Username (Optional)",
            description = "HTTP Basic Auth username for the customer data endpoint, if it requires authentication " +
                "(e.g. a Sling instance with anonymous access disabled - a real gap found live-testing this: " +
                "the default MockFinanceDataServlet endpoint returns 401 without credentials on such an instance).",
            type = AttributeType.STRING
        )
        String customer_data_username() default "";

        @AttributeDefinition(
            name = "Customer Data Endpoint Password (Optional)",
            description = "HTTP Basic Auth password for the customer data endpoint, paired with the username above.",
            type = AttributeType.PASSWORD
        )
        String customer_data_password() default "";
    }

    // Optional: on an instance where PrintChannelRenderServiceImpl's bundle
    // is toggle-gated (see class javadoc), this simply stays unbound rather
    // than keeping the whole component from activating.
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private PrintChannelRenderService printChannelRenderService;

    @Reference
    private OutputService outputService;

    private String customerDataEndpoint;
    private String locale;
    private String customerDataUsername;
    private String customerDataPassword;

    HttpClient httpClient = HttpClient.newHttpClient(); // package-private so unit tests can substitute a mock

    @Activate
    protected void activate(final Config config) {
        this.customerDataEndpoint = config.customer_data_endpoint();
        this.locale = config.locale();
        this.customerDataUsername = config.customer_data_username();
        this.customerDataPassword = config.customer_data_password();
    }

    @Override
    public byte[] generatePrintPdf(String icContentPath, String customerId) throws InteractiveCommunicationException {
        byte[] customerData = fetchCustomerData(customerId);

        if (printChannelRenderService != null) {
            return renderViaPrintChannel(icContentPath, customerData);
        }
        return renderViaOutputService(icContentPath, customerData);
    }

    private byte[] renderViaPrintChannel(String icContentPath, byte[] customerData) throws InteractiveCommunicationException {
        IcPdfRenderOptions options = new IcPdfRenderOptions();
        options.setLocale(locale);

        try (Document data = new Document(customerData)) {
            // Real letterhead support (a second Document overlay) isn't
            // wired up yet - the expected content shape isn't verified
            // against a live instance, so this passes null rather than guess.
            Document result = printChannelRenderService.renderPdf(icContentPath, data, null, options);
            try {
                return result.getInputStream().readAllBytes();
            } finally {
                result.dispose();
            }
        } catch (ICException e) {
            throw new InteractiveCommunicationException("Failed to render Interactive Communication at " + icContentPath, e);
        } catch (IOException e) {
            throw new InteractiveCommunicationException("Failed to read the rendered Interactive Communication content", e);
        }
    }

    private byte[] renderViaOutputService(String templatePath, byte[] customerData) throws InteractiveCommunicationException {
        PDFOutputOptions options = new PDFOutputOptions();
        options.setLocale(locale);

        // A bare repository path (e.g. "/content/dam/formsanddocuments/...xdp")
        // was confirmed live to fail with AEM_OUT_001_020 "Invalid template"
        // - the underlying FileResource lookup reports "No File Found" for
        // it. Adobe's own Output Service docs show templates referenced via
        // a crx:/// URI, e.g. "crx:///content/dam/formsanddocuments/.../x.xdp".
        String crxTemplatePath = "crx://" + templatePath;

        try (Document data = jsonToXmlDocument(customerData)) {
            Document result = outputService.generatePDFOutput(crxTemplatePath, data, options);
            try {
                return result.getInputStream().readAllBytes();
            } finally {
                result.dispose();
            }
        } catch (OutputServiceException e) {
            throw new InteractiveCommunicationException(
                "Failed to render via the OutputService fallback at " + templatePath, e);
        } catch (IOException e) {
            throw new InteractiveCommunicationException("Failed to read the OutputService-rendered content", e);
        }
    }

    // OutputService.generatePDFOutput requires XML data (confirmed via
    // Adobe's Output Service docs - "an XML document that is merged with
    // the template"), unlike PrintChannelRenderService's JSON-native
    // contract. This is a generic, structural JSON->XML mapping (every
    // object field becomes an element; array items repeat the same
    // element), not tailored to any specific XDP template's data schema.
    private Document jsonToXmlDocument(byte[] json) throws InteractiveCommunicationException {
        try {
            JsonNode root = MAPPER.readTree(json);
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<data>");
            appendJsonNodeAsXml(xml, root);
            xml.append("</data>");
            return new Document(xml.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InteractiveCommunicationException(
                "Failed to convert customer data JSON to XML for the OutputService fallback", e);
        }
    }

    private void appendJsonNodeAsXml(StringBuilder xml, JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                appendJsonFieldAsXml(xml, field.getKey(), field.getValue());
            }
        } else if (!node.isNull()) {
            xml.append(xmlEscape(node.asText()));
        }
    }

    private void appendJsonFieldAsXml(StringBuilder xml, String tag, JsonNode value) {
        if (value.isArray()) {
            for (JsonNode item : value) {
                xml.append('<').append(tag).append('>');
                appendJsonNodeAsXml(xml, item);
                xml.append("</").append(tag).append('>');
            }
        } else {
            xml.append('<').append(tag).append('>');
            appendJsonNodeAsXml(xml, value);
            xml.append("</").append(tag).append('>');
        }
    }

    private String xmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private byte[] fetchCustomerData(String customerId) throws InteractiveCommunicationException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(customerDataEndpoint + "?customerId=" + customerId))
            .header("Accept", "application/json");
        if (customerDataUsername != null && !customerDataUsername.isEmpty()) {
            String credentials = customerDataUsername + ":" + customerDataPassword;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            requestBuilder.header("Authorization", "Basic " + encoded);
        }
        HttpRequest request = requestBuilder.GET().build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new InteractiveCommunicationException(
                    "Failed to fetch customer data - endpoint returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            LOG.error("Failed to fetch customer data for customerId: {}", customerId, e);
            throw new InteractiveCommunicationException("Failed to fetch customer data from " + customerDataEndpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InteractiveCommunicationException("Interrupted while fetching customer data", e);
        }
    }
}
