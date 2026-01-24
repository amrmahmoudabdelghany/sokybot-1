package org.sokybot.gameevents.events.world;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired for global game notifications (e.g., unique spawn/kill).
 */
@Getter
@Builder
@ToString
public class GameNotifyEvent implements IGameEvent {

    public enum NotifyType {
        UNIQUE_SPAWNED,
        UNIQUE_KILLED,
        UNKNOWN
    }

    private final String fullName;
    private final long timestamp;
    private final NotifyType type;
    private final int modelId;

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
