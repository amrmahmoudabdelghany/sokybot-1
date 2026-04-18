package org.sokybot.trade.events;

/**
 * P2P trade/exchange UI opened (server notifies session started).
 */
public final class TradeWindowOpened extends AbstractTradeGameEvent {

    private final int otherPlayerUniqueId;

    public TradeWindowOpened(String machineFullName, int otherPlayerUniqueId) {
        super(machineFullName);
        this.otherPlayerUniqueId = otherPlayerUniqueId;
    }

    public int getOtherPlayerUniqueId() {
        return otherPlayerUniqueId;
    }
}
