package org.sokybot.gameevents.events.storage;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when items are taken from storage box (opcode 0xB558).
 */
public class StorageBoxTakeItemEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final int itemCount;
    
    public StorageBoxTakeItemEvent(String fullName, boolean success, int itemCount) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.itemCount = itemCount;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public int getItemCount() { return itemCount; }
    
    @Override
    public String toString() {
        return String.format("StorageBoxTakeItemEvent[%s, success=%b, items=%d]", 
            fullName, success, itemCount);
    }
}
