package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.guild.GuildEntityUpdateEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;

import java.util.List;

/**
 * Translates guild entity update (opcode 0x30FF).
 */
public class GuildEntityUpdateTranslator extends AbstractTranslator {

    public GuildEntityUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public GuildEntityUpdateTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0x30FF;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();

            var builder = GuildEntityUpdateEvent.builder()
                    .fullName(machineId)
                    .dwGid(reader.getInt())
                    .guildId(reader.getInt());

            int nameLen = reader.getShort() & 0xFFFF;
            builder.guildName(new String(reader.getBytes(nameLen)));

            int grandNameLen = reader.getShort() & 0xFFFF;
            builder.grandName(new String(reader.getBytes(grandNameLen)));

            builder.guildCrestRev(reader.getInt())
                    .unionId(reader.getInt())
                    .unionCrestRev(reader.getInt())
                    .isFriendly(reader.getByte())
                    .siegeAuthority(reader.getByte());

            return singleEvent(builder.build());

        } catch (Exception e) {
            return noEvents();
        }
    }
}
