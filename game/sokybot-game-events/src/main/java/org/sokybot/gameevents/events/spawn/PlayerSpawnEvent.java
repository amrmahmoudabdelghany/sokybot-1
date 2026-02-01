package org.sokybot.gameevents.events.spawn;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.dto.PlayerData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class PlayerSpawnEvent implements IGameEvent {
    
    private final String fullName;
    private final PlayerData player;
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
