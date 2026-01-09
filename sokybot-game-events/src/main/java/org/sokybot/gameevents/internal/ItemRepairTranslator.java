package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.inventory.ItemRepairEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates item repair response packets (opcode 0xB03E) to ItemRepairEvent.
 * Reference: SilkroadLeoBot SERVER_ITEMFIXED = 0xB03E
 */
public class ItemRepairTranslator extends AbstractTranslator {
    
    private static final int ITEM_REPAIR_OPCODE = 0xB03E;
    
    public ItemRepairTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return ITEM_REPAIR_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            boolean success = result == 0x01;
            
            byte slot = 0;
            long cost = 0;
            if (success) {
                slot = reader.getByte();
                cost = reader.getLong();
            }
            
            return singleEvent(new ItemRepairEvent(machineFullName, success, slot, cost));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
