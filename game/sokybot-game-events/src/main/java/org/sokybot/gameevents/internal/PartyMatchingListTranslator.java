package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.party.PartyMatchingListEvent;
import org.sokybot.gameevents.events.party.PartyMatchingListEvent.PartyMatchEntry;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.ArrayList;
import java.util.List;

public class PartyMatchingListTranslator extends AbstractTranslator {

    public PartyMatchingListTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public PartyMatchingListTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB06C;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte result = reader.getByte();
        byte pageCount = 0;
        byte pageIndex = 0;
        List<PartyMatchEntry> entries = new ArrayList<>();

        if (result == 1) {
            pageCount = reader.getByte();
            pageIndex = reader.getByte();
            byte partyCount = reader.getByte();

            for (int i = 0; i < partyCount; i++) {
                entries.add(PartyMatchEntry.builder()
                        .partyNumber(reader.getInt())
                        .masterJid(reader.getInt())
                        .masterName(reader.getString())
                        .countryType(reader.getByte())
                        .memberCount(reader.getByte())
                        .settingsFlag(reader.getByte())
                        .purposeType(reader.getByte())
                        .levelMin(reader.getByte())
                        .levelMax(reader.getByte())
                        .title(reader.getString())
                        .build());
            }
        }

        return List.of(PartyMatchingListEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .result(result)
                .pageCount(pageCount)
                .pageIndex(pageIndex)
                .entries(entries)
                .build());
    }
}
