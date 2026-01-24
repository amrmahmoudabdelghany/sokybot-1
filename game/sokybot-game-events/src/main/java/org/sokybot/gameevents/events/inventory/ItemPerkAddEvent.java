package org.sokybot.gameevents.events.inventory;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when an item perk/buff is added to an entity (opcode 0x325F).
 * Item perks are temporary effects from consumables and equipment.
 */
public class ItemPerkAddEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int targetEntityId;
    private final int itemRefId;
    private final int token;      // Unique perk identifier
    private final int value;      // Perk effect value
    private final int remainingTimeMs;
    
    public ItemPerkAddEvent(String fullName, int targetEntityId, int itemRefId,
                            int token, int value, int remainingTimeMs) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.targetEntityId = targetEntityId;
        this.itemRefId = itemRefId;
        this.token = token;
        this.value = value;
        this.remainingTimeMs = remainingTimeMs;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getTargetEntityId() { return targetEntityId; }
    public int getItemRefId() { return itemRefId; }
    public int getToken() { return token; }
    public int getValue() { return value; }
    public int getRemainingTimeMs() { return remainingTimeMs; }
    
    @Override
    public String toString() {
        return String.format("ItemPerkAddEvent[%s, target=%d, item=%d, token=%d, value=%d, time=%dms]", 
            fullName, targetEntityId, itemRefId, token, value, remainingTimeMs);
    }
}
