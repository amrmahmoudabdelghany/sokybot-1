package org.sokybot.gameevents.events.guild;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired for guild information updates (opcode 0x3101).
 */
public class GuildInfoEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int guildId;
    private final String guildName;
    private final int level;
    private final int memberCount;
    
    public GuildInfoEvent(String fullName, int guildId, String guildName, 
                          int level, int memberCount) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.guildId = guildId;
        this.guildName = guildName;
        this.level = level;
        this.memberCount = memberCount;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getGuildId() { return guildId; }
    public String getGuildName() { return guildName; }
    public int getLevel() { return level; }
    public int getMemberCount() { return memberCount; }
    
    @Override
    public String toString() {
        return String.format("GuildInfoEvent[%s, guild=%s, level=%d, members=%d]", 
            fullName, guildName, level, memberCount);
    }
}
