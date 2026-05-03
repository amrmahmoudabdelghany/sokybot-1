package org.sokybot.swarm.api.quartermaster;

import java.util.Objects;

import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #15: quartermaster authority granted a lock token for a storage session.
 */
public final class SwarmStorageLockGrantedEvent extends SwarmEvent {

    private final String storageSessionId;
    private final String grantedToken;
    private final long grantEpochMs;
    private final long ttlMs;

    public SwarmStorageLockGrantedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String storageSessionId,
            String grantedToken,
            long grantEpochMs,
            long ttlMs) {
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
        this.grantEpochMs = grantEpochMs;
        this.ttlMs = ttlMs;
    }

    public String getStorageSessionId() {
        return storageSessionId;
    }

    public String getGrantedToken() {
        return grantedToken;
    }

    public long getGrantEpochMs() {
        return grantEpochMs;
    }

    public long getTtlMs() {
        return ttlMs;
    }
}
