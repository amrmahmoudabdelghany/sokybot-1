package org.sokybot.gameevents.events.world;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when a Free PvP (FRPVP) mode update is received.
 */
@Getter
@Builder
@ToString
public class FRPVPUpdateEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final byte result;
    private final Integer uniqueId;
    private final Byte mode;
    private final Integer errorCode;

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
