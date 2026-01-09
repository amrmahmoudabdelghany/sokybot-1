package org.sokybot.api.events;

/**
 * Event emitted when an inventory operation occurs (opcode 0xB034).
 * Operations include: move, pickup, drop, buy, sell, deposit, withdraw, etc.
 */
public class InventoryOperationEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte operationType;
    private final boolean success;
    private final byte errorCode;     // Only if !success
    
    // Operation-specific data
    private final Byte sourceSlot;
    private final Byte destSlot;
    private final Integer amount;
    private final Integer itemId;
    private final Long goldAmount;
    
    public InventoryOperationEvent(String fullName, byte operationType, boolean success, byte errorCode,
            Byte sourceSlot, Byte destSlot, Integer amount, Integer itemId, Long goldAmount) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.operationType = operationType;
        this.success = success;
        this.errorCode = errorCode;
        this.sourceSlot = sourceSlot;
        this.destSlot = destSlot;
        this.amount = amount;
        this.itemId = itemId;
        this.goldAmount = goldAmount;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public byte getOperationType() { return operationType; }
    public boolean isSuccess() { return success; }
    public byte getErrorCode() { return errorCode; }
    public Byte getSourceSlot() { return sourceSlot; }
    public Byte getDestSlot() { return destSlot; }
    public Integer getAmount() { return amount; }
    public Integer getItemId() { return itemId; }
    public Long getGoldAmount() { return goldAmount; }
    
    // Operation type constants (from RSBot InventoryOperation enum)
    public static final byte OP_MOVE_SLOTS = 0x00;
    public static final byte OP_MOVE_STORAGE = 0x01;
    public static final byte OP_DEPOSIT_ITEM = 0x02;
    public static final byte OP_WITHDRAW_ITEM = 0x03;
    public static final byte OP_PICK_ITEM = 0x04;
    public static final byte OP_DROP_ITEM = 0x05;
    public static final byte OP_BUY_ITEM = 0x06;
    public static final byte OP_SELL_ITEM = 0x07;
    public static final byte OP_DROP_GOLD = 0x08;
    public static final byte OP_DEPOSIT_GOLD = 0x09;
    public static final byte OP_WITHDRAW_GOLD = 0x0A;
    public static final byte OP_ADD_BY_SERVER = 0x0B;
    public static final byte OP_DELETE_BY_SERVER = 0x0C;
    public static final byte OP_GUILD_DEPOSIT = 0x1A;
    public static final byte OP_GUILD_WITHDRAW = 0x1B;
    
    public boolean isPickup() { return operationType == OP_PICK_ITEM; }
    public boolean isDrop() { return operationType == OP_DROP_ITEM; }
    public boolean isBuy() { return operationType == OP_BUY_ITEM; }
    public boolean isSell() { return operationType == OP_SELL_ITEM; }
    
    @Override
    public String toString() {
        return "InventoryOperationEvent{operationType=" + operationType + 
               ", success=" + success +
               ", sourceSlot=" + sourceSlot +
               ", destSlot=" + destSlot +
               ", amount=" + amount + "}";
    }
}
