package org.sokybot.swarm.api.router;

import java.util.Objects;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #24 Silk Road Router: designates the mule bot to begin its town-return / unload sequence.
 */
public final class SwarmMuleDispatchEvent extends SwarmEvent {

    private final String muleMachineId;
    private final String swarmGroupId;
    private final String dispatchReason;

    public SwarmMuleDispatchEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String muleMachineId,
            String swarmGroupId,
            String dispatchReason) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(muleMachineId, "muleMachineId");
        Objects.requireNonNull(swarmGroupId, "swarmGroupId");
        Objects.requireNonNull(dispatchReason, "dispatchReason");
        if (muleMachineId.trim().isEmpty()) {
            throw new IllegalArgumentException("muleMachineId must not be blank");
        }
        if (swarmGroupId.trim().isEmpty()) {
            throw new IllegalArgumentException("swarmGroupId must not be blank");
        }
        this.muleMachineId = muleMachineId.trim();
        this.swarmGroupId = swarmGroupId.trim();
        this.dispatchReason = dispatchReason.trim();
    }

    public String getMuleMachineId() {
        return muleMachineId;
    }

    public String getSwarmGroupId() {
        return swarmGroupId;
    }

    public String getDispatchReason() {
        return dispatchReason;
    }
}
