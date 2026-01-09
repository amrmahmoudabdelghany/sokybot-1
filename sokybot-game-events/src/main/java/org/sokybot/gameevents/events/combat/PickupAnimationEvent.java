package org.sokybot.gameevents.events.combat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when an entity plays a pickup animation (opcode 0x3036).
 * Triggered when player or NPC picks up an item from the ground.
 */
public class PickupAnimationEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;       // Entity performing the pickup
    private final int targetItemId;   // Unique ID of item being picked up
    
    public PickupAnimationEvent(String fullName, int entityId, int targetItemId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.targetItemId = targetItemId;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public int getTargetItemId() { return targetItemId; }
    
    @Override
    public String toString() {
        return String.format("PickupAnimationEvent[%s, entity=%d, item=%d]", 
            fullName, entityId, targetItemId);
    }
}
