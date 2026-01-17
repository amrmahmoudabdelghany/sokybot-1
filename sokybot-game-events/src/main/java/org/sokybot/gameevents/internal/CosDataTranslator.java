package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.cos.CosDataEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.cos.CosType;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates COS data packets (opcode 0x30C8).
 * This is fired when a COS (controlled object) is summoned.
 * COS types: Transport(1), JobTransport(2), Growth(3), AbilityPet(4), Fellow(9)
 * Based on RSBot CosDataResponse.
 */
public class CosDataTranslator extends AbstractTranslator {
    
    public CosDataTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x30C8;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            int uniqueId = reader.getInt();
            int objectId = reader.getInt();
            // Determine COS type from object reference
            // TypeID2 == 2 && TypeID3 == 3 means it's a COS
            // TypeID4 determines the specific type
            // For now, we read the basic data and determine type from packet structure
            int hp = reader.getInt();
            int maxHp = reader.getInt();
            // COS type needs to be determined by looking up the objectId
            // in game reference data. Since we don't have that lookup yet,
            // we emit with null type - consumers can determine type from objectId
            CosType cosType = null;
            int ownerUniqueId = 0;
            if (cosType == CosType.JOB_TRANSPORT) {
                // JobTransport has inventory data followed by owner ID
                // Skip inventory for now, read owner
                // This is simplified - full implementation would parse inventory
                try {
                    // Try to read owner if available
                    ownerUniqueId = reader.getInt();
                } catch (Exception e) {
                    // Ignore if not available
                }
            }
            return singleEvent(new CosDataEvent(
                machineFullName, uniqueId, objectId, cosType, hp, maxHp, ownerUniqueId));
        } catch (Exception e) {
            return noEvents();
        }
}
