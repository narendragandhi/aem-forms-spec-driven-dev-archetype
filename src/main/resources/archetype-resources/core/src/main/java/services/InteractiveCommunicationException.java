package ${package}.services;

/**
 * Thrown when generating an Interactive Communication fails - either
 * fetching the customer data it's rendered with, or the real
 * PrintChannelRenderService call itself.
 */
public class InteractiveCommunicationException extends Exception {

    public InteractiveCommunicationException(String message) {
        super(message);
    }

    public InteractiveCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
