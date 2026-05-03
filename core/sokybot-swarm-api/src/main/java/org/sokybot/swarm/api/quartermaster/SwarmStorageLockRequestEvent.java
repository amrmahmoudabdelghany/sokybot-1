package org.sokybot.swarm.api.quartermaster;

import java.util.Objects;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #15: farmer requests exclusive access to shared storage for a session.
 */
public final class SwarmStorageLockRequestEvent extends SwarmEvent {

    private final String storageSessionId;
    private final String swarmGroupId;

    public SwarmStorageLockRequestEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String storageSessionId,
            String swarmGroupId) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(storageSessionId, "storageSessionId");
        if (storageSessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("storageSessionId must be non-blank");
        }
        Objects.requireNonNull(swarmGroupId, "swarmGroupId");
        if (swarmGroupId.trim().isEmpty()) {
            throw new IllegalArgumentException("swarmGroupId must be non-blank");
        }
        this.storageSessionId = storageSessionId.trim();
        this.swarmGroupId = swarmGroupId.trim();
    }

    public String getStorageSessionId() {
        return storageSessionId;
    }

    public String getSwarmGroupId() {
        return swarmGroupId;
    }
}
