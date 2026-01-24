package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.world.FRPVPUpdateEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.List;

public class FRPVPUpdateTranslator extends AbstractTranslator {

    public FRPVPUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public FRPVPUpdateTranslator() {
        this(null);
    }

    public int getOpcode() {
        return 0xB516;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte result = reader.getByte();
        FRPVPUpdateEvent.FRPVPUpdateEventBuilder builder = FRPVPUpdateEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .result(result);

        if (result == 0x01) {
            builder.uniqueId(reader.getInt());
            builder.mode(reader.getByte());
        } else if (result == 0x02) {
            builder.errorCode((int) reader.getShort());
        }

        return List.of(builder.build());
    }
}
