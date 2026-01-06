package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.InventoryItemUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates inventory item update packets (opcode 0x3040).
 * Updates can include: RefObjID, OptLevel, Variance, Quantity, Durability, State, MagicParams.
 * Based on RSBot InventoryUpdateItemResponse.
 */
public class InventoryItemUpdateTranslator extends AbstractTranslator {
    
    public InventoryItemUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return 0x3040;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte sourceSlot = reader.getByte();
            byte updateFlags = reader.getByte();
            
            Integer itemId = null;
            Byte optLevel = null;
            Integer quantity = null;
            Integer durability = null;
            
            // Parse based on flags (from RSBot ItemUpdateFlag enum)
            if ((updateFlags & InventoryItemUpdateEvent.FLAG_REF_OBJ_ID) != 0) {
                itemId = reader.getInt();
            }
            
            if ((updateFlags & InventoryItemUpdateEvent.FLAG_OPT_LEVEL) != 0) {
                optLevel = reader.getByte();
            }
            
            if ((updateFlags & InventoryItemUpdateEvent.FLAG_VARIANCE) != 0) {
                reader.getLong(); // Read variance but don't store
            }
            
            if ((updateFlags & InventoryItemUpdateEvent.FLAG_QUANTITY) != 0) {
                quantity = reader.getShort() & 0xFFFF;
            }
            
            if ((updateFlags & InventoryItemUpdateEvent.FLAG_DURABILITY) != 0) {
                durability = reader.getInt();
            }
            
            if ((updateFlags & InventoryItemUpdateEvent.FLAG_STATE) != 0) {
                reader.getByte(); // Read state but don't store
            }
            
            if ((updateFlags & InventoryItemUpdateEvent.FLAG_MAG_PARAMS) != 0) {
                int magParamCount = reader.getByte() & 0xFF;
                for (int i = 0; i < magParamCount; i++) {
                    // Skip magic option parsing for now
                    reader.getInt(); // type
                    reader.getInt(); // value
                }
            }
            
            return singleEvent(new InventoryItemUpdateEvent(
                machineFullName, sourceSlot, updateFlags, itemId, optLevel, quantity, durability));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
