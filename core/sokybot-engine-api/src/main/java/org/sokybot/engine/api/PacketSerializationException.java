package org.sokybot.engine.api;

/**
 * Thrown when Java-owned packet serialization fails.
 */
public class PacketSerializationException extends DispatchException {
    public PacketSerializationException(String message) {
        super(message);
    }

    public PacketSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
