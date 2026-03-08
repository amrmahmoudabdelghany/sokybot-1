package org.sokybot.gameevents.events.core;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class AbstractGameEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;

    public AbstractGameEvent(String fullName) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
    }
}
