package ${package}.core.models;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class CardTest {

    private final AemContext context = new AemContext();

    private Card card;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(Card.class);

        context.create().resource("/content/test/card",
            "cardTitle", "Test Card Title",
            "cardText", "This is test card text content",
            "imagePath", "/content/dam/images/test.jpg",
            "buttonText", "Learn More",
            "buttonLink", "/content/test/page"
        );

        context.currentResource("/content/test/card");
    }

    @Test
    void testGetCardTitle() {
        card = context.request().adaptTo(Card.class);
        assertNotNull(card);
        assertEquals("Test Card Title", card.getCardTitle());
    }

    @Test
    void testGetCardText() {
        card = context.request().adaptTo(Card.class);
        assertNotNull(card);
        assertEquals("This is test card text content", card.getCardText());
    }

    @Test
    void testGetImagePath() {
        card = context.request().adaptTo(Card.class);
        assertNotNull(card);
        assertEquals("/content/dam/images/test.jpg", card.getImagePath());
    }

    @Test
    void testGetButtonText() {
        card = context.request().adaptTo(Card.class);
        assertNotNull(card);
        assertEquals("Learn More", card.getButtonText());
    }

    @Test
    void testGetButtonLink() {
        card = context.request().adaptTo(Card.class);
        assertNotNull(card);
        assertEquals("/content/test/page", card.getButtonLink());
    }

    @Test
    void testCardWithMissingProperties() {
        context.create().resource("/content/test/empty-card");
        context.currentResource("/content/test/empty-card");

        card = context.request().adaptTo(Card.class);
        assertNotNull(card);
        assertNull(card.getCardTitle());
        assertNull(card.getCardText());
    }
}
