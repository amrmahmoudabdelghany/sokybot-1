package org.sokybot.translators.trade.internal;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.trade.events.StallClosed;

enum StallDestroyedTranslator implements IPacketTranslator {
    INSTANCE;

    @Override
    public int getOpcode() {
        return TradePacketOpcodes.STALL_DESTROYED;
    }

    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
        try {
            IStreamReader sr = packet.getPacketReader().asStreamReader();
            int entityId = sr.getInt();
            return Collections.singletonList(new StallClosed(machineFullName, entityId));
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }
}
