package org.sokybot.translators.trade.internal;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.trade.events.StallOpened;

/**
 * Name change uses same projection shape as {@link StallOpened} (stall title update).
 */
enum StallNameChangedTranslator implements IPacketTranslator {
    INSTANCE;

    @Override
    public int getOpcode() {
        return TradePacketOpcodes.STALL_NAME_CHANGED;
    }

    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
        try {
            IStreamReader sr = packet.getPacketReader().asStreamReader();
            int entityId = sr.getInt();
            int len = sr.getShort() & 0xFFFF;
            String title = len > 0 && len < 2048 ? sr.getUnicodeString(len) : "";
            return Collections.singletonList(new StallOpened(machineFullName, entityId, title));
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }
}
