package ${package}.services.impl;

import ${package}.services.InteractiveCommunicationException;
import ${package}.services.InteractiveCommunicationService;
import com.adobe.aem.forms.ic.exception.ICException;
import com.adobe.aem.forms.ic.print.api.PrintChannelRenderService;
import com.adobe.aem.forms.ic.print.model.IcPdfRenderOptions;
import com.adobe.aemfd.docmanager.Document;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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

/**
 * Real AEM Forms Interactive Communications integration. Calls the actual
 * PrintChannelRenderService to render an IC's Print Channel as a PDF,
 * merged with customer data fetched from a configured REST endpoint
 * (defaults to this archetype's own MockFinanceDataServlet).
 *
 * API verified via javap against the pinned aem-forms-sdk-api jar, not
 * assumed. Not live-tested: on the instance this was built against,
 * PrintChannelRenderServiceImpl itself is OSGi-unsatisfied (gated behind a
 * feature toggle not registered on that instance) - see README. If that's
 * also true on your instance, this component won't activate either, since
 * printChannelRenderService is a mandatory reference - a loud failure, not
 * a silent no-op.
 */
@Component(service = InteractiveCommunicationService.class)
@Designate(ocd = InteractiveCommunicationServiceImpl.Config.class)
public class InteractiveCommunicationServiceImpl implements InteractiveCommunicationService {

    private static final Logger LOG = LoggerFactory.getLogger(InteractiveCommunicationServiceImpl.class);

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
    }

    @Reference
    private PrintChannelRenderService printChannelRenderService;

    private String customerDataEndpoint;
    private String locale;

    HttpClient httpClient = HttpClient.newHttpClient(); // package-private so unit tests can substitute a mock

    @Activate
    protected void activate(final Config config) {
        this.customerDataEndpoint = config.customer_data_endpoint();
        this.locale = config.locale();
    }

    @Override
    public byte[] generatePrintPdf(String icContentPath, String customerId) throws InteractiveCommunicationException {
        byte[] customerData = fetchCustomerData(customerId);

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

    private byte[] fetchCustomerData(String customerId) throws InteractiveCommunicationException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(customerDataEndpoint + "?customerId=" + customerId))
            .header("Accept", "application/json")
            .GET()
            .build();

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
