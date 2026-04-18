package org.sokybot.trade.coordination.api;

import java.util.Objects;

/**
 * Snapshot returned to farmers when selecting a mule.
 */
public final class MuleHandle {

    private final String ticketId;
    private final String machineId;
    private final int freeCargoSlots;
    private final String locationHint;
    private final long registeredAtEpochMillis;

    public MuleHandle(String ticketId,
            String machineId,
            int freeCargoSlots,
            String locationHint,
            long registeredAtEpochMillis) {
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.machineId = Objects.requireNonNull(machineId, "machineId");
        this.freeCargoSlots = freeCargoSlots;
        this.locationHint = locationHint != null ? locationHint : "";
        this.registeredAtEpochMillis = registeredAtEpochMillis;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getMachineId() {
        return machineId;
    }

    public int getFreeCargoSlots() {
        return freeCargoSlots;
    }

    public String getLocationHint() {
        return locationHint;
    }

    public long getRegisteredAtEpochMillis() {
        return registeredAtEpochMillis;
    }
}
