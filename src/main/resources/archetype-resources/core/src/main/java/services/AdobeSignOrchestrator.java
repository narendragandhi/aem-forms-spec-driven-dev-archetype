package ${package}.services;

/**
 * Service to orchestrate Adobe Sign operations within AEM Forms workflows.
 */
public interface AdobeSignOrchestrator {
    
    /**
     * Create a mock signing agreement.
     * @param data The form data to be included in the agreement
     * @return The unique agreement ID
     */
    String createAgreement(String data);

    /**
     * Get the status of an agreement.
     * @param agreementId The ID to check
     * @return status (e.g. OUT_FOR_SIGNATURE, SIGNED)
     */
    String getStatus(String agreementId);
}
