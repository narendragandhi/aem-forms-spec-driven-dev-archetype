package ${package}.core.servlets;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class SimpleServletTest {

    private final AemContext context = new AemContext();
    private SimpleServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new SimpleServlet();
        context.create().resource("/content/test/resource", "jcr:title", "Test Resource");
    }

    @Test
    void testDoGet() throws ServletException, IOException {
        context.currentResource("/content/test/resource");
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        String output = response.getOutputAsString();
        assertNotNull(output);
        assertTrue(output.contains("Title"));
    }

    @Test
    void testDoGetWithResourceTitle() throws ServletException, IOException {
        context.create().resource("/content/test/titled-resource", "jcr:title", "My Custom Title");
        context.currentResource("/content/test/titled-resource");

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        String output = response.getOutputAsString();
        assertTrue(output.contains("My Custom Title"));
    }

    @Test
    void testResponseContentType() throws ServletException, IOException {
        context.currentResource("/content/test/resource");
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals("text/plain", response.getContentType());
    }
}
