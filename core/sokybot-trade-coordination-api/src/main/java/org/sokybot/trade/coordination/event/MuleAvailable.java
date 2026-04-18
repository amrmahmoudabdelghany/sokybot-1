package org.sokybot.trade.coordination.event;

import org.sokybot.engine.api.EngineEvent;

import java.util.Objects;

/**
 * Broadcast when a mule registers (all engines receive; behaviors filter by role).
 */
public final class MuleAvailable extends EngineEvent {

    private final String sourceMachineId;
    private final String ticketId;
    private final int freeCargoSlots;
    private final String locationHint;

    public MuleAvailable(String sourceMachineId, String ticketId, int freeCargoSlots, String locationHint) {
        super("MULE_AVAILABLE");
        this.sourceMachineId = Objects.requireNonNull(sourceMachineId, "sourceMachineId");
        this.ticketId = Objects.requireNonNull(ticketId, "ticketId");
        this.freeCargoSlots = freeCargoSlots;
        this.locationHint = locationHint != null ? locationHint : "";
    }

    public String getSourceMachineId() {
        return sourceMachineId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public int getFreeCargoSlots() {
        return freeCargoSlots;
    }

    public String getLocationHint() {
        return locationHint;
    }
}
