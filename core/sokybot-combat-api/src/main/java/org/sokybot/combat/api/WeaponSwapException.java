package org.sokybot.combat.api;

/**
 * Failure completing an asynchronous weapon swap (timeout, inventory error, etc.).
 */
public final class WeaponSwapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WeaponSwapException(String message) {
        super(message);
    }

    public WeaponSwapException(String message, Throwable cause) {
        super(message, cause);
    }
}
