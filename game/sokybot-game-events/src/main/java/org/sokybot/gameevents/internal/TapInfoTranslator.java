package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.tap.TapInfoEvent;
import org.sokybot.gameevents.events.tap.TapInfoEvent.TapEntry;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.ArrayList;
import java.util.List;

public class TapInfoTranslator extends AbstractTranslator {

    public TapInfoTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public TapInfoTranslator() {
        this(null);
    }

    @Override
    public int getOpcode() {
        return 0xB4DF;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        byte count = reader.getByte();
        List<TapEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(TapEntry.builder()
                    .entryType(reader.getByte())
                    .fromMonth(reader.getByte())
                    .fromDay(reader.getByte())
                    .fromHour(reader.getByte())
                    .fromMinute(reader.getByte())
                    .toMonth(reader.getByte())
                    .toDay(reader.getByte())
                    .toHour(reader.getByte())
                    .toMinute(reader.getByte())
                    .traderUnionPoints(reader.getInt())
                    .thiefUnionPoints(reader.getInt())
                    .build());
        }

        return List.of(TapInfoEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .entries(entries)
                .build());
    }
}
