package org.sokybot.party.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Party roster matrix: desired headcount per fleet profile id (Epic #17).
 * Persisted under settings scope {@code party-matrix}.
 */
public final class PartyMatrixSettings {

    private boolean rosterMatrixEnabled;

    private final Map<String, Integer> profileIdToRequiredCount = new LinkedHashMap<>();

    private long gapDebounceMs = 5000L;

    private long recruitmentBidWindowMs = 2000L;

    private float inviteProximityRadius = 20.0f;

    public boolean isRosterMatrixEnabled() {
        return rosterMatrixEnabled;
    }

    public void setRosterMatrixEnabled(boolean rosterMatrixEnabled) {
        this.rosterMatrixEnabled = rosterMatrixEnabled;
    }

    public Map<String, Integer> getProfileIdToRequiredCount() {
        return Map.copyOf(profileIdToRequiredCount);
    }

    public void setProfileIdToRequiredCount(Map<String, Integer> map) {
        profileIdToRequiredCount.clear();
        if (map != null) {
            profileIdToRequiredCount.putAll(map);
        }
    }

    public long getGapDebounceMs() {
        return gapDebounceMs;
    }

    public void setGapDebounceMs(long gapDebounceMs) {
        this.gapDebounceMs = gapDebounceMs;
    }

    public long getRecruitmentBidWindowMs() {
        return recruitmentBidWindowMs;
    }

    public void setRecruitmentBidWindowMs(long recruitmentBidWindowMs) {
        this.recruitmentBidWindowMs = recruitmentBidWindowMs;
    }

    public float getInviteProximityRadius() {
        return inviteProximityRadius;
    }

    public void setInviteProximityRadius(float inviteProximityRadius) {
        this.inviteProximityRadius = inviteProximityRadius;
    }
}
