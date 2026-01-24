package org.sokybot.gameevents.internal;

import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.tap.TapUpdateEvent;
import org.sokybot.gameevents.events.tap.TapUpdateEvent.TapEntryPoints;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;

import java.util.ArrayList;
import java.util.List;

public class TapUpdateTranslator extends AbstractTranslator {

    public TapUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }

    public TapUpdateTranslator() {
        this(null);
    }

    public int getOpcode() {
        return 0xB4E0;
    }

    @Override
    protected List<IGameEvent> translateInternal(String machineId, ImmutablePacket packet) {
        IStreamReader reader = packet.getStreamReader();
        long timestamp = System.currentTimeMillis();

        // The number of entries is determined by the payload size / 8 (2 uints)
        // or context from the INFO packet as per docs.
        // We'll parse until the end of the packet.
        List<TapEntryPoints> entries = new ArrayList<>();
        try {
            while (true) {
                entries.add(TapEntryPoints.builder()
                        .traderUnionPoints(reader.getInt())
                        .thiefUnionPoints(reader.getInt())
                        .build());
            }
        } catch (IndexOutOfBoundsException e) {
            // End of packet
        }

        return List.of(TapUpdateEvent.builder()
                .fullName(machineId)
                .timestamp(timestamp)
                .entries(entries)
                .build());
    }
}
