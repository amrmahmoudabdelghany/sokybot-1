package org.sokybot.gameevents.events.skill;

import org.sokybot.gameevents.events.core.IGameEvent;
import lombok.Getter;
import lombok.ToString;

/**
 * Event for mastery level down response (opcode 0xB203).
 */
@Getter
@ToString
public class MasteryLevelDownEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final int errorCode;

    public MasteryLevelDownEvent(String fullName, boolean success, int errorCode) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.errorCode = errorCode;
    }
}
