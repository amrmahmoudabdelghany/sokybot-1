package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.inventory.EquipItemVisualEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates equip item visual packets (opcode 0x3038) to EquipItemVisualEvent.
 * This is the visual update broadcast to other players when equipment changes.
 * Reference: SilkroadScript ItemWear = 0x3038
 */
public class EquipItemTranslator extends AbstractTranslator {
    
    private static final int EQUIP_ITEM_OPCODE = 0x3038;
    
    public EquipItemTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return EQUIP_ITEM_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read entity ID
            int entityId = reader.getInt();
            
            // Read equipment slot
            byte slot = reader.getByte();
            
            // Read item reference ID (static data ID)
            int itemRefId = reader.getInt();
            
            // Read enhancement/opt level
            byte optLevel = reader.getByte();
            
            return singleEvent(new EquipItemVisualEvent(machineFullName, entityId, slot, itemRefId, optLevel));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
