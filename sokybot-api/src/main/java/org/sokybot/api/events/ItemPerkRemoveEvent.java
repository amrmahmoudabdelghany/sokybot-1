package org.sokybot.api.events;

/**
 * Event fired when an item perk/buff is removed from an entity (opcode 0x3261).
 */
public class ItemPerkRemoveEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int targetEntityId;
    private final int itemRefId;
    private final int token;
    
    public ItemPerkRemoveEvent(String fullName, int targetEntityId, int itemRefId, int token) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.targetEntityId = targetEntityId;
        this.itemRefId = itemRefId;
        this.token = token;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getTargetEntityId() { return targetEntityId; }
    public int getItemRefId() { return itemRefId; }
    public int getToken() { return token; }
    
    @Override
    public String toString() {
        return String.format("ItemPerkRemoveEvent[%s, target=%d, item=%d, token=%d]", 
            fullName, targetEntityId, itemRefId, token);
    }
}
