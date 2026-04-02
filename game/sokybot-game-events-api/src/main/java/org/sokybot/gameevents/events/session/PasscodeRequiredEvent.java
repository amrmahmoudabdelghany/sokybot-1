package org.sokybot.gameevents.events.session;

import org.sokybot.gameevents.events.core.IGameEvent;

public class PasscodeRequiredEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;

    public PasscodeRequiredEvent(String machineFullName) {
        this.fullName = machineFullName;
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
}
