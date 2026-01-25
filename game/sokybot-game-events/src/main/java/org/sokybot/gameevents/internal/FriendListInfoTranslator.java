package org.sokybot.gameevents.internal;

import java.util.ArrayList;
import java.util.List;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.community.FriendListInfoEvent;
import org.sokybot.gameevents.events.community.FriendListInfoEvent.FriendEntry;
import org.sokybot.gameevents.events.community.FriendListInfoEvent.FriendGroup;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates friend list info (opcode 0x3305).
 */
public class FriendListInfoTranslator extends AbstractTranslator {
    public FriendListInfoTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public FriendListInfoTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0x3305;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();

            int groupCount = reader.getByte() & 0xFF;
            List<FriendGroup> groups = new ArrayList<>();
            for (int i = 0; i < groupCount; i++) {
                groups.add(FriendGroup.builder()
                        .id(reader.getShort() & 0xFFFF)
                        .name(reader.getString())
                        .build());
            }

            int friendCount = reader.getByte() & 0xFF;
            List<FriendEntry> friends = new ArrayList<>();
            for (int i = 0; i < friendCount; i++) {
                friends.add(FriendEntry.builder()
                        .charId(reader.getInt())
                        .name(reader.getString())
                        .modelId(reader.getInt())
                        .groupId(reader.getShort() & 0xFFFF)
                        .offline(reader.getByte() != 0)
                        .build());
            }

            return singleEvent(new FriendListInfoEvent(machineFullName, groups, friends));
        } catch (Exception e) {
            return noEvents();
        }
    }
}
