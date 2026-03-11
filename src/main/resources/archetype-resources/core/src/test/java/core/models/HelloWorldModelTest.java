package ${package}.core.models;

import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class HelloWorldModelTest {

    private final AemContext context = new AemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(HelloWorldModel.class);
        Page page = context.create().page("/content/test/page", "test-template");
        context.create().resource("/content/test/page/jcr:content/component",
            "sling:resourceType", "${appName}/components/helloworld"
        );
    }

    @Test
    void testGetMessage() {
        context.currentResource("/content/test/page/jcr:content/component");
        HelloWorldModel model = context.currentResource().adaptTo(HelloWorldModel.class);
        assertNotNull(model);
        String message = model.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Hello World!"));
    }

    @Test
    void testMessageContainsResourceType() {
        context.currentResource("/content/test/page/jcr:content/component");
        HelloWorldModel model = context.currentResource().adaptTo(HelloWorldModel.class);
        assertNotNull(model);
        assertTrue(model.getMessage().contains("${appName}/components/helloworld"));
    }

    @Test
    void testMessageContainsCurrentPage() {
        context.currentResource("/content/test/page/jcr:content/component");
        HelloWorldModel model = context.currentResource().adaptTo(HelloWorldModel.class);
        assertNotNull(model);
        assertTrue(model.getMessage().contains("/content/test/page"));
    }

    @Test
    void testGetUppercaseMessage() {
        context.currentResource("/content/test/page/jcr:content/component");
        HelloWorldModel model = context.currentResource().adaptTo(HelloWorldModel.class);
        assertNotNull(model);
        String message = model.getMessage();
        String upperMessage = model.getUppercaseMessage();
        assertNotNull(upperMessage);
        assertEquals(message.toUpperCase(), upperMessage);
    }
}
