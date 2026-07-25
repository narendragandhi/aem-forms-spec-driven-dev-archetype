package ${package}.services;

/**
 * Generates AEM Forms Interactive Communications - personalized documents
 * merging IC content (fragments, layout) with customer data.
 */
public interface InteractiveCommunicationService {

    /**
     * Render the Print Channel of an Interactive Communication as a PDF.
     *
     * @param icContentPath The DAM path of the Interactive Communication
     *                       (e.g. /content/dam/formsanddocuments/ic/&lt;appName&gt;/account-statement)
     * @param customerId The customer to fetch data for
     * @return the rendered PDF bytes
     */
    byte[] generatePrintPdf(String icContentPath, String customerId) throws InteractiveCommunicationException;
}
