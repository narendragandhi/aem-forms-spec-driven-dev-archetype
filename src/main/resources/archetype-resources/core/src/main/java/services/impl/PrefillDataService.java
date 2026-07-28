package ${package}.services.impl;

import com.adobe.forms.common.service.ContentType;
import com.adobe.forms.common.service.DataOptions;
import com.adobe.forms.common.service.DataProvider;
import com.adobe.forms.common.service.FormsException;
import com.adobe.forms.common.service.PrefillData;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Real AEM Forms prefill integration: implements the actual
 * com.adobe.forms.common.service.DataProvider interface (verified via
 * javap against the pinned aem-forms-sdk-api jar - not assumed from a
 * class name), the documented extension point for prefilling Core
 * Components Adaptive Forms.
 *
 * The real, live-confirmed data shape for a Core Components form is a
 * flat {@code {"data": {fieldName: value, ...}}} object - the same shape
 * already proven for the framework's own submit action this session
 * (POSTing to /adobe/forms/af/submit/&lt;id&gt; needs the same wrapper).
 * Confirmed by inspecting a real, unauthenticated GET to
 * /adobe/forms/af/data/&lt;id&gt; on a live instance, which returns
 * {@code {"data":{},"metadata":{...}}} by default.
 *
 * <p><b>The form-to-service selection mechanism, live-confirmed</b> (not
 * documented in Adobe's own tutorial for this feature, which shows the
 * Java interface but not this wiring): decompiling the real
 * {@code AdaptiveFormDataServlet}/{@code FormDataProviderRegistryImpl}
 * classes from the running instance's own
 * {@code com.adobe.aem.forms.af.rest}/{@code aemds-guide-core-impl}
 * bundles (not guessed) showed the servlet reads a plain
 * {@code prefillService} content property off the {@code guideContainer}
 * (via {@code FormContainer#getPrefillService()}) and, when present (and
 * no {@code dataRef} request parameter is passed - the two are mutually
 * exclusive branches), sets it as {@link DataOptions#setServiceName}, which
 * the registry looks up against each registered provider's
 * {@link #getServiceName()} in a real name-keyed map. Every request query
 * parameter (e.g. {@code ?customerId=...}) is separately copied into
 * {@link DataOptions#getExtras()} regardless of that branch, which is what
 * this class's {@code customerId} fallback below actually receives. Live
 * end-to-end confirmed: authoring {@code prefillService=bmadPrefillDataService}
 * on a real guideContainer and GETting
 * {@code /adobe/forms/af/data/<id>?customerId=CUST-10293} produced a real
 * log line from {@code FormDataProviderRegistryImpl} naming this service
 * and invoking {@link #getPrefillData}. (An earlier attempt this session
 * had instead authored a JCR property literally named {@code dataRef} on
 * the guideContainer, which this servlet never reads - only a same-named
 * *request query parameter* is checked, a different mechanism entirely -
 * which is why that earlier attempt never fired.)
 *
 * <p>{@code DataProvider} actually extends {@code DataProviderBase}
 * (verified via javap - not visible from {@code DataProvider}'s own
 * method alone), which requires {@link #getServiceName()} and
 * {@link #getServiceDescription()} - both confirmed load-bearing above,
 * not just required-to-compile.
 */
@Component(service = { DataProvider.class, PrefillDataService.class }, immediate = true)
@Designate(ocd = PrefillDataService.Config.class)
public class PrefillDataService implements DataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PrefillDataService.class);
    private static final String SERVICE_NAME = "bmadPrefillDataService";
    private static final String EMPTY_DATA = "{\"data\":{}}";

    @ObjectClassDefinition(
        name = "AEM Forms Prefill Data Service Configuration",
        description = "Configuration for prefilling Adaptive Forms from an external data source."
    )
    public @interface Config {
        @AttributeDefinition(
            name = "Prefill Data Endpoint",
            description = "REST endpoint returning prefill data as flat JSON (field name -> value), given a ?customerId= query param.",
            type = AttributeType.STRING
        )
        String prefill_data_endpoint() default "http://localhost:4502/bin/bmad/mock-finance-data";

        @AttributeDefinition(
            name = "Prefill Data Endpoint Username (Optional)",
            description = "HTTP Basic Auth username for the prefill data endpoint. The default endpoint "
                + "(MockFinanceDataServlet, a real Sling servlet on this same instance) requires AEM auth like "
                + "any other Sling resource - live-confirmed this returns 401 with no credentials configured.",
            type = AttributeType.STRING
        )
        String prefill_data_endpoint_username() default "";

        @AttributeDefinition(
            name = "Prefill Data Endpoint Password (Optional)",
            description = "HTTP Basic Auth password for the prefill data endpoint.",
            type = AttributeType.PASSWORD
        )
        String prefill_data_endpoint_password() default "";
    }

    private String prefillDataEndpoint;
    private String prefillDataEndpointUsername;
    private String prefillDataEndpointPassword;

    HttpClient httpClient = HttpClient.newHttpClient(); // package-private so unit tests can substitute a mock

    @Activate
    protected void activate(final Config config) {
        this.prefillDataEndpoint = config.prefill_data_endpoint();
        this.prefillDataEndpointUsername = config.prefill_data_endpoint_username();
        this.prefillDataEndpointPassword = config.prefill_data_endpoint_password();
    }

    @Override
    public PrefillData getPrefillData(DataOptions dataOptions) throws FormsException {
        String identifier = resolveIdentifier(dataOptions);
        if (identifier == null || identifier.isEmpty()) {
            LOG.debug("No prefill identifier present (dataRef/customerId) - returning empty prefill data");
            return emptyPrefillData();
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(prefillDataEndpoint + "?customerId=" + identifier))
            .header("Accept", "application/json")
            .GET();
        if (prefillDataEndpointUsername != null && !prefillDataEndpointUsername.isEmpty()) {
            String credentials = prefillDataEndpointUsername + ":" + prefillDataEndpointPassword;
            requestBuilder.header("Authorization",
                "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn("Prefill data endpoint returned HTTP {} for identifier {} - returning empty prefill data",
                    response.statusCode(), identifier);
                return emptyPrefillData();
            }
            String wrapped = "{\"data\":" + new String(response.body(), StandardCharsets.UTF_8) + "}";
            return new PrefillData(
                new ByteArrayInputStream(wrapped.getBytes(StandardCharsets.UTF_8)), ContentType.JSON);
        } catch (IOException e) {
            throw new FormsException("Failed to fetch prefill data for identifier " + identifier
                + " from " + prefillDataEndpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FormsException("Interrupted while fetching prefill data for identifier " + identifier, e);
        }
    }

    private String resolveIdentifier(DataOptions dataOptions) {
        String dataRef = dataOptions.getDataRef();
        if (dataRef != null && !dataRef.isEmpty()) {
            return dataRef;
        }
        Object customerId = dataOptions.getExtras() != null ? dataOptions.getExtras().get("customerId") : null;
        return customerId != null ? customerId.toString() : null;
    }

    private PrefillData emptyPrefillData() {
        return new PrefillData(
            new ByteArrayInputStream(EMPTY_DATA.getBytes(StandardCharsets.UTF_8)), ContentType.JSON);
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public String getServiceDescription() {
        return "Fetches Adaptive Form prefill data from a configurable REST endpoint, keyed by dataRef/customerId.";
    }
}
