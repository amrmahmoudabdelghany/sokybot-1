package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.world.GameNotifyEvent;
import org.sokybot.gameevents.events.world.GameNotifyEvent.NotifyType;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.Collections;
import java.util.List;

public class GameNotifyTranslator extends AbstractTranslator {

    public GameNotifyTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public GameNotifyTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0x300C;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte result = reader.getByte();
        if (result != 0) {
            switch (result) {
                case 0x05: // unique spawned
                    reader.getByte(); // result
                    int spawnModelId = reader.getInt();
                    return List.of(GameNotifyEvent.builder()
                            .fullName(machineId)
                            .timestamp(timestamp)
                            .type(NotifyType.UNIQUE_SPAWNED)
                            .modelId(spawnModelId)
                            .build());
                case 0x06: // unique killed
                    int killModelId = reader.getInt();
                    return List.of(GameNotifyEvent.builder()
                            .fullName(machineId)
                            .timestamp(timestamp)
                            .type(NotifyType.UNIQUE_KILLED)
                            .modelId(killModelId)
                            .build());
            }
        }

        return Collections.emptyList();
    }
}
