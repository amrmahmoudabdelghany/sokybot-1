package org.sokybot.gameevents.events.stall;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when a stall (shop) is updated.
 */
@Getter
@Builder
@ToString
public class StallUpdateEvent implements IGameEvent {

    public enum StallUpdateType {
        UPDATE_ITEM,
        ADD_ITEM,
        REMOVE_ITEM,
        STATE,
        MESSAGE,
        NAME,
        UNKNOWN
    }

    private final String fullName;
    private final long timestamp;
    private final StallUpdateType type;
    private final Byte result;
    private final Byte slot;
    private final Integer stackCount;
    private final Long price;
    private final Integer errorCode;
    private final Boolean isOpen;
    private final String message;
    private final String name;

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
