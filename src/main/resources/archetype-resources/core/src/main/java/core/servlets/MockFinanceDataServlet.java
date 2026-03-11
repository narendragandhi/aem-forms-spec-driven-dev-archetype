package ${package}.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Mock Servlet to provide data for Form Data Model (FDM) and Dynamic Tables.
 * This simulates an external banking or employment API.
 */
@Component(service = { Servlet.class })
@SlingServletPaths("/bin/bmad/mock-finance-data")
@ServiceDescription("BMAD Mock Finance Data Servlet")
public class MockFinanceDataServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Dynamic Mock Data for AF and IC Tables
        String mockJson = "{"
            + "\"customer\": {"
            + "  \"id\": \"CUST-10293\","
            + "  \"name\": \"John Doe\","
            + "  \"status\": \"Premier\""
            + "},"
            + "\"employmentHistory\": ["
            + "  {\"company\": \"Tech Corp\", \"role\": \"Lead Dev\", \"years\": \"5\"},"
            + "  {\"company\": \"Startup Inc\", \"role\": \"Senior Dev\", \"years\": \"2\"},"
            + "  {\"company\": \"Classic Solutions\", \"role\": \"Junior Dev\", \"years\": \"3\"}"
            + "],"
            + "\"transactions\": ["
            + "  {\"date\": \"2026-03-01\", \"description\": \"Amazon Purchase\", \"amount\": \"-120.50\"},"
            + "  {\"date\": \"2026-03-02\", \"description\": \"Salary Deposit\", \"amount\": \"5500.00\"},"
            + "  {\"date\": \"2026-03-05\", \"description\": \"Utility Bill\", \"amount\": \"-210.00\"},"
            + "  {\"date\": \"2026-03-08\", \"description\": \"Coffee Shop\", \"amount\": \"-15.75\"}"
            + "]"
            + "}";

        resp.getWriter().write(mockJson);
    }
}
