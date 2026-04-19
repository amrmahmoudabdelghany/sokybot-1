package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when the bot session successfully authenticates and
 * the game channel is fully established.
 */
public class SessionConnectedEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;

    public SessionConnectedEvent(String fullName) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SessionConnectedEvent{fullName='" + fullName + "'}";
    }
}
