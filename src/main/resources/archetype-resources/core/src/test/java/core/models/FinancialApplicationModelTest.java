package ${package}.core.models;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class FinancialApplicationModelTest {

    private final AemContext context = new AemContext();

    @BeforeEach
    void setUp() {
        context.create().resource("/content/finance",
            "customerId", "CUST-123",
            "customerName", "Jane Doe",
            "customerStatus", "Gold"
        );
        // Add employment history sub-resources
        context.create().resource("/content/finance/employment/job1", "company", "Tech", "role", "Dev", "years", "3");
        context.create().resource("/content/finance/employment/job2", "company", "Finance", "role", "Analyst", "years", "2");
    }

    @Test
    void testCustomerDataMapping() {
        Resource resource = context.resourceResolver().getResource("/content/finance");
        FinancialApplicationModel model = resource.adaptTo(FinancialApplicationModel.class);

        assertNotNull(model);
        assertEquals("CUST-123", model.getCustomerId());
        assertEquals("Jane Doe", model.getCustomerName());
        assertEquals("Gold", model.getCustomerStatus());
    }

    @Test
    void testEmploymentHistoryCollection() {
        Resource resource = context.resourceResolver().getResource("/content/finance");
        FinancialApplicationModel model = resource.adaptTo(FinancialApplicationModel.class);

        assertNotNull(model);
        assertNotNull(model.getEmploymentHistory());
        assertEquals(2, model.getEmploymentHistory().size());
        assertEquals("Tech", model.getEmploymentHistory().get(0).getCompany());
    }
}
