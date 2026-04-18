package org.sokybot.translators.trade.internal;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.trade.events.TradeItemAdded;

enum ExchangeUpdateTranslator implements IPacketTranslator {
    INSTANCE;

    @Override
    public int getOpcode() {
        return TradePacketOpcodes.EXCHANGE_UPDATE;
    }

    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
        try {
            IStreamReader sr = packet.getPacketReader().asStreamReader();
            int exchangeId = sr.getInt();
            boolean selfOffer = sr.getByte() != 0;
            byte slot = sr.getByte();
            int itemRefId = sr.getInt();
            int quantity = sr.getShort() & 0xFFFF;
            long gold = sr.getLong();
            return Collections.singletonList(new TradeItemAdded(machineFullName, exchangeId, selfOffer, slot,
                    itemRefId, quantity, gold));
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }
}
