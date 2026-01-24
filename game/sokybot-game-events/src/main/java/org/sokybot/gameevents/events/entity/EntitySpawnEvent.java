package org.sokybot.gameevents.events.entity;
import org.sokybot.gameevents.events.core.IGameEvent;

import org.sokybot.persistence.entities.navmesh.Position;

/**
 * Event fired when an entity (monster, NPC, player, item) spawns in the game world.
 * Based on SpawnParser.readSpawnData() pattern from engine.
 */
public class EntitySpawnEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    // Entity identification
    private final int entityId;  // Server-assigned unique ID
    private final int refId;     // Static data reference ID
    private final String entityName;  // Name from static data lookup
    
    // Position data
    private final int xSector;
    private final int ySector;
    private final float xOffset;
    private final float yOffset;
    private final float zOffset;
    private final short angle;
    
    // World coordinates (computed from sector + offset)
    private final Position position;
    
    public EntitySpawnEvent(String fullName, int entityId, int refId, String entityName,
                           int xSector, int ySector,
                           float xOffset, float yOffset, float zOffset,
                           short angle, Position position) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.refId = refId;
        this.entityName = entityName;
        this.xSector = xSector;
        this.ySector = ySector;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.angle = angle;
        this.position = position;
    }
    
    // Legacy constructor for backward compatibility
    public EntitySpawnEvent(String fullName, int entityId, int refId, 
                           int xSector, int ySector,
                           float xOffset, float yOffset, float zOffset,
                           short angle, Position position) {
        this(fullName, entityId, refId, null, xSector, ySector, xOffset, yOffset, zOffset, angle, position);
    }
    
    // Legacy constructor for backward compatibility
    public EntitySpawnEvent(String fullName, int entityId, int refId, Position position) {
        this(fullName, entityId, refId, null, 0, 0, 
             (float)position.getX(), (float)position.getY(), (float)position.getZ(),
             (short)0, position);
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public int getRefId() { return refId; }
    public String getEntityName() { return entityName; }
    public int getXSector() { return xSector; }
    public int getYSector() { return ySector; }
    public float getXOffset() { return xOffset; }
    public float getYOffset() { return yOffset; }
    public float getZOffset() { return zOffset; }
    public short getAngle() { return angle; }
    public Position getPosition() { return position; }
    
    @Override
    public String toString() {
        String name = entityName != null ? entityName : "ref=" + refId;
        return String.format("EntitySpawnEvent[%s, entity=%d, %s, sector=(%d,%d), angle=%d]",
            fullName, entityId, name, xSector, ySector, angle);
    }
}


