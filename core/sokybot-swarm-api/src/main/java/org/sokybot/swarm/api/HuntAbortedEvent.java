package org.sokybot.swarm.api;

import java.util.Objects;

public final class HuntAbortedEvent extends SwarmEvent {

    public enum Reason {
        TIMEOUT,
        NAVIGATION_FAILED,
        TARGET_NOT_FOUND,
        ALREADY_DEAD,
        HUNTER_DISCONNECTED,
        HUNTER_DIED,
        NO_HUNTER_AVAILABLE
    }

    private final String huntId;
    private final String hunterMachineId;
    private final int targetRefId;
    private final Reason reason;

    public HuntAbortedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String huntId,
            String hunterMachineId,
            int targetRefId,
            Reason reason) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.huntId = Objects.requireNonNull(huntId, "huntId").trim();
        this.hunterMachineId = hunterMachineId == null ? "" : hunterMachineId.trim();
        this.targetRefId = targetRefId;
        this.reason = reason != null ? reason : Reason.TIMEOUT;
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

    public Reason getReason() {
        return reason;
    }
}
