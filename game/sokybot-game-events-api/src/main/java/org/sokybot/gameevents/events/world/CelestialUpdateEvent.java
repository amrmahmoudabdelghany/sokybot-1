package org.sokybot.gameevents.events.world;

import org.sokybot.gameevents.events.core.IGameEvent;
import lombok.Getter;
import lombok.ToString;

/**
 * Event for celestial/time updates (opcode 0x3027).
 */
@Getter
@ToString
public class CelestialUpdateEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;
    private final int moonphase;
    private final int hour;
    private final int minute;

    public CelestialUpdateEvent(String fullName, int moonphase, int hour, int minute) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.moonphase = moonphase;
        this.hour = hour;
        this.minute = minute;
    }
}
