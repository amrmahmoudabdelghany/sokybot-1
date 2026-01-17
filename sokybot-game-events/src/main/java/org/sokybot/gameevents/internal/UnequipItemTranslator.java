package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.inventory.UnequipItemVisualEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates unequip item visual packets (opcode 0x3039) to UnequipItemVisualEvent.
 * This is the visual update broadcast to other players when equipment is removed.
 * Reference: SilkroadScript ItemUnwear = 0x3039
 */
public class UnequipItemTranslator extends AbstractTranslator {
    
    private static final int UNEQUIP_ITEM_OPCODE = 0x3039;
    public UnequipItemTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return UNEQUIP_ITEM_OPCODE;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read entity ID
            int entityId = reader.getInt();
            // Read equipment slot being cleared
            byte slot = reader.getByte();
            return singleEvent(new UnequipItemVisualEvent(machineFullName, entityId, slot));
        } catch (Exception e) {
            return noEvents();
        }
}
