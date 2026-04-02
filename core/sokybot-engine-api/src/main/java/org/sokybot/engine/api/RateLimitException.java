package org.sokybot.engine.api;

/**
 * Thrown when protocol operation is blocked by rate-limiter policy.
 */
public class RateLimitException extends DispatchException {
    public RateLimitException(String message) {
        super(message);
    }
}
