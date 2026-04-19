package org.sokybot.navigation.api;

/**
 * Failure to complete a navigation request (e.g. no trainer position, packet not sent).
 */
public final class NavigationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NavigationException(String message) {
        super(message);
    }

    public NavigationException(String message, Throwable cause) {
        super(message, cause);
    }
}
