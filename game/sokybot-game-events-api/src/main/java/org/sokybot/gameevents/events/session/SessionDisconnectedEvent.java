package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when the bot session loses its network connection.
 * Carries the disconnect reason and whether it was operator-initiated.
 */
public class SessionDisconnectedEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final String reason;
    private final boolean expected;

    /**
     * @param fullName  machine full name (group.machine)
     * @param reason    disconnect reason from Netty cause (nullable)
     * @param expected  true when the disconnect was operator-initiated (e.g. logout)
     */
    public SessionDisconnectedEvent(String fullName, String reason, boolean expected) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.reason = reason;
        this.expected = expected;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the disconnect reason (may be null for clean disconnects).
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns true if this disconnect was operator-initiated (e.g. explicit logout).
     */
    public boolean isExpected() {
        return expected;
    }

    @Override
    public String toString() {
        return "SessionDisconnectedEvent{fullName='" + fullName
                + "', reason='" + reason
                + "', expected=" + expected + '}';
    }
}
