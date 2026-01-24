package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.cos.CosUpdateEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.cos.CosUpdateType;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates COS update packets (opcode 0x30C9).
 * This handles pet state changes: termination, exp, hunger, name, model change.
 * Based on RSBot CosUpdateResponse.
 */
public class CosUpdateTranslator extends AbstractTranslator {
    
    public CosUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x30C9;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            int uniqueId = reader.getInt();
            byte typeId = reader.getByte();
            CosUpdateType updateType = CosUpdateType.fromTypeId(typeId);
            if (updateType == null) {
                // Unknown update type, still emit event with null type
                return singleEvent(new CosUpdateEvent(machineFullName, uniqueId, null));
            }
            long experience = 0;
            int sourceUniqueId = 0;
            int hungerPoints = 0;
            String newName = null;
            int newObjectId = 0;
            switch (updateType) {
                case TERMINATE:
                    // No additional data
                    break;
                    
                case INVENTORY:
                    // Inventory update - complex parsing, skip for now
                case EXPERIENCE:
                    experience = reader.getLong();
                    sourceUniqueId = reader.getInt();
                case HUNGER:
                    hungerPoints = reader.getShort() & 0xFFFF;
                case NAME_CHANGE:
                    newName = reader.getString();
                case MODEL_CHANGE:
                    newObjectId = reader.getInt();
                case FELLOW_KILL_EXP:
                    // Read exp data for fellow kill
                    experience = reader.getLong();  // pet exp
                    reader.getLong();  // skill exp
                    reader.getInt();   // total SP
                    sourceUniqueId = reader.getInt();  // mob id
            }
            return singleEvent(new CosUpdateEvent(
                machineFullName, uniqueId, updateType,
                experience, sourceUniqueId, hungerPoints, newName, newObjectId));
        } catch (Exception e) {
            return noEvents();
        }
}
}
