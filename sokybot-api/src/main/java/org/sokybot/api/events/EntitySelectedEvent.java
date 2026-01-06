package org.sokybot.api.events;

/**
 * Event fired when an entity is selected (targeted).
 * Based on SPAWN_SELECTED packet.
 */
public class EntitySelectedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int selectedEntityId;
    private final Integer currentHP;
    
    public EntitySelectedEvent(String machineFullName, int selectedEntityId, Integer currentHP) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.selectedEntityId = selectedEntityId;
        this.currentHP = currentHP;
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
    
    public int getSelectedEntityId() {
        return selectedEntityId;
    }
    
    public Integer getCurrentHP() {
        return currentHP;
    }
}
