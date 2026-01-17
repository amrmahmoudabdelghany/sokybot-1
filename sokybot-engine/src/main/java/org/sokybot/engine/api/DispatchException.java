package org.sokybot.engine.api;

/**
 * Exception thrown when packet dispatch fails.
 */
public class DispatchException extends RuntimeException {
    
    public DispatchException(String message) {
        super(message);
    }
    
    public DispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
