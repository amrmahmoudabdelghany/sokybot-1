package org.sokybot.gameevents.events.entity;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when an entity despawns (dies, disappears, leaves range)
 */
public class EntityDespawnEvent implements IGameEvent {
    
    private final String fullName;
    private final int entityId;
    private final long timestamp;
    
    public EntityDespawnEvent(String fullName, int entityId) {
        this.fullName = fullName;
        this.entityId = entityId;
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
    
    public int getEntityId() {
        return entityId;
    }
    
    @Override
    public String toString() {
        return String.format("EntityDespawnEvent[%s, entityId=%d]", fullName, entityId);
    }
}
