package org.sokybot.api.events;

/**
 * Event fired when an entity levels up (opcode 0x3054).
 * Also used for pet/fellow level ups.
 */
public class LevelUpEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    
    public LevelUpEvent(String fullName, int entityId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    
    @Override
    public String toString() {
        return String.format("LevelUpEvent[%s, entity=%d]", fullName, entityId);
    }
}
