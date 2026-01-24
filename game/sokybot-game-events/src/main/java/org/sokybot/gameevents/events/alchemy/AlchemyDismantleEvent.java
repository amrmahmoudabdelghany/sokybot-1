package org.sokybot.gameevents.events.alchemy;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when alchemy dismantle operation completes.
 */
@Getter
@Builder
@ToString
public class AlchemyDismantleEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;
    private final byte result;
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
