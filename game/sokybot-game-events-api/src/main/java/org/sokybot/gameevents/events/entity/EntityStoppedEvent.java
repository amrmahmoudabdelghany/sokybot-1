package org.sokybot.gameevents.events.entity;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when an entity stops moving (stuck/stopped).
 * Based on SPAWN_STUCK packet.
 */
public class EntityStoppedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    
    public EntityStoppedEvent(String machineFullName, int entityId) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }
    
    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getEntityId() {
        return entityId;
    }
}
