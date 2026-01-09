package org.sokybot.gameevents.events.inventory;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when item durability is updated (opcode 0x3052).
 */
public class ItemDurabilityUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final int slot;
    private final long durability;
    
    public ItemDurabilityUpdateEvent(String fullName, int slot, long durability) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.slot = slot;
        this.durability = durability;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getSlot() { return slot; }
    public long getDurability() { return durability; }
    
    @Override
    public String toString() {
        return "ItemDurabilityUpdateEvent{slot=" + slot + ", dur=" + durability + "}";
    }
}
