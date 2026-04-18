package org.sokybot.trade.events;

/**
 * Exchange cancelled / closed without completion (opcode 0x3088 pattern).
 */
public final class TradeExchangeCancelled extends AbstractTradeGameEvent {

    public TradeExchangeCancelled(String machineFullName) {
        super(machineFullName);
    }
}
