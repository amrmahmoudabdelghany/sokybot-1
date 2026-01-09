package org.sokybot.api.events;

/**
 * Event emitted when inventory or storage size is updated (opcode 0x3092).
 */
public class InventorySizeUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte type; // 1=Inventory, 2=Storage
    private final int size;
    
    public InventorySizeUpdateEvent(String fullName, byte type, int size) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.size = size;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public byte getType() { return type; }
    public int getSize() { return size; }
    
    public boolean isInventory() { return type == 1; }
    public boolean isStorage() { return type == 2; }
    
    @Override
    public String toString() {
        return "InventorySizeUpdateEvent{type=" + type + ", size=" + size + "}";
    }
}
