package org.sokybot.gameevents.events.academy;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when academy honor points are updated.
 */
@Getter
@Builder
@ToString
public class AcademyUpdateEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp;
    private final byte updateType;
    private final int charId;
    private final int newHonorBalance;

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
