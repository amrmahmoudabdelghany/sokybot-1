package org.sokybot.trade.coordination.api;

import java.util.Objects;

/**
 * Registration payload when a mule bot advertises availability to receive farmer loot.
 */
public final class MuleIntent {

    private final String machineId;
    private final int freeCargoSlots;
    private final String locationHint;

    public MuleIntent(String machineId, int freeCargoSlots, String locationHint) {
        String mid = Objects.requireNonNull(machineId, "machineId").trim();
        if (mid.isEmpty()) {
            throw new IllegalArgumentException("machineId cannot be empty");
        }
        if (freeCargoSlots < 0) {
            throw new IllegalArgumentException("freeCargoSlots cannot be negative");
        }
        this.machineId = mid;
        this.freeCargoSlots = freeCargoSlots;
        this.locationHint = locationHint != null ? locationHint.trim() : "";
    }

    public String getMachineId() {
        return machineId;
    }

    public int getFreeCargoSlots() {
        return freeCargoSlots;
    }

    /**
     * Optional human-readable or navigation hint (e.g. region); may be empty.
     */
    public String getLocationHint() {
        return locationHint;
    }
}
