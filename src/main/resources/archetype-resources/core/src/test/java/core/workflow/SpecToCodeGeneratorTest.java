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

    // --- nested objects, repeatable fields, conditional visibility ---

    @Test
    void testGenerateNestedObjectCreatesChildModelAndChildResource() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Employee Profile\",\n" +
                "  \"properties\": {\n" +
                "    \"employeeName\": { \"type\": \"string\", \"title\": \"Employee Name\" },\n" +
                "    \"address\": {\n" +
                "      \"type\": \"object\", \"title\": \"Home Address\",\n" +
                "      \"properties\": {\n" +
                "        \"street\": { \"type\": \"string\", \"title\": \"Street\" },\n" +
                "        \"city\": { \"type\": \"string\", \"title\": \"City\" }\n" +
                "      },\n" +
                "      \"required\": [\"street\"]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        Path specFile = writeSpec("employee-profile.json", spec);

        specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        Path childModel = tempDir.resolve("core/src/main/java/com/acme/core/models/HomeAddress.java");
        assertTrue(Files.exists(childModel), "Nested object should generate its own child Sling Model");
        String childSrc = Files.readString(childModel);
        // Child models adapt the child Resource directly, not the request —
        // ChildResourceInjector adapts the Resource itself via ModelFactory.
        assertTrue(childSrc.contains("import org.apache.sling.api.resource.Resource;"));
        assertTrue(childSrc.contains("@Model(adaptables = Resource.class,"));
        assertTrue(childSrc.contains("private String street;"));
        assertTrue(childSrc.contains("errors.add(\"Street is required\");"), "Child model gets its own validate()");

        String parentSrc = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/EmployeeProfile.java"));
        assertTrue(parentSrc.contains("import org.apache.sling.models.annotations.injectorspecific.ChildResource;"));
        assertTrue(parentSrc.contains("@ChildResource"));
        assertTrue(parentSrc.contains("private HomeAddress address;"));
        assertTrue(parentSrc.contains("public HomeAddress getAddress() {"));
        assertTrue(parentSrc.contains("for (String e : getAddress().validate()) {"), "Parent validate() recurses into the child");

        String html = Files.readString(tempDir.resolve(
                "ui.apps/src/main/content/jcr_root/apps/AcmeApp/components/generated/employee-profile/employee-profile.html"));
        // HTL's expression resolver follows Java getter chains — no
        // data-sly-resource/include needed for nested field access.
        assertTrue(html.contains("${model.address.street}"));
        assertTrue(html.contains("${model.address.city}"));

        String jsx = Files.readString(tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/EmployeeProfile.jsx"));
        assertTrue(jsx.contains("<fieldset className=\"employee-profile-address-group\">"));
        assertTrue(jsx.contains("<legend>Home Address</legend>"));
        assertTrue(jsx.contains("name={`${name}.address.street`}"), "Nested field gets a dotted name path");
        assertTrue(jsx.contains("id={`${id}-address-street`}"), "Nested field gets a hyphenated id");
    }

    @Test
    void testGenerateScalarArrayCreatesStandaloneItemComponentNoAddRemoveLogic() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Contact List\",\n" +
                "  \"properties\": {\n" +
                "    \"phoneNumbers\": {\n" +
                "      \"type\": \"array\", \"title\": \"Phone Numbers\",\n" +
                "      \"items\": { \"type\": \"string\", \"format\": \"tel\" }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        Path specFile = writeSpec("contact-list.json", spec);

        specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        String model = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/ContactList.java"));
        assertTrue(model.contains("private String[] phoneNumbers;"), "Scalar arrays bind via multi-value @ValueMapValue, not a child model");
        assertTrue(model.contains("public String[] getPhoneNumbers() {"));
        assertFalse(Files.exists(tempDir.resolve("core/src/main/java/com/acme/core/models/PhoneNumbers.java")),
                "No child Sling Model file for a scalar array — nothing to adapt as a Resource");

        String html = Files.readString(tempDir.resolve(
                "ui.apps/src/main/content/jcr_root/apps/AcmeApp/components/generated/contact-list/contact-list.html"));
        assertTrue(html.contains("data-sly-list.item=\"${model.phoneNumbers}\""));

        // Repetition in real Adaptive Forms is a panel/form-model concern
        // (see @aemforms/af-react-renderer's renderChildren) — the item
        // component itself must not contain add/remove logic.
        Path itemFile = tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/PhoneNumbers.jsx");
        assertTrue(Files.exists(itemFile), "Array item gets its own standalone, independently-mappable component");
        String itemJsx = Files.readString(itemFile);
        assertTrue(itemJsx.contains("type=\"tel\""));
        assertFalse(itemJsx.contains("useState"));
        assertFalse(itemJsx.contains("dispatchAddItem"));
        assertFalse(itemJsx.contains(".map("), "No client-side iteration — one item per component instance");
    }

    @Test
    void testGenerateObjectArrayCreatesChildModelAndStandaloneItemComponent() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Household\",\n" +
                "  \"properties\": {\n" +
                "    \"dependents\": {\n" +
                "      \"type\": \"array\", \"title\": \"Dependents\", \"itemTitle\": \"Dependent\",\n" +
                "      \"items\": {\n" +
                "        \"type\": \"object\",\n" +
                "        \"properties\": {\n" +
                "          \"name\": { \"type\": \"string\", \"title\": \"Name\" },\n" +
                "          \"age\": { \"type\": \"integer\", \"title\": \"Age\" }\n" +
                "        },\n" +
                "        \"required\": [\"name\"]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        Path specFile = writeSpec("household.json", spec);

        specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        assertTrue(Files.exists(tempDir.resolve("core/src/main/java/com/acme/core/models/Dependent.java")),
                "\"itemTitle\" names the child model, not a guessed singularization of the array field");

        String model = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/Household.java"));
        assertTrue(model.contains("private List<Dependent> dependents;"));
        assertTrue(model.contains(
                "return dependents != null ? dependents : Collections.emptyList();"),
                "Null-safe: @ChildResource yields null, not an empty list, when unauthored");

        String html = Files.readString(tempDir.resolve(
                "ui.apps/src/main/content/jcr_root/apps/AcmeApp/components/generated/household/household.html"));
        assertTrue(html.contains("data-sly-list.item=\"${model.dependents}\""));
        assertTrue(html.contains("${item.name}"));
        assertTrue(html.contains("${item.age}"));

        Path itemFile = tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/Dependent.jsx");
        assertTrue(Files.exists(itemFile));
        String itemJsx = Files.readString(itemFile);
        assertTrue(itemJsx.contains("name={`${name}.name`}"));
        assertTrue(itemJsx.contains("name={`${name}.age`}"));
        assertFalse(itemJsx.contains("useState"));
    }

    @Test
    void testGenerateConditionalVisibilityAffectsHtlReactAndValidate() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Marital Info\",\n" +
                "  \"properties\": {\n" +
                "    \"maritalStatus\": { \"type\": \"string\", \"title\": \"Marital Status\", \"enum\": [\"Single\", \"Married\"] },\n" +
                "    \"spouseName\": { \"type\": \"string\", \"title\": \"Spouse Name\",\n" +
                "      \"visibleWhen\": { \"field\": \"maritalStatus\", \"equals\": \"Married\" } }\n" +
                "  },\n" +
                "  \"required\": [\"spouseName\"]\n" +
                "}\n";
        Path specFile = writeSpec("marital-info.json", spec);

        specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        String model = Files.readString(tempDir.resolve("core/src/main/java/com/acme/core/models/MaritalInfo.java"));
        assertTrue(model.contains("if (\"Married\".equals(getMaritalStatus())) {"));
        assertTrue(model.contains("errors.add(\"Spouse Name is required\");"),
                "Required-ness only applies inside the visibleWhen guard");

        String html = Files.readString(tempDir.resolve(
                "ui.apps/src/main/content/jcr_root/apps/AcmeApp/components/generated/marital-info/marital-info.html"));
        // core/src/test/java is Velocity-filtered too — a literal dollar-brace
        // wrapping a complex expression (operators, quoted string) fails to
        // parse as a Velocity reference, the same collision the generator's
        // own ref() helper guards against. Simple property-chain assertions
        // elsewhere in this file (${model.firstName} etc.) parse fine; this
        // one has && / == / a quoted string inside the braces, so build it
        // from two literals instead of writing the sequence directly.
        assertTrue(html.contains("$" + "{model.spouseName && model.maritalStatus == 'Married'}"));

        String jsx = Files.readString(tempDir.resolve(
                "ui.frontend.react.forms.af/src/main/webpack/components/generated/MaritalInfo.jsx"));
        assertTrue(jsx.contains("{(value && value.maritalStatus === 'Married') && ("));
    }

    @Test
    void testGenerateThrowsForNestingBeyondOneLevel() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Bad\",\n" +
                "  \"properties\": {\n" +
                "    \"outer\": { \"type\": \"object\", \"title\": \"Outer\",\n" +
                "      \"properties\": {\n" +
                "        \"inner\": { \"type\": \"object\", \"title\": \"Inner\",\n" +
                "          \"properties\": { \"x\": { \"type\": \"string\", \"title\": \"X\" } } }\n" +
                "      } }\n" +
                "  }\n" +
                "}\n";
        Path specFile = writeSpec("bad-nesting.json", spec);

        IOException ex = assertThrows(IOException.class,
                () -> specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp"));
        assertTrue(ex.getMessage().contains("one-level nesting limit"));
    }

    @Test
    void testGenerateThrowsForVisibleWhenReferencingUnknownField() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Bad\",\n" +
                "  \"properties\": {\n" +
                "    \"a\": { \"type\": \"string\", \"title\": \"A\",\n" +
                "      \"visibleWhen\": { \"field\": \"doesNotExist\", \"equals\": \"x\" } }\n" +
                "  }\n" +
                "}\n";
        Path specFile = writeSpec("bad-visiblewhen.json", spec);

        IOException ex = assertThrows(IOException.class,
                () -> specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp"));
        assertTrue(ex.getMessage().contains("referencing unknown field"));
    }

    @Test
    void testGenerateThrowsForVisibleWhenSelfReference() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Bad\",\n" +
                "  \"properties\": {\n" +
                "    \"a\": { \"type\": \"string\", \"title\": \"A\",\n" +
                "      \"visibleWhen\": { \"field\": \"a\", \"equals\": \"x\" } }\n" +
                "  }\n" +
                "}\n";
        Path specFile = writeSpec("bad-self-ref.json", spec);

        IOException ex = assertThrows(IOException.class,
                () -> specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp"));
        assertTrue(ex.getMessage().contains("referencing itself"));
    }

    @Test
    void testGenerateThrowsForVisibleWhenReferencingNonScalarField() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Bad\",\n" +
                "  \"properties\": {\n" +
                "    \"address\": { \"type\": \"object\", \"title\": \"Address\",\n" +
                "      \"properties\": { \"street\": { \"type\": \"string\", \"title\": \"Street\" } } },\n" +
                "    \"a\": { \"type\": \"string\", \"title\": \"A\",\n" +
                "      \"visibleWhen\": { \"field\": \"address\", \"equals\": \"x\" } }\n" +
                "  }\n" +
                "}\n";
        Path specFile = writeSpec("bad-nonscalar-ref.json", spec);

        IOException ex = assertThrows(IOException.class,
                () -> specToCodeGenerator.generate(specFile.toString(), tempDir.toString(), "com.acme", "AcmeApp"));
        assertTrue(ex.getMessage().contains("referencing non-scalar field"));
    }

    // The actual specs/benefits-enrollment.json shipped by this archetype —
    // exercises nested objects, scalar arrays, object arrays, and
    // conditional visibility together, the same integration-golden pattern
    // as job-application.json for validation keywords.
    private static final String BENEFITS_ENROLLMENT_SPEC = "{\n" +
            "  \"title\": \"Benefits Enrollment\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"employeeName\": { \"type\": \"string\", \"title\": \"Employee Name\", \"minLength\": 2, \"maxLength\": 100 },\n" +
            "    \"maritalStatus\": { \"type\": \"string\", \"title\": \"Marital Status\", \"enum\": [\"Single\", \"Married\"] },\n" +
            "    \"spouseName\": { \"type\": \"string\", \"title\": \"Spouse Name\",\n" +
            "      \"visibleWhen\": { \"field\": \"maritalStatus\", \"equals\": \"Married\" } },\n" +
            "    \"mailingAddress\": {\n" +
            "      \"type\": \"object\", \"title\": \"Mailing Address\",\n" +
            "      \"properties\": {\n" +
            "        \"street\": { \"type\": \"string\", \"title\": \"Street\" },\n" +
            "        \"city\": { \"type\": \"string\", \"title\": \"City\" },\n" +
            "        \"zipCode\": { \"type\": \"string\", \"title\": \"ZIP Code\", \"pattern\": \"^\\\\d{5}$\" }\n" +
            "      },\n" +
            "      \"required\": [\"street\", \"city\", \"zipCode\"]\n" +
            "    },\n" +
            "    \"phoneNumbers\": {\n" +
            "      \"type\": \"array\", \"title\": \"Phone Numbers\",\n" +
            "      \"items\": { \"type\": \"string\", \"format\": \"tel\", \"pattern\": \"^\\\\d{3}-\\\\d{3}-\\\\d{4}$\" }\n" +
            "    },\n" +
            "    \"dependents\": {\n" +
            "      \"type\": \"array\", \"title\": \"Dependents\", \"itemTitle\": \"Dependent\",\n" +
            "      \"items\": {\n" +
            "        \"type\": \"object\",\n" +
            "        \"properties\": {\n" +
            "          \"name\": { \"type\": \"string\", \"title\": \"Name\" },\n" +
            "          \"age\": { \"type\": \"integer\", \"title\": \"Age\", \"minimum\": 0, \"maximum\": 120 },\n" +
            "          \"relationship\": { \"type\": \"string\", \"title\": \"Relationship\", \"enum\": [\"Child\", \"Parent\", \"Other\"] }\n" +
            "        },\n" +
            "        \"required\": [\"name\", \"relationship\"]\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\"employeeName\", \"maritalStatus\", \"spouseName\", \"mailingAddress\"]\n" +
            "}\n";

    @Test
    void testGenerateWithBenefitsEnrollmentSpecExercisesEveryCapabilityTogether() throws IOException {
        Path spec = writeSpec("benefits-enrollment.json", BENEFITS_ENROLLMENT_SPEC);

        specToCodeGenerator.generate(spec.toString(), tempDir.toString(), "com.acme", "AcmeApp");

        Path modelsDir = tempDir.resolve("core/src/main/java/com/acme/core/models");
        assertTrue(Files.exists(modelsDir.resolve("BenefitsEnrollment.java")));
        assertTrue(Files.exists(modelsDir.resolve("MailingAddress.java")), "Nested object -> child model");
        assertTrue(Files.exists(modelsDir.resolve("Dependent.java")), "Object array -> child model, named via itemTitle");
        assertFalse(Files.exists(modelsDir.resolve("PhoneNumbers.java")), "Scalar array never gets a child model");

        Path reactDir = tempDir.resolve("ui.frontend.react.forms.af/src/main/webpack/components/generated");
        assertTrue(Files.exists(reactDir.resolve("BenefitsEnrollment.jsx")));
        assertTrue(Files.exists(reactDir.resolve("PhoneNumbers.jsx")), "Scalar array item -> standalone component");
        assertTrue(Files.exists(reactDir.resolve("Dependent.jsx")), "Object array item -> standalone component");

        String model = Files.readString(modelsDir.resolve("BenefitsEnrollment.java"));
        assertTrue(model.contains("if (\"Married\".equals(getMaritalStatus())) {"));
        assertTrue(model.contains("for (String e : getMailingAddress().validate()) {"));
        assertTrue(model.contains("for (String e : dependentsItem.validate()) {"));
    }

    // --- generateForm() (complete Adaptive Form) -----------------------------

    private static final String ONBOARDING_FORM_SPEC = "{\n" +
            "  \"title\": \"Employee Onboarding\",\n" +
            "  \"panels\": [\n" +
            "    {\n" +
            "      \"title\": \"Personal Details\",\n" +
            "      \"properties\": {\n" +
            "        \"fullName\": { \"type\": \"string\", \"title\": \"Full Name\" },\n" +
            "        \"email\": { \"type\": \"string\", \"title\": \"Email\", \"format\": \"email\" },\n" +
            "        \"startDate\": { \"type\": \"string\", \"title\": \"Start Date\", \"format\": \"date\" },\n" +
            "        \"department\": { \"type\": \"string\", \"title\": \"Department\", \"enum\": [\"Engineering\", \"Sales\"] },\n" +
            "        \"homeAddress\": {\n" +
            "          \"type\": \"object\", \"title\": \"Home Address\",\n" +
            "          \"properties\": {\n" +
            "            \"street\": { \"type\": \"string\", \"title\": \"Street\" },\n" +
            "            \"city\": { \"type\": \"string\", \"title\": \"City\" }\n" +
            "          },\n" +
            "          \"required\": [\"street\"]\n" +
            "        }\n" +
            "      },\n" +
            "      \"required\": [\"fullName\", \"email\", \"startDate\"]\n" +
            "    },\n" +
            "    {\n" +
            "      \"title\": \"Emergency Contacts\",\n" +
            "      \"properties\": {\n" +
            "        \"contacts\": {\n" +
            "          \"type\": \"array\", \"title\": \"Emergency Contacts\", \"itemTitle\": \"Contact\",\n" +
            "          \"items\": {\n" +
            "            \"type\": \"object\",\n" +
            "            \"properties\": {\n" +
            "              \"name\": { \"type\": \"string\", \"title\": \"Name\" },\n" +
            "              \"phone\": { \"type\": \"string\", \"title\": \"Phone\" }\n" +
            "            },\n" +
            "            \"required\": [\"name\"]\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"required\": [\"contacts\"]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    private Path generatedFormXml(String appName, String slug) {
        return tempDir.resolve("ui.content/src/main/content/jcr_root/content/forms/af/" + appName + "/" + slug + "/.content.xml");
    }

    @Test
    void testGenerateFormCreatesPageWithGuideContainerAndPanels() throws IOException {
        Path spec = writeSpec("employee-onboarding.json", ONBOARDING_FORM_SPEC);

        specToCodeGenerator.generateForm(spec.toString(), tempDir.toString(), "AcmeApp");

        Path pageFile = generatedFormXml("AcmeApp", "employee-onboarding");
        assertTrue(Files.exists(pageFile), "Adaptive Form page should be generated");
        String xml = Files.readString(pageFile);
        assertTrue(xml.contains("jcr:primaryType=\"cq:Page\""));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/page\""));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/formcontainer\""));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/panel\""));
        assertTrue(xml.contains("name=\"rootPanel\""));
        assertTrue(xml.contains("cq:template=\"/conf/AcmeApp/settings/wcm/templates/page-content\""));
    }

    @Test
    void testGenerateFormEmitsOnePanelNodePerSpecPanel() throws IOException {
        Path spec = writeSpec("employee-onboarding.json", ONBOARDING_FORM_SPEC);

        specToCodeGenerator.generateForm(spec.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "employee-onboarding"));
        assertTrue(xml.contains("jcr:title=\"Personal Details\""));
        assertTrue(xml.contains("jcr:title=\"Emergency Contacts\""));
        assertTrue(xml.contains("<personalDetailsPanel"));
        assertTrue(xml.contains("<emergencyContactsPanel"));
    }

    @Test
    void testGenerateFormMapsFieldTypesToStandardResourceTypes() throws IOException {
        Path spec = writeSpec("employee-onboarding.json", ONBOARDING_FORM_SPEC);

        specToCodeGenerator.generateForm(spec.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "employee-onboarding"));
        assertTrue(xml.contains("<fullName"));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/textinput\""));
        assertTrue(xml.contains("fieldType=\"text-input\""));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/emailinput\""));
        assertTrue(xml.contains("fieldType=\"email-input\""));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/datepicker\""));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/dropdown\""));
        assertTrue(xml.contains("required=\"{Boolean}true\""));
    }

    @Test
    void testGenerateFormEmitsNestedObjectAsPanel() throws IOException {
        Path spec = writeSpec("employee-onboarding.json", ONBOARDING_FORM_SPEC);

        specToCodeGenerator.generateForm(spec.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "employee-onboarding"));
        assertTrue(xml.contains("<homeAddress"));
        assertTrue(xml.contains("jcr:title=\"Home Address\""));
        assertTrue(xml.contains("<street"));
        assertTrue(xml.contains("<city"));
    }

    @Test
    void testGenerateFormEmitsRepeatableObjectArrayAsTable() throws IOException {
        Path spec = writeSpec("employee-onboarding.json", ONBOARDING_FORM_SPEC);

        specToCodeGenerator.generateForm(spec.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "employee-onboarding"));
        assertTrue(xml.contains("<contacts"));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/table\""));
        assertTrue(xml.contains("minOccur=\"{Long}1\""), "contacts is required -> minOccur 1");
        assertTrue(xml.contains("<row1"));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/tablerow\""));
        assertTrue(xml.contains("<name"));
        assertTrue(xml.contains("<phone"));
    }

    @Test
    void testGenerateFormWiresRealSubmitAction() throws IOException {
        Path spec = writeSpec("employee-onboarding.json", ONBOARDING_FORM_SPEC);

        specToCodeGenerator.generateForm(spec.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "employee-onboarding"));
        assertTrue(xml.contains("xmlns:fd=\"http://www.adobe.com/aemfd/fd/1.0\""));
        assertTrue(xml.contains("actionType=\"fd/af/components/guidesubmittype/restendpoint\""));
        assertTrue(xml.contains("thankYouOption=\"page\""));
        assertTrue(xml.contains("<submitButton"));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/actions/submit\""));
        assertTrue(xml.contains("fieldType=\"button\""));
        assertTrue(xml.contains("buttonType=\"submit\""));
        assertTrue(xml.contains("click=\"[submitForm()]\""));
    }

    @Test
    void testGenerateFormPlacesSubmitButtonInLastPanelOnly() throws IOException {
        Path spec = writeSpec("employee-onboarding.json", ONBOARDING_FORM_SPEC);

        specToCodeGenerator.generateForm(spec.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "employee-onboarding"));
        int firstPanelEnd = xml.indexOf("</personalDetailsPanel>");
        int secondPanelStart = xml.indexOf("<emergencyContactsPanel");
        int submitButtonIndex = xml.indexOf("<submitButton");
        assertTrue(firstPanelEnd > 0 && secondPanelStart > firstPanelEnd, "sanity check on panel ordering");
        assertTrue(submitButtonIndex > secondPanelStart, "submit button should be inside the last panel, not the first");
        assertEquals(1, xml.split("<submitButton", -1).length - 1, "exactly one submit button should be generated");
    }

    @Test
    void testGenerateFormEmitsRepeatableScalarArrayAsTable() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Tagged Form\",\n" +
                "  \"panels\": [{\n" +
                "    \"title\": \"Panel\",\n" +
                "    \"properties\": {\n" +
                "      \"tags\": { \"type\": \"array\", \"title\": \"Tags\", \"items\": { \"type\": \"string\" } }\n" +
                "    },\n" +
                "    \"required\": [\"tags\"]\n" +
                "  }]\n" +
                "}\n";
        Path specFile = writeSpec("tagged-form.json", spec);

        specToCodeGenerator.generateForm(specFile.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "tagged-form"));
        assertTrue(xml.contains("<tags"));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/table\""));
        assertTrue(xml.contains("minOccur=\"{Long}1\""), "tags is required -> minOccur 1");
        assertTrue(xml.contains("<row1"));
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/tablerow\""));
        assertTrue(xml.contains("<value"));
        assertTrue(xml.contains("name=\"value\""),
                "the row-item field is named 'value', not 'tags' - it would otherwise collide with the enclosing "
                        + "table's own name and produce per-row data keyed by the array's own name");
        assertTrue(xml.contains("sling:resourceType=\"AcmeApp/components/adaptiveForm/textinput\""),
                "the synthesized single-item field for the scalar array should render as a normal scalar field");
    }

    @Test
    void testGenerateFormEmitsVisibilityRuleForVisibleWhen() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Conditional Form\",\n" +
                "  \"panels\": [{\n" +
                "    \"title\": \"Panel\",\n" +
                "    \"properties\": {\n" +
                "      \"maritalStatus\": { \"type\": \"string\", \"title\": \"Marital Status\" },\n" +
                "      \"spouseName\": { \"type\": \"string\", \"title\": \"Spouse Name\", \"visibleWhen\": { \"field\": \"maritalStatus\", \"equals\": \"Married\" } }\n" +
                "    }\n" +
                "  }]\n" +
                "}\n";
        Path specFile = writeSpec("conditional-form.json", spec);

        specToCodeGenerator.generateForm(specFile.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "conditional-form"));
        assertTrue(xml.contains("<spouseName"));
        assertTrue(xml.contains("<fd:rules"));
        assertTrue(xml.contains("visible=\"maritalStatus == 'Married'\""),
                "Expected a visibility rule referencing maritalStatus == 'Married', got: " + xml);
        // maritalStatus itself has no visibleWhen, so it should be self-closing, not wrapped around an fd:rules child.
        assertTrue(xml.contains("<maritalStatus\n") || xml.contains("<maritalStatus "));
    }

    @Test
    void testGenerateFormEscapesSingleQuoteInVisibleWhenValue() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Conditional Form Quote\",\n" +
                "  \"panels\": [{\n" +
                "    \"title\": \"Panel\",\n" +
                "    \"properties\": {\n" +
                "      \"status\": { \"type\": \"string\", \"title\": \"Status\" },\n" +
                "      \"detail\": { \"type\": \"string\", \"title\": \"Detail\", \"visibleWhen\": { \"field\": \"status\", \"equals\": \"It's Complicated\" } }\n" +
                "    }\n" +
                "  }]\n" +
                "}\n";
        Path specFile = writeSpec("conditional-form-quote.json", spec);

        assertDoesNotThrow(() -> specToCodeGenerator.generateForm(specFile.toString(), tempDir.toString(), "AcmeApp"));
        String xml = Files.readString(generatedFormXml("AcmeApp", "conditional-form-quote"));
        assertTrue(xml.contains("visible=\"status == 'It\\'s Complicated'\""),
                "Expected the embedded single quote to be escaped, got: " + xml);
    }

    @Test
    void testGenerateFormEmitsTextinputValidationConstraints() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Validated Form\",\n" +
                "  \"panels\": [{\n" +
                "    \"title\": \"Panel\",\n" +
                "    \"properties\": {\n" +
                "      \"username\": { \"type\": \"string\", \"title\": \"Username\", \"minLength\": 3, \"maxLength\": 20, \"pattern\": \"^[a-z0-9]+$\" }\n" +
                "    }\n" +
                "  }]\n" +
                "}\n";
        Path specFile = writeSpec("validated-form.json", spec);

        specToCodeGenerator.generateForm(specFile.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "validated-form"));
        assertTrue(xml.contains("minLength=\"3\""));
        assertTrue(xml.contains("maxLength=\"20\""));
        assertTrue(xml.contains("pattern=\"^[a-z0-9]+$\""));
    }

    @Test
    void testGenerateFormEmitsNumberinputMinMaxConstraints() throws IOException {
        String spec = "{\n" +
                "  \"title\": \"Age Form\",\n" +
                "  \"panels\": [{\n" +
                "    \"title\": \"Panel\",\n" +
                "    \"properties\": {\n" +
                "      \"age\": { \"type\": \"integer\", \"title\": \"Age\", \"minimum\": 18, \"maximum\": 120 }\n" +
                "    }\n" +
                "  }]\n" +
                "}\n";
        Path specFile = writeSpec("age-form.json", spec);

        specToCodeGenerator.generateForm(specFile.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "age-form"));
        assertTrue(xml.contains("minimum=\"18\""), "whole-number minimum should print without a trailing .0, got: " + xml);
        assertTrue(xml.contains("maximum=\"120\""), "whole-number maximum should print without a trailing .0, got: " + xml);
    }

    @Test
    void testGenerateFormOmitsLengthConstraintsOnEmailinputButKeepsPattern() throws IOException {
        // emailinput's real _cq_dialog (AEM Core Forms Components) only
        // exposes a pattern property, not minLength/maxLength - unlike
        // textinput's dialog, which has both.
        String spec = "{\n" +
                "  \"title\": \"Email Form\",\n" +
                "  \"panels\": [{\n" +
                "    \"title\": \"Panel\",\n" +
                "    \"properties\": {\n" +
                "      \"email\": { \"type\": \"string\", \"title\": \"Email\", \"format\": \"email\", \"minLength\": 5, \"pattern\": \".+@.+\" }\n" +
                "    }\n" +
                "  }]\n" +
                "}\n";
        Path specFile = writeSpec("email-form.json", spec);

        specToCodeGenerator.generateForm(specFile.toString(), tempDir.toString(), "AcmeApp");

        String xml = Files.readString(generatedFormXml("AcmeApp", "email-form"));
        assertTrue(xml.contains("pattern=\".+@.+\""));
        assertFalse(xml.contains("minLength="), "emailinput's real dialog has no minLength property, unlike textinput's");
    }

    @Test
    void testGenerateFormThrowsWhenNoPanels() throws IOException {
        Path specFile = writeSpec("no-panels.json", "{\"title\": \"Empty\"}\n");

        assertThrows(IOException.class,
                () -> specToCodeGenerator.generateForm(specFile.toString(), tempDir.toString(), "AcmeApp"));
    }
}
