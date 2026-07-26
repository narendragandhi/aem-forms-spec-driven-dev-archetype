package ${package}.services;

/**
 * Thrown when dispatching a form submission to the configured external
 * REST API fails.
 */
public class FormSubmissionException extends Exception {

    public FormSubmissionException(String message) {
        super(message);
    }

    public FormSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
