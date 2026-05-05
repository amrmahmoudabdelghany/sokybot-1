package org.sokybot.swarm.api.warroom;

import java.util.Objects;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #25 Hunting Grid: command a bot to move to a formation hold point.
 */
public final class SwarmFormationCommandEvent extends SwarmEvent {

    private final String targetMachineId;
    private final double holdX;
    private final double holdY;
    private final String formationId;

    public SwarmFormationCommandEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String targetMachineId,
            double holdX,
            double holdY,
            String formationId) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(targetMachineId, "targetMachineId");
        Objects.requireNonNull(formationId, "formationId");
        if (targetMachineId.trim().isEmpty()) {
            throw new IllegalArgumentException("targetMachineId must not be blank");
        }
        if (formationId.trim().isEmpty()) {
            throw new IllegalArgumentException("formationId must not be blank");
        }
        this.targetMachineId = targetMachineId.trim();
        this.holdX = holdX;
        this.holdY = holdY;
        this.formationId = formationId.trim();
    }

    public String getTargetMachineId() {
        return targetMachineId;
    }

    public double getHoldX() {
        return holdX;
    }

    public double getHoldY() {
        return holdY;
    }

    public String getFormationId() {
        return formationId;
    }
}
