package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.EntitySpawnEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.entities.navmesh.Position;

/**
 * Translates entity spawn packets (opcode 0x3015) to EntitySpawnEvent.
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
        // Implementation based on engine's SpawnParser.readSpawnData() pattern
        // ImmutablePacket provides read-only access to received packets
        
        try {
            // Get stream reader from packet (standard pattern from engine)
            var reader = packet.getStreamReader();
            
            // Read refId first (identifies the entity in static data)
            int refId = reader.getInt();
            
            // Read uniqueId (server-assigned entity ID)
            int entityId = reader.getInt();
            
            // Read sector coordinates
            int xSector = reader.getUnsignedByte();
            int ySector = reader.getUnsignedByte();
            
            // Read offset coordinates (position within sector)
            float xOffset = reader.getFloat();
            float zOffset = reader.getFloat();
            float yOffset = reader.getFloat();
            
            // Convert to world coordinates using SilkroadUtils pattern
            // For now, just use offsets directly (TODO: apply sector conversion)
            Position position = new Position(xOffset, yOffset, zOffset);
            
            return new EntitySpawnEvent(machineFullName, entityId, refId, position);
            
        } catch (Exception e) {
            // Return null to indicate translation failure
            // Publisher will log the error
            return null;
        }
    }
}
