package org.sokybot.api.events;

/**
 * Event fired when an entity visually unequips an item (opcode 0x3039).
 * This is a visual update for other players to see equipment removals.
 */
public class UnequipItemVisualEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final byte slot;    // Equipment slot being cleared
    
    public UnequipItemVisualEvent(String fullName, int entityId, byte slot) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.slot = slot;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public byte getSlot() { return slot; }
    
    @Override
    public String toString() {
        return String.format("UnequipItemVisualEvent[%s, entity=%d, slot=%d]", 
            fullName, entityId, slot);
    }
}
