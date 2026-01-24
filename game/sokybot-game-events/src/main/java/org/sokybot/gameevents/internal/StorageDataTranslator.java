package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.storage.StorageDataEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;

import java.util.List;

public class StorageDataTranslator extends AbstractTranslator {

    public StorageDataTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public StorageDataTranslator() {
        this(null);
    }

    public int getOpcode() {
        return 0x3049;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        long timestamp = System.currentTimeMillis();

        return List.of(StorageDataEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .data(packet.getPacketReader().readFully())
                .build());
    }
}
