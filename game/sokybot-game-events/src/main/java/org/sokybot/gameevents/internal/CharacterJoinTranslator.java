package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.character.CharacterJoinEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.List;

public class CharacterJoinTranslator extends AbstractTranslator {

    public CharacterJoinTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public CharacterJoinTranslator() {
        this(null);
    }

    public int getOpcode() {
        return 0xB001;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte result = reader.getByte();
        boolean success = result == 0x01;
        Integer errorCode = null;
        if (!success) {
            errorCode = (int) reader.getShort();
        }

        return List.of(CharacterJoinEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .success(success)
                .errorCode(errorCode)
                .build());
    }
}
