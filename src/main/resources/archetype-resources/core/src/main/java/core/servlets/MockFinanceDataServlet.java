package ${package}.core.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component(service = { Servlet.class })
@SlingServletPaths("/bin/bmad/mock-finance-data")
@ServiceDescription("BMAD Mock Finance Data Servlet")
public class MockFinanceDataServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected void doGet(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        LocalDate today = LocalDate.now();

        ObjectNode customer = MAPPER.createObjectNode();
        customer.put("id", "CUST-10293");
        customer.put("name", "John Doe");
        customer.put("status", "Premier");

        ArrayNode employment = MAPPER.createArrayNode();
        employment.add(job("Tech Corp", "Lead Dev", "5"));
        employment.add(job("Startup Inc", "Senior Dev", "2"));
        employment.add(job("Classic Solutions", "Junior Dev", "3"));

        ArrayNode transactions = MAPPER.createArrayNode();
        transactions.add(txn(today.minusDays(10).format(DATE_FMT), "Amazon Purchase", "-120.50"));
        transactions.add(txn(today.minusDays(9).format(DATE_FMT), "Salary Deposit", "5500.00"));
        transactions.add(txn(today.minusDays(6).format(DATE_FMT), "Utility Bill", "-210.00"));
        transactions.add(txn(today.minusDays(3).format(DATE_FMT), "Coffee Shop", "-15.75"));

        ObjectNode root = MAPPER.createObjectNode();
        root.set("customer", customer);
        root.set("employmentHistory", employment);
        root.set("transactions", transactions);

        resp.getWriter().write(MAPPER.writeValueAsString(root));
    }

    private ObjectNode job(String company, String role, String years) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("company", company);
        n.put("role", role);
        n.put("years", years);
        return n;
    }

    private ObjectNode txn(String date, String description, String amount) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("date", date);
        n.put("description", description);
        n.put("amount", amount);
        return n;
    }
}
