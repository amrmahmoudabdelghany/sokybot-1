package org.sokybot.trade.events;

/**
 * Item or gold updated in the exchange window (self or partner side).
 */
public final class TradeItemAdded extends AbstractTradeGameEvent {

    private final int exchangeId;
    private final boolean selfOffer;
    private final byte slot;
    private final int itemRefId;
    private final int quantity;
    private final long goldAmount;

    public TradeItemAdded(String machineFullName,
            int exchangeId,
            boolean selfOffer,
            byte slot,
            int itemRefId,
            int quantity,
            long goldAmount) {
        super(machineFullName);
        this.exchangeId = exchangeId;
        this.selfOffer = selfOffer;
        this.slot = slot;
        this.itemRefId = itemRefId;
        this.quantity = quantity;
        this.goldAmount = goldAmount;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public boolean isSelfOffer() {
        return selfOffer;
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

    public long getGoldAmount() {
        return goldAmount;
    }
}
