package org.sokybot.api.events;

/**
 * Event emitted when an item is used (opcode 0xB04C).
 */
public class ItemUseEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final int sourceSlot;
    private final int newAmount;
    
    public ItemUseEvent(String fullName, int sourceSlot, int newAmount) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.sourceSlot = sourceSlot;
        this.newAmount = newAmount;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getSourceSlot() { return sourceSlot; }
    public int getNewAmount() { return newAmount; }
    
    @Override
    public String toString() {
        return "ItemUseEvent{slot=" + sourceSlot + ", amount=" + newAmount + "}";
    }
}
