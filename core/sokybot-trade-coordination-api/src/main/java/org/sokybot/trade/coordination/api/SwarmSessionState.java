package org.sokybot.trade.coordination.api;

import java.util.Objects;

/**
 * Immutable snapshot of an in-process swarm trade session for one machine id key.
 */
public final class SwarmSessionState {

    private final String requestId;
    private final String partnerMachineId;
    private final SwarmSessionPhase phase;
    private final long startedAtEpochMs;
    private final SwarmRole localRole;

    public SwarmSessionState(
            String requestId,
            String partnerMachineId,
            SwarmSessionPhase phase,
            long startedAtEpochMs,
            SwarmRole localRole) {
        this.requestId = Objects.requireNonNull(requestId, "requestId").trim();
        this.partnerMachineId = Objects.requireNonNull(partnerMachineId, "partnerMachineId").trim();
        this.phase = Objects.requireNonNull(phase, "phase");
        this.startedAtEpochMs = startedAtEpochMs;
        this.localRole = Objects.requireNonNull(localRole, "localRole");
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPartnerMachineId() {
        return partnerMachineId;
    }

    public SwarmSessionPhase getPhase() {
        return phase;
    }

    public long getStartedAtEpochMs() {
        return startedAtEpochMs;
    }

    public SwarmRole getLocalRole() {
        return localRole;
    }

    public SwarmSessionState withPhase(SwarmSessionPhase newPhase) {
        return new SwarmSessionState(requestId, partnerMachineId, newPhase, startedAtEpochMs, localRole);
    }
}
