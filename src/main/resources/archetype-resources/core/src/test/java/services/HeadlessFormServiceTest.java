package ${package}.services;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class HeadlessFormServiceTest {

    private final HeadlessFormService servlet = new HeadlessFormService();

    @Test
    void testGetWithMissingFormPathReturns400(AemContext context) throws ServletException, IOException {
        servlet.doGet(context.request(), context.response());
        assertEquals(400, context.response().getStatus());
    }

    @Test
    void testGetWithEmptyFormPathReturns400(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("formPath", "");
        servlet.doGet(context.request(), context.response());
        assertEquals(400, context.response().getStatus());
    }

    @Test
    void testGetWithValidFormPathReturns200(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("formPath", "/content/dam/formsanddocuments/myform");
        servlet.doGet(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
    }

    @Test
    void testGetResponseContentType(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("formPath", "/content/forms/myform");
        servlet.doGet(context.request(), context.response());
        assertEquals("application/json", context.response().getContentType());
    }

    @Test
    void testGetResponseContainsBmadVersion(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("formPath", "/content/forms/myform");
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("bmadVersion"));
    }

    @Test
    void testGetResponseContainsFormPath(AemContext context) throws ServletException, IOException {
        String formPath = "/content/dam/formsanddocuments/registration";
        context.request().addRequestParameter("formPath", formPath);
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains(formPath));
    }

    @Test
    void testGetResponseContainsSubmitUrl(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("formPath", "/content/forms/myform");
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("submitUrl"));
    }

    @Test
    void testGetResponseContainsPrefillUrl(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("formPath", "/content/forms/myform");
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("prefillUrl"));
    }

    @Test
    void testGetResponseContainsEndpoint(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("formPath", "/content/forms/myform");
        servlet.doGet(context.request(), context.response());
        assertTrue(context.response().getOutputAsString().contains("endpoint"));
    }

    @Test
    void testFormIdIsDeterministicForSamePath(AemContext context) throws ServletException, IOException {
        String formPath = "/content/forms/consistent";
        context.request().addRequestParameter("formPath", formPath);
        servlet.doGet(context.request(), context.response());
        String expectedFormId = String.valueOf(formPath.hashCode());
        assertTrue(context.response().getOutputAsString().contains(expectedFormId));
    }

    @Test
    void testGetResponseCharacterEncoding(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("formPath", "/content/forms/myform");
        servlet.doGet(context.request(), context.response());
        assertEquals("UTF-8", context.response().getCharacterEncoding());
    }
}
