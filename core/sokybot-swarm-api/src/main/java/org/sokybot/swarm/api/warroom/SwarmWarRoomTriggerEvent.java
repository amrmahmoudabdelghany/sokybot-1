package org.sokybot.swarm.api.warroom;

import java.util.Objects;
import java.util.UUID;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #23 War Room: signals the orchestrator to run roster optimization for a swarm group.
 */
public final class SwarmWarRoomTriggerEvent extends SwarmEvent {

    private final String swarmGroupId;

    public SwarmWarRoomTriggerEvent(String swarmGroupId) {
        this("war-room", System.currentTimeMillis(), UUID.randomUUID().toString(), swarmGroupId);
    }

    public SwarmWarRoomTriggerEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String swarmGroupId) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(swarmGroupId, "swarmGroupId");
        String trimmed = swarmGroupId.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("swarmGroupId must not be blank");
        }
        this.swarmGroupId = trimmed;
    }

    public String getSwarmGroupId() {
        return swarmGroupId;
    }
}
