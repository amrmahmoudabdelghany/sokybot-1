package org.sokybot.swarm.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * JVM-wide lure coordination signal (Black Hole / synchronized swarm luring).
 */
public final class SwarmLureCycleEvent extends SwarmEvent {

    private final SwarmLurePhase phase;
    private final long syncEpochMs;
    private final long agreedTimeToTargetMs;
    private final String anchorMachineId;
    private final WorldPoint anchorPoint;
    private final float fanRadiusWorld;
    private final List<String> assignedLurers;

    public SwarmLureCycleEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            SwarmLurePhase phase,
            long syncEpochMs,
            long agreedTimeToTargetMs,
            String anchorMachineId,
            WorldPoint anchorPoint,
            float fanRadiusWorld,
            List<String> assignedLurers) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.phase = Objects.requireNonNull(phase, "phase");
        this.syncEpochMs = syncEpochMs;
        this.agreedTimeToTargetMs = agreedTimeToTargetMs;
        this.anchorMachineId = Objects.requireNonNull(anchorMachineId, "anchorMachineId").trim();
        this.anchorPoint = Objects.requireNonNull(anchorPoint, "anchorPoint");
        this.fanRadiusWorld = fanRadiusWorld;
        if (this.anchorMachineId.isEmpty()) {
            throw new IllegalArgumentException("anchorMachineId must not be blank");
        }
        List<String> copy = new ArrayList<>();
        if (assignedLurers != null) {
            for (String id : assignedLurers) {
                if (id == null) {
                    continue;
                }
                String t = id.trim();
                if (!t.isEmpty()) {
                    copy.add(t);
                }
            }
        }
        this.assignedLurers = Collections.unmodifiableList(copy);
    }

    public SwarmLurePhase getPhase() {
        return phase;
    }

    public long getSyncEpochMs() {
        return syncEpochMs;
    }

    public long getAgreedTimeToTargetMs() {
        return agreedTimeToTargetMs;
    }

    public String getAnchorMachineId() {
        return anchorMachineId;
    }

    public WorldPoint getAnchorPoint() {
        return anchorPoint;
    }

    public float getFanRadiusWorld() {
        return fanRadiusWorld;
    }

    public List<String> getAssignedLurers() {
        return assignedLurers;
    }
}
