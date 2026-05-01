package org.sokybot.swarm.api;

import java.util.Objects;

/**
 * Published after a fleet profile alignment pass completes (Epic #16).
 */
public final class SwarmFleetAlignedEvent extends SwarmEvent {

    private final String profileId;
    private final int expectedCount;
    private final int successCount;

    public SwarmFleetAlignedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String profileId,
            int expectedCount,
            int successCount) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.profileId = Objects.requireNonNull(profileId, "profileId").trim();
        if (this.profileId.isEmpty()) {
            throw new IllegalArgumentException("profileId must not be blank");
        }
        this.expectedCount = expectedCount;
        this.successCount = successCount;
    }

    public String getProfileId() {
        return profileId;
    }

    public int getExpectedCount() {
        return expectedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }
}
