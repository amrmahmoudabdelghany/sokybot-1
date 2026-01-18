package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.inventory.ItemPerkRemoveEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates item perk remove packets (opcode 0x3261) to ItemPerkRemoveEvent.
 * Reference: RSBot ActionItemPerkRemoveResponse = 0x3261
 */
public class ItemPerkRemoveTranslator extends AbstractTranslator {
    
    private static final int ITEM_PERK_REMOVE_OPCODE = 0x3261;
    public ItemPerkRemoveTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return ITEM_PERK_REMOVE_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int targetId = reader.getInt();
            int itemRefId = reader.getInt();
            int token = reader.getInt();
            return singleEvent(new ItemPerkRemoveEvent(machineFullName, targetId, itemRefId, token));
        } catch (Exception e) {
            return noEvents();
        }
}

}
