package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.storage.StorageBoxTakeItemEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates storage box take item packets (opcode 0xB558) to StorageBoxTakeItemEvent.
 * Reference: RSBot StorageBoxTakeItemResponse = 0xB558
 */
public class StorageBoxTakeItemTranslator extends AbstractTranslator {
    
    private static final int STORAGE_BOX_TAKE_OPCODE = 0xB558;
    public StorageBoxTakeItemTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return STORAGE_BOX_TAKE_OPCODE;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            boolean success = reader.getBoolean();
            int itemCount = 0;
            if (success) {
                itemCount = reader.getInt();
                // Skip the item data - we just emit notification
            }
            return singleEvent(new StorageBoxTakeItemEvent(machineFullName, success, itemCount));
        } catch (Exception e) {
            return noEvents();
        }
}
