package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.guild.GuildInfoEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates guild info packets (opcode 0x3101) to GuildInfoEvent.
 * Reference: SilkroadLeoBot SERVER_GUILDINFO = 0x3101
 */
public class GuildInfoTranslator extends AbstractTranslator {
    
    private static final int GUILD_INFO_OPCODE = 0x3101;
    
    public GuildInfoTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return GUILD_INFO_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int guildId = reader.getInt();
            String guildName = reader.getString();
            int level = reader.getUnsignedByte();
            int memberCount = reader.getUnsignedByte();
            
            return singleEvent(new GuildInfoEvent(machineFullName, guildId, guildName, level, memberCount));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
