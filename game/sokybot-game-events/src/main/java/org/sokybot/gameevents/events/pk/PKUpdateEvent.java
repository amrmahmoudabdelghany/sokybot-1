package org.sokybot.gameevents.events.pk;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when player kill statistics are updated.
 */
@Getter
@Builder
@ToString
public class PKUpdateEvent implements IGameEvent {

    public enum PKUpdateType {
        PENALTY,
        DAILY,
        LEVEL
    }

    private final String fullName;
    private final long timestamp;
    private final PKUpdateType type;
    private final Integer penaltyPoints;
    private final Byte dailyPk;
    private final Integer totalPk;

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
