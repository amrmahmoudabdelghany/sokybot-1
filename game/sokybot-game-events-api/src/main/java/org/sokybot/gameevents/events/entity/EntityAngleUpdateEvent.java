package org.sokybot.gameevents.events.entity;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when an entity's angle/direction changes.
 * Based on EnvironmentHandler.onAngleChanged() pattern.
 */
public class EntityAngleUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final short newAngle;
    
    public EntityAngleUpdateEvent(String machineFullName, int entityId, short newAngle) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.newAngle = newAngle;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public short getNewAngle() { return newAngle; }
}
