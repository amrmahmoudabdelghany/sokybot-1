package org.sokybot.swarm.api;

import java.util.Objects;

/**
 * Idle-eligible bot offers to fill a {@link SwarmRecruitmentEvent} (Epic #17).
 */
public final class SwarmRecruitmentBidEvent extends SwarmEvent {

    private final String recruitmentId;
    private final String bidderMachineId;
    private final String profileId;
    private final String bidderCharName;

    public SwarmRecruitmentBidEvent(
            String bidderMachineId,
            long timestampEpochMs,
            String requestId,
            String recruitmentId,
            String profileId,
            String bidderCharName) {
        super(bidderMachineId, timestampEpochMs, requestId);
        this.recruitmentId = Objects.requireNonNull(recruitmentId, "recruitmentId").trim();
        this.bidderMachineId = Objects.requireNonNull(bidderMachineId, "bidderMachineId").trim();
        this.profileId = Objects.requireNonNull(profileId, "profileId").trim();
        this.bidderCharName = bidderCharName == null ? "" : bidderCharName.trim();
        if (this.recruitmentId.isEmpty() || this.bidderMachineId.isEmpty() || this.profileId.isEmpty()) {
            throw new IllegalArgumentException("recruitmentId, bidderMachineId, and profileId must be non-blank");
        }
    }

    public String getRecruitmentId() {
        return recruitmentId;
    }

    public String getBidderMachineId() {
        return bidderMachineId;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getBidderCharName() {
        return bidderCharName;
    }
}
