package org.sokybot.swarm.api.quartermaster;

import java.util.Objects;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #15: farmer releases the storage lock after dump (or abort).
 */
public final class SwarmStorageReleaseEvent extends SwarmEvent {

    private final String storageSessionId;
    private final String grantedToken;

    public SwarmStorageReleaseEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String storageSessionId,
            String grantedToken) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(storageSessionId, "storageSessionId");
        if (storageSessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("storageSessionId must be non-blank");
        }
        Objects.requireNonNull(grantedToken, "grantedToken");
        if (grantedToken.trim().isEmpty()) {
            throw new IllegalArgumentException("grantedToken must be non-blank");
        }
        this.storageSessionId = storageSessionId.trim();
        this.grantedToken = grantedToken.trim();
    }

    public String getStorageSessionId() {
        return storageSessionId;
    }

    public String getGrantedToken() {
        return grantedToken;
    }
}
