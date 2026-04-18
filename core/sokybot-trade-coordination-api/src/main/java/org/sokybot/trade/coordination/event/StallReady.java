package org.sokybot.trade.coordination.event;

import org.sokybot.engine.api.EngineEvent;

import java.util.Objects;

/**
 * Mule announces stall UI is ready for listing / selling.
 */
public final class StallReady extends EngineEvent {

    private final String machineId;
    private final String stallId;

    public StallReady(String machineId, String stallId) {
        super("STALL_READY");
        this.machineId = Objects.requireNonNull(machineId, "machineId");
        this.stallId = Objects.requireNonNull(stallId, "stallId");
    }

    public String getMachineId() {
        return machineId;
    }

    public String getStallId() {
        return stallId;
    }
}
