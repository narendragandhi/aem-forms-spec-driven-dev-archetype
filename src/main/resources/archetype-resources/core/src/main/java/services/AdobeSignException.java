package ${package}.services;

/**
 * Thrown when a real Adobe Sign REST API call fails (network error, non-2xx
 * response, or a malformed response for a call this integration depends on).
 */
public class AdobeSignException extends Exception {

    public AdobeSignException(String message) {
        super(message);
    }

    public AdobeSignException(String message, Throwable cause) {
        super(message, cause);
    }
}
