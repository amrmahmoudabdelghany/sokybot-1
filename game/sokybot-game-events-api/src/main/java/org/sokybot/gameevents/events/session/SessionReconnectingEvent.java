package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when the login-cycle enters a retry delay and is about
 * to attempt reconnection.
 */
public class SessionReconnectingEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final int attempt;
    private final long nextAttemptAtMs;

    /**
     * @param fullName        machine full name (group.machine)
     * @param attempt         1-based retry counter
     * @param nextAttemptAtMs epoch millis when the next connection attempt will fire
     */
    public SessionReconnectingEvent(String fullName, int attempt, long nextAttemptAtMs) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.attempt = attempt;
        this.nextAttemptAtMs = nextAttemptAtMs;
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
     * Gets the 1-based retry attempt counter (resets on successful connect).
     */
    public int getAttempt() {
        return attempt;
    }

    /**
     * Gets the epoch millis timestamp when the next reconnection attempt is scheduled.
     */
    public long getNextAttemptAtMs() {
        return nextAttemptAtMs;
    }

    @Override
    public String toString() {
        return "SessionReconnectingEvent{fullName='" + fullName
                + "', attempt=" + attempt
                + ", nextAttemptAtMs=" + nextAttemptAtMs + '}';
    }
}
