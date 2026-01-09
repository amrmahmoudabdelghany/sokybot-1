package org.sokybot.gameevents.events.inventory;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when an item is obtained (picked up, looted, etc.).
 * Can represent inventory changes.
 */
public class ItemObtainedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int itemRefId;
    private final byte slot;
    private final short quantity;
    
    public ItemObtainedEvent(String machineFullName, int itemRefId, byte slot, short quantity) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.itemRefId = itemRefId;
        this.slot = slot;
        this.quantity = quantity;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }
    
    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getItemRefId() {
        return itemRefId;
    }
    
    public byte getSlot() {
        return slot;
    }
    
    public short getQuantity() {
        return quantity;
    }
}
