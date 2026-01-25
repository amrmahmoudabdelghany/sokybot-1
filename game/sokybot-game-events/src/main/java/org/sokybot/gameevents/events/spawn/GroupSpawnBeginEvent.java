package org.sokybot.gameevents.events.spawn;

import org.sokybot.gameevents.events.core.IGameEvent;
import lombok.Getter;
import lombok.ToString;

/**
 * Event indicating the start of a group spawn (opcode 0x3017).
 */
@Getter
@ToString
public class GroupSpawnBeginEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;
    private final byte spawnType;
    private final int expectedCount;

    public GroupSpawnBeginEvent(String fullName, byte spawnType, int expectedCount) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.spawnType = spawnType;
        this.expectedCount = expectedCount;
    }
}
