package org.sokybot.translators.trade.internal;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.trade.events.TradeExchangeApproved;

enum ExchangeApprovedTranslator implements IPacketTranslator {
    INSTANCE;

    @Override
    public int getOpcode() {
        return TradePacketOpcodes.EXCHANGE_APPROVED;
    }

    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
        try {
            IStreamReader sr = packet.getPacketReader().asStreamReader();
            int exchangeId = sr.getInt();
            boolean success = sr.getByte() != 0;
            return Collections.singletonList(new TradeExchangeApproved(machineFullName, exchangeId, success));
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }
}
