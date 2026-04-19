package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when the login cycle transitions to a new phase.
 */
public class LoginPhaseEvent implements IGameEvent {

    private final String fullName;
    private final String phase;
    private final long timestamp;

    public LoginPhaseEvent(String fullName, String phase) {
        this.fullName = fullName;
        this.phase = phase;
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

    public String getPhase() {
        return phase;
    }

    @Override
    public String toString() {
        return "LoginPhaseEvent{fullName='" + fullName + "', phase='" + phase + "'}";
    }
}
