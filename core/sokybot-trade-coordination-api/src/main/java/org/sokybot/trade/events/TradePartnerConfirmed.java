package org.sokybot.trade.events;

/**
 * Partner pressed confirm on their side of the exchange (opcode 0x3086 pattern).
 */
public final class TradePartnerConfirmed extends AbstractTradeGameEvent {

    private final int exchangeId;
    private final boolean selfConfirmed;

    public TradePartnerConfirmed(String machineFullName, int exchangeId, boolean selfConfirmed) {
        super(machineFullName);
        this.exchangeId = exchangeId;
        this.selfConfirmed = selfConfirmed;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public boolean isSelfConfirmed() {
        return selfConfirmed;
    }
}
