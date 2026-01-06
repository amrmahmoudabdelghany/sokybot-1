package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.StorageOpenEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates storage data begin packets (opcode 0x3047).
 * This is the begin packet for chunked storage data.
 * Based on RSBot InventoryStorageDataBeginResponse.
 */
public class StorageOpenTranslator extends AbstractTranslator {
    
    private final int opcode;
    private final byte storageType;
    
    public StorageOpenTranslator(IGameDataLookup lookup, int opcode, byte storageType) {
        super(lookup);
        this.opcode = opcode;
        this.storageType = storageType;
    }
    
    public StorageOpenTranslator(IGameDataLookup lookup) {
        this(lookup, 0x3047, StorageOpenEvent.TYPE_PERSONAL);
    }
    
    @Override
    public int getOpcode() {
        return opcode;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            long storageGold = reader.getLong();
            
            return singleEvent(new StorageOpenEvent(machineFullName, storageGold, storageType));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
