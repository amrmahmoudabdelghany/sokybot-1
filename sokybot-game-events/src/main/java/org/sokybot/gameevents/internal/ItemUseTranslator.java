package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.inventory.ItemUseEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates item use response packets (opcode 0xB04C).
 * Based on RSBot InventoryItemUseResponse.
 */
public class ItemUseTranslator extends AbstractTranslator {
    
    public ItemUseTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0xB04C;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            if (reader.getByte() != 1) { // Success check
                return noEvents();
            }
            int sourceSlot = reader.getByte();
            int newAmount = reader.getShort() & 0xFFFF; // UShort
            return singleEvent(new ItemUseEvent(machineFullName, sourceSlot, newAmount));
        } catch (Exception e) {
            return noEvents();
        }
}
