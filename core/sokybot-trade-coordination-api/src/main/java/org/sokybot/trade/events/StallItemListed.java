package org.sokybot.trade.events;

/**
 * Item listed on stall with price (best-effort parse; layout varies by shard).
 */
public final class StallItemListed extends AbstractTradeGameEvent {

    private final int stallEntityId;
    private final byte slot;
    private final int itemRefId;
    private final int quantity;
    private final long pricePerStack;

    public StallItemListed(String machineFullName,
            int stallEntityId,
            byte slot,
            int itemRefId,
            int quantity,
            long pricePerStack) {
        super(machineFullName);
        this.stallEntityId = stallEntityId;
        this.slot = slot;
        this.itemRefId = itemRefId;
        this.quantity = quantity;
        this.pricePerStack = pricePerStack;
    }

    public int getStallEntityId() {
        return stallEntityId;
    }

    public byte getSlot() {
        return slot;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getPricePerStack() {
        return pricePerStack;
    }
}
