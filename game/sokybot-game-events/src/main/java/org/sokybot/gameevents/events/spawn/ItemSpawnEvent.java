package org.sokybot.gameevents.events.spawn;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.game.dto.ItemData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class ItemSpawnEvent implements IGameEvent {
    
    private final String fullName;
    private final ItemData item;
    private final long timestamp = System.currentTimeMillis();

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
