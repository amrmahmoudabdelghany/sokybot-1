package org.sokybot.translators.trade.internal;

import java.util.Collections;
import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.trade.events.StallItemListed;
import org.sokybot.trade.events.StallItemSold;

/**
 * Stall action opcode: best-effort decode of sale vs listing (layouts vary).
 */
enum StallActionTranslator implements IPacketTranslator {
    INSTANCE;

    private static final byte ACTION_LIST_ITEM = 1;
    private static final byte ACTION_BUY = 2;

    @Override
    public int getOpcode() {
        return TradePacketOpcodes.STALL_ACTION;
    }

    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet, ChunkedPacketManager chunkManager) {
        try {
            IStreamReader sr = packet.getPacketReader().asStreamReader();
            int stallEntityId = sr.getInt();
            byte action = sr.getByte();
            if (action == ACTION_LIST_ITEM) {
                byte slot = sr.getByte();
                int itemRefId = sr.getInt();
                int quantity = sr.getInt();
                long price = sr.getLong();
                return Collections.singletonList(new StallItemListed(machineFullName, stallEntityId, slot, itemRefId,
                        quantity, price));
            }
            if (action == ACTION_BUY) {
                int itemRefId = sr.getInt();
                int quantity = sr.getInt();
                long totalGold = sr.getLong();
                int nameLen = sr.getShort() & 0xFFFF;
                String buyer = nameLen > 0 && nameLen < 512 ? sr.getUnicodeString(nameLen) : "";
                return Collections.singletonList(
                        new StallItemSold(machineFullName, stallEntityId, itemRefId, quantity, totalGold, buyer));
            }
            return Collections.emptyList();
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }
}
