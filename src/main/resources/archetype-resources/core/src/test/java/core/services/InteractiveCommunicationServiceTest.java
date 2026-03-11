package ${package}.core.services;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Interactive Communications service functionality.
 * Tests document fragment resolution and IC template validation.
 */
@ExtendWith(AemContextExtension.class)
class InteractiveCommunicationServiceTest {

    private final AemContext context = new AemContext();

    @BeforeEach
    void setUp() {
        // Create document fragment structure
        context.create().resource("/content/dam/formsanddocuments/fragments/${appName}/header",
            "jcr:title", "Document Header",
            "fragmentType", "documentFragment"
        );

        context.create().resource("/content/dam/formsanddocuments/fragments/${appName}/footer",
            "jcr:title", "Document Footer",
            "fragmentType", "documentFragment"
        );

        // Create IC template
        context.create().resource("/content/dam/formsanddocuments/ic/${appName}/account-statement",
            "jcr:title", "Account Statement",
            "icType", "interactiveCommunication",
            "printChannelEnabled", true,
            "webChannelEnabled", true
        );
    }

    @Test
    void testDocumentFragmentExists() {
        Resource header = context.resourceResolver()
            .getResource("/content/dam/formsanddocuments/fragments/${appName}/header");

        assertNotNull(header);
        assertEquals("Document Header", header.getValueMap().get("jcr:title", String.class));
    }

    @Test
    void testDocumentFragmentType() {
        Resource footer = context.resourceResolver()
            .getResource("/content/dam/formsanddocuments/fragments/${appName}/footer");

        assertNotNull(footer);
        assertEquals("documentFragment", footer.getValueMap().get("fragmentType", String.class));
    }

    @Test
    void testICTemplateExists() {
        Resource ic = context.resourceResolver()
            .getResource("/content/dam/formsanddocuments/ic/${appName}/account-statement");

        assertNotNull(ic);
        assertEquals("Account Statement", ic.getValueMap().get("jcr:title", String.class));
    }

    @Test
    void testICPrintChannelEnabled() {
        Resource ic = context.resourceResolver()
            .getResource("/content/dam/formsanddocuments/ic/${appName}/account-statement");

        assertNotNull(ic);
        assertTrue(ic.getValueMap().get("printChannelEnabled", false));
    }

    @Test
    void testICWebChannelEnabled() {
        Resource ic = context.resourceResolver()
            .getResource("/content/dam/formsanddocuments/ic/${appName}/account-statement");

        assertNotNull(ic);
        assertTrue(ic.getValueMap().get("webChannelEnabled", false));
    }

    @Test
    void testFormDataModelResolution() {
        context.create().resource("/content/dam/formsanddocuments-fdm/${appName}/rest-customer-api",
            "jcr:title", "REST Customer API",
            "fdmType", "rest"
        );

        Resource fdm = context.resourceResolver()
            .getResource("/content/dam/formsanddocuments-fdm/${appName}/rest-customer-api");

        assertNotNull(fdm);
        assertEquals("rest", fdm.getValueMap().get("fdmType", String.class));
    }
}
