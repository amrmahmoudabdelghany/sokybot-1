package org.sokybot.swarm.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sokybot.navigation.api.WorldPoint;

public final class SwarmEntityDetectedEvent extends SwarmEvent {

    public enum DetectionKind {
        UNIQUE_GLOBAL_NOTIFY,
        UNIQUE_VISUAL_CONFIRM,
        WATCHED_REFID
    }

    private final int refId;
    private final String displayName;
    private final DetectionKind kind;
    private final List<WorldPoint> candidatePositions;
    private final int regionPackedSector;
    private final long expiresAtEpochMs;

    public SwarmEntityDetectedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            int refId,
            String displayName,
            DetectionKind kind,
            List<WorldPoint> candidatePositions,
            int regionPackedSector,
            long expiresAtEpochMs) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.refId = refId;
        this.displayName = displayName;
        this.kind = kind != null ? kind : DetectionKind.UNIQUE_GLOBAL_NOTIFY;
        this.candidatePositions = Collections.unmodifiableList(new ArrayList<>(candidatePositions == null
                ? Collections.emptyList()
                : candidatePositions));
        this.regionPackedSector = regionPackedSector;
        this.expiresAtEpochMs = expiresAtEpochMs;
    }

    public int getRefId() {
        return refId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DetectionKind getKind() {
        return kind;
    }

    public List<WorldPoint> getCandidatePositions() {
        return candidatePositions;
    }

    public int getRegionPackedSector() {
        return regionPackedSector;
    }

    public long getExpiresAtEpochMs() {
        return expiresAtEpochMs;
    }
}
