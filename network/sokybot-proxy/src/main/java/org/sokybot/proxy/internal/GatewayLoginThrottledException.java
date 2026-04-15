package org.sokybot.proxy.internal;

/**
 * Thrown from {@link PacketEncoder} when a duplicate gateway 0x6102 is blocked (min-interval gate).
 * Fails the write promise without emitting bytes.
 */
public final class GatewayLoginThrottledException extends RuntimeException {

    public GatewayLoginThrottledException(String message) {
        super(message);
    }
}
