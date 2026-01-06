package org.sokybot.api.events;

/**
 * Event emitted when an inventory item is updated (opcode 0x3040).
 * Updates can include: RefObjID, OptLevel, Variance, Quantity, Durability, State, MagicParams.
 */
public class InventoryItemUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte slot;
    private final byte updateFlags;  // Bitmask indicating what was updated
    private final Integer itemId;     // Only if RefObjID flag set
    private final Byte optLevel;      // Only if OptLevel flag set
    private final Integer quantity;   // Only if Quantity flag set
    private final Integer durability; // Only if Durability flag set
    
    public InventoryItemUpdateEvent(String fullName, byte slot, byte updateFlags,
            Integer itemId, Byte optLevel, Integer quantity, Integer durability) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.slot = slot;
        this.updateFlags = updateFlags;
        this.itemId = itemId;
        this.optLevel = optLevel;
        this.quantity = quantity;
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
    
    public byte getSlot() { return slot; }
    public byte getUpdateFlags() { return updateFlags; }
    public Integer getItemId() { return itemId; }
    public Byte getOptLevel() { return optLevel; }
    public Integer getQuantity() { return quantity; }
    public Integer getDurability() { return durability; }
    
    // Update flag constants (from RSBot ItemUpdateFlag enum)
    public static final byte FLAG_REF_OBJ_ID = 0x01;
    public static final byte FLAG_OPT_LEVEL = 0x02;
    public static final byte FLAG_VARIANCE = 0x04;
    public static final byte FLAG_QUANTITY = 0x08;
    public static final byte FLAG_DURABILITY = 0x10;
    public static final byte FLAG_STATE = 0x20;
    public static final byte FLAG_MAG_PARAMS = 0x40;
    
    @Override
    public String toString() {
        return "InventoryItemUpdateEvent{slot=" + slot + 
               ", updateFlags=0x" + Integer.toHexString(updateFlags & 0xFF) +
               ", itemId=" + itemId +
               ", quantity=" + quantity + "}";
    }
}
