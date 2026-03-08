package org.sokybot.gameevents.events.arena;

import lombok.Builder;
import lombok.Getter;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.Map;

/**
 * Event fired when battle arena operation occurs (opcode 0x34D2).
 */
@Getter
@Builder
public class BattleArenaOperationEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp = System.currentTimeMillis();

    private final byte operation;
    private final Map<String, Object> data;

    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }

    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }
}
