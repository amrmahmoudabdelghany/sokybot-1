package org.sokybot.api.events;

/**
 * Event fired when an entity's movement speed changes.
 * Based on SPEED_UPDATE packet.
 */
public class EntitySpeedUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final float walkSpeed;
    private final float runSpeed;
    
    public EntitySpeedUpdateEvent(String machineFullName, int entityId, 
                                 float walkSpeed, float runSpeed) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.walkSpeed = walkSpeed;
        this.runSpeed = runSpeed;
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
    
    public float getWalkSpeed() {
        return walkSpeed;
    }
    
    public float getRunSpeed() {
        return runSpeed;
    }
}
