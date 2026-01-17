package org.sokybot.gameevents.events.character;

import org.sokybot.gameevents.events.core.AbstractGameEvent;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class TrainerStuckEvent extends AbstractGameEvent {

    private final int xSector;
    private final int ySector;
    private final float xOffset;
    private final float yOffset;
    private final float zOffset;
    private final short angle;
    // Calculated world coordinates for convenience
    private final int x;
    private final int y;

    public TrainerStuckEvent(String fullName, int xSector, int ySector, float xOffset, float yOffset, float zOffset, short angle, int x, int y) {
        super(fullName);
        this.xSector = xSector;
        this.ySector = ySector;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.angle = angle;
        this.x = x;
        this.y = y;
    }
}
