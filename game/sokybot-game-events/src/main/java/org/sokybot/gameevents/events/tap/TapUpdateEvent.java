package org.sokybot.gameevents.events.tap;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

/**
 * Event fired when Temple Area Points (TAP) update is received.
 */
@Getter
@Builder
@ToString
public class TapUpdateEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final List<TapEntryPoints> entries;

    @Getter
    @Builder
    @ToString
    public static class TapEntryPoints {
        private final int traderUnionPoints;
        private final int thiefUnionPoints;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
