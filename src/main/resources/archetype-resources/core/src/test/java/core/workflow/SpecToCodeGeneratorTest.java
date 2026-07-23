package ${package}.core.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpecToCodeGeneratorTest {

    private static final String SAMPLE_FORM_SPEC = "{\n" +
            "  \"title\": \"Sample Registration Form\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"firstName\": { \"type\": \"string\", \"title\": \"First Name\", \"description\": \"Enter your first name.\" },\n" +
            "    \"lastName\": { \"type\": \"string\", \"title\": \"Last Name\", \"description\": \"Enter your last name.\" },\n" +
            "    \"email\": { \"type\": \"string\", \"format\": \"email\", \"title\": \"Email Address\", \"description\": \"Enter a valid email address.\" }\n" +
            "  },\n" +
            "  \"required\": [\"firstName\", \"lastName\", \"email\"]\n" +
            "}\n";

    // The actual specs/card-component.json shipped by this archetype — this is
    // the spec that the hand-written Card.java/card.html were built from, so it
    // doubles as a golden reference: generated output should line up with them.
    private static final String CARD_COMPONENT_SPEC = "{\n" +
            "  \"title\": \"Card Component\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"cardTitle\": { \"type\": \"string\", \"title\": \"Card Title\", \"description\": \"The main heading for the card.\" },\n" +
            "    \"cardText\": { \"type\": \"string\", \"title\": \"Card Text\", \"description\": \"The descriptive body text for the card.\" },\n" +
            "    \"imagePath\": { \"type\": \"string\", \"title\": \"Image Path\", \"description\": \"The path to the card's image in the DAM.\" },\n" +
            "    \"buttonText\": { \"type\": \"string\", \"title\": \"Button Text\", \"description\": \"The text for the card's call-to-action button.\" },\n" +
            "    \"buttonLink\": { \"type\": \"string\", \"title\": \"Button Link\", \"description\": \"The destination URL for the call-to-action button.\" }\n" +
            "  },\n" +
            "  \"required\": [\"cardTitle\", \"cardText\"]\n" +
            "}\n";

    private SpecToCodeGenerator specToCodeGenerator;
    private final TestLogger logger = TestLoggerFactory.getTestLogger(SpecToCodeGenerator.class);

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TestLoggerFactory.clear();
        specToCodeGenerator = new SpecToCodeGenerator();
    }

    private Path writeSpec(String name, String content) throws IOException {
        Path spec = tempDir.resolve(name);
        Files.write(spec, content.getBytes(StandardCharsets.UTF_8));
        return spec;
    }

    @Test
    void testGenerateCreatesSlingModelWithFieldsAndGetters() throws IOException {
        Path spec = writeSpec("sample-form.json", SAMPLE_FORM_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        Path modelFile = tempDir.resolve("core/src/main/java/com/acme/core/models/SampleRegistrationForm.java");
        assertTrue(Files.exists(modelFile), "Sling Model should be generated");
        String src = Files.readString(modelFile);
        assertTrue(src.contains("package com.acme.core.models;"));
        assertTrue(src.contains("public class SampleRegistrationForm"));
        assertTrue(src.contains("@Model(adaptables = SlingHttpServletRequest.class,"));
        assertTrue(src.contains("private String firstName;"));
        assertTrue(src.contains("private String lastName;"));
        assertTrue(src.contains("private String email;"));
        assertTrue(src.contains("public String getFirstName() {"));
        assertTrue(src.contains("return firstName;"));
    }

    @Test
    void testGenerateMapsJsonSchemaTypesToJavaTypes() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Mixed Types\",\n" +
                "  \"properties\": {\n" +
                "    \"label\": { \"type\": \"string\", \"title\": \"Label\" },\n" +
                "    \"isActive\": { \"type\": \"boolean\", \"title\": \"Active\" },\n" +
                "    \"count\": { \"type\": \"integer\", \"title\": \"Count\" },\n" +
                "    \"price\": { \"type\": \"number\", \"title\": \"Price\" }\n" +
                "  }\n" +
                "}\n";
        Path specFile = writeSpec("mixed-types.json", spec);

        specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        String src = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/MixedTypes.java"));
        assertTrue(src.contains("private String label;"));
        assertTrue(src.contains("private Boolean isActive;"));
        assertTrue(src.contains("private Long count;"));
        assertTrue(src.contains("private Double price;"));
        assertTrue(src.contains("public Boolean getIsActive() {"));
    }

    @Test
    void testGenerateCreatesAemComponentContentXmlAndHtl() throws IOException {
        Path spec = writeSpec("sample-form.json", SAMPLE_FORM_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        Path componentDir = tempDir.resolve(
                "ui.apps/src/main/content/jcr_root/apps/AcmeApp/components/generated/sample-registration-form");
        Path contentXml = componentDir.resolve(".content.xml");
        Path htl = componentDir.resolve("sample-registration-form.html");
        assertTrue(Files.exists(contentXml), "content.xml should be generated");
        assertTrue(Files.exists(htl), "HTL script should be generated");

        String xml = Files.readString(contentXml);
        assertTrue(xml.contains("jcr:primaryType=\"cq:Component\""));
        assertTrue(xml.contains("jcr:title=\"Sample Registration Form\""));
        assertTrue(xml.contains("componentGroup=\"AEM Forms BMAD - AcmeApp - Generated\""));

        String html = Files.readString(htl);
        assertTrue(html.contains("data-sly-use.model=\"com.acme.core.models.SampleRegistrationForm\""));
        assertTrue(html.contains("${model.firstName}"));
        assertTrue(html.contains("${model.lastName}"));
        assertTrue(html.contains("${model.email}"));
    }

    @Test
    void testGenerateCreatesReactComponent() throws IOException {
        Path spec = writeSpec("sample-form.json", SAMPLE_FORM_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        Path reactFile = tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/SampleRegistrationForm.jsx");
        assertTrue(Files.exists(reactFile), "React component should be generated");
        String jsx = Files.readString(reactFile);
        assertTrue(jsx.contains("import { Field } from '@aemforms/af-react-components';"));
        assertTrue(jsx.contains("const SampleRegistrationForm ="));
        assertTrue(jsx.contains("name={`${name}.firstName`}"));
        assertTrue(jsx.contains("name={`${name}.email`}"));
        assertTrue(jsx.contains("export default function (props)"));
    }

    @Test
    void testGenerateWithCardComponentSpecMatchesHandWrittenReference() throws IOException {
        // specs/card-component.json is the real spec this archetype ships, and
        // Card.java/card.html were hand-written to match it. A generator that
        // actually implements the spec should reproduce the same field set.
        Path spec = writeSpec("card-component.json", CARD_COMPONENT_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        // "Card Component" -> "Card" (trailing "Component" is stripped, matching
        // the hand-written Card.java's class name)
        Path modelFile = tempDir.resolve("core/src/main/java/com/acme/core/models/Card.java");
        assertTrue(Files.exists(modelFile), "Should generate Card.java, matching the hand-written model's name");
        String src = Files.readString(modelFile);
        for (String field : new String[]{"cardTitle", "cardText", "imagePath", "buttonText", "buttonLink"}) {
            assertTrue(src.contains("private String " + field + ";"), "Missing field: " + field);
            assertTrue(src.contains("public String get" + Character.toUpperCase(field.charAt(0)) + field.substring(1) + "() {"),
                    "Missing getter for: " + field);
        }

        Path componentDir = tempDir.resolve(
                "ui.apps/src/main/content/jcr_root/apps/AcmeApp/components/generated/card");
        assertTrue(Files.exists(componentDir.resolve(".content.xml")));
        assertTrue(Files.exists(componentDir.resolve("card.html")));
    }

    @Test
    void testGenerateThrowsForMissingSpecFile() {
        Path missing = tempDir.resolve("does-not-exist.json");
        assertThrows(IOException.class,
                () -> specToCodeGenerator.generate(missing.toString(), tempDir.toString(), "com.acme", "AcmeApp"));
    }

    @Test
    void testGenerateThrowsForSpecWithNoProperties() throws IOException {
        Path spec = writeSpec("empty.json", "{ \"title\": \"Empty\" }");
        IOException ex = assertThrows(IOException.class,
                () -> specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp"));
        assertTrue(ex.getMessage().contains("no 'properties'"));
    }

    @Test
    void testGenerateLogsCompletionMessage() throws IOException {
        Path spec = writeSpec("sample-form.json", SAMPLE_FORM_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        boolean hasCompletionLog = logger.getLoggingEvents().stream()
                .anyMatch(e -> e.getMessage().contains("Spec-to-Code Generation completed for:"));
        assertTrue(hasCompletionLog, "Should log completion message");
    }

    // --- richer JSON Schema keywords: required, format, constraints, enum ---

    // The actual specs/job-application.json shipped by this archetype —
    // exercises required, format (email/tel), pattern, minimum/maximum, and
    // enum together, so it doubles as an integration-level golden reference.
    private static final String JOB_APPLICATION_SPEC = "{\n" +
            "  \"title\": \"Job Application\",\n" +
            "  \"properties\": {\n" +
            "    \"fullName\": { \"type\": \"string\", \"title\": \"Full Name\", \"minLength\": 2, \"maxLength\": 100 },\n" +
            "    \"email\": { \"type\": \"string\", \"format\": \"email\", \"title\": \"Email Address\" },\n" +
            "    \"phone\": { \"type\": \"string\", \"format\": \"tel\", \"title\": \"Phone Number\", \"pattern\": \"^\\\\d{3}-\\\\d{3}-\\\\d{4}$\" },\n" +
            "    \"yearsOfExperience\": { \"type\": \"integer\", \"title\": \"Years of Experience\", \"minimum\": 0, \"maximum\": 50 },\n" +
            "    \"department\": { \"type\": \"string\", \"title\": \"Department\", \"enum\": [\"Engineering\", \"Sales\", \"Marketing\", \"Support\"] },\n" +
            "    \"coverLetter\": { \"type\": \"string\", \"title\": \"Cover Letter\", \"maxLength\": 2000 }\n" +
            "  },\n" +
            "  \"required\": [\"fullName\", \"email\", \"phone\", \"department\"]\n" +
            "}\n";

    @Test
    void testGenerateAppliesRequiredToReactAndValidate() throws IOException {
        Path spec = writeSpec("job-application.json", JOB_APPLICATION_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        String jsx = Files.readString(tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/JobApplication.jsx"));
        assertTrue(jsx.contains("Full Name *"), "Required fields get a visible marker");
        assertTrue(jsx.contains("Cover Letter"), "Optional field's label has no marker");
        assertFalse(jsx.contains("Cover Letter *"), "Cover letter is not required per the spec");

        String model = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/JobApplication.java"));
        assertTrue(model.contains("public List<String> validate() {"));
        assertTrue(model.contains("if (getFullName() == null || getFullName().isEmpty()) {"));
        assertTrue(model.contains("errors.add(\"Full Name is required\");"));
        // coverLetter is optional, so validate() must not require it
        assertFalse(model.contains("Cover Letter is required"));
    }

    @Test
    void testGenerateMapsFormatToHtmlInputType() throws IOException {
        Path spec = writeSpec("job-application.json", JOB_APPLICATION_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        String jsx = Files.readString(tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/JobApplication.jsx"));
        assertTrue(jsx.contains("type=\"email\""), "email format should render an email input");
        assertTrue(jsx.contains("type=\"tel\""), "tel format should render a tel input");
        assertTrue(jsx.contains("type=\"number\""), "integer type should render a number input regardless of format");
    }

    @Test
    void testGenerateAppliesStringConstraints() throws IOException {
        Path spec = writeSpec("job-application.json", JOB_APPLICATION_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        String jsx = Files.readString(tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/JobApplication.jsx"));
        assertTrue(jsx.contains("minLength={2}"));
        assertTrue(jsx.contains("maxLength={100}"));
        assertTrue(jsx.contains("pattern=\"^\\\\d{3}-\\\\d{3}-\\\\d{4}$\""));

        String model = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/JobApplication.java"));
        assertTrue(model.contains("getFullName().length() < 2"));
        assertTrue(model.contains("getFullName().length() > 100"));
        assertTrue(model.contains("getPhone().matches(\"^\\\\d{3}-\\\\d{3}-\\\\d{4}$\")"));
    }

    @Test
    void testGenerateAppliesNumericConstraints() throws IOException {
        Path spec = writeSpec("job-application.json", JOB_APPLICATION_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        String jsx = Files.readString(tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/JobApplication.jsx"));
        assertTrue(jsx.contains("min={0.0}"));
        assertTrue(jsx.contains("max={50.0}"));

        String model = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/JobApplication.java"));
        assertTrue(model.contains("getYearsOfExperience() < 0.0"));
        assertTrue(model.contains("getYearsOfExperience() > 50.0"));
    }

    @Test
    void testGenerateEnumRendersAsSelectNotInput() throws IOException {
        Path spec = writeSpec("job-application.json", JOB_APPLICATION_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        String jsx = Files.readString(tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/JobApplication.jsx"));
        assertTrue(jsx.contains("<select"));
        assertTrue(jsx.contains("<option value=\"Engineering\">Engineering</option>"));
        assertTrue(jsx.contains("<option value=\"Support\">Support</option>"));

        String model = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/JobApplication.java"));
        assertTrue(model.contains("Arrays.asList(\"Engineering\", \"Sales\", \"Marketing\", \"Support\")"));
        assertTrue(model.contains(".contains(getDepartment())"));
    }
}
