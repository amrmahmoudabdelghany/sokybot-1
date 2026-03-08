package org.sokybot.gameevents.events.storage;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when a chunk of storage data (0x3049) is received.
 */
@Getter
@Builder
@ToString
public class StorageDataEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final byte[] data;

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
