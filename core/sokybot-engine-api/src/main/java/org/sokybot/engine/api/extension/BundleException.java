package org.sokybot.engine.api.extension;

/**
 * Exception thrown when actuator initialization or operation fails.
 */
public class BundleException extends RuntimeException {

    public BundleException(String message) {
        super(message);
    }

    public BundleException(String message, Throwable cause) {
        super(message, cause);
    }
}
