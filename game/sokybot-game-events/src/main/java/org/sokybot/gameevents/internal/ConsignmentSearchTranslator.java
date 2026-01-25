package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.consignment.ConsignmentSearchEvent;
import org.sokybot.gameevents.events.consignment.ConsignmentSearchEvent.ConsignmentSearchEntry;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.ArrayList;
import java.util.List;

public class ConsignmentSearchTranslator extends AbstractTranslator {

    public ConsignmentSearchTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public ConsignmentSearchTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB50C;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte result = reader.getByte();
        byte entryCount = 0;
        byte pageCount = 0;
        List<ConsignmentSearchEntry> entries = new ArrayList<>();
        Integer errorCode = null;

        if (result == 1) {
            entryCount = reader.getByte();
            pageCount = reader.getByte();
            for (int i = 0; i < entryCount; i++) {
                entries.add(ConsignmentSearchEntry.builder()
                        .personalId(reader.getInt())
                        .sellerName(reader.getString())
                        .saleStatus(reader.getByte())
                        .refItemId(reader.getInt())
                        .sellCount(reader.getInt())
                        .price(reader.getLong())
                        .regDate(reader.getInt())
                        .build());
            }
        } else if (result == 2) {
            errorCode = (int) reader.getShort() & 0xFFFF;
        }

        return List.of(ConsignmentSearchEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .result(result)
                .entryCount(entryCount)
                .pageCount(pageCount)
                .entries(entries)
                .errorCode(errorCode)
                .build());
    }
}
