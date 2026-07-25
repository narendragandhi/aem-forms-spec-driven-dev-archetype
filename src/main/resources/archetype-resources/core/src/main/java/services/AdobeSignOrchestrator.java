package ${package}.services;

/**
 * Service to orchestrate Adobe Sign operations within AEM Forms workflows.
 */
public interface AdobeSignOrchestrator {

    /**
     * Send a document to Adobe Sign for signature. Adobe Sign signs a real
     * document, not raw form data, so callers must render one first (e.g.
     * via the AEM Forms DoRService, as SignToDoRProcess does).
     *
     * @param documentContent The PDF (or other supported format) bytes to send for signature
     * @param documentName A display name for the document/agreement
     * @param signerEmail The signer's email address
     * @return The real Adobe Sign agreement ID
     */
    String createAgreement(byte[] documentContent, String documentName, String signerEmail) throws AdobeSignException;

    /**
     * Get the current status of an agreement.
     * @param agreementId The ID to check
     * @return status (e.g. OUT_FOR_SIGNATURE, SIGNED)
     */
    String getStatus(String agreementId) throws AdobeSignException;

    /**
     * Download the fully executed agreement (signed document + audit trail)
     * once its status is SIGNED.
     * @param agreementId The signed agreement's ID
     * @return the combined PDF bytes
     */
    byte[] getSignedDocument(String agreementId) throws AdobeSignException;
}
