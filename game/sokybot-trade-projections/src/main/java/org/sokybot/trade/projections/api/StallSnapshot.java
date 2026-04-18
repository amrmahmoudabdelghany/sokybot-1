package org.sokybot.trade.projections.api;

import java.util.Objects;

import org.sokybot.trade.events.StallItemSold;

/**
 * Read-model snapshot for the current machine's stall.
 */
public final class StallSnapshot {

    private final String machineFullName;
    private final boolean stallOpen;
    private final int stallEntityId;
    private final String stallTitle;
    private final StallItemSold lastSale;

    public StallSnapshot(String machineFullName,
            boolean stallOpen,
            int stallEntityId,
            String stallTitle,
            StallItemSold lastSale) {
        this.machineFullName = Objects.requireNonNull(machineFullName, "machineFullName");
        this.stallOpen = stallOpen;
        this.stallEntityId = stallEntityId;
        this.stallTitle = stallTitle != null ? stallTitle : "";
        this.lastSale = lastSale;
    }

    public String getMachineFullName() {
        return machineFullName;
    }

    public boolean isStallOpen() {
        return stallOpen;
    }

    /** -1 if unknown. */
    public int getStallEntityId() {
        return stallEntityId;
    }

    public String getStallTitle() {
        return stallTitle;
    }

    public StallItemSold getLastSale() {
        return lastSale;
    }
}
