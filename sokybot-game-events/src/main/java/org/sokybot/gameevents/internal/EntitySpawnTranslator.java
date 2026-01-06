package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.EntitySpawnEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates entity spawn packets (opcode 0x3015) to EntitySpawnEvent.
 * Parsing logic based on engine's SpawnParser.readSpawnData() pattern.
 * Uses IGameDataLookup to enrich event with entity name.
 */
public class EntitySpawnTranslator extends AbstractTranslator {
    
    private static final int ENTITY_SPAWN_OPCODE = 0x3015;
    
    public EntitySpawnTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return ENTITY_SPAWN_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
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
            float worldX = xSector * 1920 + xOffset;
            float worldY = ySector * 1920 + yOffset;
            
            Position position = new Position(worldX, worldY, zOffset);
            
            // 7. Lookup entity name from static data
            String entityName = null;
            if (lookup != null) {
                entityName = lookup.findNPC(refId)
                    .map(npc -> npc.getName())
                    .orElseGet(() -> lookup.findItem(refId)
                        .map(item -> item.getName())
                        .orElse(null));
            }
            
            return singleEvent(new EntitySpawnEvent(machineFullName, entityId, refId, entityName,
                                       xSector, ySector, xOffset, yOffset, zOffset,
                                       angle, position));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}



