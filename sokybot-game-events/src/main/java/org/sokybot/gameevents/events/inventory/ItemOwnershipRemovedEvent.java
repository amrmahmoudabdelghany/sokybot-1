package org.sokybot.gameevents.events.inventory;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when an item loses its owner (opcode 0x304D).
 * Dropped items become available for pickup by anyone.
 */
public class ItemOwnershipRemovedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int itemUniqueId;
    
    public ItemOwnershipRemovedEvent(String fullName, int itemUniqueId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.itemUniqueId = itemUniqueId;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getItemUniqueId() { return itemUniqueId; }
    
    @Override
    public String toString() {
        return String.format("ItemOwnershipRemovedEvent[%s, item=%d]", fullName, itemUniqueId);
    }
}
