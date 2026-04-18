package org.sokybot.trade.projections.api;

import java.util.Objects;

import org.sokybot.trade.events.TradeItemAdded;

/**
 * Read-model snapshot for the current machine's exchange window (best-effort).
 */
public final class TradeSnapshot {

    private final String machineFullName;
    private final boolean exchangeActive;
    private final int otherPlayerUniqueId;
    private final int exchangeId;
    private final TradeItemAdded lastSlotUpdate;

    public TradeSnapshot(String machineFullName,
            boolean exchangeActive,
            int otherPlayerUniqueId,
            int exchangeId,
            TradeItemAdded lastSlotUpdate) {
        this.machineFullName = Objects.requireNonNull(machineFullName, "machineFullName");
        this.exchangeActive = exchangeActive;
        this.otherPlayerUniqueId = otherPlayerUniqueId;
        this.exchangeId = exchangeId;
        this.lastSlotUpdate = lastSlotUpdate;
    }

    public String getMachineFullName() {
        return machineFullName;
    }

    public boolean isExchangeActive() {
        return exchangeActive;
    }

    /** -1 if unknown. */
    public int getOtherPlayerUniqueId() {
        return otherPlayerUniqueId;
    }

    /** -1 if unknown. */
    public int getExchangeId() {
        return exchangeId;
    }

    public TradeItemAdded getLastSlotUpdate() {
        return lastSlotUpdate;
    }
}
