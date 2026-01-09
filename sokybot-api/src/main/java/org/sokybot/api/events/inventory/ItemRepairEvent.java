package org.sokybot.api.events;

/**
 * Event fired for item repair confirmation (opcode 0xB03E).
 */
public class ItemRepairEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final byte slot;
    private final long cost;
    
    public ItemRepairEvent(String fullName, boolean success, byte slot, long cost) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.slot = slot;
        this.cost = cost;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public byte getSlot() { return slot; }
    public long getCost() { return cost; }
    
    @Override
    public String toString() {
        return String.format("ItemRepairEvent[%s, success=%b, slot=%d, cost=%d]", 
            fullName, success, slot, cost);
    }
}
