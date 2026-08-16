package ${package}.observability;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.EngineConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** Propagates a safe correlation id through request logs and browser responses. */
@Component(service = Filter.class, property = EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST)
public class CorrelationIdFilter implements Filter {
    public static final String ATTRIBUTE = "bmad.correlationId";

    @Reference
    private ObservabilityService observability;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String id = observability.beginRequest(((SlingHttpServletRequest) request).getHeader("X-Correlation-ID"));
        request.setAttribute(ATTRIBUTE, id);
        ((SlingHttpServletResponse) response).setHeader("X-Correlation-ID", id);
        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig filterConfig) { }
    @Override public void destroy() { }
}
