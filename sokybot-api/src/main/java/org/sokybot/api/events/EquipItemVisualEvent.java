package org.sokybot.api.events;

/**
 * Event fired when an entity visually equips an item (opcode 0x3038).
 * This is a visual update for other players to see equipment changes.
 */
public class EquipItemVisualEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final byte slot;          // Equipment slot (head, body, weapon, etc.)
    private final int itemRefId;      // Static item reference ID
    private final byte optLevel;      // Item enhancement level (+1, +2, etc.)
    
    public EquipItemVisualEvent(String fullName, int entityId, byte slot, 
                                int itemRefId, byte optLevel) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.slot = slot;
        this.itemRefId = itemRefId;
        this.optLevel = optLevel;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public byte getSlot() { return slot; }
    public int getItemRefId() { return itemRefId; }
    public byte getOptLevel() { return optLevel; }
    
    @Override
    public String toString() {
        return String.format("EquipItemVisualEvent[%s, entity=%d, slot=%d, item=%d, +%d]", 
            fullName, entityId, slot, itemRefId, optLevel);
    }
}
