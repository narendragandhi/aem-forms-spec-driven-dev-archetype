package ${package}.core.models;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.List;

@Model(
    adaptables = Resource.class,
    adapters = {FinancialApplicationModel.class, ComponentExporter.class},
    resourceType = "${appName}/components/adaptiveForm/financial-application",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class FinancialApplicationModel implements ComponentExporter {

    @ValueMapValue
    private String customerId;

    @ValueMapValue
    private String customerName;

    @ValueMapValue
    private String customerStatus;

    @ChildResource(name = "employment")
    private List<EmploymentRecord> employmentHistory;

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerStatus() {
        return customerStatus;
    }

    public List<EmploymentRecord> getEmploymentHistory() {
        return employmentHistory;
    }

    @Override
    public String getExportedType() {
        return "${appName}/components/adaptiveForm/financial-application";
    }

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class EmploymentRecord {
        @ValueMapValue
        private String company;
        @ValueMapValue
        private String role;
        @ValueMapValue
        private String years;

        public String getCompany() {
            return company;
        }

        public String getRole() {
            return role;
        }

        public String getYears() {
            return years;
        }
    }
}
