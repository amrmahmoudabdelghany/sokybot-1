package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.pk.PKUpdateEvent;
import org.sokybot.gameevents.events.pk.PKUpdateEvent.PKUpdateType;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.Collections;
import java.util.List;

public class PKUpdateTranslator extends AbstractTranslator {

    public PKUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public PKUpdateTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0; // Unified translator for multiple PK opcodes
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        int opcode = packet.getOpcode();
        long timestamp = System.currentTimeMillis();

        switch (opcode) {
            case 0x30CD: // Penalty Point
                return List.of(PKUpdateEvent.builder()
                        .fullName(machineId)
                        .timestamp(timestamp)
                        .type(PKUpdateType.PENALTY)
                        .penaltyPoints(reader.getInt())
                        .build());
            case 0x30CE: // Daily PK
                return List.of(PKUpdateEvent.builder()
                        .fullName(machineId)
                        .timestamp(timestamp)
                        .type(PKUpdateType.DAILY)
                        .dailyPk(reader.getByte())
                        .build());
            case 0x30D3: // PK Level
                return List.of(PKUpdateEvent.builder()
                        .fullName(machineId)
                        .timestamp(timestamp)
                        .type(PKUpdateType.LEVEL)
                        .totalPk((int) reader.getShort())
                        .build());
            default:
                return Collections.emptyList();
        }
    }
}
