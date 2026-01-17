package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.inventory.ItemOwnershipRemovedEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates item ownership removal packets (opcode 0x304D) to ItemOwnershipRemovedEvent.
 * Reference: RSBot EntityRemoveOwnershipResponse = 0x304D
 */
public class ItemOwnershipRemovedTranslator extends AbstractTranslator {
    
    private static final int ITEM_OWNERSHIP_OPCODE = 0x304D;
    public ItemOwnershipRemovedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return ITEM_OWNERSHIP_OPCODE;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int itemUniqueId = reader.getInt();
            return singleEvent(new ItemOwnershipRemovedEvent(machineFullName, itemUniqueId));
        } catch (Exception e) {
            return noEvents();
        }
}
