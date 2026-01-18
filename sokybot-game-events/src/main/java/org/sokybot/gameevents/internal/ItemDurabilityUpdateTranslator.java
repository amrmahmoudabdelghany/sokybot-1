package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.inventory.ItemDurabilityUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates item durability update packets (opcode 0x3052).
 * Based on RSBot InventoryUpdateDurabilityResponse.
 */
public class ItemDurabilityUpdateTranslator extends AbstractTranslator {
    
    public ItemDurabilityUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x3052;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            int slot = reader.getByte();
            long durability = Integer.toUnsignedLong(reader.getInt()); // UInt in RSBot -> long in Java
            return singleEvent(new ItemDurabilityUpdateEvent(machineFullName, slot, durability));
        } catch (Exception e) {
            return noEvents();
        }
}
}
