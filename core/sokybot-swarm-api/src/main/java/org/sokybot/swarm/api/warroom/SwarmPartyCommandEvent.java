package org.sokybot.swarm.api.warroom;

import java.util.Objects;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #23 War Room: command a bot to leave its current party and join the party led by another machine.
 */
public final class SwarmPartyCommandEvent extends SwarmEvent {

    private final String targetMachineId;
    private final String targetLeaderMachineId;

    public SwarmPartyCommandEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String targetMachineId,
            String targetLeaderMachineId) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(targetMachineId, "targetMachineId");
        Objects.requireNonNull(targetLeaderMachineId, "targetLeaderMachineId");
        if (targetMachineId.trim().isEmpty()) {
            throw new IllegalArgumentException("targetMachineId must not be blank");
        }
        if (targetLeaderMachineId.trim().isEmpty()) {
            throw new IllegalArgumentException("targetLeaderMachineId must not be blank");
        }
        this.targetMachineId = targetMachineId.trim();
        this.targetLeaderMachineId = targetLeaderMachineId.trim();
    }

    public String getTargetMachineId() {
        return targetMachineId;
    }

    public String getTargetLeaderMachineId() {
        return targetLeaderMachineId;
    }
}
