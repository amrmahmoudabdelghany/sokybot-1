package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.academy.AcademyUpdateEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.Collections;
import java.util.List;

public class AcademyUpdateTranslator extends AbstractTranslator {

    public AcademyUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public AcademyUpdateTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0x3C80;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte updateType = reader.getByte();
        if (updateType == 5) { // Honor point update
            int charId = reader.getInt();
            reader.getByte(); // unknown
            reader.getByte(); // unknown
            reader.getByte(); // balance length
            int newHonorBalance = reader.getInt();

            return List.of(AcademyUpdateEvent.builder()
                    .fullName(machineId)
                    .timestamp(timestamp)
                    .updateType(updateType)
                    .charId(charId)
                    .newHonorBalance(newHonorBalance)
                    .build());
        }

        return Collections.emptyList();
    }
}
