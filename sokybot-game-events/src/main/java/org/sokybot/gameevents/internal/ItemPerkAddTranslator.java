package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.inventory.ItemPerkAddEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates item perk add packets (opcode 0x325F) to ItemPerkAddEvent.
 * Reference: RSBot ActionItemPerkAddResponse = 0x325F
 */
public class ItemPerkAddTranslator extends AbstractTranslator {
    
    private static final int ITEM_PERK_ADD_OPCODE = 0x325F;
    
    public ItemPerkAddTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return ITEM_PERK_ADD_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int targetId = reader.getInt();
            int itemRefId = reader.getInt();
            int token = reader.getInt();
            int value = reader.getInt();
            int remainingTime = reader.getInt();
            
            return singleEvent(new ItemPerkAddEvent(machineFullName, targetId, itemRefId, 
                                                     token, value, remainingTime));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
