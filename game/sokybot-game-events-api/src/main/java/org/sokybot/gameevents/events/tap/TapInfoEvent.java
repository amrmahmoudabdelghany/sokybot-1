package org.sokybot.gameevents.events.tap;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

/**
 * Event fired when Temple Area Points (TAP) info is received.
 */
@Getter
@Builder
@ToString
public class TapInfoEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final List<TapEntry> entries;

    @Getter
    @Builder
    @ToString
    public static class TapEntry {
        private final byte entryType;
        private final byte fromMonth;
        private final byte fromDay;
        private final byte fromHour;
        private final byte fromMinute;
        private final byte toMonth;
        private final byte toDay;
        private final byte toHour;
        private final byte toMinute;
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
