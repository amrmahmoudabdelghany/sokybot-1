package org.sokybot.swarm.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * Assignee bot should navigate to {@link #getDestination()} (Epic #17 roster rally, etc.).
 */
public final class SwarmDispatchEvent extends SwarmEvent {

    private final String dispatchId;
    private final String assigneeMachineId;
    private final WorldPoint destination;
    private final String recruitmentId;
    private final String purpose;

    public SwarmDispatchEvent(
            String assigneeMachineId,
            long timestampEpochMs,
            String dispatchId,
            WorldPoint destination,
            String recruitmentId,
            String purpose) {
        super(assigneeMachineId, timestampEpochMs, dispatchId);
        this.dispatchId = Objects.requireNonNull(dispatchId, "dispatchId").trim();
        this.assigneeMachineId = Objects.requireNonNull(assigneeMachineId, "assigneeMachineId").trim();
        this.destination = Objects.requireNonNull(destination, "destination");
        this.recruitmentId = recruitmentId == null ? "" : recruitmentId.trim();
        this.purpose = purpose == null ? "" : purpose.trim();
        if (this.dispatchId.isEmpty() || this.assigneeMachineId.isEmpty()) {
            throw new IllegalArgumentException("dispatchId and assigneeMachineId must be non-blank");
        }
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public String getAssigneeMachineId() {
        return assigneeMachineId;
    }

    public WorldPoint getDestination() {
        return destination;
    }

    public String getRecruitmentId() {
        return recruitmentId;
    }

    public String getPurpose() {
        return purpose;
    }
}
