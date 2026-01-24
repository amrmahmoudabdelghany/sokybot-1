package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.alchemy.AlchemyDismantleEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.List;

public class AlchemyDismantleTranslator extends AbstractTranslator {

    public AlchemyDismantleTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public AlchemyDismantleTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB157;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte result = reader.getByte();
        if (result == 2) { // Error
            int errorCode = (int) reader.getShort();
            return List.of(AlchemyDismantleEvent.builder()
                    .fullName(machineId)
                    .timestamp(timestamp)
                    .result(result)
                    .errorCode(errorCode)
                    .build());
        }

        return List.of(AlchemyDismantleEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .result(result)
                .build());
    }
}
