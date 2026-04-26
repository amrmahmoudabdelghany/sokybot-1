package org.sokybot.swarm.api;

import java.util.Objects;

public final class HuntCompletedEvent extends SwarmEvent {

    private final String huntId;
    private final String hunterMachineId;
    private final int targetRefId;

    public HuntCompletedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String huntId,
            String hunterMachineId,
            int targetRefId) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.huntId = Objects.requireNonNull(huntId, "huntId").trim();
        this.hunterMachineId = Objects.requireNonNull(hunterMachineId, "hunterMachineId").trim();
        this.targetRefId = targetRefId;
    }

    public String getHuntId() {
        return huntId;
    }

    public String getHunterMachineId() {
        return hunterMachineId;
    }

    public int getTargetRefId() {
        return targetRefId;
    }
}
