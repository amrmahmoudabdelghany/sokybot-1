package org.sokybot.trade.events;

/**
 * Stall sale notification (opcode 0x30B7 action / buy patterns).
 */
public final class StallItemSold extends AbstractTradeGameEvent {

    private final int stallEntityId;
    private final int itemRefId;
    private final int quantity;
    private final long totalGold;
    private final String buyerName;

    public StallItemSold(String machineFullName,
            int stallEntityId,
            int itemRefId,
            int quantity,
            long totalGold,
            String buyerName) {
        super(machineFullName);
        this.stallEntityId = stallEntityId;
        this.itemRefId = itemRefId;
        this.quantity = quantity;
        this.totalGold = totalGold;
        this.buyerName = buyerName != null ? buyerName : "";
    }

    public int getStallEntityId() {
        return stallEntityId;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTotalGold() {
        return totalGold;
    }

    public String getBuyerName() {
        return buyerName;
    }
}
