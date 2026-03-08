package org.sokybot.gameevents.events.character;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when a character world entry response is received.
 */
@Getter
@Builder
@ToString
public class CharacterJoinEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final boolean success;
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
