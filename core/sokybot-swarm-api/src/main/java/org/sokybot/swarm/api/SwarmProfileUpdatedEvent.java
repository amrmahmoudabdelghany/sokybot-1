package org.sokybot.swarm.api;

import java.util.Map;
import java.util.Objects;

/**
 * JVM-wide notification that a fleet profile's policy payload changed (Epic #16).
 */
public final class SwarmProfileUpdatedEvent extends SwarmEvent {

    private final String profileId;
    private final String groupName;
    private final String profileDisplayName;
    private final Map<String, String> scopeToSettingsJson;
    private final long profileVersionEpochMs;
    private final int expectedMachineCount;

    public SwarmProfileUpdatedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String profileId,
            String groupName,
            String profileDisplayName,
            Map<String, String> scopeToSettingsJson,
            long profileVersionEpochMs,
            int expectedMachineCount) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.profileId = Objects.requireNonNull(profileId, "profileId").trim();
        this.groupName = Objects.requireNonNull(groupName, "groupName").trim();
        if (this.profileId.isEmpty()) {
            throw new IllegalArgumentException("profileId must not be blank");
        }
        if (this.groupName.isEmpty()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }
        this.profileDisplayName = profileDisplayName == null ? "" : profileDisplayName.trim();
        this.scopeToSettingsJson = scopeToSettingsJson == null ? Map.of() : Map.copyOf(scopeToSettingsJson);
        this.profileVersionEpochMs = profileVersionEpochMs;
        this.expectedMachineCount = expectedMachineCount;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getProfileDisplayName() {
        return profileDisplayName;
    }

    public Map<String, String> getScopeToSettingsJson() {
        return scopeToSettingsJson;
    }

    public long getProfileVersionEpochMs() {
        return profileVersionEpochMs;
    }

    public int getExpectedMachineCount() {
        return expectedMachineCount;
    }
}
