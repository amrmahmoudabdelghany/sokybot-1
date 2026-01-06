package org.sokybot.api.events;

import org.sokybot.persistence.entities.navmesh.Position;

/**
 * Event fired when an entity (monster, NPC, player, item) spawns in the game world.
 */
public class EntitySpawnEvent implements IGameEvent {
    
    private final String fullName;
    private final int entityId;
    private final int refId;
    private final Position position;
    private final long timestamp;
    
    public EntitySpawnEvent(String fullName, int entityId, int refId, Position position) {
        this.fullName = fullName;
        this.entityId = entityId;
        this.refId = refId;
        this.position = position;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the unique entity ID assigned by the server.
     */
    public int getEntityId() {
        return entityId;
    }
    
    /**
     * Gets the reference ID pointing to NPCEntity, ItemEntity, etc.
     */
    public int getRefId() {
        return refId;
    }
    
    /**
     * Gets the spawn position.
     */
    public Position getPosition() {
        return position;
    }
    
    @Override
    public String toString() {
        return String.format("EntitySpawnEvent[%s, entityId=%d, refId=%d, pos=%s]",
            fullName, entityId, refId, position);
    }
}
