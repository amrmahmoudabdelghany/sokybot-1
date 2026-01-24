package org.sokybot.gameevents.events.guild;

import lombok.Builder;
import lombok.Getter;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when a guild entity is updated (opcode 0x30FF).
 */
@Getter
@Builder
public class GuildEntityUpdateEvent implements IGameEvent {
    private final String fullName;
    private final long timestamp = System.currentTimeMillis();

    private final int dwGid;
    private final int guildId;
    private final String guildName;
    private final String grandName;
    private final int guildCrestRev;
    private final int unionId;
    private final int unionCrestRev;
    private final byte isFriendly;
    private final byte siegeAuthority;

    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }

    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }
}
