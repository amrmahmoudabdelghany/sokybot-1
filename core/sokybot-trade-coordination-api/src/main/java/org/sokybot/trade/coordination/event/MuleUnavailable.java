package org.sokybot.trade.coordination.event;

import org.sokybot.engine.api.EngineEvent;

import java.util.Objects;

/**
 * Broadcast when a mule unregisters.
 */
public final class MuleUnavailable extends EngineEvent {

    private final String sourceMachineId;

    public MuleUnavailable(String sourceMachineId) {
        super("MULE_UNAVAILABLE");
        this.sourceMachineId = Objects.requireNonNull(sourceMachineId, "sourceMachineId");
    }

    public String getSourceMachineId() {
        return sourceMachineId;
    }
}
