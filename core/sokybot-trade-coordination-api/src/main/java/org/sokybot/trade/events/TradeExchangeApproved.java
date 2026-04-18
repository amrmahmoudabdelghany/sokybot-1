package org.sokybot.trade.events;

/**
 * Exchange approved / finalized by server (opcode 0x3087 pattern).
 */
public final class TradeExchangeApproved extends AbstractTradeGameEvent {

    private final int exchangeId;
    private final boolean success;

    public TradeExchangeApproved(String machineFullName, int exchangeId, boolean success) {
        super(machineFullName);
        this.exchangeId = exchangeId;
        this.success = success;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public boolean isSuccess() {
        return success;
    }
}
