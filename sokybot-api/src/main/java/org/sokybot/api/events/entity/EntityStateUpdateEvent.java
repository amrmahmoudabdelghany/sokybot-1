package org.sokybot.api.events;

/**
 * Event fired when an entity's state changes (HP, MP, position, etc.)
 */
public class EntityStateUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final int entityId;
    private final Integer currentHP;  // null if not in update
    private final Integer currentMP;  // null if not in update
    private final long timestamp;
    
    public EntityStateUpdateEvent(String fullName, int entityId, Integer currentHP, Integer currentMP) {
        this.fullName = fullName;
        this.entityId = entityId;
        this.currentHP = currentHP;
        this.currentMP = currentMP;
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
    
    public Integer getCurrentHP() {
        return currentHP;
    }
    
    public Integer getCurrentMP() {
        return currentMP;
    }
    
    @Override
    public String toString() {
        return String.format("EntityStateUpdateEvent[%s, entityId=%d, HP=%s, MP=%s]",
            fullName, entityId, currentHP, currentMP);
    }
}
