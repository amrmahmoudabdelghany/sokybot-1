package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.EntitySpawnEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.entities.navmesh.Position;

/**
 * Translates entity spawn packets (opcode 0x3015) to EntitySpawnEvent.
 * Parsing logic based on engine's SpawnParser.readSpawnData() pattern.
 */
@Component(service = IPacketTranslator.class)
public class EntitySpawnTranslator implements IPacketTranslator {
    
    private static final int ENTITY_SPAWN_OPCODE = 0x3015;
    
    @Override
    public int getOpcode() {
        return ENTITY_SPAWN_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // 1. Read refId first (identifies entity type in static data)
            int refId = reader.getInt();
            
            // 2. Read uniqueId (server-assigned entity ID)
            int entityId = reader.getInt();
            
            // 3. Read sector coordinates (unsigned bytes)
            int xSector = reader.getUnsignedByte();
            int ySector = reader.getUnsignedByte();
            
            // 4. Read offset coordinates (float values)
            float xOffset = reader.getFloat();
            float zOffset = reader.getFloat();  // Z is "height"
            float yOffset = reader.getFloat();
            
            // 5. Read angle (short, needs conversion via SilkroadUtils.getAngle)
            short angle = reader.getShort();
            
            // 6. Compute world coordinates
            // World X = xSector * 192 * 10 + xOffset  (simplified)
            // World Y = ySector * 192 * 10 + yOffset
            float worldX = xSector * 1920 + xOffset;
            float worldY = ySector * 1920 + yOffset;
            
            Position position = new Position(worldX, worldY, zOffset);
            
            return new EntitySpawnEvent(machineFullName, entityId, refId,
                                       xSector, ySector, xOffset, yOffset, zOffset,
                                       angle, position);
            
        } catch (Exception e) {
            return null;
        }
    }
}

