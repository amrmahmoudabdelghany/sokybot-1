package org.sokybot.swarm.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * Broadcast: party roster needs a bot with a given fleet profile at the leader rally point (Epic #17).
 */
public final class SwarmRecruitmentEvent extends SwarmEvent {

    private final String recruitmentId;
    private final String leaderMachineId;
    private final String missingProfileId;
    private final WorldPoint rallyPoint;
    private final long bidDeadlineEpochMs;

    public SwarmRecruitmentEvent(
            String leaderMachineId,
            long timestampEpochMs,
            String recruitmentId,
            String missingProfileId,
            WorldPoint rallyPoint,
            long bidDeadlineEpochMs) {
        super(leaderMachineId, timestampEpochMs, recruitmentId);
        this.recruitmentId = Objects.requireNonNull(recruitmentId, "recruitmentId").trim();
        this.leaderMachineId = Objects.requireNonNull(leaderMachineId, "leaderMachineId").trim();
        this.missingProfileId = Objects.requireNonNull(missingProfileId, "missingProfileId").trim();
        this.rallyPoint = Objects.requireNonNull(rallyPoint, "rallyPoint");
        this.bidDeadlineEpochMs = bidDeadlineEpochMs;
        if (this.recruitmentId.isEmpty() || this.leaderMachineId.isEmpty() || this.missingProfileId.isEmpty()) {
            throw new IllegalArgumentException("recruitmentId, leaderMachineId, and missingProfileId must be non-blank");
        }
    }

    public String getRecruitmentId() {
        return recruitmentId;
    }

    public String getLeaderMachineId() {
        return leaderMachineId;
    }

    public String getMissingProfileId() {
        return missingProfileId;
    }

    public WorldPoint getRallyPoint() {
        return rallyPoint;
    }

    public long getBidDeadlineEpochMs() {
        return bidDeadlineEpochMs;
    }
}
