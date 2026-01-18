package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.inventory.InventorySizeUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates inventory/storage size update packets (opcode 0x3092).
 * Based on RSBot InventoryUpdateSizeResponse.
 */
public class InventorySizeUpdateTranslator extends AbstractTranslator {
    
    public InventorySizeUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x3092;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte type = reader.getByte();
            int size = reader.getByte();
            return singleEvent(new InventorySizeUpdateEvent(machineFullName, type, size));
        } catch (Exception e) {
            return noEvents();
        }
}
}
