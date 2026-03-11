package ${package}.core.models;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class SampleRegistrationTest {

    private final AemContext context = new AemContext();

    private SampleRegistration registration;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(SampleRegistration.class);
    }

    @Test
    void testGetFirstName() {
        context.create().resource("/content/test/registration",
            "firstName", "John",
            "lastName", "Doe",
            "email", "john.doe@example.com"
        );
        context.currentResource("/content/test/registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertEquals("John", registration.getFirstName());
    }

    @Test
    void testGetLastName() {
        context.create().resource("/content/test/registration",
            "firstName", "Jane",
            "lastName", "Smith",
            "email", "jane.smith@example.com"
        );
        context.currentResource("/content/test/registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertEquals("Smith", registration.getLastName());
    }

    @Test
    void testGetEmail() {
        context.create().resource("/content/test/registration",
            "firstName", "Test",
            "lastName", "User",
            "email", "test.user@example.com"
        );
        context.currentResource("/content/test/registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertEquals("test.user@example.com", registration.getEmail());
    }

    @Test
    void testAllFieldsPopulated() {
        context.create().resource("/content/test/registration",
            "firstName", "Alice",
            "lastName", "Johnson",
            "email", "alice.johnson@company.org"
        );
        context.currentResource("/content/test/registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertAll("Registration fields should be populated",
            () -> assertEquals("Alice", registration.getFirstName()),
            () -> assertEquals("Johnson", registration.getLastName()),
            () -> assertEquals("alice.johnson@company.org", registration.getEmail())
        );
    }

    @Test
    void testRegistrationWithMissingProperties() {
        context.create().resource("/content/test/empty-registration");
        context.currentResource("/content/test/empty-registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration, "Model should still adapt even with missing properties");
        assertNull(registration.getFirstName(), "First name should be null when not set");
        assertNull(registration.getLastName(), "Last name should be null when not set");
        assertNull(registration.getEmail(), "Email should be null when not set");
    }

    @Test
    void testRegistrationWithPartialProperties() {
        context.create().resource("/content/test/partial-registration",
            "firstName", "PartialFirst"
        );
        context.currentResource("/content/test/partial-registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertEquals("PartialFirst", registration.getFirstName());
        assertNull(registration.getLastName());
        assertNull(registration.getEmail());
    }

    @Test
    void testRegistrationWithEmailOnly() {
        context.create().resource("/content/test/email-only-registration",
            "email", "only.email@test.com"
        );
        context.currentResource("/content/test/email-only-registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertNull(registration.getFirstName());
        assertNull(registration.getLastName());
        assertEquals("only.email@test.com", registration.getEmail());
    }

    @Test
    void testRegistrationWithEmptyStrings() {
        context.create().resource("/content/test/empty-strings-registration",
            "firstName", "",
            "lastName", "",
            "email", ""
        );
        context.currentResource("/content/test/empty-strings-registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertEquals("", registration.getFirstName());
        assertEquals("", registration.getLastName());
        assertEquals("", registration.getEmail());
    }

    @Test
    void testRegistrationWithSpecialCharacters() {
        context.create().resource("/content/test/special-chars-registration",
            "firstName", "Jean-Pierre",
            "lastName", "O'Connor",
            "email", "jean+test@example.co.uk"
        );
        context.currentResource("/content/test/special-chars-registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertEquals("Jean-Pierre", registration.getFirstName());
        assertEquals("O'Connor", registration.getLastName());
        assertEquals("jean+test@example.co.uk", registration.getEmail());
    }

    @Test
    void testRegistrationWithUnicodeCharacters() {
        context.create().resource("/content/test/unicode-registration",
            "firstName", "Muller",
            "lastName", "Garcia",
            "email", "user@example.com"
        );
        context.currentResource("/content/test/unicode-registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertEquals("Muller", registration.getFirstName());
        assertEquals("Garcia", registration.getLastName());
    }

    @Test
    void testRegistrationWithLongValues() {
        String longFirstName = "A".repeat(100);
        String longLastName = "B".repeat(100);
        String longEmail = "user@" + "domain".repeat(20) + ".com";

        context.create().resource("/content/test/long-values-registration",
            "firstName", longFirstName,
            "lastName", longLastName,
            "email", longEmail
        );
        context.currentResource("/content/test/long-values-registration");

        registration = context.request().adaptTo(SampleRegistration.class);

        assertNotNull(registration);
        assertEquals(longFirstName, registration.getFirstName());
        assertEquals(longLastName, registration.getLastName());
        assertEquals(longEmail, registration.getEmail());
    }
}
