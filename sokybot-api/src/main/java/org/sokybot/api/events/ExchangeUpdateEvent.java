package org.sokybot.api.events;

/**
 * Event fired when exchange items are updated (opcode 0x3089).
 */
public class ExchangeUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int exchangeId;
    private final boolean selfUpdate;  // true if self updated, false if partner
    private final byte slot;
    private final int itemRefId;
    private final int quantity;
    private final long goldAmount;
    
    public ExchangeUpdateEvent(String fullName, int exchangeId, boolean selfUpdate,
                              byte slot, int itemRefId, int quantity, long goldAmount) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.exchangeId = exchangeId;
        this.selfUpdate = selfUpdate;
        this.slot = slot;
        this.itemRefId = itemRefId;
        this.quantity = quantity;
        this.goldAmount = goldAmount;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getExchangeId() { return exchangeId; }
    public boolean isSelfUpdate() { return selfUpdate; }
    public byte getSlot() { return slot; }
    public int getItemRefId() { return itemRefId; }
    public int getQuantity() { return quantity; }
    public long getGoldAmount() { return goldAmount; }
    
    @Override
    public String toString() {
        return String.format("ExchangeUpdateEvent[%s, exchange=%d, self=%b, slot=%d, item=%d, qty=%d, gold=%d]", 
            fullName, exchangeId, selfUpdate, slot, itemRefId, quantity, goldAmount);
    }
}
